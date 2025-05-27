package com.example.jellyfinnew.ui.utils

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.jellyfinnew.data.JellyfinConfig
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Enhanced image cache manager for optimal TV performance
 */
object ImageCacheManager {
    
    private val TAG = JellyfinConfig.Logging.getTag("ImageCache")
    
    /**
     * Creates a high-performance ImageLoader optimized for TV with aggressive caching
     */
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        Log.d(TAG, "Creating optimized ImageLoader for TV")
        
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.30) // Use 30% of available memory for images
                    .strongReferencesEnabled(true) // Keep strong references for better performance
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("jellyfin_image_cache"))
                    .maxSizeBytes(200 * 1024 * 1024) // 200MB disk cache
                    .build()
            }
            .crossfade(false) // Disable crossfade for better TV performance
            .allowHardware(true) // Use hardware acceleration when available
            .respectCacheHeaders(false) // Jellyfin doesn't always send proper cache headers
            .dispatcher(Dispatchers.IO) // Use IO dispatcher for network calls
            .build()
    }
    
    /**
     * Creates an optimized image request with aggressive caching and TV-specific optimizations
     */
    @Composable
    fun createOptimizedImageRequest(
        url: String?,
        contentDescription: String? = null,
        enableCrossfade: Boolean = false,
        placeholder: Int? = null,
        error: Int? = null
    ): ImageRequest? {
        val context = LocalContext.current
        
        return url?.let { imageUrl ->
            remember(imageUrl) {
                ImageRequest.Builder(context)
                    .data(imageUrl)
                    .memoryCacheKey(imageUrl) // Explicit memory cache key
                    .diskCacheKey(imageUrl) // Explicit disk cache key
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)                    .crossfade(enableCrossfade)
                    .allowHardware(true)
                    .apply {
                        placeholder?.let { placeholder(it) }
                        error?.let { error(it) }
                    }
                    .build()
            }
        }
    }
    
    /**
     * Preload images for better user experience
     */
    fun preloadImages(context: Context, imageUrls: List<String>) {
        val imageLoader = createOptimizedImageLoader(context)
        
        imageUrls.forEach { url ->
            val request = ImageRequest.Builder(context)
                .data(url)
                .build()
                
            imageLoader.enqueue(request)
        }
        
        Log.d(TAG, "Preloading ${imageUrls.size} images")
    }
    
    /**
     * Clear image cache when memory is low
     */
    fun clearCache(context: Context) {
        val cacheDir = File(context.cacheDir, "jellyfin_image_cache")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            Log.d(TAG, "Cleared image cache")
        }
    }
    
    /**
     * Get cache usage statistics
     */
    fun getCacheStats(context: Context): CacheStats {
        val cacheDir = File(context.cacheDir, "jellyfin_image_cache")
        val size = if (cacheDir.exists()) {
            cacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
        } else 0L
        
        return CacheStats(
            diskCacheSize = size,
            diskCacheDirectory = cacheDir.absolutePath
        )
    }
}

/**
 * Data class for cache statistics
 */
data class CacheStats(
    val diskCacheSize: Long,
    val diskCacheDirectory: String
)
