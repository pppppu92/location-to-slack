package com.example.location_to_slack.service

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationForegroundServiceTest {

    @Test
    fun testResolveAction() {
        // System restart (null action) resumes monitoring when it was enabled
        assertEquals(
            LocationForegroundService.ACTION_START,
            LocationForegroundService.resolveAction(null, true)
        )

        // System restart (null action) stops the service when monitoring was disabled
        assertEquals(
            LocationForegroundService.ACTION_STOP,
            LocationForegroundService.resolveAction(null, false)
        )

        // Explicit action is kept as is
        assertEquals(
            LocationForegroundService.ACTION_UPDATE_GEOFENCES,
            LocationForegroundService.resolveAction(LocationForegroundService.ACTION_UPDATE_GEOFENCES, false)
        )
    }
}
