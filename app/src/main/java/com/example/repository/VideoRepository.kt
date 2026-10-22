package com.example.repository

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import com.example.model.VideoModel
import com.example.vault.NovaDatabase
import com.example.vault.PlaybackHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * مستودع البيانات (VideoRepository) لتطبيق "نوفا بلاير" (Nova Player).
 * يدمج بين استعلامات MediaStore لجلب فيديوهات الجهاز مع قاعدة بيانات Room (PlaybackHistoryDao)
 * لحفظ واسترجاع موضع التوقف الأخير لكل مقطع (Resume Playback).
 */
class VideoRepository(private val context: Context) {

    private val db = NovaDatabase.getInstance(context)
    private val playbackHistoryDao = db.playbackHistoryDao()

    /**
     * استعلام MediaStore مع دمج مواضع التشغيل الأخيرة المحفوظة في Room DB
     */
    fun getAllVideos(): Flow<List<VideoModel>> {
        val mediaStoreFlow = flow {
            emit(queryMediaStoreVideos())
        }
        val historyFlow = playbackHistoryDao.getAllHistory()

        return combine(mediaStoreFlow, historyFlow) { rawVideos, historyList ->
            val historyMap = historyList.associateBy({ it.videoId }, { it.lastPosition })
            rawVideos.map { video ->
                val savedPos = historyMap[video.id] ?: 0L
                video.copy(lastPlayedPosition = savedPos)
            }
        }.flowOn(Dispatchers.IO)
    }

    /**
     * حفظ موضع التوقف الأخير للفيديو في قاعدة بيانات Room لاستئناف المشاهدة لاحقاً
     */
    suspend fun savePlaybackPosition(videoId: Long, position: Long, duration: Long) = withContext(Dispatchers.IO) {
        if (videoId > 0L) {
            playbackHistoryDao.savePosition(
                PlaybackHistory(
                    videoId = videoId,
                    lastPosition = position,
                    duration = duration
                )
            )
        }
    }

    /**
     * جلب آخر موضع تشغيل محفوظ لفيديو معين
     */
    suspend fun getLastPosition(videoId: Long): Long = withContext(Dispatchers.IO) {
        playbackHistoryDao.getLastPosition(videoId) ?: 0L
    }

    /**
     * جلب الفيديوهات وتجميعها حسب اسم المجلد
     */
    fun getVideosGroupedByFolder(): Flow<Map<String, List<VideoModel>>> {
        return combine(getAllVideos()) { videosArray ->
            videosArray[0].groupBy { it.folderName }
        }
    }

    /**
     * البحث في مقاطع الفيديو بالاسم
     */
    fun searchVideos(query: String): Flow<List<VideoModel>> = flow {
        val allVideos = queryMediaStoreVideos()
        if (query.isBlank()) {
            emit(allVideos)
        } else {
            val filtered = allVideos.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.folderName.contains(query, ignoreCase = true)
            }
            emit(filtered)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * استعلام MediaStore الأساسي
     */
    private suspend fun queryMediaStoreVideos(): List<VideoModel> = withContext(Dispatchers.IO) {
        val videos = mutableListOf<VideoModel>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Video.Media.SIZE} > 0"
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val widthColumn = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightColumn = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val bucketColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val dateAddedColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Untitled"
                    
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    
                    val filePath = if (dataColumn != -1 && !cursor.isNull(dataColumn)) {
                        cursor.getString(dataColumn) ?: contentUri.toString()
                    } else {
                        contentUri.toString()
                    }

                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)

                    val width = if (widthColumn != -1 && !cursor.isNull(widthColumn)) cursor.getInt(widthColumn) else 0
                    val height = if (heightColumn != -1 && !cursor.isNull(heightColumn)) cursor.getInt(heightColumn) else 0
                    val resolution = if (width > 0 && height > 0) "${width}x${height}" else "HD"

                    val folderName = if (bucketColumn != -1 && !cursor.isNull(bucketColumn)) {
                        cursor.getString(bucketColumn) ?: "Internal Storage"
                    } else {
                        "Internal Storage"
                    }

                    val dateAdded = if (dateAddedColumn != -1 && !cursor.isNull(dateAddedColumn)) {
                        cursor.getLong(dateAddedColumn) * 1000L
                    } else {
                        System.currentTimeMillis()
                    }

                    videos.add(
                        VideoModel(
                            id = id,
                            path = filePath,
                            title = name,
                            size = size,
                            duration = duration,
                            resolution = resolution,
                            folderName = folderName,
                            thumbnailUri = contentUri.toString(),
                            lastPlayedPosition = 0L,
                            dateAdded = dateAdded
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        videos
    }
}
