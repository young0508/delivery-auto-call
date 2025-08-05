package com.deliveryhelper.autocall

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.deliveryhelper.autocall.databinding.ActivityMapBinding
import com.deliveryhelper.autocall.models.BlacklistArea
import com.deliveryhelper.autocall.utils.BlacklistManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*

class MapActivity : AppCompatActivity(), OnMapReadyCallback {
    
    private lateinit var binding: ActivityMapBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var blacklistManager: BlacklistManager
    
    private val currentPolygonPoints = mutableListOf<LatLng>()
    private var currentPolygon: Polygon? = null
    private val savedPolygons = mutableListOf<Polygon>()
    private var selectedPolygon: Polygon? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        blacklistManager = BlacklistManager(this)
        
        setupToolbar()
        setupMap()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    
    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    
    private fun setupClickListeners() {
        // 그리기 초기화 버튼
        binding.fabClearDrawing.setOnClickListener {
            clearCurrentDrawing()
        }
        
        // 영역 저장 버튼
        binding.btnSaveArea.setOnClickListener {
            saveCurrentArea()
        }
        
        // 영역 삭제 버튼
        binding.btnDeleteArea.setOnClickListener {
            deleteSelectedArea()
        }
    }
    
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        
        // 서울 중심으로 카메라 설정
        val seoul = LatLng(37.5665, 126.9780)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(seoul, 12f))
        
        // 지도 클릭 리스너 설정
        googleMap.setOnMapClickListener { latLng ->
            addPointToCurrentPolygon(latLng)
        }
        
        // 폴리곤 클릭 리스너 설정 (기존 영역 선택)
        googleMap.setOnPolygonClickListener { polygon ->
            selectPolygon(polygon)
        }
        
        // 저장된 블랙리스트 영역들 로드
        loadSavedAreas()
    }
    
    private fun addPointToCurrentPolygon(latLng: LatLng) {
        currentPolygonPoints.add(latLng)
        
        // 기존 폴리곤 제거
        currentPolygon?.remove()
        
        if (currentPolygonPoints.size >= 3) {
            // 3개 이상의 점이 있으면 폴리곤 그리기
            currentPolygon = googleMap.addPolygon(
                PolygonOptions()
                    .addAll(currentPolygonPoints)
                    .fillColor(Color.argb(50, 255, 0, 0))
                    .strokeColor(Color.RED)
                    .strokeWidth(3f)
            )
            
            binding.btnSaveArea.isEnabled = true
            binding.tvInstructions.text = "영역 그리기 완료! 이름을 입력하고 저장하세요."
        } else {
            // 점들만 표시
            currentPolygonPoints.forEach { point ->
                googleMap.addMarker(
                    MarkerOptions()
                        .position(point)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )
            }
            
            val pointsNeeded = 3 - currentPolygonPoints.size
            binding.tvInstructions.text = "점을 ${pointsNeeded}개 더 찍어주세요"
        }
    }
    
    private fun clearCurrentDrawing() {
        currentPolygonPoints.clear()
        currentPolygon?.remove()
        currentPolygon = null
        selectedPolygon = null
        
        // 마커들도 제거 (임시 점들)
        googleMap.clear()
        
        // 저장된 영역들 다시 로드
        loadSavedAreas()
        
        binding.btnSaveArea.isEnabled = false
        binding.btnDeleteArea.isEnabled = false
        binding.etAreaName.text?.clear()
        binding.tvInstructions.text = "지도를 터치하여 금지구역을 그려주세요"
    }
    
    private fun saveCurrentArea() {
        val areaName = binding.etAreaName.text.toString().trim()
        
        if (areaName.isEmpty()) {
            Toast.makeText(this, "영역 이름을 입력해주세요", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (currentPolygonPoints.size < 3) {
            Toast.makeText(this, "최소 3개의 점이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }
        
        // 블랙리스트 영역 저장
        val blacklistArea = BlacklistArea(
            id = System.currentTimeMillis(),
            name = areaName,
            points = currentPolygonPoints.toList()
        )
        
        blacklistManager.addBlacklistArea(blacklistArea)
        
        // 현재 폴리곤을 저장된 폴리곤으로 변경
        currentPolygon?.remove()
        val savedPolygon = googleMap.addPolygon(
            PolygonOptions()
                .addAll(currentPolygonPoints)
                .fillColor(Color.argb(70, 255, 0, 0))
                .strokeColor(Color.BLUE)
                .strokeWidth(2f)
        )
        
        savedPolygon.tag = blacklistArea.id
        savedPolygons.add(savedPolygon)
        
        // 상태 초기화
        currentPolygonPoints.clear()
        currentPolygon = null
        binding.etAreaName.text?.clear()
        binding.btnSaveArea.isEnabled = false
        binding.tvInstructions.text = "영역이 저장되었습니다!"
        
        Toast.makeText(this, "블랙리스트 영역이 저장되었습니다", Toast.LENGTH_SHORT).show()
    }
    
    private fun selectPolygon(polygon: Polygon) {
        // 기존 선택 해제
        selectedPolygon?.strokeColor = Color.BLUE
        
        // 새 폴리곤 선택
        selectedPolygon = polygon
        polygon.strokeColor = Color.GREEN
        polygon.strokeWidth = 4f
        
        // 영역 정보 표시
        val areaId = polygon.tag as? Long
        val area = blacklistManager.getBlacklistAreas().find { it.id == areaId }
        
        if (area != null) {
            binding.etAreaName.setText(area.name)
            binding.btnDeleteArea.isEnabled = true
            binding.tvInstructions.text = "선택된 영역: ${area.name}"
        }
    }
    
    private fun deleteSelectedArea() {
        selectedPolygon?.let { polygon ->
            val areaId = polygon.tag as? Long
            
            if (areaId != null) {
                blacklistManager.removeBlacklistArea(areaId)
                polygon.remove()
                savedPolygons.remove(polygon)
                
                binding.etAreaName.text?.clear()
                binding.btnDeleteArea.isEnabled = false
                binding.tvInstructions.text = "영역이 삭제되었습니다"
                
                Toast.makeText(this, "블랙리스트 영역이 삭제되었습니다", Toast.LENGTH_SHORT).show()
            }
            
            selectedPolygon = null
        }
    }
    
    private fun loadSavedAreas() {
        savedPolygons.forEach { it.remove() }
        savedPolygons.clear()
        
        blacklistManager.getBlacklistAreas().forEach { area ->
            val polygon = googleMap.addPolygon(
                PolygonOptions()
                    .addAll(area.points)
                    .fillColor(Color.argb(70, 255, 0, 0))
                    .strokeColor(Color.BLUE)
                    .strokeWidth(2f)
            )
            
            polygon.tag = area.id
            savedPolygons.add(polygon)
        }
    }
}