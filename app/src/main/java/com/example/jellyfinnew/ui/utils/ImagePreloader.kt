package com.example.jellyfinnew.ui.utils

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.*
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.data.JellyfinConfig

/**
 * Preloads images for adjacent items to improve scrolling performance
 */
class ImagePreloader(private val application: Application) {
    
    companion object {
        private const val TAG = "ImagePreloader"
    }
    
    private val imageLoader = ImageLoader(application)
    private val preloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val preloadedUrls = mutableSetOf<String>()
    
    /**
     * Preload images for items adjacent to the currently focused item
     */
    fun preloadAdjacentItems(focusedIndex: Int, items: List<MediaItem>) {
        preloadScope.launch {
            val preloadDistance = JellyfinConfig.Performance.PRELOAD_DISTANCE
            val startIndex = maxOf(0, focusedIndex - preloadDistance)
            val endIndex = minOf(items.size - 1, focusedIndex + preloadDistance)
            
            Log.d(TAG, "Preloading items ${startIndex} to ${endIndex} (focused: ${focusedIndex})")
            
            for (i in startIndex..endIndex) {
                if (i != focusedIndex && i < items.size) {
                    preloadItemImages(items[i])
                }
            }
        }
    }
    
    /**
     * Preload images for a specific media item
     */
    private suspend fun preloadItemImages(mediaItem: MediaItem) {
        try {
            // Preload poster image
            mediaItem.imageUrl?.let { imageUrl ->
                if (!preloadedUrls.contains(imageUrl)) {
                    preloadImage(imageUrl)
                    preloadedUrls.add(imageUrl)
                }
            }
            
            // Preload backdrop image if available
            mediaItem.backdropUrl?.let { backdropUrl ->
                if (!preloadedUrls.contains(backdropUrl)) {
                    preloadImage(backdropUrl)
                    preloadedUrls.add(backdropUrl)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to preload images for item ${mediaItem.id}", e)
        }
    }
    
    /**
     * Preload a single image URL
     */
    private suspend fun preloadImage(imageUrl: String) {
        try {
            val request = ImageRequest.Builder(application)
                .data(imageUrl)
                .build()
            
            imageLoader.execute(request)
            Log.v(TAG, "Preloaded image: $imageUrl")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to preload image: $imageUrl", e)
        }
    }
    
    /**
     * Clear preloaded images cache
     */
    fun clearCache() {
        preloadedUrls.clear()
        imageLoader.memoryCache?.clear()
        Log.d(TAG, "Cleared preload cache")
    }
    
    /**
     * Get number of preloaded images
     */
    fun getPreloadedCount(): Int = preloadedUrls.size
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        preloadScope.cancel()
        clearCache()
        Log.d(TAG, "ImagePreloader cleaned up")
    }
}