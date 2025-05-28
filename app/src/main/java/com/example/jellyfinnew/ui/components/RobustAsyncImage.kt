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
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    enableRetry: Boolean = true,
    maxRetries: Int = JellyfinConfig.Performance.MAX_RETRIES,
    showPlaceholder: Boolean = true,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    var retryCount by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var imageState by remember { mutableStateOf<AsyncImagePainter.State?>(null) }

    // Reset state when URL changes
    LaunchedEffect(imageUrl) {
        retryCount = 0
        isLoading = true
        hasError = false
        imageState = null
    }

    Box(modifier = modifier) {
        when {
            imageUrl.isNullOrEmpty() -> {
                // No image URL - show placeholder
                if (showPlaceholder) {
                    SkeletonPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        color = placeholderColor
                    )
                }
            }
            
            else -> {
                val imageRequest = ImageCacheManager.createOptimizedImageRequest(
                    url = imageUrl,
                    contentDescription = contentDescription,
                    enableCrossfade = false
                )

                if (imageRequest != null) {
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = contentDescription,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = contentScale,
                        onState = { state ->
                            imageState = state
                            when (state) {
                                is AsyncImagePainter.State.Loading -> {
                                    isLoading = true
                                    hasError = false
                                }
                                is AsyncImagePainter.State.Success -> {
                                    isLoading = false
                                    hasError = false
                                    retryCount = 0
                                }
                                is AsyncImagePainter.State.Error -> {
                                    isLoading = false
                                    hasError = true
                                }
                                else -> {
                                    isLoading = false
                                }
                            }
                        }
                    )
                }
            }
        }

        // Loading skeleton overlay
        if (isLoading && showPlaceholder) {
            SkeletonPlaceholder(
                modifier = Modifier.fillMaxSize(),
                isAnimated = true
            )
        }

        // Error state with retry option
        if (hasError && enableRetry && retryCount < maxRetries) {
            ErrorStateWithRetry(
                modifier = Modifier.fillMaxSize(),
                onRetry = {
                    retryCount++
                    isLoading = true
                    hasError = false
                },
                retryCount = retryCount,
                maxRetries = maxRetries
            )
        } else if (hasError && showPlaceholder) {
            // Final error state - no more retries
            ErrorPlaceholder(
                modifier = Modifier.fillMaxSize(),
                color = placeholderColor
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
    maxRetries: Int
) {
    val coroutineScope = rememberCoroutineScope()
    
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
                text = "Failed to load",
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
    color: Color = MaterialTheme.colorScheme.errorContainer
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No Image",
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}