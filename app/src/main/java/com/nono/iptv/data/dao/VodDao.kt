package com.nono.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nono.iptv.data.entity.VodEntity

@Dao
interface VodDao {

    @Query("SELECT * FROM vod WHERE playlistId = :playlistId AND kind = :kind AND categoryRemoteId = :categoryRemoteId ORDER BY name ASC")
    suspend fun getByCategory(playlistId: Long, kind: String, categoryRemoteId: String): List<VodEntity>

    @Query("SELECT * FROM vod WHERE playlistId = :playlistId AND kind = :kind ORDER BY name ASC LIMIT 500")
    suspend fun getAllForKind(playlistId: Long, kind: String): List<VodEntity>

    @Query("SELECT COUNT(*) FROM vod WHERE playlistId = :playlistId AND kind = :kind")
    suspend fun count(playlistId: Long, kind: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChunk(items: List<VodEntity>)

    @Query("DELETE FROM vod WHERE playlistId = :playlistId")
    suspend fun clearForPlaylist(playlistId: Long)

    @Transaction
    suspend fun insertAllChunked(items: List<VodEntity>, chunkSize: Int = 500) {
        var i = 0
        while (i < items.size) {
            val end = minOf(i + chunkSize, items.size)
            insertChunk(items.subList(i, end))
            i = end
        }
    }
}
