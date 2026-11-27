package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PlaylistSummaryRow(
    val playlistId: Long,
    val trackCount: Int,
    val leadMediaUri: String?
)

@Dao
interface BnjMediaDao {

    @Query("SELECT * FROM media_items ORDER BY title ASC")
    fun observeAllMedia(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 ORDER BY title ASC")
    fun observeAllSongs(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 1 ORDER BY title ASC")
    fun observeAllVideos(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC LIMIT 20")
    fun observeRecentlyPlayed(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 AND lastPositionMs > 5000 ORDER BY lastPlayedTimestamp DESC LIMIT 15")
    fun observeContinueListening(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 1 AND lastPositionMs > 5000 ORDER BY lastPlayedTimestamp DESC LIMIT 15")
    fun observeContinueWatching(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 ORDER BY dateAddedSec DESC LIMIT 20")
    fun observeRecentlyAddedSongs(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 1 ORDER BY dateAddedSec DESC LIMIT 20")
    fun observeRecentlyAddedVideos(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 0 AND isFavorite = 1 ORDER BY title ASC")
    fun observeFavoriteSongs(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE isVideo = 1 AND isFavorite = 1 ORDER BY title ASC")
    fun observeFavoriteVideos(): Flow<List<MediaEntity>>

    @Query(
        """
        SELECT * FROM media_items
        WHERE isVideo = 0 AND playCount > 0
        ORDER BY playCount DESC, lastPlayedTimestamp DESC, title ASC
        LIMIT 50
        """
    )
    fun observeMostPlayedSongs(): Flow<List<MediaEntity>>

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getMediaByUri(uri: String): MediaEntity?

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaSnapshot(): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItems(items: List<MediaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaEntity)

    @Update
    suspend fun updateMediaItem(item: MediaEntity)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE uri = :uri")
    suspend fun setFavorite(uri: String, isFavorite: Boolean)

    @Query(
        """
        UPDATE media_items 
        SET lastPositionMs = :positionMs, 
            lastPlayedTimestamp = :timestamp
        WHERE uri = :uri
        """
    )
    suspend fun updatePlaybackProgress(uri: String, positionMs: Long, timestamp: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE media_items 
        SET playCount = playCount + 1,
            lastPlayedTimestamp = :timestamp
        WHERE uri = :uri
        """
    )
    suspend fun incrementPlayCountOnce(uri: String, timestamp: Long = System.currentTimeMillis())

    @Query(
        """
        UPDATE media_items
        SET title = :title,
            artist = :artist,
            album = :album,
            genre = :genre
        WHERE uri = :uri
        """
    )
    suspend fun updateMediaMetadataTags(
        uri: String,
        title: String,
        artist: String,
        album: String,
        genre: String
    )

    @Query("UPDATE media_items SET localLyrics = :lyrics WHERE uri = :uri")
    suspend fun updateMediaLocalLyrics(uri: String, lyrics: String)

    @Query(
        """
        UPDATE media_items 
        SET subtitleDelayMs = :subtitleDelayMs, 
            audioDelayMs = :audioDelayMs, 
            preferredSpeed = :speed 
        WHERE uri = :uri
        """
    )
    suspend fun updateSyncAndSpeed(uri: String, subtitleDelayMs: Long, audioDelayMs: Long, speed: Float)

    @Query("DELETE FROM media_items WHERE uri = :uri")
    suspend fun deleteMediaByUri(uri: String)

    @Query("DELETE FROM media_items WHERE uri IN (:uris)")
    suspend fun deleteMediaByUris(uris: List<String>)

    @Query("DELETE FROM media_items WHERE folderPath = :folderPath")
    suspend fun deleteMediaInFolder(folderPath: String)

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistTrack(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND mediaUri = :mediaUri")
    suspend fun removeTrackFromPlaylist(playlistId: Long, mediaUri: String)

    @Query("DELETE FROM playlist_tracks WHERE mediaUri IN (:uris)")
    suspend fun removeTracksFromAllPlaylists(uris: List<String>)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY positionOrder ASC, addedAt ASC")
    suspend fun getPlaylistCrossRefs(playlistId: Long): List<PlaylistTrackCrossRef>

    @Query(
        """
        SELECT m.* FROM media_items m
        INNER JOIN playlist_tracks pt ON m.uri = pt.mediaUri
        WHERE pt.playlistId = :playlistId
        ORDER BY pt.positionOrder ASC, pt.addedAt ASC
        """
    )
    fun observePlaylistItems(playlistId: Long): Flow<List<MediaEntity>>

    @Query(
        """
        SELECT pt.playlistId AS playlistId,
               COUNT(*) AS trackCount,
               (SELECT pt2.mediaUri FROM playlist_tracks pt2
                WHERE pt2.playlistId = pt.playlistId
                ORDER BY pt2.positionOrder ASC, pt2.addedAt ASC LIMIT 1) AS leadMediaUri
        FROM playlist_tracks pt
        GROUP BY pt.playlistId
        """
    )
    fun observePlaylistSummaries(): Flow<List<PlaylistSummaryRow>>

    // Video Bookmarks
    @Query("SELECT * FROM video_bookmarks WHERE mediaUri = :mediaUri ORDER BY timestampMs ASC")
    fun observeBookmarksForVideo(mediaUri: String): Flow<List<VideoBookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: VideoBookmarkEntity): Long

    @Query("DELETE FROM video_bookmarks WHERE id = :bookmarkId")
    suspend fun deleteBookmark(bookmarkId: Long)

    // Private Media Vault
    @Query("SELECT * FROM vault_media ORDER BY addedTimestamp DESC")
    fun observeVaultMedia(): Flow<List<VaultMediaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultMedia(vaultItem: VaultMediaEntity): Long

    @Query("DELETE FROM vault_media WHERE id = :id")
    suspend fun deleteVaultMediaById(id: Long)

    // Excluded Folders & Shortcuts
    @Query("SELECT * FROM excluded_folders ORDER BY addedTimestamp DESC")
    fun observeExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT folderPath FROM excluded_folders")
    suspend fun getExcludedFolderPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addExcludedFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :folderPath")
    suspend fun removeExcludedFolder(folderPath: String)

    @Query("SELECT * FROM folder_shortcuts ORDER BY addedTimestamp DESC")
    fun observeFolderShortcuts(): Flow<List<FolderShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFolderShortcut(shortcut: FolderShortcutEntity)

    @Query("DELETE FROM folder_shortcuts WHERE folderPath = :folderPath")
    suspend fun removeFolderShortcut(folderPath: String)

    // Equalizer Presets
    @Query("SELECT * FROM equalizer_presets ORDER BY isBuiltIn DESC, name ASC")
    fun observeEqualizerPresets(): Flow<List<EqualizerPresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEqualizerPreset(preset: EqualizerPresetEntity): Long

    @Query("DELETE FROM equalizer_presets WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustomEqualizerPreset(id: Long)

    @Query("UPDATE equalizer_presets SET name = :newName WHERE id = :id AND isBuiltIn = 0")
    suspend fun renameCustomEqualizerPreset(id: Long, newName: String)
}
