@file:OptIn(androidx.media3.common.util.UnstableApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.player

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackGroup
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

/**
 * تمثيل بيانات المسار الصوتي (Audio Track)
 */
data class AudioTrackItem(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val trackGroup: TrackGroup,
    val trackIndex: Int
)

/**
 * تمثيل بيانات مسار الترجمة (Subtitle Track)
 */
data class SubtitleTrackItem(
    val id: String,
    val name: String,
    val language: String?,
    val isSelected: Boolean,
    val isExternal: Boolean = false,
    val trackGroup: TrackGroup?,
    val trackIndex: Int
)

/**
 * مدير المسارات الصوتية وملفات الترجمة (TrackManager) لتطبيق "نوفا بلاير" (Nova Player).
 * يدعم:
 * 1. استخراج وقراءة كافة المسارات الصوتية المدمجة في الفيديو وتغيير المسار النشط.
 * 2. استخراج وقراءة ملفات الترجمة المدمجة (Embedded Subtitles) وتفعيلها أو إيقافها.
 * 3. استيراد ملف ترجمة خارجي (External SRT / VTT) وإلحاقه بالفيديو الحالي لحظياً.
 */
@OptIn(UnstableApi::class)
class TrackManager(private val player: ExoPlayer) {

    /**
     * جلب قائمة كافة المسارات الصوتية المتاحة في الفيديو الحالي
     */
    fun getAudioTracks(): List<AudioTrackItem> {
        val tracksList = mutableListOf<AudioTrackItem>()
        val tracks = player.currentTracks

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_AUDIO) {
                val mediaTrackGroup = group.mediaTrackGroup
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val isSelected = group.isTrackSelected(i)

                    val language = format.language?.uppercase()?.takeIf { it.isNotBlank() }
                    val label = format.label?.takeIf { it.isNotBlank() }
                    val displayName = when {
                        label != null -> label
                        language != null -> "صوت ($language)"
                        else -> "مسار صوتي #${tracksList.size + 1}"
                    }

                    tracksList.add(
                        AudioTrackItem(
                            id = format.id ?: "audio_${tracksList.size}",
                            name = displayName,
                            language = language,
                            isSelected = isSelected,
                            trackGroup = mediaTrackGroup,
                            trackIndex = i
                        )
                    )
                }
            }
        }
        return tracksList
    }

    /**
     * تغيير المسار الصوتي الحالي للفيديو
     */
    fun selectAudioTrack(track: AudioTrackItem) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(TrackSelectionOverride(track.trackGroup, track.trackIndex))
            .build()
    }

    /**
     * جلب قائمة كافة مسارات الترجمة المدمجة والمستوردة
     */
    fun getSubtitleTracks(): List<SubtitleTrackItem> {
        val subtitleList = mutableListOf<SubtitleTrackItem>()
        val tracks = player.currentTracks

        for (group in tracks.groups) {
            if (group.type == C.TRACK_TYPE_TEXT) {
                val mediaTrackGroup = group.mediaTrackGroup
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val isSelected = group.isTrackSelected(i)

                    val language = format.language?.uppercase()?.takeIf { it.isNotBlank() }
                    val label = format.label?.takeIf { it.isNotBlank() }
                    val displayName = when {
                        label != null -> label
                        language != null -> "ترجمة ($language)"
                        else -> "ترجمة #${subtitleList.size + 1}"
                    }

                    subtitleList.add(
                        SubtitleTrackItem(
                            id = format.id ?: "subtitle_${subtitleList.size}",
                            name = displayName,
                            language = language,
                            isSelected = isSelected,
                            isExternal = format.sampleMimeType == MimeTypes.APPLICATION_SUBRIP,
                            trackGroup = mediaTrackGroup,
                            trackIndex = i
                        )
                    )
                }
            }
        }
        return subtitleList
    }

    /**
     * اختيار مسار ترجمة معين لتفعيله، أو تمرير null لإيقاف الترجمة بالكامل
     */
    fun selectSubtitleTrack(track: SubtitleTrackItem?) {
        if (track == null || track.trackGroup == null) {
            // إيقاف الترجمة
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
        } else {
            // تفعيل مسار الترجمة المحدد
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(track.trackGroup, track.trackIndex))
                .build()
        }
    }

    /**
     * استيراد ملف ترجمة خارجي (SRT أو VTT) وإلحاقه بالفيديو الحالي
     * مع الحفاظ على موضع وحالة التشغيل الحالية
     */
    fun addExternalSubtitle(subtitleUri: Uri, label: String = "ترجمة خارجية (SRT)") {
        val currentMediaItem = player.currentMediaItem ?: return

        // تكوين مسار الترجمة الخارجي بصيغة SubRip (SRT)
        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
            .setMimeType(MimeTypes.APPLICATION_SUBRIP)
            .setLanguage("ar")
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        // بناء عنصر الوسائط الجديد مع دمج الترجمة الخارجية
        val existingConfigs = currentMediaItem.localConfiguration?.subtitleConfigurations ?: emptyList()
        val newConfigs = existingConfigs + subtitleConfig

        val updatedMediaItem = currentMediaItem.buildUpon()
            .setSubtitleConfigurations(newConfigs)
            .build()

        val currentPosition = player.currentPosition
        val playWhenReady = player.playWhenReady

        player.setMediaItem(updatedMediaItem, currentPosition)
        player.playWhenReady = playWhenReady
        player.prepare()

        // تفعيل الترجمة الخارجية المضافة افتراضياً
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .build()
    }
}

