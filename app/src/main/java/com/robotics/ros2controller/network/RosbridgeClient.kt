package com.robotics.ros2controller.network

import android.os.Handler
import android.os.Looper
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

class RosbridgeClient {
    private var webSocket: WebSocket? = null

    // OkHttp Client configured with strict connection timeouts and automatic heartbeat (ping interval)
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)   // Fails fast if IP/Port is unreachable
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .pingInterval(3, TimeUnit.SECONDS)     // Automatically checks if Wi-Fi connection is still alive
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())

    var onConnectionStateChanged: ((Boolean, String?) -> Unit)? = null

    fun connect(ip: String, port: String) {
        // Disconnect existing socket safely before starting a new connection
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
            put("topic", "/cmd_vel")
            put("type", "geometry_msgs/msg/Twist")
        }
        webSocket?.send(cmdVelAdv.toString())

        val goalPoseAdv = JSONObject().apply {
            put("op", "advertise")
            put("topic", "/goal_pose")
            put("type", "geometry_msgs/msg/PoseStamped")
        }
        webSocket?.send(goalPoseAdv.toString())
    }

    fun sendCmdVel(linearX: Double, angularZ: Double) {
        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", "/cmd_vel")
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

    fun sendNav2Goal(x: Double, y: Double, yaw: Double) {
        val qz = sin(yaw / 2.0)
        val qw = cos(yaw / 2.0)

        val msg = JSONObject().apply {
            put("op", "publish")
            put("topic", "/goal_pose")
            put("type", "geometry_msgs/msg/PoseStamped")
            put("msg", JSONObject().apply {
                put("header", JSONObject().apply {
                    put("frame_id", "map")
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

    // Emergency Stop: Immediately publish 0 velocity and cancel active Nav2 goals
    fun triggerEmergencyStop() {
        // 1. Force Send Zero Velocity multiple times to guarantee twist_mux receives it
        sendCmdVel(0.0, 0.0)

        // 2. Publish action cancellation message to Nav2 over Rosbridge service
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