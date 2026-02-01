package com.example.sysmonitor

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.RandomAccessFile
import androidx.compose.ui.graphics.Path

// Colors
val DarkBackground = Color(0xFF0F1115)
val CardBackground = Color(0xFF1A1D24)
val AccentBlue = Color(0xFF00D9FF)
val AccentPurple = Color(0xFF6C63FF)
val AccentGreen = Color(0xFF00E676)
val AccentOrange = Color(0xFFFF6D00)

data class SystemStats(
    val cpuUsage: Float = 0f,
    val memoryUsed: Long = 0,
    val memoryTotal: Long = 0,
    val gpuFrequency: Int = 0,
    val cpuTemperature: Float = 0f
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AccentBlue,
                    secondary = AccentPurple,
                    background = DarkBackground,
                    surface = CardBackground
                )
            ) {
                Surface(color = DarkBackground) {
                    SystemMonitorScreen()
                }
            }
        }
    }
}

@Composable
fun SystemMonitorScreen() {
    var stats by remember { mutableStateOf(SystemStats()) }
    var history by remember { mutableStateOf(listOf<Float>()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            val newStats = readSystemStats()
            stats = newStats
            
            val newHistory = history.toMutableList()
            newHistory.add(newStats.cpuUsage)
            if(newHistory.size > 20) newHistory.removeAt(0)
            history = newHistory
            
            kotlinx.coroutines.delay(1000)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "System Monitor",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        // CPU Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("CPU Usage", color = Color.Gray, fontSize = 16.sp)
                    Text("${stats.cpuUsage.toInt()}%", color = AccentBlue, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgress(
                        progress = stats.cpuUsage / 100f,
                        color = AccentBlue,
                        modifier = Modifier.size(120.dp)
                    )
                }
                
                if(history.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Sparkline(data = history, color = AccentBlue, modifier = Modifier.fillMaxWidth().height(40.dp))
                }
            }
        }
        
        // Memory Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Memory", color = Color.Gray, fontSize = 16.sp)
                    val pct = if(stats.memoryTotal > 0) ((stats.memoryUsed.toFloat()/stats.memoryTotal.toFloat())*100).toInt() else 0
                    Text("$pct%", color = AccentPurple, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                val progress = if(stats.memoryTotal > 0) stats.memoryUsed.toFloat()/stats.memoryTotal.toFloat() else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                    color = AccentPurple,
                    trackColor = Color.DarkGray.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("${stats.memoryUsed}MB / ${stats.memoryTotal}MB", color = Color.Gray, fontSize = 14.sp)
            }
        }
        
        // GPU Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("GPU Frequency", color = Color.Gray, fontSize = 16.sp)
                    Text("${stats.gpuFrequency} MHz", color = AccentOrange, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier.size(60.dp).background(AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(30.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GPU", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Temperature Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            Row(
                modifier = Modifier.padding(20.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CPU Temperature", color = Color.Gray, fontSize = 16.sp)
                    val tempText = if(stats.cpuTemperature > 0) "${stats.cpuTemperature}°C" else "N/A"
                    val tempColor = when {
                        stats.cpuTemperature > 70 -> Color.Red
                        stats.cpuTemperature > 50 -> AccentOrange
                        else -> AccentGreen
                    }
                    Text(tempText, color = tempColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CircularProgress(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")
    Canvas(modifier = modifier) {
        val strokeWidth = 12f
        drawArc(
            color = Color.DarkGray.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = animatedProgress * 360f,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun Sparkline(data: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if(data.size < 2) return@Canvas
        val width = size.width
        val height = size.height
        val max = data.maxOrNull() ?: 100f
        val min = data.minOrNull() ?: 0f
        val range = max - min
        if(range == 0f) return@Canvas
        
        val stepX = width / (data.size - 1)
        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - min) / range * height)
                if(index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(path = path, color = color, style = Stroke(3f, cap = StrokeCap.Round))
    }
}

fun readSystemStats(): SystemStats {
    return SystemStats(
        cpuUsage = readCpuUsage(),
        memoryUsed = readMemory().first,
        memoryTotal = readMemory().second,
        gpuFrequency = readGpuFreq(),
        cpuTemperature = readTemp()
    )
}

fun readCpuUsage(): Float {
    return try {
        val reader = RandomAccessFile("/proc/stat", "r")
        val line = reader.readLine()
        reader.close()
        val parts = line.split(" ").filter { it.isNotEmpty() }.drop(1).map { it.toLong() }
        if(parts.size >= 4) {
            val active = parts[0] + parts[1] + parts[2]
            val total = active + parts[3]
            if(total > 0) (active.toFloat() / total.toFloat() * 100) else 0f
        } else 0f
    } catch(e: Exception) { 0f }
}

fun readMemory(): Pair<Long, Long> {
    // This is a placeholder - in real app you'd use ActivityManager
    return Pair(4000, 8000)
}

fun readGpuFreq(): Int {
    val paths = arrayOf(
        "/sys/class/kgsl/kgsl-3d0/gpuclk",
        "/sys/class/misc/mali0/device/clock"
    )
    for(path in paths) {
        try {
            val reader = RandomAccessFile(path, "r")
            val freq = reader.readLine().toInt() / 1000000
            reader.close()
            if(freq > 0) return freq
        } catch(e: Exception) {}
    }
    return 0
}

fun readTemp(): Float {
    val paths = arrayOf(
        "/sys/class/thermal/thermal_zone0/temp"
    )
    for(path in paths) {
        try {
            val reader = RandomAccessFile(path, "r")
            val temp = reader.readLine().toFloat() / 1000f
            reader.close()
            return temp
        } catch(e: Exception) {}
    }
    return 0f
}
