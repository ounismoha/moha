package com.nono.iptv.ui.setup

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import com.nono.iptv.ui.main.MainActivity

/**
 * Hosts the first-launch setup wizard: choose a playlist type, then enter Xtream credentials
 * or an M3U URL. On success it opens [MainActivity].
 */
class SetupActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            GuidedStepSupportFragment.addAsRoot(this, PlaylistTypeStepFragment(), android.R.id.content)
        }
    }

    fun finishToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
