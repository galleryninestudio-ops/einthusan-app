package com.bench.einthusan.parse

data class Movie(
    val id: String,
    val title: String,
    val year: String,
    val lang: String,
    val poster: String?,
    val uhd: Boolean,
    val subtitle: Boolean,
    val synopsis: String?,
    val badge: String?,
    val playable: Boolean,
)

data class Paged(
    val movies: List<Movie>,
    val totalPages: Int,
)

/**
 * One playable stream. [expiresAt] is the unix second the CDN signature dies;
 * zero means the URL carried no expiry (local files, unknown shapes).
 */
data class WatchInfo(
    val mp4: String?,
    val hls: String?,
    val expiresAt: Long = 0L,
    val premiumHd: Boolean = false,
) {
    val playable: Boolean get() = !mp4.isNullOrBlank() || !hls.isNullOrBlank()
}
