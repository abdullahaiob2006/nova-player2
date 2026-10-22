package com.example.vault

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.VideoModel
import kotlinx.coroutines.flow.Flow

/**
 * كيان تخزين موضع التوقف الأخير للفيديو (Resume Playback) في قاعدة بيانات Room
 */
@Entity(tableName = "playback_history")
data class PlaybackHistory(
    @PrimaryKey
    val videoId: Long,
    val lastPosition: Long,
    val duration: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * كيان بيانات الفيديو المحفوظ داخل الخزنة المشفرة (VaultVideoEntity).
 */
@Entity(tableName = "vault_videos")
data class VaultVideoEntity(
    @PrimaryKey
    val id: Long,
    val originalPath: String,
    val vaultPath: String,
    val title: String,
    val size: Long,
    val duration: Long,
    val resolution: String,
    val folderName: String,
    val thumbnailUri: String? = null,
    val dateHidden: Long = System.currentTimeMillis()
) {
    fun toVideoModel(): VideoModel {
        return VideoModel(
            id = id,
            path = vaultPath,
            title = title,
            size = size,
            duration = duration,
            resolution = resolution,
            folderName = "الخزنة الخاصة",
            thumbnailUri = thumbnailUri,
            lastPlayedPosition = 0L,
            dateAdded = dateHidden
        )
    }
}

/**
 * DAO لسجل واستئناف المشاهدة
 */
@Dao
interface PlaybackHistoryDao {

    @Query("SELECT * FROM playback_history")
    fun getAllHistory(): Flow<List<PlaybackHistory>>

    @Query("SELECT lastPosition FROM playback_history WHERE videoId = :id LIMIT 1")
    suspend fun getLastPosition(id: Long): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePosition(history: PlaybackHistory)

    @Query("DELETE FROM playback_history WHERE videoId = :id")
    suspend fun clearHistory(id: Long)
}

/**
 * DAO للخزنة الخاصة
 */
@Dao
interface VaultDao {

    @Query("SELECT * FROM vault_videos ORDER BY dateHidden DESC")
    fun getAllVaultVideos(): Flow<List<VaultVideoEntity>>

    @Query("SELECT * FROM vault_videos WHERE id = :videoId LIMIT 1")
    suspend fun getVaultVideoById(videoId: Long): VaultVideoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultVideo(video: VaultVideoEntity)

    @Delete
    suspend fun deleteVaultVideo(video: VaultVideoEntity)

    @Query("DELETE FROM vault_videos WHERE id = :videoId")
    suspend fun deleteById(videoId: Long)

    @Query("SELECT COUNT(*) FROM vault_videos")
    fun getVaultVideosCount(): Flow<Int>
}

/**
 * قاعدة بيانات Room لتطبيق "نوفا بلاير" (Nova Database)
 */
@Database(
    entities = [VideoModel::class, VaultVideoEntity::class, PlaybackHistory::class],
    version = 2,
    exportSchema = false
)
abstract class NovaDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: NovaDatabase? = null

        fun getInstance(context: Context): NovaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NovaDatabase::class.java,
                    "nova_player_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
