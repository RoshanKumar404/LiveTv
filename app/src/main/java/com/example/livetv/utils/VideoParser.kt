package com.example.livetv.utils

import androidx.compose.runtime.mutableStateListOf
import com.example.livetv.Data.Model.Channel

object VideoParser {
    fun parse(text: String):List<Channel>{
        val channels= mutableStateListOf<Channel>()
        var name=""
        var group: String?=null
        text.lines().forEach { line->
            when{
                line.startsWith("#EXTINF")->{
                    name= line.substringAfter(",").trim()
                    group = Regex("""group-title="(.*?)"""")
                        .find(line)?.groupValues?.get(1)
                }
                line.startsWith("http")&& line.contains(".m3u8")->{
                    channels.add(Channel(name,line,group))
                }
            }

        }
        return channels

    }
}