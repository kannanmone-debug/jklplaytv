package com.example

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.adaptive.AdaptiveAppRoot
import com.example.viewmodel.BnjMainViewModel

class MainActivity : FragmentActivity() {

    private val mainViewModel: BnjMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        setContent {
            AdaptiveAppRoot(
                viewModel = mainViewModel,
                activity = this,
                onEnterPipMode = { triggerPictureInPicture() }
            )
        }
        if (savedInstanceState == null) {
            mainViewModel.handleExternalViewIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        mainViewModel.handleExternalViewIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val pState = mainViewModel.playbackController.state.value
        val activeVideo = pState.currentMedia?.isVideo == true
        if (
            pState.isPlaying &&
            activeVideo &&
            (pState.isFullscreenPlayerOpen || pState.isFloatingPlayerActive) &&
            !pState.isVaultSession &&
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
        if (isInPictureInPictureMode && mainViewModel.playbackController.state.value.isFloatingPlayerActive) {
            mainViewModel.playbackController.setFullscreenPlayerOpen(true)
        }
    }

    override fun onStop() {
        super.onStop()
        // Automatically lock Private Media Vault when leaving the app to guarantee security
        mainViewModel.lockVaultNow()
    }

    private fun triggerPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
                mainViewModel.playbackController.setFloatingMiniPlayer(true)
            }
        }
    }
}
