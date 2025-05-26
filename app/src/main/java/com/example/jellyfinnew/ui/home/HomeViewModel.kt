package com.example.jellyfinnew.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.data.MediaItem
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.jellyfinnew.di.ServiceLocator

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: JellyfinRepository = ServiceLocator.provideRepository(application)
    
    val mediaLibraries: StateFlow<List<MediaItem>> = repository.mediaLibraries
    val currentLibraryItems: StateFlow<List<MediaItem>> = repository.currentLibraryItems
    val featuredItem: StateFlow<MediaItem?> = repository.featuredItem
    val featuredItems: StateFlow<List<MediaItem>> = repository.featuredItems
    val recentlyAdded: StateFlow<Map<String, List<MediaItem>>> = repository.recentlyAdded
    val connectionState = repository.connectionState
    
    init {
        // Only load content if already connected
        viewModelScope.launch {
            connectionState.collect { state ->
                if (state.isConnected) {
                    loadHomeContent()
                }
            }
        }
    }
    
    private fun loadHomeContent() {
        viewModelScope.launch {
            try {
                repository.loadFeaturedContent()
                repository.loadRecentlyAdded()
            } catch (e: Exception) {
                println("Error loading home content: ${e.message}")
            }        }
    }
    
    fun loadLibraryItems(libraryId: String) {
        viewModelScope.launch {
            try {
                repository.loadLibraryItems(libraryId)
            } catch (e: Exception) {
                println("Error loading library items: ${e.message}")
            }
        }
    }
    
    fun refreshLibraries() {
        viewModelScope.launch {
            try {
                repository.loadMediaLibraries()
            } catch (e: Exception) {
                println("Error refreshing libraries: ${e.message}")
            }
        }
    }
    
    fun refreshHomeContent() {
        loadHomeContent()
    }
    
    fun getStreamUrl(itemId: String): String? {
        return repository.getStreamUrl(itemId)
    }
    
    fun disconnect() {
        repository.disconnect()
    }
}
