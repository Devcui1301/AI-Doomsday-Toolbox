package com.example.llamadroid.ui.pdf

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.llamadroid.data.SettingsRepository
import com.example.llamadroid.service.PDFService
import androidx.compose.ui.res.stringResource
import com.example.llamadroid.R
import kotlinx.coroutines.launch

/**
 * PDF Toolbox Screen - Merge, Split, Extract tools
 * Uses rememberSaveable for state persistence across navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFToolboxScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepo = remember { SettingsRepository(context) }
    val pdfService = remember { PDFService(context) }
    
    // State - using rememberSaveable for persistence across tab changes
    var selectedTool by rememberSaveable { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) } // Not saveable - reset on return
    var currentJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var splitPageRange by rememberSaveable { mutableStateOf("") }
    var extractPages by rememberSaveable { mutableStateOf("") }
    var ocrResult by rememberSaveable { mutableStateOf("") }
    
    // Uri lists cannot be saved directly - store as strings
    var selectedPdfStrings by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var selectedImageStrings by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    
    // Convert strings back to Uris
    val selectedPdfs = selectedPdfStrings.map { android.net.Uri.parse(it) }
    val selectedImages = selectedImageStrings.map { android.net.Uri.parse(it) }
    
    // PDF picker
    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Take persistent permission
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Permission might already be granted
                }
            }
            selectedPdfStrings = uris.map { it.toString() }
        }
    }
    
    val singlePdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }
            selectedPdfStrings = listOf(it.toString())
        }
    }
    
    // Image picker for OCR and Images-to-PDF
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { }
            }
            selectedImageStrings = uris.map { it.toString() }
        }
    }
    
    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) { }
            selectedImageStrings = listOf(it.toString())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Toolbox") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (selectedTool != null) {
                            selectedTool = null
                            selectedPdfStrings = emptyList()
                        } else {
                            navController.popBackStack() 
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (selectedTool == null) {
            // Tool selection
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "📄 PDF Tools",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                item {
                    PDFToolCard(
                        icon = "🔗",
                        title = "Merge PDFs",
                        description = "Combine multiple PDF files into one",
                        gradientColors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.15f),
                            Color(0xFF388E3C).copy(alpha = 0.3f)
                        ),
                        onClick = { selectedTool = "merge" }
                    )
                }
                
                item {
                    PDFToolCard(
                        icon = "✂️",
                        title = "Split PDF",
                        description = "Extract specific pages or split into parts",
                        gradientColors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.15f),
                            Color(0xFF1976D2).copy(alpha = 0.3f)
                        ),
                        onClick = { selectedTool = "split" }
                    )
                }
                
                item {
                    PDFToolCard(
                        icon = "📝",
                        title = "Extract Text",
                        description = "Extract all text content from PDF",
                        gradientColors = listOf(
                            Color(0xFF9C27B0).copy(alpha = 0.15f),
                            Color(0xFF7B1FA2).copy(alpha = 0.3f)
                        ),
                        onClick = { selectedTool = "extract" }
                    )
                }
                
                item {
                    PDFToolCard(
                        icon = "🤖",
                        title = "AI Summary",
                        description = "Summarize PDF content using AI",
                        gradientColors = listOf(
                            Color(0xFFFF9800).copy(alpha = 0.15f),
                            Color(0xFFF57C00).copy(alpha = 0.3f)
                        ),
                        onClick = { navController.navigate("pdf_summary") }
                    )
                }
                
                item {
                    PDFToolCard(
                        icon = "🔍",
                        title = "OCR (Text Recognition)",
                        description = "Extract text from scanned PDFs/images",
                        gradientColors = listOf(
                            Color(0xFF00BCD4).copy(alpha = 0.15f),
                            Color(0xFF0097A7).copy(alpha = 0.3f)
                        ),
                        onClick = { selectedTool = "ocr" }
                    )
                }
                
                item {
                    PDFToolCard(
                        icon = "🖼️",
                        title = "Images to PDF",
                        description = "Convert images into a PDF document",
                        gradientColors = listOf(
                            Color(0xFF673AB7).copy(alpha = 0.15f),
                            Color(0xFF512DA8).copy(alpha = 0.3f)
                        ),
                        onClick = { selectedTool = "images_to_pdf" }
                    )
                }
                
                item {
                    PDFToolCard(
                        icon = "📦",
                        title = "Compress PDF",
                        description = "Reduce file size with 9 quality levels",
                        gradientColors = listOf(
                            Color(0xFF607D8B).copy(alpha = 0.15f),
                            Color(0xFF455A64).copy(alpha = 0.3f)
                        ),
                        onClick = { selectedTool = "compress" }
                    )
                }
                
                item {
                    PDFToolCard(
                        icon = "📐",
                        title = "Split by Size",
                        description = "Divide PDF into parts of max size",
                        gradientColors = listOf(
                            Color(0xFF795548).copy(alpha = 0.15f),
                            Color(0xFF5D4037).copy(alpha = 0.3f)
                        ),
                        onClick = { selectedTool = "split_size" }
                    )
                }
            }
        } else {
            // Tool interface
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                when (selectedTool) {
                    "merge" -> {
                        Text(
                            "🔗 Merge PDFs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Select multiple PDFs to combine into one file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select PDFs")
                        }
                        
                        if (selectedPdfs.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Selected: ${selectedPdfs.size} files", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(selectedPdfs) { index, uri ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("📄", style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "${index + 1}. ${uri.lastPathSegment ?: "PDF"}",
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1
                                            )
                                            IconButton(
                                                onClick = { 
                                                    selectedPdfStrings = selectedPdfStrings.toMutableList().apply { removeAt(index) }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        try {
                                            val result = pdfService.mergePdfs(selectedPdfs)
                                            result.fold(
                                                onSuccess = { 
                                                    Toast.makeText(context, "PDFs merged successfully!", Toast.LENGTH_SHORT).show()
                                                    selectedTool = null
                                                    selectedPdfStrings = emptyList()
                                                },
                                                onFailure = {
                                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedPdfs.size >= 2 && !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Merge PDFs")
                            }
                        }
                    }
                    
                    "split" -> {
                        Text(
                            "✂️ Split PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Extract specific pages from a PDF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (selectedPdfs.isEmpty()) {
                            OutlinedButton(
                                onClick = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select PDF")
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📄", style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        selectedPdfs.first().lastPathSegment ?: "Selected PDF",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { selectedPdfStrings = emptyList() }) {
                                        Icon(Icons.Default.Close, "Remove")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = splitPageRange,
                                onValueChange = { splitPageRange = it },
                                label = { Text("Pages to extract") },
                                placeholder = { Text("e.g. 1-3, 5, 7-10") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Enter page numbers or ranges separated by commas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        try {
                                            val result = pdfService.splitPdf(selectedPdfs.first(), splitPageRange)
                                            result.fold(
                                                onSuccess = {
                                                    Toast.makeText(context, "PDF split successfully!", Toast.LENGTH_SHORT).show()
                                                    selectedTool = null
                                                    selectedPdfStrings = emptyList()
                                                    splitPageRange = ""
                                                },
                                                onFailure = {
                                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedPdfs.isNotEmpty() && splitPageRange.isNotBlank() && !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Extract Pages")
                            }
                        }
                    }
                    
                    "extract" -> {
                        Text(
                            "📝 Extract Text",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Extract all text content from a PDF file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (selectedPdfs.isEmpty()) {
                            OutlinedButton(
                                onClick = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select PDF")
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📄", style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        selectedPdfs.first().lastPathSegment ?: "Selected PDF",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { selectedPdfStrings = emptyList() }) {
                                        Icon(Icons.Default.Close, "Remove")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        try {
                                            val result = pdfService.extractText(selectedPdfs.first())
                                            result.fold(
                                                onSuccess = { text ->
                                                    // Save to notes
                                                    val db = com.example.llamadroid.data.db.AppDatabase.getDatabase(context)
                                                    db.noteDao().insert(
                                                        com.example.llamadroid.data.db.NoteEntity(
                                                            title = "PDF: ${selectedPdfs.first().lastPathSegment ?: "Extracted"}",
                                                            content = text,
                                                            type = com.example.llamadroid.data.db.NoteType.PDF_SUMMARY,
                                                            sourceFile = selectedPdfs.first().toString()
                                                        )
                                                    )
                                                    Toast.makeText(context, "Text extracted and saved to Notes!", Toast.LENGTH_SHORT).show()
                                                    selectedTool = null
                                                    selectedPdfStrings = emptyList()
                                                },
                                                onFailure = {
                                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedPdfs.isNotEmpty() && !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Extract Text")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Extracted text will be saved to Notes Manager",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    "ocr" -> {
                        Text(
                            "🔍 OCR (Text Recognition)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Extract text from images using ML Kit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (selectedImages.isEmpty()) {
                            OutlinedButton(
                                onClick = { singleImagePicker.launch(arrayOf("image/*")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Image")
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🖼️", style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        selectedImages.first().lastPathSegment ?: "Selected Image",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { selectedImageStrings = emptyList(); ocrResult = "" }) {
                                        Icon(Icons.Default.Close, "Remove")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (ocrResult.isEmpty()) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isProcessing = true
                                            try {
                                                val result = pdfService.performOCR(selectedImages.first())
                                                result.fold(
                                                    onSuccess = { text ->
                                                        ocrResult = text
                                                    },
                                                    onFailure = {
                                                        Toast.makeText(context, "OCR Error: ${it.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            } finally {
                                                isProcessing = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isProcessing
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Extract Text (OCR)")
                                }
                            } else {
                                Text("📝 Extracted Text", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                            .fillMaxWidth()
                                            .heightIn(min = 100.dp, max = 200.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        Text(ocrResult)
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val db = com.example.llamadroid.data.db.AppDatabase.getDatabase(context)
                                            db.noteDao().insert(
                                                com.example.llamadroid.data.db.NoteEntity(
                                                    title = "OCR: ${selectedImages.first().lastPathSegment ?: "Image"}",
                                                    content = ocrResult,
                                                    type = com.example.llamadroid.data.db.NoteType.PDF_SUMMARY,
                                                    sourceFile = selectedImages.first().toString()
                                                )
                                            )
                                            Toast.makeText(context, "OCR result saved to Notes!", Toast.LENGTH_SHORT).show()
                                            selectedTool = null
                                            selectedImageStrings = emptyList()
                                            ocrResult = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save to Notes")
                                }
                            }
                        }
                    }
                    
                    "images_to_pdf" -> {
                        Text(
                            "🖼️ Images to PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Select multiple images to combine into a PDF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = { imagePicker.launch(arrayOf("image/*")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Images")
                        }
                        
                        if (selectedImages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Selected: ${selectedImages.size} images", fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(selectedImages) { index, uri ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🖼️", style = MaterialTheme.typography.titleMedium)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "${index + 1}. ${uri.lastPathSegment ?: "Image"}",
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1
                                            )
                                            IconButton(
                                                onClick = { 
                                                    selectedImageStrings = selectedImageStrings.toMutableList().apply { removeAt(index) }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    scope.launch {
                                        isProcessing = true
                                        try {
                                            val result = pdfService.imagesToPdf(selectedImages)
                                            result.fold(
                                                onSuccess = { 
                                                    Toast.makeText(context, "PDF created successfully!", Toast.LENGTH_SHORT).show()
                                                    selectedTool = null
                                                    selectedImageStrings = emptyList()
                                                },
                                                onFailure = {
                                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                                }
                                            )
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = selectedImages.isNotEmpty() && !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text("Create PDF")
                            }
                        }
                    }
                    
                    "compress" -> {
                        var compressionLevel by remember { mutableStateOf(5) }
                        
                        Text(
                            "📦 Compress PDF",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Reduce file size by compressing images",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (selectedPdfs.isEmpty()) {
                            Button(
                                onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Select PDF")
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📄", style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        selectedPdfs.first().lastPathSegment ?: "PDF",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { selectedPdfStrings = emptyList() }) {
                                        Icon(Icons.Default.Close, "Remove")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "Compression Level: $compressionLevel (${if (compressionLevel <= 3) "High Quality" else if (compressionLevel <= 6) "Medium" else "Max Compression"})",
                                fontWeight = FontWeight.Medium
                            )
                            Slider(
                                value = compressionLevel.toFloat(),
                                onValueChange = { compressionLevel = it.toInt() },
                                valueRange = 1f..9f,
                                steps = 7,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                "1 = Best quality, 9 = Smallest file",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isProcessing) {
                                    OutlinedButton(
                                        onClick = {
                                            currentJob?.cancel()
                                            isProcessing = false
                                            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel")
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        currentJob = scope.launch {
                                            isProcessing = true
                                            try {
                                                pdfService.compressPdf(selectedPdfs.first(), compressionLevel).fold(
                                                    onSuccess = { result ->
                                                        Toast.makeText(context, "Compressed! Saved to output folder", Toast.LENGTH_LONG).show()
                                                        selectedPdfStrings = emptyList()
                                                    },
                                                    onFailure = {
                                                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            } finally {
                                                isProcessing = false
                                                currentJob = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isProcessing
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Compress")
                                }
                            }
                        }
                    }
                    
                    "split_size" -> {
                        var sizeInput by remember { mutableStateOf("5") }
                        
                        Text(
                            "📐 Split by Size",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Divide PDF into parts with maximum file size",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        if (selectedPdfs.isEmpty()) {
                            Button(
                                onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Select PDF")
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📄", style = MaterialTheme.typography.titleLarge)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        selectedPdfs.first().lastPathSegment ?: "PDF",
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { selectedPdfStrings = emptyList() }) {
                                        Icon(Icons.Default.Close, "Remove")
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = sizeInput,
                                onValueChange = { sizeInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Max Size (MB)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isProcessing) {
                                    OutlinedButton(
                                        onClick = {
                                            currentJob?.cancel()
                                            isProcessing = false
                                            Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Cancel")
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        val sizeMb = sizeInput.toLongOrNull() ?: 5
                                        val sizeBytes = sizeMb * 1024 * 1024
                                        currentJob = scope.launch {
                                            isProcessing = true
                                            try {
                                                pdfService.splitBySize(selectedPdfs.first(), sizeBytes).fold(
                                                    onSuccess = { uris ->
                                                        Toast.makeText(context, "Split into ${uris.size} parts!", Toast.LENGTH_LONG).show()
                                                        selectedPdfStrings = emptyList()
                                                    },
                                                    onFailure = {
                                                        Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                                    }
                                                )
                                            } finally {
                                                isProcessing = false
                                                currentJob = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isProcessing && sizeInput.isNotEmpty()
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text("Split by Size")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PDFToolCard(
    icon: String,
    title: String,
    description: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.horizontalGradient(gradientColors))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, style = MaterialTheme.typography.headlineMedium)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
