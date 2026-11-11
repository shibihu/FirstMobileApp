package com.example.nativestarter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendSettingsTest {

    @Test
    fun normalizesTrailingSlashOnHttpsOrigin() {
        assertEquals("https://studio.example.com", BackendSettings.normalizeBaseUrl(" https://studio.example.com/ "))
    }

    @Test
    fun rejectsCredentialBearingUrls() {
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

    @Test
    fun acceptsLocalhostHttp() {
        assertEquals("http://localhost:3000", BackendSettings.normalizeBaseUrl("http://localhost:3000"))
        assertEquals("http://localhost:3000", BackendSettings.normalizeBaseUrl(" http://localhost:3000/ "))
        assertEquals("http://localhost:10000", BackendSettings.normalizeBaseUrl("http://localhost:10000"))
        assertEquals("http://localhost:10000", BackendSettings.normalizeBaseUrl("HTTP://LOCALHOST:10000/"))
    }

    @Test
    fun acceptsLoopbackIpHttp() {
        assertEquals("http://127.0.0.1:3000", BackendSettings.normalizeBaseUrl("http://127.0.0.1:3000"))
        assertEquals("http://127.0.0.1:3000", BackendSettings.normalizeBaseUrl(" http://127.0.0.1:3000/ "))
        assertEquals("http://127.0.0.1:10000", BackendSettings.normalizeBaseUrl("http://127.0.0.1:10000"))
    }

    @Test
    fun acceptsRemoteHttps() {
        assertEquals("https://studio.example.com", BackendSettings.normalizeBaseUrl("https://studio.example.com"))
        assertEquals(
            "https://studio.example.com:8443",
            BackendSettings.normalizeBaseUrl("https://studio.example.com:8443/"),
        )
    }

    @Test
    fun rejectsRemoteHttp() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://studio.example.com")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://192.168.1.10:3000")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://10.0.0.2:10000")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://localhost:8080")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://127.0.0.1:65536")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://[::1]:3000")
        }
    }

    @Test
    fun rejectsMalformedUrls() {
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("not-a-url")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("https://")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("http://localhost:not-a-port")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            BackendSettings.normalizeBaseUrl("ftp://studio.example.com")
        }
    }

    @Test
    fun flagsLocalHttpBaseUrls() {
        assertTrue(BackendSettings.isLocalHttpBaseUrl("http://localhost:3000"))
        assertTrue(BackendSettings.isLocalHttpBaseUrl("http://127.0.0.1:10000/"))
        assertFalse(BackendSettings.isLocalHttpBaseUrl("http://localhost:8080"))
        assertFalse(BackendSettings.isLocalHttpBaseUrl("https://studio.example.com"))
        assertFalse(BackendSettings.isLocalHttpBaseUrl("http://studio.example.com"))
        assertFalse(BackendSettings.isLocalHttpBaseUrl("not-a-url"))
    }

    @Test
    fun endpointJoiningAddsOneSeparatorAndDoesNotDuplicateApiPrefix() {
        assertEquals(
            "http://localhost:3000/api/health",
            AiClipStudioApi.buildEndpointUrl("http://localhost:3000/", "/api/health").toString(),
        )
        assertEquals(
            "https://studio.example.com/api/render?id=a&rid=b",
            AiClipStudioApi.buildEndpointUrl("https://studio.example.com", "api/render?id=a&rid=b").toString(),
        )
    }
}
