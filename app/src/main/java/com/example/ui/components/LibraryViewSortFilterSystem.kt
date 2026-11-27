package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codec.CodecCapabilityInspector
import com.example.codec.VideoStreamInspection
import com.example.data.local.MediaEntity
import com.example.storage.FileCategoryFilter
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.bnjGlassTextFieldColors
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.resolveContextualAccent
import java.io.File
import java.util.Locale

enum class MediaViewType(
    val label: String,
    val shortLabel: String,
    val icon: ImageVector
) {
    GRID("Grid View", "Grid", Icons.Default.GridView),
    LIST("List View", "List", Icons.AutoMirrored.Filled.ViewList),
    COMPACT("Compact View", "Compact", Icons.Default.ViewHeadline),
    DETAILED("Detailed View", "Detailed", Icons.Default.ViewAgenda);

    companion object {
        fun fromName(value: String): MediaViewType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: LIST
    }
}

enum class MusicSortField(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Date Added"),
    DATE_MODIFIED("Date Modified"),
    FILE_SIZE("File Size");

    companion object {
        fun fromName(value: String): MusicSortField =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TITLE
    }
}

enum class VideoSortField(val label: String) {
    TITLE("Title"),
    DATE_ADDED("Date Added"),
    DATE_MODIFIED("Date Modified"),
    DURATION("Duration"),
    FILE_SIZE("File Size"),
    RESOLUTION("Resolution");

    companion object {
        fun fromName(value: String): VideoSortField =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: TITLE
    }
}

enum class DurationRangeFilter(val label: String, val minMs: Long, val maxMs: Long) {
    SHORT_UNDER_2M("< 2 min", 0L, 120_000L),
    MEDIUM_2_TO_5M("2–5 min", 120_000L, 300_000L),
    LONG_5_TO_10M("5–10 min", 300_000L, 600_000L),
    EXTENDED_OVER_10M("> 10 min", 600_000L, Long.MAX_VALUE);

    fun matches(durationMs: Long): Boolean = durationMs >= minMs && durationMs < maxMs
}

enum class VideoExtensionFilter(val label: String, val ext: String) {
    MP4("MP4", "mp4"),
    MKV("MKV", "mkv"),
    AVI("AVI", "avi"),
    MOV("MOV", "mov"),
    WEBM("WebM", "webm"),
    THREE_GP("3GP", "3gp"),
    TS("TS", "ts"),
    M4V("M4V", "m4v"),
    MPG("MPG", "mpg"),
    MPEG("MPEG", "mpeg"),
    FLV("FLV", "flv"),
    WMV("WMV", "wmv")
}

enum class VideoResolutionFilter(val label: String) {
    RES_4K("4K UHD"),
    RES_1440P("1440p"),
    RES_1080P("1080p"),
    RES_720P("720p"),
    RES_480P("480p"),
    RES_OTHER("Other");

    fun matches(video: MediaEntity): Boolean = VideoMetadataClassifier.classifyResolution(video) == this
}

enum class VideoCodecFilter(val label: String) {
    H264("H.264"),
    HEVC("H.265/HEVC"),
    AV1("AV1"),
    VP8("VP8"),
    VP9("VP9"),
    MPEG4("MPEG-4"),
    OTHER("Other")
}

enum class VideoHdrFilter(val label: String) {
    HDR10("HDR10"),
    HDR10_PLUS("HDR10+"),
    DOLBY_VISION("Dolby Vision"),
    HLG("HLG"),
    SDR("SDR")
}

enum class VideoAudioCodecFilter(val label: String) {
    AAC("AAC"),
    MP3("MP3"),
    FLAC("FLAC"),
    AC3("AC3"),
    EAC3("EAC3"),
    DTS("DTS"),
    OPUS("Opus"),
    PCM("PCM"),
    OTHER("Other")
}

enum class VideoDurationRangeFilter(val label: String, val minMs: Long, val maxMs: Long) {
    UNDER_5_MIN("Under 5 minutes", 0L, 300_000L),
    MIN_5_TO_30("5–30 minutes", 300_000L, 1_800_000L),
    MIN_30_TO_60("30–60 minutes", 1_800_000L, 3_600_000L),
    OVER_60_MIN("Over 60 minutes", 3_600_000L, Long.MAX_VALUE);

    fun matches(durationMs: Long): Boolean = durationMs >= minMs && durationMs < maxMs
}

enum class VideoFileSizeRangeFilter(val label: String, val minBytes: Long, val maxBytes: Long) {
    UNDER_100_MB("Under 100 MB", 0L, 100L * 1024L * 1024L),
    MB_100_TO_500("100–500 MB", 100L * 1024L * 1024L, 500L * 1024L * 1024L),
    MB_500_TO_1_GB("500 MB–1 GB", 500L * 1024L * 1024L, 1024L * 1024L * 1024L),
    OVER_1_GB("Over 1 GB", 1024L * 1024L * 1024L, Long.MAX_VALUE);

    fun matches(sizeBytes: Long): Boolean = sizeBytes >= minBytes && sizeBytes < maxBytes
}

object VideoMetadataClassifier {

