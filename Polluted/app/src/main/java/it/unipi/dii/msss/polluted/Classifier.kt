package it.unipi.dii.msss.polluted

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

import java.io.File
import kotlin.math.pow

class Classifier(assetManager: AssetManager, modelPath: String, inputSize: Int) {

    private lateinit var INTERPRETER: Interpreter
    private val INPUT_SIZE: Int = inputSize
    private val PIXEL_SIZE: Int = 3
    private val IMAGE_MEAN = 0
    private val IMAGE_STD = 255.0f

    init {
        INTERPRETER = Interpreter(loadModelFile(assetManager, modelPath))
    }

    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4*INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val `val` = intValues[pixel++]

                byteBuffer.putFloat((((`val`.shr(16)  and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((`val`.shr(8) and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
                byteBuffer.putFloat((((`val` and 0xFF) - IMAGE_MEAN) / IMAGE_STD))
            }
        }
        return byteBuffer
    }

    private fun convertBitmapToByteBuffer1(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(160 * 160 * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(bitmap.width * bitmap.height)

        bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        //var pixel = 0
        for (i in 0 until 160) {
            for (j in 0 until 160) {
                //pixel = bitmap.getPixel(i,j)
                val index = j * bitmap.width / 160 + i * bitmap.height / 160 * bitmap.width
                val value = intValues[index]
                //val value = pixel
                byteBuffer.put(((value shr 16) and 0xFF - 128).toByte())
                byteBuffer.put(((value shr 8) and 0xFF - 128).toByte())
                byteBuffer.put((value and 0xFF - 128).toByte())
            }
        }
        byteBuffer.rewind()

        return byteBuffer

    }

    fun recognizeImage(bitmap: Bitmap) : Int {

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)

        val result = Array(1) { FloatArray(AirQuality.values().size) }

        INTERPRETER.run(byteBuffer, result)

        val argmax = result[0].mapIndexed { index, value -> index to value }.maxByOrNull { it.second }!!.first

        return argmax
    }
}