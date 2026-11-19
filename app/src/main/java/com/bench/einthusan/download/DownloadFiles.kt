package com.bench.einthusan.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/** Publication and lookup of completed media, separate from temporary transfer parts. */
internal object DownloadFiles {
    fun playUri(task: DlTask): String? = task.playUri ?: legacyFile(task).takeIf { it.exists() }
        ?.let { Uri.fromFile(it).toString() }

    fun delete(context: Context, task: DlTask) {
        val uri = playUri(task)?.let(Uri::parse) ?: return
        runCatching {
            when (uri.scheme) {
                "content" -> context.contentResolver.delete(uri, null, null)
                "file" -> uri.path?.let { File(it).delete() }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun legacyFile(task: DlTask): File = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
        "Einthusan/${task.fileName}.mp4",
    )

    /**
     * Put the finished film in Movies/Einthusan. MediaStore for Android 10 and
     * up so it shows in gallery apps; a direct file write on older devices.
     */
    fun publish(context: Context, task: DlTask, merged: File): String {
        val name = task.fileName.ifBlank { "film" } + ".mp4"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/Einthusan")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create the media entry")

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    merged.inputStream().buffered(1 shl 20).use { it.copyTo(out, 512 * 1024) }
                } ?: error("Could not open the media entry for writing")
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (error: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw error
            }
            merged.delete()
            return uri.toString()
        }

        @Suppress("DEPRECATION")
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "Einthusan",
        ).apply { mkdirs() }
        val dest = File(dir, name)
        merged.copyTo(dest, overwrite = true)
        merged.delete()
        return "file://" + dest.absolutePath
    }

}
