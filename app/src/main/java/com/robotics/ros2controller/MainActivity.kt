package com.robotics.ros2controller

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
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
import com.robotics.ros2controller.ui.components.InteractiveMapCanvas
import com.robotics.ros2controller.ui.screens.AdvancedTeleopScreen
import com.robotics.ros2controller.ui.screens.CameraScreen
import com.robotics.ros2controller.ui.screens.UnifiedDashboardScreen
import com.robotics.ros2controller.ui.theme.ROS2ControllerTheme

// --- Modern Glassmorphic Soft Red/Rose Palette ---
val ModernRoseRed = Color(0xFFF43F5E)          // Lighter modern neon rose accent
val SoftRoseBanner = Color(0x33F43F5E)          // Glassmorphic translucent red background
val RoseBorder = Color(0xFFFB7185)              // Subtle luminous border outline

class MainActivity : ComponentActivity() {
    private val rosClient = RosbridgeClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ROS2ControllerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF090D16)
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

// Hospital Locations Registry
val hospitalLocations = mapOf(
    "Pharmacy" to listOf(-13.77, -5.23, 0.0),
    "ICU Ward" to listOf(2.00, -5.61, 0.0),
    "Emergency/OT" to listOf(4.74, -3.98, 0.0),
    "Lab Samples" to listOf(-9.48, -4.51, 0.0),
    "Ward A" to listOf(0.79, 5.22, 0.0),
    "Ward B" to listOf(-7.86, 5.21, 0.0),
    "Charging Base" to listOf(0.0, 0.0, 0.0)
)

