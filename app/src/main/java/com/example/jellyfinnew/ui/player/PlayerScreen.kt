package com.example.jellyfinnew.ui.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text

@OptIn(ExperimentalTvMaterial3Api::class, UnstableApi::class)
@Composable
fun PlayerScreen(
    streamUrl: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var showControls by remember { mutableStateOf(true) }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    exoPlayer.release()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Video player
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false // We'll create our own controls
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        // Custom controls overlay
        if (showControls) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                // Back button
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text("← Back")
                }
                
                // Play/Pause controls
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Color.Black.copy(alpha = 0.6f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                        }
                    ) {
                        Text("⏪ 10s")
                    }
                    
                    Button(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                        }
                    ) {
                        Text(if (exoPlayer.isPlaying) "⏸️" else "▶️")
                    }
                    
                    Button(
                        onClick = {
                            exoPlayer.seekTo(exoPlayer.currentPosition + 30000)
                        }
                    ) {
                        Text("30s ⏩")
                    }
                }
            }
        }
    }
    
    // Auto-hide controls after a few seconds
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }
    
    // Show controls on tap
    LaunchedEffect(Unit) {
        // This would typically be handled by gesture detection
        // For now, we'll just show controls initially
    }
}
