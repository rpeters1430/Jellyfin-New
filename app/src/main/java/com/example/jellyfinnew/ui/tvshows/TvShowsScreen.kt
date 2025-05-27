package com.example.jellyfinnew.ui.tvshows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.jellyfinnew.ui.components.UnifiedMediaCard
import com.example.jellyfinnew.ui.components.MediaCardType
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvShowsScreen(
    viewModel: TvShowsViewModel,
    onSeriesClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tvShows by viewModel.tvShows.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var focusedItemImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTvShows()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background image that changes based on focused item
        focusedItemImageUrl?.let { imageUrl ->
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
                alpha = 0.3f
            )
        }

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) {
                    Text("← Back")
                }

                Text(
                    text = "TV Shows",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (connectionState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (tvShows.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No TV shows found",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Immersive List for TV Shows with vertical cards
                ImmersiveTvShowsList(
                    tvShows = tvShows,
                    onSeriesClick = onSeriesClick,
                    onFocus = { focusedItemImageUrl = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ImmersiveTvShowsList(
    tvShows: List<MediaItem>,
    onSeriesClick: (String) -> Unit,
    onFocus: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedSeries = if (tvShows.isNotEmpty() && selectedIndex < tvShows.size) {
        tvShows[selectedIndex]
    } else null

    // Trigger focus for the initially selected item
    LaunchedEffect(selectedSeries) {
        selectedSeries?.let { series ->
            onFocus(series.backdropUrl ?: series.imageUrl)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Details section for focused item (top 45% of screen)
        selectedSeries?.let { series ->
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
                        text = series.name,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        series.productionYear?.let { year ->
                            Text(
                                text = year.toString(),
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )                        }

                        series.communityRating?.let { rating ->
                            Text(
                                text = "★ ${String.format(Locale.getDefault(), "%.1f", rating)}",
                                fontSize = 16.sp,
                                color = Color.Yellow
                            )
                        }
                    }

                    series.overview?.let { overview ->
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

        // TV Shows vertical grid (bottom 55% of screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6), // 6 columns of vertical posters
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tvShows.size) { index ->
                    val series = tvShows[index]
                    val isSelected = index == selectedIndex

                    VerticalTvShowCard(
                        series = series,
                        isSelected = isSelected,
                        onClick = { onSeriesClick(series.id) },
                        onFocus = {
                            selectedIndex = index
                            onFocus(series.backdropUrl ?: series.imageUrl)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VerticalTvShowCard(
    series: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use the new UnifiedMediaCard with optimized caching
    UnifiedMediaCard(
        mediaItem = series,
        onClick = onClick,
        modifier = modifier,
        onFocus = onFocus,
        cardType = MediaCardType.POSTER,
        showProgress = true,
        showOverlay = false // Don't show overlay text for TV shows grid
    )
}