package com.nono.iptv.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY channelNumber ASC, name ASC")
    suspend fun getAll(): List<ChannelEntity>

    @Query("SELECT * FROM channels WHERE groupName = :group ORDER BY channelNumber ASC, name ASC")
    suspend fun getByGroup(group: String): List<ChannelEntity>

    @Query("SELECT DISTINCT groupName FROM channels ORDER BY groupName ASC")
    suspend fun getGroups(): List<String>

    @Query("SELECT COUNT(*) FROM channels")
    suspend fun count(): Int

    @Query("UPDATE channels SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels")
    suspend fun clear()

    /**
     * Insert a large channel list in bounded chunks. A single giant INSERT can blow past
     * SQLite's statement/variable limits and spike memory on big M3U files, so we commit
     * [chunkSize] rows at a time inside one transaction.
     */
    @Transaction
    suspend fun insertAllChunked(channels: List<ChannelEntity>, chunkSize: Int = 500) {
        var index = 0
        while (index < channels.size) {
            val end = minOf(index + chunkSize, channels.size)
            insertChunk(channels.subList(index, end))
            index = end
        }
    }

    /** Replace the whole channel set atomically (clear + chunked insert). */
    @Transaction
    suspend fun replaceAll(channels: List<ChannelEntity>, chunkSize: Int = 500) {
        clear()
        insertAllChunked(channels, chunkSize)
    }
}
