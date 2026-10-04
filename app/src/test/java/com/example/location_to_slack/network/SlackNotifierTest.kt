package com.example.location_to_slack.network

import androidx.work.NetworkType
import com.example.location_to_slack.data.Checkpoint
import org.junit.Assert.assertEquals
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
    fun buildRetryRequest_shouldRequireNetworkAndCarryMessageText() {
        val request = SlackNotifier.buildRetryRequest("本文")
        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals("本文", request.workSpec.input.getString(SlackRetryWorker.KEY_TEXT))
        assertEquals(SlackRetryWorker::class.java.name, request.workSpec.workerClassName)
    }
}