    private val containerOnlyLabels = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "3gp", "ts", "mpeg-ts",
        "m4v", "mpg", "mpeg", "flv", "wmv", "vob", "ogv", "mts", "m2ts"
    )

    private val containerOnlyMimes = setOf(
        "video/mp4", "video/x-matroska", "video/x-msvideo", "video/quicktime",
        "video/webm", "video/3gpp", "video/mp2t", "video/x-m4v",
        "video/mpeg", "video/x-flv", "video/x-ms-wmv", "video/ogg"
    )

    fun classifyExtension(
        video: MediaEntity,
        inspected: VideoStreamInspection? = null
    ): VideoExtensionFilter? {
        val canonicalExt = (inspected?.canonicalExtension ?: CodecCapabilityInspector.resolveCanonicalExtension(
            uriOrPath = video.uri,
            containerFormat = video.containerFormat,
            mimeType = video.mimeType,
            title = video.title,
            isVideo = true
        )).lowercase(Locale.US)
        return VideoExtensionFilter.entries.firstOrNull { it.ext == canonicalExt }
    }

    fun classifyResolution(video: MediaEntity): VideoResolutionFilter {
        val maxDim = maxOf(video.width, video.height)
        val minDim = minOf(video.width, video.height)
        val badge = video.resolutionBadge.lowercase(Locale.US)
        return when {
            maxDim >= 3840 || minDim >= 2160 || badge.contains("4k") -> VideoResolutionFilter.RES_4K
            maxDim >= 2560 || minDim >= 1440 || badge.contains("1440") -> VideoResolutionFilter.RES_1440P
            maxDim >= 1920 || minDim >= 1080 || badge.contains("1080") -> VideoResolutionFilter.RES_1080P
            maxDim >= 1280 || minDim >= 720 || badge.contains("720") -> VideoResolutionFilter.RES_720P
            maxDim in 640..1279 || minDim in 360..719 || badge.contains("480") -> VideoResolutionFilter.RES_480P
            else -> VideoResolutionFilter.RES_OTHER
        }
    }

    fun resolveDisplayVideoCodec(
        video: MediaEntity,
        inspected: VideoStreamInspection? = null
    ): String? {
        inspected?.videoCodecLabel?.takeIf { it.isNotBlank() }?.let { return it }
        inspected?.videoMime?.let { mime ->
            CodecCapabilityInspector.mapVideoMimeToCodecLabel(mime)?.let { return it }
        }
        val rawCodec = video.codecName.trim()
        val lowerCodec = rawCodec.lowercase(Locale.US)
        if (lowerCodec.isNotEmpty() && lowerCodec !in containerOnlyLabels) {
            return when {
                lowerCodec.contains("hevc") || lowerCodec.contains("h.265") || lowerCodec.contains("h265") -> "H.265/HEVC"
                lowerCodec.contains("avc") || lowerCodec.contains("h.264") || lowerCodec.contains("h264") -> "H.264"
                lowerCodec.contains("av01") || lowerCodec == "av1" || lowerCodec.contains("av1 ") -> "AV1"
                lowerCodec.contains("vp9") -> "VP9"
                lowerCodec.contains("vp8") -> "VP8"
                lowerCodec.contains("mpeg-4") || lowerCodec.contains("mpeg4") || lowerCodec.contains("mp4v") ||
                    lowerCodec.contains("xvid") || lowerCodec.contains("divx") -> "MPEG-4"
                else -> rawCodec
            }
        }
        val lowerMime = video.mimeType.lowercase(Locale.US).trim()
        if (lowerMime.isNotEmpty() && lowerMime !in containerOnlyMimes) {
            return CodecCapabilityInspector.mapVideoMimeToCodecLabel(lowerMime)
        }
        return null
    }

    fun classifyVideoCodec(
        video: MediaEntity,
        inspected: VideoStreamInspection? = null
    ): VideoCodecFilter {
        val label = resolveDisplayVideoCodec(video, inspected)?.lowercase(Locale.US).orEmpty()
        return when {
            label.contains("h.264") || label.contains("h264") || label.contains("avc") -> VideoCodecFilter.H264
            label.contains("hevc") || label.contains("h.265") || label.contains("h265") -> VideoCodecFilter.HEVC
            label.contains("av1") || label.contains("av01") -> VideoCodecFilter.AV1
            label.contains("vp8") -> VideoCodecFilter.VP8
            label.contains("vp9") -> VideoCodecFilter.VP9
            label.contains("mpeg-4") || label.contains("mpeg4") || label.contains("mp4v") -> VideoCodecFilter.MPEG4
            else -> VideoCodecFilter.OTHER
        }
    }

    fun classifyHdr(
        video: MediaEntity,
        inspected: VideoStreamInspection? = null
    ): VideoHdrFilter {
        val raw = (inspected?.hdrFormat ?: video.hdrFormat).lowercase(Locale.US).trim()
        return when {
            raw.contains("hdr10+") || raw.contains("hdr10plus") -> VideoHdrFilter.HDR10_PLUS
            raw.contains("hdr10") -> VideoHdrFilter.HDR10
            raw.contains("dolby") || raw.contains("dovi") -> VideoHdrFilter.DOLBY_VISION
            raw.contains("hlg") -> VideoHdrFilter.HLG
            else -> VideoHdrFilter.SDR
        }
    }

    fun classifyAudioCodecs(
        video: MediaEntity,
        inspected: VideoStreamInspection? = null
    ): Set<VideoAudioCodecFilter> {
        if (inspected != null && inspected.inspectionSucceeded) {
            val tracks = inspected.audioTrackCount ?: 0
            if (tracks == 0 && inspected.audioCodecLabels.isEmpty()) {
                return emptySet()
            }
            if (inspected.audioCodecLabels.isEmpty()) {
                return setOf(VideoAudioCodecFilter.OTHER)
            }
            return inspected.audioCodecLabels.map { raw ->
                val lower = raw.lowercase(Locale.US)
                when {
                    lower == "aac" || lower.contains("mp4a") -> VideoAudioCodecFilter.AAC
                    lower == "mp3" || lower.contains("mpeg") -> VideoAudioCodecFilter.MP3
                    lower == "flac" -> VideoAudioCodecFilter.FLAC
                    lower == "eac3" || lower.contains("e-ac") -> VideoAudioCodecFilter.EAC3
                    lower == "ac3" || lower.contains("ac-3") -> VideoAudioCodecFilter.AC3
                    lower.contains("dts") -> VideoAudioCodecFilter.DTS
                    lower == "opus" -> VideoAudioCodecFilter.OPUS
                    lower == "pcm" || lower.contains("raw") -> VideoAudioCodecFilter.PCM
                    else -> VideoAudioCodecFilter.OTHER
                }
            }.toSet()
        }
        return setOf(VideoAudioCodecFilter.OTHER)
    }
}

enum class FileSizeRangeFilter(val label: String, val minBytes: Long, val maxBytes: Long) {
    SMALL_UNDER_50MB("< 50 MB", 0L, 50L * 1024L * 1024L),
    MEDIUM_50_TO_500MB("50–500 MB", 50L * 1024L * 1024L, 500L * 1024L * 1024L),
    LARGE_OVER_500MB("> 500 MB", 500L * 1024L * 1024L, Long.MAX_VALUE);

    fun matches(sizeBytes: Long): Boolean = sizeBytes >= minBytes && sizeBytes < maxBytes
}

data class ActiveFilterChipItem(
    val id: String,
    val label: String
)

data class MusicFilterState(
    val favoritesOnly: Boolean = false,
    val recentlyAddedOnly: Boolean = false,
    val recentlyPlayedOnly: Boolean = false,
    val selectedArtists: Set<String> = emptySet(),
    val selectedAlbums: Set<String> = emptySet(),
    val selectedGenres: Set<String> = emptySet(),
    val selectedDurationRanges: Set<DurationRangeFilter> = emptySet(),
    val selectedFolders: Set<String> = emptySet(),
    val selectedCodecs: Set<String> = emptySet()
) {
    val activeFilterCount: Int
        get() = (if (favoritesOnly) 1 else 0) +
            (if (recentlyAddedOnly) 1 else 0) +
            (if (recentlyPlayedOnly) 1 else 0) +
            selectedArtists.size +
            selectedAlbums.size +
            selectedGenres.size +
            selectedDurationRanges.size +
            selectedFolders.size +
            selectedCodecs.size

    fun toActiveChips(): List<ActiveFilterChipItem> = buildList {
        if (favoritesOnly) add(ActiveFilterChipItem("fav", "Favorites"))
        if (recentlyAddedOnly) add(ActiveFilterChipItem("recent_added", "Recently Added"))
        if (recentlyPlayedOnly) add(ActiveFilterChipItem("recent_played", "Recently Played"))
        selectedArtists.forEach { add(ActiveFilterChipItem("artist:$it", "Artist: $it")) }
        selectedAlbums.forEach { add(ActiveFilterChipItem("album:$it", "Album: $it")) }
        selectedGenres.forEach { add(ActiveFilterChipItem("genre:$it", "Genre: $it")) }
        selectedDurationRanges.forEach { add(ActiveFilterChipItem("dur:${it.name}", "Duration: ${it.label}")) }
        selectedFolders.forEach { add(ActiveFilterChipItem("folder:$it", "Folder: $it")) }
        selectedCodecs.forEach { add(ActiveFilterChipItem("codec:$it", "Codec: $it")) }
    }

    fun removeChip(chipId: String): MusicFilterState {
        return when {
            chipId == "fav" -> copy(favoritesOnly = false)
            chipId == "recent_added" -> copy(recentlyAddedOnly = false)
            chipId == "recent_played" -> copy(recentlyPlayedOnly = false)
            chipId.startsWith("artist:") -> copy(selectedArtists = selectedArtists - chipId.removePrefix("artist:"))
            chipId.startsWith("album:") -> copy(selectedAlbums = selectedAlbums - chipId.removePrefix("album:"))
            chipId.startsWith("genre:") -> copy(selectedGenres = selectedGenres - chipId.removePrefix("genre:"))
            chipId.startsWith("dur:") -> {
                val target = DurationRangeFilter.entries.firstOrNull { it.name == chipId.removePrefix("dur:") }
                if (target != null) copy(selectedDurationRanges = selectedDurationRanges - target) else this
            }
            chipId.startsWith("folder:") -> copy(selectedFolders = selectedFolders - chipId.removePrefix("folder:"))
            chipId.startsWith("codec:") -> copy(selectedCodecs = selectedCodecs - chipId.removePrefix("codec:"))
            else -> this
        }
    }
}

