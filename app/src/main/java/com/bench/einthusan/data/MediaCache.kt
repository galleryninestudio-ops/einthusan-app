package com.bench.einthusan.data

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.bench.einthusan.net.Net
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Disk cache for the stream itself.
 *
 * These are plain progressive MP4s with the index at the head of a 1-2 GB file,
 * so the first frame costs a multi-megabyte read before any picture appears.
 * Caching the bytes means the second open of a title starts from local disk,
 * and a head-warm while the detail page is on screen makes the first open fast
 * too. Bounded at 512 MB, evicted least-recently-used.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object MediaCache {

    private const val MAX_BYTES = 512L * 1024 * 1024

    /** Enough for the moov atom plus the first minute on a 480p stream. */
    const val WARM_BYTES = 24L * 1024 * 1024

    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        instance?.let { return it }
        return synchronized(this) {
            instance ?: SimpleCache(
                File(context.applicationContext.cacheDir, "stream"),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context.applicationContext),
            ).also { instance = it }
        }
    }

    fun upstream(): DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        .setUserAgent(Net.UA)
        .setConnectTimeoutMs(12_000)
        .setReadTimeoutMs(18_000)
        .setAllowCrossProtocolRedirects(true)

    fun source(context: Context, upstream: DataSource.Factory = upstream()): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(get(context))
            .setUpstreamDataSourceFactory(upstream)
            // The signed query changes on every page load; the bytes behind the
            // path do not. Key on the path or the cache never hits.
            .setCacheKeyFactory { spec -> spec.uri.path ?: spec.uri.toString() }
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    /**
     * Pull the head of the stream into the cache in the background. Runs while
     * the viewer is still reading the synopsis, so Play lands on local bytes.
     */
    suspend fun warm(context: Context, url: String?, bytes: Long = WARM_BYTES) {
        if (url.isNullOrBlank()) return
        withContext(Dispatchers.IO) {
            runCatching {
                val spec = DataSpec.Builder()
                    .setUri(url)
                    .setLength(bytes)
                    .build()
                CacheWriter(
                    source(context).createDataSource(),
                    spec,
                    ByteArray(64 * 1024),
                    null,
                ).cache()
            }
        }
    }
}
