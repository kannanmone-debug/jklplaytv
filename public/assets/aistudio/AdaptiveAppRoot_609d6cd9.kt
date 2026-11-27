package com.example.adaptive

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppSettingsState
import com.example.navigation.BnjDestination
import com.example.navigation.BnjNavigationShell
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.BnjLiquidGlassTheme
import com.example.ui.theme.BnjThemeMode
import com.example.ui.theme.GlassComponentTarget
import com.example.ui.theme.GlassStylePreset
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LocalBnjTextColors
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ThemePriorityResolver
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.liquidGlassSurface
import com.example.ui.theme.resolveEffectiveDarkGlass
import com.example.viewmodel.BnjMainViewModel

/**
 * Universal adaptive application root for BNJ MEDIA PRO.
 *
 * Routes automatically to:
 * - [TvAppRoot] when running on Android TV / Google TV (`DeviceFormFactor.TV`).
 * - [TabletAppRoot] when running on an Android Tablet (`DeviceFormFactor.TABLET`), including
 *   when the tablet is in a compact split-screen or resizable multi-window.
 * - [MobileAppRoot] when running on an Android Mobile handset (`DeviceFormFactor.MOBILE`).
 */
@Composable
fun AdaptiveAppRoot(
    viewModel: BnjMainViewModel,
    activity: FragmentActivity,
    onEnterPipMode: () -> Unit,
    overrideWindowInfo: AdaptiveWindowInfo? = null,
    overrideWidthDp: Dp = Dp.Unspecified,
    overrideHeightDp: Dp = Dp.Unspecified
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .testTag("bnj_adaptive_app_root")
    ) {
        val effectiveWidthDp = if (overrideWidthDp != Dp.Unspecified) {
            overrideWidthDp
        } else if (maxWidth != Dp.Unspecified && maxWidth.value.isFinite() && maxWidth.value > 0f) {
            maxWidth
        } else {
            Dp.Unspecified
        }

        val effectiveHeightDp = if (overrideHeightDp != Dp.Unspecified) {
            overrideHeightDp
        } else if (maxHeight != Dp.Unspecified && maxHeight.value.isFinite() && maxHeight.value > 0f) {
            maxHeight
        } else {
            Dp.Unspecified
        }

        val detectedWindowInfo = rememberAdaptiveWindowInfo(
            overrideWidthDp = effectiveWidthDp,
            overrideHeightDp = effectiveHeightDp
        )
        val activeWindowInfo = overrideWindowInfo ?: detectedWindowInfo

        CompositionLocalProvider(LocalAdaptiveWindowInfo provides activeWindowInfo) {
            when (activeWindowInfo.deviceFormFactor) {
                DeviceFormFactor.TV -> {
                    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
                    val playbackState by viewModel.playbackController.shellState.collectAsStateWithLifecycle()
                    TvAppRoot(
                        windowInfo = activeWindowInfo,
                        settings = settings,
                        activeMediaTitle = playbackState.currentMedia?.title
                    )
                }

                DeviceFormFactor.TABLET -> {
                    BnjNavigationShell(
                        viewModel = viewModel,
                        activity = activity,
                        onEnterPipMode = onEnterPipMode
                    )
                }

                DeviceFormFactor.MOBILE -> {
                    MobileAppRoot(
                        viewModel = viewModel,
                        activity = activity,
                        onEnterPipMode = onEnterPipMode
                    )
                }
            }
        }
    }
}

/**
 * Stateless adaptive root router for deterministic testing and previewing of form-factor routing
 * without requiring a full ViewModel/Activity graph.
 */
@Composable
fun AdaptiveAppRootRouter(
    windowInfo: AdaptiveWindowInfo,
    settings: AppSettingsState = AppSettingsState(),
    activeMediaTitle: String? = null,
    songCount: Int = 0,
    videoCount: Int = 0,
    mobileContent: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAdaptiveWindowInfo provides windowInfo) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("bnj_adaptive_app_root")
        ) {
            when (windowInfo.deviceFormFactor) {
                DeviceFormFactor.TV -> TvAppRoot(
                    windowInfo = windowInfo,
                    settings = settings,
                    activeMediaTitle = activeMediaTitle
                )

                DeviceFormFactor.TABLET -> TabletAppRoot(
                    windowInfo = windowInfo,
                    settings = settings,
                    activeMediaTitle = activeMediaTitle,
                    songCount = songCount,
                    videoCount = videoCount
                )

                DeviceFormFactor.MOBILE -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("adaptive_root_mobile")
                ) {
                    mobileContent()
                }
            }
        }
    }
}

