package com.example.livetv.Data.Repo

import com.example.livetv.Data.Model.Channel
import com.example.livetv.utils.VideoParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RepoLink{
    private  val client= OkHttpClient()
    suspend fun fetchSportsChannels(): List<Channel> =
        withContext(Dispatchers.IO){
            val request = Request.Builder()
                .url("https://iptv-org.github.io/iptv/index.m3u")
                .build()
            val response= client.newCall(request).execute()
            val body= response.body?.string().orEmpty()
            VideoParser.parse(body)
                .filter {
                    it.group?.lowercase()?.contains("sport")==true
                }

    }
}
