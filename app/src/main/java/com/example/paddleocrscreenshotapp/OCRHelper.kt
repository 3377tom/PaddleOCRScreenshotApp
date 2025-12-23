package com.example.paddleocrscreenshotapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class OCRHelper private constructor(context: Context) {

    private val TAG = "OCRHelper"
    private val context: Context = context.applicationContext
    private var isInitialized = false
    private val modelPath = "models/ocr_v4_rec_arm_opencl_int8.nb"

    companion object {
        @Volatile
        private var instance: OCRHelper? = null

        fun getInstance(context: Context): OCRHelper {
            if (instance == null) {
                synchronized(OCRHelper::class.java) {
                    if (instance == null) {
                        instance = OCRHelper(context)
                    }
                }
            }
            return instance!!
        }
    }

    init {
        initialize()
    }

    /**
     * 初始化OCR帮助类
     */
    private fun initialize() {
        try {
            Log.d(TAG, "开始初始化OCRHelper")

            // 将模型文件从assets复制到内部存储
            val modelFile = copyModelFile(modelPath)
            if (modelFile == null) {
                Log.e(TAG, "模型文件复制失败")
                isInitialized = false
            } else {
                Log.d(TAG, "模型文件复制成功: ${modelFile.absolutePath}")
                isInitialized = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCRHelper初始化失败: ${e.message}")
            Log.e(TAG, "异常堆栈: ${Log.getStackTraceString(e)}")
            isInitialized = false
        }
    }

    /**
     * 将模型文件从assets复制到内部存储
     */
    private fun copyModelFile(assetPath: String): File? {
        val destFile = File(context.filesDir, assetPath)
        if (destFile.exists()) {
            return destFile
        }

        // 确保目录存在
        destFile.parentFile?.mkdirs()

        try {
            val inputStream: InputStream = context.assets.open(assetPath)
            val outputStream = FileOutputStream(destFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            return destFile
        } catch (e: IOException) {
            Log.e(TAG, "复制模型文件失败: ${e.message}")
            return null
        }
    }

    /**
     * 识别图片中的文字（简化版，返回模拟结果）
     * @param bitmap 图片
     * @return 识别结果
     */
    fun recognizeText(bitmap: Bitmap): String {
        try {
            if (!isInitialized) {
                Log.e(TAG, "OCRHelper未初始化成功，无法进行识别")
                return "[ERROR] OCRHelper未初始化成功"
            }

            Log.d(TAG, "开始文字识别，Bitmap尺寸: ${bitmap.width}x${bitmap.height}")
            
            // 简化版：返回模拟识别结果
            // 实际使用时需要集成PaddleOCR预测逻辑
            val result = "[模拟识别结果] 这是一张截图，尺寸为 ${bitmap.width}x${bitmap.height}"
            
            Log.d(TAG, "文字识别完成，结果: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "文字识别失败: ${e.message}")
            Log.e(TAG, "异常堆栈: ${Log.getStackTraceString(e)}")
            return "[ERROR] 文字识别失败: ${e.message}"
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            isInitialized = false
            Log.d(TAG, "OCRHelper资源释放成功")
        } catch (e: Exception) {
            Log.e(TAG, "释放OCRHelper资源失败: ${e.message}")
        }
    }
}