package com.example.nativestarter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackendSettingsTest {

    @Test
    fun normalizesTrailingSlashOnHttpsOrigin() {
        assertEquals("https://studio.example.com", BackendSettings.normalizeBaseUrl(" https://studio.example.com/ "))
    }

    @Test
    fun rejectsHttpAndCredentialBearingUrls() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://studio.example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("https://user:pass@studio.example.com")
        }
    }

    @Test
    fun rejectsUrlsWithPathOrQuery() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("https://studio.example.com/studio")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("https://studio.example.com?token=secret")
        }
    }
}