package com.catalogoapp.consultoras.ui.screens.devolucion

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

val CoralColor = Color(0xFFE87D6E)

val BeigeBackground = Color(0xFFFDF8F0)

val DarkText = Color(0xFF2C2C2C)

val SurfaceCard = Color(0xFFFFFFFF)

val GreenAptoBg = Color(0xFFE8F5E9)

val GreenAptoText = Color(0xFF2E7D32)

val RedNoAptoBg = Color(0xFFFFEBEE)

val RedNoAptoText = Color(0xFFC62828)

val RedBannerBg = Color(0xFFB71C1C)

object DevolucionTypography {
    val topBarTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        color = DarkText
    )
    val productName = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = DarkText
    )
    val category = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Gray
    )
    val metadata = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = Color.Gray
    )
    val mainButton = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White
    )
    val guideText = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = DarkText
    )
    val guideSubtext = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = Color.Gray
    )
}
