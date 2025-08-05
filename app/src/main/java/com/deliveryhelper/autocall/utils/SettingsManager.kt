package com.deliveryhelper.autocall.utils

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "delivery_auto_call_prefs"
        
        // 서비스 설정
        private const val KEY_SERVICE_ENABLED = "service_enabled"
        
        // 콜비 설정
        private const val KEY_SINGLE_CALL_PRICE = "single_call_price"
        private const val KEY_DOUBLE_CALL_PRICE = "double_call_price"
        private const val KEY_TRIPLE_CALL_PRICE = "triple_call_price"
        
        // 거리 설정
        private const val KEY_MAX_DISTANCE = "max_distance"
        
        // 블랙리스트 설정
        private const val KEY_BLACKLIST_AREAS = "blacklist_areas"
        
        // 기본값
        private const val DEFAULT_SINGLE_PRICE = 4000
        private const val DEFAULT_DOUBLE_PRICE = 7000
        private const val DEFAULT_TRIPLE_PRICE = 9900
        private const val DEFAULT_MAX_DISTANCE = 2.0
    }
    
    // 서비스 설정
    fun setServiceEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }
    
    fun isServiceEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SERVICE_ENABLED, false)
    }
    
    // 콜비 설정
    fun setSingleCallPrice(price: Int) {
        sharedPreferences.edit().putInt(KEY_SINGLE_CALL_PRICE, price).apply()
    }
    
    fun getSingleCallPrice(): Int {
        return sharedPreferences.getInt(KEY_SINGLE_CALL_PRICE, DEFAULT_SINGLE_PRICE)
    }
    
    fun setDoubleCallPrice(price: Int) {
        sharedPreferences.edit().putInt(KEY_DOUBLE_CALL_PRICE, price).apply()
    }
    
    fun getDoubleCallPrice(): Int {
        return sharedPreferences.getInt(KEY_DOUBLE_CALL_PRICE, DEFAULT_DOUBLE_PRICE)
    }
    
    fun setTripleCallPrice(price: Int) {
        sharedPreferences.edit().putInt(KEY_TRIPLE_CALL_PRICE, price).apply()
    }
    
    fun getTripleCallPrice(): Int {
        return sharedPreferences.getInt(KEY_TRIPLE_CALL_PRICE, DEFAULT_TRIPLE_PRICE)
    }
    
    // 거리 설정
    fun setMaxDistance(distance: Double) {
        sharedPreferences.edit().putFloat(KEY_MAX_DISTANCE, distance.toFloat()).apply()
    }
    
    fun getMaxDistance(): Double {
        return sharedPreferences.getFloat(KEY_MAX_DISTANCE, DEFAULT_MAX_DISTANCE.toFloat()).toDouble()
    }
    
    // 블랙리스트 설정
    fun setBlacklistAreas(areas: String) {
        sharedPreferences.edit().putString(KEY_BLACKLIST_AREAS, areas).apply()
    }
    
    fun getBlacklistAreas(): String {
        return sharedPreferences.getString(KEY_BLACKLIST_AREAS, "") ?: ""
    }
    
    // 콜 조건 체크
    fun shouldAcceptCall(callCount: Int, price: Int, distance: Double): Boolean {
        if (!isServiceEnabled()) return false
        
        // 거리 체크
        if (distance > getMaxDistance()) return false
        
        // 콜비 체크
        val minimumPrice = when (callCount) {
            1 -> getSingleCallPrice()
            2 -> getDoubleCallPrice()
            3 -> getTripleCallPrice()
            else -> return false
        }
        
        return price >= minimumPrice
    }
    
    // 모든 설정 리셋
    fun resetAllSettings() {
        sharedPreferences.edit().clear().apply()
    }
}