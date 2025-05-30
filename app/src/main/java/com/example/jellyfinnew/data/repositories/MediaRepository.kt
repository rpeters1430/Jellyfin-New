package com.example.jellyfinnew.data.repositories

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
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
 * Repository responsible for managing general media content (libraries, items, featured content)
 */
class MediaRepository {
    
    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("Media")
    }
    
    // Media libraries
    private val _mediaLibraries = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaLibraries: StateFlow<List<MediaItem>> = _mediaLibraries.asStateFlow()

    private val _currentLibraryItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentLibraryItems: StateFlow<List<MediaItem>> = _currentLibraryItems.asStateFlow()

    // Featured content
    private val _featuredItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val featuredItems: StateFlow<List<MediaItem>> = _featuredItems.asStateFlow()

    private val _featuredItem = MutableStateFlow<MediaItem?>(null)
    val featuredItem: StateFlow<MediaItem?> = _featuredItem.asStateFlow()

    // Recently added content
    private val _recentlyAdded = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val recentlyAdded: StateFlow<Map<String, List<MediaItem>>> = _recentlyAdded.asStateFlow()
    
    /**
     * Load media libraries from the server
     */
    suspend fun loadMediaLibraries(apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading media libraries...")
                val result = apiClient.userViewsApi.getUserViews()
                Log.d(TAG, "Got ${result.content.items?.size ?: 0} libraries")

                val libraries = result.content.items?.mapNotNull { item ->
                    createMediaItemFromDto(item, imageUrlHelper)
                } ?: emptyList()

                _mediaLibraries.value = libraries
                Log.d(TAG, "Loaded ${libraries.size} libraries")
            },
            errorMessage = "Failed to load media libraries"
        )
    }
    
    /**
     * Load items for a specific library
     */
    suspend fun loadLibraryItems(libraryId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading library items for: $libraryId")
                val result = apiClient.itemsApi.getItems(
                    parentId = UUID.fromString(libraryId),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW
                    ),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    limit = JellyfinConfig.UI.LIST_SIZE_LIMIT
                )

                val items = result.content.items?.mapNotNull { item ->
                    createMediaItemFromDto(item, imageUrlHelper)
                } ?: emptyList()

                _currentLibraryItems.value = items
                Log.d(TAG, "Loaded ${items.size} library items")
            },
            errorMessage = "Failed to load library items"
        )
    }
    
    /**
     * Load featured content from all libraries
     */
    suspend fun loadFeaturedContent(apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading featured content...")

                val userViewsApi = apiClient.userViewsApi
                val libraries = userViewsApi.getUserViews().content

                val allFeaturedItems = mutableListOf<MediaItem>()

                libraries?.items?.forEach { library ->
                    try {
                        Log.d(TAG, "Loading featured items for library: ${library.name}")
                        val itemsApi = apiClient.itemsApi

                        val response = itemsApi.getItems(
                            parentId = library.id,
                            sortBy = setOf(ItemSortBy.DATE_CREATED),
                            sortOrder = setOf(SortOrder.DESCENDING),
                            fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, ItemFields.OVERVIEW),
                            limit = 5
                        )

                        val items = response.content.items?.mapNotNull { movie ->
                            if (movie.overview?.isNotEmpty() == true) {
                                createMediaItemFromDto(movie, imageUrlHelper)
                            } else null
                        } ?: listOf()

                        allFeaturedItems.addAll(items)
                        Log.d(TAG, "Added ${items.size} featured items from library: ${library.name}")
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading featured items for library ${library.name}", e)
                    }
                }

                _featuredItems.value = allFeaturedItems.take(JellyfinConfig.UI.FEATURED_ITEMS_LIMIT)

                if (allFeaturedItems.isNotEmpty()) {
                    _featuredItem.value = allFeaturedItems.first()
                }

                Log.d(TAG, "Loaded ${allFeaturedItems.size} total featured items")
            },
            errorMessage = "Failed to load featured content"
        )
    }
    
    /**
     * Load recently added content from all libraries
     */
    suspend fun loadRecentlyAdded(apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading recently added content...")

                val userViewsApi = apiClient.userViewsApi
                val libraries = userViewsApi.getUserViews().content

                val recentlyAddedMap = mutableMapOf<String, List<MediaItem>>()

                libraries?.items?.forEach { library: BaseItemDto ->
                    try {
                        Log.d(TAG, "Loading recently added for library: ${library.name}")
                        val libraryName = library.name ?: "Unknown"

                        val isTvLibrary = libraryName.contains("TV", ignoreCase = true) ||
                                libraryName.contains("Show", ignoreCase = true) ||
                                libraryName.contains("Series", ignoreCase = true) ||
                                library.collectionType?.toString() == "tvshows"

                        Log.d(TAG, "Library: $libraryName, Type: ${library.collectionType}, isTvLibrary: $isTvLibrary")
                        
                        if (isTvLibrary) {
                            loadRecentlyAddedEpisodes(library, libraryName, apiClient, imageUrlHelper, recentlyAddedMap)
                        } else {
                            loadRecentlyAddedGeneral(library, libraryName, apiClient, imageUrlHelper, recentlyAddedMap)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error loading recently added for library ${library.name}", e)
                    }
                }

                _recentlyAdded.value = recentlyAddedMap
                Log.d(TAG, "Loaded recently added content for ${recentlyAddedMap.size} libraries:")
                recentlyAddedMap.forEach { (section: String, items: List<MediaItem>) ->
                    Log.d(TAG, "  - $section: ${items.size} items")
                }
            },
            errorMessage = "Failed to load recently added content"
        )
    }
    
    /**
     * Clear all media data
     */
    fun clearAllData() {
        _mediaLibraries.value = emptyList()
        _currentLibraryItems.value = emptyList()
        _featuredItem.value = null
        _featuredItems.value = emptyList()
        _recentlyAdded.value = emptyMap()    }
    
    // Private helper methods
    
    private suspend fun loadRecentlyAddedEpisodes(
        library: BaseItemDto,
        libraryName: String,
        apiClient: ApiClient,
        imageUrlHelper: ImageUrlHelper,
        recentlyAddedMap: MutableMap<String, List<MediaItem>>
    ) {
        Log.d(TAG, "Loading recently added episodes for TV library: $libraryName")
        val response = apiClient.itemsApi.getItems(
            parentId = library.id,
            includeItemTypes = setOf(BaseItemKind.EPISODE),
            sortBy = setOf(ItemSortBy.DATE_CREATED),
            sortOrder = setOf(SortOrder.DESCENDING),
            fields = setOf(
                ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                ItemFields.OVERVIEW,
                ItemFields.SERIES_PRIMARY_IMAGE,
                ItemFields.SERIES_STUDIO,
                ItemFields.DATE_CREATED
            ),
            limit = JellyfinConfig.UI.RECENTLY_ADDED_LIMIT,            recursive = true
        )

        Log.d(TAG, "Found ${response.content.items?.size ?: 0} episodes for $libraryName")
        
        val items = response.content.items?.mapNotNull { episode: BaseItemDto ->
            createEpisodeMediaItem(episode, imageUrlHelper)
        } ?: emptyList()

        Log.d(TAG, "Processed ${items.size} episode items for $libraryName")

        if (items.isNotEmpty()) {
            recentlyAddedMap["Recently Added Episodes - $libraryName"] = items
            Log.d(TAG, "Added recently added episodes section: Recently Added Episodes - $libraryName")
        }
    }
    
    private suspend fun loadRecentlyAddedGeneral(
        library: BaseItemDto,
        libraryName: String,
        apiClient: ApiClient,
        imageUrlHelper: ImageUrlHelper,
        recentlyAddedMap: MutableMap<String, List<MediaItem>>
    ) {
        val response = apiClient.itemsApi.getItems(
            parentId = library.id,
            sortBy = setOf(ItemSortBy.DATE_CREATED),
            sortOrder = setOf(SortOrder.DESCENDING),
            fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, ItemFields.OVERVIEW),            limit = JellyfinConfig.UI.RECENTLY_ADDED_LIMIT
        )

        val items = response.content.items?.mapNotNull { item: BaseItemDto ->
            createMediaItemFromDto(item, imageUrlHelper)
        } ?: emptyList()

        if (items.isNotEmpty()) {
            recentlyAddedMap["Recently Added - $libraryName"] = items
        }
    }
    
    private fun createMediaItemFromDto(
        dto: BaseItemDto,
        imageHelper: ImageUrlHelper
    ): MediaItem? {
        return try {
            val itemId = dto.id.toString()
            
            // Use proper image URLs based on item type
            val (imageUrl, backdropUrl) = when (dto.type) {
                BaseItemKind.COLLECTION_FOLDER, BaseItemKind.FOLDER -> {
                    // Libraries should use backdrop images
                    imageHelper.getImageUrlsForCardType(itemId, "library")
                }
                BaseItemKind.EPISODE -> {
                    // Episodes should use episode thumbs
                    imageHelper.getImageUrlsForCardType(itemId, "episode")
                }
                BaseItemKind.MUSIC_ARTIST, BaseItemKind.MUSIC_ALBUM -> {
                    // Music items should use square images
                    imageHelper.getImageUrlsForCardType(itemId, "square")
                }
                else -> {
                    // Movies, TV shows use poster format
                    imageHelper.getImageUrlsForCardType(itemId, "poster")
                }
            }

            MediaItem(
                id = itemId,
                name = dto.name ?: "Unknown",
                overview = dto.overview,
                imageUrl = imageUrl,
                backdropUrl = backdropUrl,
                type = dto.type,
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
    
    private fun createEpisodeMediaItem(episode: BaseItemDto, imageUrlHelper: ImageUrlHelper): MediaItem? {
        return try {
            val episodeId = episode.id.toString()
            val seriesId = episode.seriesId?.toString()
            val seriesName = episode.seriesName
            val seasonName = episode.seasonName
            val episodeNumber = episode.indexNumber

            Log.d(TAG, "Episode: ${episode.name}, Series: $seriesName, Season: $seasonName, Ep: $episodeNumber")

            // Build episode name with season and episode info
            val episodeName = buildString {
                if (seasonName != null && episodeNumber != null) {
                    append("$seasonName E$episodeNumber")
                } else if (episodeNumber != null) {
                    append("Episode $episodeNumber")
                }
                if (episode.name != null && episode.name != seriesName) {
                    if (isNotEmpty()) append(" - ")
                    append(episode.name)
                }
            }.takeIf { it.isNotEmpty() } ?: episode.name ?: "Episode"

            // Get proper episode images (horizontal format)
            val (episodeImageUrl, fallbackImageUrl) = imageUrlHelper.getImageUrlsForCardType(episodeId, "episode")
            val seriesPosterUrl = seriesId?.let { imageUrlHelper.buildPosterUrl(it) }

            MediaItem(
                id = episodeId,
                name = episodeName,
                overview = episode.overview,
                imageUrl = episodeImageUrl ?: seriesPosterUrl, // Episode thumb or series poster
                backdropUrl = fallbackImageUrl ?: seriesPosterUrl, // Backdrop or series poster
                type = BaseItemKind.EPISODE,
                runTimeTicks = episode.runTimeTicks,
                userData = episode.userData?.let { userData ->
                    UserData(
                        played = userData.played == true,
                        playbackPositionTicks = userData.playbackPositionTicks,
                        playCount = userData.playCount
                    )
                },
                productionYear = episode.productionYear,
                communityRating = episode.communityRating?.toDouble(),
                episodeName = episodeName,
                seriesName = seriesName,
                seriesId = seriesId,
                seriesPosterUrl = seriesPosterUrl
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create episode MediaItem: ${episode.name}", e)
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

    /**
     * Fetch streaming URL for a media item
     */
    suspend fun getStreamingUrl(itemId: String, apiClient: ApiClient): String? {
        return try {
            Log.d(TAG, "Fetching streaming URL for item: $itemId")
            val streamingRepository = StreamingRepository()
            streamingRepository.getDirectPlayUrl(itemId, apiClient)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch streaming URL for item: $itemId", e)
            null
        }
    }
}
