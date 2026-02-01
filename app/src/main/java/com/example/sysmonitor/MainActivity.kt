import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val repository = SystemStatsRepository(this)
        val factory = MonitorViewModelFactory(repository)
        
        setContent {
            SystemMonitorTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SystemMonitorScreen(factory = factory)
                }
            }
        }
    }
}

@Composable
fun SystemMonitorScreen(factory: MonitorViewModelFactory) {
    val viewModel: MonitorViewModel = viewModel(factory = factory)
    val stats by viewModel.stats.collectAsState()
    val cpuHistory by viewModel.cpuHistory.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "System Monitor",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        item {
            CPUMonitorCard(stats, cpuHistory)
        }
        
        item {
            MemoryMonitorCard(stats)
        }
        
        item {
            GPUMonitorCard(stats)
        }
        
        item {
            TemperatureCard(stats)
        }
        
        item {
            CoreFrequencies(stats)
        }
    }
}

@Composable
fun CPUMonitorCard(stats: SystemStats, history: List<Float>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CPU Usage",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${stats.cpuUsage.roundToInt()}%",
                        color = AccentBlue,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Circular Progress with Animation
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedCircularProgress(
                        progress = stats.cpuUsage / 100f,
                        color = AccentBlue,
                        modifier = Modifier.size(140.dp)
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${stats.processes}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Processes",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
                
                // Mini Chart
                if (history.isNotEmpty()) {
                    SparkLine(
                        data = history,
                        color = AccentBlue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryMonitorCard(stats: SystemStats) {
    val usagePercent = if (stats.memoryTotal > 0) {
        (stats.memoryUsed.toFloat() / stats.memoryTotal.toFloat())
    } else 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Memory",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${(usagePercent * 100).roundToInt()}%",
                    color = AccentPurple,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = usagePercent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = AccentPurple,
                trackColor = Color.DarkGray.copy(alpha = 0.3f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MemoryStat("Used", "${stats.memoryUsed} MB", AccentPurple)
                MemoryStat("Total", "${stats.memoryTotal} MB", Color.Gray)
                MemoryStat("Free", "${stats.memoryTotal - stats.memoryUsed} MB", AccentGreen)
            }
        }
    }
}

@Composable
fun GPUMonitorCard(stats: SystemStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "GPU",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${stats.gpuFrequency} MHz",
                        color = AccentOrange,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        "Current Frequency",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                // GPU Visual Representation
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(AccentOrange.copy(alpha = 0.4f), Color.Transparent)
                            ),
                            shape = RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("GPU", color = AccentOrange, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TemperatureCard(stats: SystemStats) {
    val tempColor = when {
        stats.cpuTemperature > 70 -> Color(0xFFFF4444)
        stats.cpuTemperature > 50 -> AccentOrange
        else -> AccentGreen
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "CPU Temperature",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    if (stats.cpuTemperature > 0) "${stats.cpuTemperature}°C" else "N/A",
                    color = tempColor,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Thermal Indicator
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        color = tempColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "°C",
                    color = tempColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CoreFrequencies(stats: SystemStats) {
    if (stats.cpuFreq.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                "Core Frequencies",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            stats.cpuFreq.chunked(2).forEachIndexed { index, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    pair.forEachIndexed { coreIndex, freq ->
                        val coreNum = index * 2 + coreIndex
                        CoreFreqItem(coreNum, freq, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun CoreFreqItem(core: Int, freq: Int, modifier: Modifier) {
    Row(
        modifier = modifier
            .background(Color.DarkGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Core $core",
            color = Color.Gray,
            fontSize = 12.sp
        )
        Text(
            if (freq > 0) "$freq MHz" else "Offline",
            color = if (freq > 2000) AccentGreen else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MemoryStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
fun AnimatedCircularProgress(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 12f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )
    
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sweepAngle = animatedProgress * 360f
        
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
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun SparkLine(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return
    
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val max = data.maxOrNull() ?: 100f
        val min = data.minOrNull() ?: 0f
        val range = max - min
        
        val stepX = width / (data.size - 1)
        
        val path = androidx.compose.ui.graphics.Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - ((value - min) / range * height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(3f, cap = StrokeCap.Round)
        )
        
        // Gradient fill
        val fillPath = androidx.compose.ui.graphics.Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
    }
}
