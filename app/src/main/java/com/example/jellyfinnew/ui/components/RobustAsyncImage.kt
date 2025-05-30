package com.example.jellyfinnew.ui.components

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

    val imageUrl = imageUrls.firstOrNull()
    if (imageUrl.isNullOrEmpty()) {
        // Log error if image URL is empty or invalid
        println("Error: Image URL is empty or invalid")
    }

    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = painterResource(id = R.drawable.placeholder),
        error = painterResource(id = R.drawable.error_placeholder)
    )
}
