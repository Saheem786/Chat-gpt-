package com.example.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices

class EmergencySosHandler(private val context: Context) {

    companion object {
        private const val TAG = "EmergencySosHandler"
    }

    @SuppressLint("MissingPermission")
    fun triggerEmergencySos(onResult: (String) -> Unit) {
        Log.e(TAG, "EMERGENCY SOS TRIGGERED!")

        val prefs = context.getSharedPreferences("sam_prefs", Context.MODE_PRIVATE)
        val emergencyContact = prefs.getString("emergency_contact_phone", "112") ?: "112"

        val locationClient = LocationServices.getFusedLocationProviderClient(context)
        
        val hasLocationPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasLocationPerm) {
            try {
                locationClient.lastLocation.addOnSuccessListener { location ->
                    val lat = location?.latitude ?: 28.6139 // New Delhi fallback
                    val lon = location?.longitude ?: 77.2090
                    
                    val mapLink = "https://maps.google.com/?q=$lat,$lon"
                    val message = "EMERGENCY ALERT: I need immediate help. My current location: $mapLink"
                    
                    sendEmergencySms(emergencyContact, message)
                    autoDialContact(emergencyContact)
                    
                    onResult("Emergency SOS activated! Dispatching emergency SMS to $emergencyContact with your location and calling now.")
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get location during emergency", e)
                    val message = "EMERGENCY ALERT: I need immediate help. Location unavailable."
                    sendEmergencySms(emergencyContact, message)
                    autoDialContact(emergencyContact)
                    onResult("Emergency SOS activated! Location unavailable. Sending backup SMS to $emergencyContact and calling now.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in location fetch", e)
                val message = "EMERGENCY ALERT: I need immediate help. Location unavailable."
                sendEmergencySms(emergencyContact, message)
                autoDialContact(emergencyContact)
                onResult("Emergency SOS activated! Location unavailable. Sending backup SMS to $emergencyContact and calling now.")
            }
        } else {
            val message = "EMERGENCY ALERT: I need immediate help. Location permission was not granted."
            sendEmergencySms(emergencyContact, message)
            autoDialContact(emergencyContact)
            onResult("Emergency SOS activated! Sending SMS to $emergencyContact and calling now. (Location permissions missing).")
        }
    }

    private fun sendEmergencySms(phoneNumber: String, message: String) {
        val hasSmsPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        if (hasSmsPerm) {
            try {
                val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                Log.i(TAG, "Emergency SMS dispatched to $phoneNumber successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send emergency SMS", e)
            }
        } else {
            Log.e(TAG, "Missing SEND_SMS permission. Cannot dispatch automated alert.")
        }
    }

    private fun autoDialContact(phoneNumber: String) {
        val hasCallPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        try {
            val callIntent = if (hasCallPerm) {
                Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(callIntent)
            Log.i(TAG, "Emergency call dispatched to $phoneNumber successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start emergency dialer / call", e)
        }
    }
}
