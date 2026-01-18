package com.example.livetv.ScreensWithModels

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainviewModel = viewModel()
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsStateWithLifecycle()

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val filteredChannels = remember(searchQuery, channels) {
        if (searchQuery.isBlank()) channels
        else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val activity = context as? Activity
    val window = activity?.window
    val view = window?.decorView
    val controller = remember(window, view) {
        if (window != null && view != null) {
            androidx.core.view.WindowCompat.getInsetsController(window, view)
        } else null
    }

    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Root Container
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. LIST LAYER (Only visible if NOT fullscreen)
            if (!isFullscreen) {
                Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    // This spacer keeps the list below the 250.dp player
                    Spacer(modifier = Modifier.height(250.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        placeholder = { Text("Search sports channels") },
                        singleLine = true
                    )

                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(filteredChannels) { channel ->
                            Text(
                                text = channel.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.playChannel(channel.url) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }

            // 2. PLAYER LAYER (Dynamic Size)
            Box(
                modifier = if (isFullscreen) {
                    Modifier.fillMaxSize().background(Color.Black)
                } else {
                    Modifier.fillMaxWidth().height(250.dp).background(Color.Black)
                }
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = viewModel.player
                            useController = true
                            // Important for sizing
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Green
                    )
                }

                // 🔥 FULLSCREEN BUTTON (Moved inside the specific Player Box)
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .zIndex(5f) // High zIndex within the Box
                ) {
                    Text(
                        text = if (isFullscreen) "EXIT FULL" else "FULL SCREEN",
                        color = Color.White,
                        modifier = Modifier
                            .clickable { isFullscreen = !isFullscreen }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}