package com.robotics.ros2controller.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

@Composable
fun AdvancedTeleopScreen(
    ipAddress: String = "10.0.2.2",
    isConnected: Boolean,
    onSendCmdVel: (Double, Double) -> Unit,
    onTriggerEStop: () -> Unit,
    cameraTopic: String = "/camera/camera_sensor/image_raw",
    streamPort: String = "8080"
) {
    var isEmergencyLocked by remember { mutableStateOf(false) }
    var isSafetyUnlocked by remember { mutableStateOf(false) }
    var maxLinearSpeed by remember { mutableFloatStateOf(0.5f) }
    var maxAngularSpeed by remember { mutableFloatStateOf(1.0f) }

    var currentLinearVel by remember { mutableDoubleStateOf(0.0) }
    var currentAngularVel by remember { mutableDoubleStateOf(0.0) }

    val streamUrl = "http://$ipAddress:$streamPort/stream?topic=$cameraTopic"

    // Deep Royal Navy/Cyan Gradient background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF031330),
            Color(0xFF07214A),
            Color(0xFF020D24)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // ================= 1. HEADER =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color(0xFF132B52).copy(alpha = 0.8f)
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "Live Video",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = Color(0xFF132B52).copy(alpha = 0.8f)
            ) {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Options",
                        tint = Color.White
                    )
                }
            }
        }

        // ================= 2. LIVE CAMERA FEED WITH GLASS OVERLAYS =================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(340.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF091B3A))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (isConnected) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                webViewClient = WebViewClient()
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                setBackgroundColor(android.graphics.Color.BLACK)
                                val html = "<html><head><style>body{margin:0;padding:0;background-color:black;display:flex;justify-content:center;align-items:center;height:100vh;} img{width:100%;height:100%;object-fit:cover;}</style></head><body><img src=\"$streamUrl\" /></body></html>"
                                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A1832)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Camera Disconnected",
                            color = Color(0xFF60A5FA),
                            fontSize = 14.sp
                        )
                    }
                }

                // Overlay Status Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Battery Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0B1930).copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("72%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Speed Trim Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0B1930).copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("${"%.1f".format(maxLinearSpeed)}m/s", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Live Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF0B1930).copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (isConnected) Color(0xFFEF4444) else Color.Gray, CircleShape)
                            )
                            Text("Live", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ================= 3. CONTROLLER DECK =================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Spacer placeholder to keep D-Pad centered on screen
            Box(modifier = Modifier.size(50.dp))

            // --- CENTER CIRCULAR D-PAD CONTROLLER ---
            CustomCircularDpad(
                isEmergencyLocked = isEmergencyLocked,
                isSafetyUnlocked = isSafetyUnlocked,
                onDrive = { lin, ang ->
                    if (isConnected && isSafetyUnlocked && !isEmergencyLocked) {
                        val finalLin = lin * maxLinearSpeed
                        val finalAng = ang * maxAngularSpeed
                        currentLinearVel = finalLin.toDouble()
                        currentAngularVel = finalAng.toDouble()
                        onSendCmdVel(finalLin.toDouble(), finalAng.toDouble())
                    }
                }
            )

            // --- RIGHT FUNCTION BUTTONS (SAFETY LOCK & E-STOP ONLY) ---
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularActionButton(
                    icon = if (isSafetyUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                    tint = if (isSafetyUnlocked) Color(0xFF10B981) else Color(0xFFF43F5E),
                    onClick = {
                        if (!isEmergencyLocked) {
                            isSafetyUnlocked = !isSafetyUnlocked
                        }
                    }
                )
                CircularActionButton(
                    icon = Icons.Default.Warning,
                    tint = Color(0xFFF43F5E),
                    onClick = {
                        isEmergencyLocked = true
                        isSafetyUnlocked = false
                        currentLinearVel = 0.0
                        currentAngularVel = 0.0
                        onTriggerEStop()
                    }
                )
            }
        }

        // Reset Lock Button Banner
        AnimatedVisibility(visible = isEmergencyLocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x33F43F5E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🚨 Emergency Stop Latched", color = Color(0xFFFB7185), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { isEmergencyLocked = false }) {
                    Text("RESET LOCK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ================= CIRCULAR D-PAD JOYSTICK COMPONENT =================
@Composable
fun CustomCircularDpad(
    isEmergencyLocked: Boolean,
    isSafetyUnlocked: Boolean,
    onDrive: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val radius = 90.dp
    val maxPx = 110f

    val isDriveEnabled = isSafetyUnlocked && !isEmergencyLocked

    Box(
        modifier = Modifier
            .size(radius * 2)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF132B52), Color(0xFF091A38))
                ),
                shape = CircleShape
            )
            .border(1.dp, Color(0xFF204278), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // D-pad Direction Arrows
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Forward",
            tint = Color(0xFF93C5FD).copy(alpha = if (isDriveEnabled) 0.8f else 0.3f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(24.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Backward",
            tint = Color(0xFF93C5FD).copy(alpha = if (isDriveEnabled) 0.8f else 0.3f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(24.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Turn Left",
            tint = Color(0xFF93C5FD).copy(alpha = if (isDriveEnabled) 0.8f else 0.3f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(24.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Turn Right",
            tint = Color(0xFF93C5FD).copy(alpha = if (isDriveEnabled) 0.8f else 0.3f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp)
                .size(24.dp)
        )

        // Center Floating Joystick Knob
        Surface(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(70.dp)
                .pointerInput(isDriveEnabled) {
                    if (isDriveEnabled) {
                        detectDragGestures(
                            onDragEnd = {
                                offsetX = 0f
                                offsetY = 0f
                                onDrive(0f, 0f)
                            },
                            onDragCancel = {
                                offsetX = 0f
                                offsetY = 0f
                                onDrive(0f, 0f)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val nextX = (offsetX + dragAmount.x).coerceIn(-maxPx, maxPx)
                                val nextY = (offsetY + dragAmount.y).coerceIn(-maxPx, maxPx)
                                offsetX = nextX
                                offsetY = nextY

                                val normalizedLinear = -offsetY / maxPx
                                val normalizedAngular = -offsetX / maxPx
                                onDrive(normalizedLinear, normalizedAngular)
                            }
                        )
                    }
                },
            shape = CircleShape,
            color = if (isDriveEnabled) Color.White else Color(0xFF94A3B8),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(0xFF0F172A), CircleShape)
                )
            }
        }
    }
}

// ================= CIRCULAR ACTION BUTTON COMPONENT =================
@Composable
fun CircularActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = Color(0xFF93C5FD),
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = Color(0xFF10264A).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E3A6B))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}