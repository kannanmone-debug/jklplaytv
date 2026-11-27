package com.example

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.tv.ui.BnjTvAppRoot
import com.example.tv.ui.JaasSplashSessionGuard
import com.example.tv.ui.JaasSplashTimeline
import com.example.tv.ui.TvJaasSplashOverlay
import com.example.viewmodel.BnjMainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModelDelegate = viewModels<BnjMainViewModel>()
    private val mainViewModel: BnjMainViewModel
        get() = mainViewModelDelegate.value

    private val isViewModelReady: Boolean
        get() = mainViewModelDelegate.isInitialized()

    private var hasRequestedNotifications = false
    private var externalViewBypassSplash by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isExternalViewLaunch = intent?.action == Intent.ACTION_VIEW
        val shouldShowColdStartSplash = !JaasSplashSessionGuard.hasPlayedThisLaunch && !isExternalViewLaunch

        if (isExternalViewLaunch && savedInstanceState == null) {
            mainViewModel.handleExternalViewIntent(intent)
        }
        if (!shouldShowColdStartSplash) {
            requestNotificationPermissionIfNeeded()
        }

        setContent {
            var showSplash by rememberSaveable { mutableStateOf(shouldShowColdStartSplash) }
            var mountMainShell by rememberSaveable { mutableStateOf(!shouldShowColdStartSplash) }
            var splashDismissedTrigger by remember { mutableIntStateOf(0) }

            val effectiveShowSplash = showSplash && !externalViewBypassSplash
            val effectiveMountMainShell = mountMainShell || externalViewBypassSplash

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(JaasSplashTimeline.DeepForestOuter)
            ) {
                if (effectiveMountMainShell) {
                    BnjTvAppRoot(
                        viewModel = mainViewModel,
                        activity = this@MainActivity,
                        splashDismissedTrigger = splashDismissedTrigger
                    )
                }

                if (effectiveShowSplash) {
                    TvJaasSplashOverlay(
                        onReadyToMountHome = {
                            // Mount ViewModel + Home shell invisibly during stationary 2000-2500ms hold
                            // so cold start to splash frame 0 is instant on Android TV with zero stutter.
                            mountMainShell = true
                        },
                        onFinished = {
                            mountMainShell = true
                            showSplash = false
                            splashDismissedTrigger++
                            requestNotificationPermissionIfNeeded()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (hasRequestedNotifications) return
        hasRequestedNotifications = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            JaasSplashSessionGuard.markCompleted()
            externalViewBypassSplash = true
            mainViewModel.handleExternalViewIntent(intent)
        } else if (isViewModelReady) {
            mainViewModel.handleExternalViewIntent(intent)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!isViewModelReady) return
        val pState = mainViewModel.playbackController.state.value
        val activeVideo = pState.currentMedia?.isVideo == true
        if (
            pState.isPlaying &&
            activeVideo &&
            (pState.isFullscreenPlayerOpen || pState.isFloatingPlayerActive) &&
            !pState.isAudioOnlyVideoMode
        ) {
            triggerPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (!isViewModelReady) return
        if (isInPictureInPictureMode && mainViewModel.playbackController.state.value.isFloatingPlayerActive) {
            mainViewModel.playbackController.setFullscreenPlayerOpen(true)
        }
    }

    internal fun triggerPictureInPicture() {
        if (!isViewModelReady) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                return
            }
            try {
                val media = mainViewModel.playbackController.state.value.currentMedia
                val ratio = if (media != null && media.width > 0 && media.height > 0) {
                    val raw = media.width.toFloat() / media.height.toFloat()
                    when {
                        raw > 2.39f -> Rational(239, 100)
                        raw < 0.42f -> Rational(100, 239)
                        else -> Rational(media.width, media.height)
                    }
                } else {
                    Rational(16, 9)
                }
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(ratio)
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Throwable) {
                // TV devices without PiP support remain safely paused/backgrounded without mobile floating overlay
                val state = mainViewModel.playbackController.state.value
                if (state.isPlaying && state.currentMedia?.isVideo == true && !state.isAudioOnlyVideoMode) {
                    mainViewModel.playbackController.togglePlayPause()
                }
            }
        }
    }
}
