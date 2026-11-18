package com.example.nativestarter.viewmodel

import com.example.nativestarter.data.caption.CaptionJobRecord
import com.example.nativestarter.data.caption.CaptionJobRecordStore
import com.example.nativestarter.data.caption.CaptionJobRequest
import com.example.nativestarter.data.caption.CaptionPollingConfig
import com.example.nativestarter.data.caption.CaptionStage
import com.example.nativestarter.data.caption.CaptionTaskApiException
import com.example.nativestarter.data.caption.CaptionTaskError
import com.example.nativestarter.data.caption.CaptionTaskSnapshot
import com.example.nativestarter.data.caption.CaptionTasksApi
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun snapshot(
    taskId: String,
    stage: CaptionStage,
    progress: Int? = null,
    renderId: String? = null,
    error: CaptionTaskError? = null,
    jobId: String = "job-1",
) = CaptionTaskSnapshot(
    taskId = taskId,
    jobId = jobId,
    stage = stage,
    rawStage = stage.wireName ?: "unknown",
    status = stage.wireName ?: "running",
    progress = progress,
    renderId = renderId,
    error = error,
    createdAtMs = 0L,
    updatedAtMs = 0L,
)

private class FakeRecordStore : CaptionJobRecordStore {
    var record: CaptionJobRecord? = null
    override fun load(): CaptionJobRecord? = record
    override fun save(record: CaptionJobRecord) {
        this.record = record
    }
    override fun clear() {
        record = null
    }
}

/** Scripted fake: responses are queued per taskId; failures are queued too. */
private class FakeCaptionTasksApi : CaptionTasksApi {

    data class Submission(val jobId: String, val instructions: String, val idempotencyKey: String)
    data class CancelCall(val taskId: String, val jobId: String)

    val submissions = mutableListOf<Submission>()
    val cancelCalls = mutableListOf<CancelCall>()
    var submitError: Throwable? = null
    var submitResult: CaptionTaskSnapshot = snapshot("task-1", CaptionStage.QUEUED)
    private val statusQueue = ArrayDeque<Any>() // CaptionTaskSnapshot or Throwable
    private var cancelScript: (suspend (String, String) -> Unit)? = null

    fun enqueueStatus(response: Any) {
        statusQueue.add(response)
    }

    fun onCancel(block: suspend (taskId: String, jobId: String) -> Unit) {
        cancelScript = block
    }

    override suspend fun submitCaptionJob(jobId: String, instructions: String, idempotencyKey: String): CaptionTaskSnapshot {
        submissions += Submission(jobId, instructions, idempotencyKey)
        submitError?.let { error -> throw error }
        return submitResult
    }

    override suspend fun getCaptionTask(taskId: String, jobId: String): CaptionTaskSnapshot {
        if (statusQueue.isEmpty()) throw AssertionError("Unexpected status request for $taskId")
        return when (val next = statusQueue.removeFirst()) {
            is CaptionTaskSnapshot -> next
            is Throwable -> throw next
            else -> throw AssertionError("bad queue entry")
        }
    }

    override suspend fun cancelCaptionTask(taskId: String, jobId: String): CaptionTaskSnapshot {
        cancelCalls += CancelCall(taskId, jobId)
        cancelScript?.invoke(taskId, jobId)
        return snapshot(taskId, CaptionStage.CANCELLED, jobId = jobId)
    }
}

/** Coordinator behaviour: submission, resume, polling, cancellation, retries. */
class CaptionJobCoordinatorTest {

    private val config = CaptionPollingConfig(
        initialIntervalMs = 50L,
        maxIntervalMs = 100L,
        backoffMultiplier = 2.0,
        maxConsecutiveFailures = 3,
        maxPollDurationMs = 30_000L,
    )
    private var nextKey = 0
    private val idGenerator = { "key-${nextKey++}" }
    private val request = CaptionJobRequest(jobId = "job-1", instructions = "Thai", videoUri = "file://v.mp4")

