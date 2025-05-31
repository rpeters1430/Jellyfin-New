package com.example.jellyfinnew.ui.utils

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardBorder
import androidx.tv.material3.CardColors
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.CardShape
import androidx.tv.material3.ExperimentalTvMaterial3Api

/**
 * Fixed TV-optimized focus management utilities
 */
object TvFocusUtils {

    fun handleDpadNavigation(
        keyEvent: KeyEvent,
        onUp: () -> Boolean = { false },
        onDown: () -> Boolean = { false },
        onLeft: () -> Boolean = { false },
        onRight: () -> Boolean = { false },
        onCenter: () -> Boolean = { false }
    ): Boolean {
        if (keyEvent.type != KeyEventType.KeyUp) return false

        return when (keyEvent.key) {
            Key.DirectionUp -> onUp()
            Key.DirectionDown -> onDown()
            Key.DirectionLeft -> onLeft()
            Key.DirectionRight -> onRight()
            Key.DirectionCenter, Key.Enter -> onCenter()
            else -> false
        }
    }
}

/**
 * Fixed TV-optimized card with proper Compose structure
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvFocusableCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocus: () -> Unit = {},
    onUnfocus: () -> Unit = {},
    shape: CardShape = CardDefaults.shape(),
    colors: CardColors = CardDefaults.colors(),
    scale: CardScale = CardDefaults.scale(),
    border: CardBorder = CardDefaults.border(),
    content: @Composable (Boolean) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { focusState ->
                val newFocused = focusState.isFocused
                if (isFocused != newFocused) {
                    isFocused = newFocused
                    if (newFocused) {
                        onFocus()
                    } else {
                        onUnfocus()
                    }
                }
            },
        shape = shape,
        colors = colors,
        scale = scale,
        border = border
    ) {
        // Use stable composition for content
        key(isFocused) {
            content(isFocused)
        }
    }
}

/**
 * Simplified LazyRow with better focus handling
 */
@Composable
fun <T> FocusAwareLazyRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    horizontalArrangement: androidx.compose.foundation.layout.Arrangement.Horizontal = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    onItemFocus: (Int, T) -> Unit = { _, _ -> },
    itemContent: @Composable (Int, T, Boolean) -> Unit
) {
    var focusedIndex by remember { mutableIntStateOf(-1) }

    // Auto-scroll to focused item with safety check
    LaunchedEffect(focusedIndex) {
        if (focusedIndex in 0 until items.size) {
            state.animateScrollToItem(focusedIndex)
        }
    }

    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement
    ) {
        itemsIndexed(
            items = items,
            key = { index, item -> "${index}-${item.hashCode()}" }
        ) { index, item ->
            val isFocused = index == focusedIndex

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused && focusedIndex != index) {
                            focusedIndex = index
                            onItemFocus(index, item)
                        } else if (!focusState.isFocused && focusedIndex == index) {
                            // Item lost focus but we only update if no other item gained it
                        }
                    }
                    .focusable()
            ) {
                // Use key for stable composition
                key("item-$index") {
                    itemContent(index, item, isFocused)
                }
            }
        }
    }
}

/**
 * Remember lazy list state with better memory management
 */
@Composable
fun <T> rememberOptimizedLazyListState(
    items: List<T>,
    keyExtractor: (T) -> String = { it.toString() }
): LazyListState {
    // Create stable key based on list structure
    val stableKey = remember(items.size) {
        if (items.isNotEmpty()) {
            "${items.size}-${items.take(3).joinToString { keyExtractor(it) }}"
        } else {
            "empty"
        }
    }

    return remember(stableKey) {
        LazyListState()
    }
}