/**
 * Mobile application root: strictly delegates to the existing [BnjNavigationShell]
 * without any redesign, layout mutation, or behavioral change.
 */
@Composable
fun MobileAppRoot(
    viewModel: BnjMainViewModel,
    activity: FragmentActivity,
    onEnterPipMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("adaptive_root_mobile")
    ) {
        BnjNavigationShell(
            viewModel = viewModel,
            activity = activity,
            onEnterPipMode = onEnterPipMode
        )
    }
}

/**
 * Phase 2 Tablet application root scaffold.
 *
 * Uses shared application state, reads [AdaptiveWindowInfo], supports portrait, landscape,
 * medium/expanded two-pane foundation, and compact split-screen presentation while retaining
 * the Tablet root identity and active navigation state across configuration/window changes.
 */
@Composable
fun TabletAppRoot(
    windowInfo: AdaptiveWindowInfo = LocalAdaptiveWindowInfo.current,
    settings: AppSettingsState = AppSettingsState(),
    activeMediaTitle: String? = null,
    songCount: Int = 0,
    videoCount: Int = 0
) {
    val effectiveDarkGlass = resolveEffectiveDarkGlass(settings)
    val activeThemeMode = BnjThemeMode.resolveActiveMode(settings.themeMode, settings.isDarkGlass)
    val resolvedTheme = remember(settings, effectiveDarkGlass) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = null,
            customBackgroundPalette = null,
            isPlayerContext = false,
            effectiveDarkGlass = effectiveDarkGlass
        )
    }
    val enabledDestinations = remember(
        settings.navHomeEnabled,
        settings.navMusicEnabled,
        settings.navVideoEnabled,
        settings.navFilesEnabled,
        settings.navVaultEnabled
    ) {
        BnjDestination.enabledBottomDestinations(settings)
    }
    val fallbackDestination = enabledDestinations.firstOrNull() ?: BnjDestination.HOME

    var selectedRoute by rememberSaveable { mutableStateOf(fallbackDestination.route) }
    val currentDestination = remember(selectedRoute, fallbackDestination) {
        BnjDestination.fromRoute(selectedRoute) ?: fallbackDestination
    }

    if (currentDestination != fallbackDestination) {
        BackHandler {
            selectedRoute = fallbackDestination.route
        }
    }

    BnjLiquidGlassTheme(
        isDarkGlass = effectiveDarkGlass,
        themeMode = activeThemeMode,
        glassOpacity = settings.glassOpacity,
        blurEnabled = settings.glassBlurEnabled,
        blurIntensity = settings.blurIntensity,
        glassGlowEnabled = settings.glassGlowEnabled,
        glassGlowStrength = settings.glassGlowStrength,
        glassGlowRadiusDp = settings.glassGlowRadiusDp,
        glassBorderEnabled = settings.glassBorderEnabled,
        glassBorderIntensity = settings.glassBorderIntensity,
        dynamicAccent = resolvedTheme.primaryAccent,
        secondaryAccent = resolvedTheme.secondaryGlow,
        supportingTonal = resolvedTheme.supportingTonal,
        backgroundTonal = resolvedTheme.backgroundTonal,
        surfaceWash = resolvedTheme.surfaceWash,
        glassStylePreset = GlassStylePreset.fromId(settings.glassStylePreset)
    ) {
        val config = LocalLiquidGlassConfig.current
        val textColors = LocalBnjTextColors.current
        val accent = adaptiveGlassAccent(config.dynamicAccent, config.isDarkGlass)

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("adaptive_root_tablet"),
            color = if (config.isDarkGlass) AmoledBlack else MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tablet_root_width_class_${windowInfo.widthClass.name.lowercase()}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("tablet_root_orientation_${windowInfo.orientation.name.lowercase()}")
                ) {
                    if (windowInfo.isCompactWidth) {
                // Tablet in compact split-screen / narrow multi-window:
                // Retains TabletAppRoot while adapting to a compact single-column layout.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(12.dp)
                        .testTag("tablet_root_compact_split_layout"),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tablet_main_content_placeholder"),
                        tier = GlassTier.FROSTED
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "JKL PLAY — Tablet Split-Screen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textColors.heading
                            )
                            Text(
                                text = "Active Section: ${currentDestination.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColors.primary,
                                modifier = Modifier.testTag("tablet_active_destination_label")
                            )
                            Text(
                                text = "Window: ${windowInfo.windowWidthDp.toInt()}x${windowInfo.windowHeightDp.toInt()}dp (${windowInfo.widthClass.name})",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColors.secondary
                            )
                            if (!activeMediaTitle.isNullOrBlank()) {
                                Text(
                                    text = "Now Playing: $activeMediaTitle",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassSurface(
                                shape = RoundedCornerShape(18.dp),
                                elevated = true,
                                tier = GlassTier.FLOATING,
                                componentTarget = GlassComponentTarget.NAVIGATION
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("tablet_compact_nav_bar"),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        enabledDestinations.forEach { dest ->
                            val isSelected = currentDestination == dest
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) accent.copy(alpha = 0.2f) else Color.Transparent)
                                    .semantics { selected = isSelected }
                                    .clickable { selectedRoute = dest.route }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("tablet_nav_item_${dest.route}")
                            ) {
                                Icon(
                                    imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label,
                                    tint = if (isSelected) textColors.selected else textColors.navigationLabel,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = dest.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) textColors.selected else textColors.navigationLabel
                                )
                            }
                        }
                    }
                }
            } else {
                // Tablet in Medium or Expanded window (Portrait or Landscape):
                // Persistent left navigation rail + expansive main content scaffold.
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(12.dp)
                        .testTag("tablet_root_large_screen_layout"),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(if (windowInfo.isExpandedWidth) 96.dp else 80.dp)
                            .liquidGlassSurface(
                                shape = RoundedCornerShape(22.dp),
                                elevated = true,
                                tier = GlassTier.FLOATING,
                                componentTarget = GlassComponentTarget.NAVIGATION
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                            .testTag("tablet_nav_rail_placeholder"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        enabledDestinations.forEach { dest ->
                            val isSelected = currentDestination == dest
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) accent.copy(alpha = 0.2f) else Color.Transparent)
                                    .semantics { selected = isSelected }
                                    .clickable { selectedRoute = dest.route }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .testTag("tablet_nav_item_${dest.route}")
                            ) {
                                Icon(
                                    imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                                    contentDescription = dest.label,
                                    tint = if (isSelected) textColors.selected else textColors.navigationLabel,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = dest.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) textColors.selected else textColors.navigationLabel,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    LiquidGlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("tablet_main_content_placeholder"),
                        tier = GlassTier.FROSTED
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "JKL PLAY — Tablet Adaptive Workspace",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColors.heading
                            )
                            Text(
                                text = "Active Section: ${currentDestination.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = textColors.primary,
                                modifier = Modifier.testTag("tablet_active_destination_label")
                            )
                            Text(
                                text = "Window: ${windowInfo.windowWidthDp.toInt()}x${windowInfo.windowHeightDp.toInt()}dp • ${windowInfo.widthClass.name} • ${windowInfo.orientation.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = textColors.secondary
                            )
                            Text(
                                text = "Library Indexed: $songCount Audio • $videoCount Video",
                                style = MaterialTheme.typography.labelMedium,
                                color = textColors.secondary
                            )
                            if (!activeMediaTitle.isNullOrBlank()) {
                                Text(
                                    text = "Now Playing: $activeMediaTitle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = accent
                                )
                            }
                        }
                    }
                }
            }
                }
            }
        }
    }
}

