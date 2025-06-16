package dev.markodojkic.singiattend

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

object SecureStorage {

    private const val PREFS_NAME = "secure_prefs"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun save(context: Context, key: String, data: ByteArray) {
        val encoded = android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP)
        getPrefs(context).edit { putString(key, encoded) }
    }

    fun load(context: Context, key: String): ByteArray? {
        val encoded = getPrefs(context).getString(key, null) ?: return null
        return android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
    }

    fun deleteAll(context: Context) {
        getPrefs(context).edit {clear()}
    }
}