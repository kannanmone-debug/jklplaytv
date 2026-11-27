package com.example.equalizer

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.roundToInt

/**
 * Pure Media3 PCM AudioProcessor implementing real-time:
 * 1. L/R Stereo Channel Balance (-1.0f left-only to +1.0f right-only)
 * 2. Millisecond Audio Lip-Sync Delay (-5000ms to +5000ms) using a bounded PCM ring buffer.
 */
@UnstableApi
class BnjAudioDspProcessor : BaseAudioProcessor() {

    private val stateLock = Any()

    @Volatile
    private var stereoBalance: Float = 0f

    @Volatile
    private var audioDelayMs: Long = 0L

    private var delayBuffer = ShortArray(0)
    private var capacityFrames = 0
    private var readFramePos = 0
    private var writeFramePos = 0
    private var bufferedFrames = 0
    private var targetPositiveFrames = 0
    private var skipFramesRemaining = 0L
    private var appliedNegativeFrames = 0L
    private var configuredSampleRate = 0
    private var configuredChannels = 0
    private var scratchFrame = ShortArray(0)

    fun setStereoBalance(balance: Float) {
        stereoBalance = balance.coerceIn(-1f, 1f)
    }

    fun getStereoBalance(): Float = stereoBalance

    fun setAudioDelayMs(delayMs: Long) {
        val clamped = delayMs.coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
        synchronized(stateLock) {
            if (clamped != audioDelayMs) {
                audioDelayMs = clamped
                applyDelayStateLocked(clamped)
            }
        }
    }