data class VideoFilterState(
    val recentlyPlayedOnly: Boolean = false,
    val selectedFolders: Set<String> = emptySet(),
    val selectedExtensions: Set<VideoExtensionFilter> = emptySet(),
    val selectedResolutions: Set<VideoResolutionFilter> = emptySet(),
    val selectedVideoCodecs: Set<VideoCodecFilter> = emptySet(),
    val selectedHdrFormats: Set<VideoHdrFilter> = emptySet(),
    val selectedAudioCodecs: Set<VideoAudioCodecFilter> = emptySet(),
    val selectedVideoDurations: Set<VideoDurationRangeFilter> = emptySet(),
    val selectedVideoSizes: Set<VideoFileSizeRangeFilter> = emptySet(),
    // Legacy compatibility fields preserved for callers/tests
    val selectedDurationRanges: Set<DurationRangeFilter> = emptySet(),
    val selectedFormats: Set<String> = emptySet(),
    val selectedSizeRanges: Set<FileSizeRangeFilter> = emptySet()
) {
    val activeFilterCount: Int
        get() = (if (recentlyPlayedOnly) 1 else 0) +
            selectedFolders.size +
            selectedExtensions.size +
            selectedResolutions.size +
            selectedVideoCodecs.size +
            selectedHdrFormats.size +
            selectedAudioCodecs.size +
            selectedVideoDurations.size +
            selectedVideoSizes.size +
            selectedDurationRanges.size +
            selectedFormats.size +
            selectedSizeRanges.size

    fun toActiveChips(): List<ActiveFilterChipItem> = buildList {
        if (recentlyPlayedOnly) add(ActiveFilterChipItem("recent_played", "Recently Played"))
        selectedExtensions.forEach { add(ActiveFilterChipItem("ext:${it.name}", it.label)) }
        selectedResolutions.forEach { add(ActiveFilterChipItem("res:${it.name}", it.label)) }
        selectedVideoCodecs.forEach { add(ActiveFilterChipItem("vcodec:${it.name}", it.label)) }
        selectedHdrFormats.forEach { add(ActiveFilterChipItem("hdr:${it.name}", it.label)) }
        selectedAudioCodecs.forEach { add(ActiveFilterChipItem("acodec:${it.name}", "Audio: ${it.label}")) }
        selectedVideoDurations.forEach { add(ActiveFilterChipItem("vdur:${it.name}", it.label)) }
        selectedVideoSizes.forEach { add(ActiveFilterChipItem("vsize:${it.name}", it.label)) }
        selectedFormats.forEach { add(ActiveFilterChipItem("fmt:$it", "Format: $it")) }
        selectedDurationRanges.forEach { add(ActiveFilterChipItem("dur:${it.name}", "Duration: ${it.label}")) }
        selectedSizeRanges.forEach { add(ActiveFilterChipItem("size:${it.name}", "Size: ${it.label}")) }
        selectedFolders.forEach { add(ActiveFilterChipItem("folder:$it", "Folder: $it")) }
    }

    fun removeChip(chipId: String): VideoFilterState {
        return when {
            chipId == "recent_played" -> copy(recentlyPlayedOnly = false)
            chipId.startsWith("ext:") -> {
                val target = VideoExtensionFilter.entries.firstOrNull { it.name == chipId.removePrefix("ext:") }
                if (target != null) copy(selectedExtensions = selectedExtensions - target) else this
            }
            chipId.startsWith("res:") -> {
                val target = VideoResolutionFilter.entries.firstOrNull { it.name == chipId.removePrefix("res:") }
                if (target != null) copy(selectedResolutions = selectedResolutions - target) else this
            }
            chipId.startsWith("vcodec:") -> {
                val target = VideoCodecFilter.entries.firstOrNull { it.name == chipId.removePrefix("vcodec:") }
                if (target != null) copy(selectedVideoCodecs = selectedVideoCodecs - target) else this
            }
            chipId.startsWith("hdr:") -> {
                val target = VideoHdrFilter.entries.firstOrNull { it.name == chipId.removePrefix("hdr:") }
                if (target != null) copy(selectedHdrFormats = selectedHdrFormats - target) else this
            }
            chipId.startsWith("acodec:") -> {
                val target = VideoAudioCodecFilter.entries.firstOrNull { it.name == chipId.removePrefix("acodec:") }
                if (target != null) copy(selectedAudioCodecs = selectedAudioCodecs - target) else this
            }
            chipId.startsWith("vdur:") -> {
                val target = VideoDurationRangeFilter.entries.firstOrNull { it.name == chipId.removePrefix("vdur:") }
                if (target != null) copy(selectedVideoDurations = selectedVideoDurations - target) else this
            }
            chipId.startsWith("vsize:") -> {
                val target = VideoFileSizeRangeFilter.entries.firstOrNull { it.name == chipId.removePrefix("vsize:") }
                if (target != null) copy(selectedVideoSizes = selectedVideoSizes - target) else this
            }
            chipId.startsWith("fmt:") -> copy(selectedFormats = selectedFormats - chipId.removePrefix("fmt:"))
            chipId.startsWith("dur:") -> {
                val target = DurationRangeFilter.entries.firstOrNull { it.name == chipId.removePrefix("dur:") }
                if (target != null) copy(selectedDurationRanges = selectedDurationRanges - target) else this
            }
            chipId.startsWith("size:") -> {
                val target = FileSizeRangeFilter.entries.firstOrNull { it.name == chipId.removePrefix("size:") }
                if (target != null) copy(selectedSizeRanges = selectedSizeRanges - target) else this
            }
            chipId.startsWith("folder:") -> copy(selectedFolders = selectedFolders - chipId.removePrefix("folder:"))
            else -> this
        }
    }
}

data class FileFilterState(
    val selectedCategories: Set<FileCategoryFilter> = emptySet()
) {
    val activeFilterCount: Int
        get() = selectedCategories.size

    fun toActiveChips(): List<ActiveFilterChipItem> =
        selectedCategories.map { ActiveFilterChipItem("cat:${it.name}", it.label) }

    fun removeChip(chipId: String): FileFilterState {
        if (chipId.startsWith("cat:")) {
            val target = FileCategoryFilter.entries.firstOrNull { it.name == chipId.removePrefix("cat:") }
            if (target != null) {
                return copy(selectedCategories = selectedCategories - target)
            }
        }
        return this
    }
}

object LibraryQueryEngine {

    private fun resolveDateModifiedSec(item: MediaEntity): Long {
        if (item.uri.startsWith("file://")) {
            val path = item.uri.removePrefix("file://")
            val f = File(path)
            if (f.exists()) return f.lastModified() / 1000L
        }
        return item.dateAddedSec
    }

    private fun buildDateModifiedMap(items: List<MediaEntity>): Map<String, Long> {
        val map = HashMap<String, Long>(items.size)
        for (i in items.indices) {
            val item = items[i]
            map[item.uri] = resolveDateModifiedSec(item)
        }
        return map
    }

