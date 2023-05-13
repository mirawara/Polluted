package it.unipi.dii.msss.polluted

import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import androidx.annotation.RequiresApi
import java.io.File

class ClassificationActivity : AppCompatActivity() {

    lateinit var bitmap: Bitmap
    private val mInputSize = 224
    private val classifierInputSize = 224
    private val mModelPath = "trained.tflite"
    private val mLabelPath = "labels.txt"
    //private val asset = baseContext.assets
    //private val classifier = Classifier(asset, mModelPath, classifierInputSize)

    //@RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classification)

        val asset = applicationContext.assets
        val classifier = Classifier(asset, mModelPath, classifierInputSize)

        val intent = getIntent()

        val clazz = Bitmap::class.java
        //val bitmapParcelable = intent.getParcelableExtra<BitmapParcelable>("bitmap")
        //bitmap = scaleImage(intent.getParcelableExtra("bitmap", clazz))

        val imagePath = intent.getStringExtra("filename")
        //val file = File(imagePath)
        val bitmap = scaleImage(BitmapFactory.decodeStream(applicationContext.getContentResolver().openInputStream(Uri.parse(imagePath))))

        val imageview : ImageView  = findViewById(R.id.imageView4)
        imageview.setImageBitmap(bitmap)

        val getAQButton : Button = findViewById(R.id.button3)

        getAQButton.setOnClickListener {

            //classify
            val result = classifier.recognizeImage(bitmap)
            Log.e("Result: ", result.toString());
            //send
        }
    }

    fun scaleImage(bitmap: Bitmap?): Bitmap {
        val orignalWidth = bitmap!!.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / orignalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, orignalWidth, originalHeight, matrix, true)
    }
}