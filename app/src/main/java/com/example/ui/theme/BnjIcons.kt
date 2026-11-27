package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewHeadline
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Unified BNJ MEDIA PRO Modern Premium Icon System (Material 3 Rounded + Outlined states).
 * Never uses emoji as functional icons.
 */
object BnjIcons {
    // Primary Navigation (Selected vs Unselected)
    val HomeSelected: ImageVector = Icons.Rounded.Home
    val HomeUnselected: ImageVector = Icons.Outlined.Home

    val MusicSelected: ImageVector = Icons.Rounded.Audiotrack
    val MusicUnselected: ImageVector = Icons.Outlined.Audiotrack

    val VideoSelected: ImageVector = Icons.Rounded.Movie
    val VideoUnselected: ImageVector = Icons.Outlined.Movie

    val FilesSelected: ImageVector = Icons.Rounded.Folder
    val FilesUnselected: ImageVector = Icons.Outlined.Folder

    val SettingsSelected: ImageVector = Icons.Rounded.Settings
    val SettingsUnselected: ImageVector = Icons.Outlined.Settings
    val Menu: ImageVector = Icons.Rounded.Menu
    val CastSelected: ImageVector = Icons.Rounded.CastConnected
    val CastUnselected: ImageVector = Icons.Rounded.Cast

    // Transport & Playback Controls
    val Play: ImageVector = Icons.Rounded.PlayArrow
    val Pause: ImageVector = Icons.Rounded.Pause
    val Previous: ImageVector = Icons.Rounded.SkipPrevious
    val Next: ImageVector = Icons.Rounded.SkipNext
    val SkipNext: ImageVector = Icons.Rounded.SkipNext
    val Rewind: ImageVector = Icons.Rounded.FastRewind
    val Forward: ImageVector = Icons.Rounded.FastForward
    val Shuffle: ImageVector = Icons.Rounded.Shuffle
    val Repeat: ImageVector = Icons.Rounded.Repeat
    val RepeatOne: ImageVector = Icons.Rounded.RepeatOne
    val Check: ImageVector = Icons.Rounded.Check

    // Media & Player Tools
    val Queue: ImageVector = Icons.AutoMirrored.Rounded.QueueMusic
    val Lyrics: ImageVector = Icons.Rounded.Lyrics
    val EqualizerSelected: ImageVector = Icons.Rounded.Equalizer
    val EqualizerUnselected: ImageVector = Icons.Outlined.Equalizer
    val SleepTimerSelected: ImageVector = Icons.Rounded.Timer
    val SleepTimerUnselected: ImageVector = Icons.Outlined.Timer
    val BookmarkSelected: ImageVector = Icons.Rounded.Bookmark
    val BookmarkUnselected: ImageVector = Icons.Outlined.BookmarkBorder
    val Speed: ImageVector = Icons.Rounded.Speed
    val SubtitlesSelected: ImageVector = Icons.Rounded.Subtitles
    val SubtitlesUnselected: ImageVector = Icons.Outlined.Subtitles
    val Volume: ImageVector = Icons.AutoMirrored.Rounded.VolumeUp
    val Brightness: ImageVector = Icons.Rounded.BrightnessHigh
    val AspectRatio: ImageVector = Icons.Rounded.AspectRatio
    val Zoom: ImageVector = Icons.Rounded.ZoomIn
    val Fullscreen: ImageVector = Icons.Rounded.Fullscreen
    val FullscreenExit: ImageVector = Icons.Rounded.FullscreenExit
    val PictureInPicture: ImageVector = Icons.Rounded.PictureInPictureAlt
    val Lock: ImageVector = Icons.Rounded.Lock
    val Unlock: ImageVector = Icons.Rounded.LockOpen

    // Library Browsing & Discovery
    val Search: ImageVector = Icons.Rounded.Search
    val Sort: ImageVector = Icons.AutoMirrored.Rounded.Sort
    val FilterSelected: ImageVector = Icons.Rounded.FilterList
    val FilterUnselected: ImageVector = Icons.Outlined.FilterList
    val History: ImageVector = Icons.Rounded.History
    val FavoriteFilled: ImageVector = Icons.Rounded.Favorite
    val FavoriteOutline: ImageVector = Icons.Outlined.FavoriteBorder
    val AlbumSelected: ImageVector = Icons.Rounded.Album
    val AlbumUnselected: ImageVector = Icons.Outlined.Album
    val ArtistSelected: ImageVector = Icons.Rounded.Person
    val ArtistUnselected: ImageVector = Icons.Outlined.Person
    val MoreOptions: ImageVector = Icons.Rounded.MoreVert
    val Back: ImageVector = Icons.AutoMirrored.Rounded.ArrowBack
    val Close: ImageVector = Icons.Rounded.Close
    val Expand: ImageVector = Icons.Rounded.ExpandLess
    val Collapse: ImageVector = Icons.Rounded.ExpandMore

    // View Modes
    val ViewGrid: ImageVector = Icons.Rounded.GridView
    val ViewList: ImageVector = Icons.AutoMirrored.Rounded.ViewList
    val ViewCompact: ImageVector = Icons.Rounded.ViewHeadline
    val ViewDetailed: ImageVector = Icons.Rounded.ViewAgenda
}
