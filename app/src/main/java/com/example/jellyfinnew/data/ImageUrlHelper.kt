package com.example.jellyfinnew.data

import android.util.Log
import org.jellyfin.sdk.api.client.ApiClient

/**
 * Enhanced helper class for building Jellyfin image URLs with proper aspect ratios
 */
class ImageUrlHelper(private val apiClient: ApiClient?) {
    
    companion object {
        private const val DEFAULT_QUALITY = 90
        private const val TAG = "ImageUrlHelper"
        
        // TV-optimized sizes for different card types
        private const val POSTER_WIDTH = 400 // Reduced for TV performance
        private const val POSTER_HEIGHT = 600
        private const val BACKDROP_WIDTH = 1280 // Reduced from 1920 for TV
        private const val BACKDROP_HEIGHT = 720
        private const val EPISODE_WIDTH = 800 // Horizontal episode cards
        private const val EPISODE_HEIGHT = 450
        private const val LIBRARY_WIDTH = 1200 // Library backdrop cards
        private const val LIBRARY_HEIGHT = 675
    }

    private fun buildImageUrl(
        itemId: String,
        imageType: String,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        quality: Int = DEFAULT_QUALITY
    ): String? {
        return apiClient?.let { client ->
            val url = buildString {
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
            Log.d(TAG, "Built image URL for $itemId ($imageType): $url")
            url
        } ?: run {
            Log.w(TAG, "ApiClient is null, cannot build image URL for $itemId")
            null
        }
    }

    // Vertical poster images (2:3 aspect ratio)
    fun buildPosterUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", POSTER_WIDTH, POSTER_HEIGHT)    // Horizontal backdrop images (16:9 aspect ratio)
    fun buildBackdropUrl(itemId: String): String? =
        buildImageUrl(itemId, "Backdrop", BACKDROP_WIDTH, BACKDROP_HEIGHT)

    // Episode thumb images (16:9 aspect ratio, smaller)
    fun buildEpisodeThumbUrl(itemId: String): String? =
        buildImageUrl(itemId, "Thumb", EPISODE_WIDTH, EPISODE_HEIGHT)

    // Library backdrop images (16:9 aspect ratio, medium size)
    fun buildLibraryBackdropUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", LIBRARY_WIDTH, LIBRARY_HEIGHT)

    // Episode thumb images (horizontal, legacy method name for compatibility)
    fun buildThumbUrl(itemId: String): String? =
        buildImageUrl(itemId, "Thumb", EPISODE_WIDTH, EPISODE_HEIGHT)

    // Square images (1:1 aspect ratio)
    fun buildSquareUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", 500, 500)

    /**
     * Smart image URL selection based on card type and available images
     */
    fun getOptimalImageUrl(itemId: String, cardType: String): String? {
        return when (cardType) {
            "poster" -> buildPosterUrl(itemId)
            "backdrop" -> buildBackdropUrl(itemId)
            "episode" -> buildThumbUrl(itemId) ?: buildBackdropUrl(itemId)
            "library" -> buildLibraryBackdropUrl(itemId) ?: buildBackdropUrl(itemId)
            "square" -> buildSquareUrl(itemId)
            else -> buildPosterUrl(itemId)
        }
    }

    /**
     * Get appropriate image URLs with fallbacks for media cards
     */
    fun getImageUrlsForCardType(itemId: String, cardType: String): Pair<String?, String?> {
        return when (cardType) {
            "poster" -> {
                val poster = buildPosterUrl(itemId)
                val backdrop = buildBackdropUrl(itemId)
                poster to backdrop
            }            "library" -> {
                val primary = buildImageUrl(itemId, "Primary", JellyfinConfig.Images.POSTER_WIDTH, JellyfinConfig.Images.POSTER_HEIGHT)
                val backdrop = buildImageUrl(itemId, "Backdrop", LIBRARY_WIDTH, LIBRARY_HEIGHT)
                primary to backdrop
            }
            "backdrop" -> { // Assuming this case remains unchanged for now
                val backdrop = buildLibraryBackdropUrl(itemId) ?: buildBackdropUrl(itemId)
                val poster = buildPosterUrl(itemId)
                backdrop to poster
            }
            "episode" -> {
                val episodeThumb = buildThumbUrl(itemId) // Use Thumb image type for episodes
                val backdrop = buildBackdropUrl(itemId)
                episodeThumb to backdrop
            }
            "square" -> {
                val square = buildSquareUrl(itemId)
                val poster = buildPosterUrl(itemId)
                square to poster
            }
            else -> buildMediaImageUrls(itemId)
        }
    }

    /**
     * Legacy method for compatibility
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
}