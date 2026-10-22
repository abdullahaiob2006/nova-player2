package com.example.model

import android.net.Uri
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * نموذج بيانات الفيديو لتطبيق "نوفا بلاير" (Nova Player).
 * يدعم التخزين المحلي عبر Room وكذلك التمرير الآمن بين شاشات واجهة المستخدم.
 *
 * @param id المعرف الفريد للفيديو (من MediaStore أو قاعدة البيانات)
 * @param path المسار الفعلي للملف أو رابط تشغيل الفيديو (URI / File Path)
 * @param title عنوان أو اسم مقطع الفيديو
 * @param size حجم الفيديو بالبايت (Byte)
 * @param duration مدة الفيديو الإجمالية بالمللي ثانية (ms)
 * @param resolution دقة الفيديو (مثل "1920x1080" أو "4K")
 * @param folderName اسم المجلد الحاوي للفيديو
 * @param thumbnailUri رابط أو مسار الصورة المصغرة للفيديو
 * @param lastPlayedPosition موضع التوقف الأخير بالمللي ثانية لميزة استئناف المشاهدة
 * @param dateAdded تاريخ إضافة الفيديو (Timestamp)
 */
@Entity(tableName = "videos")
data class VideoModel(
    @PrimaryKey
    val id: Long,
    val path: String,
    val title: String,
    val size: Long,
    val duration: Long,
    val resolution: String,
    val folderName: String,
    val thumbnailUri: String? = null,
    val lastPlayedPosition: Long = 0L,
    val dateAdded: Long = System.currentTimeMillis()
) {
    /**
     * تحويل مسار الفيديو إلى كائن Uri جاهز للاستخدام مع ExoPlayer
     */
    val contentUri: Uri
        get() = Uri.parse(path)

    /**
     * تحويل مسار الصورة المصغرة إلى كائن Uri جاهز للعرض مع Coil
     */
    val parsedThumbnailUri: Uri?
        get() = thumbnailUri?.let { Uri.parse(it) }

    /**
     * تنسيق مدة الفيديو لصيغة مقروءة للمستخدم:
     * مثلاً (03:45) أو (01:24:10) إذا تجاوزت الساعة.
     */
    val formattedDuration: String
        get() {
            if (duration <= 0) return "00:00"
            val hours = TimeUnit.MILLISECONDS.toHours(duration)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60

            return if (hours > 0) {
                String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
        }

    /**
     * تنسيق حجم الملف لصيغة سهلة القراءة (B, KB, MB, GB):
     * مثلاً: "15.4 MB" أو "1.2 GB"
     */
    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            val value = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
        }

    /**
     * نسبة المشاهدة المكتملة (من 0.0 إلى 1.0) لشريط التقدم على بطاقة الفيديو
     */
    val playbackProgressRatio: Float
        get() = if (duration > 0) (lastPlayedPosition.toFloat() / duration).coerceIn(0f, 1f) else 0f
}
