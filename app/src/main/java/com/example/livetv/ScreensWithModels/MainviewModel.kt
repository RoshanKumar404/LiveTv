package com.example.livetv.ScreensWithModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.livetv.Data.Model.Channel
import com.example.livetv.Data.Repo.RepoLink
import com.example.livetv.utils.VideoParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainviewModel: ViewModel() {
    private  val repo= RepoLink()
    private  val _channels= MutableStateFlow<List<Channel>>(emptyList())
    val channels= _channels.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                _channels.value= repo.fetchSportsChannels()
            }
            catch (e:Exception){
                e.printStackTrace()
            }
        }
    }

}