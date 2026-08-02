package com.surveyyemen.aerialmap.utils

import org.locationtech.proj4j.CRSFactory
import org.locationtech.proj4j.CoordinateTransformFactory
import org.locationtech.proj4j.ProjCoordinate
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * تحويل الإحداثيات بين WGS84 (خط الطول/العرض) ونظام UTM العالمي.
 * اليمن تقع ضمن النطاقين UTM Zone 38N و 39N (خط زوايا 42 شرقًا تقريبًا يفصل بينهما).
 * صنعاء تقع ضمن Zone 38N.
 */
object CoordinateConverter {

    private val crsFactory = CRSFactory()
    private val ctFactory = CoordinateTransformFactory()

    data class UtmResult(val zone: Int, val hemisphere: Char, val easting: Double, val northing: Double)

    /** حساب رقم منطقة UTM من خط الطول */
    fun utmZoneFromLongitude(lon: Double): Int {
        return floor((lon + 180) / 6).toInt() + 1
    }

    /** تحويل من WGS84 (Lat/Lon) إلى UTM */
    fun wgs84ToUtm(lat: Double, lon: Double): UtmResult {
        val zone = utmZoneFromLongitude(lon)
        val hemisphere = if (lat >= 0) 'N' else 'S'
        val epsgCode = if (hemisphere == 'N') "EPSG:326$zone" else "EPSG:327$zone"

        val wgs84 = crsFactory.createFromName("EPSG:4326")
        val utmCrs = crsFactory.createFromName(epsgCode)
        val transform = ctFactory.createTransform(wgs84, utmCrs)

        val src = ProjCoordinate(lon, lat)
        val dst = ProjCoordinate()
        transform.transform(src, dst)

        return UtmResult(zone, hemisphere, dst.x, dst.y)
    }

    /** تحويل من UTM إلى WGS84 (Lat/Lon) */
    fun utmToWgs84(easting: Double, northing: Double, zone: Int, hemisphere: Char): Pair<Double, Double> {
        val epsgCode = if (hemisphere == 'N') "EPSG:326$zone" else "EPSG:327$zone"
        val utmCrs = crsFactory.createFromName(epsgCode)
        val wgs84 = crsFactory.createFromName("EPSG:4326")
        val transform = ctFactory.createTransform(utmCrs, wgs84)

        val src = ProjCoordinate(easting, northing)
        val dst = ProjCoordinate()
        transform.transform(src, dst)

        return Pair(dst.y, dst.x) // lat, lon
    }

    fun formatUtm(r: UtmResult): String {
        return "Zone ${r.zone}${r.hemisphere}  E: ${r.easting.roundToInt()}m  N: ${r.northing.roundToInt()}m"
    }

    fun formatDecimalDegrees(lat: Double, lon: Double): String {
        return String.format("%.6f°, %.6f°", lat, lon)
    }

    /** تحويل الدرجات العشرية إلى درجات/دقائق/ثواني */
    fun toDMS(decimal: Double): String {
        val deg = decimal.toInt()
        val minFull = (decimal - deg) * 60
        val min = minFull.toInt()
        val sec = (minFull - min) * 60
        return "${Math.abs(deg)}° ${Math.abs(min)}' ${String.format("%.2f", Math.abs(sec))}\""
    }
}

/**
 * وحدات القياس اليمنية التقليدية المستخدمة في مسح الأراضي والعقارات،
 * مع التحويل إلى النظام المتري العالمي.
 * القيم المرجعية الأكثر شيوعًا في اليمن (قد تختلف قليلاً حسب المحافظة والعرف المحلي).
 */
object YemeniUnits {

    enum class LengthUnit(val label: String, val toMeters: Double) {
        METER("متر", 1.0),
        QASABA("قصبة", 3.5),        // القصبة اليمنية الشائعة ≈ 3.5 م (تختلف محليًا)
        DHIRAA("ذراع", 0.58),        // الذراع الحساوي/اليمني التقليدي
        BAAA("باع", 2.0)             // الباع ≈ مدّ الذراعين
    }

    enum class AreaUnit(val label: String, val toSquareMeters: Double) {
        SQUARE_METER("متر مربع", 1.0),
        LIBNA("لبنة", 646.0),        // اللبنة اليمنية الشائعة في صنعاء ومحيطها ≈ 646 م²
        QADAH("قدح", 269.0),
        MAAD("مأد", 2584.0),
        FEDDAN("فدان (يمني تقريبي)", 4200.0)
    }

    fun lengthToMeters(value: Double, unit: LengthUnit): Double = value * unit.toMeters
    fun metersToLength(meters: Double, unit: LengthUnit): Double = meters / unit.toMeters

    fun areaToSquareMeters(value: Double, unit: AreaUnit): Double = value * unit.toSquareMeters
    fun squareMetersToArea(sqMeters: Double, unit: AreaUnit): Double = sqMeters / unit.toSquareMeters
}
