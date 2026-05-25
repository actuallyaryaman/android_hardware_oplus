package org.voltageos.separatesound

import android.os.Bundle
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity

class SeparateSoundSettingsActivity : CollapsingToolbarBaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
            .beginTransaction()
            .replace(
                com.android.settingslib.collapsingtoolbar.R.id.content_frame,
                SeparateSoundSettingsFragment(),
                TAG,
            )
            .commit()
    }

    companion object {
        private const val TAG = "SeparateSoundSettingsActivity"
    }
}
