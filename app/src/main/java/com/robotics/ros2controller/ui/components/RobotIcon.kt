package com.robotics.ros2controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.robotics.ros2controller.ui.theme.ROS2ControllerTheme

@Composable
fun RobotIcon(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isSmiling: Boolean = true
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
    ) {
        val width = size.width
        val height = size.height

        // Monitor Stand (Base)
        val standPath = Path().apply {
            moveTo(width * 0.4f, height * 0.85f)
            lineTo(width * 0.6f, height * 0.85f)
            lineTo(width * 0.725f, height * 0.95f)
            lineTo(width * 0.275f, height * 0.95f)
            close()
        }
        drawPath(path = standPath, color = color)

        // Monitor Neck
        drawRect(
            color = color,
            topLeft = Offset(width * 0.455f, height * 0.75f),
            size = Size(width * 0.09f, height * 0.12f)
        )

        // Monitor Frame
        val monitorWidth = width * 0.85f
        val monitorHeight = height * 0.5f
        val monitorTop = height * 0.28f
        val monitorLeft = (width - monitorWidth) / 2
        val cornerRadius = width * 0.08f

        drawRoundRect(
            color = color,
            topLeft = Offset(monitorLeft, monitorTop),
            size = Size(monitorWidth, monitorHeight),
            cornerRadius = CornerRadius(cornerRadius),
            style = Stroke(width = width * 0.02f)
        )

        // Bottom bar of monitor
        val bottomBarHeight = monitorHeight * 0.2f
        drawPath(
            path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset(monitorLeft, monitorTop + monitorHeight - bottomBarHeight),
                            size = Size(monitorWidth, bottomBarHeight)
                        ),
                        bottomLeft = CornerRadius(cornerRadius),
                        bottomRight = CornerRadius(cornerRadius)
                    )
                )
            },
            color = color
        )

        // Monitor Button (Circle)
        drawCircle(
            color = Color.White,
            radius = bottomBarHeight * 0.25f,
            center = Offset(width / 2, monitorTop + monitorHeight - bottomBarHeight / 2)
        )

        // Robot Body
        val bodyWidth = width * 0.55f
        val bodyHeight = height * 0.28f
        val bodyTop = monitorTop + monitorHeight * 0.42f
        drawPath(
            path = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(
                            offset = Offset((width - bodyWidth) / 2, bodyTop),
                            size = Size(bodyWidth, bodyHeight)
                        ),
                        topLeft = CornerRadius(width * 0.18f),
                        topRight = CornerRadius(width * 0.18f)
                    )
                )
            },
            color = color
        )

        // Robot Neck
        drawRect(
            color = color,
            topLeft = Offset(width * 0.475f, monitorTop + monitorHeight * 0.25f),
            size = Size(width * 0.05f, monitorHeight * 0.12f)
        )

        // Robot Head
        val headWidth = width * 0.45f
        val headHeight = height * 0.25f
        val headTop = height * 0.15f
        drawRoundRect(
            color = color,
            topLeft = Offset((width - headWidth) / 2, headTop),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(width * 0.12f)
        )

        // Robot Face (White area)
        val faceWidth = headWidth * 0.75f
        val faceHeight = headHeight * 0.55f
        drawRoundRect(
            color = Color.White,
            topLeft = Offset((width - faceWidth) / 2, headTop + (headHeight - faceHeight) / 2),
            size = Size(faceWidth, faceHeight),
            cornerRadius = CornerRadius(width * 0.08f)
        )

        // Robot Eyes
        val eyeRadius = faceHeight * 0.15f
        val eyeOffset = faceWidth * 0.25f

        if (isSmiling) {
            val strokeWidth = width * 0.02f
            val eyeSize = Size(eyeRadius * 2, eyeRadius * 1.5f)

            // Left Eye Arc
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(width / 2 - eyeOffset - eyeRadius, headTop + headHeight / 2 - eyeRadius * 0.5f),
                size = eyeSize,
                style = Stroke(width = strokeWidth)
            )

            // Right Eye Arc
            drawArc(
                color = color,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(width / 2 + eyeOffset - eyeRadius, headTop + headHeight / 2 - eyeRadius * 0.5f),
                size = eyeSize,
                style = Stroke(width = strokeWidth)
            )
        } else {
            drawCircle(
                color = color,
                radius = eyeRadius,
                center = Offset(width / 2 - eyeOffset, headTop + headHeight / 2)
            )
            drawCircle(
                color = color,
                radius = eyeRadius,
                center = Offset(width / 2 + eyeOffset, headTop + headHeight / 2)
            )
        }

        // Antenna Neck
        drawRect(
            color = color,
            topLeft = Offset(width * 0.49f, headTop - height * 0.04f),
            size = Size(width * 0.02f, height * 0.05f)
        )

        // Antenna Top (Keyhole shape approx)
        val antennaTopRadius = height * 0.04f
        val antennaCenterY = headTop - height * 0.08f
        drawCircle(
            color = color,
            radius = antennaTopRadius,
            center = Offset(width / 2, antennaCenterY)
        )
        // Keyhole cutout (Circle and Triangle/Rect)
        drawCircle(
            color = Color.White,
            radius = antennaTopRadius * 0.4f,
            center = Offset(width / 2, antennaCenterY - antennaTopRadius * 0.2f)
        )
        drawPath(
            path = Path().apply {
                moveTo(width * 0.485f, antennaCenterY + antennaTopRadius * 0.6f)
                lineTo(width * 0.515f, antennaCenterY + antennaTopRadius * 0.6f)
                lineTo(width * 0.5f, antennaCenterY)
                close()
            },
            color = Color.White
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RobotIconPreview() {
    ROS2ControllerTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            RobotIcon(modifier = Modifier.size(200.dp))
        }
    }
}
