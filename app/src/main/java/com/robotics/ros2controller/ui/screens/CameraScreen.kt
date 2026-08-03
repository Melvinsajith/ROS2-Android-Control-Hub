package com.robotics.ros2controller.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.robotics.ros2controller.ui.theme.Reference_Text_Secondary

@Composable
fun CameraScreen(
    ipAddress: String,
    isConnected: Boolean,
    cameraTopic: String = "/camera/image_raw",
    streamPort: String = "8080"
) {
    var isStreamActive by remember { mutableStateOf(true) }

    // Construct web_video_server MJPEG stream URL
    val streamUrl = "http://$ipAddress:$streamPort/stream?topic=$cameraTopic"

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    Column {
                        Text(
                            text = "Live Camera Feed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Topic: $cameraTopic",
                            fontSize = 12.sp,
                            color = Reference_Text_Secondary
                        )
                    }

                    // Toggle Stream
                    IconButton(onClick = { isStreamActive = !isStreamActive }) {
                        Icon(
                            imageVector = if (isStreamActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Camera",
                            tint = if (isStreamActive) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // MJPEG Video Viewport Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color.Black, MaterialTheme.shapes.medium),
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
                                    // Raw HTML wrapping to fit the MJPEG stream to view dimensions
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
                                tint = Color.DarkGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!isConnected) "Robot Disconnected" else "Stream Paused",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Stream Source: $streamUrl",
                    fontSize = 11.sp,
                    color = Reference_Text_Secondary
                )
            }
        }
    }
}