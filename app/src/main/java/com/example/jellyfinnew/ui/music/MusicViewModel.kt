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
import com.example.jellyfinnew.data.repositories.MusicRepository
import android.util.Log
import com.example.jellyfinnew.data.JellyfinConfig

/**
 * ViewModel for managing music screen state and operations
 */
class MusicViewModel(
    private val repository: JellyfinRepository,
    private val musicRepository: MusicRepository
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
    val artists: StateFlow<List<MediaItem>> = musicRepository.artists
    val albums: StateFlow<List<MediaItem>> = musicRepository.albums
    val songs: StateFlow<List<MediaItem>> = musicRepository.songs
    val musicGenres: StateFlow<List<String>> = musicRepository.musicGenres
    val currentArtist: StateFlow<MediaItem?> = musicRepository.currentArtist
    val currentAlbum: StateFlow<MediaItem?> = musicRepository.currentAlbum    // Filtered artists based on selected genre
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
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    musicRepository.loadArtists(libraryId, apiClient, imageUrlHelper)
                    
                    // Set the first artist as focused if no artist is currently focused
                    if (_focusedArtist.value == null && artists.value.isNotEmpty()) {
                        _focusedArtist.value = artists.value.first()
                    }
                } else {
                    Log.w(TAG, "Cannot load artists - not connected to server")
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
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    if (genre.isNullOrEmpty()) {
                        musicRepository.loadArtists(libraryId, apiClient, imageUrlHelper)
                    } else {
                        musicRepository.loadArtistsByGenre(libraryId, genre, apiClient, imageUrlHelper)
                    }
                    
                    // Update focused artist to first in filtered list
                    if (artists.value.isNotEmpty()) {
                        _focusedArtist.value = artists.value.first()
                    }
                } else {
                    Log.w(TAG, "Cannot load artists by genre - not connected to server")
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
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    musicRepository.loadAlbums(artistId, apiClient, imageUrlHelper)
                    
                    // Set the first album as focused if no album is currently focused
                    if (_focusedAlbum.value == null && albums.value.isNotEmpty()) {
                        _focusedAlbum.value = albums.value.first()
                    }
                } else {
                    Log.w(TAG, "Cannot load albums - not connected to server")
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
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    musicRepository.loadSongs(albumId, apiClient, imageUrlHelper)
                    
                    // Set the first song as focused if no song is currently focused
                    if (_focusedSong.value == null && songs.value.isNotEmpty()) {
                        _focusedSong.value = songs.value.first()
                    }
                } else {
                    Log.w(TAG, "Cannot load songs - not connected to server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading songs", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load detailed information for a specific song
     */
    fun loadSongDetails(songId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading song details for: $songId")
                
                val apiClient = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.apiClient else null
                }
                
                val imageUrlHelper = repository.connectionState.value.let {
                    if (it.isConnected) repository.connectionRepository.imageUrlHelper else null
                }

                if (apiClient != null && imageUrlHelper != null) {
                    musicRepository.loadSongDetails(songId, apiClient, imageUrlHelper)
                } else {
                    Log.w(TAG, "Cannot load song details - not connected to server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading song details", e)
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
        // Optionally load detailed info when song is focused
        song?.let { loadSongDetails(it.id) }
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
        musicRepository.clearAllData()
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
    private val repository: JellyfinRepository,
    private val musicRepository: MusicRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MusicViewModel::class.java)) {
            return MusicViewModel(repository, musicRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
