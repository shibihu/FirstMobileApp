package com.example.nativestarter.data.caption

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * User-facing strings for caption jobs. Messages never include stack traces,
 * secrets or raw backend internals; structured backend error messages are
 * passed through because the server already sanitises them.
 */
object CaptionJobMessages {

    /** Short label for the stage currently reported by the backend. */
    fun stageLabel(stage: CaptionStage): String = when (stage) {
        CaptionStage.QUEUED -> "Waiting in the caption queue…"
        CaptionStage.EXTRACTING_AUDIO -> "Extracting the audio track…"
        CaptionStage.UPLOADING_AUDIO -> "Uploading audio to the speech service…"
        CaptionStage.TRANSCRIBING -> "Transcribing speech…"
        CaptionStage.NORMALIZING -> "Formatting subtitle cues…"
        CaptionStage.RENDERING -> "Burning captions into the video…"
        CaptionStage.COMPLETED -> "Automatic subtitles are ready."
        CaptionStage.FAILED -> "Automatic subtitles failed."
        CaptionStage.CANCELLED -> "Automatic subtitles were cancelled."
        CaptionStage.UNKNOWN -> "Waiting for the backend to report the next stage…"
    }

    /** True when the failure is worth offering a Retry action for. */
    fun isRetryable(error: Throwable): Boolean = when (error) {
        is CaptionTaskApiException ->
            error.retryable || error.httpStatus >= 500 || error.httpStatus == 408 || error.httpStatus == 429
        is CaptionResponseException -> true
        is IOException -> true
        else -> false
    }

    /** True for task-level failures whose backend error is marked retryable. */
    fun isTaskRetryable(error: CaptionTaskError?): Boolean = error?.retryable == true

    /** Human-readable explanation for a coordinator/API level failure. */
    fun forThrowable(error: Throwable): String = when (error) {
        is CaptionTaskApiException -> when {
            error.httpStatus == 401 || error.httpStatus == 403 ->
                "The backend rejected the access token. Check Settings."
            error.httpStatus == 404 ->
                "The subtitle job could not be found on the backend. Submit it again."
            error.httpStatus in 500..599 || error.httpStatus == 408 || error.httpStatus == 429 ->
                error.message?.ifBlank { "The backend is busy or temporarily unavailable. Retry shortly." }
                    ?: "The backend is busy or temporarily unavailable. Retry shortly."
            else -> error.message?.ifBlank { "The backend rejected the subtitle request." }
                ?: "The backend rejected the subtitle request."
        }
        is SocketTimeoutException -> "The backend request timed out. Check the server and retry."
        is UnknownHostException, is ConnectException ->
            "Could not reach the backend. Check its URL and that the server is running."
        is IOException -> "Network problem while talking to the backend. Check the connection and retry."
        is CaptionResponseException -> "The backend returned an unexpected subtitle response."
        else -> error.message.orEmpty().take(300).ifBlank { "The subtitle job failed unexpectedly." }
            .ifBlank { "The subtitle job failed unexpectedly." }
    }

    /** Human-readable explanation for a task that reached the failed stage. */
    fun forTaskError(error: CaptionTaskError?): String = when (error?.code) {
        null -> "The subtitle job failed without a backend explanation."
        else -> error.message
    }
}
