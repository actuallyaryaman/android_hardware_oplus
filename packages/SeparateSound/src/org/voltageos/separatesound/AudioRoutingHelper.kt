package org.voltageos.separatesound

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log

object AudioRoutingHelper {
    private const val TAG = "AudioRoutingHelper"
    private var audioService: Any? = null
    private var mediaStrategy: Any? = null
    private var speakerDevice: AudioDeviceInfo? = null

    private fun getAudioService(): Any? {
        if (audioService != null) return audioService
        return try {
            val binder = ServiceManager.getService("audio")
            val stubClass = Class.forName("android.media.IAudioService\$Stub")
            val asInterface = stubClass.getMethod("asInterface", IBinder::class.java)
            val service = asInterface.invoke(null, binder)
            audioService = service
            service
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get IAudioService", e)
            null
        }
    }

    fun init(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        speakerDevice = findSpeakerDevice(am)
        mediaStrategy = findMediaStrategy(am)
        Log.d(TAG, "init: speakerDevice=$speakerDevice mediaStrategy=$mediaStrategy")
    }

    fun setUidDeviceAffinity(uid: Int, deviceType: Int, address: String): Boolean {
        val service = getAudioService() ?: return false
        return try {
            val method = service.javaClass.getMethod(
                "setUidDeviceAffinity",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            method.invoke(service, uid, deviceType, address)
            Log.d(TAG, "setUidDeviceAffinity(uid=$uid, type=$deviceType, addr=$address) succeeded")
            true
        } catch (e: Exception) {
            Log.e(TAG, "setUidDeviceAffinity failed", e)
            false
        }
    }

    fun removeUidDeviceAffinity(uid: Int): Boolean {
        val service = getAudioService() ?: return false
        return try {
            try {
                val removeMethod = service.javaClass.getMethod(
                    "removeUidDeviceAffinity",
                    Int::class.javaPrimitiveType
                )
                removeMethod.invoke(service, uid)
                Log.d(TAG, "removeUidDeviceAffinity(uid=$uid) succeeded")
            } catch (e1: NoSuchMethodException) {
                val setMethod = service.javaClass.getMethod(
                    "setUidDeviceAffinity",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                setMethod.invoke(service, uid, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "")
                Log.d(TAG, "remove via setUidDeviceAffinity->Speaker succeeded")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "removeUidDeviceAffinity failed", e)
            false
        }
    }

    fun forceDefaultToSpeaker(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val strategy = mediaStrategy ?: findMediaStrategy(am) ?: return false
        val speaker = speakerDevice ?: findSpeakerDevice(am) ?: return false
        return try {
            val method = am.javaClass.getMethod(
                "setPreferredDeviceForStrategy",
                Class.forName("android.media.AudioProductStrategy"),
                AudioDeviceInfo::class.java
            )
            method.invoke(am, strategy, speaker) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "forceDefaultToSpeaker failed", e)
            false
        }
    }

    fun restoreDefaultRouting(ctx: Context): Boolean {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val strategy = mediaStrategy ?: findMediaStrategy(am) ?: return false
        return try {
            val method = am.javaClass.getMethod(
                "setPreferredDeviceForStrategy",
                Class.forName("android.media.AudioProductStrategy"),
                AudioDeviceInfo::class.java
            )
            method.invoke(am, strategy, null) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "restoreDefaultRouting failed", e)
            false
        }
    }

    private fun findSpeakerDevice(am: AudioManager): AudioDeviceInfo? {
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    }

    private fun findMediaStrategy(am: AudioManager): Any? {
        return try {
            val getStrategies = am.javaClass.getMethod("getAudioProductStrategies")
            val strategies = getStrategies.invoke(am) as? List<*> ?: return null
            for (strategy in strategies) {
                if (strategy == null) continue
                val getLegacyType = strategy.javaClass.getMethod("getLegacyStreamType")
                val legacyType = getLegacyType.invoke(strategy) as? Int ?: continue
                if (legacyType == android.media.AudioManager.STREAM_MUSIC) {
                    return strategy
                }
            }
            strategies.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "findMediaStrategy failed", e)
            null
        }
    }
}
