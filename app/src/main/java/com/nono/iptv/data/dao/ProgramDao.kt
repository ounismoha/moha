package com.nono.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nono.iptv.data.entity.ProgramEntity

@Dao
interface ProgramDao {

    /** The program airing at [nowMs] for a channel, if any. */
    @Query("SELECT * FROM programs WHERE epgChannelId = :epgChannelId AND startMs <= :nowMs AND stopMs > :nowMs ORDER BY startMs ASC LIMIT 1")
    suspend fun getNow(epgChannelId: String, nowMs: Long): ProgramEntity?

    /** Upcoming programs for a channel after [afterMs]. */
    @Query("SELECT * FROM programs WHERE epgChannelId = :epgChannelId AND startMs >= :afterMs ORDER BY startMs ASC LIMIT :limit")
    suspend fun getUpcoming(epgChannelId: String, afterMs: Long, limit: Int = 10): List<ProgramEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<ProgramEntity>)

    @Query("DELETE FROM programs")
    suspend fun clearAll()
}
