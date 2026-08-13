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
import androidx.compose.material.icons.filled.BatteryUnknown
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
    batteryPercent: Int? = null,
    cameraTopic: String = "/camera/camera_sensor/image_raw",
    streamPort: String = "8080"
) {
    var isEmergencyLocked by remember { mutableStateOf(false) }
    var isSafetyUnlocked by remember { mutableStateOf(false) }
    var maxLinearSpeed by remember { mutableFloatStateOf(0.5f) }
    var maxAngularSpeed by remember { mutableFloatStateOf(1.0f) }

    var currentLinearVel by remember { mutableDoubleStateOf(0.0) }
    var currentAngularVel by remember { mutableDoubleStateOf(0.0) }

    val colors = MaterialTheme.colorScheme
    val streamUrl = "http://$ipAddress:$streamPort/stream?topic=$cameraTopic"

    val actualBattery = if (isConnected) batteryPercent else null
    val displayBatteryText = actualBattery?.let { "$it%" } ?: "--%"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // ================= 1. FULLSCREEN CAMERA FEED (BACKGROUND) =================
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
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera Disconnected",
                    color = colors.primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Dark gradient scrim overlay to maintain UI readability over live camera video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // ================= 2. FOREGROUND CONTROLS & HUD OVERLAY =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // --- HEADER & STATUS CHIPS ---
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Top Action Bar
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
                        color = colors.surface.copy(alpha = 0.85f)
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.onSurface
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
                        color = colors.surface.copy(alpha = 0.85f)
                    ) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Options",
                                tint = colors.onSurface
                            )
                        }
                    }
                }

                // Glassmorphic Status Chips Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Dynamic Battery Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (actualBattery != null) Icons.Default.BatteryChargingFull else Icons.Default.BatteryUnknown,
                                    contentDescription = null,
                                    tint = when {
                                        actualBattery == null -> colors.onSurface.copy(alpha = 0.4f)
                                        actualBattery > 60 -> Color(0xFF10B981)
                                        actualBattery > 20 -> Color(0xFFF59E0B)
                                        else -> Color(0xFFF43F5E)
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(displayBatteryText, color = colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Speed Trim Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = colors.surface.copy(alpha = 0.85f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = colors.onSurface,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text("${"%.1f".format(maxLinearSpeed)}m/s", color = colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Live Indicator Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surface.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceVariant)
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
                            Text("Live", color = colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- CONTROLLER DECK ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Spacer placeholder to center the circular D-Pad
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

                // --- RIGHT FUNCTION BUTTONS (SAFETY LOCK & E-STOP) ---
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
        }

        // --- RESET EMERGENCY LOCK BANNER ---
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) {
            AnimatedVisibility(visible = isEmergencyLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .background(Color(0xEEF43F5E), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🚨 Emergency Stop Latched", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { isEmergencyLocked = false }) {
                        Text("RESET LOCK", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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

    val colors = MaterialTheme.colorScheme
    val isDriveEnabled = isSafetyUnlocked && !isEmergencyLocked

    Box(
        modifier = Modifier
            .size(radius * 2)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.surface.copy(alpha = 0.9f),
                        colors.surfaceVariant.copy(alpha = 0.9f)
                    )
                ),
                shape = CircleShape
            )
            .border(1.dp, colors.primary.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // D-pad Direction Arrows
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Forward",
            tint = colors.primary.copy(alpha = if (isDriveEnabled) 0.9f else 0.3f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .size(24.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Backward",
            tint = colors.primary.copy(alpha = if (isDriveEnabled) 0.9f else 0.3f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .size(24.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "Turn Left",
            tint = colors.primary.copy(alpha = if (isDriveEnabled) 0.9f else 0.3f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp)
                .size(24.dp)
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Turn Right",
            tint = colors.primary.copy(alpha = if (isDriveEnabled) 0.9f else 0.3f),
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
            color = if (isDriveEnabled) colors.primary else colors.onSurface.copy(alpha = 0.4f),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(colors.onPrimary, CircleShape)
                )
            }
        }
    }
}

// ================= CIRCULAR ACTION BUTTON COMPONENT =================
@Composable
fun CircularActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = colors.surface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.surfaceVariant)
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