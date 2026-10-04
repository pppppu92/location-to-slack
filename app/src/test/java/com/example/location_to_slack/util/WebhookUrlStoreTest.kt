package com.example.location_to_slack.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import javax.crypto.KeyGenerator

class WebhookUrlStoreTest {
    @Test
    fun encryptThenDecrypt_shouldRestoreOriginalUrlWithoutStoringPlainText() {
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val url = "https://hooks.slack.com/services/T000/B000/XXXX"

        val encrypted = WebhookUrlStore.encrypt(key, url)

        assertFalse(encrypted.contains("hooks.slack.com"))
        assertEquals(url, WebhookUrlStore.decrypt(key, encrypted))
    }
}
