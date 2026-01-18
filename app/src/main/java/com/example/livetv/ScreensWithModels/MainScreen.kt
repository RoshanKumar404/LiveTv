package com.example.livetv.ScreensWithModels


import android.app.Activity
import android.content.pm.ActivityInfo

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView



@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainviewModel = viewModel()
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsStateWithLifecycle()
//this too
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val filteredChannels = remember(searchQuery, channels) {
        if (searchQuery.isBlank()) channels
        else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val player = remember {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0")
            .setAllowCrossProtocolRedirects(true)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
    }

    val activity = context as Activity
    val window = activity.window
    val view = activity.window.decorView
    val controller = remember(window, view) {
        androidx.core.view.WindowCompat.getInsetsController(window, view)
    }

    DisposableEffect(isFullscreen) {
        if (isFullscreen) {
            // Lock to Landscape
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            // Hide both Status Bar and Navigation Bar
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Lock to Portrait
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            // Show the bars back
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }

        onDispose {
            // Reset if the composable leaves the screen
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Player Listener
    DisposableEffect(player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == androidx.media3.common.Player.STATE_BUFFERING
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isLoading = false
                Toast.makeText(context, "Stream Offline", Toast.LENGTH_SHORT).show()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // ROOT BOX = Critical thing for Fullscreen
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // 1. CONTENT LAYER (Search + List) - Hidden if Fullscreen
        if (!isFullscreen) {
            Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                // Placeholder space where the player usually sits
                Box(modifier = Modifier.fillMaxWidth().height(250.dp))

                androidx.compose.material3.OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    placeholder = { Text("Search sports channels") },
                    singleLine = true
                )
// here we have to do some changes**********************************
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(filteredChannels) { channel ->
                        Text(
                            text = channel.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isLoading = true
                                    player.setMediaItem(MediaItem.fromUri(channel.url))
                                    player.prepare()
                                    player.play()
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }

        // 2. PLAYER LAYER - Stays on top
        Box(
            modifier = if (isFullscreen) {
                Modifier.fillMaxSize().background(Color.Black)
            } else {
                Modifier.padding(top = 0.dp).fillMaxWidth().height(250.dp)
            },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        this.keepScreenOn = true
                        this.useController = true
                        this.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.Green)
            }

            // Fullscreen Toggle Button
            Text(
                text = if (isFullscreen) "EXIT" else "FULL",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { isFullscreen = !isFullscreen }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = Color.White
            )
        }
    }
}