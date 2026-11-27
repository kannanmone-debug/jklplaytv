package com.example.movies.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieMetadataDao {

    @Query("SELECT * FROM movie_metadata ORDER BY title ASC")
    fun observeAll(): Flow<List<MovieMetadataEntity>>

    @Query("SELECT * FROM movie_metadata WHERE mediaUri = :uri LIMIT 1")
    fun observeMovie(uri: String): Flow<MovieMetadataEntity?>

    @Query("SELECT * FROM movie_metadata WHERE mediaUri = :uri LIMIT 1")
    suspend fun getMovie(uri: String): MovieMetadataEntity?

    @Query("SELECT * FROM movie_metadata")
    suspend fun getAllMovies(): List<MovieMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: MovieMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<MovieMetadataEntity>)

    @Query("DELETE FROM movie_metadata WHERE mediaUri = :uri")
    suspend fun delete(uri: String)

    @Query("UPDATE movie_metadata SET isFavorite = :isFav WHERE mediaUri = :uri")
    suspend fun updateFavorite(uri: String, isFav: Boolean)

    @Query("UPDATE movie_metadata SET userCustomTitle = :customTitle, userCustomPoster = :customPoster WHERE mediaUri = :uri")
    suspend fun updateCustomDetails(uri: String, customTitle: String?, customPoster: String?)

    @Query("DELETE FROM movie_metadata")
    suspend fun clearAll()
}
