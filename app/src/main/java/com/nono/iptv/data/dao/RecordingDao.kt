package com.nono.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.nono.iptv.data.entity.RecordingEntity

@Dao
interface RecordingDao {

    @Insert
    suspend fun insert(recording: RecordingEntity): Long

    @Query("UPDATE recordings SET filePath = :path WHERE id = :id")
    suspend fun updatePath(id: Long, path: String)

    @Query("UPDATE recordings SET status = :status, sizeBytes = :size WHERE id = :id")
    suspend fun finish(id: Long, status: String, size: Long)

    @Query("SELECT * FROM recordings ORDER BY startedAt DESC")
    suspend fun getAll(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE status = 'recording' LIMIT 1")
    suspend fun active(): RecordingEntity?

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun get(id: Long): RecordingEntity?

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun delete(id: Long)
}