    fun filterAndSortMusic(
        songs: List<MediaEntity>,
        searchQuery: String,
        filter: MusicFilterState,
        sortField: MusicSortField,
        ascending: Boolean
    ): List<MediaEntity> {
        val recentCutoffSec = if (songs.isNotEmpty()) {
            val maxAdded = songs.maxOf { it.dateAddedSec }
            (maxAdded - 30L * 24L * 3600L).coerceAtLeast(0L)
        } else 0L

        val filtered = songs.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.artist.contains(searchQuery, ignoreCase = true) ||
                item.album.contains(searchQuery, ignoreCase = true) ||
                item.genre.contains(searchQuery, ignoreCase = true) ||
                item.codecName.contains(searchQuery, ignoreCase = true) ||
                item.folderName.contains(searchQuery, ignoreCase = true)

            val matchesFav = !filter.favoritesOnly || item.isFavorite
            val matchesRecentAdded = !filter.recentlyAddedOnly || item.dateAddedSec >= recentCutoffSec
            val matchesRecentPlayed = !filter.recentlyPlayedOnly || item.lastPlayedTimestamp > 0L || item.playCount > 0

            val matchesArtist = filter.selectedArtists.isEmpty() || item.artist in filter.selectedArtists
            val matchesAlbum = filter.selectedAlbums.isEmpty() || item.album in filter.selectedAlbums
            val matchesGenre = filter.selectedGenres.isEmpty() || item.genre in filter.selectedGenres
            val matchesDuration = filter.selectedDurationRanges.isEmpty() ||
                filter.selectedDurationRanges.any { it.matches(item.durationMs) }
            val folderLabel = item.folderName.ifBlank { item.folderPath.substringAfterLast('/') }
            val matchesFolder = filter.selectedFolders.isEmpty() ||
                folderLabel in filter.selectedFolders ||
                item.folderPath in filter.selectedFolders
            val matchesCodec = filter.selectedCodecs.isEmpty() ||
                filter.selectedCodecs.any { codec ->
                    item.codecName.contains(codec, ignoreCase = true) ||
                        item.containerFormat.contains(codec, ignoreCase = true)
                }

            matchesQuery && matchesFav && matchesRecentAdded && matchesRecentPlayed &&
                matchesArtist && matchesAlbum && matchesGenre && matchesDuration && matchesFolder && matchesCodec
        }

        val modTimeByUri = if (sortField == MusicSortField.DATE_MODIFIED) {
            buildDateModifiedMap(filtered)
        } else {
            emptyMap()
        }
        val comparator: Comparator<MediaEntity> = when (sortField) {
            MusicSortField.TITLE -> compareBy { it.title.lowercase(Locale.US) }
            MusicSortField.ARTIST -> compareBy<MediaEntity> { it.artist.lowercase(Locale.US) }.thenBy { it.title.lowercase(Locale.US) }
            MusicSortField.ALBUM -> compareBy<MediaEntity> { it.album.lowercase(Locale.US) }.thenBy { it.title.lowercase(Locale.US) }
            MusicSortField.DURATION -> compareBy<MediaEntity> { it.durationMs }.thenBy { it.title.lowercase(Locale.US) }
            MusicSortField.DATE_ADDED -> compareBy<MediaEntity> { it.dateAddedSec }.thenBy { it.title.lowercase(Locale.US) }
            MusicSortField.DATE_MODIFIED -> compareBy<MediaEntity> { modTimeByUri[it.uri] ?: it.dateAddedSec }.thenBy { it.title.lowercase(Locale.US) }
            MusicSortField.FILE_SIZE -> compareBy<MediaEntity> { it.sizeBytes }.thenBy { it.title.lowercase(Locale.US) }
        }

        return filtered.sortedWith(if (ascending) comparator else comparator.reversed())
    }

    fun filterAndSortVideos(
        videos: List<MediaEntity>,
        searchQuery: String,
        filter: VideoFilterState,
        sortField: VideoSortField,
        ascending: Boolean,
        inspectedStreamsByUri: Map<String, VideoStreamInspection> = emptyMap()
    ): List<MediaEntity> {
        val filtered = videos.filter { video ->
            val inspected = inspectedStreamsByUri[video.uri]
                ?: CodecCapabilityInspector.getCachedVideoStreamInspection(video.uri, video.sizeBytes)

            val matchesQuery = searchQuery.isBlank() ||
                video.title.contains(searchQuery, ignoreCase = true) ||
                video.codecName.contains(searchQuery, ignoreCase = true) ||
                video.containerFormat.contains(searchQuery, ignoreCase = true) ||
                video.resolutionBadge.contains(searchQuery, ignoreCase = true) ||
                video.folderName.contains(searchQuery, ignoreCase = true) ||
                (inspected?.videoCodecLabel?.contains(searchQuery, ignoreCase = true) == true) ||
                (inspected?.audioCodecLabels?.any { it.contains(searchQuery, ignoreCase = true) } == true)

            val matchesRecentPlayed = !filter.recentlyPlayedOnly ||
                video.lastPlayedTimestamp > 0L ||
                video.lastPositionMs > 0L ||
                video.playCount > 0

            val folderLabel = video.folderName.ifBlank { video.folderPath.substringAfterLast('/') }
            val matchesFolder = filter.selectedFolders.isEmpty() ||
                folderLabel in filter.selectedFolders ||
                video.folderPath in filter.selectedFolders

            val matchesExtension = filter.selectedExtensions.isEmpty() ||
                VideoMetadataClassifier.classifyExtension(video, inspected) in filter.selectedExtensions

            val matchesResolution = filter.selectedResolutions.isEmpty() ||
                VideoMetadataClassifier.classifyResolution(video) in filter.selectedResolutions

            val matchesVideoCodec = filter.selectedVideoCodecs.isEmpty() ||
                VideoMetadataClassifier.classifyVideoCodec(video, inspected) in filter.selectedVideoCodecs

            val matchesHdr = filter.selectedHdrFormats.isEmpty() ||
                VideoMetadataClassifier.classifyHdr(video, inspected) in filter.selectedHdrFormats

            val videoAudioCodecs = if (filter.selectedAudioCodecs.isEmpty()) {
                emptySet()
            } else {
                VideoMetadataClassifier.classifyAudioCodecs(video, inspected)
            }
            val matchesAudioCodec = filter.selectedAudioCodecs.isEmpty() ||
                videoAudioCodecs.any { it in filter.selectedAudioCodecs }

            val matchesVideoDuration = filter.selectedVideoDurations.isEmpty() ||
                filter.selectedVideoDurations.any { it.matches(video.durationMs) }

            val matchesVideoSize = filter.selectedVideoSizes.isEmpty() ||
                filter.selectedVideoSizes.any { it.matches(video.sizeBytes) }

            val matchesLegacyDuration = filter.selectedDurationRanges.isEmpty() ||
                filter.selectedDurationRanges.any { it.matches(video.durationMs) }

            val matchesLegacyFormat = filter.selectedFormats.isEmpty() ||
                filter.selectedFormats.any { fmt ->
                    video.containerFormat.contains(fmt, ignoreCase = true) ||
                        video.codecName.contains(fmt, ignoreCase = true) ||
                        video.hdrFormat.contains(fmt, ignoreCase = true)
                }

            val matchesLegacySize = filter.selectedSizeRanges.isEmpty() ||
                filter.selectedSizeRanges.any { it.matches(video.sizeBytes) }

            matchesQuery && matchesRecentPlayed && matchesFolder &&
                matchesExtension && matchesResolution && matchesVideoCodec &&
                matchesHdr && matchesAudioCodec && matchesVideoDuration &&
                matchesVideoSize && matchesLegacyDuration && matchesLegacyFormat &&
                matchesLegacySize
        }

        val modTimeByUri = if (sortField == VideoSortField.DATE_MODIFIED) {
            buildDateModifiedMap(filtered)
        } else {
            emptyMap()
        }
        val comparator: Comparator<MediaEntity> = when (sortField) {
            VideoSortField.TITLE -> compareBy { it.title.lowercase(Locale.US) }
            VideoSortField.DATE_ADDED -> compareBy<MediaEntity> { it.dateAddedSec }.thenBy { it.title.lowercase(Locale.US) }
            VideoSortField.DATE_MODIFIED -> compareBy<MediaEntity> { modTimeByUri[it.uri] ?: it.dateAddedSec }.thenBy { it.title.lowercase(Locale.US) }
            VideoSortField.DURATION -> compareBy<MediaEntity> { it.durationMs }.thenBy { it.title.lowercase(Locale.US) }
            VideoSortField.FILE_SIZE -> compareBy<MediaEntity> { it.sizeBytes }.thenBy { it.title.lowercase(Locale.US) }
            VideoSortField.RESOLUTION -> compareBy<MediaEntity> { it.width.toLong() * it.height.toLong() }.thenBy { it.title.lowercase(Locale.US) }
        }

        return filtered.sortedWith(if (ascending) comparator else comparator.reversed())
    }
}

