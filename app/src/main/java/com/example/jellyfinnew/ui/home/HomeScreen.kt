package com.example.jellyfinnew.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.ui.home.components.*
import com.example.jellyfinnew.ui.utils.TvOptimizations
import com.example.jellyfinnew.ui.components.UnifiedMediaCard
import com.example.jellyfinnew.ui.components.MediaCardType
import org.jellyfin.sdk.model.api.BaseItemKind

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayMedia: (String) -> Unit,
    onNavigateToTvShows: (String) -> Unit,
    onNavigateToMovies: (String) -> Unit,
    onNavigateToMusic: (String) -> Unit,    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaLibraries by viewModel.mediaLibraries.collectAsStateWithLifecycle()
    val currentLibraryItems by viewModel.currentLibraryItems.collectAsStateWithLifecycle()
    val featuredItems by viewModel.featuredItems.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var selectedLibraryId by remember { mutableStateOf<String?>(null) }
    var focusedItemImageUrl by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // Optimized background image with debounced updates
        TvOptimizations.DebouncedBackgroundImage(
            imageUrl = focusedItemImageUrl,
            debounceMs = 300L // Faster response for TV
        )

        // Handle connection state
        when {
            connectionState.isLoading -> {
                LoadingState(message = "Connecting to Jellyfin...")
            }

            connectionState.error != null -> {
                ErrorState(
                    error = connectionState.error!!,
                    onRetry = {
                        viewModel.refreshHomeContent()
                    }
                )
            }            selectedLibraryId == null -> {
                // Main home view with featured content and library rows
                MainHomeContent(
                    mediaLibraries = mediaLibraries,
                    featuredItems = featuredItems,
                    recentlyAdded = recentlyAdded,
                    onPlayMedia = onPlayMedia,
                    onNavigateToTvShows = onNavigateToTvShows,
                    onNavigateToMovies = onNavigateToMovies,
                    onNavigateToMusic = onNavigateToMusic,
                    onLibraryClick = { library ->
                        when (library.collectionType) {
                            "tvshows" -> onNavigateToTvShows(library.id)
                            "movies" -> onNavigateToMovies(library.id)
                            "music" -> onNavigateToMusic(library.id)
                            else -> {
                                selectedLibraryId = library.id
                                viewModel.loadLibraryItems(library.id)
                            }
                        }
                    },
                    onFocusChange = { imageUrl ->
                        focusedItemImageUrl = imageUrl
                    },
                    onDisconnect = {
                        viewModel.disconnect()
                        onDisconnect()
                    },
                    viewModel = viewModel
                )
            }

            else -> {
                // Library content view
                LibraryContentView(
                    selectedLibraryId = selectedLibraryId!!,
                    mediaLibraries = mediaLibraries,
                    currentLibraryItems = currentLibraryItems,
                    onBackClick = { selectedLibraryId = null },
                    onItemClick = { item ->
                        when (item.type) {
                            BaseItemKind.MOVIE, BaseItemKind.EPISODE -> onPlayMedia(item.id)
                            else -> {
                                selectedLibraryId = item.id
                                viewModel.loadLibraryItems(item.id)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MainHomeContent(
    mediaLibraries: List<MediaItem>,
    featuredItems: List<MediaItem>,
    recentlyAdded: Map<String, List<MediaItem>>,
    onPlayMedia: (String) -> Unit,
    onNavigateToTvShows: (String) -> Unit,
    onNavigateToMovies: (String) -> Unit,
    onNavigateToMusic: (String) -> Unit,
    onLibraryClick: (MediaItem) -> Unit,
    onFocusChange: (String?) -> Unit,
    onDisconnect: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TvOptimizations.TvListDefaults.contentPadding),
        verticalArrangement = Arrangement.spacedBy(TvOptimizations.TvListDefaults.sectionSpacing)
    ) {
        // Header
        item {
            HomeHeader(onDisconnect = onDisconnect)
        }

        // Featured Carousel - closer to header
        if (featuredItems.isNotEmpty()) {            item {
                Column(
                    modifier = Modifier.padding(top = TvOptimizations.TvSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(TvOptimizations.TvSpacing.medium)
                ) {
                    EnhancedFeaturedCarousel(
                        featuredItems = featuredItems,
                        onPlayClick = onPlayMedia,
                        onFocus = onFocusChange
                    )
                }
            }
        }        // Media Libraries Section
        if (mediaLibraries.isNotEmpty()) {
            item {
                MediaLibrarySection(
                    libraries = mediaLibraries,
                    onLibraryClick = onLibraryClick,
                    onLibraryFocus = onFocusChange,
                    onLibraryIndexFocused = { index, items ->
                        viewModel.onItemFocused(index, items)
                    }
                )
            }
        }

        // Recently Added Sections
        recentlyAdded.forEach { (libraryName, items) ->
            if (items.isNotEmpty()) {
                item {
                    RecentlyAddedSection(
                        title = libraryName,
                        items = items,
                        onItemClick = onPlayMedia,
                        onItemFocus = onFocusChange,
                        onItemIndexFocused = { index, listItems ->
                            viewModel.onItemFocused(index, listItems)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryContentView(
    selectedLibraryId: String,
    mediaLibraries: List<MediaItem>,
    currentLibraryItems: List<MediaItem>,
    onBackClick: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button and library title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Button(onClick = onBackClick) {
                Text("← Back")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = mediaLibraries.find { it.id == selectedLibraryId }?.name ?: "Library",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Content
        if (currentLibraryItems.isEmpty()) {
            LoadingState(message = "Loading library items...")
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 180.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(currentLibraryItems) { item ->
                    LibraryItemCard(
                        mediaItem = item,
                        onClick = { onItemClick(item) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryItemCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use the new UnifiedMediaCard with optimized caching
    UnifiedMediaCard(
        mediaItem = mediaItem,
        onClick = onClick,
        modifier = modifier,
        cardType = when (mediaItem.type) {
            BaseItemKind.EPISODE -> MediaCardType.EPISODE
            else -> MediaCardType.POSTER
        },
        showProgress = true,
        showOverlay = true
    )
}