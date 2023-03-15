package com.example.bitcoinwallet.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val DarkColorPalette = darkColors(
    primary = Red,
    primaryVariant = Red60,
    secondary = Teal200,
)

private val LightColorPalette = lightColors(
    primary = Red,
    primaryVariant = Red60,
    secondary = Teal200,
    background = LightCream400,
    surface = LightCream100

)

@Composable
fun BitcoinWalletTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val systemUiController = rememberSystemUiController()
    DisposableEffect(key1 = true) {
        systemUiController.setStatusBarColor(
            color = colors.background,
            darkIcons = !darkTheme
        )
        onDispose {}
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}