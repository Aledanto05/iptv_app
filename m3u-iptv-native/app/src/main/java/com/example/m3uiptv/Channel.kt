package com.example.m3uiptv

data class Channel(
    val id: String,
    val name: String,
    val group: String,
    val url: String,
    val logo: String = "",
    val metaName: String = ""
)
