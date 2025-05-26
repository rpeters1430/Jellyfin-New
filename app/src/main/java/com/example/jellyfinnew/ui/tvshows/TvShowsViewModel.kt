package com.example.jellyfinnew.ui.tvshows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.di.ServiceLocator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TvShowsViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: JellyfinRepository = ServiceLocator.provideRepository(application)
    
    val tvShows: StateFlow<List<com.example.jellyfinnew.data.MediaItem>> = repository.tvShows
    val connectionState = repository.connectionState
    
    private var currentLibraryId: String? = null
    
    fun setLibraryId(libraryId: String) {
        currentLibraryId = libraryId
    }
    
    fun loadTvShows() {
        currentLibraryId?.let { libraryId ->
            viewModelScope.launch {
                try {
                    repository.loadTvShows(libraryId)
                } catch (e: Exception) {
                    println("Error loading TV shows: ${e.message}")
                }
            }
        }
    }
}
