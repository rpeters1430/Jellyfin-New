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
import org.jellyfin.sdk.model.api.BaseItemKind

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayMedia: (String) -> Unit,
    onNavigateToTvShows: (String) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {    val mediaLibraries by viewModel.mediaLibraries.collectAsStateWithLifecycle()
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
            }

            selectedLibraryId == null -> {
                // Main home view with featured content and library rows
                MainHomeContent(
                    mediaLibraries = mediaLibraries,
                    featuredItems = featuredItems,
                    recentlyAdded = recentlyAdded,
                    onPlayMedia = onPlayMedia,
                    onNavigateToTvShows = onNavigateToTvShows,
                    onLibraryClick = { library ->
                        if (library.collectionType == "tvshows") {
                            onNavigateToTvShows(library.id)
                        } else {
                            selectedLibraryId = library.id
                            viewModel.loadLibraryItems(library.id)
                        }
                    },
                    onFocusChange = { imageUrl ->
                        focusedItemImageUrl = imageUrl
                    },
                    onDisconnect = {
                        viewModel.disconnect()
                        onDisconnect()
                    }
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
    onLibraryClick: (MediaItem) -> Unit,
    onFocusChange: (String?) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {    LazyColumn(
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
        }

        // Media Libraries Section
        if (mediaLibraries.isNotEmpty()) {
            item {
                MediaLibrarySection(
                    libraries = mediaLibraries,
                    onLibraryClick = onLibraryClick,
                    onLibraryFocus = onFocusChange
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
                        onItemFocus = onFocusChange
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
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(2f / 3f),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.05f
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaItem.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = mediaItem.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = mediaItem.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Show progress indicator for partially watched content
            mediaItem.userData?.let { userData ->
                if (userData.playbackPositionTicks != null && userData.playbackPositionTicks > 0) {
                    val progress = mediaItem.runTimeTicks?.let { total ->
                        if (total > 0) userData.playbackPositionTicks.toFloat() / total.toFloat()
                        else 0f
                    } ?: 0f

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                if (userData.played) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                Color.Green.copy(alpha = 0.9f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                            .padding(6.dp)
                    ) {
                        Text(
                            text = "✓",
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}