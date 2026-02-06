package com.example.callfeedback.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class DeviceMetadataCollector(private val context: Context) {

    companion object {
        private const val TAG = "DeviceMetadataCollector"
    }


    private val connectivityManager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val telephonyManager: TelephonyManager? =
        context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getNetworkGeneration(): String {
        return try {
            val cm = connectivityManager
            val tm = telephonyManager

            if (cm != null) {
                val network = cm.activeNetwork
                if (network != null) {
                    val caps = cm.getNetworkCapabilities(network)
                    if (caps != null) {
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            return "WiFi"
                        }
                        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            return getCellularNetworkType()
                        }
                    }
                }
            }

            return if (tm != null &&
                tm.dataNetworkType != TelephonyManager.NETWORK_TYPE_UNKNOWN
            ) {
                getCellularNetworkType()
            } else {
                "Unknown"
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting network generation", e)
            "Unknown"
        }
    }


    @SuppressLint("MissingPermission")
    private fun getCellularNetworkType(): String {
        return try {
            val dataNetworkType = telephonyManager?.dataNetworkType
            Log.d(TAG, "Data network type: $dataNetworkType")
            when (dataNetworkType) {
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN -> "2G"

                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"

                TelephonyManager.NETWORK_TYPE_LTE -> "4G/LTE"

                TelephonyManager.NETWORK_TYPE_NR -> "5G/NR"

                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cellular network type", e)
            "Unknown"
        }
    }


    @SuppressLint("MissingPermission")
    fun getSignalStrength(): Int? {
        return try {
            Log.d(TAG, "Getting signal strength...")

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Log.w(TAG, "SDK_INT (${Build.VERSION.SDK_INT}) is less than Q (29), cannot get signal strength")
                return null
            }

            if (telephonyManager == null) {
                Log.w(TAG, "TelephonyManager is null")
                return null
            }

            val cellInfo = try {
                telephonyManager.allCellInfo
            } catch (e: Exception) {
                null
            }


            if (cellInfo.isNullOrEmpty()) {
                Log.w(TAG, "No cell info available")
                return null
            }

            for ((index, cell) in cellInfo.withIndex()) {

                val dbm = when (cell) {
                    is CellInfoGsm -> {
                        val strength = cell.cellSignalStrength.dbm
                        strength
                    }
                    is CellInfoWcdma -> {
                        val strength = cell.cellSignalStrength.dbm
                        strength
                    }
                    is CellInfoLte -> {
                        val strength = cell.cellSignalStrength.dbm
                        strength
                    }
                    is CellInfoNr -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val strength = cell.cellSignalStrength.dbm
                            strength
                        } else {
                            null
                        }
                    }
                    else -> {
                        null
                    }
                }

                if (dbm != null && dbm != Int.MAX_VALUE && dbm != 0) {
                    return dbm
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }


    fun getLocation(callback: (latitude: Double?, longitude: Double?) -> Unit) {
        try {

            val hasFinePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasCoarsePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasFinePermission && !hasCoarsePermission) {
                callback(null, null)
                return
            }

            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY,null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        callback(location.latitude, location.longitude)
                    } else {
                        callback(null, null)
                    }
                }
                .addOnFailureListener { exception ->
                    callback(null, null)
                }
        } catch (e: SecurityException) {
            callback(null, null)
        } catch (e: Exception) {
            callback(null, null)
        }
    }

    fun getTimestamp(): Long {
        return System.currentTimeMillis()
    }
}
