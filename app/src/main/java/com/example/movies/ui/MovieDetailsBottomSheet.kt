package com.example.movies.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.codec.VideoStreamInspection
import com.example.data.local.MediaEntity
import com.example.movies.model.MovieItem
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MovieDetailsBottomSheet(
    movieItem: MovieItem,
    inspection: VideoStreamInspection?,
    onDismiss: () -> Unit,
    onPlayMovie: (MediaEntity, Long) -> Unit, // media and starting positionMs
    onToggleFavorite: (MovieItem) -> Unit,
    onOpenMatchDialog: (MovieItem) -> Unit,
    onRefreshMetadata: (MovieItem) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val glassConfig = LocalLiquidGlassConfig.current
    val themePrimary = adaptiveGlassAccent(glassConfig.dynamicAccent, glassConfig.isDarkGlass)
    val themeSecondary = adaptiveGlassAccent(glassConfig.secondaryAccent, glassConfig.isDarkGlass)
    val onPrimary = GlassContrastResolver.resolveReadableForeground(themePrimary, glassConfig.isDarkGlass, highContrast = true)
    val onSecondary = GlassContrastResolver.resolveReadableForeground(themeSecondary, glassConfig.isDarkGlass, highContrast = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = com.example.ui.theme.dynamicGlassDialogContainerColor(),
        dragHandle = null,
        modifier = Modifier.testTag("movie_details_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Cinematic Backdrop with Gradient Scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                if (movieItem.backdropUrl.isNotBlank()) {
                    AsyncImage(
                        model = movieItem.backdropUrl,
                        contentDescription = "Movie Backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (movieItem.posterUrl.isNotBlank()) {
                    AsyncImage(
                        model = movieItem.posterUrl,
                        contentDescription = "Movie Poster Backdrop",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(themeSecondary.copy(alpha = 0.5f), themePrimary.copy(alpha = 0.3f), Color.Black)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }

                // Vertical dark scrim to seamlessly merge into the sheet body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.35f),
                                    0.5f to Color.Transparent,
                                    1.0f to MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                )

                // Top Close & Favorite Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .testTag("movie_details_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { onToggleFavorite(movieItem) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("movie_favorite_button")
                        ) {
                            Icon(
                                imageVector = if (movieItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (movieItem.isFavorite) GlassRoseTint else Color.White
                            )
                        }

                        IconButton(
                            onClick = { onOpenMatchDialog(movieItem) },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f))
                                .testTag("movie_correct_match_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Correct Match", tint = Color.White)
                        }
                    }
                }
            }

            // Movie Main Details Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Poster + Title, Year, Runtime, Rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // 2:3 Movie Poster
                    Box(
                        modifier = Modifier
                            .width(105.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        if (movieItem.posterUrl.isNotBlank()) {
                            AsyncImage(
                                model = movieItem.posterUrl,
                                contentDescription = movieItem.displayTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                themeSecondary.copy(alpha = 0.4f),
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = themePrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Text(
                                        text = movieItem.displayTitle.take(16),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Metadata Text & Badges
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = movieItem.displayTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("movie_details_title")
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (movieItem.releaseYearDisplay.isNotBlank()) {
                                Text(
                                    text = movieItem.releaseYearDisplay,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "•",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = movieItem.runtimeDisplay,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (movieItem.media.is4kUhd) {
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(themePrimary.copy(alpha = 0.18f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "4K UHD",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = themePrimary
                                    )
                                }
                            }
                        }

                        // Rating pill
                        if (movieItem.metadata.rating > 0f) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GlassAmberTint.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = GlassAmberTint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = String.format("%.1f / 10", movieItem.metadata.rating),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = GlassAmberTint
                                )
                                if (movieItem.metadata.voteCount > 0) {
                                    Text(
                                        text = "(${movieItem.metadata.voteCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Genres FlowRow
                        if (movieItem.genresList.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                movieItem.genresList.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = genre,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Playback Actions (Resume & Play from Beginning)
                val hasProgress = movieItem.media.lastPositionMs > 2000L && movieItem.media.durationMs > 0L
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasProgress) {
                        val progressFraction = (movieItem.media.lastPositionMs.toFloat() / movieItem.media.durationMs.toFloat()).coerceIn(0f, 1f)
                        val posSec = movieItem.media.lastPositionMs / 1000L
                        val posMin = (posSec % 3600) / 60
                        val posHour = posSec / 3600
                        val posFormatted = if (posHour > 0) "${posHour}h ${posMin}m" else "${posMin}m"

                        Button(
                            onClick = {
                                onPlayMovie(movieItem.media, movieItem.media.lastPositionMs)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themePrimary,
                                contentColor = onPrimary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("movie_resume_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Resume Playback (at $posFormatted)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            color = themePrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                        )

                        OutlinedButton(
                            onClick = {
                                onPlayMovie(movieItem.media, 0L)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("movie_play_beginning_button")
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Play from Beginning", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Button(
                            onClick = {
                                onPlayMovie(movieItem.media, 0L)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeSecondary,
                                contentColor = onSecondary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("movie_play_beginning_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Play Movie", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }

                // Overview Synopsis
                if (movieItem.metadata.overview.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Storyline",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = movieItem.metadata.overview,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Technical File & Stream Information Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Technical File & Stream Info",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = themePrimary
                    )

                    TechnicalRow("File Name", movieItem.media.title)
                    TechnicalRow("Format", "${movieItem.media.containerFormat.uppercase()} • ${formatFileSize(movieItem.media.sizeBytes)}")
                    TechnicalRow("Video Codec", movieItem.media.codecName.ifBlank { "HEVC / H.264" })
                    TechnicalRow(
                        "Resolution",
                        "${movieItem.media.width} x ${movieItem.media.height} (${movieItem.media.resolutionBadge})"
                    )
                    TechnicalRow("Color Space", movieItem.media.hdrFormat.ifBlank { "SDR" })

                    if (inspection != null) {
                        if (inspection.audioCodecLabels.isNotEmpty() || (inspection.audioTrackCount ?: 0) > 0) {
                            val trackCount = inspection.audioTrackCount ?: inspection.audioCodecLabels.size
                            val codecs = if (inspection.audioCodecLabels.isNotEmpty()) {
                                ": " + inspection.audioCodecLabels.joinToString()
                            } else ""
                            TechnicalRow(
                                "Audio Tracks",
                                "$trackCount track(s)$codecs"
                            )
                        }
                        val subCount = (inspection.embeddedSubtitleTrackCount ?: 0) + inspection.sidecarSubtitleNames.size
                        if (subCount > 0) {
                            val detail = if (inspection.sidecarSubtitleNames.isNotEmpty()) {
                                "$subCount subtitle(s) (${inspection.sidecarSubtitleNames.size} external)"
                            } else {
                                "$subCount embedded subtitle(s)"
                            }
                            TechnicalRow(
                                "Subtitles",
                                detail
                            )
                        }
                    }

                    TechnicalRow("Storage Path", movieItem.media.folderPath)
                }

                // Bottom Secondary Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onRefreshMetadata(movieItem) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movie_refresh_metadata_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Refresh Metadata", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { onOpenMatchDialog(movieItem) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("movie_edit_match_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit Match", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TechnicalRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1000.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.0f MB", mb)
    }
}
