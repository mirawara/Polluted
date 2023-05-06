package it.unipi.dii.msss.polluted

import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import org.junit.Test
import org.junit.Assert.assertEquals
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith
import java.io.File
import java.io.InputStream

class ClassifierTest {

    fun getAsset(context: Context, filename: String): InputStream {
        val assetManager: AssetManager = context.assets
        return assetManager.open(filename)
    }

    @Test
    fun classifierTest() {

        //val assetManager = InstrumentationRegistry.getInstrumentation().targetContext.assets

        val classifier = Classifier(assetManager, "10_1024_best.tflite", 224)

        val imagePath =
            "C:/Users/User/Documents/GitHub/Polluted/Polluted/app/src/main/res/drawable-xxxhdpi/bengr_good_2023_02_19_08_30_1_1.jpg"
        val file = File(imagePath)
        val bitmap1 = BitmapFactory.decodeFile(file.absolutePath)

        println(classifier.recognizeImage(bitmap1))
        assert(true)
    }
}