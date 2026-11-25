package com.snapany.app.model

import java.util.UUID

enum class PlatformType(val label: String) {
    YOUTUBE("YouTube"),
    TIKTOK("TikTok"),
    INSTAGRAM("Instagram"),
    OTHER("Video");

    companion object {
        fun from(raw: String): PlatformType = when (raw.lowercase()) {
            "youtube" -> YOUTUBE
            "tiktok" -> TIKTOK
            "instagram" -> INSTAGRAM
            else -> OTHER
        }
    }
}

data class FormatInfo(
    val formatId: String,
    val quality: String,
    val ext: String,
    val filesize: String,
    val downloadEndpoint: String
) {
    val isAudio: Boolean
        get() = ext.equals("mp3", ignoreCase = true) || formatId.contains("mp3", ignoreCase = true)
}

data class ParseResponse(
    val title: String,
    val thumbnail: String,
    val duration: String,
    val platform: String,
    val formats: List<FormatInfo>,
    val author: String?
) {
    val platformType: PlatformType
        get() = PlatformType.from(platform)
}

sealed class FormatDownloadState {
    data object Idle : FormatDownloadState()
    data class Downloading(val progress: Float) : FormatDownloadState()
    data class Completed(val filePath: String) : FormatDownloadState()
    data class Failed(val message: String) : FormatDownloadState()
}

data class DownloadedItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val thumbnailUrl: String,
    val platform: String,
    val quality: String,
    val ext: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isAudio: Boolean
        get() = ext.equals("mp3", ignoreCase = true)

    fun formattedSize(): String {
        if (fileSizeBytes <= 0) return "Noma'lum"
        val kb = fileSizeBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.0f KB", kb)
    }
}
