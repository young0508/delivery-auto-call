package com.deliveryhelper.autocall.models

import com.google.android.gms.maps.model.LatLng

data class BlacklistArea(
    val id: Long,
    val name: String,
    val points: List<LatLng>
) {
    // Point-in-Polygon 알고리즘으로 좌표가 영역 내부에 있는지 확인
    fun containsPoint(point: LatLng): Boolean {
        return isPointInPolygon(point, points)
    }
    
    // Ray casting 알고리즘을 사용한 Point-in-Polygon 판정
    private fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        if (polygon.size < 3) return false
        
        var isInside = false
        var j = polygon.size - 1
        
        for (i in polygon.indices) {
            val xi = polygon[i].longitude
            val yi = polygon[i].latitude
            val xj = polygon[j].longitude
            val yj = polygon[j].latitude
            
            if (((yi > point.latitude) != (yj > point.latitude)) &&
                (point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi) + xi)) {
                isInside = !isInside
            }
            j = i
        }
        
        return isInside
    }
    
    // JSON 문자열로 변환 (저장용)
    fun toJsonString(): String {
        val pointsJson = points.joinToString(",") { 
            "${it.latitude}:${it.longitude}" 
        }
        return "$id|$name|$pointsJson"
    }
    
    companion object {
        // JSON 문자열에서 객체 생성
        fun fromJsonString(jsonString: String): BlacklistArea? {
            return try {
                val parts = jsonString.split("|")
                if (parts.size != 3) return null
                
                val id = parts[0].toLong()
                val name = parts[1]
                val points = parts[2].split(",").map { pointStr ->
                    val coords = pointStr.split(":")
                    LatLng(coords[0].toDouble(), coords[1].toDouble())
                }
                
                BlacklistArea(id, name, points)
            } catch (e: Exception) {
                null
            }
        }
    }
}