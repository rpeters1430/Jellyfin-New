package com.example.jellyfinnew.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape // Keep for specific non-theme shapes if any
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme // Ensure MaterialTheme is imported
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.ui.components.UnifiedMediaCard
import com.example.jellyfinnew.ui.components.MediaCardType
import com.example.jellyfinnew.ui.utils.TvFocusableCard
import com.example.jellyfinnew.ui.utils.TvOptimizations
import com.example.jellyfinnew.ui.utils.TvSpacing
import com.example.jellyfinnew.ui.utils.TvListDefaults
import kotlinx.coroutines.delay
import java.util.Locale
import org.jellyfin.sdk.model.api.BaseItemKind

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
            style = MaterialTheme.typography.headlineMedium, // Use theme typography
            modifier = Modifier.padding(horizontal = 4.dp)
            // Color will be MaterialTheme.colorScheme.onSurface by default
        )

        TvFocusableCard(
            onClick = { onPlayClick(currentItem.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp), // Increased from 320dp to 420dp
            onFocus = { isPaused = true },
            onUnfocus = { isPaused = false },
            shape = MaterialTheme.shapes.large, // Use theme shape
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
) {
    Box(modifier = Modifier.fillMaxSize()) {
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
                style = if (isFocused) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                item.communityRating?.let { rating ->
                    Text(
                        text = "★ ${String.format(Locale.getDefault(), "%.1f", rating)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary // Use theme color for rating
                    )
                }

                item.productionYear?.let { year ->
                    Text(
                        text = year.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                    )
                }

                item.runTimeTicks?.let { ticks ->
                    val minutes = (ticks / 10_000_000 / 60).toInt()
                    Text(
                        text = "${minutes}m",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                    )
                }
            }

            // Overview
            item.overview?.let { overview ->
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium, // Use theme typography
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.9f), // Use theme color
                    maxLines = if (isFocused) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                    // Removed explicit lineHeight, rely on theme style's line height
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
                                    MaterialTheme.colorScheme.inverseOnSurface // Use theme color
                                else
                                    MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.4f), // Use theme color
                                shape = androidx.compose.foundation.shape.CircleShape // Keeping circle shape
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
                        MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.05f), // Use theme color
                        MaterialTheme.shapes.large // Use theme shape
                    )
            )
        }
    }
}

/**
 * TV-optimized media library section with preloading
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaLibrarySection(
    libraries: List<MediaItem>,
    onLibraryClick: (MediaItem) -> Unit,
    onLibraryFocus: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onLibraryIndexFocused: ((Int, List<MediaItem>) -> Unit)? = null,
    isLoading: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(TvSpacing.medium)
    ) {
        Text(
            text = "Libraries",
            style = MaterialTheme.typography.headlineSmall, // Use theme typography
            modifier = Modifier.padding(horizontal = 4.dp)
            // Color will be MaterialTheme.colorScheme.onSurface by default
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator() // Color will be MaterialTheme.colorScheme.primary by default
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
                    style = MaterialTheme.typography.bodyLarge, // Use theme typography
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Keep existing color logic
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(TvListDefaults.itemSpacing)
            ) {
                itemsIndexed(libraries) { index, library ->
                    LibraryCard(
                        library = library,
                        onClick = { onLibraryClick(library) },
                        onFocus = {
                            // Try backdrop first, fallback to image, then use backdrop again
                            val focusImageUrl = library.backdropUrl ?: library.imageUrl ?: library.backdropUrl
                            onLibraryFocus(focusImageUrl)
                            // Trigger preloading for adjacent libraries
                            onLibraryIndexFocused?.invoke(index, libraries)
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
) {
    // Use the new UnifiedMediaCard with optimized caching for library cards
    UnifiedMediaCard(
        mediaItem = library,
        onClick = onClick,
        modifier = modifier.width(320.dp),
        onFocus = onFocus,
        cardType = MediaCardType.BACKDROP, // Libraries use backdrop layout
        showProgress = false, // Libraries don't have progress
        showOverlay = true
    )
}

/**
 * Recently added content section with improved layout and preloading
 * Fixed to use proper card types for episodes (banner cards)
 */
@Composable
fun RecentlyAddedSection(
    title: String,
    items: List<MediaItem>,
    onItemClick: (String) -> Unit,
    onItemFocus: (String?) -> Unit,
    modifier: Modifier = Modifier,
    onItemIndexFocused: ((Int, List<MediaItem>) -> Unit)? = null
) {
    if (items.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge, // Use theme typography
                modifier = Modifier.padding(horizontal = 4.dp)
                // Color will be MaterialTheme.colorScheme.onSurface by default
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp) // Recently added cards spacing
            ) {
                itemsIndexed(items) { index, item ->
                    MediaCard(
                        mediaItem = item,
                        onClick = { onItemClick(item.id) },
                        onFocus = {
                            // For episodes, prefer backdrop/series poster, otherwise use item images
                            val focusImageUrl = when (item.type) {
                                BaseItemKind.EPISODE -> {
                                    item.backdropUrl ?: item.seriesPosterUrl ?: item.imageUrl
                                }
                                else -> item.backdropUrl ?: item.imageUrl
                            }
                            onItemFocus(focusImageUrl)
                            // Trigger preloading for adjacent items
                            onItemIndexFocused?.invoke(index, items)
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
    // Determine the appropriate card type and size based on media type
    val (cardType, cardModifier) = when (mediaItem.type) {
        BaseItemKind.EPISODE -> {
            // Episodes should now use vertical poster cards
            MediaCardType.POSTER to modifier.width(180.dp) // This width is typical for poster cards in this file
        }
        else -> {
            // Other media uses poster cards
            MediaCardType.POSTER to modifier.width(180.dp)
        }
    }

    // Use the new UnifiedMediaCard with optimized caching
    UnifiedMediaCard(
        mediaItem = mediaItem,
        onClick = onClick,
        modifier = cardModifier,
        onFocus = onFocus,
        cardType = cardType,
        showProgress = true,
        showOverlay = true
    )
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
            style = MaterialTheme.typography.displaySmall, // Use theme typography
            color = MaterialTheme.colorScheme.primary // Keep existing color logic
        )

        Button(onClick = onDisconnect) {
            Text("Disconnect") // Button text style will come from theme's default for buttons
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
            CircularProgressIndicator() // Color will be MaterialTheme.colorScheme.primary by default
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge, // Use theme typography
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Keep existing color logic
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
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
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
                style = MaterialTheme.typography.headlineSmall, // Use theme typography
                color = MaterialTheme.colorScheme.error // Keep existing color logic
            )

            Text(
                text = error,
                style = MaterialTheme.typography.bodyLarge, // Use theme typography
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // Keep existing color logic
            )

            onRetry?.let { retry ->
                Button(onClick = retry) {
                    Text("Retry") // Button text style will come from theme's default for buttons
                }
            }
        }
    }
}
