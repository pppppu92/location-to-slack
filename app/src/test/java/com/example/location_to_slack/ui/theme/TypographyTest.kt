package com.example.location_to_slack.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class TypographyTest {
    @Test
    fun allTextStyles_useBizUdpGothic() {
        val styles = with(Typography) {
            listOf(
                displayLarge, displayMedium, displaySmall,
                headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall,
                bodyLarge, bodyMedium, bodySmall,
                labelLarge, labelMedium, labelSmall
            )
        }
        styles.forEach { assertEquals(BizUdpGothic, it.fontFamily) }
    }
}
