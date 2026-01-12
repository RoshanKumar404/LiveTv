package com.example.livetv.Data.Repo

import android.util.Log // Add this import
import com.example.livetv.Data.Model.Channel
import com.example.livetv.utils.VideoParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class RepoLink {
    private val client = OkHttpClient()
    private val TAG = "IPTV_REPO"

    suspend fun fetchSportsChannels(): List<Channel> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://iptv-org.github.io/iptv/index.m3u")
                .header("User-Agent", "Mozilla/5.0") // Helps bypass some server blocks
                .build()

            val response = client.newCall(request).execute()

            // Use .use to automatically close the response body and prevent memory leaks
            response.body?.use { body ->
                val content = body.string()
                val allChannels = VideoParser.parse(content)

                val sportsChannels = allChannels.filter {
                    it.group?.lowercase()?.contains("sport") == true ||
                            it.name?.lowercase()?.contains("sport") == true
                }

                Log.d(TAG, "Channels loaded: ${sportsChannels.size}")
                return@withContext sportsChannels
            } ?: emptyList()

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching channels", e)
            emptyList()
        }
    }
}