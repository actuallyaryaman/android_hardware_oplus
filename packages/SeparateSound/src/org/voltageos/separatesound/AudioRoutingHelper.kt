package org.voltageos.separatesound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioProductStrategy
import android.util.Log

object AudioRoutingHelper {
    private const val TAG = "AudioRoutingHelper"
    private var speakerDevice: AudioDeviceInfo? = null
    private var mediaStrategy: AudioProductStrategy? = null

    fun init(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        speakerDevice = findSpeakerDevice(am)
        mediaStrategy = findMediaStrategy(am)

        dumpAudioSystemMethods()

        Log.d(TAG, "init: speaker=${speakerDevice != null} strategy=${mediaStrategy != null}")
    }

    fun setUidDeviceAffinity(uid: Int, deviceType: Int, address: String): Boolean {
        val methodName = "setUidDeviceAffinity"
        try {
            val audioSystemClass = Class.forName("android.media.AudioSystem")
            val methods = audioSystemClass.methods.filter { it.name == methodName }
            Log.d(TAG, "AudioSystem.$methodName candidates: ${methods.map { it.toGenericString() }}")

            try {
                val m = audioSystemClass.getMethod(
                    methodName,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                val result = m.invoke(null, uid, deviceType, address) as Int
                Log.d(TAG, "AudioSystem.$methodName(int,int,String) -> $result")
                return result == 0
            } catch (_: NoSuchMethodException) {}

            val deviceInfoClass = Class.forName("android.media.AudioDeviceAttributes")
            try {
                val ctor = deviceInfoClass.getConstructor(
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                val deviceAttr = ctor.newInstance(2, deviceType, address)
                val m = audioSystemClass.getMethod(methodName,
                    Int::class.javaPrimitiveType, deviceInfoClass)
                val result = m.invoke(null, uid, deviceAttr) as Int
                Log.d(TAG, "AudioSystem.$methodName(int,AudioDeviceAttributes) -> $result")
                return result == 0
            } catch (_: Exception) {}

            Log.w(TAG, "No working $methodName found")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "$methodName failed", e)
            return false
        }
    }

    fun removeUidDeviceAffinity(uid: Int): Boolean {
        try {
            val audioSystemClass = Class.forName("android.media.AudioSystem")
            try {
                val m = audioSystemClass.getMethod("removeUidDeviceAffinity",
                    Int::class.javaPrimitiveType)
                val result = m.invoke(null, uid) as Int
                Log.d(TAG, "removeUidDeviceAffinity(uid=$uid) -> $result")
                return result == 0
            } catch (_: NoSuchMethodException) {}

            try {
                val m = audioSystemClass.getMethod("removeUidDeviceAffinity",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType)
                val result = m.invoke(null, uid, 0) as Int
                Log.d(TAG, "removeUidDeviceAffinity(uid,flags) -> $result")
                return result == 0
            } catch (_: NoSuchMethodException) {}

            Log.w(TAG, "removeUidDeviceAffinity not found")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "removeUidDeviceAffinity failed", e)
            return false
        }
    }

    fun forceDefaultToSpeaker(ctx: Context): Boolean {
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val strategy = mediaStrategy ?: findMediaStrategy(am) ?: return false
            val speaker = speakerDevice ?: findSpeakerDevice(am) ?: return false
            val result = am.setPreferredDeviceForStrategy(strategy, speaker)
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
            val result = am.setPreferredDeviceForStrategy(strategy, null)
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

    private fun findMediaStrategy(am: AudioManager): AudioProductStrategy? {
        return try {
            val strategies = am.audioProductStrategies
            strategies?.firstOrNull { s ->
                s.audioAttributes?.any { it.usage == AudioAttributes.USAGE_MEDIA } == true
            } ?: strategies?.firstOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "findMediaStrategy failed", e)
            null
        }
    }

    private fun dumpAudioSystemMethods() {
        try {
            val cls = Class.forName("android.media.AudioSystem")
            val methods = cls.methods.filter {
                it.name.contains("uid", ignoreCase = true)
                        || it.name.contains("affinity", ignoreCase = true)
                        || it.name.contains("routing", ignoreCase = true)
            }
            if (methods.isNotEmpty()) {
                Log.d(TAG, "AudioSystem relevant methods:")
                methods.forEach { Log.d(TAG, "  ${it.toGenericString()}") }
            } else {
                Log.d(TAG, "No uid/affinity/routing methods found on AudioSystem")
            }
        } catch (e: Exception) {
            Log.e(TAG, "dumpAudioSystemMethods failed", e)
        }
    }
}
