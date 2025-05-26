package com.example.jellyfinnew.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jellyfin.sdk.createJellyfin
import java.util.UUID
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.DeviceInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder

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
    val type: BaseItemKind,
    val runTimeTicks: Long? = null,
    val userData: UserData? = null,
    val productionYear: Int? = null,
    val communityRating: Double? = null,
    val collectionType: String? = null, // For library detection
    // Episode-specific fields
    val episodeName: String? = null, // For TV episodes
    val seriesName: String? = null, // Show name for episodes
    val seriesId: String? = null, // Show ID for episodes
    val seriesPosterUrl: String? = null // Show poster for episodes
)

data class UserData(
    val played: Boolean,
    val playbackPositionTicks: Long?,
    val playCount: Int?
)

class JellyfinRepository(androidContext: Context) {
    private val jellyfin = createJellyfin {
        clientInfo = ClientInfo("Jellyfin Android TV", "1.0.0")
        context = androidContext
    }
    
    private var apiClient: ApiClient? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _mediaLibraries = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaLibraries: StateFlow<List<MediaItem>> = _mediaLibraries.asStateFlow()
    
    private val _currentLibraryItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val currentLibraryItems: StateFlow<List<MediaItem>> = _currentLibraryItems.asStateFlow()
    
    private val _featuredItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val featuredItems: StateFlow<List<MediaItem>> = _featuredItems.asStateFlow()
    
    private val _featuredItem = MutableStateFlow<MediaItem?>(null)
    val featuredItem: StateFlow<MediaItem?> = _featuredItem.asStateFlow()
    
    private val _recentlyAdded = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val recentlyAdded: StateFlow<Map<String, List<MediaItem>>> = _recentlyAdded.asStateFlow()
    
    private val _featuredContent = MutableStateFlow<MediaItem?>(null)
    val featuredContent: StateFlow<MediaItem?> = _featuredContent.asStateFlow()
      private val _recentlyAddedByLibrary = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val recentlyAddedByLibrary: StateFlow<Map<String, List<MediaItem>>> = _recentlyAddedByLibrary.asStateFlow()
    
    private val _tvShows = MutableStateFlow<List<MediaItem>>(emptyList())
    val tvShows: StateFlow<List<MediaItem>> = _tvShows.asStateFlow()
    
    private val _tvSeasons = MutableStateFlow<List<MediaItem>>(emptyList())
    val tvSeasons: StateFlow<List<MediaItem>> = _tvSeasons.asStateFlow()
    
    private val _tvEpisodes = MutableStateFlow<List<MediaItem>>(emptyList())
    val tvEpisodes: StateFlow<List<MediaItem>> = _tvEpisodes.asStateFlow()
    
    private val _currentSeries = MutableStateFlow<MediaItem?>(null)
    val currentSeries: StateFlow<MediaItem?> = _currentSeries.asStateFlow()
    
    private val _currentSeason = MutableStateFlow<MediaItem?>(null)
    val currentSeason: StateFlow<MediaItem?> = _currentSeason.asStateFlow()

    suspend fun connect(serverUrl: String, username: String, password: String): Boolean {
        return try {
            println("DEBUG: Attempting to connect to $serverUrl with username: $username")
            _connectionState.value = _connectionState.value.copy(isLoading = true, error = null)
            
            val client = jellyfin.createApi(            baseUrl = serverUrl
            )
            
            println("DEBUG: Authenticating user...")
            val authResult = client.userApi.authenticateUserByName(
                username = username,
                password = password
            )
            
            authResult.content.accessToken?.let { token ->
                println("DEBUG: Authentication successful, got token: ${token.take(10)}...")
                
                // Store user information from auth response
                val user = authResult.content.user
                val userId = user?.id
                println("DEBUG: User ID: $userId, User Name: ${user?.name}")
                
                // Create a new authenticated client with the token
                val authenticatedClient = jellyfin.createApi(
                    baseUrl = serverUrl,
                    accessToken = token
                )
                
                apiClient = authenticatedClient
                
                println("DEBUG: Authenticated client accessToken: ${authenticatedClient.accessToken?.take(10)}...")
                _connectionState.value = ConnectionState(isConnected = true)
                
                println("DEBUG: Loading initial data...")
                
                // Load initial data
                loadMediaLibraries()
                loadFeaturedContent()
                loadRecentlyAdded()
                
                true
            } ?: run {
                println("DEBUG: Authentication failed - no access token received")
                _connectionState.value = ConnectionState(error = "Authentication failed")
                false
            }
        } catch (e: Exception) {
            println("DEBUG: Connection error: ${e.message}")
            e.printStackTrace()
            _connectionState.value = ConnectionState(error = e.message ?: "Unknown error")
            false
        }
    }
    
