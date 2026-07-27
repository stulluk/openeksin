package com.drejo.openeksin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extra brand tokens that do not map cleanly onto Material's color roles but are
 * needed to reproduce the original look (drawer, rank badge, highlight, etc.).
 */
data class EkColors(
    val isDark: Boolean,
    val rankBadge: Color,
    val rankBadgeText: Color,
    val highlight: Color,
    val mainText: Color,
    val secondaryText: Color,
    val readMore: Color,
    val entryCardBg: Color,
    val entryListBg: Color,
    val divider: Color,
    val drawerBackground: Color,
)

val LocalEkColors = staticCompositionLocalOf {
    EkColors(
        isDark = false,
        rankBadge = EksiPalette.RankBadge,
        rankBadgeText = EksiPalette.White,
        highlight = EksiPalette.Mark,
        mainText = EksiPalette.LightMainText,
        secondaryText = EksiPalette.LightSecondaryText,
        readMore = EksiPalette.LightReadMore,
        entryCardBg = EksiPalette.LightBackground,
        entryListBg = EksiPalette.LightEntryListBackground,
        divider = Color(0xFFDDDDDD),
        drawerBackground = EksiPalette.DrawerBackground,
    )
}

private val LightColors = lightColorScheme(
    primary = EksiPalette.Blue,
    onPrimary = EksiPalette.White,
    secondary = EksiPalette.BlueBackground,
    background = EksiPalette.LightBackground,
    onBackground = EksiPalette.LightMainText,
    surface = EksiPalette.LightBackground,
    onSurface = EksiPalette.LightMainText,
    surfaceVariant = EksiPalette.LightEntryListBackground,
)

private val DarkColors = darkColorScheme(
    primary = EksiPalette.Blue,
    onPrimary = EksiPalette.White,
    secondary = EksiPalette.BlueBackground,
    background = EksiPalette.DarkBackground,
    onBackground = EksiPalette.DarkMainText,
    surface = EksiPalette.DarkSurface,
    onSurface = EksiPalette.DarkMainText,
    surfaceVariant = EksiPalette.DarkSurface,
)

@Composable
fun OpeneksinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val ekColors = EkColors(
        isDark = darkTheme,
        rankBadge = EksiPalette.RankBadge,
        rankBadgeText = EksiPalette.White,
        highlight = EksiPalette.Mark,
        mainText = if (darkTheme) EksiPalette.DarkMainText else EksiPalette.LightMainText,
        secondaryText = if (darkTheme) EksiPalette.DarkSecondaryText else EksiPalette.LightSecondaryText,
        readMore = if (darkTheme) EksiPalette.DarkSecondaryText else EksiPalette.LightReadMore,
        entryCardBg = if (darkTheme) EksiPalette.DarkSurface else EksiPalette.LightBackground,
        entryListBg = if (darkTheme) EksiPalette.DarkBackground else EksiPalette.LightEntryListBackground,
        divider = if (darkTheme) Color(0xFF333333) else Color(0xFFDDDDDD),
        drawerBackground = EksiPalette.DrawerBackground,
    )
    CompositionLocalProvider(LocalEkColors provides ekColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = OpeneksinTypography,
            content = content,
        )
    }
}
