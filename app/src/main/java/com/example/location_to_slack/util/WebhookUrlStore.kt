package com.example.location_to_slack.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Slack Webhook URL を Android Keystore の鍵で暗号化して端末に保存する管理クラス
 */
object WebhookUrlStore {
    private const val TAG = "WebhookUrlStore"
    private const val PREF_NAME = "webhook_prefs"
    private const val KEY_WEBHOOK_URL = "encrypted_webhook_url"
    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "webhook_url_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH_BITS = 128

    /**
     * Webhook URL を暗号化して保存する
     */
    fun save(context: Context, url: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_WEBHOOK_URL, encrypt(getOrCreateKey(), url.trim()))
            .apply()
    }

    /**
     * 保存済みの Webhook URL を復号して取得する（未設定・復号失敗時は空文字）
     */
    fun load(context: Context): String {
        val encoded = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WEBHOOK_URL, null) ?: return ""
        return try {
            decrypt(getOrCreateKey(), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt Slack Webhook URL", e)
            ""
        }
    }

    /**
     * IV (12バイト) + 暗号文を Base64 化した文字列を返す
     */
    internal fun encrypt(key: SecretKey, plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(plainText.toByteArray()))
    }

    internal fun decrypt(key: SecretKey, encoded: String): String {
        val bytes = Base64.getDecoder().decode(encoded)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, bytes, 0, IV_LENGTH))
        return String(cipher.doFinal(bytes, IV_LENGTH, bytes.size - IV_LENGTH))
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }
}
