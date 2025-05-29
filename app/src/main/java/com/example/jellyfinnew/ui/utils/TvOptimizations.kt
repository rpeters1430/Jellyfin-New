package com.example.jellyfinnew.ui.utils

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.CardDefaults
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.ui.utils.ImageCacheManager
import kotlinx.coroutines.delay

/**
 * TV-specific optimizations for better performance and UX
 */
object TvOptimizations {
    
    /**
     * Optimized card scaling for TV - reduced animation complexity
     */
    @OptIn(ExperimentalTvMaterial3Api::class)
    val optimizedCardScale = CardDefaults.scale(
        scale = 1.0f,
        focusedScale = 1.02f // Much smaller scale change for better performance
    )
    
    /**
     * Fast animation specs for TV navigation
     */
    val fastAnimation = tween<Float>(
        durationMillis = 150, // Faster transitions
        easing = LinearEasing
    )
    
    /**
     * Background image with debounced updates to prevent performance issues
     */
    @Composable
    fun DebouncedBackgroundImage(
        imageUrl: String?,
        modifier: Modifier = Modifier,
        debounceMs: Long = 500L
    ) {
        var debouncedImageUrl by remember { mutableStateOf<String?>(null) }
        
        LaunchedEffect(imageUrl) {
            if (imageUrl != null) {
                delay(debounceMs)
                debouncedImageUrl = imageUrl
            }
        }
        
        debouncedImageUrl?.let { url ->
            val imageRequest = ImageCacheManager.createOptimizedImageRequest(
                url = url,
                enableCrossfade = false
            )
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = null,
                    modifier = modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val gradient = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                            onDrawBehind {
                                drawRect(gradient)
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
    
    /**
     * TV-optimized focus handling with reduced re-compositions
     */
    @Composable
    fun Modifier.tvFocusState(
        onFocusChange: (Boolean) -> Unit
    ): Modifier {
        var isFocused by remember { mutableStateOf(false) }
        
        return this.onFocusChanged { focusState ->
            val focused = focusState.isFocused
            if (isFocused != focused) {
                isFocused = focused
                onFocusChange(focused)
            }
        }.focusable()
    }
    
    /**
     * Optimized image loading with reduced memory footprint
     */
    @Composable
    fun OptimizedAsyncImage(
        imageUrl: String?,
        contentDescription: String? = null,
        modifier: Modifier = Modifier,
        contentScale: ContentScale = ContentScale.Crop
    ) {
        if (imageUrl != null) {
            val context = LocalContext.current
            AsyncImage(
                model = remember(imageUrl) {
                    ImageRequest.Builder(context)
                        .data(imageUrl)
                        .memoryCacheKey(imageUrl)
                        .diskCacheKey(imageUrl)
                        .crossfade(false) // Disable for performance
                        .allowHardware(true)
                        .size(coil.size.Size.ORIGINAL) // Let Coil handle sizing
                        .build()
                },
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
    }
}

/**
 * Reduced padding and spacing for better TV layout
 */
object TvSpacing {
    val small = 8.dp
    val medium = 12.dp
    val large = 16.dp
    val xlarge = 20.dp
}

/**
 * TV-optimized list spacing to reduce scroll lag
 */
object TvListDefaults {
    val itemSpacing = 12.dp
    val sectionSpacing = 20.dp
    val contentPadding = 16.dp
}

/**
 * Composable for optimized TV card performance
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PerformantTvCard(
    onClick: () -> Unit,
    onFocus: () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = modifier,
        scale = TvOptimizations.optimizedCardScale,
        content = {
            LaunchedEffect(Unit) {
                onFocus()
            }
            content()
        }
    )
}
