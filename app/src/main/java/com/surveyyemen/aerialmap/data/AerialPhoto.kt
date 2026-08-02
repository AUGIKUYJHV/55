package com.surveyyemen.aerialmap.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * يمثل صورة جوية مضافة من المستخدم (قديمة أو حديثة) مع بيانات الفهرسة
 * والإحداثيات المرجعية لعرضها كطبقة (Overlay) فوق الخريطة الأساسية.
 */
@Entity(tableName = "aerial_photos")
data class AerialPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,          // مسار الصورة على التخزين المحلي
    val title: String,             // اسم/وصف الصورة
    val captureDate: Long,         // تاريخ التقاط الصورة (لعرضها بترتيب زمني)
    val addedDate: Long,           // تاريخ إضافتها للتطبيق
    val topLeftLat: Double,        // إحداثيات تثبيت الصورة على الخريطة (Georeference)
    val topLeftLon: Double,
    val bottomRightLat: Double,
    val bottomRightLon: Double,
    val opacity: Float = 1.0f,
    val isVisible: Boolean = true,
    val notes: String = ""
)

/**
 * يمثل ملف KML/KMZ تم استيراده
 */
@Entity(tableName = "imported_vector_files")
data class ImportedVectorFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val title: String,
    val importedDate: Long,
    val fileType: String,          // "KML" أو "KMZ"
    val isVisible: Boolean = true
)
