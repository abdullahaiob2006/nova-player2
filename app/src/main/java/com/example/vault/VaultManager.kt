package com.example.vault

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import com.example.model.VideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * مدير الخزنة الخاصة (VaultManager) المسؤول عن:
 * 1. التحقق وتعيين رمز المرور (PIN) المشفر.
 * 2. نقل ملفات الفيديو من الذاكرة العامة إلى مجلد التطبيق الداخلي الخاص والمشفر.
 * 3. استعادة الفيديوهات من الخزنة إلى الذاكرة العامة.
 */
class VaultManager(private val context: Context) {

    private val db = NovaDatabase.getInstance(context)
    private val vaultDao = db.vaultDao()
    private val prefs = context.getSharedPreferences("nova_vault_prefs", Context.MODE_PRIVATE)

    // مجلد الخزنة السري داخل ذاكرة التطبيق الداخلية المحمية
    private val vaultDir: File
        get() {
            val dir = File(context.filesDir, "vault_storage")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    /**
     * تدفق مقاطع الفيديو المخفية في الخزنة
     */
    val vaultVideos: Flow<List<VaultVideoEntity>> = vaultDao.getAllVaultVideos()

    /**
     * هل قام المستخدم بتعيين رمز PIN مسبقاً؟
     */
    fun isPinSet(): Boolean {
        return prefs.contains(KEY_PIN_HASH)
    }

    /**
     * تعيين رمز PIN جديد للخزنة (يتم تشفيره بـ SHA-256)
     */
    fun setPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val hash = hashString(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        return true
    }

    /**
     * التحقق من صحة رمز الـ PIN المدخل
     */
    fun verifyPin(pin: String): Boolean {
        val savedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return savedHash == hashString(pin)
    }

    /**
     * نقل وإخفاء فيديو من العرض العام إلى الخزنة الخاصة
     */
    suspend fun moveToVault(video: VideoModel): Boolean = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(video.path)
            val destFileName = "vault_${video.id}_${System.currentTimeMillis()}.nova_vault"
            val destFile = File(vaultDir, destFileName)

            // نسخ/نقل الملف إلى المجلد الخاص الآمن للتطبيق
            if (sourceFile.exists()) {
                FileInputStream(sourceFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                // حذف الملف الأصلي من الذاكرة العامة ليختفي من المعرض فوراً
                sourceFile.delete()
                // تحديث مؤشر MediaStore لإزالة الفيديو من المعرض العام
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(sourceFile.absolutePath),
                    null,
                    null
                )
            } else {
                // في حال كان المسار عبر Content Uri
                context.contentResolver.openInputStream(video.contentUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // تسجيل الفيديو في قاعدة بيانات الخزنة
            val vaultEntity = VaultVideoEntity(
                id = video.id,
                originalPath = video.path,
                vaultPath = destFile.absolutePath,
                title = video.title,
                size = video.size,
                duration = video.duration,
                resolution = video.resolution,
                folderName = video.folderName,
                thumbnailUri = destFile.absolutePath,
                dateHidden = System.currentTimeMillis()
            )
            vaultDao.insertVaultVideo(vaultEntity)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * استعادة الفيديو من الخزنة إلى الذاكرة العامة وإلغاء إخفائه
     */
    suspend fun restoreFromVault(vaultVideo: VaultVideoEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val vaultFile = File(vaultVideo.vaultPath)
            val targetFile = File(vaultVideo.originalPath)

            // التأكد من وجود المجلد الوجهة في الذاكرة العامة
            targetFile.parentFile?.mkdirs()

            if (vaultFile.exists()) {
                FileInputStream(vaultFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                vaultFile.delete()

                // إعلام نظام أندرويد ليعود الفيديو للظهور في المعرض العام
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    null,
                    null
                )
            }

            // حذفه من قاعدة بيانات الخزنة
            vaultDao.deleteVaultVideo(vaultVideo)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * تشفير النصوص برمجياً باستخدام SHA-256
     */
    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_PIN_HASH = "vault_pin_hash_key"
    }
}
