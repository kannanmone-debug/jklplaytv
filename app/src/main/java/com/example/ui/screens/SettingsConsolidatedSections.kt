package com.example.ui.screens

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.adaptive.LocalAdaptiveWindowInfo
import com.example.codec.CodecSupportReport
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.dynamicGlassUnselectedSurfaceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Real metadata extracted from a user-selected local media file in the Codec & HDR Inspector.
 * Strictly separates file metadata from device decoder capabilities and actual playback capability.
 */
internal data class ProbedMediaFileReport(
    val fileName: String,
    val containerFormat: String,
    val durationMs: Long,
    val bitrateBps: Long,
    val videoMime: String?,
    val videoCodecLabel: String?,
    val width: Int,
    val height: Int,
    val frameRate: Float,
    val hdrDetectedProfile: String,
    val audioMime: String?,
    val audioCodecLabel: String?,
    val sampleRateHz: Int,
    val channelCount: Int,
    val videoPlaybackVerdict: String,
    val audioPlaybackVerdict: String,
    val isPlayableOnDevice: Boolean
)

/**
 * Complete functional Codec & HDR Inspector embedded directly inside Video Settings -> Decoder & HDR.
 * Uses flat Column layout (never a nested LazyColumn) so it scrolls seamlessly inside the main
 * continuous vertical Settings list while preserving every single Inspector capability and testTag.
 */