/**
 * واجهة منبثقة سفلية (BottomSheet) لاختيار وإدارة الترجمة والمسارات الصوتية
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracksSelectionBottomSheet(
    trackManager: TrackManager,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: الصوت, 1: الترجمة

    val audioTracks = remember(trackManager) { trackManager.getAudioTracks() }
    val subtitleTracks = remember(trackManager) { trackManager.getSubtitleTracks() }

    // لاقط الملفات لاختيار ملف ترجمة خارجي (.srt) من جهاز المستخدم
    val srtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            trackManager.addExternalSubtitle(it, "ملف ترجمة مستورد")
            onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.testTag("tracks_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // شريط التبويب: المسارات الصوتية / الترجمة
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("المسارات الصوتية (${audioTracks.size})")
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Subtitles, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("الترجمة (${subtitleTracks.size})")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                // تبويب المسارات الصوتية
                0 -> {
                    if (audioTracks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا توجد مسارات صوتية متعددة في هذا الفيديو",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(audioTracks) { track ->
                                TrackSelectionRow(
                                    title = track.name,
                                    subtitle = track.language?.let { "اللغة: $it" },
                                    isSelected = track.isSelected,
                                    onClick = {
                                        trackManager.selectAudioTrack(track)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }
                }

                // تبويب ملفات الترجمة
                1 -> {
                    Column {
                        // خيار إيقاف الترجمة
                        val isAnySubtitleSelected = subtitleTracks.any { it.isSelected }
                        TrackSelectionRow(
                            title = "إيقاف الترجمة",
                            subtitle = null,
                            isSelected = !isAnySubtitleSelected,
                            icon = Icons.Default.SubtitlesOff,
                            onClick = {
                                trackManager.selectSubtitleTrack(null)
                                onDismissRequest()
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // قائمة مسارات الترجمة المدمجة
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(subtitleTracks) { track ->
                                TrackSelectionRow(
                                    title = track.name,
                                    subtitle = if (track.isExternal) "ملف ترجمة خارجي" else track.language?.let { "اللغة: $it" },
                                    isSelected = track.isSelected,
                                    onClick = {
                                        trackManager.selectSubtitleTrack(track)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // زر استيراد ملف ترجمة خارجي (SRT)
                        FilledTonalButton(
                            onClick = {
                                srtPickerLauncher.launch(
                                    arrayOf(
                                        "application/x-subrip",
                                        "text/plain",
                                        "text/vtt",
                                        "*/*"
                                    )
                                )
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("import_srt_button")
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "استيراد ملف ترجمة خارجي (SRT)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * صف عنصر المسار الصوتي أو الترجمة داخل القائمة
 */
@Composable
private fun TrackSelectionRow(
    title: String,
    subtitle: String?,
    isSelected: Boolean,
    icon: ImageVector = Icons.Default.Check,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
