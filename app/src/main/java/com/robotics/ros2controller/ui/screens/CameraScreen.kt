package com.robotics.ros2controller.ui.screens

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.robotics.ros2controller.ui.theme.Reference_Text_Secondary

@Composable
fun CameraScreen(
    ipAddress: String,
    isConnected: Boolean,
    cameraTopic: String = "/camera/image_raw",
    streamPort: String = "8080"
) {
    var isStreamActive by remember { mutableStateOf(true) }
    val context = LocalContext.current

    // Construct the web_video_server stream URL
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

                    // Toggle Video Stream Button
                    IconButton(onClick = { isStreamActive = !isStreamActive }) {
                        Icon(
                            imageVector = if (isStreamActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Toggle Camera",
                            tint = if (isStreamActive) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Video Viewport Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color.Black, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    if (isConnected && isStreamActive) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(streamUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "AMR Live Feed",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
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