package com.example.dpicontroller

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.unit.dp
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
            DpiControllerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DpiControllerApp()
                }
            }
        }
    }
}

@Composable
fun DpiControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFD0BCFF),
            secondary = Color(0xFFCCC2DC),
            tertiary = Color(0xFFEFB8C8)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpiControllerApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val baseUrl = remember { mutableStateOf("http://192.168.10.238:5000") }
    
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

    fun fetchData() {
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

    LaunchedEffect(api) {
        fetchData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DPI Controller") },
                actions = {
                    IconButton(onClick = { fetchData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    statusMsg = "Saving..."
                                    val res = withContext(Dispatchers.IO) { api?.saveConfig(config) }
                                    if (res != null && res.isSuccessful) {
                                        statusMsg = "Saved!"
                                        Toast.makeText(context, "Config Saved!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        statusMsg = "Save Failed"
                                    }
                                } catch (e: Exception) {
                                    statusMsg = "Error: ${e.message}"
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                try {
                                    api?.reboot()
                                    statusMsg = "Rebooting..."
                                } catch (e: Exception) {}
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.weight(1f).padding(8.dp)
                    ) {
                        Text("Reboot")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Connection", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = baseUrl.value, 
                        onValueChange = { baseUrl.value = it },
                        label = { Text("Server URL") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Status: $statusMsg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text("Frame Preview (Drag to adjust porches)", style = MaterialTheme.typography.titleMedium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 8.dp)
                    .background(Color.Black, shape = MaterialTheme.shapes.medium)
            ) {
                InteractiveDpiCanvas(config = config, onConfigChange = { config = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            SettingsSection("Horizontal Timing") {
                ConfigInput("Active", config.hActive) { config = config.copy(hActive = it) }
                ConfigInput("Front Porch", config.hFp) { config = config.copy(hFp = it) }
                ConfigInput("Sync", config.hSync) { config = config.copy(hSync = it) }
                ConfigInput("Back Porch", config.hBp) { config = config.copy(hBp = it) }
            }

            SettingsSection("Vertical Timing") {
                ConfigInput("Active", config.vActive) { config = config.copy(vActive = it) }
                ConfigInput("Front Porch", config.vFp) { config = config.copy(vFp = it) }
                ConfigInput("Sync", config.vSync) { config = config.copy(vSync = it) }
                ConfigInput("Back Porch", config.vBp) { config = config.copy(vBp = it) }
            }

            SettingsSection("Color & Clock") {
                ConfigInput("Clock (Hz)", config.clockFreq) { config = config.copy(clockFreq = it) }
                
                DropdownSetting("Color Format", config.colorFormat, listOf("rgb565", "rgb666", "rgb888")) {
                    config = config.copy(colorFormat = it)
                }
                
                DropdownSetting("Color Mode", config.colorMode, listOf("default", "inverted", "grayscale")) {
                    config = config.copy(colorMode = it)
                }

                Text("Temperature: ${config.temperature}K", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = config.temperature.toFloat(),
                    onValueChange = { config = config.copy(temperature = it.toInt()) },
                    valueRange = 1000f..12000f,
                    steps = 110
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSetting(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ConfigInput(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, modifier = Modifier.width(100.dp), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { onChange(it.toIntOrNull() ?: 0) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun InteractiveDpiCanvas(config: DpiConfig, onConfigChange: (DpiConfig) -> Unit) {
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
                        
                        val dxUnits = (dragAmount.x / scaleX).toInt()
                        val dyUnits = (dragAmount.y / scaleY).toInt()
                        
                        var newConfig = config.copy()
                        var changed = false

                        if (dxUnits != 0) {
                            val newHbp = config.hBp + dxUnits
                            val newHfp = config.hFp - dxUnits
                            if (newHbp >= 0 && newHfp >= 0) {
                                newConfig = newConfig.copy(hBp = newHbp, hFp = newHfp)
                                changed = true
                            }
                        }
                        
                        if (dyUnits != 0) {
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

            val xSyncStart = margin
            val xSyncEnd = xSyncStart + config.hSync * scaleX
            val xBpEnd = xSyncEnd + config.hBp * scaleX
            val xActEnd = xBpEnd + config.hActive * scaleX

            val ySyncStart = margin
            val ySyncEnd = ySyncStart + config.vSync * scaleY
            val yBpEnd = ySyncEnd + config.vBp * scaleY
            val yActEnd = yBpEnd + config.vActive * scaleY

            drawRect(
                color = Color.DarkGray,
                topLeft = Offset(margin, margin),
                size = Size(availW, availH),
                style = Stroke(width = 1f)
            )

            drawRect(
                color = Color(0x44FF0000),
                topLeft = Offset(xSyncStart, margin),
                size = Size(xSyncEnd - xSyncStart, availH)
            )
            drawRect(
                color = Color(0x44FF0000),
                topLeft = Offset(margin, ySyncStart),
                size = Size(availW, ySyncEnd - ySyncStart)
            )

            drawRect(
                color = Color(0xFF2196F3),
                topLeft = Offset(xBpEnd, yBpEnd),
                size = Size(xActEnd - xBpEnd, yActEnd - yBpEnd)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(xBpEnd, yBpEnd),
                size = Size(xActEnd - xBpEnd, yActEnd - yBpEnd),
                style = Stroke(width = 2f)
            )
        }
    }
}