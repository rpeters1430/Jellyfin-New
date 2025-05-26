package com.example.jellyfinnew.ui.tvshows

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.di.ServiceLocator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TvEpisodesViewModel(
    application: Application
) : AndroidViewModel(application) {
    
    private val repository: JellyfinRepository = ServiceLocator.provideRepository(application)
    
    val tvEpisodes: StateFlow<List<com.example.jellyfinnew.data.MediaItem>> = repository.tvEpisodes
    val currentSeason: StateFlow<com.example.jellyfinnew.data.MediaItem?> = repository.currentSeason
    val currentSeries: StateFlow<com.example.jellyfinnew.data.MediaItem?> = repository.currentSeries
    val connectionState = repository.connectionState
    
    private var currentSeriesId: String? = null
    private var currentSeasonId: String? = null
    
    fun setSeriesAndSeasonId(seriesId: String, seasonId: String) {
        currentSeriesId = seriesId
        currentSeasonId = seasonId
    }
    
    fun loadEpisodes() {
        if (currentSeriesId != null && currentSeasonId != null) {
            viewModelScope.launch {
                try {
                    repository.loadTvEpisodes(currentSeriesId!!, currentSeasonId!!)
                } catch (e: Exception) {
                    println("Error loading TV episodes: ${e.message}")
                }
            }
        }
    }
}
