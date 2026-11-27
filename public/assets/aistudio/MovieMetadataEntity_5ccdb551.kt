package com.example.movies.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movie_metadata",
    indices = [
        Index(value = ["title"]),
        Index(value = ["releaseYear"]),
        Index(value = ["isFavorite"])
    ]
)
data class MovieMetadataEntity(
    @PrimaryKey
    val mediaUri: String,
    val title: String,
    val originalTitle: String = "",
    val releaseYear: Int? = null,
    val releaseDate: String = "",
    val runtimeMinutes: Int = 0,
    val rating: Float = 0f,
    val voteCount: Int = 0,
    val genres: String = "",
    val overview: String = "",
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val tmdbId: Int? = null,
    val isMatched: Boolean = false,
    val isFavorite: Boolean = false,
    val lastScannedMs: Long = System.currentTimeMillis(),
    val userCustomTitle: String? = null,
    val userCustomPoster: String? = null,
    val userCustomBackdrop: String? = null
)
