package com.bench.einthusan.download

import org.json.JSONArray
import org.json.JSONObject

enum class DlStatus { QUEUED, RUNNING, PAUSED, DONE, FAILED }

/**
 * One film on its way to the device. Chunks live beside it on disk, so a task
 * can be stopped, the app killed, the phone rebooted, and the next run picks up
 * exactly where each chunk stopped — byte for byte.
 */
data class DlTask(
    val id: String,
    val movieId: String,
    val title: String,
    val poster: String?,
    val url: String,
    val fileName: String,
    val total: Long,
    val chunks: Int,
    val status: DlStatus,
    val createdAt: Long,
    val error: String? = null,
    /** content:// or file:// URI of the finished film, once it is published. */
    val playUri: String? = null,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("movieId", movieId)
        .put("title", title)
        .put("poster", poster ?: "")
        .put("url", url)
        .put("fileName", fileName)
        .put("total", total)
        .put("chunks", chunks)
        .put("status", status.name)
        .put("createdAt", createdAt)
        .put("error", error ?: "")
        .put("playUri", playUri ?: "")

    companion object {
        fun from(o: JSONObject) = DlTask(
            id = o.optString("id"),
            movieId = o.optString("movieId"),
            title = o.optString("title"),
            poster = o.optString("poster").takeIf { it.isNotBlank() },
            url = o.optString("url"),
            fileName = o.optString("fileName"),
            total = o.optLong("total"),
            chunks = o.optInt("chunks", DEFAULT_CHUNKS),
            status = runCatching { DlStatus.valueOf(o.optString("status")) }.getOrDefault(DlStatus.QUEUED),
            createdAt = o.optLong("createdAt"),
            error = o.optString("error").takeIf { it.isNotBlank() },
            playUri = o.optString("playUri").takeIf { it.isNotBlank() },
        )

        fun array(list: List<DlTask>): JSONArray = JSONArray().also { arr -> list.forEach { arr.put(it.toJson()) } }

        const val DEFAULT_CHUNKS = 6
    }
}

/** Live numbers for a running task, pushed to the UI as they change. */
data class DlProgress(
    val bytes: Long,
    val total: Long,
    val speedBps: Long,
) {
    val fraction: Float get() = if (total > 0L) (bytes.toFloat() / total).coerceIn(0f, 1f) else 0f
}
