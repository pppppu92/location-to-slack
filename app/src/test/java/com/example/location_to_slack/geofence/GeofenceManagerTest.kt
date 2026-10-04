package com.example.location_to_slack.geofence

import com.google.android.gms.location.GeofenceStatusCodes
import org.junit.Assert.assertEquals
import org.junit.Test

class GeofenceManagerTest {

    @Test
    fun errorMessage_mapsPermissionFailure() {
        assertEquals(
            "位置情報の権限がないため監視できません",
            GeofenceManager.errorMessage(GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION)
        )
    }
}
