package com.example.nativestarter.data.caption

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract tests against the exact shapes AIClipStudio returns for the
 * asynchronous caption job endpoints (POST/GET/DELETE /api/captions/jobs).
 */
class CaptionTaskParsingTest {

    private fun submissionDocument(): JSONObject = JSONObject(
        """
        {
          "taskId": "a1b2c3d4-e5f6-7890-abcd-ef0123456789",
          "jobId": "0f1e2d3c4b5a69788a7b6c5d4e3f2019",
          "status": "queued",
          "stage": "queued",
          "progress": 0,
          "renderId": null,
          "error": null,
          "createdAt": 1736265600000,
          "updatedAt": 1736265600000,
          "attempts": 0,
          "metrics": {}
        }
        """.trimIndent(),
    )

    @Test
    fun parsesSubmissionResponseWithTaskIdentifiers() {
        val snapshot = CaptionTaskSnapshot.parse(submissionDocument())
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef0123456789", snapshot.taskId)
        assertEquals("0f1e2d3c4b5a69788a7b6c5d4e3f2019", snapshot.jobId)
        assertEquals(CaptionStage.QUEUED, snapshot.stage)
        assertEquals("queued", snapshot.status)
        assertEquals(0, snapshot.progress)
        assertNull(snapshot.renderId)
        assertNull(snapshot.error)
        assertFalse(snapshot.isTerminal)
    }

    @Test
    fun duplicateSubmissionResponseParsesIdentically() {
        val first = CaptionTaskSnapshot.parse(submissionDocument())
        val second = CaptionTaskSnapshot.parse(submissionDocument())
        assertEquals(first.taskId, second.taskId)
    }

    @Test
    fun parsesStatusResponseWithStageAndProgress() {
        val status = CaptionTaskSnapshot.parse(
            JSONObject(
                """
                {
                  "taskId": "t1",
                  "jobId": "j1",
                  "status": "running",
                  "stage": "transcribing",
                  "progress": 30,
                  "renderId": null,
                  "error": null,
                  "createdAt": 1,
                  "updatedAt": 2
                }
                """.trimIndent(),
            ),
        )
        assertEquals(CaptionStage.TRANSCRIBING, status.stage)
        assertEquals(30, status.progress)
        assertFalse(status.isTerminal)
    }

    @Test
    fun parsesCompletedResponseWithRenderId() {
        val completed = CaptionTaskSnapshot.parse(
            JSONObject(
                """
                {
                  "taskId": "t1",
                  "jobId": "j1",
                  "status": "completed",
                  "stage": "completed",
                  "progress": 100,
                  "renderId": "r-123",
                  "error": null,
                  "createdAt": 1,
                  "updatedAt": 2
                }
                """.trimIndent(),
            ),
        )
        assertEquals(CaptionStage.COMPLETED, completed.stage)
        assertTrue(completed.isTerminal)
        assertEquals("r-123", completed.renderId)
    }

    @Test
    fun parsesFailedResponseWithStructuredError() {
        val failed = CaptionTaskSnapshot.parse(
            JSONObject(
                """
                {
                  "taskId": "t1",
                  "jobId": "j1",
                  "status": "failed",
                  "stage": "failed",
                  "progress": 100,
                  "renderId": null,
                  "error": {
                    "code": "STT_API_KEY_MISSING",
                    "message": "No speech provider key is configured on the server.",
                    "retryable": false,
                    "stage": "transcribing"
                  },
                  "createdAt": 1,
                  "updatedAt": 2
                }
                """.trimIndent(),
            ),
        )
        assertEquals(CaptionStage.FAILED, failed.stage)
        assertTrue(failed.isTerminal)
        assertNotNull(failed.error)
        assertEquals("STT_API_KEY_MISSING", failed.error?.code)
        assertFalse(failed.error?.retryable == true)
    }

    @Test
    fun parsesCancelledResponse() {
        val cancelled = CaptionTaskSnapshot.parse(
            JSONObject(
                """
                {
                  "taskId": "t1",
                  "jobId": "j1",
                  "status": "cancelled",
                  "stage": "cancelled",
                  "progress": 30,
                  "renderId": null,
                  "error": null,
                  "createdAt": 1,
                  "updatedAt": 2
                }
                """.trimIndent(),
            ),
        )
        assertEquals(CaptionStage.CANCELLED, cancelled.stage)
        assertTrue(cancelled.isTerminal)
    }

    @Test
    fun missingTaskIdFailsClosed() {
        val invalid = JSONObject("""{"jobId": "j1", "stage": "queued"}""")
        assertThrows(CaptionResponseException::class.java) { CaptionTaskSnapshot.parse(invalid) }
    }

    @Test
    fun unknownFutureStageMapsToUnknownAndKeepsPolling() {
        val future = CaptionTaskSnapshot.parse(
            JSONObject(
                """
                {
                  "taskId": "t1",
                  "jobId": "j1",
                  "status": "running",
                  "stage": "quantum_transcribing",
                  "progress": 55,
                  "renderId": null,
                  "error": null,
                  "createdAt": 1,
                  "updatedAt": 2
                }
                """.trimIndent(),
            ),
        )
        assertEquals(CaptionStage.UNKNOWN, future.stage)
        assertFalse(future.isTerminal)
        assertEquals("quantum_transcribing", future.rawStage)
    }

    @Test
    fun invalidJsonPayloadIsRejected() {
        assertThrows(JSONException::class.java) { JSONObject("not json") }
    }

    @Test
    fun localCancelledSnapshotIsTerminalWithoutRenderId() {
        val local = CaptionTaskSnapshot.localCancelled("j1", "t1")
        assertEquals(CaptionStage.CANCELLED, local.stage)
        assertTrue(local.isTerminal)
        assertNull(local.renderId)
        assertNull(local.error)
    }
}
