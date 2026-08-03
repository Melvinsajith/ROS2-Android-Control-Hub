package com.robotics.ros2controller

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robotics.ros2controller.network.RosbridgeClient
import com.robotics.ros2controller.ui.screens.AdvancedTeleopScreen
import com.robotics.ros2controller.ui.screens.CameraScreen
import com.robotics.ros2controller.ui.theme.ROS2ControllerTheme
import com.robotics.ros2controller.ui.theme.Reference_HeaderBackground
import com.robotics.ros2controller.ui.theme.Reference_Text_OnHeader
import com.robotics.ros2controller.ui.theme.Reference_Text_Secondary

class MainActivity : ComponentActivity() {
    private val rosClient = RosbridgeClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ROS2ControllerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ROS2AppNavigation(rosClient)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rosClient.disconnect()
    }
}

// Hospital Locations Dictionary[cite: 9]
val hospitalLocations = mapOf(
    "Pharmacy" to listOf(-13.77, -5.23, 0.0),
    "ICU Ward" to listOf(2.00, -5.61, 0.0),
    "Emergency/OT" to listOf(4.74, -3.98, 0.0),
    "Lab Samples" to listOf(-9.48, -4.51, 0.0),
    "Ward A" to listOf(0.79, 5.22, 0.0),
    "Ward B" to listOf(-7.86, 5.21, 0.0),
    "Charging Base" to listOf(0.0, 0.0, 0.0)
)

// Navigation Tab Definitions
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Teleop : Screen("teleop", "Teleop", Icons.Default.SportsMotorsports)
    object Camera : Screen("camera", "Camera", Icons.Default.Videocam)
    object Nav2 : Screen("nav2", "Nav2 Goal", Icons.Default.Navigation)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

