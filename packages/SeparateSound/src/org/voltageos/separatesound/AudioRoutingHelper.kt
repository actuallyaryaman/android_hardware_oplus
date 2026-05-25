package org.voltageos.separatesound

import android.media.AudioDeviceInfo
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log

object AudioRoutingHelper {
    private const val TAG = "AudioRoutingHelper"
    private var audioService: Any? = null

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

    fun setUidDeviceAffinityForPackage(
        packageName: String,
        deviceType: Int,
        address: String
    ): Boolean {
        val uid = getUidForPackage(packageName) ?: return false
        return setUidDeviceAffinity(uid, deviceType, address)
    }

    fun removeUidDeviceAffinityForPackage(packageName: String): Boolean {
        val uid = getUidForPackage(packageName) ?: return false
        return removeUidDeviceAffinity(uid)
    }

    private fun getUidForPackage(pkg: String): Int? {
        return try {
            val pm = android.app.AppGlobals.getInitialApplication()
                .packageManager
            pm.getApplicationInfo(pkg, 0).uid
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get UID for package $pkg", e)
            null
        }
    }
}
