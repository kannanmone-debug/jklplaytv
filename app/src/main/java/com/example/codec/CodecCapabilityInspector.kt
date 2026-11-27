package com.example.codec

import android.content.Context
import android.hardware.display.DisplayManager
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.Display
import java.io.File
import java.util.Locale

data class CodecSupportReport(
    val displayName: String,
    val mimeTypes: List<String>,
    val hasHardwareDecoder: Boolean,
    val hasSoftwareDecoder: Boolean,
    val decoderNames: List<String>,
    val maxResolution: String = "Audio Stream",
    val hdrProfilesSupported: List<String> = emptyList()
) {
    val isSupportedOnDevice: Boolean
        get() = hasHardwareDecoder || hasSoftwareDecoder
}

enum class MediaFormatCategory {
    AUDIO,
    VIDEO
}

/**
 * Definition of a supported local media format and extension in BNJ MEDIA PRO.
 * Strictly distinguishes extension/container recognition from actual device decoder support.
 */
data class MediaFormatSpec(
    val extension: String, // lowercase without dot, e.g. "mp3", "mkv"
    val category: MediaFormatCategory,
    val formatName: String,
    val containerFormat: String,
    val primaryCodecMime: String,
    val primaryCodecLabel: String,
    val candidateDecoderMimes: List<String>,
    val isPcmOrExtractorNative: Boolean = false
) {
    val dotExtension: String get() = ".$extension"
}

/**
 * 5-stage compatibility evaluation for a format or media item:
 * 1. Extension recognized
 * 2. Container detected
 * 3. Audio/video codec identified
 * 4. Device decoder available
 * 5. Playback actually successful (null = not yet attempted, true = verified at runtime, false = failed at runtime)
 */
data class FormatCompatibilityStatus(
    val spec: MediaFormatSpec?,
    val normalizedExtension: String,
    val category: MediaFormatCategory,
    val extensionRecognized: Boolean,
    val containerDetected: Boolean,
    val containerFormat: String,
    val codecIdentified: Boolean,
    val codecMime: String,
    val codecLabel: String,
    val deviceDecoderAvailable: Boolean,
    val hasHardwareDecoder: Boolean,
    val hasSoftwareDecoder: Boolean,
    val decoderNames: List<String>,
    val playbackSuccessful: Boolean? = null,
    val statusBadge: String,
    val compatibilitySummary: String
)

data class DeviceMediaCapabilities(
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
    val androidSdk: Int = Build.VERSION.SDK_INT,
    val videoCodecs: List<CodecSupportReport> = emptyList(),
    val audioCodecs: List<CodecSupportReport> = emptyList(),
    val displaySupportsHdr10: Boolean = false,
    val displaySupportsHdr10Plus: Boolean = false,
    val displaySupportsHlg: Boolean = false,
    val displaySupportsDolbyVision: Boolean = false,
    val maxDisplayLuminanceNits: Float = 1000f,
    val fallbackEngineStatus: String = "Media3 Hardware/Software Active",
    val audioFormatCompatibility: List<FormatCompatibilityStatus> = emptyList(),
    val videoFormatCompatibility: List<FormatCompatibilityStatus> = emptyList()
)

data class ProbedMediaMetadata(
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val bitrate: Int = 0,
    val sampleRateHz: Int = 44100,
    val channelCount: Int = 2,
    val hdrFormat: String = "SDR",
    val genre: String = ""
)

data class VideoStreamInspection(
    val uri: String,
    val canonicalExtension: String,
    val videoCodecLabel: String? = null,
    val videoMime: String? = null,
    val audioCodecLabels: List<String> = emptyList(),
    val audioTrackCount: Int? = null,
    val embeddedSubtitleTrackCount: Int? = null,
    val sidecarSubtitleNames: List<String> = emptyList(),
    val frameRate: Float? = null,
    val bitrate: Int? = null,
    val hdrFormat: String? = null,
    val sampleRateHz: Int? = null,
    val channelCount: Int? = null,
    val inspectionSucceeded: Boolean = false
)

object CodecCapabilityInspector {

