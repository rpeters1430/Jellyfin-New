package com.example.jellyfinnew.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.ui.components.UnifiedMediaCard
import com.example.jellyfinnew.ui.components.MediaCardType

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ArtistsScreen(
    viewModel: MusicViewModel,
    libraryId: String,
    onArtistClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artists by viewModel.filteredArtists.collectAsStateWithLifecycle()
    val musicGenres by viewModel.musicGenres.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val focusedArtist by viewModel.focusedArtist.collectAsStateWithLifecycle()
    
    var focusedItemImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(libraryId) {
        viewModel.loadArtists(libraryId)
    }

    // Update background image when focused artist changes
    LaunchedEffect(focusedArtist) {
        focusedItemImageUrl = focusedArtist?.backdropUrl ?: focusedArtist?.imageUrl
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
            // Header with back button and genre filters
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

                    Text(
                        text = "Artists",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Genre filter chips
                if (musicGenres.isNotEmpty()) {
                    GenreFilterRow(
                        genres = musicGenres,
                        selectedGenre = selectedGenre,
                        onGenreSelected = { genre ->
                            if (genre == selectedGenre) {
                                viewModel.clearGenreFilter(libraryId)
                            } else {
                                viewModel.loadArtistsByGenre(libraryId, genre)
                            }
                        },
                        onClearFilter = {
                            viewModel.clearGenreFilter(libraryId)
                        }
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (artists.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedGenre != null) {
                            "No artists found for genre: $selectedGenre"
                        } else {
                            "No artists found"
                        },
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Immersive List for Artists with circular cards
                ImmersiveArtistsList(
                    artists = artists,
                    onArtistClick = onArtistClick,
                    onFocusChange = { artist ->
                        viewModel.updateFocusedArtist(artist)
                        focusedItemImageUrl = artist?.backdropUrl ?: artist?.imageUrl
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GenreFilterRow(
    genres: List<String>,
    selectedGenre: String?,
    onGenreSelected: (String) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // "All" filter chip
        item {            FilterChip(
                selected = selectedGenre == null,
                onClick = onClearFilter,
                colors = FilterChipDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("All")
            }
        }

        // Genre filter chips
        items(genres) { genre ->            FilterChip(
                selected = selectedGenre == genre,
                onClick = { onGenreSelected(genre) },
                colors = FilterChipDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(genre)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ImmersiveArtistsList(
    artists: List<MediaItem>,
    onArtistClick: (String) -> Unit,
    onFocusChange: (MediaItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedArtist = if (artists.isNotEmpty() && selectedIndex < artists.size) {
        artists[selectedIndex]
    } else null

    // Trigger focus for the initially selected item
    LaunchedEffect(selectedArtist) {
        onFocusChange(selectedArtist)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Details section for focused item (top 40% of screen)
        selectedArtist?.let { artist ->
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
                        text = artist.name,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    artist.overview?.let { overview ->
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

        // Artists circular grid (bottom 60% of screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5), // 5 columns of circular artist images
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items(artists.size) { index ->
                    val artist = artists[index]
                    val isSelected = index == selectedIndex

                    CircularArtistCard(
                        artist = artist,
                        isSelected = isSelected,
                        onClick = { onArtistClick(artist.id) },
                        onFocus = {
                            selectedIndex = index
                            onFocusChange(artist)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun CircularArtistCard(
    artist: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Use UnifiedMediaCard with SQUARE type for circular artist images
        UnifiedMediaCard(
            mediaItem = artist,
            onClick = onClick,
            modifier = Modifier
                .aspectRatio(1f)
                .clip(CircleShape), // Make it circular
            onFocus = onFocus,
            cardType = MediaCardType.SQUARE,
            showProgress = false,
            showOverlay = false
        )
        
        // Artist name below the image
        Text(
            text = artist.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        )
    }
}
