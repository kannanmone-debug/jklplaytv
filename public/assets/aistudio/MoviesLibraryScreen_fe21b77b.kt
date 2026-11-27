package com.example.movies.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.codec.CodecCapabilityInspector
import com.example.codec.DeviceMediaCapabilities
import com.example.codec.VideoStreamInspection
import com.example.data.local.MediaEntity
import com.example.movies.data.BnjMovieDatabase
import com.example.movies.data.MovieMetadataEntity
import com.example.movies.engine.MovieMetadataEngine
import com.example.movies.model.MovieItem
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassContrastResolver
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalDynamicBottomInsets
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MovieSortOption(val label: String) {
    TITLE_ASC("Title (A–Z)"),
    TITLE_DESC("Title (Z–A)"),
    YEAR_DESC("Release Year (Newest)"),
    YEAR_ASC("Release Year (Oldest)"),
    RATING_DESC("Rating (Highest)"),
    RUNTIME_DESC("Runtime (Longest)"),
    RECENTLY_ADDED("Recently Added")
}

@Composable
fun MoviesLibraryScreen(
    videos: List<MediaEntity>,
    capabilities: DeviceMediaCapabilities,
    tmdbApiKey: String = "",
    onPlayVideoQueue: (List<MediaEntity>, Int, Boolean) -> Unit,
    onToggleMediaFavorite: (MediaEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val glassConfig = LocalLiquidGlassConfig.current
    val themePrimary = adaptiveGlassAccent(glassConfig.dynamicAccent, glassConfig.isDarkGlass)
    val themeSecondary = adaptiveGlassAccent(glassConfig.secondaryAccent, glassConfig.isDarkGlass)
    val movieDatabase = remember { BnjMovieDatabase.getInstance(context) }
    val movieDao = remember { movieDatabase.movieMetadataDao() }

    // Live metadata stream from Room database
    val cachedMetadataList by movieDao.observeAll().collectAsState(initial = emptyList())
    val metadataMap = remember(cachedMetadataList) {
        cachedMetadataList.associateBy { it.mediaUri }
    }

    // Build MovieItem list combining actual local videos with cached metadata
    val allMovieItems = remember(videos, metadataMap) {
        videos.map { media ->
            val meta = metadataMap[media.uri] ?: MovieMetadataEntity(
                mediaUri = media.uri,
                title = media.title,
                releaseYear = null,
                runtimeMinutes = (media.durationMs / 60000L).toInt().coerceAtLeast(1),
                isMatched = false
            )
            MovieItem(media = media, metadata = meta)
        }
    }

    // Auto-background identification for uncached local files
    var isScanningMetadata by remember { mutableStateOf(false) }
    LaunchedEffect(videos, cachedMetadataList) {
        val uncached = videos.filter { !metadataMap.containsKey(it.uri) }
        if (uncached.isNotEmpty()) {
            isScanningMetadata = true
            withContext(Dispatchers.IO) {
                for (media in uncached) {
                    MovieMetadataEngine.getOrMatchMovie(context, media, tmdbApiKey)
                }
            }
            isScanningMetadata = false
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedGenreFilter by rememberSaveable { mutableStateOf("ALL") }
    var currentSort by rememberSaveable { mutableStateOf(MovieSortOption.TITLE_ASC) }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedMovieForDetails by remember { mutableStateOf<MovieItem?>(null) }
    var movieToMatch by remember { mutableStateOf<MovieItem?>(null) }

    // Dynamic Genre Chips
    val availableGenres = remember(allMovieItems) {
        val set = sortedSetOf<String>()
        allMovieItems.forEach { item ->
            item.genresList.forEach { g -> if (g.isNotBlank()) set.add(g) }
        }
        listOf("ALL", "FAVORITES", "RECENTLY_ADDED", "4K_UHD", "UNMATCHED") + set
    }

    // Filter and Sort Movie List
    val displayedMovies = remember(
        allMovieItems,
        searchQuery,
        selectedGenreFilter,
        currentSort
    ) {
        var filtered = allMovieItems

        // Filter by Query
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            filtered = filtered.filter { item ->
                item.displayTitle.lowercase().contains(q) ||
                    item.metadata.genres.lowercase().contains(q) ||
                    item.metadata.overview.lowercase().contains(q) ||
                    item.releaseYearDisplay.contains(q)
            }
        }

        // Filter by Genre / Category Chip
        filtered = when (selectedGenreFilter) {
            "ALL" -> filtered
            "FAVORITES" -> filtered.filter { it.isFavorite }
            "RECENTLY_ADDED" -> filtered.sortedByDescending { it.media.dateAddedSec }
            "4K_UHD" -> filtered.filter { it.media.is4kUhd }
            "UNMATCHED" -> filtered.filter { !it.isMatched }
            else -> filtered.filter { item ->
                item.genresList.any { it.equals(selectedGenreFilter, ignoreCase = true) }
            }
        }

        // Sort
        when (currentSort) {
            MovieSortOption.TITLE_ASC -> filtered.sortedBy { it.displayTitle.lowercase() }
            MovieSortOption.TITLE_DESC -> filtered.sortedByDescending { it.displayTitle.lowercase() }
            MovieSortOption.YEAR_DESC -> filtered.sortedByDescending { it.metadata.releaseYear ?: 0 }
            MovieSortOption.YEAR_ASC -> filtered.sortedBy { it.metadata.releaseYear ?: 9999 }
            MovieSortOption.RATING_DESC -> filtered.sortedByDescending { it.metadata.rating }
            MovieSortOption.RUNTIME_DESC -> filtered.sortedByDescending { it.metadata.runtimeMinutes }
            MovieSortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.media.dateAddedSec }
        }
    }

    val bottomPadding = LocalDynamicBottomInsets.current + 28.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("movies_library_screen")
    ) {
        // Top Toolbar: Search input, Sort action, Scanning indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search movies, genres, actors...", fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = themePrimary, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = themePrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("movies_search_input")
            )

            // Sort Menu Button
            Box {
                LiquidGlassPillButton(
                    onClick = { showSortMenu = true },
                    accentColor = themeSecondary,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("movies_sort_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort Movies", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sort", style = MaterialTheme.typography.labelMedium)
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false },
                    containerColor = com.example.ui.theme.dynamicGlassDialogContainerColor()
                ) {
                    MovieSortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (currentSort == option) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentSort == option) themePrimary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                currentSort = option
                                showSortMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Horizontal Genre Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableGenres.forEach { chipKey ->
                val isSelected = selectedGenreFilter == chipKey
                val label = when (chipKey) {
                    "ALL" -> "All (${allMovieItems.size})"
                    "FAVORITES" -> "Favorites"
                    "RECENTLY_ADDED" -> "Recent"
                    "4K_UHD" -> "4K UHD"
                    "UNMATCHED" -> "Unmatched"
                    else -> chipKey
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) themePrimary.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) themePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedGenreFilter = chipKey }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("movies_filter_chip_${chipKey.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) themePrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Scanning Status Indicator
        AnimatedVisibility(visible = isScanningMetadata, enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = themePrimary)
                Text(
                    text = "Matching movie metadata & posters...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Poster Grid
        if (displayedMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = bottomPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No movies matching '$searchQuery'" else "No movies in library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Any video files detected in device storage appear here with automatic poster art.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .testTag("movies_grid"),
                contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(displayedMovies, key = { _, item -> item.media.uri }) { index, movie ->
                    MoviePosterCard(
                        movieItem = movie,
                        index = index,
                        onClick = { selectedMovieForDetails = movie },
                        onQuickPlay = {
                            val targetIndex = videos.indexOfFirst { it.uri == movie.media.uri }.coerceAtLeast(0)
                            onPlayVideoQueue(videos, targetIndex, false)
                        }
                    )
                }
            }
        }
    }

    // Movie Details Sheet
    selectedMovieForDetails?.let { movie ->
        val inspection = remember(movie.media.uri) {
            CodecCapabilityInspector.inspectVideoStreams(
                context = context,
                uriString = movie.media.uri,
                containerFormat = movie.media.containerFormat,
                mimeType = movie.media.mimeType,
                title = movie.media.title,
                sizeBytes = movie.media.sizeBytes
            )
        }

        MovieDetailsBottomSheet(
            movieItem = movie,
            inspection = inspection,
            onDismiss = { selectedMovieForDetails = null },
            onPlayMovie = { media, startPos ->
                val targetIndex = videos.indexOfFirst { it.uri == media.uri }.coerceAtLeast(0)
                onPlayVideoQueue(videos, targetIndex, false)
            },
            onToggleFavorite = { item ->
                scope.launch {
                    val newFav = !item.isFavorite
                    movieDao.updateFavorite(item.media.uri, newFav)
                    onToggleMediaFavorite(item.media)
                }
            },
            onOpenMatchDialog = { item ->
                movieToMatch = item
            },
            onRefreshMetadata = { item ->
                scope.launch {
                    val refreshed = MovieMetadataEngine.getOrMatchMovie(context, item.media, tmdbApiKey)
                    selectedMovieForDetails = item.copy(metadata = refreshed)
                }
            }
        )
    }

    // Match / Correct Dialog
    movieToMatch?.let { movie ->
        MovieMatchDialog(
            movieItem = movie,
            tmdbApiKey = tmdbApiKey,
            onDismiss = { movieToMatch = null },
            onSaveMatch = { updated ->
                scope.launch {
                    movieDao.insertOrUpdate(updated)
                    selectedMovieForDetails = selectedMovieForDetails?.copy(metadata = updated)
                    movieToMatch = null
                }
            }
        )
    }
}

