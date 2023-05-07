package it.unipi.dii.msss.polluted

import android.Manifest
import android.annotation.SuppressLint
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
import java.util.*
import kotlin.math.cos
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
        "latitude" to location?.latitude,
        "longitude" to location?.longitude,
        "timestamp" to Date(),
        "pollutionLevel" to tag
    )

    db.collection("poll_info").add(photo)
        .addOnSuccessListener { documentReference ->
            Log.d(
                TAG,
                "Document added with id: " + documentReference.id
            )
        }
        .addOnFailureListener { e -> Log.w(TAG, "Error adding document", e) }
}


@SuppressLint("RestrictedApi")
fun do_avg(location: Location, distanceInMeters: Int) {

// Calcola le coordinate di latitudine e longitudine minime e massime per il range di 30km intorno alla posizione desiderata
    val latMax = location.latitude + Math.toDegrees(distanceInMeters / 6371000.0)
    val latMin = location.latitude - Math.toDegrees(distanceInMeters / 6371000.0)
    val lngMax = location.longitude + Math.toDegrees(distanceInMeters / (6371000.0 * cos(Math.toRadians(location.latitude))))
    val lngMin = location.longitude - Math.toDegrees(distanceInMeters / (6371000.0 * cos(Math.toRadians(location.latitude))))

// Costruisci un oggetto Date per rappresentare il limite di tempo inferiore della ricerca (24 ore fa dalla data e ora attuale)
    val now = Date()
    val oneDayAgo = Date(now.time - (24 * 60 * 60 * 1000))

    val db = FirebaseFirestore.getInstance()
    db.collection("poll_info")
        .whereGreaterThan("timestamp",oneDayAgo)
        .whereGreaterThan("latitude",latMin)
        .whereGreaterThan("longitude",lngMin)
        .whereLessThan("latitude",latMax)
        .whereLessThan("longitude",lngMax)
        .get().addOnSuccessListener { documents ->
            var totalPollutionLevel = 0
            var numDocuments = 0
            for (document in documents) {
                // Recupera il valore del campo "pollutionLevel" dal documento e aggiungilo al totale
                val pollutionLevel = document.getDouble("pollutionLevel")?.toInt() ?: 0
                totalPollutionLevel += pollutionLevel
                numDocuments++
            }
            val avg=totalPollutionLevel/numDocuments
        }
}
