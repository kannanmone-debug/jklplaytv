package com.example.movies.engine

data class ParsedMovieTitle(
    val cleanTitle: String,
    val year: Int?
)

object MovieTitleParser {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "avi", "mov", "webm", "ts", "flv", "wmv", "m4v", "3gp", "vob"
    )

    private val KNOWN_NUMBERED_TITLES = setOf(
        "blade runner 2049",
        "1917",
        "2001 a space odyssey",
        "2012",
        "district 9",
        "super 8",
        "apollo 13",
        "se7en",
        "ocean's eleven",
        "12 angry men",
        "12 monkeys",
        "300",
        "fahrenheit 451",
        "thx 1138"
    )

    private val NOISE_PATTERNS = listOf(
        // Resolutions
        Regex("""(?i)\b(2160p|1080p|1080i|720p|576p|480p|4k|uhd|fhd|hd|sd)\b"""),
        // Sources & Rips
        Regex("""(?i)\b(bluray|blu-ray|bdrip|brrip|webrip|web-dl|webdl|web|hdtv|dvdrip|dvd|remux|vhs)\b"""),
        // Codecs
        Regex("""(?i)\b(x264|x265|h264|h265|hevc|avc|av1|xvid|divx|10bit|8bit)\b"""),
        // Color & Dynamic Range
        Regex("""(?i)\b(hdr10\+|hdr10plus|hdr10|hdr|dolby[\s._-]*vision|dovi|dv|sdr)\b"""),
        // Audio Formats
        Regex("""(?i)\b(truehd|atmos|dts-hd|dts|ddp5\.1|dd5\.1|ac3|eac3|aac|flac|mp3|opus|5\.1|7\.1|stereo)\b"""),
        // Editions
        Regex("""(?i)\b(extended[\s._-]*cut|extended|unrated|directors[\s._-]*cut|director's[\s._-]*cut|imax|remastered|proper|repack|criterion|theatrical)\b"""),
        // Common Release Groups
        Regex("""(?i)\b(yify|rarbg|eztv|sparks|fgt|evo|geckos|rovers|amiable|cmrg|galaxyrg|tgx|psa|qxr|utr)\b"""),
        // Brackets / Parentheses containing technical noise
        Regex("""\[.*?\]"""),
        Regex("""(?i)\((2160p|1080p|720p|4k|bluray|webrip|hevc|x264).*?\)""")
    )

    private val YEAR_REGEX = Regex("""\b(19\d{2}|20\d{2})\b""")

    fun parse(rawTitle: String, folderName: String = ""): ParsedMovieTitle {
        var working = rawTitle.trim()

        // Strip file extension if present
        val ext = working.substringAfterLast('.', "").lowercase()
        if (ext in VIDEO_EXTENSIONS) {
            working = working.substringBeforeLast('.')
        }

        // If filename is generic ("movie", "video", "film"), use parent folder name
        val lower = working.lowercase()
        if (lower in setOf("movie", "video", "film", "feature", "track", "sample") && folderName.isNotBlank()) {
            working = folderName
        }

        // Replace separators (dots, underscores, pluses) with spaces
        working = working.replace('.', ' ')
            .replace('_', ' ')
            .replace('+', ' ')
            .replace('-', ' ')

        // Check if full string contains a known numbered title like "Blade Runner 2049"
        val lowerWorking = working.lowercase().trim()
        val specialTitle = KNOWN_NUMBERED_TITLES.firstOrNull { lowerWorking.startsWith(it) }

        var detectedYear: Int? = null

        if (specialTitle != null) {
            // Check if there is an explicit separate year after the title, e.g. "Blade Runner 2049 2017"
            val afterSpecial = working.substring(specialTitle.length)
            val yearMatch = YEAR_REGEX.find(afterSpecial)
            detectedYear = yearMatch?.value?.toIntOrNull()
            working = specialTitle
        } else {
            // Find year match
            val yearMatches = YEAR_REGEX.findAll(working).toList()
            if (yearMatches.isNotEmpty()) {
                val lastYearMatch = yearMatches.last()
                val candidateYear = lastYearMatch.value.toIntOrNull()
                // Take everything before the year as the title
                val prefix = working.substring(0, lastYearMatch.range.first).trim()
                if (prefix.length >= 2) {
                    detectedYear = candidateYear
                    working = prefix
                }
            }
        }

        // Strip remaining technical noise patterns
        for (pattern in NOISE_PATTERNS) {
            working = pattern.replace(working, " ")
        }

        // Clean parentheses and brackets remaining
        working = working.replace("(", " ")
            .replace(")", " ")
            .replace("[", " ")
            .replace("]", " ")
            .replace("{", " ")
            .replace("}", " ")

        // Collapse multiple spaces
        working = working.replace(Regex("""\s+"""), " ").trim()

        // Capitalize words nicely if working is not empty
        val formattedTitle = if (working.isBlank()) {
            rawTitle.substringBeforeLast('.').replace('.', ' ').replace('_', ' ').trim()
        } else {
            working.split(" ").joinToString(" ") { word ->
                if (word.length <= 3 && word.all { it.isUpperCase() }) word
                else word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        return ParsedMovieTitle(
            cleanTitle = formattedTitle,
            year = detectedYear
        )
    }
}
