package com.example.location_to_slack.receiver

import org.junit.Test

class BootReceiverTest {

    @Test
    fun testShouldRestart() {
        val bootAction = "android.intent.action.BOOT_COMPLETED"

        // Should restart when action matches and monitoring is enabled
        assert(BootReceiver.shouldRestart(bootAction, true))

        // Should not restart when action matches but monitoring is disabled
        assert(!BootReceiver.shouldRestart(bootAction, false))

        // Should not restart when action doesn't match
        assert(!BootReceiver.shouldRestart("other.action", true))

        // Should not restart when action is null
        assert(!BootReceiver.shouldRestart(null, true))
    }
}
