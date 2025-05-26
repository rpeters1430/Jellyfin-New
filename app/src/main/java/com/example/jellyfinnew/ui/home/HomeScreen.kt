package com.example.jellyfinnew.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay

import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.data.MediaItem
import org.jellyfin.sdk.model.api.BaseItemKind

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPlayMedia: (String) -> Unit,
    onNavigateToTvShows: (String) -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {val mediaLibraries by viewModel.mediaLibraries.collectAsStateWithLifecycle()
    val currentLibraryItems by viewModel.currentLibraryItems.collectAsStateWithLifecycle()
    val featuredItem by viewModel.featuredItem.collectAsStateWithLifecycle()
    val featuredItems by viewModel.featuredItems.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentlyAdded.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    
    var selectedLibraryId by remember { mutableStateOf<String?>(null) }
    var focusedItemImageUrl by remember { mutableStateOf<String?>(null) }
    
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
        
        if (selectedLibraryId == null) {
            // Main home view with featured content and library rows
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header with disconnect button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Jellyfin",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Button(
                            onClick = {
                                viewModel.disconnect()
                                onDisconnect()
                            }
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
                  // Featured Carousel
                if (featuredItems.isNotEmpty()) {
                    item {
                        RotatingFeaturedCarousel(
                            featuredItems = featuredItems,
                            onPlayClick = { itemId -> onPlayMedia(itemId) },
                            onFocus = { imageUrl -> focusedItemImageUrl = imageUrl }
                        )
                    }
                }
                
                // Media Libraries Section
                if (mediaLibraries.isNotEmpty()) {
                    item {
                        Text(
                            text = "Libraries",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                      item {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {                            items(mediaLibraries.size) { index ->
                                val library = mediaLibraries[index]
                                LibraryCard(
                                    mediaItem = library,
                                    onClick = {
                                        // Check if this is a TV show library
                                        if (library.collectionType == "tvshows") {
                                            onNavigateToTvShows(library.id)
                                        } else {
                                            selectedLibraryId = library.id
                                            viewModel.loadLibraryItems(library.id)
                                        }
                                    },
                                    onFocus = {
                                        // Use backdrop if available, fall back to primary image
                                        focusedItemImageUrl = library.backdropUrl ?: library.imageUrl
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Recently Added Sections
                recentlyAdded.forEach { (libraryName, items) ->
                    if (items.isNotEmpty()) {                        item {
                            Text(
                                text = libraryName, // Already includes "Recently Added" prefix
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                          item {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {                                items(items.size) { index ->
                                    val item = items[index]
                                    MediaCard(
                                        mediaItem = item,
                                        onClick = { onPlayMedia(item.id) },
                                        onFocus = {
                                            // Use larger backdrop image for focus background
                                            focusedItemImageUrl = item.backdropUrl ?: item.imageUrl
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Library content view (existing functionality)
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp)
            ) {
                // Back button and library title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Button(
                        onClick = { selectedLibraryId = null }
                    ) {
                        Text("← Back")
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Text(
                        text = mediaLibraries.find { it.id == selectedLibraryId }?.name ?: "Library",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                if (currentLibraryItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading items...")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(currentLibraryItems) { item ->
                            MediaItemCard(
                                mediaItem = item,
                                onClick = {
                                    if (item.type in listOf(BaseItemKind.MOVIE, BaseItemKind.EPISODE)) {
                                        onPlayMedia(item.id)
                                    } else {
                                        // Navigate to item (for series, seasons, etc.)
                                        selectedLibraryId = item.id
                                        viewModel.loadLibraryItems(item.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Error overlay
        connectionState.error?.let { error ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: $error",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LibraryCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Debug logging
    println("DEBUG LibraryCard: ${mediaItem.name}")
    println("DEBUG - imageUrl: ${mediaItem.imageUrl}")
    println("DEBUG - backdropUrl: ${mediaItem.backdropUrl}")
    
    Column(
        modifier = modifier.width(240.dp), // Wider for horizontal layout
        horizontalAlignment = Alignment.CenterHorizontally
    ) {        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f) // 16:9 landscape aspect ratio
                .onFocusChanged { if (it.isFocused) onFocus() },
            shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
            scale = CardDefaults.scale(
                scale = 1.0f,
                focusedScale = 1.05f // Subtle scaling for library cards
            )) {
            val imageUrl = mediaItem.backdropUrl ?: mediaItem.imageUrl
            println("DEBUG LibraryCard: Library '${mediaItem.name}', imageUrl: $imageUrl, backdropUrl: ${mediaItem.backdropUrl}")
            
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl) // Prefer backdrop for horizontal cards
                    .crossfade(true)
                    .build(),
                contentDescription = mediaItem.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = mediaItem.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun MediaItemCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(2f / 3f)
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
                    .padding(8.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = mediaItem.name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.ui.graphics.Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Show progress indicator for partially watched content
            mediaItem.userData?.let { userData ->
                if (userData.playbackPositionTicks != null && userData.playbackPositionTicks > 0) {
                    val progress = mediaItem.runTimeTicks?.let { total ->
                        if (total > 0) userData.playbackPositionTicks.toFloat() / total.toFloat()
                        else 0f
                    } ?: 0f
                    
                    // Simple progress bar placeholder - would need actual progress indicator for TV
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        // Progress visualization could be added here
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FeaturedCarousel(
    featuredItem: MediaItem,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onPlayClick,
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = CardDefaults.shape(RoundedCornerShape(16.dp))
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(featuredItem.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = featuredItem.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )
            
            // Content info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(32.dp)
                    .fillMaxWidth(0.6f)
            ) {
                Text(
                    text = featuredItem.name,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                featuredItem.overview?.let { overview ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = overview,
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text("▶ Play", fontSize = 18.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun MediaCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.width(160.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f / 1.5f) // 1:1.5 poster aspect ratio
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onFocus()
                    }
                },
            shape = CardDefaults.shape(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaItem.seriesPosterUrl ?: mediaItem.imageUrl) // Use series poster for episodes
                    .crossfade(true)
                    .build(),
                contentDescription = mediaItem.seriesName ?: mediaItem.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // For episodes, show episode name and series name on separate lines
            if (mediaItem.episodeName != null && mediaItem.seriesName != null) {
                Text(
                    text = mediaItem.episodeName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mediaItem.seriesName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            } else {
                // For regular items, show just the name
                Text(
                    text = mediaItem.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
              if (mediaItem.userData?.played == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Watched",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RotatingFeaturedCarousel(
    featuredItems: List<MediaItem>,
    onPlayClick: (String) -> Unit,
    onFocus: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentIndex by remember { mutableStateOf(0) }
      // Auto-rotation effect
    LaunchedEffect(featuredItems) {
        if (featuredItems.isNotEmpty()) {
            // Trigger focus for the first item when the carousel loads
            onFocus(featuredItems[0].backdropUrl ?: featuredItems[0].imageUrl)
            
            while (true) {
                delay(25000) // 25 seconds
                currentIndex = (currentIndex + 1) % featuredItems.size
            }
        }
    }
      if (featuredItems.isNotEmpty()) {
        val currentItem = featuredItems[currentIndex]
        
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {            Text(
                text = "Featured",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            Card(
                onClick = { onPlayClick(currentItem.id) },
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Reduced from 0.95f to prevent off-screen scaling
                    .height(300.dp) // Reduced from 320.dp
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            onFocus(currentItem.backdropUrl ?: currentItem.imageUrl)
                        }
                    },
                shape = CardDefaults.shape(RoundedCornerShape(24.dp)),
                scale = CardDefaults.scale(
                    scale = 1.0f,
                    focusedScale = 1.05f // Subtle scaling to prevent off-screen issues
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentItem.backdropUrl ?: currentItem.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = currentItem.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // Gradient overlay for better text readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    ),
                                    startY = 0f,
                                    endY = 800f
                                )
                            )
                    )
                    
                    // Content overlay
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = currentItem.name,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Rating and Year row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rating
                            currentItem.communityRating?.let { rating ->
                                Text(
                                    text = "★ ${String.format("%.1f", rating)}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Yellow
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                            }
                            
                            // Year
                            currentItem.productionYear?.let { year ->
                                Text(
                                    text = year.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Overview
                        currentItem.overview?.let { overview ->
                            Text(
                                text = overview,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 20.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Dots indicator for current position
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            featuredItems.forEachIndexed { index, _ ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == currentIndex) 10.dp else 6.dp)
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
                }
            }
        }
    }
}
