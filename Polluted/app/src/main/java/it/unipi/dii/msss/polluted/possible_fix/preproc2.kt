package it.unipi.dii.msss.polluted.possible_fix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect

private fun preprocessImage(image: Bitmap): Bitmap {
    // Converti l'immagine in scala di grigi
    val grayImage = convertToGrayscale(image)

    // Estrai il canale H, il canale V e il dark channel
    val hsvImage = convertToHSV(image)
    val hChannel = extractChannel(hsvImage, 0)
    val vChannel = extractChannel(hsvImage, 2)
    val darkChannel = erode(vChannel)

    // Crea l'immagine combinata sovrapponendo i canali
    val combinedImage = mergeChannels(darkChannel, hChannel, vChannel, grayImage)

    return combinedImage
}

private fun convertToGrayscale(image: Bitmap): Bitmap {
    val grayImage = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(grayImage)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter
    canvas.drawBitmap(image, 0f, 0f, paint)
    return grayImage
}

private fun convertToHSV(image: Bitmap): Bitmap {
    val hsvImage = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(hsvImage)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setRotate(0, 0f)
    colorMatrix.setRotate(1, 0f)
    colorMatrix.setRotate(2, 180f)
    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter
    canvas.drawBitmap(image, 0f, 0f, paint)
    return hsvImage
}

private fun extractChannel(image: Bitmap, channelIndex: Int): Bitmap {
    val channelImage = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(channelImage)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    val channelArray = FloatArray(20)
    channelArray[channelIndex] = 1f
    colorMatrix.set(channelArray)
    val filter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = filter
    canvas.drawBitmap(image, 0f, 0f, paint)
    return channelImage
}

private fun erode(image: Bitmap): Bitmap {
    val erodedImage = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.RGB_565)
    val canvas = Canvas(erodedImage)
    val paint = Paint()
    val erodeMatrix = ColorMatrix()
    erodeMatrix.setSaturation(0f)
    val erodeFilter = ColorMatrixColorFilter(erodeMatrix)
    paint.colorFilter = erodeFilter
    canvas.drawBitmap(image, 0f, 0f, paint)
    return erodedImage
}

private fun mergeChannels(darkChannel: Bitmap, hChannel: Bitmap, vChannel: Bitmap, grayImage: Bitmap): Bitmap {
    val combinedImage = Bitmap.createBitmap(darkChannel.width, darkChannel.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(combinedImage)
    val paint = Paint()

    val channels = arrayOf(darkChannel, hChannel, vChannel, grayImage)
    val rect = Rect(0, 0, darkChannel.width, darkChannel.height)
    for (i in channels.indices) {
        canvas.drawBitmap(channels[i], null, rect, paint)

    }

    return combinedImage}