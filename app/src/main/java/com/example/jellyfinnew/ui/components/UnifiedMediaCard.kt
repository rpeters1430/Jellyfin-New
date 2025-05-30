package com.example.jellyfinnew.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.ui.utils.ImageCacheManager
import com.example.jellyfinnew.ui.utils.TvFocusableCard
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * Unified media card component with optimized image loading and caching
 * Replaces all the repetitive card components across the app
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun UnifiedMediaCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onUnfocus: () -> Unit = {},
    cardType: MediaCardType = MediaCardType.POSTER,
    showProgress: Boolean = true,
    showOverlay: Boolean = true
) {
    TvFocusableCard(
        onClick = onClick,
        modifier = modifier.then(
            when (cardType) {
                MediaCardType.POSTER -> Modifier.aspectRatio(2f / 3f)
                MediaCardType.BACKDROP -> Modifier.aspectRatio(16f / 9f)
                MediaCardType.SQUARE -> Modifier.aspectRatio(1f)
                MediaCardType.EPISODE -> Modifier.aspectRatio(16f / 9f)
            }
        ),
        onFocus = onFocus,
        onUnfocus = onUnfocus,
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.05f
        ),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent
        )
    ) { isFocused ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Optimized image with enhanced caching
            OptimizedMediaImage(
                mediaItem = mediaItem,
                cardType = cardType,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
            
            // Overlay with text information
            if (showOverlay) {
                MediaOverlay(
                    mediaItem = mediaItem,
                    cardType = cardType,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Progress indicator for partially watched content
            if (showProgress) {
                WatchProgressIndicator(
                    mediaItem = mediaItem,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            
            // Focus indicator
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun OptimizedMediaImage(
    mediaItem: MediaItem,
    cardType: MediaCardType,
    modifier: Modifier = Modifier
) {
    // Prepare a list of URLs to try, in order of preference
    val urlsToTry = mutableListOf<String?>()
    when (cardType) {
        MediaCardType.BACKDROP, MediaCardType.EPISODE -> {
            urlsToTry.add(mediaItem.backdropUrl)
            urlsToTry.add(mediaItem.imageUrl) // Fallback to primary/poster
            // Potentially add a third fallback if available, e.g., a generic series poster for episodes
        }
        MediaCardType.POSTER, MediaCardType.SQUARE -> {
            urlsToTry.add(mediaItem.imageUrl)
            urlsToTry.add(mediaItem.backdropUrl) // Fallback to backdrop
        }
        // Add other card types if necessary, or a default case
    }

    // Filter out null or blank URLs before passing to RobustAsyncImage
    val validUrls = urlsToTry.filter { !it.isNullOrBlank() }

    RobustAsyncImage(
        imageUrls = validUrls, // Pass the list of URLs
        contentDescription = mediaItem.name,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        enableRetry = true,
        showPlaceholder = true
    )
}

@Composable
private fun MediaOverlay(
    mediaItem: MediaItem,
    cardType: MediaCardType,
    modifier: Modifier = Modifier
) {
    when (cardType) {
        MediaCardType.EPISODE -> {
            EpisodeOverlay(mediaItem, modifier)
        }
        else -> {
            StandardOverlay(mediaItem, modifier)
        }
    }
}

@Composable
private fun StandardOverlay(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(12.dp),
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
}

@Composable
private fun EpisodeOverlay(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Series name
        mediaItem.seriesName?.let { seriesName ->
            Text(
                text = seriesName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
        }
        
        // Episode name
        Text(
            text = mediaItem.episodeName ?: mediaItem.name,
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
}

@Composable
private fun WatchProgressIndicator(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier
) {
    val userData = mediaItem.userData
    if (userData?.playbackPositionTicks != null && userData.playbackPositionTicks > 0) {
        val progress = if (mediaItem.runTimeTicks != null && mediaItem.runTimeTicks > 0) {
            (userData.playbackPositionTicks.toFloat() / mediaItem.runTimeTicks.toFloat()).coerceIn(0f, 1f)
        } else 0f
        
        if (progress > 0f) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(4.dp)
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
    } else if (userData?.played == true) {
        // Show "watched" indicator
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

/**
 * Media card types for different layouts
 */
enum class MediaCardType {
    POSTER,    // 2:3 aspect ratio for movies, shows
    BACKDROP,  // 16:9 aspect ratio for featured content
    SQUARE,    // 1:1 aspect ratio for libraries
    EPISODE    // 16:9 aspect ratio with episode-specific overlay
}
