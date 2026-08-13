package com.robotics.ros2controller

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
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
import com.robotics.ros2controller.data.AppThemeMode
import com.robotics.ros2controller.data.RobotConfigHolder
import com.robotics.ros2controller.network.RosbridgeClient
import com.robotics.ros2controller.ui.components.InteractiveMapCanvas
import com.robotics.ros2controller.ui.screens.AdvancedTeleopScreen
import com.robotics.ros2controller.ui.screens.CameraScreen
import com.robotics.ros2controller.ui.screens.UnifiedDashboardScreen
import com.robotics.ros2controller.ui.theme.ROS2ControllerTheme

// --- Shared Rose / Emergency Colors ---
val ModernRoseRed = Color(0xFFF43F5E)
val SoftRoseBanner = Color(0x33F43F5E)
val RoseBorder = Color(0xFFFB7185)

class MainActivity : ComponentActivity() {
    private val robotConfig = RobotConfigHolder()
    private val rosClient by lazy { RosbridgeClient(robotConfig) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemInDark = isSystemInDarkTheme()
            val useDarkTheme = when (robotConfig.themeMode) {
                AppThemeMode.SYSTEM -> systemInDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            ROS2ControllerTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ROS2AppNavigation(rosClient, robotConfig)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rosClient.disconnect()
    }
}

// App Navigation Routes (Camera removed from bottom navigation)
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Teleop : Screen("teleop", "Teleop", Icons.Default.SportsEsports)
    object Nav2 : Screen("nav2", "Nav2 Goal", Icons.Default.Navigation)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Camera : Screen("camera", "Camera Feed", Icons.Default.Videocam)
}

