package com.example.nativestarter.viewmodel

import com.example.nativestarter.data.caption.CaptionJobMessages
import com.example.nativestarter.data.caption.CaptionJobPoller
import com.example.nativestarter.data.caption.CaptionJobRecord
import com.example.nativestarter.data.caption.CaptionJobRecordStore
import com.example.nativestarter.data.caption.CaptionJobRequest
import com.example.nativestarter.data.caption.CaptionJobUiState
import com.example.nativestarter.data.caption.CaptionPollingConfig
import com.example.nativestarter.data.caption.CaptionStage
import com.example.nativestarter.data.caption.CaptionTaskApiException
import com.example.nativestarter.data.caption.CaptionTaskSnapshot
import com.example.nativestarter.data.caption.CaptionTasksApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * Single-flight owner of one asynchronous automatic-subtitle job.
 *
 * Responsibilities:
 *  - submit POST /api/captions/jobs once per user action (idempotency key is
 *    persisted before the request so a crash mid-submission can never create
 *    two tasks for one action),
 *  - resume monitoring of an already-persisted task instead of resubmitting,
 *  - run exactly one poll loop and never overlap a second one,
 *  - expose stage/progress snapshots for the UI,
 *  - perform real backend cancellation (DELETE) when the user cancels,
 *  - clear the persisted record on terminal states.
 *
 * The coordinator never touches Android UI classes, so it is unit-testable.
 */
