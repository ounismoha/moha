package com.nono.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nono.iptv.data.entity.ChannelEntity

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND categoryRemoteId = :categoryRemoteId ORDER BY channelNumber ASC, name ASC")
    suspend fun getByCategory(playlistId: Long, categoryRemoteId: String): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY channelNumber ASC, name ASC")
    suspend fun getAll(playlistId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND isFavorite = 1 ORDER BY channelNumber ASC, name ASC")
    suspend fun getFavorites(playlistId: Long): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 200")
    suspend fun search(playlistId: Long, query: String): List<ChannelEntity>

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun count(playlistId: Long): Int

    @Query("UPDATE channels SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun clearForPlaylist(playlistId: Long)

    /** Insert large channel lists in bounded chunks to stay within SQLite limits. */
    @Transaction
    suspend fun insertAllChunked(channels: List<ChannelEntity>, chunkSize: Int = 500) {
        var i = 0
        while (i < channels.size) {
            val end = minOf(i + chunkSize, channels.size)
            insertChunk(channels.subList(i, end))
            i = end
        }
    }
}
