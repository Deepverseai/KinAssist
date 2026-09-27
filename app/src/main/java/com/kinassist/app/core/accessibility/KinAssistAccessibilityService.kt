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

        // Known sensitive package names (Banking, UPI, Crypto, Settings Passwords)
        private val SENSITIVE_PACKAGES = setOf(
            "com.google.android.apps.nbu.paisa.user", // Google Pay
            "net.one97.paytm",                        // Paytm
            "com.phonepe.app",                       // PhonePe
            "in.org.npci.upiapp",                    // BHIM
            "com.sbi.lotusintouch",                  // YONO SBI
            "com.icicibank.mobile",                  // iMobile
            "com.snapwork.hdfc",                     // HDFC Bank MobileBanking
            "com.axis.mobile",                       // Axis Mobile
            "com.msf.kbank.mobile",                  // Kotak Mobile Banking
            "com.bankofbaroda.mconnect",             // bob World
            "com.pnb.pnbone",                        // PNB ONE
            "com.canarabank.ai1",                    // Canara ai1
            "com.dreamplug.androidapp",              // CRED
            "com.zerodha.kite3",                     // Kite by Zerodha
            "com.nextbillion.groww",                 // Groww
            "com.android.settings.password",         // Password screen
            "com.android.credentialmanager",         // Android Passkey/Creds
            "com.onepassword.android",               // 1Password
            "com.x8bit.bitwarden"                    // Bitwarden
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
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val packageName = event.packageName?.toString() ?: ""
            var isSensitive = SENSITIVE_PACKAGES.contains(packageName)

            // Dynamic check for password or PIN input fields on screen
            if (!isSensitive) {
                try {
                    val root = rootInActiveWindow
                    if (root != null) {
                        isSensitive = containsPasswordField(root)
                    }
                } catch (_: Exception) {}
            }

            if (_isSensitiveAppForeground.value != isSensitive) {
                _isSensitiveAppForeground.value = isSensitive
                com.kinassist.app.core.webrtc.WebRtcManager.getInstance(applicationContext).setPrivacyBlackout(
                    blackout = isSensitive,
                    reason = if (isSensitive) "Sensitive screen active ($packageName)" else "Screen resumed"
                )
            }
        }
    }

    private fun containsPasswordField(node: android.view.accessibility.AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return true
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (containsPasswordField(child)) return true
        }
        return false
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
