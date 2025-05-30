package com.example.jellyfinnew.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreenSimple(
    viewModel: HomeViewModel,
    onPlayMedia: (String) -> Unit,
    onDisconnect: () -> Unit,
    onFocusChange: (String?) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when {
            connectionState.isLoading -> {
                Text("Loading...")
            }
            connectionState.error != null -> {
                Text("Error: ${connectionState.error}")
            }
            else -> {
                // Simple LazyColumn without complex structures
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("Jellyfin Home")
                    }
                    
                    item {
                        Text("Connected successfully!")
                    }
                }
            }
        }
    }
}
