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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage

@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainviewModel = viewModel()
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val filteredChannels = remember(searchQuery, channels) {
        if (searchQuery.isBlank()) channels
        else channels.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val activity = context.findActivity()
    val window = activity?.window
    val view = window?.decorView
    val controller = remember(window, view) {
        if (window != null && view != null) {
            androidx.core.view.WindowCompat.getInsetsController(window, view)
        } else null
    }

    // Handle Orientation & System Bars
    LaunchedEffect(isFullscreen) {
        android.util.Log.d("MainScreen", "isFullscreen changed from ${!isFullscreen} to $isFullscreen")

        if (isFullscreen) {
            android.util.Log.d("MainScreen", "Attempting to set LANDSCAPE. Activity found: ${activity != null}")
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            
            // Use modern WindowInsetsController for maximum compatibility
            controller?.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            android.util.Log.d("MainScreen", "Attempting to set PORTRAIT")
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            
            // Show system bars
            controller?.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        }
    }

    // Sync Player Loading State
    DisposableEffect(viewModel.player) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isLoading = state == androidx.media3.common.Player.STATE_BUFFERING
            }
        }
        viewModel.player.addListener(listener)
        onDispose { viewModel.player.removeListener(listener) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. LIST LAYER (Only Portrait)
            if (!isFullscreen) {
                Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    Spacer(modifier = Modifier.height(250.dp))
                    
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        placeholder = { Text("Search channels") },
                        singleLine = true
                    )

                    // Categories Tab Row
                    if (categories.isNotEmpty()) {
                        ScrollableTabRow(
                            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                            edgePadding = 8.dp,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[categories.indexOf(selectedCategory).coerceAtLeast(0)])
                                )
                            }
                        ) {
                            categories.forEach { category ->
                                Tab(
                                    selected = selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) },
                                    text = { Text(text = category) }
                                )
                            }
                        }
                    }

                    // Channel List
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        items(filteredChannels) { channel ->
                            ListItem(
                                headlineContent = { Text(channel.name) },
                                supportingContent = { channel.group?.let { Text(it, style=MaterialTheme.typography.labelSmall) } },
                                leadingContent = {
                                    AsyncImage(
                                        model = channel.logo,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp).background(Color.LightGray),
                                        error = null // maybe a placeholder?
                                    )
                                },
                                modifier = Modifier.clickable { viewModel.playChannel(channel.url) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            // 2. PLAYER LAYER (Dynamic size)
            Box(
                modifier = if (isFullscreen) Modifier.fillMaxSize()
                else Modifier.fillMaxWidth().height(250.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = viewModel.player
                            useController = true
                            setBackgroundColor(android.graphics.Color.BLACK)
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
            }

            // 3. FULLSCREEN BUTTON LAYER - SEPARATE from PlayerView to receive clicks
            Box(
                modifier = Modifier
                    .align(if (isFullscreen) Alignment.TopEnd else Alignment.TopEnd)
                    .padding(16.dp)
                    .zIndex(Float.MAX_VALUE) // Maximum Z-index to ensure it's on top
                    .background(
                        color = Color(0xFF2196F3), // Bright blue to make it visible
                        shape = MaterialTheme.shapes.medium
                    )
                    .clickable {
                        android.util.Log.e("MainScreen", " BUTTON CLICKED! ")
                        android.util.Log.d("MainScreen", "Current isFullscreen: $isFullscreen")
                        android.util.Log.d("MainScreen", "Will change to: ${!isFullscreen}")
                        
                        Toast.makeText(
                            context, 
                            "Button Clicked! Fullscreen}",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        isFullscreen = !isFullscreen
                        
                        android.util.Log.d("MainScreen", "After toggle, isFullscreen: $isFullscreen")
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (isFullscreen) "EXIT FULL" else "FULL SCREEN",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}
