package com.example.jellyfinnew.data

import android.util.Log
import org.jellyfin.sdk.api.client.ApiClient

/**
 * Enhanced helper class for building Jellyfin image URLs with proper aspect ratios and fallbacks
 */
class ImageUrlHelper(private val apiClient: ApiClient?) {
    
    companion object {
        private const val TAG = "ImageUrlHelper"
    }    private fun buildImageUrl(
        itemId: String,
        imageType: String,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        quality: Int = JellyfinConfig.Images.DEFAULT_QUALITY
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
        buildImageUrl(itemId, "Primary", JellyfinConfig.Images.POSTER_WIDTH, JellyfinConfig.Images.POSTER_HEIGHT)

    // Horizontal backdrop images (16:9 aspect ratio)
    fun buildBackdropUrl(itemId: String): String? =
        buildImageUrl(itemId, "Backdrop", JellyfinConfig.Images.BACKDROP_WIDTH, JellyfinConfig.Images.BACKDROP_HEIGHT)

    // Episode thumb images (16:9 aspect ratio, smaller)
    fun buildEpisodeThumbUrl(itemId: String): String? =
        buildImageUrl(itemId, "Thumb", JellyfinConfig.Images.THUMB_WIDTH, JellyfinConfig.Images.THUMB_HEIGHT)

    // Library backdrop images (16:9 aspect ratio, medium size)
    fun buildLibraryBackdropUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", JellyfinConfig.Images.LIBRARY_BACKDROP_WIDTH, JellyfinConfig.Images.LIBRARY_BACKDROP_HEIGHT)

    // Episode thumb images (horizontal, legacy method name for compatibility)
    fun buildThumbUrl(itemId: String): String? =
        buildImageUrl(itemId, "Thumb", JellyfinConfig.Images.THUMB_WIDTH, JellyfinConfig.Images.THUMB_HEIGHT)

    // Square images (1:1 aspect ratio)
    fun buildSquareUrl(itemId: String): String? =
        buildImageUrl(itemId, "Primary", 500, 500)    /**
     * Smart image URL selection based on card type and available images
     */
    fun getOptimalImageUrl(itemId: String, cardType: String): String? {
        return when (cardType) {
            "poster" -> buildPosterUrl(itemId) ?: buildBackdropUrl(itemId)
            "backdrop" -> buildBackdropUrl(itemId) ?: buildPosterUrl(itemId)
            "episode" -> buildThumbUrl(itemId) ?: buildBackdropUrl(itemId) ?: buildPosterUrl(itemId)
            "library" -> buildLibraryBackdropUrl(itemId) ?: buildBackdropUrl(itemId) ?: buildPosterUrl(itemId)
            "square" -> buildSquareUrl(itemId) ?: buildPosterUrl(itemId)
            else -> buildPosterUrl(itemId) ?: buildBackdropUrl(itemId)
        }
    }

    /**
     * Get appropriate image URLs with fallbacks for media cards
     */
    fun getImageUrlsForCardType(itemId: String, cardType: String): Pair<String?, String?> {
        Log.d(TAG, "Getting image URLs for $itemId, cardType: $cardType")
        
        return when (cardType) {
            "poster" -> {
                val poster = buildPosterUrl(itemId)
                val backdrop = buildBackdropUrl(itemId)
                Log.d(TAG, "Poster URLs - primary: $poster, fallback: $backdrop")
                poster to backdrop
            }            "library" -> {
                // Try Primary first, then Backdrop as fallback for libraries
                val primary = buildLibraryBackdropUrl(itemId)
                val backdrop = buildBackdropUrl(itemId)
                val posterFallback = buildPosterUrl(itemId)
                val finalPrimary = primary ?: backdrop ?: posterFallback
                val finalFallback = backdrop ?: posterFallback
                Log.d(TAG, "Library URLs - primary: $primary, fallback: $backdrop, poster: $posterFallback, final: $finalPrimary")
                finalPrimary to finalFallback
            }
            "backdrop" -> {
                val backdrop = buildBackdropUrl(itemId)
                val poster = buildPosterUrl(itemId)
                Log.d(TAG, "Backdrop URLs - primary: $backdrop, fallback: $poster")
                backdrop to poster
            }
            "episode" -> {
                // Try Thumb first, then Backdrop, then Primary as fallbacks
                val episodeThumb = buildThumbUrl(itemId)
                val backdrop = buildBackdropUrl(itemId) 
                val poster = buildPosterUrl(itemId)
                val finalPrimary = episodeThumb ?: backdrop ?: poster
                val finalFallback = backdrop ?: poster
                Log.d(TAG, "Episode URLs - primary: $finalPrimary, fallback: $finalFallback")
                finalPrimary to finalFallback
            }
            "square" -> {
                val square = buildSquareUrl(itemId)
                val poster = buildPosterUrl(itemId)
                Log.d(TAG, "Square URLs - primary: $square, fallback: $poster")
                square to poster
            }
            else -> {
                Log.d(TAG, "Using default media image URLs for $cardType")
                buildMediaImageUrls(itemId)
            }
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