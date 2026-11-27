package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["isVideo"]),
        Index(value = ["folderPath"]),
        Index(value = ["album"]),
        Index(value = ["artist"])
    ]
)
data class MediaEntity(
    @PrimaryKey val uri: String,
    val mediaStoreId: Long = 0L,
    val title: String,
    val artist: String = "Unknown Artist",
    val album: String = "Unknown Album",
    val genre: String = "General",
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val codecName: String = "MP3",
    val containerFormat: String = "MP3",
    val isVideo: Boolean = false,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val bitrate: Int = 0,
    val hdrFormat: String = "SDR",
    val sampleRateHz: Int = 44100,
    val channelCount: Int = 2,
    val folderPath: String = "/storage/emulated/0/Music",
    val folderName: String = "Music",
    val dateAddedSec: Long = 0L,
    val isFavorite: Boolean = false,
    val lastPlayedTimestamp: Long = 0L,
    val lastPositionMs: Long = 0L,
    val playCount: Int = 0,
    val subtitleDelayMs: Long = 0L,
    val audioDelayMs: Long = 0L,
    val preferredSpeed: Float = 1.0f,
    val localLyrics: String = ""
) {
    val is4kUhd: Boolean
        get() = width >= 3840 || height >= 2160

    val isHdr: Boolean
        get() = hdrFormat != "SDR" && hdrFormat.isNotBlank()

    val resolutionBadge: String
        get() = when {
            width >= 3840 || height >= 2160 -> "4K UHD"
            width >= 2560 || height >= 1440 -> "1440p QHD"
            width >= 1920 || height >= 1080 -> "1080p FHD"
            width >= 1280 || height >= 720 -> "720p HD"
            width > 0 && height > 0 -> "${height}p"
            else -> containerFormat
        }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val isVideoPlaylist: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "mediaUri"]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,
    val mediaUri: String,
    val positionOrder: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "video_bookmarks",
    indices = [Index(value = ["mediaUri"])]
)
data class VideoBookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val mediaUri: String,
    val timestampMs: Long,
    val label: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val encryptedFileName: String,
    val originalTitle: String,
    val originalMimeType: String,
    val codecBadge: String,
    val isVideo: Boolean,
    val durationMs: Long,
    val sizeBytes: Long,
    val ivBase64: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "folder_shortcuts")
data class FolderShortcutEntity(
    @PrimaryKey val folderPath: String,
    val label: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "equalizer_presets")
data class EqualizerPresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val bandLevelsCsv: String,
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val isBuiltIn: Boolean = false
)
