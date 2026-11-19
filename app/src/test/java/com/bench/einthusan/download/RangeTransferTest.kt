package com.bench.einthusan.download

import com.sun.net.httpserver.HttpServer
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicLong

class RangeTransferTest {
    private val client = OkHttpClient()

    @Test fun resumesAtTheExactByteAndAppendsOnlyTheRemainingRange() {
        val part = Files.createTempFile("range-resume", ".part").toFile()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/video") { exchange ->
            assertEquals("bytes=5-9", exchange.requestHeaders.getFirst("Range"))
            exchange.responseHeaders.set("Content-Range", "bytes 5-9/10")
            exchange.sendResponseHeaders(206, 5)
            exchange.responseBody.use { it.write(byteArrayOf(5, 6, 7, 8, 9)) }
        }
        server.start()
        try {
            part.writeBytes(byteArrayOf(2, 3, 4))
            val bytes = AtomicLong(3)
            RangeTransfer.download(client, "http://127.0.0.1:${server.address.port}/video", part,
                ByteRange(2, 9), 10, bytes) { false }
            assertArrayEquals(byteArrayOf(2, 3, 4, 5, 6, 7, 8, 9), part.readBytes())
            assertEquals(8L, bytes.get())
        } finally {
            server.stop(0)
            part.delete()
        }
    }

    @Test fun refusesAFullBodyWhenTheServerIgnoresRange() {
        val part = Files.createTempFile("range-ignored", ".part").toFile()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/video") { exchange ->
            exchange.sendResponseHeaders(200, 10)
            exchange.responseBody.use { it.write(ByteArray(10)) }
        }
        server.start()
        try {
            part.writeBytes(byteArrayOf(2, 3, 4))
            assertThrows(IllegalStateException::class.java) {
                RangeTransfer.download(client, "http://127.0.0.1:${server.address.port}/video", part,
                    ByteRange(2, 9), 10, AtomicLong(3)) { false }
            }
            assertArrayEquals(byteArrayOf(2, 3, 4), part.readBytes())
        } finally {
            server.stop(0)
            part.delete()
        }
    }
}
