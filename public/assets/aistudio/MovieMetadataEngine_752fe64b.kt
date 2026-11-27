package com.example.movies.engine

import android.content.Context
import com.example.data.local.MediaEntity
import com.example.movies.data.BnjMovieDatabase
import com.example.movies.data.MovieMetadataEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object MovieMetadataEngine {

    private const val TMDB_IMAGE_BASE_POSTER = "https://image.tmdb.org/t/p/w500"
    private const val TMDB_IMAGE_BASE_BACKDROP = "https://image.tmdb.org/t/p/w1280"

    private val GENRE_MAP = mapOf(
        28 to "Action",
        12 to "Adventure",
        16 to "Animation",
        35 to "Comedy",
        80 to "Crime",
        99 to "Documentary",
        18 to "Drama",
        10751 to "Family",
        14 to "Fantasy",
        36 to "History",
        27 to "Horror",
        10402 to "Music",
        9648 to "Mystery",
        10749 to "Romance",
        878 to "Sci-Fi",
        10770 to "TV Movie",
        53 to "Thriller",
        10752 to "War",
        37 to "Western"
    )

    // Curated high-res catalog for offline / instant identification
    private val OFFLINE_CATALOG: List<MovieMetadataEntity> = listOf(
        MovieMetadataEntity(
            mediaUri = "",
            title = "Blade Runner 2049",
            originalTitle = "Blade Runner 2049",
            releaseYear = 2017,
            releaseDate = "2017-10-06",
            runtimeMinutes = 164,
            rating = 8.1f,
            voteCount = 13500,
            genres = "Sci-Fi, Mystery, Drama",
            overview = "Thirty years after the events of the first film, a new blade runner, LAPD Officer K, unearths a long-buried secret that has the potential to plunge what's left of society into chaos.",
            posterUrl = "https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/ilRyASDvt7vDU90VzEZrcZ42q72.jpg",
            tmdbId = 335984,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "Inception",
            originalTitle = "Inception",
            releaseYear = 2010,
            releaseDate = "2010-07-16",
            runtimeMinutes = 148,
            rating = 8.4f,
            voteCount = 36000,
            genres = "Action, Sci-Fi, Adventure",
            overview = "Cobb, a skilled thief who commits corporate espionage by infiltrating the subconscious of his targets, is offered a chance to regain his old life as payment for a task considered to be impossible: inception.",
            posterUrl = "https://image.tmdb.org/t/p/w500/edv5CZvWj09upOsy2Y6IwDhK8bt.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/8ZTVqvKDQ8emSGUEMjsS4yHAwrp.jpg",
            tmdbId = 27205,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "Interstellar",
            originalTitle = "Interstellar",
            releaseYear = 2014,
            releaseDate = "2014-11-05",
            runtimeMinutes = 169,
            rating = 8.4f,
            voteCount = 34500,
            genres = "Adventure, Drama, Sci-Fi",
            overview = "The adventures of a group of explorers who make use of a newly discovered wormhole to surpass the limitations on human space travel and conquer the vast distances involved in an interstellar voyage.",
            posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
            tmdbId = 157336,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "The Dark Knight",
            originalTitle = "The Dark Knight",
            releaseYear = 2008,
            releaseDate = "2008-07-16",
            runtimeMinutes = 152,
            rating = 8.5f,
            voteCount = 32000,
            genres = "Drama, Action, Crime, Thriller",
            overview = "Batman raises the stakes in his war on crime. With the help of Lt. Jim Gordon and District Attorney Harvey Dent, Batman sets out to dismantle the remaining criminal organizations that plague the streets.",
            posterUrl = "https://image.tmdb.org/t/p/w500/qJ2tW6WMUDux911r6m7haRef0WH.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/dqK9Hag1054tghRQSqLSfrkvQnA.jpg",
            tmdbId = 155,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "Dune",
            originalTitle = "Dune",
            releaseYear = 2021,
            releaseDate = "2021-09-15",
            runtimeMinutes = 155,
            rating = 7.8f,
            voteCount = 11000,
            genres = "Sci-Fi, Adventure",
            overview = "Paul Atreides, a brilliant and gifted young man born into a great destiny beyond his understanding, must travel to the most dangerous planet in the universe to ensure the future of his family and his people.",
            posterUrl = "https://image.tmdb.org/t/p/w500/d5NXSklXo0qyIYkgV94XAgMIckC.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/jYEW5xZkZk2WTrdbMGAPFuBqbDc.jpg",
            tmdbId = 438631,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "Dune: Part Two",
            originalTitle = "Dune: Part Two",
            releaseYear = 2024,
            releaseDate = "2024-02-27",
            runtimeMinutes = 166,
            rating = 8.2f,
            voteCount = 5200,
            genres = "Sci-Fi, Adventure",
            overview = "Follow the mythic journey of Paul Atreides as he unites with Chani and the Fremen while on a path of revenge against the conspirators who destroyed his family.",
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8BRK7PfcJv9JCnx7s520QIq.jpg",
            tmdbId = 693134,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "Oppenheimer",
            originalTitle = "Oppenheimer",
            releaseYear = 2023,
            releaseDate = "2023-07-19",
            runtimeMinutes = 181,
            rating = 8.1f,
            voteCount = 8900,
            genres = "Drama, History",
            overview = "The story of J. Robert Oppenheimer's role in the development of the atomic bomb during World War II.",
            posterUrl = "https://image.tmdb.org/t/p/w500/8Gxv8gSFCU0XGDykEGv7zR1n2ua.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/nb3xI8XI3w4pMVZ38VijbsyBqP4.jpg",
            tmdbId = 872585,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "Spider-Man: Across the Spider-Verse",
            originalTitle = "Spider-Man: Across the Spider-Verse",
            releaseYear = 2023,
            releaseDate = "2023-05-31",
            runtimeMinutes = 140,
            rating = 8.4f,
            voteCount = 6700,
            genres = "Animation, Action, Adventure, Sci-Fi",
            overview = "After reuniting with Gwen Stacy, Brooklyn's full-time, friendly neighborhood Spider-Man is catapulted across the Multiverse, where he encounters the Spider Society, a team of Spider-People charged with protecting the Multiverse's very existence.",
            posterUrl = "https://image.tmdb.org/t/p/w500/8Vt6mWEReuy4Of61Lnj5Xj704m8.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/4HodYYKEIsGOdinkGi2Ucz6X9i0.jpg",
            tmdbId = 569094,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "The Matrix",
            originalTitle = "The Matrix",
            releaseYear = 1999,
            releaseDate = "1999-03-30",
            runtimeMinutes = 136,
            rating = 8.2f,
            voteCount = 25000,
            genres = "Action, Sci-Fi",
            overview = "Set in the 22nd century, The Matrix tells the story of a computer hacker who joins a group of underground insurgents fighting the vast and powerful computers who now rule the earth.",
            posterUrl = "https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/7u3fhRwF6n4j2r1fd76s98j1500.jpg",
            tmdbId = 603,
            isMatched = true
        ),
        MovieMetadataEntity(
            mediaUri = "",
            title = "Gladiator",
            originalTitle = "Gladiator",
            releaseYear = 2000,
            releaseDate = "2000-05-04",
            runtimeMinutes = 155,
            rating = 8.2f,
            voteCount = 18000,
            genres = "Action, Drama, Adventure",
            overview = "In the year 180, the death of Emperor Marcus Aurelius throws the Roman Empire into chaos. Maximums, one of the Roman army's most capable and trusted generals, is stripped of his rank and forced into slavery as a gladiator.",
            posterUrl = "https://image.tmdb.org/t/p/w500/ty8TGRuvJLPUmAR1H1nRIsgwvim.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/hZkgoQYus5vegHoetLkCJzb17zJ.jpg",
            tmdbId = 98,
            isMatched = true
        )
    )

    suspend fun getOrMatchMovie(
        context: Context,
        media: MediaEntity,
        userApiKey: String? = null
    ): MovieMetadataEntity = withContext(Dispatchers.IO) {
        val database = BnjMovieDatabase.getInstance(context)
        val dao = database.movieMetadataDao()

        // 1. Check local cache in database
        val cached = dao.getMovie(media.uri)
        if (cached != null) {
            return@withContext cached
        }

        // 2. Parse filename & folder
        val parsed = MovieTitleParser.parse(media.title, media.folderName)

        // 3. Check curated offline catalog (fuzzy match on cleanTitle and year)
        val offlineMatch = matchOfflineCatalog(parsed.cleanTitle, parsed.year)
        if (offlineMatch != null) {
            val entity = offlineMatch.copy(
                mediaUri = media.uri,
                runtimeMinutes = if (offlineMatch.runtimeMinutes > 0) offlineMatch.runtimeMinutes
                else (media.durationMs / 60_000L).toInt().coerceAtLeast(1)
            )
            dao.insertOrUpdate(entity)
            return@withContext entity
        }

        // 4. Online TMDB Query if API key available
        val apiKey = userApiKey?.takeIf { it.isNotBlank() }
        if (apiKey != null) {
            val onlineCandidates = searchTmdb(parsed.cleanTitle, parsed.year, apiKey)
            val firstCandidate = onlineCandidates.firstOrNull()
            if (firstCandidate != null) {
                val entity = firstCandidate.copy(
                    mediaUri = media.uri,
                    runtimeMinutes = if (firstCandidate.runtimeMinutes > 0) firstCandidate.runtimeMinutes
                    else (media.durationMs / 60_000L).toInt().coerceAtLeast(1)
                )
                dao.insertOrUpdate(entity)
                return@withContext entity
            }
        }

        // 5. Intelligent Fallback (cleanly structured with parsed title and year)
        val fallback = MovieMetadataEntity(
            mediaUri = media.uri,
            title = parsed.cleanTitle,
            releaseYear = parsed.year,
            runtimeMinutes = (media.durationMs / 60_000L).toInt().coerceAtLeast(1),
            rating = 0f,
            genres = "General",
            overview = "Local video file (${media.containerFormat.uppercase()}). Tap 'Match Movie' to fetch poster, rating, and details.",
            posterUrl = "",
            backdropUrl = "",
            isMatched = false,
            lastScannedMs = System.currentTimeMillis()
        )
        dao.insertOrUpdate(fallback)
        fallback
    }

    fun matchOfflineCatalog(queryTitle: String, year: Int?): MovieMetadataEntity? {
        val normQuery = queryTitle.lowercase().replace(":", "").replace("'", "").trim()
        if (normQuery.isBlank()) return null

        // Exact or high similarity match
        return OFFLINE_CATALOG.firstOrNull { item ->
            val normItem = item.title.lowercase().replace(":", "").replace("'", "").trim()
            if (year != null && item.releaseYear != null && kotlin.math.abs(item.releaseYear - year) > 1) {
                false
            } else {
                normQuery == normItem || normQuery.contains(normItem) || normItem.contains(normQuery)
            }
        }
    }

    suspend fun searchTmdb(
        query: String,
        year: Int? = null,
        apiKey: String
    ): List<MovieMetadataEntity> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank() || apiKey.isBlank()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val yearParam = if (year != null && year in 1900..2100) "&year=$year" else ""
            val urlString = "https://api.themoviedb.org/3/search/movie?api_key=$apiKey&query=$encodedQuery$yearParam&include_adult=false"

            val url = URL(urlString)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 7000
                setRequestProperty("Accept", "application/json")
            }

            if (connection.responseCode != 200) {
                return@withContext emptyList()
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.use { it.readText() }
            val jsonObject = JSONObject(response)
            val resultsArray = jsonObject.optJSONArray("results") ?: return@withContext emptyList()

            val candidates = mutableListOf<MovieMetadataEntity>()
            for (i in 0 until minOf(resultsArray.length(), 10)) {
                val item = resultsArray.getJSONObject(i)
                val title = item.optString("title", cleanQuery)
                val originalTitle = item.optString("original_title", "")
                val releaseDate = item.optString("release_date", "")
                val releaseYear = releaseDate.take(4).toIntOrNull()
                val rating = item.optDouble("vote_average", 0.0).toFloat()
                val voteCount = item.optInt("vote_count", 0)
                val overview = item.optString("overview", "")
                val posterPath = item.optString("poster_path", "")
                val backdropPath = item.optString("backdrop_path", "")
                val tmdbId = item.optInt("id", 0)

                val genreIds = item.optJSONArray("genre_ids")
                val genreNames = mutableListOf<String>()
                if (genreIds != null) {
                    for (g in 0 until genreIds.length()) {
                        val gid = genreIds.optInt(g)
                        GENRE_MAP[gid]?.let { genreNames.add(it) }
                    }
                }

                candidates.add(
                    MovieMetadataEntity(
                        mediaUri = "",
                        title = title,
                        originalTitle = originalTitle,
                        releaseYear = releaseYear,
                        releaseDate = releaseDate,
                        rating = rating,
                        voteCount = voteCount,
                        genres = genreNames.joinToString(", ").ifBlank { "Feature Film" },
                        overview = overview,
                        posterUrl = if (posterPath.isNotBlank() && posterPath != "null") "$TMDB_IMAGE_BASE_POSTER$posterPath" else "",
                        backdropUrl = if (backdropPath.isNotBlank() && backdropPath != "null") "$TMDB_IMAGE_BASE_BACKDROP$backdropPath" else "",
                        tmdbId = if (tmdbId > 0) tmdbId else null,
                        isMatched = true
                    )
                )
            }
            candidates
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
