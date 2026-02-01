package com.example.sysmonitor

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.FileReader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF00D9FF),
                    background = Color(0xFF0F1115),
                    surface = Color(0xFF1A1D24)
                )
            ) {
                Surface(color = Color(0xFF0F1115)) {
                    MonitorScreen()
                }
            }
        }
    }
}

@Composable
fun MonitorScreen() {
    val context = LocalContext.current
    var cpu by remember { mutableFloatStateOf(0f) }
    var ramUsed by remember { mutableLongStateOf(0L) }
    var ramTotal by remember { mutableLongStateOf(0L) }
    var cpuTemp by remember { mutableFloatStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while(true) {
            // Get CPU usage
            cpu = getCpuUsage()
            
            // Get Memory using ActivityManager (works without root)
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            am.getMemoryInfo(memInfo)
            ramTotal = memInfo.totalMem / (1024 * 1024) // MB
            ramUsed = ramTotal - (memInfo.availMem / (1024 * 1024))
            
            // Get CPU temperature
            cpuTemp = getCpuTemp()
            
            delay(1000)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("System Monitor", fontSize = 32.sp, color = Color.White)
        Spacer(modifier = Modifier.height(30.dp))
        
        // CPU Card
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("CPU Usage", color = Color.Gray, fontSize = 16.sp)
                Text("${cpu.toInt()}%", color = Color(0xFF00D9FF), fontSize = 40.sp)
                LinearProgressIndicator(
                    progress = cpu/100f, 
                    modifier = Modifier.fillMaxWidth().padding(top=8.dp), 
                    color = Color(0xFF00D9FF)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Memory Card
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Memory", color = Color.Gray, fontSize = 16.sp)
                val usedGB = ramUsed / 1024f
                val totalGB = ramTotal / 1024f
                Text(
                    String.format("%.1fGB / %.1fGB", usedGB, totalGB), 
                    color = Color(0xFF6C63FF), 
                    fontSize = 24.sp
                )
                val progress = if(ramTotal > 0) ramUsed.toFloat()/ramTotal.toFloat() else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().padding(top=8.dp),
                    color = Color(0xFF6C63FF)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Temperature Card
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("CPU Temperature", color = Color.Gray, fontSize = 16.sp)
                val tempText = if(cpuTemp > 0) String.format("%.1f°C", cpuTemp) else "N/A"
                val tempColor = when {
                    cpuTemp > 70 -> Color.Red
                    cpuTemp > 50 -> Color(0xFFFF6D00)
                    else -> Color(0xFF00E676)
                }
                Text(tempText, color = tempColor, fontSize = 28.sp)
            }
        }
    }
}

private var lastCpuTime: Long = 0
private var lastAppCpuTime: Long = 0

fun getCpuUsage(): Float {
    return try {
        // Method 1: Try reading /proc/stat (may fail on some devices)
        val reader = BufferedReader(FileReader("/proc/stat"))
        val line = reader.readLine()
        reader.close()
        
        if(line.startsWith("cpu ")) {
            val parts = line.split(" ").filter { it.isNotEmpty() }.drop(1).map { it.toLong() }
            if(parts.size >= 4) {
                val user = parts[0]
                val nice = parts[1]
                val system = parts[2]
                val idle = parts[3]
                val total = user + nice + system + idle
                
                if(lastCpuTime > 0) {
                    val diffTotal = total - lastCpuTime
                    val diffIdle = idle - lastIdleTime
                    if(diffTotal > 0) {
                        val usage = ((diffTotal - diffIdle).toFloat() / diffTotal * 100)
                        lastCpuTime = total
                        lastIdleTime = idle
                        return usage.coerceIn(0f, 100f)
                    }
                }
                lastCpuTime = total
                lastIdleTime = idle
            }
        }
        0f
    } catch(e: Exception) {
        // Fallback: Use app-specific CPU usage if system-wide is restricted
        getAppCpuUsage()
    }
}

private var lastIdleTime: Long = 0

fun getAppCpuUsage(): Float {
    // Calculate CPU usage for this app only as fallback
    val pid = Process.myPid()
    try {
        val reader = BufferedReader(FileReader("/proc/$pid/stat"))
        val line = reader.readLine()
        reader.close()
        val parts = line.split(" ")
        if(parts.size > 13) {
            val utime = parts[13].toLong()
            val stime = parts[14].toLong()
            val current = utime + stime
            
            if(lastAppCpuTime > 0) {
                val diff = current - lastAppCpuTime
                // Rough estimate: assume 100 ticks per second per core
                val usage = (diff / 100f).coerceIn(0f, 100f)
                lastAppCpuTime = current
                return usage
            }
            lastAppCpuTime = current
        }
    } catch(e: Exception) {}
    return 0f
}

fun getCpuTemp(): Float {
    val paths = arrayOf(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/devices/virtual/thermal/thermal_zone0/temp"
    )
    for(path in paths) {
        try {
            val reader = BufferedReader(FileReader(path))
            val temp = reader.readLine().toFloat() / 1000f
            reader.close()
            if(temp > 0) return temp
        } catch(e: Exception) {}
    }
    return 0f
}
