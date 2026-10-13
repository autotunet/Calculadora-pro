package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.FileItem
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileDetailsBottomSheet
import com.example.ui.components.FileListItem
import com.example.ui.components.FileViewerDialog
import com.example.ui.components.RenameDialog
import com.example.viewmodel.FileManagerViewModel

@Composable
fun RecentsScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val recentsList by viewModel.recentsList.collectAsState()
    val viewingFile by viewModel.viewingFile.collectAsState()
    val editorContent by viewModel.fileEditorContent.collectAsState()
    val detailsFile by viewModel.detailsFile.collectAsState()

    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Archivos Recientes",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Últimos archivos abiertos o modificados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (recentsList.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearRecents() },
                        modifier = Modifier.testTag("clear_recents_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpiar historial",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (recentsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No hay archivos recientes",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Los archivos que abras o consultes aparecerán organizados aquí",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recents_lazy_column"),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(recentsList, key = { it.path }) { item ->
                        FileListItem(
                            fileItem = item,
                            onClick = { viewModel.openFile(item) },
                            onFavoriteToggle = { viewModel.toggleFavorite(item) },
                            onRename = { itemToRename = item },
                            onDelete = { itemToDelete = item },
                            onCopy = { viewModel.copyItem(item) },
                            onCut = { viewModel.cutItem(item) },
                            onDetails = { viewModel.showFileDetails(item) }
                        )
                    }
                }
            }
        }
    }

    itemToRename?.let { item ->
        RenameDialog(
            currentName = item.name,
            onDismiss = { itemToRename = null },
            onConfirm = {
                itemToRename = null
                viewModel.renameFile(item, it)
            }
        )
    }

    itemToDelete?.let { item ->
        DeleteConfirmDialog(
            fileItem = item,
            onDismiss = { itemToDelete = null },
            onConfirm = {
                itemToDelete = null
                viewModel.deleteFile(item)
            }
        )
    }

    detailsFile?.let { item ->
        FileDetailsBottomSheet(
            fileItem = item,
            onDismiss = { viewModel.closeFileDetails() }
        )
    }

    viewingFile?.let { item ->
        FileViewerDialog(
            fileItem = item,
            editorContent = editorContent,
            onContentChange = { viewModel.updateEditorContent(it) },
            onSaveContent = { viewModel.saveFileContent() },
            onClose = { viewModel.closeViewer() }
        )
    }
}
