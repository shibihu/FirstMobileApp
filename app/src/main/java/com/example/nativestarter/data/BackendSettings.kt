package com.example.nativestarter.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.net.URI
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class BackendSettings(context: Context) {

    private val preferences = context.getSharedPreferences("backend_settings", Context.MODE_PRIVATE)

    fun baseUrl(): String = preferences.getString(KEY_BASE_URL, "") ?: ""

    fun saveBaseUrl(value: String): String {
        val normalized = normalizeBaseUrl(value)
        preferences.edit().putString(KEY_BASE_URL, normalized).apply()
        return normalized
    }

    fun authToken(): String? = preferences.getString(KEY_AUTH_TOKEN, null)?.let(::decrypt)

    fun jobId(): String? = preferences.getString(KEY_JOB_ID, null)

    fun saveJobId(id: String) {
        preferences.edit().putString(KEY_JOB_ID, id).apply()
    }

    fun jobSourceUri(): String? = preferences.getString(KEY_JOB_SOURCE_URI, null)

    fun saveJobSourceUri(uri: String) {
        preferences.edit().putString(KEY_JOB_SOURCE_URI, uri).apply()
    }

    fun saveAuthToken(value: String) {
        if (value.isBlank()) preferences.edit().remove(KEY_AUTH_TOKEN).apply()
        else preferences.edit().putString(KEY_AUTH_TOKEN, encrypt(value.trim())).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String? = runCatching {
        val payload = Base64.decode(value, Base64.NO_WRAP)
        require(payload.size > GCM_IV_LENGTH_BYTES) { "Stored token is invalid." }
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val encrypted = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_JOB_ID = "job_id"
        private const val KEY_JOB_SOURCE_URI = "job_source_uri"
        private const val KEY_ALIAS = "ai_clip_studio_backend_token"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        fun normalizeBaseUrl(value: String): String {
            val trimmed = value.trim().trimEnd('/')
            require(trimmed.isNotEmpty()) { "Enter the backend HTTPS URL." }
            val uri = runCatching { URI(trimmed) }.getOrElse {
                throw IllegalArgumentException("Enter a valid backend URL.")
            }
            require(uri.scheme.equals("https", ignoreCase = true)) {
                "Backend URL must use HTTPS. Put a TLS reverse proxy in front of the reference server."
            }
            require(
                !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null &&
                    uri.fragment == null && uri.path.orEmpty() in listOf("", "/"),
            ) {
                "Backend URL must be a root origin without credentials, path, query, or fragment."
            }
            return trimmed
        }
    }
}
