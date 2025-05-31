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
import coil.request.ImageRequest
import com.example.jellyfinnew.data.MediaItem
import org.jellyfin.sdk.model.api.BaseItemKind

/**
 * Fixed unified media card component with proper Compose structure
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
    // Stable keys for Compose
    val cardKey = remember(mediaItem.id, cardType) { "${mediaItem.id}-${cardType.name}" }

    Card(
        onClick = onClick,
        modifier = modifier.then(
            when (cardType) {
                MediaCardType.POSTER -> Modifier.aspectRatio(2f / 3f)
                MediaCardType.BACKDROP -> Modifier.aspectRatio(16f / 9f)
                MediaCardType.SQUARE -> Modifier.aspectRatio(1f)
                MediaCardType.EPISODE -> Modifier.aspectRatio(16f / 9f)
            }
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.05f
        ),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent
        ),
        border = CardDefaults.border()
    ) {
        // Use key to ensure stable composition
        key(cardKey) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Image with proper error handling
                MediaImageContent(
                    mediaItem = mediaItem,
                    cardType = cardType,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )

                // Overlay content
                if (showOverlay) {
                    MediaOverlayContent(
                        mediaItem = mediaItem,
                        cardType = cardType,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Progress indicator
                if (showProgress) {
                    WatchProgressContent(
                        mediaItem = mediaItem,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }

    // Handle focus events outside the Card composition
    LaunchedEffect(Unit) {
        onFocus()
    }

    DisposableEffect(Unit) {
        onDispose {
            onUnfocus()
        }
    }
}

@Composable
private fun MediaImageContent(
    mediaItem: MediaItem,
    cardType: MediaCardType,
    modifier: Modifier = Modifier
) {
    // Determine image URL based on card type
    val imageUrl = when (cardType) {
        MediaCardType.BACKDROP, MediaCardType.EPISODE -> {
            mediaItem.backdropUrl ?: mediaItem.imageUrl
        }
        MediaCardType.POSTER, MediaCardType.SQUARE -> {
            mediaItem.imageUrl ?: mediaItem.backdropUrl
        }
    }

    if (imageUrl != null) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = mediaItem.name,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        // Fallback placeholder
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No Image",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MediaOverlayContent(
    mediaItem: MediaItem,
    cardType: MediaCardType,
    modifier: Modifier = Modifier
) {
    when (cardType) {
        MediaCardType.EPISODE -> {
            EpisodeOverlayContent(mediaItem, modifier)
        }
        else -> {
            StandardOverlayContent(mediaItem, modifier)
        }
    }
}

@Composable
private fun StandardOverlayContent(
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
private fun EpisodeOverlayContent(
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
private fun WatchProgressContent(
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