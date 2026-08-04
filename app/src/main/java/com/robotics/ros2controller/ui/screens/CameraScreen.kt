package com.robotics.ros2controller.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CameraScreen(
    ipAddress: String,
    isConnected: Boolean,
    cameraTopic: String = "/camera/camera_sensor/image_raw", // Default Gazebo camera topic
    streamPort: String = "8080"
) {
    var isStreamActive by remember { mutableStateOf(true) }

    // Construct web_video_server MJPEG stream URL
    val streamUrl = "http://$ipAddress:$streamPort/stream?topic=$cameraTopic"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16)) // Deep Dark Slate Background
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    Column {
                        Text(
                            text = "Live Camera Feed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Topic: $cameraTopic",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // Toggle Stream Button
                    IconButton(onClick = { isStreamActive = !isStreamActive }) {
                        Icon(
                            imageVector = if (isStreamActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Camera",
                            tint = if (isStreamActive) Color(0xFF38BDF8) else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continuous MJPEG Stream Viewport
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color(0xFF0F172A), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnected && isStreamActive) {
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
                                    val html = "<html><head><style>body{margin:0;padding:0;background-color:black;display:flex;justify-content:center;align-items:center;height:100vh;} img{max-width:100%;max-height:100%;object-fit:contain;}</style></head><body><img src=\"$streamUrl\" /></body></html>"
                                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                                }
                            },
                            update = { webView ->
                                val html = "<html><head><style>body{margin:0;padding:0;background-color:black;display:flex;justify-content:center;align-items:center;height:100vh;} img{max-width:100%;max-height:100%;object-fit:contain;}</style></head><body><img src=\"$streamUrl\" /></body></html>"
                                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.VideocamOff,
                                contentDescription = "Camera Off",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!isConnected) "Robot Disconnected" else "Stream Paused",
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Stream Source: $streamUrl",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}