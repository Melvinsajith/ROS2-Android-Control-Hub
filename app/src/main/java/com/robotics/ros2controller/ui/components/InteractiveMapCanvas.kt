package com.robotics.ros2controller.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sin

@Composable
fun InteractiveMapCanvas(
    robotX: Double,
    robotY: Double,
    robotYaw: Double,
    globalPath: List<Pair<Double, Double>>,
    mapWidth: Int,
    mapHeight: Int,
    mapData: IntArray?,
    mapResolution: Double,
    mapOriginX: Double,
    mapOriginY: Double,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1.0f) }
    var offsetX by remember { mutableFloatStateOf(0.0f) }
    var offsetY by remember { mutableFloatStateOf(0.0f) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 4.0f)
                    offsetX += pan.x
                    offsetY += pan.y
                }
            }
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val center = Offset(canvasWidth / 2f + offsetX, canvasHeight / 2f + offsetY)

        // Grid Scale Mapping (Meters to Canvas Pixels)
        val pixelsPerMeter = 20f * scale

        // --- 1. DRAW BACKGROUND GRID LINES ---
        val gridSpacing = 1.0f * pixelsPerMeter
        var currentX = (center.x % gridSpacing)
        while (currentX < canvasWidth) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(currentX, 0f),
                end = Offset(currentX, canvasHeight),
                strokeWidth = 1f
            )
            currentX += gridSpacing
        }
        var currentY = (center.y % gridSpacing)
        while (currentY < canvasHeight) {
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(0f, currentY),
                end = Offset(canvasWidth, currentY),
                strokeWidth = 1f
            )
            currentY += gridSpacing
        }

        // --- 2. DRAW OCCUPANCY GRID MAP DATA (If available) ---
        if (mapData != null && mapWidth > 0 && mapHeight > 0) {
            val cellPixelSize = (mapResolution * pixelsPerMeter).toFloat()
            for (cy in 0 until mapHeight) {
                for (cx in 0 until mapWidth) {
                    val index = cy * mapWidth + cx
                    val valCell = mapData[index]
                    if (valCell > 50) { // Occupied / Obstacle
                        val wx = mapOriginX + (cx * mapResolution)
                        val wy = mapOriginY + (cy * mapResolution)

                        val screenX = center.x + (wx * pixelsPerMeter).toFloat()
                        val screenY = center.y - (wy * pixelsPerMeter).toFloat()

                        drawRect(
                            color = Color(0xFF475569),
                            topLeft = Offset(screenX, screenY),
                            size = Size(cellPixelSize, cellPixelSize)
                        )
                    }
                }
            }
        }

        // --- 3. DRAW GLOBAL NAV2 PATH ---
        if (globalPath.isNotEmpty()) {
            val pathObject = Path()
            globalPath.forEachIndexed { idx, pt ->
                val px = center.x + (pt.first * pixelsPerMeter).toFloat()
                val py = center.y - (pt.second * pixelsPerMeter).toFloat()
                if (idx == 0) {
                    pathObject.moveTo(px, py)
                } else {
                    pathObject.lineTo(px, py)
                }
            }
            drawPath(
                path = pathObject,
                color = Color(0xFF38BDF8),
                style = Stroke(
                    width = 3f * scale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )
        }

        // --- 4. DRAW VECTOR ROBOT ICON AT (robotX, robotY) ---
        val robotScreenX = center.x + (robotX * pixelsPerMeter).toFloat()
        val robotScreenY = center.y - (robotY * pixelsPerMeter).toFloat()
        val robotCenter = Offset(robotScreenX, robotScreenY)

        // Yaw angle conversion to degrees for Canvas rotation
        val yawDegrees = Math.toDegrees(-robotYaw).toFloat()

        rotate(degrees = yawDegrees, pivot = robotCenter) {
            val robotLength = 28f * scale
            val robotWidth = 22f * scale

            // A. Left & Right Tracks / Wheels
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(robotCenter.x - robotLength / 2f, robotCenter.y - robotWidth / 2f - 3f * scale),
                size = Size(robotLength, 5f * scale),
                cornerRadius = CornerRadius(2f, 2f)
            )
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(robotCenter.x - robotLength / 2f, robotCenter.y + robotWidth / 2f - 2f * scale),
                size = Size(robotLength, 5f * scale),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // B. Main Robot Chassis Frame
            drawRoundRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(robotCenter.x - robotLength / 2f, robotCenter.y - robotWidth / 2f),
                size = Size(robotLength, robotWidth),
                cornerRadius = CornerRadius(6f * scale, 6f * scale)
            )

            // C. Front Headlights / Direction Marker
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 3f * scale,
                center = Offset(robotCenter.x + robotLength / 2f - 4f * scale, robotCenter.y - robotWidth / 4f)
            )
            drawCircle(
                color = Color(0xFF38BDF8),
                radius = 3f * scale,
                center = Offset(robotCenter.x + robotLength / 2f - 4f * scale, robotCenter.y + robotWidth / 4f)
            )

            // D. Center LIDAR Scanner Dome
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 6f * scale,
                center = robotCenter
            )
            drawCircle(
                color = Color(0xFFF43F5E), // Laser / LED Indicator
                radius = 2.5f * scale,
                center = robotCenter
            )
        }
    }
}