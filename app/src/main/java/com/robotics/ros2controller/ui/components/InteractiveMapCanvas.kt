package com.robotics.ros2controller.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

@Composable
fun InteractiveMapCanvas(
    robotX: Double,
    robotY: Double,
    robotYaw: Double,
    globalPath: List<Pair<Double, Double>>,
    mapWidth: Int,
    mapHeight: Int,
    mapData: IntArray?,
    mapResolution: Double = 0.05,
    mapOriginX: Double = -10.0,
    mapOriginY: Double = -10.0,
    modifier: Modifier = Modifier
) {
    // Generate Bitmap from ROS 2 Occupancy Grid data
    val mapBitmap = remember(mapData, mapWidth, mapHeight) {
        if (mapData != null && mapWidth > 0 && mapHeight > 0 && mapData.size == mapWidth * mapHeight) {
            val pixels = IntArray(mapWidth * mapHeight)
            for (y in 0 until mapHeight) {
                for (x in 0 until mapWidth) {
                    // ROS OccupancyGrid y is bottom-to-top; Bitmap y is top-to-bottom
                    val rosIndex = (mapHeight - 1 - y) * mapWidth + x
                    val valOccupancy = mapData[rosIndex]

                    pixels[y * mapWidth + x] = when (valOccupancy) {
                        -1 -> AndroidColor.parseColor("#0F172A") // Unknown background
                        in 0..20 -> AndroidColor.parseColor("#334155") // Free drivable space
                        in 21..100 -> AndroidColor.parseColor("#94A3B8") // Walls / Obstacles
                        else -> AndroidColor.parseColor("#0F172A")
                    }
                }
            }
            Bitmap.createBitmap(pixels, mapWidth, mapHeight, Bitmap.Config.ARGB_8888)
        } else null
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        // Transform World Coordinates (meters) -> Screen Canvas Pixels (Centered)
        fun worldToPixel(wx: Double, wy: Double): Offset {
            val mapPxX = ((wx - mapOriginX) / mapResolution).toFloat()
            val mapPxY = (((mapHeight * mapResolution) - (wy - mapOriginY)) / mapResolution).toFloat()

            // Calculate auto-center scaling
            val scaleX = if (mapWidth > 0) canvasW / mapWidth else 1f
            val scaleY = if (mapHeight > 0) canvasH / mapHeight else 1f
            val scale = minOf(scaleX, scaleY) * 1.2f

            val offsetX = (canvasW - (mapWidth * scale)) / 2f
            val offsetY = (canvasH - (mapHeight * scale)) / 2f

            return Offset(
                x = (mapPxX * scale + offsetX).coerceIn(10f, canvasW - 10f),
                y = (mapPxY * scale + offsetY).coerceIn(10f, canvasH - 10f)
            )
        }

        // 1. Render Map Occupancy Image Background
        mapBitmap?.let { bmp ->
            drawImage(
                image = bmp.asImageBitmap(),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasW.toInt(), canvasH.toInt())
            )
        } ?: run {
            // Fallback grid lines when map data is initializing
            for (x in 0..canvasW.toInt() step 40) {
                drawLine(Color(0xFF334155), Offset(x.toFloat(), 0f), Offset(x.toFloat(), canvasH), strokeWidth = 1f)
            }
            for (y in 0..canvasH.toInt() step 40) {
                drawLine(Color(0xFF334155), Offset(0f, y.toFloat()), Offset(canvasW, y.toFloat()), strokeWidth = 1f)
            }
        }

        // 2. Render Nav2 Active Global Planned Path
        if (globalPath.size > 1) {
            val path = Path().apply {
                val start = worldToPixel(globalPath[0].first, globalPath[0].second)
                moveTo(start.x, start.y)
                for (i in 1 until globalPath.size) {
                    val pt = worldToPixel(globalPath[i].first, globalPath[i].second)
                    lineTo(pt.x, pt.y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFF38BDF8),
                style = Stroke(width = 5f)
            )

            // Destination Goal Pin
            val goalPt = worldToPixel(globalPath.last().first, globalPath.last().second)
            drawCircle(Color(0xFF22C55E), radius = 12f, center = goalPt)
            drawCircle(Color.White, radius = 5f, center = goalPt)
        }

        // 3. Render Moving Robot Entity Circle & Direction Arrow
        val robotCenter = worldToPixel(robotX, robotY)
        val yawDeg = Math.toDegrees(robotYaw).toFloat()

        // Outer pulse halo
        drawCircle(Color(0xFF3B82F6).copy(alpha = 0.35f), radius = 24f, center = robotCenter)
        // Main Robot Circle
        drawCircle(Color(0xFF3B82F6), radius = 14f, center = robotCenter)
        drawCircle(Color.White, radius = 6f, center = robotCenter)

        // Orientation Arrow Pointer
        rotate(degrees = -yawDeg, pivot = robotCenter) {
            drawLine(
                color = Color(0xFFFACC15),
                start = robotCenter,
                end = Offset(robotCenter.x + 22f, robotCenter.y),
                strokeWidth = 4f
            )
        }
    }
}