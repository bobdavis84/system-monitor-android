package com.example.sysmonitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.RandomAccessFile
import kotlinx.coroutines.delay

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
    var cpu by remember { mutableFloatStateOf(0f) }
    var ram by remember { mutableStateOf("Loading...") }
    
    LaunchedEffect(Unit) {
        while(true) {
            cpu = readCpu()
            ram = "8GB / 16GB" // Simplified for now
            delay(1000)
        }
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("System Monitor", fontSize = 32.sp, color = Color.White)
        Spacer(modifier = Modifier.height(30.dp))
        
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("CPU Usage", color = Color.Gray, fontSize = 16.sp)
                Text("${cpu.toInt()}%", color = Color(0xFF00D9FF), fontSize = 40.sp)
                LinearProgressIndicator(progress = cpu/100f, modifier = Modifier.fillMaxWidth().padding(top=8.dp), color = Color(0xFF00D9FF))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Memory", color = Color.Gray, fontSize = 16.sp)
                Text(ram, color = Color(0xFF6C63FF), fontSize = 24.sp)
            }
        }
    }
}

fun readCpu(): Float {
    return try {
        val r = RandomAccessFile("/proc/stat", "r")
        val line = r.readLine()
        r.close()
        val nums = line.split(" ").filter{it.isNotEmpty()}.drop(1).map{it.toLong()}
        if(nums.size >= 4) {
            val active = nums[0] + nums[1] + nums[2]
            val idle = nums[3]
            val total = active + idle
            if(total > 0) (active.toFloat() / total * 100) else 0f
        } else 0f
    } catch(e: Exception) { 0f }
}
