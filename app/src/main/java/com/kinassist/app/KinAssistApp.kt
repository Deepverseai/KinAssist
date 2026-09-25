package com.kinassist.app

import android.app.Application

class KinAssistApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize WebRTC Factory and hardware acceleration globally
        com.kinassist.app.core.webrtc.WebRtcManager.getInstance(this).initFactory()
    }
}
