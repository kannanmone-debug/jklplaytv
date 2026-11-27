package com.example

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsController
import com.example.player.BnjPlaybackController
import com.example.player.service.BnjMediaSessionService
import com.example.viewmodel.BnjMainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase6AuditVerificationTest {

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val app = ApplicationProvider.getApplicationContext<Application>()
        SettingsDataStore(app).clearPreferencesForTesting()
    }

    @After
    fun tearDown() = runBlocking {
        try {
            val app = ApplicationProvider.getApplicationContext<Application>()
            SettingsDataStore(app).clearPreferencesForTesting()
        } catch (_: Throwable) {}
        Dispatchers.resetMain()
        try {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
        } catch (_: Throwable) {}
    }

    @Test
    fun `handleExternalViewIntent launches fullscreen playback for external audio URI`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val tempFile = File(app.cacheDir, "external_audit_track.wav")
        tempFile.writeBytes(ByteArray(128))
        val viewModel = BnjMainViewModel(app)
        try {
            viewModel.awaitInitialization()
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(tempFile), "audio/wav")
            }
            viewModel.handleExternalViewIntent(intent)

            withTimeout(5_000L) {
                while (viewModel.playbackController.state.value.currentMedia == null) {
                    kotlinx.coroutines.delay(25L)
                }
            }
            val pState = viewModel.playbackController.state.value
            assertNotNull(pState.currentMedia)
            assertEquals("external_audit_track", pState.currentMedia?.title)
            assertFalse(pState.currentMedia?.isVideo ?: true)
            assertTrue(pState.isFullscreenPlayerOpen)
        } finally {
            viewModel.playbackController.closePlaybackSession()
            viewModel.playbackController.releaseControllerConnection()
            tempFile.delete()
        }
    }

    @Test
    fun `releaseControllerConnection detaches listener cleanly while preserving service sharedPlayer`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val controller = BnjPlaybackController(
            context = app,
            audioEffectsController = AudioEffectsController(),
            onPersistProgress = { _, _ -> }
        )
        val servicePlayer = BnjMediaSessionService.getOrCreatePlayer(app)
        val attached = controller.exoPlayer
        assertSame(servicePlayer, attached)

        // Should detach cleanly without throwing or invalidating service singleton
        controller.releaseControllerConnection()
        assertSame(servicePlayer, BnjMediaSessionService.getOrCreatePlayer(app))
    }
}
