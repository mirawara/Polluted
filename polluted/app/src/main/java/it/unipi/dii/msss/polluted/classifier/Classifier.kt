package it.unipi.dii.msss.polluted.classifier

import android.content.res.AssetManager
import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc


import org.opencv.core.Mat

class Classifier(assetManager: AssetManager, modelPath: String, inputSize: Int) {

    private lateinit var INTERPRETER: Interpreter
    private val INPUT_SIZE: Int = inputSize
    private val PIXEL_SIZE: Int = 3
    private val IMAGE_MEAN = 0
    private val IMAGE_STD = 255.0f

    init {
        INTERPRETER = Interpreter(loadModelFile(assetManager, modelPath))
        if (!OpenCVLoader.initDebug()) {
            print("error")
        }
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

    private fun preprocessImage(image: Bitmap): Bitmap { // Converti l'immagine in spazio colore HSV
        val hsvImage = Mat()
        Utils.bitmapToMat(image, hsvImage)
        Imgproc.cvtColor(hsvImage, hsvImage, Imgproc.COLOR_BGR2HSV)

        // Estrai il canale H, il canale V e il dark channel
        val hChannel = Mat()
        val vChannel = Mat()
        val darkChannel = Mat()
        Core.extractChannel(hsvImage, hChannel, 0)
        Core.extractChannel(hsvImage, vChannel, 2)
        Imgproc.erode(vChannel, darkChannel, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 15.0)))

        // Scala di grigi dell'immagine originale
        val grayImage = Mat()
        Utils.bitmapToMat(image, grayImage)
        Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_BGR2GRAY)

        // Crea l'immagine combinata sovrapponendo i canali
        val combinedImage = Mat()
        val channels = ArrayList<Mat>()
        channels.add(darkChannel)
        channels.add(hChannel)
        channels.add(vChannel)
        channels.add(grayImage)
        Core.merge(channels, combinedImage)

        // Converti l'immagine di nuovo in formato Bitmap
        val preprocessedBitmap = Bitmap.createBitmap(combinedImage.cols(), combinedImage.rows(), Bitmap.Config.ARGB_8888)

        Utils.matToBitmap(combinedImage, preprocessedBitmap)
        return preprocessedBitmap
    }

    fun recognizeImage(bitmap: Bitmap) : Int {

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)
        val processedBitmap = preprocessImage(scaledBitmap)
        val byteBuffer = convertBitmapToByteBuffer(processedBitmap)

        val result = Array(1) { FloatArray(AirQuality.values().size) }

        INTERPRETER.run(byteBuffer, result)

        val argmax = result[0].mapIndexed { index, value -> index to value }.maxByOrNull { it.second }!!.first

        return argmax
    }
}