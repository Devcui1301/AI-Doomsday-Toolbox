package com.example.llamadroid.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.llamadroid.data.db.AppDatabase
import com.example.llamadroid.data.db.NoteEntity
import com.example.llamadroid.data.db.NoteType
import androidx.compose.ui.res.stringResource
import com.example.llamadroid.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Notes Manager Screen - Unified view for transcriptions, PDF summaries, and manual notes
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesManagerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val clipboardManager = LocalClipboardManager.current
    
    // State
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<NoteType?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedNote by remember { mutableStateOf<NoteEntity?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<NoteEntity?>(null) }
    
    // Load notes
    val allNotes by db.noteDao().getAllNotes().collectAsState(initial = emptyList())
    
    // Filter notes based on search and type filter
    val filteredNotes = remember(allNotes, searchQuery, selectedFilter) {
        allNotes.filter { note ->
            val matchesSearch = searchQuery.isEmpty() || 
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
            val matchesType = selectedFilter == null || note.type == selectedFilter
            matchesSearch && matchesType
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, stringResource(R.string.notes_new))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search notes...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedFilter == NoteType.TRANSCRIPTION,
                    onClick = { selectedFilter = if (selectedFilter == NoteType.TRANSCRIPTION) null else NoteType.TRANSCRIPTION },
                    label = { Text("🎤 Transcriptions") }
                )
                FilterChip(
                    selected = selectedFilter == NoteType.PDF_SUMMARY,
                    onClick = { selectedFilter = if (selectedFilter == NoteType.PDF_SUMMARY) null else NoteType.PDF_SUMMARY },
                    label = { Text("📄 PDFs") }
                )
                FilterChip(
                    selected = selectedFilter == NoteType.MANUAL,
                    onClick = { selectedFilter = if (selectedFilter == NoteType.MANUAL) null else NoteType.MANUAL },
                    label = { Text("📝 Notes") }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notes list
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No notes yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            "Create a note or transcribe audio to get started",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onEdit = { selectedNote = note },
                            onCopy = {
                                clipboardManager.setText(AnnotatedString(note.content))
                            },
                            onDelete = {
                                noteToDelete = note
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Add/Edit note dialog
    if (showAddDialog || selectedNote != null) {
        NoteEditDialog(
            note = selectedNote,
            onDismiss = {
                showAddDialog = false
                selectedNote = null
            },
            onSave = { title, content ->
                scope.launch {
                    if (selectedNote != null) {
                        db.noteDao().update(
                            selectedNote!!.copy(
                                title = title,
                                content = content,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        db.noteDao().insert(
                            NoteEntity(
                                title = title,
                                content = content,
                                type = NoteType.MANUAL
                            )
                        )
                    }
                    showAddDialog = false
                    selectedNote = null
                }
            }
        )
    }
    
    // Delete confirmation dialog
    if (showDeleteDialog && noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                noteToDelete = null
            },
            title = { Text("Delete Note?") },
            text = { Text("Are you sure you want to delete \"${noteToDelete?.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            noteToDelete?.let { db.noteDao().delete(it) }
                            showDeleteDialog = false
                            noteToDelete = null
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    noteToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun NoteCard(
    note: NoteEntity,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type badge
                val (emoji, badgeColor) = when (note.type) {
                    NoteType.TRANSCRIPTION -> "🎤" to MaterialTheme.colorScheme.primaryContainer
                    NoteType.PDF_SUMMARY -> "📄" to MaterialTheme.colorScheme.secondaryContainer
                    NoteType.MANUAL -> "📝" to MaterialTheme.colorScheme.tertiaryContainer
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(emoji, style = MaterialTheme.typography.bodyMedium)
                }
                
                // Actions
                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, "Copy", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                note.sourceFile?.let {
                    Text(
                        "Source: ${it.substringAfterLast("/")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                Text(
                    dateFormat.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditDialog(
    note: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: (title: String, content: String) -> Unit
) {
    var title by remember(note) { mutableStateOf(note?.title ?: "") }
    var content by remember(note) { mutableStateOf(note?.content ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "New Note" else "Edit Note") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title, content) },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
