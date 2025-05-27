package com.example.jellyfinnew.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.ui.utils.TvFocusableCard
import com.example.jellyfinnew.ui.utils.TvOptimizations
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Enhanced Featured Carousel with better TV optimization
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EnhancedFeaturedCarousel(
    featuredItems: List<MediaItem>,
    onPlayClick: (String) -> Unit,
    onFocus: (String?) -> Unit,
    modifier: Modifier = Modifier,
    autoRotateEnabled: Boolean = true,
    autoRotateInterval: Long = 20_000L // 20 seconds
) {
    if (featuredItems.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }

    // Auto-rotation with pause on focus
    LaunchedEffect(currentIndex, isPaused, autoRotateEnabled, featuredItems.size) {
        if (autoRotateEnabled && !isPaused && featuredItems.isNotEmpty()) {
            delay(autoRotateInterval)
            currentIndex = (currentIndex + 1) % featuredItems.size
        }
    }

    // Update background when item changes
    LaunchedEffect(currentIndex, featuredItems) {
        if (currentIndex < featuredItems.size) {
            val currentItem = featuredItems[currentIndex]
            onFocus(currentItem.backdropUrl ?: currentItem.imageUrl)
        }
    }

    val currentItem = featuredItems[currentIndex]

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Featured",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        TvFocusableCard(
            onClick = { onPlayClick(currentItem.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp), // Increased from 320dp to 420dp
            onFocus = { isPaused = true },
            onUnfocus = { isPaused = false },
            shape = CardDefaults.shape(RoundedCornerShape(16.dp)),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.02f
            )
        ) { isFocused ->
            FeaturedItemContent(
                item = currentItem,
                isFocused = isFocused,
                currentIndex = currentIndex,
                totalItems = featuredItems.size
            )
        }
    }
}

@Composable
private fun FeaturedItemContent(
    item: MediaItem,
    isFocused: Boolean,
    currentIndex: Int,
    totalItems: Int
) {    Box(modifier = Modifier.fillMaxSize()) {
        // Optimized background image
        TvOptimizations.OptimizedAsyncImage(
            imageUrl = item.backdropUrl ?: item.imageUrl,
            contentDescription = item.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = item.name,
                fontSize = if (isFocused) 32.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {                item.communityRating?.let { rating ->
                    Text(
                        text = "★ ${String.format(Locale.getDefault(), "%.1f", rating)}",
                        fontSize = 16.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Medium
                    )
                }

                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                item.runTimeTicks?.let { ticks ->
                    val minutes = (ticks / 10_000_000 / 60).toInt()
                    Text(
                        text = "${minutes}m",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Overview
            item.overview?.let { overview ->
                Text(
                    text = overview,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = if (isFocused) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // Progress indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalItems) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == currentIndex) 12.dp else 8.dp)
                            .background(
                                color = if (index == currentIndex)
                                    Color.White
                                else
                                    Color.White.copy(alpha = 0.4f),
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    )
                }
            }
        }

        // Focus indicator
        if (isFocused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(16.dp)
                    )
            )
        }
    }
}

/**
 * TV-optimized media library section
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaLibrarySection(
    libraries: List<MediaItem>,
    onLibraryClick: (MediaItem) -> Unit,
    onLibraryFocus: (String?) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TvOptimizations.TvSpacing.medium)
    ) {
        Text(
            text = "Libraries",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (libraries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No libraries found",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(TvOptimizations.TvListDefaults.itemSpacing)
            ) {
                items(libraries) { library ->
                    LibraryCard(
                        library = library,
                        onClick = { onLibraryClick(library) },
                        onFocus = {
                            onLibraryFocus(library.backdropUrl ?: library.imageUrl)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun LibraryCard(
    library: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {    TvFocusableCard(
        onClick = onClick,
        modifier = modifier.width(320.dp), // Increased from 280dp
        onFocus = onFocus,
        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
        scale = TvOptimizations.optimizedCardScale,
        colors = CardDefaults.colors(
            containerColor = Color.Transparent // Remove colored background
        )
    ) { isFocused ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp), // Increased spacing
            modifier = Modifier.padding(12.dp) // Increased padding
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
            ) {                TvOptimizations.OptimizedAsyncImage(
                    imageUrl = library.imageUrl,
                    contentDescription = library.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }

            Text(
                text = library.name,
                fontSize = if (isFocused) 18.sp else 16.sp, // Increased font size
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Recently added content section with improved layout
 */
@Composable
fun RecentlyAddedSection(
    title: String,
    items: List<MediaItem>,
    onItemClick: (String) -> Unit,
    onItemFocus: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Recently added cards spacing
            ) {
                items(items) { item ->
                    MediaCard(
                        mediaItem = item,
                        onClick = { onItemClick(item.id) },
                        onFocus = {
                            onItemFocus(item.backdropUrl ?: item.imageUrl)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = modifier.width(180.dp), // Increased from 160dp
        onFocus = onFocus,
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.05f
        ),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent // Remove colored background
        )
    ) { isFocused ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp) // Increased spacing
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(mediaItem.seriesPosterUrl ?: mediaItem.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = mediaItem.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Progress indicator
                mediaItem.userData?.let { userData ->
                    if (userData.played) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(
                                    Color.Green.copy(alpha = 0.9f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "✓",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if ((userData.playbackPositionTicks ?: 0) > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .background(
                                    Color.Blue.copy(alpha = 0.9f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "▶",
                                fontSize = 8.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.1f))
                    )
                }
            }

            // Title and metadata - centered
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .height(60.dp), // Fixed height to prevent layout shift
                verticalArrangement = Arrangement.Top
            ) {
                if (mediaItem.episodeName != null && mediaItem.seriesName != null) {
                    Text(
                        text = mediaItem.episodeName,
                        fontSize = if (isFocused) 14.sp else 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = mediaItem.seriesName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = mediaItem.name,
                        fontSize = if (isFocused) 14.sp else 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Header component with disconnect button
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeHeader(
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Jellyfin",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Button(onClick = onDisconnect) {
            Text("Disconnect")
        }
    }
}

/**
 * Loading state component
 */
@Composable
fun LoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Error state component
 */
@Composable
fun ErrorState(
    error: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Error",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )

            Text(
                text = error,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            onRetry?.let { retry ->
                Button(onClick = retry) {
                    Text("Retry")
                }
            }
        }
    }
}