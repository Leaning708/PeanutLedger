package com.peanut.ledger.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.peanut.ledger.data.EntryLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.round

object LocationHelper {
    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun currentLocation(context: Context): EntryLocation? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        ).filter { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }
        val lastKnown = providers
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        val location = lastKnown
            ?: withTimeoutOrNull(6000) { requestSingleUpdate(manager, providers) }
            ?: return null

        val label = withTimeoutOrNull(3500) {
            withContext(Dispatchers.IO) { reverseGeocode(context, location) }
        }
        return EntryLocation(
            latitude = round6(location.latitude),
            longitude = round6(location.longitude),
            label = label?.takeIf { it.isNotBlank() }
        )
    }

    @Suppress("DEPRECATION")
    private suspend fun requestSingleUpdate(
        manager: LocationManager,
        providers: List<String>
    ): Location? {
        if (providers.isEmpty()) return null
        return suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    if (continuation.isActive) {
                        providers.forEach { provider ->
                            runCatching { manager.removeUpdates(this) }
                        }
                        continuation.resume(location)
                    }
                }

                override fun onProviderDisabled(provider: String) = Unit

                override fun onProviderEnabled(provider: String) = Unit

                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            try {
                providers.forEach { provider ->
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }
                continuation.invokeOnCancellation {
                    providers.forEach { provider ->
                        runCatching { manager.removeUpdates(listener) }
                    }
                }
            } catch (_: SecurityException) {
                providers.forEach { provider ->
                    runCatching { manager.removeUpdates(listener) }
                }
                if (continuation.isActive) continuation.resume(null)
            } catch (_: Exception) {
                providers.forEach { provider ->
                    runCatching { manager.removeUpdates(listener) }
                }
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(context: Context, location: Location): String? = runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val address = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()
            ?: return@runCatching null
        address.getAddressLine(0)?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(
                address.locality,
                address.subLocality,
                address.thoroughfare
            ).distinct().joinToString("")
    }.getOrNull()

    private fun round6(value: Double): Double = round(value * 1_000_000.0) / 1_000_000.0
}
