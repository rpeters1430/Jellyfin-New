package com.example.jellyfinnew.data

import org.jellyfin.sdk.api.client.ApiClient

/**
 * Helper class for building Jellyfin image URLs with consistent parameters
 */
class ImageUrlHelper(private val apiClient: ApiClient?) {    companion object {
        private const val DEFAULT_QUALITY = 90 // Increased quality for better TV viewing
        
        // Optimized sizes for TV viewing
        private const val POSTER_WIDTH = 480
        private const val POSTER_HEIGHT = 720
        private const val BACKDROP_WIDTH = 1920
        private const val BACKDROP_HEIGHT = 1080
        private const val THUMB_WIDTH = 960
        private const val THUMB_HEIGHT = 540
        private const val LIBRARY_BACKDROP_WIDTH = 1600
        private const val LIBRARY_BACKDROP_HEIGHT = 900
        
        // Additional sizes for different card types
        private const val SMALL_POSTER_WIDTH = 320
        private const val SMALL_POSTER_HEIGHT = 480
        private const val EPISODE_THUMB_WIDTH = 854
        private const val EPISODE_THUMB_HEIGHT = 480
    }

    private fun buildImageUrl(
        itemId: String,
        imageType: String,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        quality: Int = DEFAULT_QUALITY
    ): String? {
        return apiClient?.let { client ->
            buildString {
                append("${client.baseUrl}/Items/$itemId/Images/$imageType")

                val params = mutableListOf<String>()
                maxWidth?.let { params.add("maxWidth=$it") }
                maxHeight?.let { params.add("maxHeight=$it") }
                params.add("quality=$quality")
                client.accessToken?.let { params.add("api_key=$it") }

                if (params.isNotEmpty()) {
                    append("?${params.joinToString("&")}")
                }
            }
        }
    }

    fun buildPosterUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", POSTER_WIDTH, POSTER_HEIGHT)

    fun buildBackdropUrl(itemId: String): String? =
        buildImageUrl(itemId, "Backdrop", BACKDROP_WIDTH, BACKDROP_HEIGHT)

    fun buildThumbUrl(itemId: String): String? =
        buildImageUrl(itemId, "Thumb", THUMB_WIDTH, THUMB_HEIGHT)

    fun buildLibraryBackdropUrl(itemId: String): String? =
        buildImageUrl(itemId, "Backdrop", LIBRARY_BACKDROP_WIDTH, LIBRARY_BACKDROP_HEIGHT)

    fun buildSmallPosterUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", SMALL_POSTER_WIDTH, SMALL_POSTER_HEIGHT)

    fun buildEpisodeThumbUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", EPISODE_THUMB_WIDTH, EPISODE_THUMB_HEIGHT)

    /**
     * Get the best available image URL for library cards (prefer backdrop, fallback to primary/thumb)
     */
    fun buildLibraryImageUrl(itemId: String): String? {
        return buildLibraryBackdropUrl(itemId) ?: buildPosterUrl(itemId) ?: buildThumbUrl(itemId)
    }

    /**
     * Get poster and backdrop URLs for media items
     */
    fun buildMediaImageUrls(itemId: String): Pair<String?, String?> {
        val posterUrl = buildPosterUrl(itemId)
        val backdropUrl = buildBackdropUrl(itemId)
        return posterUrl to backdropUrl
    }

    /**
     * Build stream URL for video playback
     */
    fun buildStreamUrl(itemId: String): String? {
        return apiClient?.let { client ->
            buildString {
                append("${client.baseUrl}/Videos/$itemId/stream")
                client.accessToken?.let { token ->
                    append("?api_key=$token")
                }
            }
        }
    }

    /**
     * Get optimized image URLs based on card type for better caching
     */
    fun buildOptimizedImageUrls(itemId: String, cardType: String): Pair<String?, String?> {
        return when (cardType) {
            "episode" -> {
                val thumbUrl = buildEpisodeThumbUrl(itemId)
                val backdropUrl = buildBackdropUrl(itemId)
                thumbUrl to backdropUrl
            }
            "small" -> {
                val smallPosterUrl = buildSmallPosterUrl(itemId)
                val backdropUrl = buildBackdropUrl(itemId)
                smallPosterUrl to backdropUrl
            }
            else -> buildMediaImageUrls(itemId)
        }
    }
}