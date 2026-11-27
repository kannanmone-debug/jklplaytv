package com.example.subtitle

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class SubtitleCueItem(
    val index: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

data class LrcCueItem(
    val index: Int,
    val timeMs: Long,
    val text: String
)

object SubtitleEngine {

    /**
     * Parses SRT, WebVTT, ASS/SSA, or TTML/DFXP subtitle text into sorted [SubtitleCueItem] cues.
     */
    fun parseSubtitles(rawContent: String): List<SubtitleCueItem> {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) return emptyList()

        return when {
            trimmed.contains("<tt", ignoreCase = true) ||
                trimmed.contains("xmlns=\"http://www.w3.org/ns/ttml", ignoreCase = true) ->
                parseTtmlSubtitles(trimmed)
            trimmed.contains("[Events]", ignoreCase = true) ||
                trimmed.contains("Dialogue:", ignoreCase = true) -> parseAssSubtitles(trimmed)
            else -> parseSrtOrVttSubtitles(trimmed)
        }
    }

    /**
     * Parses LRC synchronized lyrics into timestamp-sorted [LrcCueItem] items.
     * Supports multiple timestamps per line (e.g. [00:12.30][01:05.10] Chorus) and [offset:+/-ms].
     */
    fun parseLrcLyrics(rawContent: String): List<LrcCueItem> {
        val normalized = rawContent.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (normalized.isEmpty()) return emptyList()

        val tagRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
        val offsetRegex = Regex("^\\[offset:([+-]?\\d+)]$", RegexOption.IGNORE_CASE)
        var globalOffsetMs = 0L

        val entries = mutableListOf<Pair<Long, String>>()
        for (rawLine in normalized.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue
            val offsetMatch = offsetRegex.matchEntire(trimmed)
            if (offsetMatch != null) {
                globalOffsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                continue
            }

            val matches = tagRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val lyricText = trimmed.replace(tagRegex, "").trim()
                if (lyricText.isNotEmpty()) {
                    for (m in matches) {
                        val min = m.groupValues[1].toLongOrNull() ?: 0L
                        val sec = m.groupValues[2].toLongOrNull() ?: 0L
                        val fracRaw = m.groupValues.getOrNull(3).orEmpty()
                        val millis = when (fracRaw.length) {
                            1 -> (fracRaw.toLongOrNull() ?: 0L) * 100L
                            2 -> (fracRaw.toLongOrNull() ?: 0L) * 10L
                            3 -> fracRaw.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val totalMs = ((min * 60_000L) + (sec * 1000L) + millis + globalOffsetMs)
                            .coerceAtLeast(0L)
                        entries.add(totalMs to lyricText)
                    }
                }
            }
        }

        if (entries.isEmpty()) {
            // Plain text lyrics without timestamps — preserve clean lines with sequential display index
            return normalized.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("[") }
                .mapIndexed { idx, line -> LrcCueItem(idx, -1L, line) }
        }

        return entries.sortedBy { it.first }
            .mapIndexed { idx, (timeMs, text) ->
                LrcCueItem(index = idx, timeMs = timeMs, text = text)
            }
    }

    private class SubtitleCueLookupIndex(
        val cuesRef: List<SubtitleCueItem>,
        val size: Int,
        val isSortedByStart: Boolean,
        val maxPrefixEndMs: LongArray
    )

    @Volatile
    private var lastSubtitleIndex: SubtitleCueLookupIndex? = null

    private fun getOrBuildSubtitleIndex(cues: List<SubtitleCueItem>): SubtitleCueLookupIndex {
        val cached = lastSubtitleIndex
        if (cached != null && cached.cuesRef === cues && cached.size == cues.size) {
            return cached
        }
        val n = cues.size
        val maxPrefixEnd = LongArray(n)
        var sorted = true
        var prevStart = Long.MIN_VALUE
        var runningMaxEnd = Long.MIN_VALUE
        for (i in 0 until n) {
            val cue = cues[i]
            if (cue.startTimeMs < prevStart) {
                sorted = false
            }
            prevStart = cue.startTimeMs
            if (cue.endTimeMs > runningMaxEnd) {
                runningMaxEnd = cue.endTimeMs
            }
            maxPrefixEnd[i] = runningMaxEnd
        }
        return SubtitleCueLookupIndex(cues, n, sorted, maxPrefixEnd).also {
            lastSubtitleIndex = it
        }
    }

    fun findActiveLrcIndex(
        cues: List<LrcCueItem>,
        playbackPositionMs: Long,
        lyricOffsetMs: Long = 0L
    ): Int {
        if (cues.isEmpty()) return -1
        val effectivePos = playbackPositionMs - lyricOffsetMs
        var hasTimestamped = false
        var isSortedTimestamped = true
        var prevTime = Long.MIN_VALUE
        for (i in cues.indices) {
            val t = cues[i].timeMs
            if (t >= 0L) {
                hasTimestamped = true
                if (t < prevTime) {
                    isSortedTimestamped = false
                    break
                }
                prevTime = t
            }
        }
        if (!hasTimestamped) return 0

        if (isSortedTimestamped && cues.first().timeMs >= 0L) {
            var lo = 0
            var hi = cues.lastIndex
            var ans = 0
            while (lo <= hi) {
                val mid = (lo + hi) ushr 1
                if (cues[mid].timeMs <= effectivePos) {
                    ans = mid
                    lo = mid + 1
                } else {
                    hi = mid - 1
                }
            }
            return ans
        }

        var activeIdx = 0
        for (i in cues.indices) {
            val t = cues[i].timeMs
            if (t >= 0L && t <= effectivePos) {
                activeIdx = i
            }
        }
        return activeIdx
    }

    /**
     * Finds the active subtitle text at [playbackPositionMs] after applying [subtitleDelayMs].
     * Note: A positive [subtitleDelayMs] shifts subtitles to appear later, so we look up
     * `playbackPositionMs - subtitleDelayMs` against the original cue timestamps.
     */
    fun findActiveSubtitleText(
        cues: List<SubtitleCueItem>,
        playbackPositionMs: Long,
        subtitleDelayMs: Long
    ): String {
        if (cues.isEmpty()) return ""
        val effectiveTimeMs = playbackPositionMs - subtitleDelayMs
        val index = getOrBuildSubtitleIndex(cues)

        if (!index.isSortedByStart) {
            var firstMatch: String? = null
            var multi: StringBuilder? = null
            for (i in cues.indices) {
                val cue = cues[i]
                if (effectiveTimeMs in cue.startTimeMs..cue.endTimeMs) {
                    if (firstMatch == null) {
                        firstMatch = cue.text
                    } else {
                        val sb = multi ?: StringBuilder(firstMatch).also { multi = it }
                        sb.append('\n').append(cue.text)
                    }
                }
            }
            return multi?.toString() ?: firstMatch.orEmpty()
        }

        // Binary search rightmost cue with startTimeMs <= effectiveTimeMs
        var lo = 0
        var hi = cues.lastIndex
        var upperIdx = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].startTimeMs <= effectiveTimeMs) {
                upperIdx = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }
        if (upperIdx < 0) return ""

        val maxPrefixEnd = index.maxPrefixEndMs
        var lastMatch: String? = null
        var firstMatchIdx = -1
        var matchCount = 0
        var i = upperIdx
        while (i >= 0) {
            if (maxPrefixEnd[i] < effectiveTimeMs) break
            val cue = cues[i]
            if (effectiveTimeMs <= cue.endTimeMs) {
                if (matchCount == 0) {
                    lastMatch = cue.text
                }
                firstMatchIdx = i
                matchCount++
            }
            i--
        }

        return when (matchCount) {
            0 -> ""
            1 -> lastMatch.orEmpty()
            else -> {
                val sb = StringBuilder()
                for (j in firstMatchIdx..upperIdx) {
                    val cue = cues[j]
                    if (effectiveTimeMs <= cue.endTimeMs) {
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(cue.text)
                    }
                }
                sb.toString()
            }
        }
    }

    fun loadExternalSubtitleFromUri(context: Context, uri: Uri): List<SubtitleCueItem> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val content = BufferedReader(InputStreamReader(stream)).readText()
                parseSubtitles(content)
            } ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }

    fun loadRawTextFromUri(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            } ?: ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun findSidecarSubtitleForFile(mediaUriString: String): List<SubtitleCueItem> {
        if (!mediaUriString.startsWith("file://")) return emptyList()
        val path = Uri.parse(mediaUriString).path ?: return emptyList()
        val base = path.substringBeforeLast('.')
        for (ext in listOf("srt", "vtt", "ass", "ssa", "ttml")) {
            val candidate = File("$base.$ext")
            if (candidate.exists() && candidate.canRead() && candidate.length() < 1_048_576L) {
                val parsed = parseSubtitles(candidate.readText())
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return emptyList()
    }

    private fun parseSrtOrVttSubtitles(content: String): List<SubtitleCueItem> {
        val cues = mutableListOf<SubtitleCueItem>()
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        val blocks = normalized.split(Regex("\n\\s*\n"))
        var cueCounter = 1

        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            if (lines.first().startsWith("WEBVTT", ignoreCase = true)) continue

            val timingLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingLineIndex == -1) continue

            val timingLine = lines[timingLineIndex]
            val parts = timingLine.split("-->")
            if (parts.size < 2) continue

            val startMs = parseTimestampToMs(parts[0].trim())
            val endMs = parseTimestampToMs(parts[1].trim().substringBefore(" "))
            if (startMs < 0 || endMs <= startMs) continue

            val textLines = lines.drop(timingLineIndex + 1)
                .map { cleanFormattingTags(it) }
                .filter { it.isNotBlank() }

            if (textLines.isNotEmpty()) {
                cues.add(
                    SubtitleCueItem(
                        index = cueCounter++,
                        startTimeMs = startMs,
                        endTimeMs = endMs,
                        text = textLines.joinToString("\n")
                    )
                )
            }
        }
        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseAssSubtitles(content: String): List<SubtitleCueItem> {
        val cues = mutableListOf<SubtitleCueItem>()
        var counter = 1
        for (line in content.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("Dialogue:", ignoreCase = true)) {
                val payload = trimmed.substringAfter(":")
                val columns = payload.split(",", limit = 10)
                if (columns.size >= 10) {
                    val startMs = parseTimestampToMs(columns[1].trim())
                    val endMs = parseTimestampToMs(columns[2].trim())
                    val rawText = columns[9]
                        .replace("\\N", "\n")
                        .replace("\\n", "\n")
                        .replace(Regex("\\{[^}]*\\}"), "")
                        .trim()
                    if (startMs >= 0 && endMs > startMs && rawText.isNotEmpty()) {
                        cues.add(
                            SubtitleCueItem(
                                index = counter++,
                                startTimeMs = startMs,
                                endTimeMs = endMs,
                                text = rawText
                            )
                        )
                    }
                }
            }
        }
        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseTtmlSubtitles(content: String): List<SubtitleCueItem> {
        val cues = mutableListOf<SubtitleCueItem>()
        val pRegex = Regex(
            "<p\\b([^>]*)>(.*?)</p>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val beginRegex = Regex("begin\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val endRegex = Regex("end\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        val durRegex = Regex("dur\\s*=\\s*[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)

        var counter = 1
        for (match in pRegex.findAll(content)) {
            val attrs = match.groupValues[1]
            val bodyRaw = match.groupValues[2]
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            val text = cleanFormattingTags(bodyRaw)
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .trim()
            if (text.isEmpty()) continue

            val beginRaw = beginRegex.find(attrs)?.groupValues?.getOrNull(1) ?: continue
            val startMs = parseTtmlTime(beginRaw)
            val endMs = endRegex.find(attrs)?.groupValues?.getOrNull(1)?.let { parseTtmlTime(it) }
                ?: durRegex.find(attrs)?.groupValues?.getOrNull(1)?.let { dur ->
                    val d = parseTtmlTime(dur)
                    if (d > 0) startMs + d else -1L
                }
                ?: -1L

            if (startMs >= 0L && endMs > startMs) {
                cues.add(
                    SubtitleCueItem(
                        index = counter++,
                        startTimeMs = startMs,
                        endTimeMs = endMs,
                        text = text
                    )
                )
            }
        }
        return cues.sortedBy { it.startTimeMs }
    }

    private fun parseTtmlTime(raw: String): Long {
        val t = raw.trim()
        return if (t.endsWith("ms", ignoreCase = true)) {
            t.dropLast(2).toDoubleOrNull()?.toLong() ?: -1L
        } else if (t.endsWith("s", ignoreCase = true)) {
            ((t.dropLast(1).toDoubleOrNull() ?: -1.0) * 1000.0).toLong()
        } else {
            parseTimestampToMs(t)
        }
    }

    fun parseTimestampToMs(rawTimestamp: String): Long {
        val cleaned = rawTimestamp.trim().replace(',', '.')
        val parts = cleaned.split(":")
        return try {
            when (parts.size) {
                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val secParts = parts[2].split(".")
                    val seconds = secParts[0].toLong()
                    val millis = if (secParts.size > 1) {
                        secParts[1].padEnd(3, '0').take(3).toLong()
                    } else 0L
                    (hours * 3600_000L) + (minutes * 60_000L) + (seconds * 1000L) + millis
                }
                2 -> {
                    val minutes = parts[0].toLong()
                    val secParts = parts[1].split(".")
                    val seconds = secParts[0].toLong()
                    val millis = if (secParts.size > 1) {
                        secParts[1].padEnd(3, '0').take(3).toLong()
                    } else 0L
                    (minutes * 60_000L) + (seconds * 1000L) + millis
                }
                else -> -1L
            }
        } catch (_: Throwable) {
            -1L
        }
    }

    private fun cleanFormattingTags(input: String): String {
        return input
            .replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\{[^}]*\\}"), "")
            .trim()
    }
}
