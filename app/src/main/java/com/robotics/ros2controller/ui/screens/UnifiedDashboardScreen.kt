package com.robotics.ros2controller.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
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
    batteryPercent: Int = 50,
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

    // Dynamic Battery Display: use provided percentage when connected, else fallback to 50%
    val displayBattery = if (isConnected) batteryPercent else 50

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Dark tactical background
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ================= 1. STATUS HEADER =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isConnected) Color(0xFF22C55E) else Color(0xFFEF4444),
                            shape = MaterialTheme.shapes.small
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isConnected) "Robot Online" else "Robot Offline",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // DYNAMIC BATTERY DISPLAY
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$displayBattery%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery Status",
                    tint = when {
                        displayBattery > 60 -> Color(0xFF22C55E)
                        displayBattery > 20 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ================= 2. MISSION TASK STATUS BANNER =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = when {
                    taskStatus.contains("Reached") -> Color(0xFF15803D) // Success Green
                    taskStatus.contains("En Route") || taskStatus.contains("Received") -> Color(0xFF0284C7) // Active Blue
                    taskStatus.contains("Dispatched") -> Color(0xFFD97706) // Warning Amber
                    taskStatus.contains("STOP") || taskStatus.contains("Aborted") -> Color(0xFFDC2626) // Alert Red
                    else -> Color(0xFF1E293B) // Default Dark Slate
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (destinationName.isNotEmpty()) "Target: $destinationName" else "AMR Navigation Control",
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
            }
        }

        // ================= 3. LIVE MAP VIEWPORT =================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(
                    text = "Live Map & Nav2 Path | Pose: (${"%.2f".format(robotX)}, ${"%.2f".format(robotY)})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )

                // Interactive Live Map Canvas rendering real-time AMR pose, Occupancy Grid, and trajectory
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
                        .padding(top = 18.dp)
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
                    .height(150.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Camera Feed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black, MaterialTheme.shapes.small)
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
                    .height(150.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("LiDAR Scan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black, MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                            val center = Offset(size.width / 2, size.height / 2)
                            drawCircle(Color(0xFF0284C7), radius = size.width / 2, style = Stroke(width = 2f), center = center)
                            drawCircle(Color(0xFF0284C7), radius = size.width / 4, style = Stroke(width = 1f), center = center)
                            drawLine(Color(0xFF0284C7), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1f)
                            drawLine(Color(0xFF0284C7), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 1f)
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
                    .height(180.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Robot Status", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)

                    StatusRow("Battery", "$displayBattery%")
                    StatusRow("Speed", "0.5 m/s")
                    StatusRow("Mode", if (isConnected) "Autonomous" else "Offline")
                    StatusRow("Uptime", "02:35:12")
                }
            }

            // Joystick Teleop Control Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(180.dp),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Control", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    VirtualJoystick(
                        size = 120.dp,
                        thumbColor = MaterialTheme.colorScheme.primary
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            shape = MaterialTheme.shapes.medium
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}