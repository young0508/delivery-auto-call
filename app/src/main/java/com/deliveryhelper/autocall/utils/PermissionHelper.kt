package com.deliveryhelper.autocall.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: Activity) {
    
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1002
    }
    
    // 위치 권한 체크
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // 위치 권한 요청
    fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    // 오버레이 권한 체크
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(activity)
    }
    
    // 오버레이 권한 요청
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
    }
    
    // 접근성 서비스 권한 체크
    fun hasAccessibilityPermission(): Boolean {
        val serviceName = "${activity.packageName}/${activity.packageName}.AutoCallService"
        val enabledServices = Settings.Secure.getString(
            activity.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName) == true
    }
    
    // 접근성 서비스 설정 화면으로 이동
    fun requestAccessibilityPermission() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        activity.startActivity(intent)
    }
    
    // 모든 권한 체크
    fun hasAllPermissions(): Boolean {
        return hasLocationPermission() && hasOverlayPermission() && hasAccessibilityPermission()
    }
    
    // 권한 결과 처리
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onResult: (Boolean) -> Unit
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                val granted = grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                onResult(granted)
            }
        }
    }
    
    // 권한 설명 다이얼로그 표시 여부
    fun shouldShowLocationPermissionRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
}