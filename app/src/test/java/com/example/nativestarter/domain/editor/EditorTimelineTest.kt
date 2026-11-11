package com.example.nativestarter.domain.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorTimelineTest {

    @Test
    fun splitAtPlayheadCreatesAdjacentSourceSegments() {
        val initial = EditorTimeline.fromSource(10_000L)
        val split = EditorTimeline.splitAtPlayhead(EditorTimeline.seek(initial, 4_000L))

        assertEquals(2, split.videoSegments.size)
        assertEquals(4_000L, split.videoSegments[0].sourceEndMs)
        assertEquals(4_000L, split.videoSegments[1].sourceStartMs)
        assertEquals(10_000L, split.durationMs)
        assertNotEquals(split.videoSegments[0].id, split.videoSegments[1].id)
    }

    @Test
    fun trimChangesDurationAndDeleteRemovesOnlySelectedSegment() {
        val initial = EditorTimeline.fromSource(10_000L)
        val trimmed = EditorTimeline.trimSelected(initial, 1_000L, 8_000L)
        val split = EditorTimeline.splitAtPlayhead(EditorTimeline.seek(trimmed, 3_000L))
        val deleted = EditorTimeline.deleteSelected(split)

        assertEquals(7_000L, trimmed.durationMs)
        assertEquals(2, split.videoSegments.size)
        assertEquals(1, deleted.videoSegments.size)
        assertEquals(3_000L, deleted.durationMs)
    }

    @Test
    fun srtRoundTripsMillisecondPrecisionAndMultilineCueText() {
        val source = """1
00:00:01,250 --> 00:00:03,000
Hello
world

2
00:01:02,005 --> 00:01:03,125
Second cue"""

        val cues = EditorTimeline.parseSrt(source)
        val exported = EditorTimeline.toSrt(cues)

        assertEquals(2, cues.size)
        assertEquals(1_250L, cues[0].startMs)
        assertEquals("Hello\nworld", cues[0].text)
        assertTrue(exported.contains("00:01:02,005 --> 00:01:03,125"))
        assertEquals(
            cues.map { it.copy(id = "") },
            EditorTimeline.parseSrt(exported).map { it.copy(id = "") },
        )
    }
}
