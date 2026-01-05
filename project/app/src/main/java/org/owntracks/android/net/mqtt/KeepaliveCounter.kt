package org.owntracks.android.net.mqtt

import android.location.Location
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.owntracks.android.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.owntracks.android.BuildConfig
import org.owntracks.android.data.repos.LocationRepo
import org.owntracks.android.model.messages.MessageLocation
import org.owntracks.android.services.LocationProcessor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeepaliveCounter @Inject constructor(
    private val locationRepo: LocationRepo,
    private val locationProcessor: Lazy<LocationProcessor>,
    @ApplicationScope private val scope: CoroutineScope
) {
    // Keepalive counter for OSS flavor
    private var keepaliveCounter: Int = 0
    // Last published location for comparison
    private var lastPublishedLocation: Location? = null
    // Fibonacci sequence first 11 terms
    private val fibonacciSequence = intArrayOf(1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89)
    // Current index in fibonacci sequence
    private var fibonacciIndex: Int = 0
    
    fun incrementKeepaliveCounter() {
        scope.launch {
            Timber.i("Incrementing keepalive counter, processing location update logic")
            
            // Only apply the fibonacci logic for OSS flavor
            if (BuildConfig.FLAVOR == "oss") {
                // Get the current published location
                val currentLocation = locationRepo.currentPublishedLocation.value
                
                // Create local copies to avoid smart cast issues
                val localLastPublishedLocation = lastPublishedLocation
                val localKeepaliveCounter = keepaliveCounter
                val localFibonacciIndex = fibonacciIndex
                
                // Check if location has changed
                val locationChanged = if (localLastPublishedLocation == null || currentLocation == null) {
                    // No previous location or no current location, consider it changed
                    true
                } else {
                    // Compare locations (using distance threshold of 1 meter)
                    localLastPublishedLocation.distanceTo(currentLocation) > 1.0
                }
                
                if (locationChanged) {
                    // Location changed, reset counters and index
                    keepaliveCounter = 1
                    fibonacciIndex = 0
                    lastPublishedLocation = currentLocation
                    // Trigger immediate location update
                    locationProcessor.get().publishLocationMessage(MessageLocation.ReportType.PING)
                    Timber.i("Location changed, resetting counters and triggering immediate update")
                } else {
                    // Location not changed, increment counter
                    val newCounter = localKeepaliveCounter + 1
                    keepaliveCounter = newCounter
                    
                    // Get current fibonacci threshold
                    val fibonacciThreshold = fibonacciSequence[localFibonacciIndex]
                    
                    // Check if we need to trigger location update
                    if (newCounter >= fibonacciThreshold) {
                        // Create a copy of the current location with updated time
                        val locationWithUpdatedTime = currentLocation?.let {
                            Location(it).apply {
                                time = System.currentTimeMillis()
                            }
                        } ?: currentLocation
                        
                        // Trigger location update with the location that has updated time
                        locationWithUpdatedTime?.let {
                            locationProcessor.get().publishLocationMessage(MessageLocation.ReportType.PING, it)
                        } ?: run {
                            locationProcessor.get().publishLocationMessage(MessageLocation.ReportType.PING)
                        }
                        Timber.i("Keepalive count reached fibonacci threshold $fibonacciThreshold (count: $newCounter), triggering location update with updated time")
                        
                        // Increment fibonacci index, but don't exceed the sequence length
                        if (localFibonacciIndex < fibonacciSequence.size - 1) {
                            fibonacciIndex = localFibonacciIndex + 1
                        }
                        // Reset counter
                        keepaliveCounter = 0
                    } else {
                        Timber.i("Keepalive count $newCounter (threshold: $fibonacciThreshold), no location update needed")
                    }
                }
            } else {
                // For non-OSS flavors, keep the original behavior
                locationProcessor.get().publishLocationMessage(MessageLocation.ReportType.PING)
                Timber.i("Non-OSS flavor, triggering location update immediately")
            }
        }
    }
}