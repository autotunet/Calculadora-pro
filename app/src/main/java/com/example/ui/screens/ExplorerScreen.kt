package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.example.ads.AdMobManager
import java.io.File
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import com.example.ui.components.DeleteSelectedItemsConfirmDialog
import com.example.ui.components.RecentFilesSection
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import com.example.model.FileItem
import com.example.model.FileSortOption
import com.example.model.ViewMode
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.ClipboardFloatingBar
import com.example.ui.components.CreateDocumentDialog
import com.example.ui.components.CreateFolderDialog
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.FileDetailsBottomSheet
import com.example.ui.components.FileGridItem
import com.example.ui.components.FileListItem
import com.example.ui.components.FileViewerDialog
import com.example.ui.components.QuickSearchBar
import com.example.ui.components.RenameDialog
import com.example.viewmodel.FileManagerViewModel

@Composable
fun ExplorerScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val currentDirectory by viewModel.currentDirectory.collectAsState()
    val breadcrumbs by viewModel.breadcrumbs.collectAsState()
    val files by viewModel.currentFiles.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchCategory by viewModel.searchCategory.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val clipboard by viewModel.clipboard.collectAsState()
    val recentsList by viewModel.recentsList.collectAsState()

    val viewingFile by viewModel.viewingFile.collectAsState()
    val editorContent by viewModel.fileEditorContent.collectAsState()
    val detailsFile by viewModel.detailsFile.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateDocDialog by remember { mutableStateOf(false) }
    var itemToRename by remember { mutableStateOf<FileItem?>(null) }
    var itemToDelete by remember { mutableStateOf<FileItem?>(null) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var selectedPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isSelectionActive by remember { mutableStateOf(false) }
    var showDeleteSelectedDialog by remember { mutableStateOf(false) }
    val isSelectionMode = isSelectionActive || selectedPaths.isNotEmpty()

    val context = LocalContext.current
    val activity = context as? Activity
    val adManager = remember { AdMobManager.getInstance(context) }

    // Intercept back button to clear selection first or navigate up folders
    BackHandler(enabled = isSelectionMode) {
        selectedPaths = emptySet()
        isSelectionActive = false
    }

    BackHandler(enabled = !isSelectionMode && breadcrumbs.size > 1) {
        viewModel.navigateUp()
    }

    val displayedFiles = if (isSearching) searchResults else files

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AnimatedVisibility(visible = showFabMenu) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                if (activity != null) {
                                    adManager.showAdForTool(activity, "Nuevo Documento") {
                                        showCreateDocDialog = true
                                    }
                                } else {
                                    showCreateDocDialog = true
                                }
                            },
                            icon = { Icon(Icons.Default.Description, contentDescription = null) },
                            text = { Text("Nuevo Documento") },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("fab_new_document")
                        )

                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                if (activity != null) {
                                    adManager.showAdForTool(activity, "Nueva Carpeta") {
                                        showCreateFolderDialog = true
                                    }
                                } else {
                                    showCreateFolderDialog = true
                                }
                            },
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            text = { Text("Nueva Carpeta") },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.testTag("fab_new_folder")
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_main_add")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar elemento",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        bottomBar = {
            ClipboardFloatingBar(
                clipboardItem = clipboard,
                currentDirectoryName = currentDirectory.name,
                onPaste = {
                    if (activity != null) {
                        adManager.showAdForTool(activity, "Pegar Archivos") {
                            viewModel.pasteToCurrentDirectory()
                        }
                    } else {
                        viewModel.pasteToCurrentDirectory()
                    }
                },
                onCancel = { viewModel.clearClipboard() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Fast Search Bar
            QuickSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                selectedCategory = searchCategory,
                onCategoryChange = { viewModel.setSearchCategory(it) },
                onClear = { viewModel.clearSearch() }
            )

            // Contextual Selection Bar
            if (isSelectionMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("selection_action_bar"),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                selectedPaths = emptySet()
                                isSelectionActive = false
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar selección")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${selectedPaths.size} seleccionados",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Seleccionar todo
                            IconButton(
                                onClick = {
                                    selectedPaths = if (selectedPaths.size == displayedFiles.size) {
                                        emptySet()
                                    } else {
                                        displayedFiles.map { it.path }.toSet()
                                    }
                                },
                                modifier = Modifier.testTag("select_all_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = "Seleccionar todo",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            val selectedItems = displayedFiles.filter { selectedPaths.contains(it.path) }

                            // Mover seleccionados (Cortar lote)
                            IconButton(
                                onClick = {
                                    if (selectedItems.isNotEmpty()) {
                                        viewModel.cutMultipleFiles(selectedItems)
                                        selectedPaths = emptySet()
                                        isSelectionActive = false
                                    }
                                },
                                enabled = selectedItems.isNotEmpty(),
                                modifier = Modifier.testTag("batch_move_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCut,
                                    contentDescription = "Mover seleccionados",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Copiar seleccionados (Copiar lote)
                            IconButton(
                                onClick = {
                                    if (selectedItems.isNotEmpty()) {
                                        viewModel.copyMultipleFiles(selectedItems)
                                        selectedPaths = emptySet()
                                        isSelectionActive = false
                                    }
                                },
                                enabled = selectedItems.isNotEmpty(),
                                modifier = Modifier.testTag("batch_copy_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copiar seleccionados",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Compartir seleccionados
                            IconButton(
                                onClick = {
                                    if (selectedItems.isNotEmpty()) {
                                        shareMultipleFiles(context, selectedItems)
                                    }
                                },
                                enabled = selectedItems.isNotEmpty(),
                                modifier = Modifier.testTag("batch_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Compartir seleccionados",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // Eliminar seleccionados
                            IconButton(
                                onClick = { showDeleteSelectedDialog = true },
                                enabled = selectedItems.isNotEmpty(),
                                modifier = Modifier.testTag("delete_selected_items_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar seleccionados",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Header Bar: View mode, Sort, Refresh
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSearching) {
                            "${displayedFiles.size} resultados encontrados"
                        } else {
                            "${displayedFiles.size} elementos"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Toggle View Mode (List / Grid)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleViewMode() }
                                .testTag("toggle_view_mode_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                    contentDescription = if (viewMode == ViewMode.LIST) "Cambiar a cuadrícula" else "Cambiar a lista",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (viewMode == ViewMode.LIST) "Cuadrícula" else "Lista",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Multi-select toggle button
                        IconButton(
                            onClick = {
                                isSelectionActive = !isSelectionActive
                                if (!isSelectionActive) selectedPaths = emptySet()
                            },
                            modifier = Modifier.testTag("toggle_multi_select_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = "Modo selección múltiple",
                                tint = if (isSelectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Sort Menu
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.testTag("sort_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Ordenar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                FileSortOption.values().forEach { option ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (sortOption == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            showSortMenu = false
                                            viewModel.setSortOption(option)
                                        }
                                    )
                                }
                            }
                        }

                        // Refresh Button
                        IconButton(
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.testTag("refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Breadcrumbs Navigation Bar (hidden during active search)
            if (!isSearching) {
                BreadcrumbBar(
                    breadcrumbs = breadcrumbs,
                    onBreadcrumbClick = { viewModel.navigateTo(it) },
                    onNavigateUp = { viewModel.navigateUp() },
                    canNavigateUp = breadcrumbs.size > 1
                )
            }

            // Recent Files Section for quick navigation (shown on main screen)
            if (!isSearching && !isSelectionMode && breadcrumbs.size <= 1 && recentsList.isNotEmpty()) {
                RecentFilesSection(
                    recentFiles = recentsList,
                    onFileClick = { viewModel.openFile(it) },
                    onSeeAll = { viewModel.setNavTab(com.example.viewmodel.AppNavTab.RECENTS) }
                )
            }

            // Main Content: List or Grid of Files
            if (displayedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.SearchOff else Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isSearching) "No se encontraron archivos" else "Carpeta vacía",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isSearching) {
                                "Intenta con otro término o elimina los filtros de categoría"
                            } else {
                                "Usa el botón '+' para crear una carpeta o documento de texto"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (viewMode == ViewMode.LIST) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("files_lazy_column"),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(displayedFiles, key = { it.path }) { item ->
                            val isSelected = selectedPaths.contains(item.path)
                            FileListItem(
                                fileItem = item,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                    } else {
                                        viewModel.openFile(item)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionActive = true
                                        selectedPaths = setOf(item.path)
                                    } else {
                                        selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                    }
                                },
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onFavoriteToggle = { viewModel.toggleFavorite(item) },
                                onRename = { itemToRename = item },
                                onDelete = { itemToDelete = item },
                                onShare = { shareFile(context, item) },
                                onCopy = { viewModel.copyItem(item) },
                                onCut = { viewModel.cutItem(item) },
                                onDetails = { viewModel.showFileDetails(item) }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 130.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                            .testTag("files_grid_view"),
                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedFiles, key = { it.path }) { item ->
                            val isSelected = selectedPaths.contains(item.path)
                            FileGridItem(
                                fileItem = item,
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                    } else {
                                        viewModel.openFile(item)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionActive = true
                                        selectedPaths = setOf(item.path)
                                    } else {
                                        selectedPaths = if (isSelected) selectedPaths - item.path else selectedPaths + item.path
                                    }
                                },
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                onFavoriteToggle = { viewModel.toggleFavorite(item) },
                                onRename = { itemToRename = item },
                                onDelete = { itemToDelete = item },
                                onShare = { shareFile(context, item) },
                                onCopy = { viewModel.copyItem(item) },
                                onCut = { viewModel.cutItem(item) },
                                onDetails = { viewModel.showFileDetails(item) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = {
                showCreateFolderDialog = false
                viewModel.createFolder(it)
            }
        )
    }

    if (showCreateDocDialog) {
        CreateDocumentDialog(
            onDismiss = { showCreateDocDialog = false },
            onConfirm = { name, content ->
                showCreateDocDialog = false
                viewModel.createTextFile(name, content)
            }
        )
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

    if (showDeleteSelectedDialog) {
        val selectedItemsList = displayedFiles.filter { selectedPaths.contains(it.path) }
        DeleteSelectedItemsConfirmDialog(
            selectedItems = selectedItemsList,
            onDismiss = { showDeleteSelectedDialog = false },
            onConfirm = {
                showDeleteSelectedDialog = false
                viewModel.deleteMultipleFiles(selectedItemsList)
                selectedPaths = emptySet()
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
            onSaveContent = {
                if (activity != null) {
                    adManager.showAdForTool(activity, "Guardar Archivo") {
                        viewModel.saveFileContent()
                    }
                } else {
                    viewModel.saveFileContent()
                }
            },
            onClose = { viewModel.closeViewer() }
        )
    }
}

private fun shareFile(context: Context, fileItem: com.example.model.FileItem) {
    try {
        val file = File(fileItem.path)
        if (file.exists() && !fileItem.isDirectory) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = fileItem.mimeType.ifBlank { "*/*" }
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir ${fileItem.name}"))
        } else {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Carpeta/Archivo: ${fileItem.name}\nRuta: ${fileItem.path}")
            }
            context.startActivity(Intent.createChooser(textIntent, "Compartir ${fileItem.name}"))
        }
    } catch (_: Exception) {
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${fileItem.name} (${fileItem.formattedSize})")
        }
        context.startActivity(Intent.createChooser(textIntent, "Compartir ${fileItem.name}"))
    }
}

private fun shareMultipleFiles(context: Context, items: List<com.example.model.FileItem>) {
    if (items.isEmpty()) return
    try {
        val files = items.map { File(it.path) }.filter { it.exists() && it.isFile }
        if (files.isNotEmpty()) {
            val uris = ArrayList(files.map {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it)
            })
            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir ${files.size} archivos"))
        } else {
            val textIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                val names = items.joinToString("\n") { "- ${it.name} (${it.formattedSize})" }
                putExtra(Intent.EXTRA_TEXT, "Archivos seleccionados:\n$names")
            }
            context.startActivity(Intent.createChooser(textIntent, "Compartir ${items.size} elementos"))
        }
    } catch (_: Exception) {
        val textIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "${items.size} elementos seleccionados")
        }
        context.startActivity(Intent.createChooser(textIntent, "Compartir ${items.size} elementos"))
    }
}
