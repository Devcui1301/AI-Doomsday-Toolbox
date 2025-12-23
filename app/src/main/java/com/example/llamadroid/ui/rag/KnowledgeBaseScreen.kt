package com.example.llamadroid.ui.rag

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.llamadroid.data.db.AppDatabase
import com.example.llamadroid.data.rag.KnowledgeBaseManager
import com.example.llamadroid.data.rag.PdfExtractor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KnowledgeBaseScreen(navController: NavController) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val pdfExtractor = remember { PdfExtractor(context) }
    val kbManager = remember { KnowledgeBaseManager(db.ragDao(), pdfExtractor) }
    val viewModel = remember { KnowledgeBaseViewModel(db.ragDao(), db.modelDao(), kbManager) }
    
    val kbs by viewModel.knowledgeBases.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    // Quick Fix: Re-compose launcher isn't good. Use a mutable state accessible in callback.
    
    val currentKbTarget = remember { mutableStateOf<Long?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { 
            val targetId = currentKbTarget.value
            if (targetId != null) {
                 val file = com.example.llamadroid.util.FileUtils.copyUriToInternalStorage(context, it, "pdfs")
                 if (file != null) viewModel.addPdf(targetId, file)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.padding(16.dp)) {
            items(kbs) { kb ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(kb.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Created: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(kb.createdDate))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { 
                            currentKbTarget.value = kb.id
                            pdfLauncher.launch(arrayOf("application/pdf")) 
                        }) {
                            Text("Add Documents")
                        }
                    }
                }
            }
        }
    }
    
    if (showDialog) {
        CreateKbDialog(
            onDismiss = { showDialog = false },
            onCreate = { name -> 
                viewModel.createKb(name)
                showDialog = false 
            }
        )
    }
}

@Composable
fun CreateKbDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Knowledge Base") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }) },
        confirmButton = { Button(onClick = { onCreate(name) }) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
