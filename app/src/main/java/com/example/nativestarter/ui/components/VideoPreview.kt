package com.example.nativestarter.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.nativestarter.data.ImportedVideo
import com.example.nativestarter.domain.editor.EditorTimelineState
import com.example.nativestarter.domain.editor.AudioSegment
import com.example.nativestarter.domain.editor.SubtitleCue
import com.example.nativestarter.domain.editor.VideoSegment
import com.example.nativestarter.viewmodel.AppViewModel
import kotlinx.coroutines.delay

private const val TAG = "NativeEditorPlayer"

@Composable
fun VideoPreview(
    video: ImportedVideo,
    viewModel: AppViewModel,
    playheadMs: Long,
    seekRequestId: Long,
    timeline: EditorTimelineState,
    isPlaying: Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember(video.uri) { ExoPlayer.Builder(context).build() }
    val currentSegments = rememberUpdatedState(timeline.videoSegments)
    val activeCue = timeline.subtitleCues.firstOrNull { playheadMs in it.startMs until it.endMs }
    val aspectRatio = if (video.width != null && video.height != null && video.height > 0) {
        video.width.toFloat() / video.height.toFloat()
    } else {
        16f / 9f
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "playback state=$playbackState media=${video.uri}")
                if (playbackState == Player.STATE_BUFFERING) viewModel.onPreviewPrepared()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                Log.d(
                    TAG,
                    "video size=${videoSize.width}x${videoSize.height} rotation=${videoSize.unappliedRotationDegrees}",
                )
            }

            override fun onRenderedFirstFrame() {
                Log.i(TAG, "first frame rendered for ${video.uri}")
                viewModel.onPreviewRendered()
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "playback failed code=${error.errorCodeName} media=${video.uri}", error)
                viewModel.onPreviewError("${error.errorCodeName}: ${error.message ?: "Unable to decode this video."}")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                viewModel.updatePlayback(globalPosition(exoPlayer, currentSegments.value), isPlaying)
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            Log.d(TAG, "releasing player for ${video.uri}")
            exoPlayer.release()
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.updatePlayback(globalPosition(exoPlayer, currentSegments.value), false)
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(exoPlayer, video.uri, timeline.videoSegments) {
        viewModel.onPreviewPrepared()
        val resumePlayback = exoPlayer.isPlaying
        if (timeline.videoSegments.isEmpty() && timeline.sourceDurationMs > 0L) {
            exoPlayer.clearMediaItems()
            return@LaunchedEffect
        }
        if (timeline.videoSegments.isEmpty()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(video.uri))
        } else {
            val items = timeline.videoSegments.mapIndexed { index, segment ->
                MediaItem.Builder()
                    .setMediaId("clip-$index")
                    .setUri(video.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(segment.sourceStartMs)
                            .setEndPositionMs(segment.sourceEndMs)
                            .build(),
                    )
                    .build()
            }
            exoPlayer.setMediaItems(items)
        }
        exoPlayer.prepare()
        if (playheadMs > 0L) exoPlayer.seekTo(playheadMs)
        exoPlayer.playWhenReady = resumePlayback
    }

    LaunchedEffect(exoPlayer, seekRequestId) {
        if (seekRequestId > 0L) exoPlayer.seekTo(playheadMs.coerceAtLeast(0L))
    }

    LaunchedEffect(exoPlayer, timeline.videoSegments) {
        while (true) {
            if (exoPlayer.isPlaying) {
                viewModel.updatePlayback(globalPosition(exoPlayer, currentSegments.value), true)
            }
            delay(200L)
        }
    }

    AudioTrackPlayback(
        segments = timeline.audioSegments,
        positionMs = playheadMs,
        isPlaying = isPlaying,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.35f, 3f))
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    useController = true
                    player = exoPlayer
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.matchParentSize(),
        )
        activeCue?.let { cue ->
            Text(
                text = cue.text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        if (!viewModel.uiState.value.previewReady && viewModel.uiState.value.playerError == null) {
            CircularProgressIndicator()
        }
        viewModel.uiState.value.playerError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun AudioTrackPlayback(
    segments: List<AudioSegment>,
    positionMs: Long,
    isPlaying: Boolean,
) {
    val context = LocalContext.current
    val ids = segments.map(AudioSegment::id)
    val players = remember(ids) {
        segments.associate { segment ->
            segment.id to ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(segment.uri))
                prepare()
            }
        }
    }
    DisposableEffect(players) {
        onDispose { players.values.forEach(ExoPlayer::release) }
    }
    LaunchedEffect(segments, positionMs, isPlaying) {
        segments.forEach { segment ->
            val player = players[segment.id] ?: return@forEach
            player.volume = segment.volume.coerceIn(0f, 1f)
            val clipPosition = positionMs - segment.timelineStartMs
            if (isPlaying && clipPosition in 0 until segment.durationMs) {
                if (!player.isPlaying || kotlin.math.abs(player.currentPosition - clipPosition) > 400L) {
                    player.seekTo(clipPosition)
                }
                player.play()
            } else {
                player.pause()
            }
        }
    }
}

private fun globalPosition(player: Player, segments: List<VideoSegment>): Long {
    if (segments.isEmpty()) return player.currentPosition.coerceAtLeast(0L)
    val index = player.currentMediaItemIndex.coerceIn(0, segments.lastIndex)
    return segments.take(index).sumOf(VideoSegment::durationMs) + player.currentPosition.coerceAtLeast(0L)
}
