package com.example

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TvPhase5ReleaseReadinessTest {

    @Test
    fun phase5_releaseIdentityAndVersioningMatchProductionSpec() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertEquals("com.aistudio.bnjmediapro.tv", BuildConfig.APPLICATION_ID)
        assertNotEquals("com.aistudio.bnjmediapro.vryx", BuildConfig.APPLICATION_ID)
        assertEquals("1.0.0", BuildConfig.VERSION_NAME)
        assertEquals(1, BuildConfig.VERSION_CODE)

        assertEquals("JAAS PLAY TV", context.getString(R.string.app_name))
        val rationale = context.getString(R.string.permission_rationale_desc)
        assertTrue("Permission rationale must reference JAAS PLAY TV", rationale.contains("JAAS PLAY TV"))
        assertFalse("Permission rationale must not reference legacy JKL PLAY", rationale.contains("JKL PLAY"))
    }

    @Test
    fun phase5_manifestSecurityAndBannerHardeningAudit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val appInfo = context.applicationInfo

        // Cloud/ADB backup must be disabled
        assertEquals(
            "allowBackup must be false for offline privacy",
            0,
            appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP
        )

        // Cleartext HTTP traffic must be explicitly disabled across API 26-36
        assertEquals(
            "usesCleartextTraffic must be false",
            0,
            appInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC
        )

        // TV banner must be configured on both Application and MainActivity
        assertEquals(R.drawable.tv_banner, appInfo.banner)
        val pkgInfo = pm.getPackageInfo(
            context.packageName,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_SERVICES or PackageManager.GET_RECEIVERS or PackageManager.GET_PROVIDERS or PackageManager.GET_PERMISSIONS
        )
        val mainActivity = pkgInfo.activities?.firstOrNull { it.name == MainActivity::class.java.name }
        assertTrue("MainActivity must be declared", mainActivity != null)
        assertEquals("MainActivity must explicitly declare tv_banner", R.drawable.tv_banner, mainActivity!!.banner)
        assertTrue("MainActivity must be exported for LEANBACK_LAUNCHER", mainActivity.exported)

        // Verify USE_BIOMETRIC permission was cleanly removed with Vault
        val requestedPermissions = pkgInfo.requestedPermissions.orEmpty().toList()
        assertFalse(
            "USE_BIOMETRIC must not be requested after Vault removal",
            requestedPermissions.contains(android.Manifest.permission.USE_BIOMETRIC)
        )

        // Verify TV navigation destinations contain strictly the 8 non-Vault items
        assertEquals(
            listOf("home", "movies", "videos", "music", "playlists", "favorites", "search", "settings"),
            com.example.tv.ui.TvDestination.entries.map { it.route }
        )

        // Verify least-privilege exported components: only MainActivity and BnjMediaSessionService among app-owned components
        val exportedAppActivities = pkgInfo.activities.orEmpty()
            .filter { it.name.startsWith("com.example.") && it.exported }
            .map { it.name }
        assertEquals(listOf(MainActivity::class.java.name), exportedAppActivities)

        val exportedAppServices = pkgInfo.services.orEmpty()
            .filter { it.name.startsWith("com.example.") && it.exported }
            .map { it.name }
        assertEquals(listOf("com.example.player.service.BnjMediaSessionService"), exportedAppServices)
    }
}
