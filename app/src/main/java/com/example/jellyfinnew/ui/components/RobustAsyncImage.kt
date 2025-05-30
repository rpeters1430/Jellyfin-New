package com.example.jellyfinnew.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.example.jellyfinnew.data.JellyfinConfig
import com.example.jellyfinnew.ui.utils.ImageCacheManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Robust AsyncImage component with enhanced error handling, retry logic,
 * and skeleton loading states for better user experience
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RobustAsyncImage(
    imageUrls: List<String?>, // Changed from imageUrl: String?
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    enableRetry: Boolean = true,
    maxRetries: Int = JellyfinConfig.Performance.MAX_RETRIES,
    showPlaceholder: Boolean = true,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onStateChange: ((AsyncImagePainter.State) -> Unit)? = null
) {
    var currentUrlIndex by remember(imageUrls) { mutableIntStateOf(0) }
    var retryCountForCurrentUrl by remember(imageUrls, currentUrlIndex) { mutableIntStateOf(0) }
    var isLoading by remember(imageUrls, currentUrlIndex) { mutableStateOf(true) }
    var hasErrorForCurrentUrl by remember(imageUrls, currentUrlIndex) { mutableStateOf(false) }
    var currentImageState by remember(imageUrls, currentUrlIndex) { mutableStateOf<AsyncImagePainter.State?>(null) }

    val currentImageUrl = imageUrls.getOrNull(currentUrlIndex)?.takeIf { it.isNotBlank() }

    // Reset state when imageUrls list or currentUrlIndex changes (e.g. trying next URL)
    LaunchedEffect(imageUrls, currentUrlIndex) {
        retryCountForCurrentUrl = 0
        isLoading = true
        hasErrorForCurrentUrl = false
        currentImageState = null
    }

    Box(modifier = modifier) {
        if (currentImageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentImageUrl)
                    .crossfade(JellyfinConfig.Performance.IMAGE_CROSSFADE_ENABLED) // Assuming this constant exists
                    .placeholder(if (showPlaceholder) com.example.jellyfinnew.R.drawable.ic_placeholder_image else null) // Use null for no placeholder
                    .error(com.example.jellyfinnew.R.drawable.ic_error_image) // Generic error for Coil
                    .build(),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    currentImageState = state
                    isLoading = state is AsyncImagePainter.State.Loading
                    hasErrorForCurrentUrl = state is AsyncImagePainter.State.Error
                    onStateChange?.invoke(state)

                    if (state is AsyncImagePainter.State.Error) {
                        if (retryCountForCurrentUrl >= maxRetries) { // Retries for this URL exhausted
                            if (currentUrlIndex < imageUrls.size - 1) { // More URLs to try
                                currentUrlIndex++ // Move to next URL
                            }
                            // If no more URLs, final error state will be shown below
                        }
                    }
                }
            )
        } else { // currentImageUrl is null or blank, or we've run out of URLs
            LaunchedEffect(Unit) { // Ensure isLoading is false and hasError is true for placeholder
                isLoading = false
                hasErrorForCurrentUrl = true // Treat as error to show final placeholder
            }
        }

        if (isLoading && showPlaceholder && currentImageUrl != null) {
            SkeletonPlaceholder(
                modifier = Modifier.fillMaxSize(),
                isAnimated = true
            )
        }

        val canRetryCurrentUrl = enableRetry && retryCountForCurrentUrl < maxRetries
        val isLastUrl = currentUrlIndex >= imageUrls.size - 1 || imageUrls.drop(currentUrlIndex + 1).all { it.isNullOrBlank() }

        if (hasErrorForCurrentUrl && currentImageUrl != null) {
            if (canRetryCurrentUrl) {
                ErrorStateWithRetry(
                    modifier = Modifier.fillMaxSize(),
                    onRetry = {
                        retryCountForCurrentUrl++
                        // isLoading = true; hasErrorForCurrentUrl = false; // Will be handled by AsyncImage state change
                    },
                    retryCount = retryCountForCurrentUrl,
                    maxRetries = maxRetries,
                    imageState = currentImageState,
                    imageUrl = currentImageUrl // Pass currentImageUrl for context
                )
            } else if (!isLastUrl && currentUrlIndex < imageUrls.size -1) {
                // Current URL failed terminally, and we are about to try the next one.
                // Show loading for a brief moment or rely on LaunchedEffect for next URL.
                // This state is transient as currentUrlIndex will increment.
                // To avoid flicker, we can show a generic placeholder or rely on the next URL's loading state.
                 if (showPlaceholder) {
                    SkeletonPlaceholder(modifier = Modifier.fillMaxSize(), isAnimated = false) // Static placeholder
                 }
            } else { // All retries for current URL exhausted, and no more URLs or next URL is null/blank
                ErrorPlaceholder(
                    modifier = Modifier.fillMaxSize(),
                    color = placeholderColor,
                    imageState = currentImageState
                )
            }
        } else if (hasErrorForCurrentUrl && currentImageUrl == null && showPlaceholder) {
            // This case handles when all URLs are null or exhausted from the start
             ErrorPlaceholder(
                modifier = Modifier.fillMaxSize(),
                color = placeholderColor,
                imageState = null // No specific image state if URL was null
            )
        }
    }
}

@Composable
private fun SkeletonPlaceholder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    isAnimated: Boolean = false
) {
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isAnimated) 0.3f else 0.6f,
        animationSpec = if (isAnimated) {
            infiniteRepeatable(
                animation = tween(1000),
                repeatMode = RepeatMode.Reverse
            )
        } else {
            snap()
        },
        label = "skeleton_alpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = animatedAlpha))
    )
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ErrorStateWithRetry(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
    retryCount: Int,
    maxRetries: Int,
    imageState: AsyncImagePainter.State?,
    imageUrl: String? // Added imageUrl for context
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Determine error message based on the error
    val errorMessage = when {
        imageState is AsyncImagePainter.State.Error -> {
            val throwable = imageState.result.throwable
            when {
                throwable.message?.contains("401") == true -> "Auth Error"
                throwable.message?.contains("404") == true -> "Not Found"
                throwable.message?.contains("403") == true -> "Access Denied"
                else -> "Failed to load"
            }
        }
        else -> "Failed to load"
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Retry ${retryCount}/${maxRetries}",
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
            
            Button(
                onClick = {
                    // Add slight delay before retry to avoid overwhelming the server
                    coroutineScope.launch {
                        delay(500)
                        onRetry()
                    }
                }
            ) {
                Text("Retry", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ErrorPlaceholder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.errorContainer,
    imageState: AsyncImagePainter.State?
) {
    // Determine error message based on the error
    val errorMessage = when {
        imageState is AsyncImagePainter.State.Error -> {
            val throwable = imageState.result.throwable
            when {
                throwable.message?.contains("401") == true -> "Auth Error"
                throwable.message?.contains("404") == true -> "Not Found"
                throwable.message?.contains("403") == true -> "No Access"
                else -> "Load Failed"
            }
        }
        else -> "No Image"
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = errorMessage,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}