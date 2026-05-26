package org.voltageos.separatesound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log

object AudioRoutingHelper {
    private const val TAG = "AudioRoutingHelper"
    private var speakerDevice: AudioDeviceInfo? = null
    private var mediaStrategy: Any? = null

    fun init(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        speakerDevice = findSpeakerDevice(am)
        mediaStrategy = findMediaStrategy(am)
        Log.d(TAG, "init: speaker=${speakerDevice != null} strategy=${mediaStrategy != null}")
    }

    fun setUidDeviceAffinity(uid: Int, deviceType: Int, address: String): Boolean {
        return try {
            val audioSystemClass = Class.forName("android.media.AudioSystem")
            val method = audioSystemClass.getMethod(
                "setUidDeviceAffinity",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result = method.invoke(null, uid, deviceType, address) as Int
            Log.d(TAG, "AudioSystem.setUidDeviceAffinity(uid=$uid, type=$deviceType) -> $result")
            result == 0
        } catch (e: Exception) {
            Log.e(TAG, "AudioSystem.setUidDeviceAffinity failed", e)
            false
        }
    }

    fun removeUidDeviceAffinity(uid: Int): Boolean {
        return try {
            val audioSystemClass = Class.forName("android.media.AudioSystem")
            try {
                val removeMethod = audioSystemClass.getMethod(
                    "removeUidDeviceAffinity",
                    Int::class.javaPrimitiveType
                )
                val result = removeMethod.invoke(null, uid) as Int
                Log.d(TAG, "AudioSystem.removeUidDeviceAffinity(uid=$uid) -> $result")
                result == 0
            } catch (e1: NoSuchMethodException) {
                val setMethod = audioSystemClass.getMethod(
                    "setUidDeviceAffinity",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                val result = setMethod.invoke(
                    null, uid, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, ""
                ) as Int
                Log.d(TAG, "reset via setUidDeviceAffinity->Speaker -> $result")
                result == 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "removeUidDeviceAffinity failed", e)
            false
        }
    }

    fun forceDefaultToSpeaker(ctx: Context): Boolean {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val strategy = mediaStrategy ?: findMediaStrategy(am) ?: return false
            val speaker = speakerDevice ?: findSpeakerDevice(am) ?: return false

            val strategyClass = Class.forName("android.media.audiopolicy.AudioProductStrategy")
            val setPreferred = am.javaClass.getMethod(
                "setPreferredDeviceForStrategy",
                strategyClass,
                AudioDeviceInfo::class.java
            )
            val result = setPreferred.invoke(am, strategy, speaker) as? Boolean ?: false
            Log.d(TAG, "forceDefaultToSpeaker: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "forceDefaultToSpeaker failed", e)
            return false
        }
    }

    fun restoreDefaultRouting(ctx: Context): Boolean {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val strategy = mediaStrategy ?: findMediaStrategy(am) ?: return false

            val strategyClass = Class.forName("android.media.audiopolicy.AudioProductStrategy")
            val setPreferred = am.javaClass.getMethod(
                "setPreferredDeviceForStrategy",
                strategyClass,
                AudioDeviceInfo::class.java
            )
            val result = setPreferred.invoke(am, strategy, null) as? Boolean ?: false
            Log.d(TAG, "restoreDefaultRouting: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "restoreDefaultRouting failed", e)
            return false
        }
    }

    private fun findSpeakerDevice(am: AudioManager): AudioDeviceInfo? {
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
    }

    private fun findMediaStrategy(am: AudioManager): Any? {
        return try {
            val getStrategies = am.javaClass.getMethod("getAudioProductStrategies")
            val strategies = getStrategies.invoke(am) as? List<*> ?: return null

            val strategyClass = Class.forName("android.media.audiopolicy.AudioProductStrategy")
            val getAttrMethod = strategyClass.getMethod("getAudioAttributes")

            for (strategy in strategies) {
                if (strategy == null) continue
                val attrsList = getAttrMethod.invoke(strategy) as? List<*> ?: continue
                for (attrs in attrsList) {
                    if (attrs is AudioAttributes && attrs.usage == AudioAttributes.USAGE_MEDIA) {
                        return strategy
                    }
                }
            }
            strategies.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "findMediaStrategy failed, trying fallback", e)
            try {
                val strategies = (am.javaClass.getMethod("getAudioProductStrategies")
                    .invoke(am) as? List<*>) ?: return null
                strategies.firstOrNull()
            } catch (e2: Exception) {
                Log.e(TAG, "findMediaStrategy fallback also failed", e2)
                null
            }
        }
    }
}
