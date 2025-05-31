package com.example.jellyfinnew.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.ui.home.components.*
import com.example.jellyfinnew.ui.utils.TvOptimizations
import com.example.jellyfinnew.ui.utils.TvSpacing
import com.example.jellyfinnew.ui.utils.TvListDefaults
import com.example.jellyfinnew.ui.components.UnifiedMediaCard
import com.example.jellyfinnew.ui.components.MediaCardType
import org.jellyfin.sdk.model.api.BaseItemKind

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayMedia: (String) -> Unit,
    onDisconnect: () -> Unit,
    onFocusChange: (String?) -> Unit = {},
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
            debounceMs = 300L
        )

        // Handle connection state with proper composition stability
        when {
            connectionState.isLoading -> {
                LoadingState(message = "Connecting to Jellyfin...")
            }
            connectionState.error != null -> {
                ErrorState(
                    error = connectionState.error!!,
                    onRetry = { viewModel.refreshHomeContent() }
                )
            }
            selectedLibraryId == null -> {
                // Main home view with featured content and library rows
                MainHomeContent(
                    mediaLibraries = mediaLibraries,
                    featuredItems = featuredItems,
                    recentlyAdded = recentlyAdded,
                    onPlayMedia = onPlayMedia,
                    onLibraryClick = { library ->
                        selectedLibraryId = library.id
                        viewModel.loadLibraryItems(library.id)
                    },
                    onFocusChange = { imageUrl ->
                        focusedItemImageUrl = imageUrl
                        onFocusChange(imageUrl)
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
                            BaseItemKind.MOVIE, BaseItemKind.EPISODE, BaseItemKind.AUDIO -> onPlayMedia(item.id)
                            BaseItemKind.FOLDER, BaseItemKind.COLLECTION_FOLDER -> {
                                selectedLibraryId = item.id
                                viewModel.loadLibraryItems(item.id)
                            }
                            else -> {
                                onPlayMedia(item.id)
                            }
                        }
                    },
                    onFocusChange = { imageUrl ->
                        focusedItemImageUrl = imageUrl
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
    onLibraryClick: (MediaItem) -> Unit,
    onFocusChange: (String?) -> Unit,
    onDisconnect: () -> Unit,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    // Filter empty sections and create stable list for LazyColumn
    val recentlyAddedSections = remember(recentlyAdded) {
        recentlyAdded.filter { it.value.isNotEmpty() }.toList()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(TvListDefaults.contentPadding),
        verticalArrangement = Arrangement.spacedBy(TvListDefaults.sectionSpacing)
    ) {
        // Header - always present with stable key
        item(key = "header") {
            HomeHeader(onDisconnect = onDisconnect)
        }

        // Featured Carousel - only if has content
        if (featuredItems.isNotEmpty()) {
            item(key = "featured") {
                Column(
                    modifier = Modifier.padding(top = TvSpacing.small),
                    verticalArrangement = Arrangement.spacedBy(TvSpacing.medium)
                ) {
                    EnhancedFeaturedCarousel(
                        featuredItems = featuredItems,
                        onPlayClick = onPlayMedia,
                        onFocus = onFocusChange
                    )
                }
            }
        }

        // Media Libraries Section - only if has content
        if (mediaLibraries.isNotEmpty()) {
            item(key = "libraries") {
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

        // Recently Added Sections with stable keys
        items(
            items = recentlyAddedSections,
            key = { (libraryName, _) -> "recently_added_${libraryName.hashCode()}" }
        ) { (libraryName, items) ->
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryContentView(
    selectedLibraryId: String,
    mediaLibraries: List<MediaItem>,
    currentLibraryItems: List<MediaItem>,
    onBackClick: () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onFocusChange: (String?) -> Unit,
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
        when {
            currentLibraryItems.isEmpty() -> {
                LoadingState(message = "Loading library items...")
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    gridItems(
                        items = currentLibraryItems,
                        key = { item -> "library_item_${item.id}" }
                    ) { item ->
                        LibraryItemCard(
                            mediaItem = item,
                            onClick = { onItemClick(item) },
                            onFocus = {
                                onFocusChange(item.backdropUrl ?: item.imageUrl)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryItemCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardType = when (mediaItem.type) {
        BaseItemKind.EPISODE -> MediaCardType.EPISODE
        BaseItemKind.FOLDER, BaseItemKind.COLLECTION_FOLDER -> MediaCardType.BACKDROP
        else -> MediaCardType.POSTER
    }

    UnifiedMediaCard(
        mediaItem = mediaItem,
        onClick = onClick,
        modifier = modifier,
        onFocus = onFocus,
        cardType = cardType,
        showProgress = true,
        showOverlay = true
    )
}
