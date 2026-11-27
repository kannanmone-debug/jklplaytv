package com.example.tv.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.codec.DeviceMediaCapabilities
import com.example.data.local.AppSettingsState
import com.example.data.local.EqualizerPresetEntity
import com.example.equalizer.AudioEffectsUiState
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Category 5: Audio & Equalizer — Integrates existing AudioEffectsController & BnjAudioDspProcessor
 * with honest hardware capability reporting, built-in presets, custom Room presets, 5-band EQ,
 * Bass Boost, Virtualizer, Loudness Enhancer, Reverb, Stereo Balance, and Audio Delay.
 */
@OptIn(ExperimentalLayoutApi::class)
internal fun LazyListScope.tvAudioEqualizerSettingsSection(
    settings: AppSettingsState,
    audioEffectsState: AudioEffectsUiState,
    customEqPresets: List<EqualizerPresetEntity>,
    actions: TvSettingsActions
) {
    item(key = "audio_hw_status_card") {
        val hwSupported = audioEffectsState.isHardwareSupported
        val statusTitle = if (hwSupported) {
            "Hardware Audio DSP Active"
        } else {
            "Hardware Audio DSP Unavailable or Standby"
        }
        val statusBadge = if (hwSupported) "HW ATTACHED" else "STANDBY / UNSUPPORTED"
        val supportedModules = buildList {
            if (hwSupported) add("5-Band EQ")
            if (audioEffectsState.bassBoostSupported) add("Bass Boost")
            if (audioEffectsState.virtualizerSupported) add("Virtualizer")
            if (audioEffectsState.loudnessSupported) add("Loudness")
            if (audioEffectsState.reverbSupported) add("Reverb")
        }
        val statusBody = if (hwSupported) {
            "Session #${audioEffectsState.audioSessionId} attached to Media3 ExoPlayer. " +
                "Active modules: ${if (supportedModules.isEmpty()) "Equalizer" else supportedModules.joinToString(" • ")}. " +
                "Stereo Balance and Audio Delay are handled by BnjAudioDspProcessor."
        } else {
            "Android hardware audio effects have not attached to an active playback session or are unsupported on this TV audio HAL. " +
                "Start playback to probe hardware EQ; Stereo Balance, Audio Sync Delay, and Fade-In remain available."
        }
        TvSettingInfoCard(
            title = statusTitle,
            body = statusBody,
            badge = statusBadge,
            badgeColorOverride = if (hwSupported) GlassEmeraldTint else null,
            icon = Icons.Rounded.GraphicEq,
            modifier = Modifier.testTag("tv_setting_audio_hw_status")
        )
    }

    item(key = "audio_eq_enable") {
        TvSettingToggleRow(
            title = "Audio Equalizer & Effects",
            subtitle = if (audioEffectsState.isHardwareSupported) {
                "Enable 5-band hardware EQ, bass boost, loudness, and reverb"
            } else {
                "Preference saved; hardware EQ activates when supported by the TV audio session"
            },
            checked = audioEffectsState.isEnabled,
            onCheckedChange = actions.onToggleEqEnabled,
            icon = Icons.Rounded.Equalizer,
            modifier = Modifier.testTag("tv_setting_eq_enabled")
        )
    }

    item(key = "audio_preset_cycle") {
        TvSettingChoiceRow(
            title = "Active Sound Profile",
            subtitle = "Cycle through JAAS PLAY TV built-in studio equalizer curves",
            valueLabel = audioEffectsState.activePresetName,
            onClick = actions.onCycleEqPreset,
            icon = Icons.Rounded.Tune,
            modifier = Modifier.testTag("tv_setting_eq_preset")
        )
    }

    item(key = "audio_custom_presets") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TvSettingsSectionHeader(
                title = "Custom Equalizer Presets",
                subtitle = if (customEqPresets.isEmpty()) {
                    "No custom EQ presets saved yet. Saved Room presets appear here automatically."
                } else {
                    "Apply any of your ${customEqPresets.size} saved custom EQ curves"
                }
            )
            if (customEqPresets.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    customEqPresets.forEach { preset ->
                        val isSelected = audioEffectsState.activePresetName.equals(preset.name, ignoreCase = true)
                        TvActionButton(
                            text = preset.name,
                            icon = Icons.Rounded.Equalizer,
                            primary = isSelected,
                            onClick = { actions.onSelectCustomEqPreset(preset) },
                            modifier = Modifier.testTag("tv_setting_custom_eq_${preset.id}")
                        )
                    }
                }
            }
        }
    }

    item(key = "audio_bands_header") {
        TvSettingsSectionHeader(
            title = "5-Band Graphic Equalizer",
            subtitle = if (audioEffectsState.isHardwareSupported) {
                "Adjust each frequency band in 1.5 dB steps with the TV remote"
            } else {
                "Band levels display persisted curve; hardware EQ required for live band processing"
            }
        )
    }

    items(audioEffectsState.bands, key = { "eq_band_${it.index}" }) { band ->
        val dbValue = band.currentLevelMb / 100f
        val formattedDb = String.format(Locale.US, "%+.1f dB", dbValue)
        TvSettingStepperRow(
            title = "${band.label} Band",
            subtitle = "Range ${band.minLevelMb / 100} dB to +${band.maxLevelMb / 100} dB",
            valueLabel = formattedDb,
            decrementLabel = "-1.5",
            incrementLabel = "+1.5",
            onDecrement = {
                val next = (band.currentLevelMb - 150).coerceIn(band.minLevelMb, band.maxLevelMb)
                actions.onAdjustEqBand(band.index, next)
            },
            onIncrement = {
                val next = (band.currentLevelMb + 150).coerceIn(band.minLevelMb, band.maxLevelMb)
                actions.onAdjustEqBand(band.index, next)
            },
            decrementTestTag = "tv_setting_eq_band_${band.index}_minus",
            incrementTestTag = "tv_setting_eq_band_${band.index}_plus",
            enabled = true
        )
    }

    item(key = "audio_dsp_enhancements_header") {
        TvSettingsSectionHeader(
            title = "Spatial & Dynamics Processing",
            subtitle = "Hardware-backed bass, virtualizer, loudness, reverb, and DSP balance"
        )
    }

    item(key = "audio_bass_boost") {
        val bassPct = (audioEffectsState.bassBoostStrength / 10).coerceIn(0, 100)
        TvSettingChoiceRow(
            title = "Bass Boost",
            subtitle = if (audioEffectsState.bassBoostSupported) {
                "Low-frequency sub-bass enhancement for TV speakers & soundbars"
            } else {
                "Preference saved (hardware BassBoost not reported on current audio HAL)"
            },
            valueLabel = if (bassPct == 0) "Off" else "$bassPct%",
            onClick = actions.onCycleAudioBassBoost,
            icon = Icons.Rounded.SpeakerGroup,
            modifier = Modifier.testTag("tv_setting_audio_bass")
        )
    }

    item(key = "audio_virtualizer") {
        val virtPct = (audioEffectsState.virtualizerStrength / 10).coerceIn(0, 100)
        TvSettingChoiceRow(
            title = "Surround Virtualizer",
            subtitle = if (audioEffectsState.virtualizerSupported) {
                "Widens stereo soundstage for living-room cinema playback"
            } else {
                "Preference saved (hardware Virtualizer not reported on current audio HAL)"
            },
            valueLabel = if (virtPct == 0) "Off" else "$virtPct%",
            onClick = actions.onCycleAudioVirtualizer,
            icon = Icons.Rounded.SurroundSound,
            modifier = Modifier.testTag("tv_setting_audio_virtualizer")
        )
    }

    item(key = "audio_loudness") {
        val gainDb = (audioEffectsState.loudnessGainMb / 100f).roundToInt()
        TvSettingChoiceRow(
            title = "Loudness Enhancer",
            subtitle = if (audioEffectsState.loudnessSupported) {
                "Boost dialogue clarity on quiet movies and late-night listening"
            } else {
                "Preference saved (hardware LoudnessEnhancer not reported on current HAL)"
            },
            valueLabel = if (gainDb <= 0) "Off" else "+${gainDb} dB",
            onClick = actions.onCycleAudioLoudness,
            icon = Icons.Rounded.VolumeUp,
            modifier = Modifier.testTag("tv_setting_audio_loudness")
        )
    }

    item(key = "audio_reverb") {
        val reverbName = AudioEffectsUiState.reverbPresetLabels.getOrElse(audioEffectsState.reverbPreset) { "None" }
        TvSettingChoiceRow(
            title = "Room Reverb",
            subtitle = if (audioEffectsState.reverbSupported) {
                "Acoustic hall and room reverberation environment"
            } else {
                "Preference saved (hardware PresetReverb not reported on current HAL)"
            },
            valueLabel = reverbName,
            onClick = actions.onCycleAudioReverb,
            icon = Icons.Rounded.Hearing,
            modifier = Modifier.testTag("tv_setting_audio_reverb")
        )
    }

    item(key = "audio_balance") {
        val balanceText = when {
            audioEffectsState.balance < -0.05f -> "L ${(audioEffectsState.balance * -100).roundToInt()}%"
            audioEffectsState.balance > 0.05f -> "R ${(audioEffectsState.balance * 100).roundToInt()}%"
            else -> "Center"
        }
        TvSettingChoiceRow(
            title = "Stereo Balance (DSP)",
            subtitle = "Fine-tune Left / Right channel output via BnjAudioDspProcessor",
            valueLabel = balanceText,
            onClick = actions.onCycleAudioBalance,
            icon = Icons.Rounded.Tune,
            modifier = Modifier.testTag("tv_setting_audio_balance")
        )
    }

    item(key = "audio_delay_stepper") {
        TvSettingStepperRow(
            title = "Audio Sync Delay",
            subtitle = "Compensate Bluetooth soundbar or AV receiver lip-sync latency",
            valueLabel = "${settings.audioDelayMs} ms",
            decrementLabel = "-50ms",
            incrementLabel = "+50ms",
            onDecrement = { actions.onAdjustAudioDelay(-50L) },
            onIncrement = { actions.onAdjustAudioDelay(50L) },
            decrementTestTag = "tv_setting_audio_delay_minus",
            incrementTestTag = "tv_setting_audio_delay_plus"
        )
    }

    item(key = "audio_fade_in") {
        TvSettingToggleRow(
            title = "Smooth Audio Fade-In",
            subtitle = "Gently ramp audio volume when starting or resuming media",
            checked = settings.fadeInAtStart,
            onCheckedChange = actions.onToggleFadeIn,
            modifier = Modifier.testTag("tv_setting_fade_in")
        )
    }

    item(key = "audio_reset") {
        TvActionButton(
            text = "Reset Equalizer & DSP Effects",
            icon = Icons.Rounded.RestartAlt,
            onClick = actions.onResetEqualizer,
            modifier = Modifier.testTag("tv_setting_reset_eq")
        )
    }
}

