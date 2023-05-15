package it.unipi.dii.msss.polluted.classifier

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Criteria
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.QuerySnapshot
import it.unipi.dii.msss.polluted.R
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt


private const val REQUEST_LOCATION_PERMISSION = 1
private const val TAG = "ClassificationActivity"
private lateinit var fusedLocationClient: FusedLocationProviderClient

class ClassificationActivity : AppCompatActivity() {

    private val mInputSize = 1080
    private val classifierInputSize = 224
    private val mModelPath = "aq_classifier.tflite"
    //private val asset = baseContext.assets
    //private val classifier = Classifier(asset, mModelPath, classifierInputSize)

    //@RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_classification)

        val asset = applicationContext.assets
        val classifier = Classifier(asset, mModelPath, classifierInputSize, AirQuality::class.java, true)

        val imagePath = intent.getStringExtra("filename")!!
        val rotateImage = getCameraPhotoOrientation(
            this, Uri.parse(imagePath),
            imagePath
        )

        val bitmap = BitmapFactory.decodeStream(applicationContext.contentResolver.openInputStream(Uri.parse(imagePath)))

        val imageview : ImageView  = findViewById(R.id.imageView4)
        imageview.rotation = rotateImage.toFloat()
        imageview.setImageBitmap(scaleImage(bitmap))

        val getAQButton : Button = findViewById(R.id.button3)

