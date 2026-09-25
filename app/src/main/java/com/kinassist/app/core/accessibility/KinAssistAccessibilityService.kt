package com.kinassist.app.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.kinassist.app.core.webrtc.RescueCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class KinAssistAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: KinAssistAccessibilityService? = null

        private val _isSensitiveAppForeground = MutableStateFlow(false)
        val isSensitiveAppForeground = _isSensitiveAppForeground.asStateFlow()

        // Known sensitive package names (Banking, UPI, Settings Passwords)
        private val SENSITIVE_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "net.one97.paytm",                        // Paytm
            "com.phonepe.app",                       // PhonePe
            "in.org.npci.upiapp",                    // BHIM
            "com.sbi.lotusintouch",                  // YONO SBI
            "com.icicibank.mobile",                  // iMobile
            "com.android.settings.password",         // Password screen
            "com.android.credentialmanager"          // Android Passkey/Creds
        )

        fun performRescue(command: RescueCommand): Boolean {
            val service = instance ?: return false
            return when (command) {
                RescueCommand.GLOBAL_BACK -> service.performGlobalAction(GLOBAL_ACTION_BACK)
                RescueCommand.GLOBAL_HOME -> service.performGlobalAction(GLOBAL_ACTION_HOME)
                RescueCommand.GLOBAL_RECENTS -> service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                RescueCommand.GLOBAL_NOTIFICATIONS -> service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                RescueCommand.NONE -> false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val isSensitive = SENSITIVE_PACKAGES.contains(packageName)
            _isSensitiveAppForeground.value = isSensitive
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
