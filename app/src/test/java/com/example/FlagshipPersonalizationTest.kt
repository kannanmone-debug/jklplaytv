package com.example

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.example.cast.CastUiState
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.MediaEntity
import com.example.data.local.SettingsDataStore
import com.example.equalizer.AudioEffectsUiState
import com.example.player.PlaybackUiState
import com.example.ui.components.ArtworkResolutionTier
import com.example.ui.components.BnjArtworkCache
import com.example.ui.components.CustomBackgroundManager
import com.example.ui.screens.AppearancePersonalizationPanel
import com.example.ui.screens.MusicPlayerOverlay
import com.example.ui.screens.PersonalizationActions
import com.example.ui.screens.SettingsAndCodecScreen
import com.example.ui.theme.ArtworkDisplayStyle
import com.example.ui.theme.ArtworkFitMode
import com.example.ui.theme.BnjArtworkPaletteExtractor
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.DynamicArtworkScope
import com.example.ui.theme.ExtractedArtworkPalette
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.ThemeAccentPreset
import com.example.ui.theme.ThemePriorityResolver
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.computeGlassSurfaceAlpha
import com.example.ui.theme.computeWallpaperScrimAlphas
import com.example.ui.theme.formatHexColorString
import com.example.ui.theme.hexToHsv
import com.example.ui.theme.hsvToAccentHex
import com.example.ui.theme.parseAndValidateHexColor
import com.example.ui.theme.resolveEffectiveDarkGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FlagshipPersonalizationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var app: Application

    private val sampleCapabilities = DeviceMediaCapabilities(
        deviceModel = "SM-S928B",
        androidSdk = 36,
        displaySupportsHdr10 = true,
        displaySupportsHdr10Plus = true,
        displaySupportsHlg = true,
        displaySupportsDolbyVision = false,
        maxDisplayLuminanceNits = 2600f,
        audioCodecs = emptyList(),
        videoCodecs = emptyList(),
        fallbackEngineStatus = "Media3 Active"
    )

    @Before
    fun setUp() {
        runBlocking {
            Dispatchers.setMain(UnconfinedTestDispatcher())
            app = ApplicationProvider.getApplicationContext()
            SettingsDataStore(app).clearPreferencesForTesting()
            BnjArtworkPaletteExtractor.clearCache()
            BnjArtworkCache.clearCache()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            try {
                SettingsDataStore(app).clearPreferencesForTesting()
                CustomBackgroundManager.deleteCustomBackgroundFiles(app)
            } catch (_: Throwable) {}
            Dispatchers.resetMain()
            try {
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
            } catch (_: Throwable) {}
        }
    }

    @Test
    fun `palette extractor extracts vibrant colors from bitmap and caches result`() {
        runBlocking {
            val redBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(225, 42, 68))
            }

            val blueBitmap = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(32, 165, 240))
            }

            val warmPalette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                bitmap = redBitmap,
                cacheKey = "test:red",
                fallbackSeed = "Warm"
            )
            val coolPalette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
                bitmap = blueBitmap,
                cacheKey = "test:blue",
                fallbackSeed = "Cool"
            )

            assertTrue(warmPalette.isFromRealBitmap)
            assertTrue(coolPalette.isFromRealBitmap)
            assertTrue(
                "Red artwork must yield red-dominant channel",
                warmPalette.vibrantAccent.red > warmPalette.vibrantAccent.blue
            )
            assertTrue(
                "Blue artwork must yield blue-dominant channel",
                coolPalette.vibrantAccent.blue > coolPalette.vibrantAccent.red
            )
            assertNotNull(BnjArtworkPaletteExtractor.getCachedPalette("test:red"))
            redBitmap.recycle()
            blueBitmap.recycle()
        }
    }

    @Test
    fun `theme priority resolver enforces artwork then custom background then manual preset`() {
        val artworkPalette = ExtractedArtworkPalette(
            dominantColor = Color(0xFFE11D48),
            vibrantAccent = Color(0xFFFB7185),
            secondaryGlow = Color(0xFFF59E0B),
            surfaceWash = Color(0x33FB7185),
            isDarkArtwork = true,
            isFromRealBitmap = true
        )
        val bgPalette = ExtractedArtworkPalette(
            dominantColor = Color(0xFF059669),
            vibrantAccent = Color(0xFF34D399),
            secondaryGlow = Color(0xFF0EA5E9),
            surfaceWash = Color(0x3334D399),
            isDarkArtwork = true,
            isFromRealBitmap = true
        )

        // 1. Dynamic artwork ON, PLAYER_ONLY: player uses artwork, shell uses custom background match
        val playerOnlySettings = AppSettingsState(
            themeAccentPreset = ThemeAccentPreset.ELECTRIC_INDIGO.id,
            customBgEnabled = true,
            customBgFilePath = "/tmp/fake_bg.jpg",
            customBgMatchAccent = true,
            dynamicArtworkThemeEnabled = true,
            dynamicArtworkScope = DynamicArtworkScope.PLAYER_ONLY.id,
            dynamicArtworkIntensity = 1.0f
        )

        val playerColors = ThemePriorityResolver.resolveEffectiveTheme(
            settings = playerOnlySettings,
            activeArtworkPalette = artworkPalette,
            customBackgroundPalette = bgPalette,
            isPlayerContext = true
        )
        assertTrue(playerColors.isDrivenByArtwork)
        assertFalse(playerColors.isDrivenByBackground)

        val shellColors = ThemePriorityResolver.resolveEffectiveTheme(
            settings = playerOnlySettings,
            activeArtworkPalette = artworkPalette,
            customBackgroundPalette = bgPalette,
            isPlayerContext = false
        )
        assertFalse(shellColors.isDrivenByArtwork)
        assertTrue(shellColors.isDrivenByBackground)

        // 2. APP_WIDE scope: shell also adopts artwork theme
        val globalArtworkSettings = playerOnlySettings.copy(dynamicArtworkScope = DynamicArtworkScope.APP_WIDE.id)
        val globalShellColors = ThemePriorityResolver.resolveEffectiveTheme(
            settings = globalArtworkSettings,
            activeArtworkPalette = artworkPalette,
            customBackgroundPalette = bgPalette,
            isPlayerContext = false
        )
        assertTrue(globalShellColors.isDrivenByArtwork)

        // 3. Dynamic artwork OFF & background match OFF -> manual preset
        val manualSettings = playerOnlySettings.copy(
            dynamicArtworkThemeEnabled = false,
            customBgMatchAccent = false
        )
        val manualColors = ThemePriorityResolver.resolveEffectiveTheme(
            settings = manualSettings,
            activeArtworkPalette = artworkPalette,
            customBackgroundPalette = bgPalette,
            isPlayerContext = true
        )
        assertFalse(manualColors.isDrivenByArtwork)
        assertFalse(manualColors.isDrivenByBackground)
        assertEquals(ThemeAccentPreset.ELECTRIC_INDIGO.label, manualColors.sourceLabel)
    }

    @Test
    fun `p1 theme modes amoled dark pearl light and system default resolve and adapt contrast`() {
        assertTrue(BnjThemeMode.resolveIsDarkGlass("AMOLED_DARK", false, false))
        assertFalse(BnjThemeMode.resolveIsDarkGlass("PEARL_LIGHT", true, true))
        assertTrue(BnjThemeMode.resolveIsDarkGlass("SYSTEM_DEFAULT", false, true))
        assertFalse(BnjThemeMode.resolveIsDarkGlass("SYSTEM_DEFAULT", true, false))

        // Verify all 7 presets exist and adapt cleanly for both AMOLED Dark and Frosted Pearl Light
        val presets = ThemeAccentPreset.selectableSwatches
        assertEquals(7, presets.size)
        val expectedIds = listOf(
            "LIQUID_CYAN",
            "ELECTRIC_INDIGO",
            "VELVET_VIOLET",
            "CRIMSON_ROSE",
            "EMERALD_PULSE",
            "SUNSET_AMBER",
            "ARCTIC_SILVER"
        )
        assertEquals(expectedIds, presets.map { it.id })

        presets.forEach { preset ->
            val rawColor = Color(preset.primaryHex)
            val darkAdapted = adaptiveGlassAccent(rawColor, isDarkGlass = true)
            val lightAdapted = adaptiveGlassAccent(rawColor, isDarkGlass = false)
            assertEquals(1f, darkAdapted.alpha, 0.001f)
            assertEquals(1f, lightAdapted.alpha, 0.001f)
        }
    }

    @Test
    fun `p1 custom hex validation hsv conversion and vibrance boost handle valid and invalid inputs safely`() {
        // Valid 6-digit, 3-digit shorthand, 8-digit ARGB, and 0x-prefixed hex inputs
        assertEquals(0xFFFF3366L, parseAndValidateHexColor("#FF3366"))
        assertEquals(0xFF00E5FFL, parseAndValidateHexColor("00e5ff"))
        assertEquals(0xFF33BBFFL, parseAndValidateHexColor("#3BF"))
        assertEquals(0xFF10B981L, parseAndValidateHexColor("0x10B981"))
        assertEquals(0xFF6366F1L, parseAndValidateHexColor("#AA6366F1"))

        // Invalid hex strings must return null and never throw/crash
        assertNull(parseAndValidateHexColor(""))
        assertNull(parseAndValidateHexColor("   "))
        assertNull(parseAndValidateHexColor("#ZZZZZZ"))
        assertNull(parseAndValidateHexColor("not-a-color"))
        assertNull(parseAndValidateHexColor("#12345"))
        assertNull(parseAndValidateHexColor("#123456789"))

        // HSV <-> HEX round-trip
        val generatedHex = hsvToAccentHex(hue = 330f, saturation = 0.85f, value = 0.96f)
        val formatted = formatHexColorString(generatedHex)
        assertTrue(formatted.startsWith("#") && formatted.length == 7)
        val hsvBack = hexToHsv(generatedHex)
        assertEquals(330f, hsvBack[0], 3f)
        assertEquals(0.85f, hsvBack[1], 0.03f)
    }

    @Test
    fun `p2 custom background manager downsamples 4k photos to max 2048px preserves aspect ratio and handles atomic replacement`() {
        runBlocking {
            // High-res 4K check downsamples cleanly, while <=1440px hero stays lossless (inSampleSize == 1)
            assertTrue(CustomBackgroundManager.calculateInSampleSize(4000, 3000, 2048, 2048) >= 2)
            assertEquals(1, CustomBackgroundManager.calculateInSampleSize(1080, 1080, 1440, 1440))

            // Verify strict <= 2048px scaling with aspect ratio preservation (e.g. 3000x1500 -> 2048x1024)
            val oversizedBmp = Bitmap.createBitmap(3000, 1500, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(30, 140, 220))
            }
            val scaledBmp = CustomBackgroundManager.scaleDownToMaxDimension(oversizedBmp, 2048)
            assertTrue("Width must be capped at <= 2048px", scaledBmp.width <= 2048)
            assertTrue("Height must be capped at <= 2048px", scaledBmp.height <= 2048)
            assertEquals(2048, scaledBmp.width)
            assertEquals(1024, scaledBmp.height)
            scaledBmp.recycle()

            // Verify EXIF 90-degree rotation swaps width and height
            val landscapeBmp = Bitmap.createBitmap(400, 200, Bitmap.Config.ARGB_8888)
            val rotatedBmp = CustomBackgroundManager.applyExifOrientation(
                landscapeBmp,
                android.media.ExifInterface.ORIENTATION_ROTATE_90
            )
            assertEquals(200, rotatedBmp.width)
            assertEquals(400, rotatedBmp.height)
            rotatedBmp.recycle()

            // 1. Import first valid wallpaper
            val tempSource1 = File(app.cacheDir, "picker_source_1.jpg")
            val bmp1 = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(45, 190, 150))
            }
            FileOutputStream(tempSource1).use { fos ->
                bmp1.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
            bmp1.recycle()

            val imported1 = CustomBackgroundManager.importBackgroundImageFromUri(app, Uri.fromFile(tempSource1))
            assertNotNull(imported1)
            val savedFile1 = File(imported1!!.savedFilePath)
            assertTrue(savedFile1.exists())
            assertTrue(CustomBackgroundManager.isValidWallpaperFile(imported1.savedFilePath))
            assertTrue(imported1.extractedAccentHex != 0L)
            assertNotNull(CustomBackgroundManager.getCachedBitmap(imported1.savedFilePath))

            // 2. Cancelled selection (null URI) or invalid/corrupt URI must NOT delete the active wallpaper
            assertNull(CustomBackgroundManager.importBackgroundImageFromUri(app, null))
            val invalidFile = File(app.cacheDir, "invalid_corrupt.jpg").apply {
                writeText("not-a-valid-jpeg-image")
            }
            assertNull(CustomBackgroundManager.importBackgroundImageFromUri(app, Uri.fromFile(invalidFile)))
            assertTrue(
                "Active wallpaper must remain intact when replacement import fails",
                savedFile1.exists() && CustomBackgroundManager.isValidWallpaperFile(savedFile1.absolutePath)
            )
            invalidFile.delete()

            // 3. Replace wallpaper with a second valid image -> old file is cleaned up after new file succeeds
            val tempSource2 = File(app.cacheDir, "picker_source_2.jpg")
            val bmp2 = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(220, 60, 110))
            }
            FileOutputStream(tempSource2).use { fos ->
                bmp2.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }
            bmp2.recycle()

            val imported2 = CustomBackgroundManager.importBackgroundImageFromUri(app, Uri.fromFile(tempSource2))
            assertNotNull(imported2)
            val savedFile2 = File(imported2!!.savedFilePath)
            assertTrue(savedFile2.exists())
            assertFalse("Old wallpaper file must be removed after new replacement succeeds", savedFile1.exists())

            // 4. Simulate external deletion / missing file -> loadBackgroundBitmapAsync returns null safely
            savedFile2.delete()
            assertFalse(CustomBackgroundManager.isValidWallpaperFile(savedFile2.absolutePath))
            assertNull(CustomBackgroundManager.loadBackgroundBitmapAsync(savedFile2.absolutePath))

            CustomBackgroundManager.deleteCustomBackgroundFiles(app, imported2.savedFilePath)
            tempSource1.delete()
            tempSource2.delete()
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `p2 wallpaper controls blur scrim match accent enable disable and remove update preview and theme priority`() {
        val tempWallpaper = File(app.filesDir, "custom_theme_bg/test_wallpaper_p2.jpg").apply {
            parentFile?.mkdirs()
        }
        val bmp = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(40, 200, 140))
        }
        FileOutputStream(tempWallpaper).use { fos ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        }
        bmp.recycle()

        val extractedPalette = BnjArtworkPaletteExtractor.extractQuantizedPalette(
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
                eraseColor(AndroidColor.rgb(40, 200, 140))
            },
            "BNJ_BG"
        )

        var latestState = AppSettingsState(
            themeAccentPreset = ThemeAccentPreset.CRIMSON_ROSE.id,
            customBgEnabled = true,
            customBgFilePath = tempWallpaper.absolutePath,
            customBgBlurIntensity = 0.50f,
            customBgDimAmount = 0.45f,
            customBgMatchAccent = true,
            customBgExtractedHex = 0xFF28C88CL
        )

        composeRule.setContent {
            var settingsState by remember { mutableStateOf(latestState) }
            latestState = settingsState
            val effectiveDark = resolveEffectiveDarkGlass(settingsState)
            BnjLiquidGlassTheme(
                isDarkGlass = effectiveDark,
                themeMode = BnjThemeMode.resolveActiveMode(settingsState.themeMode, settingsState.isDarkGlass),
                glassOpacity = settingsState.glassOpacity,
                blurIntensity = settingsState.blurIntensity,
                glassBorderIntensity = settingsState.glassBorderIntensity,
                glassStylePreset = GlassStylePreset.fromId(settingsState.glassStylePreset),
                customBgActive = settingsState.customBgEnabled && settingsState.customBgFilePath.isNotBlank()
            ) {
                SettingsAndCodecScreen(
                    settings = settingsState,
                    capabilities = sampleCapabilities,
                    castState = CastUiState(),
                    eqState = AudioEffectsUiState(),
                    customBackgroundPalette = extractedPalette,
                    onToggleDarkGlass = { settingsState = settingsState.copy(isDarkGlass = it) },
                    onChangeGlassOpacity = { settingsState = settingsState.copy(glassOpacity = it) },
                    onChangeBlurIntensity = { settingsState = settingsState.copy(blurIntensity = it) },
                    onResetAppearance = { settingsState = AppSettingsState() },
                    onChangeDecoderMode = {},
                    onToggleGapless = {},
                    onUpdateSubtitleStyle = { _, _, _, _, _ -> },
                    onToggleCastEnabled = {},
                    onSelectCastReceiver = {},
                    onRefreshCodecCapabilities = {},
                    personalizationActions = PersonalizationActions(
                        onToggleCustomBgEnabled = { enabled ->
                            settingsState = settingsState.copy(customBgEnabled = enabled)
                        },
                        onRemoveCustomBackground = {
                            settingsState = settingsState.copy(
                                customBgEnabled = false,
                                customBgFilePath = "",
                                customBgExtractedHex = 0L
                            )
                        },
                        onChangeCustomBgBlur = { blur ->
                            settingsState = settingsState.copy(customBgBlurIntensity = blur)
                        },
                        onChangeCustomBgDim = { dim ->
                            settingsState = settingsState.copy(customBgDimAmount = dim)
                        },
                        onToggleCustomBgMatchAccent = { match ->
                            settingsState = settingsState.copy(customBgMatchAccent = match)
                        }
                    )
                )
            }
        }

        // Verify all P2 wallpaper controls are visible and active
        composeRule.onNodeWithTag("pick_custom_bg_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("remove_custom_bg_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("custom_bg_enable_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("custom_bg_blur_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("custom_bg_dim_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("custom_bg_match_accent_switch").performScrollTo().assertIsDisplayed()

        // Toggle Match Accent OFF -> falls back immediately to manual preset
        composeRule.onNodeWithTag("custom_bg_match_accent_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(latestState.customBgMatchAccent)

        // Remove Wallpaper -> clears path & disables wallpaper
        composeRule.onNodeWithTag("remove_custom_bg_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(latestState.customBgEnabled)
        assertEquals("", latestState.customBgFilePath)

        tempWallpaper.delete()
    }

    @Test
    fun `settings datastore persists p1 theme mode custom accent hsv vibrance and restores backup safely`() {
        runBlocking {
            val store = SettingsDataStore(app)
            store.setThemeMode(BnjThemeMode.SYSTEM_DEFAULT.id)
            store.saveCustomAccentStudio(
                argbHex = 0xFFFF3366L,
                hue = 345f,
                saturation = 0.80f,
                vibranceBoost = 1.22f
            )
            store.applyGlassStylePreset(
                presetId = GlassStylePreset.CLEAR_CRYSTAL.id,
                opacity = GlassStylePreset.CLEAR_CRYSTAL.defaultOpacity,
                blur = GlassStylePreset.CLEAR_CRYSTAL.defaultBlur,
                border = GlassStylePreset.CLEAR_CRYSTAL.defaultBorder,
                tintStrength = GlassStylePreset.CLEAR_CRYSTAL.defaultTintStrength
            )
            store.setArtworkDisplayStyle(ArtworkDisplayStyle.VINYL_DISC.id)
            store.setArtworkFitMode(ArtworkFitMode.SMART_FIT.id)
            store.setDynamicArtworkScope(DynamicArtworkScope.APP_WIDE.id)
            store.setDynamicArtworkIntensity(0.92f)
            store.setCustomBackgroundPhoto("/private/local_bg.jpg", 0xFF34D399L)

            val current = store.settingsFlow.first()
            assertEquals(BnjThemeMode.SYSTEM_DEFAULT.id, current.themeMode)
            assertEquals(ThemeAccentPreset.CUSTOM.id, current.themeAccentPreset)
            assertEquals(0xFFFF3366L, current.customAccentColorHex)
            assertEquals(345f, current.customAccentHue, 0.5f)
            assertEquals(0.80f, current.customAccentSaturation, 0.02f)
            assertEquals(1.22f, current.accentSaturationBoost, 0.01f)
            assertEquals(GlassStylePreset.CLEAR_CRYSTAL.id, current.glassStylePreset)
            assertEquals(ArtworkDisplayStyle.VINYL_DISC.id, current.artworkDisplayStyle)
            assertEquals(ArtworkFitMode.SMART_FIT.id, current.artworkFitMode)
            assertEquals(DynamicArtworkScope.APP_WIDE.id, current.dynamicArtworkScope)

            val backupJson = SettingsDataStore.serializeBackupJson(current, "Nov 14, 2026 12:00")
            assertFalse("Backup JSON must never contain vault PIN credentials", backupJson.contains("vaultPinHash"))

            // Reset custom accent defaults first
            store.resetCustomAccentDefaults()
            val customResetState = store.settingsFlow.first()
            assertEquals(ThemeAccentPreset.LIQUID_CYAN.id, customResetState.themeAccentPreset)
            assertEquals(0xFF38BDF8L, customResetState.customAccentColorHex)
            assertEquals(1.0f, customResetState.accentSaturationBoost, 0.01f)

            store.resetAppearanceDefaults()
            val resetState = store.settingsFlow.first()
            assertEquals(BnjThemeMode.AMOLED_DARK.id, resetState.themeMode)
            assertEquals(ThemeAccentPreset.LIQUID_CYAN.id, resetState.themeAccentPreset)
            assertEquals(ArtworkDisplayStyle.ROUNDED_CINEMA.id, resetState.artworkDisplayStyle)
            assertFalse(resetState.customBgEnabled)

            val restore = store.applyRestoredSettingsAtomically(backupJson, emptySet())
            assertTrue(restore.success)
            val restoredState = store.settingsFlow.first()
            assertEquals(BnjThemeMode.SYSTEM_DEFAULT.id, restoredState.themeMode)
            assertEquals(ThemeAccentPreset.CUSTOM.id, restoredState.themeAccentPreset)
            assertEquals(0xFFFF3366L, restoredState.customAccentColorHex)
            assertEquals(345f, restoredState.customAccentHue, 0.5f)
            assertEquals(0.80f, restoredState.customAccentSaturation, 0.02f)
            assertEquals(1.22f, restoredState.accentSaturationBoost, 0.01f)
            assertEquals(GlassStylePreset.CLEAR_CRYSTAL.id, restoredState.glassStylePreset)
            assertEquals(ArtworkDisplayStyle.VINYL_DISC.id, restoredState.artworkDisplayStyle)
            // Local background photo path must not be force-enabled by imported backup JSON
            assertFalse(restoredState.customBgEnabled)
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `settings appearance panel interactively updates theme modes accent studio and live preview`() {
        var latestState = AppSettingsState()
        composeRule.setContent {
            var settingsState by remember { mutableStateOf(AppSettingsState()) }
            latestState = settingsState
            val effectiveDark = resolveEffectiveDarkGlass(settingsState)
            BnjLiquidGlassTheme(
                isDarkGlass = effectiveDark,
                themeMode = BnjThemeMode.resolveActiveMode(settingsState.themeMode, settingsState.isDarkGlass),
                glassOpacity = settingsState.glassOpacity,
                blurIntensity = settingsState.blurIntensity,
                glassBorderIntensity = settingsState.glassBorderIntensity,
                glassStylePreset = GlassStylePreset.fromId(settingsState.glassStylePreset)
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppearancePersonalizationPanel(
                        settings = settingsState,
                        isAdvancedMode = true,
                        onToggleDarkGlass = { settingsState = settingsState.copy(isDarkGlass = it) },
                        onChangeGlassOpacity = { settingsState = settingsState.copy(glassOpacity = it) },
                        onChangeBlurIntensity = { settingsState = settingsState.copy(blurIntensity = it) },
                        onChangeGlassBorderIntensity = { settingsState = settingsState.copy(glassBorderIntensity = it) },
                        onResetAppearance = { settingsState = AppSettingsState() },
                        actions = PersonalizationActions(
                            onSelectThemeMode = { mode ->
                                settingsState = settingsState.copy(themeMode = mode)
                            },
                            onSelectAccentPreset = { preset ->
                                settingsState = settingsState.copy(themeAccentPreset = preset)
                            },
                            onSelectCustomAccentHex = { hex ->
                                settingsState = settingsState.copy(
                                    themeAccentPreset = ThemeAccentPreset.CUSTOM.id,
                                    customAccentColorHex = hex
                                )
                            },
                            onChangeCustomAccentHsv = { h, s ->
                                settingsState = settingsState.copy(
                                    customAccentHue = h,
                                    customAccentSaturation = s
                                )
                            },
                            onSaveCustomAccentStudio = { hex, h, s, vib ->
                                settingsState = settingsState.copy(
                                    themeAccentPreset = ThemeAccentPreset.CUSTOM.id,
                                    customAccentColorHex = hex,
                                    customAccentHue = h,
                                    customAccentSaturation = s,
                                    accentSaturationBoost = vib
                                )
                            },
                            onResetCustomAccent = {
                                settingsState = settingsState.copy(
                                    themeAccentPreset = ThemeAccentPreset.LIQUID_CYAN.id,
                                    customAccentColorHex = 0xFF38BDF8L,
                                    customAccentHue = 199f,
                                    customAccentSaturation = 0.78f,
                                    accentSaturationBoost = 1.0f
                                )
                            },
                            onApplyGlassStylePreset = { preset ->
                                settingsState = settingsState.copy(
                                    glassStylePreset = preset.id,
                                    glassOpacity = preset.defaultOpacity,
                                    blurIntensity = preset.defaultBlur
                                )
                            },
                            onSelectArtworkDisplayStyle = { style ->
                                settingsState = settingsState.copy(artworkDisplayStyle = style)
                            }
                        )
                    )
                }
            }
        }

        // Switch theme modes: Pearl Light -> System Default -> AMOLED Dark
        composeRule.onNodeWithTag("settings_light_glass_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(BnjThemeMode.PEARL_LIGHT.id, latestState.themeMode)
        assertFalse(latestState.isDarkGlass)

        composeRule.onNodeWithTag("settings_system_glass_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(BnjThemeMode.SYSTEM_DEFAULT.id, latestState.themeMode)

        composeRule.onNodeWithTag("settings_dark_glass_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(BnjThemeMode.AMOLED_DARK.id, latestState.themeMode)
        assertTrue(latestState.isDarkGlass)

        // Open Custom Accent Studio and validate HEX input (invalid & valid)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("accent_preset_CUSTOM").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("custom_accent_studio_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("custom_accent_hex_input").performScrollTo().performTextReplacement("#ZZZZZZ")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("custom_accent_hex_error").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("custom_accent_hex_input").performScrollTo().performTextReplacement("#FF3366")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("save_custom_accent_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeAccentPreset.CUSTOM.id, latestState.themeAccentPreset)
        assertEquals(0xFFFF3366L, latestState.customAccentColorHex)

        // Select Crimson Rose preset, Clear Crystal glass preset, and Vinyl Disc artwork style
        composeRule.onNodeWithTag("accent_preset_CRIMSON_ROSE").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("artwork_style_VINYL_DISC").performScrollTo().performClick()
        composeRule.waitForIdle()

        assertEquals(ThemeAccentPreset.CRIMSON_ROSE.id, latestState.themeAccentPreset)
        assertEquals(ArtworkDisplayStyle.VINYL_DISC.id, latestState.artworkDisplayStyle)

        composeRule.onNodeWithText("Crimson Rose").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `music player overlay renders vinyl disc smart fit and pearl light theme cleanly`() {
        val sampleTrack = MediaEntity(
            uri = "content://media/external/audio/media/991",
            title = "Audiophile Vinyl Studio Master",
            artist = "BNJ Philharmonic",
            album = "Pearl Sessions",
            durationMs = 210_000L,
            sizeBytes = 32_000_000L,
            mimeType = "audio/flac",
            codecName = "FLAC",
            bitrate = 1411000,
            sampleRateHz = 96000,
            isVideo = false,
            isFavorite = false
        )

        val customSettings = AppSettingsState(
            isDarkGlass = false,
            themeMode = BnjThemeMode.PEARL_LIGHT.id,
            themeAccentPreset = ThemeAccentPreset.EMERALD_PULSE.id,
            glassStylePreset = GlassStylePreset.FROSTED_SILK.id,
            artworkDisplayStyle = ArtworkDisplayStyle.VINYL_DISC.id,
            artworkFitMode = ArtworkFitMode.SMART_FIT.id,
            artworkHighClarityEnabled = true,
            dynamicArtworkThemeEnabled = true
        )

        composeRule.setContent {
            MusicPlayerOverlay(
                playbackState = PlaybackUiState(
                    currentMedia = sampleTrack,
                    isPlaying = false,
                    positionMs = 45_000L,
                    durationMs = 210_000L,
                    queue = listOf(sampleTrack)
                ),
                eqState = AudioEffectsUiState(),
                customPresets = emptyList(),
                onCloseFullscreen = {},
                onTogglePlayPause = {},
                onSeekTo = {},
                onSkipNext = {},
                onSkipPrevious = {},
                onToggleShuffle = {},
                onCycleRepeat = {},
                onToggleFavorite = {},
                onSetPlaybackSpeed = {},
                onStartSleepTimer = { _, _ -> },
                onRemoveFromQueue = {},
                onSelectQueueIndex = {},
                onSetEqEnabled = {},
                onSetEqBand = { _, _ -> },
                onSetBassBoost = {},
                onSetVirtualizer = {},
                onApplyPreset = {},
                onSaveCustomPreset = {},
                settings = customSettings
            )
        }

        composeRule.onNodeWithTag("fullscreen_music_player").assertIsDisplayed()
        composeRule.onNodeWithTag("music_artwork_gesture_surface").assertIsDisplayed()
        composeRule.onNodeWithTag("music_player_play_pause_button").assertIsDisplayed()
        composeRule.onNodeWithTag("music_player_track_title").assertIsDisplayed()
    }

    @Test
    fun `p3 five glass presets and datastore persistence backup and restore round trip`() {
        runBlocking {
            val presets = GlassStylePreset.entries
            assertEquals(5, presets.size)
            assertEquals(
                listOf("Transparent Liquid Glass", "Frosted Silk", "Clear Crystal", "Deep Obsidian", "Satin Matte"),
                presets.map { it.label }
            )
            // Legacy alias maps cleanly to DEEP_OBSIDIAN
            assertEquals(GlassStylePreset.DEEP_OBSIDIAN, GlassStylePreset.fromId("OBSIDIAN_SMOKE"))
            assertEquals(GlassStylePreset.DEEP_OBSIDIAN, GlassStylePreset.fromId("DEEP_OBSIDIAN"))

            presets.forEach { preset ->
                assertTrue(preset.defaultOpacity in 0.0f..1.0f)
                assertTrue(preset.defaultBlur in 0.0f..1.0f)
                assertTrue(preset.defaultGlow in 0.0f..1.0f)
                assertTrue(preset.defaultBorder in 0.0f..1.0f)
                assertTrue(preset.defaultTintStrength in 0.0f..1.0f)
                assertTrue(preset.defaultCornerRadiusDp in 8f..32f)
            }

            val store = SettingsDataStore(app)
            store.applyGlassStylePreset(
                presetId = GlassStylePreset.DEEP_OBSIDIAN.id,
                opacity = GlassStylePreset.DEEP_OBSIDIAN.defaultOpacity,
                blur = GlassStylePreset.DEEP_OBSIDIAN.defaultBlur,
                glow = GlassStylePreset.DEEP_OBSIDIAN.defaultGlow,
                border = GlassStylePreset.DEEP_OBSIDIAN.defaultBorder,
                tintEnabled = GlassStylePreset.DEEP_OBSIDIAN.defaultTintEnabled,
                tintStrength = GlassStylePreset.DEEP_OBSIDIAN.defaultTintStrength,
                cornerRadiusDp = GlassStylePreset.DEEP_OBSIDIAN.defaultCornerRadiusDp
            )

            val obsidianState = store.settingsFlow.first()
            assertEquals(GlassStylePreset.DEEP_OBSIDIAN.id, obsidianState.glassStylePreset)
            assertEquals(0.62f, obsidianState.glassOpacity, 0.01f)
            assertEquals(0.78f, obsidianState.blurIntensity, 0.01f)
            assertEquals(0.75f, obsidianState.glassGlowStrength, 0.01f)
            assertEquals(0.54f, obsidianState.glassBorderIntensity, 0.01f)
            assertTrue(obsidianState.glassTintEnabled)
            assertEquals(0.48f, obsidianState.glassTintStrength, 0.01f)
            assertEquals(20f, obsidianState.glassCornerRadiusDp, 0.1f)

            // Test individual P3 setters and full 0.0..1.0 transparency range clamping
            store.setGlassOpacity(-0.1f)
            assertEquals(0.0f, store.settingsFlow.first().glassOpacity, 0.005f)
            store.setGlassOpacity(1.15f) // clamps to 1.0f
            store.setBlurIntensity(0.75f) // 24dp
            store.setGlassGlowStrength(0.88f)
            store.setGlassBorderIntensity(0.92f)
            store.setGlassTintEnabled(false)
            store.setGlassTintStrength(0.64f)
            store.setGlassCornerRadiusDp(30f)

            val customGlass = store.settingsFlow.first()
            assertEquals(1.0f, customGlass.glassOpacity, 0.01f)
            assertEquals(0.75f, customGlass.blurIntensity, 0.01f)
            assertEquals(0.88f, customGlass.glassGlowStrength, 0.01f)
            assertEquals(0.92f, customGlass.glassBorderIntensity, 0.01f)
            assertFalse(customGlass.glassTintEnabled)
            assertEquals(0.64f, customGlass.glassTintStrength, 0.01f)
            assertEquals(30f, customGlass.glassCornerRadiusDp, 0.1f)

            val backupJson = SettingsDataStore.serializeBackupJson(customGlass, "Nov 14, 2026 14:00")
            store.resetAppearanceDefaults()
            val resetState = store.settingsFlow.first()
            assertEquals(GlassStylePreset.FROSTED_SILK.id, resetState.glassStylePreset)
            assertTrue(resetState.glassTintEnabled)
            assertEquals(0.65f, resetState.glassGlowStrength, 0.01f)
            assertEquals(18f, resetState.glassCornerRadiusDp, 0.1f)

            val restoreResult = store.applyRestoredSettingsAtomically(backupJson, emptySet())
            assertTrue(restoreResult.success)
            val restored = store.settingsFlow.first()
            assertEquals(GlassStylePreset.DEEP_OBSIDIAN.id, restored.glassStylePreset)
            assertEquals(1.0f, restored.glassOpacity, 0.01f)
            assertEquals(0.75f, restored.blurIntensity, 0.01f)
            assertEquals(0.88f, restored.glassGlowStrength, 0.01f)
            assertEquals(0.92f, restored.glassBorderIntensity, 0.01f)
            assertFalse(restored.glassTintEnabled)
            assertEquals(0.64f, restored.glassTintStrength, 0.01f)
            assertEquals(30f, restored.glassCornerRadiusDp, 0.1f)
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `p3 liquid glass studio presets all six controls and live preview update in real time`() {
        var latestState = AppSettingsState()
        composeRule.setContent {
            var settingsState by remember { mutableStateOf(AppSettingsState()) }
            latestState = settingsState
            val effectiveDark = resolveEffectiveDarkGlass(settingsState)
            BnjLiquidGlassTheme(
                isDarkGlass = effectiveDark,
                themeMode = BnjThemeMode.resolveActiveMode(settingsState.themeMode, settingsState.isDarkGlass),
                glassOpacity = settingsState.glassOpacity,
                blurIntensity = settingsState.blurIntensity,
                glassGlowStrength = settingsState.glassGlowStrength,
                glassBorderIntensity = settingsState.glassBorderIntensity,
                glassStylePreset = GlassStylePreset.fromId(settingsState.glassStylePreset),
                glassTintEnabled = settingsState.glassTintEnabled,
                glassTintStrength = settingsState.glassTintStrength,
                glassCornerRadiusDp = settingsState.glassCornerRadiusDp
            ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppearancePersonalizationPanel(
                        settings = settingsState,
                        isAdvancedMode = true,
                        onToggleDarkGlass = { settingsState = settingsState.copy(isDarkGlass = it) },
                        onChangeGlassOpacity = { settingsState = settingsState.copy(glassOpacity = it) },
                        onChangeBlurIntensity = { settingsState = settingsState.copy(blurIntensity = it) },
                        onChangeGlassBorderIntensity = { settingsState = settingsState.copy(glassBorderIntensity = it) },
                        onResetAppearance = { settingsState = AppSettingsState() },
                        actions = PersonalizationActions(
                            onApplyGlassStylePreset = { preset ->
                                settingsState = settingsState.copy(
                                    glassStylePreset = preset.id,
                                    glassOpacity = preset.defaultOpacity,
                                    blurIntensity = preset.defaultBlur,
                                    glassGlowStrength = preset.defaultGlow,
                                    glassBorderIntensity = preset.defaultBorder,
                                    glassTintEnabled = preset.defaultTintEnabled,
                                    glassTintStrength = preset.defaultTintStrength,
                                    glassCornerRadiusDp = preset.defaultCornerRadiusDp
                                )
                            },
                            onChangeGlassGlowStrength = { glow ->
                                settingsState = settingsState.copy(glassGlowStrength = glow)
                            },
                            onToggleGlassTintEnabled = { enabled ->
                                settingsState = settingsState.copy(glassTintEnabled = enabled)
                            },
                            onChangeGlassTintStrength = { tint ->
                                settingsState = settingsState.copy(glassTintStrength = tint)
                            },
                            onChangeGlassCornerRadiusDp = { radius ->
                                settingsState = settingsState.copy(glassCornerRadiusDp = radius)
                            }
                        )
                    )
                }
            }
        }

        // Verify all 4 P3 glass presets and all 6 P3 controls are visible
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("glass_preset_FROSTED_SILK").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_preset_CLEAR_CRYSTAL").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_preset_DEEP_OBSIDIAN").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_preset_SATIN_MATTE").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("glass_opacity_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_blur_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_glow_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_border_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_tint_enable_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_tint_slider").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("glass_corner_radius_slider").performScrollTo().assertIsDisplayed()

        // Switch to Deep Obsidian preset -> updates state immediately
        composeRule.onNodeWithTag("glass_preset_DEEP_OBSIDIAN").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(GlassStylePreset.DEEP_OBSIDIAN.id, latestState.glassStylePreset)
        assertEquals(0.62f, latestState.glassOpacity, 0.01f)
        assertEquals(20f, latestState.glassCornerRadiusDp, 0.1f)

        // Switch to Satin Matte preset -> updates state immediately
        composeRule.onNodeWithTag("glass_preset_SATIN_MATTE").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(GlassStylePreset.SATIN_MATTE.id, latestState.glassStylePreset)
        assertEquals(0.46f, latestState.glassOpacity, 0.01f)
        assertEquals(14f, latestState.glassCornerRadiusDp, 0.1f)

        // Toggle Glass Accent Tint OFF -> updates state
        composeRule.onNodeWithTag("glass_tint_enable_switch").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(latestState.glassTintEnabled)
    }

    @Test
    fun `p4 artwork resolution tiers 360px thumbnail 1024px detail 1440px hero aspect ratio and no upscaling`() {
        // 1. Create a 2400x1800 (4:3 aspect ratio) source bitmap and compress to JPEG bytes
        val largeSource = Bitmap.createBitmap(2400, 1800, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(24, 160, 220))
        }
        val jpegBytes = ByteArrayOutputStream().use { out ->
            largeSource.compress(Bitmap.CompressFormat.JPEG, 92, out)
            out.toByteArray()
        }
        assertTrue(BnjArtworkCache.hasValidImageSignature(jpegBytes))

        // High Clarity ON: THUMBNAIL = 360px, DETAIL = 1024px, HERO = 1440px
        val thumbHc = BnjArtworkCache.decodeArtworkBytesForTier(
            jpegBytes,
            ArtworkResolutionTier.THUMBNAIL,
            highClarity = true
        )
        assertNotNull(thumbHc)
        assertEquals(360, thumbHc!!.width)
        assertEquals(270, thumbHc.height)

        val detailHc = BnjArtworkCache.decodeArtworkBytesForTier(
            jpegBytes,
            ArtworkResolutionTier.DETAIL,
            highClarity = true
        )
        assertNotNull(detailHc)
        assertEquals(1024, detailHc!!.width)
        assertEquals(768, detailHc.height)

        val heroHc = BnjArtworkCache.decodeArtworkBytesForTier(
            jpegBytes,
            ArtworkResolutionTier.HERO,
            highClarity = true
        )
        assertNotNull(heroHc)
        assertEquals(1440, heroHc!!.width)
        assertEquals(1080, heroHc.height)

        // High Clarity OFF: THUMBNAIL = 280px, DETAIL = 720px, HERO = 960px
        val thumbStd = BnjArtworkCache.decodeArtworkBytesForTier(
            jpegBytes,
            ArtworkResolutionTier.THUMBNAIL,
            highClarity = false
        )
        assertNotNull(thumbStd)
        assertEquals(280, thumbStd!!.width)
        assertEquals(210, thumbStd.height)

        val detailStd = BnjArtworkCache.decodeArtworkBytesForTier(
            jpegBytes,
            ArtworkResolutionTier.DETAIL,
            highClarity = false
        )
        assertNotNull(detailStd)
        assertEquals(720, detailStd!!.width)
        assertEquals(540, detailStd.height)

        val heroStd = BnjArtworkCache.decodeArtworkBytesForTier(
            jpegBytes,
            ArtworkResolutionTier.HERO,
            highClarity = false
        )
        assertNotNull(heroStd)
        assertEquals(960, heroStd!!.width)
        assertEquals(720, heroStd.height)

        // 2. Small original artwork (220x180) must NEVER be upscaled in any tier
        val smallSource = Bitmap.createBitmap(220, 180, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(200, 60, 120))
        }
        val smallPngBytes = ByteArrayOutputStream().use { out ->
            smallSource.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        val smallHero = BnjArtworkCache.decodeArtworkBytesForTier(
            smallPngBytes,
            ArtworkResolutionTier.HERO,
            highClarity = true
        )
        assertNotNull(smallHero)
        assertEquals(220, smallHero!!.width)
        assertEquals(180, smallHero.height)

        val smallDetail = BnjArtworkCache.decodeArtworkBytesForTier(
            smallPngBytes,
            ArtworkResolutionTier.DETAIL,
            highClarity = true
        )
        assertNotNull(smallDetail)
        assertEquals(220, smallDetail!!.width)
        assertEquals(180, smallDetail.height)

        // 3. Corrupted or non-image bytes return null safely
        val corruptedBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A)
        assertFalse(BnjArtworkCache.hasValidImageSignature(corruptedBytes))
        assertNull(BnjArtworkCache.decodeByteArrayHighClarity(corruptedBytes, 1440))
    }

    @Test
    fun `p4 sibling folder cover lookup case insensitive matching caching and multi album scope isolation`() {
        runBlocking {
            BnjArtworkCache.clearCache()
            val tempRoot = File(app.cacheDir, "p4_folder_test_${System.currentTimeMillis()}").apply { mkdirs() }
            try {
                // 1. Dedicated album folder with mixed-case "CoVeR.JpG" (1600x1600)
                val dedicatedAlbumDir = File(tempRoot, "Daft_Punk_Discovery").apply { mkdirs() }
                val dummyTrack = File(dedicatedAlbumDir, "01_One_More_Time.flac").apply {
                    writeBytes(ByteArray(64) { 0x11 }) // No valid embedded artwork -> falls back to sibling folder cover
                }
                val coverFile = File(dedicatedAlbumDir, "CoVeR.JpG")
                val coverBitmap = Bitmap.createBitmap(1600, 1600, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(AndroidColor.rgb(30, 215, 96))
                }
                FileOutputStream(coverFile).use { out ->
                    coverBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }

                val discovered = BnjArtworkCache.discoverSiblingFolderCoverFile(dummyTrack.absolutePath, app)
                assertNotNull(discovered)
                assertEquals(coverFile.absolutePath, discovered!!.absolutePath)

                // Decode via full BnjArtworkCache pipeline across THUMBNAIL (360), DETAIL (1024), and HERO (1440)
                val trackUri = Uri.fromFile(dummyTrack).toString()
                val thumbImg = BnjArtworkCache.loadOrExtractArtwork(
                    context = app,
                    uriString = trackUri,
                    isVideo = false,
                    highClarity = true,
                    tier = ArtworkResolutionTier.THUMBNAIL
                )
                assertNotNull(thumbImg)
                assertEquals(360, thumbImg!!.width)
                assertEquals(360, thumbImg.height)

                val detailImg = BnjArtworkCache.loadOrExtractArtwork(
                    context = app,
                    uriString = trackUri,
                    isVideo = false,
                    highClarity = true,
                    tier = ArtworkResolutionTier.DETAIL
                )
                assertNotNull(detailImg)
                assertEquals(1024, detailImg!!.width)
                assertEquals(1024, detailImg.height)

                val heroImg = BnjArtworkCache.loadOrExtractArtwork(
                    context = app,
                    uriString = trackUri,
                    isVideo = false,
                    highClarity = true,
                    tier = ArtworkResolutionTier.HERO
                )
                assertNotNull(heroImg)
                assertEquals(1440, heroImg!!.width)
                assertEquals(1440, heroImg.height)

                // 2. Generic shared folder named "Music" -> must NOT apply loose cover across unrelated tracks
                val genericMusicDir = File(tempRoot, "Music").apply { mkdirs() }
                val looseTrack = File(genericMusicDir, "random_single.mp3").apply {
                    writeBytes(ByteArray(64) { 0x22 })
                }
                val looseCover = File(genericMusicDir, "folder.jpg")
                FileOutputStream(looseCover).use { out ->
                    coverBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                assertNull(BnjArtworkCache.discoverSiblingFolderCoverFile(looseTrack.absolutePath, app))

                // 3. Multi-album parent directory with 2 audio subdirectories -> must NOT apply parent loose cover
                val multiAlbumParent = File(tempRoot, "Artist_Collection").apply { mkdirs() }
                File(multiAlbumParent, "Album1").apply { mkdirs() }.let { sub1 ->
                    File(sub1, "s1.mp3").writeBytes(ByteArray(32))
                }
                File(multiAlbumParent, "Album2").apply { mkdirs() }.let { sub2 ->
                    File(sub2, "s2.mp3").writeBytes(ByteArray(32))
                }
                FileOutputStream(File(multiAlbumParent, "cover.png")).use { out ->
                    coverBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                val parentLooseTrack = File(multiAlbumParent, "intro.mp3").apply {
                    writeBytes(ByteArray(32))
                }
                assertNull(BnjArtworkCache.discoverSiblingFolderCoverFile(parentLooseTrack.absolutePath, app))
            } finally {
                tempRoot.deleteRecursively()
            }
        }
    }

    @Test
    fun `p4 resolution aware cache separation hero reuse and source invalidation`() {
        BnjArtworkCache.clearCache()
        val source2000 = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888).apply {
            eraseColor(AndroidColor.rgb(99, 102, 241))
        }

        // 1. Put 360px THUMBNAIL for track A -> must NOT be reused for 1024px DETAIL or 1440px HERO
        val uriA = "content://media/external/audio/media/9001"
        val thumbA = BnjArtworkCache.putBitmapForTier(
            uri = uriA,
            bitmap = source2000,
            tier = ArtworkResolutionTier.THUMBNAIL,
            highClarity = true
        )
        assertEquals(360, thumbA.width)
        assertNotNull(BnjArtworkCache.getCached(uriA, highClarity = true, tier = ArtworkResolutionTier.THUMBNAIL))
        assertNull(BnjArtworkCache.getCached(uriA, highClarity = true, tier = ArtworkResolutionTier.DETAIL))
        assertNull(BnjArtworkCache.getCached(uriA, highClarity = true, tier = ArtworkResolutionTier.HERO))

        // 2. Put 1440px HERO for track B -> CAN satisfy DETAIL (1024px) and THUMBNAIL (360px) by downscaling
        val uriB = "content://media/external/audio/media/9002"
        val heroB = BnjArtworkCache.putBitmapForTier(
            uri = uriB,
            bitmap = source2000,
            tier = ArtworkResolutionTier.HERO,
            highClarity = true
        )
        assertEquals(1440, heroB.width)

        val derivedDetailB = BnjArtworkCache.getCached(uriB, highClarity = true, tier = ArtworkResolutionTier.DETAIL)
        assertNotNull(derivedDetailB)
        assertEquals(1024, derivedDetailB!!.width)

        val derivedThumbB = BnjArtworkCache.getCached(uriB, highClarity = true, tier = ArtworkResolutionTier.THUMBNAIL)
        assertNotNull(derivedThumbB)
        assertEquals(360, derivedThumbB!!.width)

        // 3. Invalidate track B -> clears all resolution tiers for track B while preserving track A
        BnjArtworkCache.invalidateMedia(uriB)
        assertNull(BnjArtworkCache.getCached(uriB, highClarity = true, tier = ArtworkResolutionTier.HERO))
        assertNull(BnjArtworkCache.getCached(uriB, highClarity = true, tier = ArtworkResolutionTier.DETAIL))
        assertNull(BnjArtworkCache.getCached(uriB, highClarity = true, tier = ArtworkResolutionTier.THUMBNAIL))
        assertNotNull(BnjArtworkCache.getCached(uriA, highClarity = true, tier = ArtworkResolutionTier.THUMBNAIL))
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `p4 four presentation modes crop fill smart fit high clarity toggle and rapid track switching`() {
        val track1 = MediaEntity(
            uri = "content://media/external/audio/media/4001",
            title = "Horizons Overture",
            artist = "BNJ Studio Ensemble",
            album = "Liquid Glass Sessions",
            durationMs = 245_000L,
            sizeBytes = 28_000_000L,
            mimeType = "audio/flac",
            isVideo = false,
            codecName = "FLAC",
            sampleRateHz = 96_000
        )
        val track2 = MediaEntity(
            uri = "content://media/external/audio/media/4002",
            title = "Midnight Prism",
            artist = "Aurora Synth",
            album = "Obsidian Echoes",
            durationMs = 198_000L,
            sizeBytes = 22_000_000L,
            mimeType = "audio/flac",
            isVideo = false,
            codecName = "FLAC",
            sampleRateHz = 48_000
        )

        var activeTrackState by mutableStateOf(track1)
        var isPlayingState by mutableStateOf(true)
        var settingsState by mutableStateOf(
            AppSettingsState(
                artworkDisplayStyle = ArtworkDisplayStyle.ROUNDED_CINEMA.id,
                artworkFitMode = ArtworkFitMode.CROP_FILL.id,
                artworkHighClarityEnabled = true,
                artworkCornerRadiusDp = 22f
            )
        )

        composeRule.setContent {
            MusicPlayerOverlay(
                playbackState = PlaybackUiState(
                    currentMedia = activeTrackState,
                    queue = listOf(track1, track2),
                    currentQueueIndex = if (activeTrackState.uri == track1.uri) 0 else 1,
                    isPlaying = isPlayingState,
                    positionMs = 42_000L,
                    durationMs = activeTrackState.durationMs,
                    isFullscreenPlayerOpen = true
                ),
                eqState = AudioEffectsUiState(),
                customPresets = emptyList(),
                onCloseFullscreen = {},
                onTogglePlayPause = { isPlayingState = !isPlayingState },
                onSeekTo = {},
                onSkipNext = { activeTrackState = track2 },
                onSkipPrevious = { activeTrackState = track1 },
                onToggleShuffle = {},
                onCycleRepeat = {},
                onToggleFavorite = {},
                onSetPlaybackSpeed = {},
                onStartSleepTimer = { _, _ -> },
                onRemoveFromQueue = {},
                onSelectQueueIndex = {},
                onSetEqEnabled = {},
                onSetEqBand = { _, _ -> },
                onSetBassBoost = {},
                onSetVirtualizer = {},
                onApplyPreset = {},
                onSaveCustomPreset = {},
                settings = settingsState
            )
        }

        // Verify initial track 1 in ROUNDED_CINEMA + CROP_FILL + High Clarity ON
        composeRule.onNodeWithTag("fullscreen_music_player").assertIsDisplayed()
        composeRule.onNodeWithTag("music_artwork_gesture_surface").assertIsDisplayed()
        composeRule.onNodeWithText("Horizons Overture").assertIsDisplayed()

        // Switch presentation modes live: VINYL_DISC (playing vs paused), FRAMED_GALLERY, IMMERSIVE_EDGE
        settingsState = settingsState.copy(artworkDisplayStyle = ArtworkDisplayStyle.VINYL_DISC.id)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_artwork_gesture_surface").assertIsDisplayed()

        // Pause playback in VINYL_DISC mode -> stops rotation cleanly
        isPlayingState = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_artwork_gesture_surface").assertIsDisplayed()

        // Switch to FRAMED_GALLERY + SMART_FIT
        settingsState = settingsState.copy(
            artworkDisplayStyle = ArtworkDisplayStyle.FRAMED_GALLERY.id,
            artworkFitMode = ArtworkFitMode.SMART_FIT.id
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_artwork_gesture_surface").assertIsDisplayed()

        // Switch to IMMERSIVE_EDGE + High Clarity OFF
        settingsState = settingsState.copy(
            artworkDisplayStyle = ArtworkDisplayStyle.IMMERSIVE_EDGE.id,
            artworkHighClarityEnabled = false
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("music_artwork_gesture_surface").assertIsDisplayed()

        // Rapid track switching (Track 1 -> Track 2 -> Track 1 -> Track 2) without stale artwork or title
        activeTrackState = track2
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Midnight Prism").assertIsDisplayed()

        activeTrackState = track1
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Horizons Overture").assertIsDisplayed()

        activeTrackState = track2
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Midnight Prism").assertIsDisplayed()
    }

    // ========================================================================
    // P5 TESTS: DYNAMIC ALBUM ARTWORK BASED THEME ENGINE (STEP 13 ITEMS 1–22)
    // ========================================================================

    @Test
    fun `p5 palette extraction outputs primary secondary supporting and background tonal and filters near-black and washed-out pixels`() = runBlocking {
        BnjArtworkPaletteExtractor.clearCache()
        BnjArtworkCache.clearCache()

        // Create a 120x120 bitmap where 70% is near-black (#08080A) and washed-out white (#F9FAFB),
        // 20% is vibrant emerald (#10B981), and 10% is warm coral (#F43F5E)
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        for (y in 0 until 120) {
            for (x in 0 until 120) {
                val color = when {
                    y < 42 -> android.graphics.Color.rgb(8, 8, 10)       // near-black (filtered out of vibrant buckets)
                    y < 84 -> android.graphics.Color.rgb(249, 250, 251)  // washed-out white (filtered out of vibrant buckets)
                    y < 108 -> android.graphics.Color.rgb(16, 185, 129)  // vibrant emerald (primary bucket)
                    else -> android.graphics.Color.rgb(244, 63, 94)      // warm coral (secondary bucket)
                }
                bmp.setPixel(x, y, color)
            }
        }

        val palette = BnjArtworkPaletteExtractor.extractFromBitmapAsync(
            bitmap = bmp,
            cacheKey = "media::test_emerald_coral",
            fallbackSeed = "Emerald Coral Track"
        )

        assertTrue("Palette should mark isFromRealBitmap = true", palette.isFromRealBitmap)
        // Primary vibrant accent must reflect the emerald chroma bucket (green channel dominant), not near-black or washed white
        assertTrue(
            "Primary accent should be green-dominant from emerald region: ${palette.vibrantAccent}",
            palette.vibrantAccent.green > palette.vibrantAccent.red
        )
        // Secondary accent should be distinct from primary (coral red-dominant or complementary)
        assertNotEquals(
            "Secondary accent should differ from primary accent",
            palette.vibrantAccent,
            palette.secondaryGlow
        )
        // Supporting tonal and mode-specific background tonal colors must be populated and valid
        val darkBgTonal = palette.backgroundTonalForMode(isDarkGlass = true)
        val lightBgTonal = palette.backgroundTonalForMode(isDarkGlass = false)
        assertTrue("AMOLED Dark background tonal should have low luminance (< 0.25)", darkBgTonal.luminance() < 0.25f)
        assertTrue("Pearl Light background tonal should have high luminance (> 0.70)", lightBgTonal.luminance() > 0.70f)
        assertTrue("Supporting tonal should have balanced mid luminance", palette.supportingTonal.luminance() in 0.12f..0.85f)

        // Contrast & readability in AMOLED Dark and Frosted Pearl Light
        val darkAdapted = adaptiveGlassAccent(palette.vibrantAccent, isDarkGlass = true)
        val lightAdapted = adaptiveGlassAccent(palette.vibrantAccent, isDarkGlass = false)
        assertTrue("AMOLED Dark adapted accent must be readable (> 0.20 luminance)", darkAdapted.luminance() > 0.20f)
        assertTrue("Pearl Light adapted accent must not be washed out (< 0.80 luminance)", lightAdapted.luminance() < 0.80f)
    }

    @Test
    fun `p5 scope player only vs app wide intensity 25 to 100 blending and priority hierarchy`() {
        val sampleArtworkPalette = ExtractedArtworkPalette(
            dominantColor = Color(0xFF1E293B),
            vibrantAccent = Color(0xFF10B981), // Emerald
            secondaryGlow = Color(0xFF06B6D4),
            surfaceWash = Color(0x3D10B981),
            isDarkArtwork = true,
            isFromRealBitmap = true
        )
        val sampleWallpaperPalette = ExtractedArtworkPalette(
            dominantColor = Color(0xFF31102F),
            vibrantAccent = Color(0xFFF43F5E), // Rose
            secondaryGlow = Color(0xFFF59E0B),
            surfaceWash = Color(0x3DF43F5E),
            isDarkArtwork = true,
            isFromRealBitmap = true
        )

        // 1. PLAYER_ONLY scope: Player uses Artwork Palette, Shell uses Manual Preset (or Wallpaper if active)
        val playerOnlySettings = AppSettingsState(
            themeMode = BnjThemeMode.AMOLED_DARK.id,
            isDarkGlass = true,
            themeAccentPreset = ThemeAccentPreset.ELECTRIC_INDIGO.id,
            dynamicArtworkThemeEnabled = true,
            dynamicArtworkScope = DynamicArtworkScope.PLAYER_ONLY.id,
            dynamicArtworkIntensity = 1.0f,
            customBgEnabled = false
        )
        val resolvedShellPlayerOnly = ThemePriorityResolver.resolveEffectiveTheme(
            settings = playerOnlySettings,
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = null,
            isPlayerContext = false
        )
        val resolvedPlayerPlayerOnly = ThemePriorityResolver.resolveEffectiveTheme(
            settings = playerOnlySettings,
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = null,
            isPlayerContext = true
        )
        assertFalse("In PLAYER_ONLY scope, non-player shell must NOT be driven by artwork", resolvedShellPlayerOnly.isDrivenByArtwork)
        assertEquals("Electric Indigo", resolvedShellPlayerOnly.sourceLabel)
        assertTrue("In PLAYER_ONLY scope, player context MUST be driven by artwork", resolvedPlayerPlayerOnly.isDrivenByArtwork)
        assertEquals("Artwork Palette", resolvedPlayerPlayerOnly.sourceLabel)

        // 2. APP_WIDE scope: Both Shell and Player use Artwork Palette
        val appWideSettings = playerOnlySettings.copy(dynamicArtworkScope = DynamicArtworkScope.APP_WIDE.id)
        val resolvedShellAppWide = ThemePriorityResolver.resolveEffectiveTheme(
            settings = appWideSettings,
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = null,
            isPlayerContext = false
        )
        assertTrue("In APP_WIDE scope, shell MUST be driven by artwork", resolvedShellAppWide.isDrivenByArtwork)
        assertEquals(resolvedPlayerPlayerOnly.primaryAccent, resolvedShellAppWide.primaryAccent)

        // 3. Dynamic Accent Intensity (25%, 50%, 75%, 100%) progressive blending
        val p25 = ThemePriorityResolver.resolveEffectiveTheme(
            settings = appWideSettings.copy(dynamicArtworkIntensity = 0.25f),
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = null,
            isPlayerContext = true
        ).primaryAccent
        val p50 = ThemePriorityResolver.resolveEffectiveTheme(
            settings = appWideSettings.copy(dynamicArtworkIntensity = 0.50f),
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = null,
            isPlayerContext = true
        ).primaryAccent
        val p75 = ThemePriorityResolver.resolveEffectiveTheme(
            settings = appWideSettings.copy(dynamicArtworkIntensity = 0.75f),
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = null,
            isPlayerContext = true
        ).primaryAccent
        val p100 = ThemePriorityResolver.resolveEffectiveTheme(
            settings = appWideSettings.copy(dynamicArtworkIntensity = 1.00f),
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = null,
            isPlayerContext = true
        ).primaryAccent

        assertNotEquals("25% and 50% intensity must produce distinct blended accents", p25, p50)
        assertNotEquals("50% and 75% intensity must produce distinct blended accents", p50, p75)
        assertNotEquals("75% and 100% intensity must produce distinct blended accents", p75, p100)
        assertTrue("Green channel should increase from 25% to 100% toward emerald target", p100.green > p25.green)

        // 4. Priority Compatibility: Artwork -> Custom Wallpaper -> Manual Preset / Custom Studio
        val withWallpaperSettings = playerOnlySettings.copy(
            customBgEnabled = true,
            customBgFilePath = "/data/user/0/com.aistudio.bnjmediapro.vryx/files/wallpapers/bg.jpg",
            customBgMatchAccent = true
        )
        // Shell in PLAYER_ONLY should fall back to Wallpaper (#2 priority) when Artwork is scoped to Player Only
        val shellWithWallpaper = ThemePriorityResolver.resolveEffectiveTheme(
            settings = withWallpaperSettings,
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = sampleWallpaperPalette,
            isPlayerContext = false
        )
        assertTrue("Shell should use Custom Wallpaper when Artwork is PLAYER_ONLY", shellWithWallpaper.isDrivenByBackground)
        assertEquals("Background Photo", shellWithWallpaper.sourceLabel)

        // Player in PLAYER_ONLY should use Artwork (#1 priority) even when Custom Wallpaper is active
        val playerWithWallpaper = ThemePriorityResolver.resolveEffectiveTheme(
            settings = withWallpaperSettings,
            activeArtworkPalette = sampleArtworkPalette,
            customBackgroundPalette = sampleWallpaperPalette,
            isPlayerContext = true
        )
        assertTrue("Player should override Wallpaper with Artwork Palette", playerWithWallpaper.isDrivenByArtwork)

        // When playback stops (activeArtworkPalette == null), Player falls back to Wallpaper (#2) or Custom Studio (#3)
        val stoppedWithWallpaper = ThemePriorityResolver.resolveEffectiveTheme(
            settings = withWallpaperSettings,
            activeArtworkPalette = null,
            customBackgroundPalette = sampleWallpaperPalette,
            isPlayerContext = true
        )
        assertTrue("When playback stops, theme falls back to Custom Wallpaper", stoppedWithWallpaper.isDrivenByBackground)

        val customStudioSettings = withWallpaperSettings.copy(
            customBgEnabled = false,
            themeAccentPreset = ThemeAccentPreset.CUSTOM.id,
            customAccentColorHex = 0xFFE11D48L
        )
        val stoppedWithCustomStudio = ThemePriorityResolver.resolveEffectiveTheme(
            settings = customStudioSettings,
            activeArtworkPalette = null,
            customBackgroundPalette = null,
            isPlayerContext = true
        )
        assertFalse(stoppedWithCustomStudio.isDrivenByArtwork)
        assertFalse(stoppedWithCustomStudio.isDrivenByBackground)
        assertEquals("Custom", stoppedWithCustomStudio.sourceLabel)
    }

    @Test
    fun `p5 palette extraction uses thumbnail tier without forcing 1440px hero decode and handles missing or corrupt artwork`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        BnjArtworkCache.clearCache()
        BnjArtworkPaletteExtractor.clearCache()

        val tempDir = File(context.cacheDir, "p5_palette_tier_test_${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val coverFile = File(tempDir, "cover.jpg")
            val sourceBmp = Bitmap.createBitmap(2000, 2000, Bitmap.Config.ARGB_8888).apply {
                eraseColor(android.graphics.Color.rgb(20, 184, 166)) // Teal
            }
            FileOutputStream(coverFile).use { out ->
                sourceBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            val fakeTrackFile = File(tempDir, "track01.flac").apply { writeBytes(ByteArray(32)) }
            val trackUri = Uri.fromFile(fakeTrackFile).toString()

            // Extract palette using P5 extractPaletteWithoutHeroDecode
            val extracted = BnjArtworkCache.extractPaletteWithoutHeroDecode(
                context = context,
                uriString = trackUri,
                isVideo = false,
                titleSeed = "Teal Horizon",
                highClarity = true
            )
            assertTrue("Should extract real bitmap palette from sibling cover", extracted.isFromRealBitmap)

            // Verify THUMBNAIL tier (360px) was populated, and HERO tier (1440px) was NOT forced!
            val cachedThumb = BnjArtworkCache.getCached(trackUri, highClarity = true, tier = ArtworkResolutionTier.THUMBNAIL)
            val cachedHero = BnjArtworkCache.getCached(trackUri, highClarity = true, tier = ArtworkResolutionTier.HERO)
            assertNotNull("THUMBNAIL tier should be cached after palette extraction", cachedThumb)
            assertEquals(360, cachedThumb!!.width)
            assertNull("HERO 1440px tier must NOT be decoded just for palette extraction", cachedHero)

            // Subsequent call to extractPaletteWithoutHeroDecode hits the palette cache directly
            val cachedPalette = BnjArtworkCache.getCachedPaletteForUri(trackUri)
            assertNotNull("Palette must be cached by URI and source version", cachedPalette)
            assertEquals(extracted.vibrantAccent, cachedPalette!!.vibrantAccent)

            // Missing or corrupt artwork falls back cleanly without crash
            val corruptPalette = BnjArtworkCache.extractPaletteWithoutHeroDecode(
                context = context,
                uriString = "file:///nonexistent_dir_p5/missing_track.mp3",
                isVideo = false,
                titleSeed = "Fallback Track Seed",
                highClarity = true
            )
            assertFalse("Missing file should produce deterministic fallback palette", corruptPalette.isFromRealBitmap)
            assertTrue("Fallback primary accent must be readable", corruptPalette.vibrantAccent.luminance() > 0.15f)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `p5 datastore persistence clamping and backup restore round-trip for dynamic artwork theme`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = SettingsDataStore(context)
        dataStore.clearPreferencesForTesting()

        // Test persistence and clamping (below 0.25 -> 0.25, above 1.0 -> 1.0, GLOBAL_APP -> APP_WIDE)
        dataStore.setDynamicArtworkThemeEnabled(false)
        dataStore.setDynamicArtworkScope("GLOBAL_APP")
        dataStore.setDynamicArtworkIntensity(0.10f)
        var state = dataStore.settingsFlow.first()
        assertFalse(state.dynamicArtworkThemeEnabled)
        assertEquals(DynamicArtworkScope.APP_WIDE.id, state.dynamicArtworkScope)
        assertEquals(0.25f, state.dynamicArtworkIntensity, 0.001f)

        dataStore.setDynamicArtworkThemeEnabled(true)
        dataStore.setDynamicArtworkScope(DynamicArtworkScope.PLAYER_ONLY.id)
        dataStore.setDynamicArtworkIntensity(1.45f)
        state = dataStore.settingsFlow.first()
        assertTrue(state.dynamicArtworkThemeEnabled)
        assertEquals(DynamicArtworkScope.PLAYER_ONLY.id, state.dynamicArtworkScope)
        assertEquals(1.0f, state.dynamicArtworkIntensity, 0.001f)

        // Set 65% intensity + APP_WIDE and verify Backup & Restore round-trip
        dataStore.setDynamicArtworkScope(DynamicArtworkScope.APP_WIDE.id)
        dataStore.setDynamicArtworkIntensity(0.65f)
        val beforeBackup = dataStore.settingsFlow.first()
        val backupJson = SettingsDataStore.serializeBackupJson(beforeBackup, "Nov 14, 2026 18:00")

        // Mutate settings, then restore from backup JSON
        dataStore.setDynamicArtworkThemeEnabled(false)
        dataStore.setDynamicArtworkScope(DynamicArtworkScope.PLAYER_ONLY.id)
        dataStore.setDynamicArtworkIntensity(0.30f)

        val restoreRes = dataStore.applyRestoredSettingsAtomically(backupJson, emptySet())
        assertTrue("Backup restore must succeed", restoreRes.success)
        val afterRestore = dataStore.settingsFlow.first()
        assertTrue(afterRestore.dynamicArtworkThemeEnabled)
        assertEquals(DynamicArtworkScope.APP_WIDE.id, afterRestore.dynamicArtworkScope)
        assertEquals(0.65f, afterRestore.dynamicArtworkIntensity, 0.001f)
    }

    @Test
    @Config(qualifiers = "w412dp-h2400dp")
    fun `p5 appearance panel live dynamic palette preview scope toggle and intensity slider update immediately`() {
        var settingsState by mutableStateOf(
            AppSettingsState(
                dynamicArtworkThemeEnabled = true,
                dynamicArtworkScope = DynamicArtworkScope.PLAYER_ONLY.id,
                dynamicArtworkIntensity = 0.75f
            )
        )
        val testPalette = ExtractedArtworkPalette(
            dominantColor = Color(0xFF0F172A),
            vibrantAccent = Color(0xFF38BDF8),
            secondaryGlow = Color(0xFFA855F7),
            surfaceWash = Color(0x3D38BDF8),
            isDarkArtwork = true,
            isFromRealBitmap = true
        )

        composeRule.setContent {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AppearancePersonalizationPanel(
                    settings = settingsState,
                    isAdvancedMode = true,
                    activeArtworkPalette = testPalette,
                    onToggleDarkGlass = { settingsState = settingsState.copy(isDarkGlass = it) },
                    onChangeGlassOpacity = { settingsState = settingsState.copy(glassOpacity = it) },
                    onChangeBlurIntensity = { settingsState = settingsState.copy(blurIntensity = it) },
                    onChangeGlassBorderIntensity = { settingsState = settingsState.copy(glassBorderIntensity = it) },
                    onResetAppearance = { settingsState = AppSettingsState() },
                    actions = PersonalizationActions(
                        onToggleDynamicArtworkTheme = {
                            settingsState = settingsState.copy(dynamicArtworkThemeEnabled = it)
                        },
                        onSelectDynamicArtworkScope = {
                            settingsState = settingsState.copy(dynamicArtworkScope = it)
                        },
                        onChangeDynamicArtworkIntensity = {
                            settingsState = settingsState.copy(dynamicArtworkIntensity = it)
                        }
                    )
                )
            }
        }

        // Verify Live Dynamic Palette Preview and swatches are displayed
        composeRule.onNodeWithTag("dynamic_artwork_palette_preview").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic_preview_primary_swatch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic_preview_secondary_swatch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic_preview_supporting_swatch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic_preview_glass_player_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("dynamic_artwork_intensity_slider").performScrollTo().assertIsDisplayed()

        // Switch scope from Player Only to App Wide via UI chip
        composeRule.onNodeWithTag("dynamic_scope_APP_WIDE").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(DynamicArtworkScope.APP_WIDE.id, settingsState.dynamicArtworkScope)

        // Switch back to Player Only
        composeRule.onNodeWithTag("dynamic_scope_PLAYER_ONLY").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(DynamicArtworkScope.PLAYER_ONLY.id, settingsState.dynamicArtworkScope)
    }

    @Test
    fun `appearance audit glass surface alpha scales translucently from 10 to 95 percent in both Dark and Pearl Light`() {
        val pearlLowNoBg = computeGlassSurfaceAlpha(
            LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.10f, customBgActive = false, glassStylePreset = GlassStylePreset.FROSTED_SILK),
            elevated = true,
            tier = GlassTier.FROSTED
        )
        val pearlMidNoBg = computeGlassSurfaceAlpha(
            LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.50f, customBgActive = false, glassStylePreset = GlassStylePreset.FROSTED_SILK),
            elevated = true,
            tier = GlassTier.FROSTED
        )
        val pearlHighNoBg = computeGlassSurfaceAlpha(
            LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.95f, customBgActive = false, glassStylePreset = GlassStylePreset.FROSTED_SILK),
            elevated = true,
            tier = GlassTier.FROSTED
        )
        assertTrue("Pearl 10% glass must be genuinely translucent (<= 0.22f), was $pearlLowNoBg", pearlLowNoBg <= 0.22f)
        assertTrue("Pearl 50% glass must sit cleanly between 10% and 95%", pearlMidNoBg in 0.42f..0.64f)
        assertTrue("Pearl 95% glass must approach solid frost (>= 0.88f), was $pearlHighNoBg", pearlHighNoBg >= 0.88f)
        assertTrue(pearlLowNoBg < pearlMidNoBg && pearlMidNoBg < pearlHighNoBg)

        val pearlLowWithBg = computeGlassSurfaceAlpha(
            LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.10f, customBgActive = true, glassStylePreset = GlassStylePreset.FROSTED_SILK),
            elevated = true,
            tier = GlassTier.FROSTED
        )
        assertTrue("Wallpaper active in Pearl Light must stay even more transparent ($pearlLowWithBg < $pearlLowNoBg)", pearlLowWithBg < pearlLowNoBg)

        val darkLowWithBg = computeGlassSurfaceAlpha(
            LiquidGlassStyleConfig(isDarkGlass = true, glassOpacity = 0.10f, customBgActive = true, glassStylePreset = GlassStylePreset.FROSTED_SILK),
            elevated = true,
            tier = GlassTier.FROSTED
        )
        val darkHighWithBg = computeGlassSurfaceAlpha(
            LiquidGlassStyleConfig(isDarkGlass = true, glassOpacity = 0.95f, customBgActive = true, glassStylePreset = GlassStylePreset.FROSTED_SILK),
            elevated = true,
            tier = GlassTier.FROSTED
        )
        assertTrue("AMOLED Dark 10% glass with wallpaper must be clear (<= 0.16f), was $darkLowWithBg", darkLowWithBg <= 0.16f)
        assertTrue("AMOLED Dark 95% glass with wallpaper must be nearly opaque (>= 0.85f), was $darkHighWithBg", darkHighWithBg >= 0.85f)

        // Preset ordering in Pearl Light & Dark
        val clearAlpha = computeGlassSurfaceAlpha(LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.30f, glassStylePreset = GlassStylePreset.CLEAR_CRYSTAL))
        val frostedAlpha = computeGlassSurfaceAlpha(LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.30f, glassStylePreset = GlassStylePreset.FROSTED_SILK))
        val satinAlpha = computeGlassSurfaceAlpha(LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.30f, glassStylePreset = GlassStylePreset.SATIN_MATTE))
        val obsidianAlpha = computeGlassSurfaceAlpha(LiquidGlassStyleConfig(isDarkGlass = false, glassOpacity = 0.30f, glassStylePreset = GlassStylePreset.DEEP_OBSIDIAN))
        assertTrue(clearAlpha < frostedAlpha && frostedAlpha < satinAlpha && satinAlpha < obsidianAlpha)

        // Wallpaper scrim obeys 15%-85% accurately without +0.14f white veil
        val (_, midMin, _) = computeWallpaperScrimAlphas(0.15f)
        val (_, midMax, _) = computeWallpaperScrimAlphas(0.85f)
        assertEquals(0.15f, midMin, 0.01f)
        assertEquals(0.85f, midMax, 0.01f)
    }

    @Test
    @Config(qualifiers = "w412dp-h2400dp")
    fun `appearance audit system default click does not overwrite themeMode in DataStore and artwork corner radius shows in simple mode`() = runBlocking {
        val dataStore = SettingsDataStore(app)
        dataStore.clearPreferencesForTesting()
        dataStore.setDarkGlass(false)

        var currentSettings by mutableStateOf(dataStore.settingsFlow.first())
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)

        composeRule.setContent {
            val flowState by dataStore.settingsFlow.collectAsState(initial = currentSettings)
            currentSettings = flowState
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                AppearancePersonalizationPanel(
                    settings = flowState,
                    isAdvancedMode = false,
                    onToggleDarkGlass = { enabled -> scope.launch { dataStore.setDarkGlass(enabled) } },
                    onChangeGlassOpacity = { op -> scope.launch { dataStore.setGlassOpacity(op) } },
                    onChangeBlurIntensity = { b -> scope.launch { dataStore.setBlurIntensity(b) } },
                    onChangeGlassBorderIntensity = { r -> scope.launch { dataStore.setGlassBorderIntensity(r) } },
                    onResetAppearance = { scope.launch { dataStore.resetAppearanceDefaults() } },
                    actions = PersonalizationActions(
                        onSelectThemeMode = { mode -> scope.launch { dataStore.setThemeMode(mode) } },
                        onSelectArtworkDisplayStyle = { style -> scope.launch { dataStore.setArtworkDisplayStyle(style) } },
                        onChangeArtworkCornerRadiusDp = { dp -> scope.launch { dataStore.setArtworkCornerRadiusDp(dp) } }
                    )
                )
            }
        }

        composeRule.waitForIdle()
        // Artwork Corner Radius slider must be visible in Simple mode for ROUNDED_CINEMA
        composeRule.onNodeWithTag("artwork_corner_radius_slider").performScrollTo().assertIsDisplayed()

        // Click System Default card in UI and verify DataStore persists SYSTEM_DEFAULT cleanly
        composeRule.onNodeWithTag("settings_system_glass_switch").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            currentSettings.themeMode == BnjThemeMode.SYSTEM_DEFAULT.id
        }
        // Re-read from a fresh SettingsDataStore instance simulating app restart
        val freshRead = SettingsDataStore(app).settingsFlow.first()
        assertEquals(BnjThemeMode.SYSTEM_DEFAULT.id, freshRead.themeMode)

        // Switching to VINYL_DISC hides Artwork Corner Radius slider
        composeRule.onNodeWithTag("artwork_style_VINYL_DISC").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 3_000L) {
            currentSettings.artworkDisplayStyle == ArtworkDisplayStyle.VINYL_DISC.id
        }
        composeRule.onNodeWithTag("artwork_corner_radius_slider").assertDoesNotExist()
    }

    @Test
    fun `appearance audit all Sections A through E settings persist across DataStore restart and Backup Restore round trip`() = runBlocking {
        val dataStore = SettingsDataStore(app)
        dataStore.clearPreferencesForTesting()

        // Mutate every single Section A-E setting
        dataStore.setThemeMode(BnjThemeMode.SYSTEM_DEFAULT.id)
        dataStore.setCustomAccentHsv(hue = 285f, saturation = 0.0f, activateCustom = true)
        dataStore.setCustomAccentHsv(hue = 285f, saturation = 0.72f, activateCustom = true)
        dataStore.setAccentSaturationBoost(1.22f)
        dataStore.setCustomBgBlurIntensity(0.58f)
        dataStore.setCustomBgDimAmount(0.28f)
        dataStore.setCustomBgMatchAccent(false)
        dataStore.applyGlassStylePreset(
            presetId = GlassStylePreset.CLEAR_CRYSTAL.id,
            opacity = 0.10f,
            blur = 0.30f,
            border = 0.0f,
            tintStrength = 0.45f,
            glow = 0.0f,
            tintEnabled = false,
            cornerRadiusDp = 28f
        )
        dataStore.setArtworkDisplayStyle(ArtworkDisplayStyle.FRAMED_GALLERY.id)
        dataStore.setArtworkFitMode(ArtworkFitMode.SMART_FIT.id)
        dataStore.setArtworkHighClarityEnabled(false)
        dataStore.setArtworkCornerRadiusDp(14f)
        dataStore.setDynamicArtworkThemeEnabled(false)
        dataStore.setDynamicArtworkScope(DynamicArtworkScope.APP_WIDE.id)
        dataStore.setDynamicArtworkIntensity(0.45f)

        val mutated = SettingsDataStore(app).settingsFlow.first()
        assertEquals(BnjThemeMode.SYSTEM_DEFAULT.id, mutated.themeMode)
        assertEquals(ThemeAccentPreset.CUSTOM.id, mutated.themeAccentPreset)
        assertEquals(285f, mutated.customAccentHue, 1.5f)
        assertEquals(0.72f, mutated.customAccentSaturation, 0.02f)
        assertEquals(1.22f, mutated.accentSaturationBoost, 0.01f)
        assertEquals(0.58f, mutated.customBgBlurIntensity, 0.01f)
        assertEquals(0.28f, mutated.customBgDimAmount, 0.01f)
        assertFalse(mutated.customBgMatchAccent)
        assertEquals(GlassStylePreset.CLEAR_CRYSTAL.id, mutated.glassStylePreset)
        assertEquals(0.10f, mutated.glassOpacity, 0.005f)
        assertEquals(0.30f, mutated.blurIntensity, 0.005f)
        assertEquals(0.0f, mutated.glassBorderIntensity, 0.005f)
        assertEquals(0.0f, mutated.glassGlowStrength, 0.005f)
        assertFalse(mutated.glassTintEnabled)
        assertEquals(0.45f, mutated.glassTintStrength, 0.005f)
        assertEquals(28f, mutated.glassCornerRadiusDp, 0.1f)
        assertEquals(ArtworkDisplayStyle.FRAMED_GALLERY.id, mutated.artworkDisplayStyle)
        assertEquals(ArtworkFitMode.SMART_FIT.id, mutated.artworkFitMode)
        assertFalse(mutated.artworkHighClarityEnabled)
        assertEquals(14f, mutated.artworkCornerRadiusDp, 0.1f)
        assertFalse(mutated.dynamicArtworkThemeEnabled)
        assertEquals(DynamicArtworkScope.APP_WIDE.id, mutated.dynamicArtworkScope)
        assertEquals(0.45f, mutated.dynamicArtworkIntensity, 0.005f)

        // Serialize backup JSON, reset defaults, then restore and verify exact round-trip
        val json = SettingsDataStore.serializeBackupJson(mutated, "Nov 14, 2026 20:00")
        dataStore.resetAppearanceDefaults()
        val res = dataStore.applyRestoredSettingsAtomically(json, emptySet())
        assertTrue(res.success)

        val restored = SettingsDataStore(app).settingsFlow.first()
        assertEquals(mutated.themeMode, restored.themeMode)
        assertEquals(mutated.themeAccentPreset, restored.themeAccentPreset)
        assertEquals(mutated.customAccentColorHex, restored.customAccentColorHex)
        assertEquals(mutated.customAccentHue, restored.customAccentHue, 1.5f)
        assertEquals(mutated.customAccentSaturation, restored.customAccentSaturation, 0.02f)
        assertEquals(mutated.accentSaturationBoost, restored.accentSaturationBoost, 0.01f)
        assertEquals(mutated.customBgBlurIntensity, restored.customBgBlurIntensity, 0.01f)
        assertEquals(mutated.customBgDimAmount, restored.customBgDimAmount, 0.01f)
        assertEquals(mutated.customBgMatchAccent, restored.customBgMatchAccent)
        assertEquals(mutated.glassStylePreset, restored.glassStylePreset)
        assertEquals(mutated.glassOpacity, restored.glassOpacity, 0.005f)
        assertEquals(mutated.blurIntensity, restored.blurIntensity, 0.005f)
        assertEquals(mutated.glassBorderIntensity, restored.glassBorderIntensity, 0.005f)
        assertEquals(mutated.glassGlowStrength, restored.glassGlowStrength, 0.005f)
        assertEquals(mutated.glassTintEnabled, restored.glassTintEnabled)
        assertEquals(mutated.glassTintStrength, restored.glassTintStrength, 0.005f)
        assertEquals(mutated.glassCornerRadiusDp, restored.glassCornerRadiusDp, 0.1f)
        assertEquals(mutated.artworkDisplayStyle, restored.artworkDisplayStyle)
        assertEquals(mutated.artworkFitMode, restored.artworkFitMode)
        assertEquals(mutated.artworkHighClarityEnabled, restored.artworkHighClarityEnabled)
        assertEquals(mutated.artworkCornerRadiusDp, restored.artworkCornerRadiusDp, 0.1f)
        assertEquals(mutated.dynamicArtworkThemeEnabled, restored.dynamicArtworkThemeEnabled)
        assertEquals(mutated.dynamicArtworkScope, restored.dynamicArtworkScope)
        assertEquals(mutated.dynamicArtworkIntensity, restored.dynamicArtworkIntensity, 0.005f)

        // Verify ThemePriorityResolver switches backgroundTonal cleanly when dynamicArtworkThemeEnabled is toggled OFF
        val resolvedOff = ThemePriorityResolver.resolveEffectiveTheme(
            settings = restored,
            activeArtworkPalette = BnjArtworkPaletteExtractor.fallbackPalette("Test Track"),
            customBackgroundPalette = null,
            isPlayerContext = true,
            effectiveDarkGlass = false
        )
        assertFalse(resolvedOff.isDrivenByArtwork)
        val resolvedOn = ThemePriorityResolver.resolveEffectiveTheme(
            settings = restored.copy(dynamicArtworkThemeEnabled = true),
            activeArtworkPalette = BnjArtworkPaletteExtractor.fallbackPalette("Test Track"),
            customBackgroundPalette = null,
            isPlayerContext = true,
            effectiveDarkGlass = false
        )
        assertTrue(resolvedOn.isDrivenByArtwork)
    }
}



