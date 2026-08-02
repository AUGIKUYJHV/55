package com.surveyyemen.aerialmap.utils

import org.osmdroid.util.GeoPoint
import kotlin.math.*

/**
 * أداة القياس الهندسي: مسافات، مساحات، ميول، وفروق ارتفاع بين نقاط محددة على الخريطة.
 * تعتمد على بيانات الارتفاع (Elevation) المرفقة مع GeoPoint إن وُجدت (من GPS أو من DEM محلي).
 */
object MeasureTool {

    private const val EARTH_RADIUS_M = 6371000.0

    /** المسافة الأفقية بين نقطتين بمتر (صيغة Haversine) */
    fun horizontalDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val lat1 = Math.toRadians(p1.latitude)
        val lat2 = Math.toRadians(p2.latitude)
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /** إجمالي طول مسار متعدد النقاط */
    fun totalPathLength(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 0 until points.size - 1) {
            total += horizontalDistance(points[i], points[i + 1])
        }
        return total
    }

    /** مساحة مضلع مغلق بالمتر المربع (صيغة Shoelace مع تصحيح كروي تقريبي) */
    fun polygonArea(points: List<GeoPoint>): Double {
        if (points.size < 3) return 0.0
        var area = 0.0
        val n = points.size
        for (i in 0 until n) {
            val p1 = points[i]
            val p2 = points[(i + 1) % n]
            val lat1 = Math.toRadians(p1.latitude)
            val lat2 = Math.toRadians(p2.latitude)
            val lon1 = Math.toRadians(p1.longitude)
            val lon2 = Math.toRadians(p2.longitude)
            area += (lon2 - lon1) * (2 + sin(lat1) + sin(lat2))
        }
        area = abs(area * EARTH_RADIUS_M * EARTH_RADIUS_M / 2.0)
        return area
    }

    /** فرق الارتفاع بين نقطتين (متر). altitude بوحدة المتر فوق سطح البحر */
    fun elevationDifference(altitude1: Double, altitude2: Double): Double {
        return altitude2 - altitude1
    }

    /**
     * حساب الميل (Slope) بين نقطتين، كنسبة مئوية وكزاوية بالدرجات.
     * horizontalDist: المسافة الأفقية بالمتر بين النقطتين.
     * elevationDiff: فرق الارتفاع الرأسي بالمتر.
     */
    data class SlopeResult(val percentSlope: Double, val angleDegrees: Double)

    fun calculateSlope(horizontalDist: Double, elevationDiff: Double): SlopeResult {
        if (horizontalDist == 0.0) return SlopeResult(0.0, 90.0)
        val percent = (elevationDiff / horizontalDist) * 100.0
        val angle = Math.toDegrees(atan2(elevationDiff, horizontalDist))
        return SlopeResult(percent, angle)
    }

    fun formatDistance(meters: Double): String {
        return if (meters >= 1000) String.format("%.3f كم", meters / 1000)
        else String.format("%.2f م", meters)
    }

    fun formatArea(sqMeters: Double): String {
        return if (sqMeters >= 10000) String.format("%.4f هكتار", sqMeters / 10000)
        else String.format("%.2f م²", sqMeters)
    }
}
