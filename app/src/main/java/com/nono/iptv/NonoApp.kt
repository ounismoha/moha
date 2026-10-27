package com.nono.iptv

import android.app.Application
import android.os.Build
import com.nono.iptv.data.IptvRepository
import com.nono.iptv.util.DebugLog

/**
 * Application singleton + tiny service locator. Keeps one [IptvRepository] (and through it
 * one Room database) for the whole process so fragments/activities share the same data.
 * Also installs the debug logger and a crash handler so errors can be reviewed in-app.
 */
class NonoApp : Application() {

    val repository: IptvRepository by lazy { IptvRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        DebugLog.init(this)
        DebugLog.d(
            "App",
            "nono app start — ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
        )

        // Capture uncaught crashes to the debug log before the system handles them.
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            DebugLog.e("CRASH", "Uncaught on ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        lateinit var instance: NonoApp
            private set

        fun repository(): IptvRepository = instance.repository
    }
}
