package com.example.paddleocrscreenshotapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.paddle.lite.*
import com.paddle.lite.nn.Predictor
import com.paddle.lite.nn.PredictorConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class OCRHelper private constructor(context: Context) {

    private val TAG = "OCRHelper"
    private val context: Context = context.applicationContext
    private var predictor: Predictor? = null
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
     * 初始化PaddleOCR
     */
    private fun initialize() {
        try {
            Log.d(TAG, "开始初始化PaddleOCR")

            // 将模型文件从assets复制到内部存储
            val modelFile = copyModelFile(modelPath)
            if (modelFile == null) {
                Log.e(TAG, "模型文件复制失败")
                return
            }

            // 配置PaddleLite预测器
            val config = PredictorConfig()
            config.modelFromFile(modelFile.absolutePath)
            
            // 设置线程数
            config.setThreads(4)
            
            // 设置运行模式为GPU
            config.setPowerMode(PowerMode.LITE_POWER_HIGH)
            
            // 设置精度模式
            config.setPrecision(PrecisionType.LITE_PRECISION_INT8)

            // 创建预测器
            predictor = Predictor(config)
            isInitialized = true
            Log.d(TAG, "PaddleOCR初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "PaddleOCR初始化失败: ${e.message}")
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
     * 识别图片中的文字
     * @param bitmap 图片
     * @return 识别结果
     */
    fun recognizeText(bitmap: Bitmap): String {
        try {
            if (!isInitialized || predictor == null) {
                Log.e(TAG, "PaddleOCR未初始化成功，无法进行识别")
                return "[ERROR] PaddleOCR未初始化成功"
            }

            Log.d(TAG, "开始PaddleOCR识别，Bitmap尺寸: ${bitmap.width}x${bitmap.height}")

            // 预处理图片
            val inputTensor = predictor!!.getInput(0)
            preprocessImage(bitmap, inputTensor)

            // 执行预测
            predictor!!.run()

            // 获取输出
            val outputTensor = predictor!!.getOutput(0)
            val result = postprocessOutput(outputTensor)

            Log.d(TAG, "PaddleOCR识别完成，结果: $result")
            return result
        } catch (e: Exception) {
            Log.e(TAG, "PaddleOCR识别失败: ${e.message}")
            Log.e(TAG, "异常堆栈: ${Log.getStackTraceString(e)}")
            return "[ERROR] PaddleOCR识别失败: ${e.message}"
        }
    }

    /**
     * 预处理图片
     */
    private fun preprocessImage(bitmap: Bitmap, inputTensor: Tensor) {
        // 这里需要根据实际模型的输入要求进行预处理
        // 包括缩放、归一化、通道转换等
        // 示例代码，需要根据实际模型调整
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 320, 32, true)
        val inputData = FloatArray(320 * 32 * 3)
        
        // 示例：将Bitmap转换为模型输入格式
        // 这里需要根据实际模型的输入要求调整
        for (y in 0 until 32) {
            for (x in 0 until 320) {
                val pixel = resizedBitmap.getPixel(x, y)
                val r = ((pixel shr 16 and 0xFF) / 255.0f - 0.5f) * 2.0f
                val g = ((pixel shr 8 and 0xFF) / 255.0f - 0.5f) * 2.0f
                val b = ((pixel and 0xFF) / 255.0f - 0.5f) * 2.0f
                
                val index = y * 320 * 3 + x * 3
                inputData[index] = r
                inputData[index + 1] = g
                inputData[index + 2] = b
            }
        }
        
        // 将数据设置到输入张量
        inputTensor.setData(inputData)
    }

    /**
     * 后处理输出结果
     */
    private fun postprocessOutput(outputTensor: Tensor): String {
        // 这里需要根据实际模型的输出格式进行后处理
        // 示例代码，需要根据实际模型调整
        val outputData = outputTensor.getData<FloatArray>()
        val outputShape = outputTensor.shape
        
        // 示例：简单处理输出结果
        // 实际需要根据模型的输出格式解析文字
        val result = StringBuilder()
        for (i in 0 until outputShape[0]) {
            val maxIndex = outputData.sliceArray(i * outputShape[1] until (i + 1) * outputShape[1]).indexOf(outputData.maxOrNull()!!)
            // 将索引转换为字符
            // 这里需要根据模型的字典映射
            result.append(maxIndex.toChar())
        }
        
        return result.toString()
    }

    /**
     * 释放资源
     */
    fun release() {
        try {
            if (predictor != null) {
                predictor?.close()
                predictor = null
                isInitialized = false
                Log.d(TAG, "PaddleOCR资源释放成功")
            }
        } catch (e: Exception) {
            Log.e(TAG, "释放PaddleOCR资源失败: ${e.message}")
        }
    }
}