package com.example.nativestarter.domain.editor

import java.util.UUID

data class VideoSegment(
    val id: String = UUID.randomUUID().toString(),
    val sourceStartMs: Long,
    val sourceEndMs: Long,
) {
    val durationMs: Long
        get() = (sourceEndMs - sourceStartMs).coerceAtLeast(0L)
}

data class AudioSegment(
    val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val name: String,
    val durationMs: Long,
    val volume: Float = 1f,
    val timelineStartMs: Long = 0L,
)

data class SubtitleCue(
    val id: String = UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class EditorTimelineState(
    val sourceDurationMs: Long = 0L,
    val videoSegments: List<VideoSegment> = emptyList(),
    val audioSegments: List<AudioSegment> = emptyList(),
    val subtitleCues: List<SubtitleCue> = emptyList(),
    val sceneMarkersMs: List<Long> = emptyList(),
    val selectedSegmentId: String? = videoSegments.firstOrNull()?.id,
    val playheadMs: Long = 0L,
) {
    val durationMs: Long
        get() = videoSegments.sumOf(VideoSegment::durationMs)

    val selectedSegment: VideoSegment?
        get() = videoSegments.firstOrNull { it.id == selectedSegmentId }
}

object EditorTimeline {
    private const val MIN_SEGMENT_MS = 100L

    fun fromSource(durationMs: Long): EditorTimelineState {
        val duration = durationMs.coerceAtLeast(0L)
        return EditorTimelineState(
            sourceDurationMs = duration,
            videoSegments = if (duration > 0) listOf(VideoSegment(sourceStartMs = 0, sourceEndMs = duration)) else emptyList(),
        )
    }

    fun seek(state: EditorTimelineState, positionMs: Long): EditorTimelineState {
        val position = positionMs.coerceIn(0L, state.durationMs)
        val location = locate(state.videoSegments, position)
        return state.copy(playheadMs = position, selectedSegmentId = location?.first?.id ?: state.selectedSegmentId)
    }

    fun select(state: EditorTimelineState, segmentId: String): EditorTimelineState =
        if (state.videoSegments.any { it.id == segmentId }) state.copy(selectedSegmentId = segmentId) else state

    fun trimSelected(
        state: EditorTimelineState,
        sourceStartMs: Long,
        sourceEndMs: Long,
    ): EditorTimelineState {
        val index = state.videoSegments.indexOfFirst { it.id == state.selectedSegmentId }
        if (index < 0) return state
        val selected = state.videoSegments[index]
        val start = sourceStartMs.coerceIn(0L, selected.sourceEndMs - MIN_SEGMENT_MS)
        val end = sourceEndMs.coerceIn(start + MIN_SEGMENT_MS, state.sourceDurationMs)
        val updated = selected.copy(sourceStartMs = start, sourceEndMs = end)
        val segments = state.videoSegments.toMutableList().apply { set(index, updated) }
        return state.copy(videoSegments = segments, playheadMs = state.playheadMs.coerceAtMost(segments.sumOf(VideoSegment::durationMs)))
    }

    fun splitAtPlayhead(state: EditorTimelineState): EditorTimelineState {
        val (segment, segmentTimelineStart) = locate(state.videoSegments, state.playheadMs) ?: return state
        val splitSourcePosition = segment.sourceStartMs + (state.playheadMs - segmentTimelineStart)
        if (splitSourcePosition - segment.sourceStartMs < MIN_SEGMENT_MS ||
            segment.sourceEndMs - splitSourcePosition < MIN_SEGMENT_MS
        ) return state
        val index = state.videoSegments.indexOfFirst { it.id == segment.id }
        val left = segment.copy(sourceEndMs = splitSourcePosition)
        val right = VideoSegment(sourceStartMs = splitSourcePosition, sourceEndMs = segment.sourceEndMs)
        val segments = state.videoSegments.toMutableList().apply {
            set(index, left)
            add(index + 1, right)
        }
        return state.copy(videoSegments = segments, selectedSegmentId = right.id)
    }

    fun deleteSelected(state: EditorTimelineState): EditorTimelineState {
        val index = state.videoSegments.indexOfFirst { it.id == state.selectedSegmentId }
        if (index < 0) return state
        val segments = state.videoSegments.toMutableList().apply { removeAt(index) }
        val nextSelected = segments.getOrNull(index)?.id ?: segments.getOrNull(index - 1)?.id
        return state.copy(
            videoSegments = segments,
            selectedSegmentId = nextSelected,
            playheadMs = state.playheadMs.coerceAtMost(segments.sumOf(VideoSegment::durationMs)),
        )
    }

    fun locate(segments: List<VideoSegment>, timelinePositionMs: Long): Pair<VideoSegment, Long>? {
        var cursor = 0L
        for (segment in segments) {
            val next = cursor + segment.durationMs
            if (timelinePositionMs < next || (timelinePositionMs == next && segment == segments.lastOrNull())) {
                return segment to cursor
            }
            cursor = next
        }
        return null
    }

    fun parseSrt(source: String): List<SubtitleCue> {
        val normalized = source.replace("\r\n", "\n").replace('\r', '\n').trim()
        require(normalized.isNotEmpty()) { "SRT file is empty." }
        val blocks = normalized.split(Regex("\\n\\s*\\n"))
        val cues = blocks.mapNotNull { block ->
            val lines = block.lines().map(String::trim).filter(String::isNotEmpty)
            val timeIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeIndex < 0) return@mapNotNull null
            val parts = lines[timeIndex].split("-->").map(String::trim)
            require(parts.size == 2) { "Invalid SRT timestamp line: ${lines[timeIndex]}" }
            val start = parseSrtTimestamp(parts[0])
            val end = parseSrtTimestamp(parts[1])
            require(end > start) { "Subtitle end must be after its start." }
            val text = lines.drop(timeIndex + 1).joinToString("\n")
            require(text.isNotBlank()) { "Subtitle cue text is empty." }
            SubtitleCue(startMs = start, endMs = end, text = text)
        }
        require(cues.isNotEmpty()) { "No valid SRT subtitle cues found." }
        return cues.sortedBy(SubtitleCue::startMs)
    }

    fun toSrt(cues: List<SubtitleCue>): String = cues.sortedBy(SubtitleCue::startMs)
        .mapIndexed { index, cue ->
            "${index + 1}\n${formatSrtTimestamp(cue.startMs)} --> ${formatSrtTimestamp(cue.endMs)}\n${cue.text}"
        }
        .joinToString("\n\n")

    private fun parseSrtTimestamp(value: String): Long {
        val match = Regex("^(\\d{2,}):(\\d{2}):(\\d{2})[,.](\\d{3})$").matchEntire(value)
            ?: throw IllegalArgumentException("Invalid SRT timestamp: $value")
        val (hours, minutes, seconds, millis) = match.destructured
        return (((hours.toLong() * 60 + minutes.toLong()) * 60 + seconds.toLong()) * 1000) + millis.toLong()
    }

    private fun formatSrtTimestamp(timeMs: Long): String {
        val safe = timeMs.coerceAtLeast(0L)
        val hours = safe / 3_600_000
        val minutes = safe / 60_000 % 60
        val seconds = safe / 1_000 % 60
        val millis = safe % 1_000
        return "%02d:%02d:%02d,%03d".format(hours, minutes, seconds, millis)
    }
}