/**
 * Category 9: Codec & Device Information — Real device specs, display tier, HDR flags,
 * and hardware/software video & audio decoders powered directly by CodecCapabilityInspector.
 */
internal fun LazyListScope.tvCodecDeviceInfoSection(
    settings: AppSettingsState,
    capabilities: DeviceMediaCapabilities,
    isReprobingCodecs: Boolean,
    actions: TvSettingsActions
) {
    item(key = "codec_header_actions") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TvActionButton(
                text = if (isReprobingCodecs) "Probing Hardware..." else "Re-Probe Decoders",
                icon = Icons.Rounded.Refresh,
                primary = true,
                onClick = actions.onRefreshCodecs,
                modifier = Modifier.testTag("tv_setting_reprobe_codecs")
            )
            TvActionButton(
                text = "Full Codec Inspector",
                icon = Icons.Rounded.Memory,
                onClick = actions.onOpenCodecInspector,
                modifier = Modifier.testTag("tv_setting_open_codec_inspector")
            )
        }
    }

    item(key = "codec_device_specs") {
        val metrics = LocalTvLayoutMetrics.current
        val config = LocalConfiguration.current
        val deviceName = capabilities.deviceModel.ifBlank { "${Build.MANUFACTURER} ${Build.MODEL}".trim() }
        val hwVideoCount = capabilities.videoCodecs.count { it.hasHardwareDecoder }
        val supportedAudioCount = capabilities.audioCodecs.count { it.isSupportedOnDevice }
        TvSettingInfoCard(
            title = deviceName,
            body = "Android ${Build.VERSION.RELEASE} (API ${capabilities.androidSdk}) • " +
                "Display Tier: ${metrics.tierLabel} (${config.screenWidthDp}×${config.screenHeightDp} dp)\n" +
                "Decoder Engine: ${formatDecoderMode(settings.decoderMode)} • " +
                "HW Video Decoders: $hwVideoCount/${capabilities.videoCodecs.size} • " +
                "Supported Audio Codecs: $supportedAudioCount/${capabilities.audioCodecs.size}",
            badge = "API ${capabilities.androidSdk}",
            icon = Icons.Rounded.Memory,
            modifier = Modifier.testTag("tv_setting_device_specs_card")
        )
    }

    item(key = "codec_hdr_summary") {
        val has4kHw = hasReal4kHardwareDecoder(capabilities)
        val hasDolbyAudio = hasRealDolbyAudioSupport(capabilities)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tv_codec_summary_grid"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvSettingInfoCard(
                    title = "4K UHD HW Decode",
                    body = if (has4kHw) "Verified 3840×2160+ hardware video decoder" else "No 4K hardware decoder reported",
                    badge = if (has4kHw) "HW 4K" else "NO HW 4K",
                    badgeColorOverride = if (has4kHw) GlassEmeraldTint else null,
                    modifier = Modifier.weight(1f)
                )
                TvSettingInfoCard(
                    title = "HDR10 / HLG / HDR10+",
                    body = buildString {
                        append("HDR10: ${if (capabilities.displaySupportsHdr10) "Yes" else "No"}")
                        append(" • HLG: ${if (capabilities.displaySupportsHlg) "Yes" else "No"}")
                        append(" • HDR10+: ${if (capabilities.displaySupportsHdr10Plus) "Yes" else "No"}")
                    },
                    badge = if (capabilities.displaySupportsHdr10 || capabilities.displaySupportsHlg) "HDR" else "SDR",
                    badgeColorOverride = if (capabilities.displaySupportsHdr10) GlassEmeraldTint else null,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TvSettingInfoCard(
                    title = "Dolby Vision",
                    body = if (capabilities.displaySupportsDolbyVision) {
                        "Display reports Dolby Vision HDR support"
                    } else {
                        "Not reported by active Android display"
                    },
                    badge = if (capabilities.displaySupportsDolbyVision) "SUPPORTED" else "NOT REPORTED",
                    badgeColorOverride = if (capabilities.displaySupportsDolbyVision) GlassEmeraldTint else null,
                    modifier = Modifier.weight(1f)
                )
                TvSettingInfoCard(
                    title = "Dolby Audio (AC-3 / E-AC-3)",
                    body = if (hasDolbyAudio) {
                        "Device reports AC-3 / E-AC-3 decoder support"
                    } else {
                        "No AC-3 / E-AC-3 hardware or software decoder reported"
                    },
                    badge = if (hasDolbyAudio) "SUPPORTED" else "PCM / STEREO",
                    badgeColorOverride = if (hasDolbyAudio) GlassEmeraldTint else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    item(key = "codec_video_header") {
        TvSettingsSectionHeader(
            title = "Video Decoders (${capabilities.videoCodecs.size})",
            subtitle = "Real MediaCodecList hardware and software decoders probed on this device"
        )
    }

    if (capabilities.videoCodecs.isEmpty()) {
        item(key = "codec_video_empty") {
            TvSettingInfoCard(
                title = "No Video Decoders Reported Yet",
                body = "Press Re-Probe Decoders to inspect MediaCodecList hardware video codecs."
            )
        }
    } else {
        items(
            items = capabilities.videoCodecs,
            key = { "inline_vcodec_${it.displayName}_${it.mimeTypes.joinToString(",")}" }
        ) { codec ->
            val badge = formatCodecSupportBadge(codec)
            val badgeColor: Color? = when {
                codec.hasHardwareDecoder -> GlassEmeraldTint
                !codec.isSupportedOnDevice -> GlassRoseTint
                else -> null
            }
            val hdrNote = if (codec.hdrProfilesSupported.isNotEmpty()) {
                " • ${codec.hdrProfilesSupported.joinToString(", ")}"
            } else ""
            TvSettingInfoCard(
                title = codec.displayName,
                body = "${codec.mimeTypes.firstOrNull() ?: "video/*"} • Max ${codec.maxResolution}$hdrNote",
                badge = badge,
                badgeColorOverride = badgeColor
            )
        }
    }

    item(key = "codec_audio_header") {
        TvSettingsSectionHeader(
            title = "Audio Decoders (${capabilities.audioCodecs.size})",
            subtitle = "Honest on-device audio codec availability"
        )
    }

    if (capabilities.audioCodecs.isEmpty()) {
        item(key = "codec_audio_empty") {
            TvSettingInfoCard(
                title = "No Audio Decoders Reported Yet",
                body = "Press Re-Probe Decoders to inspect MediaCodecList audio codecs."
            )
        }
    } else {
        items(
            items = capabilities.audioCodecs,
            key = { "inline_acodec_${it.displayName}_${it.mimeTypes.joinToString(",")}" }
        ) { codec ->
            val badge = formatCodecSupportBadge(codec)
            val badgeColor: Color? = when {
                codec.hasHardwareDecoder -> GlassEmeraldTint
                !codec.isSupportedOnDevice -> GlassRoseTint
                else -> null
            }
            TvSettingInfoCard(
                title = codec.displayName,
                body = "${codec.mimeTypes.firstOrNull() ?: "audio/*"} • ${codec.maxResolution}",
                badge = badge,
                badgeColorOverride = badgeColor
            )
        }
    }
}
