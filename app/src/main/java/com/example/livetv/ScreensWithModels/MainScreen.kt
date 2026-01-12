package com.example.livetv.ScreensWithModels

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.nio.file.WatchEvent


@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainviewModel = viewModel()
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    // State to track if the video is currently loading
    var isLoading by remember { mutableStateOf(false) }

    val filteredChannels = remember(searchQuery, channels) {
        if (searchQuery.isEmpty()) channels
        else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val player = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setAllowCrossProtocolRedirects(true)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build().apply {
                // ADDED: Listener to update the isLoading state
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        isLoading = (state == androidx.media3.common.Player.STATE_BUFFERING)
                    }
                })
            }
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column {
        // 1. Player Area with Spinner Overlay
        Box(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth()
                .height(250.dp),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        this.keepScreenOn = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Show spinner if isLoading is true
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = androidx.compose.ui.graphics.Color.White,
                    strokeWidth = 4.dp
                )
            }
        }

        // 2. Search Bar
        androidx.compose.material3.OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search sports channels...") },
            singleLine = true
        )

        // 3. Channel List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(filteredChannels) { channel ->
                Text(
                    text = channel.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isLoading = true // Start spinner immediately on click
                            val mediaItem = MediaItem.Builder()
                                .setUri(channel.url)
                                .build()
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.play()
                        }
                        .padding(16.dp)
                )
            }
        }
    }
}