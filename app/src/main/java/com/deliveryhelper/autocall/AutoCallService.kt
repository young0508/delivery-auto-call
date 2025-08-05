package com.deliveryhelper.autocall

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.deliveryhelper.autocall.models.CallInfo
import com.deliveryhelper.autocall.utils.BlacklistManager
import com.deliveryhelper.autocall.utils.ScreenCaptureManager
import com.deliveryhelper.autocall.utils.SettingsManager

class AutoCallService : AccessibilityService() {
    
    private lateinit var settingsManager: SettingsManager
    private lateinit var blacklistManager: BlacklistManager
    private lateinit var screenCaptureManager: ScreenCaptureManager
    
    private val handler = Handler(Looper.getMainLooper())
    private var isProcessingCall = false
    
    companion object {
        private const val TAG = "AutoCallService"
        private val BAEMIN_PACKAGES = arrayOf(
            "com.woowahan.baemin",          // 배달의민족
            "com.woowahan.baeminrider",     // 배민라이더
            "com.woowahan.baeminpartners",  // 배민커넥트
            "com.sampleapp"                 // 테스트용
        )
        private const val PROCESSING_DELAY = 1000L // 1초 딜레이
    }
    
    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        blacklistManager = BlacklistManager(this)
        screenCaptureManager = ScreenCaptureManager(this)
        
