package com.example.movies.model

import com.example.data.local.MediaEntity
import com.example.movies.data.MovieMetadataEntity
import java.util.Locale

data class MovieItem(
    val media: MediaEntity,
    val metadata: MovieMetadataEntity
) {
    val displayTitle: String
        get() = metadata.userCustomTitle?.takeIf { it.isNotBlank() }
            ?: metadata.title.takeIf { it.isNotBlank() }
            ?: media.title

    val releaseYearDisplay: String
        get() = metadata.releaseYear?.toString() ?: ""

    val runtimeDisplay: String
        get() = if (metadata.runtimeMinutes > 0) {
            val h = metadata.runtimeMinutes / 60
            val m = metadata.runtimeMinutes % 60
            if (h > 0) "${h}h ${m}m" else "${m}m"
        } else {
            val totalSec = media.durationMs / 1000L
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            if (h > 0) "${h}h ${m}m" else "${m}m"
        }

    val ratingDisplay: String
        get() = if (metadata.rating > 0f) String.format(Locale.US, "★ %.1f", metadata.rating) else ""

    val posterUrl: String
        get() = metadata.userCustomPoster?.takeIf { it.isNotBlank() }
            ?: metadata.posterUrl

    val backdropUrl: String
        get() = metadata.userCustomBackdrop?.takeIf { it.isNotBlank() }
            ?: metadata.backdropUrl

    val isMatched: Boolean
        get() = metadata.isMatched

    val isFavorite: Boolean
        get() = metadata.isFavorite || media.isFavorite

    val genresList: List<String>
        get() = if (metadata.genres.isNotBlank()) {
            metadata.genres.split(",").map { it.trim() }.filter { it.isNotBlank() }
        } else {
            emptyList()
        }
}
