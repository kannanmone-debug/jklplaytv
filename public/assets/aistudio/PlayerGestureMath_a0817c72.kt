package com.example.player

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToLong

enum class VideoGestureAxis {
    NONE,
    HORIZONTAL_SEEK,
    VERTICAL_BRIGHTNESS,
    VERTICAL_VOLUME,
    PINCH_ZOOM
}

data class SeekPreview(
    val targetPositionMs: Long,
    val deltaMs: Long
)

enum class MusicGestureAxis {
    NONE,
    HORIZONTAL_TRACK_SWITCH,
    VERTICAL_VOLUME
}

enum class MusicSwipeDirection {
    NONE,
    NEXT_TRACK,
    PREVIOUS_TRACK
}

object PlayerGestureMath {

    fun classifyVideoGestureAxis(
        dx: Float,
        dy: Float,
        startX: Float,
        containerWidthPx: Float,
        pointerCount: Int,
        touchSlopPx: Float
    ): VideoGestureAxis {
        if (pointerCount >= 2) return VideoGestureAxis.PINCH_ZOOM
        val absX = abs(dx)
        val absY = abs(dy)
        if (absX < touchSlopPx && absY < touchSlopPx) return VideoGestureAxis.NONE
        return if (absX > absY * 1.15f) {
            VideoGestureAxis.HORIZONTAL_SEEK
        } else if (startX < containerWidthPx * 0.5f) {
            VideoGestureAxis.VERTICAL_BRIGHTNESS
        } else {
            VideoGestureAxis.VERTICAL_VOLUME
        }
    }

    fun computeSeekTargetMs(
        anchorPositionMs: Long,
        totalDx: Float,
        containerWidthPx: Float,
        durationMs: Long,
        fullWidthSweepMs: Long = 90_000L
    ): SeekPreview {
        val safeDuration = durationMs.coerceAtLeast(1_000L)
        val safeWidth = containerWidthPx.coerceAtLeast(1f)
        val safeAnchor = anchorPositionMs.coerceIn(0L, safeDuration)
        val rawDeltaMs = ((totalDx / safeWidth) * fullWidthSweepMs.toFloat()).roundToLong()
        val targetMs = (safeAnchor + rawDeltaMs).coerceIn(0L, safeDuration)
        return SeekPreview(
            targetPositionMs = targetMs,
            deltaMs = targetMs - safeAnchor
        )
    }

    fun computeNormalizedVerticalAdjustment(
        startFraction: Float,
        totalDy: Float,
        containerHeightPx: Float,
        minFraction: Float = 0f,
        maxFraction: Float = 1f,
        fullHeightSensitivity: Float = 1.15f
    ): Float {
        val safeHeight = containerHeightPx.coerceAtLeast(1f)
        val deltaFraction = (-totalDy / safeHeight) * fullHeightSensitivity
        return (startFraction + deltaFraction).coerceIn(minFraction, maxFraction)
    }

    fun formatSignedSeekDelta(deltaMs: Long): String {
        val totalSec = (deltaMs / 1000.0).roundToLong()
        val sign = if (totalSec >= 0L) "+" else "-"
        val absSec = abs(totalSec)
        val mins = absSec / 60L
        val secs = absSec % 60L
        return if (mins > 0L) {
            "$sign${mins}m ${secs.toString().padStart(2, '0')}s"
        } else {
            "$sign${absSec}s"
        }
    }

    fun classifyMusicGestureAxis(
        dx: Float,
        dy: Float,
        touchSlopPx: Float
    ): MusicGestureAxis {
        val absX = abs(dx)
        val absY = abs(dy)
        if (absX < touchSlopPx && absY < touchSlopPx) return MusicGestureAxis.NONE
        return when {
            absX >= absY * 1.15f -> MusicGestureAxis.HORIZONTAL_TRACK_SWITCH
            absY >= absX * 1.15f -> MusicGestureAxis.VERTICAL_VOLUME
            else -> MusicGestureAxis.NONE
        }
    }

    fun musicHorizontalThresholdPx(containerWidthPx: Float): Float =
        max(72f, containerWidthPx * 0.16f)

    fun resolveMusicHorizontalSwipeRelease(
        totalDx: Float,
        containerWidthPx: Float,
        canceled: Boolean = false
    ): MusicSwipeDirection {
        if (canceled) return MusicSwipeDirection.NONE
        val threshold = musicHorizontalThresholdPx(containerWidthPx)
        return when {
            totalDx >= threshold -> MusicSwipeDirection.NEXT_TRACK
            totalDx <= -threshold -> MusicSwipeDirection.PREVIOUS_TRACK
            else -> MusicSwipeDirection.NONE
        }
    }
}
