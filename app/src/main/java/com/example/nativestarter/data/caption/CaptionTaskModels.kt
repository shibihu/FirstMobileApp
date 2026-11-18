package com.example.nativestarter.data.caption

import org.json.JSONObject

/**
 * Backend caption-task stages (AIClipStudio `caption-jobs.js` STAGES).
 * Unknown wire values map to [UNKNOWN] so future backend stages cannot crash
 * or wedge the client; the poller keeps polling until a terminal stage.
 */
enum class CaptionStage(val wireName: String?) {
    QUEUED("queued"),
    EXTRACTING_AUDIO("extracting_audio"),
    UPLOADING_AUDIO("uploading_audio"),
    TRANSCRIBING("transcribing"),
    NORMALIZING("normalizing"),
    RENDERING("rendering"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled"),
    UNKNOWN(null);

    val isTerminal: Boolean
        get() = this == COMPLETED || this == FAILED || this == CANCELLED

    companion object {
        fun fromWire(raw: String?): CaptionStage = entries.firstOrNull { it.wireName == raw } ?: UNKNOWN
    }
}

/** Structured, client-safe failure payload from the backend task document. */
data class CaptionTaskError(
    val code: String?,
    val message: String,
    val retryable: Boolean,
    val stage: String?,
)

/**
 * Immutable snapshot of a caption task exactly as the backend exposes it via
 * POST/GET/DELETE /api/captions/jobs — the public task document contains
 * taskId, jobId, status, stage, progress, renderId, error, createdAt,
 * updatedAt, attempts and metrics. Intermediate artifacts (including the raw
 * transcript cues) are never exposed by the backend.
 */
data class CaptionTaskSnapshot(
    val taskId: String,
    val jobId: String,
    val stage: CaptionStage,
    val rawStage: String,
    val status: String,
    /** Backend-reported progress 0..100, or null when the field is absent. */
    val progress: Int?,
    val renderId: String?,
    val error: CaptionTaskError?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
) {
    val isTerminal: Boolean get() = stage.isTerminal

    companion object {
        /** Parses a task document; throws [CaptionResponseException] when the taskId is missing. */
        fun parse(json: JSONObject): CaptionTaskSnapshot {
            val taskId = json.optString("taskId").takeIf(String::isNotBlank)
                ?: throw CaptionResponseException("Caption task response did not contain a taskId.")
            val stageRaw = json.optString("stage").takeIf(String::isNotBlank)
                ?: json.optString("status").takeIf(String::isNotBlank)
                ?: ""
            val errorJson = json.optJSONObject("error")
            return CaptionTaskSnapshot(
                taskId = taskId,
                jobId = json.optString("jobId"),
                stage = CaptionStage.fromWire(stageRaw),
                rawStage = stageRaw,
                status = json.optString("status"),
                progress = json.optInt("progress", -1).takeIf { it >= 0 }?.coerceIn(0, 100),
                renderId = json.optString("renderId").takeIf(String::isNotBlank),
                error = errorJson?.let {
                    CaptionTaskError(
                        code = it.optString("code").takeIf(String::isNotBlank),
                        message = it.optString("message").ifBlank { "The subtitle job failed." },
                        retryable = it.optBoolean("retryable"),
                        stage = it.optString("stage").takeIf(String::isNotBlank),
                    )
                },
                createdAtMs = json.optLong("createdAt", 0L),
                updatedAtMs = json.optLong("updatedAt", 0L),
            )
        }

        /** Local-only snapshot used when the user cancels before the backend confirms. */
        fun localCancelled(jobId: String, taskId: String? = null): CaptionTaskSnapshot = CaptionTaskSnapshot(
            taskId = taskId.orEmpty(),
            jobId = jobId,
            stage = CaptionStage.CANCELLED,
            rawStage = "cancelled",
            status = "cancelled",
            progress = null,
            renderId = null,
            error = null,
            createdAtMs = 0L,
            updatedAtMs = 0L,
        )
    }
}

/** Thrown when a task response cannot be parsed into the documented shape. */
class CaptionResponseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * HTTP-level failure from a caption-task endpoint. [retryable] mirrors the
 * backend's flag for structured errors, defaulting to transport-style rules
 * (5xx, 408, 429) when the body carries no structured error.
 */
class CaptionTaskApiException(
    val httpStatus: Int,
    val errorCode: String?,
    message: String,
    val retryable: Boolean,
) : Exception(message)

/** A pending or resumable caption job request owned by the coordinator. */
data class CaptionJobRequest(
    val jobId: String,
    val instructions: String,
    val videoUri: String,
)
