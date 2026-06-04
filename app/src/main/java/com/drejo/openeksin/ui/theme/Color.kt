package com.drejo.openeksin.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color tokens replicated from the original app's resources (functional design
 * values: plain hex codes). These drive the "same look" requirement.
 */
object EksiPalette {
    val Blue = Color(0xFF0099CC)
    val BlueBackground = Color(0xFF1865A9)
    val DrawerBackground = Color(0xFF2C3240)
    val DrawerSecondaryText = Color(0xFF727886)
    val HeaderRed = Color(0xFFDB4B4B)
    val Fab = Color(0xFFF50057)
    val Mark = Color(0xFFFFFF9E)

    // Light scheme.
    val LightBackground = Color(0xFFFFFFFF)
    val LightEntryListBackground = Color(0xFFEBEBEB)
    val LightMainText = Color(0xFF000000)
    val LightSecondaryText = Color(0xFF3A3A3A)
    val LightReadMore = Color(0xFF606060)
    val TopicEmptyLight = Color(0xFFCCCCCC)

    // Dark scheme.
    val DarkBackground = Color(0xFF1A1A1A)
    val DarkSurface = Color(0xFF232323)
    val DarkMainText = Color(0xFFE6E6E6)
    val DarkSecondaryText = Color(0xFFBFBFBF)
    val TopicEmptyDark = Color(0xFF6F6F6F)

    // Top app bar (toolbar) and tab strip, sampled from the original app.
    val Toolbar = Color(0xFF212121)
    val TabBar = Color(0xFF000000)
    val TabSelected = Color(0xFFFFFFFF)
    val TabUnselected = Color(0xFF9AA0A6)

    // Rank badge in topic lists (slate blue-gray), sampled from the original.
    val RankBadge = Color(0xFF404F64)

    val TabBackground = Color(0xFF222222)
    val TabText = Color(0xFFFFFFFF)
    val White = Color(0xFFFFFFFF)
}
