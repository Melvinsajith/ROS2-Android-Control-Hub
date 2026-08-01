package com.robotics.ros2controller.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Reference image color palette
val Reference_HeaderBackground = Color(0xFF1E2F9F) // Deep Blue header
val Reference_MainBackground = Color(0xFFF0F2FB) // Soft Light Gray/Blue background
val Reference_CardBackground = Color(0xFFFFFFFF) // White cards
val Reference_ActionPrimary = Color(0xFF233FFD) // Bright Blue for active buttons
val Reference_Text_OnHeader = Color(0xFFFFFFFF)
val Reference_Text_Primary = Color(0xFF111111)
val Reference_Text_Secondary = Color(0xFF888888)

// Custom shapes derived from the image
val Reference_Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp), // For main header/card
    extraLarge = RoundedCornerShape(32.dp) // The top deep-blue header shape
)

private val LightColorScheme = lightColorScheme(
    primary = Reference_ActionPrimary,
    background = Reference_MainBackground,
    surface = Reference_CardBackground,
    onPrimary = Color.White,
    onBackground = Reference_Text_Primary,
    onSurface = Reference_Text_Primary,
    secondaryContainer = Color(0xFFE0E7FF), // for badge backgrounds
    onSecondaryContainer = Reference_ActionPrimary // for badge text
)

@Composable
fun ROS2ControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // For this design, we ignore system dark theme and use the light/neumorphic reference style
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = Reference_Shapes,
        content = content
    )
}