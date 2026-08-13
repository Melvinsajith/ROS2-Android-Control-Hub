package com.robotics.ros2controller.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryUnknown
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.robotics.ros2controller.ui.components.InteractiveMapCanvas
import com.robotics.ros2controller.ui.components.VirtualJoystick

@Composable
fun UnifiedDashboardScreen(
    ipAddress: String,
    isConnected: Boolean,
    onSendCmdVel: (Double, Double) -> Unit,
    onTriggerEStop: () -> Unit,
    batteryPercent: Int? = null,
    robotX: Double = 0.0,
    robotY: Double = 0.0,
    robotYaw: Double = 0.0,
    globalPath: List<Pair<Double, Double>> = emptyList(),
    mapWidth: Int = 0,
    mapHeight: Int = 0,
    mapData: IntArray? = null,
    mapResolution: Double = 0.05,
    mapOriginX: Double = -10.0,
    mapOriginY: Double = -10.0,
    taskStatus: String = "Idle",
    destinationName: String = "",
    cameraTopic: String = "/camera/camera_sensor/image_raw",
    streamPort: String = "8080"
) {
    val streamUrl = "http://$ipAddress:$streamPort/stream?topic=$cameraTopic"
    val colors = MaterialTheme.colorScheme

    // Determine actual battery value (null if disconnected or no ROS topic received yet)
    val actualBattery = if (isConnected) batteryPercent else null
    val displayBatteryText = actualBattery?.let { "$it%" } ?: "--%"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ================= 1. STATUS HEADER =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (isConnected) Color(0xFF10B981) else Color(0xFFF43F5E),
                                shape = RoundedCornerShape(5.dp)
                            )
                    )
                    Text(
                        text = if (isConnected) "DIO Robot Online" else "DIO Robot Offline",
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // DYNAMIC BATTERY DISPLAY WITH FALLBACK
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = displayBatteryText,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(
                        imageVector = if (actualBattery != null) Icons.Default.BatteryChargingFull else Icons.Default.BatteryUnknown,
                        contentDescription = "Battery Status",
                        tint = when {
                            actualBattery == null -> colors.onSurface.copy(alpha = 0.4f)
                            actualBattery > 60 -> Color(0xFF10B981)
                            actualBattery > 20 -> Color(0xFFF59E0B)
                            else -> Color(0xFFF43F5E)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ================= 2. MISSION TASK STATUS BANNER =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    taskStatus.contains("Reached") -> Color(0xFF10B981)
                    taskStatus.contains("En Route") || taskStatus.contains("Received") -> colors.primary
                    taskStatus.contains("Dispatched") -> Color(0xFFF59E0B)
                    taskStatus.contains("STOP") || taskStatus.contains("Aborted") -> Color(0xFFF43F5E)
                    else -> colors.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (destinationName.isNotEmpty()) "Target Station: $destinationName" else "DIO Navigation Controller",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Status: $taskStatus",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }

        // ================= 3. LIVE MAP VIEWPORT =================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Text(
                    text = "Live Map & Nav2 Path | Pose: (${"%.2f".format(robotX)}, ${"%.2f".format(robotY)})",
                    color = colors.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                InteractiveMapCanvas(
                    robotX = robotX,
                    robotY = robotY,
                    robotYaw = robotYaw,
                    globalPath = globalPath,
                    mapWidth = mapWidth,
                    mapHeight = mapHeight,
                    mapData = mapData,
                    mapResolution = mapResolution,
                    mapOriginX = mapOriginX,
                    mapOriginY = mapOriginY,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 22.dp)
                )
            }
        }

        // ================= 4. MIDDLE ROW: CAMERA FEED & LIDAR SCAN =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Camera Feed Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Camera Feed", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        if (isConnected) {
                            AndroidView(
                                factory = { context ->
                                    WebView(context).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        webViewClient = WebViewClient()
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                        val html = "<html><head><style>body{margin:0;padding:0;background-color:black;display:flex;justify-content:center;align-items:center;height:100vh;} img{max-width:100%;max-height:100%;object-fit:contain;}</style></head><body><img src=\"$streamUrl\" /></body></html>"
                                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // LiDAR Scan Radar Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(160.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("LiDAR Scan", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.surfaceVariant, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            drawCircle(colors.primary, radius = size.width / 2, style = Stroke(width = 2f), center = center)
                            drawCircle(colors.primary.copy(alpha = 0.5f), radius = size.width / 4, style = Stroke(width = 1f), center = center)
                            drawLine(colors.primary.copy(alpha = 0.5f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1f)
                            drawLine(colors.primary.copy(alpha = 0.5f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1f)
                        }
                    }
                }
            }
        }

        // ================= 5. BOTTOM ROW: ROBOT STATUS & TELEOP JOYSTICK =================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Status Readings Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(190.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Robot Status", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    StatusRow("Battery", displayBatteryText)
                    StatusRow("Speed", "0.5 m/s")
                    StatusRow("Mode", if (isConnected) "Autonomous" else "Offline")
                    StatusRow("Uptime", "02:35:12")
                }
            }

            // Joystick Teleop Control Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(190.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Quick Teleop", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    VirtualJoystick(
                        size = 120.dp,
                        thumbColor = colors.primary
                    ) { normX, normZ ->
                        if (isConnected) {
                            onSendCmdVel(normX * 0.5, normZ * 1.0)
                        }
                    }
                }
            }
        }

        // ================= 6. MASTER EMERGENCY STOP BANNER =================
        Button(
            onClick = onTriggerEStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "E-Stop",
                    tint = Color.White
                )
                Text(
                    text = "EMERGENCY STOP",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
        Text(text = value, color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}