package com.example.movies.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.movies.data.MovieMetadataEntity
import com.example.movies.engine.MovieMetadataEngine
import com.example.movies.model.MovieItem
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import kotlinx.coroutines.launch

@Composable
fun MovieMatchDialog(
    movieItem: MovieItem,
    tmdbApiKey: String,
    onDismiss: () -> Unit,
    onSaveMatch: (MovieMetadataEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf(movieItem.displayTitle) }
    var searchYearText by remember { mutableStateOf(movieItem.metadata.releaseYear?.toString() ?: "") }
    var userApiKey by remember { mutableStateOf(tmdbApiKey) }
    var isSearching by remember { mutableStateOf(false) }
    var searchCandidates by remember { mutableStateOf<List<MovieMetadataEntity>>(emptyList()) }
    var selectedCandidate by remember { mutableStateOf<MovieMetadataEntity?>(null) }
    val scope = rememberCoroutineScope()
    val glassConfig = LocalLiquidGlassConfig.current
    val themePrimary = adaptiveGlassAccent(glassConfig.dynamicAccent, glassConfig.isDarkGlass)
    val themeSecondary = adaptiveGlassAccent(glassConfig.secondaryAccent, glassConfig.isDarkGlass)
    val onPrimary = GlassContrastResolver.resolveReadableForeground(themePrimary, glassConfig.isDarkGlass, highContrast = true)
    val onSecondary = GlassContrastResolver.resolveReadableForeground(themeSecondary, glassConfig.isDarkGlass, highContrast = true)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("movie_match_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = themePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Match Movie Metadata",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("movie_match_cancel_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "Search TMDB and reference cinema catalogs to automatically link posters, ratings, synopsis, and release details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Search Input Fields
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Movie Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("movie_match_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = themePrimary,
                        focusedLabelColor = themePrimary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchYearText,
                        onValueChange = { searchYearText = it },
                        label = { Text("Year (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = {
                            val cleanQuery = searchQuery.trim()
                            val cleanYear = searchYearText.toIntOrNull()
                            if (cleanQuery.isNotBlank()) {
                                isSearching = true
                                scope.launch {
                                    val offlineMatch = MovieMetadataEngine.matchOfflineCatalog(cleanQuery, cleanYear)
                                    val onlineResults = if (userApiKey.isNotBlank()) {
                                        MovieMetadataEngine.searchTmdb(cleanQuery, cleanYear, userApiKey)
                                    } else {
                                        emptyList()
                                    }

                                    val combined = mutableListOf<MovieMetadataEntity>()
                                    if (offlineMatch != null) {
                                        combined.add(offlineMatch)
                                    }
                                    combined.addAll(onlineResults.filter { it.title != offlineMatch?.title })
                                    searchCandidates = combined
                                    isSearching = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themePrimary, contentColor = onPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("movie_match_search_button")
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Search", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Optional TMDB API Key entry
                OutlinedTextField(
                    value = userApiKey,
                    onValueChange = { userApiKey = it },
                    label = { Text("TMDB API Key (Optional for online search)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Results list
                if (searchCandidates.isNotEmpty()) {
                    Text(
                        text = "Select Matching Movie (${searchCandidates.size} found):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(searchCandidates) { candidate ->
                            val isSelected = selectedCandidate == candidate
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) themePrimary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) themePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCandidate = candidate }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (candidate.posterUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = candidate.posterUrl,
                                        contentDescription = candidate.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 60.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 40.dp, height = 60.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.DarkGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.LightGray)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (candidate.releaseYear != null) {
                                            Text(
                                                text = "${candidate.releaseYear}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (candidate.rating > 0f) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = GlassAmberTint, modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text(
                                                    text = String.format("%.1f", candidate.rating),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GlassAmberTint,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    if (candidate.genres.isNotBlank()) {
                                        Text(
                                            text = candidate.genres,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!isSearching) {
                    Text(
                        text = "Tip: You can also manually rename this movie or paste an image URL below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val chosen = selectedCandidate
                            if (chosen != null) {
                                val updated = chosen.copy(
                                    mediaUri = movieItem.media.uri,
                                    runtimeMinutes = if (chosen.runtimeMinutes > 0) chosen.runtimeMinutes
                                    else (movieItem.media.durationMs / 60000L).toInt().coerceAtLeast(1),
                                    isMatched = true
                                )
                                onSaveMatch(updated)
                            } else {
                                // Manual custom rename
                                val updated = movieItem.metadata.copy(
                                    userCustomTitle = searchQuery.trim().ifBlank { null },
                                    releaseYear = searchYearText.toIntOrNull() ?: movieItem.metadata.releaseYear,
                                    isMatched = true
                                )
                                onSaveMatch(updated)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = themeSecondary, contentColor = onSecondary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("movie_match_confirm_button")
                    ) {
                        Text("Apply Match", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
