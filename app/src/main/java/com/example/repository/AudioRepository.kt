package com.example.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.model.AudioModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * مستودع ملفات الصوت (AudioRepository) المسؤول عن استعلام مخزن الوسائط (MediaStore)
 * لجلب وتصفية الأغاني والمقاطع الصوتية (MP3, AAC, FLAC, WAV) على جهاز المستخدم.
 */
class AudioRepository(private val context: Context) {

    /**
     * استعلام كافة الملفات الصوتية مرتبة بالأحدث
     */
    fun getAllAudios(): Flow<List<AudioModel>> = flow {
        val audioList = queryMediaStoreAudio()
        emit(audioList)
    }.flowOn(Dispatchers.IO)

    /**
     * البحث في المقاطع الصوتية بالاسم أو الفنان أو الألبوم
     */
    fun searchAudios(query: String): Flow<List<AudioModel>> = flow {
        val allAudios = queryMediaStoreAudio()
        if (query.isBlank()) {
            emit(allAudios)
        } else {
            val filtered = allAudios.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
            emit(filtered)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * استعلام MediaStore لجلب المقاطع الصوتية الحقيقية
     */
    private suspend fun queryMediaStoreAudio(): List<AudioModel> = withContext(Dispatchers.IO) {
        val audios = mutableListOf<AudioModel>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        // تصفية الملفات الموسيقية الصالحة واستثناء الإشعارات أو النغمات القصيرة جداً
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "مقطع صوتي"
                    val artist = if (artistColumn != -1 && !cursor.isNull(artistColumn)) {
                        cursor.getString(artistColumn) ?: "فنان غير معروف"
                    } else "فنان غير معروف"

                    val album = if (albumColumn != -1 && !cursor.isNull(albumColumn)) {
                        cursor.getString(albumColumn) ?: "ألبوم غير معروف"
                    } else "ألبوم غير معروف"

                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val filePath = if (dataColumn != -1 && !cursor.isNull(dataColumn)) {
                        cursor.getString(dataColumn) ?: contentUri.toString()
                    } else contentUri.toString()

                    val albumId = if (albumIdColumn != -1 && !cursor.isNull(albumIdColumn)) {
                        cursor.getLong(albumIdColumn)
                    } else 0L

                    val dateAdded = if (dateAddedColumn != -1 && !cursor.isNull(dateAddedColumn)) {
                        cursor.getLong(dateAddedColumn) * 1000L
                    } else System.currentTimeMillis()

                    audios.add(
                        AudioModel(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "فنان غير معروف" else artist,
                            album = if (album == "<unknown>") "ألبوم عام" else album,
                            duration = duration,
                            size = size,
                            path = filePath,
                            albumId = albumId,
                            albumArtUri = null,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        audios
    }
}
