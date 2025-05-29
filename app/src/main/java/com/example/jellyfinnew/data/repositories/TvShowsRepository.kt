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
 * Repository responsible for managing TV shows, seasons, and episodes
 */
class TvShowsRepository {
    
    companion object {
        private val TAG = JellyfinConfig.Logging.getTag("TvShows")
    }
    
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
    
    /**
     * Load TV shows from a specific library
     */
    suspend fun loadTvShows(libraryId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading TV shows for library: $libraryId")

                val response = apiClient.itemsApi.getItems(
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
                    createMediaItemFromDto(series, imageUrlHelper)
                } ?: emptyList()
            },
            onSuccess = { tvShows ->
                _tvShows.value = tvShows
                Log.d(TAG, "Loaded ${tvShows.size} TV shows")
            },
            errorMessage = "Failed to load TV shows"
        )
    }

    /**
     * Load seasons for a specific TV series
     */
    suspend fun loadTvSeasons(seriesId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading seasons for series: $seriesId")

                // Get series details
                val seriesResponse = apiClient.itemsApi.getItems(
                    ids = setOf(UUID.fromString(seriesId)),
                    fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                )

                val series = seriesResponse.content.items?.firstOrNull()
                val currentSeries = series?.let { createMediaItemFromDto(it, imageUrlHelper) }

                // Get seasons
                val seasonsResponse = apiClient.itemsApi.getItems(
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
                    createSeasonMediaItem(season, imageUrlHelper, seriesId, series?.name)
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

    /**
     * Load episodes for a specific season
     */
    suspend fun loadTvEpisodes(seriesId: String, seasonId: String, apiClient: ApiClient, imageUrlHelper: ImageUrlHelper) {
        safeApiCall(
            operation = {
                Log.d(TAG, "Loading episodes for season: $seasonId")

                // Get season details
                val seasonResponse = apiClient.itemsApi.getItems(
                    ids = setOf(UUID.fromString(seasonId)),
                    fields = setOf(ItemFields.OVERVIEW, ItemFields.PRIMARY_IMAGE_ASPECT_RATIO)
                )

                val season = seasonResponse.content.items?.firstOrNull()
                val currentSeason = season?.let {
                    createSeasonMediaItem(it, imageUrlHelper, seriesId, _currentSeries.value?.name)
                }

                // Get episodes
                val episodesResponse = apiClient.itemsApi.getItems(
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
                    createEpisodeMediaItem(episode, imageUrlHelper, seriesId)
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
    
    /**
     * Clear all TV shows data
     */
    fun clearAllData() {
        _tvShows.value = emptyList()
        _tvSeasons.value = emptyList()
        _tvEpisodes.value = emptyList()
        _currentSeries.value = null
        _currentSeason.value = null
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

    private fun createSeasonMediaItem(
        season: BaseItemDto,
        imageHelper: ImageUrlHelper,
        seriesId: String,
        seriesName: String?
    ): MediaItem? {
        return try {
            val seasonId = season.id.toString()
            val seasonImageUrl = imageHelper.buildPosterUrl(seasonId)
            val backdropUrl = imageHelper.buildBackdropUrl(seasonId)

            MediaItem(
                id = seasonId,
                name = season.name ?: "Season",
                overview = season.overview,
                imageUrl = seasonImageUrl,
                backdropUrl = backdropUrl,
                type = season.type ?: BaseItemKind.SEASON,
                runTimeTicks = null,
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

            // Use episode image if available, otherwise fall back to series poster
            val episodeImage = imageHelper.buildThumbUrl(episodeId)
            val episodeBackdrop = imageHelper.buildBackdropUrl(episodeId)
            val seriesPosterUrl = actualSeriesId?.let { imageHelper.buildPosterUrl(it) }

            MediaItem(
                id = episodeId,
                name = episodeName,
                overview = episode.overview,
                imageUrl = seriesPosterUrl ?: episodeImage, // Prioritize series poster for vertical card
                backdropUrl = episodeBackdrop ?: seriesPosterUrl, // Keep original backdrop logic or use another suitable landscape image
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
                seriesId = actualSeriesId,
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
    ) {        try {
            val result = operation()
            onSuccess(result)
        } catch (e: Exception) {
            val error = ErrorHandler.handleException(e, errorMessage)
            Log.e(TAG, error.getUserFriendlyMessage(), e)
        }
    }
}