        Log.d(TAG, "AutoCallService created")
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !settingsManager.isServiceEnabled()) {
            return
        }
        
        // 배민 관련 앱에서만 동작
        if (event.packageName !in BAEMIN_PACKAGES) {
            return
        }
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // 화면이 변경되었을 때 콜 정보 확인
                if (!isProcessingCall) {
                    handler.postDelayed({
                        checkForNewCall()
                    }, PROCESSING_DELAY)
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "AutoCallService interrupted")
    }
    
    private fun checkForNewCall() {
        if (isProcessingCall) return
        
        isProcessingCall = true
        
        try {
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                // 화면에서 콜 정보 추출
                val callInfo = extractCallInfo(rootNode)
                
                if (callInfo != null) {
                    Log.d(TAG, "Call detected: $callInfo")
                    processCall(callInfo)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for new call", e)
        } finally {
            handler.postDelayed({
                isProcessingCall = false
            }, 500)
        }
    }
    
    private fun extractCallInfo(rootNode: AccessibilityNodeInfo): CallInfo? {
        try {
            // 텍스트 노드들에서 정보 추출
            val textNodes = mutableListOf<String>()
            extractAllTexts(rootNode, textNodes)
            
            // OCR을 통한 화면 캡처 분석도 병행
            val ocrTexts = screenCaptureManager.captureAndExtractText()
            textNodes.addAll(ocrTexts)
            
            // 콜 정보 파싱
            return parseCallInfo(textNodes)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting call info", e)
            return null
        }
    }
    
    private fun extractAllTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        // 현재 노드의 텍스트 추가
        node.text?.toString()?.let { text ->
            if (text.isNotBlank()) {
                texts.add(text.trim())
            }
        }
        
        node.contentDescription?.toString()?.let { desc ->
            if (desc.isNotBlank()) {
                texts.add(desc.trim())
            }
        }
        
        // 자식 노드들 재귀적으로 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { childNode ->
                extractAllTexts(childNode, texts)
                childNode.recycle()
            }
        }
    }
    
    private fun parseCallInfo(texts: List<String>): CallInfo? {
        var price: Int? = null
        var distance: Double? = null
        var address: String? = null
        var callCount = 1
        
        for (text in texts) {
            // 가격 추출 (원, 만원 등)
            if (price == null) {
                price = extractPrice(text)
            }
            
            // 거리 추출 (km, m 등)
            if (distance == null) {
                distance = extractDistance(text)
            }
            
            // 주소 추출 (동, 구, 로, 길 등이 포함된 텍스트)
            if (address == null && isAddressLike(text)) {
                address = text
            }
            
            // 콜 개수 추출 (2건, 3건 등)
            val detectedCallCount = extractCallCount(text)
            if (detectedCallCount > 1) {
                callCount = detectedCallCount
            }
        }
        
        return if (price != null && distance != null) {
            CallInfo(
                callCount = callCount,
                totalPrice = price,
                distance = distance,
                address = address ?: ""
            )
        } else {
            null
        }
    }
    
    private fun extractPrice(text: String): Int? {
        try {
            // 원이 포함된 텍스트에서 숫자 추출
            val priceRegex = Regex("([0-9,]+)\\s*원")
            val match = priceRegex.find(text)
            return match?.groupValues?.get(1)?.replace(",", "")?.toInt()
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun extractDistance(text: String): Double? {
        try {
            // km 또는 m가 포함된 텍스트에서 숫자 추출
            val kmRegex = Regex("([0-9.]+)\\s*km")
            val mRegex = Regex("([0-9.]+)\\s*m")
            
            kmRegex.find(text)?.let { match ->
                return match.groupValues[1].toDouble()
            }
            
            mRegex.find(text)?.let { match ->
                return match.groupValues[1].toDouble() / 1000.0 // m를 km로 변환
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun extractCallCount(text: String): Int {
        return when {
            text.contains("3건") || text.contains("3개") -> 3
            text.contains("2건") || text.contains("2개") -> 2
            else -> 1
        }
    }
    
    private fun isAddressLike(text: String): Boolean {
        val addressKeywords = arrayOf("동", "구", "로", "길", "아파트", "빌딩", "시", "군")
        return addressKeywords.any { text.contains(it) } && text.length > 5
    }
    
    private fun processCall(callInfo: CallInfo) {
        // 1. 블랙리스트 확인
        if (blacklistManager.isAddressInBlacklist(callInfo.address)) {
            Log.d(TAG, "Call rejected: Address in blacklist")
            performRejectAction()
            return
        }
        
        // 2. 설정 조건 확인
        if (settingsManager.shouldAcceptCall(callInfo.callCount, callInfo.totalPrice, callInfo.distance)) {
            Log.d(TAG, "Call accepted: Meets criteria")
            performAcceptAction()
        } else {
            Log.d(TAG, "Call rejected: Does not meet criteria")
            performRejectAction()
        }
    }
    
    private fun performAcceptAction() {
        try {
            val rootNode = rootInActiveWindow ?: return
            
            // "수락" 버튼 찾기
            val acceptButton = findButtonByText(rootNode, arrayOf("수락", "확인", "OK"))
            
            if (acceptButton != null) {
                performClickAction(acceptButton)
                Log.d(TAG, "Accept button clicked")
            } else {
                Log.w(TAG, "Accept button not found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing accept action", e)
        }
    }
    
    private fun performRejectAction() {
        try {
            val rootNode = rootInActiveWindow ?: return
            
            // "거절" 버튼 찾기
            val rejectButton = findButtonByText(rootNode, arrayOf("거절", "취소", "패스"))
            
            if (rejectButton != null) {
                performClickAction(rejectButton)
                Log.d(TAG, "Reject button clicked")
            } else {
                Log.w(TAG, "Reject button not found")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error performing reject action", e)
        }
    }
    
    private fun findButtonByText(node: AccessibilityNodeInfo, buttonTexts: Array<String>): AccessibilityNodeInfo? {
        // 현재 노드 확인
        for (buttonText in buttonTexts) {
            if ((node.text?.contains(buttonText) == true || 
                 node.contentDescription?.contains(buttonText) == true) &&
                node.isClickable) {
                return node
            }
        }
        
        // 자식 노드들 재귀 탐색
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { childNode ->
                val result = findButtonByText(childNode, buttonTexts)
                if (result != null) {
                    childNode.recycle()
                    return result
                }
                childNode.recycle()
            }
        }
        
        return null
    }
    
    private fun performClickAction(node: AccessibilityNodeInfo) {
        // AccessibilityNodeInfo의 performAction 사용
        if (node.isClickable) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return
        }
        
        // Gesture API 사용
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()
        
        val path = Path()
        path.moveTo(centerX, centerY)
        
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        
        dispatchGesture(gesture, null, null)
    }
}