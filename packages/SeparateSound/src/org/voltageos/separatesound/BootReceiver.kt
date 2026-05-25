package org.voltageos.separatesound

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed — checking Separate Sound state")
            if (RouteStore.isEnabled(context)) {
                SeparateSoundService.start(context)
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
