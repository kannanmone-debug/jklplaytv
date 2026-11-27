package com.example

import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidGlassStyleConfig
import com.example.ui.theme.resolveDynamicGlassPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class GlobalDynamicGlassShadeTest {

    @Test
    fun testResolveDynamicGlassPaletteAmoledDark() {
        val config = LiquidGlassStyleConfig(
            isDarkGlass = true,
            glassOpacity = 0.35f,
            glassTintEnabled = true,
            glassTintStrength = 0.5f,
            dynamicAccent = Color(0xFF00E5FF)
        )
        val palette = resolveDynamicGlassPalette(config, GlassTier.FROSTED, Color(0xFF00E5FF), false)
        assertNotNull(palette)
        assertNotNull(palette.flatSurfaceColor)
        assertNotNull(palette.dialogContainerColor)
        assertNotNull(palette.chipUnselectedColor)
        assertNotNull(palette.selectedSurfaceColor)
        assertNotNull(palette.selectedBorderColor)
        assertEquals(3, palette.borderColors.size)
    }

    @Test
    fun testResolveDynamicGlassPalettePearlLightNonMilky() {
        val config = LiquidGlassStyleConfig(
            isDarkGlass = false,
            glassOpacity = 0.40f,
            glassTintEnabled = true,
            glassTintStrength = 0.6f,
            dynamicAccent = Color(0xFF10B981) // Emerald accent
        )
        val palette = resolveDynamicGlassPalette(config, GlassTier.FROSTED, Color(0xFF10B981), false)
        assertNotNull(palette)
        // Ensure Frosted Pearl Light mode produces genuine tinted non-milky glass (distinct from pure #FFFFFF)
        assertNotEquals(Color.White, palette.flatSurfaceColor)
        assertNotEquals(Color.White, palette.topColor)
    }

    @Test
    fun testGlassTintDisabledNeutralizesChromaticTint() {
        val configTinted = LiquidGlassStyleConfig(
            isDarkGlass = true,
            glassOpacity = 0.3f,
            glassTintEnabled = true,
            glassTintStrength = 1.0f,
            dynamicAccent = Color(0xFFFF0000)
        )
        val configNeutral = configTinted.copy(glassTintEnabled = false)

        val paletteTinted = resolveDynamicGlassPalette(configTinted, GlassTier.FROSTED, Color(0xFFFF0000), false)
        val paletteNeutral = resolveDynamicGlassPalette(configNeutral, GlassTier.FROSTED, Color(0xFFFF0000), false)

        // When tint is disabled, palette colors must not match the heavily tinted variant
        assertNotEquals(paletteTinted.flatSurfaceColor, paletteNeutral.flatSurfaceColor)
    }

    @Test
    fun testTiersAndElevatedSurfaces() {
        val config = LiquidGlassStyleConfig(isDarkGlass = true, glassOpacity = 0.25f)
        val frosted = resolveDynamicGlassPalette(config, GlassTier.FROSTED, elevated = false)
        val elevatedFrosted = resolveDynamicGlassPalette(config, GlassTier.FROSTED, elevated = true)
        val floating = resolveDynamicGlassPalette(config, GlassTier.FLOATING, elevated = true)

        assertNotNull(frosted)
        assertNotNull(elevatedFrosted)
        assertNotNull(floating)
        assertNotEquals(frosted.surfaceAlpha, floating.surfaceAlpha)
    }
}
