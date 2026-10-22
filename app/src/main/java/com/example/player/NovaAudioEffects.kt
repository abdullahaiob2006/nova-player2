package com.example.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer

/**
 * فئة التحكم بمعالج الصوت والمعادل (Equalizer & Bass Boost) المرتبط بـ ExoPlayer
 */
class NovaAudioEffects(private val player: ExoPlayer) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    var isInitialized by mutableStateOf(false)
        private set

    var isEnabled by mutableStateOf(false)
        private set

    var bassBoostStrength by mutableFloatStateOf(0f)
        private set

    val bandFrequencies = mutableStateListOf<String>()
    val bandLevels = mutableStateListOf<Float>()
    val presets = mutableStateListOf<String>()
    var currentPresetIndex by mutableIntStateOf(-1)
        private set

    var minBandLevel = -1500f
    var maxBandLevel = 1500f

    /**
     * تهيئة المعادل الصوتي وربطه بـ AudioSessionId الخاص بالمشغل
     */
    fun attach() {
        val sessionId = player.audioSessionId
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || isInitialized) return

        try {
            equalizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, sessionId).apply {
                enabled = true
            }

            val eq = equalizer!!
            isEnabled = eq.enabled

            val range = eq.bandLevelRange
            if (range.size >= 2) {
                minBandLevel = range[0].toFloat()
                maxBandLevel = range[1].toFloat()
            }

            val bandsCount = eq.numberOfBands.toInt()
            bandFrequencies.clear()
            bandLevels.clear()

            for (i in 0 until bandsCount) {
                val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000
                val freqLabel = if (centerFreqHz >= 1000) "${centerFreqHz / 1000}k" else "${centerFreqHz}Hz"
                bandFrequencies.add(freqLabel)
                bandLevels.add(eq.getBandLevel(i.toShort()).toFloat())
            }

            presets.clear()
            val presetsCount = eq.numberOfPresets.toInt()
            for (p in 0 until presetsCount) {
                presets.add(eq.getPresetName(p.toShort()))
            }
            currentPresetIndex = eq.currentPreset.toInt()

            bassBoostStrength = (bassBoost?.roundedStrength?.toFloat() ?: 0f) / 10f

            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandLevel(bandIndex: Int, level: Float) {
        if (bandIndex in bandLevels.indices) {
            bandLevels[bandIndex] = level
            try {
                equalizer?.setBandLevel(bandIndex.toShort(), level.toInt().toShort())
                currentPresetIndex = -1 // مخصص
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setBassBoost(percent: Float) {
        bassBoostStrength = percent.coerceIn(0f, 100f)
        try {
            val strengthShort = (bassBoostStrength * 10f).toInt().coerceIn(0, 1000).toShort()
            bassBoost?.setStrength(strengthShort)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyPreset(index: Int) {
        if (index in presets.indices) {
            currentPresetIndex = index
            try {
                equalizer?.usePreset(index.toShort())
                val bandsCount = equalizer?.numberOfBands?.toInt() ?: 0
                for (i in 0 until bandsCount) {
                    if (i in bandLevels.indices) {
                        bandLevels[i] = equalizer?.getBandLevel(i.toShort())?.toFloat() ?: 0f
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reset() {
        val bandsCount = bandLevels.size
        for (i in 0 until bandsCount) {
            setBandLevel(i, 0f)
        }
        setBassBoost(0f)
        currentPresetIndex = -1
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        equalizer = null
        bassBoost = null
        isInitialized = false
    }
}

/**
 * واجهة المعادل الصوتي والمؤثرات (Equalizer BottomSheet)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerBottomSheet(
    effects: NovaAudioEffects,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    DisposableEffect(effects) {
        effects.attach()
        onDispose { }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("equalizer_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // شريط العنوان وزر التفعيل وإعادة الضبط
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "المعادل الصوتي ومضخم الصوت",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (effects.isEnabled) "مُفعل" else "مُعطل",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (effects.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { effects.reset() },
                        enabled = effects.isEnabled,
                        modifier = Modifier.testTag("eq_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "إعادة ضبط",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = effects.isEnabled,
                        onCheckedChange = { effects.toggleEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("eq_switch_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. مضخم الباس (Bass Boost)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speaker,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تضخيم الباس (Bass Boost)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "${effects.bassBoostStrength.toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Slider(
                        value = effects.bassBoostStrength,
                        onValueChange = { effects.setBassBoost(it) },
                        valueRange = 0f..100f,
                        enabled = effects.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. أوضاع التردد الجاهزة (Presets Carousel)
            if (effects.presets.isNotEmpty()) {
                Text(
                    text = "الأوضاع المسبقة (Presets)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(effects.presets) { index, presetName ->
                        val isSelected = effects.currentPresetIndex == index
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = effects.isEnabled) { effects.applyPreset(index) }
                        ) {
                            Text(
                                text = presetName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. أشرطة ترددات المعادل (Equalizer Bands)
            Text(
                text = "نطاقات التردد (Frequency Bands)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                effects.bandFrequencies.forEachIndexed { index, freqName ->
                    val level = if (index in effects.bandLevels.indices) effects.bandLevels[index] else 0f
                    val dbValue = (level / 100f).toInt()

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${if (dbValue > 0) "+" else ""}$dbValue dB",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (effects.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )

                        // شريط تمرير عمودي مخصص
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .width(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // تمثيل شريط التردد العمودي
                            Slider(
                                value = level,
                                onValueChange = { newLevel ->
                                    effects.setBandLevel(index, newLevel)
                                },
                                valueRange = effects.minBandLevel..effects.maxBandLevel,
                                enabled = effects.isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }

                        Text(
                            text = freqName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
