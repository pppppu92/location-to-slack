package com.example.location_to_slack.ui

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionWarningTest {

    @Test
    fun warnsWhenOnlyBackgroundLocationIsMissing() {
        val warning = permissionWarning(
            hasFineLocation = true,
            hasNotification = true,
            hasBackgroundLocation = false
        )
        assertTrue(warning!!.contains("常に許可"))

        assertNull(permissionWarning(true, true, true))
    }
}
