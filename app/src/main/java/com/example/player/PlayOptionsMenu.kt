package com.example.player

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * أوضاع اتجاه الشاشة (Screen Orientation Modes)
 */
enum class ScreenOrientationMode(val title: String, val orientationCode: Int) {
    AUTO("تلقائي (حسب الحساس)", ActivityInfo.SCREEN_ORIENTATION_SENSOR),
    LANDSCAPE("أفقي إجباري", ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
    PORTRAIT("عمودي إجباري", ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
}

/**
 * أوضاع فك تشفير الفيديو (HW Hardware vs SW Software)
 */
enum class DecoderMode(val code: String, val title: String, val description: String) {
    HW("HW", "عتاد (Hardware)", "تسريع العتاد المباشر عبر معالج الرسوميات"),
    SW("SW", "برمجي (Software)", "فك التشفير البرمجي للتوافق الأقصى")
}

/**
 * فئة مساعدة لالتقاط صور الشاشة بجودة فائقة وحفظها في مجلد الصور
 */
object VideoScreenshotHelper {

    fun captureScreenshot(
        activity: Activity,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val window = activity.window ?: run {
            onError("تعذر الوصول لنافذة المشغل")
            return
        }

        val view = window.decorView
        val width = view.width.coerceAtLeast(1)
        val height = view.height.coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val rect = Rect(0, 0, width, height)
            PixelCopy.request(
                window,
                rect,
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        saveBitmapToGallery(activity, bitmap, onSuccess, onError)
                    } else {
                        onError("فشل التقاط الصورة: رمز الخطأ $copyResult")
                    }
                },
                Handler(Looper.getMainLooper())
            )
        } else {
            // للأجهزة القديمة
            try {
                view.isDrawingCacheEnabled = true
                val cacheBitmap = Bitmap.createBitmap(view.drawingCache)
                view.isDrawingCacheEnabled = false
                saveBitmapToGallery(activity, cacheBitmap, onSuccess, onError)
            } catch (e: Exception) {
                onError("خطأ أثناء التقاط الشاشة: ${e.message}")
            }
        }
    }

    private fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        onSuccess: (Uri) -> Unit,
        onError: (String) -> Unit
    ) {
        val fileName = "Nova_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NovaPlayer")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        try {
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }

                onSuccess(uri)
            } else {
                onError("تعذر إنشاء ملف الصورة في المعرض")
            }
        } catch (e: Exception) {
            onError("حدث خطأ أثناء حفظ لقطة الشاشة: ${e.message}")
        }
    }
}

/**
 * نافذة خيارات التشغيل المتقدمة (Play Options Menu Bottom Sheet) على غرار xPlayer
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayOptionsMenuBottomSheet(
    orientationMode: ScreenOrientationMode,
    onOrientationChange: (ScreenOrientationMode) -> Unit,
    decoderMode: DecoderMode,
    onDecoderChange: (DecoderMode) -> Unit,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    isNightMode: Boolean,
    onNightModeToggle: (Boolean) -> Unit,
    nightDimLevel: Float,
    onNightDimLevelChange: (Float) -> Unit,
    isMirrored: Boolean,
    onMirrorToggle: (Boolean) -> Unit,
    isVisualEnhancer: Boolean,
    onVisualEnhancerToggle: (Boolean) -> Unit,
    onTakeScreenshot: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    abRepeatPointA: Long?,
    abRepeatPointB: Long?,
    isAbRepeatActive: Boolean,
    onSetPointA: () -> Unit,
    onSetPointB: () -> Unit,
    onClearAbRepeat: () -> Unit,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("play_options_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // عنوان القائمة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "خيارات التشغيل المتقدمة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(onClick = onDismissRequest) {
                    Text("إغلاق")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. فك التشفير (HW vs SW Decoder)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("وضع فك التشفير", fontWeight = FontWeight.Bold)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DecoderMode.entries.forEach { mode ->
                                val isSelected = decoderMode == mode
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onDecoderChange(mode) }
                                ) {
                                    Text(
                                        text = mode.code,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = decoderMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. التحكم باتجاه الشاشة (Orientation)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اتجاه الشاشة", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScreenOrientationMode.entries.forEach { mode ->
                            val isSelected = orientationMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOrientationChange(mode) }
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                ) {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. الوضع الليلي (Night Mode Dim Filter)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("الوضع الليلي (Night Mode)", fontWeight = FontWeight.Bold)
                                Text("فلتر داكن لحماية العين تحت الإضاءة المنخفضة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = isNightMode,
                            onCheckedChange = onNightModeToggle
                        )
                    }

                    if (isNightMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("درجة التعتيم:", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.width(8.dp))
                            Slider(
                                value = nightDimLevel,
                                onValueChange = onNightDimLevelChange,
                                valueRange = 0.1f..0.85f,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("${(nightDimLevel * 100).toInt()}%", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. مرآة الفيديو ومحسن العرض وكتم الصوت والتقاط صورة (إجراءات سريعة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // مرآة الفيديو (Mirror)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMirrored) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onMirrorToggle(!isMirrored) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flip,
                            contentDescription = null,
                            tint = if (isMirrored) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMirrored) "مرآة: مفعّل" else "مرآة الفيديو",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // كتم الصوت الفوري
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMuted) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleMute() }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMuted) "صامت" else "كتم الصوت",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // التقاط صورة (Screenshot)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            onTakeScreenshot()
                            onDismissRequest()
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لقطة شاشة",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // محسن العرض (Visual Enhancer)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isVisualEnhancer) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVisualEnhancerToggle(!isVisualEnhancer) }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isVisualEnhancer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isVisualEnhancer) "محسن: نشط" else "محسن العرض",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. تكرار المقطع (A-B Repeat)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تكرار مقطع محدد (A-B Repeat)", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = onSetPointA) {
                            Text(text = if (abRepeatPointA != null) "A: ${formatMillis(abRepeatPointA)}" else "تعيين A")
                        }

                        FilledTonalButton(onClick = onSetPointB) {
                            Text(text = if (abRepeatPointB != null) "B: ${formatMillis(abRepeatPointB)}" else "تعيين B")
                        }

                        OutlinedButton(onClick = onClearAbRepeat) {
                            Text("إلغاء")
                        }
                    }

                    if (isAbRepeatActive && abRepeatPointA != null && abRepeatPointB != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "جاري تكرار المقطع بين ${formatMillis(abRepeatPointA)} و ${formatMillis(abRepeatPointB)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. روابط سريعة: المعادل الصوتي وسرعات التشغيل
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        onDismissRequest()
                        onOpenEqualizer()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("المعادل الصوتي")
                }

                FilledTonalButton(
                    onClick = {
                        onDismissRequest()
                        onOpenSpeedDialog()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("سرعة التشغيل")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatMillis(millis: Long): String {
    if (millis <= 0) return "00:00"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
