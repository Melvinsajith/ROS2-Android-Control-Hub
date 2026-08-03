package com.robotics.ros2controller.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotics.ros2controller.ui.components.VirtualJoystick
import com.robotics.ros2controller.ui.theme.Reference_Text_Secondary

@Composable
fun AdvancedTeleopScreen(
    isConnected: Boolean,
    onSendCmdVel: (Double, Double) -> Unit,
    onTriggerEStop: () -> Unit
) {
    var isEmergencyLocked by remember { mutableStateOf(false) }
    var isSafetyUnlocked by remember { mutableStateOf(false) }

    var maxLinearSpeed by remember { mutableStateOf(0.5f) } // m/s
    var maxAngularSpeed by remember { mutableStateOf(1.0f) } // rad/s

    var currentLinearVel by remember { mutableStateOf(0.0) }
    var currentAngularVel by remember { mutableStateOf(0.0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ================= 1. MASTER E-STOP BANNER =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (isEmergencyLocked) Color(0xFF7F1D1D) else Color(0xFF991B1B)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
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
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "E-Stop",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (isEmergencyLocked) "E-STOP LATCHED!" else "EMERGENCY STOP (E-STOP)",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                AnimatedVisibility(visible = isEmergencyLocked) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(
                            text = "Robot locked! Reset E-Stop to enable controls.",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { isEmergencyLocked = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("RESET EMERGENCY LOCK")
                        }
                    }
                }
            }
        }

        // ================= 2. SAFETY LOCK & SPEED CONTROLS =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        fontSize = 16.sp
                    )
                    IconButton(
                        onClick = { isSafetyUnlocked = !isSafetyUnlocked },
                        enabled = !isEmergencyLocked
                    ) {
                        Icon(
                            imageVector = if (isSafetyUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Safety Lock",
                            tint = if (isSafetyUnlocked) Color(0xFF22C55E) else Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Linear Speed Slider
                Text(
                    text = "Max Linear Velocity: ${String.format("%.2f", maxLinearSpeed)} m/s",
                    fontSize = 13.sp,
                    color = Reference_Text_Secondary
                )
                Slider(
                    value = maxLinearSpeed,
                    onValueChange = { maxLinearSpeed = it },
                    valueRange = 0.1f..1.5f,
                    steps = 14
                )

                // Angular Speed Slider
                Text(
                    text = "Max Angular Velocity: ${String.format("%.2f", maxAngularSpeed)} rad/s",
                    fontSize = 13.sp,
                    color = Reference_Text_Secondary
                )
                Slider(
                    value = maxAngularSpeed,
                    onValueChange = { maxAngularSpeed = it },
                    valueRange = 0.2f..2.0f,
                    steps = 18
                )
            }
        }

        // ================= 3. JOYSTICK & LIVE TELEMETRY =================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Live Velocity Gauges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TelemetryValueBox("Linear X", String.format("%.2f m/s", currentLinearVel))
                    TelemetryValueBox("Angular Z", String.format("%.2f rad/s", currentAngularVel))
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interactive 2D Joystick
                VirtualJoystick(
                    size = 260.dp,
                    thumbColor = if (isSafetyUnlocked && !isEmergencyLocked) MaterialTheme.colorScheme.primary else Color.Gray
                ) { normX, normZ ->
                    if (isConnected && isSafetyUnlocked && !isEmergencyLocked) {
                        val linX = normX * maxLinearSpeed
                        val angZ = normZ * maxAngularSpeed
                        currentLinearVel = linX
                        currentAngularVel = angZ
                        onSendCmdVel(linX, angZ)
                    } else {
                        currentLinearVel = 0.0
                        currentAngularVel = 0.0
                    }
                }

                if (!isSafetyUnlocked && !isEmergencyLocked) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🔒 Unlock safety lock icon above to enable driving",
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
fun TelemetryValueBox(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = Reference_Text_Secondary)
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}