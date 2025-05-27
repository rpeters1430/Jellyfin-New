package com.example.jellyfinnew.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import org.jellyfin.sdk.createJellyfin
import java.util.UUID
import org.jellyfin.sdk.api.client.ApiClient
import org.jellyfin.sdk.api.client.extensions.authenticateUserByName
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.exception.ApiClientException
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import org.jellyfin.sdk.model.api.ItemFields
import org.jellyfin.sdk.model.api.ItemSortBy
import org.jellyfin.sdk.model.api.SortOrder
import javax.net.ssl.SSLException
import java.security.cert.CertificateException
import java.net.ConnectException
import java.net.UnknownHostException

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
    val collectionType: String? = null,
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

class JellyfinRepository(androidContext: Context) {

    companion object {
        private const val TAG = "JellyfinRepository"
        private const val CLIENT_NAME = "Jellyfin Android TV"
        private const val CLIENT_VERSION = "1.0.0"
    }

    private val jellyfin = createJellyfin {
        clientInfo = ClientInfo(CLIENT_NAME, CLIENT_VERSION)
        context = androidContext
    }

    private var apiClient: ApiClient? = null
    private var imageUrlHelper: ImageUrlHelper? = null

    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

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

    // TV Shows specific
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
            Log.d(TAG, "Attempting to connect to $serverUrl with username: $username")
            updateConnectionState(isLoading = true, error = null)

            val client = jellyfin.createApi(baseUrl = serverUrl)

            Log.d(TAG, "Authenticating user...")
            val authResult = client.userApi.authenticateUserByName(
                username = username,
                password = password
            )

