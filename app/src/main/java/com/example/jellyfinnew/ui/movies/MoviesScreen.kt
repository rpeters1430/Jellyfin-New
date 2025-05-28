package com.example.jellyfinnew.ui.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
fun MoviesScreen(
    viewModel: MoviesViewModel,
    libraryId: String,
    onMovieClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val movies by viewModel.filteredMovies.collectAsStateWithLifecycle()
    val movieGenres by viewModel.movieGenres.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val focusedMovie by viewModel.focusedMovie.collectAsStateWithLifecycle()
    
    var focusedItemImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(libraryId) {
        viewModel.loadMovies(libraryId)
    }

    // Update background image when focused movie changes
    LaunchedEffect(focusedMovie) {
        focusedItemImageUrl = focusedMovie?.backdropUrl ?: focusedMovie?.imageUrl
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
                        text = "Movies",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Genre filter chips
                if (movieGenres.isNotEmpty()) {
                    GenreFilterRow(
                        genres = movieGenres,
                        selectedGenre = selectedGenre,
                        onGenreSelected = { genre ->
                            if (genre == selectedGenre) {
                                viewModel.clearGenreFilter(libraryId)
                            } else {
                                viewModel.loadMoviesByGenre(libraryId, genre)
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
            } else if (movies.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedGenre != null) {
                            "No movies found for genre: $selectedGenre"
                        } else {
                            "No movies found"
                        },
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            } else {
                // Immersive List for Movies with vertical cards
                ImmersiveMoviesList(
                    movies = movies,
                    onMovieClick = onMovieClick,
                    onFocusChange = { movie ->
                        viewModel.updateFocusedMovie(movie)
                        focusedItemImageUrl = movie?.backdropUrl ?: movie?.imageUrl
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
private fun ImmersiveMoviesList(
    movies: List<MediaItem>,
    onMovieClick: (String) -> Unit,
    onFocusChange: (MediaItem?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val selectedMovie = if (movies.isNotEmpty() && selectedIndex < movies.size) {
        movies[selectedIndex]
    } else null

    // Trigger focus for the initially selected item
    LaunchedEffect(selectedMovie) {
        onFocusChange(selectedMovie)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Details section for focused item (top 45% of screen)
        selectedMovie?.let { movie ->
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
                        text = movie.name,
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
                        movie.productionYear?.let { year ->
                            Text(
                                text = year.toString(),
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        movie.communityRating?.let { rating ->
                            Text(
                                text = "★ ${String.format(Locale.getDefault(), "%.1f", rating)}",
                                fontSize = 16.sp,
                                color = Color.Yellow
                            )
                        }

                        // Runtime if available
                        movie.runTimeTicks?.let { ticks ->
                            val minutes = (ticks / 10_000_000 / 60).toInt()
                            if (minutes > 0) {
                                Text(
                                    text = "${minutes}m",
                                    fontSize = 16.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    movie.overview?.let { overview ->
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

        // Movies vertical grid (bottom 55% of screen)
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
                items(movies.size) { index ->
                    val movie = movies[index]
                    val isSelected = index == selectedIndex

                    VerticalMovieCard(
                        movie = movie,
                        isSelected = isSelected,
                        onClick = { onMovieClick(movie.id) },
                        onFocus = {
                            selectedIndex = index
                            onFocusChange(movie)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun VerticalMovieCard(
    movie: MediaItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use the new UnifiedMediaCard with optimized caching
    UnifiedMediaCard(
        mediaItem = movie,
        onClick = onClick,
        modifier = modifier,
        onFocus = onFocus,
        cardType = MediaCardType.POSTER,
        showProgress = true,
        showOverlay = false // Don't show overlay text for movies grid
    )
}