@Composable
fun ROS2AppNavigation(rosClient: RosbridgeClient) {
    val context = LocalContext.current

    // Connection States
    var ipAddress by remember { mutableStateOf("10.0.2.2") }
    var port by remember { mutableStateOf("9090") }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready") }

    // Dialog state for Connection Popup
    var showConnectionDialog by remember { mutableStateOf(false) }

    // Nav2 Settings
    var targetX by remember { mutableStateOf("1.0") }
    var targetY by remember { mutableStateOf("0.5") }
    var targetYaw by remember { mutableStateOf("0.0") }

    // Selected Page State
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Settings) }

    LaunchedEffect(Unit) {
        rosClient.onConnectionStateChanged = { status, msg ->
            isConnected = status
            isConnecting = false
            statusMessage = msg ?: if (status) "Connected" else "Disconnected"
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-check connection when navigating to active control screens
    fun navigateTo(screen: Screen) {
        if (!isConnected && screen != Screen.Settings) {
            showConnectionDialog = true
        } else {
            currentScreen = screen
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val items = listOf(Screen.Teleop, Screen.Camera, Screen.Nav2, Screen.Settings)
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { navigateTo(screen) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ================= PERSISTENT TOP BLUE HEADER =================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Reference_HeaderBackground,
                        MaterialTheme.shapes.extraLarge
                    )
                    .padding(bottom = 20.dp, top = 20.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROS 2 HUB",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Reference_Text_OnHeader
                    )

                    // Persistent Quick E-STOP Button
                    IconButton(
                        onClick = {
                            rosClient.triggerEmergencyStop()
                            Toast.makeText(context, "🚨 EMERGENCY STOP TRIGGERED!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .background(Color(0xFFDC2626), MaterialTheme.shapes.small)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Quick E-Stop",
                            tint = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Page: ${currentScreen.title}",
                    fontSize = 14.sp,
                    color = Reference_Text_OnHeader.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // CONNECTION BADGE
                InputChip(
                    selected = true,
                    onClick = {
                        if (!isConnected) showConnectionDialog = true
                    },
                    label = {
                        Text(
                            text = when {
                                isConnecting -> "CONNECTING..."
                                isConnected -> "SYSTEM CONNECTED"
                                else -> "DISCONNECTED (TAP TO CONNECT)"
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = when {
                            isConnecting -> Color(0xFFF59E0B)
                            isConnected -> Color(0xFF22C55E)
                            else -> Color(0xFFDC2626)
                        }
                    ),
                    shape = MaterialTheme.shapes.medium
                )
            }

            // ================= PAGE CONTENT SWAPPER =================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when (currentScreen) {
                    is Screen.Teleop -> AdvancedTeleopScreen(
                        isConnected = isConnected,
                        onSendCmdVel = { linX, angZ -> rosClient.sendCmdVel(linX, angZ) },
                        onTriggerEStop = { rosClient.triggerEmergencyStop() }
                    )

                    is Screen.Camera -> CameraScreen(
                        ipAddress = ipAddress.trim(),
                        isConnected = isConnected
                    )

                    is Screen.Nav2 -> Nav2Screen(
                        targetX = targetX,
                        onXChange = { targetX = it },
                        targetY = targetY,
                        onYChange = { targetY = it },
                        targetYaw = targetYaw,
                        onYawChange = { targetYaw = it },
                        isConnected = isConnected,
                        onDispatch = { xVal, yVal, yawVal ->
                            rosClient.sendNav2Goal(xVal, yVal, yawVal)
                        }
                    )

                    is Screen.Settings -> SettingsScreen(
                        ipAddress = ipAddress,
                        onIpChange = { ipAddress = it },
                        port = port,
                        onPortChange = { port = it },
                        isConnected = isConnected,
                        isConnecting = isConnecting,
                        onConnectClick = {
                            if (isConnected) {
                                rosClient.disconnect()
                            } else {
                                isConnecting = true
                                rosClient.connect(ipAddress.trim(), port.trim())
                            }
                        }
                    )
                }
            }
        }
    }

    // ================= DISCONNECTED POPUP DIALOG =================
    if (showConnectionDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            title = { Text("Robot Disconnected", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Please connect to Rosbridge before operating the robot.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextFieldStyled(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = "IP Address",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextFieldStyled(
                        value = port,
                        onValueChange = { port = it },
                        label = "Port",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConnectionDialog = false
                        isConnecting = true
                        rosClient.connect(ipAddress.trim(), port.trim())
                    }
                ) {
                    Text("Connect Now")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConnectionDialog = false
                        currentScreen = Screen.Settings
                    }
                ) {
                    Text("Go to Settings")
                }
            }
        )
    }
}

// ================= PAGE: SETTINGS SCREEN =================
@Composable
fun SettingsScreen(
    ipAddress: String,
    onIpChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    isConnected: Boolean,
    isConnecting: Boolean,
    onConnectClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Connection Config
        CardStyled(title = "Robot Bridge Connection") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextFieldStyled(
                    value = ipAddress,
                    onValueChange = onIpChange,
                    label = "IP Address",
                    modifier = Modifier.weight(2f)
                )
                OutlinedTextFieldStyled(
                    value = port,
                    onValueChange = onPortChange,
                    label = "Port",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            ButtonPrimaryStyled(
                onClick = onConnectClick,
                text = when {
                    isConnecting -> "Connecting..."
                    isConnected -> "Disconnect Bridge"
                    else -> "Connect Bridge"
                },
                color = if (isConnected) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary,
                enabled = !isConnecting
            )
        }

        // Section 2: General Robot Parameters
        CardStyled(title = "ROS 2 Environment Config") {
            Text("Default Frame ID: map", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Teleop Topic: /cmd_vel", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Nav Goal Topic: /goal_pose", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Camera Feed Port: 8080 (web_video_server)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ================= PAGE: NAV2 SCREEN =================
@Composable
fun Nav2Screen(
    targetX: String,
    onXChange: (String) -> Unit,
    targetY: String,
    onYChange: (String) -> Unit,
    targetYaw: String,
    onYawChange: (String) -> Unit,
    isConnected: Boolean,
    onDispatch: (Double, Double, Double) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CardStyled(
            title = "Hospital Destinations",
            subtitle = "Tap to dispatch automatically"
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                items(hospitalLocations.keys.toList()) { locName ->
                    val coords = hospitalLocations[locName]!!
                    OutlinedButton(
                        onClick = {
                            onXChange(coords[0].toString())
                            onYChange(coords[1].toString())
                            onYawChange(coords[2].toString())
                            if (isConnected) {
                                onDispatch(coords[0], coords[1], coords[2])
                            }
                        },
                        enabled = isConnected,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = locName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        CardStyled(
            title = "Custom Pose Dispatcher",
            subtitle = "/goal_pose"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextFieldStyled(
                    value = targetX,
                    onValueChange = onXChange,
                    label = "Target X",
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextFieldStyled(
                    value = targetY,
                    onValueChange = onYChange,
                    label = "Target Y",
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextFieldStyled(
                    value = targetYaw,
                    onValueChange = onYawChange,
                    label = "Yaw (rad)",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ButtonPrimaryStyled(
                onClick = {
                    val x = targetX.toDoubleOrNull() ?: 0.0
                    val y = targetY.toDoubleOrNull() ?: 0.0
                    val yaw = targetYaw.toDoubleOrNull() ?: 0.0
                    onDispatch(x, y, yaw)
                },
                text = "Dispatch Custom Pose",
                enabled = isConnected
            )
        }
    }
}

// ================= REUSABLE STYLED COMPONENTS =================
@Composable
fun CardStyled(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Reference_Text_Secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
            content()
        }
    }
}

@Composable
fun OutlinedTextFieldStyled(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = Reference_Text_Secondary,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
fun ButtonPrimaryStyled(
    onClick: () -> Unit,
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = Reference_Text_Secondary.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(16.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}