    fun getAudioDelayMs(): Long = audioDelayMs

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT ||
            inputAudioFormat.sampleRate <= 0 ||
            inputAudioFormat.channelCount <= 0
        ) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        return inputAudioFormat
    }

    override fun onFlush() {
        synchronized(stateLock) {
            configuredSampleRate = inputAudioFormat.sampleRate.coerceAtLeast(0)
            configuredChannels = inputAudioFormat.channelCount.coerceAtLeast(0)
            if (configuredChannels > 0 && scratchFrame.size != configuredChannels) {
                scratchFrame = ShortArray(configuredChannels)
            }
            readFramePos = 0
            writeFramePos = 0
            bufferedFrames = 0
            appliedNegativeFrames = 0L
            applyDelayStateLocked(audioDelayMs)
        }
    }

    override fun onReset() {
        synchronized(stateLock) {
            delayBuffer = ShortArray(0)
            capacityFrames = 0
            readFramePos = 0
            writeFramePos = 0
            bufferedFrames = 0
            targetPositiveFrames = 0
            skipFramesRemaining = 0L
            appliedNegativeFrames = 0L
            configuredSampleRate = 0
            configuredChannels = 0
            scratchFrame = ShortArray(0)
        }
    }

    private fun msToFramesLocked(absMs: Long, sampleRate: Int, channels: Int): Int {
        if (absMs <= 0L || sampleRate <= 0 || channels <= 0) return 0
        val rawFrames = (absMs.coerceAtMost(MAX_DELAY_MS) * sampleRate.toLong()) / 1000L
        val maxSafeFrames = (MAX_BUFFER_SHORTS / channels).toLong()
        return rawFrames.coerceIn(0L, minOf(Int.MAX_VALUE.toLong(), maxSafeFrames)).toInt()
    }

    private fun applyDelayStateLocked(dMs: Long) {
        val sr = configuredSampleRate
        val ch = configuredChannels
        if (sr <= 0 || ch <= 0) {
            targetPositiveFrames = 0
            skipFramesRemaining = 0L
            appliedNegativeFrames = 0L
            return
        }

        if (dMs > 0L) {
            val maxFramesForFormat = msToFramesLocked(MAX_DELAY_MS, sr, ch)
            val requiredCapacityShorts = maxFramesForFormat * ch
            if (maxFramesForFormat > 0 && (capacityFrames != maxFramesForFormat || delayBuffer.size < requiredCapacityShorts)) {
                val prevBuffer = delayBuffer
                val prevCap = capacityFrames
                val prevRead = readFramePos
                val prevBuffered = bufferedFrames
                val newBuffer = ShortArray(requiredCapacityShorts)
                val copyFrames = if (prevCap > 0) minOf(prevBuffered, maxFramesForFormat) else 0
                for (i in 0 until copyFrames) {
                    val srcFrame = (prevRead + i) % prevCap
                    System.arraycopy(prevBuffer, srcFrame * ch, newBuffer, i * ch, ch)
                }
                delayBuffer = newBuffer
                capacityFrames = maxFramesForFormat
                readFramePos = 0
                writeFramePos = copyFrames % maxFramesForFormat
                bufferedFrames = copyFrames
            }

            val desiredFrames = msToFramesLocked(dMs, sr, ch).coerceAtMost(capacityFrames)
            targetPositiveFrames = desiredFrames
            if (bufferedFrames > desiredFrames && capacityFrames > 0) {
                val excess = bufferedFrames - desiredFrames
                readFramePos = (readFramePos + excess) % capacityFrames
                bufferedFrames = desiredFrames
            }
            skipFramesRemaining = 0L
            appliedNegativeFrames = 0L
        } else if (dMs < 0L) {
            targetPositiveFrames = 0
            readFramePos = 0
            writeFramePos = 0
            bufferedFrames = 0
            val desiredAbsFrames = msToFramesLocked(-dMs, sr, ch).toLong()
            skipFramesRemaining = (desiredAbsFrames - appliedNegativeFrames).coerceAtLeast(0L)
        } else {
            targetPositiveFrames = 0
            readFramePos = 0
            writeFramePos = 0
            bufferedFrames = 0
            skipFramesRemaining = 0L
            appliedNegativeFrames = 0L
        }
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return

        val balance = stereoBalance
        val leftGain = if (balance > 0f) (1f - balance).coerceIn(0f, 1f) else 1f
        val rightGain = if (balance < 0f) (1f + balance).coerceIn(0f, 1f) else 1f

        synchronized(stateLock) {
            val channels = configuredChannels.takeIf { it > 0 } ?: inputAudioFormat.channelCount
            if (channels <= 0) return
            if (scratchFrame.size != channels) {
                scratchFrame = ShortArray(channels)
            }
            val bytesPerFrame = channels * 2
            val frameCount = remaining / bytesPerFrame
            if (frameCount <= 0) return

            val applyBalance = (leftGain != 1f || rightGain != 1f) && channels >= 2
            val outBuf = replaceOutputBuffer(frameCount * bytesPerFrame)

            for (frameIdx in 0 until frameCount) {
                for (ch in 0 until channels) {
                    val rawSample = inputBuffer.getShort()
                    scratchFrame[ch] = if (applyBalance) {
                        val gain = when (ch) {
                            0 -> leftGain
                            1 -> rightGain
                            else -> 1f
                        }
                        (rawSample * gain).roundToInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                            .toShort()
                    } else {
                        rawSample
                    }
                }

                if (skipFramesRemaining > 0L) {
                    skipFramesRemaining--
                    appliedNegativeFrames++
                    continue
                }

                if (targetPositiveFrames > 0 && capacityFrames > 0) {
                    if (bufferedFrames < targetPositiveFrames) {
                        val writeBase = writeFramePos * channels
                        for (ch in 0 until channels) {
                            delayBuffer[writeBase + ch] = scratchFrame[ch]
                            outBuf.putShort(0)
                        }
                        writeFramePos = (writeFramePos + 1) % capacityFrames
                        bufferedFrames++
                    } else {
                        val readBase = readFramePos * channels
                        for (ch in 0 until channels) {
                            outBuf.putShort(delayBuffer[readBase + ch])
                        }
                        readFramePos = (readFramePos + 1) % capacityFrames

                        val writeBase = writeFramePos * channels
                        for (ch in 0 until channels) {
                            delayBuffer[writeBase + ch] = scratchFrame[ch]
                        }
                        writeFramePos = (writeFramePos + 1) % capacityFrames
                    }
                } else {
                    for (ch in 0 until channels) {
                        outBuf.putShort(scratchFrame[ch])
                    }
                }
            }
            outBuf.flip()
        }
    }

    private companion object {
        const val MIN_DELAY_MS = -5000L
        const val MAX_DELAY_MS = 5000L
        const val MAX_BUFFER_SHORTS = 8_388_608
    }
}
