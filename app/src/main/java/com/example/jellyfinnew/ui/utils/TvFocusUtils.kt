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
 * TV-optimized focus management utilities for better D-pad navigation
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
 * Enhanced LazyRow with better focus handling and auto-scroll
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
    var focusedIndex by remember { mutableIntStateOf(0) }

    // Auto-scroll to focused item
    LaunchedEffect(focusedIndex) {
        if (focusedIndex in items.indices) {
            state.animateScrollToItem(focusedIndex)
        }
    }

    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement
    ) {
        itemsIndexed(items) { index, item ->
            val isFocused = index == focusedIndex
            val focusRequester = remember { FocusRequester() }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            focusedIndex = index
                            onItemFocus(index, item)
                        }
                    }
                    .focusable()
            ) {
                itemContent(index, item, isFocused)
            }
        }
    }
}

/**
 * TV-optimized card that handles focus states smoothly
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
    content: @Composable ((Boolean) -> Unit)
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Card(
        onClick = onClick,
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                val wasFocused = isFocused
                isFocused = focusState.isFocused

                when {
                    !wasFocused && isFocused -> onFocus()
                    wasFocused && !isFocused -> onUnfocus()
                }
            }
            .onKeyEvent { keyEvent ->
                TvFocusUtils.handleDpadNavigation(
                    keyEvent = keyEvent,
                    onCenter = {
                        onClick()
                        true
                    }
                )
            },
        shape = shape,
        colors = colors,
        scale = scale,
        border = border
    ) {
        content(isFocused)
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
    // Only recreate state when the list structure significantly changes
    val stableKey = remember(items.size) {
        items.take(5).joinToString { keyExtractor(it) }
    }

    return remember(stableKey) {
        LazyListState()
    }
}