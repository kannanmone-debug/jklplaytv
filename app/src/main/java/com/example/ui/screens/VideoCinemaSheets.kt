package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.codec.CodecCapabilityInspector
import com.example.codec.VideoStreamInspection
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.VideoBookmarkEntity
import com.example.player.PlaybackUiState
import com.example.player.RepeatModeState
import com.example.player.TrackOptionInfo
import com.example.storage.FileSystemEntry
import com.example.ui.components.VideoMetadataClassifier
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.theme.CinemaHudBorder
import com.example.ui.theme.CinemaHudSurfaceElevated
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalGlassNestingDepth
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveContextualAccent
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.resolveEffectiveConfigForComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

enum class VideoCinemaSheetType {
    NONE,
    OPTIONS,
    AUDIO_SUBTITLES,
    AB_REPEAT_SPEED,
    BOOKMARKS
}

@Composable
fun VideoAudioSubtitleSheet(
    playbackState: PlaybackUiState,
    settings: AppSettingsState,
    initialFocusSubtitles: Boolean,
    isLandscape: Boolean,
    onClose: () -> Unit,
    onSelectTrackOption: (TrackOptionInfo) -> Unit,
    onToggleSubtitles: () -> Unit,
    onLoadPrimarySubtitleClick: () -> Unit,
    onLoadSecondarySubtitleClick: () -> Unit,
    onClearSecondarySubtitles: () -> Unit,
    onAdjustSubtitleDelay: (Long) -> Unit,
    onAdjustAudioDelay: (Long) -> Unit,
    onSetSubtitlesEnabled: ((Boolean) -> Unit)? = null,
    onUpdateSubtitleQuickStyle: ((fontSizeSp: Float, bgOpacity: Float, outlineEnabled: Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember(initialFocusSubtitles) {
        mutableIntStateOf(if (initialFocusSubtitles) 1 else 0)
    }

    CinemaSheetContainer(
        isLandscape = isLandscape,
        scrollKey = selectedTab,
        modifier = modifier.testTag("video_audio_subtitle_sheet")
    ) {
        CinemaSheetHeader(
            title = "Audio & Subtitles",
            icon = if (selectedTab == 0) Icons.Rounded.Audiotrack else Icons.Rounded.Subtitles,
            accentColor = if (selectedTab == 0) ElectricIndigo else LiquidCyan,
            onClose = onClose
        )

        // Compact Segmented Tabs (Audio Track | Subtitles)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CinemaSegmentTab(
                label = "Audio (${playbackState.audioTracks.size})",
                selected = selectedTab == 0,
                accentColor = ElectricIndigo,
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("audio_tab_selector")
            )
            CinemaSegmentTab(
                label = if (playbackState.subtitlesEnabled) "Subtitles (ON)" else "Subtitles (OFF)",
                selected = selectedTab == 1,
                accentColor = LiquidCyan,
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .testTag("subtitle_tab_selector")
            )
        }

        if (selectedTab == 0) {
            // AUDIO TRACKS TAB
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Available Audio Tracks",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.72f)
                )
                playbackState.audioTracks.forEachIndexed { idx, track ->
                    val detailLine = listOfNotNull(
                        track.language.takeIf { it.isNotBlank() && it != "und" && it != "default" }?.uppercase(Locale.US),
                        track.codecInfo.takeIf { it.isNotBlank() && it != "stream" }
                    ).joinToString(" • ")

                    CinemaTrackSelectionRow(
                        title = track.label,
                        subtitle = detailLine.ifBlank { null },
                        selected = track.isSelected,
                        accentColor = ElectricIndigo,
                        onClick = { onSelectTrackOption(track) },
                        modifier = Modifier.testTag("audio_track_option_$idx")
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                val audioSyncAccent = adaptiveGlassAccent(
                    resolveContextualAccent(ElectricIndigo, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
                    true
                )
                Text(
                    text = "Audio Lip-Sync Delay: ${playbackState.audioDelayMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = audioSyncAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassPillButton(
                        onClick = { onAdjustAudioDelay(-50L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audio_delay_minus_button")
                    ) {
                        Text("-50ms", style = MaterialTheme.typography.labelSmall)
                    }
                    LiquidGlassPillButton(
                        onClick = { onAdjustAudioDelay(50L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("audio_delay_plus_button")
                    ) {
                        Text("+50ms", style = MaterialTheme.typography.labelSmall)
                    }
                    LiquidGlassPillButton(
                        onClick = { onAdjustAudioDelay(0L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset 0ms", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            // SUBTITLES TAB
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Explicit Off Option
                CinemaTrackSelectionRow(
                    title = "Off (Disable Subtitles)",
                    subtitle = if (!playbackState.subtitlesEnabled) "Currently disabled" else "Turn off active subtitles",
                    selected = !playbackState.subtitlesEnabled,
                    accentColor = GlassRoseTint,
                    onClick = {
                        if (onSetSubtitlesEnabled != null) {
                            onSetSubtitlesEnabled(false)
                        } else if (playbackState.subtitlesEnabled) {
                            onToggleSubtitles()
                        }
                    },
                    modifier = Modifier.testTag("subtitle_off_option")
                )

                // Embedded Subtitle Tracks
                if (playbackState.subtitleTracks.isEmpty()) {
                    Text(
                        text = "No embedded subtitle tracks detected. Load an external SRT/VTT/ASS file below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.68f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else {
                    Text(
                        text = "Embedded Subtitles (${playbackState.subtitleTracks.size})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    playbackState.subtitleTracks.forEachIndexed { idx, track ->
                        val detailLine = listOfNotNull(
                            track.language.takeIf { it.isNotBlank() && it != "und" }?.uppercase(Locale.US),
                            track.codecInfo.takeIf { it.isNotBlank() && it != "stream" }
                        ).joinToString(" • ")

                        CinemaTrackSelectionRow(
                            title = track.label,
                            subtitle = detailLine.ifBlank { null },
                            selected = playbackState.subtitlesEnabled && track.isSelected,
                            accentColor = LiquidCyan,
                            onClick = {
                                onSelectTrackOption(track)
                                if (onSetSubtitlesEnabled != null) {
                                    onSetSubtitlesEnabled(true)
                                } else if (!playbackState.subtitlesEnabled) {
                                    onToggleSubtitles()
                                }
                            },
                            modifier = Modifier.testTag("subtitle_track_option_$idx")
                        )
                    }
                }

                // External / Secondary Sidecar Files
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassPillButton(
                        onClick = onLoadPrimarySubtitleClick,
                        selected = playbackState.externalSubtitleCues.isNotEmpty() && playbackState.subtitlesEnabled,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("+ Load Primary Sub", style = MaterialTheme.typography.labelSmall)
                    }
                    LiquidGlassPillButton(
                        onClick = onLoadSecondarySubtitleClick,
                        selected = playbackState.secondarySubtitleCues.isNotEmpty() && playbackState.subtitlesEnabled,
                        accentColor = ElectricIndigo,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("+ Load 2nd Sub", style = MaterialTheme.typography.labelSmall)
                    }
                    if (playbackState.secondarySubtitleCues.isNotEmpty()) {
                        LiquidGlassPillButton(
                            onClick = onClearSecondarySubtitles,
                            accentColor = GlassRoseTint,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Clear 2nd", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Subtitle Timing Sync
                val subSyncAccent = adaptiveGlassAccent(
                    resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
                    true
                )
                Text(
                    text = "Subtitle Timing Offset: ${playbackState.subtitleDelayMs} ms",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = subSyncAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LiquidGlassPillButton(
                        onClick = { onAdjustSubtitleDelay(-100L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_delay_minus_button")
                    ) {
                        Text("-100ms", style = MaterialTheme.typography.labelSmall)
                    }
                    LiquidGlassPillButton(
                        onClick = { onAdjustSubtitleDelay(100L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("sub_delay_plus_button")
                    ) {
                        Text("+100ms", style = MaterialTheme.typography.labelSmall)
                    }
                    LiquidGlassPillButton(
                        onClick = { onAdjustSubtitleDelay(0L) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset 0ms", style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Quick Subtitle Styling (only displayed when live settings callback is wired)
                if (onUpdateSubtitleQuickStyle != null) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                    Text(
                        text = "Subtitle Size & Backdrop (${settings.subtitleFontSizeSp.toInt()}sp)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(14f to "Small", 18f to "Medium", 22f to "Large", 26f to "XL").forEach { (spVal, label) ->
                            LiquidGlassPillButton(
                                onClick = {
                                    onUpdateSubtitleQuickStyle(
                                        spVal,
                                        settings.subtitleBackgroundOpacity,
                                        settings.subtitleOutlineEnabled
                                    )
                                },
                                selected = abs(settings.subtitleFontSizeSp - spVal) < 1.5f,
                                accentColor = LiquidCyan,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        LiquidGlassPillButton(
                            onClick = {
                                onUpdateSubtitleQuickStyle(
                                    settings.subtitleFontSizeSp,
                                    settings.subtitleBackgroundOpacity,
                                    !settings.subtitleOutlineEnabled
                                )
                            },
                            selected = settings.subtitleOutlineEnabled,
                            accentColor = ElectricIndigo,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                if (settings.subtitleOutlineEnabled) "Outline ON" else "Outline OFF",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoAbRepeatSpeedSheet(
    playbackState: PlaybackUiState,
    isLandscape: Boolean,
    onClose: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onSetAbPointA: () -> Unit,
    onSetAbPointB: () -> Unit,
    onClearAbRepeat: () -> Unit,
    onStepFrameForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    CinemaSheetContainer(
        isLandscape = isLandscape,
        modifier = modifier.testTag("video_ab_speed_sheet")
    ) {
        CinemaSheetHeader(
            title = "Speed & A–B Repeat",
            icon = Icons.Rounded.Speed,
            accentColor = LiquidCyan,
            onClose = onClose
        )

        val speedAccent = adaptiveGlassAccent(
            resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
            true
        )
        Text(
            text = "Playback Speed: ${playbackState.playbackSpeed}x",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = speedAccent
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f).forEach { spd ->
                LiquidGlassPillButton(
                    onClick = { onSetPlaybackSpeed(spd) },
                    selected = abs(playbackState.playbackSpeed - spd) < 0.03f,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    modifier = Modifier.testTag("video_speed_preset_${spd}x")
                ) {
                    Text("${spd}x", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        Text(
            text = "Scene Loop (A–B Repeat) & Frame Step",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.85f)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassPillButton(
                onClick = onSetAbPointA,
                selected = playbackState.abRepeatStartMs != null,
                accentColor = GlassEmeraldTint,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("set_ab_point_a_button")
            ) {
                Text(
                    text = playbackState.abRepeatStartMs?.let { "A: ${formatDurationMs(it)}" } ?: "Set Point A",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
            LiquidGlassPillButton(
                onClick = onSetAbPointB,
                selected = playbackState.abRepeatEndMs != null,
                accentColor = GlassEmeraldTint,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("set_ab_point_b_button")
            ) {
                Text(
                    text = playbackState.abRepeatEndMs?.let { "B: ${formatDurationMs(it)}" } ?: "Set Point B",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LiquidGlassPillButton(
                onClick = onClearAbRepeat,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Clear Loop", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            LiquidGlassPillButton(
                onClick = onStepFrameForward,
                accentColor = ElectricIndigo,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("+1 Frame", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
fun VideoBookmarksSheet(
    positionMs: Long,
    bookmarks: List<VideoBookmarkEntity>,
    isLandscape: Boolean,
    onClose: () -> Unit,
    onAddBookmark: (String) -> Unit,
    onJumpToBookmark: (Long) -> Unit,
    onDeleteBookmark: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    CinemaSheetContainer(
        isLandscape = isLandscape,
        modifier = modifier.testTag("video_bookmarks_sheet")
    ) {
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
                val bmAccent = adaptiveGlassAccent(
                    resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
                    true
                )
                Icon(
                    imageVector = Icons.Rounded.Bookmark,
                    contentDescription = null,
                    tint = bmAccent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Scene Bookmarks",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (bookmarks.isNotEmpty()) {
                    LiquidGlassPillBadge(text = "${bookmarks.size}", color = LiquidCyan)
                }
            }
            IconButton(
                onClick = { onAddBookmark("Scene @ ${formatDurationMs(positionMs)}") },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_video_bookmark_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkAdd,
                    contentDescription = "Add Bookmark",
                    tint = GlassEmeraldTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (bookmarks.isEmpty()) {
            Text(
                text = "No scene bookmarks saved yet. Tap + to mark the current timestamp.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                bookmarks.forEach { bm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onJumpToBookmark(bm.timestampMs) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            val bmTimeAccent = adaptiveGlassAccent(
                                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
                                true
                            )
                            Text(
                                text = formatDurationMs(bm.timestampMs),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = bmTimeAccent
                            )
                            Text(
                                text = bm.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onDeleteBookmark(bm.id) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete Bookmark",
                                tint = GlassRoseTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoPlaybackOptionsSheet(
    media: MediaEntity,
    playbackState: PlaybackUiState,
    bookmarksCount: Int,
    brightnessLevel: Float,
    isLandscape: Boolean,
    onClose: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onCycleAspectRatio: () -> Unit,
    onResetZoom: () -> Unit,
    onToggleOrientation: () -> Unit,
    onOpenAudioSubtitles: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenAbSpeed: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenPlaybackInfo: () -> Unit,
    onOpenFullDetails: () -> Unit,
    onStartSleepTimer: (Int, Boolean) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onToggleDecoderBadge: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onCaptureFrame: () -> Unit,
    onMinimizeToFloatingPlayer: () -> Unit,
    onEnterPipMode: () -> Unit,
    onToggleAudioOnlyMode: () -> Unit,
    onToggleScreenLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSection by remember { mutableIntStateOf(0) }

    CinemaSheetContainer(
        isLandscape = isLandscape,
        modifier = modifier.testTag("video_options_sheet")
    ) {
        CinemaSheetHeader(
            title = "Playback Options",
            icon = Icons.Rounded.Tune,
            accentColor = LiquidCyan,
            onClose = onClose
        )

        // Compact Top Action Strip for Instant Access
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LiquidGlassPillButton(
                onClick = onOpenPlaybackInfo,
                accentColor = LiquidCyan,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("video_options_playback_info_button")
            ) {
                Icon(Icons.Rounded.Info, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Playback Info", style = MaterialTheme.typography.labelSmall)
            }
            LiquidGlassPillButton(
                onClick = onOpenFullDetails,
                accentColor = ElectricIndigo,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("video_options_details_button")
            ) {
                Text("Video Details", style = MaterialTheme.typography.labelSmall)
            }
            LiquidGlassPillButton(
                onClick = onToggleFavorite,
                selected = media.isFavorite,
                accentColor = GlassRoseTint,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (media.isFavorite) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (media.isFavorite) "Favorited" else "Favorite", style = MaterialTheme.typography.labelSmall)
            }
            LiquidGlassPillButton(
                onClick = onOpenBookmarks,
                accentColor = GlassEmeraldTint,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.testTag("video_bookmarks_button")
            ) {
                Icon(Icons.Rounded.Bookmark, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("Bookmarks ($bookmarksCount)", style = MaterialTheme.typography.labelSmall)
            }
        }

        // Section 0: Speed & A–B Repeat
        CinemaAccordionSection(
            title = "Playback Speed (${playbackState.playbackSpeed}x) & Repeat",
            icon = Icons.Rounded.Speed,
            expanded = expandedSection == 0,
            onClick = { expandedSection = if (expandedSection == 0) -1 else 0 }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                    LiquidGlassPillButton(
                        onClick = { onSetPlaybackSpeed(spd) },
                        selected = abs(playbackState.playbackSpeed - spd) < 0.03f,
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text("${spd}x", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = onOpenAbSpeed,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("A–B Loop & Frame Step", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onCycleRepeatMode,
                    selected = playbackState.repeatMode != RepeatModeState.OFF,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatModeState.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(playbackState.repeatMode.label, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Section 1: Screen Fit, Zoom & Orientation
        CinemaAccordionSection(
            title = "Screen Fit (${playbackState.aspectRatioMode.label}) & Orientation",
            icon = Icons.Rounded.AspectRatio,
            expanded = expandedSection == 1,
            onClick = { expandedSection = if (expandedSection == 1) -1 else 1 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = onCycleAspectRatio,
                    selected = true,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("video_aspect_ratio_button")
                ) {
                    Text("Fit: ${playbackState.aspectRatioMode.label}", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
                LiquidGlassPillButton(
                    onClick = onToggleOrientation,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp),
                    modifier = Modifier.testTag("video_rotate_button")
                ) {
                    Icon(Icons.Rounded.ScreenRotation, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rotate", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onResetZoom,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("100%", style = MaterialTheme.typography.labelSmall)
                }
            }
            Spacer(Modifier.height(6.dp))
            val brightnessAccent = adaptiveGlassAccent(
                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
                true
            )
            Text(
                text = "Screen Brightness: ${(brightnessLevel * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = brightnessAccent
            )
            com.example.ui.components.BnjCompactSlider(
                value = brightnessLevel.coerceIn(0.05f, 1f),
                onValueChange = onBrightnessChange,
                valueRange = 0.05f..1f,
                accentColor = LiquidCyan
            )
        }

        // Section 2: Audio, Subtitles & Equalizer
        CinemaAccordionSection(
            title = "Audio, Subtitles & Equalizer",
            icon = Icons.Rounded.Equalizer,
            expanded = expandedSection == 2,
            onClick = { expandedSection = if (expandedSection == 2) -1 else 2 }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = onOpenAudioSubtitles,
                    selected = true,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Audio & Subtitle Tracks", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onOpenEqualizer,
                    accentColor = ElectricIndigo,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text("Equalizer", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Section 3: Sleep Timer
        CinemaAccordionSection(
            title = if (playbackState.sleepTimerRemainingSec > 0) {
                "Sleep Timer (${playbackState.sleepTimerRemainingSec / 60}m left)"
            } else {
                "Sleep Timer"
            },
            icon = Icons.Rounded.Bedtime,
            expanded = expandedSection == 3,
            onClick = { expandedSection = if (expandedSection == 3) -1 else 3 }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                listOf(0 to "Off", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m").forEach { (min, label) ->
                    LiquidGlassPillButton(
                        onClick = { onStartSleepTimer(min, false) },
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(label, style = MaterialTheme.typography.labelSmall)
                    }
                }
                LiquidGlassPillButton(
                    onClick = { onStartSleepTimer(0, true) },
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text("End Video", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Section 4: Playback Modes & Tools (Decoder, PiP, Floating, Capture, Audio-Only, Lock)
        CinemaAccordionSection(
            title = "Decoder, Window & Capture Tools",
            icon = Icons.Rounded.NightlightRound,
            expanded = expandedSection == 4,
            onClick = { expandedSection = if (expandedSection == 4) -1 else 4 }
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                LiquidGlassPillButton(
                    onClick = onToggleDecoderBadge,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text("Decoder: ${playbackState.activeDecoderBadge}", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onEnterPipMode,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("PiP", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onMinimizeToFloatingPlayer,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Rounded.FullscreenExit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Floating", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onToggleAudioOnlyMode,
                    selected = playbackState.isAudioOnlyVideoMode,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Rounded.Headphones, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Audio Only", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onCaptureFrame,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Snapshot", style = MaterialTheme.typography.labelSmall)
                }
                LiquidGlassPillButton(
                    onClick = onToggleScreenLock,
                    accentColor = GlassAmberTint,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lock Touch", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun VideoPlaybackInfoOverlay(
    media: MediaEntity,
    playbackState: PlaybackUiState,
    isLandscape: Boolean,
    cachedInspection: VideoStreamInspection? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inspection by remember(media.uri, cachedInspection) {
        mutableStateOf(
            cachedInspection ?: CodecCapabilityInspector.getCachedVideoStreamInspection(media.uri, media.sizeBytes)
        )
    }

    LaunchedEffect(media.uri) {
        if (inspection == null) {
            inspection = withContext(Dispatchers.IO) {
                CodecCapabilityInspector.inspectVideoStreams(
                    context = context,
                    uriString = media.uri,
                    containerFormat = media.containerFormat,
                    mimeType = media.mimeType,
                    title = media.title,
                    sizeBytes = media.sizeBytes
                )
            }
        }
    }

    val canonicalExt = remember(media, inspection) {
        inspection?.canonicalExtension ?: CodecCapabilityInspector.resolveCanonicalExtension(
            uriOrPath = media.uri,
            containerFormat = media.containerFormat,
            mimeType = media.mimeType,
            title = media.title,
            isVideo = true
        )
    }
    val resolvedVideoCodec = remember(media, inspection) {
        VideoMetadataClassifier.resolveDisplayVideoCodec(media, inspection)
    }
    val resolvedFps = inspection?.frameRate?.takeIf { it > 0f } ?: media.frameRate.takeIf { it > 0f }
    val resolvedBitrate = inspection?.bitrate?.takeIf { it > 0 } ?: media.bitrate.takeIf { it > 0 }
    val resolvedHdr = (inspection?.hdrFormat ?: media.hdrFormat).takeIf { it.isNotBlank() }

    val resolutionText = if (media.width > 0 && media.height > 0) {
        "${media.width} × ${media.height} (${media.resolutionBadge})"
    } else {
        "Not reported"
    }
    val containerText = buildString {
        if (canonicalExt.isNotBlank()) append(".${canonicalExt.uppercase(Locale.US)}")
        val c = media.containerFormat.trim()
        if (c.isNotBlank() && !c.equals(canonicalExt, ignoreCase = true)) {
            if (isNotEmpty()) append(" • ")
            append(c)
        }
    }.ifBlank { "Not reported" }
    val fpsText = resolvedFps?.let { fps ->
        if (fps % 1f == 0f) "${fps.toInt()} fps" else String.format(Locale.US, "%.2f fps", fps)
    } ?: "Not reported"
    val bitrateText = resolvedBitrate?.let { bps ->
        when {
            bps >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", bps / 1_000_000f)
            bps >= 1_000 -> "${bps / 1_000} kbps"
            else -> "$bps bps"
        }
    } ?: "Not reported"
    val audioCodecText = when {
        inspection?.audioCodecLabels?.isNotEmpty() == true -> inspection!!.audioCodecLabels.joinToString(", ")
        playbackState.audioTracks.isNotEmpty() -> {
            playbackState.audioTracks.mapNotNull { t ->
                t.codecInfo.takeIf { it.isNotBlank() && it != "stream" && it != "audio/raw" }
                    ?.substringAfter('/')?.uppercase(Locale.US)
            }.distinct().joinToString(", ").ifBlank { "Not reported" }
        }
        else -> "Not reported"
    }
    val activeAudioTrack = playbackState.audioTracks.firstOrNull { it.isSelected }?.label ?: "Not reported"
    val embeddedSubCount = inspection?.embeddedSubtitleTrackCount ?: playbackState.subtitleTracks.size
    val sidecarCount = inspection?.sidecarSubtitleNames?.size ?: 0
    val externalLoaded = playbackState.externalSubtitleCues.isNotEmpty()
    val subtitleStatusText = when {
        !playbackState.subtitlesEnabled -> "Off ($embeddedSubCount embedded, $sidecarCount sidecar)"
        embeddedSubCount > 0 || sidecarCount > 0 || externalLoaded -> {
            val parts = buildList {
                if (embeddedSubCount > 0) add("$embeddedSubCount embedded")
                if (sidecarCount > 0 || externalLoaded) add("External loaded")
            }.joinToString(", ")
            "Active ($parts)"
        }
        inspection?.inspectionSucceeded == true -> "None detected"
        else -> "Not reported"
    }

    CinemaSheetContainer(
        isLandscape = isLandscape,
        modifier = modifier.testTag("video_playback_info_overlay")
    ) {
        CinemaSheetHeader(
            title = "Playback Information",
            icon = Icons.Rounded.Info,
            accentColor = LiquidCyan,
            onClose = onClose
        )

        Text(
            text = media.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

        CinemaMetadataRow("Resolution", resolutionText)
        CinemaMetadataRow("Video Codec", resolvedVideoCodec ?: "Not reported")
        CinemaMetadataRow("Container", containerText)
        CinemaMetadataRow("Frame Rate", fpsText)
        CinemaMetadataRow("Bitrate", bitrateText)
        CinemaMetadataRow("HDR Format", resolvedHdr ?: "Not reported")
        CinemaMetadataRow("Audio Codec", audioCodecText)
        CinemaMetadataRow("Audio Track", activeAudioTrack)
        CinemaMetadataRow("Subtitle Status", subtitleStatusText)
        CinemaMetadataRow("Decoder", playbackState.activeDecoderBadge)
        if (media.sizeBytes > 0L) {
            CinemaMetadataRow("File Size", FileSystemEntry.formatBytes(media.sizeBytes))
        }
        if (playbackState.liveStreamStats.isNotBlank()) {
            CinemaMetadataRow("Active Stream", playbackState.liveStreamStats)
        }
    }
}

@Composable
private fun CinemaSheetContainer(
    isLandscape: Boolean,
    modifier: Modifier = Modifier,
    scrollKey: Any = Unit,
    content: @Composable () -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollKey) {
        scrollState.scrollTo(0)
    }
    val darkGlassConfig = LocalLiquidGlassConfig.current.copy(isDarkGlass = true)
    val sheetCorner = (darkGlassConfig.glassCornerRadiusDp + 2f).coerceIn(14f, 28f).dp
    val sheetShape = RoundedCornerShape(sheetCorner)
    val parentDepth = LocalGlassNestingDepth.current

    CompositionLocalProvider(
        LocalLiquidGlassConfig provides darkGlassConfig
    ) {
        Column(
            modifier = modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .heightIn(max = if (isLandscape) 248.dp else 440.dp)
                .liquidGlassSurface(
                    shape = sheetShape,
                    elevated = true,
                    tier = GlassTier.FLOATING,
                    componentTarget = GlassComponentTarget.PLAYER_OVERLAY
                )
                .background(CinemaHudSurfaceElevated.copy(alpha = 0.42f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(34.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.34f))
            )
            CompositionLocalProvider(LocalGlassNestingDepth provides (parentDepth + 1)) {
                content()
            }
        }
    }
}

@Composable
private fun CinemaSheetHeader(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    onClose: () -> Unit
) {
    val headerAccent = adaptiveGlassAccent(
        resolveContextualAccent(accentColor, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
        true
    )
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
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = headerAccent,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close Panel",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CinemaSegmentTab(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabAccent = adaptiveGlassAccent(
        resolveContextualAccent(accentColor, LocalLiquidGlassConfig.current.copy(isDarkGlass = true)),
        true
    )
    Box(
        modifier = modifier
            .sizeIn(minHeight = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) tabAccent.copy(alpha = 0.24f) else Color.Transparent)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) tabAccent.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CinemaTrackSelectionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val config = LocalLiquidGlassConfig.current
    val rowConfig = config.copy(isDarkGlass = true)
    val contextual = resolveContextualAccent(accentColor, rowConfig)
    val resolvedAccent = adaptiveGlassAccent(contextual, true)
    val palette = resolveDynamicGlassPalette(
        config = rowConfig,
        tier = if (selected) GlassTier.ACTIVE else GlassTier.CLEAR,
        accentTint = resolvedAccent,
        elevated = selected
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.selectedSurfaceColor else palette.chipUnselectedColor)
            .border(
                width = 1.dp,
                color = if (selected) palette.selectedBorderColor else palette.borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(resolvedAccent.copy(alpha = 0.26f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = resolvedAccent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun CinemaAccordionSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val cinemaConfig = config.copy(isDarkGlass = true)
    val palette = resolveDynamicGlassPalette(
        config = cinemaConfig,
        tier = if (expanded) GlassTier.FROSTED else GlassTier.CLEAR,
        elevated = expanded
    )
    val accordionAccent = palette.effectiveAccent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (expanded) palette.selectedSurfaceColor.copy(alpha = (palette.selectedSurfaceColor.alpha * 0.72f).coerceIn(0.10f, 0.42f)) else palette.chipUnselectedColor)
            .border(
                width = 1.dp,
                color = if (expanded) palette.selectedBorderColor.copy(alpha = 0.55f) else palette.borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 40.dp)
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = accordionAccent, modifier = Modifier.size(17.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 2.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun CinemaMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.68f),
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
