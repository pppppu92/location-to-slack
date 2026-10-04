package com.example.location_to_slack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.location_to_slack.R

// BIZ UDPGothic (SIL OFL 1.1)。ライセンス本文は assets/licenses/BIZUDPGothic-OFL.txt
val BizUdpGothic = FontFamily(
    Font(R.font.biz_udpgothic_regular, FontWeight.Normal),
    Font(R.font.biz_udpgothic_bold, FontWeight.Bold)
)

private val defaultTypography = Typography()

// Material 3 の全テキストスタイルのフォントを BIZ UDPGothic にする
val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = BizUdpGothic),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = BizUdpGothic),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = BizUdpGothic),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = BizUdpGothic),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = BizUdpGothic),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = BizUdpGothic),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = BizUdpGothic),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = BizUdpGothic),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = BizUdpGothic),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = BizUdpGothic),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = BizUdpGothic),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = BizUdpGothic),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = BizUdpGothic),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = BizUdpGothic),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = BizUdpGothic)
)
