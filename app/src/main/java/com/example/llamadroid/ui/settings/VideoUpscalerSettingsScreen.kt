package com.example.llamadroid.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.llamadroid.data.SettingsRepository

/**
 * Video Upscaler Settings - Thread controls for load/process/save
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoUpscalerSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    
    val loadThreads by settingsRepo.upscalerLoadThreads.collectAsState()
    val procThreads by settingsRepo.upscalerProcThreads.collectAsState()
    val saveThreads by settingsRepo.upscalerSaveThreads.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Video Upscaler Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Load Threads
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📥 Load Threads", fontWeight = FontWeight.Bold)
                            Text("$loadThreads", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = loadThreads.toFloat(),
                            onValueChange = { settingsRepo.setUpscalerLoadThreads(it.toInt()) },
                            valueRange = 1f..4f,
                            steps = 2
                        )
                        Text(
                            "Threads for loading video frames",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Process Threads
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚙️ Process Threads", fontWeight = FontWeight.Bold)
                            Text("$procThreads", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = procThreads.toFloat(),
                            onValueChange = { settingsRepo.setUpscalerProcThreads(it.toInt()) },
                            valueRange = 1f..4f,
                            steps = 2
                        )
                        Text(
                            "Threads for GPU/CPU upscaling",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Save Threads
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("💾 Save Threads", fontWeight = FontWeight.Bold)
                            Text("$saveThreads", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = saveThreads.toFloat(),
                            onValueChange = { settingsRepo.setUpscalerSaveThreads(it.toInt()) },
                            valueRange = 1f..4f,
                            steps = 2
                        )
                        Text(
                            "Threads for saving upscaled frames",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "ℹ️ Thread Optimization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Load/Save threads handle disk I/O\n" +
                            "• Process threads handle the actual upscaling\n" +
                            "• Lower values = less memory, slower processing\n" +
                            "• Higher values = more memory, faster processing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
