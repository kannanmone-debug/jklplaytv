package com.example.equalizer

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.example.player.service.BnjMediaSessionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class EqBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val label: String,
    val currentLevelMb: Int,
    val minLevelMb: Int,
    val maxLevelMb: Int
)

data class AudioEffectsUiState(
    val isHardwareSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val audioSessionId: Int = 0,
    val bands: List<EqBandInfo> = defaultBands(),
    val bassBoostSupported: Boolean = false,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerSupported: Boolean = false,
    val virtualizerStrength: Int = 0, // 0..1000
    val loudnessSupported: Boolean = false,
    val loudnessGainMb: Int = 0, // 0..1500 mB
    val reverbSupported: Boolean = false,
    val reverbPreset: Int = 0, // 0..6 (PresetReverb constants)
    val balance: Float = 0f, // -1.0f Left .. +1.0f Right
    val activePresetName: String = "Flat Studio",
    val builtInPresets: List<String> = listOf(
        "Flat Studio",
        "Liquid Bass",
        "Audiophile Vocal",
        "Cinema Surround",
        "Electronic Glow",
        "Acoustic Warmth"
    )
) {
    companion object {
        val reverbPresetLabels: List<String> = listOf(
            "None",
            "Small Room",
            "Medium Room",
            "Large Room",
            "Medium Hall",
            "Large Hall",
            "Plate"
        )

        fun defaultBands(): List<EqBandInfo> = listOf(
            EqBandInfo(0, 60, "60Hz", 0, -1500, 1500),
            EqBandInfo(1, 230, "230Hz", 0, -1500, 1500),
            EqBandInfo(2, 910, "910Hz", 0, -1500, 1500),
            EqBandInfo(3, 3600, "3.6kHz", 0, -1500, 1500),
            EqBandInfo(4, 14000, "14kHz", 0, -1500, 1500)
        )
    }
}

