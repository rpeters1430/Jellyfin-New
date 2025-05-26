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
fun TvSeasonsScreen(
    viewModel: TvSeasonsViewModel,
    onSeasonClick: (String, String) -> Unit, // seriesId, seasonId
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val seasons by viewModel.tvSeasons.collectAsStateWithLifecycle()
    val currentSeries by viewModel.currentSeries.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var focusedSeasonIndex by remember { mutableStateOf(0) }
    
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
                }            } else {
                // TV-optimized seasons list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(seasons) { index, season ->
                        SeasonCard(
                            season = season,
                            seriesId = currentSeries?.id ?: "",
                            onClick = { 
                                currentSeries?.let { series ->
                                    onSeasonClick(series.id, season.id)
                                }
                            },
                            onFocus = { 
                                focusedSeasonIndex = index
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
private fun SeasonCard(
    season: MediaItem,
    seriesId: String,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Season poster
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(season.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = season.name,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            // Season info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = season.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    season.overview?.let { overview ->
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
                
                // Watch progress indicator
                season.userData?.let { userData ->
                    if (userData.played || (userData.playbackPositionTicks ?: 0) > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color.Green.copy(alpha = 0.8f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
