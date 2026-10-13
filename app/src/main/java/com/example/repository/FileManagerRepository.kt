package com.example.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Environment
import android.os.StatFs
import android.webkit.MimeTypeMap
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteEntity
import com.example.data.local.RecentEntity
import com.example.model.FileCategory
import com.example.model.FileItem
import com.example.model.StorageStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

class FileManagerRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val favoriteDao = db.favoriteDao()
    private val recentDao = db.recentDao()

    val favoritesFlow: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    val recentsFlow: Flow<List<RecentEntity>> = recentDao.getAllRecents()

    val defaultWorkspaceDir: File by lazy {
        val dir = File(context.filesDir, "Mi Espacio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    val externalStorageDir: File? by lazy {
        try {
            val state = Environment.getExternalStorageState()
            if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                Environment.getExternalStorageDirectory()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun initDefaultFilesIfNeeded() = withContext(Dispatchers.IO) {
        val root = defaultWorkspaceDir
        val marker = File(root, ".initialized")
        if (!marker.exists()) {
            try {
                // 1. Carpetas
                val docsDir = File(root, "Documentos").apply { mkdirs() }
                val fotosDir = File(root, "Fotos").apply { mkdirs() }
                val proyectosDir = File(root, "Proyectos").apply { mkdirs() }
                val descargasDir = File(root, "Descargas").apply { mkdirs() }
                val notasDir = File(root, "Notas Rápidas").apply { mkdirs() }

                // Archivos de imagen y documentos en el directorio principal
                generateSamplePhoto(root, "Foto_Perfil.png", "Mi Avatar", 0xFF6366F1.toInt(), 0xFF8B5CF6.toInt())
                generateSamplePhoto(root, "Paisaje_Amanecer.png", "Amanecer", 0xFF0D9488.toInt(), 0xFF06B6D4.toInt())
                File(root, "Bienvenido_Gestor_Pro.txt").writeText(
                    "Bienvenido a Gestor de Archivos Pro.\nExplora tus carpetas locales, fotos y documentos con facilidad."
                )

                // Documentos
                File(docsDir, "Presupuesto_Mensual_2026.txt").writeText(
                    """
                    === PRESUPUESTO MENSUAL 2026 ===
                    
                    INGRESOS:
                    - Salario Principal:   $ 3,500.00
                    - Consultorías:        $   850.00
                    - Total Ingresos:      $ 4,350.00
                    
                    GASTOS FIJOS:
                    - Alquiler / Vivienda: $ 1,200.00
                    - Servicios (Luz, Agua)$   180.00
                    - Internet y Teléfono: $    65.00
                    - Seguro de Salud:     $   210.00
                    
                    GASTOS VARIABLES:
                    - Alimentación y Super:$   550.00
                    - Transporte / Auto:   $   180.00
                    - Ocio y Restaurantes: $   250.00
                    
                    AHORRO E INVERSIÓN:
                    - Fondo de Emergencia: $   500.00
                    - Fondos Indexados:    $ 1,215.00
                    
                    Estado: Finanzas balanceadas y al día.
                    """.trimIndent()
                )

                File(docsDir, "Minuta_Reunion_Estrategica.md").writeText(
                    """
                    # Minuta de Reunión Estratégica
                    **Fecha:** 10 de Octubre de 2026  
                    **Participantes:** Equipo de Desarrollo y Diseño  
                    
                    ## Objetivos
                    1. Lanzamiento del nuevo Gestor de Archivos Inteligente.
                    2. Optimización de la búsqueda instantánea en carpetas locales.
                    3. Integración de previsualización de fotos y visor de documentos.
                    
                    ## Acuerdos y Próximos Pasos
                    - [x] Implementar navegación por migas de pan (breadcrumbs).
                    - [x] Soporte para modo lista y cuadrícula.
                    - [x] Sistema de favoritos y archivos recientes con Room.
                    - [ ] Pruebas finales de rendimiento en almacenamiento masivo.
                    """.trimIndent()
                )

                File(docsDir, "Guia_Gestor_Archivos.txt").writeText(
                    """
                    BIENVENIDO A TU GESTOR DE ARCHIVOS
                    
                    Características Principales:
                    1. BÚSQUEDA RÁPIDA: Busca cualquier archivo al instante desde la barra superior.
                    2. ORGANIZACIÓN: Crea carpetas, renombra, copia, mueve y elimina con facilidad.
                    3. CATEGORÍAS: Filtra rápidamente por Documentos, Fotos, Audio y Videos.
                    4. VISOR INTEGRADO: Lee y edita archivos de texto directamente en la app.
                    5. FAVORITOS: Marca archivos con estrella para acceso inmediato.
                    """.trimIndent()
                )

                // Notas Rápidas
                File(notasDir, "Ideas_Nuevo_Proyecto.txt").writeText(
                    """
                    IDEAS Y LLUVIA DE PROYECTOS:
                    
                    1. Sistema de sincronización local peer-to-peer.
                    2. Compresión inteligente de imágenes sin pérdida.
                    3. Búsqueda semántica de notas y etiquetas personalizadas.
                    4. Modo oscuro de alto contraste para ahorro de batería.
                    """.trimIndent()
                )

                File(notasDir, "Lista_Supermercado.txt").writeText(
                    """
                    COMPRAS PENDIENTES:
                    - Café en grano tostado
                    - Fruta fresca (plátanos, manzanas)
                    - Leche de almendras
                    - Aceite de oliva virgen extra
                    - Avena y frutos secos
                    """.trimIndent()
                )

                // Proyectos
                File(proyectosDir, "configuracion_app.json").writeText(
                    """
                    {
                      "appName": "Gestor de Archivos",
                      "version": "1.0.0",
                      "theme": "modern_material3",
                      "features": {
                        "quickSearch": true,
                        "photoGallery": true,
                        "documentEditor": true,
                        "breadcrumbs": true
                      }
                    }
                    """.trimIndent()
                )

                File(proyectosDir, "Resumen_Tecnico.txt").writeText(
                    "Arquitectura desarrollada en Kotlin 100% nativo con Jetpack Compose y Room Database."
                )

                // Descargas
                File(descargasDir, "factura_servicio_octubre.txt").writeText(
                    "Factura #98432 - Monto: $45.00 - Estado: Pagado - Fecha: 05/10/2026"
                )

                // Crear fotos reales generadas por código para la carpeta Fotos
                generateSamplePhoto(fotosDir, "paisaje_montana.png", "Paisaje de Montaña", 0xFF0D9488.toInt(), 0xFF3B82F6.toInt())
                generateSamplePhoto(fotosDir, "atardecer_costero.png", "Atardecer Costero", 0xFFF59E0B.toInt(), 0xFFF43F5E.toInt())
                generateSamplePhoto(fotosDir, "arquitectura_urbana.png", "Diseño Moderno", 0xFF6366F1.toInt(), 0xFF8B5CF6.toInt())

                // Marcar inicializado
                marker.createNewFile()

                // Insertar favoritos de ejemplo
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        path = docsDir.absolutePath,
                        name = "Documentos",
                        isDirectory = true
                    )
                )
                favoriteDao.insertFavorite(
                    FavoriteEntity(
                        path = fotosDir.absolutePath,
                        name = "Fotos",
                        isDirectory = true
                    )
                )

                // Insertar recientes de ejemplo para acceso rápido
                recentDao.insertRecent(
                    RecentEntity(
                        path = File(root, "Foto_Perfil.png").absolutePath,
                        name = "Foto_Perfil.png",
                        isDirectory = false
                    )
                )
                recentDao.insertRecent(
                    RecentEntity(
                        path = File(docsDir, "Minuta_Reunion_Estrategica.md").absolutePath,
                        name = "Minuta_Reunion_Estrategica.md",
                        isDirectory = false
                    )
                )
                recentDao.insertRecent(
                    RecentEntity(
                        path = File(root, "Bienvenido_Gestor_Pro.txt").absolutePath,
                        name = "Bienvenido_Gestor_Pro.txt",
                        isDirectory = false
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getRecentlyModifiedFiles(limit: Int = 10): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<File>()
        fun scan(dir: File) {
            val list = dir.listFiles() ?: return
            for (f in list) {
                if (f.name.startsWith(".")) continue
                if (f.isFile) {
                    results.add(f)
                } else if (f.isDirectory) {
                    scan(f)
                }
            }
        }
        scan(defaultWorkspaceDir)
        val sorted = results.sortedByDescending { it.lastModified() }.take(limit)
        sorted.map { file ->
            FileItem(
                name = file.name,
                path = file.absolutePath,
                isDirectory = false,
                sizeBytes = file.length(),
                lastModified = file.lastModified(),
                extension = file.extension,
                mimeType = "*/*",
                isFavorite = favoriteDao.isFavorite(file.absolutePath)
            )
        }
    }

    private fun generateSamplePhoto(
        folder: File,
        fileName: String,
        label: String,
        colorStart: Int,
        colorEnd: Int
    ) {
        try {
            val file = File(folder, fileName)
            if (file.exists()) return

            val width = 600
            val height = 450
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            // Fondo degradado simple dibujando bandas
            for (y in 0 until height) {
                val ratio = y.toFloat() / height.toFloat()
                val r = ((colorStart shr 16 and 0xFF) * (1 - ratio) + (colorEnd shr 16 and 0xFF) * ratio).toInt()
                val g = ((colorStart shr 8 and 0xFF) * (1 - ratio) + (colorEnd shr 8 and 0xFF) * ratio).toInt()
                val b = ((colorStart and 0xFF) * (1 - ratio) + (colorEnd and 0xFF) * ratio).toInt()
                paint.color = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
            }

            // Dibujar figuras decorativas
            paint.color = 0x33FFFFFF
            canvas.drawCircle(width * 0.8f, height * 0.3f, 120f, paint)
            canvas.drawCircle(width * 0.2f, height * 0.8f, 160f, paint)

            // Tarjeta central con texto
            val cardRect = RectF(60f, height * 0.65f, width - 60f, height * 0.90f)
            paint.color = 0xAA000000.toInt()
            canvas.drawRoundRect(cardRect, 20f, 20f, paint)

            paint.color = 0xFFFFFFFF.toInt()
            paint.textSize = 32f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(label, width / 2f, height * 0.80f, paint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getFiles(directory: File, favorites: Set<String>): List<FileItem> = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.canRead()) return@withContext emptyList()

        val files = directory.listFiles() ?: return@withContext emptyList()
        files.map { file ->
            mapToFileItem(file, favorites.contains(file.absolutePath))
        }
    }

    suspend fun searchFiles(
        query: String,
        rootDir: File,
        category: FileCategory = FileCategory.ALL,
        favorites: Set<String>
    ): List<FileItem> = withContext(Dispatchers.IO) {
        if (query.isBlank() && category == FileCategory.ALL) return@withContext emptyList()

        val results = mutableListOf<FileItem>()
        val trimmedQuery = query.trim().lowercase(Locale.ROOT)

        fun searchRecursive(dir: File) {
            if (results.size >= 200) return
            val list = dir.listFiles() ?: return
            for (file in list) {
                if (file.name.startsWith(".")) continue
                val isDir = file.isDirectory
                val ext = file.extension.lowercase(Locale.ROOT)
                val itemCategory = FileCategory.fromExtension(ext, isDir)

                val matchesCategory = category == FileCategory.ALL || itemCategory == category
                val matchesQuery = trimmedQuery.isEmpty() || file.name.lowercase(Locale.ROOT).contains(trimmedQuery)

                if (matchesCategory && matchesQuery) {
                    results.add(mapToFileItem(file, favorites.contains(file.absolutePath)))
                }

                if (isDir && results.size < 200) {
                    searchRecursive(file)
                }
            }
        }

        searchRecursive(rootDir)
        results
    }

    suspend fun getFilesByCategory(
        category: FileCategory,
        rootDir: File,
        favorites: Set<String>
    ): List<FileItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileItem>()

        fun scanRecursive(dir: File) {
            if (results.size >= 300) return
            val list = dir.listFiles() ?: return
            for (file in list) {
                if (file.name.startsWith(".")) continue
                val isDir = file.isDirectory
                val ext = file.extension.lowercase(Locale.ROOT)
                val itemCategory = FileCategory.fromExtension(ext, isDir)

                if (itemCategory == category) {
                    results.add(mapToFileItem(file, favorites.contains(file.absolutePath)))
                }

                if (isDir) {
                    scanRecursive(file)
                }
            }
        }

        scanRecursive(rootDir)
        results
    }

    suspend fun createFolder(parentDir: File, folderName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeName = folderName.trim()
            if (safeName.isEmpty()) return@withContext Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
            val target = File(parentDir, safeName)
            if (target.exists()) {
                return@withContext Result.failure(IllegalStateException("Ya existe una carpeta o archivo con ese nombre"))
            }
            if (target.mkdirs()) {
                Result.success(target)
            } else {
                Result.failure(IOException("No se pudo crear la carpeta"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTextFile(parentDir: File, fileName: String, content: String = ""): Result<File> = withContext(Dispatchers.IO) {
        try {
            var name = fileName.trim()
            if (name.isEmpty()) return@withContext Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
            if (!name.contains(".")) {
                name = "$name.txt"
            }
            val target = File(parentDir, name)
            if (target.exists()) {
                return@withContext Result.failure(IllegalStateException("Ya existe un archivo con ese nombre"))
            }
            target.writeText(content)
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renameFile(file: File, newName: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val trimmed = newName.trim()
            if (trimmed.isEmpty()) return@withContext Result.failure(IllegalArgumentException("El nombre no puede estar vacío"))
            val target = File(file.parentFile, trimmed)
            if (target.exists() && target != file) {
                return@withContext Result.failure(IllegalStateException("Ya existe un elemento con el nuevo nombre"))
            }
            if (file.renameTo(target)) {
                // Actualizar favoritos si estaba marcado
                if (favoriteDao.isFavorite(file.absolutePath)) {
                    favoriteDao.removeFavorite(file.absolutePath)
                    favoriteDao.insertFavorite(
                        FavoriteEntity(
                            path = target.absolutePath,
                            name = target.name,
                            isDirectory = target.isDirectory
                        )
                    )
                }
                // Actualizar recientes si estaba presente
                recentDao.removeRecent(file.absolutePath)
                recentDao.insertRecent(
                    RecentEntity(
                        path = target.absolutePath,
                        name = target.name,
                        isDirectory = target.isDirectory
                    )
                )
                Result.success(target)
            } else {
                Result.failure(IOException("No se pudo renombrar el elemento"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val path = file.absolutePath
            val success = if (file.isDirectory) {
                file.deleteRecursively()
            } else {
                file.delete()
            }
            if (success) {
                favoriteDao.removeFavorite(path)
                recentDao.removeRecent(path)
                Result.success(true)
            } else {
                Result.failure(IOException("No se pudo eliminar el elemento"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun copyFileOrDirectory(source: File, targetDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) return@withContext Result.failure(IllegalArgumentException("El elemento de origen no existe"))
            val dest = getUniqueDestination(targetDir, source.name)
            if (source.isDirectory) {
                source.copyRecursively(dest, overwrite = true)
            } else {
                source.copyTo(dest, overwrite = true)
            }
            Result.success(dest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun moveFileOrDirectory(source: File, targetDir: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) return@withContext Result.failure(IllegalArgumentException("El elemento de origen no existe"))
            val dest = getUniqueDestination(targetDir, source.name)
            val success = source.renameTo(dest)
            if (success) {
                // Actualizar base de datos
                if (favoriteDao.isFavorite(source.absolutePath)) {
                    favoriteDao.removeFavorite(source.absolutePath)
                    favoriteDao.insertFavorite(
                        FavoriteEntity(
                            path = dest.absolutePath,
                            name = dest.name,
                            isDirectory = dest.isDirectory
                        )
                    )
                }
                Result.success(dest)
            } else {
                // Fallback: copiar y eliminar
                val copyResult = copyFileOrDirectory(source, targetDir)
                if (copyResult.isSuccess) {
                    deleteFile(source)
                    copyResult
                } else {
                    Result.failure(IOException("No se pudo mover el elemento"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getUniqueDestination(parent: File, originalName: String): File {
        var candidate = File(parent, originalName)
        if (!candidate.exists()) return candidate

        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex > 0) originalName.substring(dotIndex) else ""

        var counter = 1
        while (candidate.exists()) {
            candidate = File(parent, "$baseName ($counter)$extension")
            counter++
        }
        return candidate
    }

    suspend fun readFileContent(file: File): String = withContext(Dispatchers.IO) {
        try {
            if (file.exists() && file.isFile) {
                file.readText()
            } else {
                ""
            }
        } catch (e: Exception) {
            "Error al leer el archivo: ${e.message}"
        }
    }

    suspend fun saveFileContent(file: File, content: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            file.writeText(content)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(fileItem: FileItem) = withContext(Dispatchers.IO) {
        if (fileItem.isFavorite) {
            favoriteDao.removeFavorite(fileItem.path)
        } else {
            favoriteDao.insertFavorite(
                FavoriteEntity(
                    path = fileItem.path,
                    name = fileItem.name,
                    isDirectory = fileItem.isDirectory
                )
            )
        }
    }

    suspend fun recordRecent(fileItem: FileItem) = withContext(Dispatchers.IO) {
        recentDao.insertRecent(
            RecentEntity(
                path = fileItem.path,
                name = fileItem.name,
                isDirectory = fileItem.isDirectory
            )
        )
    }

    suspend fun clearRecents() = withContext(Dispatchers.IO) {
        recentDao.clearRecents()
    }

    suspend fun getStorageStats(rootDir: File): StorageStats = withContext(Dispatchers.IO) {
        var totalFiles = 0
        var totalFolders = 0
        var docBytes = 0L
        var photoBytes = 0L
        var audioBytes = 0L
        var videoBytes = 0L
        var otherBytes = 0L

        fun calculateDir(dir: File) {
            val list = dir.listFiles() ?: return
            for (file in list) {
                if (file.name.startsWith(".")) continue
                if (file.isDirectory) {
                    totalFolders++
                    calculateDir(file)
                } else {
                    totalFiles++
                    val size = file.length()
                    when (FileCategory.fromExtension(file.extension, false)) {
                        FileCategory.DOCUMENTS -> docBytes += size
                        FileCategory.PHOTOS -> photoBytes += size
                        FileCategory.AUDIO -> audioBytes += size
                        FileCategory.VIDEO -> videoBytes += size
                        else -> otherBytes += size
                    }
                }
            }
        }

        calculateDir(rootDir)

        val statFs = try {
            StatFs(rootDir.absolutePath)
        } catch (e: Exception) {
            null
        }

        val totalBytes = statFs?.totalBytes ?: (docBytes + photoBytes + audioBytes + videoBytes + otherBytes + 1024 * 1024 * 100)
        val freeBytes = statFs?.availableBytes ?: (totalBytes - (docBytes + photoBytes + audioBytes + videoBytes + otherBytes))
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

        StorageStats(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            documentsBytes = docBytes,
            photosBytes = photoBytes,
            audioBytes = audioBytes,
            videoBytes = videoBytes,
            otherBytes = otherBytes,
            totalFiles = totalFiles,
            totalFolders = totalFolders
        )
    }

    private fun mapToFileItem(file: File, isFavorite: Boolean): FileItem {
        val isDir = file.isDirectory
        val ext = file.extension
        val mimeType = getMimeType(ext)
        val childCount = if (isDir) file.listFiles()?.size ?: 0 else 0
        val size = if (isDir) 0L else file.length()

        return FileItem(
            name = file.name,
            path = file.absolutePath,
            isDirectory = isDir,
            sizeBytes = size,
            lastModified = file.lastModified(),
            extension = ext,
            mimeType = mimeType,
            itemCount = childCount,
            isFavorite = isFavorite
        )
    }

    private fun getMimeType(extension: String): String {
        if (extension.isBlank()) return "*/*"
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ROOT))
        return mime ?: "*/*"
    }
}
