package com.example.livetv.ScreensWithModels

import android.app.Application
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.livetv.Data.Model.Channel
import com.example.livetv.Data.Repo.RepoLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
// Use AndroidViewModel so we have access to the Application Context
class MainviewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RepoLink()

    // 1. Channel List State
    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    private val _selectedCategory = MutableStateFlow("All")
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    
    val categories = MutableStateFlow<List<String>>(listOf("All"))
    val selectedCategory = _selectedCategory.asStateFlow()
    val currentChannel = _currentChannel.asStateFlow()

    val channels = combine(_allChannels, _selectedCategory) { channels, category ->
        if (category == "All") channels
        else channels.filter { it.group == category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 2. ExoPlayer Instance (Survives rotation)
    val player: ExoPlayer = ExoPlayer.Builder(application).apply {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0")
            .setAllowCrossProtocolRedirects(true)
        setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
    }.build()

    init {
        fetchChannels()
    }

    private fun fetchChannels() {
        viewModelScope.launch {
            try {
                val fetched = repo.fetchChannels()
                _allChannels.value = fetched
                
                // Extract unique categories
                val cats = fetched.mapNotNull { it.group }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                categories.value = listOf("All") + cats
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // 3. Helper function to play a channel
    fun playChannel(channel: Channel) {
        _currentChannel.value = channel
        val mediaItem = MediaItem.fromUri(channel.url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    // 4. Cleanup when ViewModel is destroyed
    override fun onCleared() {
        super.onCleared()
        player.release()
    }
}