package com.example

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class JklPlayLauncherIconTest {

    @Test
    fun launcherLabelAndApplicationInfoMatchBnjMediaProTv() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("JAAS PLAY TV", context.getString(R.string.app_name))

        val packageManager: PackageManager = context.packageManager
        val appInfo = context.applicationInfo
        assertEquals("JAAS PLAY TV", packageManager.getApplicationLabel(appInfo).toString())
        assertEquals(R.mipmap.ic_launcher, appInfo.icon)
        assertTrue(R.mipmap.ic_launcher_round != 0)
    }

    @Test
    fun adaptiveLauncherIconsLoadForegroundAndBackgroundLayers() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val launcher = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        val roundLauncher = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
        assertTrue(launcher is AdaptiveIconDrawable)
        assertTrue(roundLauncher is AdaptiveIconDrawable)

        val adaptive = launcher as AdaptiveIconDrawable
        assertNotNull(adaptive.background)
        assertNotNull(adaptive.foreground)

        val foregroundLayer = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        assertTrue(foregroundLayer is LayerDrawable)
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_background))
        assertNotNull(ContextCompat.getDrawable(context, R.drawable.ic_launcher_jkl_foreground))
    }

    @Test
    fun foregroundDrawableAssetIs1024Square() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_jkl_foreground, opts)
        assertEquals(1024, opts.outWidth)
        assertEquals(1024, opts.outHeight)
    }
}
