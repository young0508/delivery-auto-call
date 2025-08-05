package com.deliveryhelper.autocall

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.deliveryhelper.autocall.databinding.ActivityMainBinding
import com.deliveryhelper.autocall.utils.PermissionHelper
import com.deliveryhelper.autocall.utils.SettingsManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var settingsManager: SettingsManager
    private lateinit var permissionHelper: PermissionHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        settingsManager = SettingsManager(this)
        permissionHelper = PermissionHelper(this)
        
        setupUI()
        loadSettings()
        setupClickListeners()
        checkServiceStatus()
    }
    
    private fun setupUI() {
        // 거리 슬라이더 설정
        binding.seekBarDistance.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val distance = (progress + 5) / 10.0 // 0.5km ~ 5.0km
                binding.tvDistanceValue.text = "${String.format("%.1f", distance)} km"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }
    
    private fun loadSettings() {
        // 저장된 설정값 불러오기
        binding.etSingleCallPrice.setText(settingsManager.getSingleCallPrice().toString())
        binding.etDoubleCallPrice.setText(settingsManager.getDoubleCallPrice().toString())
        binding.etTripleCallPrice.setText(settingsManager.getTripleCallPrice().toString())
        
        val distance = settingsManager.getMaxDistance()
        val progress = ((distance * 10) - 5).toInt()
        binding.seekBarDistance.progress = progress
        binding.tvDistanceValue.text = "${String.format("%.1f", distance)} km"
    }
    
    private fun setupClickListeners() {
        // 서비스 스위치
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (permissionHelper.hasAllPermissions()) {
                    enableService()
                } else {
                    binding.switchService.isChecked = false
                    requestPermissions()
                }
            } else {
                disableService()
            }
        }
        
        // 블랙리스트 관리 버튼
        binding.btnManageBlacklist.setOnClickListener {
            // 임시로 비활성화 - Maps API 설정 후 활성화
            Toast.makeText(this, "지도 기능은 Google Maps API 설정 후 사용 가능합니다", Toast.LENGTH_LONG).show()
            /*
            if (permissionHelper.hasLocationPermission()) {
                startActivity(Intent(this, MapActivity::class.java))
            } else {
                permissionHelper.requestLocationPermission()
            }
            */
        }
        
        // 저장 버튼
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }
    
    private fun enableService() {
        if (isAccessibilityServiceEnabled()) {
            settingsManager.setServiceEnabled(true)
            updateServiceStatus(true)
            Toast.makeText(this, "자동 콜 서비스가 활성화되었습니다", Toast.LENGTH_SHORT).show()
        } else {
            binding.switchService.isChecked = false
            showAccessibilityServiceDialog()
        }
    }
    
    private fun disableService() {
        settingsManager.setServiceEnabled(false)
        updateServiceStatus(false)
        Toast.makeText(this, "자동 콜 서비스가 비활성화되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateServiceStatus(isEnabled: Boolean) {
        if (isEnabled) {
            binding.tvServiceStatus.text = getString(R.string.service_on)
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            binding.tvServiceStatus.text = getString(R.string.service_off)
            binding.tvServiceStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
    }
    
    private fun checkServiceStatus() {
        val isEnabled = settingsManager.isServiceEnabled() && isAccessibilityServiceEnabled()
        binding.switchService.isChecked = isEnabled
        updateServiceStatus(isEnabled)
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val serviceName = "$packageName/${AutoCallService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(serviceName) == true
    }
    
    private fun showAccessibilityServiceDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("접근성 권한 필요")
            .setMessage("자동 콜 기능을 사용하려면 접근성 권한이 필요합니다. 설정에서 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
            .setNegativeButton("취소", null)
            .show()
    }
    
    private fun requestPermissions() {
        if (!permissionHelper.hasLocationPermission()) {
            permissionHelper.requestLocationPermission()
        }
        if (!permissionHelper.hasOverlayPermission()) {
            permissionHelper.requestOverlayPermission()
        }
    }
    
    private fun saveSettings() {
        try {
            val singlePrice = binding.etSingleCallPrice.text.toString().toIntOrNull() ?: 4000
            val doublePrice = binding.etDoubleCallPrice.text.toString().toIntOrNull() ?: 7000
            val triplePrice = binding.etTripleCallPrice.text.toString().toIntOrNull() ?: 9900
            val distance = (binding.seekBarDistance.progress + 5) / 10.0
            
            settingsManager.setSingleCallPrice(singlePrice)
            settingsManager.setDoubleCallPrice(doublePrice)
            settingsManager.setTripleCallPrice(triplePrice)
            settingsManager.setMaxDistance(distance)
            
            Toast.makeText(this, "설정이 저장되었습니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "설정 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkServiceStatus()
    }
}