@Composable
fun ROS2AppNavigation(rosClient: RosbridgeClient, config: RobotConfigHolder) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

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
        containerColor = colors.background,
        bottomBar = {
            // Bottom navigation bar displays 4 core tabs
            NavigationBar(
                containerColor = colors.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                val bottomNavItems = listOf(
                    Screen.Dashboard,
                    Screen.Teleop,
                    Screen.Nav2,
                    Screen.Settings
                )
                bottomNavItems.forEach { screen ->
                    val selected = currentScreen == screen
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                tint = if (selected) colors.primary else colors.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        label = {
                            Text(
                                screen.title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (selected) colors.primary else colors.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        selected = selected,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colors.surfaceVariant
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
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Back button when viewing camera page from settings
                            if (currentScreen == Screen.Camera) {
                                IconButton(
                                    onClick = { currentScreen = Screen.Settings },
                                    modifier = Modifier
                                        .background(colors.surfaceVariant, CircleShape)
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back to Settings",
                                        tint = colors.onSurface
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "DIO ROBOT CONNECT",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Page: ${currentScreen.title}",
                                    fontSize = 12.sp,
                                    color = colors.onSurface.copy(alpha = 0.6f)
                                )
                            }
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Connection Status Badge
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = when {
                                    isConnecting -> "CONNECTING TO ROBOT..."
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
                        cameraTopic = config.cameraTopic
                    )

                    is Screen.Teleop -> AdvancedTeleopScreen(
                        ipAddress = ipAddress.trim(),
                        isConnected = isConnected,
                        onSendCmdVel = { linX, angZ -> rosClient.sendCmdVel(linX, angZ) },
                        onTriggerEStop = { rosClient.triggerEmergencyStop() },
                        cameraTopic = config.cameraTopic
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
                        config = config,
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
                        config = config,
                        onConnectClick = {
                            if (isConnected) {
                                rosClient.disconnect()
                            } else {
                                isConnecting = true
                                rosClient.connect(ipAddress.trim(), port.trim())
                            }
                        },
                        onOpenCameraButtonClick = {
                            currentScreen = Screen.Camera
                        }
                    )

                    is Screen.Camera -> CameraScreen(
                        ipAddress = ipAddress.trim(),
                        isConnected = isConnected,
                        cameraTopic = config.cameraTopic
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
    config: RobotConfigHolder,
    onDispatch: (Double, Double, Double, String) -> Unit
) {
    var selectedWpId by remember { mutableStateOf(config.waypoints.firstOrNull()?.id ?: "") }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mission Task Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = when {
                        taskStatus.contains("Reached") -> Color(0xFF10B981)
                        taskStatus.contains("En Route") || taskStatus.contains("Received") -> colors.primary
                        taskStatus.contains("Dispatched") -> Color(0xFFF59E0B)
                        taskStatus.contains("STOP") || taskStatus.contains("Aborted") -> RoseBorder
                        else -> colors.surfaceVariant
                    },
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    taskStatus.contains("Reached") -> Color(0x3310B981)
                    taskStatus.contains("En Route") || taskStatus.contains("Received") -> colors.primary.copy(alpha = 0.2f)
                    taskStatus.contains("Dispatched") -> Color(0x33D97706)
                    taskStatus.contains("STOP") || taskStatus.contains("Aborted") -> SoftRoseBanner
                    else -> colors.surface
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
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Status: $taskStatus",
                        color = if (taskStatus.contains("STOP")) ModernRoseRed else colors.onSurface.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = if (taskStatus.contains("STOP")) ModernRoseRed else colors.primary
                )
            }
        }

        // Live Interactive Occupancy Grid Map
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                Text(
                    text = "Live Map | Pose: (${"%.2f".format(robotX)}, ${"%.2f".format(robotY)})",
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

        // Dynamic Station Waypoint Carousel
        StyledCard(
            title = "Station Waypoints",
            subtitle = "Swipe and select a location"
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(config.waypoints) { wp ->
                    val isSelected = wp.id == selectedWpId

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedWpId = wp.id
                            onXChange(wp.x.toString())
                            onYChange(wp.y.toString())
                            onYawChange(wp.yaw.toString())
                            if (isConnected) {
                                onDispatch(wp.x, wp.y, wp.yaw, wp.name)
                            }
                        },
                        label = { Text(wp.name, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White,
                            containerColor = colors.surfaceVariant,
                            labelColor = colors.onSurface.copy(alpha = 0.7f),
                            iconColor = colors.onSurface.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Manual Target Pose Dispatcher
        StyledCard(
            title = "Custom Pose Dispatcher",
            subtitle = "Publish target pose to ${config.goalPoseTopic}"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StyledOutlinedTextField(value = targetX, onValueChange = onXChange, label = "Target X", modifier = Modifier.weight(1f))
                StyledOutlinedTextField(value = targetY, onValueChange = onYChange, label = "Target Y", modifier = Modifier.weight(1f))
                StyledOutlinedTextField(value = targetYaw, onValueChange = onYawChange, label = "Yaw (rad)", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            StyledPrimaryButton(
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
    config: RobotConfigHolder,
    onConnectClick: () -> Unit,
    onOpenCameraButtonClick: () -> Unit
) {
    var newWpName by remember { mutableStateOf("") }
    var newWpX by remember { mutableStateOf("") }
    var newWpY by remember { mutableStateOf("") }

    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. LIVE CAMERA FEED LAUNCH BUTTON ---
        StyledCard(
            title = "Robot Vision",
            subtitle = "Open dedicated full-screen live video monitor"
        ) {
            Button(
                onClick = onOpenCameraButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Open Camera",
                        tint = Color.White
                    )
                    Text(
                        text = "Open Live Camera View",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }

        // --- 2. APPEARANCE & THEME SWITCHER CARD ---
        StyledCard(title = "App Appearance", subtitle = "Choose your preferred visual theme") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val themeOptions = listOf(
                    AppThemeMode.SYSTEM to "System",
                    AppThemeMode.LIGHT to "Light",
                    AppThemeMode.DARK to "Dark"
                )

                themeOptions.forEach { (mode, label) ->
                    val isSelected = config.themeMode == mode
                    Button(
                        onClick = { config.themeMode = mode },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) colors.primary else Color.Transparent,
                            contentColor = if (isSelected) Color.White else colors.onSurface.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                    ) {
                        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // --- 3. BRIDGE CONNECTION ---
        StyledCard(title = "DIO Bridge Connection", subtitle = "Network WebSocket endpoint") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StyledOutlinedTextField(value = ipAddress, onValueChange = onIpChange, label = "Robot IP Address", modifier = Modifier.weight(2f))
                StyledOutlinedTextField(value = port, onValueChange = onPortChange, label = "Port", modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onConnectClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) ModernRoseRed else colors.primary
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = !isConnecting
            ) {
                Text(
                    text = when {
                        isConnecting -> "Connecting to Robot..."
                        isConnected -> "Disconnect DIO Bridge"
                        else -> "Connect to DIO Robot"
                    },
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // --- 4. ROS 2 TOPIC CONFIGURATOR ---
        StyledCard(
            title = "ROS 2 Topic Configurator",
            subtitle = "Modify topic mapping dynamically for any robot"
        ) {
            StyledOutlinedTextField(value = config.cmdVelTopic, onValueChange = { config.cmdVelTopic = it }, label = "Teleop Velocity Topic", modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            StyledOutlinedTextField(value = config.goalPoseTopic, onValueChange = { config.goalPoseTopic = it }, label = "Nav2 Goal Topic", modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            StyledOutlinedTextField(value = config.cameraTopic, onValueChange = { config.cameraTopic = it }, label = "Camera Stream Topic", modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            StyledOutlinedTextField(value = config.globalFrameId, onValueChange = { config.globalFrameId = it }, label = "Global Frame ID (e.g. map or odom)", modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            StyledOutlinedTextField(value = config.mapTopic, onValueChange = { config.mapTopic = it }, label = "Occupancy Map Topic", modifier = Modifier.fillMaxWidth())
        }

        // --- 5. DYNAMIC WAYPOINT MANAGER ---
        StyledCard(
            title = "Waypoints Registry",
            subtitle = "Add, edit or remove target stations"
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StyledOutlinedTextField(value = newWpName, onValueChange = { newWpName = it }, label = "Name", modifier = Modifier.weight(1.5f))
                StyledOutlinedTextField(value = newWpX, onValueChange = { newWpX = it }, label = "X (m)", modifier = Modifier.weight(1f))
                StyledOutlinedTextField(value = newWpY, onValueChange = { newWpY = it }, label = "Y (m)", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val xVal = newWpX.toDoubleOrNull() ?: 0.0
                    val yVal = newWpY.toDoubleOrNull() ?: 0.0
                    if (newWpName.isNotEmpty()) {
                        config.addWaypoint(newWpName, xVal, yVal, 0.0)
                        newWpName = ""
                        newWpX = ""
                        newWpY = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add New Station Waypoint", fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            config.waypoints.forEach { wp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(colors.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(wp.name, color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("X: ${wp.x}, Y: ${wp.y}", color = colors.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
                    }

                    IconButton(onClick = { config.removeWaypoint(wp.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ModernRoseRed)
                    }
                }
            }
        }
    }
}

// ================= REUSABLE STYLED COMPONENTS =================
@Composable
fun StyledCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
fun StyledOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) },
        modifier = modifier,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun StyledPrimaryButton(
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
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
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