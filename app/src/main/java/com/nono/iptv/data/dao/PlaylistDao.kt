package com.nono.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nono.iptv.data.entity.PlaylistEntity

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt ASC")
    suspend fun getAll(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getById(id: Long): PlaylistEntity?

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun count(): Int

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)
}
