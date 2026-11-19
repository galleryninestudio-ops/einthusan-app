package com.bench.einthusan.download

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray

/**
 * The queue. Small JSON list in prefs, chunk files on disk under the app's
 * external folder, and a live map of byte counters the UI can collect.
 */
object DlStore {

    private const val KEY = "tasks_v2"

    private val lock = Any()

    /** id -> live counters, only for tasks currently moving. */
    private val _progress = MutableStateFlow<Map<String, DlProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DlProgress>> = _progress

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences("downloads", Context.MODE_PRIVATE)

    fun workDir(context: Context): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "downloads").apply { mkdirs() }

    fun taskDir(context: Context, id: String): File =
        File(workDir(context), id).apply { mkdirs() }

    fun all(context: Context): List<DlTask> = synchronized(lock) {
        val raw = prefs(context).getString(KEY, "[]") ?: "[]"
        runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let { DlTask.from(it) }
            }
        }.getOrDefault(emptyList()).sortedByDescending { it.createdAt }
    }

    fun find(context: Context, id: String): DlTask? = all(context).firstOrNull { it.id == id }

    fun findForMovie(context: Context, movieId: String): DlTask? =
        all(context).firstOrNull { it.movieId == movieId }

    fun put(context: Context, task: DlTask) = synchronized(lock) {
        val next = all(context).filterNot { it.id == task.id } + task
        prefs(context).edit().putString(KEY, DlTask.array(next).toString()).apply()
    }

    /** Do not recreate a row removed while its transfer was finishing. */
    fun update(context: Context, id: String, transform: (DlTask) -> DlTask) = synchronized(lock) {
        val current = find(context, id) ?: return@synchronized
        put(context, transform(current))
    }

    fun remove(context: Context, id: String) = synchronized(lock) {
        val next = all(context).filterNot { it.id == id }
        prefs(context).edit().putString(KEY, DlTask.array(next).toString()).apply()
        taskDir(context, id).deleteRecursively()
        setProgress(id, null)
    }

    fun setProgress(id: String, p: DlProgress?) {
        _progress.update { current ->
            current.toMutableMap().apply { if (p == null) remove(id) else put(id, p) }
        }
    }

    /** Bytes already on disk across every chunk of a task. */
    fun bytesOnDisk(context: Context, task: DlTask): Long {
        val dir = taskDir(context, task.id)
        val files = dir.listFiles { f -> f.name.endsWith(".part") } ?: return 0L
        return files.sumOf { it.length() }
    }

    fun isActive(task: DlTask): Boolean = task.status == DlStatus.RUNNING || task.status == DlStatus.QUEUED

    /**
     * Create a queue entry; the caller starts DownloadService. Same film twice returns the
     * task already on disk instead of starting a second copy.
     */
    fun enqueue(
        context: Context,
        movieId: String,
        title: String,
        poster: String?,
        url: String,
    ): DlTask {
        all(context).firstOrNull {
            it.movieId == movieId && (it.status == DlStatus.QUEUED || it.status == DlStatus.RUNNING || it.status == DlStatus.DONE)
        }?.let { return it }

        val safe = title.replace(Regex("[^A-Za-z0-9 _-]"), "").trim().ifEmpty { "film" }
        val task = DlTask(
            id = java.util.UUID.randomUUID().toString(),
            movieId = movieId,
            title = title,
            poster = poster,
            url = url,
            fileName = safe,
            total = -1L,
            chunks = DlTask.DEFAULT_CHUNKS,
            status = DlStatus.QUEUED,
            createdAt = System.currentTimeMillis(),
        )
        put(context, task)
        return task
    }
}
