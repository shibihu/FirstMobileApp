package com.example.nativestarter.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.example.nativestarter.domain.editor.AudioSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaRepository(private val context: Context) {

    suspend fun loadVideo(uri: Uri, persistedGrant: Boolean): ImportedVideo = withContext(Dispatchers.IO) {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameColumn >= 0) cursor.getString(nameColumn) else null
        } ?: "Imported video"

        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()
        try {
            if (uri.scheme == "file") {
                val path = uri.path ?: error("Invalid app-private video file URI.")
                retriever.setDataSource(path)
                extractor.setDataSource(path)
            } else {
                retriever.setDataSource(context, uri)
                extractor.setDataSource(context, uri, null)
            }
            var videoMimeType: String? = null
            val audioMimeTypes = mutableListOf<String>()
            for (trackIndex in 0 until extractor.trackCount) {
                val mime = extractor.getTrackFormat(trackIndex).getString(android.media.MediaFormat.KEY_MIME)
                when {
                    mime?.startsWith("video/") == true && videoMimeType == null -> videoMimeType = mime
                    mime?.startsWith("audio/") == true -> audioMimeTypes += mime
                }
            }
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull()
            val rawHeight = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0
            val rotated = rotation == 90 || rotation == 270
            ImportedVideo(
                uri = uri,
                displayName = name,
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull(),
                width = if (rotated) rawHeight else rawWidth,
                height = if (rotated) rawWidth else rawHeight,
                videoMimeType = videoMimeType,
                audioMimeTypes = audioMimeTypes,
                persistedGrant = persistedGrant,
            )
        } finally {
            retriever.release()
            extractor.release()
        }
    }

    suspend fun loadAudio(uri: Uri): AudioSegment = withContext(Dispatchers.IO) {
        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameColumn >= 0) cursor.getString(nameColumn) else null
        } ?: "Imported audio"
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            AudioSegment(
                uri = uri.toString(),
                name = name,
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L,
            )
        } finally {
            retriever.release()
        }
    }
}