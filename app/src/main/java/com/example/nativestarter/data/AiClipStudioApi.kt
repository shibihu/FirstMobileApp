package com.example.nativestarter.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class AiClipStudioApi(private val context: Context, private val settings: BackendSettings) {

    private val contentResolver = context.contentResolver

    suspend fun health(): JSONObject = withContext(Dispatchers.IO) {
        requestJson("GET", "/api/health")
    }

    suspend fun uploadVideo(uri: Uri, displayName: String, mimeType: String?): JSONObject =
        withContext(Dispatchers.IO) {
            val boundary = "----NativeAIClipStudio${UUID.randomUUID().toString().replace("-", "")}" 
            val connection = openConnection("/api/upload", "POST")
            connection.doOutput = true
            connection.setChunkedStreamingMode(COPY_BUFFER_SIZE)
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            try {
                val input = contentResolver.openInputStream(uri)
                    ?: error("Android could not read the selected video.")
                BufferedOutputStream(connection.outputStream).use { output ->
                    val safeName = displayName.replace("\"", "_").replace("\r", "_").replace("\n", "_")
                    output.write("--$boundary\r\n".toByteArray())
                    output.write("Content-Disposition: form-data; name=\"file\"; filename=\"$safeName\"\r\n".toByteArray())
                    output.write("Content-Type: ${mimeType ?: "video/mp4"}\r\n\r\n".toByteArray())
                    BufferedInputStream(input).use { source -> source.copyTo(output, COPY_BUFFER_SIZE) }
                    output.write("\r\n--$boundary--\r\n".toByteArray())
                    output.flush()
                }
                readResponse(connection)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun detectScenes(jobId: String): JSONObject = withContext(Dispatchers.IO) {
        requestJson(
            "POST",
            "/api/tool",
            JSONObject().put("id", jobId).put("tool", "detect_scenes").put("args", JSONObject()),
        )
    }

    suspend fun automaticSubtitles(jobId: String): JSONObject = withContext(Dispatchers.IO) {
        requestJson(
            "POST",
            "/api/captions",
            JSONObject().put("id", jobId).put("auto", true),
        )
    }

    suspend fun chat(jobId: String, message: String): JSONObject = withContext(Dispatchers.IO) {
        requestJson(
            "POST",
            "/api/chat",
            JSONObject().put("id", jobId).put("message", message),
        )
    }

    suspend fun render(jobId: String, renderId: String): JSONObject = withContext(Dispatchers.IO) {
        val id = URLEncoder.encode(jobId, Charsets.UTF_8.name())
        val rid = URLEncoder.encode(renderId, Charsets.UTF_8.name())
        requestJson("GET", "/api/render?id=$id&rid=$rid")
    }

    suspend fun downloadRenderedMedia(relativePath: String, displayName: String): Uri = withContext(Dispatchers.IO) {
        require(relativePath.startsWith("/media/") && !relativePath.contains("..")) {
            "Backend returned an invalid rendered-media path."
        }
        val connection = openConnection(relativePath, "GET")
        val safeName = displayName.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "ai-edit.mp4" }
        val mediaDirectory = File(context.filesDir, "backend-media").apply { mkdirs() }
        val outputFile = File(mediaDirectory, "backend-${System.currentTimeMillis()}-$safeName")
        try {
            if (connection.responseCode !in 200..299) {
                val payload = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Could not download rendered video (HTTP ${connection.responseCode}): $payload")
            }
            connection.inputStream.use { input -> outputFile.outputStream().use { output -> input.copyTo(output) } }
            check(outputFile.length() > 0) { "Backend returned an empty rendered video." }
            Uri.fromFile(outputFile)
        } catch (error: Exception) {
            outputFile.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    private fun requestJson(method: String, path: String, body: JSONObject? = null): JSONObject {
        val connection = openConnection(path, method)
        try {
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            return readResponse(connection)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        val base = BackendSettings.normalizeBaseUrl(settings.baseUrl())
        return (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            settings.authToken()?.takeIf(String::isNotBlank)?.let {
                setRequestProperty("x-auth-token", it)
            }
        }
    }

    private fun readResponse(connection: HttpURLConnection): JSONObject {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val payload = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = runCatching { JSONObject(payload) }.getOrElse {
            throw IllegalStateException("Backend returned HTTP $code with an invalid JSON response.")
        }
        if (code !in 200..299) {
            val message = json.optString("error").ifBlank { "Backend returned HTTP $code." }
            throw IllegalStateException(message)
        }
        return json
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 60_000
        private const val COPY_BUFFER_SIZE = 64 * 1024
    }
}
