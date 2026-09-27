package com.kinassist.app.core.pairing

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

data class PairingConfig(
    val isPaired: Boolean,
    val role: String, // "SENIOR" or "HELPER"
    val familyCode: String,
    val seniorName: String,
    val helperName: String,
    val helperPhone: String,
    val signalingUrl: String
)

class PairingManager(private val context: Context) {

    companion object {
        private const val PREFS_FILE = "kinassist_secure_prefs"
        private const val KEY_IS_PAIRED = "key_is_paired"
        private const val KEY_ROLE = "key_role"
        private const val KEY_FAMILY_CODE = "key_family_code"
        private const val KEY_SENIOR_NAME = "key_senior_name"
        private const val KEY_HELPER_NAME = "key_helper_name"
        private const val KEY_HELPER_PHONE = "key_helper_phone"
        private const val KEY_SIGNALING_URL = "key_signaling_url"
        const val DEFAULT_SIGNALING_URL = "ws://10.0.2.2:8080"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    fun isPaired(): Boolean = prefs.getBoolean(KEY_IS_PAIRED, false)

    fun getPairingConfig(): PairingConfig {
        return PairingConfig(
            isPaired = prefs.getBoolean(KEY_IS_PAIRED, false),
            role = prefs.getString(KEY_ROLE, "SENIOR") ?: "SENIOR",
            familyCode = prefs.getString(KEY_FAMILY_CODE, "884219") ?: "884219",
            seniorName = prefs.getString(KEY_SENIOR_NAME, "Senior") ?: "Senior",
            helperName = prefs.getString(KEY_HELPER_NAME, "Caregiver") ?: "Caregiver",
            helperPhone = prefs.getString(KEY_HELPER_PHONE, "") ?: "",
            signalingUrl = prefs.getString(KEY_SIGNALING_URL, DEFAULT_SIGNALING_URL) ?: DEFAULT_SIGNALING_URL
        )
    }

    fun savePairing(
        role: String,
        familyCode: String,
        seniorName: String = "Senior",
        helperName: String = "Caregiver",
        helperPhone: String = "",
        signalingUrl: String = DEFAULT_SIGNALING_URL
    ) {
        prefs.edit()
            .putBoolean(KEY_IS_PAIRED, true)
            .putString(KEY_ROLE, role)
            .putString(KEY_FAMILY_CODE, familyCode)
            .putString(KEY_SENIOR_NAME, seniorName)
            .putString(KEY_HELPER_NAME, helperName)
            .putString(KEY_HELPER_PHONE, helperPhone)
            .putString(KEY_SIGNALING_URL, signalingUrl)
            .apply()
    }

    fun clearPairing() {
        prefs.edit().clear().apply()
    }
}
