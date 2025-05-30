package com.example.jellyfinnew.ui.utils

import android.util.Log
import com.example.jellyfinnew.data.ImageUrlHelper
import com.example.jellyfinnew.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Helper class for debugging image loading issues
 */
object ImageDebugHelper {
    private const val TAG = "ImageDebugHelper"
    
    /**
     * Test if an image URL is accessible
     */
    suspend fun testImageUrl(imageUrl: String?): ImageTestResult = withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrEmpty()) {
            return@withContext ImageTestResult.NoUrl
        }
        
        try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            val responseCode = connection.responseCode
            connection.disconnect()
            
            when (responseCode) {
                200 -> ImageTestResult.Success
                401 -> ImageTestResult.AuthError
                403 -> ImageTestResult.Forbidden
                404 -> ImageTestResult.NotFound
                else -> ImageTestResult.Error(responseCode)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error testing image URL: $imageUrl", e)
            ImageTestResult.NetworkError(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Debug image URLs for a media item
     */
    suspend fun debugMediaItemImages(
        mediaItem: MediaItem,
        imageHelper: ImageUrlHelper
    ): ImageDebugInfo {
        val primaryUrl = imageHelper.buildPosterUrl(mediaItem.id)
        val backdropUrl = imageHelper.buildBackdropUrl(mediaItem.id) 
        val thumbUrl = imageHelper.buildThumbUrl(mediaItem.id)
        
        val primaryResult = testImageUrl(primaryUrl)
        val backdropResult = testImageUrl(backdropUrl)
        val thumbResult = testImageUrl(thumbUrl)
        
        Log.d(TAG, "Image debug for ${mediaItem.name}:")
        Log.d(TAG, "  Primary URL: $primaryUrl -> $primaryResult")
        Log.d(TAG, "  Backdrop URL: $backdropUrl -> $backdropResult")
        Log.d(TAG, "  Thumb URL: $thumbUrl -> $thumbResult")
        Log.d(TAG, "  Current imageUrl: ${mediaItem.imageUrl}")
        Log.d(TAG, "  Current backdropUrl: ${mediaItem.backdropUrl}")
        
        return ImageDebugInfo(
            mediaItem = mediaItem,
            primaryUrl = primaryUrl,
            backdropUrl = backdropUrl,
            thumbUrl = thumbUrl,
            primaryResult = primaryResult,
            backdropResult = backdropResult,
            thumbResult = thumbResult
        )
    }
    
    /**
     * Get recommended image URL based on test results
     */
    fun getRecommendedImageUrl(debugInfo: ImageDebugInfo): String? {
        return when {
            debugInfo.primaryResult is ImageTestResult.Success -> debugInfo.primaryUrl
            debugInfo.backdropResult is ImageTestResult.Success -> debugInfo.backdropUrl
            debugInfo.thumbResult is ImageTestResult.Success -> debugInfo.thumbUrl
            else -> null
        }
    }
}

sealed class ImageTestResult {
    object Success : ImageTestResult()
    object NoUrl : ImageTestResult()
    object AuthError : ImageTestResult()
    object Forbidden : ImageTestResult()
    object NotFound : ImageTestResult()
    data class Error(val code: Int) : ImageTestResult()
    data class NetworkError(val message: String) : ImageTestResult()
    
    override fun toString(): String = when (this) {
        is Success -> "✓ Success"
        is NoUrl -> "✗ No URL"
        is AuthError -> "✗ Auth Error (401)"
        is Forbidden -> "✗ Forbidden (403)"
        is NotFound -> "✗ Not Found (404)"
        is Error -> "✗ HTTP Error ($code)"
        is NetworkError -> "✗ Network Error: $message"
    }
}

data class ImageDebugInfo(
    val mediaItem: MediaItem,
    val primaryUrl: String?,
    val backdropUrl: String?,
    val thumbUrl: String?,
    val primaryResult: ImageTestResult,
    val backdropResult: ImageTestResult,
    val thumbResult: ImageTestResult
)
