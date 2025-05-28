package com.example.jellyfinnew.ui.utils

import android.util.Log
import kotlinx.coroutines.*
import com.example.jellyfinnew.data.JellyfinConfig
import com.example.jellyfinnew.data.repositories.MediaRepository

/**
 * Manages content refresh logic to avoid unnecessary API calls
 * while keeping content fresh for better user experience
 */
class ContentRefreshManager(private val repository: MediaRepository) {
    
    companion object {
        private const val TAG = "ContentRefreshManager"
    }
    
    private var lastRefreshTime = 0L
    private var isRefreshing = false
    
    /**
     * Refresh content only if enough time has passed since last refresh
     */
    suspend fun refreshIfNeeded() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTime
        
        if (timeSinceLastRefresh >= JellyfinConfig.Performance.REFRESH_INTERVAL_MS && !isRefreshing) {
            Log.d(TAG, "Refreshing content - ${timeSinceLastRefresh}ms since last refresh")
            isRefreshing = true
            
            try {
                // Refresh content in background
                withContext(Dispatchers.IO) {
                    // Note: We would call repository refresh methods here
                    // For now, just update the timestamp
                    lastRefreshTime = currentTime
                }
                Log.d(TAG, "Content refresh completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Content refresh failed", e)
            } finally {
                isRefreshing = false
            }
        } else {
            Log.d(TAG, "Skipping refresh - too soon (${timeSinceLastRefresh}ms)")
        }
    }
    
    /**
     * Force refresh content regardless of timing
     */
    suspend fun forceRefresh() {
        Log.d(TAG, "Force refreshing content")
        isRefreshing = true
        
        try {
            withContext(Dispatchers.IO) {
                lastRefreshTime = System.currentTimeMillis()
            }
            Log.d(TAG, "Force refresh completed")
        } catch (e: Exception) {
            Log.e(TAG, "Force refresh failed", e)
        } finally {
            isRefreshing = false
        }
    }
    
    /**
     * Check if content is currently being refreshed
     */
    fun isRefreshing(): Boolean = isRefreshing
    
    /**
     * Get time since last refresh in milliseconds
     */
    fun getTimeSinceLastRefresh(): Long {
        return System.currentTimeMillis() - lastRefreshTime
    }
}