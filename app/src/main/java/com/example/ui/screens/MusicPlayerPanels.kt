package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.MediaEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackProgressTickState
import kotlinx.coroutines.flow.StateFlow
import com.example.player.PlaybackUiState
import com.example.player.SleepTimerMode
import com.example.storage.FileSystemEntry
import com.example.subtitle.SubtitleEngine
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.GlassVioletTint
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.bnjGlassTextFieldColors
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.resolveContextualAccent
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun UpNextQueueGlassPanel(
    queue: List<MediaEntity>,
    currentIndex: Int,
    accentColor: Color = LiquidCyan,
    onPlayIndex: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onMoveQueueItem: (Int, Int) -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    onCloseToArtwork: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var playlistNameDraft by remember { mutableStateOf("") }
    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(
        resolveContextualAccent(accentColor, glassConfig),
        glassConfig.isDarkGlass
    )

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxSize()
            .testTag("music_queue_panel"),
        accentTint = accentColor,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Playing Queue",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${queue.size} tracks • Track ${(currentIndex + 1).coerceIn(1, queue.size.coerceAtLeast(1))} active",
                        style = MaterialTheme.typography.labelSmall,
                        color = resolvedAccent
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LiquidGlassPillButton(
                        onClick = { showSaveDialog = true },
                        accentColor = GlassEmeraldTint,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("queue_save_playlist_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save", style = MaterialTheme.typography.labelSmall)
                    }
                    IconButton(
                        onClick = onCloseToArtwork,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("queue_close_button")
                    ) {
                        Icon(
                            imageVector = BnjIcons.Close,
                            contentDescription = "Close Queue",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                modifier = Modifier.padding(vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(queue, key = { idx, item -> "${idx}_${item.uri}" }) { index, item ->
                    val isCurrent = index == currentIndex
                    val isUpcoming = index > currentIndex
                    if (isCurrent && queue.size > 1) {
                        Text(
                            text = "NOW PLAYING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = resolvedAccent,
                            letterSpacing = 0.9.sp,
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("queue_now_playing_header")
                        )
                    } else if (index == currentIndex + 1) {
                        Text(
                            text = "UP NEXT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.9.sp,
                            modifier = Modifier
                                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp)
                                .testTag("queue_up_next_header")
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isCurrent) resolvedAccent.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = if (isUpcoming) 0.06f else 0.03f)
                            )
                            .border(
                                width = if (isCurrent) 1.dp else 0.dp,
                                color = if (isCurrent) resolvedAccent.copy(alpha = 0.55f) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onPlayIndex(index) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("music_queue_item_$index"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            BnjArtworkSurface(
                                titleSeed = item.title,
                                mediaUri = item.uri,
                                isVideo = false,
                                shape = RoundedCornerShape(9.dp),
                                iconSize = 17.dp,
                                modifier = Modifier.size(38.dp)
                            )
                            if (isCurrent) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = "Currently Playing",
                                        tint = resolvedAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrent) resolvedAccent else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.artist.ifBlank { "Unknown Artist" }} • ${formatDurationMs(item.durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (index > 0) {
                            IconButton(
                                onClick = { onMoveQueueItem(index, index - 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        if (index < queue.lastIndex) {
                            IconButton(
                                onClick = { onMoveQueueItem(index, index + 1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        if (queue.size > 1) {
                            IconButton(
                                onClick = { onRemoveItem(index) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("music_queue_remove_$index")
                            ) {
                                Icon(
                                    imageVector = BnjIcons.Close,
                                    contentDescription = "Remove from Queue",
                                    tint = GlassRoseTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("Save Queue as Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistNameDraft,
                    onValueChange = { playlistNameDraft = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = bnjGlassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistNameDraft.isNotBlank()) {
                            onSaveQueueAsPlaylist(playlistNameDraft.trim())
                            playlistNameDraft = ""
                            showSaveDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
internal fun SyncedLyricsGlassPanel(
    media: MediaEntity,
    currentPositionMs: Long,
    accentColor: Color = LiquidCyan,
    onSeekTo: (Long) -> Unit,
    onImportLrcUri: (Uri) -> Unit,
    onCloseToArtwork: () -> Unit,
    progressStateFlow: StateFlow<PlaybackProgressTickState>? = null
) {
    val context = LocalContext.current
    var lyricOffsetMs by remember(media.uri) { mutableLongStateOf(0L) }
    var largeLyricsFont by remember { mutableStateOf(false) }
    var showFullscreenDialog by remember { mutableStateOf(false) }

    val lrcPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onImportLrcUri(uri)
    }

    val parsedCues = remember(media.localLyrics) {
        SubtitleEngine.parseLrcLyrics(media.localLyrics)
    }
    val hasTimestampedLyrics = remember(parsedCues) {
        parsedCues.any { it.timeMs >= 0L }
    }
    val liveTick = progressStateFlow?.collectAsState()?.value
    val effectivePosMs = liveTick?.positionMs ?: currentPositionMs
    val activeIndex = remember(parsedCues, hasTimestampedLyrics, effectivePosMs, lyricOffsetMs) {
        if (hasTimestampedLyrics) {
            SubtitleEngine.findActiveLrcIndex(parsedCues, effectivePosMs, lyricOffsetMs)
        } else {
            -1
        }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex, hasTimestampedLyrics) {
        if (hasTimestampedLyrics && activeIndex in parsedCues.indices) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedAccent = adaptiveGlassAccent(
        resolveContextualAccent(accentColor, glassConfig),
        glassConfig.isDarkGlass
    )

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxSize()
            .testTag("music_lyrics_panel"),
        accentTint = accentColor,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (hasTimestampedLyrics) "Synced Lyrics" else "Lyrics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = resolvedAccent
                    )
                    if (parsedCues.isNotEmpty() && !hasTimestampedLyrics) {
                        Text(
                            text = "Unsynced plain text",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassPillButton(
                        onClick = { lrcPicker.launch("*/*") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("lyrics_import_lrc_button")
                    ) {
                        Text("+ .LRC", style = MaterialTheme.typography.labelSmall)
                    }
                    if (parsedCues.isNotEmpty()) {
                        LiquidGlassPillButton(
                            onClick = { largeLyricsFont = !largeLyricsFont },
                            selected = largeLyricsFont,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(if (largeLyricsFont) "A-" else "A+", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(
                            onClick = { showFullscreenDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(BnjIcons.Fullscreen, contentDescription = "Fullscreen Lyrics", modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val textToShare = buildString {
                                    appendLine("${media.title} — ${media.artist.ifBlank { "Unknown Artist" }}")
                                    appendLine()
                                    parsedCues.forEach { appendLine(it.text) }
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Lyrics"))
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share Lyrics", modifier = Modifier.size(16.dp))
                        }
                    }
                    IconButton(
                        onClick = onCloseToArtwork,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("lyrics_close_button")
                    ) {
                        Icon(
                            imageVector = BnjIcons.Close,
                            contentDescription = "Close Lyrics",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (hasTimestampedLyrics) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sync Offset: ${if (lyricOffsetMs >= 0) "+$lyricOffsetMs" else "$lyricOffsetMs"} ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        LiquidGlassPillButton(
                            onClick = { lyricOffsetMs = (lyricOffsetMs - 250L).coerceIn(-5000L, 5000L) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("-250ms", style = MaterialTheme.typography.labelSmall)
                        }
                        LiquidGlassPillButton(
                            onClick = { lyricOffsetMs = 0L },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("0ms", style = MaterialTheme.typography.labelSmall)
                        }
                        LiquidGlassPillButton(
                            onClick = { lyricOffsetMs = (lyricOffsetMs + 250L).coerceIn(-5000L, 5000L) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("+250ms", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f),
                modifier = Modifier.padding(vertical = 4.dp)
            )

            if (parsedCues.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = BnjIcons.Lyrics,
                            contentDescription = null,
                            tint = resolvedAccent.copy(alpha = 0.72f),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "No lyrics attached to this track",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Tap '+ .LRC' above to load a synchronized .lrc or plain-text lyrics file from device storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(parsedCues, key = { idx, cue -> "lrc_${idx}_${cue.index}_${cue.timeMs}" }) { index, cue ->
                        val isCurrent = hasTimestampedLyrics && index == activeIndex
                        val fontSize = when {
                            isCurrent && largeLyricsFont -> 22.sp
                            isCurrent -> 19.sp
                            largeLyricsFont -> 17.sp
                            else -> 15.sp
                        }
                        Text(
                            text = cue.text,
                            fontSize = fontSize,
                            lineHeight = (fontSize.value * 1.35f).sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                            color = when {
                                isCurrent -> com.example.ui.theme.LocalBnjTextColors.current.accent
                                !hasTimestampedLyrics -> com.example.ui.theme.LocalBnjTextColors.current.lyrics
                                else -> com.example.ui.theme.LocalBnjTextColors.current.tertiary
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) resolvedAccent.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable(enabled = cue.timeMs >= 0L) {
                                    if (cue.timeMs >= 0L) onSeekTo(cue.timeMs)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (showFullscreenDialog && parsedCues.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showFullscreenDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("${media.title} — Lyrics") },
            text = {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(parsedCues, key = { idx, cue -> "lrc_fs_${idx}_${cue.index}_${cue.timeMs}" }) { idx, cue ->
                        val isCurrent = hasTimestampedLyrics && idx == activeIndex
                        Text(
                            text = cue.text,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) resolvedAccent else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = cue.timeMs >= 0L) {
                                    if (cue.timeMs >= 0L) onSeekTo(cue.timeMs)
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFullscreenDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun EqualizerDspGlassPanel(
    eqState: AudioEffectsUiState,
    customPresets: List<EqualizerPresetEntity>,
    customPresetName: String,
    onCustomPresetNameChange: (String) -> Unit,
    onSetEqEnabled: (Boolean) -> Unit,
    onSetEqBand: (Int, Int) -> Unit,
    onSetBassBoost: (Int) -> Unit,
    onSetVirtualizer: (Int) -> Unit,
    onSetLoudnessGain: (Int) -> Unit = {},
    onSetReverbPreset: (Int) -> Unit = {},
    onSetStereoBalance: (Float) -> Unit = {},
    onApplyPreset: (String) -> Unit,
    onSaveCustomPreset: () -> Unit,
    onRenameCustomPreset: (Long, String) -> Unit = { _, _ -> },
    onDeleteCustomPreset: (Long) -> Unit = {},
    initialExpanded: Boolean = false,
    onCloseToArtwork: (() -> Unit)? = null
) {
    var isExpanded by rememberSaveable { mutableStateOf(initialExpanded) }
    var activeSubSection by rememberSaveable { mutableStateOf(0) }
    var presetToManage by remember { mutableStateOf<EqualizerPresetEntity?>(null) }
    var renameDraft by remember { mutableStateOf("") }

    val glassConfig = LocalLiquidGlassConfig.current
    val eqPrimary = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    val eqSecondary = adaptiveGlassAccent(
        resolveContextualAccent(ElectricIndigo, glassConfig),
        glassConfig.isDarkGlass
    )

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .testTag("music_equalizer_panel"),
        accentTint = LiquidCyan,
        elevated = true,
        contentPadding = PaddingValues(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(eqPrimary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Equalizer",
                            tint = eqPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Equalizer & Android DSP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Preset: ${eqState.activePresetName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = eqPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = onSetEqEnabled,
                        modifier = Modifier
                            .scale(0.8f)
                            .testTag("eq_master_switch")
                    )
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse Equalizer" else "Expand Equalizer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (onCloseToArtwork != null) {
                        IconButton(
                            onClick = onCloseToArtwork,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("eq_close_button")
                        ) {
                            Icon(
                                imageVector = BnjIcons.Close,
                                contentDescription = "Close Equalizer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LiquidGlassPillButton(
                                onClick = { activeSubSection = 0 },
                                selected = activeSubSection == 0,
                                accentColor = LiquidCyan,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("EQ Bands", style = MaterialTheme.typography.labelMedium)
                            }
                            LiquidGlassPillButton(
                                onClick = { activeSubSection = 1 },
                                selected = activeSubSection == 1,
                                accentColor = ElectricIndigo,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("Audio DSP", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        LiquidGlassPillButton(
                            onClick = {
                                eqState.bands.forEach { b -> onSetEqBand(b.index, 0) }
                                onSetBassBoost(0)
                                onSetVirtualizer(0)
                                onSetLoudnessGain(0)
                                onSetStereoBalance(0f)
                                onApplyPreset("Flat")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (activeSubSection == 0) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "Presets",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                eqState.builtInPresets.forEach { presetName ->
                                    val selected = eqState.activePresetName == presetName
                                    LiquidGlassPillButton(
                                        onClick = { onApplyPreset(presetName) },
                                        selected = selected,
                                        accentColor = LiquidCyan,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(presetName, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        if (customPresets.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                customPresets.forEach { custom ->
                                    val selected = eqState.activePresetName == custom.name
                                    LiquidGlassPillButton(
                                        onClick = { onApplyPreset(custom.name) },
                                        selected = selected,
                                        accentColor = GlassEmeraldTint,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(custom.name, style = MaterialTheme.typography.labelSmall)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = "Edit Preset",
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable {
                                                    presetToManage = custom
                                                    renameDraft = custom.name
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            eqState.bands.forEach { band ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = band.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.width(44.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    com.example.ui.components.BnjCompactSlider(
                                        value = band.currentLevelMb.toFloat(),
                                        onValueChange = { onSetEqBand(band.index, it.toInt()) },
                                        valueRange = band.minLevelMb.toFloat()..band.maxLevelMb.toFloat(),
                                        accentColor = LiquidCyan,
                                        bipolarCenter = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${band.currentLevelMb / 100}dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.widthIn(min = 40.dp),
                                        color = eqPrimary,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = customPresetName,
                                onValueChange = onCustomPresetNameChange,
                                placeholder = { Text("Custom preset name", style = MaterialTheme.typography.labelSmall) },
                                singleLine = true,
                                colors = bnjGlassTextFieldColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                shape = RoundedCornerShape(10.dp)
                            )
                            LiquidGlassPillButton(
                                onClick = onSaveCustomPreset,
                                selected = true,
                                accentColor = LiquidCyan,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Save", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bass Boost", style = MaterialTheme.typography.labelSmall, color = eqSecondary)
                                Text("${eqState.bassBoostStrength / 10}%", style = MaterialTheme.typography.labelSmall, color = eqSecondary)
                            }
                            com.example.ui.components.BnjCompactSlider(
                                value = eqState.bassBoostStrength.toFloat(),
                                onValueChange = { onSetBassBoost(it.toInt()) },
                                valueRange = 0f..1000f,
                                accentColor = GlassVioletTint
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("3D Surround", style = MaterialTheme.typography.labelSmall, color = eqSecondary)
                                Text("${eqState.virtualizerStrength / 10}%", style = MaterialTheme.typography.labelSmall, color = eqSecondary)
                            }
                            com.example.ui.components.BnjCompactSlider(
                                value = eqState.virtualizerStrength.toFloat(),
                                onValueChange = { onSetVirtualizer(it.toInt()) },
                                valueRange = 0f..1000f,
                                accentColor = ElectricIndigo
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Loudness Gain", style = MaterialTheme.typography.labelSmall, color = GlassEmeraldTint)
                                Text("+${eqState.loudnessGainMb}mB", style = MaterialTheme.typography.labelSmall, color = GlassEmeraldTint)
                            }
                            com.example.ui.components.BnjCompactSlider(
                                value = eqState.loudnessGainMb.toFloat(),
                                onValueChange = { onSetLoudnessGain(it.toInt()) },
                                valueRange = 0f..1500f,
                                accentColor = GlassEmeraldTint,
                                modifier = Modifier.testTag("eq_loudness_slider")
                            )

                            val balPct = (eqState.balance * 100).roundToInt()
                            val balLabel = when {
                                balPct < -3 -> "${abs(balPct)}% L"
                                balPct > 3 -> "${balPct}% R"
                                else -> "Center"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stereo Balance ($balLabel)", style = MaterialTheme.typography.labelSmall, color = GlassAmberTint)
                                if (balPct != 0) {
                                    Text(
                                        text = "Center",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = eqPrimary,
                                        modifier = Modifier.clickable { onSetStereoBalance(0f) }
                                    )
                                }
                            }
                            com.example.ui.components.BnjCompactSlider(
                                value = eqState.balance,
                                onValueChange = { onSetStereoBalance(it) },
                                valueRange = -1f..1f,
                                accentColor = GlassAmberTint,
                                bipolarCenter = true,
                                modifier = Modifier.testTag("eq_balance_slider")
                            )

                            Text("Reverb Preset", style = MaterialTheme.typography.labelSmall, color = eqPrimary)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AudioEffectsUiState.reverbPresetLabels.forEachIndexed { idx, label ->
                                    LiquidGlassPillButton(
                                        onClick = { onSetReverbPreset(idx) },
                                        selected = eqState.reverbPreset == idx,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    presetToManage?.let { target ->
        AlertDialog(
            onDismissRequest = { presetToManage = null },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("Manage Custom Preset") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("Preset Name") },
                    singleLine = true,
                    colors = bnjGlassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDraft.isNotBlank()) {
                            onRenameCustomPreset(target.id, renameDraft.trim())
                            presetToManage = null
                        }
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onDeleteCustomPreset(target.id)
                            presetToManage = null
                        }
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = null, tint = GlassRoseTint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = GlassRoseTint)
                    }
                    TextButton(onClick = { presetToManage = null }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
internal fun SpeedSleepAbGlassPanel(
    playbackState: PlaybackUiState,
    accentColor: Color = GlassAmberTint,
    onSetSpeed: (Float) -> Unit,
    onSetSleepTimer: (Int, Boolean) -> Unit,
    onCloseToArtwork: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    val sleepTimers = listOf(0, 15, 30, 45, 60)

    LiquidGlassCard(
        modifier = Modifier
            .fillMaxSize()
            .testTag("music_speed_timer_panel"),
        accentTint = accentColor,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Speed & Sleep Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onCloseToArtwork,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = BnjIcons.Close,
                        contentDescription = "Close Timer Panel",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            val speedPrimary = adaptiveGlassAccent(
                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                LocalLiquidGlassConfig.current.isDarkGlass
            )
            Text(
                text = "Playback Speed (${playbackState.playbackSpeed}x)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = speedPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                speeds.forEach { spd ->
                    LiquidGlassPillButton(
                        onClick = { onSetSpeed(spd) },
                        selected = abs(playbackState.playbackSpeed - spd) < 0.03f,
                        accentColor = LiquidCyan,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("${spd}x", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))

            val timerHeader = when {
                playbackState.sleepTimerMode == SleepTimerMode.END_OF_TRACK ->
                    "Sleep Timer: Pause at End of Current Track"
                playbackState.sleepTimerRemainingSec > 0 ->
                    "Sleep Timer: ${playbackState.sleepTimerRemainingSec / 60}m ${playbackState.sleepTimerRemainingSec % 60}s (6s fade-out)"
                else -> "Sleep Timer: Off"
            }
            Text(
                text = timerHeader,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (playbackState.sleepTimerMode != SleepTimerMode.OFF) GlassAmberTint else MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                sleepTimers.forEach { min ->
                    val selected = if (min == 0) {
                        playbackState.sleepTimerMode == SleepTimerMode.OFF
                    } else {
                        playbackState.sleepTimerMode == SleepTimerMode.MINUTES &&
                            playbackState.sleepTimerRemainingSec > (min - 1) * 60 &&
                            playbackState.sleepTimerRemainingSec <= min * 60
                    }
                    LiquidGlassPillButton(
                        onClick = { onSetSleepTimer(min, false) },
                        selected = selected,
                        accentColor = GlassAmberTint,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(if (min == 0) "Off" else "${min}m", style = MaterialTheme.typography.labelMedium)
                    }
                }
                LiquidGlassPillButton(
                    onClick = { onSetSleepTimer(0, true) },
                    selected = playbackState.sleepTimerMode == SleepTimerMode.END_OF_TRACK,
                    accentColor = ElectricIndigo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("sleep_timer_end_of_track")
                ) {
                    Text("End of Track", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingAudioDetailsSheet(
    media: MediaEntity,
    playbackState: PlaybackUiState,
    eqState: AudioEffectsUiState,
    onDismiss: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = dynamicGlassDialogContainerColor(),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 34.dp, height = 3.5.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
                .testTag("music_track_info_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BnjArtworkSurface(
                    titleSeed = media.title,
                    mediaUri = media.uri,
                    isVideo = false,
                    shape = RoundedCornerShape(12.dp),
                    badgeText = media.codecName.takeIf { it.isNotBlank() },
                    modifier = Modifier.size(48.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${media.artist.ifBlank { "Unknown Artist" }} • ${media.album.ifBlank { "Single / Local" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = onOpenEqualizer,
                    selected = true,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("EQ (${eqState.activePresetName})", style = MaterialTheme.typography.labelMedium)
                }
                LiquidGlassPillButton(
                    onClick = onOpenSleepTimer,
                    accentColor = ElectricIndigo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Timer / Speed", style = MaterialTheme.typography.labelMedium)
                }
            }

            LiquidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentTint = LiquidCyan,
                contentPadding = PaddingValues(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val infoAccent = adaptiveGlassAccent(
                            resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                            LocalLiquidGlassConfig.current.isDarkGlass
                        )
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = infoAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Track & Audio Output Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = infoAccent
                        )
                    }
                    MusicMetadataRow(
                        label = "Codec / Format",
                        value = listOfNotNull(
                            media.codecName.takeIf { it.isNotBlank() },
                            media.mimeType.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "Not reported" }
                    )
                    MusicMetadataRow(
                        label = "Sample Rate",
                        value = if (media.sampleRateHz > 0) {
                            "${media.sampleRateHz} Hz" + if (media.channelCount > 0) " • ${media.channelCount} ch" else ""
                        } else "Not reported"
                    )
                    MusicMetadataRow(
                        label = "Bitrate",
                        value = if (media.bitrate > 0) "${media.bitrate / 1000} kbps" else "Not reported"
                    )
                    MusicMetadataRow(
                        label = "Duration / Size",
                        value = "${formatDurationMs(media.durationMs)}" +
                            if (media.sizeBytes > 0L) " • ${FileSystemEntry.formatBytes(media.sizeBytes)}" else ""
                    )
                    MusicMetadataRow("Decoder", playbackState.activeDecoderBadge)
                    if (media.playCount > 0) {
                        MusicMetadataRow("Play Count", "${media.playCount}")
                    }
                    if (media.folderPath.isNotBlank()) {
                        MusicMetadataRow("Location", media.folderPath)
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicMetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
