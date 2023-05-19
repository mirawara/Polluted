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

/*This class is responsible for managing all the operations related to a Tensorflow Lite
classifier (loading the model, converting inputs in the right formats, preprocessing them, running
the model*/
class Classifier(assetManager: AssetManager, modelPath: String, inputSize: Int,
                 labelClass: Class<out Enum<*>>, needPreprocessing: Boolean) {

    private var INTERPRETER: Interpreter
    private val INPUT_SIZE: Int = inputSize
    private val PIXEL_SIZE: Int = 3
    private val IMAGE_MEAN = 0
    private val IMAGE_STD = 255.0f
    private val LABEL_CLASS : Class<out Enum<*>> = labelClass
    private val needPreprocessing: Boolean = needPreprocessing

    init {
        INTERPRETER = Interpreter(loadModelFile(assetManager, modelPath))
        if (!OpenCVLoader.initDebug()) {
            print("error")
        }
    }

    /*Loads the model, saved in tflite format, from the assets folder*/
    private fun loadModelFile(assetManager: AssetManager, modelPath: String): MappedByteBuffer {
        val fileDescriptor = assetManager.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    /*Converts an input image, represented as a bitmap, in the required format for the model*/
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

    /*Performs image preprocessing on the input bitmap
    Extracts H, V channels, dark channel image, grey scale image, and merge them to obtain the
    feature map*/
    private fun preprocessImage(image: Bitmap): Bitmap { // Converti l'immagine in spazio colore HSV
        val hsvImage = Mat()
        Utils.bitmapToMat(image, hsvImage)
        Imgproc.cvtColor(hsvImage, hsvImage, Imgproc.COLOR_BGR2HSV)

        // Extracts channel H, channel V and dark channel
        val hChannel = Mat()
        val vChannel = Mat()
        val darkChannel = Mat()
        Core.extractChannel(hsvImage, hChannel, 0)
        Core.extractChannel(hsvImage, vChannel, 2)
        Imgproc.erode(vChannel, darkChannel, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 15.0)))

        // Extracts gray scale image
        val grayImage = Mat()
        Utils.bitmapToMat(image, grayImage)
        Imgproc.cvtColor(grayImage, grayImage, Imgproc.COLOR_BGR2GRAY)

        // Merges the four channels to obtain the feature map
        val combinedImage = Mat()
        val channels = ArrayList<Mat>()
        channels.add(darkChannel)
        channels.add(hChannel)
        channels.add(vChannel)
        channels.add(grayImage)
        Core.merge(channels, combinedImage)

        // Converts preprocessed image in bitmap format
        val preprocessedBitmap = Bitmap.createBitmap(combinedImage.cols(), combinedImage.rows(), Bitmap.Config.ARGB_8888)

        Utils.matToBitmap(combinedImage, preprocessedBitmap)
        return preprocessedBitmap
    }

    /*Takes an image as input, resizes it, and gives it as input of the loaded classifier,
    possibily before the initial preprocessing
    Returns the classification result as an Integer label*/
    fun recognizeImage(bitmap: Bitmap) : Int {

        var scaledBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false)

        if (needPreprocessing) {
            scaledBitmap = preprocessImage(scaledBitmap)
        }
        val byteBuffer = convertBitmapToByteBuffer(scaledBitmap)

        val result = Array(1) { FloatArray(LABEL_CLASS.enumConstants.size) }

        INTERPRETER.run(byteBuffer, result)

        val argmax = result[0].mapIndexed { index, value -> index to value }.maxByOrNull { it.second }!!.first

        return argmax
    }
}