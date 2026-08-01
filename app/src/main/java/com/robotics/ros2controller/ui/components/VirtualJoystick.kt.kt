package com.robotics.ros2controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun VirtualJoystick(
    modifier: Modifier = Modifier,
    size: Dp = 260.dp, // Scaled up for better visibility
    thumbColor: Color = Color(0xFF233FFD),
    onValueChange: (linearX: Double, angularZ: Double) -> Unit
) {
    var thumbOffset by remember { mutableStateOf(Offset.Zero) }

    // Convert Dp to pixels accurately
    val density = LocalDensity.current
    val sizePx = with(density) { size.toPx() }
    val maxRadius = sizePx / 2f
    val thumbRadius = sizePx / 6f // Scales nicely with total joystick size

    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbOffset = Offset.Zero
                        onValueChange(0.0, 0.0)
                    },
                    onDragCancel = {
                        thumbOffset = Offset.Zero
                        onValueChange(0.0, 0.0)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = thumbOffset + dragAmount
                        val distance = hypot(newOffset.x, newOffset.y)

                        thumbOffset = if (distance <= maxRadius - thumbRadius) {
                            newOffset
                        } else {
                            val angle = atan2(newOffset.y, newOffset.x)
                            val limit = maxRadius - thumbRadius
                            Offset(
                                (limit * cos(angle)),
                                (limit * sin(angle))
                            )
                        }

                        // Normalized values from -1.0 to 1.0
                        val normLinearX = (-thumbOffset.y / (maxRadius - thumbRadius)).toDouble().coerceIn(-1.0, 1.0)
                        val normAngularZ = (-thumbOffset.x / (maxRadius - thumbRadius)).toDouble().coerceIn(-1.0, 1.0)

                        onValueChange(normLinearX, normAngularZ)
                    }
                )
            }
    ) {
        val center = Offset(this.size.width / 2, this.size.height / 2)

        // Outer Bounding Ring
        drawCircle(
            color = Color(0xFFCBD5E1),
            radius = maxRadius - 8f,
            center = center,
            style = Stroke(width = 8f)
        )

        // Inner Crosshair Lines
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(center.x - maxRadius + 16f, center.y),
            end = Offset(center.x + maxRadius - 16f, center.y),
            strokeWidth = 3f
        )
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(center.x, center.y - maxRadius + 16f),
            end = Offset(center.x, center.y + maxRadius - 16f),
            strokeWidth = 3f
        )

        // Control Thumb
        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = center + thumbOffset
        )
    }
}