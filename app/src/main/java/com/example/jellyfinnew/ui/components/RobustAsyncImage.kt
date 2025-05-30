package com.example.jellyfinnew.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.jellyfinnew.R
import androidx.compose.ui.res.painterResource

/**
 * A robust image loader for Jetpack Compose using Coil.
 * Falls back to placeholder or error image if needed.
 */
@Composable
fun RobustAsyncImage(
    imageUrls: List<String>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    enableRetry: Boolean = false,
    showPlaceholder: Boolean = false
) {
    val context = LocalContext.current
    val tag = "RobustAsyncImage"

    val imageUrl = imageUrls.firstOrNull()
    if (imageUrl.isNullOrEmpty()) {
        // Log warning if image URL is empty or invalid
        Log.w(tag, "Image URL is empty or invalid for URLs: $imageUrls")
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .apply {
                // Add error handling and retry logic
                if (enableRetry) {
                    allowHardware(false)
                    allowRgb565(true)
                }
            }
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = if (showPlaceholder) {
            // Check if drawable exists, use null if not
            runCatching { painterResource(id = R.drawable.placeholder) }.getOrNull()
        } else null,
        error = runCatching { painterResource(id = R.drawable.error_placeholder) }.getOrNull(),
        onError = { state ->
            Log.e(tag, "Failed to load image: ${state.result.throwable?.message}", state.result.throwable)
            Log.e(tag, "Failed URL: $imageUrl")
        },
        onSuccess = { state ->
            Log.d(tag, "Successfully loaded image: $imageUrl")
        }
    )
}
