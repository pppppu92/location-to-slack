package com.example.location_to_slack.geofence

import com.google.android.gms.location.GeofenceStatusCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GeofenceManagerTest {

    @Test
    fun errorMessage_mapsPermissionFailure() {
        assertEquals(
            "位置情報の権限がないため監視できません",
            GeofenceManager.errorMessage(GeofenceStatusCodes.GEOFENCE_INSUFFICIENT_LOCATION_PERMISSION)
        )
    }

    @Test
    fun limitMessage_onlyWhenOverLimit() {
        assertNull(GeofenceManager.limitMessage(100))
        assertEquals("100件を超えた地点は監視されません", GeofenceManager.limitMessage(101))
    }
}
