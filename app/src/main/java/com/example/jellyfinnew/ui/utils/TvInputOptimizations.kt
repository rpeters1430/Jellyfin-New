package com.example.jellyfinnew.ui.utils

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*

/**
 * TV Remote input handling optimizations
 */
object TvInputOptimizations {
      /**
     * Optimized key event handling for TV remote
     */
    fun Modifier.tvKeyHandler(
        onBack: (() -> Boolean)? = null,
        onSelect: (() -> Boolean)? = null,
        onUp: (() -> Boolean)? = null,
        onDown: (() -> Boolean)? = null,
        onLeft: (() -> Boolean)? = null,
        onRight: (() -> Boolean)? = null
    ): Modifier = this.onKeyEvent { keyEvent ->
        when {
            keyEvent.type != KeyEventType.KeyUp -> false
            keyEvent.key == Key.Back && onBack != null -> onBack()
            keyEvent.key == Key.Enter && onSelect != null -> onSelect()
            keyEvent.key == Key.DirectionCenter && onSelect != null -> onSelect()
            keyEvent.key == Key.DirectionUp && onUp != null -> onUp()
            keyEvent.key == Key.DirectionDown && onDown != null -> onDown()
            keyEvent.key == Key.DirectionLeft && onLeft != null -> onLeft()
            keyEvent.key == Key.DirectionRight && onRight != null -> onRight()
            else -> false
        }
    }
    
    /**
     * Debounced focus handling to reduce performance impact
     */
    @Composable
    fun rememberDebouncedFocusHandler(
        onFocusChanged: (Boolean) -> Unit,
        debounceMs: Long = 100L
    ): Modifier {
        var isFocused by remember { mutableStateOf(false) }
        
        LaunchedEffect(isFocused) {
            if (debounceMs > 0) {
                kotlinx.coroutines.delay(debounceMs)
            }
            onFocusChanged(isFocused)
        }
        
        return Modifier.onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        }
    }
    
    /**
     * Optimized focusable modifier with reduced overhead
     */
    fun Modifier.tvFocusable(
        enabled: Boolean = true
    ): Modifier = if (enabled) {
        this.focusable()
    } else {
        this
    }
}

/**
 * Memory management for TV apps
 */
object TvMemoryOptimizations {
    
    /**
     * Cleanup unused resources
     */
    @Composable
    fun MemoryCleanupEffect() {
        DisposableEffect(Unit) {
            onDispose {
                // Force garbage collection on disposal
                System.gc()
            }
        }
    }
    
    /**
     * Reduce re-compositions with stable keys
     */
    @Composable
    fun <T> rememberStableKey(
        vararg inputs: Any?
    ): String {
        return remember(*inputs) {
            inputs.joinToString(separator = "_") { it?.hashCode()?.toString() ?: "null" }
        }
    }
}
