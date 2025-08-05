package com.deliveryhelper.autocall.utils

import android.content.Context
import com.deliveryhelper.autocall.models.BlacklistArea
import com.google.android.gms.maps.model.LatLng

class BlacklistManager(private val context: Context) {
    
    private val settingsManager = SettingsManager(context)
    private val blacklistAreas = mutableListOf<BlacklistArea>()
    
    init {
        loadBlacklistAreas()
    }
    
    // 블랙리스트 영역 추가
    fun addBlacklistArea(area: BlacklistArea) {
        blacklistAreas.add(area)
        saveBlacklistAreas()
    }
    
    // 블랙리스트 영역 제거
    fun removeBlacklistArea(areaId: Long) {
        blacklistAreas.removeAll { it.id == areaId }
        saveBlacklistAreas()
    }
    
    // 모든 블랙리스트 영역 가져오기
    fun getBlacklistAreas(): List<BlacklistArea> {
        return blacklistAreas.toList()
    }
    
    // 특정 좌표가 블랙리스트 영역에 포함되는지 확인
    fun isInBlacklistArea(point: LatLng): Boolean {
        return blacklistAreas.any { area ->
            area.containsPoint(point)
        }
    }
    
    // 주소로 블랙리스트 확인 (geocoding 필요)
    fun isAddressInBlacklist(address: String): Boolean {
        // 실제 구현에서는 geocoding을 통해 주소를 좌표로 변환해야 함
        // 여기서는 간단히 주소 문자열 매칭으로 구현
        return blacklistAreas.any { area ->
            address.contains(area.name, ignoreCase = true)
        }
    }
    
    // 블랙리스트 영역들을 SharedPreferences에 저장
    private fun saveBlacklistAreas() {
        val jsonString = blacklistAreas.joinToString(";") { area ->
            area.toJsonString()
        }
        settingsManager.setBlacklistAreas(jsonString)
    }
    
    // SharedPreferences에서 블랙리스트 영역들 로드
    private fun loadBlacklistAreas() {
        blacklistAreas.clear()
        val jsonString = settingsManager.getBlacklistAreas()
        
        if (jsonString.isNotEmpty()) {
            jsonString.split(";").forEach { areaJson ->
                BlacklistArea.fromJsonString(areaJson)?.let { area ->
                    blacklistAreas.add(area)
                }
            }
        }
    }
    
    // 모든 블랙리스트 영역 삭제
    fun clearAllAreas() {
        blacklistAreas.clear()
        saveBlacklistAreas()
    }
    
    // 블랙리스트 영역 개수
    fun getAreaCount(): Int {
        return blacklistAreas.size
    }
}