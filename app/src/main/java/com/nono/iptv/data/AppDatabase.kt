package com.nono.iptv.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nono.iptv.data.dao.CategoryDao
import com.nono.iptv.data.dao.ChannelDao
import com.nono.iptv.data.dao.PlaylistDao
import com.nono.iptv.data.dao.ProgramDao
import com.nono.iptv.data.dao.VodDao
import com.nono.iptv.data.entity.CategoryEntity
import com.nono.iptv.data.entity.ChannelEntity
import com.nono.iptv.data.entity.PlaylistEntity
import com.nono.iptv.data.entity.ProgramEntity
import com.nono.iptv.data.entity.VodEntity

@Database(
    entities = [
        PlaylistEntity::class,
        CategoryEntity::class,
        ChannelEntity::class,
        VodEntity::class,
        ProgramEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun categoryDao(): CategoryDao
    abstract fun channelDao(): ChannelDao
    abstract fun vodDao(): VodDao
    abstract fun programDao(): ProgramDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nono.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