    private val videoStreamCache = object : LinkedHashMap<String, VideoStreamInspection>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, VideoStreamInspection>?): Boolean {
            return size > 128
        }
    }

    /**
     * Complete list of 19 common local audio extensions inspected and managed by BNJ MEDIA PRO:
     * .mp3, .m4a, .aac, .flac, .wav, .ogg, .opus, .wma, .aiff, .ape, .amr, .mid, .midi, .3gp, .ac3, .eac3, .mka, .mp2, .alac
     */
    val SUPPORTED_AUDIO_FORMAT_SPECS: List<MediaFormatSpec> = listOf(
        MediaFormatSpec(
            extension = "mp3",
            category = MediaFormatCategory.AUDIO,
            formatName = "MP3 (MPEG-1/2 Audio Layer III)",
            containerFormat = "MPEG Audio Stream",
            primaryCodecMime = "audio/mpeg",
            primaryCodecLabel = "MP3",
            candidateDecoderMimes = listOf("audio/mpeg")
        ),
        MediaFormatSpec(
            extension = "m4a",
            category = MediaFormatCategory.AUDIO,
            formatName = "M4A (MPEG-4 Audio Container)",
            containerFormat = "MP4 / ISO-BMFF (M4A)",
            primaryCodecMime = "audio/mp4",
            primaryCodecLabel = "AAC / M4A",
            candidateDecoderMimes = listOf("audio/mp4a-latm", "audio/mp4", "audio/alac")
        ),
        MediaFormatSpec(
            extension = "aac",
            category = MediaFormatCategory.AUDIO,
            formatName = "AAC (Advanced Audio Coding)",
            containerFormat = "ADTS / AAC Audio",
            primaryCodecMime = "audio/aac",
            primaryCodecLabel = "AAC",
            candidateDecoderMimes = listOf("audio/mp4a-latm", "audio/aac")
        ),
        MediaFormatSpec(
            extension = "flac",
            category = MediaFormatCategory.AUDIO,
            formatName = "FLAC (Free Lossless Audio Codec)",
            containerFormat = "FLAC Lossless Container",
            primaryCodecMime = "audio/flac",
            primaryCodecLabel = "FLAC Lossless",
            candidateDecoderMimes = listOf("audio/flac")
        ),
        MediaFormatSpec(
            extension = "wav",
            category = MediaFormatCategory.AUDIO,
            formatName = "WAV (Waveform PCM Audio)",
            containerFormat = "RIFF / WAVE Container",
            primaryCodecMime = "audio/raw",
            primaryCodecLabel = "WAV PCM",
            candidateDecoderMimes = listOf("audio/raw", "audio/x-wav", "audio/wav"),
            isPcmOrExtractorNative = true
        ),
        MediaFormatSpec(
            extension = "ogg",
            category = MediaFormatCategory.AUDIO,
            formatName = "OGG (Ogg Vorbis Audio)",
            containerFormat = "Ogg Bitstream Container",
            primaryCodecMime = "audio/ogg",
            primaryCodecLabel = "OGG Vorbis",
            candidateDecoderMimes = listOf("audio/vorbis", "audio/ogg")
        ),
        MediaFormatSpec(
            extension = "opus",
            category = MediaFormatCategory.AUDIO,
            formatName = "Opus (Low-Latency Interactive Audio)",
            containerFormat = "Ogg Opus Container",
            primaryCodecMime = "audio/opus",
            primaryCodecLabel = "Opus",
            candidateDecoderMimes = listOf("audio/opus")
        ),
        MediaFormatSpec(
            extension = "wma",
            category = MediaFormatCategory.AUDIO,
            formatName = "WMA (Windows Media Audio)",
            containerFormat = "ASF / Windows Media",
            primaryCodecMime = "audio/x-ms-wma",
            primaryCodecLabel = "WMA",
            candidateDecoderMimes = listOf("audio/x-ms-wma", "audio/wma")
        ),
        MediaFormatSpec(
            extension = "aiff",
            category = MediaFormatCategory.AUDIO,
            formatName = "AIFF (Audio Interchange File Format)",
            containerFormat = "IFF / AIFF PCM Container",
            primaryCodecMime = "audio/x-aiff",
            primaryCodecLabel = "AIFF PCM",
            candidateDecoderMimes = listOf("audio/x-aiff", "audio/aiff")
        ),
        MediaFormatSpec(
            extension = "ape",
            category = MediaFormatCategory.AUDIO,
            formatName = "APE (Monkey's Audio Lossless)",
            containerFormat = "Monkey's Audio Container",
            primaryCodecMime = "audio/x-ape",
            primaryCodecLabel = "APE Lossless",
            candidateDecoderMimes = listOf("audio/x-ape", "audio/ape")
        ),
        MediaFormatSpec(
            extension = "amr",
            category = MediaFormatCategory.AUDIO,
            formatName = "AMR (Adaptive Multi-Rate Speech)",
            containerFormat = "AMR / 3GPP Speech",
            primaryCodecMime = "audio/3gpp",
            primaryCodecLabel = "AMR Speech",
            candidateDecoderMimes = listOf("audio/3gpp", "audio/amr-wb")
        ),
        MediaFormatSpec(
            extension = "mid",
            category = MediaFormatCategory.AUDIO,
            formatName = "MID (Standard MIDI Sequence)",
            containerFormat = "Standard MIDI File (SMF)",
            primaryCodecMime = "audio/midi",
            primaryCodecLabel = "MIDI",
            candidateDecoderMimes = listOf("audio/midi", "audio/x-midi")
        ),
        MediaFormatSpec(
            extension = "midi",
            category = MediaFormatCategory.AUDIO,
            formatName = "MIDI (Musical Instrument Digital Interface)",
            containerFormat = "Standard MIDI File (SMF)",
            primaryCodecMime = "audio/midi",
            primaryCodecLabel = "MIDI",
            candidateDecoderMimes = listOf("audio/midi", "audio/x-midi")
        ),
        MediaFormatSpec(
            extension = "3gp",
            category = MediaFormatCategory.AUDIO,
            formatName = "3GP Audio (3GPP Mobile Audio)",
            containerFormat = "3GPP / ISO-BMFF Container",
            primaryCodecMime = "audio/3gpp",
            primaryCodecLabel = "3GP Audio",
            candidateDecoderMimes = listOf("audio/3gpp", "audio/amr-wb", "audio/mp4a-latm")
        ),
        MediaFormatSpec(
            extension = "ac3",
            category = MediaFormatCategory.AUDIO,
            formatName = "AC-3 (Dolby Digital Surround)",
            containerFormat = "AC-3 Audio Stream",
            primaryCodecMime = "audio/ac3",
            primaryCodecLabel = "AC-3 Dolby",
            candidateDecoderMimes = listOf("audio/ac3")
        ),
        MediaFormatSpec(
            extension = "eac3",
            category = MediaFormatCategory.AUDIO,
            formatName = "E-AC-3 (Dolby Digital Plus)",
            containerFormat = "E-AC-3 / Dolby Atmos Stream",
            primaryCodecMime = "audio/eac3",
            primaryCodecLabel = "E-AC3 DD+",
            candidateDecoderMimes = listOf("audio/eac3", "audio/eac3-joc")
        ),
        MediaFormatSpec(
            extension = "mka",
            category = MediaFormatCategory.AUDIO,
            formatName = "MKA (Matroska Audio Container)",
            containerFormat = "Matroska Audio (MKA)",
            primaryCodecMime = "audio/x-matroska",
            primaryCodecLabel = "MKA Audio",
            candidateDecoderMimes = listOf("audio/opus", "audio/vorbis", "audio/flac", "audio/mp4a-latm")
        ),
        MediaFormatSpec(
            extension = "mp2",
            category = MediaFormatCategory.AUDIO,
            formatName = "MP2 (MPEG-1 Audio Layer II)",
            containerFormat = "MPEG-1/2 Layer II Stream",
            primaryCodecMime = "audio/mpeg-L2",
            primaryCodecLabel = "MP2 Audio",
            candidateDecoderMimes = listOf("audio/mpeg-L2", "audio/mpeg")
        ),
        MediaFormatSpec(
            extension = "alac",
            category = MediaFormatCategory.AUDIO,
            formatName = "ALAC (Apple Lossless Audio Codec)",
            containerFormat = "ISO-BMFF / ALAC Container",
            primaryCodecMime = "audio/alac",
            primaryCodecLabel = "ALAC Lossless",
            candidateDecoderMimes = listOf("audio/alac")
        )
    )

    /**
     * Complete list of 16 common local video extensions inspected and managed by BNJ MEDIA PRO:
     * .mp4, .mkv, .avi, .mov, .3gp, .webm, .m4v, .ts, .mpeg, .mpg, .flv, .wmv, .vob, .ogv, .mts, .m2ts
     */
    val SUPPORTED_VIDEO_FORMAT_SPECS: List<MediaFormatSpec> = listOf(
        MediaFormatSpec(
            extension = "mp4",
            category = MediaFormatCategory.VIDEO,
            formatName = "MP4 (MPEG-4 Part 14 Container)",
            containerFormat = "MP4 / ISO-BMFF",
            primaryCodecMime = "video/mp4",
            primaryCodecLabel = "MP4",
            candidateDecoderMimes = listOf("video/avc", "video/hevc", "video/mp4v-es", "video/av01")
        ),
        MediaFormatSpec(
            extension = "mkv",
            category = MediaFormatCategory.VIDEO,
            formatName = "MKV (Matroska Multimedia Container)",
            containerFormat = "Matroska (MKV)",
            primaryCodecMime = "video/x-matroska",
            primaryCodecLabel = "MKV",
            candidateDecoderMimes = listOf("video/hevc", "video/avc", "video/x-vnd.on2.vp9", "video/av01")
        ),
        MediaFormatSpec(
            extension = "avi",
            category = MediaFormatCategory.VIDEO,
            formatName = "AVI (Audio Video Interleave)",
            containerFormat = "RIFF / AVI Container",
            primaryCodecMime = "video/x-msvideo",
            primaryCodecLabel = "AVI",
            candidateDecoderMimes = listOf("video/mp4v-es", "video/avc")
        ),
        MediaFormatSpec(
            extension = "mov",
            category = MediaFormatCategory.VIDEO,
            formatName = "MOV (QuickTime Movie Container)",
            containerFormat = "Apple QuickTime (MOV)",
            primaryCodecMime = "video/quicktime",
            primaryCodecLabel = "MOV",
            candidateDecoderMimes = listOf("video/avc", "video/hevc")
        ),
        MediaFormatSpec(
            extension = "3gp",
            category = MediaFormatCategory.VIDEO,
            formatName = "3GP Video (3GPP Mobile Video)",
            containerFormat = "3GPP Multimedia Container",
            primaryCodecMime = "video/3gpp",
            primaryCodecLabel = "3GP",
            candidateDecoderMimes = listOf("video/3gpp", "video/mp4v-es", "video/avc")
        ),
        MediaFormatSpec(
            extension = "webm",
            category = MediaFormatCategory.VIDEO,
            formatName = "WebM (Open Web Media Container)",
            containerFormat = "WebM / Matroska Subset",
            primaryCodecMime = "video/webm",
            primaryCodecLabel = "WebM",
            candidateDecoderMimes = listOf("video/x-vnd.on2.vp9", "video/x-vnd.on2.vp8", "video/av01")
        ),
        MediaFormatSpec(
            extension = "m4v",
            category = MediaFormatCategory.VIDEO,
            formatName = "M4V (MPEG-4 Video Container)",
            containerFormat = "MP4 / M4V ISO-BMFF",
            primaryCodecMime = "video/x-m4v",
            primaryCodecLabel = "M4V",
            candidateDecoderMimes = listOf("video/avc", "video/hevc", "video/mp4v-es")
        ),
        MediaFormatSpec(
            extension = "ts",
            category = MediaFormatCategory.VIDEO,
            formatName = "TS (MPEG-2 Transport Stream)",
            containerFormat = "MPEG Transport Stream (TS)",
            primaryCodecMime = "video/mp2t",
            primaryCodecLabel = "MPEG-TS",
            candidateDecoderMimes = listOf("video/avc", "video/hevc", "video/mpeg2")
        ),
        MediaFormatSpec(
            extension = "mpeg",
            category = MediaFormatCategory.VIDEO,
            formatName = "MPEG (MPEG-1/2 Program Stream)",
            containerFormat = "MPEG Program Stream",
            primaryCodecMime = "video/mpeg",
            primaryCodecLabel = "MPEG",
            candidateDecoderMimes = listOf("video/mpeg2", "video/mpeg")
        ),
        MediaFormatSpec(
            extension = "mpg",
            category = MediaFormatCategory.VIDEO,
            formatName = "MPG (MPEG-1/2 Video Container)",
            containerFormat = "MPEG Program Stream (MPG)",
            primaryCodecMime = "video/mpeg",
            primaryCodecLabel = "MPG",
            candidateDecoderMimes = listOf("video/mpeg2", "video/mpeg")
        ),
        MediaFormatSpec(
            extension = "flv",
            category = MediaFormatCategory.VIDEO,
            formatName = "FLV (Flash Video Container)",
            containerFormat = "Adobe Flash Video (FLV)",
            primaryCodecMime = "video/x-flv",
            primaryCodecLabel = "FLV",
            candidateDecoderMimes = listOf("video/avc", "video/x-flv")
        ),
        MediaFormatSpec(
            extension = "wmv",
            category = MediaFormatCategory.VIDEO,
            formatName = "WMV (Windows Media Video)",
            containerFormat = "ASF / Windows Media Video",
            primaryCodecMime = "video/x-ms-wmv",
            primaryCodecLabel = "WMV",
            candidateDecoderMimes = listOf("video/x-ms-wmv", "video/wvc1")
        ),
        MediaFormatSpec(
            extension = "vob",
            category = MediaFormatCategory.VIDEO,
            formatName = "VOB (DVD Video Object)",
            containerFormat = "DVD-Video MPEG-2 PS (VOB)",
            primaryCodecMime = "video/mpeg2",
            primaryCodecLabel = "VOB",
            candidateDecoderMimes = listOf("video/mpeg2", "video/mpeg")
        ),
        MediaFormatSpec(
            extension = "ogv",
            category = MediaFormatCategory.VIDEO,
            formatName = "OGV (Ogg Video Container)",
            containerFormat = "Ogg Video Bitstream (OGV)",
            primaryCodecMime = "video/ogg",
            primaryCodecLabel = "OGV",
            candidateDecoderMimes = listOf("video/ogg", "video/x-theora")
        ),
        MediaFormatSpec(
            extension = "mts",
            category = MediaFormatCategory.VIDEO,
            formatName = "MTS (AVCHD High-Definition Stream)",
            containerFormat = "AVCHD MPEG-2 TS (MTS)",
            primaryCodecMime = "video/mp2t",
            primaryCodecLabel = "MTS",
            candidateDecoderMimes = listOf("video/avc", "video/hevc")
        ),
        MediaFormatSpec(
            extension = "m2ts",
            category = MediaFormatCategory.VIDEO,
            formatName = "M2TS (Blu-ray BDAV Transport Stream)",
            containerFormat = "BDAV MPEG-2 TS (M2TS)",
            primaryCodecMime = "video/mp2t",
            primaryCodecLabel = "M2TS",
            candidateDecoderMimes = listOf("video/avc", "video/hevc", "video/mpeg2")
        )
    )

    val ALL_AUDIO_EXTENSIONS: Set<String> =
        SUPPORTED_AUDIO_FORMAT_SPECS.map { it.extension }.toSet()

    val ALL_VIDEO_EXTENSIONS: Set<String> =
        SUPPORTED_VIDEO_FORMAT_SPECS.map { it.extension }.toSet()

    fun normalizeExtension(rawExtOrPath: String): String {
        val cleaned = rawExtOrPath.trim()
        if (cleaned.isEmpty()) return ""
        val afterSlash = cleaned.substringAfterLast('/')
        val ext = if (afterSlash.contains('.')) {
            afterSlash.substringAfterLast('.')
        } else {
            afterSlash.removePrefix(".")
        }
        return ext.lowercase(Locale.US).trim()
    }

    fun findAudioFormatSpec(extOrPath: String): MediaFormatSpec? {
        val norm = normalizeExtension(extOrPath)
        return SUPPORTED_AUDIO_FORMAT_SPECS.firstOrNull { it.extension == norm }
    }

    fun findVideoFormatSpec(extOrPath: String): MediaFormatSpec? {
        val norm = normalizeExtension(extOrPath)
        return SUPPORTED_VIDEO_FORMAT_SPECS.firstOrNull { it.extension == norm }
    }

    /**
     * Resolves the canonical lowercase extension for a MediaEntity or discovered file
     * using URI/filename, containerFormat, or MIME fallback.
     */
    fun resolveCanonicalExtension(
        uriOrPath: String,
        containerFormat: String,
        mimeType: String,
        title: String,
        isVideo: Boolean
    ): String {
        val validSet = if (isVideo) ALL_VIDEO_EXTENSIONS else ALL_AUDIO_EXTENSIONS

        // 1. Check URI / path file extension if present
        val uriLastSegment = uriOrPath.substringBefore('?').substringAfterLast('/')
        if (uriLastSegment.contains('.')) {
            val uriExt = uriLastSegment.substringAfterLast('.').lowercase(Locale.US)
            if (uriExt in validSet) return uriExt
        }

        // 2. Check containerFormat (e.g. "MP3", "FLAC", "M4A", "MKV")
        val cleanContainer = containerFormat.trim().removePrefix(".").lowercase(Locale.US)
        if (cleanContainer in validSet) return cleanContainer
        val firstToken = cleanContainer.substringBefore(' ').substringBefore('/')
        if (firstToken in validSet) return firstToken

        // 3. Check title if it still has an extension
        if (title.contains('.')) {
            val titleExt = title.substringAfterLast('.').lowercase(Locale.US)
            if (titleExt in validSet) return titleExt
        }

        // 4. Fallback to MIME type mapping
        val m = mimeType.lowercase(Locale.US)
        return if (isVideo) {
            when {
                m.contains("matroska") || m.contains("mkv") -> "mkv"
                m.contains("webm") -> "webm"
                m.contains("3gpp") || m.contains("3gp") -> "3gp"
                m.contains("avi") || m.contains("msvideo") -> "avi"
                m.contains("quicktime") || m.contains("mov") -> "mov"
                m.contains("m4v") -> "m4v"
                m.contains("mp2t") || m.contains("m2ts") -> "ts"
                m.contains("flv") -> "flv"
                m.contains("wmv") || m.contains("wvc1") -> "wmv"
                m.contains("vob") -> "vob"
                m.contains("ogg") || m.contains("theora") -> "ogv"
                m.contains("mpeg") -> "mpeg"
                else -> "mp4"
            }
        } else {
            when {
                m.contains("flac") -> "flac"
                m.contains("wav") || m.contains("raw") -> "wav"
                m.contains("opus") -> "opus"
                m.contains("vorbis") || m.contains("ogg") -> "ogg"
                m.contains("alac") -> "alac"
                m.contains("aac") -> "aac"
                m.contains("mp4") || m.contains("m4a") -> "m4a"
                m.contains("eac3") -> "eac3"
                m.contains("ac3") -> "ac3"
                m.contains("wma") -> "wma"
                m.contains("aiff") -> "aiff"
                m.contains("ape") -> "ape"
                m.contains("amr") -> "amr"
                m.contains("3gpp") -> "3gp"
                m.contains("midi") || m.contains("mid") -> "mid"
                m.contains("matroska") || m.contains("mka") -> "mka"
                m.contains("mpeg-l2") || m.contains("mp2") -> "mp2"
                else -> "mp3"
            }
        }
    }

    /**
     * Queries the device's real MediaCodecList and Display HDR capabilities.
     */
    fun inspectDeviceCapabilities(context: Context): DeviceMediaCapabilities {
        val codecInfos = try {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        } catch (_: Throwable) {
            emptyArray()
        }

        val videoTargets = listOf(
            "H.264 / AVC" to listOf("video/avc"),
            "H.265 / HEVC" to listOf("video/hevc"),
            "AV1" to listOf("video/av01"),
            "VP9" to listOf("video/x-vnd.on2.vp9"),
            "VP8" to listOf("video/x-vnd.on2.vp8"),
            "MPEG-4 SP" to listOf("video/mp4v-es"),
            "Dolby Vision" to listOf("video/dolby-vision")
        )

        val audioTargets = listOf(
            "AAC (Advanced Audio)" to listOf("audio/mp4a-latm", "audio/aac"),
            "MP3 (MPEG Layer 3)" to listOf("audio/mpeg"),
            "FLAC (Lossless)" to listOf("audio/flac"),
            "Opus" to listOf("audio/opus"),
            "Vorbis (OGG)" to listOf("audio/vorbis"),
            "ALAC (Apple Lossless)" to listOf("audio/alac"),
            "AC-3 (Dolby Digital)" to listOf("audio/ac3"),
            "E-AC-3 (Dolby Plus)" to listOf("audio/eac3", "audio/eac3-joc"),
            "RAW PCM (WAV)" to listOf("audio/raw")
        )

        val videoReports = videoTargets.map { (label, mimes) ->
            inspectMimeDecoders(codecInfos, label, mimes, isVideo = true)
        }

        val audioReports = audioTargets.map { (label, mimes) ->
            inspectMimeDecoders(codecInfos, label, mimes, isVideo = false)
        }

        val hdrStatus = inspectHdrCapabilities(context, codecInfos)

        val audioFormatStatuses = SUPPORTED_AUDIO_FORMAT_SPECS.map { spec ->
            evaluateFormatSpecCompatibility(spec, codecInfos)
        }

        val videoFormatStatuses = SUPPORTED_VIDEO_FORMAT_SPECS.map { spec ->
            evaluateFormatSpecCompatibility(spec, codecInfos)
        }

        return DeviceMediaCapabilities(
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
            androidSdk = Build.VERSION.SDK_INT,
            videoCodecs = videoReports,
            audioCodecs = audioReports,
            displaySupportsHdr10 = hdrStatus.hdr10,
            displaySupportsHdr10Plus = hdrStatus.hdr10Plus,
            displaySupportsHlg = hdrStatus.hlg,
            displaySupportsDolbyVision = hdrStatus.dolbyVision,
            maxDisplayLuminanceNits = hdrStatus.maxNits,
            audioFormatCompatibility = audioFormatStatuses,
            videoFormatCompatibility = videoFormatStatuses
        )
    }

    /**
     * Evaluates the 5-stage compatibility pipeline for a specific MediaFormatSpec
     * against the device's registered MediaCodec decoders.
     */
    fun evaluateFormatSpecCompatibility(
        spec: MediaFormatSpec,
        codecInfosInput: Array<MediaCodecInfo>? = null
    ): FormatCompatibilityStatus {
        val codecInfos = codecInfosInput ?: try {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        } catch (_: Throwable) {
            emptyArray()
        }

        val matchingDecoders = codecInfos.filter { info ->
            !info.isEncoder && info.supportedTypes.any { supportedMime ->
                spec.candidateDecoderMimes.any { candidate ->
                    supportedMime.equals(candidate, ignoreCase = true)
                }
            }
        }

        val hwDecoders = matchingDecoders.filter { isHardwareAccelerated(it) }
        val swDecoders = matchingDecoders.filter { !isHardwareAccelerated(it) }
        val decoderNames = matchingDecoders.map { it.name }
        val hasDeviceDecoder = matchingDecoders.isNotEmpty() || spec.isPcmOrExtractorNative

        val statusBadge: String
        val summary: String

        when {
            hwDecoders.isNotEmpty() && swDecoders.isNotEmpty() -> {
                statusBadge = "HW + SW Decoders Ready"
                summary = "Stage 1–4 Verified: .${spec.extension} (${spec.containerFormat}) • ${hwDecoders.size} HW & ${swDecoders.size} SW decoder(s) available"
            }
            hwDecoders.isNotEmpty() -> {
                statusBadge = "Hardware Accelerated"
                summary = "Stage 1–4 Verified: .${spec.extension} (${spec.containerFormat}) • Hardware MediaCodec (${hwDecoders.first().name})"
            }
            swDecoders.isNotEmpty() -> {
                statusBadge = "Software Decoder Ready"
                summary = "Stage 1–4 Verified: .${spec.extension} (${spec.containerFormat}) • Software MediaCodec (${swDecoders.first().name})"
            }
            spec.isPcmOrExtractorNative -> {
                statusBadge = "Native PCM Extractor"
                summary = "Stage 1–4 Verified: .${spec.extension} (${spec.containerFormat}) • Direct Media3 PCM AudioTrack pipeline"
            }
            else -> {
                statusBadge = "Recognized • Codec Dependent"
                summary = "Stage 1–3 Recognized (.${spec.extension} / ${spec.containerFormat}), Stage 4: No native ${spec.primaryCodecMime} MediaCodec registered on this device"
            }
        }

        return FormatCompatibilityStatus(
            spec = spec,
            normalizedExtension = spec.extension,
            category = spec.category,
            extensionRecognized = true,
            containerDetected = spec.containerFormat.isNotBlank(),
            containerFormat = spec.containerFormat,
            codecIdentified = spec.primaryCodecMime.isNotBlank(),
            codecMime = spec.primaryCodecMime,
            codecLabel = spec.primaryCodecLabel,
            deviceDecoderAvailable = hasDeviceDecoder,
            hasHardwareDecoder = hwDecoders.isNotEmpty(),
            hasSoftwareDecoder = swDecoders.isNotEmpty() || spec.isPcmOrExtractorNative,
            decoderNames = decoderNames,
            playbackSuccessful = null,
            statusBadge = statusBadge,
            compatibilitySummary = summary
        )
    }

    /**
     * Evaluates the full 5-stage diagnostic for a specific file or MediaEntity,
     * including optional runtime playback outcome (Stage 5).
     */
    fun evaluateFilePlaybackPipeline(
        fileNameOrUri: String,
        rawMimeType: String?,
        isVideo: Boolean,
        runtimePlaybackSuccess: Boolean? = null,
        runtimeErrorDetail: String? = null,
        codecInfosInput: Array<MediaCodecInfo>? = null
    ): FormatCompatibilityStatus {
        val ext = normalizeExtension(fileNameOrUri)
        val spec = if (isVideo) {
            findVideoFormatSpec(ext) ?: findAudioFormatSpec(ext)
        } else {
            findAudioFormatSpec(ext) ?: findVideoFormatSpec(ext)
        }

        val codecInfos = codecInfosInput ?: try {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        } catch (_: Throwable) {
            emptyArray()
        }

        if (spec == null) {
            val (resolvedMime, resolvedBadge) = resolveMimeAndCodecBadge(fileNameOrUri, rawMimeType, isVideo)
            return FormatCompatibilityStatus(
                spec = null,
                normalizedExtension = ext,
                category = if (isVideo) MediaFormatCategory.VIDEO else MediaFormatCategory.AUDIO,
                extensionRecognized = false,
                containerDetected = !rawMimeType.isNullOrBlank(),
                containerFormat = rawMimeType ?: "Unknown Container",
                codecIdentified = !rawMimeType.isNullOrBlank(),
                codecMime = resolvedMime,
                codecLabel = resolvedBadge,
                deviceDecoderAvailable = false,
                hasHardwareDecoder = false,
                hasSoftwareDecoder = false,
                decoderNames = emptyList(),
                playbackSuccessful = runtimePlaybackSuccess,
                statusBadge = "Unrecognized Extension",
                compatibilitySummary = "Extension '.${ext.ifEmpty { "unknown" }}' is not in the recognized format registry."
            )
        }

        val baseStatus = evaluateFormatSpecCompatibility(spec, codecInfos)
        if (runtimePlaybackSuccess == null) {
            return baseStatus
        }

        return if (runtimePlaybackSuccess) {
            baseStatus.copy(
                playbackSuccessful = true,
                statusBadge = "Playback Verified",
                compatibilitySummary = "Stage 1–5 Complete: .${spec.extension} (${spec.containerFormat}) decoded and playing via Media3 ExoPlayer."
            )
        } else {
            val reason = if (!baseStatus.deviceDecoderAvailable) {
                "Recognized .${spec.extension} (${spec.containerFormat}), but this device has no native MediaCodec decoder for ${spec.primaryCodecLabel} (${spec.primaryCodecMime})."
            } else {
                "Recognized .${spec.extension} (${spec.containerFormat}) with ${baseStatus.decoderNames.size} decoder(s), but stream playback failed${runtimeErrorDetail?.let { ": $it" } ?: "."}"
            }
            baseStatus.copy(
                playbackSuccessful = false,
                statusBadge = if (!baseStatus.deviceDecoderAvailable) "Unsupported Device Codec" else "Stream Decode Failed",
                compatibilitySummary = reason
            )
        }
    }

    private fun inspectMimeDecoders(
        codecInfos: Array<MediaCodecInfo>,
        displayName: String,
        mimeTypes: List<String>,
        isVideo: Boolean
    ): CodecSupportReport {
        val primaryMime = mimeTypes.first()
        val matchingDecoders = codecInfos.filter { info ->
            !info.isEncoder && info.supportedTypes.any { supported ->
                mimeTypes.any { target -> supported.equals(target, ignoreCase = true) }
            }
        }

        val hwDecoders = matchingDecoders.filter { isHardwareAccelerated(it) }
        val swDecoders = matchingDecoders.filter { !isHardwareAccelerated(it) }
        val decoderNames = matchingDecoders.map { it.name }

        val hdrProfiles = mutableListOf<String>()
        val maxSummary = if (matchingDecoders.isEmpty()) {
            if (primaryMime == "audio/raw") "PCM AudioTrack" else "No Native Decoder"
        } else {
            val primary = hwDecoders.firstOrNull() ?: matchingDecoders.first()
            try {
                val caps = primary.getCapabilitiesForType(primaryMime)
                if (isVideo) {
                    for (pl in caps.profileLevels) {
                        if (primaryMime.equals("video/hevc", ignoreCase = true)) {
                            if (pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 && "HDR10" !in hdrProfiles) {
                                hdrProfiles.add("HDR10")
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus &&
                                "HDR10+" !in hdrProfiles
                            ) {
                                hdrProfiles.add("HDR10+")
                            }
                        }
                    }
                    val vc = caps.videoCapabilities
                    if (vc != null) {
                        "${vc.supportedWidths.upper}x${vc.supportedHeights.upper}"
                    } else {
                        "Up to 4K UHD"
                    }
                } else {
                    val ac = caps.audioCapabilities
                    if (ac != null) {
                        "${ac.maxInputChannelCount}ch Audio"
                    } else {
                        "Stereo / Multichannel"
                    }
                }
            } catch (_: Throwable) {
                if (isVideo) "1080p / 4K" else "Audio Stream"
            }
        }

        return CodecSupportReport(
            displayName = displayName,
            mimeTypes = mimeTypes,
            hasHardwareDecoder = hwDecoders.isNotEmpty(),
            hasSoftwareDecoder = swDecoders.isNotEmpty() || primaryMime == "audio/raw",
            decoderNames = decoderNames,
            maxResolution = maxSummary,
            hdrProfilesSupported = hdrProfiles
        )
    }

    private fun isHardwareAccelerated(info: MediaCodecInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            info.isHardwareAccelerated
        } else {
            val name = info.name.lowercase(Locale.US)
            !name.startsWith("omx.google.") &&
                !name.startsWith("c2.android.") &&
                !name.contains(".sw.")
        }
    }

    private data class HdrProbeResult(
        val hdr10: Boolean,
        val hdr10Plus: Boolean,
        val hlg: Boolean,
        val dolbyVision: Boolean,
        val maxNits: Float
    )

    @Suppress("DEPRECATION")
    private fun inspectHdrCapabilities(
        context: Context,
        codecInfos: Array<MediaCodecInfo>
    ): HdrProbeResult {
        var displayHdr10 = false
        var displayHdr10Plus = false
        var displayHlg = false
        var displayDolbyVision = false
        var maxNits = 600f

        try {
            val dm = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            val display = dm?.getDisplay(Display.DEFAULT_DISPLAY)
            val hdrCaps = display?.hdrCapabilities
            val hdrTypes = hdrCaps?.supportedHdrTypes ?: IntArray(0)
            displayHdr10 = hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10)
            displayHlg = hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HLG)
            displayDolbyVision = hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                displayHdr10Plus = hdrTypes.contains(Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS)
            }
            val reportedLum = hdrCaps?.desiredMaxLuminance ?: 0f
            if (reportedLum > 100f) {
                maxNits = reportedLum
            }
        } catch (_: Throwable) {
            // Safe fallback in headless/test environments
        }

        var codecHdr10 = false
        var codecHdr10Plus = false
        var codecDolbyVision = false

        for (info in codecInfos) {
            if (info.isEncoder) continue
            for (type in info.supportedTypes) {
                if (type.equals("video/dolby-vision", ignoreCase = true)) {
                    codecDolbyVision = true
                }
                if (type.equals("video/hevc", ignoreCase = true)) {
                    try {
                        val caps = info.getCapabilitiesForType(type)
                        for (profileLevel in caps.profileLevels) {
                            if (profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                                profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
                            ) {
                                codecHdr10 = true
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                profileLevel.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
                            ) {
                                codecHdr10Plus = true
                            }
                        }
                    } catch (_: Throwable) {
                    }
                }
            }
        }

        return HdrProbeResult(
            hdr10 = displayHdr10 || codecHdr10,
            hdr10Plus = displayHdr10Plus || codecHdr10Plus,
            hlg = displayHlg || codecHdr10,
            dolbyVision = displayDolbyVision || codecDolbyVision,
            maxNits = maxNits
        )
    }

    /**
     * Resolves HDR format from track MIME and color transfer characteristics.
     */
    fun resolveHdrFormat(
        videoMime: String?,
        colorTransfer: Int,
        titleHint: String = ""
    ): String {
        val lowerMime = videoMime?.lowercase(Locale.US).orEmpty()
        val lowerTitle = titleHint.lowercase(Locale.US)
        return when {
            lowerMime.contains("dolby-vision") || lowerTitle.contains("dolby.vision") || lowerTitle.contains("dovi") ->
                "Dolby Vision"
            colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 -> {
                if (lowerTitle.contains("hdr10+") || lowerTitle.contains("hdr10plus")) "HDR10+" else "HDR10"
            }
            colorTransfer == MediaFormat.COLOR_TRANSFER_HLG -> "HLG"
            else -> "SDR"
        }
    }

    /**
     * Probes real track metadata from a local URI using MediaMetadataRetriever and MediaExtractor.
     */
    fun probeMediaMetadata(
        context: Context,
        uriString: String,
        isVideo: Boolean,
        titleHint: String = ""
    ): ProbedMediaMetadata {
        val uri = try {
            Uri.parse(uriString)
        } catch (_: Throwable) {
            return ProbedMediaMetadata()
        }

        var durationMs = 0L
        var width = 0
        var height = 0
        var frameRate = 0f
        var bitrate = 0
        var sampleRateHz = if (isVideo) 48000 else 44100
        var channelCount = 2
        var colorTransfer = 0
        var videoMime: String? = null
        var genre = ""

        val retriever = MediaMetadataRetriever()
        try {
            if (uriString.startsWith("content://")) {
                retriever.setDataSource(context, uri)
            } else {
                retriever.setDataSource(uri.path ?: uriString.removePrefix("file://"))
            }
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim().orEmpty()
            if (isVideo) {
                width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            }
        } catch (_: Throwable) {
        } finally {
            try {
                retriever.release()
            } catch (_: Throwable) {}
        }

        val extractor = MediaExtractor()
        try {
            if (uriString.startsWith("content://")) {
                extractor.setDataSource(context, uri, null)
            } else {
                extractor.setDataSource(uri.path ?: uriString.removePrefix("file://"))
            }
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") && videoMime == null) {
                    videoMime = mime
                    if (format.containsKey(MediaFormat.KEY_WIDTH)) width = format.getInteger(MediaFormat.KEY_WIDTH)
                    if (format.containsKey(MediaFormat.KEY_HEIGHT)) height = format.getInteger(MediaFormat.KEY_HEIGHT)
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = try {
                            format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                        } catch (_: Throwable) {
                            try { format.getFloat(MediaFormat.KEY_FRAME_RATE) } catch (_: Throwable) { 0f }
                        }
                    }
                    if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                        colorTransfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                    }
                } else if (mime.startsWith("audio/")) {
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRateHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                }
            }
        } catch (_: Throwable) {
        } finally {
            try {
                extractor.release()
            } catch (_: Throwable) {}
        }

        val hdrFormat = if (isVideo) {
            resolveHdrFormat(videoMime = videoMime, colorTransfer = colorTransfer, titleHint = titleHint)
        } else {
            "SDR"
        }

        return ProbedMediaMetadata(
            durationMs = durationMs,
            width = width,
            height = height,
            frameRate = frameRate,
            bitrate = bitrate,
            sampleRateHz = sampleRateHz,
            channelCount = channelCount,
            hdrFormat = hdrFormat,
            genre = genre
        )
    }

    fun mapVideoMimeToCodecLabel(mime: String?): String? {
        val m = mime?.lowercase(Locale.US)?.trim().orEmpty()
        if (m.isEmpty()) return null
        return when {
            m.contains("dolby-vision") -> "Dolby Vision"
            m.contains("hevc") || m.contains("h265") -> "H.265/HEVC"
            m.contains("avc") || m.contains("h264") -> "H.264"
            m.contains("av01") || m.contains("av1") -> "AV1"
            m.contains("vp9") -> "VP9"
            m.contains("vp8") -> "VP8"
            m.contains("mp4v") -> "MPEG-4"
            m.contains("mpeg2") || m == "video/mpeg" -> "MPEG-2"
            m.contains("wmv") || m.contains("wvc1") -> "VC-1 / WMV"
            m.contains("3gpp") -> "H.263 / 3GP"
            else -> m.substringAfter('/').uppercase(Locale.US)
        }
    }

    fun mapAudioMimeToCodecLabel(mime: String?): String? {
        val m = mime?.lowercase(Locale.US)?.trim().orEmpty()
        if (m.isEmpty()) return null
        return when {
            m.contains("eac3") -> "EAC3"
            m.contains("ac3") -> "AC3"
            m.contains("dts") -> "DTS"
            m.contains("opus") -> "Opus"
            m.contains("flac") -> "FLAC"
            m.contains("mp4a") || m.contains("aac") -> "AAC"
            m == "audio/mpeg" || m.contains("mp3") -> "MP3"
            m.contains("raw") || m.contains("pcm") || m.contains("wav") -> "PCM"
            m.contains("vorbis") -> "Vorbis"
            m.contains("true-hd") || m.contains("truehd") -> "TrueHD"
            m.contains("alac") -> "ALAC"
            else -> m.substringAfter('/').uppercase(Locale.US)
        }
    }

    fun findSidecarSubtitlesForFile(uriString: String): List<String> {
        if (!uriString.startsWith("file://")) return emptyList()
        val videoFile = File(uriString.removePrefix("file://"))
        val parent = videoFile.parentFile ?: return emptyList()
        val baseName = videoFile.nameWithoutExtension
        if (baseName.isBlank() || !parent.exists() || !parent.isDirectory) return emptyList()
        val subExtensions = setOf("srt", "vtt", "ass", "ssa", "ttml")
        return try {
            parent.listFiles()
                ?.filter { candidate ->
                    candidate.isFile &&
                        candidate.extension.lowercase(Locale.US) in subExtensions &&
                        candidate.nameWithoutExtension.startsWith(baseName, ignoreCase = true)
                }
                ?.map { it.name }
                ?.sorted()
                .orEmpty()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun getCachedVideoStreamInspection(uriString: String, sizeBytes: Long): VideoStreamInspection? {
        return synchronized(videoStreamCache) {
            videoStreamCache["$uriString|$sizeBytes"]
        }
    }

    /**
     * Inspects actual video, audio, and subtitle tracks for a local video using MediaExtractor,
     * MediaMetadataRetriever, and sidecar file checks. Never fabricates missing metadata.
     */
    fun inspectVideoStreams(
        context: Context,
        uriString: String,
        containerFormat: String,
        mimeType: String,
        title: String,
        sizeBytes: Long
    ): VideoStreamInspection {
        val cacheKey = "$uriString|$sizeBytes"
        synchronized(videoStreamCache) {
            videoStreamCache[cacheKey]?.let { return it }
        }

        val canonicalExt = resolveCanonicalExtension(
            uriOrPath = uriString,
            containerFormat = containerFormat,
            mimeType = mimeType,
            title = title,
            isVideo = true
        )
        val sidecarNames = findSidecarSubtitlesForFile(uriString)

        val uri = try {
            Uri.parse(uriString)
        } catch (_: Throwable) {
            return VideoStreamInspection(
                uri = uriString,
                canonicalExtension = canonicalExt,
                sidecarSubtitleNames = sidecarNames
            )
        }

        var videoMime: String? = null
        var videoCodecLabel: String? = null
        var frameRate: Float? = null
        var bitrate: Int? = null
        var colorTransfer = 0
        val audioCodecs = mutableListOf<String>()
        var audioTracks = 0
        var subtitleTracks = 0
        var sampleRate: Int? = null
        var channels: Int? = null
        var extractorOpened = false

        val extractor = MediaExtractor()
        try {
            if (uriString.startsWith("content://")) {
                extractor.setDataSource(context, uri, null)
            } else {
                extractor.setDataSource(uri.path ?: uriString.removePrefix("file://"))
            }
            extractorOpened = true
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue
                when {
                    trackMime.startsWith("video/") -> {
                        if (videoMime == null) {
                            videoMime = trackMime
                            videoCodecLabel = mapVideoMimeToCodecLabel(trackMime)
                            if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                                val fps = try {
                                    format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
                                } catch (_: Throwable) {
                                    try { format.getFloat(MediaFormat.KEY_FRAME_RATE) } catch (_: Throwable) { 0f }
                                }
                                if (fps > 0f) frameRate = fps
                            }
                            if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                                val br = format.getInteger(MediaFormat.KEY_BIT_RATE)
                                if (br > 0) bitrate = br
                            }
                            if (format.containsKey(MediaFormat.KEY_COLOR_TRANSFER)) {
                                colorTransfer = format.getInteger(MediaFormat.KEY_COLOR_TRANSFER)
                            }
                        }
                    }
                    trackMime.startsWith("audio/") -> {
                        audioTracks++
                        mapAudioMimeToCodecLabel(trackMime)?.let { label ->
                            if (label !in audioCodecs) audioCodecs.add(label)
                        }
                        if (sampleRate == null && format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                            sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE).takeIf { it > 0 }
                        }
                        if (channels == null && format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).takeIf { it > 0 }
                        }
                    }
                    trackMime.startsWith("text/") ||
                        trackMime.equals("application/x-subrip", ignoreCase = true) ||
                        trackMime.equals("application/ttml+xml", ignoreCase = true) ||
                        trackMime.equals("application/x-quicktime-tx3g", ignoreCase = true) ||
                        trackMime.equals("application/cea-608", ignoreCase = true) ||
                        trackMime.equals("application/cea-708", ignoreCase = true) ||
                        trackMime.equals("application/pgs", ignoreCase = true) ||
                        trackMime.equals("application/vobsub", ignoreCase = true) ||
                        trackMime.equals("application/dvbsubs", ignoreCase = true) -> {
                        subtitleTracks++
                    }
                }
            }
        } catch (_: Throwable) {
        } finally {
            try {
                extractor.release()
            } catch (_: Throwable) {}
        }

        if (bitrate == null) {
            val retriever = MediaMetadataRetriever()
            try {
                if (uriString.startsWith("content://")) {
                    retriever.setDataSource(context, uri)
                } else {
                    retriever.setDataSource(uri.path ?: uriString.removePrefix("file://"))
                }
                bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toIntOrNull()
                    ?.takeIf { it > 0 }
            } catch (_: Throwable) {
            } finally {
                try {
                    retriever.release()
                } catch (_: Throwable) {}
            }
        }

        val hdr = if (extractorOpened && videoMime != null) {
            resolveHdrFormat(videoMime = videoMime, colorTransfer = colorTransfer, titleHint = title)
        } else null

        val result = VideoStreamInspection(
            uri = uriString,
            canonicalExtension = canonicalExt,
            videoCodecLabel = videoCodecLabel,
            videoMime = videoMime,
            audioCodecLabels = audioCodecs,
            audioTrackCount = if (extractorOpened) audioTracks else null,
            embeddedSubtitleTrackCount = if (extractorOpened) subtitleTracks else null,
            sidecarSubtitleNames = sidecarNames,
            frameRate = frameRate,
            bitrate = bitrate,
            hdrFormat = hdr,
            sampleRateHz = sampleRate,
            channelCount = channels,
            inspectionSucceeded = extractorOpened
        )
        synchronized(videoStreamCache) {
            videoStreamCache.put(cacheKey, result)
        }
        return result
    }

    /**
     * Resolves file extension and MIME type into a normalized MIME type and human-readable codec badge.
     * Supports all 19 audio formats and 16 video formats case-insensitively while preserving existing badge labels.
     */
    fun resolveMimeAndCodecBadge(
        fileNameOrPath: String,
        rawMimeType: String?,
        isVideoHint: Boolean = false
    ): Pair<String, String> {
        val ext = fileNameOrPath.substringAfterLast('.', "").lowercase(Locale.US)
        val normalizedRaw = rawMimeType?.lowercase(Locale.US)?.trim().orEmpty()

        return when (ext) {
            // Audio formats (19 + surround/cinema extensions)
            "flac" -> "audio/flac" to "FLAC Lossless"
            "alac" -> "audio/alac" to "ALAC Lossless"
            "wav" -> "audio/raw" to "WAV PCM"
            "aiff", "aif" -> "audio/x-aiff" to "AIFF PCM"
            "mp3" -> "audio/mpeg" to "MP3"
            "m4a" -> "audio/mp4" to "AAC / M4A"
            "aac" -> "audio/aac" to "AAC"
            "ogg", "oga" -> "audio/ogg" to "OGG Vorbis"
            "opus" -> "audio/opus" to "Opus"
            "wma" -> "audio/x-ms-wma" to "WMA"
            "ape" -> "audio/x-ape" to "APE Lossless"
            "amr" -> "audio/3gpp" to "AMR"
            "mid" -> "audio/midi" to "MID"
            "midi" -> "audio/midi" to "MIDI"
            "ac3" -> "audio/ac3" to "AC-3 Dolby"
            "eac3", "ec3" -> "audio/eac3" to "E-AC3 DD+"
            "mka" -> "audio/x-matroska" to "MKA"
            "mp2" -> "audio/mpeg-L2" to "MP2"
            "dts" -> "audio/vnd.dts" to "DTS"
            "dtshd" -> "audio/vnd.dts.hd" to "DTS-HD"
            "truehd" -> "audio/true-hd" to "TrueHD"
            // Dual-capable .3gp container
            "3gp", "3gpp" -> if (isVideoHint || normalizedRaw.startsWith("video/")) {
                "video/3gpp" to "3GP"
            } else {
                "audio/3gpp" to "3GP"
            }
            // Video formats (16)
            "mp4" -> "video/mp4" to "MP4"
            "m4v" -> "video/x-m4v" to "M4V"
            "mkv" -> "video/x-matroska" to "MKV"
            "webm" -> "video/webm" to "WebM"
            "mov" -> "video/quicktime" to "MOV"
            "avi" -> "video/x-msvideo" to "AVI"
            "ts" -> "video/mp2t" to "MPEG-TS"
            "mts" -> "video/mp2t" to "MTS"
            "m2ts" -> "video/mp2t" to "M2TS"
            "mpeg" -> "video/mpeg" to "MPEG"
            "mpg" -> "video/mpeg" to "MPG"
            "flv" -> "video/x-flv" to "FLV"
            "wmv" -> "video/x-ms-wmv" to "WMV"
            "vob" -> "video/mpeg2" to "VOB"
            "ogv" -> "video/ogg" to "OGV"
            else -> {
                val fallbackMime = when {
                    normalizedRaw.isNotEmpty() -> normalizedRaw
                    isVideoHint -> "video/mp4"
                    else -> "audio/mpeg"
                }
                val badge = when {
                    normalizedRaw.contains("hevc") || normalizedRaw.contains("h265") -> "HEVC"
                    normalizedRaw.contains("av01") || normalizedRaw.contains("av1") -> "AV1"
                    normalizedRaw.contains("vp9") -> "VP9"
                    normalizedRaw.contains("flac") -> "FLAC Lossless"
                    normalizedRaw.contains("opus") -> "Opus"
                    normalizedRaw.contains("vorbis") || normalizedRaw.contains("ogg") -> if (isVideoHint) "OGV" else "OGG Vorbis"
                    normalizedRaw.contains("matroska") -> if (isVideoHint) "MKV" else "MKA"
                    normalizedRaw.contains("mp4a") || normalizedRaw.contains("aac") -> "AAC"
                    isVideoHint -> "MP4"
                    else -> "MP3"
                }
                fallbackMime to badge
            }
        }
    }

    fun isAudioExtension(ext: String): Boolean {
        return normalizeExtension(ext) in ALL_AUDIO_EXTENSIONS
    }

    fun isVideoExtension(ext: String): Boolean {
        return normalizeExtension(ext) in ALL_VIDEO_EXTENSIONS
    }
}
