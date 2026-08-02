package com.surveyyemen.aerialmap.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.surveyyemen.aerialmap.LocaleHelper
import com.surveyyemen.aerialmap.R
import com.surveyyemen.aerialmap.data.SurveyDatabase
import com.surveyyemen.aerialmap.databinding.ActivityMainBinding
import com.surveyyemen.aerialmap.utils.CoordinateConverter
import com.surveyyemen.aerialmap.utils.KmlKmzImporter
import com.surveyyemen.aerialmap.utils.MeasureTool
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

/**
 * الشاشة الرئيسية: تعرض الخريطة (طبقة القمر الصناعي أو طبقة الشوارع)،
 * تتيح تفعيل GPS، أداة القياس، استيراد KML/KMZ، وتبديل نظام الإحداثيات.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var db: SurveyDatabase

    // نقاط أداة القياس النشطة
    private val measurePoints = mutableListOf<GeoPoint>()
    private var measurePolyline: Polyline? = null
    private var isMeasuring = false

    // نمط عرض الإحداثيات: false = خط طول/عرض، true = UTM
    private var showUtm = false

    private val kmlPickerLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importKmlKmz(it) }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableMyLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = SurveyDatabase.getInstance(this)

        setupMap()
        setupToolbar()
        checkLocationPermission()
    }

    private fun setupMap() {
        val map = binding.mapView
        map.setTileSource(TileSourceFactory.MAPNIK) // طبقة افتراضية؛ تُستبدل بطبقة القمر الصناعي المحلية عند إضافتها
        map.setMultiTouchControls(true)
        map.setUseDataConnection(true) // يعمل تلقائيًا أوفلاين إذا كانت البلاطات محفوظة مسبقًا في الكاش المحلي

        // مركز الخريطة الافتراضي: صنعاء
        val sanaa = GeoPoint(15.3694, 44.1910)
        map.controller.setZoom(13.0)
        map.controller.setCenter(sanaa)

        // إظهار موقع المستخدم الحالي
        myLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), map)
        myLocationOverlay.enableMyLocation()
        map.overlays.add(myLocationOverlay)

        // النقر على الخريطة أثناء وضع القياس يضيف نقطة جديدة
        map.setOnTouchListener { _, event ->
            if (isMeasuring && event.action == android.view.MotionEvent.ACTION_UP) {
                val proj = map.projection
                val geoPoint = proj.fromPixels(event.x.toInt(), event.y.toInt()) as GeoPoint
                addMeasurePoint(geoPoint)
            }
            false
        }
    }

    private fun setupToolbar() {
        // تبديل الطبقة الأساسية: صور القمر الصناعي مقابل خريطة الشوارع
        binding.btnSatelliteToggle.setOnClickListener {
            val map = binding.mapView
            if (map.tileProvider.tileSource.name() == TileSourceFactory.MAPNIK.name()) {
                // ملاحظة: لعرض قمر صناعي حقيقي عالي الجودة أوفلاين، يجب إضافة مصدر بلاطات
                // محلي (MBTiles) عبر مدير الطبقات - راجع LayerManagerActivity
                binding.tvStatus.text = getString(R.string.load_local_satellite_layer)
            }
        }

        // أداة القياس (مسافة/مساحة/ميل)
        binding.btnMeasure.setOnClickListener { toggleMeasureMode() }

        // استيراد KML/KMZ
        binding.btnImportKml.setOnClickListener {
            kmlPickerLauncher.launch("*/*")
        }

        // تبديل عرض الإحداثيات بين خط الطول/العرض و UTM
        binding.btnToggleCoords.setOnClickListener {
            showUtm = !showUtm
            updateCoordDisplay()
        }

        // فتح مدير الطبقات (لإضافة الصور الجوية القديمة وفهرستها)
        binding.btnLayers.setOnClickListener {
            startActivity(android.content.Intent(this, LayerManagerActivity::class.java))
        }

        // فتح الإعدادات (تبديل اللغة)
        binding.btnSettings.setOnClickListener {
            startActivity(android.content.Intent(this, SettingsActivity::class.java))
        }

        myLocationOverlay.runOnFirstFix {
            runOnUiThread { updateCoordDisplay() }
        }
    }

    private fun toggleMeasureMode() {
        isMeasuring = !isMeasuring
        if (!isMeasuring) {
            finalizeMeasurement()
        } else {
            measurePoints.clear()
            binding.tvStatus.text = getString(R.string.tap_map_to_measure)
        }
    }

    private fun addMeasurePoint(point: GeoPoint) {
        measurePoints.add(point)

        val map = binding.mapView
        measurePolyline?.let { map.overlays.remove(it) }

        if (measurePoints.size >= 2) {
            val polyline = Polyline()
            polyline.setPoints(measurePoints)
            map.overlays.add(polyline)
            measurePolyline = polyline

            val totalDist = MeasureTool.totalPathLength(measurePoints)

            // حساب الميل إن توفر ارتفاع (Altitude) بين آخر نقطتين
            val p1 = measurePoints[measurePoints.size - 2]
            val p2 = measurePoints[measurePoints.size - 1]
            val elevDiff = p2.altitude - p1.altitude
            val horizDist = MeasureTool.horizontalDistance(p1, p2)
            val slope = MeasureTool.calculateSlope(horizDist, elevDiff)

            binding.tvStatus.text = getString(
                R.string.measure_result,
                MeasureTool.formatDistance(totalDist),
                String.format("%.1f", slope.percentSlope),
                String.format("%.1f", slope.angleDegrees)
            )
        }
        map.invalidate()
    }

    private fun finalizeMeasurement() {
        if (measurePoints.size >= 3) {
            val area = MeasureTool.polygonArea(measurePoints)
            binding.tvStatus.text = getString(R.string.total_area, MeasureTool.formatArea(area))
        }
    }

    private fun importKmlKmz(uri: Uri) {
        val importer = KmlKmzImporter(this)
        val overlays = importer.importFile(uri, binding.mapView)
        binding.mapView.overlays.addAll(overlays)
        binding.mapView.invalidate()
        binding.tvStatus.text = getString(R.string.kml_imported, overlays.size)
    }

    private fun updateCoordDisplay() {
        val center = binding.mapView.mapCenter
        val text = if (showUtm) {
            val utm = CoordinateConverter.wgs84ToUtm(center.latitude, center.longitude)
            CoordinateConverter.formatUtm(utm)
        } else {
            CoordinateConverter.formatDecimalDegrees(center.latitude, center.longitude)
        }
        binding.tvCoords.text = text
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            enableMyLocation()
        }
    }

    private fun enableMyLocation() {
        myLocationOverlay.enableMyLocation()
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
