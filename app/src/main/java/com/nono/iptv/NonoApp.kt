package com.nono.iptv

import android.app.Application
import com.nono.iptv.data.IptvRepository

/**
 * Application singleton + tiny service locator. Keeps one [IptvRepository] (and through it
 * one Room database) for the whole process so fragments/activities share the same data.
 */
class NonoApp : Application() {

    val repository: IptvRepository by lazy { IptvRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NonoApp
            private set

        fun repository(): IptvRepository = instance.repository
    }
}
