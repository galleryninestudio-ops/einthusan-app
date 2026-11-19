package com.bench.einthusan.download

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DlTaskTest {
    @Test fun preservesExistingDownloadRegistryFields() {
        val task = DlTask("task", "movie", "Title", null, "https://example.test/movie.mp4",
            "Title", 1000, 6, DlStatus.PAUSED, 1234, "network", "content://media/42")
        assertEquals(task, DlTask.from(JSONObject(task.toJson().toString())))
    }

    @Test fun olderRegistryEntriesRetainSafeDefaults() {
        val task = DlTask.from(JSONObject("""{"id":"task","movieId":"movie","status":"UNKNOWN"}"""))
        assertEquals(DlTask.DEFAULT_CHUNKS, task.chunks)
        assertEquals(DlStatus.QUEUED, task.status)
        assertNull(task.playUri)
    }
}
