package com.example.jellyfinnew.ui.general

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.example.jellyfinnew.data.MediaItem
import com.example.jellyfinnew.ui.components.UnifiedMediaCard
import com.example.jellyfinnew.ui.components.MediaCardType
import com.example.jellyfinnew.ui.utils.TvOptimizations
import com.example.jellyfinnew.ui.utils.TvSpacing
import com.example.jellyfinnew.ui.home.components.LoadingState
import com.example.jellyfinnew.ui.home.components.ErrorState
import org.jellyfin.sdk.model.api.BaseItemKind

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun GeneralMediaScreen(
    viewModel: GeneralMediaViewModel,
    libraryId: String,
    onItemClick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val library by viewModel.library.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val focusedItem by viewModel.focusedItem.collectAsStateWithLifecycle()

    LaunchedEffect(libraryId) {
        viewModel.loadLibrary(libraryId)
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Optimized background image with debounced updates
        TvOptimizations.DebouncedBackgroundImage(
            imageUrl = focusedItem?.backdropUrl,
            debounceMs = 300L
        )

        when {
            uiState.isLoading -> {
                LoadingState(
                    message = "Loading ${library?.name ?: "library"} content...",
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.error != null -> {
                ErrorState(
                    error = uiState.error!!,
                    onRetry = { viewModel.loadLibrary(libraryId) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                GeneralMediaContent(
                    libraryName = library?.name ?: "Library",
                    items = items,
                    onItemClick = onItemClick,
                    onItemFocus = viewModel::setFocusedItem,
                    onBack = onBack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun GeneralMediaContent(
    libraryName: String,
    items: List<MediaItem>,
    onItemClick: (String) -> Unit,
    onItemFocus: (MediaItem?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(TvSpacing.large)
    ) {
        // Header with back button and title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TvSpacing.large)
        ) {
            Button(
                onClick = onBack,
                modifier = Modifier.onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        onItemFocus(null)
                    }
                }
            ) {
                Text("← Back")
            }

            Spacer(modifier = Modifier.width(TvSpacing.large))

            Text(
                text = libraryName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Content grid
        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items found in this library",
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 200.dp),
                contentPadding = PaddingValues(
                    horizontal = TvSpacing.medium,
                    vertical = TvSpacing.medium
                ),
                horizontalArrangement = Arrangement.spacedBy(TvSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(TvSpacing.large),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    GeneralMediaCard(
                        mediaItem = item,
                        onClick = { onItemClick(item.id) },
                        onFocus = { focused ->
                            if (focused) onItemFocus(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralMediaCard(
    mediaItem: MediaItem,
    onClick: () -> Unit,
    onFocus: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine the appropriate card type based on the media type
    val cardType = when (mediaItem.type) {
        BaseItemKind.PHOTO -> MediaCardType.SQUARE
        BaseItemKind.BOOK -> MediaCardType.POSTER
        BaseItemKind.AUDIO_BOOK -> MediaCardType.POSTER
        BaseItemKind.FOLDER -> MediaCardType.SQUARE
        BaseItemKind.COLLECTION_FOLDER -> MediaCardType.SQUARE
        else -> MediaCardType.POSTER
    }

    UnifiedMediaCard(
        mediaItem = mediaItem,
        onClick = onClick,
        modifier = modifier.onFocusChanged { focusState ->
            onFocus(focusState.isFocused)
        },
        cardType = cardType,
        showProgress = true,
        showOverlay = true
    )
}
