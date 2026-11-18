package com.example.nativestarter.data.caption

/** Typed API surface for the asynchronous caption jobs endpoints. */
interface CaptionTasksApi {
    /** POST /api/captions/jobs — 202 with the persisted task document. */
    suspend fun submitCaptionJob(jobId: String, instructions: String, idempotencyKey: String): CaptionTaskSnapshot

    /** GET /api/captions/jobs/:taskId?id=:jobId — current task document. */
    suspend fun getCaptionTask(taskId: String, jobId: String): CaptionTaskSnapshot

    /** DELETE /api/captions/jobs/:taskId?id=:jobId — cancels a queued/running task. */
    suspend fun cancelCaptionTask(taskId: String, jobId: String): CaptionTaskSnapshot
}
