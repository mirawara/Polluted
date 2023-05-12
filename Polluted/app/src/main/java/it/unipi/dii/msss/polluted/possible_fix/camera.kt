import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import it.unipi.dii.msss.polluted.CameraActivity
import it.unipi.dii.msss.polluted.ClassificationActivity
import java.text.SimpleDateFormat
import java.util.Locale


//per mandare direttamente il bitmap alla classification activity
private fun takePhoto() {
    // Get a stable reference of the modifiable image capture use case
    val imageCapture = imageCapture ?: return

    // Create time stamped name and MediaStore entry.
    val name = SimpleDateFormat(CameraActivity.FILENAME_FORMAT, Locale.US)
        .format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }

    // Create output options object which contains file + metadata
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues)
        .build()

    // Set up image capture listener, which is triggered after photo has
    // been taken
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(this),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e(CameraActivity.TAG, "Photo capture failed: ${exc.message}", exc)
            }

            override fun
                    onImageSaved(output: ImageCapture.OutputFileResults){
                val msg = "Photo capture succeeded: ${output.savedUri}"
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                Log.d(CameraActivity.TAG, msg)
                val i = Intent(this@CameraActivity, ClassificationActivity::class.java)
                Log.e("savedUri: ", output.savedUri.toString())
                i.putExtra("filename", output.savedUri.toString())
                startActivity(i)
            }
        }
    )
}