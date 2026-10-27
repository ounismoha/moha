package com.nono.iptv.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nono.iptv.data.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE playlistId = :playlistId AND kind = :kind ORDER BY sortOrder ASC, name ASC")
    suspend fun getForKind(playlistId: Long, kind: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE playlistId = :playlistId")
    suspend fun clearForPlaylist(playlistId: Long)
}
