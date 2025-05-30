package com.example.jellyfinnew.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import com.example.jellyfinnew.data.repositories.ConnectionRepository
import com.example.jellyfinnew.data.repositories.MediaRepository
import com.example.jellyfinnew.data.repositories.TvShowsRepository
import com.example.jellyfinnew.data.repositories.StreamingRepository
import com.example.jellyfinnew.data.repositories.MoviesRepository
import com.example.jellyfinnew.data.repositories.MusicRepository

data class ConnectionState(
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class MediaItem(
    val id: String,
    val name: String,
    val overview: String? = null,
    val imageUrl: String? = null,
    val backdropUrl: String? = null,
    val type: org.jellyfin.sdk.model.api.BaseItemKind,
    val runTimeTicks: Long? = null,
    val userData: UserData? = null,
    val productionYear: Int? = null,
    val communityRating: Double? = null,
    val collectionType: String? = null,
    val genres: List<String>? = null,
    // Episode-specific fields
    val episodeName: String? = null,
    val seriesName: String? = null,
    val seriesId: String? = null,
    val seriesPosterUrl: String? = null
)

data class UserData(
    val played: Boolean,
    val playbackPositionTicks: Long?,
    val playCount: Int?
)

/**
 * Main repository that coordinates between focused repositories
 * This acts as a facade to maintain backward compatibility while using the new modular architecture
 */
class JellyfinRepository(
    private val androidContext: Context,
    val connectionRepository: ConnectionRepository,
    private val mediaRepository: MediaRepository,
    private val tvShowsRepository: TvShowsRepository,
    private val streamingRepository: StreamingRepository,
    private val moviesRepository: MoviesRepository,
    private val musicRepository: MusicRepository
) {

    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("Repository")
    }

    // Delegate connection state to ConnectionRepository
    val connectionState: StateFlow<ConnectionState> = connectionRepository.connectionState

    // Delegate media state to MediaRepository
    val mediaLibraries: StateFlow<List<MediaItem>> = mediaRepository.mediaLibraries
    val currentLibraryItems: StateFlow<List<MediaItem>> = mediaRepository.currentLibraryItems
    val featuredItems: StateFlow<List<MediaItem>> = mediaRepository.featuredItems
    val featuredItem: StateFlow<MediaItem?> = mediaRepository.featuredItem
    val recentlyAdded: StateFlow<Map<String, List<MediaItem>>> = mediaRepository.recentlyAdded    // Delegate TV shows state to TvShowsRepository
    val tvShows: StateFlow<List<MediaItem>> = tvShowsRepository.tvShows
    val tvSeasons: StateFlow<List<MediaItem>> = tvShowsRepository.tvSeasons
    val tvEpisodes: StateFlow<List<MediaItem>> = tvShowsRepository.tvEpisodes
    val currentSeries: StateFlow<MediaItem?> = tvShowsRepository.currentSeries
    val currentSeason: StateFlow<MediaItem?> = tvShowsRepository.currentSeason    // Delegate Movies state to MoviesRepository
    val movies: StateFlow<List<MediaItem>> = moviesRepository.movies
    val movieGenres: StateFlow<List<String>> = moviesRepository.movieGenres
    val currentMovieDetails: StateFlow<MediaItem?> = moviesRepository.currentMovieDetails    // Delegate Music state to MusicRepository
    val artists: StateFlow<List<MediaItem>> = musicRepository.artists
    val albums: StateFlow<List<MediaItem>> = musicRepository.albums
    val songs: StateFlow<List<MediaItem>> = musicRepository.songs
    val musicGenres: StateFlow<List<String>> = musicRepository.musicGenres
    val currentArtist: StateFlow<MediaItem?> = musicRepository.currentArtist
    val currentAlbum: StateFlow<MediaItem?> = musicRepository.currentAlbum    /**
     * Connect to Jellyfin server with authentication
     */
    suspend fun connect(serverUrl: String, username: String, password: String): Boolean {
        return try {
            Log.d(TAG, "Coordinating connection through ConnectionRepository")
            val connected = connectionRepository.connect(serverUrl, username, password)
            
            if (connected) {
                Log.d(TAG, "Connection successful, loading initial data")
                loadInitialData()
            }
            
            connected        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Connection cancelled (navigation)")
            throw e // Re-throw to maintain proper coroutine cancellation semantics
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Failed to connect to server")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
            false
        }
    }

    /**
     * Enhanced connect method that handles self-signed certificates and reverse proxy scenarios
     */
    suspend fun connectWithSSLFallback(
        serverUrl: String,
        username: String,
        password: String
    ): Boolean {
        val result = connect(serverUrl, username, password)
        
        if (!result && serverUrl.startsWith("https://")) {
            Log.i(TAG, "HTTPS connection failed, this may be due to self-signed certificate")
            Log.i(TAG, "Consider installing the certificate or configuring your reverse proxy with a valid certificate")
        }
        
        return result
    }

    /**
     * Disconnect from Jellyfin server
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from server")
        connectionRepository.disconnect()
        clearAllData()
    }

    /**
     * Load media libraries from the server
     */
    suspend fun loadMediaLibraries() {
        val apiClient = connectionRepository.apiClient
        val imageUrlHelper = connectionRepository.imageUrlHelper
        
        if (apiClient != null && imageUrlHelper != null) {
            Log.d(TAG, "Loading media libraries through MediaRepository")
            mediaRepository.loadMediaLibraries(apiClient, imageUrlHelper)
        } else {
            Log.w(TAG, "Cannot load media libraries - not connected")
        }
    }

    /**
     * Load items for a specific library
     */
    suspend fun loadLibraryItems(libraryId: String) {
        val apiClient = connectionRepository.apiClient
        val imageUrlHelper = connectionRepository.imageUrlHelper
        
        if (apiClient != null && imageUrlHelper != null) {
            Log.d(TAG, "Loading library items through MediaRepository")
            mediaRepository.loadLibraryItems(libraryId, apiClient, imageUrlHelper)
        } else {
            Log.w(TAG, "Cannot load library items - not connected")
        }
    }

    /**
     * Load featured content from all libraries
     */
    suspend fun loadFeaturedContent() {
        val apiClient = connectionRepository.apiClient
        val imageUrlHelper = connectionRepository.imageUrlHelper
        
        if (apiClient != null && imageUrlHelper != null) {
            Log.d(TAG, "Loading featured content through MediaRepository")
            mediaRepository.loadFeaturedContent(apiClient, imageUrlHelper)
        } else {
            Log.w(TAG, "Cannot load featured content - not connected")
        }
    }

    /**
     * Load recently added content from all libraries
     */
    suspend fun loadRecentlyAdded() {
        val apiClient = connectionRepository.apiClient
        val imageUrlHelper = connectionRepository.imageUrlHelper
        
        if (apiClient != null && imageUrlHelper != null) {
            Log.d(TAG, "Loading recently added content through MediaRepository")
            mediaRepository.loadRecentlyAdded(apiClient, imageUrlHelper)
        } else {
            Log.w(TAG, "Cannot load recently added content - not connected")
        }
    }

    /**
     * Load TV shows from a specific library
     */
    suspend fun loadTvShows(libraryId: String) {
        val apiClient = connectionRepository.apiClient
        val imageUrlHelper = connectionRepository.imageUrlHelper
        
        if (apiClient != null && imageUrlHelper != null) {
            Log.d(TAG, "Loading TV shows through TvShowsRepository")
            tvShowsRepository.loadTvShows(libraryId, apiClient, imageUrlHelper)
        } else {
            Log.w(TAG, "Cannot load TV shows - not connected")
        }
    }

    /**
     * Load seasons for a specific TV series
     */
    suspend fun loadTvSeasons(seriesId: String) {
        val apiClient = connectionRepository.apiClient
        val imageUrlHelper = connectionRepository.imageUrlHelper
        
        if (apiClient != null && imageUrlHelper != null) {
            Log.d(TAG, "Loading TV seasons through TvShowsRepository")
            tvShowsRepository.loadTvSeasons(seriesId, apiClient, imageUrlHelper)
        } else {
            Log.w(TAG, "Cannot load TV seasons - not connected")
        }
    }

    /**
     * Load episodes for a specific season
     */
    suspend fun loadTvEpisodes(seriesId: String, seasonId: String) {
        val apiClient = connectionRepository.apiClient
        val imageUrlHelper = connectionRepository.imageUrlHelper
        
        if (apiClient != null && imageUrlHelper != null) {
            Log.d(TAG, "Loading TV episodes through TvShowsRepository")
            tvShowsRepository.loadTvEpisodes(seriesId, seasonId, apiClient, imageUrlHelper)
        } else {
            Log.w(TAG, "Cannot load TV episodes - not connected")
        }
    }

    /**
     * Get stream URL for media playback
     */
    fun getStreamUrl(itemId: String): String? {
        Log.d(TAG, "Getting stream URL through StreamingRepository")
        return streamingRepository.getStreamUrl(itemId, connectionRepository.imageUrlHelper)
    }

    /**
     * Get transcoding URL for media playback with specific quality
     */
    fun getTranscodingUrl(
        itemId: String,
        maxBitrate: Int = JellyfinConfig.Streaming.DEFAULT_MAX_BITRATE,
        audioCodec: String = JellyfinConfig.Streaming.DEFAULT_AUDIO_CODEC,
        videoCodec: String = JellyfinConfig.Streaming.DEFAULT_VIDEO_CODEC
    ): String? {
        Log.d(TAG, "Getting transcoding URL through StreamingRepository")
        return streamingRepository.getTranscodingUrl(
            itemId, 
            connectionRepository.apiClient, 
            maxBitrate, 
            audioCodec, 
            videoCodec
        )
    }

    /**
     * Get HLS streaming URL for adaptive streaming
     */
    fun getHlsUrl(
        itemId: String,
        maxBitrate: Int = JellyfinConfig.Streaming.DEFAULT_MAX_BITRATE
    ): String? {
        Log.d(TAG, "Getting HLS URL through StreamingRepository")
        return streamingRepository.getHlsUrl(itemId, connectionRepository.apiClient, maxBitrate)
    }

    /**
     * Check if direct play is supported for media
     */
    fun supportsDirectPlay(
        container: String?,
        videoCodec: String?,
        audioCodec: String?
    ): Boolean {
        return streamingRepository.supportsDirectPlay(container, videoCodec, audioCodec)
    }    /**
     * Load initial data after successful connection
     */
    private suspend fun loadInitialData() {
        try {
            Log.d(TAG, "Loading initial data after successful connection")
            
            // Load data sequentially to avoid overwhelming the server
            loadMediaLibraries()
            loadFeaturedContent()
            loadRecentlyAdded()
              Log.d(TAG, "Initial data loading completed")
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.d(TAG, "Initial data loading cancelled (navigation)")
            throw e // Re-throw to maintain proper coroutine cancellation semantics
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Failed to load initial data")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
        }
    }/**
     * Clear all data from repositories
     */
    private fun clearAllData() {
        Log.d(TAG, "Clearing all repository data")
        mediaRepository.clearAllData()
        tvShowsRepository.clearAllData()
        moviesRepository.clearAllData()
        musicRepository.clearAllData()
    }    // Delegated methods for MoviesRepository
    suspend fun loadMovies(libraryId: String) {
        val client = connectionRepository.apiClient
        val helper = connectionRepository.imageUrlHelper
        if (client != null && helper != null) {
            moviesRepository.loadMovies(libraryId, client, helper)
        }
    }
      suspend fun loadMovieDetails(movieId: String) {
        val client = connectionRepository.apiClient
        val helper = connectionRepository.imageUrlHelper
        if (client != null && helper != null) {
            moviesRepository.loadMovieDetails(movieId, client, helper)
        }
    }
    
    fun clearMoviesData() = moviesRepository.clearAllData()

    // Delegated methods for MusicRepository
    suspend fun loadArtists(libraryId: String) {
        val client = connectionRepository.apiClient
        val helper = connectionRepository.imageUrlHelper
        if (client != null && helper != null) {
            musicRepository.loadArtists(libraryId, client, helper)
        }
    }
    
    suspend fun loadAlbums(artistId: String) {
        val client = connectionRepository.apiClient
        val helper = connectionRepository.imageUrlHelper
        if (client != null && helper != null) {
            musicRepository.loadAlbums(artistId, client, helper)
        }
    }
    
    suspend fun loadSongs(albumId: String) {
        val client = connectionRepository.apiClient
        val helper = connectionRepository.imageUrlHelper
        if (client != null && helper != null) {
            musicRepository.loadSongs(albumId, client, helper)
        }
    }
    
    fun clearMusicData() = musicRepository.clearAllData()
}