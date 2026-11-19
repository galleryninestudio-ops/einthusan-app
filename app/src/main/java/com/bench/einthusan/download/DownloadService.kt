package com.bench.einthusan.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps the queue moving with the app in the background, and shows the same
 * numbers the Library screen shows. One task at a time; the rest wait their
 * turn in order.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val queueLock = Mutex()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, build(null))
        scope.launch {
            queueLock.withLock {
                drain()
                if (stopSelfResult(startId)) stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }
        return START_STICKY
    }

    private suspend fun drain() {
        while (true) {
            val next = DlStore.all(this).firstOrNull {
                it.status == DlStatus.QUEUED ||
                    (it.status == DlStatus.RUNNING && !DlEngine.isRunning(it.id))
            } ?: break

            val ticker = scope.launch {
                while (true) {
                    val active = DlStore.all(this@DownloadService)
                        .firstOrNull { DlEngine.isRunning(it.id) }
                    notify(active)
                    delay(1000)
                }
            }

            try {
                DlEngine.run(this, next)
            } finally {
                ticker.cancel()
            }
        }
    }

    private fun notify(task: DlTask?) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIF_ID, build(task))
    }

    private fun build(task: DlTask?): Notification {
        ensureChannel()

        if (task == null) {
            return NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Einthusan")
                .setContentText("Preparing download…")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        }

        val progress = DlStore.progress.value[task.id]
        val bytes = progress?.bytes ?: DlStore.bytesOnDisk(this, task)
        val total = progress?.total ?: task.total
        val percent = if (total > 0L) ((bytes * 100) / total).toInt() else 0

        val builder = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(task.title)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        if (total > 0L) {
            builder.setProgress(100, percent, false)
            val mb = bytes / (1024 * 1024)
            val of = total / (1024 * 1024)
            val speed = progress?.speedBps ?: 0L
            val mbps = if (speed > 0) String.format(java.util.Locale.US, "%.1f MB/s", speed / 1048576.0) else ""
            builder.setContentText("$percent% · $mb of $of MB  $mbps")
        } else {
            builder.setProgress(0, 0, true)
            builder.setContentText("Downloading…")
        }

        return builder.build()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Films downloading to the device"
                setShowBadge(false)
            }
        )
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL = "downloads"
        private const val NOTIF_ID = 4211

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
