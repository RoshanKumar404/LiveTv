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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
// Use AndroidViewModel so we have access to the Application Context
class MainviewModel(application: Application) : AndroidViewModel(application) {

    private val repo = RepoLink()

    // 1. Channel List State
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

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
                _channels.value = repo.fetchSportsChannels()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 3. Helper function to play a channel
    fun playChannel(url: String) {
        val mediaItem = MediaItem.fromUri(url)
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