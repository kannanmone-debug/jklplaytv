package com.example

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.equalizer.BnjAudioDspProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@OptIn(UnstableApi::class)
class AudioDspProcessorTest {

    private fun pcmBufferOf(vararg samples: Short): ByteBuffer =
        ByteBuffer.allocateDirect(samples.size * 2)
            .order(ByteOrder.nativeOrder())
            .apply {
                samples.forEach { putShort(it) }
                flip()
            }

    private fun readShorts(buffer: ByteBuffer): List<Short> {
        val ordered = buffer.order(ByteOrder.nativeOrder())
        val out = ArrayList<Short>(ordered.remaining() / 2)
        while (ordered.remaining() >= 2) {
            out.add(ordered.getShort())
        }
        return out
    }

    private fun configureAndFlush(
        processor: BnjAudioDspProcessor,
        sampleRate: Int = 1000, // 1 frame per ms for exact deterministic assertions
        channels: Int = 2,
        encoding: Int = C.ENCODING_PCM_16BIT
    ): AudioProcessor.AudioFormat {
        val fmt = processor.configure(AudioProcessor.AudioFormat(sampleRate, channels, encoding))
        processor.flush()
        return fmt
    }

    @Test
    fun `zero delay emits stereo PCM16 frames bit-exact without attenuation or lag`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)
        processor.setAudioDelayMs(0L)
        processor.setStereoBalance(0f)

        val input = pcmBufferOf(101, 202, -303, 404, 505, -606)
        processor.queueInput(input)

        assertEquals(
            listOf<Short>(101, 202, -303, 404, 505, -606),
            readShorts(processor.output)
        )
    }

    @Test
    fun `positive delay buffers exact frames and emits buffered audio continuously`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)
        processor.setAudioDelayMs(3L)

        processor.queueInput(pcmBufferOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100))
        // First 3 frames (6 shorts) are silent fill, followed by frames 1 & 2
        assertEquals(
            listOf<Short>(0, 0, 0, 0, 0, 0, 10, 20, 30, 40),
            readShorts(processor.output)
        )

        // Next chunk emits previously buffered frames (3, 4, 5) seamlessly
        processor.queueInput(pcmBufferOf(110, 120, 130, 140))
        assertEquals(
            listOf<Short>(50, 60, 70, 80),
            readShorts(processor.output)
        )
    }

    @Test
    fun `negative delay skips whole frames cleanly without audio corruption`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)
        processor.setAudioDelayMs(-2L)

        // Chunk 1 has 1 frame -> skipped completely (0 bytes output), 1 skip frame remains
        processor.queueInput(pcmBufferOf(111, 222))
        assertEquals(emptyList<Short>(), readShorts(processor.output))

        // Chunk 2 skips its 1st frame and passes subsequent frames intact
        processor.queueInput(pcmBufferOf(333, 444, 555, 666, 777, 888))
        assertEquals(
            listOf<Short>(555, 666, 777, 888),
            readShorts(processor.output)
        )
    }

    @Test
    fun `delay adjustment during active processing does not wipe buffer or produce unwanted silence`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)
        processor.setAudioDelayMs(4L)

        // Push 4 frames so buffer holds [F1, F2, F3, F4]
        processor.queueInput(pcmBufferOf(11, 12, 21, 22, 31, 32, 41, 42))
        assertEquals(listOf<Short>(0, 0, 0, 0, 0, 0, 0, 0), readShorts(processor.output))

        // Reduce live delay from +4ms to +2ms: oldest 2 excess frames trimmed -> [F3, F4] preserved!
        processor.setAudioDelayMs(2L)
        processor.queueInput(pcmBufferOf(51, 52, 61, 62))
        // Immediately emits F3 and F4 with zero silence gap!
        assertEquals(listOf<Short>(31, 32, 41, 42), readShorts(processor.output))

        // Increase live delay from +2ms to +3ms: existing [F5, F6] remain preserved!
        processor.setAudioDelayMs(3L)
        processor.queueInput(pcmBufferOf(71, 72, 81, 82, 91, 92))
        // First 1 frame fills the +1ms delta, then immediately reads preserved F5 and F6!
        assertEquals(listOf<Short>(0, 0, 51, 52, 61, 62), readShorts(processor.output))
    }

    @Test
    fun `repeated rapid and concurrent delay changes remain stable without exceptions or deadlocks`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 48000, channels = 2)

        val executor = Executors.newSingleThreadExecutor()
        val errorRef = AtomicReference<Throwable?>(null)
        val latch = CountDownLatch(1)

        executor.submit {
            try {
                val sweeps = listOf(0L, 50L, 250L, 120L, 600L, 40L, -100L, -350L, 0L, 5000L, -5000L, 180L)
                for (i in 0 until 200) {
                    processor.setAudioDelayMs(sweeps[i % sweeps.size])
                    processor.setStereoBalance(((i % 21) - 10) / 10f)
                }
            } catch (t: Throwable) {
                errorRef.set(t)
            } finally {
                latch.countDown()
            }
        }

        try {
            val frameBatch = ShortArray(480) { idx -> (idx + 1).toShort() }
            repeat(80) {
                processor.queueInput(pcmBufferOf(*frameBatch))
                val out = readShorts(processor.output)
                assertTrue("Output shorts must stay stereo-aligned", out.size % 2 == 0)
            }
            latch.await()
            assertEquals(null, errorRef.get())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `existing buffered audio is preserved bit-exact across multiple delay increments`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)
        processor.setAudioDelayMs(2L)

        processor.queueInput(pcmBufferOf(100, 101, 200, 201))
        assertEquals(listOf<Short>(0, 0, 0, 0), readShorts(processor.output))

        // Increase delay +2ms -> +4ms while buffer already holds (100,101), (200,201)
        processor.setAudioDelayMs(4L)
        processor.queueInput(pcmBufferOf(300, 301, 400, 401, 500, 501, 600, 601))
        val out = readShorts(processor.output)

        // First 2 frames fill delta (+4 - +2 = 2ms), next 2 frames emit the exact original buffered frames
        assertEquals(
            listOf<Short>(0, 0, 0, 0, 100, 101, 200, 201),
            out
        )
    }

    @Test
    fun `PCM16 stereo L-R channel alignment stays intact across negative skip delay transitions and remainder bytes`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)

        // Negative delay step 1 -> step 2 only skips the remaining delta frames, never odd shorts
        processor.setAudioDelayMs(-1L)
        processor.queueInput(pcmBufferOf(10, -10)) // skipped 1 frame
        assertEquals(emptyList<Short>(), readShorts(processor.output))

        // Dragging further negative from -1ms to -3ms only skips 2 more frames (3 - 1 = 2)
        processor.setAudioDelayMs(-3L)
        processor.queueInput(pcmBufferOf(20, -20, 30, -30, 40, -40))
        // Frame (40, -40) keeps positive on Left and negative on Right!
        assertEquals(listOf<Short>(40, -40), readShorts(processor.output))

        // Trailing partial byte in input buffer must never misalign L/R channels
        val oddBuf = ByteBuffer.allocateDirect(5).order(ByteOrder.nativeOrder()).apply {
            putShort(500)
            putShort(-500)
            put(0x7F.toByte())
            flip()
        }
        processor.queueInput(oddBuf)
        assertEquals(listOf<Short>(500, -500), readShorts(processor.output))
    }

    @Test
    fun `stereo L-R balance regression attenuates channels accurately alongside delay`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)

        // Left-only (-1.0f): Right muted, Left intact
        processor.setStereoBalance(-1.0f)
        processor.queueInput(pcmBufferOf(1000, 2000))
        assertEquals(listOf<Short>(1000, 0), readShorts(processor.output))

        // Mid-right (+0.5f) with +1ms delay: Left scaled to 50%, Right 100%, buffered and emitted after 1ms
        processor.setStereoBalance(0.5f)
        processor.setAudioDelayMs(1L)
        processor.queueInput(pcmBufferOf(2000, -4000, 1000, 3000))
        assertEquals(listOf<Short>(0, 0, 1000, -4000), readShorts(processor.output))
    }

    @Test
    fun `buffer boundary clamping and high sample rate overflow safety`() {
        val processor = BnjAudioDspProcessor()
        processor.setAudioDelayMs(99_999L)
        assertEquals(5000L, processor.getAudioDelayMs())
        processor.setAudioDelayMs(-99_999L)
        assertEquals(-5000L, processor.getAudioDelayMs())

        processor.setStereoBalance(4.5f)
        assertEquals(1f, processor.getStereoBalance(), 0.0001f)
        processor.setStereoBalance(-4.5f)
        assertEquals(-1f, processor.getStereoBalance(), 0.0001f)

        // Unsupported encodings return NOT_SET cleanly
        val unsupported = processor.configure(
            AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT)
        )
        assertEquals(AudioProcessor.AudioFormat.NOT_SET, unsupported)

        // High sample rate + 8ch multi-channel max delay must not Int-overflow or OOM
        val multiChannelFmt = configureAndFlush(processor, sampleRate = 192_000, channels = 8)
        assertEquals(8, multiChannelFmt.channelCount)
        processor.setAudioDelayMs(5000L)
        processor.queueInput(pcmBufferOf(1, 2, 3, 4, 5, 6, 7, 8))
        assertEquals(List(8) { 0.toShort() }, readShorts(processor.output))
    }

    @Test
    fun `processor flush clears stale buffered audio while reset releases resources safely`() {
        val processor = BnjAudioDspProcessor()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)
        processor.setAudioDelayMs(2L)

        // Queue old pre-seek frames into ring buffer
        processor.queueInput(pcmBufferOf(901, 902, 903, 904))
        assertEquals(listOf<Short>(0, 0, 0, 0), readShorts(processor.output))

        // Seek -> flush() discards old pre-seek buffered frames cleanly
        processor.flush()
        processor.queueInput(pcmBufferOf(10, 11, 20, 21, 30, 31))
        assertEquals(
            listOf<Short>(0, 0, 0, 0, 10, 11),
            readShorts(processor.output)
        )

        // reset() releases ring buffer cleanly and allows fresh reconfiguration later
        processor.reset()
        configureAndFlush(processor, sampleRate = 1000, channels = 2)
        processor.setAudioDelayMs(0L)
        processor.queueInput(pcmBufferOf(77, 88))
        assertEquals(listOf<Short>(77, 88), readShorts(processor.output))
    }
}
