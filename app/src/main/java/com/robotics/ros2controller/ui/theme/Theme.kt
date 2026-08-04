package com.robotics.ros2controller.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Reference_HeaderBackground = Color(0xFF4F46E5) // Deep Indigo / Violet Accent
val Reference_Text_OnHeader = Color(0xFFFFFFFF)
val Reference_Text_Secondary = Color(0xFF64748B)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEF2FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF3B82F6),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF1F5F9), // Clean soft light gray-blue background
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun ROS2ControllerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}