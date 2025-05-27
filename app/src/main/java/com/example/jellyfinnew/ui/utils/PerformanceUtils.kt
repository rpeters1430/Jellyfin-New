package com.example.jellyfinnew.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.repeatOnLifecycle
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

/**
 * Performance utilities for TV-optimized media loading and state management
 */
object PerformanceUtils {

    /**
     * Creates an optimized ImageLoader for TV with proper caching
     */
    fun createOptimizedImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Jellyfin images don't always have proper cache headers
            .build()
    }

    /**
     * Creates optimized image request for TV viewing
     */
    fun createOptimizedImageRequest(
        context: Context,
        url: String?,
        crossfadeEnabled: Boolean = true
    ): ImageRequest {
        return ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(crossfadeEnabled)
            .build()
    }
}

/**
 * Lifecycle-aware state collection that automatically manages subscription
 */
@Composable
fun <T> StateFlow<T>.collectAsLifecycleAwareState(): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    return collectAsState(
        context = remember {
            kotlinx.coroutines.Dispatchers.Main.immediate +
                    kotlinx.coroutines.SupervisorJob()
        }
    )
}

/**
 * Memory leak prevention for ViewModels
 */
@Composable
fun <T> Flow<T>.collectWithLifecycle(
    lifecycle: Lifecycle = LocalLifecycleOwner.current.lifecycle,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit
) {
    LaunchedEffect(this, lifecycle, minActiveState) {
        lifecycle.repeatOnLifecycle(minActiveState) {
            this@collectWithLifecycle.collectLatest(collector)
        }
    }
}

/**
 * Resource cleanup for TV lifecycle
 */
@Composable
fun OnLifecycleEvent(
    onStart: () -> Unit = {},
    onStop: () -> Unit = {},
    onDestroy: () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> onStart()
                Lifecycle.Event.ON_STOP -> onStop()
                Lifecycle.Event.ON_DESTROY -> onDestroy()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * TV-optimized error handling
 */
sealed class TvError {
    object NetworkError : TvError()
    object AuthenticationError : TvError()
    object ServerError : TvError()
    data class UnknownError(val message: String) : TvError()

    fun getUserFriendlyMessage(): String = when (this) {
        NetworkError -> "Connection failed. Check your network and try again."
        AuthenticationError -> "Login failed. Check your credentials."
        ServerError -> "Server error. Please try again later."
        is UnknownError -> message
    }
}

/**
 * Extensions for better TV UX
 */
fun Long.formatDuration(): String {
    val seconds = this / 10_000_000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60

    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "< 1m"
    }
}

fun Double.formatRating(): String = String.format(Locale.getDefault(), "%.1f", this)

/**
 * Debounced search/filter utility for TV input
 */
class DebouncedCallback<T>(
    private val delayMs: Long = 300L,
    private val callback: suspend (T) -> Unit
) {
    private var lastCallTime = 0L
    private var pendingValue: T? = null

    suspend fun call(value: T) {
        pendingValue = value
        val currentTime = System.currentTimeMillis()
        lastCallTime = currentTime

        kotlinx.coroutines.delay(delayMs)

        // Only execute if this is still the latest call
        if (lastCallTime == currentTime && pendingValue == value) {
            callback(value)
        }
    }
}