// App Navigation Routes
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Teleop : Screen("teleop", "Teleop", Icons.Default.SportsEsports)
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

    // Nav2 State
    var targetX by remember { mutableStateOf("1.0") }
    var targetY by remember { mutableStateOf("0.5") }
    var targetYaw by remember { mutableStateOf("0.0") }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    LaunchedEffect(Unit) {
        rosClient.onConnectionStateChanged = { status, msg ->
            isConnected = status
            isConnecting = false
            statusMessage = msg ?: if (status) "Connected" else "Disconnected"
            Toast.makeText(context, statusMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        containerColor = Color(0xFF090D16),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF131C2E),
                tonalElevation = 12.dp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val items = listOf(
                    Screen.Dashboard,
                    Screen.Teleop,
                    Screen.Camera,
                    Screen.Nav2,
                    Screen.Settings
                )
                items.forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                tint = if (selected) Color(0xFF38BDF8) else Color(0xFF64748B)
                            )
                        },
                        label = {
                            Text(
                                screen.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (selected) Color(0xFF38BDF8) else Color(0xFF64748B)
                            )
                        },
                        selected = selected,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color(0xFF1E293B)
                        )
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
            // ================= TOP HEADER CARD =================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DIO ROBOT CONNECT",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Page: ${currentScreen.title}",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        // Glassmorphic Soft E-Stop Button
                        IconButton(
                            onClick = {
                                rosClient.triggerEmergencyStop()
                                Toast.makeText(context, "🚨 EMERGENCY STOP!", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier
                                .background(SoftRoseBanner, RoundedCornerShape(12.dp))
                                .border(1.dp, RoseBorder.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "E-Stop",
                                tint = ModernRoseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Connection Status Badge
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = when {
                                    isConnecting -> "CONNECTING TO DIO..."
                                    isConnected -> "SYSTEM CONNECTED"
                                    else -> "BRIDGE DISCONNECTED"
                                },
                                color = when {
                                    isConnecting -> Color(0xFFFBBF24)
                                    isConnected -> Color(0xFF34D399)
                                    else -> ModernRoseRed
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when {
                                isConnecting -> Color(0x33F59E0B)
                                isConnected -> Color(0x3310B981)
                                else -> SoftRoseBanner
                            }
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = when {
                                isConnecting -> Color(0xFFF59E0B)
                                isConnected -> Color(0xFF10B981)
                                else -> RoseBorder.copy(alpha = 0.5f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ================= PAGE VIEW SWITCHER =================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                when (currentScreen) {
                    is Screen.Dashboard -> UnifiedDashboardScreen(
                        ipAddress = ipAddress.trim(),
                        isConnected = isConnected,
                        onSendCmdVel = { linX, angZ -> rosClient.sendCmdVel(linX, angZ) },
                        onTriggerEStop = { rosClient.triggerEmergencyStop() },
                        batteryPercent = rosClient.batteryPercentage,
                        robotX = rosClient.robotX,
                        robotY = rosClient.robotY,
                        robotYaw = rosClient.robotYaw,
                        globalPath = rosClient.globalPath,
                        mapWidth = rosClient.mapWidth,
                        mapHeight = rosClient.mapHeight,
                        mapData = rosClient.mapData,
                        mapResolution = rosClient.mapResolution,
                        mapOriginX = rosClient.mapOriginX,
                        mapOriginY = rosClient.mapOriginY,
                        taskStatus = rosClient.taskStatus,
                        destinationName = rosClient.currentDestinationName,
                        cameraTopic = "/camera/camera_sensor/image_raw"
                    )

                    is Screen.Teleop -> AdvancedTeleopScreen(
                        isConnected = isConnected,
                        onSendCmdVel = { linX, angZ -> rosClient.sendCmdVel(linX, angZ) },
                        onTriggerEStop = { rosClient.triggerEmergencyStop() }
                    )

                    is Screen.Camera -> CameraScreen(
                        ipAddress = ipAddress.trim(),
                        isConnected = isConnected,
                        cameraTopic = "/camera/camera_sensor/image_raw"
                    )

                    is Screen.Nav2 -> Nav2Screen(
                        targetX = targetX,
                        onXChange = { targetX = it },
                        targetY = targetY,
                        onYChange = { targetY = it },
                        targetYaw = targetYaw,
                        onYawChange = { targetYaw = it },
                        isConnected = isConnected,
                        robotX = rosClient.robotX,
                        robotY = rosClient.robotY,
                        robotYaw = rosClient.robotYaw,
                        globalPath = rosClient.globalPath,
                        mapWidth = rosClient.mapWidth,
                        mapHeight = rosClient.mapHeight,
                        mapData = rosClient.mapData,
                        mapResolution = rosClient.mapResolution,
                        mapOriginX = rosClient.mapOriginX,
                        mapOriginY = rosClient.mapOriginY,
                        taskStatus = rosClient.taskStatus,
                        destinationName = rosClient.currentDestinationName,
                        onDispatch = { xVal, yVal, yawVal, destName ->
                            rosClient.sendNav2Goal(xVal, yVal, yawVal, destName)
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
}

// ================= NAV2 GOAL SCREEN =================
@Composable
fun Nav2Screen(
    targetX: String,
    onXChange: (String) -> Unit,
    targetY: String,
    onYChange: (String) -> Unit,
    targetYaw: String,
    onYawChange: (String) -> Unit,
    isConnected: Boolean,
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
    taskStatus: String,
    destinationName: String,
    onDispatch: (Double, Double, Double, String) -> Unit
) {
    var selectedLocation by remember { mutableStateOf(hospitalLocations.keys.first()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mission Task Status Banner with Glassmorphic Translucent Red Tint
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = when {
                        taskStatus.contains("Reached") -> Color(0xFF10B981)
                        taskStatus.contains("En Route") || taskStatus.contains("Received") -> Color(0xFF38BDF8)
                        taskStatus.contains("Dispatched") -> Color(0xFFF59E0B)
                        taskStatus.contains("STOP") || taskStatus.contains("Aborted") -> RoseBorder
                        else -> Color(0xFF334155)
                    },
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    taskStatus.contains("Reached") -> Color(0x3310B981)
                    taskStatus.contains("En Route") || taskStatus.contains("Received") -> Color(0x330284C7)
                    taskStatus.contains("Dispatched") -> Color(0x33D97706)
                    taskStatus.contains("STOP") || taskStatus.contains("Aborted") -> SoftRoseBanner
                    else -> Color(0xFF131C2E)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
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
                        color = if (taskStatus.contains("STOP")) ModernRoseRed else Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = if (taskStatus.contains("STOP")) ModernRoseRed else Color.White
                )
            }
        }

        // Live Interactive Occupancy Grid Map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Text(
                    text = "Live Map | Pose: (${"%.2f".format(robotX)}, ${"%.2f".format(robotY)})",
                    color = Color.White,
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

        // Horizontal Station Waypoint Carousel
        DarkCardStyled(
            title = "Station Waypoints",
            subtitle = "Swipe and select a location"
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(hospitalLocations.keys.toList()) { locName ->
                    val coords = hospitalLocations[locName]!!
                    val isSelected = locName == selectedLocation

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedLocation = locName
                            onXChange(coords[0].toString())
                            onYChange(coords[1].toString())
                            onYawChange(coords[2].toString())
                            if (isConnected) {
                                onDispatch(coords[0], coords[1], coords[2], locName)
                            }
                        },
                        label = { Text(locName, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = Color(0xFF0F172A),
                            labelColor = Color(0xFF94A3B8),
                            iconColor = Color(0xFF64748B)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Manual Target Pose Dispatcher
        DarkCardStyled(
            title = "Custom Pose Dispatcher",
            subtitle = "Publish target pose to /goal_pose"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkOutlinedTextField(value = targetX, onValueChange = onXChange, label = "Target X", modifier = Modifier.weight(1f))
                DarkOutlinedTextField(value = targetY, onValueChange = onYChange, label = "Target Y", modifier = Modifier.weight(1f))
                DarkOutlinedTextField(value = targetYaw, onValueChange = onYawChange, label = "Yaw (rad)", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            DarkPrimaryButton(
                onClick = {
                    val x = targetX.toDoubleOrNull() ?: 0.0
                    val y = targetY.toDoubleOrNull() ?: 0.0
                    val yaw = targetYaw.toDoubleOrNull() ?: 0.0
                    onDispatch(x, y, yaw, "Custom Pose")
                },
                text = "Dispatch Target Pose",
                enabled = isConnected
            )
        }
    }
}

// ================= SETTINGS SCREEN =================
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
        DarkCardStyled(title = "DIO Bridge Connection") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DarkOutlinedTextField(value = ipAddress, onValueChange = onIpChange, label = "Robot IP Address", modifier = Modifier.weight(2f))
                DarkOutlinedTextField(value = port, onValueChange = onPortChange, label = "Port", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            DarkPrimaryButton(
                onClick = onConnectClick,
                text = when {
                    isConnecting -> "Connecting to DIO Robot..."
                    isConnected -> "Disconnect DIO Bridge"
                    else -> "Connect to DIO Robot"
                },
                color = if (isConnected) ModernRoseRed else Color(0xFF0284C7),
                enabled = !isConnecting
            )
        }

        DarkCardStyled(title = "DIO System Architecture") {
            Text("Default Frame ID: map", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFCBD5E1))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Teleop Topic: /cmd_vel", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFCBD5E1))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Nav Goal Topic: /goal_pose", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFCBD5E1))
            Spacer(modifier = Modifier.height(4.dp))
            Text("Camera Feed Port: 8080 (web_video_server)", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFCBD5E1))
        }
    }
}

// ================= REUSABLE DARK THEME COMPONENTS =================
@Composable
fun DarkCardStyled(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131C2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
            content()
        }
    }
}

@Composable
fun DarkOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp, color = Color(0xFF94A3B8)) },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFF334155),
            focusedBorderColor = Color(0xFF38BDF8),
            unfocusedTextColor = Color.White,
            focusedTextColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp)
    )
}

@Composable
fun DarkPrimaryButton(
    onClick: () -> Unit,
    text: String,
    color: Color = Color(0xFF0284C7),
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = Color(0xFF334155)
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
        }
    }
}