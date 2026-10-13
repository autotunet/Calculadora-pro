package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ads.AdMobConfig
import com.example.model.FileCategory
import com.example.model.FileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Gestor de Archivos Pro", appName)
  }

  @Test
  fun `test file category classification`() {
    assertEquals(FileCategory.PHOTOS, FileCategory.fromExtension("jpg", false))
    assertEquals(FileCategory.PHOTOS, FileCategory.fromExtension("png", false))
    assertEquals(FileCategory.DOCUMENTS, FileCategory.fromExtension("pdf", false))
    assertEquals(FileCategory.DOCUMENTS, FileCategory.fromExtension("txt", false))
    assertEquals(FileCategory.DOCUMENTS, FileCategory.fromExtension("md", false))
    assertEquals(FileCategory.AUDIO, FileCategory.fromExtension("mp3", false))
    assertEquals(FileCategory.VIDEO, FileCategory.fromExtension("mp4", false))
    assertEquals(FileCategory.ARCHIVES, FileCategory.fromExtension("zip", false))
    assertEquals(FileCategory.FOLDERS, FileCategory.fromExtension("", true))
  }

  @Test
  fun `test file item size formatting`() {
    val smallItem = FileItem(
      name = "test.txt",
      path = "/path/test.txt",
      isDirectory = false,
      sizeBytes = 500,
      lastModified = 0L,
      extension = "txt",
      mimeType = "text/plain"
    )
    assertEquals("500 B", smallItem.formattedSize)

    val kbItem = FileItem(
      name = "doc.pdf",
      path = "/path/doc.pdf",
      isDirectory = false,
      sizeBytes = 1536,
      lastModified = 0L,
      extension = "pdf",
      mimeType = "application/pdf"
    )
    assertTrue(kbItem.formattedSize.contains("KB"))
  }

  @Test
  fun `verify AdMob ad units configuration`() {
    assertEquals("ca-app-pub-1495262574338316/2803163788", AdMobConfig.BANNER_UNIT_1)
    assertEquals("ca-app-pub-1495262574338316/2903409205", AdMobConfig.BANNER_UNIT_2)
    assertEquals(6, AdMobConfig.POPUP_AD_UNITS.size)
    assertTrue(AdMobConfig.POPUP_AD_UNITS.contains("ca-app-pub-1495262574338316/2452733430"))
    assertTrue(AdMobConfig.POPUP_AD_UNITS.contains("ca-app-pub-1495262574338316/8970040322"))
    assertTrue(AdMobConfig.POPUP_AD_UNITS.contains("ca-app-pub-1495262574338316/1182179137"))
    assertTrue(AdMobConfig.POPUP_AD_UNITS.contains("ca-app-pub-1495262574338316/4962596335"))
    assertTrue(AdMobConfig.POPUP_AD_UNITS.contains("ca-app-pub-1495262574338316/2714752655"))
    assertTrue(AdMobConfig.POPUP_AD_UNITS.contains("ca-app-pub-1495262574338316/3503687777"))
  }

  @Test
  fun `verify recent files properties and categorization`() {
    val recentFile = FileItem(
      name = "Mi_Documento.txt",
      path = "/workspace/Mi_Documento.txt",
      isDirectory = false,
      sizeBytes = 2048,
      lastModified = System.currentTimeMillis(),
      extension = "txt",
      mimeType = "text/plain"
    )
    assertEquals("Mi_Documento.txt", recentFile.name)
    assertEquals(FileCategory.DOCUMENTS, recentFile.category)
    assertTrue(recentFile.formattedDate.isNotEmpty())
  }
}
