package com.example.llamadroid.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.llamadroid.data.SettingsRepository
import com.example.llamadroid.data.db.AppDatabase
import com.example.llamadroid.data.db.ModelType

/**
 * PDF AI Summary Settings Screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val db = remember { AppDatabase.getDatabase(context) }
    
    // Settings state
    val pdfModelPath by settingsRepo.pdfModelPath.collectAsState()
    val pdfContextSize by settingsRepo.pdfContextSize.collectAsState()
    val pdfThreads by settingsRepo.pdfThreads.collectAsState()
    val pdfTemperature by settingsRepo.pdfTemperature.collectAsState()
    val pdfMaxTokens by settingsRepo.pdfMaxTokens.collectAsState()
    
    // Available LLM models
    val llmModels by db.modelDao().getModelsByType(ModelType.LLM).collectAsState(initial = emptyList())
    
    var showModelPicker by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF AI Settings") },
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
            // Model Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🤖 Summary Model",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select a dedicated LLM for PDF summarization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showModelPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                pdfModelPath?.substringAfterLast("/") ?: "Use Chat Model (default)",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // Context Size
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
                            Text("📏 Context Size", fontWeight = FontWeight.Bold)
                            Text("$pdfContextSize tokens", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = pdfContextSize.toFloat(),
                            onValueChange = { settingsRepo.setPdfContextSize(it.toInt()) },
                            valueRange = 2048f..32768f,
                            steps = 7
                        )
                        Text(
                            "Larger context allows processing more text at once",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Threads
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
                            Text("⚡ CPU Threads", fontWeight = FontWeight.Bold)
                            Text("$pdfThreads", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = pdfThreads.toFloat(),
                            onValueChange = { settingsRepo.setPdfThreads(it.toInt()) },
                            valueRange = 1f..16f,
                            steps = 14
                        )
                        Text(
                            "More threads = faster processing, but higher CPU usage",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Temperature
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
                            Text("🌡️ Temperature", fontWeight = FontWeight.Bold)
                            Text("%.1f".format(pdfTemperature), color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = pdfTemperature,
                            onValueChange = { settingsRepo.setPdfTemperature(it) },
                            valueRange = 0f..1f
                        )
                        Text(
                            "Lower = more focused, Higher = more creative",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Max Tokens
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
                            Text("📝 Max Output Tokens", fontWeight = FontWeight.Bold)
                            Text("$pdfMaxTokens", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = pdfMaxTokens.toFloat(),
                            onValueChange = { settingsRepo.setPdfMaxTokens(it.toInt()) },
                            valueRange = 256f..4096f,
                            steps = 14
                        )
                        Text(
                            "Maximum length of the generated summary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // KV Cache Optimization
            item {
                val kvCacheEnabled by settingsRepo.pdfKvCacheEnabled.collectAsState()
                val kvCacheTypeK by settingsRepo.pdfKvCacheTypeK.collectAsState()
                val kvCacheTypeV by settingsRepo.pdfKvCacheTypeV.collectAsState()
                
                val cacheTypes = listOf("f16", "q8_0", "q4_0")
                var showTypeKMenu by remember { mutableStateOf(false) }
                var showTypeVMenu by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("💾 KV Cache Quantization", fontWeight = FontWeight.Bold)
                                Text(
                                    "Reduce memory usage",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = kvCacheEnabled,
                                onCheckedChange = { settingsRepo.setPdfKvCacheEnabled(it) }
                            )
                        }
                        
                        if (kvCacheEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cache Type K")
                                Box {
                                    OutlinedButton(onClick = { showTypeKMenu = true }) {
                                        Text(kvCacheTypeK)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                    DropdownMenu(expanded = showTypeKMenu, onDismissRequest = { showTypeKMenu = false }) {
                                        cacheTypes.forEach { type ->
                                            DropdownMenuItem(text = { Text(type) }, onClick = {
                                                settingsRepo.setPdfKvCacheTypeK(type)
                                                showTypeKMenu = false
                                            })
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cache Type V")
                                Box {
                                    OutlinedButton(onClick = { showTypeVMenu = true }) {
                                        Text(kvCacheTypeV)
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                    DropdownMenu(expanded = showTypeVMenu, onDismissRequest = { showTypeVMenu = false }) {
                                        cacheTypes.forEach { type ->
                                            DropdownMenuItem(text = { Text(type) }, onClick = {
                                                settingsRepo.setPdfKvCacheTypeV(type)
                                                showTypeVMenu = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Model Picker Dialog
    if (showModelPicker) {
        AlertDialog(
            onDismissRequest = { showModelPicker = false },
            title = { Text("Select PDF Model", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    // Default option
                    item {
                        Surface(
                            onClick = {
                                settingsRepo.setPdfModelPath(null)
                                showModelPicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (pdfModelPath == null)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Settings, null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Use Chat Model", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Uses the main LLM model",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    
                    // LLM models
                    items(llmModels.size) { index ->
                        val model = llmModels[index]
                        Surface(
                            onClick = {
                                settingsRepo.setPdfModelPath(model.path)
                                showModelPicker = false
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (pdfModelPath == model.path)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(model.filename, fontWeight = FontWeight.Medium)
                                    Text(
                                        "${model.sizeBytes / (1024 * 1024)} MB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelPicker = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
