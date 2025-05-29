package com.example.jellyfinnew.ui.music

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
import android.util.Log
import com.example.jellyfinnew.data.JellyfinConfig

/**
 * ViewModel for managing music screen state and operations
 */
class MusicViewModel(
    private val repository: JellyfinRepository
) : ViewModel() {

    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("MusicVM")
    }

    // UI state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre: StateFlow<String?> = _selectedGenre.asStateFlow()

    private val _focusedArtist = MutableStateFlow<MediaItem?>(null)
    val focusedArtist: StateFlow<MediaItem?> = _focusedArtist.asStateFlow()

    private val _focusedAlbum = MutableStateFlow<MediaItem?>(null)
    val focusedAlbum: StateFlow<MediaItem?> = _focusedAlbum.asStateFlow()

    private val _focusedSong = MutableStateFlow<MediaItem?>(null)
    val focusedSong: StateFlow<MediaItem?> = _focusedSong.asStateFlow()

    // Repository state flows
    val artists: StateFlow<List<MediaItem>> = repository.artists
    val albums: StateFlow<List<MediaItem>> = repository.albums
    val songs: StateFlow<List<MediaItem>> = repository.songs
    val musicGenres: StateFlow<List<String>> = repository.musicGenres
    val currentArtist: StateFlow<MediaItem?> = repository.currentArtist
    val currentAlbum: StateFlow<MediaItem?> = repository.currentAlbum

    // Filtered artists based on selected genre
    val filteredArtists: StateFlow<List<MediaItem>> = combine(
        artists,
        selectedGenre
    ) { artistsList, genre ->
        if (genre.isNullOrEmpty()) {
            artistsList
        } else {
            artistsList // Artists are already filtered by repository when genre is selected
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Load artists for the specified music library
     */
    fun loadArtists(libraryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading artists for library: $libraryId")

                // Use repository delegate method instead of accessing connectionRepository directly
                repository.loadArtists(libraryId)

                // Set the first artist as focused if no artist is currently focused
                if (_focusedArtist.value == null && artists.value.isNotEmpty()) {
                    _focusedArtist.value = artists.value.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading artists", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load artists filtered by genre
     */
    fun loadArtistsByGenre(libraryId: String, genre: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading artists for genre: $genre in library: $libraryId")
                _selectedGenre.value = genre

                if (genre.isNullOrEmpty()) {
                    repository.loadArtists(libraryId)
                } else {
                    // For now, just load all artists and let the UI filter
                    repository.loadArtists(libraryId)
                }

                // Update focused artist to first in filtered list
                if (artists.value.isNotEmpty()) {
                    _focusedArtist.value = artists.value.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading artists by genre", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load albums for a specific artist
     */
    fun loadAlbums(artistId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading albums for artist: $artistId")
                repository.loadAlbums(artistId)

                // Set the first album as focused if no album is currently focused
                if (_focusedAlbum.value == null && albums.value.isNotEmpty()) {
                    _focusedAlbum.value = albums.value.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading albums", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load songs for a specific album
     */
    fun loadSongs(albumId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading songs for album: $albumId")
                repository.loadSongs(albumId)

                // Set the first song as focused if no song is currently focused
                if (_focusedSong.value == null && songs.value.isNotEmpty()) {
                    _focusedSong.value = songs.value.first()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Update the currently focused artist for UI purposes
     */
    fun updateFocusedArtist(artist: MediaItem?) {
        _focusedArtist.value = artist
    }

    /**
     * Update the currently focused album for UI purposes
     */
    fun updateFocusedAlbum(album: MediaItem?) {
        _focusedAlbum.value = album
    }

    /**
     * Update the currently focused song for UI purposes
     */
    fun updateFocusedSong(song: MediaItem?) {
        _focusedSong.value = song
    }

    /**
     * Clear selected genre filter
     */
    fun clearGenreFilter(libraryId: String) {
        loadArtistsByGenre(libraryId, null)
    }

    /**
     * Get stream URL for a song
     */
    fun getStreamUrl(songId: String): String? {
        return repository.getStreamUrl(songId)
    }

    /**
     * Check if a song supports direct play
     */
    fun supportsDirectPlay(container: String?, videoCodec: String?, audioCodec: String?): Boolean {
        return repository.supportsDirectPlay(container, videoCodec, audioCodec)
    }

    /**
     * Clear all music data
     */
    fun clearData() {
        repository.clearMusicData()
        _focusedArtist.value = null
        _focusedAlbum.value = null
        _focusedSong.value = null
        _selectedGenre.value = null
    }
}

/**
 * Factory for creating MusicViewModel instances
 */
class MusicViewModelFactory(
    private val repository: JellyfinRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            return MusicViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}