    @Test
    fun queuedTranscribingCompletedProducesTerminalSnapshotAndClearsRecord() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED, 0)
        api.enqueueStatus(snapshot("task-1", CaptionStage.TRANSCRIBING, 30))
        api.enqueueStatus(snapshot("task-1", CaptionStage.COMPLETED, 100, "r-1"))
        val store = FakeRecordStore()
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = store,
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val result = coordinator.submitOrAwait(request)
        assertEquals(CaptionStage.COMPLETED, result.stage)
        assertEquals("r-1", result.renderId)
        assertEquals(1, api.submissions.size)
        assertNull(store.record)
        val state = coordinator.uiState.value
        assertEquals(CaptionStage.COMPLETED, state?.stage)
        assertTrue(state?.finished == true)
    }

    @Test
    fun extractingAudioThroughRenderingCompletes() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        listOf(
            CaptionStage.EXTRACTING_AUDIO,
            CaptionStage.UPLOADING_AUDIO,
            CaptionStage.TRANSCRIBING,
            CaptionStage.NORMALIZING,
            CaptionStage.RENDERING,
            CaptionStage.COMPLETED,
        ).forEachIndexed { index, stage ->
            api.enqueueStatus(
                snapshot("task-1", stage, if (stage == CaptionStage.COMPLETED) 100 else index * 10, "r-1".takeIf { stage == CaptionStage.COMPLETED }),
            )
        }
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val result = coordinator.submitOrAwait(request)
        assertEquals(CaptionStage.COMPLETED, result.stage)
    }

    @Test
    fun concurrentCallersAwaitTheSameSubmissionWithoutDuplicates() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        api.enqueueStatus(snapshot("task-1", CaptionStage.TRANSCRIBING, 30))
        api.enqueueStatus(snapshot("task-1", CaptionStage.COMPLETED, 100, "r-1"))
        val coordinator = CaptionJobCoordinator(
            scope = this,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val first = launch { coordinator.submitOrAwait(request) }
        runCurrent()
        // The first caller submitted once and is suspended inside the poll loop.
        assertEquals(1, api.submissions.size)
        // A concurrent caller awaits the same in-flight job.
        val secondResult = coordinator.submitOrAwait(request)
        assertEquals(CaptionStage.COMPLETED, secondResult.stage)
        assertEquals(1, api.submissions.size)
        first.join()
    }

    @Test
    fun pendingRecordWithTaskIdResumesPollingWithoutResubmitting() = runTest {
        val api = FakeCaptionTasksApi()
        api.enqueueStatus(snapshot("task-9", CaptionStage.TRANSCRIBING, 30))
        api.enqueueStatus(snapshot("task-9", CaptionStage.COMPLETED, 100, "r-2"))
        val store = FakeRecordStore().apply {
            record = CaptionJobRecord("job-1", "task-9", "key-old", "Thai", "file://v.mp4")
        }
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = store,
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val result = coordinator.resumePending()
        assertNotNull(result)
        assertEquals(CaptionStage.COMPLETED, result?.stage)
        assertEquals(0, api.submissions.size)
        assertNull(store.record)
    }

    @Test
    fun blankTaskIdAfterCrashResubmitsWithStoredIdempotencyKey() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        api.enqueueStatus(snapshot("task-1", CaptionStage.COMPLETED, 100, "r-1"))
        val store = FakeRecordStore().apply {
            record = CaptionJobRecord("job-1", "", "key-crash", "Thai", "file://v.mp4")
        }
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = store,
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        coordinator.resumePending()
        assertEquals(1, api.submissions.size)
        assertEquals("key-crash", api.submissions.single().idempotencyKey)
    }

    @Test
    fun failedTaskSurfacesBackendErrorAndRetryableFlag() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        api.enqueueStatus(
            snapshot(
                "task-1",
                CaptionStage.FAILED,
                error = CaptionTaskError("STT_API_KEY_MISSING", "No speech provider key is configured on the server.", retryable = false, stage = "transcribing"),
            ),
        )
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val result = coordinator.submitOrAwait(request)
        assertEquals(CaptionStage.FAILED, result.stage)
        assertEquals("STT_API_KEY_MISSING", result.error?.code)
        val state = coordinator.uiState.value
        assertNotNull(state)
        assertTrue(state!!.finished)
        assertFalse(state.retryable)
        assertEquals("No speech provider key is configured on the server.", state.message)
    }

    @Test
    fun retryableTaskFailureShowsRetryAction() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        api.enqueueStatus(
            snapshot(
                "task-1",
                CaptionStage.FAILED,
                error = CaptionTaskError("STT_PROVIDER_UNREACHABLE", "The speech service is unreachable right now.", retryable = true, stage = "transcribing"),
            ),
        )
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        coordinator.submitOrAwait(request)
        assertTrue(coordinator.uiState.value?.retryable == true)
    }

    @Test
    fun networkGiveUpEmitsRetryableErrorStateAndKeepsRecord() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        repeat(3) { api.enqueueStatus(IOException("connection reset")) }
        val store = FakeRecordStore()
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = store,
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val thrown = runCatching { coordinator.submitOrAwait(request) }.exceptionOrNull()
        assertTrue(thrown is IOException)
        val state = coordinator.uiState.value
        assertNotNull(state)
        assertTrue(state!!.retryable)
        assertTrue(state.message.contains("Network problem"))
        // The record is kept so Retry can resume or resubmit with one key.
        assertNotNull(store.record)
        assertEquals(1, coordinator.pendingRequest()?.let { 1 } ?: 0)
    }

    @Test
    fun nonRetryableSubmitErrorAbortsAndMarksStateNonRetryable() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitError = CaptionTaskApiException(404, "UNKNOWN_JOB", "unknown job — upload a clip first", retryable = false)
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val thrown = runCatching { coordinator.submitOrAwait(request) }.exceptionOrNull()
        assertTrue(thrown is CaptionTaskApiException)
        val state = coordinator.uiState.value
        assertNotNull(state)
        assertFalse(state!!.retryable)
        assertTrue(state.message.contains("could not be found"))
    }

    @Test
    fun userCancelStopsPollingAndSendsBackendDelete() = runTest(UnconfinedTestDispatcher()) {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        api.enqueueStatus(snapshot("task-1", CaptionStage.TRANSCRIBING, 30))
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val awaited = CompletableDeferred<CaptionTaskSnapshot>()
        backgroundScope.launch {
            try {
                awaited.complete(coordinator.submitOrAwait(request))
            } catch (failure: Throwable) {
                if (failure !is CancellationException) awaited.completeExceptionally(failure)
            }
        }
        runCurrent()
        coordinator.cancelByUser()
        runCurrent()
        assertEquals(1, api.cancelCalls.size)
        assertEquals("task-1", api.cancelCalls.single().taskId)
        val result = awaited.await()
        assertEquals(CaptionStage.CANCELLED, result.stage)
        val state = coordinator.uiState.value
        assertTrue(state!!.finished)
        assertFalse(state.cancellable)
    }

    @Test
    fun cancelWithFailingBackendStillLeavesRunningState() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        api.enqueueStatus(snapshot("task-1", CaptionStage.TRANSCRIBING, 30))
        api.onCancel { _, _ -> throw IOException("connection reset") }
        val coordinator = CaptionJobCoordinator(
            scope = this,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val awaited = CompletableDeferred<CaptionTaskSnapshot>()
        val waiter = launch {
            try {
                awaited.complete(coordinator.submitOrAwait(request))
            } catch (failure: Throwable) {
                if (failure !is CancellationException) awaited.completeExceptionally(failure)
            }
        }
        runCurrent()
        coordinator.cancelByUser()
        advanceUntilIdle()
        assertEquals(3, api.cancelCalls.size) // retried, then gave up
        val result = awaited.await()
        assertEquals(CaptionStage.CANCELLED, result.stage)
        assertTrue(coordinator.uiState.value!!.message.contains("did not confirm"))
        waiter.join()
    }

    @Test
    fun unexpectedBackendStateMapsToUnknownAndPollsOn() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        api.enqueueStatus(snapshot("task-1", CaptionStage.fromWire("laser_etching"), 42))
        api.enqueueStatus(snapshot("task-1", CaptionStage.COMPLETED, 100, "r-3"))
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        val observedStages = mutableListOf<CaptionStage?>()
        val collector = backgroundScope.launch {
            coordinator.uiState.collect { state -> observedStages.add(state?.stage) }
        }
        val result = coordinator.submitOrAwait(request)
        assertEquals(CaptionStage.COMPLETED, result.stage)
        assertTrue(observedStages.contains(CaptionStage.UNKNOWN))
        assertEquals(CaptionStage.COMPLETED, observedStages.last())
        collector.cancel()
    }

    @Test
    fun scopeCancellationStopsEverything() = runTest {
        val api = FakeCaptionTasksApi()
        api.submitResult = snapshot("task-1", CaptionStage.QUEUED)
        // Keep the poll loop alive past the submission so isActive is true.
        api.enqueueStatus(snapshot("task-1", CaptionStage.TRANSCRIBING, 30))
        val coordinator = CaptionJobCoordinator(
            scope = backgroundScope,
            api = api,
            recordStore = FakeRecordStore(),
            pollingConfig = config,
            idGenerator = idGenerator,
        )
        backgroundScope.launch { runCatching { coordinator.submitOrAwait(request) } }
        runCurrent()
        assertTrue(coordinator.isActive)
        backgroundScope.coroutineContext.cancelChildren()
        assertTrue(!coordinator.isActive)
    }
}
