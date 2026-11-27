package com.example.movies

import com.example.movies.engine.MovieTitleParser
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieTitleParserTest {

    @Test
    fun `parses standard release filename with year, resolution, and codecs`() {
        val result = MovieTitleParser.parse("Inception.2010.1080p.BluRay.x264.mkv")
        assertEquals("Inception", result.cleanTitle)
        assertEquals(2010, result.year)
    }

    @Test
    fun `parses title with dots and underscores without year`() {
        val result = MovieTitleParser.parse("The_Dark_Knight_FHD.mp4")
        assertEquals("The Dark Knight", result.cleanTitle)
        assertEquals(null, result.year)
    }

    @Test
    fun `parses 4K HDR release with brackets and release group`() {
        val result = MovieTitleParser.parse("Oppenheimer (2023) [WEBRip] [2160p] [HDR10+]-YTS.mp4")
        assertEquals("Oppenheimer", result.cleanTitle)
        assertEquals(2023, result.year)
    }

    @Test
    fun `preserves known numbered titles like Blade Runner 2049`() {
        val result = MovieTitleParser.parse("Blade.Runner.2049.2017.1080p.Remux.mkv")
        assertEquals("Blade Runner 2049", result.cleanTitle)
        assertEquals(2017, result.year)
    }

    @Test
    fun `falls back to folder name if filename is generic`() {
        val result = MovieTitleParser.parse("movie.mkv", folderName = "Interstellar 2014")
        assertEquals("Interstellar", result.cleanTitle)
        assertEquals(2014, result.year)
    }
}
