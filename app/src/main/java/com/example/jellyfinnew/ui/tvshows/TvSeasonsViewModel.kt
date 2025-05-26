package com.example.jellyfinnew.ui.tvshows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.di.ServiceLocator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TvSeasonsViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: JellyfinRepository = ServiceLocator.provideRepository(application)
    
    val tvSeasons: StateFlow<List<com.example.jellyfinnew.data.MediaItem>> = repository.tvSeasons
    val currentSeries: StateFlow<com.example.jellyfinnew.data.MediaItem?> = repository.currentSeries
    val connectionState = repository.connectionState
    
    private var currentSeriesId: String? = null
    
    fun setSeriesId(seriesId: String) {
        currentSeriesId = seriesId
    }
    
    fun loadSeasons() {
        currentSeriesId?.let { seriesId ->
            viewModelScope.launch {
                try {
                    repository.loadTvSeasons(seriesId)
                } catch (e: Exception) {
                    println("Error loading TV seasons: ${e.message}")
                }
            }
        }
    }
}
