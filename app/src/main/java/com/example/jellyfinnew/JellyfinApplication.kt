package com.example.jellyfinnew

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.jellyfinnew.ui.utils.ImageCacheManager

/**
 * Custom Application class to configure global image loading optimizations
 */
class JellyfinApplication : Application(), ImageLoaderFactory {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize any global configurations here
    }
    
    /**
     * Provide a custom ImageLoader with TV-optimized caching for the entire app
     */
    override fun newImageLoader(): ImageLoader {
        return ImageCacheManager.createOptimizedImageLoader(this)
    }
}
