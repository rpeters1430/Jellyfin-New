package com.example.jellyfinnew.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: MusicViewModel,
    artistId: String,
    onAlbumClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val currentArtist by viewModel.currentArtist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val focusedAlbum by viewModel.focusedAlbum.collectAsStateWithLifecycle()
    
    var focusedItemImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(artistId) {
        viewModel.loadAlbums(artistId)
    }

    // Update background image when focused album changes
    LaunchedEffect(focusedAlbum) {
        focusedItemImageUrl = focusedAlbum?.backdropUrl ?: focusedAlbum?.imageUrl
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
                    text = currentArtist?.name?.let { "Albums - $it" } ?: "Albums",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (albums.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No albums found",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Immersive List for Albums with square cards
                ImmersiveAlbumsList(
                    albums = albums,
                    onAlbumClick = onAlbumClick,
                    onFocusChange = { album ->
                        viewModel.updateFocusedAlbum(album)
                        focusedItemImageUrl = album?.backdropUrl ?: album?.imageUrl
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ImmersiveAlbumsList(
    albums: List<MediaItem>,
    onAlbumClick: (String) -> Unit,
    onFocusChange: (MediaItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedAlbum = if (albums.isNotEmpty() && selectedIndex < albums.size) {
        albums[selectedIndex]
    } else null

    // Trigger focus for the initially selected item
    LaunchedEffect(selectedAlbum) {
        onFocusChange(selectedAlbum)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Details section for focused item (top 40% of screen)
        selectedAlbum?.let { album ->
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
                        text = album.name,
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
                        album.productionYear?.let { year ->
                            Text(
                                text = year.toString(),
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Artist name from seriesName field
                        album.seriesName?.let { artistName ->
                            Text(
                                text = "by $artistName",
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    album.overview?.let { overview ->
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

        // Albums square grid (bottom 60% of screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(6), // 6 columns of square album covers
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(albums.size) { index ->
                    val album = albums[index]
                    val isSelected = index == selectedIndex

                    SquareAlbumCard(
                        album = album,
                        isSelected = isSelected,
                        onClick = { onAlbumClick(album.id) },
                        onFocus = {
                            selectedIndex = index
                            onFocusChange(album)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SquareAlbumCard(
    album: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use the UnifiedMediaCard with SQUARE type for album covers
    UnifiedMediaCard(
        mediaItem = album,
        onClick = onClick,
        modifier = modifier,
        onFocus = onFocus,
        cardType = MediaCardType.SQUARE,
        showProgress = false,
        showOverlay = true // Show album title overlay
    )
}