class AudioEffectsController(
    private val onPersistEffects: ((AudioEffectsUiState) -> Unit)? = null
) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var presetReverb: PresetReverb? = null

    private val _state = MutableStateFlow(AudioEffectsUiState())
    val state: StateFlow<AudioEffectsUiState> = _state.asStateFlow()

    @OptIn(UnstableApi::class)
    fun attachToAudioSession(sessionId: Int) {
        if (sessionId <= 0 || sessionId == _state.value.audioSessionId) return
        releaseEffects()

        val cur = _state.value
        try {
            val availableTypes = try {
                android.media.audiofx.AudioEffect.queryEffects().map { it.type }.toSet()
            } catch (_: Throwable) {
                emptySet()
            }

            val eq = if (availableTypes.isEmpty() || availableTypes.contains(android.media.audiofx.AudioEffect.EFFECT_TYPE_EQUALIZER)) {
                try { Equalizer(0, sessionId) } catch (_: Throwable) { null }
            } else null

            val bandList = if (eq != null) {
                eq.enabled = cur.isEnabled
                equalizer = eq

                val range = eq.bandLevelRange
                val rawMin = range?.getOrNull(0)?.toInt() ?: -1500
                val rawMax = range?.getOrNull(1)?.toInt() ?: 1500
                val minMb = if (rawMin < rawMax) rawMin else -1500
                val maxMb = if (rawMin < rawMax) rawMax else 1500
                val numBands = eq.numberOfBands.toInt()
                val defaultBands = AudioEffectsUiState.defaultBands()

                (0 until numBands).map { idx ->
                    val bandShort = idx.toShort()
                    val freqMilliHz = eq.getCenterFreq(bandShort)
                    val fallbackBand = cur.bands.getOrNull(idx) ?: defaultBands.getOrNull(idx)
                    val freqHz = if (freqMilliHz > 0) {
                        (freqMilliHz / 1000).coerceAtLeast(1)
                    } else {
                        fallbackBand?.centerFreqHz ?: 1000
                    }
                    val label = if (freqHz >= 1000) "${String.format(Locale.US, "%.1f", freqHz / 1000f)}kHz" else "${freqHz}Hz"
                    val restoredLevel = cur.bands.getOrNull(idx)?.currentLevelMb?.coerceIn(minMb, maxMb) ?: 0
                    try {
                        eq.setBandLevel(bandShort, restoredLevel.toShort())
                    } catch (_: Throwable) {}
                    EqBandInfo(
                        index = idx,
                        centerFreqHz = freqHz,
                        label = label,
                        currentLevelMb = restoredLevel,
                        minLevelMb = minMb,
                        maxLevelMb = maxMb
                    )
                }
            } else {
                cur.bands.ifEmpty { AudioEffectsUiState.defaultBands() }
            }

            val bb = if (availableTypes.isNotEmpty() && availableTypes.contains(android.media.audiofx.AudioEffect.EFFECT_TYPE_BASS_BOOST)) {
                try {
                    BassBoost(0, sessionId).apply {
                        enabled = cur.isEnabled
                        if (strengthSupported) setStrength(cur.bassBoostStrength.toShort())
                    }
                } catch (_: Throwable) {
                    null
                }
            } else null
            bassBoost = bb

            val virt = if (availableTypes.isNotEmpty() && availableTypes.contains(android.media.audiofx.AudioEffect.EFFECT_TYPE_VIRTUALIZER)) {
                try {
                    Virtualizer(0, sessionId).apply {
                        enabled = cur.isEnabled
                        if (strengthSupported) setStrength(cur.virtualizerStrength.toShort())
                    }
                } catch (_: Throwable) {
                    null
                }
            } else null
            virtualizer = virt

            val loud = if (availableTypes.isNotEmpty() && availableTypes.contains(android.media.audiofx.AudioEffect.EFFECT_TYPE_LOUDNESS_ENHANCER)) {
                try {
                    LoudnessEnhancer(sessionId).apply {
                        setTargetGain(cur.loudnessGainMb.coerceIn(0, 1500))
                        enabled = cur.isEnabled && cur.loudnessGainMb > 0
                    }
                } catch (_: Throwable) {
                    null
                }
            } else null
            loudnessEnhancer = loud

            val reverb = if (availableTypes.isNotEmpty() && availableTypes.contains(android.media.audiofx.AudioEffect.EFFECT_TYPE_PRESET_REVERB)) {
                try {
                    PresetReverb(0, sessionId).apply {
                        preset = cur.reverbPreset.coerceIn(0, 6).toShort()
                        enabled = cur.isEnabled && cur.reverbPreset > 0
                    }
                } catch (_: Throwable) {
                    null
                }
            } else null
            presetReverb = reverb

            BnjMediaSessionService.sharedDspProcessor.setStereoBalance(cur.balance)

            _state.value = cur.copy(
                isHardwareSupported = eq != null,
                audioSessionId = sessionId,
                bands = bandList,
                bassBoostSupported = bb?.strengthSupported == true,
                virtualizerSupported = virt?.strengthSupported == true,
                loudnessSupported = loud != null,
                reverbSupported = reverb != null
            )
        } catch (_: Throwable) {
            _state.value = cur.copy(
                isHardwareSupported = false,
                audioSessionId = sessionId
            )
        }
    }

    @OptIn(UnstableApi::class)
    fun restorePersistedEffects(
        enabled: Boolean,
        activePreset: String,
        bandLevelsCsv: String,
        bassStrength: Int,
        virtualizerStrength: Int,
        loudnessGainMb: Int,
        reverbPreset: Int,
        balance: Float
    ) {
        val parsedLevels = bandLevelsCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val clamped = parsedLevels.getOrElse(idx) { band.currentLevelMb }
                .coerceIn(band.minLevelMb, band.maxLevelMb)
            try {
                equalizer?.setBandLevel(band.index.toShort(), clamped.toShort())
            } catch (_: Throwable) {}
            band.copy(currentLevelMb = clamped)
        }

        val clampedBass = bassStrength.coerceIn(0, 1000)
        val clampedVirt = virtualizerStrength.coerceIn(0, 1000)
        val clampedLoud = loudnessGainMb.coerceIn(0, 1500)
        val clampedReverb = reverbPreset.coerceIn(0, 6)
        val clampedBalance = balance.coerceIn(-1f, 1f)

        try {
            equalizer?.enabled = enabled
            bassBoost?.let {
                if (it.strengthSupported) it.setStrength(clampedBass.toShort())
                it.enabled = enabled
            }
            virtualizer?.let {
                if (it.strengthSupported) it.setStrength(clampedVirt.toShort())
                it.enabled = enabled
            }
            loudnessEnhancer?.let {
                it.setTargetGain(clampedLoud)
                it.enabled = enabled && clampedLoud > 0
            }
            presetReverb?.let {
                it.preset = clampedReverb.toShort()
                it.enabled = enabled && clampedReverb > 0
            }
        } catch (_: Throwable) {}

        BnjMediaSessionService.sharedDspProcessor.setStereoBalance(clampedBalance)

        _state.value = _state.value.copy(
            isEnabled = enabled,
            activePresetName = activePreset.ifBlank { "Flat Studio" },
            bands = updatedBands,
            bassBoostStrength = clampedBass,
            virtualizerStrength = clampedVirt,
            loudnessGainMb = clampedLoud,
            reverbPreset = clampedReverb,
            balance = clampedBalance
        )
    }

    fun setEffectsEnabled(enabled: Boolean) {
        val cur = _state.value
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            loudnessEnhancer?.enabled = enabled && cur.loudnessGainMb > 0
            presetReverb?.enabled = enabled && cur.reverbPreset > 0
        } catch (_: Throwable) {}
        _state.value = cur.copy(isEnabled = enabled).also { onPersistEffects?.invoke(it) }
    }

    fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val currentBands = _state.value.bands.toMutableList()
        val targetIdx = currentBands.indexOfFirst { it.index == bandIndex }
        if (targetIdx == -1) return

        val bandInfo = currentBands[targetIdx]
        val clamped = levelMb.coerceIn(bandInfo.minLevelMb, bandInfo.maxLevelMb)
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), clamped.toShort())
        } catch (_: Throwable) {}

        currentBands[targetIdx] = bandInfo.copy(currentLevelMb = clamped)
        _state.value = _state.value.copy(
            bands = currentBands,
            activePresetName = "Custom"
        ).also { onPersistEffects?.invoke(it) }
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (_: Throwable) {}
        _state.value = _state.value.copy(bassBoostStrength = clamped).also { onPersistEffects?.invoke(it) }
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (_: Throwable) {}
        _state.value = _state.value.copy(virtualizerStrength = clamped).also { onPersistEffects?.invoke(it) }
    }

    fun setLoudnessGain(gainMb: Int) {
        val clamped = gainMb.coerceIn(0, 1500)
        val enabled = _state.value.isEnabled
        try {
            loudnessEnhancer?.setTargetGain(clamped)
            loudnessEnhancer?.enabled = enabled && clamped > 0
        } catch (_: Throwable) {}
        _state.value = _state.value.copy(loudnessGainMb = clamped).also { onPersistEffects?.invoke(it) }
    }

    fun setReverbPreset(preset: Int) {
        val clamped = preset.coerceIn(0, 6)
        val enabled = _state.value.isEnabled
        try {
            presetReverb?.preset = clamped.toShort()
            presetReverb?.enabled = enabled && clamped > 0
        } catch (_: Throwable) {}
        _state.value = _state.value.copy(reverbPreset = clamped).also { onPersistEffects?.invoke(it) }
    }

    @OptIn(UnstableApi::class)
    fun setStereoBalance(balance: Float) {
        val clamped = balance.coerceIn(-1f, 1f)
        BnjMediaSessionService.sharedDspProcessor.setStereoBalance(clamped)
        _state.value = _state.value.copy(balance = clamped).also { onPersistEffects?.invoke(it) }
    }

    fun applyPreset(presetName: String) {
        val curve = when (presetName) {
            "Liquid Bass" -> listOf(900, 650, 100, 200, 400)
            "Audiophile Vocal" -> listOf(-200, 250, 750, 600, 200)
            "Cinema Surround" -> listOf(750, 400, -100, 450, 850)
            "Electronic Glow" -> listOf(850, 500, -200, 500, 900)
            "Acoustic Warmth" -> listOf(450, 350, 200, 400, 550)
            else -> listOf(0, 0, 0, 0, 0)
        }
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val targetMb = curve.getOrElse(idx) { 0 }.coerceIn(band.minLevelMb, band.maxLevelMb)
            try {
                equalizer?.setBandLevel(band.index.toShort(), targetMb.toShort())
            } catch (_: Throwable) {}
            band.copy(currentLevelMb = targetMb)
        }
        val newBass = if (presetName == "Liquid Bass") 750 else if (presetName == "Flat Studio") 0 else _state.value.bassBoostStrength
        val newVirt = if (presetName == "Cinema Surround") 800 else if (presetName == "Flat Studio") 0 else _state.value.virtualizerStrength
        try {
            if (bassBoost?.strengthSupported == true) bassBoost?.setStrength(newBass.toShort())
            if (virtualizer?.strengthSupported == true) virtualizer?.setStrength(newVirt.toShort())
        } catch (_: Throwable) {}

        _state.value = _state.value.copy(
            isEnabled = true,
            bands = updatedBands,
            bassBoostStrength = newBass,
            virtualizerStrength = newVirt,
            activePresetName = presetName
        )
        setEffectsEnabled(true)
    }

    fun applyCustomPresetCsv(name: String, csv: String, bass: Int, virt: Int) {
        val levels = csv.split(",").mapNotNull { it.trim().toIntOrNull() }
        val updatedBands = _state.value.bands.mapIndexed { idx, band ->
            val targetMb = levels.getOrElse(idx) { 0 }.coerceIn(band.minLevelMb, band.maxLevelMb)
            try {
                equalizer?.setBandLevel(band.index.toShort(), targetMb.toShort())
            } catch (_: Throwable) {}
            band.copy(currentLevelMb = targetMb)
        }
        val clampedBass = bass.coerceIn(0, 1000)
        val clampedVirt = virt.coerceIn(0, 1000)
        try {
            if (bassBoost?.strengthSupported == true) bassBoost?.setStrength(clampedBass.toShort())
            if (virtualizer?.strengthSupported == true) virtualizer?.setStrength(clampedVirt.toShort())
        } catch (_: Throwable) {}
        _state.value = _state.value.copy(
            isEnabled = true,
            bands = updatedBands,
            bassBoostStrength = clampedBass,
            virtualizerStrength = clampedVirt,
            activePresetName = name
        )
        setEffectsEnabled(true)
    }

    @OptIn(UnstableApi::class)
    fun resetEqualizer() {
        val updatedBands = _state.value.bands.map { band ->
            try {
                equalizer?.setBandLevel(band.index.toShort(), 0)
            } catch (_: Throwable) {}
            band.copy(currentLevelMb = 0)
        }
        try {
            if (bassBoost?.strengthSupported == true) bassBoost?.setStrength(0)
            if (virtualizer?.strengthSupported == true) virtualizer?.setStrength(0)
            loudnessEnhancer?.setTargetGain(0)
            loudnessEnhancer?.enabled = false
            presetReverb?.preset = 0
            presetReverb?.enabled = false
        } catch (_: Throwable) {}
        BnjMediaSessionService.sharedDspProcessor.setStereoBalance(0f)

        _state.value = _state.value.copy(
            bands = updatedBands,
            bassBoostStrength = 0,
            virtualizerStrength = 0,
            loudnessGainMb = 0,
            reverbPreset = 0,
            balance = 0f,
            activePresetName = "Flat Studio"
        ).also { onPersistEffects?.invoke(it) }
    }

    fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()
            presetReverb?.release()
        } catch (_: Throwable) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudnessEnhancer = null
        presetReverb = null
    }
}
