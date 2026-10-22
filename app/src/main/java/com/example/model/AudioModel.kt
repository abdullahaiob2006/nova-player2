package com.example.model

import android.content.ContentUris
import android.net.Uri
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * نموذج بيانات الملفات الصوتية (AudioModel) لتطبيق "نوفا بلاير" (Nova Player).
 * يدعم تشغيل ملفات MP3, AAC, FLAC, WAV, OGG, M4A.
 *
 * @param id المعرف الفريد للملف الصوتي
 * @param title عنوان المقطع الصوتي
 * @param artist اسم الفنان / المؤدي
 * @param album اسم الألبوم
 * @param duration مدة المقطع بالمللي ثانية
 * @param size حجم الملف بالبايت
 * @param path مسار الملف على الجهاز
 * @param albumId معرف الألبوم لجلب غلاف الألبوم
 * @param albumArtUri رابط الغلاف أو الصورة المصغرة للألبوم
 * @param dateAdded تاريخ إضافة الملف
 */
data class AudioModel(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val size: Long,
    val path: String,
    val albumId: Long = 0L,
    val albumArtUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
) {
    /**
     * رابط Uri الفعلي للملف
     */
    val contentUri: Uri
        get() = Uri.parse(path)

    /**
     * رابط Uri لغلاف الألبوم
     */
    val parsedAlbumArtUri: Uri?
        get() {
            if (albumArtUri != null) return Uri.parse(albumArtUri)
            if (albumId > 0L) {
                return ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
            }
            return null
        }

    /**
     * تنسيق المدة الزمنية بصيغة (mm:ss أو HH:mm:ss)
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
     * تنسيق حجم الملف
     */
    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB")
            var digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            if (digitGroups >= units.size) digitGroups = units.size - 1
            val value = size / Math.pow(1024.0, digitGroups.toDouble())
            return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
        }

    /**
     * تحويل ملف الصوت إلى VideoModel لتشغيله بنظام المشغل الموحد
     */
    fun toVideoModel(): VideoModel {
        return VideoModel(
            id = id,
            path = path,
            title = title,
            size = size,
            duration = duration,
            resolution = "Audio (${artist.ifBlank { "Unknown" }})",
            folderName = album.ifBlank { "Music" },
            thumbnailUri = parsedAlbumArtUri?.toString(),
            lastPlayedPosition = 0L,
            dateAdded = dateAdded
        )
    }
}
