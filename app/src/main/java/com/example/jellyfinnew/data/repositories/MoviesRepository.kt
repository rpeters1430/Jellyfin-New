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
 * Repository responsible for managing movies content
 */
class MoviesRepository {
    
    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("Movies")
    }
    
    // Movies specific
    private val _movies = MutableStateFlow<List<MediaItem>>(emptyList())
    val movies: StateFlow<List<MediaItem>> = _movies.asStateFlow()

    private val _movieGenres = MutableStateFlow<List<String>>(emptyList())
    val movieGenres: StateFlow<List<String>> = _movieGenres.asStateFlow()

    private val _currentMovieDetails = MutableStateFlow<MediaItem?>(null)
    val currentMovieDetails: StateFlow<MediaItem?> = _currentMovieDetails.asStateFlow()    /**
     * Load movies from a specific library
     */
    suspend fun loadMovies(libraryId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        Log.d(TAG, "Loading movies for library: $libraryId")
        
        safeApiCall(
            operation = {
                val response = apiClient.itemsApi.getItems(
                    parentId = UUID.fromString(libraryId),
                    includeItemTypes = setOf(BaseItemKind.MOVIE),
                    fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, ItemFields.GENRES),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    recursive = true,
                    limit = JellyfinConfig.UI.LIST_SIZE_LIMIT
                )

                response.content.items?.mapNotNull { movie ->
                    createMovieMediaItem(movie, imageUrlHelper)
                } ?: emptyList()
            },
            onSuccess = { moviesList ->
                _movies.value = moviesList
                  // Extract unique genres
                val genres = moviesList.flatMap { it.genres ?: emptyList() }.distinct().sorted()
                _movieGenres.value = genres
                
                Log.d(TAG, "Loaded ${moviesList.size} movies with ${genres.size} unique genres")
            },
            errorMessage = "Failed to load movies"
        )
    }    /**
     * Load movies by genre
     */
    suspend fun loadMoviesByGenre(libraryId: String, genre: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading movies for genre: $genre in library: $libraryId")
                val response = apiClient.itemsApi.getItems(
                    parentId = UUID.fromString(libraryId),
                    includeItemTypes = setOf(BaseItemKind.MOVIE),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.GENRES
                    ),
                    genres = setOf(genre),
                    recursive = true,
                    limit = JellyfinConfig.UI.LIST_SIZE_LIMIT
                )

                response.content.items?.mapNotNull { movie ->
                    createMovieMediaItem(movie, imageUrlHelper)
                } ?: emptyList()
            },
            onSuccess = { movies ->
                _movies.value = movies
                Log.d(TAG, "Loaded ${movies.size} movies for genre: $genre")
            },
            errorMessage = "Failed to load movies by genre"
        )
    }    /**
     * Load movie details
     */
    suspend fun loadMovieDetails(movieId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading movie details for: $movieId")
                val response = apiClient.itemsApi.getItems(
                    ids = setOf(UUID.fromString(movieId)),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW,
                        ItemFields.GENRES,
                        ItemFields.PEOPLE,
                        ItemFields.STUDIOS
                    )
                )

                response.content.items?.firstOrNull()?.let { movie ->
                    createMovieMediaItem(movie, imageUrlHelper)
                }
            },
            onSuccess = { movieDetails ->
                _currentMovieDetails.value = movieDetails
                Log.d(TAG, "Loaded movie details: ${movieDetails?.name}")
            },
            errorMessage = "Failed to load movie details"
        )
    }
    
    /**
     * Clear all movies data
     */
    fun clearAllData() {
        _movies.value = emptyList()
        _movieGenres.value = emptyList()
        _currentMovieDetails.value = null
    }
    
    // Private helper methods
    
    private fun createMovieMediaItem(
        movie: BaseItemDto,
        imageHelper: ImageUrlHelper
    ): MediaItem? {
        return try {
            val itemId = movie.id.toString()
            val (posterUrl, backdropUrl) = imageHelper.buildMediaImageUrls(itemId)

            MediaItem(
                id = itemId,
                name = movie.name ?: "Unknown Movie",
                overview = movie.overview,
                imageUrl = posterUrl,
                backdropUrl = backdropUrl,
                type = movie.type ?: BaseItemKind.MOVIE,
                runTimeTicks = movie.runTimeTicks,
                userData = movie.userData?.let { userData ->
                    UserData(
                        played = userData.played == true,
                        playbackPositionTicks = userData.playbackPositionTicks,
                        playCount = userData.playCount
                    )
                },
                productionYear = movie.productionYear,
                communityRating = movie.communityRating?.toDouble(),
                collectionType = null,
                genres = movie.genreItems?.map { it.name.orEmpty() } ?: emptyList()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create movie MediaItem: ${movie.name}", e)
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
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Expected when navigation cancels operations - don't log as error
            Log.d(TAG, "$errorMessage: Operation cancelled (navigation)")
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, errorMessage)
            Log.e(TAG, "$errorMessage: ${error.getUserFriendlyMessage()}", e)
        }
    }
}
