package com.example.jellyfinnew.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.data.MediaItem
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.example.jellyfinnew.di.ServiceLocator

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val repository: JellyfinRepository = ServiceLocator.provideRepository(application)

    // Repository state flows
    val mediaLibraries: StateFlow<List<MediaItem>> = repository.mediaLibraries
    val currentLibraryItems: StateFlow<List<MediaItem>> = repository.currentLibraryItems
    val featuredItems: StateFlow<List<MediaItem>> = repository.featuredItems
    val recentlyAdded: StateFlow<Map<String, List<MediaItem>>> = repository.recentlyAdded
    val connectionState = repository.connectionState

    // Internal state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var refreshJob: Job? = null

    init {
        Log.d(TAG, "HomeViewModel initialized")

        // Monitor connection state and load content when connected
        viewModelScope.launch {
            connectionState.collect { state ->
                Log.d(TAG, "Connection state changed: isConnected=${state.isConnected}, error=${state.error}")
                if (state.isConnected && !state.isLoading) {
                    loadHomeContentIfNeeded()
                }
            }
        }
    }

    private suspend fun loadHomeContentIfNeeded() {
        // Only load if we don't have content yet
        if (mediaLibraries.value.isEmpty() && featuredItems.value.isEmpty()) {
            Log.d(TAG, "Loading initial home content")
            loadHomeContent()
        }
    }

    private fun loadHomeContent() {
        if (refreshJob?.isActive == true) {
            Log.d(TAG, "Refresh already in progress, skipping")
            return
        }

        refreshJob = viewModelScope.launch {
            try {
                _isRefreshing.value = true
                Log.d(TAG, "Starting home content refresh")

                // Load content sequentially to avoid overwhelming the server
                repository.loadMediaLibraries()
                repository.loadFeaturedContent()
                repository.loadRecentlyAdded()

                Log.d(TAG, "Home content refresh completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing home content", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /**
     * Load items for a specific library
     */
    fun loadLibraryItems(libraryId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading library items for: $libraryId")
                repository.loadLibraryItems(libraryId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading library items for $libraryId", e)
            }
        }
    }

    /**
     * Refresh all libraries
     */
    fun refreshLibraries() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing libraries")
                repository.loadMediaLibraries()
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing libraries", e)
            }
        }
    }

    /**
     * Refresh all home content (featured, recently added, etc.)
     */
    fun refreshHomeContent() {
        Log.d(TAG, "Manual home content refresh requested")
        loadHomeContent()
    }

    /**
     * Get stream URL for media playback
     */
    fun getStreamUrl(itemId: String): String? {
        val streamUrl = repository.getStreamUrl(itemId)
        Log.d(TAG, "Stream URL for $itemId: $streamUrl")
        return streamUrl
    }

    /**
     * Disconnect from Jellyfin server
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from Jellyfin server")
        refreshJob?.cancel()
        repository.disconnect()
    }

    /**
     * Retry failed operations
     */
    fun retry() {
        Log.d(TAG, "Retrying failed operations")
        if (connectionState.value.isConnected) {
            refreshHomeContent()
        }
    }

    /**
     * Clear current library items (useful when navigating back from library view)
     */
    fun clearCurrentLibraryItems() {
        // This would require adding a method to the repository
        // For now, we'll just log the action
        Log.d(TAG, "Request to clear current library items")
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "HomeViewModel cleared")
        refreshJob?.cancel()
    }
}
