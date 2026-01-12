package com.example.livetv.ScreensWithModels

import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
    var search by remember{ mutableStateOf("") }
    val filterChannels= remember(search,channels){
       if (search.isEmpty()){
           channels
       }else{
           channels.filter { it.name.contains(search, ignoreCase = true) }
       }
    }

    // FIX: Wrap the player in remember so it isn't recreated on every scroll/touch
    var player = remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .setAllowCrossProtocolRedirects(true)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
            .build()
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        // player area
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                   this.player=player
                    keepScreenOn = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        )
        //code for search bar
        androidx.compose.material3.OutlinedTextField(
            value = search,
            onValueChange = {search=it},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = {Text("Search your channel..")},
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null,modifier = Modifier.padding(start = 8.dp))
            }
        )

        // 📜 Channel list (scrolls independently)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)   // ⭐ THIS IS THE KEY FIX
        ) {
            items(filterChannels) { channel ->
                Text(
                    text = channel.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val mediaItem = MediaItem.fromUri(channel.url)
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