package it.unipi.dii.msss.polluted

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

private const val REQUEST_LOCATION_PERMISSION = 1

fun sendPhotoData(context: Context, activity: Activity, tag: String) {
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

    // Build the JSON object with the tag and location data
    val json = JSONObject().apply {
        put("tag", tag)
        put("latitude", location?.latitude)
        put("longitude", location?.longitude)
    }

    // Send the JSON object via PUT request to example.com/photo
    val queue = Volley.newRequestQueue(context)
    val url = "http://example.com/photo"
    val jsonObjectRequest = JsonObjectRequest(
        Request.Method.PUT, url, json,
        { response ->
            // Handle successful response
        },
        { error ->
            // Handle error response
        }
    )
    queue.add(jsonObjectRequest)
}
