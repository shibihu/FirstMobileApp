package com.example.nativestarter.data.caption

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private fun task(stage: CaptionStage, progress: Int? = null, renderId: String? = null) = CaptionTaskSnapshot(
    taskId = "t1",
    jobId = "j1",
    stage = stage,
    rawStage = stage.wireName ?: "unknown",
    status = stage.wireName ?: "running",
    progress = progress,
    renderId = renderId,
    error = null,
    createdAtMs = 0L,
    updatedAtMs = 0L,
)

/** Poller behaviour: stage progression, backoff, give-up, cancellation. */
class CaptionJobPollerTest {

    private val tightConfig = CaptionPollingConfig(
        initialIntervalMs = 100L,
        maxIntervalMs = 400L,
        backoffMultiplier = 2.0,
        maxConsecutiveFailures = 3,
        maxPollDurationMs = 60_000L,
    )

    private fun recordingPoller(
        config: CaptionPollingConfig = tightConfig,
        delays: MutableList<Long> = mutableListOf(),
    ) = CaptionJobPoller(config, delayFn = { delays.add(it) })

    @Test
    fun queuedToTranscribingToCompletedReturnsTerminalSnapshot() = runTest {
        val poller = recordingPoller()
        val sequence = mutableListOf(
            task(CaptionStage.QUEUED, 0),
            task(CaptionStage.TRANSCRIBING, 30),
            task(CaptionStage.COMPLETED, 100, "r-1"),
        )
        val seen = mutableListOf<CaptionTaskSnapshot>()
        val result = poller.poll(fetch = { sequence.removeAt(0) }, onSnapshot = seen::add)
        assertEquals(CaptionStage.COMPLETED, result.stage)
        assertEquals("r-1", result.renderId)
        assertEquals(3, seen.size)
        assertTrue(sequence.isEmpty())
    }

    @Test
    fun fullPipelineStagesReachCompletion() = runTest {
        val poller = recordingPoller()
        val sequence = mutableListOf(
            task(CaptionStage.EXTRACTING_AUDIO, 8),
            task(CaptionStage.UPLOADING_AUDIO, 14),
            task(CaptionStage.TRANSCRIBING, 30),
            task(CaptionStage.NORMALIZING, 68),
            task(CaptionStage.RENDERING, 74),
            task(CaptionStage.COMPLETED, 100, "r-2"),
        )
        val result = poller.poll(fetch = { sequence.removeAt(0) })
        assertEquals(CaptionStage.COMPLETED, result.stage)
    }

    @Test
    fun failedTaskStopsPolling() = runTest {
        val poller = recordingPoller()
        var calls = 0
        val result = poller.poll(fetch = {
            calls++
            task(CaptionStage.FAILED)
        })
        assertEquals(CaptionStage.FAILED, result.stage)
        assertEquals(1, calls)
    }

    @Test
    fun cancelledTaskStopsPolling() = runTest {
        val poller = recordingPoller()
        var calls = 0
        val result = poller.poll(fetch = {
            calls++
            task(CaptionStage.CANCELLED)
        })
        assertEquals(CaptionStage.CANCELLED, result.stage)
        assertEquals(1, calls)
    }

    @Test
    fun temporaryNetworkFailuresRecoverAfterBackoff() = runTest {
        val delays = mutableListOf<Long>()
        val poller = recordingPoller(delays = delays)
        val fetchErrors = mutableListOf<Exception>(
            IOException("connection reset"),
            IOException("connection reset"),
        )
        val seen = mutableListOf<CaptionTaskSnapshot>()
        val result = poller.poll(
            fetch = {
                fetchErrors.removeFirstOrNull()?.let { throw it }
                task(CaptionStage.COMPLETED, 100, "r-3")
            },
            onSnapshot = seen::add,
        )
        assertEquals(CaptionStage.COMPLETED, result.stage)
        assertEquals(1, seen.size)
        assertEquals(listOf(100L, 200L), delays)
    }

