package it.unipi.dii.msss.polluted

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.firebase.geofire.util.GeoUtils
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.imperiumlabs.geofirestore.GeoFirestore
import com.google.firebase.firestore.Query
import org.imperiumlabs.geofirestore.extension.getAtLocation
import org.imperiumlabs.geofirestore.extension.setLocation
import kotlin.math.round


private const val REQUEST_LOCATION_PERMISSION = 1
private const val TAG = "MyActivity"


fun sendPollInfo(context: Context, activity: Activity, tag: String) {
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
        return
    }

    // Get the user's current location
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)


    //FirebaseApp.initializeApp(this) questo va messo nella main activity
    val db = FirebaseFirestore.getInstance()

    val photo = hashMapOf(
        "location" to location?.let { GeoPoint(it.latitude, location.longitude) },
        "timestamp" to FieldValue.serverTimestamp(),
        "pollutionLevel" to tag
    )
    val geoFirestore = GeoFirestore(db.collection("poll_info"))

    db.collection("poll_info").add(photo)
        .addOnSuccessListener { documentReference ->
            if (location != null) {
                geoFirestore.setLocation(
                    documentReference.id,
                    GeoPoint(location.latitude, location.longitude)
                ) { exception ->
                    if (exception == null) {
                        Log.d(TAG, "Location saved on server successfully!")
                    } else {
                        Log.d(TAG, "An error has occurred: $exception")
                    }
                }
            }
        }
        .addOnFailureListener { e -> Log.w(TAG, "Error adding document", e) }
}


fun do_avg(center: GeoPoint, radiusInKm: Double, callback: (Int) -> Unit) {
    val collectionRef = FirebaseFirestore.getInstance().collection("poll-info")
    val geoFirestore = GeoFirestore(collectionRef)
    geoFirestore.getAtLocation(center, radiusInKm) { docs, ex ->
        if (ex != null) {
            Log.e(TAG, "onError: ", ex)
            return@getAtLocation
        } else {
            var totalPollutionLevel = 0
            var count = 0

            if (docs != null) {
                for (doc in docs) {
                    val pollutionLevel = doc.getDouble("pollutionLevel")
                    if (pollutionLevel != null) {
                        totalPollutionLevel += pollutionLevel.toInt()
                        count++
                    }
                }
            }

            val averagePollutionLevel = totalPollutionLevel / count
            callback(round(averagePollutionLevel.toDouble()).toInt())
        }
    }

}
