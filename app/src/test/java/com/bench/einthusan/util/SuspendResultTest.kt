package com.bench.einthusan.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SuspendResultTest {
    @Test fun cancellationIsNeverConvertedToAFailedRequest() {
        assertThrows(CancellationException::class.java) {
            runBlocking { suspendResult { throw CancellationException("screen left") } }
        }
    }

    @Test fun ordinaryFailuresRemainAvailableToTheUi() = runBlocking {
        val result = suspendResult { error("offline") }
        assertEquals("offline", result.exceptionOrNull()?.message)
        assertEquals(42, suspendResult { 42 }.getOrThrow())
    }
}