            authResult.content.accessToken?.let { token ->
                Log.d(TAG, "Authentication successful")

                val user = authResult.content.user
                Log.d(TAG, "User ID: ${user?.id}, User Name: ${user?.name}")

                val authenticatedClient = jellyfin.createApi(
                    baseUrl = serverUrl,
                    accessToken = token
                )

                apiClient = authenticatedClient
                imageUrlHelper = ImageUrlHelper(authenticatedClient)

                updateConnectionState(isConnected = true, isLoading = false)

                Log.d(TAG, "Loading initial data...")
                loadInitialData()

                true
            } ?: run {
                Log.w(TAG, "Authentication failed - no access token received")
                updateConnectionState(error = "Authentication failed - no access token received")
                false
            }        } catch (e: ApiClientException) {
            val errorMessage = when {
                                e.message?.contains("401") == true -> "Invalid username or password"
                e.message?.contains("404") == true -> "Server not found - check URL"
                e.message?.contains("timeout") == true -> "Connection timeout - check network"
                e.message?.contains("SSL") == true || e.message?.contains("certificate") == true -> 
                    "SSL certificate error - For self-signed certificates behind reverse proxy: " +
                    "Install certificate in Android settings or use HTTP connection"
                e.message?.contains("Connection refused") == true -> "Connection refused - check server and port"
                e.message?.contains("UnknownHostException") == true -> "Cannot resolve hostname - check URL"
                else -> "Connection failed: ${e.message}"
            }
            Log.e(TAG, "API Connection error", e)
            updateConnectionState(error = errorMessage)
            false
        } catch (e: javax.net.ssl.SSLException) {
            Log.e(TAG, "SSL error occurred", e)
            val errorMessage = when {
                e.message?.contains("certificate", ignoreCase = true) == true -> 
                    "SSL certificate error - For self-signed certificates behind reverse proxy: " +
                    "1) Install the certificate in Android's trusted certificates, or " +
                    "2) Use HTTP instead of HTTPS, or " +
                    "3) Configure your reverse proxy to use a valid certificate"
                e.message?.contains("handshake", ignoreCase = true) == true ->
                    "SSL handshake failed - Check certificate configuration on your reverse proxy"
                e.message?.contains("hostname", ignoreCase = true) == true ->
                    "SSL hostname verification failed - Ensure certificate matches domain name"
                else -> "SSL connection error - check server certificate configuration"
            }
            updateConnectionState(error = errorMessage)
            false
        } catch (e: java.security.cert.CertificateException) {
            Log.e(TAG, "Certificate error occurred", e)
            updateConnectionState(error = "Certificate validation failed - For self-signed certificates: Install the certificate in Android's trusted store or use HTTP connection")
            false
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "Connection refused", e)
            updateConnectionState(error = "Connection refused - check server URL and port")
            false
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Unknown host", e)
            updateConnectionState(error = "Cannot resolve hostname - check server URL")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected connection error", e)
            updateConnectionState(error = "Unexpected error: ${e.message}")
            false
        }
    }

    private suspend fun loadInitialData() {
        try {
            // Load data sequentially to avoid overwhelming the server
            safeApiCall(
                operation = { loadMediaLibraries() },
                errorMessage = "Failed to load libraries"
            )

            // Add small delay to prevent rapid cancellation
            kotlinx.coroutines.delay(100)

            safeApiCall(
                operation = { loadFeaturedContent() },
                errorMessage = "Failed to load featured content"
            )

            // Add small delay to prevent rapid cancellation
            kotlinx.coroutines.delay(100)

            safeApiCall(
                operation = { loadRecentlyAdded() },
                errorMessage = "Failed to load recently added content"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error loading initial data", e)
        }
    }

    private fun updateConnectionState(
        isConnected: Boolean = _connectionState.value.isConnected,
        isLoading: Boolean = _connectionState.value.isLoading,
        error: String? = _connectionState.value.error
    ) {
        _connectionState.value = ConnectionState(isConnected, isLoading, error)
    }

    suspend fun loadMediaLibraries() {
        val client = apiClient ?: return
        val helper = imageUrlHelper ?: return

        try {
            Log.d(TAG, "Loading media libraries...")
            val result = client.userViewsApi.getUserViews()
            Log.d(TAG, "Got ${result.content.items?.size ?: 0} libraries")

            val libraries = result.content.items?.mapNotNull { item ->
                createMediaItemFromDto(item, helper)
            } ?: emptyList()

            _mediaLibraries.value = libraries
            Log.d(TAG, "Loaded ${libraries.size} libraries")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading libraries", e)
        }
    }

    suspend fun loadLibraryItems(libraryId: String) {
        val client = apiClient ?: return
        val helper = imageUrlHelper ?: return

        try {
            Log.d(TAG, "Loading library items for: $libraryId")
            val result = client.itemsApi.getItems(
                parentId = UUID.fromString(libraryId),
                fields = setOf(
                    ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                    ItemFields.OVERVIEW
                ),
                sortBy = setOf(ItemSortBy.SORT_NAME),
                sortOrder = setOf(SortOrder.ASCENDING),
                limit = 200
            )

            val items = result.content.items?.mapNotNull { item ->
                createMediaItemFromDto(item, helper)
            } ?: emptyList()

            _currentLibraryItems.value = items
            Log.d(TAG, "Loaded ${items.size} library items")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading library items", e)
        }
    }

    suspend fun loadFeaturedContent() {
        val client = apiClient ?: return
        val helper = imageUrlHelper ?: return

        try {
            Log.d(TAG, "Loading featured content...")

            val userViewsApi = client.userViewsApi
            val libraries = userViewsApi.getUserViews().content

            val allFeaturedItems = mutableListOf<MediaItem>()

            libraries?.items?.forEach { library ->
                try {
                    Log.d(TAG, "Loading featured items for library: ${library.name}")
                    val itemsApi = client.itemsApi

                    val response = itemsApi.getItems(
                        parentId = library.id,
                        sortBy = setOf(ItemSortBy.DATE_CREATED),
                        sortOrder = setOf(SortOrder.DESCENDING),
                        fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, ItemFields.OVERVIEW),
                        limit = 5
                    )

                    val items = response.content.items?.mapNotNull { movie ->
                        if (movie.overview?.isNotEmpty() == true) {
                            createMediaItemFromDto(movie, helper)
                        } else null
                    } ?: emptyList()

                    allFeaturedItems.addAll(items)
                    Log.d(TAG, "Added ${items.size} featured items from library: ${library.name}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error loading featured items for library ${library.name}", e)
                }
            }

            _featuredItems.value = allFeaturedItems.take(10)

            if (allFeaturedItems.isNotEmpty()) {
                _featuredItem.value = allFeaturedItems.first()
            }

            Log.d(TAG, "Loaded ${allFeaturedItems.size} total featured items")

        } catch (e: Exception) {
            Log.e(TAG, "Error loading featured content", e)
        }
    }

    suspend fun loadRecentlyAdded() {
        val client = apiClient ?: return
        val helper = imageUrlHelper ?: return

        try {
            Log.d(TAG, "Loading recently added content...")

            val userViewsApi = client.userViewsApi
            val libraries = userViewsApi.getUserViews().content

            val recentlyAddedMap = mutableMapOf<String, List<MediaItem>>()

            libraries?.items?.forEach { library ->
                try {
                    Log.d(TAG, "Loading recently added for library: ${library.name}")
                    val itemsApi = client.itemsApi
                    val libraryName = library.name ?: "Unknown"

                    val isTvLibrary = libraryName.contains("TV", ignoreCase = true) ||
                            libraryName.contains("Show", ignoreCase = true) ||
                            libraryName.contains("Series", ignoreCase = true) ||
                            library.collectionType?.toString() == "tvshows"

                    Log.d(TAG, "Library: $libraryName, Type: ${library.collectionType}, isTvLibrary: $isTvLibrary")

                    if (isTvLibrary) {
                        Log.d(TAG, "Loading recently added episodes for TV library: $libraryName")
                        val response = itemsApi.getItems(
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
                            limit = 10,
                            recursive = true // This ensures we search all subdirectories
                        )

                        Log.d(TAG, "Found ${response.content.items?.size ?: 0} episodes for $libraryName")

                        val items = response.content.items?.mapNotNull { episode ->
                            createEpisodeMediaItem(episode, helper)
                        } ?: emptyList()

                        Log.d(TAG, "Processed ${items.size} episode items for $libraryName")

                        if (items.isNotEmpty()) {
                            recentlyAddedMap["Recently Added Episodes - $libraryName"] = items
                            Log.d(TAG, "Added recently added episodes section: Recently Added Episodes - $libraryName")
                        } else {
                            Log.d(TAG, "No recent episodes found for $libraryName - checking if episodes exist at all")
                            // Try to get any episodes to see if the library has episodes
                            val anyEpisodesResponse = itemsApi.getItems(
                                parentId = library.id,
                                includeItemTypes = setOf(BaseItemKind.EPISODE),
                                limit = 5,
                                recursive = true
                            )
                            Log.d(TAG, "Total episodes in $libraryName: ${anyEpisodesResponse.content.items?.size ?: 0}")
                        }
                    } else {
                        val response = itemsApi.getItems(
                            parentId = library.id,
                            sortBy = setOf(ItemSortBy.DATE_CREATED),
                            sortOrder = setOf(SortOrder.DESCENDING),
                            fields = setOf(ItemFields.PRIMARY_IMAGE_ASPECT_RATIO, ItemFields.OVERVIEW),
                            limit = 10
                        )

                        val items = response.content.items?.mapNotNull { item ->
                            createMediaItemFromDto(item, helper)
                        } ?: emptyList()

                        if (items.isNotEmpty()) {
                            recentlyAddedMap["Recently Added - $libraryName"] = items
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error loading recently added for library ${library.name}", e)
                }
            }

            _recentlyAdded.value = recentlyAddedMap
            Log.d(TAG, "Loaded recently added content for ${recentlyAddedMap.size} libraries:")
            recentlyAddedMap.forEach { (section, items) ->
                Log.d(TAG, "  - $section: ${items.size} items")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error loading recently added content", e)
        }
    }

    suspend fun loadTvShows(libraryId: String) {
        safeApiCall(
            operation = {
                val client = requireNotNull(apiClient)
                val helper = requireNotNull(imageUrlHelper)

                Log.d(TAG, "Loading TV shows for library: $libraryId")
                val response = client.itemsApi.getItems(
                    parentId = UUID.fromString(libraryId),
                    includeItemTypes = setOf(BaseItemKind.SERIES),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW
                    ),
                    recursive = true
                )

                response.content.items?.mapNotNull { series ->
                    createMediaItemFromDto(series, helper)
                } ?: emptyList()
            },
            onSuccess = { tvShows ->
                _tvShows.value = tvShows
                Log.d(TAG, "Loaded ${tvShows.size} TV shows")
            },
            errorMessage = "Failed to load TV shows"
        )
    }

    suspend fun loadTvSeasons(seriesId: String) {
        safeApiCall(
            operation = {
                val client = requireNotNull(apiClient)
                val helper = requireNotNull(imageUrlHelper)

                Log.d(TAG, "Loading seasons for series: $seriesId")

                // Get series details
                val seriesResponse = client.itemsApi.getItems(
                    ids = setOf(UUID.fromString(seriesId)),
                    fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                )

                val series = seriesResponse.content.items?.firstOrNull()
                val currentSeries = series?.let { createMediaItemFromDto(it, helper) }

                // Get seasons
                val seasonsResponse = client.itemsApi.getItems(
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

                val seasons = seasonsResponse.content.items?.mapNotNull { season ->
                    createSeasonMediaItem(season, helper, seriesId, series?.name)
                } ?: emptyList()

                Pair(currentSeries, seasons)
            },
            onSuccess = { (currentSeries, seasons) ->
                _currentSeries.value = currentSeries
                _tvSeasons.value = seasons
                Log.d(TAG, "Loaded ${seasons.size} seasons for series: ${currentSeries?.name}")
            },
            errorMessage = "Failed to load TV seasons"
        )
    }

    suspend fun loadTvEpisodes(seriesId: String, seasonId: String) {
        safeApiCall(
            operation = {
                val client = requireNotNull(apiClient)
                val helper = requireNotNull(imageUrlHelper)

                Log.d(TAG, "Loading episodes for season: $seasonId")

                // Get season details
                val seasonResponse = client.itemsApi.getItems(
                    ids = setOf(UUID.fromString(seasonId)),
                    fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                )

                val season = seasonResponse.content.items?.firstOrNull()
                val currentSeason = season?.let {
                    createSeasonMediaItem(it, helper, seriesId, _currentSeries.value?.name)
                }

                // Get episodes
                val episodesResponse = client.itemsApi.getItems(
                    parentId = UUID.fromString(seasonId),
                    includeItemTypes = setOf(BaseItemKind.EPISODE),
                    sortBy = setOf(ItemSortBy.SORT_NAME),
                    sortOrder = setOf(SortOrder.ASCENDING),
                    fields = setOf(
                        ItemFields.PRIMARY_IMAGE_ASPECT_RATIO,
                        ItemFields.OVERVIEW
                    )
                )

                val episodes = episodesResponse.content.items?.mapNotNull { episode ->
                    createEpisodeMediaItem(episode, helper, seriesId)
                } ?: emptyList()

                Pair(currentSeason, episodes)
            },
            onSuccess = { (currentSeason, episodes) ->
                _currentSeason.value = currentSeason
                _tvEpisodes.value = episodes
                Log.d(TAG, "Loaded ${episodes.size} episodes for season: ${currentSeason?.name}")
            },
            errorMessage = "Failed to load TV episodes"
        )
    }

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

    private fun createEpisodeMediaItem(
        episode: BaseItemDto,
        imageHelper: ImageUrlHelper,
        seriesId: String? = null
    ): MediaItem? {
        return try {
            val episodeId = episode.id.toString()
            val actualSeriesId = seriesId ?: episode.seriesId?.toString()
            val seriesName = episode.seriesName
            val seasonName = episode.seasonName
            val episodeNumber = episode.indexNumber

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

            val (episodeImage, episodeBackdrop) = imageHelper.buildMediaImageUrls(episodeId)
            val seriesPosterUrl = actualSeriesId?.let { imageHelper.buildPosterUrl(it) }
            val seriesBackdropUrl = actualSeriesId?.let { imageHelper.buildBackdropUrl(it) }

            MediaItem(
                id = episodeId,
                name = episodeName,
                overview = episode.overview,
                imageUrl = episodeImage ?: seriesPosterUrl,
                backdropUrl = episodeBackdrop ?: seriesBackdropUrl,
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
                collectionType = null,
                episodeName = episodeName,
                seriesName = seriesName,
                seriesId = actualSeriesId,
                seriesPosterUrl = seriesPosterUrl
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create episode MediaItem: ${episode.name}", e)
            null
        }
    }

    private fun createSeasonMediaItem(
        season: BaseItemDto,
        imageHelper: ImageUrlHelper,
        seriesId: String,
        seriesName: String?
    ): MediaItem? {
        return try {
            val seasonId = season.id.toString()
            val (seasonImage, seasonBackdrop) = imageHelper.buildMediaImageUrls(seasonId)
            val seriesPosterUrl = imageHelper.buildPosterUrl(seriesId)
            val seriesBackdropUrl = imageHelper.buildBackdropUrl(seriesId)

            MediaItem(
                id = seasonId,
                name = season.name ?: "Unknown Season",
                overview = season.overview,
                imageUrl = seasonImage ?: seriesPosterUrl,
                backdropUrl = seasonBackdrop ?: seriesBackdropUrl,
                type = season.type ?: BaseItemKind.SEASON,
                runTimeTicks = season.runTimeTicks,
                userData = season.userData?.let { userData ->
                    UserData(
                        played = userData.played == true,
                        playbackPositionTicks = userData.playbackPositionTicks,
                        playCount = userData.playCount
                    )
                },
                collectionType = null,
                seriesId = seriesId,
                seriesName = seriesName
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create season MediaItem: ${season.name}", e)
            null
        }
    }

    fun getStreamUrl(itemId: String): String? {
        return imageUrlHelper?.buildStreamUrl(itemId)
    }

    private suspend fun <T> safeApiCall(
        operation: suspend () -> T,
        onSuccess: suspend (T) -> Unit = {},
        errorMessage: String = "API call failed"
    ) {
        try {
            val result = operation()
            onSuccess(result)
        } catch (e: ApiClientException) {
            Log.e(TAG, "$errorMessage: API error", e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Expected when navigation cancels operations - don't log as error
            Log.d(TAG, "$errorMessage: Operation cancelled (navigation)")
        } catch (e: Exception) {
            Log.e(TAG, "$errorMessage: Unexpected error", e)
        }
    }

    fun disconnect() {
        apiClient = null
        imageUrlHelper = null
        updateConnectionState(isConnected = false, isLoading = false, error = null)
        clearAllData()
    }

    private fun clearAllData() {
        _mediaLibraries.value = emptyList()
        _currentLibraryItems.value = emptyList()
        _featuredItem.value = null
        _featuredItems.value = emptyList()
        _recentlyAdded.value = emptyMap()
        _tvShows.value = emptyList()
        _tvSeasons.value = emptyList()
        _tvEpisodes.value = emptyList()
        _currentSeries.value = null
        _currentSeason.value = null
    }

    /**
     * Enhanced connect method that handles self-signed certificates and reverse proxy scenarios
     */
    suspend fun connectWithSSLFallback(
        serverUrl: String,
        username: String,
        password: String
    ): Boolean {
        // First try the original URL
        val result = connect(serverUrl, username, password)
        
        if (!result && serverUrl.startsWith("https://")) {
            Log.i(TAG, "HTTPS connection failed, this may be due to self-signed certificate")
            Log.i(TAG, "Consider installing the certificate or configuring your reverse proxy with a valid certificate")
            
            // Note: We don't automatically fall back to HTTP for security reasons
            // Users should explicitly use HTTP if they want an insecure connection
        }
        
        return result
    }
}