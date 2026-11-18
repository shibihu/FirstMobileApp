package com.example.nativestarter.data.caption

import android.content.Context

/** Persisted caption-task reference used to resume monitoring after process death. */
data class CaptionJobRecord(
    val jobId: String,
    /** Blank when the process died between submitting and reading the response. */
    val taskId: String,
    val idempotencyKey: String,
    val instructions: String,
    val videoUri: String,
)

/**
 * Storage for the single active caption job. Records are written before the
 * submission request is sent (with a blank taskId) and cleared on any terminal
 * state, so a crash can always be recovered through the idempotency key or the
 * stored taskId without creating a duplicate job.
 */
interface CaptionJobRecordStore {
    fun load(): CaptionJobRecord?
    fun save(record: CaptionJobRecord)
    fun clear()
}

/** SharedPreferences-backed implementation; one record at a time. */
class SharedPreferencesCaptionJobRecordStore(context: Context) : CaptionJobRecordStore {

    private val preferences = context.getSharedPreferences("caption_jobs", Context.MODE_PRIVATE)

    override fun load(): CaptionJobRecord? {
        val jobId = preferences.getString(KEY_JOB_ID, null)?.takeIf(String::isNotBlank) ?: return null
        return CaptionJobRecord(
            jobId = jobId,
            taskId = preferences.getString(KEY_TASK_ID, null).orEmpty(),
            idempotencyKey = preferences.getString(KEY_IDEMPOTENCY_KEY, null).orEmpty(),
            instructions = preferences.getString(KEY_INSTRUCTIONS, null).orEmpty(),
            videoUri = preferences.getString(KEY_VIDEO_URI, null).orEmpty(),
        )
    }

    override fun save(record: CaptionJobRecord) {
        preferences.edit()
            .putString(KEY_JOB_ID, record.jobId)
            .putString(KEY_TASK_ID, record.taskId)
            .putString(KEY_IDEMPOTENCY_KEY, record.idempotencyKey)
            .putString(KEY_INSTRUCTIONS, record.instructions)
            .putString(KEY_VIDEO_URI, record.videoUri)
            .apply()
    }

    override fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_TASK_ID = "task_id"
        const val KEY_IDEMPOTENCY_KEY = "idempotency_key"
        const val KEY_INSTRUCTIONS = "instructions"
        const val KEY_VIDEO_URI = "video_uri"
    }
}