@Composable
internal fun EmbeddedCodecAndHdrInspectorContent(
    settings: AppSettingsState,
    capabilities: DeviceMediaCapabilities,
    isReprobingCodecs: Boolean,
    globalSearchQuery: String,
    onChangeDecoderMode: (String) -> Unit,
    onRefreshCodecCapabilities: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val expandedDetailsMap = remember { mutableStateMapOf<String, Boolean>() }
    var videoDecodersExpanded by rememberSaveable { mutableStateOf(true) }
    var audioDecodersExpanded by rememberSaveable { mutableStateOf(true) }
    var probedFileReport by remember { mutableStateOf<ProbedMediaFileReport?>(null) }
    var isProbingFile by remember { mutableStateOf(false) }
    var inspectorSearchQuery by rememberSaveable { mutableStateOf("") }
    val inspectorTabs = listOf("All", "File Probe", "HDR & Display", "Video Decoders", "Audio Decoders")
    var selectedInspectorTab by rememberSaveable { mutableStateOf("All") }
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    val activeInspectorTab = if (globalSearchQuery.isNotBlank()) {
        resolveActiveTab("codec_inspector_inner", globalSearchQuery, selectedInspectorTab)
    } else {
        selectedInspectorTab
    }

    val fileProbeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProbingFile = true
                probedFileReport = probeLocalMediaFile(context, uri, capabilities)
                isProbingFile = false
            }
        }
    }

    val hwCount = (capabilities.videoCodecs + capabilities.audioCodecs).count { it.hasHardwareDecoder }
    val swCount = (capabilities.videoCodecs + capabilities.audioCodecs).count { it.hasSoftwareDecoder }
    val trimmedCodecQuery = inspectorSearchQuery.trim().lowercase(Locale.ROOT)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("codec_inspector_subscreen"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Canonical Decoder Priority Preference
        val modes = listOf(
            "AUTO_HW" to "Auto Hardware",
            "HW_PLUS" to "HW+ Hybrid",
            "SOFTWARE_FALLBACK" to "Software Fallback"
        )
        val currentDecoderLabel = modes.firstOrNull { (code, _) ->
            settings.decoderMode == code || (code == "AUTO_HW" && settings.decoderMode == "AUTO")
        }?.second ?: "Auto Hardware"

        SettingsSubgroupHeader(
            title = "Hardware / Software Decoder Priority",
            accent = unifiedAccent
        )
        CompactDropdownSelectorRow(
            label = "Decoder Preference",
            selectedLabel = currentDecoderLabel,
            options = modes,
            accentColor = unifiedAccent,
            testTag = "dropdown_decoder_priority",
            onSelectOption = onChangeDecoderMode
        )

        UnifiedSettingsDivider()

        // Inspector Search + Single-Line Horizontal Filter Tabs
        SettingsSubgroupHeader(
            title = "Codec & HDR Inspector",
            accent = unifiedAccent
        )
        UnifiedSettingsSearchBar(
            query = inspectorSearchQuery,
            onQueryChange = { inspectorSearchQuery = it },
            placeholder = "Search codecs, MIME types, HDR profiles...",
            testTag = "codec_inspector_search_input"
        )
        SettingsSubTabRow(
            tabs = inspectorTabs,
            selectedTab = activeInspectorTab,
            onSelectTab = { selectedInspectorTab = it },
            accentColor = unifiedAccent,
            categoryKey = "codec_inspector"
        )

        // SECTION A — INSPECT MEDIA FILE
        if (activeInspectorTab == "All" || activeInspectorTab == "File Probe") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(dynamicGlassUnselectedSurfaceColor(unifiedAccent))
                    .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
                    .testTag("inspector_section_file"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Inspect Media File",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = unifiedAccent,
                        modifier = Modifier.weight(1f)
                    )
                    CompactActionPill(
                        text = if (isProbingFile) "Inspecting..." else "Select File",
                        icon = Icons.Default.FileOpen,
                        color = unifiedAccent,
                        testTag = "inspector_pick_file_button",
                        onClick = { fileProbeLauncher.launch("*/*") }
                    )
                }

                InspectorKeyValueRow(
                    label = "Selected File",
                    value = probedFileReport?.fileName ?: "None selected"
                )
                InspectorKeyValueRow(
                    label = "Inspection Status",
                    value = when {
                        isProbingFile -> "Inspecting..."
                        probedFileReport != null -> if (probedFileReport!!.isPlayableOnDevice) "Verified Playable" else "Unsupported Stream"
                        else -> "Ready"
                    }
                )

                probedFileReport?.let { report ->
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
                    Spacer(Modifier.height(4.dp))

                    InspectorKeyValueRow("Container Format", report.containerFormat)
                    InspectorKeyValueRow("Duration", if (report.durationMs > 0L) formatDurationMs(report.durationMs) else "Unknown")
                    InspectorKeyValueRow(
                        "Bitrate",
                        if (report.bitrateBps > 0L) "${report.bitrateBps / 1000L} kbps" else "Variable"
                    )
                    if (report.videoMime != null) {
                        InspectorKeyValueRow("Video Codec", report.videoCodecLabel ?: report.videoMime)
                        InspectorKeyValueRow(
                            "Resolution",
                            if (report.width > 0 && report.height > 0) {
                                "${report.width}×${report.height}" + if (report.frameRate > 0f) " @ ${report.frameRate} fps" else ""
                            } else "Unspecified"
                        )
                        InspectorKeyValueRow("HDR Profile", report.hdrDetectedProfile)
                        InspectorKeyValueRow("Video Decoder", report.videoPlaybackVerdict)
                    }
                    if (report.audioMime != null) {
                        InspectorKeyValueRow("Audio Codec", report.audioCodecLabel ?: report.audioMime)
                        InspectorKeyValueRow(
                            "Audio Format",
                            "${report.channelCount} ch • ${report.sampleRateHz} Hz"
                        )
                        InspectorKeyValueRow("Audio Decoder", report.audioPlaybackVerdict)
                    }
                }
            }
        }

        // SECTION B & C — DEVICE CAPABILITIES & HDR SUPPORT
        if (activeInspectorTab == "All" || activeInspectorTab == "HDR & Display") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(dynamicGlassUnselectedSurfaceColor(unifiedAccent))
                    .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
                    .testTag("inspector_section_device"),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Device Capabilities",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = unifiedAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    Row(
                        modifier = Modifier
                            .widthIn(min = 96.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(unifiedAccent.copy(alpha = if (isReprobingCodecs) 0.12f else 0.18f))
                            .border(1.dp, unifiedAccent.copy(alpha = 0.48f), RoundedCornerShape(10.dp))
                            .clickable(enabled = !isReprobingCodecs, onClick = onRefreshCodecCapabilities)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("settings_reprobe_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
                    ) {
                        if (isReprobingCodecs) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = unifiedAccent
                            )
                            Text(
                                text = "Probing...",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = unifiedAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Re-Probe Codecs",
                                tint = unifiedAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Re-Probe",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = unifiedAccent,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                InspectorKeyValueRow("Device Model", capabilities.deviceModel)
                InspectorKeyValueRow("Android Version", "Android API ${capabilities.androidSdk}")
                InspectorKeyValueRow("Hardware Decoder Availability", "$hwCount Available")
                InspectorKeyValueRow("Software Decoder Availability", "$swCount Available")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(dynamicGlassUnselectedSurfaceColor(unifiedAccent))
                    .border(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
                    .testTag("inspector_section_hdr"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "HDR Support",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = unifiedAccent
                )

                val hdrCheckTargets = listOf(
                    "HDR10" to capabilities.displaySupportsHdr10,
                    "HDR10+" to capabilities.displaySupportsHdr10Plus,
                    "HLG" to capabilities.displaySupportsHlg,
                    "Dolby Vision" to capabilities.displaySupportsDolbyVision
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    hdrCheckTargets.forEach { (formatName, supported) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (supported) unifiedAccent.copy(alpha = 0.14f)
                                    else dynamicGlassUnselectedSurfaceColor()
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (supported) unifiedAccent.copy(alpha = 0.45f)
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                                    shape = RoundedCornerShape(9.dp)
                                )
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = formatName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (supported) unifiedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (supported) "Supported" else "Not Reported",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (supported) unifiedAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // SECTION D — VIDEO DECODER SUPPORT
        if (activeInspectorTab == "All" || activeInspectorTab == "Video Decoders") {
            val filteredVideoCodecs = capabilities.videoCodecs.filter { codec ->
                trimmedCodecQuery.isEmpty() ||
                    codec.displayName.lowercase(Locale.ROOT).contains(trimmedCodecQuery) ||
                    codec.mimeTypes.any { it.lowercase(Locale.ROOT).contains(trimmedCodecQuery) }
            }
            val supportedVideoCount = capabilities.videoCodecs.count { it.isSupportedOnDevice }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspector_section_video_decoders"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { videoDecodersExpanded = !videoDecodersExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Video Decoder Support ($supportedVideoCount/${capabilities.videoCodecs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = unifiedAccent
                    )
                    Icon(
                        imageVector = if (videoDecodersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (videoDecodersExpanded) "Collapse Video Decoders" else "Expand Video Decoders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = videoDecodersExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        filteredVideoCodecs.forEach { codec ->
                            val key = "video_${codec.displayName}"
                            val isDetailsOpen = expandedDetailsMap[key] == true
                            InspectableCodecRowItem(
                                codec = codec,
                                detailsExpanded = isDetailsOpen,
                                onToggleDetails = { expandedDetailsMap[key] = !isDetailsOpen }
                            )
                        }
                    }
                }
            }
        }

        // SECTION E — AUDIO DECODER SUPPORT
        if (activeInspectorTab == "All" || activeInspectorTab == "Audio Decoders") {
            val filteredAudioCodecs = capabilities.audioCodecs.filter { codec ->
                trimmedCodecQuery.isEmpty() ||
                    codec.displayName.lowercase(Locale.ROOT).contains(trimmedCodecQuery) ||
                    codec.mimeTypes.any { it.lowercase(Locale.ROOT).contains(trimmedCodecQuery) }
            }
            val supportedAudioCount = capabilities.audioCodecs.count { it.isSupportedOnDevice }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("inspector_section_audio_decoders"),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { audioDecodersExpanded = !audioDecodersExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Audio Decoder Support ($supportedAudioCount/${capabilities.audioCodecs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = unifiedAccent
                    )
                    Icon(
                        imageVector = if (audioDecodersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (audioDecodersExpanded) "Collapse Audio Decoders" else "Expand Audio Decoders",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                AnimatedVisibility(visible = audioDecodersExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        filteredAudioCodecs.forEach { codec ->
                            val key = "audio_${codec.displayName}"
                            val isDetailsOpen = expandedDetailsMap[key] == true
                            InspectableCodecRowItem(
                                codec = codec,
                                detailsExpanded = isDetailsOpen,
                                onToggleDetails = { expandedDetailsMap[key] = !isDetailsOpen }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InspectorKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InspectableCodecRowItem(
    codec: CodecSupportReport,
    detailsExpanded: Boolean,
    onToggleDetails: () -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()
    val badgeColor = when {
        codec.hasHardwareDecoder -> unifiedAccent
        codec.hasSoftwareDecoder -> UnifiedSettingsTokens.secondaryAccent()
        else -> GlassRoseTint
    }
    val statusText = when {
        codec.hasHardwareDecoder && codec.hasSoftwareDecoder -> "HW + SW"
        codec.hasHardwareDecoder -> "Hardware"
        codec.hasSoftwareDecoder -> "Software"
        else -> "Unsupported"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(dynamicGlassUnselectedSurfaceColor())
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.20f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = codec.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusPillBadge(text = statusText, color = badgeColor)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable(onClick = onToggleDetails)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (detailsExpanded) "Hide" else "Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = unifiedAccent,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }

        AnimatedVisibility(visible = detailsExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(dynamicGlassUnselectedSurfaceColor())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                InspectorKeyValueRow("MIME Type(s)", codec.mimeTypes.joinToString(", "))
                InspectorKeyValueRow("Capability", codec.maxResolution)
                if (codec.hdrProfilesSupported.isNotEmpty()) {
                    InspectorKeyValueRow("HDR Profiles", codec.hdrProfilesSupported.joinToString(" / "))
                }
                if (codec.decoderNames.isNotEmpty()) {
                    Text(
                        text = "Decoders: ${codec.decoderNames.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Consolidated Headset & Audio Delay tab inside Audio Settings -> Headset.
 */
@Composable
internal fun AudioHeadsetTabContent(
    settings: AppSettingsState,
    onUpdatePauseOnHeadsetDisconnect: (Boolean) -> Unit,
    onUpdateFadeInAtStart: (Boolean) -> Unit,
    onUpdateAudioDelayMs: (Long) -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()

    SettingsSubgroupHeader(
        title = "Headset Behavior & Audio Sync",
        accent = unifiedAccent
    )
    CompactToggleRow(
        title = "Pause on Headset Disconnect",
        checked = settings.pauseOnHeadsetDisconnect,
        accentColor = unifiedAccent,
        testTag = "pause_on_headset_switch",
        onCheckedChange = onUpdatePauseOnHeadsetDisconnect
    )
    CompactToggleRow(
        title = "Fade In at Playback Start",
        checked = settings.fadeInAtStart,
        accentColor = unifiedAccent,
        testTag = "fade_in_at_start_switch",
        onCheckedChange = onUpdateFadeInAtStart
    )

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Audio Sync Offset",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            val audText = if (settings.audioDelayMs == 0L) "0 ms"
            else "${if (settings.audioDelayMs > 0) "+" else ""}${settings.audioDelayMs} ms"
            Text(
                text = audText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (settings.audioDelayMs != 0L) unifiedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("audio_delay_value_text")
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(-500L to "-500ms", -100L to "-100ms", 0L to "Reset", 100L to "+100ms", 500L to "+500ms").forEach { (delta, lbl) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f), RoundedCornerShape(8.dp))
                        .clickable {
                            val newAudDelay = if (delta == 0L) 0L else (settings.audioDelayMs + delta).coerceIn(-10000L, 10000L)
                            onUpdateAudioDelayMs(newAudDelay)
                        }
                        .padding(vertical = 5.dp)
                        .testTag("audio_delay_btn_${delta}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lbl,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (delta == 0L) unifiedAccent else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * Consolidated Music Player Shortcuts tab inside Audio Settings -> Shortcuts.
 */
@Composable
internal fun MusicPlayerShortcutsTabContent(
    settings: AppSettingsState,
    onUpdateMusicShortcuts: (String) -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()
    val allMusicShortcuts = listOf(
        "QUEUE", "LYRICS", "EQUALIZER", "SLEEP_TIMER", "SPEED",
        "SHUFFLE", "REPEAT", "PLAYLIST", "INFO"
    )
    val currentList = settings.musicShortcutsList
    val fullList = (currentList + allMusicShortcuts.filter { it !in currentList }).distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("music_player_shortcuts_content"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SettingsSubgroupHeader(
            title = "Music Player Shortcuts (${currentList.size}/${allMusicShortcuts.size} Active)",
            accent = unifiedAccent
        )
        fullList.forEachIndexed { index, key ->
            val isEnabled = key in currentList
            val label = getMusicShortcutLabel(key)
            ShortcutReorderRow(
                index = index,
                totalCount = fullList.size,
                label = label,
                enabled = isEnabled,
                accentColor = unifiedAccent,
                switchTestTag = "music_shortcut_switch_$key",
                onMoveUp = {
                    if (index > 0) {
                        val newList = fullList.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index - 1, item)
                        val orderedEnabled = newList.filter { it in currentList }
                        onUpdateMusicShortcuts(orderedEnabled.joinToString(","))
                    }
                },
                onMoveDown = {
                    if (index < fullList.size - 1) {
                        val newList = fullList.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index + 1, item)
                        val orderedEnabled = newList.filter { it in currentList }
                        onUpdateMusicShortcuts(orderedEnabled.joinToString(","))
                    }
                },
                onToggle = { checked ->
                    val newList = if (checked) {
                        if (key !in currentList) currentList + key else currentList
                    } else {
                        currentList - key
                    }
                    onUpdateMusicShortcuts(newList.joinToString(","))
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            CompactActionPill(
                text = "Restore Defaults",
                icon = Icons.Default.Restore,
                color = unifiedAccent,
                testTag = "restore_music_shortcuts_button",
                onClick = { onUpdateMusicShortcuts("QUEUE,LYRICS,EQUALIZER,SLEEP_TIMER,SPEED") }
            )
        }
    }
}

/**
 * Consolidated Video Player Shortcuts tab inside Video Settings -> Shortcuts.
 */
@Composable
internal fun VideoPlayerShortcutsTabContent(
    settings: AppSettingsState,
    onUpdateVideoShortcuts: (String) -> Unit
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()
    val allVideoShortcuts = listOf(
        "EQUALIZER", "ROTATE", "FIT", "SPEED", "AUDIO", "SUBTITLES",
        "AB_REPEAT", "SLEEP_TIMER", "PIP", "FLOATING", "CAPTURE", "LOCK", "INFO", "MORE"
    )
    val currentList = settings.videoShortcutsList
    val fullList = (currentList + allVideoShortcuts.filter { it !in currentList }).distinct()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_player_shortcuts_content"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SettingsSubgroupHeader(
            title = "Video Player Shortcuts (${currentList.size}/${allVideoShortcuts.size} Active)",
            accent = unifiedAccent
        )
        fullList.forEachIndexed { index, key ->
            val isEnabled = key in currentList
            val label = getVideoShortcutLabel(key)
            ShortcutReorderRow(
                index = index,
                totalCount = fullList.size,
                label = label,
                enabled = isEnabled,
                accentColor = unifiedAccent,
                switchTestTag = "video_shortcut_switch_$key",
                onMoveUp = {
                    if (index > 0) {
                        val newList = fullList.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index - 1, item)
                        val orderedEnabled = newList.filter { it in currentList }
                        onUpdateVideoShortcuts(orderedEnabled.joinToString(","))
                    }
                },
                onMoveDown = {
                    if (index < fullList.size - 1) {
                        val newList = fullList.toMutableList()
                        val item = newList.removeAt(index)
                        newList.add(index + 1, item)
                        val orderedEnabled = newList.filter { it in currentList }
                        onUpdateVideoShortcuts(orderedEnabled.joinToString(","))
                    }
                },
                onToggle = { checked ->
                    val newList = if (checked) {
                        if (key !in currentList) currentList + key else currentList
                    } else {
                        currentList - key
                    }
                    onUpdateVideoShortcuts(newList.joinToString(","))
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            CompactActionPill(
                text = "Restore Defaults",
                icon = Icons.Default.Restore,
                color = unifiedAccent,
                testTag = "restore_video_shortcuts_button",
                onClick = { onUpdateVideoShortcuts("EQUALIZER,ROTATE,FIT,SPEED,AUDIO,SUBTITLES,AB_REPEAT,PIP,LOCK,MORE") }
            )
        }
    }
}

@Composable
private fun ShortcutReorderRow(
    index: Int,
    totalCount: Int,
    label: String,
    enabled: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    switchTestTag: String,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${index + 1}.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = "Move Up",
                    tint = if (index > 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Move Down",
                    tint = if (index < totalCount - 1) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag(switchTestTag),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.35f)
                )
            )
        }
    }
}

/**
 * Functional Navigation Settings covering all 4 tabs:
 * Destinations | Bottom Bar | Shortcuts | Layout.
 * Backed directly by existing SettingsDataStore navigation keys.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FunctionalNavigationSettingsContent(
    settings: AppSettingsState,
    activeTab: String = "Destinations",
    onSetNavDestinationEnabled: (String, Boolean) -> Unit,
    onResetNavDefaults: () -> Unit,
    onJumpToLiquidGlass: () -> Unit = {}
) {
    val unifiedAccent = UnifiedSettingsTokens.primaryAccent()
    val windowInfo = LocalAdaptiveWindowInfo.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when (activeTab) {
            "Destinations" -> {
                SettingsSubgroupHeader(
                    title = "Primary Library Destinations",
                    accent = unifiedAccent
                )
                CompactToggleRow(
                    title = "Home",
                    checked = settings.navHomeEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_home",
                    onCheckedChange = { onSetNavDestinationEnabled("home", it) }
                )
                CompactToggleRow(
                    title = "Music Library",
                    checked = settings.navMusicEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_music",
                    onCheckedChange = { onSetNavDestinationEnabled("music", it) }
                )
                CompactToggleRow(
                    title = "Video Library",
                    checked = settings.navVideoEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_video",
                    onCheckedChange = { onSetNavDestinationEnabled("video", it) }
                )
                CompactToggleRow(
                    title = "Files",
                    checked = settings.navFilesEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_files",
                    onCheckedChange = { onSetNavDestinationEnabled("files", it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CompactActionPill(
                        text = "Restore Defaults",
                        icon = Icons.Default.Restore,
                        color = unifiedAccent,
                        testTag = "reset_nav_defaults_button",
                        onClick = onResetNavDefaults
                    )
                }
            }

            "Bottom Bar" -> {
                val activeLabels = buildList {
                    if (settings.navHomeEnabled) add("Home")
                    if (settings.navMusicEnabled) add("Music")
                    if (settings.navVideoEnabled) add("Video")
                    if (settings.navFilesEnabled) add("Files")
                }

                SettingsSubgroupHeader(
                    title = "Active Bottom Bar Tabs (${activeLabels.size})",
                    accent = unifiedAccent
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeLabels.forEach { label ->
                        StatusPillBadge(text = label, color = unifiedAccent)
                    }
                }

                SettingsSubgroupHeader(
                    title = "Quick Bottom Bar Presets",
                    accent = unifiedAccent
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CompactActionPill(
                        text = "Standard (4 Tabs)",
                        icon = Icons.Default.Check,
                        color = unifiedAccent,
                        testTag = "nav_preset_standard",
                        onClick = {
                            onSetNavDestinationEnabled("home", true)
                            onSetNavDestinationEnabled("music", true)
                            onSetNavDestinationEnabled("video", true)
                            onSetNavDestinationEnabled("files", true)
                        }
                    )
                    CompactActionPill(
                        text = "Media Only (2)",
                        icon = Icons.Default.Check,
                        color = unifiedAccent,
                        testTag = "nav_preset_media_only",
                        onClick = {
                            onSetNavDestinationEnabled("music", true)
                            onSetNavDestinationEnabled("video", true)
                            onSetNavDestinationEnabled("home", false)
                            onSetNavDestinationEnabled("files", false)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactActionPill(
                        text = "Glass Indicator Style",
                        icon = Icons.Default.Palette,
                        color = unifiedAccent,
                        testTag = "nav_jump_liquid_glass_button",
                        onClick = onJumpToLiquidGlass
                    )
                    CompactActionPill(
                        text = "Restore Defaults",
                        icon = Icons.Default.Restore,
                        color = unifiedAccent,
                        testTag = "reset_nav_bar_defaults_button",
                        onClick = onResetNavDefaults
                    )
                }
            }

            "Shortcuts" -> {
                SettingsSubgroupHeader(
                    title = "Quick Switcher & Drawer Shortcuts",
                    accent = unifiedAccent
                )
                CompactToggleRow(
                    title = "Playlists Quick Shortcut",
                    subtitle = "Expose quick jump to saved playlists",
                    checked = settings.navPlaylistsEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_playlists",
                    onCheckedChange = { onSetNavDestinationEnabled("playlists", it) }
                )
                CompactToggleRow(
                    title = "Albums Quick Shortcut",
                    subtitle = "Expose quick jump to album browser",
                    checked = settings.navAlbumsEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_albums",
                    onCheckedChange = { onSetNavDestinationEnabled("albums", it) }
                )
                CompactToggleRow(
                    title = "Artists Quick Shortcut",
                    subtitle = "Expose quick jump to artist directory",
                    checked = settings.navArtistsEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_artists",
                    onCheckedChange = { onSetNavDestinationEnabled("artists", it) }
                )
                CompactToggleRow(
                    title = "Equalizer Quick Shortcut",
                    subtitle = "Quick audio DSP & equalizer access",
                    checked = settings.navEqualizerEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_equalizer",
                    onCheckedChange = { onSetNavDestinationEnabled("equalizer", it) }
                )
                CompactToggleRow(
                    title = "Codec Inspector Shortcut",
                    subtitle = "Quick jump to Decoder & HDR diagnostics",
                    checked = settings.navCodecInspectorEnabled,
                    accentColor = unifiedAccent,
                    testTag = "nav_switch_codec_inspector",
                    onCheckedChange = { onSetNavDestinationEnabled("codec_inspector", it) }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    CompactActionPill(
                        text = "Restore Defaults",
                        icon = Icons.Default.Restore,
                        color = unifiedAccent,
                        testTag = "reset_nav_shortcuts_button",
                        onClick = onResetNavDefaults
                    )
                }
            }

            "Layout" -> {
                val activeChrome = if (windowInfo.widthClass.name == "COMPACT") {
                    "Bottom Navigation Bar"
                } else {
                    "Side Navigation Rail"
                }
                SettingsSubgroupHeader(
                    title = "Adaptive Canonical Layout",
                    accent = unifiedAccent
                )
                InspectorKeyValueRow("Active Navigation Chrome", activeChrome)
                InspectorKeyValueRow("Display Width Class", windowInfo.widthClass.name)
                InspectorKeyValueRow("Orientation", windowInfo.orientation.name)
                InspectorKeyValueRow("Viewport Size", "${windowInfo.windowWidthDp.toInt()} × ${windowInfo.windowHeightDp.toInt()} dp")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactActionPill(
                        text = "Customize Glass Chrome",
                        icon = Icons.Default.Palette,
                        color = unifiedAccent,
                        testTag = "nav_layout_glass_button",
                        onClick = onJumpToLiquidGlass
                    )
                    CompactActionPill(
                        text = "Restore Defaults",
                        icon = Icons.Default.Restore,
                        color = unifiedAccent,
                        testTag = "reset_nav_layout_button",
                        onClick = onResetNavDefaults
                    )
                }
            }
        }
    }
}

private fun getVideoShortcutLabel(key: String): String = when (key) {
    "EQUALIZER" -> "Equalizer / Audio DSP"
    "ROTATE" -> "Screen Rotation / Lock"
    "FIT" -> "Screen Aspect Ratio / Fit"
    "SPEED" -> "Playback Speed"
    "AUDIO" -> "Audio Tracks"
    "SUBTITLES" -> "Subtitles & CC"
    "AB_REPEAT" -> "A–B Repeat Loop"
    "SLEEP_TIMER" -> "Sleep Timer"
    "PIP" -> "Picture-in-Picture"
    "FLOATING" -> "Floating Window Player"
    "CAPTURE" -> "Frame Capture"
    "LOCK" -> "Lock Screen Controls"
    "INFO" -> "Playback & Stream Info"
    "MORE" -> "More Video Actions"
    else -> key
}

private fun getMusicShortcutLabel(key: String): String = when (key) {
    "QUEUE" -> "Queue / Up Next"
    "LYRICS" -> "Lyrics"
    "EQUALIZER" -> "Equalizer / DSP"
    "SLEEP_TIMER" -> "Sleep Timer"
    "SPEED" -> "Playback Speed"
    "SHUFFLE" -> "Shuffle"
    "REPEAT" -> "Repeat"
    "PLAYLIST" -> "Add to Playlist"
    "INFO" -> "Audio Information"
    else -> key
}

/**
 * Probes an actual local media file using `MediaMetadataRetriever` and `MediaExtractor`
 * and cross-references its track MIME types against `DeviceMediaCapabilities`.
 * Never guesses HDR or codec support from the file extension alone.
 */
internal suspend fun probeLocalMediaFile(
    context: Context,
    uri: Uri,
    capabilities: DeviceMediaCapabilities
): ProbedMediaFileReport = withContext(Dispatchers.IO) {
    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Selected Media Stream"
    val retriever = MediaMetadataRetriever()
    val extractor = MediaExtractor()

    var durationMs = 0L
    var bitrateBps = 0L
    var containerMime = context.contentResolver.getType(uri) ?: "Unknown Container"
    var videoMime: String? = null
    var audioMime: String? = null
    var width = 0
    var height = 0
    var frameRate = 0f
    var sampleRate = 0
    var channels = 0
    var colorTransfer = 0

    try {
        retriever.setDataSource(context, uri)
        durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        bitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)?.takeIf { it.isNotBlank() }?.let {
            containerMime = it
        }
        width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
    } catch (_: Throwable) {
    } finally {
        try {
            retriever.release()
        } catch (_: Throwable) {}
    }

    try {
        extractor.setDataSource(context, uri, null)
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
            } else if (mime.startsWith("audio/") && audioMime == null) {
                audioMime = mime
                if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                    sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                }
                if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                    channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
        }
    } catch (_: Throwable) {
    } finally {
        try {
            extractor.release()
        } catch (_: Throwable) {}
    }

    val hdrProfile = when {
        videoMime?.contains("dolby-vision", ignoreCase = true) == true -> "Dolby Vision (Stream Verified)"
        colorTransfer == MediaFormat.COLOR_TRANSFER_ST2084 -> "HDR10 / PQ (ST2084 Transfer Verified)"
        colorTransfer == MediaFormat.COLOR_TRANSFER_HLG -> "HLG (Hybrid Log-Gamma Verified)"
        videoMime != null -> "SDR (Standard Dynamic Range)"
        else -> "N/A (Audio Only)"
    }

    val matchingVideoReport = videoMime?.let { vMime ->
        capabilities.videoCodecs.firstOrNull { report ->
            report.mimeTypes.any { it.equals(vMime, ignoreCase = true) }
        }
    }
    val matchingAudioReport = audioMime?.let { aMime ->
        capabilities.audioCodecs.firstOrNull { report ->
            report.mimeTypes.any { it.equals(aMime, ignoreCase = true) }
        }
    }

    val videoVerdict = when {
        videoMime == null -> "No video stream"
        matchingVideoReport?.hasHardwareDecoder == true ->
            "Hardware Accelerated (${matchingVideoReport.decoderNames.firstOrNull() ?: "MediaCodec HW"})"
        matchingVideoReport?.hasSoftwareDecoder == true ->
            "Software Decoded (${matchingVideoReport.decoderNames.firstOrNull() ?: "Media3 SW"})"
        else -> "No native decoder registered for $videoMime"
    }

    val audioVerdict = when {
        audioMime == null -> "No audio stream"
        matchingAudioReport?.hasHardwareDecoder == true ->
            "Hardware Decoded (${matchingAudioReport.decoderNames.firstOrNull() ?: "MediaCodec HW"})"
        matchingAudioReport?.hasSoftwareDecoder == true ->
            "Software / AudioTrack Decoded (${matchingAudioReport.decoderNames.firstOrNull() ?: "Media3 SW"})"
        else -> "No native decoder registered for $audioMime"
    }

    val playable = (videoMime == null || matchingVideoReport?.isSupportedOnDevice == true) &&
        (audioMime == null || matchingAudioReport?.isSupportedOnDevice == true) &&
        (videoMime != null || audioMime != null)

    ProbedMediaFileReport(
        fileName = fileName,
        containerFormat = containerMime,
        durationMs = durationMs,
        bitrateBps = bitrateBps,
        videoMime = videoMime,
        videoCodecLabel = matchingVideoReport?.displayName ?: videoMime,
        width = width,
        height = height,
        frameRate = frameRate,
        hdrDetectedProfile = hdrProfile,
        audioMime = audioMime,
        audioCodecLabel = matchingAudioReport?.displayName ?: audioMime,
        sampleRateHz = sampleRate,
        channelCount = channels,
        videoPlaybackVerdict = videoVerdict,
        audioPlaybackVerdict = audioVerdict,
        isPlayableOnDevice = playable
    )
}
