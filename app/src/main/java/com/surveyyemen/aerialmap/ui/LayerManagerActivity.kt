package com.surveyyemen.aerialmap.ui

import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.surveyyemen.aerialmap.R
import com.surveyyemen.aerialmap.data.AerialPhoto
import com.surveyyemen.aerialmap.data.SurveyDatabase
import com.surveyyemen.aerialmap.databinding.ActivityLayerManagerBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * تتيح للمستخدم إضافة صور جوية قديمة (بصيغة صورة عادية + تحديد إحداثيات
 * الزاويتين لتثبيتها Georeference) وفهرستها تلقائيًا بترتيب التاريخ.
 */
class LayerManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLayerManagerBinding
    private lateinit var db: SurveyDatabase
    private var selectedImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.tvSelectedFile.text = it.lastPathSegment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLayerManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = SurveyDatabase.getInstance(this)

        binding.btnPickImage.setOnClickListener { imagePickerLauncher.launch("image/*") }
        binding.btnSavePhoto.setOnClickListener { savePhoto() }

        observePhotos()
    }

    private fun savePhoto() {
        val uri = selectedImageUri ?: return
        val title = binding.etPhotoTitle.text.toString().ifBlank { "صورة جوية" }
        val dateText = binding.etCaptureDate.text.toString() // بصيغة yyyy-MM-dd

        val captureDate = try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateText)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }

        val topLeftLat = binding.etTopLeftLat.text.toString().toDoubleOrNull() ?: 15.40
        val topLeftLon = binding.etTopLeftLon.text.toString().toDoubleOrNull() ?: 44.15
        val bottomRightLat = binding.etBottomRightLat.text.toString().toDoubleOrNull() ?: 15.35
        val bottomRightLon = binding.etBottomRightLon.text.toString().toDoubleOrNull() ?: 44.22

        val photo = AerialPhoto(
            filePath = uri.toString(),
            title = title,
            captureDate = captureDate,
            addedDate = System.currentTimeMillis(),
            topLeftLat = topLeftLat,
            topLeftLon = topLeftLon,
            bottomRightLat = bottomRightLat,
            bottomRightLon = bottomRightLon
        )

        lifecycleScope.launch {
            db.photoDao().insertPhoto(photo)
        }
    }

    private fun observePhotos() {
        lifecycleScope.launch {
            db.photoDao().getAllPhotosSortedByDate().collectLatest { photos ->
                val labels = photos.map {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.captureDate))
                    "${it.title}  ($date)"
                }
                binding.listPhotos.adapter = ArrayAdapter(
                    this@LayerManagerActivity,
                    android.R.layout.simple_list_item_1,
                    labels
                )
            }
        }
    }
}