    fun disconnect() {
        apiClient = null
        _connectionState.value = ConnectionState()
        _mediaLibraries.value = emptyList()
        _currentLibraryItems.value = emptyList()
        _featuredItem.value = null
        _featuredItems.value = emptyList()
        _recentlyAdded.value = emptyMap()
    }
    
    suspend fun loadMediaLibraries() {
        try {
            apiClient?.let { client ->
                println("DEBUG: Loading media libraries...")
                val result = client.userViewsApi.getUserViews()
                println("DEBUG: Got ${result.content.items?.size ?: 0} libraries")
                
                val libraries = result.content.items?.mapNotNull { item ->
                    val primaryImageUrl = buildImageUrl(item.id.toString(), "Primary")
                    val thumbImageUrl = buildImageUrl(item.id.toString(), "Thumb") // Fallback
                    val backdropImageUrl = buildLargeImageUrl(item.id.toString(), "Backdrop")
                    
                    println("DEBUG: Library '${item.name}' - Primary: $primaryImageUrl, Thumb: $thumbImageUrl, Backdrop: $backdropImageUrl")
                      MediaItem(
                        id = item.id.toString(),
                        name = item.name ?: "Unknown",
                        overview = item.overview,
                        imageUrl = primaryImageUrl ?: thumbImageUrl, // Try Primary first, then Thumb
                        backdropUrl = backdropImageUrl ?: primaryImageUrl ?: thumbImageUrl, // Prefer backdrop, fall back to primary or thumb
                        type = item.type ?: BaseItemKind.FOLDER,
                        runTimeTicks = item.runTimeTicks,
                        userData = item.userData?.let { userData ->
                            UserData(
                                played = userData.played == true,
                                playbackPositionTicks = userData.playbackPositionTicks,
                                playCount = userData.playCount
                            )
                        },
                        productionYear = item.productionYear,
                        communityRating = item.communityRating?.toDouble(),
                        collectionType = item.collectionType?.toString() // Add collection type for library detection
                    )
                } ?: emptyList()
                
                _mediaLibraries.value = libraries
            }
        } catch (e: Exception) {
            println("Error loading libraries: ${e.message}")
            // Don't update connection state error for content loading failures
        }
    }
    