@Composable
private fun MoviePosterCard(
    movieItem: MovieItem,
    index: Int,
    onClick: () -> Unit,
    onQuickPlay: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val themePrimary = adaptiveGlassAccent(glassConfig.dynamicAccent, glassConfig.isDarkGlass)
    val themeSecondary = adaptiveGlassAccent(glassConfig.secondaryAccent, glassConfig.isDarkGlass)
    val onPrimaryBadge = GlassContrastResolver.resolveReadableForeground(themePrimary, glassConfig.isDarkGlass, highContrast = true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("movie_card_$index"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Poster Image (Standard 2:3 Cinematic Aspect Ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
        ) {
            if (movieItem.posterUrl.isNotBlank()) {
                AsyncImage(
                    model = movieItem.posterUrl,
                    contentDescription = movieItem.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Procedural cinematic card artwork
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    themeSecondary.copy(alpha = 0.45f),
                                    themePrimary.copy(alpha = 0.25f),
                                    Color.Black
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = themePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = movieItem.displayTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Floating Top Badges (Rating & 4K / HDR)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (movieItem.ratingDisplay.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = movieItem.ratingDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GlassAmberTint,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (movieItem.media.is4kUhd) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(themePrimary.copy(alpha = 0.85f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "4K",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = onPrimaryBadge,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Quick Play Button on Hover / Card Bottom
            IconButton(
                onClick = onQuickPlay,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Movie",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Play Progress Bar if partly watched
            if (movieItem.media.lastPositionMs > 2000L && movieItem.media.durationMs > 0L) {
                val progressFraction = (movieItem.media.lastPositionMs.toFloat() / movieItem.media.durationMs.toFloat()).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    color = themePrimary,
                    trackColor = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter)
                )
            }
        }

        // Title and Year / Runtime
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = movieItem.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (movieItem.releaseYearDisplay.isNotBlank()) {
                    Text(
                        text = movieItem.releaseYearDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (movieItem.runtimeDisplay.isNotBlank()) {
                    Text(
                        text = "• ${movieItem.runtimeDisplay}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
