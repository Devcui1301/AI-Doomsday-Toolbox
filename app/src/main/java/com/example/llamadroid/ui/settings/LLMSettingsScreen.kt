package com.example.llamadroid.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * LLM/Chat Settings - Threads, Context Size, Temperature, Vision
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLMSettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val db = remember { AppDatabase.getDatabase(context) }
    
    val threads by settingsRepo.threads.collectAsState()
    val ctxSize by settingsRepo.contextSize.collectAsState()
    val temp by settingsRepo.temperature.collectAsState()
    val selectedModelPath by settingsRepo.selectedModelPath.collectAsState()
    val enableVision by settingsRepo.enableVision.collectAsState()
    
    val llmModels by db.modelDao().getModelsByType(ModelType.LLM).collectAsState(initial = emptyList())
    val visionProjectorModels by db.modelDao().getModelsByType(ModelType.VISION_PROJECTOR).collectAsState(initial = emptyList())
    
    val selectedModel = llmModels.find { it.path == selectedModelPath }
    // Only show vision toggle if selected model has isVision=true
    val hasVisionCapability = selectedModel?.isVision == true && visionProjectorModels.isNotEmpty()
    
    // Only disable vision when models are loaded AND no vision capability
    // This prevents race condition where llmModels is initially empty
    LaunchedEffect(hasVisionCapability, llmModels) {
        if (llmModels.isNotEmpty() && !hasVisionCapability && enableVision) {
            settingsRepo.setEnableVision(false)
        }
    }
    
    var showLlmSelector by remember { mutableStateOf(false) }
    var ctxText by remember(ctxSize) { mutableStateOf(ctxSize.toString()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LLM / Chat Settings") },
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
            // Active Model
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
                            "⭐ Active Model",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showLlmSelector = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                selectedModelPath?.substringAfterLast("/") ?: "No model selected",
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
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
                            Text("Threads", fontWeight = FontWeight.Medium)
                            Text("$threads", color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = threads.toFloat(),
                            onValueChange = { settingsRepo.setThreads(it.toInt()) },
                            valueRange = 1f..8f,
                            steps = 6
                        )
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
                        Text("Context Size", fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ctxText,
                            onValueChange = { newValue ->
                                ctxText = newValue.filter { it.isDigit() }
                                val parsed = ctxText.toIntOrNull()
                                if (parsed != null && parsed in 128..131072) {
                                    settingsRepo.setContextSize(parsed)
                                }
                            },
                            label = { Text("Tokens (128 - 131072)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
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
                            Text("Temperature", fontWeight = FontWeight.Medium)
                            Text(String.format("%.1f", temp), color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = temp,
                            onValueChange = { settingsRepo.setTemperature(it) },
                            valueRange = 0f..2f,
                            steps = 19
                        )
                    }
                }
            }
            
            // Remote Access
            item {
                val remoteAccess by settingsRepo.remoteAccess.collectAsState()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("📡 Remote Access", fontWeight = FontWeight.Bold)
                                Text(
                                    if (remoteAccess) "Server accessible from other devices (0.0.0.0)" else "Local only (127.0.0.1)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = remoteAccess,
                                onCheckedChange = { settingsRepo.setRemoteAccess(it) }
                            )
                        }
                    }
                }
            }
            
            // KV Cache Optimization
            item {
                val kvCacheEnabled by settingsRepo.serverKvCacheEnabled.collectAsState()
                val kvCacheTypeK by settingsRepo.serverKvCacheTypeK.collectAsState()
                val kvCacheTypeV by settingsRepo.serverKvCacheTypeV.collectAsState()
                val kvCacheReuse by settingsRepo.serverKvCacheReuse.collectAsState()
                
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
                                    "Reduce memory for larger contexts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = kvCacheEnabled,
                                onCheckedChange = { settingsRepo.setServerKvCacheEnabled(it) }
                            )
                        }
                        
                        if (kvCacheEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            Text(
                                "⚠️ Quantized cache uses less memory but may reduce accuracy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Cache Type K
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cache Type K", fontWeight = FontWeight.Medium)
                                Box {
                                    OutlinedButton(onClick = { showTypeKMenu = true }) {
                                        Text(kvCacheTypeK)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = showTypeKMenu,
                                        onDismissRequest = { showTypeKMenu = false }
                                    ) {
                                        cacheTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    settingsRepo.setServerKvCacheTypeK(type)
                                                    showTypeKMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Cache Type V
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cache Type V", fontWeight = FontWeight.Medium)
                                Box {
                                    OutlinedButton(onClick = { showTypeVMenu = true }) {
                                        Text(kvCacheTypeV)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                    DropdownMenu(
                                        expanded = showTypeVMenu,
                                        onDismissRequest = { showTypeVMenu = false }
                                    ) {
                                        cacheTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    settingsRepo.setServerKvCacheTypeV(type)
                                                    showTypeVMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Cache Reuse
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cache Reuse Tokens", fontWeight = FontWeight.Medium)
                                Text(
                                    if (kvCacheReuse == 0) "Disabled" else "$kvCacheReuse",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = kvCacheReuse.toFloat(),
                                onValueChange = { settingsRepo.setServerKvCacheReuse(it.toInt()) },
                                valueRange = 0f..512f,
                                steps = 7  // 0, 64, 128, 192, 256, 320, 384, 448, 512
                            )
                        }
                    }
                }
            }
            
            // Vision Settings
            if (hasVisionCapability) {
                item {
                    val selectedMmprojPath by settingsRepo.selectedMmprojPath.collectAsState()
                    var showMmprojSelector by remember { mutableStateOf(false) }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("👁️ Vision Mode", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Enable image understanding",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = enableVision,
                                    onCheckedChange = { settingsRepo.setEnableVision(it) }
                                )
                            }
                            
                            // Mmproj selector when vision is enabled
                            if (enableVision && visionProjectorModels.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Vision Projector", fontWeight = FontWeight.Medium)
                                        Text(
                                            selectedMmprojPath?.substringAfterLast("/") ?: "Not selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { showMmprojSelector = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(if (selectedMmprojPath != null) "Change" else "Select")
                                    }
                                }
                            }
                        }
                    }
                    
                    // Mmproj selector dialog
                    if (showMmprojSelector) {
                        AlertDialog(
                            onDismissRequest = { showMmprojSelector = false },
                            title = { Text("Select Vision Projector", fontWeight = FontWeight.Bold) },
                            text = {
                                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                                    items(visionProjectorModels) { model ->
                                        Surface(
                                            onClick = {
                                                settingsRepo.setSelectedMmprojPath(model.path)
                                                showMmprojSelector = false
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (model.path == selectedMmprojPath)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.secondary)
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
                                TextButton(onClick = { showMmprojSelector = false }) {
                                    Text("Cancel")
                                }
                            },
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Model Selector Dialog
    if (showLlmSelector) {
        AlertDialog(
            onDismissRequest = { showLlmSelector = false },
            title = { Text("Select Model", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(llmModels) { model ->
                        Surface(
                            onClick = {
                                settingsRepo.setSelectedModelPath(model.path)
                                showLlmSelector = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (model.path == selectedModelPath)
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
                TextButton(onClick = { showLlmSelector = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