class CaptionJobCoordinator(
    private val scope: CoroutineScope,
    private val api: CaptionTasksApi,
    private val recordStore: CaptionJobRecordStore,
    private val pollingConfig: CaptionPollingConfig = CaptionPollingConfig(),
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {

    private val _uiState = MutableStateFlow<CaptionJobUiState?>(null)
    val uiState: StateFlow<CaptionJobUiState?> = _uiState.asStateFlow()

    private val startMutex = Mutex()
    private var pollJob: Job? = null
    private var completion = CompletableDeferred<CaptionTaskSnapshot>()
    private var lastRequest: CaptionJobRequest? = null

    /** True while a submission or poll loop is in flight. */
    val isActive: Boolean
        get() = pollJob?.isActive == true

    /** Parameters of the last submission, kept for the Retry action. */
    fun lastSubmission(): CaptionJobRequest? = lastRequest

    /**
     * Submits [request] (or resumes the persisted task it refers to) and
     * suspends until a terminal state. Concurrent callers while a job is
     * running await the same completion instead of starting a duplicate
     * submission or poll loop.
     */
    suspend fun submitOrAwait(request: CaptionJobRequest): CaptionTaskSnapshot {
        if (isActive) return completion.await()
        startMutex.withLock {
            if (isActive) return completion.await()
            completion = CompletableDeferred()
            lastRequest = request
            pollJob = scope.launch {
                try {
                    val terminal = runToTerminal(request)
                    completion.complete(terminal)
                } catch (cancelled: CancellationException) {
                    // User cancellation completes with a local cancelled snapshot;
                    // scope teardown (view model cleared) has no listener left.
                    if (!completion.isCompleted) {
                        completion.complete(CaptionTaskSnapshot.localCancelled(request.jobId))
                    }
                } catch (failure: Throwable) {
                    // Keep the persisted record so Retry can resume or resubmit.
                    _uiState.value = CaptionJobUiState(
                        message = CaptionJobMessages.forThrowable(failure),
                        retryable = CaptionJobMessages.isRetryable(failure),
                        finished = true,
                    )
                    if (!completion.isCompleted) completion.completeExceptionally(failure)
                }
            }
        }
        return completion.await()
    }

    /**
     * Resumes monitoring of a persisted, non-terminal task after process death
     * or app restart. Returns null when nothing is stored. Never resubmits a
     * task that already has a taskId; a blank taskId (crash during submission)
     * resubmits with the stored idempotency key so the backend dedupes.
     */
    suspend fun resumePending(): CaptionTaskSnapshot? {
        val record = recordStore.load() ?: return null
        if (isActive) return null
        return submitOrAwait(
            CaptionJobRequest(
                jobId = record.jobId,
                instructions = record.instructions,
                videoUri = record.videoUri,
            ),
        )
    }

    /** Parameters of the persisted (possibly resumable) request, if any. */
    fun pendingRequest(): CaptionJobRequest? = recordStore.load()?.let { record ->
        CaptionJobRequest(record.jobId, record.instructions, record.videoUri)
    }

    /**
     * Stops the poll loop first, then asks the backend to cancel the task.
     * The DELETE request is retried a few times against transient failures;
     * the UI leaves the running state immediately regardless of its outcome.
     */
    fun cancelByUser() {
        val running = pollJob?.takeIf(Job::isActive) ?: return
        running.cancel()
        scope.launch {
            val record = recordStore.load()
            var backendConfirmed = false
            var lastFailure: Throwable? = null
            if (record != null && record.taskId.isNotBlank()) {
                var attempt = 0
                while (!backendConfirmed && attempt < CANCEL_ATTEMPTS) {
                    attempt++
                    try {
                        api.cancelCaptionTask(record.taskId, record.jobId)
                        backendConfirmed = true
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Throwable) {
                        lastFailure = failure
                        if (attempt < CANCEL_ATTEMPTS) delay(CANCEL_RETRY_BASE_DELAY_MS * attempt)
                    }
                }
            }
            recordStore.clear()
            lastRequest = null
            _uiState.value = if (backendConfirmed) {
                CaptionJobUiState(
                    stage = CaptionStage.CANCELLED,
                    message = "Automatic subtitles were cancelled.",
                    cancellable = false,
                    retryable = false,
                    finished = true,
                )
            } else {
                CaptionJobUiState(
                    stage = CaptionStage.CANCELLED,
                    message = "Automatic subtitles were cancelled locally, but the backend did not confirm " +
                        "the cancellation${lastFailure?.let { ": ${CaptionJobMessages.forThrowable(it)}" } ?: "."}",
                    cancellable = false,
                    retryable = false,
                    finished = true,
                )
            }
            if (!completion.isCompleted) {
                completion.complete(CaptionTaskSnapshot.localCancelled(record?.jobId.orEmpty(), record?.taskId))
            }
        }
    }

    /** Removes the finished card from the UI. */
    fun clearUi() {
        _uiState.value = null
    }

    private suspend fun runToTerminal(request: CaptionJobRequest): CaptionTaskSnapshot {
        _uiState.value = CaptionJobUiState(
            stage = CaptionStage.QUEUED,
            message = CaptionJobMessages.stageLabel(CaptionStage.QUEUED),
            cancellable = true,
        )
        val stored = recordStore.load()
        val matchesStored = stored != null &&
            stored.jobId == request.jobId &&
            (request.videoUri.isBlank() || stored.videoUri == request.videoUri)
        val idempotencyKey: String
        val resumableTaskId: String?
        if (matchesStored) {
            idempotencyKey = stored.idempotencyKey.takeIf(String::isNotBlank) ?: idGenerator()
            resumableTaskId = stored.taskId.takeIf(String::isNotBlank)
            if (resumableTaskId == null || idempotencyKey != stored.idempotencyKey) {
                // Persist the key BEFORE the submission request so a crash
                // mid-submission can never create two tasks for one action.
                recordStore.save(
                    CaptionJobRecord(
                        jobId = request.jobId,
                        taskId = resumableTaskId.orEmpty(),
                        idempotencyKey = idempotencyKey,
                        instructions = request.instructions,
                        videoUri = request.videoUri,
                    ),
                )
            }
        } else {
            idempotencyKey = idGenerator()
            resumableTaskId = null
            recordStore.save(
                CaptionJobRecord(
                    jobId = request.jobId,
                    taskId = "",
                    idempotencyKey = idempotencyKey,
                    instructions = request.instructions,
                    videoUri = request.videoUri,
                ),
            )
        }

        val snapshot = if (resumableTaskId != null) {
            // Resume: ask the backend for the current state; never resubmit.
            api.getCaptionTask(resumableTaskId, request.jobId)
        } else {
            val created = api.submitCaptionJob(request.jobId, request.instructions, idempotencyKey)
            recordStore.save(
                CaptionJobRecord(request.jobId, created.taskId, idempotencyKey, request.instructions, request.videoUri),
            )
            created
        }
        if (snapshot.isTerminal) {
            recordStore.clear()
            emitTerminal(snapshot)
            return snapshot
        }
        return pollUntilTerminal(request, snapshot)
    }

    private suspend fun pollUntilTerminal(request: CaptionJobRequest, first: CaptionTaskSnapshot): CaptionTaskSnapshot {
        val poller = CaptionJobPoller(pollingConfig)
        val terminal = poller.poll(
            fetch = { api.getCaptionTask(first.taskId, request.jobId) },
        ) { snapshot ->
            _uiState.value = CaptionJobUiState(
                stage = snapshot.stage,
                message = when {
                    snapshot.stage == CaptionStage.FAILED ->
                        CaptionJobMessages.forTaskError(snapshot.error)
                    else -> CaptionJobMessages.stageLabel(snapshot.stage)
                },
                progress = snapshot.progress,
                cancellable = true,
            )
        }
        recordStore.clear()
        emitTerminal(terminal)
        return terminal
    }

    private fun emitTerminal(snapshot: CaptionTaskSnapshot) {
        lastRequest = null
        _uiState.value = when (snapshot.stage) {
            CaptionStage.FAILED -> CaptionJobUiState(
                stage = CaptionStage.FAILED,
                message = CaptionJobMessages.forTaskError(snapshot.error),
                retryable = CaptionJobMessages.isTaskRetryable(snapshot.error),
                finished = true,
            )
            CaptionStage.CANCELLED -> CaptionJobUiState(
                stage = CaptionStage.CANCELLED,
                message = CaptionJobMessages.stageLabel(CaptionStage.CANCELLED),
                finished = true,
            )
            else -> CaptionJobUiState(
                stage = snapshot.stage,
                message = CaptionJobMessages.stageLabel(snapshot.stage),
                progress = snapshot.progress,
                finished = true,
            )
        }
    }

    private companion object {
        const val CANCEL_ATTEMPTS = 3
        const val CANCEL_RETRY_BASE_DELAY_MS = 500L
    }
}
