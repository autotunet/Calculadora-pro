package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class FileCategory(val displayName: String) {
    ALL("Todos"),
    DOCUMENTS("Documentos"),
    PHOTOS("Fotos"),
    AUDIO("Audio"),
    VIDEO("Videos"),
    ARCHIVES("Comprimidos"),
    FOLDERS("Carpetas");

    companion object {
        fun fromExtension(ext: String, isDirectory: Boolean): FileCategory {
            if (isDirectory) return FOLDERS
            val lower = ext.lowercase(Locale.ROOT)
            return when (lower) {
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic" -> PHOTOS
                "pdf", "doc", "docx", "txt", "md", "xls", "xlsx", "ppt", "pptx", "csv", "json", "xml", "html", "rtf", "odt", "epub" -> DOCUMENTS
                "mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus" -> AUDIO
                "mp4", "mkv", "avi", "mov", "webm", "3gp", "flv", "m4v" -> VIDEO
                "zip", "rar", "7z", "tar", "gz", "bz2" -> ARCHIVES
                else -> ALL
            }
        }
    }
}

enum class FileSortOption(val label: String) {
    NAME_ASC("Nombre (A-Z)"),
    NAME_DESC("Nombre (Z-A)"),
    DATE_DESC("Más reciente"),
    DATE_ASC("Más antiguo"),
    SIZE_DESC("Mayor tamaño"),
    SIZE_ASC("Menor tamaño")
}

enum class ViewMode {
    LIST, GRID
}

data class ClipboardItem(
    val items: List<FileItem>,
    val isCut: Boolean // true for move, false for copy
) {
    constructor(fileItem: FileItem, isCut: Boolean) : this(listOf(fileItem), isCut)

    val fileItem: FileItem
        get() = items.firstOrNull() ?: FileItem(
            name = "",
            path = "",
            isDirectory = false,
            sizeBytes = 0L,
            lastModified = 0L,
            extension = "",
            mimeType = "*/*"
        )

    val count: Int
        get() = items.size

    val description: String
        get() = if (items.size == 1) items[0].name else "${items.size} elementos seleccionados"
}

data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val documentsBytes: Long,
    val photosBytes: Long,
    val audioBytes: Long,
    val videoBytes: Long,
    val otherBytes: Long,
    val totalFiles: Int,
    val totalFolders: Int
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String,
    val mimeType: String,
    val itemCount: Int = 0,
    val isFavorite: Boolean = false
) {
    val category: FileCategory
        get() = FileCategory.fromExtension(extension, isDirectory)

    val formattedSize: String
        get() {
            if (isDirectory) {
                return if (itemCount == 1) "1 elemento" else "$itemCount elementos"
            }
            if (sizeBytes < 1024) return "$sizeBytes B"
            val kb = sizeBytes / 1024.0
            if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format(Locale.US, "%.2f GB", gb)
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(lastModified))
        }
}
