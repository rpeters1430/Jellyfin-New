package com.example.jellyfinnew.ui.tvshows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.data.MediaItem
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvEpisodesScreen(
    viewModel: TvEpisodesViewModel,
    onEpisodeClick: (String) -> Unit, // episodeId
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val episodes by viewModel.tvEpisodes.collectAsStateWithLifecycle()
    val currentSeries by viewModel.currentSeries.collectAsStateWithLifecycle()
    val currentSeason by viewModel.currentSeason.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var focusedEpisodeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadEpisodes()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background image from focused episode, season, or series
        val backgroundImage = if (episodes.isNotEmpty() && focusedEpisodeIndex < episodes.size) {
            episodes[focusedEpisodeIndex].backdropUrl
                ?: currentSeason?.backdropUrl
                ?: currentSeries?.backdropUrl
        } else {
            currentSeason?.backdropUrl ?: currentSeries?.backdropUrl
        }

        backgroundImage?.let { imageUrl ->
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }

        // Dark overlay for better text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 32.dp)
        ) {
            // Header section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                ) {
                    Text("← Back to Seasons")
                }

                Column {
                    Text(
                        text = currentSeries?.name ?: "TV Series",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Text(
                        text = currentSeason?.name ?: "Season",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (connectionState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (episodes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No episodes found",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Immersive episodes list with horizontal cards
                ImmersiveEpisodesList(
                    episodes = episodes,
                    onEpisodeClick = onEpisodeClick,
                    onFocusChanged = { index ->
                        focusedEpisodeIndex = index
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ImmersiveEpisodesList(
    episodes: List<MediaItem>,
    onEpisodeClick: (String) -> Unit,
    onFocusChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedEpisode = if (episodes.isNotEmpty() && selectedIndex < episodes.size) {
        episodes[selectedIndex]
    } else null

    LaunchedEffect(selectedIndex) {
        onFocusChanged(selectedIndex)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Details section for focused episode (top 45% of screen)
        selectedEpisode?.let { episode ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = episode.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Episode metadata
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        episode.runTimeTicks?.let { ticks ->
                            val minutes = (ticks / 10_000_000 / 60).toInt()
                            Text(
                                text = "${minutes}m",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )                        }

                        episode.communityRating?.let { rating ->
                            Text(
                                text = "★ ${String.format(Locale.getDefault(), "%.1f", rating)}",
                                fontSize = 14.sp,
                                color = Color.Yellow,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    episode.overview?.let { overview ->
                        Text(
                            text = overview,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Episodes horizontal row (bottom 55% of screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                itemsIndexed(episodes) { index, episode ->
                    val isSelected = index == selectedIndex

                    HorizontalEpisodeCard(
                        episode = episode,
                        isSelected = isSelected,
                        onClick = { onEpisodeClick(episode.id) },
                        onFocus = {
                            selectedIndex = index
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun HorizontalEpisodeCard(
    episode: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(320.dp)
            .height(180.dp) // Horizontal card dimensions
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocus()
                }
            },
        scale = CardDefaults.scale(
            scale = if (isSelected) 1.05f else 1.0f,
            focusedScale = 1.1f
        ),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent
        )
    ) {
        Box {
            // Episode thumbnail (horizontal)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(episode.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = episode.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Selected indicator overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                )
            }

            // Focus indicator
            if (isFocused) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.1f))
                )
            }

            // Title overlay
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = episode.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Runtime if available
                    episode.runTimeTicks?.let { ticks ->
                        val minutes = (ticks / 10_000_000 / 60).toInt()
                        Text(
                            text = "${minutes}m",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Watch progress indicator
            episode.userData?.let { userData ->
                if (userData.played || (userData.playbackPositionTicks ?: 0) > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                if (userData.played) Color.Green.copy(alpha = 0.9f)
                                else Color.Blue.copy(alpha = 0.9f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (userData.played) "✓" else "▶",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}