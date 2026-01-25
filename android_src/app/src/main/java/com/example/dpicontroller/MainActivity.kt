package com.example.dpicontroller

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dpicontroller.model.DpiConfig
import com.example.dpicontroller.network.DpiApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DpiControllerApp()
            }
        }
    }
}

@Composable
fun DpiControllerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // CHANGE THIS IP TO YOUR PI'S IP ADDRESS
    val baseUrl = remember { mutableStateOf("http://192.168.1.100:5000") }
    
    var config by remember { mutableStateOf(DpiConfig()) }
    var statusMsg by remember { mutableStateOf("Idle") }

    val api = remember(baseUrl.value) {
        try {
            Retrofit.Builder()
                .baseUrl(baseUrl.value)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DpiApi::class.java)
        } catch (e: Exception) {
            null
        }
    }

    LaunchedEffect(Unit) {
        // Initial Fetch
        scope.launch {
            try {
                statusMsg = "Fetching..."
                val res = withContext(Dispatchers.IO) { api?.getConfig() }
                if (res != null && res.isSuccessful && res.body() != null) {
                    config = res.body()!!
                    statusMsg = "Loaded"
                } else {
                    statusMsg = "Fetch Failed"
                }
            } catch (e: Exception) {
                statusMsg = "Error: ${e.message}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("DPI Controller", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        
        OutlinedTextField(
            value = baseUrl.value, 
            onValueChange = { baseUrl.value = it },
            label = { Text("Pi IP Address (e.g. http://192.168.1.xxx:5000)") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        
        Text("Status: $statusMsg", color = Color.Gray, fontSize = 12.sp)

        // Visualizer (Top Half)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFF333333))
                .padding(8.dp)
        ) {
            InteractiveDpiCanvas(config = config, onConfigChange = { newConfig ->
                config = newConfig
            })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls (Bottom Half - Scrollable)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            
            // --- Buttons ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = {
                    scope.launch {
                        try {
                            statusMsg = "Saving..."
                            val res = withContext(Dispatchers.IO) { api?.saveConfig(config) }
                            if (res != null && res.isSuccessful) {
                                statusMsg = "Saved! Reboot needed."
                                Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                statusMsg = "Save Failed"
                            }
                        } catch (e: Exception) {
                            statusMsg = "Error: ${e.message}"
                        }
                    }
                }) { Text("Save to Pi") }

                Button(onClick = {
                    scope.launch {
                        try {
                            api?.reboot()
                            statusMsg = "Rebooting..."
                        } catch (e: Exception) {}
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Reboot") }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // --- Sliders / Inputs ---
            Text("Horizontal", fontWeight = FontWeight.Bold)
            ConfigInput("Active", config.hActive) { config = config.copy(hActive = it) }
            ConfigInput("Front Porch", config.hFp) { config = config.copy(hFp = it) }
            ConfigInput("Sync", config.hSync) { config = config.copy(hSync = it) }
            ConfigInput("Back Porch", config.hBp) { config = config.copy(hBp = it) }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Vertical", fontWeight = FontWeight.Bold)
            ConfigInput("Active", config.vActive) { config = config.copy(vActive = it) }
            ConfigInput("Front Porch", config.vFp) { config = config.copy(vFp = it) }
            ConfigInput("Sync", config.vSync) { config = config.copy(vSync = it) }
            ConfigInput("Back Porch", config.vBp) { config = config.copy(vBp = it) }

            Spacer(modifier = Modifier.height(8.dp))
            ConfigInput("Clock (Hz)", config.clockFreq) { config = config.copy(clockFreq = it) }
        }
    }
}

@Composable
fun ConfigInput(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(100.dp))
        // Simple TextField for now, easy to type numbers
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { onChange(it.toIntOrNull() ?: 0) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun InteractiveDpiCanvas(config: DpiConfig, onConfigChange: (DpiConfig) -> Unit) {
    // Logic similar to Python canvas
    // We render the total frame, and allow dragging the active area to adjust Porches.
    
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    
                    val hTotal = (config.hActive + config.hFp + config.hSync + config.hBp).toFloat()
                    val vTotal = (config.vActive + config.vFp + config.vSync + config.vBp).toFloat()
                    
                    if (hTotal > 0 && vTotal > 0 && canvasSize.width > 0) {
                        val margin = 20f
                        val availW = canvasSize.width - 2 * margin
                        val availH = canvasSize.height - 2 * margin
                        
                        val scaleX = availW / hTotal
                        val scaleY = availH / vTotal
                        
                        // Convert drag pixels to units
                        val dxUnits = (dragAmount.x / scaleX).toInt()
                        val dyUnits = (dragAmount.y / scaleY).toInt()
                        
                        var newConfig = config.copy()
                        var changed = false

                        if (dxUnits != 0) {
                            // Drag Right -> Increase HBP, Decrease HFP
                            val newHbp = config.hBp + dxUnits
                            val newHfp = config.hFp - dxUnits
                            if (newHbp >= 0 && newHfp >= 0) {
                                newConfig = newConfig.copy(hBp = newHbp, hFp = newHfp)
                                changed = true
                            }
                        }
                        
                        if (dyUnits != 0) {
                            // Drag Down -> Increase VBP, Decrease VFP
                            val newVbp = config.vBp + dyUnits
                            val newVfp = config.vFp - dyUnits
                            if (newVbp >= 0 && newVfp >= 0) {
                                newConfig = newConfig.copy(vBp = newVbp, vFp = newVfp)
                                changed = true
                            }
                        }
                        
                        if (changed) {
                            onConfigChange(newConfig)
                        }
                    }
                }
            }
    ) {
        canvasSize = size
        val w = size.width
        val h = size.height
        val margin = 20f
        
        val hTotal = config.hActive + config.hFp + config.hSync + config.hBp
        val vTotal = config.vActive + config.vFp + config.vSync + config.vBp

        if (hTotal > 0 && vTotal > 0) {
            val availW = w - 2 * margin
            val availH = h - 2 * margin
            val scaleX = availW / hTotal
            val scaleY = availH / vTotal

            // Coordinates
            val xSyncStart = margin
            val xSyncEnd = xSyncStart + config.hSync * scaleX
            val xBpEnd = xSyncEnd + config.hBp * scaleX
            val xActEnd = xBpEnd + config.hActive * scaleX
            val xFpEnd = xActEnd + config.hFp * scaleX // Should match margin + availW

            val ySyncStart = margin
            val ySyncEnd = ySyncStart + config.vSync * scaleY
            val yBpEnd = ySyncEnd + config.vBp * scaleY
            val yActEnd = yBpEnd + config.vActive * scaleY
            val yFpEnd = yActEnd + config.vFp * scaleY

            // 1. Total Frame Outline
            drawRect(
                color = Color.Gray,
                topLeft = Offset(margin, margin),
                size = Size(availW, availH),
                style = Stroke(width = 2f)
            )

            // 2. Sync (Redish)
            // H-Sync Stripe
            drawRect(
                color = Color(0xFF442222),
                topLeft = Offset(xSyncStart, margin),
                size = Size(xSyncEnd - xSyncStart, availH)
            )
            // V-Sync Stripe
            drawRect(
                color = Color(0xFF442222),
                topLeft = Offset(margin, ySyncStart),
                size = Size(availW, ySyncEnd - ySyncStart)
            )

            // 3. Active Area (Blue)
            drawRect(
                color = Color(0xFF225588),
                topLeft = Offset(xBpEnd, yBpEnd),
                size = Size(xActEnd - xBpEnd, yActEnd - yBpEnd)
            )
            drawRect(
                color = Color(0xFF4488AA),
                topLeft = Offset(xBpEnd, yBpEnd),
                size = Size(xActEnd - xBpEnd, yActEnd - yBpEnd),
                style = Stroke(width = 3f)
            )
        }
    }
}
