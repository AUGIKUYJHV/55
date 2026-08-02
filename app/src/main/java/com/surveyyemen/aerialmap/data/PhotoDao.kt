package com.surveyyemen.aerialmap.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert
    suspend fun insertPhoto(photo: AerialPhoto): Long

    @Update
    suspend fun updatePhoto(photo: AerialPhoto)

    @Delete
    suspend fun deletePhoto(photo: AerialPhoto)

    // فرز الصور الجوية القديمة حسب تاريخ الالتقاط (الأحدث أولًا)
    @Query("SELECT * FROM aerial_photos ORDER BY captureDate DESC")
    fun getAllPhotosSortedByDate(): Flow<List<AerialPhoto>>

    @Query("SELECT * FROM aerial_photos WHERE isVisible = 1")
    fun getVisiblePhotos(): Flow<List<AerialPhoto>>

    @Query("SELECT * FROM aerial_photos WHERE captureDate BETWEEN :startDate AND :endDate ORDER BY captureDate DESC")
    fun getPhotosInDateRange(startDate: Long, endDate: Long): Flow<List<AerialPhoto>>

    @Insert
    suspend fun insertVectorFile(file: ImportedVectorFile): Long

    @Delete
    suspend fun deleteVectorFile(file: ImportedVectorFile)

    @Query("SELECT * FROM imported_vector_files ORDER BY importedDate DESC")
    fun getAllVectorFiles(): Flow<List<ImportedVectorFile>>
}

@Database(entities = [AerialPhoto::class, ImportedVectorFile::class], version = 1, exportSchema = false)
abstract class SurveyDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile private var INSTANCE: SurveyDatabase? = null

        fun getInstance(context: android.content.Context): SurveyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SurveyDatabase::class.java,
                    "survey_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
