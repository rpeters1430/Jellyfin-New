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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvSeasonsScreen(
    viewModel: TvSeasonsViewModel,
    onSeasonClick: (String, String) -> Unit, // seriesId, seasonId
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons by viewModel.tvSeasons.collectAsStateWithLifecycle()
    val currentSeries by viewModel.currentSeries.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var focusedSeasonIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.loadSeasons()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Background image from focused season or series
        val backgroundImage = if (seasons.isNotEmpty() && focusedSeasonIndex < seasons.size) {
            seasons[focusedSeasonIndex].backdropUrl ?: currentSeries?.backdropUrl
        } else {
            currentSeries?.backdropUrl
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
                    Text("← Back to TV Shows")
                }

                Column {
                    Text(
                        text = currentSeries?.name ?: "TV Series",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    currentSeries?.productionYear?.let { year ->
                        Text(
                            text = year.toString(),
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (connectionState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else if (seasons.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No seasons found",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Immersive seasons list with horizontal cards
                ImmersiveSeasonsList(
                    seasons = seasons,
                    currentSeries = currentSeries,
                    onSeasonClick = { season ->
                        currentSeries?.let { series ->
                            onSeasonClick(series.id, season.id)
                        }
                    },
                    onFocusChanged = { index ->
                        focusedSeasonIndex = index
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ImmersiveSeasonsList(
    seasons: List<MediaItem>,
    currentSeries: MediaItem?,
    onSeasonClick: (MediaItem) -> Unit,
    onFocusChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedSeason = if (seasons.isNotEmpty() && selectedIndex < seasons.size) {
        seasons[selectedIndex]
    } else null

    LaunchedEffect(selectedIndex) {
        onFocusChanged(selectedIndex)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Details section for focused season (top 40% of screen)
        selectedSeason?.let { season ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.4f)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.BottomStart),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = season.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    season.overview?.let { overview ->
                        Text(
                            text = overview,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        // Seasons horizontal row (bottom 60% of screen)
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
                itemsIndexed(seasons) { index, season ->
                    val isSelected = index == selectedIndex

                    HorizontalSeasonCard(
                        season = season,
                        isSelected = isSelected,
                        onClick = { onSeasonClick(season) },
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
private fun HorizontalSeasonCard(
    season: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(280.dp)
            .height(160.dp) // Horizontal card dimensions
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
            // Season image (horizontal)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(season.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = season.name,
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
                Text(
                    text = season.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Watch progress indicator
            season.userData?.let { userData ->
                if (userData.played || (userData.playbackPositionTicks ?: 0) > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                Color.Green.copy(alpha = 0.9f),
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