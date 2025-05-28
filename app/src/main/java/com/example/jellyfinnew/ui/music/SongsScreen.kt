package com.example.jellyfinnew.ui.music

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.jellyfinnew.ui.utils.TvFocusableCard
import java.util.Locale

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SongsScreen(
    viewModel: MusicViewModel,
    albumId: String,
    onSongClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val currentAlbum by viewModel.currentAlbum.collectAsStateWithLifecycle()
    val currentArtist by viewModel.currentArtist.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val focusedSong by viewModel.focusedSong.collectAsStateWithLifecycle()
    
    var focusedItemImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(albumId) {
        viewModel.loadSongs(albumId)
    }

    // Update background image when focused song changes
    LaunchedEffect(focusedSong) {
        focusedItemImageUrl = focusedSong?.imageUrl
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
                alpha = 0.2f
            )
        }

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Header with back button and album info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Back button and title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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

                    Column {
                        Text(
                            text = currentAlbum?.name ?: "Songs",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Show artist name if available
                        currentArtist?.name?.let { artistName ->
                            Text(
                                text = "by $artistName",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Album artwork and details section
                currentAlbum?.let { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Album artwork
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(album.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Album artwork",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                            contentScale = ContentScale.Crop
                        )

                        // Album details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            album.productionYear?.let { year ->
                                Text(
                                    text = year.toString(),
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }

                            album.overview?.let { overview ->
                                Text(
                                    text = overview,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Text(
                                text = "${songs.size} ${if (songs.size == 1) "song" else "songs"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (songs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No songs found",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Songs list
                SongsList(
                    songs = songs,
                    onSongClick = onSongClick,
                    onFocusChange = { song ->
                        viewModel.updateFocusedSong(song)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SongsList(
    songs: List<MediaItem>,
    onSongClick: (String) -> Unit,
    onFocusChange: (MediaItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    // Trigger focus for the initially selected item
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            onFocusChange(songs[selectedIndex])
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(songs) { index, song ->
            val isSelected = index == selectedIndex

            SongListItem(
                song = song,
                isSelected = isSelected,
                onClick = { onSongClick(song.id) },
                onFocus = {
                    selectedIndex = index
                    onFocusChange(song)
                }
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SongListItem(
    song: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use TvFocusableCard to handle clicks and focus
    TvFocusableCard(
        onClick = onClick,
        onFocus = onFocus,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = song.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.seriesName ?: "Unknown Artist",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