        getAQButton.setOnClickListener {

            //classify
            val result = classifier.recognizeImage(bitmap)
            Log.e("Result: ", result.toString())
            start(this, this, result)
            getAQButton.visibility=View.GONE
        }
    }

    private fun scaleImage(bitmap: Bitmap?): Bitmap {
        val originalWidth = bitmap!!.width
        val originalHeight = bitmap.height
        val scaleWidth = mInputSize.toFloat() / originalWidth
        val scaleHeight = mInputSize.toFloat() / originalHeight
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(bitmap, 0, 0, originalWidth, originalHeight, matrix, true)
    }


    @Suppress("DEPRECATION")
    private fun start(context: Context, activity: Activity, tag: Int): Boolean {

        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permission if not already granted
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
            Toast.makeText(this, "Click the button again", Toast.LENGTH_SHORT).show()
            return false
        }


        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Toast.makeText(
                this,
                "Activate GPS first",
                Toast.LENGTH_SHORT).show()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.getCurrentLocation(LocationRequest.QUALITY_HIGH_ACCURACY, object : CancellationToken() {
                override fun onCanceledRequested(p0: OnTokenCanceledListener) = CancellationTokenSource().token

                override fun isCancellationRequested() = false
            })
                .addOnSuccessListener { location: Location? ->
                    if (location == null)
                        Toast.makeText(this, "Cannot get location.", Toast.LENGTH_SHORT).show()
                    else {
                        sendPollInfo(tag, location)
                    }

                }
        } else {
            val locationListener =
                LocationListener { location -> sendPollInfo(tag, location) }
            val criteria = Criteria()
            criteria.accuracy = Criteria.ACCURACY_COARSE
            criteria.powerRequirement = Criteria.POWER_LOW
            criteria.isAltitudeRequired = false
            criteria.isBearingRequired = false
            criteria.isSpeedRequired = false
            criteria.isCostAllowed = true
            criteria.horizontalAccuracy = Criteria.ACCURACY_HIGH
            criteria.verticalAccuracy = Criteria.ACCURACY_HIGH

            val looper: Looper? = null
            locationManager.requestSingleUpdate(criteria, locationListener, looper)
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun sendPollInfo(
        tag: Int,
        location: Location
    ): Boolean {

        var cityName = ""

        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        if (addresses != null) {
            if (addresses.isNotEmpty()) {
                cityName = addresses[0].locality
                //Toast.makeText(this, "City Name: $cityName", Toast.LENGTH_SHORT).show()
            }
        }


        FirebaseApp.initializeApp(this) //questo va messo nella main activity
        val db = FirebaseFirestore.getInstance()


        val hash =
            GeoFireUtils.getGeoHashForLocation(GeoLocation(location.latitude, location.longitude))

        val geoPoint = GeoPoint(location.latitude, location.longitude)
        val photo = hashMapOf(
            "geo_hash" to hash,
            "location" to geoPoint,
            "timestamp" to Timestamp(Date()),
            "pollution_level" to tag
        )

        db.collection("poll_info").add(photo)
            .addOnSuccessListener {

                Log.d(TAG, "Location saved on server successfully!")
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error adding document", e) }

        doAvg(location, 30.0,cityName)

        return true
    }


    @SuppressLint("SetTextI18n")
    private fun doAvg(location: Location, radiusInKm: Double, cityName: String) {
        val db = FirebaseFirestore.getInstance()
        val center = GeoLocation(location.latitude, location.longitude)
        val radiusInM = radiusInKm * 1000.0

// Each item in 'bounds' represents a startAt/endAt pair. We have to issue
// a separate query for each pair. There can be up to 9 pairs of bounds
// depending on overlap, but in most cases there are 4.
        val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM)
        val tasks: MutableList<Task<QuerySnapshot>> = java.util.ArrayList()
        for (b in bounds) {
            val q = db.collection("poll_info")
                .orderBy("geo_hash")
                .startAt(b.startHash)
                .endAt(b.endHash)
            tasks.add(q.get())
        }

// Collect all the query results together into a single list
        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener {
                val matchingDocs: MutableList<DocumentSnapshot> = java.util.ArrayList()
                for (task in tasks) {
                    val snap = task.result
                    for (doc in snap!!.documents) {
                        val c = doc.getGeoPoint("location")
                        val lat = c!!.latitude
                        val lng = c.longitude

                        // We have to filter out a few false positives due to GeoHash
                        // accuracy, but most will match
                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)
                        if (distanceInM <= radiusInM) {
                            matchingDocs.add(doc)
                        }
                    }
                }

                var count = 0
                var acc = 0
                val now = Date()
                val avg: Int
                val oneDayAgo = Date(now.time - (24 * 60 * 60 * 1000))
                for (doc in matchingDocs) {
                    if (doc.getTimestamp("timestamp")!! > Timestamp(oneDayAgo)) {
                        acc = (acc + doc.getDouble("pollution_level")!!).toInt()
                        count += 1
                    }
                }
                if (count != 0) {
                    avg = (acc / count).toDouble().roundToInt()
                    val qualityIcon : ImageView =findViewById(R.id.qualityIcon)
                    val quality : TextView =findViewById(R.id.quality)
                    //text.text = avg.toString()
                    quality.visibility = View.VISIBLE
                    qualityIcon.visibility = View.VISIBLE
                    //quality.text = "Air quality level is $avg"
                    val value = AirQuality from(avg)
                    quality.text = "Air quality is $value"



                    //val legenda : TextView =findViewById(R.id.legenda)
                    //legenda.visibility = View.VISIBLE


                    val location : TextView =findViewById(R.id.location)
                    val locationIcon : ImageView =findViewById(R.id.locationIcon)
                    location.visibility = View.VISIBLE
                    locationIcon.visibility = View.VISIBLE
                    location.text = cityName
                }

                }
            }
    fun getCameraPhotoOrientation(
        context: Context, imageUri: Uri?,
        imagePath: String
    ): Int {
        var rotate = 0
        try {
            context.contentResolver.notifyChange(imageUri!!, null)
            val exif = applicationContext.contentResolver.openInputStream(Uri.parse(imagePath))?.let {
                ExifInterface(
                    it
                )
            }
            val orientation = exif!!.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_270 -> rotate = 270
                ExifInterface.ORIENTATION_ROTATE_180 -> rotate = 180
                ExifInterface.ORIENTATION_ROTATE_90 -> rotate = 90
            }
            Log.i("RotateImage", "Exif orientation: $orientation")
            Log.i("RotateImage", "Rotate value: $rotate")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rotate
    }



}
