package org.voltageos.separatesound

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class SeparateSoundSettingsActivity : android.app.Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, SeparateSoundSettingsFragment())
            .commit()
    }
}
