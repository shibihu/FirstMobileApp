package com.example.nativestarter.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wire-contract checks for caption job endpoint paths and timeouts. */
class AiClipStudioApiCaptionEndpointsTest {

    @Test
    fun captionTaskPathUsesQueryJobIdAndSafeCharacters() {
        assertEquals(
            "/api/captions/jobs/task-1?id=job-1",
            AiClipStudioApi.captionTaskPath("task-1", "job-1"),
        )
        // UUID-style ids stay untouched so the backend regex matches.
        assertEquals(
            "/api/captions/jobs/a1b2c3d4-e5f6-7890-abcd-ef0123456789?id=0f1e2d3c4b5a69788a7b6c5d4e3f2019",
            AiClipStudioApi.captionTaskPath("a1b2c3d4-e5f6-7890-abcd-ef0123456789", "0f1e2d3c4b5a69788a7b6c5d4e3f2019"),
        )
    }

    @Test
    fun captionTaskPathPercentEncodesHostileInput() {
        val path = AiClipStudioApi.captionTaskPath("../../etc", "job id")
        assertTrue(path.startsWith("/api/captions/jobs/"))
        assertTrue(!path.contains("../"))
        assertTrue(!path.contains(" "))
    }

    @Test
    fun pollAndCancelUseShorterReadTimeoutsThanUpload() {
        assertTrue(AiClipStudioApi.POLL_READ_TIMEOUT_MS < 60_000)
        assertTrue(AiClipStudioApi.CANCEL_READ_TIMEOUT_MS < 60_000)
        assertTrue(AiClipStudioApi.SUBMIT_READ_TIMEOUT_MS < 60_000)
    }
}
