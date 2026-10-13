package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.StoragePermissionRationaleDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.ads.AdMobBanner
import com.example.ads.AdMobConfig
import com.example.ads.AdMobManager
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import com.example.ui.components.LanguageSelectionDialog
import com.example.util.LocaleManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.FileSortOption
import com.example.model.ViewMode
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.ExplorerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.StorageOverviewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppNavTab
import com.example.viewmodel.FileManagerViewModel
import kotlinx.coroutines.flow.collectLatest

import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        LocaleManager.init(this)
        super.onCreate(savedInstanceState)
        try {
            val webViewCache = File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            if (!webViewCache.exists()) {
                webViewCache.mkdirs()
            }
        } catch (_: Exception) {}

        AdMobManager.getInstance(this).initialize()
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FileManagerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerApp(
    viewModel: FileManagerViewModel = viewModel()
) {
    val currentTab by viewModel.currentNavTab.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val currentLanguage by LocaleManager.currentLanguage.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var showPermissionRationaleDialog by remember { mutableStateOf(false) }

    // Runtime permission launcher using ActivityResultContracts.RequestPermission for storage
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.refresh()
        }
    }

    // Android 11+ (API 30) All-files management launcher fallback
    val manageAllFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            viewModel.refresh()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager() || ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, permissionToRequest) == PackageManager.PERMISSION_GRANTED
        }

        if (!hasPermission) {
            showPermissionRationaleDialog = true
        }
    }

    if (showPermissionRationaleDialog) {
        StoragePermissionRationaleDialog(
            onConfirm = {
                showPermissionRationaleDialog = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        manageAllFilesLauncher.launch(intent)
                    } catch (e: Exception) {
                        requestPermissionLauncher.launch(permissionToRequest)
                    }
                } else {
                    requestPermissionLauncher.launch(permissionToRequest)
                }
            },
            onDismiss = {
                showPermissionRationaleDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { selectedLang ->
                LocaleManager.setLanguage(context, selectedLang)
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    // Collect user snackbar messages
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentTab) {
                            AppNavTab.EXPLORER -> stringResource(R.string.app_name)
                            AppNavTab.CATEGORIES -> stringResource(R.string.nav_categories)
                            AppNavTab.FAVORITES -> stringResource(R.string.nav_favorites)
                            AppNavTab.RECENTS -> stringResource(R.string.nav_recents)
                            AppNavTab.STORAGE -> stringResource(R.string.nav_storage)
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    val activeSort by viewModel.sortOption.collectAsState()
                    val viewMode by viewModel.viewMode.collectAsState()
                    var sortMenuExpanded by remember { mutableStateOf(false) }

                    // Language Selector Button
                    IconButton(
                        onClick = { showLanguageDialog = true },
                        modifier = Modifier.testTag("language_selector_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = stringResource(R.string.language_settings),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (currentTab == AppNavTab.EXPLORER || currentTab == AppNavTab.CATEGORIES) {
                        IconButton(
                            onClick = { viewModel.toggleViewMode() },
                            modifier = Modifier.testTag("top_app_bar_view_mode_button")
                        ) {
                            Icon(
                                imageVector = if (viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = if (viewMode == ViewMode.LIST) "Cambiar a cuadrícula" else "Cambiar a lista",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Box {
                        IconButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.testTag("top_app_bar_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = stringResource(R.string.sort_by),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false },
                            modifier = Modifier.testTag("top_app_bar_sort_menu")
                        ) {
                            Text(
                                text = stringResource(R.string.sort_by),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            // Orden por Nombre
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name_asc)) },
                                leadingIcon = {
                                    Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    if (activeSort == FileSortOption.NAME_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption.NAME_ASC)
                                    sortMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_name_desc)) },
                                leadingIcon = {
                                    Icon(Icons.Default.SortByAlpha, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    if (activeSort == FileSortOption.NAME_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption.NAME_DESC)
                                    sortMenuExpanded = false
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Orden por Fecha de Modificación
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_date_desc)) },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    if (activeSort == FileSortOption.DATE_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption.DATE_DESC)
                                    sortMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_date_asc)) },
                                leadingIcon = {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    if (activeSort == FileSortOption.DATE_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption.DATE_ASC)
                                    sortMenuExpanded = false
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            // Orden por Tamaño
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_size_desc)) },
                                leadingIcon = {
                                    Icon(Icons.Default.DataUsage, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    if (activeSort == FileSortOption.SIZE_DESC) {
                                        Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption.SIZE_DESC)
                                    sortMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_size_asc)) },
                                leadingIcon = {
                                    Icon(Icons.Default.DataUsage, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                trailingIcon = {
                                    if (activeSort == FileSortOption.SIZE_ASC) {
                                        Icon(Icons.Default.Check, contentDescription = "Seleccionado", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOption(FileSortOption.SIZE_ASC)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                FileManagerBottomNav(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.setNavTab(it) }
                )
                // Banner de abajo: ca-app-pub-1495262574338316/2803163788
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    AdMobBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        adUnitId = AdMobConfig.BANNER_UNIT_1
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.EXPLORER -> ExplorerScreen(viewModel = viewModel)
                AppNavTab.CATEGORIES -> CategoriesScreen(viewModel = viewModel)
                AppNavTab.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                AppNavTab.RECENTS -> RecentsScreen(viewModel = viewModel)
                AppNavTab.STORAGE -> StorageOverviewScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun FileManagerBottomNav(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        data class NavItem(val tab: AppNavTab, val label: String, val icon: ImageVector)

        val items = listOf(
            NavItem(AppNavTab.EXPLORER, stringResource(R.string.nav_explorer), Icons.Default.Folder),
            NavItem(AppNavTab.CATEGORIES, stringResource(R.string.nav_categories), Icons.Default.Category),
            NavItem(AppNavTab.FAVORITES, stringResource(R.string.nav_favorites), Icons.Default.Star),
            NavItem(AppNavTab.RECENTS, stringResource(R.string.nav_recents), Icons.Default.History),
            NavItem(AppNavTab.STORAGE, stringResource(R.string.nav_storage), Icons.Default.Storage)
        )

        items.forEach { item ->
            val selected = currentTab == item.tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_item_${item.tab.name}")
            )
        }
    }
}
