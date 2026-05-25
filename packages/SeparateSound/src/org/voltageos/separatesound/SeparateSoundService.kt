package org.voltageos.separatesound

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.IBinder
import android.util.Log

class SeparateSoundService : Service() {
    private lateinit var audioManager: AudioManager
    private var targetAppUid: Int? = null
    private var previousWasActive: Boolean = false

    private val playbackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            handlePlaybackConfigs(configs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        when (intent?.action) {
            ACTION_STOP -> {
                clearRouting()
                audioManager.unregisterAudioPlaybackCallback(playbackCallback)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_UPDATE -> {
                clearRouting()
                applyCurrentRouting()
            }
            else -> {
                audioManager.registerAudioPlaybackCallback(playbackCallback, null)
                applyCurrentRouting()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            audioManager.unregisterAudioPlaybackCallback(playbackCallback)
        } catch (_: Exception) {}
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }

    private fun handlePlaybackConfigs(configs: MutableList<AudioPlaybackConfiguration>) {
        if (!RouteStore.isEnabled(this)) return
        val targetPkg = RouteStore.getSelectedPackage(this) ?: return

        val uid = getUidForPackage(targetPkg) ?: run {
            targetAppUid = null
            return
        }
        targetAppUid = uid

        val hasActiveSession = configs.any { config ->
            try {
                config.clientUid == uid
            } catch (_: Exception) {
                false
            }
        }

        if (hasActiveSession && !previousWasActive) {
            Log.d(TAG, "Target app $targetPkg (uid=$uid) started playing — applying routing")
            applyRouting()
            previousWasActive = true
        } else if (!hasActiveSession && previousWasActive) {
            Log.d(TAG, "Target app $targetPkg (uid=$uid) stopped — clearing routing")
            clearRouting()
            previousWasActive = false
        }
    }

    private fun applyCurrentRouting() {
        if (!RouteStore.isEnabled(this)) return
        val pkg = RouteStore.getSelectedPackage(this) ?: return

        if (isPackagePlaying(pkg)) {
            applyRouting()
            previousWasActive = true
        } else {
            previousWasActive = false
        }
    }

    private fun applyRouting() {
        val pkg = RouteStore.getSelectedPackage(this) ?: return
        val deviceType = RouteStore.getSelectedDeviceType(this) ?: return
        val address = RouteStore.getSelectedDeviceAddress(this) ?: ""

        val uid = getUidForPackage(pkg) ?: return
        targetAppUid = uid

        val success = AudioRoutingHelper.setUidDeviceAffinity(uid, deviceType, address)
        Log.d(TAG, "applyRouting: pkg=$pkg uid=$uid type=$deviceType addr=$address success=$success")
    }

    private fun clearRouting() {
        val uid = targetAppUid ?: return
        val success = AudioRoutingHelper.removeUidDeviceAffinity(uid)
        Log.d(TAG, "clearRouting: uid=$uid success=$success")
        targetAppUid = null
        previousWasActive = false
    }

    private fun isPackagePlaying(pkg: String): Boolean {
        val configs = audioManager.getActivePlaybackConfigurations()
        val uid = getUidForPackage(pkg) ?: return false
        return configs.any { it.clientUid == uid }
    }

    private fun getUidForPackage(pkg: String): Int? {
        return try {
            packageManager.getApplicationInfo(pkg, 0).uid
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "SeparateSoundService"
        const val ACTION_STOP = "org.voltageos.separatesound.action.STOP"
        const val ACTION_UPDATE = "org.voltageos.separatesound.action.UPDATE"

        fun start(context: Context) {
            context.startService(Intent(context, SeparateSoundService::class.java))
        }

        fun stop(context: Context) {
            context.startService(Intent(context, SeparateSoundService::class.java).apply {
                action = ACTION_STOP
            })
        }

        fun update(context: Context) {
            context.startService(Intent(context, SeparateSoundService::class.java).apply {
                action = ACTION_UPDATE
            })
        }
    }
}
