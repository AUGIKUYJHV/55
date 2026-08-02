package com.surveyyemen.aerialmap.utils

import android.content.Context
import android.net.Uri
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.MapView
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.NodeList

/**
 * يستورد ملفات KML و KMZ (وهي KML مضغوط) ويحوّل عناصرها (نقاط/خطوط/مضلعات)
 * إلى طبقات (Overlays) قابلة للعرض مباشرة فوق خريطة osmdroid.
 */
class KmlKmzImporter(private val context: Context) {

    /**
     * يقرأ ملف KML أو KMZ من مسار Uri، ويعيد قائمة العناصر الجاهزة لإضافتها للخريطة.
     */
    fun importFile(uri: Uri, mapView: MapView): List<org.osmdroid.views.overlay.Overlay> {
        val fileName = uri.lastPathSegment ?: "file"
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return emptyList()

        val kmlText: String = if (fileName.endsWith(".kmz", ignoreCase = true)) {
            extractKmlFromKmz(inputStream)
        } else {
            inputStream.bufferedReader().use { it.readText() }
        }

        return parseKml(kmlText, mapView)
    }

    /** KMZ هو ملف ZIP يحتوي على doc.kml بداخله بالإضافة لصور مرفقة */
    private fun extractKmlFromKmz(inputStream: java.io.InputStream): String {
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".kml", ignoreCase = true)) {
                    return zip.bufferedReader().readText()
                }
                entry = zip.nextEntry
            }
        }
        return ""
    }

    private fun parseKml(kmlText: String, mapView: MapView): List<org.osmdroid.views.overlay.Overlay> {
        val overlays = mutableListOf<org.osmdroid.views.overlay.Overlay>()
        if (kmlText.isBlank()) return overlays

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(kmlText.byteInputStream())

        // النقاط (Placemark > Point)
        val placemarks: NodeList = doc.getElementsByTagName("Placemark")
        for (i in 0 until placemarks.length) {
            val placemark = placemarks.item(i) as Element
            val name = getTagValue(placemark, "name") ?: "نقطة"

            // نقطة
            val pointCoords = getNestedTagValue(placemark, "Point", "coordinates")
            if (pointCoords != null) {
                val geoPoint = parseCoordinate(pointCoords)
                if (geoPoint != null) {
                    val marker = Marker(mapView)
                    marker.position = geoPoint
                    marker.title = name
                    overlays.add(marker)
                }
            }

            // خط (LineString)
            val lineCoords = getNestedTagValue(placemark, "LineString", "coordinates")
            if (lineCoords != null) {
                val points = parseCoordinateList(lineCoords)
                if (points.isNotEmpty()) {
                    val polyline = Polyline()
                    polyline.setPoints(points)
                    polyline.title = name
                    overlays.add(polyline)
                }
            }

            // مضلع (Polygon)
            val polyCoords = getNestedTagValue(placemark, "Polygon", "coordinates")
            if (polyCoords != null) {
                val points = parseCoordinateList(polyCoords)
                if (points.isNotEmpty()) {
                    val polygon = Polygon()
                    polygon.points = points
                    polygon.title = name
                    overlays.add(polygon)
                }
            }
        }

        return overlays
    }

    private fun getTagValue(element: Element, tag: String): String? {
        val nodes = element.getElementsByTagName(tag)
        return if (nodes.length > 0) nodes.item(0).textContent.trim() else null
    }

    private fun getNestedTagValue(parent: Element, parentTag: String, childTag: String): String? {
        val parentNodes = parent.getElementsByTagName(parentTag)
        if (parentNodes.length == 0) return null
        val parentElement = parentNodes.item(0) as Element
        return getTagValue(parentElement, childTag)
    }

    private fun parseCoordinate(raw: String): GeoPoint? {
        val parts = raw.trim().split(",")
        if (parts.size < 2) return null
        val lon = parts[0].toDoubleOrNull() ?: return null
        val lat = parts[1].toDoubleOrNull() ?: return null
        return GeoPoint(lat, lon)
    }

    private fun parseCoordinateList(raw: String): List<GeoPoint> {
        return raw.trim().split(Regex("\\s+")).mapNotNull { parseCoordinate(it) }
    }
}
