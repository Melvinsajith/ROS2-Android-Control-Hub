package com.robotics.ros2controller.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

data class Waypoint(
    val id: String,
    val name: String,
    val x: Double,
    val y: Double,
    val yaw: Double
)

class RobotConfigHolder {
    // --- Dynamic Theme Selection State ---
    var themeMode by mutableStateOf(AppThemeMode.SYSTEM)

    // --- ROS2 Topic Configurations (With Defaults) ---
    var cmdVelTopic by mutableStateOf("/cmd_vel")
    var goalPoseTopic by mutableStateOf("/goal_pose")
    var batteryTopic by mutableStateOf("/battery_state")
    var amclPoseTopic by mutableStateOf("/amcl_pose")
    var slamPoseTopic by mutableStateOf("/pose")
    var planTopic by mutableStateOf("/plan")
    var mapTopic by mutableStateOf("/map")
    var cameraTopic by mutableStateOf("/camera/camera_sensor/image_raw")
    var globalFrameId by mutableStateOf("map")

    // --- Dynamic Waypoints Registry (Editable inside App) ---
    val waypoints = mutableStateListOf(
        Waypoint("1", "Pharmacy", -13.77, -5.23, 0.0),
        Waypoint("2", "ICU Ward", 2.00, -5.61, 0.0),
        Waypoint("3", "Emergency/OT", 4.74, -3.98, 0.0),
        Waypoint("4", "Lab Samples", -9.48, -4.51, 0.0),
        Waypoint("5", "Ward A", 0.79, 5.22, 0.0),
        Waypoint("6", "Ward B", -7.86, 5.21, 0.0),
        Waypoint("7", "Charging Base", 0.0, 0.0, 0.0)
    )

    fun addWaypoint(name: String, x: Double, y: Double, yaw: Double) {
        waypoints.add(Waypoint(System.currentTimeMillis().toString(), name, x, y, yaw))
    }

    fun removeWaypoint(id: String) {
        waypoints.removeAll { it.id == id }
    }
}