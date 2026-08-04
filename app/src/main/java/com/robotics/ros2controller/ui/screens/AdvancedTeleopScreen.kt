package com.robotics.ros2controller.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotics.ros2controller.ui.components.VirtualJoystick

@Composable
fun AdvancedTeleopScreen(
    isConnected: Boolean,
    onSendCmdVel: (Double, Double) -> Unit,
    onTriggerEStop: () -> Unit
) {
    var isEmergencyLocked by remember { mutableStateOf(false) }
    var isSafetyUnlocked by remember { mutableStateOf(false) }

    var maxLinearSpeed by remember { mutableFloatStateOf(0.5f) } // m/s
    var maxAngularSpeed by remember { mutableFloatStateOf(1.0f) } // rad/s

    var currentLinearVel by remember { mutableDoubleStateOf(0.0) }
    var currentAngularVel by remember { mutableDoubleStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)) // Deep Dark Slate Background
            .padding(12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ================= 1. MASTER E-STOP BANNER =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isEmergencyLocked) Color(0xFF7F1D1D) else Color(0xFF131C2E)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        isEmergencyLocked = true
                        isSafetyUnlocked = false
                        onTriggerEStop()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "E-Stop",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = if (isEmergencyLocked) "E-STOP LATCHED!" else "EMERGENCY STOP (E-STOP)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                AnimatedVisibility(visible = isEmergencyLocked) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            text = "Robot locked! Reset E-Stop to enable controls.",
                            color = Color(0xFFFECACA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { isEmergencyLocked = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.White))
                        ) {
                            Text("RESET EMERGENCY LOCK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ================= 2. SAFETY LOCK & SPEED CONTROLS =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Drive Safety Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    IconButton(
                        onClick = { isSafetyUnlocked = !isSafetyUnlocked },
                        enabled = !isEmergencyLocked
                    ) {
                        Icon(
                            imageVector = if (isSafetyUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Safety Lock",
                            tint = if (isSafetyUnlocked) Color(0xFF10B981) else Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Linear Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Max Linear Velocity",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${String.format("%.2f", maxLinearSpeed)} m/s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
                Slider(
                    value = maxLinearSpeed,
                    onValueChange = { maxLinearSpeed = it },
                    valueRange = 0.1f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0284C7),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )

                // Angular Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Max Angular Velocity",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Text(
                        text = "${String.format("%.2f", maxAngularSpeed)} rad/s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
                Slider(
                    value = maxAngularSpeed,
                    onValueChange = { maxAngularSpeed = it },
                    valueRange = 0.2f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF0284C7),
                        activeTrackColor = Color(0xFF38BDF8),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )
            }
        }

        // ================= 3. JOYSTICK & LIVE TELEMETRY =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Velocity Gauges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryValueBox("Linear X", String.format("%.2f m/s", currentLinearVel), Modifier.weight(1f))
                    TelemetryValueBox("Angular Z", String.format("%.2f rad/s", currentAngularVel), Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive 2D Joystick
                VirtualJoystick(
                    size = 240.dp,
                    thumbColor = if (isSafetyUnlocked && !isEmergencyLocked) Color(0xFF0284C7) else Color(0xFF475569)
                ) { normX, normZ ->
                    if (isConnected && isSafetyUnlocked && !isEmergencyLocked) {
                        val linX = normX * maxLinearSpeed
                        val angZ = normZ * maxAngularSpeed
                        currentLinearVel = linX.toDouble()
                        currentAngularVel = angZ.toDouble()
                        onSendCmdVel(linX.toDouble(), angZ.toDouble())
                    } else {
                        currentLinearVel = 0.0
                        currentAngularVel = 0.0
                    }
                }

                if (!isSafetyUnlocked && !isEmergencyLocked) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🔒 Tap safety lock icon above to enable driving",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryValueBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(
                color = Color(0xFF0F172A),
                shape = RoundedCornerShape(14.dp)
            )
            .padding(vertical = 10.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}