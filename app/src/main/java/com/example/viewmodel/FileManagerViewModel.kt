package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.ClipboardItem
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.FileSortOption
import com.example.model.StorageStats
import com.example.model.ViewMode
import com.example.repository.FileManagerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

enum class AppNavTab(val title: String) {
    EXPLORER("Explorar"),
    CATEGORIES("Categorías"),
    FAVORITES("Favoritos"),
    RECENTS("Recientes"),
    STORAGE("Almacenamiento")
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FileManagerRepository(application)

    private val _currentNavTab = MutableStateFlow(AppNavTab.EXPLORER)
    val currentNavTab: StateFlow<AppNavTab> = _currentNavTab.asStateFlow()

    private val _currentDirectory = MutableStateFlow<File>(repository.defaultWorkspaceDir)
    val currentDirectory: StateFlow<File> = _currentDirectory.asStateFlow()

    private val _breadcrumbs = MutableStateFlow<List<File>>(listOf(repository.defaultWorkspaceDir))
    val breadcrumbs: StateFlow<List<File>> = _breadcrumbs.asStateFlow()

    private val _rawFiles = MutableStateFlow<List<FileItem>>(emptyList())
    private val _sortOption = MutableStateFlow(FileSortOption.NAME_ASC)
    val sortOption: StateFlow<FileSortOption> = _sortOption.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    val currentFiles: StateFlow<List<FileItem>> = combine(_rawFiles, _sortOption) { files, sort ->
        sortFiles(files, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchCategory = MutableStateFlow(FileCategory.ALL)
    val searchCategory: StateFlow<FileCategory> = _searchCategory.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FileItem>>(emptyList())
    val searchResults: StateFlow<List<FileItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Clipboard
    private val _clipboard = MutableStateFlow<ClipboardItem?>(null)
    val clipboard: StateFlow<ClipboardItem?> = _clipboard.asStateFlow()

    // Category Screen Filter & files
    private val _selectedCategory = MutableStateFlow(FileCategory.DOCUMENTS)
    val selectedCategory: StateFlow<FileCategory> = _selectedCategory.asStateFlow()

    private val _categoryFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val categoryFiles: StateFlow<List<FileItem>> = _categoryFiles.asStateFlow()

    // Storage Stats
    private val _storageStats = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storageStats.asStateFlow()

    // Favorites & Recents items
    private val _favoritesList = MutableStateFlow<List<FileItem>>(emptyList())
    val favoritesList: StateFlow<List<FileItem>> = _favoritesList.asStateFlow()

    private val _recentsList = MutableStateFlow<List<FileItem>>(emptyList())
    val recentsList: StateFlow<List<FileItem>> = _recentsList.asStateFlow()
    private var userClearedRecents = false

    private val favoritesSet = MutableStateFlow<Set<String>>(emptySet())

    // Active File for Viewer / Editor
    private val _viewingFile = MutableStateFlow<FileItem?>(null)
    val viewingFile: StateFlow<FileItem?> = _viewingFile.asStateFlow()

    private val _fileEditorContent = MutableStateFlow("")
    val fileEditorContent: StateFlow<String> = _fileEditorContent.asStateFlow()

    private val _detailsFile = MutableStateFlow<FileItem?>(null)
    val detailsFile: StateFlow<FileItem?> = _detailsFile.asStateFlow()

    // Notification toast / snackbar
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    init {
        viewModelScope.launch {
            repository.initDefaultFilesIfNeeded()
            loadDirectory(repository.defaultWorkspaceDir)
            refreshStorageStats()
            refreshRecentsInternal()
        }

        viewModelScope.launch {
            repository.favoritesFlow.collect { favEntities ->
                val set = favEntities.map { it.path }.toSet()
                favoritesSet.value = set
                _favoritesList.value = favEntities.map { entity ->
                    val file = File(entity.path)
                    val isDir = entity.isDirectory
                    val ext = file.extension
                    FileItem(
                        name = entity.name,
                        path = entity.path,
                        isDirectory = isDir,
                        sizeBytes = if (isDir) 0L else file.length(),
                        lastModified = file.lastModified(),
                        extension = ext,
                        mimeType = if (isDir) "*/*" else "application/octet-stream",
                        itemCount = if (isDir) file.listFiles()?.size ?: 0 else 0,
                        isFavorite = true
                    )
                }
                // Refresh directory to reflect updated star states
                loadDirectory(_currentDirectory.value)
            }
        }

        viewModelScope.launch {
            repository.recentsFlow.collect { recentEntities ->
                if (userClearedRecents) {
                    _recentsList.value = emptyList()
                    return@collect
                }
                val mapped = recentEntities.mapNotNull { entity ->
                    val file = File(entity.path)
                    if (!file.exists()) return@mapNotNull null
                    val isDir = entity.isDirectory
                    FileItem(
                        name = entity.name,
                        path = entity.path,
                        isDirectory = isDir,
                        sizeBytes = if (isDir) 0L else file.length(),
                        lastModified = file.lastModified(),
                        extension = file.extension,
                        mimeType = "*/*",
                        itemCount = if (isDir) file.listFiles()?.size ?: 0 else 0,
                        isFavorite = favoritesSet.value.contains(entity.path)
                    )
                }

                val modified = repository.getRecentlyModifiedFiles(15)
                val combined = (mapped + modified).distinctBy { it.path }.take(15)
                _recentsList.value = combined
            }
        }
    }

    fun setNavTab(tab: AppNavTab) {
        _currentNavTab.value = tab
        when (tab) {
            AppNavTab.CATEGORIES -> loadCategoryFiles(_selectedCategory.value)
            AppNavTab.STORAGE -> refreshStorageStats()
            AppNavTab.EXPLORER -> loadDirectory(_currentDirectory.value)
            else -> {}
        }
    }

    fun navigateTo(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return
        _currentDirectory.value = directory
        rebuildBreadcrumbs(directory)
        loadDirectory(directory)
    }

    fun navigateUp(): Boolean {
        val current = _currentDirectory.value
        val parent = current.parentFile
        val root = repository.defaultWorkspaceDir
        // Allow navigating up until parent is null or beyond storage
        if (parent != null && (current.absolutePath != root.absolutePath || parent.canRead())) {
            navigateTo(parent)
            return true
        }
        return false
    }

    fun navigateToRoot() {
        navigateTo(repository.defaultWorkspaceDir)
    }

    fun switchToExternalStorage(): Boolean {
        val ext = repository.externalStorageDir
        if (ext != null && ext.canRead()) {
            navigateTo(ext)
            return true
        }
        return false
    }

    fun switchToWorkspace() {
        navigateTo(repository.defaultWorkspaceDir)
    }

    private fun rebuildBreadcrumbs(current: File) {
        val chain = mutableListOf<File>()
        var curr: File? = current
        val root = repository.defaultWorkspaceDir
        while (curr != null) {
            chain.add(0, curr)
            if (curr.absolutePath == root.absolutePath || curr.parentFile == null) {
                break
            }
            curr = curr.parentFile
        }
        _breadcrumbs.value = chain
    }

    fun refresh() {
        loadDirectory(_currentDirectory.value)
        refreshStorageStats()
        viewModelScope.launch {
            refreshRecentsInternal()
        }
        if (_currentNavTab.value == AppNavTab.CATEGORIES) {
            loadCategoryFiles(_selectedCategory.value)
        }
    }

    private suspend fun refreshRecentsInternal() {
        if (userClearedRecents) return
        val current = _recentsList.value
        val modified = repository.getRecentlyModifiedFiles(15)
        val combined = (current + modified).filter { File(it.path).exists() }.distinctBy { it.path }.take(15)
        _recentsList.value = combined
    }

    private fun loadDirectory(directory: File) {
        viewModelScope.launch {
            val list = repository.getFiles(directory, favoritesSet.value)
            _rawFiles.value = list
        }
    }

    fun setSortOption(option: FileSortOption) {
        _sortOption.value = option
    }

    fun toggleViewMode() {
        _viewMode.value = if (_viewMode.value == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST
    }

    // Search methods
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        executeSearch()
    }

    fun setSearchCategory(category: FileCategory) {
        _searchCategory.value = category
        executeSearch()
    }

    private fun executeSearch() {
        val query = _searchQuery.value
        val cat = _searchCategory.value

        if (query.isBlank() && cat == FileCategory.ALL) {
            _isSearching.value = false
            _searchResults.value = emptyList()
            return
        }

        _isSearching.value = true
        viewModelScope.launch {
            val results = repository.searchFiles(
                query = query,
                rootDir = _currentDirectory.value,
                category = cat,
                favorites = favoritesSet.value
            )
            _searchResults.value = sortFiles(results, _sortOption.value)
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchCategory.value = FileCategory.ALL
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    // Categories
    fun selectCategory(category: FileCategory) {
        _selectedCategory.value = category
        loadCategoryFiles(category)
    }

    private fun loadCategoryFiles(category: FileCategory) {
        viewModelScope.launch {
            val files = repository.getFilesByCategory(
                category = category,
                rootDir = repository.defaultWorkspaceDir,
                favorites = favoritesSet.value
            )
            _categoryFiles.value = sortFiles(files, _sortOption.value)
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val stats = repository.getStorageStats(repository.defaultWorkspaceDir)
            _storageStats.value = stats
        }
    }

    // File Actions
    fun createFolder(name: String) {
        viewModelScope.launch {
            val result = repository.createFolder(_currentDirectory.value, name)
            if (result.isSuccess) {
                _userMessage.emit("Carpeta '$name' creada con éxito")
                refresh()
            } else {
                _userMessage.emit("Error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun createTextFile(name: String, content: String = "") {
        viewModelScope.launch {
            val result = repository.createTextFile(_currentDirectory.value, name, content)
            if (result.isSuccess) {
                val file = result.getOrNull()
                if (file != null) {
                    repository.recordRecent(
                        FileItem(
                            name = file.name,
                            path = file.absolutePath,
                            isDirectory = false,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified(),
                            extension = file.extension,
                            mimeType = "text/plain"
                        )
                    )
                }
                _userMessage.emit("Documento '${file?.name}' creado")
                refresh()
            } else {
                _userMessage.emit("Error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            val target = File(fileItem.path)
            val result = repository.renameFile(target, newName)
            if (result.isSuccess) {
                _userMessage.emit("Renombrado a '${result.getOrNull()?.name}'")
                refresh()
            } else {
                _userMessage.emit("Error al renombrar: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            val target = File(fileItem.path)
            val result = repository.deleteFile(target)
            if (result.isSuccess) {
                _userMessage.emit("'${fileItem.name}' eliminado")
                refresh()
            } else {
                _userMessage.emit("Error al eliminar: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteMultipleFiles(fileItems: List<FileItem>) {
        if (fileItems.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            for (item in fileItems) {
                val target = File(item.path)
                val result = repository.deleteFile(target)
                if (result.isSuccess) {
                    count++
                }
            }
            if (count == fileItems.size) {
                _userMessage.emit("$count elementos eliminados con éxito")
            } else {
                _userMessage.emit("$count de ${fileItems.size} elementos eliminados")
            }
            refresh()
        }
    }

    fun copyItem(fileItem: FileItem) {
        _clipboard.value = ClipboardItem(fileItem, isCut = false)
        viewModelScope.launch {
            _userMessage.emit("'${fileItem.name}' copiado al portapapeles")
        }
    }

    fun cutItem(fileItem: FileItem) {
        _clipboard.value = ClipboardItem(fileItem, isCut = true)
        viewModelScope.launch {
            _userMessage.emit("'${fileItem.name}' cortado para mover")
        }
    }

    fun copyMultipleFiles(fileItems: List<FileItem>) {
        if (fileItems.isEmpty()) return
        _clipboard.value = ClipboardItem(fileItems, isCut = false)
        viewModelScope.launch {
            _userMessage.emit("${fileItems.size} elementos copiados para duplicar/pegar")
        }
    }

    fun cutMultipleFiles(fileItems: List<FileItem>) {
        if (fileItems.isEmpty()) return
        _clipboard.value = ClipboardItem(fileItems, isCut = true)
        viewModelScope.launch {
            _userMessage.emit("${fileItems.size} elementos cortados para mover")
        }
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun pasteToCurrentDirectory() {
        val clip = _clipboard.value ?: return
        viewModelScope.launch {
            val targetDir = _currentDirectory.value
            var successCount = 0
            for (item in clip.items) {
                val source = File(item.path)
                val result = if (clip.isCut) {
                    repository.moveFileOrDirectory(source, targetDir)
                } else {
                    repository.copyFileOrDirectory(source, targetDir)
                }
                if (result.isSuccess) {
                    successCount++
                }
            }

            val actionName = if (clip.isCut) "movido(s)" else "copiado(s)"
            if (successCount == clip.items.size) {
                _userMessage.emit("$successCount elemento(s) $actionName correctamente")
                _clipboard.value = null
            } else {
                _userMessage.emit("$successCount de ${clip.items.size} elemento(s) $actionName")
            }
            refresh()
        }
    }

    fun toggleFavorite(fileItem: FileItem) {
        viewModelScope.launch {
            repository.toggleFavorite(fileItem)
            val msg = if (fileItem.isFavorite) "Eliminado de favoritos" else "Agregado a favoritos"
            _userMessage.emit(msg)
        }
    }

    fun openFile(fileItem: FileItem) {
        viewModelScope.launch {
            userClearedRecents = false
            repository.recordRecent(fileItem)
            if (fileItem.isDirectory) {
                _currentNavTab.value = AppNavTab.EXPLORER
                navigateTo(File(fileItem.path))
            } else {
                _viewingFile.value = fileItem
                if (fileItem.category == FileCategory.DOCUMENTS || isTextFile(fileItem.extension)) {
                    val content = repository.readFileContent(File(fileItem.path))
                    _fileEditorContent.value = content
                }
            }
        }
    }

    fun closeViewer() {
        _viewingFile.value = null
        _fileEditorContent.value = ""
    }

    fun updateEditorContent(newContent: String) {
        _fileEditorContent.value = newContent
    }

    fun saveFileContent() {
        val viewing = _viewingFile.value ?: return
        viewModelScope.launch {
            val file = File(viewing.path)
            val result = repository.saveFileContent(file, _fileEditorContent.value)
            if (result.isSuccess) {
                userClearedRecents = false
                repository.recordRecent(viewing)
                _userMessage.emit("Archivo guardado correctamente")
                refresh()
            } else {
                _userMessage.emit("Error al guardar: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun showFileDetails(fileItem: FileItem) {
        _detailsFile.value = fileItem
    }

    fun closeFileDetails() {
        _detailsFile.value = null
    }

    fun clearRecents() {
        viewModelScope.launch {
            userClearedRecents = true
            repository.clearRecents()
            _recentsList.value = emptyList()
            _userMessage.emit("Historial de recientes vaciado")
        }
    }

    private fun isTextFile(extension: String): Boolean {
        val lower = extension.lowercase(Locale.ROOT)
        return lower in listOf("txt", "md", "json", "xml", "html", "css", "js", "ts", "kt", "java", "csv", "log", "conf", "ini", "yaml", "yml")
    }

    private fun sortFiles(files: List<FileItem>, sort: FileSortOption): List<FileItem> {
        val (dirs, normalFiles) = files.partition { it.isDirectory }

        val sortedDirs = when (sort) {
            FileSortOption.NAME_ASC -> dirs.sortedBy { it.name.lowercase(Locale.ROOT) }
            FileSortOption.NAME_DESC -> dirs.sortedByDescending { it.name.lowercase(Locale.ROOT) }
            FileSortOption.DATE_DESC -> dirs.sortedByDescending { it.lastModified }
            FileSortOption.DATE_ASC -> dirs.sortedBy { it.lastModified }
            FileSortOption.SIZE_DESC -> dirs.sortedByDescending { it.itemCount }
            FileSortOption.SIZE_ASC -> dirs.sortedBy { it.itemCount }
        }

        val sortedFiles = when (sort) {
            FileSortOption.NAME_ASC -> normalFiles.sortedBy { it.name.lowercase(Locale.ROOT) }
            FileSortOption.NAME_DESC -> normalFiles.sortedByDescending { it.name.lowercase(Locale.ROOT) }
            FileSortOption.DATE_DESC -> normalFiles.sortedByDescending { it.lastModified }
            FileSortOption.DATE_ASC -> normalFiles.sortedBy { it.lastModified }
            FileSortOption.SIZE_DESC -> normalFiles.sortedByDescending { it.sizeBytes }
            FileSortOption.SIZE_ASC -> normalFiles.sortedBy { it.sizeBytes }
        }

        return sortedDirs + sortedFiles
    }
}