@Composable
fun BnjLibraryTopToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    searchTestTag: String,
    currentViewType: MediaViewType,
    onSelectViewType: (MediaViewType) -> Unit,
    sortButtonLabel: String,
    onOpenSortSheet: () -> Unit,
    activeFilterCount: Int,
    onOpenFilterSheet: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier,
    moreOptionsContent: (@Composable () -> Unit)? = null
) {
    var showViewTypeBar by remember { mutableStateOf(false) }
    val glassConfig = LocalLiquidGlassConfig.current
    val isDarkGlass = glassConfig.isDarkGlass
    val resolvedCyan = adaptiveGlassAccent(resolveContextualAccent(LiquidCyan, glassConfig), isDarkGlass)
    val resolvedEmerald = adaptiveGlassAccent(resolveContextualAccent(GlassEmeraldTint, glassConfig), isDarkGlass)
    val resolvedIndigo = adaptiveGlassAccent(resolveContextualAccent(ElectricIndigo, glassConfig), isDarkGlass)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Search Input + Action Icons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(searchTestTag),
                placeholder = {
                    Text(
                        text = searchPlaceholder,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = resolvedCyan,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Search",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = bnjGlassTextFieldColors(resolvedCyan)
            )

            // View Type Selector Trigger
            LiquidGlassPillButton(
                onClick = { showViewTypeBar = !showViewTypeBar },
                selected = showViewTypeBar,
                accentColor = LiquidCyan,
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                modifier = Modifier.testTag("${tagPrefix}_view_type_button")
            ) {
                Icon(
                    imageVector = currentViewType.icon,
                    contentDescription = currentViewType.label,
                    tint = resolvedCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Sort Bottom Sheet Trigger
            LiquidGlassPillButton(
                onClick = onOpenSortSheet,
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                modifier = Modifier.testTag("${tagPrefix}_sort_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort ($sortButtonLabel)",
                    tint = resolvedCyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Filter Bottom Sheet Trigger with Active Filter Count Badge
            LiquidGlassPillButton(
                onClick = onOpenFilterSheet,
                selected = activeFilterCount > 0,
                accentColor = if (activeFilterCount > 0) GlassEmeraldTint else ElectricIndigo,
                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 7.dp),
                modifier = Modifier.testTag("${tagPrefix}_filter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (activeFilterCount > 0) resolvedEmerald else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                if (activeFilterCount > 0) {
                    Spacer(Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GlassEmeraldTint)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                            .testTag("${tagPrefix}_active_filter_badge"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeFilterCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF04131A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            moreOptionsContent?.invoke()
        }

        // Compact View Type & Sort Strip (toggled by View Type button)
        AnimatedVisibility(visible = showViewTypeBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaViewType.entries.forEach { mode ->
                    val isSelected = currentViewType == mode
                    LiquidGlassPillButton(
                        onClick = {
                            onSelectViewType(mode)
                            showViewTypeBar = false
                        },
                        selected = isSelected,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.testTag("${tagPrefix}_view_mode_${mode.name.lowercase(Locale.US)}")
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = mode.label,
                            tint = if (isSelected) resolvedCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = mode.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                LiquidGlassPillButton(
                    onClick = {
                        showViewTypeBar = false
                        onOpenSortSheet()
                    },
                    accentColor = ElectricIndigo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                    modifier = Modifier.testTag("${tagPrefix}_sort_summary_pill")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = resolvedIndigo,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = sortButtonLabel,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BnjActiveFilterChipsBar(
    chips: List<ActiveFilterChipItem>,
    onRemoveChip: (String) -> Unit,
    onClearAll: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = chips.isNotEmpty()) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .testTag("${tagPrefix}_active_filter_chips"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(chips, key = { it.id }) { chip ->
                LiquidGlassPillButton(
                    onClick = { onRemoveChip(chip.id) },
                    selected = true,
                    accentColor = GlassEmeraldTint,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("${tagPrefix}_chip_${chip.id}")
                ) {
                    Text(
                        text = chip.label,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove ${chip.label}",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            item {
                LiquidGlassPillButton(
                    onClick = onClearAll,
                    accentColor = GlassRoseTint,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("${tagPrefix}_clear_all_chips_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = "Clear All Filters",
                        tint = GlassRoseTint,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.labelMedium,
                        color = GlassRoseTint
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BnjSortBottomSheet(
    title: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: (T) -> String,
    ascending: Boolean,
    onSelectSort: (T, Boolean) -> Unit,
    onDismiss: () -> Unit,
    tagPrefix: String
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = dynamicGlassDialogContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("${tagPrefix}_sort_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            // Ascending / Descending Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = { onSelectSort(selectedOption, true) },
                    selected = ascending,
                    accentColor = LiquidCyan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_sort_order_asc")
                ) {
                    val sortPrimary = adaptiveGlassAccent(
                        resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                        LocalLiquidGlassConfig.current.isDarkGlass
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Ascending",
                        tint = if (ascending) sortPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Ascending", style = MaterialTheme.typography.labelLarge)
                }

                LiquidGlassPillButton(
                    onClick = { onSelectSort(selectedOption, false) },
                    selected = !ascending,
                    accentColor = LiquidCyan,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_sort_order_desc")
                ) {
                    val sortPrimary = adaptiveGlassAccent(
                        resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                        LocalLiquidGlassConfig.current.isDarkGlass
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Descending",
                        tint = if (!ascending) sortPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Descending", style = MaterialTheme.typography.labelLarge)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))

            val glassConfig = LocalLiquidGlassConfig.current
            val isDarkGlass = glassConfig.isDarkGlass
            val resolvedCyan = adaptiveGlassAccent(resolveContextualAccent(LiquidCyan, glassConfig), isDarkGlass)
            options.forEach { option ->
                val isSelected = option == selectedOption
                val label = optionLabel(option)
                LiquidGlassCard(
                    accentTint = if (isSelected) LiquidCyan else ElectricIndigo,
                    elevated = isSelected,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    onClick = {
                        onSelectSort(option, ascending)
                        onDismiss()
                    },
                    modifier = Modifier.testTag("${tagPrefix}_sort_option_${label.lowercase(Locale.US).replace(' ', '_')}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) resolvedCyan else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = resolvedCyan
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BnjMusicFilterBottomSheet(
    initialFilter: MusicFilterState,
    availableArtists: List<String>,
    availableAlbums: List<String>,
    availableGenres: List<String>,
    availableFolders: List<String>,
    availableCodecs: List<String>,
    onApplyFilter: (MusicFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initialFilter) { mutableStateOf(initialFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = dynamicGlassDialogContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("music_filter_bottom_sheet")
        ) {
            val filterGlass = LocalLiquidGlassConfig.current
            val filterPrimary = adaptiveGlassAccent(resolveContextualAccent(LiquidCyan, filterGlass), filterGlass.isDarkGlass)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filter Music Library",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (draft.activeFilterCount == 0) "All Songs active" else "${draft.activeFilterCount} filters selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = filterPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassPillButton(
                        onClick = { draft = MusicFilterState() },
                        accentColor = GlassRoseTint,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("music_filter_clear_all_button")
                    ) {
                        Text("Clear All", style = MaterialTheme.typography.labelMedium, color = GlassRoseTint)
                    }
                    LiquidGlassPillButton(
                        onClick = {
                            onApplyFilter(draft)
                            onDismiss()
                        },
                        selected = true,
                        accentColor = GlassEmeraldTint,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("music_filter_apply_button")
                    ) {
                        Text("Apply Filters", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Quick Scope", style = MaterialTheme.typography.titleSmall, color = filterPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassPillButton(
                            onClick = { draft = MusicFilterState() },
                            selected = draft.activeFilterCount == 0,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("All Songs", style = MaterialTheme.typography.labelMedium)
                        }
                        LiquidGlassPillButton(
                            onClick = { draft = draft.copy(favoritesOnly = !draft.favoritesOnly) },
                            selected = draft.favoritesOnly,
                            accentColor = GlassRoseTint,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("music_filter_favorites_toggle")
                        ) {
                            Text("Favorites", style = MaterialTheme.typography.labelMedium)
                        }
                        LiquidGlassPillButton(
                            onClick = { draft = draft.copy(recentlyAddedOnly = !draft.recentlyAddedOnly) },
                            selected = draft.recentlyAddedOnly,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("music_filter_recently_added_toggle")
                        ) {
                            Text("Recently Added", style = MaterialTheme.typography.labelMedium)
                        }
                        LiquidGlassPillButton(
                            onClick = { draft = draft.copy(recentlyPlayedOnly = !draft.recentlyPlayedOnly) },
                            selected = draft.recentlyPlayedOnly,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("music_filter_recently_played_toggle")
                        ) {
                            Text("Recently Played", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                item {
                    Text("Duration Range", style = MaterialTheme.typography.titleSmall, color = filterPrimary)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DurationRangeFilter.entries.forEach { range ->
                            val selected = range in draft.selectedDurationRanges
                            LiquidGlassPillButton(
                                onClick = {
                                    val next = if (selected) draft.selectedDurationRanges - range else draft.selectedDurationRanges + range
                                    draft = draft.copy(selectedDurationRanges = next)
                                },
                                selected = selected,
                                accentColor = ElectricIndigo,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(range.label, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                if (availableArtists.isNotEmpty()) {
                    item {
                        Text("Artists", style = MaterialTheme.typography.titleSmall, color = filterPrimary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableArtists.forEach { artist ->
                                val selected = artist in draft.selectedArtists
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedArtists - artist else draft.selectedArtists + artist
                                        draft = draft.copy(selectedArtists = next)
                                    },
                                    selected = selected,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(artist, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                if (availableAlbums.isNotEmpty()) {
                    item {
                        Text("Albums", style = MaterialTheme.typography.titleSmall, color = filterPrimary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableAlbums.forEach { album ->
                                val selected = album in draft.selectedAlbums
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedAlbums - album else draft.selectedAlbums + album
                                        draft = draft.copy(selectedAlbums = next)
                                    },
                                    selected = selected,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(album, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                if (availableGenres.isNotEmpty()) {
                    item {
                        Text("Genres", style = MaterialTheme.typography.titleSmall, color = filterPrimary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableGenres.forEach { genre ->
                                val selected = genre in draft.selectedGenres
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedGenres - genre else draft.selectedGenres + genre
                                        draft = draft.copy(selectedGenres = next)
                                    },
                                    selected = selected,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(genre, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                if (availableFolders.isNotEmpty()) {
                    item {
                        Text("Folders", style = MaterialTheme.typography.titleSmall, color = filterPrimary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableFolders.forEach { folder ->
                                val selected = folder in draft.selectedFolders
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedFolders - folder else draft.selectedFolders + folder
                                        draft = draft.copy(selectedFolders = next)
                                    },
                                    selected = selected,
                                    accentColor = GlassAmberTint,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(folder, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }

                if (availableCodecs.isNotEmpty()) {
                    item {
                        Text("Audio Formats / Codecs", style = MaterialTheme.typography.titleSmall, color = filterPrimary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableCodecs.forEach { codec ->
                                val selected = codec in draft.selectedCodecs
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedCodecs - codec else draft.selectedCodecs + codec
                                        draft = draft.copy(selectedCodecs = next)
                                    },
                                    selected = selected,
                                    accentColor = ElectricIndigo,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(codec, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun <T> buildPresentOptionCounts(
    options: List<T>,
    selected: Set<T>,
    videos: List<MediaEntity>,
    classifier: (MediaEntity) -> Set<T>
): List<Pair<T, Int>> {
    if (videos.isEmpty()) {
        return options.map { it to 0 }
    }
    val counts = LinkedHashMap<T, Int>()
    for (opt in options) counts[opt] = 0
    for (video in videos) {
        for (matched in classifier(video)) {
            counts[matched] = (counts[matched] ?: 0) + 1
        }
    }
    return options.mapNotNull { opt ->
        val count = counts[opt] ?: 0
        if (count > 0 || opt in selected) opt to count else null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoFilterDropdownCard(
    title: String,
    selectedCount: Int,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    accentColor: Color,
    headerTag: String,
    content: @Composable () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(resolveContextualAccent(accentColor, glassConfig), glassConfig.isDarkGlass)
    LiquidGlassCard(
        accentTint = if (selectedCount > 0) accentColor else ElectricIndigo,
        elevated = selectedCount > 0 || expanded,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        onClick = onToggleExpand,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(headerTag)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selectedCount > 0) resolvedAccent else MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(resolvedAccent.copy(alpha = 0.22f))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$selectedCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = resolvedAccent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse $title" else "Expand $title",
                    tint = if (expanded || selectedCount > 0) resolvedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BnjVideoFilterBottomSheet(
    initialFilter: VideoFilterState,
    availableFolders: List<String>,
    @Suppress("UNUSED_PARAMETER") availableFormats: List<String> = emptyList(),
    videos: List<MediaEntity> = emptyList(),
    inspectedStreamsByUri: Map<String, VideoStreamInspection> = emptyMap(),
    searchQuery: String = "",
    onApplyFilter: (VideoFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initialFilter) { mutableStateOf(initialFilter) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var extExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedExtensions.isNotEmpty()) }
    var resExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedResolutions.isNotEmpty()) }
    var vcodecExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedVideoCodecs.isNotEmpty()) }
    var hdrExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedHdrFormats.isNotEmpty()) }
    var acodecExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedAudioCodecs.isNotEmpty()) }
    var durationExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedVideoDurations.isNotEmpty()) }
    var sizeExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedVideoSizes.isNotEmpty()) }
    var foldersExpanded by remember(initialFilter) { mutableStateOf(initialFilter.selectedFolders.isNotEmpty()) }

    val matchingVideoCount = remember(draft, videos, inspectedStreamsByUri, searchQuery) {
        LibraryQueryEngine.filterAndSortVideos(
            videos = videos,
            searchQuery = searchQuery,
            filter = draft,
            sortField = VideoSortField.TITLE,
            ascending = true,
            inspectedStreamsByUri = inspectedStreamsByUri
        ).size
    }

    val extensionOptions = remember(videos, draft.selectedExtensions, inspectedStreamsByUri) {
        buildPresentOptionCounts(
            options = VideoExtensionFilter.entries,
            selected = draft.selectedExtensions,
            videos = videos
        ) { video ->
            val inspected = inspectedStreamsByUri[video.uri]
                ?: CodecCapabilityInspector.getCachedVideoStreamInspection(video.uri, video.sizeBytes)
            VideoMetadataClassifier.classifyExtension(video, inspected)?.let { setOf(it) } ?: emptySet()
        }
    }

    val resolutionOptions = remember(videos, draft.selectedResolutions) {
        buildPresentOptionCounts(
            options = VideoResolutionFilter.entries,
            selected = draft.selectedResolutions,
            videos = videos
        ) { video ->
            setOf(VideoMetadataClassifier.classifyResolution(video))
        }
    }

    val videoCodecOptions = remember(videos, draft.selectedVideoCodecs, inspectedStreamsByUri) {
        buildPresentOptionCounts(
            options = VideoCodecFilter.entries,
            selected = draft.selectedVideoCodecs,
            videos = videos
        ) { video ->
            val inspected = inspectedStreamsByUri[video.uri]
                ?: CodecCapabilityInspector.getCachedVideoStreamInspection(video.uri, video.sizeBytes)
            setOf(VideoMetadataClassifier.classifyVideoCodec(video, inspected))
        }
    }

    val hdrOptions = remember(videos, draft.selectedHdrFormats, inspectedStreamsByUri) {
        buildPresentOptionCounts(
            options = VideoHdrFilter.entries,
            selected = draft.selectedHdrFormats,
            videos = videos
        ) { video ->
            val inspected = inspectedStreamsByUri[video.uri]
                ?: CodecCapabilityInspector.getCachedVideoStreamInspection(video.uri, video.sizeBytes)
            setOf(VideoMetadataClassifier.classifyHdr(video, inspected))
        }
    }

    val audioCodecOptions = remember(videos, draft.selectedAudioCodecs, inspectedStreamsByUri) {
        buildPresentOptionCounts(
            options = VideoAudioCodecFilter.entries,
            selected = draft.selectedAudioCodecs,
            videos = videos
        ) { video ->
            val inspected = inspectedStreamsByUri[video.uri]
                ?: CodecCapabilityInspector.getCachedVideoStreamInspection(video.uri, video.sizeBytes)
            VideoMetadataClassifier.classifyAudioCodecs(video, inspected)
        }
    }

    val durationOptions = remember(videos, draft.selectedVideoDurations) {
        buildPresentOptionCounts(
            options = VideoDurationRangeFilter.entries,
            selected = draft.selectedVideoDurations,
            videos = videos
        ) { video ->
            VideoDurationRangeFilter.entries.filter { it.matches(video.durationMs) }.toSet()
        }
    }

    val sizeOptions = remember(videos, draft.selectedVideoSizes) {
        buildPresentOptionCounts(
            options = VideoFileSizeRangeFilter.entries,
            selected = draft.selectedVideoSizes,
            videos = videos
        ) { video ->
            VideoFileSizeRangeFilter.entries.filter { it.matches(video.sizeBytes) }.toSet()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = dynamicGlassDialogContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("video_filter_bottom_sheet")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val glassConfig = LocalLiquidGlassConfig.current
                    val isDarkGlass = glassConfig.isDarkGlass
                    val resolvedEmerald = adaptiveGlassAccent(GlassEmeraldTint, isDarkGlass)
                    val resolvedCyan = adaptiveGlassAccent(resolveContextualAccent(LiquidCyan, glassConfig), isDarkGlass)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Filter Videos",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        if (draft.activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(resolvedEmerald.copy(alpha = 0.22f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                    .testTag("video_filter_selected_count")
                            ) {
                                Text(
                                    text = "${draft.activeFilterCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = resolvedEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        text = if (videos.isEmpty() && draft.activeFilterCount == 0) {
                            "All Videos"
                        } else {
                            "$matchingVideoCount matching ${if (matchingVideoCount == 1) "video" else "videos"}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = resolvedCyan,
                        modifier = Modifier.testTag("video_filter_matching_count")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassPillButton(
                        onClick = { draft = VideoFilterState() },
                        accentColor = GlassRoseTint,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                        modifier = Modifier.testTag("video_filter_clear_all_button")
                    ) {
                        Text("Clear All", style = MaterialTheme.typography.labelMedium, color = GlassRoseTint)
                    }
                    LiquidGlassPillButton(
                        onClick = {
                            onApplyFilter(draft)
                            onDismiss()
                        },
                        selected = true,
                        accentColor = GlassEmeraldTint,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                        modifier = Modifier.testTag("video_filter_apply_button")
                    ) {
                        Text("Apply Filters", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LiquidGlassPillButton(
                            onClick = { draft = VideoFilterState() },
                            selected = draft.activeFilterCount == 0,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("All Videos", style = MaterialTheme.typography.labelMedium)
                        }
                        LiquidGlassPillButton(
                            onClick = { draft = draft.copy(recentlyPlayedOnly = !draft.recentlyPlayedOnly) },
                            selected = draft.recentlyPlayedOnly,
                            accentColor = GlassEmeraldTint,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("video_filter_recently_played_toggle")
                        ) {
                            Text("Recently Played", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (extensionOptions.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "Video Extensions",
                            selectedCount = draft.selectedExtensions.size,
                            expanded = extExpanded,
                            onToggleExpand = { extExpanded = !extExpanded },
                            accentColor = LiquidCyan,
                            headerTag = "video_filter_dropdown_extensions"
                        ) {
                            extensionOptions.forEach { (extFilter, count) ->
                                val selected = extFilter in draft.selectedExtensions
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedExtensions - extFilter else draft.selectedExtensions + extFilter
                                        draft = draft.copy(selectedExtensions = next)
                                    },
                                    selected = selected,
                                    accentColor = LiquidCyan,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_ext_${extFilter.ext}")
                                ) {
                                    Text(
                                        text = if (count > 0) "${extFilter.label} ($count)" else extFilter.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (resolutionOptions.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "Resolution",
                            selectedCount = draft.selectedResolutions.size,
                            expanded = resExpanded,
                            onToggleExpand = { resExpanded = !resExpanded },
                            accentColor = ElectricIndigo,
                            headerTag = "video_filter_dropdown_resolution"
                        ) {
                            resolutionOptions.forEach { (res, count) ->
                                val selected = res in draft.selectedResolutions
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedResolutions - res else draft.selectedResolutions + res
                                        draft = draft.copy(selectedResolutions = next)
                                    },
                                    selected = selected,
                                    accentColor = ElectricIndigo,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_res_${res.name.lowercase(Locale.US)}")
                                ) {
                                    Text(
                                        text = if (count > 0) "${res.label} ($count)" else res.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (videoCodecOptions.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "Video Codec",
                            selectedCount = draft.selectedVideoCodecs.size,
                            expanded = vcodecExpanded,
                            onToggleExpand = { vcodecExpanded = !vcodecExpanded },
                            accentColor = GlassEmeraldTint,
                            headerTag = "video_filter_dropdown_video_codec"
                        ) {
                            videoCodecOptions.forEach { (codec, count) ->
                                val selected = codec in draft.selectedVideoCodecs
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedVideoCodecs - codec else draft.selectedVideoCodecs + codec
                                        draft = draft.copy(selectedVideoCodecs = next)
                                    },
                                    selected = selected,
                                    accentColor = GlassEmeraldTint,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_vcodec_${codec.name.lowercase(Locale.US)}")
                                ) {
                                    Text(
                                        text = if (count > 0) "${codec.label} ($count)" else codec.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (hdrOptions.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "HDR Format",
                            selectedCount = draft.selectedHdrFormats.size,
                            expanded = hdrExpanded,
                            onToggleExpand = { hdrExpanded = !hdrExpanded },
                            accentColor = GlassAmberTint,
                            headerTag = "video_filter_dropdown_hdr"
                        ) {
                            hdrOptions.forEach { (hdr, count) ->
                                val selected = hdr in draft.selectedHdrFormats
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedHdrFormats - hdr else draft.selectedHdrFormats + hdr
                                        draft = draft.copy(selectedHdrFormats = next)
                                    },
                                    selected = selected,
                                    accentColor = GlassAmberTint,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_hdr_${hdr.name.lowercase(Locale.US)}")
                                ) {
                                    Text(
                                        text = if (count > 0) "${hdr.label} ($count)" else hdr.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (audioCodecOptions.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "Audio Codec",
                            selectedCount = draft.selectedAudioCodecs.size,
                            expanded = acodecExpanded,
                            onToggleExpand = { acodecExpanded = !acodecExpanded },
                            accentColor = LiquidCyan,
                            headerTag = "video_filter_dropdown_audio_codec"
                        ) {
                            audioCodecOptions.forEach { (acodec, count) ->
                                val selected = acodec in draft.selectedAudioCodecs
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedAudioCodecs - acodec else draft.selectedAudioCodecs + acodec
                                        draft = draft.copy(selectedAudioCodecs = next)
                                    },
                                    selected = selected,
                                    accentColor = LiquidCyan,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_acodec_${acodec.name.lowercase(Locale.US)}")
                                ) {
                                    Text(
                                        text = if (count > 0) "${acodec.label} ($count)" else acodec.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (durationOptions.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "Duration",
                            selectedCount = draft.selectedVideoDurations.size,
                            expanded = durationExpanded,
                            onToggleExpand = { durationExpanded = !durationExpanded },
                            accentColor = ElectricIndigo,
                            headerTag = "video_filter_dropdown_duration"
                        ) {
                            durationOptions.forEach { (dur, count) ->
                                val selected = dur in draft.selectedVideoDurations
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedVideoDurations - dur else draft.selectedVideoDurations + dur
                                        draft = draft.copy(selectedVideoDurations = next)
                                    },
                                    selected = selected,
                                    accentColor = ElectricIndigo,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_dur_${dur.name.lowercase(Locale.US)}")
                                ) {
                                    Text(
                                        text = if (count > 0) "${dur.label} ($count)" else dur.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (sizeOptions.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "File Size",
                            selectedCount = draft.selectedVideoSizes.size,
                            expanded = sizeExpanded,
                            onToggleExpand = { sizeExpanded = !sizeExpanded },
                            accentColor = GlassRoseTint,
                            headerTag = "video_filter_dropdown_size"
                        ) {
                            sizeOptions.forEach { (sz, count) ->
                                val selected = sz in draft.selectedVideoSizes
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedVideoSizes - sz else draft.selectedVideoSizes + sz
                                        draft = draft.copy(selectedVideoSizes = next)
                                    },
                                    selected = selected,
                                    accentColor = GlassRoseTint,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_size_${sz.name.lowercase(Locale.US)}")
                                ) {
                                    Text(
                                        text = if (count > 0) "${sz.label} ($count)" else sz.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (availableFolders.isNotEmpty()) {
                    item {
                        VideoFilterDropdownCard(
                            title = "Folders",
                            selectedCount = draft.selectedFolders.size,
                            expanded = foldersExpanded,
                            onToggleExpand = { foldersExpanded = !foldersExpanded },
                            accentColor = GlassAmberTint,
                            headerTag = "video_filter_dropdown_folders"
                        ) {
                            availableFolders.forEach { folder ->
                                val selected = folder in draft.selectedFolders
                                LiquidGlassPillButton(
                                    onClick = {
                                        val next = if (selected) draft.selectedFolders - folder else draft.selectedFolders + folder
                                        draft = draft.copy(selectedFolders = next)
                                    },
                                    selected = selected,
                                    accentColor = GlassAmberTint,
                                    contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("video_filter_folder_${folder.lowercase(Locale.US)}")
                                ) {
                                    Text(folder, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BnjFileFilterBottomSheet(
    initialFilter: FileFilterState,
    onApplyFilter: (FileFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    var draft by remember(initialFilter) { mutableStateOf(initialFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = dynamicGlassDialogContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .testTag("files_filter_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val fileFilterConfig = LocalLiquidGlassConfig.current
            val fileFilterPrimary = adaptiveGlassAccent(resolveContextualAccent(LiquidCyan, fileFilterConfig), fileFilterConfig.isDarkGlass)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filter File Manager",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (draft.activeFilterCount == 0) "Showing all items" else "${draft.activeFilterCount} categories selected",
                        style = MaterialTheme.typography.labelMedium,
                        color = fileFilterPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassPillButton(
                        onClick = { draft = FileFilterState() },
                        accentColor = GlassRoseTint,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("files_filter_clear_all_button")
                    ) {
                        Text("Clear All", style = MaterialTheme.typography.labelMedium, color = GlassRoseTint)
                    }
                    LiquidGlassPillButton(
                        onClick = {
                            onApplyFilter(draft)
                            onDismiss()
                        },
                        selected = true,
                        accentColor = GlassEmeraldTint,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("files_filter_apply_button")
                    ) {
                        Text("Apply Filters", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Text(
                text = "File Categories (Select one or more)",
                style = MaterialTheme.typography.titleSmall,
                color = fileFilterPrimary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FileCategoryFilter.entries.forEach { cat ->
                    val selected = cat in draft.selectedCategories
                    LiquidGlassPillButton(
                        onClick = {
                            val next = if (selected) draft.selectedCategories - cat else draft.selectedCategories + cat
                            draft = draft.copy(selectedCategories = next)
                        },
                        selected = selected,
                        accentColor = ElectricIndigo,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("files_filter_cat_${cat.name.lowercase(Locale.US)}")
                    ) {
                        Text(cat.label, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun BnjEmptyFilterStateCard(
    title: String,
    subtitle: String,
    onClearFilters: () -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${tagPrefix}_empty_filter_state"),
        accentTint = GlassAmberTint,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FilterAltOff,
                contentDescription = null,
                tint = GlassAmberTint,
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            LiquidGlassPillButton(
                onClick = onClearFilters,
                selected = true,
                accentColor = LiquidCyan,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.testTag("${tagPrefix}_empty_reset_button")
            ) {
                Text("Reset Search & Filters", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