/**
 * Phase 2 Android TV application root scaffold.
 *
 * Provides a landscape-friendly 10-foot layout with D-pad focusable navigation items,
 * high-visibility focus rings, remote key handling, basic Back navigation, and zero touch dependency.
 */
@Composable
fun TvAppRoot(
    windowInfo: AdaptiveWindowInfo = LocalAdaptiveWindowInfo.current,
    settings: AppSettingsState = AppSettingsState(),
    activeMediaTitle: String? = null
) {
    val tvDestinations = remember {
        listOf(
            BnjDestination.HOME,
            BnjDestination.MUSIC,
            BnjDestination.VIDEO,
            BnjDestination.FILES,
            BnjDestination.SETTINGS
        )
    }

    var selectedDestination by rememberSaveable { mutableStateOf(BnjDestination.HOME.route) }
    var focusedItemTag by remember { mutableStateOf("tv_focusable_nav_home") }
    val homeFocusRequester = remember { FocusRequester() }

    val currentDest = remember(selectedDestination) {
        BnjDestination.fromRoute(selectedDestination) ?: BnjDestination.HOME
    }

    if (currentDest != BnjDestination.HOME) {
        BackHandler {
            selectedDestination = BnjDestination.HOME.route
        }
    }

    LaunchedEffect(Unit) {
        try {
            homeFocusRequester.requestFocus()
        } catch (_: Throwable) {
        }
    }

    val tvResolvedTheme = remember(settings) {
        ThemePriorityResolver.resolveEffectiveTheme(
            settings = settings,
            activeArtworkPalette = null,
            customBackgroundPalette = null,
            isPlayerContext = false,
            effectiveDarkGlass = true
        )
    }

    BnjLiquidGlassTheme(
        isDarkGlass = true,
        themeMode = BnjThemeMode.AMOLED_DARK,
        glassOpacity = settings.glassOpacity,
        blurEnabled = settings.glassBlurEnabled,
        blurIntensity = settings.blurIntensity,
        dynamicAccent = tvResolvedTheme.primaryAccent,
        secondaryAccent = tvResolvedTheme.secondaryGlow,
        supportingTonal = tvResolvedTheme.supportingTonal,
        backgroundTonal = tvResolvedTheme.backgroundTonal,
        surfaceWash = tvResolvedTheme.surfaceWash,
        glassStylePreset = GlassStylePreset.fromId(settings.glassStylePreset)
    ) {
        val tvConfig = LocalLiquidGlassConfig.current
        val tvPrimary = adaptiveGlassAccent(tvConfig.dynamicAccent, true)
        val tvSecondary = adaptiveGlassAccent(tvConfig.secondaryAccent, true)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(AmoledBlack, ObsidianSurface)
                    )
                )
                .padding(horizontal = 32.dp, vertical = 24.dp)
                .testTag("adaptive_root_tv")
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // D-pad Focusable TV Navigation Sidebar
                Column(
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp))
                        .padding(16.dp)
                        .testTag("tv_nav_sidebar"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "JKL PLAY TV",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    tvDestinations.forEach { dest ->
                        val itemTag = "tv_focusable_nav_${dest.route}"
                        val isSelected = currentDest == dest
                        var isFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (isFocused) 1.04f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "tvNavFocusScale"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = when {
                                isFocused -> tvPrimary
                                isSelected -> tvSecondary.copy(alpha = 0.7f)
                                else -> Color.Transparent
                            },
                            label = "tvNavBorderColor"
                        )
                        val bgColor by animateColorAsState(
                            targetValue = when {
                                isFocused -> tvPrimary.copy(alpha = 0.24f)
                                isSelected -> tvSecondary.copy(alpha = 0.18f)
                                else -> Color.Transparent
                            },
                            label = "tvNavBgColor"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 52.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(14.dp))
                                .background(bgColor)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag(itemTag)
                                .semantics { selected = isSelected }
                                .then(
                                    if (dest == BnjDestination.HOME) {
                                        Modifier.focusRequester(homeFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .onFocusChanged { state ->
                                    isFocused = state.isFocused
                                    if (state.isFocused || state.hasFocus) {
                                        focusedItemTag = itemTag
                                    }
                                }
                                .onKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown &&
                                        (event.key == Key.DirectionCenter || event.key == Key.Enter)
                                    ) {
                                        selectedDestination = dest.route
                                        true
                                    } else if (event.type == KeyEventType.KeyDown &&
                                        event.key == Key.Back &&
                                        currentDest != BnjDestination.HOME
                                    ) {
                                        selectedDestination = BnjDestination.HOME.route
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .focusable(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected || isFocused) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label,
                                tint = if (isFocused) tvPrimary else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = dest.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
                                color = if (isFocused) tvPrimary else Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            if (isFocused) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(tvPrimary)
                                )
                            }
                        }
                    }
                }

                // TV Main Cinematic Stage Placeholder
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                        .padding(28.dp)
                        .testTag("tv_main_stage_placeholder"),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "JKL PLAY — Android TV Edition",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Active Channel: ${currentDest.label}",
                            style = MaterialTheme.typography.titleLarge,
                            color = tvPrimary,
                            modifier = Modifier.testTag("tv_active_destination_label")
                        )
                        Text(
                            text = "Focused Element: $focusedItemTag",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.testTag("tv_focus_indicator")
                        )
                        if (!activeMediaTitle.isNullOrBlank()) {
                            Text(
                                text = "Now Playing: $activeMediaTitle",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }

                    var isPrimaryActionFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 200.dp, minHeight = 54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isPrimaryActionFocused) tvPrimary.copy(alpha = 0.28f)
                                else tvSecondary.copy(alpha = 0.20f)
                            )
                            .border(
                                width = if (isPrimaryActionFocused) 2.dp else 1.dp,
                                color = if (isPrimaryActionFocused) tvPrimary else Color.White.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .testTag("tv_focusable_primary_action")
                            .onFocusChanged { state ->
                                isPrimaryActionFocused = state.isFocused
                                if (state.isFocused || state.hasFocus) {
                                    focusedItemTag = "tv_focusable_primary_action"
                                }
                            }
                            .focusable(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "D-Pad Ready • ${windowInfo.windowWidthDp.toInt()}x${windowInfo.windowHeightDp.toInt()}dp",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
