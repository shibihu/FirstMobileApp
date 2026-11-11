package com.example.nativestarter.export

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.example.nativestarter.domain.editor.VideoSegment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

class ExportRepository(private val context: Context) {

    suspend fun exportVideo(
        sourceUri: Uri,
        segments: List<VideoSegment>,
        destinationUri: Uri,
        onProgress: (Int) -> Unit,
    ) {
        require(segments.isNotEmpty()) { "There are no video clips to export." }
        val tempFile = File(context.cacheDir, "native-editor-${System.currentTimeMillis()}.mp4")
        val completion = CompletableDeferred<ExportResult>()
        val finished = AtomicBoolean(false)
        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                finished.set(true)
                completion.complete(exportResult)
            }

            override fun onError(
                composition: Composition,
                exportResult: ExportResult,
                exportException: ExportException,
            ) {
                finished.set(true)
                completion.completeExceptionally(exportException)
            }
        }

        val items = segments.mapIndexed { index, segment ->
            val clippedMediaItem = MediaItem.Builder()
                .setMediaId("clip-$index")
                .setUri(sourceUri)
                .setClipStartPositionMs(segment.sourceStartMs)
                .setClipEndPositionMs(segment.sourceEndMs)
                .build()
            EditedMediaItem.Builder(clippedMediaItem).build()
        }
        val composition = Composition.Builder(EditedMediaItemSequence(items)).build()

        val transformer = withContext(Dispatchers.Main.immediate) {
            Transformer.Builder(context).addListener(listener).build().also {
                it.start(composition, tempFile.absolutePath)
            }
        }

        try {
            val progressHolder = ProgressHolder()
            while (!completion.isCompleted) {
                coroutineContext.ensureActive()
                withContext(Dispatchers.Main.immediate) {
                    if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(progressHolder.progress.coerceIn(0, 99))
                    }
                }
                delay(250L)
            }
            completion.await()
            onProgress(99)
            withContext(Dispatchers.IO) {
                check(tempFile.isFile && tempFile.length() > 0L) { "The encoder completed without producing an MP4 file." }
                val input = tempFile.inputStream().buffered()
                val output = context.contentResolver.openOutputStream(destinationUri, "wt")
                    ?: error("Android could not open the selected export destination.")
                input.use { source ->
                    output.use { stream ->
                        val target = stream.buffered()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = source.read(buffer)
                            if (count < 0) break
                            target.write(buffer, 0, count)
                        }
                        target.flush()
                    }
                }
            }
            onProgress(100)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            if (!finished.get()) {
                withContext(Dispatchers.Main.immediate) { transformer.cancel() }
            }
            tempFile.delete()
        }
    }
}
