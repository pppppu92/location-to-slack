package com.example.location_to_slack.network

import com.example.location_to_slack.data.Checkpoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlackNotifierTest {
    @Test
    fun buildMessageText_shouldFormatCheckpointMessageCorrectly() {
        val checkpoint = Checkpoint(
            name = "東京駅",
            latitude = 35.681236,
            longitude = 139.767125
        )
        val expected = "東京駅を通過しました。\nhttps://www.google.com/maps/search/?api=1&query=35.681236,139.767125"
        val actual = SlackNotifier.buildMessageText(checkpoint)
        assertEquals(expected, actual)
    }

    @Test
    fun shouldNotify_insideAndWithin30MinCooldown_returnsFalse() {
        val isInside = true
        val elapsedMillis = 10 * 60 * 1000L // 10分
        assertFalse(SlackNotifier.shouldNotify(isInside, elapsedMillis))
    }

    @Test
    fun shouldNotify_insideAndAfter30MinCooldown_returnsTrue() {
        val isInside = true
        val elapsedMillis = 31 * 60 * 1000L // 31分
        assertTrue(SlackNotifier.shouldNotify(isInside, elapsedMillis))
    }

    @Test
    fun shouldNotify_outsideAndWithin5MinMinRenotification_returnsFalse() {
        val isInside = false
        val elapsedMillis = 1 * 60 * 1000L // 1分
        assertFalse(SlackNotifier.shouldNotify(isInside, elapsedMillis))
    }

    @Test
    fun shouldNotify_outsideAndAfter5MinMinRenotification_returnsTrue() {
        val isInside = false
        val elapsedMillis = 6 * 60 * 1000L // 6分
        assertTrue(SlackNotifier.shouldNotify(isInside, elapsedMillis))
    }
}
