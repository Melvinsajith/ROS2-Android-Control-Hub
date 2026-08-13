package com.robotics.ros2controller.network

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.robotics.ros2controller.data.RobotConfigHolder
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class RosbridgeClient(val config: RobotConfigHolder) {
    private var webSocket: WebSocket? = null

    // --- Dynamic Battery Percentage (null indicates no data received or disconnected) ---
    var batteryPercentage by mutableStateOf<Int?>(null)
        private set

    // --- Robot Pose States (/pose & /amcl_pose) ---
    var robotX by mutableDoubleStateOf(0.0)
        private set
    var robotY by mutableDoubleStateOf(0.0)
        private set
    var robotYaw by mutableDoubleStateOf(0.0)
        private set

    // --- Active Nav2 Trajectory Path (/plan) ---
    var globalPath by mutableStateOf<List<Pair<Double, Double>>>(emptyList())
        private set

    // --- Occupancy Grid Map Metadata & Data (/map) ---
    var mapWidth by mutableIntStateOf(0)
        private set
    var mapHeight by mutableIntStateOf(0)
        private set
    var mapData by mutableStateOf<IntArray?>(null)
        private set
    var mapResolution by mutableDoubleStateOf(0.05) // Default 5cm per pixel
        private set
    var mapOriginX by mutableDoubleStateOf(-10.0)
        private set
    var mapOriginY by mutableDoubleStateOf(-10.0)
        private set

    // --- Nav2 Mission Task Status ---
    var taskStatus by mutableStateOf("Idle") // "Idle", "Task Received", "En Route", "Goal Reached"
        private set
    var currentDestinationName by mutableStateOf("")
        private set

    // OkHttp Client configured with relaxed timeouts to avoid dropouts during heavy map streaming
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS) // Prevents ping/pong timeout drops
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())

    var onConnectionStateChanged: ((Boolean, String?) -> Unit)? = null

    fun connect(ip: String, port: String) {
        disconnect()

        val url = "ws://$ip:$port"
        val request = try {
            Request.Builder().url(url).build()
        } catch (e: Exception) {
            notifyState(false, "Invalid IP or Port format")
            return
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                notifyState(true, "Successfully connected to $url")
                advertiseTopics()
                subscribeTopics()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val errorMsg = when {
                    t.message?.contains("failed to connect") == true -> "Connection refused. Is rosbridge running?"
                    t.message?.contains("timeout") == true -> "Connection timed out. Check IP address/Wi-Fi."
                    else -> t.message ?: "Network error"
                }
                notifyState(false, errorMsg)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                notifyState(false, "Bridge disconnected")
            }
        })
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User disconnect")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            webSocket = null
            batteryPercentage = null // Reset to null on disconnect
            globalPath = emptyList()
            mapData = null
            taskStatus = "Idle"
            currentDestinationName = ""
            notifyState(false, "Disconnected")
        }
    }

    private fun notifyState(isConnected: Boolean, message: String? = null) {
        mainHandler.post {
            onConnectionStateChanged?.invoke(isConnected, message)
        }
    }

    private fun advertiseTopics() {
        val cmdVelAdv = JSONObject().apply {
            put("op", "advertise")
            put("topic", config.cmdVelTopic)
            put("type", "geometry_msgs/msg/Twist")
        }
        webSocket?.send(cmdVelAdv.toString())

        val goalPoseAdv = JSONObject().apply {
            put("op", "advertise")
            put("topic", config.goalPoseTopic)
            put("type", "geometry_msgs/msg/PoseStamped")
        }
        webSocket?.send(goalPoseAdv.toString())
    }

    private fun subscribeTopics() {
        subscribeSingleTopic(config.batteryTopic, "sensor_msgs/msg/BatteryState")
        subscribeSingleTopic(config.amclPoseTopic, "geometry_msgs/msg/PoseWithCovarianceStamped")
        subscribeSingleTopic(config.slamPoseTopic, "geometry_msgs/msg/PoseWithCovarianceStamped")
        subscribeSingleTopic(config.planTopic, "nav_msgs/msg/Path")
        subscribeSingleTopic(config.mapTopic, "nav_msgs/msg/OccupancyGrid")

        // Nav2 Action Status feedback
        val statusSub = JSONObject().apply {
            put("op", "subscribe")
            put("topic", "/navigate_to_pose/_action/status")
        }
        webSocket?.send(statusSub.toString())
    }

    private fun subscribeSingleTopic(topicName: String, topicType: String) {
        if (topicName.isBlank()) return
        val sub = JSONObject().apply {
            put("op", "subscribe")
            put("topic", topicName)
            put("type", topicType)
        }
        webSocket?.send(sub.toString())
    }

    private fun handleIncomingMessage(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val topic = json.optString("topic")

            when (topic) {
                config.batteryTopic -> {
                    val msg = json.optJSONObject("msg")
                    if (msg != null) {
                        val rawPercentage = msg.optDouble("percentage", 0.5)
                        val percentageInt = if (rawPercentage <= 1.0) {
                            (rawPercentage * 100).toInt()
                        } else {
                            rawPercentage.toInt()
                        }

                        mainHandler.post {
                            batteryPercentage = percentageInt.coerceIn(0, 100)
                        }
                    }
                }

                config.amclPoseTopic, config.slamPoseTopic -> {
                    val msg = json.optJSONObject("msg")
                    if (msg != null) {
                        val poseObj = msg.optJSONObject("pose")
                        val finalPose = if (poseObj?.has("pose") == true) {
                            poseObj.optJSONObject("pose")
                        } else {
                            poseObj
                        }

                        if (finalPose != null) {
                            val pos = finalPose.optJSONObject("position")
                            val ori = finalPose.optJSONObject("orientation")

                            val x = pos?.optDouble("x", 0.0) ?: 0.0
                            val y = pos?.optDouble("y", 0.0) ?: 0.0
                            val qx = ori?.optDouble("x", 0.0) ?: 0.0
                            val qy = ori?.optDouble("y", 0.0) ?: 0.0
                            val qz = ori?.optDouble("z", 0.0) ?: 0.0
                            val qw = ori?.optDouble("w", 1.0) ?: 1.0

                            val sinyCosp = 2.0 * (qw * qz + qx * qy)
                            val cosyCosp = 1.0 - 2.0 * (qy * qy + qz * qz)
                            val yaw = atan2(sinyCosp, cosyCosp)

                            mainHandler.post {
                                robotX = x
                                robotY = y
                                robotYaw = yaw
                            }
                        }
                    }
                }

                config.planTopic -> {
                    val msg = json.optJSONObject("msg")
                    val poses = msg?.optJSONArray("poses")
                    if (poses != null) {
                        val points = mutableListOf<Pair<Double, Double>>()
                        for (i in 0 until poses.length()) {
                            val pos = poses.getJSONObject(i).optJSONObject("pose")?.optJSONObject("position")
                            if (pos != null) {
                                val x = pos.optDouble("x", 0.0)
                                val y = pos.optDouble("y", 0.0)
                                points.add(Pair(x, y))
                            }
                        }
                        mainHandler.post { globalPath = points }
                    }
                }

                config.mapTopic -> {
                    val msg = json.optJSONObject("msg")
                    val info = msg?.optJSONObject("info")
                    val dataArray = msg?.optJSONArray("data")

                    if (info != null && dataArray != null) {
                        val w = info.optInt("width", 0)
                        val h = info.optInt("height", 0)
                        val res = info.optDouble("resolution", 0.05)
                        val origin = info.optJSONObject("origin")?.optJSONObject("position")
                        val ox = origin?.optDouble("x", -10.0) ?: -10.0
                        val oy = origin?.optDouble("y", -10.0) ?: -10.0

                        val grid = IntArray(dataArray.length())
                        for (i in 0 until dataArray.length()) {
                            grid[i] = dataArray.optInt(i, -1)
                        }

                        mainHandler.post {
                            mapWidth = w
                            mapHeight = h
                            mapResolution = res
                            mapOriginX = ox
                            mapOriginY = oy
                            mapData = grid
                        }
                    }
                }

                "/navigate_to_pose/_action/status" -> {
                    val statusList = json.optJSONObject("msg")?.optJSONArray("status_list")
                    if (statusList != null && statusList.length() > 0) {
                        val latest = statusList.getJSONObject(statusList.length() - 1)
                        val statusVal = latest.optInt("status", 0)

                        mainHandler.post {
                            taskStatus = when (statusVal) {
                                1 -> "Task Received"
                                2 -> "En Route to Goal"
                                4 -> "Goal Reached! 🎉"
                                6 -> "Task Aborted / Cancelled"
                                else -> "Navigating..."
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendCmdVel(linearX: Double, angularZ: Double) {
        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", config.cmdVelTopic)
            put("msg", JSONObject().apply {
                put("linear", JSONObject().apply {
                    put("x", linearX)
                    put("y", 0.0)
                    put("z", 0.0)
                })
                put("angular", JSONObject().apply {
                    put("x", 0.0)
                    put("y", 0.0)
                    put("z", angularZ)
                })
            })
        }
        webSocket?.send(msg.toString())
    }

    fun sendNav2Goal(x: Double, y: Double, yaw: Double, destinationName: String = "Custom Pose") {
        currentDestinationName = destinationName
        taskStatus = "Task Dispatched..."

        val qz = sin(yaw / 2.0)
        val qw = cos(yaw / 2.0)

        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", config.goalPoseTopic)
            put("type", "geometry_msgs/msg/PoseStamped")
            put("msg", JSONObject().apply {
                put("header", JSONObject().apply {
                    put("frame_id", config.globalFrameId)
                })
                put("pose", JSONObject().apply {
                    put("position", JSONObject().apply {
                        put("x", x)
                        put("y", y)
                        put("z", 0.0)
                    })
                    put("orientation", JSONObject().apply {
                        put("x", 0.0)
                        put("y", 0.0)
                        put("z", qz)
                        put("w", qw)
                    })
                })
            })
        }
        webSocket?.send(msg.toString())
    }

    // Emergency Stop: Instantly halts motion, clears path line, & cancels Nav2 action
    fun triggerEmergencyStop() {
        mainHandler.post {
            taskStatus = "EMERGENCY STOP"
            globalPath = emptyList() // Clear local path line on canvas immediately
        }

        // Send a rapid burst of 0 velocities to immediately halt hardware execution
        repeat(3) {
            sendCmdVel(0.0, 0.0)
        }

        // Call Nav2 Goal Cancel Service
        val cancelGoalMsg = JSONObject().apply {
            put("op", "call_service")
            put("service", "/navigate_to_pose/_action/cancel_goal")
            put("args", JSONObject().apply {
                put("goal_info", JSONObject().apply {
                    put("goal_id", JSONObject().apply {
                        put("uuid", JSONArray())
                    })
                })
            })
        }

        try {
            webSocket?.send(cancelGoalMsg.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}