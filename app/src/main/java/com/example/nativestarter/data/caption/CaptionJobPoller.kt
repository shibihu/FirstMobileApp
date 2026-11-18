package com.example.nativestarter.data.caption

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * Tuning for [CaptionJobPoller]. The backend rate-limits every
 * /api/captions/jobs request to 30 per minute per client, so the base interval
 * stays comfortably above 2 seconds.
 */
data class CaptionPollingConfig(
    val initialIntervalMs: Long = 3_000L,
    val maxIntervalMs: Long = 20_000L,
    val backoffMultiplier: Double = 1.6,
    /** Consecutive failed responses tolerated before polling gives up. */
    val maxConsecutiveFailures: Int = 6,
    /** Total wall-clock budget for one monitored job. */
    val maxPollDurationMs: Long = 30 * 60_000L,
)

/**
 * Polls the caption task status endpoint until a terminal stage.
 *
 * Guarantees:
 *  - successful responses reset the backoff interval,
 *  - transport failures and retryable API failures (5xx, 408, 429) are retried
 *    with exponential backoff up to [CaptionPollingConfig.maxConsecutiveFailures],
 *  - non-retryable API failures (4xx such as 400/401/403/404) abort immediately,
 *  - completed/failed/cancelled stages stop the loop,
 *  - coroutine cancellation is respected at every await point,
 *  - a wall-clock budget prevents unbounded polling.
 *
 * The poller owns no shared state, so the caller (the coordinator) is
 * responsible for preventing duplicate loops.
 */
class CaptionJobPoller(
    private val config: CaptionPollingConfig = CaptionPollingConfig(),
    private val delayFn: suspend (Long) -> Unit = { delay(it) },
) {

    /**
     * Runs the poll loop. Emits every snapshot through [onSnapshot] and returns
     * the first terminal snapshot. Throws on give-up, non-retryable API errors
     * or budget exhaustion; rethrows [CancellationException] untouched.
     */
    suspend fun poll(
        fetch: suspend () -> CaptionTaskSnapshot,
        onSnapshot: (CaptionTaskSnapshot) -> Unit = {},
    ): CaptionTaskSnapshot {
        val startedAtMs = System.currentTimeMillis()
        var intervalMs = config.initialIntervalMs
        var consecutiveFailures = 0
        while (true) {
            val elapsedMs = System.currentTimeMillis() - startedAtMs
            if (elapsedMs > config.maxPollDurationMs) {
                throw CaptionTaskApiException(
                    httpStatus = 0,
                    errorCode = CAPTION_POLL_BUDGET_EXCEEDED,
                    message = "Stopped waiting for the subtitle job. Check the backend and retry.",
                    retryable = true,
                )
            }
            val snapshot = try {
                fetch()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                consecutiveFailures++
                val retryable = when (error) {
                    is CaptionTaskApiException -> error.retryable
                    is CaptionResponseException -> true
                    else -> true
                }
                if (!retryable) throw error
                if (consecutiveFailures >= config.maxConsecutiveFailures) throw error
                delayFn(intervalMs)
                intervalMs = nextInterval(intervalMs)
                continue
            }
            consecutiveFailures = 0
            intervalMs = config.initialIntervalMs
            onSnapshot(snapshot)
            if (snapshot.isTerminal) return snapshot
            delayFn(intervalMs)
        }
    }

    private fun nextInterval(currentMs: Long): Long =
        (currentMs * config.backoffMultiplier).toLong().coerceIn(config.initialIntervalMs, config.maxIntervalMs)

    companion object {
        const val CAPTION_POLL_BUDGET_EXCEEDED = "CAPTION_POLL_BUDGET_EXCEEDED"
    }
}
