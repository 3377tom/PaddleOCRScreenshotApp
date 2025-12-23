package com.example.paddleocrscreenshotapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private lateinit var screenshotButton: Button
    private lateinit var resultText: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val SCREENSHOT_REQUEST_CODE = 1
    private lateinit var ocrHelper: OCRHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        screenshotButton = findViewById(R.id.button_screenshot)
        resultText = findViewById(R.id.text_recognition_result)

        // 初始化PaddleOCR
        ocrHelper = OCRHelper.getInstance(this)

        // 设置截屏按钮点击事件
        screenshotButton.setOnClickListener {
            takeScreenshot()
        }

        // 初始化MediaProjectionManager
        mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    /**
     * 开始截屏流程
     */
    private fun takeScreenshot() {
        // 请求截屏权限
        val intent = mediaProjectionManager.createScreenCaptureIntent()
        startActivityForResult(intent, SCREENSHOT_REQUEST_CODE)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREENSHOT_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // 权限获取成功，开始截屏
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            captureScreen()
        } else {
            Toast.makeText(this, "截屏权限获取失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行截屏操作
     */
    @SuppressLint("WrongConstant")
    private fun captureScreen() {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        // 创建ImageReader
        val imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 1)

        val virtualDisplay = mediaProjection?.createVirtualDisplay(
            "screenshot",
            width,
            height,
            displayMetrics.densityDpi,
            0,
            imageReader.surface,
            null,
            handler
        )

        // 等待一帧画面
        handler.postDelayed({ 
            val image = imageReader.acquireLatestImage()
            if (image != null) {
                val bitmap = imageToBitmap(image)
                image.close()
                virtualDisplay?.release()
                processScreenshot(bitmap)
            } else {
                Toast.makeText(this, "截屏失败", Toast.LENGTH_SHORT).show()
            }
        }, 100)
    }

    /**
     * 将Image转换为Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }

    /**
     * 处理截屏图片，进行OCR识别
     */
    private fun processScreenshot(bitmap: Bitmap) {
        resultText.text = getString(R.string.loading)

        // 在后台线程进行OCR识别
        Thread {
            val result = ocrHelper.recognizeText(bitmap)
            // 更新UI
            handler.post {
                resultText.text = getString(R.string.text_recognition_result) + "\n" + result
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaProjection?.stop()
        ocrHelper.release()
    }
}