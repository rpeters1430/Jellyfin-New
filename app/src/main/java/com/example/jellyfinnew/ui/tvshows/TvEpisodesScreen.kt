package com.example.jellyfinnew.ui.tvshows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    var focusedEpisodeIndex by remember { mutableStateOf(0) }
    
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
                    .padding(bottom = 32.dp),
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
                }            } else {
                // Episodes list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(episodes) { index, episode ->
                        EpisodeCard(
                            episode = episode,
                            onClick = { 
                                onEpisodeClick(episode.id)
                            },
                            onFocus = { 
                                focusedEpisodeIndex = index
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun EpisodeCard(
    episode: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                if (focusState.isFocused) {
                    onFocus()
                }
            },
        scale = CardDefaults.scale(
            scale = if (isFocused) 1.05f else 1.0f,
            focusedScale = 1.05f
        ),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Episode thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(episode.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = episode.name,
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Episode info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = episode.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    episode.overview?.let { overview ->
                        Text(
                            text = overview,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                // Episode metadata and progress
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Runtime
                        episode.runTimeTicks?.let { ticks ->
                            val minutes = (ticks / 10_000_000 / 60).toInt()
                            Text(
                                text = "${minutes}m",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        // Rating
                        episode.communityRating?.let { rating ->
                            Text(
                                text = "★ ${String.format("%.1f", rating)}",
                                fontSize = 12.sp,
                                color = Color(0xFFFFD700), // Gold color
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Watch progress indicator
                    episode.userData?.let { userData ->
                        if (userData.played || (userData.playbackPositionTicks ?: 0) > 0) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (userData.played) Color.Green.copy(alpha = 0.8f) 
                                        else Color.Blue.copy(alpha = 0.8f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (userData.played) "Watched" else "In Progress",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
