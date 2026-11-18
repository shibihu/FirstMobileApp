package com.example.nativestarter.data.caption

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Error mapping for user-facing caption job messages. */
class CaptionJobMessagesTest {

    @Test
    fun stageLabelsCoverEveryKnownStage() {
        assertEquals("Waiting in the caption queue…", CaptionJobMessages.stageLabel(CaptionStage.QUEUED))
        assertEquals("Extracting the audio track…", CaptionJobMessages.stageLabel(CaptionStage.EXTRACTING_AUDIO))
        assertEquals("Uploading audio to the speech service…", CaptionJobMessages.stageLabel(CaptionStage.UPLOADING_AUDIO))
        assertEquals("Transcribing speech…", CaptionJobMessages.stageLabel(CaptionStage.TRANSCRIBING))
        assertEquals("Formatting subtitle cues…", CaptionJobMessages.stageLabel(CaptionStage.NORMALIZING))
        assertEquals("Burning captions into the video…", CaptionJobMessages.stageLabel(CaptionStage.RENDERING))
        assertEquals("Automatic subtitles are ready.", CaptionJobMessages.stageLabel(CaptionStage.COMPLETED))
        assertEquals("Automatic subtitles were cancelled.", CaptionJobMessages.stageLabel(CaptionStage.CANCELLED))
    }

    @Test
    fun unknownStageHasSafeFallbackLabel() {
        assertEquals(
            "Waiting for the backend to report the next stage…",
            CaptionJobMessages.stageLabel(CaptionStage.UNKNOWN),
        )
    }

    @Test
    fun authFailuresNeverExposeTheToken() {
        val message = CaptionJobMessages.forThrowable(
            CaptionTaskApiException(401, "AUTH", "wrong access token", retryable = false),
        )
        assertEquals("The backend rejected the access token. Check Settings.", message)
        assertFalse(message.contains("token="))
        assertFalse(CaptionJobMessages.isRetryable(CaptionTaskApiException(401, "AUTH", "denied", retryable = false)))
    }

    @Test
    fun forbiddenRequestsMapToTokenGuidance() {
        val message = CaptionJobMessages.forThrowable(
            CaptionTaskApiException(403, "FORBIDDEN", "forbidden", retryable = false),
        )
        assertEquals("The backend rejected the access token. Check Settings.", message)
    }

    @Test
    fun notFoundExplainsResubmission() {
        val message = CaptionJobMessages.forThrowable(
            CaptionTaskApiException(404, "CAPTION_TASK_NOT_FOUND", "Caption task not found.", retryable = true),
        )
        assertEquals("The subtitle job could not be found on the backend. Submit it again.", message)
    }

    @Test
    fun transientStatusesAreRetryable() {
        for (status in intArrayOf(408, 429, 500, 502, 503)) {
            assertTrue(
                "HTTP $status must be retryable",
                CaptionJobMessages.isRetryable(CaptionTaskApiException(status, "X", "busy", retryable = false)),
            )
        }
    }

    @Test
    fun timeoutAndDnsFailuresProduceNetworkGuidance() {
        assertTrue(
            CaptionJobMessages.forThrowable(SocketTimeoutException("connect timed out"))
                .contains("timed out"),
        )
        assertEquals(
            "Could not reach the backend. Check its URL and that the server is running.",
            CaptionJobMessages.forThrowable(UnknownHostException("Unable to resolve host")),
        )
        assertTrue(CaptionJobMessages.isRetryable(SocketTimeoutException("read timed out")))
        assertTrue(CaptionJobMessages.isRetryable(IOException("broken pipe")))
    }

    @Test
    fun invalidResponsesAreRetryableWithSafeText() {
        assertTrue(CaptionJobMessages.isRetryable(CaptionResponseException("no taskId")))
        assertEquals(
            "The backend returned an unexpected subtitle response.",
            CaptionJobMessages.forThrowable(CaptionResponseException("no taskId")),
        )
    }

    @Test
    fun taskErrorsPassThroughSanitisedBackendMessage() {
        assertEquals(
            "This version has no audio track to transcribe.",
            CaptionJobMessages.forTaskError(
                CaptionTaskError("NO_AUDIO", "This version has no audio track to transcribe.", retryable = false, stage = "queued"),
            ),
        )
        assertEquals(
            "The subtitle job failed without a backend explanation.",
            CaptionJobMessages.forTaskError(null),
        )
        assertFalse(CaptionJobMessages.isTaskRetryable(CaptionTaskError("X", "m", retryable = false, stage = null)))
        assertTrue(CaptionJobMessages.isTaskRetryable(CaptionTaskError("X", "m", retryable = true, stage = null)))
    }
}