    @Test
    fun backoffIntervalsGrowAndResetAfterSuccess() = runTest {
        val delays = mutableListOf<Long>()
        val poller = recordingPoller(
            tightConfig.copy(maxConsecutiveFailures = 10),
            delays,
        )
        var fetchCount = 0
        val result = poller.poll(fetch = {
            fetchCount++
            // 3 failures, 1 success, 2 failures, then completion.
            when {
                fetchCount <= 3 -> throw IOException("down")
                fetchCount == 4 -> task(CaptionStage.QUEUED, 0)
                fetchCount <= 6 -> throw IOException("down again")
                else -> task(CaptionStage.COMPLETED, 100, "r-4")
            }
        })
        assertEquals(CaptionStage.COMPLETED, result.stage)
        // Failures: 100, 200, 400 (capped) before the success; the success
        // itself waits the reset base interval; then 100, 200 before the
        // final success stops the loop.
        assertEquals(listOf(100L, 200L, 400L, 100L, 100L, 200L), delays)
    }

    @Test
    fun consecutiveFailuresBeyondLimitGiveUp() = runTest {
        val poller = recordingPoller()
        var calls = 0
        val thrown = runCatching {
            poller.poll(fetch = {
                calls++
                throw IOException("down")
            })
        }.exceptionOrNull()
        assertTrue(thrown is IOException)
        assertTrue(thrown!!.message!!.contains("down"))
    }

    @Test
    fun nonRetryableApiErrorAbortsImmediately() = runTest {
        val poller = recordingPoller()
        var calls = 0
        val thrown = runCatching {
            poller.poll(fetch = {
                calls++
                throw CaptionTaskApiException(400, "BAD_JOB_ID", "Invalid job id.", retryable = false)
            })
        }.exceptionOrNull()
        assertTrue(thrown is CaptionTaskApiException)
        assertEquals(400, (thrown as CaptionTaskApiException).httpStatus)
        assertEquals(1, calls)
    }

    @Test
    fun retryableApiErrorIsRetried() = runTest {
        val poller = recordingPoller()
        var failures = 0
        val result = poller.poll(fetch = {
            if (failures < 2) {
                failures++
                throw CaptionTaskApiException(503, "BUSY", "The server is busy.", retryable = true)
            }
            task(CaptionStage.COMPLETED, 100)
        })
        assertEquals(CaptionStage.COMPLETED, result.stage)
        assertEquals(2, failures)
    }

    @Test
    fun coroutineCancellationStopsTheLoop() = runTest {
        // Default (virtual-time) delay so the loop actually suspends between
        // fetches and cancellation can be observed at the await point.
        val poller = CaptionJobPoller(tightConfig)
        var calls = 0
        val job = launch {
            poller.poll(fetch = {
                calls++
                task(CaptionStage.TRANSCRIBING, 30)
            })
        }
        runCurrent()
        val callsAfterStart = calls
        job.cancelAndJoin()
        assertTrue(callsAfterStart >= 1)
        assertTrue(!job.isActive)
    }

    @Test
    fun cancellationExceptionFromFetchIsNeverSwallowed() = runTest {
        val poller = CaptionJobPoller(tightConfig)
        try {
            poller.poll(fetch = { throw CancellationException("scope torn down") })
        } catch (expected: CancellationException) {
            return@runTest
        }
        throw AssertionError("CancellationException must propagate, not be retried")
    }

    @Test
    fun unknownStageKeepsPollingUntilTerminal() = runTest {
        val poller = recordingPoller()
        val sequence = mutableListOf(
            task(CaptionStage.UNKNOWN),
            task(CaptionStage.UNKNOWN),
            task(CaptionStage.COMPLETED, 100, "r-5"),
        )
        val result = poller.poll(fetch = { sequence.removeAt(0) })
        assertEquals(CaptionStage.COMPLETED, result.stage)
        assertEquals(0, sequence.size)
    }

    @Test
    fun concurrentPollersDoNotShareState() = runTest {
        val pollerA = recordingPoller()
        val pollerB = recordingPoller()
        val a = async { pollerA.poll(fetch = { task(CaptionStage.COMPLETED, 100, "a") }) }
        val b = async { pollerB.poll(fetch = { task(CaptionStage.FAILED) }) }
        assertEquals(CaptionStage.COMPLETED, a.await().stage)
        assertEquals(CaptionStage.FAILED, b.await().stage)
    }
}
