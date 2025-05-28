package com.example.jellyfinnew.ui.movies

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import com.example.jellyfinnew.data.JellyfinRepository
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.data.repositories.MoviesRepository
import android.util.Log
import com.example.jellyfinnew.data.JellyfinConfig

/**
 * ViewModel for managing movies screen state and operations
 */
class MoviesViewModel(
    private val repository: JellyfinRepository,
    private val moviesRepository: MoviesRepository
) : ViewModel() {

    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("MoviesVM")
    }

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _focusedMovie = MutableStateFlow<MediaItem?>(null)
    val focusedMovie: StateFlow<MediaItem?> = _focusedMovie.asStateFlow()

    // Repository state flows
    val movies: StateFlow<List<MediaItem>> = moviesRepository.movies
    val movieGenres: StateFlow<List<String>> = moviesRepository.movieGenres
    val currentMovieDetails: StateFlow<MediaItem?> = moviesRepository.currentMovieDetails    // Filtered movies based on selected genre
    val filteredMovies: StateFlow<List<MediaItem>> = combine(
        movies,
        selectedGenre
    ) { moviesList, genre ->
        if (genre.isNullOrEmpty()) {
            moviesList
        } else {
            moviesList // Movies are already filtered by repository when genre is selected
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Load movies for the specified library
     */
    fun loadMovies(libraryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading movies for library: $libraryId")
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    moviesRepository.loadMovies(libraryId, apiClient, imageUrlHelper)
                    
                    // Set the first movie as focused if no movie is currently focused
                    if (_focusedMovie.value == null && movies.value.isNotEmpty()) {
                        _focusedMovie.value = movies.value.first()
                    }
                } else {
                    Log.w(TAG, "Cannot load movies - not connected to server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading movies", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load movies filtered by genre
     */
    fun loadMoviesByGenre(libraryId: String, genre: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading movies for genre: $genre in library: $libraryId")
                _selectedGenre.value = genre
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    if (genre.isNullOrEmpty()) {
                        moviesRepository.loadMovies(libraryId, apiClient, imageUrlHelper)
                    } else {
                        moviesRepository.loadMoviesByGenre(libraryId, genre, apiClient, imageUrlHelper)
                    }
                    
                    // Update focused movie to first in filtered list
                    if (movies.value.isNotEmpty()) {
                        _focusedMovie.value = movies.value.first()
                    }
                } else {
                    Log.w(TAG, "Cannot load movies by genre - not connected to server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading movies by genre", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load detailed information for a specific movie
     */
    fun loadMovieDetails(movieId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading movie details for: $movieId")
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    moviesRepository.loadMovieDetails(movieId, apiClient, imageUrlHelper)
                } else {
                    Log.w(TAG, "Cannot load movie details - not connected to server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading movie details", e)
            }
        }
    }

    /**
     * Update the currently focused movie for UI purposes
     */
    fun updateFocusedMovie(movie: MediaItem?) {
        _focusedMovie.value = movie
        // Optionally load detailed info when movie is focused
        movie?.let { loadMovieDetails(it.id) }
    }

    /**
     * Clear selected genre filter
     */
    fun clearGenreFilter(libraryId: String) {
        loadMoviesByGenre(libraryId, null)
    }

    /**
     * Get stream URL for a movie
     */
    fun getStreamUrl(movieId: String): String? {
        return repository.getStreamUrl(movieId)
    }

    /**
     * Check if a movie supports direct play
     */
    fun supportsDirectPlay(container: String?, videoCodec: String?, audioCodec: String?): Boolean {
        return repository.supportsDirectPlay(container, videoCodec, audioCodec)
    }

    /**
     * Clear all movie data
     */
    fun clearData() {
        moviesRepository.clearAllData()
        _focusedMovie.value = null
        _selectedGenre.value = null
    }
}

/**
 * Factory for creating MoviesViewModel instances
 */
class MoviesViewModelFactory(
    private val repository: JellyfinRepository,
    private val moviesRepository: MoviesRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MoviesViewModel::class.java)) {
            return MoviesViewModel(repository, moviesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
