package com.kinassist.app

import android.app.Application
import android.util.Log

class KinAssistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize WebRTC Factory and hardware acceleration globally
            com.kinassist.app.core.webrtc.WebRtcManager.getInstance(this).initFactory()
        } catch (e: UnsatisfiedLinkError) {
            Log.w("KinAssistApp", "Native WebRTC library not available in current runtime: ${e.message}")
        } catch (e: Exception) {
            Log.w("KinAssistApp", "WebRTC init note: ${e.message}")
        }
    }
}
