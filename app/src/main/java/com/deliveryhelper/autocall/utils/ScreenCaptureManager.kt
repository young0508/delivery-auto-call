package com.deliveryhelper.autocall.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.nio.ByteBuffer

class ScreenCaptureManager(private val context: Context) {
    
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics()
    
    private val textRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    
    companion object {
        private const val TAG = "ScreenCaptureManager"
    }
    
    init {
        windowManager.defaultDisplay.getMetrics(displayMetrics)
    }
    
    fun captureAndExtractText(): List<String> {
        return try {
            val bitmap = captureScreen()
            if (bitmap != null) {
                extractTextFromBitmap(bitmap)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing and extracting text", e)
            emptyList()
        }
    }
    
    private fun captureScreen(): Bitmap? {
        // 실제 구현에서는 MediaProjection을 사용해야 하지만
        // 권한이 복잡하므로 여기서는 간단한 구현만 제공
        
        // 참고: 실제로는 다음과 같은 과정이 필요:
        // 1. MediaProjectionManager를 통해 권한 요청
        // 2. MediaProjection 생성
        // 3. ImageReader와 VirtualDisplay를 통해 화면 캡처
        
        return null // 실제 구현 필요
    }
    
    private fun extractTextFromBitmap(bitmap: Bitmap): List<String> {
        val texts = mutableListOf<String>()
        
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val lineText = line.text.trim()
                            if (lineText.isNotEmpty()) {
                                texts.add(lineText)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Text recognition failed", e)
                }
                
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from bitmap", e)
        }
        
        return texts
    }
    
    // 화면 캡처 권한 설정 (실제 구현)
    fun setupMediaProjection(mediaProjection: MediaProjection) {
        this.mediaProjection = mediaProjection
        setupImageReader()
    }
    
    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            PixelFormat.RGBA_8888,
            1
        )
        
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            displayMetrics.widthPixels,
            displayMetrics.heightPixels,
            displayMetrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }
    
    fun release() {
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        textRecognizer.close()
    }
    
    // 간단한 텍스트 추출을 위한 대안 방법
    // AccessibilityService의 텍스트 추출에 실패했을 때 사용
    fun extractTextFromAccessibilityNodes(): List<String> {
        // 실제로는 더 정교한 텍스트 추출 로직 구현
        return emptyList()
    }
}