package com.example.m3uiptv

object M3UParser {
    fun parse(content: String): List<Channel> {
        val lines = content.split("\n", "\r\n")
        val channels = mutableListOf<Channel>()
        var pendingName = ""
        var pendingGroup = "Senza gruppo"
        var pendingLogo = ""
        var pendingMeta = ""

        for (raw in lines) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF:")) {
                pendingMeta = Regex("tvg-name=\"([^\"]*)\"").find(line)?.groupValues?.getOrNull(1) ?: ""
                pendingLogo = Regex("tvg-logo=\"([^\"]*)\"").find(line)?.groupValues?.getOrNull(1) ?: ""
                pendingGroup = Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.getOrNull(1) ?: "Senza gruppo"
                pendingName = line.substringAfter(",", pendingMeta.ifBlank { "Canale senza nome" }).trim()
            } else if (!line.startsWith("#")) {
                channels.add(
                    Channel(
                        id = pendingName + "-" + System.nanoTime().toString(),
                        name = pendingName.ifBlank { "Canale senza nome" },
                        group = pendingGroup.ifBlank { "Senza gruppo" },
                        url = line,
                        logo = pendingLogo,
                        metaName = pendingMeta
                    )
                )
            }
        }
        return channels
    }
}
