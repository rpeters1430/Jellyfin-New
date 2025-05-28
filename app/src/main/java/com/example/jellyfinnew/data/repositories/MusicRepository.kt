package com.example.jellyfinnew.data.repositories

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import java.util.UUID
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.data.UserData
import com.example.jellyfinnew.data.ImageUrlHelper
import com.example.jellyfinnew.data.ErrorHandler
import com.example.jellyfinnew.data.JellyfinConfig

/**
 * Repository responsible for managing music content (artists, albums, songs)
 */
class MusicRepository {
    
    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("Music")
    }
    
    // Artists specific
    private val _artists = MutableStateFlow<List<MediaItem>>(emptyList())
    val artists: StateFlow<List<MediaItem>> = _artists.asStateFlow()

    private val _musicGenres = MutableStateFlow<List<String>>(emptyList())
    val musicGenres: StateFlow<List<String>> = _musicGenres.asStateFlow()
    
    // Albums specific
    private val _albums = MutableStateFlow<List<MediaItem>>(emptyList())
    val albums: StateFlow<List<MediaItem>> = _albums.asStateFlow()
    
    private val _currentArtist = MutableStateFlow<MediaItem?>(null)
    val currentArtist: StateFlow<MediaItem?> = _currentArtist.asStateFlow()
    
    // Songs specific
    private val _songs = MutableStateFlow<List<MediaItem>>(emptyList())
    val songs: StateFlow<List<MediaItem>> = _songs.asStateFlow()
    
    private val _currentAlbum = MutableStateFlow<MediaItem?>(null)
    val currentAlbum: StateFlow<MediaItem?> = _currentAlbum.asStateFlow()
    
    /**
     * Load artists from a specific music library
     */
    suspend fun loadArtists(libraryId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading artists for library: $libraryId")

                val response = apiClient.itemsApi.getItems(
                    parentId = UUID.fromString(libraryId),
                    includeItemTypes = setOf(BaseItemKind.MUSIC_ARTIST),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.GENRES,
                        ItemFields.ITEM_COUNTS
                    ),
                    recursive = true
                )

                val artists = response.content.items?.mapNotNull { artist ->
                    createMediaItemFromDto(artist, imageUrlHelper)
                } ?: emptyList()

                // Extract unique genres from all artists
                val allGenres = response.content.items?.flatMap { artist ->
                    artist.genres ?: emptyList()
                }?.distinct()?.sorted() ?: emptyList()

                _musicGenres.value = allGenres
                
                artists
            },
            onSuccess = { artists ->
                _artists.value = artists
                Log.d(TAG, "Loaded ${artists.size} artists with ${_musicGenres.value.size} genres")
            },
            errorMessage = "Failed to load artists"
        )
    }
    
    /**
     * Load artists filtered by genre
     */
    suspend fun loadArtistsByGenre(libraryId: String, genre: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading artists for genre: $genre in library: $libraryId")

                val response = apiClient.itemsApi.getItems(
                    parentId = UUID.fromString(libraryId),
                    includeItemTypes = setOf(BaseItemKind.MUSIC_ARTIST),
                    genres = setOf(genre),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.GENRES,
                        ItemFields.ITEM_COUNTS
                    ),
                    recursive = true
                )

                response.content.items?.mapNotNull { artist ->
                    createMediaItemFromDto(artist, imageUrlHelper)
                } ?: emptyList()
            },
            onSuccess = { artists ->
                _artists.value = artists
                Log.d(TAG, "Loaded ${artists.size} artists for genre: $genre")
            },
            errorMessage = "Failed to load artists by genre"
        )
    }
    
    /**
     * Load albums for a specific artist
     */
    suspend fun loadAlbums(artistId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading albums for artist: $artistId")

                // Get artist details
                val artistResponse = apiClient.itemsApi.getItems(
                    ids = setOf(UUID.fromString(artistId)),
                    fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                )

                val artist = artistResponse.content.items?.firstOrNull()
                val currentArtist = artist?.let {
                    createMediaItemFromDto(it, imageUrlHelper)
                }

                // Get albums for this artist
                val albumsResponse = apiClient.itemsApi.getItems(
                    artistIds = setOf(UUID.fromString(artistId)),                    includeItemTypes = setOf(BaseItemKind.MUSIC_ALBUM),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.DESCENDING),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.GENRES,
                        ItemFields.ITEM_COUNTS
                    ),
                    recursive = true
                )

                val albums = albumsResponse.content.items?.mapNotNull { album ->
                    createAlbumMediaItem(album, imageUrlHelper, artistId, artist?.name)
                } ?: emptyList()

                Pair(currentArtist, albums)
            },
            onSuccess = { (currentArtist, albums) ->
                _currentArtist.value = currentArtist
                _albums.value = albums
                Log.d(TAG, "Loaded ${albums.size} albums for artist: ${currentArtist?.name}")
            },
            errorMessage = "Failed to load albums"
        )
    }
    
    /**
     * Load songs for a specific album
     */
    suspend fun loadSongs(albumId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading songs for album: $albumId")

                // Get album details
                val albumResponse = apiClient.itemsApi.getItems(
                    ids = setOf(UUID.fromString(albumId)),
                    fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                )

                val album = albumResponse.content.items?.firstOrNull()
                val currentAlbum = album?.let {
                    createMediaItemFromDto(it, imageUrlHelper)
                }

                // Get songs for this album
                val songsResponse = apiClient.itemsApi.getItems(
                    parentId = UUID.fromString(albumId),                    includeItemTypes = setOf(BaseItemKind.AUDIO),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.GENRES
                    )
                )

                val songs = songsResponse.content.items?.mapNotNull { song ->
                    createSongMediaItem(song, imageUrlHelper, albumId, album?.name, _currentArtist.value?.name)
                } ?: emptyList()

                Pair(currentAlbum, songs)
            },
            onSuccess = { (currentAlbum, songs) ->
                _currentAlbum.value = currentAlbum
                _songs.value = songs
                Log.d(TAG, "Loaded ${songs.size} songs for album: ${currentAlbum?.name}")
            },
            errorMessage = "Failed to load songs"
        )
    }
      /**
     * Load song details by ID
     */
    suspend fun loadSongDetails(songId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper): MediaItem? {
        return try {
            Log.d(TAG, "Loading song details for: $songId")
            val response = apiClient.itemsApi.getItems(
                ids = setOf(UUID.fromString(songId)),
                fields = setOf(
                    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                    ItemFields.OVERVIEW,
                    ItemFields.GENRES
                )
            )

            val songDto = response.content.items?.firstOrNull()
            songDto?.let { song ->
                createMediaItemFromDto(song, imageUrlHelper)
            }
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, "Failed to load song details")
            Log.e(TAG, error.getUserFriendlyMessage(), e)
            null
        }
    }
    
    /**
     * Clear all music data
     */
    fun clearAllData() {
        _artists.value = emptyList()
        _albums.value = emptyList()
        _songs.value = emptyList()
        _musicGenres.value = emptyList()
        _currentArtist.value = null
        _currentAlbum.value = null
    }
    
    // Private helper methods
    
    private fun createMediaItemFromDto(
        dto: BaseItemDto,
        imageHelper: ImageUrlHelper
    ): MediaItem? {
        return try {
            val itemId = dto.id.toString()
            val (posterUrl, backdropUrl) = imageHelper.buildMediaImageUrls(itemId)

            MediaItem(
                id = itemId,
                name = dto.name ?: "Unknown",
                overview = dto.overview,
                imageUrl = posterUrl,
                backdropUrl = backdropUrl,
                type = dto.type ?: BaseItemKind.FOLDER,
                runTimeTicks = dto.runTimeTicks,
                userData = dto.userData?.let { userData ->
                    UserData(
                        played = userData.played == true,
                        playbackPositionTicks = userData.playbackPositionTicks,
                        playCount = userData.playCount
                    )
                },
                productionYear = dto.productionYear,
                communityRating = dto.communityRating?.toDouble(),
                collectionType = dto.collectionType?.toString()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create MediaItem from DTO: ${dto.name}", e)
            null
        }
    }

    private fun createAlbumMediaItem(
        album: BaseItemDto,
        imageHelper: ImageUrlHelper,
        artistId: String,
        artistName: String?
    ): MediaItem? {
        return try {
            val albumId = album.id.toString()
            val albumImageUrl = imageHelper.buildPosterUrl(albumId)
            val backdropUrl = imageHelper.buildBackdropUrl(albumId)

            MediaItem(
                id = albumId,
                name = album.name ?: "Album",
                overview = album.overview,
                imageUrl = albumImageUrl,
                backdropUrl = backdropUrl,
                type = album.type ?: BaseItemKind.MUSIC_ALBUM,
                runTimeTicks = null,
                userData = album.userData?.let { userData ->
                    UserData(
                        played = userData.played == true,
                        playbackPositionTicks = userData.playbackPositionTicks,
                        playCount = userData.playCount
                    )
                },
                productionYear = album.productionYear,
                communityRating = album.communityRating?.toDouble(),
                collectionType = null,
                seriesId = artistId, // Use seriesId field for artistId
                seriesName = artistName // Use seriesName field for artistName
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create album MediaItem: ${album.name}", e)
            null
        }
    }

    private fun createSongMediaItem(
        song: BaseItemDto,
        imageHelper: ImageUrlHelper,
        albumId: String,
        albumName: String?,
        artistName: String?
    ): MediaItem? {
        return try {
            val songId = song.id.toString()
            val songImageUrl = imageHelper.buildThumbUrl(songId)
            val songBackdrop = imageHelper.buildBackdropUrl(songId)
            val albumPosterUrl = imageHelper.buildPosterUrl(albumId)

            // Format song name with track number if available
            val songName = buildString {
                if (song.indexNumber != null) {
                    append("${song.indexNumber}. ")
                }
                append(song.name ?: "Unknown Song")
            }

            MediaItem(
                id = songId,
                name = songName,
                overview = song.overview,
                imageUrl = songImageUrl ?: albumPosterUrl,
                backdropUrl = songBackdrop ?: albumPosterUrl,
                type = BaseItemKind.AUDIO,
                runTimeTicks = song.runTimeTicks,
                userData = song.userData?.let { userData ->
                    UserData(
                        played = userData.played == true,
                        playbackPositionTicks = userData.playbackPositionTicks,
                        playCount = userData.playCount
                    )
                },
                productionYear = song.productionYear,
                communityRating = song.communityRating?.toDouble(),
                episodeName = songName, // Use episodeName for song title
                seriesName = artistName, // Use seriesName for artist name
                seriesId = albumId, // Use seriesId for album ID
                seriesPosterUrl = albumPosterUrl // Use seriesPosterUrl for album artwork
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create song MediaItem: ${song.name}", e)
            null
        }
    }
    
    private suspend fun <T> safeApiCall(
        operation: suspend () -> T,
        onSuccess: suspend (T) -> Unit = {},
        errorMessage: String = "API call failed"
    ) {
        try {
            val result = operation()
            onSuccess(result)
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, errorMessage)
            Log.e(TAG, error.getUserFriendlyMessage(), e)
        }
    }
}