    suspend fun loadLibraryItems(libraryId: String) {
        try {
            apiClient?.let { client ->                val result = client.itemsApi.getItems(
                    parentId = UUID.fromString(libraryId),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, 
                        ItemFields.OVERVIEW
                    ),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    limit = 200
                )
                  val items = result.content.items?.map { item ->
                    MediaItem(
                        id = item.id.toString(),
                        name = item.name ?: "Unknown",
                        overview = item.overview,
                        imageUrl = buildImageUrl(item.id.toString(), "Primary"),
                        backdropUrl = buildLargeImageUrl(item.id.toString(), "Backdrop"),
                        type = item.type ?: BaseItemKind.FOLDER,
                        runTimeTicks = item.runTimeTicks,
                        userData = item.userData?.let { userData ->
                            UserData(
                                played = userData.played == true,
                                playbackPositionTicks = userData.playbackPositionTicks,
                                playCount = userData.playCount
                            )
                        },
                        productionYear = item.productionYear,
                        communityRating = item.communityRating?.toDouble(),
                        collectionType = null // Library items don't have collection type
                    )
                } ?: emptyList()
                
                _currentLibraryItems.value = items
            }
        } catch (e: Exception) {
            println("Error loading library items: ${e.message}")
            // Don't update connection state error for content loading failures
        }
    }
      private fun buildImageUrl(itemId: String, imageType: String): String? {
        return apiClient?.let { client ->
            val baseUrl = "${client.baseUrl}/Items/$itemId/Images/$imageType"
            val url = if (client.accessToken != null) {
                "$baseUrl?api_key=${client.accessToken}"
            } else {
                baseUrl
            }
            println("DEBUG buildImageUrl: $url")
            url
        }
    }
      private fun buildLibraryImageUrl(itemId: String): String? {
        return apiClient?.let { client ->
            // Try multiple image types for libraries
            val primaryUrl = if (client.accessToken != null) {
                "${client.baseUrl}/Items/$itemId/Images/Primary?api_key=${client.accessToken}"
            } else {
                "${client.baseUrl}/Items/$itemId/Images/Primary"
            }
            val thumbUrl = if (client.accessToken != null) {
                "${client.baseUrl}/Items/$itemId/Images/Thumb?api_key=${client.accessToken}"
            } else {
                "${client.baseUrl}/Items/$itemId/Images/Thumb"
            }
            val backdropUrl = if (client.accessToken != null) {
                "${client.baseUrl}/Items/$itemId/Images/Backdrop?api_key=${client.accessToken}"
            } else {
                "${client.baseUrl}/Items/$itemId/Images/Backdrop"
            }
            
            println("DEBUG buildLibraryImageUrl - Primary: $primaryUrl, Thumb: $thumbUrl, Backdrop: $backdropUrl")
            
            // Return Primary first, but we could add logic to check which actually exist
            primaryUrl
        }
    }
      private fun buildLargeImageUrl(itemId: String, imageType: String): String? {
        return apiClient?.let { client ->
            // Request larger image for background use - typically 1920x1080 or higher
            val baseUrl = "${client.baseUrl}/Items/$itemId/Images/$imageType"
            val url = if (client.accessToken != null) {
                "$baseUrl?maxWidth=1920&maxHeight=1080&quality=85&api_key=${client.accessToken}"
            } else {
                "$baseUrl?maxWidth=1920&maxHeight=1080&quality=85"
            }
            println("DEBUG buildLargeImageUrl: $url")
            url
        }
    }
      fun getStreamUrl(itemId: String): String? {
        return apiClient?.let { client ->
            if (client.accessToken != null) {
                "${client.baseUrl}/Videos/$itemId/stream?api_key=${client.accessToken}"
            } else {
                "${client.baseUrl}/Videos/$itemId/stream"
            }
        }
    }
      suspend fun loadFeaturedContent() {
        try {
            val client = apiClient ?: run {
                println("DEBUG: No API client available for loadFeaturedContent")
                return
            }
            
            println("DEBUG: Loading featured content...")
            println("DEBUG: Client accessToken: ${client.accessToken?.take(10)}...")
            
            val userViewsApi = client.userViewsApi
            val libraries = userViewsApi.getUserViews().content
            
            // Collect featured items from all libraries
            val allFeaturedItems = mutableListOf<MediaItem>()
            
            libraries?.items?.forEach { library ->
                try {
                    println("DEBUG: Loading featured items for library: ${library.name}")
                    val itemsApi = client.itemsApi
                    
                    // Get recently added items from this library (could be movies, shows, etc.)
                    val response = itemsApi.getItems(
                        parentId = library.id,
                        sortBy = setOf(ItemSortBy.DATE_CREATED),
                        sortOrder = setOf(SortOrder.DESCENDING),
                        fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, ItemFields.OVERVIEW),
                        limit = 5 // Get top 5 from each library
                    )
                    
                    val items = response.content.items?.mapNotNull { movie ->
                        // Only include items that have overview and backdrop for featured display
                        if (movie.overview?.isNotEmpty() == true) {                            MediaItem(
                                id = movie.id.toString(),
                                name = movie.name ?: "Unknown",
                                overview = movie.overview,
                                imageUrl = buildImageUrl(movie.id.toString(), "Primary"),
                                backdropUrl = buildLargeImageUrl(movie.id.toString(), "Backdrop"),
                                type = movie.type ?: BaseItemKind.FOLDER,
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
                                collectionType = null // Featured content items don't have collection type
                            )
                        } else null
                    } ?: emptyList()
                    
                    allFeaturedItems.addAll(items)
                    println("DEBUG: Added ${items.size} featured items from library: ${library.name}")
                } catch (e: ApiClientException) {
                    println("DEBUG: ApiClientException in featured content for library ${library.name}: HTTP ${e.message}")
                    if (e.message?.contains("401") == true) {
                        println("DEBUG: 401 error - authentication token may be invalid")
                    }
                } catch (e: Exception) {
                    println("DEBUG: Exception loading featured items for library ${library.name}: ${e.message}")
                    e.printStackTrace()
                }
            }            
            // Limit total featured items and update state
            _featuredItems.value = allFeaturedItems.take(10)
            
            // Set the first item as the main featured item
            if (allFeaturedItems.isNotEmpty()) {
                _featuredItem.value = allFeaturedItems.first()
            }
            
            println("DEBUG: Loaded ${allFeaturedItems.size} total featured items")
            
        } catch (e: ApiClientException) {
            println("DEBUG: ApiClientException in loadFeaturedContent: HTTP ${e.message}")
            if (e.message?.contains("401") == true) {
                println("DEBUG: 401 error - authentication token may be invalid for featured content")
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in loadFeaturedContent: ${e.message}")
            e.printStackTrace()
        }
    }
      suspend fun loadRecentlyAdded() {
        val client = apiClient ?: run {
            println("DEBUG: No API client available for loadRecentlyAdded")
            return
        }
        
        try {
            println("DEBUG: Loading recently added content...")
            println("DEBUG: Client accessToken: ${client.accessToken?.take(10)}...")
            
            val userViewsApi = client.userViewsApi
            val libraries = userViewsApi.getUserViews().content
            
            val recentlyAddedMap = mutableMapOf<String, List<MediaItem>>()
            
            libraries?.items?.forEach { library ->
                try {
                    println("DEBUG: Loading recently added for library: ${library.name}")
                    val itemsApi = client.itemsApi
                    val libraryName = library.name ?: "Unknown"
                    
                    // Check if this is a TV library by checking for "TV" or "Show" in the name
                    // Also check the library type/collection type
                    val isTvLibrary = libraryName.contains("TV", ignoreCase = true) || 
                                     libraryName.contains("Show", ignoreCase = true) ||
                                     libraryName.contains("Series", ignoreCase = true) ||
                                     library.collectionType?.toString() == "tvshows"
                    
                    println("DEBUG: Library: $libraryName, Type: ${library.collectionType}, isTvLibrary: $isTvLibrary")
                    
                    if (isTvLibrary) {
                        println("DEBUG: Loading episodes for TV library: $libraryName")
                        // For TV libraries, load recently added episodes
                        val response = itemsApi.getItems(
                            parentId = library.id,
                            includeItemTypes = setOf(BaseItemKind.EPISODE),
                            sortBy = setOf(ItemSortBy.DATE_CREATED),
                            sortOrder = setOf(SortOrder.DESCENDING),
                            fields = setOf(
                                ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, 
                                ItemFields.OVERVIEW
                            ),
                            limit = 10
                        )
                        
                        println("Episodes found: ${response.content.items?.size ?: 0}")
                        
                        val items = response.content.items?.mapNotNull { episode ->
                            // For episodes, we need to get the series information
                            val seriesId = episode.seriesId?.toString()
                            val seriesName = episode.seriesName
                            val seasonName = episode.seasonName
                            val episodeNumber = episode.indexNumber
                            
                            println("Episode: ${episode.name}, Series: $seriesName, Season: $seasonName, Ep: $episodeNumber")
                            
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
                              MediaItem(
                                id = episode.id.toString(),
                                name = episodeName,
                                overview = episode.overview,
                                imageUrl = seriesId?.let { buildImageUrl(it, "Primary") }, // Use series poster
                                backdropUrl = buildLargeImageUrl(episode.id.toString(), "Backdrop") 
                                    ?: seriesId?.let { buildLargeImageUrl(it, "Backdrop") }, // Episode or series backdrop
                                type = episode.type ?: BaseItemKind.EPISODE,
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
                                collectionType = null, // Episodes don't have collection type
                                // Episode-specific fields
                                episodeName = episodeName,
                                seriesName = seriesName,
                                seriesId = seriesId,
                                seriesPosterUrl = seriesId?.let { buildImageUrl(it, "Primary") }
                            )
                        } ?: emptyList()
                        
                        if (items.isNotEmpty()) {
                            recentlyAddedMap["Recently Added Episodes - $libraryName"] = items
                        }
                    } else {
                        // For non-TV libraries, use the existing logic
                        val response = itemsApi.getItems(
                            parentId = library.id,
                            sortBy = setOf(ItemSortBy.DATE_CREATED),
                            sortOrder = setOf(SortOrder.DESCENDING),
                            fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, ItemFields.OVERVIEW),
                            limit = 10
                        )
                          val items = response.content.items?.map { item ->
                            MediaItem(
                                id = item.id.toString(),
                                name = item.name ?: "Unknown",
                                overview = item.overview,
                                imageUrl = buildImageUrl(item.id.toString(), "Primary"),
                                backdropUrl = buildLargeImageUrl(item.id.toString(), "Backdrop"),
                                type = item.type ?: BaseItemKind.FOLDER,
                                runTimeTicks = item.runTimeTicks,
                                userData = item.userData?.let { userData ->
                                    UserData(
                                        played = userData.played == true,
                                        playbackPositionTicks = userData.playbackPositionTicks,
                                        playCount = userData.playCount
                                    )
                                },
                                productionYear = item.productionYear,
                                communityRating = item.communityRating?.toDouble(),
                                collectionType = null // Recently added items don't have collection type
                            )
                        } ?: emptyList()
                        
                        if (items.isNotEmpty()) {
                            recentlyAddedMap["Recently Added - $libraryName"] = items
                        }
                    }                } catch (e: ApiClientException) {
                    println("DEBUG: ApiClientException in recently added for library ${library.name}: HTTP ${e.message}")
                    if (e.message?.contains("401") == true) {
                        println("DEBUG: 401 error - authentication token may be invalid")
                    }
                } catch (e: Exception) {
                    println("DEBUG: Exception loading recently added for library ${library.name}: ${e.message}")
                    e.printStackTrace()
                }
            }            
            _recentlyAdded.value = recentlyAddedMap
            println("DEBUG: Loaded recently added content for ${recentlyAddedMap.size} libraries")
            
        } catch (e: ApiClientException) {
            println("DEBUG: ApiClientException in loadRecentlyAdded: HTTP ${e.message}")
            if (e.message?.contains("401") == true) {
                println("DEBUG: 401 error - authentication token may be invalid for recently added content")
            }
        } catch (e: Exception) {
            println("DEBUG: Exception in loadRecentlyAdded: ${e.message}")
            e.printStackTrace()
        }
    }
    
    suspend fun loadTvShows(libraryId: String) {
        val client = apiClient ?: run {
            println("DEBUG: No API client available for loadTvShows")
            return
        }
        
        try {
            println("DEBUG: Loading TV shows for library: $libraryId")
            val itemsApi = client.itemsApi
            
            val response = itemsApi.getItems(
                parentId = UUID.fromString(libraryId),
                includeItemTypes = setOf(BaseItemKind.SERIES),
                sortBy = setOf(ItemSortBy.SORT_NAME),
                sortOrder = setOf(SortOrder.ASCENDING),                fields = setOf(
                    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, 
                    ItemFields.OVERVIEW
                ),
                recursive = true
            )
              val tvShows = response.content.items?.map { series ->
                MediaItem(
                    id = series.id.toString(),
                    name = series.name ?: "Unknown Series",
                    overview = series.overview,
                    imageUrl = buildImageUrl(series.id.toString(), "Primary"),
                    backdropUrl = buildLargeImageUrl(series.id.toString(), "Backdrop"),
                    type = series.type ?: BaseItemKind.SERIES,
                    runTimeTicks = series.runTimeTicks,
                    userData = series.userData?.let { userData ->
                        UserData(
                            played = userData.played == true,
                            playbackPositionTicks = userData.playbackPositionTicks,
                            playCount = userData.playCount
                        )
                    },
                    productionYear = series.productionYear,
                    communityRating = series.communityRating?.toDouble(),
                    collectionType = null // TV show items don't have collection type
                )
            } ?: emptyList()
            
            _tvShows.value = tvShows
            println("DEBUG: Loaded ${tvShows.size} TV shows")
            
        } catch (e: ApiClientException) {
            println("DEBUG: ApiClientException in loadTvShows: HTTP ${e.message}")
        } catch (e: Exception) {
            println("DEBUG: Exception in loadTvShows: ${e.message}")
            e.printStackTrace()
        }
    }
    
    suspend fun loadTvSeasons(seriesId: String) {
        val client = apiClient ?: run {
            println("DEBUG: No API client available for loadTvSeasons")
            return
        }
        
        try {
            println("DEBUG: Loading seasons for series: $seriesId")
            
            // First get the series details
            val itemsApi = client.itemsApi
            val seriesResponse = itemsApi.getItems(
                ids = setOf(UUID.fromString(seriesId)),
                fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)            )
            
            val series = seriesResponse.content.items?.firstOrNull()
            if (series != null) {
                _currentSeries.value = MediaItem(
                    id = series.id.toString(),
                    name = series.name ?: "Unknown Series",
                    overview = series.overview,
                    imageUrl = buildImageUrl(series.id.toString(), "Primary"),
                    backdropUrl = buildLargeImageUrl(series.id.toString(), "Backdrop"),
                    type = series.type ?: BaseItemKind.SERIES,
                    runTimeTicks = series.runTimeTicks,
                    userData = series.userData?.let { userData ->
                        UserData(
                            played = userData.played == true,
                            playbackPositionTicks = userData.playbackPositionTicks,
                            playCount = userData.playCount
                        )
                    },
                    productionYear = series.productionYear,
                    communityRating = series.communityRating?.toDouble(),
                    collectionType = null // Series items don't have collection type
                )
            }
            
            // Then get the seasons
            val seasonsResponse = itemsApi.getItems(
                parentId = UUID.fromString(seriesId),
                includeItemTypes = setOf(BaseItemKind.SEASON),
                sortBy = setOf(ItemSortBy.SORT_NAME),
                sortOrder = setOf(SortOrder.ASCENDING),
                fields = setOf(
                    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                    ItemFields.OVERVIEW,
                    ItemFields.CHILD_COUNT
                )
            )
              val seasons = seasonsResponse.content.items?.map { season ->
                MediaItem(
                    id = season.id.toString(),
                    name = season.name ?: "Unknown Season",
                    overview = season.overview,
                    imageUrl = buildImageUrl(season.id.toString(), "Primary") 
                        ?: buildImageUrl(seriesId, "Primary"), // Fallback to series poster
                    backdropUrl = buildLargeImageUrl(season.id.toString(), "Backdrop")
                        ?: buildLargeImageUrl(seriesId, "Backdrop"), // Fallback to series backdrop
                    type = season.type ?: BaseItemKind.SEASON,
                    runTimeTicks = season.runTimeTicks,
                    userData = season.userData?.let { userData ->
                        UserData(
                            played = userData.played == true,
                            playbackPositionTicks = userData.playbackPositionTicks,
                            playCount = userData.playCount
                        )
                    },
                    collectionType = null, // Season items don't have collection type
                    seriesId = seriesId,
                    seriesName = series?.name
                )
            } ?: emptyList()
            
            _tvSeasons.value = seasons
            println("DEBUG: Loaded ${seasons.size} seasons for series: ${series?.name}")
            
        } catch (e: ApiClientException) {
            println("DEBUG: ApiClientException in loadTvSeasons: HTTP ${e.message}")
        } catch (e: Exception) {
            println("DEBUG: Exception in loadTvSeasons: ${e.message}")
            e.printStackTrace()
        }
    }
    
    suspend fun loadTvEpisodes(seriesId: String, seasonId: String) {
        val client = apiClient ?: run {
            println("DEBUG: No API client available for loadTvEpisodes")
            return
        }
        
        try {
            println("DEBUG: Loading episodes for season: $seasonId")
            val itemsApi = client.itemsApi
            
            // Get season details
            val seasonResponse = itemsApi.getItems(
                ids = setOf(UUID.fromString(seasonId)),
                fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)            )
            
            val season = seasonResponse.content.items?.firstOrNull()
            if (season != null) {
                _currentSeason.value = MediaItem(
                    id = season.id.toString(),
                    name = season.name ?: "Unknown Season",
                    overview = season.overview,
                    imageUrl = buildImageUrl(season.id.toString(), "Primary")
                        ?: buildImageUrl(seriesId, "Primary"),
                    backdropUrl = buildLargeImageUrl(season.id.toString(), "Backdrop")
                        ?: buildLargeImageUrl(seriesId, "Backdrop"),
                    type = season.type ?: BaseItemKind.SEASON,
                    collectionType = null, // Season items don't have collection type
                    seriesId = seriesId,
                    seriesName = _currentSeries.value?.name
                )
            }
            
            // Get episodes
            val episodesResponse = itemsApi.getItems(
                parentId = UUID.fromString(seasonId),
                includeItemTypes = setOf(BaseItemKind.EPISODE),
                sortBy = setOf(ItemSortBy.SORT_NAME),
                sortOrder = setOf(SortOrder.ASCENDING),                fields = setOf(
                    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                    ItemFields.OVERVIEW
                )
            )
            
            val episodes = episodesResponse.content.items?.map { episode ->
                val episodeNumber = episode.indexNumber
                val episodeName = buildString {
                    if (episodeNumber != null) {
                        append("$episodeNumber. ")
                    }
                    append(episode.name ?: "Episode")
                }
                  MediaItem(
                    id = episode.id.toString(),
                    name = episodeName,
                    overview = episode.overview,
                    imageUrl = buildImageUrl(episode.id.toString(), "Primary")
                        ?: buildImageUrl(seriesId, "Primary"), // Fallback to series poster
                    backdropUrl = buildLargeImageUrl(episode.id.toString(), "Backdrop")
                        ?: buildLargeImageUrl(seriesId, "Backdrop"), // Fallback to series backdrop
                    type = episode.type ?: BaseItemKind.EPISODE,
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
                    collectionType = null, // Episode items don't have collection type
                    episodeName = episode.name,
                    seriesName = _currentSeries.value?.name,
                    seriesId = seriesId,
                    seriesPosterUrl = buildImageUrl(seriesId, "Primary")
                )
            } ?: emptyList()
            
            _tvEpisodes.value = episodes
            println("DEBUG: Loaded ${episodes.size} episodes for season: ${season?.name}")
            
        } catch (e: ApiClientException) {
            println("DEBUG: ApiClientException in loadTvEpisodes: HTTP ${e.message}")
        } catch (e: Exception) {
            println("DEBUG: Exception in loadTvEpisodes: ${e.message}")
            e.printStackTrace()
        }
    }
}
