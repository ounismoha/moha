package com.nono.iptv.ui.settings

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/** Standalone host for the diagnostics screen, reachable from the setup wizard and settings. */
class DebugActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, DebugFragment())
                .commit()
        }
    }
}
