package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ExcludedFolderEntity
import com.example.data.local.FolderShortcutEntity
import com.example.data.local.MediaEntity
import com.example.scanner.LocalMediaScanner
import com.example.storage.FileCategoryFilter
import com.example.storage.FileSortField
import com.example.storage.FileSystemEntry
import com.example.storage.SafRootEntry
import com.example.storage.ScopedFileManager
import com.example.ui.components.BnjActiveFilterChipsBar
import com.example.ui.components.BnjArtworkSurface
import com.example.ui.components.BnjCategoryChipsBar
import com.example.ui.components.BnjEmptyFilterStateCard
import com.example.ui.components.BnjEmptyStateCard
import com.example.ui.components.BnjFileFilterBottomSheet
import com.example.ui.components.BnjLibraryTopToolbar
import com.example.ui.components.BnjSortBottomSheet
import com.example.ui.components.FileFilterState
import com.example.ui.components.MediaViewType
import com.example.ui.theme.BnjIcons
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GlassAmberTint
import com.example.ui.theme.GlassEmeraldTint
import com.example.ui.theme.GlassRoseTint
import com.example.ui.theme.LiquidCyan
import com.example.ui.theme.LiquidGlassCard
import com.example.ui.theme.LiquidGlassIconButton
import com.example.ui.theme.LiquidGlassPillBadge
import com.example.ui.theme.LiquidGlassPillButton
import com.example.ui.theme.LocalDynamicBottomInsets
import com.example.ui.theme.GlassTier
import com.example.ui.theme.LocalLiquidGlassConfig
import com.example.ui.theme.adaptiveGlassAccent
import com.example.ui.theme.bnjGlassTextFieldColors
import com.example.ui.theme.dynamicGlassDialogContainerColor
import com.example.ui.theme.resolveContextualAccent
import com.example.ui.theme.resolveDynamicGlassPalette
import com.example.ui.theme.LocalLiquidGlassConfig
import java.io.File
import java.util.Locale

private enum class FileQuickHubCategory(val label: String, val icon: ImageVector) {
    ALL("All Files", BnjIcons.FilesSelected),
    RECENT("Recent Files", BnjIcons.History),
    AUDIO("Audio", BnjIcons.MusicSelected),
    VIDEOS("Videos", BnjIcons.VideoSelected),
    IMAGES("Images", Icons.Default.Image),
    DOCUMENTS("Documents", Icons.Default.Description),
    DOWNLOADS("Downloads", Icons.Default.Download),
    FOLDERS("Folders", BnjIcons.FilesUnselected),
    PINNED("Pinned", Icons.Default.Bookmark)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FileManagerScreen(
    currentPath: String,
    entries: List<FileSystemEntry>,
    searchQuery: String,
    viewType: MediaViewType,
    sortField: FileSortField,
    sortAscending: Boolean,
    filterState: FileFilterState,
    folderShortcuts: List<FolderShortcutEntity>,
    excludedFolders: List<ExcludedFolderEntity>,
    showHiddenFiles: Boolean = false,
    safRoots: List<SafRootEntry> = emptyList(),
    onToggleShowHidden: (Boolean) -> Unit = {},
    onMountSafFolder: (Uri) -> Unit = {},
    onRemoveSafRoot: (String) -> Unit = {},
    onCopyOrMoveEntries: (List<String>, String, Boolean) -> Unit = { _, _, _ -> },
    onNavigateToFolder: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectViewType: (MediaViewType) -> Unit,
    onSelectSort: (FileSortField, Boolean) -> Unit,
    onApplyFilterState: (FileFilterState) -> Unit,
    onRemoveFilterChip: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    onRenameEntry: (String, String) -> Unit,
    onDeleteEntry: (String) -> Unit,
    onCreateSubfolder: (String) -> Unit,
    onPinShortcut: (String) -> Unit,
    onRemoveShortcut: (String) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onRestoreExcludedFolder: (String) -> Unit,
    onPlayMediaFile: (MediaEntity) -> Unit,
    onOpenPlayableEntry: ((FileSystemEntry) -> Unit)? = null,
    isLoading: Boolean = false,
    isRefreshing: Boolean = false,
    errorMessage: String? = null,
    hasLoadedCurrentPath: Boolean = true,
    onRetryLoad: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val rootShortcuts = remember { ScopedFileManager.defaultRootRoots(context) }

    var selectedQuickTab by rememberSaveable { mutableStateOf(FileQuickHubCategory.ALL) }
    var pinnedFilePaths by rememberSaveable { mutableStateOf(setOf<String>()) }
    var selectedPaths by remember { mutableStateOf(setOf<String>()) }

    var renameTarget by remember { mutableStateOf<FileSystemEntry?>(null) }
    var renameText by remember { mutableStateOf("") }
    var deleteConfirmTargets by remember { mutableStateOf<List<FileSystemEntry>>(emptyList()) }
    var detailsTarget by remember { mutableStateOf<FileSystemEntry?>(null) }
    var pendingCopyOrMove by remember { mutableStateOf<Pair<List<FileSystemEntry>, Boolean>?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var showSortSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val safTreeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            onMountSafFolder(treeUri)
        }
    }

    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    val parentPath = remember(currentPath) {
        if (currentPath.startsWith("content://")) null else File(currentPath).parentFile?.absolutePath
    }
    val isCurrentFolderPinned = remember(currentPath, folderShortcuts) {
        folderShortcuts.any { it.folderPath == currentPath }
    }

    // System BackHandler for multi-select, quick tab filter, or folder up navigation
    BackHandler(enabled = selectedPaths.isNotEmpty() || selectedQuickTab != FileQuickHubCategory.ALL) {
        when {
            selectedPaths.isNotEmpty() -> selectedPaths = emptySet()
            selectedQuickTab != FileQuickHubCategory.ALL -> selectedQuickTab = FileQuickHubCategory.ALL
        }
    }

    // Merge quick tab selection with explicit filterState
    val effectiveCategories = remember(selectedQuickTab, filterState.selectedCategories) {
        val tabCat = when (selectedQuickTab) {
            FileQuickHubCategory.RECENT -> setOf(FileCategoryFilter.RECENT_FILES)
            FileQuickHubCategory.AUDIO -> setOf(FileCategoryFilter.AUDIO)
            FileQuickHubCategory.VIDEOS -> setOf(FileCategoryFilter.VIDEO)
            FileQuickHubCategory.IMAGES -> setOf(FileCategoryFilter.IMAGES)
            FileQuickHubCategory.DOCUMENTS -> setOf(FileCategoryFilter.DOCUMENTS)
            FileQuickHubCategory.FOLDERS -> setOf(FileCategoryFilter.FOLDER)
            else -> emptySet()
        }
        filterState.selectedCategories + tabCat
    }

    val filteredAndSortedEntries = remember(
        entries,
        searchQuery,
        effectiveCategories,
        selectedQuickTab,
        pinnedFilePaths,
        folderShortcuts,
        sortField,
        sortAscending
    ) {
        val base = ScopedFileManager.filterAndSortEntries(
            entries = entries,
            searchQuery = searchQuery,
            activeCategories = effectiveCategories,
            sortField = sortField,
            ascending = sortAscending
        )
        if (selectedQuickTab == FileQuickHubCategory.PINNED) {
            val shortcutPaths = folderShortcuts.map { it.folderPath }.toSet()
            base.filter { it.path in pinnedFilePaths || it.path in shortcutPaths }
        } else {
            base
        }
    }

    val activeChips = remember(filterState) { filterState.toActiveChips() }
    val sortSummary = "${sortField.label} (${if (sortAscending) "Asc" else "Desc"})"
    val bottomPadding = LocalDynamicBottomInsets.current + 28.dp

    val toggleSelectEntry: (FileSystemEntry) -> Unit = { entry ->
        selectedPaths = if (selectedPaths.contains(entry.path)) {
            selectedPaths - entry.path
        } else {
            selectedPaths + entry.path
        }
    }

    val togglePinEntry: (FileSystemEntry) -> Unit = { entry ->
        if (entry.isDirectory) {
            if (folderShortcuts.any { it.folderPath == entry.path }) {
                onRemoveShortcut(entry.path)
            } else {
                onNavigateToFolder(entry.path)
                onPinShortcut(entry.name)
            }
        } else {
            pinnedFilePaths = if (pinnedFilePaths.contains(entry.path)) {
                pinnedFilePaths - entry.path
            } else {
                pinnedFilePaths + entry.path
            }
        }
    }

    val shareEntries: (List<FileSystemEntry>) -> Unit = { targets ->
        if (targets.isNotEmpty()) {
            try {
                val first = targets.first()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = first.mimeType.ifBlank { "*/*" }
                    putExtra(Intent.EXTRA_SUBJECT, first.name)
                    if (first.isSafUri || first.path.startsWith("content://")) {
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(first.path))
                    } else {
                        putExtra(Intent.EXTRA_TEXT, "${first.name} (${first.formattedSize})\n${first.path}")
                    }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share ${first.name}").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Throwable) {}
        }
    }

    val handleOpenEntry: (FileSystemEntry) -> Unit = { entry ->
        if (selectedPaths.isNotEmpty()) {
            toggleSelectEntry(entry)
        } else if (entry.isDirectory) {
            onNavigateToFolder(entry.path)
        } else {
            val isAudio = FileCategoryFilter.AUDIO.matches(entry)
            val isVideo = FileCategoryFilter.VIDEO.matches(entry)
            if (isAudio || isVideo) {
                if (onOpenPlayableEntry != null) {
                    onOpenPlayableEntry(entry)
                } else {
                    val uriStr = if (entry.isSafUri || entry.path.startsWith("content://")) {
                        entry.path
                    } else {
                        Uri.fromFile(File(entry.path)).toString()
                    }
                    onPlayMediaFile(
                        MediaEntity(
                            uri = uriStr,
                            title = entry.name.substringBeforeLast('.').ifBlank { entry.name },
                            artist = "Local File Manager",
                            album = entry.path.substringBeforeLast('/').substringAfterLast('/').ifBlank { "Folder" },
                            sizeBytes = entry.sizeBytes,
                            mimeType = entry.mimeType,
                            codecName = entry.codecBadge,
                            containerFormat = entry.extension,
                            isVideo = isVideo,
                            durationMs = 0L
                        )
                    )
                }
            } else {
                detailsTarget = entry
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("file_manager_screen")
    ) {
        Spacer(Modifier.height(10.dp))

        // ====================================================================
        // 1. COMPACT LIQUID GLASS HEADER
        // ====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Files",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                LiquidGlassPillBadge(
                    text = "${filteredAndSortedEntries.size}",
                    color = LiquidCyan
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val glassConfig = LocalLiquidGlassConfig.current
                val fmPrimary = adaptiveGlassAccent(
                    resolveContextualAccent(LiquidCyan, glassConfig),
                    glassConfig.isDarkGlass
                )
                val fmSecondary = adaptiveGlassAccent(
                    resolveContextualAccent(ElectricIndigo, glassConfig),
                    glassConfig.isDarkGlass
                )
                LiquidGlassIconButton(
                    icon = if (showHiddenFiles) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (showHiddenFiles) "Hide Dotfiles" else "Show Hidden Files",
                    onClick = { onToggleShowHidden(!showHiddenFiles) },
                    selected = showHiddenFiles,
                    tint = fmSecondary,
                    size = 34.dp,
                    iconSize = 17.dp,
                    modifier = Modifier.testTag("files_toggle_hidden_button")
                )
                LiquidGlassIconButton(
                    icon = Icons.Default.FolderOpen,
                    contentDescription = "Mount External / SAF Folder",
                    onClick = { safTreeLauncher.launch(null) },
                    tint = fmPrimary,
                    size = 34.dp,
                    iconSize = 17.dp,
                    modifier = Modifier.testTag("files_add_saf_button")
                )
                LiquidGlassIconButton(
                    icon = Icons.Default.CreateNewFolder,
                    contentDescription = "Create Subfolder",
                    onClick = { showNewFolderDialog = true },
                    tint = GlassEmeraldTint,
                    size = 34.dp,
                    iconSize = 17.dp,
                    modifier = Modifier.testTag("file_manager_new_folder_button")
                )
                LiquidGlassIconButton(
                    icon = if (isCurrentFolderPinned) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                    contentDescription = if (isCurrentFolderPinned) "Unpin Folder Shortcut" else "Pin Folder Shortcut",
                    onClick = {
                        if (isCurrentFolderPinned) {
                            onRemoveShortcut(currentPath)
                        } else {
                            val pinLabel = if (currentPath.startsWith("content://")) {
                                Uri.decode(currentPath).substringAfterLast(':').substringAfterLast('/').ifBlank { "SAF Folder" }
                            } else {
                                currentPath.substringAfterLast("/")
                            }
                            onPinShortcut(pinLabel)
                        }
                    },
                    selected = isCurrentFolderPinned,
                    tint = fmPrimary,
                    size = 34.dp,
                    iconSize = 17.dp,
                    modifier = Modifier.testTag("file_manager_pin_folder_button")
                )
                LiquidGlassIconButton(
                    icon = Icons.Default.Block,
                    contentDescription = "Exclude Folder from Media Scan",
                    onClick = { onExcludeFolder(currentPath) },
                    tint = GlassAmberTint,
                    size = 34.dp,
                    iconSize = 17.dp,
                    modifier = Modifier.testTag("file_manager_exclude_folder_button")
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ====================================================================
        // 2. TOP TOOLBAR (SEARCH, VIEW TYPE, SORT, FILTER)
        // ====================================================================
        BnjLibraryTopToolbar(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            searchPlaceholder = "Search files, extensions, folders...",
            searchTestTag = "file_manager_search_input",
            currentViewType = viewType,
            onSelectViewType = onSelectViewType,
            sortButtonLabel = sortSummary,
            onOpenSortSheet = { showSortSheet = true },
            activeFilterCount = filterState.activeFilterCount,
            onOpenFilterSheet = { showFilterSheet = true },
            tagPrefix = "files"
        )

        Spacer(Modifier.height(6.dp))

        // ====================================================================
        // 3. CATEGORY HUB TABS (Recent, Audio, Videos, Images, Documents, Downloads, Folders, Pinned)
        // ====================================================================
        BnjCategoryChipsBar(
            categories = FileQuickHubCategory.entries,
            selectedCategory = selectedQuickTab,
            labelProvider = { it.label },
            iconProvider = { it.icon },
            onSelectCategory = { tab ->
                selectedQuickTab = tab
                if (tab == FileQuickHubCategory.DOWNLOADS) {
                    val downloadsDir = try {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    } catch (_: Throwable) {
                        null
                    }
                    if (downloadsDir != null && downloadsDir.exists() && downloadsDir.canRead()) {
                        onNavigateToFolder(ScopedFileManager.normalizePath(downloadsDir.absolutePath))
                    }
                }
            },
            modifier = Modifier.testTag("files_category_chips")
        )

        Spacer(Modifier.height(6.dp))

        // ====================================================================
        // 4. STORAGE ROOTS & PINNED SHORTCUTS ROW
        // ====================================================================
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(rootShortcuts, key = { (_, path) -> "root_$path" }) { (label, path) ->
                LiquidGlassPillButton(
                    onClick = {
                        selectedQuickTab = FileQuickHubCategory.ALL
                        onNavigateToFolder(path)
                    },
                    selected = ScopedFileManager.normalizePath(currentPath) == path,
                    accentColor = LiquidCyan,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(BnjIcons.FilesSelected, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
            items(safRoots, key = { it.treeUriString }) { safRoot ->
                val docRootUri = remember(safRoot.treeUriString) {
                    ScopedFileManager.buildSafRootDocumentUri(Uri.parse(safRoot.treeUriString)).toString()
                }
                LiquidGlassPillButton(
                    onClick = {
                        selectedQuickTab = FileQuickHubCategory.ALL
                        onNavigateToFolder(docRootUri)
                    },
                    selected = currentPath == docRootUri,
                    accentColor = ElectricIndigo,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(safRoot.label, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Unmount ${safRoot.label}",
                        modifier = Modifier
                            .size(13.dp)
                            .clickable { onRemoveSafRoot(safRoot.treeUriString) }
                    )
                }
            }
            items(folderShortcuts, key = { it.folderPath }) { shortcut ->
                LiquidGlassPillButton(
                    onClick = {
                        selectedQuickTab = FileQuickHubCategory.ALL
                        onNavigateToFolder(shortcut.folderPath)
                    },
                    selected = currentPath == shortcut.folderPath,
                    accentColor = GlassEmeraldTint,
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(shortcut.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ====================================================================
        // 5. INTERACTIVE BREADCRUMB BAR
        // ====================================================================
        FileBreadcrumbBar(
            currentPath = currentPath,
            parentPath = parentPath,
            onNavigateToFolder = onNavigateToFolder
        )

        Spacer(Modifier.height(8.dp))

        // Active Removable Filter Chips
        BnjActiveFilterChipsBar(
            chips = activeChips,
            onRemoveChip = onRemoveFilterChip,
            onClearAll = onClearAllFilters,
            tagPrefix = "files"
        )

        if (activeChips.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
        }

        // ====================================================================
        // 6. MULTI-SELECT BATCH OPERATIONS BAR
        // ====================================================================
        if (selectedPaths.isNotEmpty()) {
            val selectedEntries = remember(entries, selectedPaths) {
                entries.filter { selectedPaths.contains(it.path) }
            }
            LiquidGlassCard(
                accentTint = LiquidCyan,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("files_multiselect_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selAccent = adaptiveGlassAccent(
                        resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                        LocalLiquidGlassConfig.current.isDarkGlass
                    )
                    Text(
                        text = "${selectedPaths.size} selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = selAccent
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        val fileTargets = selectedEntries.filter { !it.isDirectory }
                        if (fileTargets.isNotEmpty()) {
                            LiquidGlassPillButton(
                                onClick = { pendingCopyOrMove = fileTargets to false },
                                accentColor = ElectricIndigo,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                            }
                            LiquidGlassPillButton(
                                onClick = { pendingCopyOrMove = fileTargets to true },
                                accentColor = GlassAmberTint,
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Move", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        LiquidGlassPillButton(
                            onClick = { shareEntries(selectedEntries) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Share", style = MaterialTheme.typography.labelSmall)
                        }
                        LiquidGlassPillButton(
                            onClick = { deleteConfirmTargets = selectedEntries },
                            accentColor = GlassRoseTint,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = GlassRoseTint)
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", style = MaterialTheme.typography.labelSmall, color = GlassRoseTint)
                        }
                        LiquidGlassPillButton(
                            onClick = {
                                selectedPaths = if (selectedPaths.size == filteredAndSortedEntries.size) {
                                    emptySet()
                                } else {
                                    filteredAndSortedEntries.map { it.path }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (selectedPaths.size == filteredAndSortedEntries.size) "None" else "All",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(
                            onClick = { selectedPaths = emptySet() },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = BnjIcons.Close,
                                contentDescription = "Clear Selection",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 7. MAIN FILE BROWSER CONTENT AREA (GRID / LIST / COMPACT / DETAILED)
        // ====================================================================
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading && entries.isEmpty() -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("files_loading_indicator")
                    ) {
                        LiquidGlassCard(
                            accentTint = LiquidCyan,
                            contentPadding = PaddingValues(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val loadAccent = adaptiveGlassAccent(
                                    resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                    LocalLiquidGlassConfig.current.isDarkGlass
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = loadAccent
                                )
                                Column {
                                    Text(
                                        text = "Loading folder contents...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = currentPath,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        repeat(3) {
                            LiquidGlassCard(
                                contentPadding = PaddingValues(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                    )
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.55f)
                                                .height(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.32f)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                errorMessage != null && entries.isEmpty() -> {
                    LiquidGlassCard(
                        accentTint = GlassRoseTint,
                        contentPadding = PaddingValues(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("files_error_state")
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Folder Error",
                                    tint = GlassRoseTint,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Unable to Load Folder",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LiquidGlassPillButton(
                                onClick = onRetryLoad,
                                accentColor = LiquidCyan,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("files_error_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Retry", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                entries.isEmpty() && hasLoadedCurrentPath -> {
                    val isSafDir = currentPath.startsWith("content://")
                    val hasPermissions = LocalMediaScanner.hasMediaPermissions(context)
                    val isAppPrivateDir = currentPath.startsWith(context.filesDir.absolutePath)
                    val isInaccessible = if (isSafDir) {
                        false
                    } else {
                        val dir = File(currentPath)
                        !dir.exists() || !dir.canRead()
                    }

                    val (statusTitle, statusDetail) = when {
                        isSafDir -> {
                            "This SAF Folder is Empty" to
                                "No files or subfolders were found inside the mounted document tree."
                        }
                        !hasPermissions && !isAppPrivateDir -> {
                            "Media Storage Permission Required" to
                                "Grant Audio & Video permissions in Settings or the Home screen, or tap Mount SAF Folder above."
                        }
                        isInaccessible -> {
                            "Folder Restricted by Android Scoped Storage" to
                                "Direct file listing in '$currentPath' is restricted by Android Scoped Storage. Switch to App Studio Media or mount a SAF folder."
                        }
                        else -> {
                            "This Folder is Empty" to
                                "No files or subfolders exist in '$currentPath' yet."
                        }
                    }

                    BnjEmptyStateCard(
                        icon = BnjIcons.FilesSelected,
                        title = statusTitle,
                        subtitle = statusDetail,
                        actionLabel = if (!isAppPrivateDir && rootShortcuts.isNotEmpty()) "Open App Studio Media" else null,
                        onActionClick = if (!isAppPrivateDir && rootShortcuts.isNotEmpty()) {
                            { onNavigateToFolder(rootShortcuts.first().second) }
                        } else null
                    )
                }

                entries.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("files_loading_indicator"),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val emptyLoadAccent = adaptiveGlassAccent(
                            resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                            LocalLiquidGlassConfig.current.isDarkGlass
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.padding(top = 24.dp).size(24.dp),
                            color = emptyLoadAccent
                        )
                    }
                }

                filteredAndSortedEntries.isEmpty() -> {
                    BnjEmptyFilterStateCard(
                        title = "No Matching Files or Folders",
                        subtitle = "No entries in this folder match your current search query or active category filters.",
                        onClearFilters = {
                            selectedQuickTab = FileQuickHubCategory.ALL
                            onSearchQueryChange("")
                            onClearAllFilters()
                        },
                        tagPrefix = "files"
                    )
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isRefreshing) {
                            val refreshAccent = adaptiveGlassAccent(
                                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                LocalLiquidGlassConfig.current.isDarkGlass
                            )
                            LinearProgressIndicator(
                                color = refreshAccent,
                                trackColor = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .padding(bottom = 4.dp)
                                    .testTag("files_refreshing_indicator")
                            )
                        }
                        if (errorMessage != null) {
                            LiquidGlassCard(
                                accentTint = GlassRoseTint,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("files_error_state")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = errorMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = GlassRoseTint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    LiquidGlassPillButton(
                                        onClick = onRetryLoad,
                                        accentColor = LiquidCyan,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.testTag("files_error_retry_button")
                                    ) {
                                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        AnimatedContent(
                            targetState = viewType,
                            label = "files_view_type_transition",
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) { targetViewType ->
                    when (targetViewType) {
                        MediaViewType.GRID -> {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 152.dp),
                                state = gridState,
                                contentPadding = PaddingValues(bottom = bottomPadding),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("files_grid_container")
                            ) {
                                gridItems(filteredAndSortedEntries, key = { it.path }) { entry ->
                                    val isSelected = selectedPaths.contains(entry.path)
                                    val isPinned = pinnedFilePaths.contains(entry.path) ||
                                        folderShortcuts.any { it.folderPath == entry.path }
                                    FileGridItemCard(
                                        entry = entry,
                                        isSelected = isSelected,
                                        isPinned = isPinned,
                                        onClick = { handleOpenEntry(entry) },
                                        onLongClick = { toggleSelectEntry(entry) },
                                        onRename = {
                                            renameTarget = entry
                                            renameText = entry.name
                                        },
                                        onDelete = { deleteConfirmTargets = listOf(entry) },
                                        onMoreOptions = { detailsTarget = entry }
                                    )
                                }
                            }
                        }

                        MediaViewType.COMPACT -> {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = bottomPadding),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("files_compact_container")
                            ) {
                                items(filteredAndSortedEntries, key = { it.path }) { entry ->
                                    val isSelected = selectedPaths.contains(entry.path)
                                    val isPinned = pinnedFilePaths.contains(entry.path) ||
                                        folderShortcuts.any { it.folderPath == entry.path }
                                    FileCompactItemRow(
                                        entry = entry,
                                        isSelected = isSelected,
                                        isPinned = isPinned,
                                        onClick = { handleOpenEntry(entry) },
                                        onLongClick = { toggleSelectEntry(entry) },
                                        onShare = { shareEntries(listOf(entry)) },
                                        onRename = {
                                            renameTarget = entry
                                            renameText = entry.name
                                        },
                                        onDelete = { deleteConfirmTargets = listOf(entry) },
                                        onMoreOptions = { detailsTarget = entry }
                                    )
                                }
                            }
                        }

                        MediaViewType.DETAILED -> {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = bottomPadding),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("files_detailed_container")
                            ) {
                                items(filteredAndSortedEntries, key = { it.path }) { entry ->
                                    val isSelected = selectedPaths.contains(entry.path)
                                    val isPinned = pinnedFilePaths.contains(entry.path) ||
                                        folderShortcuts.any { it.folderPath == entry.path }
                                    FileDetailedItemCard(
                                        entry = entry,
                                        isSelected = isSelected,
                                        isPinned = isPinned,
                                        onClick = { handleOpenEntry(entry) },
                                        onLongClick = { toggleSelectEntry(entry) },
                                        onShare = { shareEntries(listOf(entry)) },
                                        onRename = {
                                            renameTarget = entry
                                            renameText = entry.name
                                        },
                                        onDelete = { deleteConfirmTargets = listOf(entry) },
                                        onMoreOptions = { detailsTarget = entry }
                                    )
                                }
                            }
                        }

                        MediaViewType.LIST -> {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(bottom = bottomPadding),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("files_list_container")
                            ) {
                                items(filteredAndSortedEntries, key = { it.path }) { entry ->
                                    val isSelected = selectedPaths.contains(entry.path)
                                    val isPinned = pinnedFilePaths.contains(entry.path) ||
                                        folderShortcuts.any { it.folderPath == entry.path }
                                    FileListItemCard(
                                        entry = entry,
                                        isSelected = isSelected,
                                        isPinned = isPinned,
                                        onClick = { handleOpenEntry(entry) },
                                        onLongClick = { toggleSelectEntry(entry) },
                                        onShare = { shareEntries(listOf(entry)) },
                                        onRename = {
                                            renameTarget = entry
                                            renameText = entry.name
                                        },
                                        onDelete = { deleteConfirmTargets = listOf(entry) },
                                        onMoreOptions = { detailsTarget = entry }
                                    )
                                }

                                if (excludedFolders.isNotEmpty()) {
                                    item {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "Excluded Folders (Hidden from MediaStore Scan)",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassAmberTint
                                        )
                                    }
                                    items(excludedFolders, key = { it.folderPath }) { excluded ->
                                        LiquidGlassCard(
                                            accentTint = GlassAmberTint,
                                            contentPadding = PaddingValues(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = excluded.folderPath,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                LiquidGlassPillButton(
                                                    onClick = { onRestoreExcludedFolder(excluded.folderPath) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Restore", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                        }
                    }
                }
            }
        }
    }

    // Sort Bottom Sheet
    if (showSortSheet) {
        BnjSortBottomSheet(
            title = "Sort File Manager",
            options = FileSortField.entries,
            selectedOption = sortField,
            optionLabel = { it.label },
            ascending = sortAscending,
            onSelectSort = onSelectSort,
            onDismiss = { showSortSheet = false },
            tagPrefix = "files"
        )
    }

    // Filter Bottom Sheet
    if (showFilterSheet) {
        BnjFileFilterBottomSheet(
            initialFilter = filterState,
            onApplyFilter = onApplyFilterState,
            onDismiss = { showFilterSheet = false }
        )
    }

    // Rename Dialog
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("Rename File / Folder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    label = { Text("New Name") },
                    colors = bnjGlassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        renameTarget?.let { target ->
                            if (renameText.isNotBlank()) {
                                onRenameEntry(target.path, renameText.trim())
                            }
                        }
                        renameTarget = null
                    }
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (deleteConfirmTargets.isNotEmpty()) {
        val count = deleteConfirmTargets.size
        val firstTarget = deleteConfirmTargets.first()
        AlertDialog(
            onDismissRequest = { deleteConfirmTargets = emptyList() },
            containerColor = dynamicGlassDialogContainerColor(),
            title = {
                Text(
                    text = if (count == 1) "Delete '${firstTarget.name}'?" else "Delete $count Selected Items?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (count == 1) {
                        "This will permanently remove '${firstTarget.name}' from device storage."
                    } else {
                        "This will permanently remove $count selected files/folders from device storage."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteConfirmTargets.forEach { target ->
                            onDeleteEntry(target.path)
                        }
                        selectedPaths = selectedPaths - deleteConfirmTargets.map { it.path }.toSet()
                        deleteConfirmTargets = emptyList()
                    }
                ) {
                    Text("Delete", color = GlassRoseTint, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmTargets = emptyList() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // New Subfolder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            containerColor = dynamicGlassDialogContainerColor(),
            title = { Text("Create New Subfolder", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    singleLine = true,
                    label = { Text("Folder Name") },
                    colors = bnjGlassTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateSubfolder(newFolderName.trim())
                            newFolderName = ""
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Copy / Move Destination Folder Picker Dialog
    pendingCopyOrMove?.let { (targets, isMove) ->
        val destinations = remember(rootShortcuts, safRoots, folderShortcuts, entries, parentPath, currentPath) {
            buildList {
                rootShortcuts.forEach { (lbl, path) ->
                    if (path != currentPath) add(lbl to path)
                }
                safRoots.forEach { saf ->
                    val docUri = ScopedFileManager.buildSafRootDocumentUri(Uri.parse(saf.treeUriString)).toString()
                    if (docUri != currentPath) add(saf.label to docUri)
                }
                folderShortcuts.forEach { sc ->
                    if (sc.folderPath != currentPath) add("Pinned: ${sc.label}" to sc.folderPath)
                }
                parentPath?.let { p ->
                    add("Parent Folder (${File(p).name})" to p)
                }
                entries.filter { it.isDirectory }.forEach { dir ->
                    add("Subfolder: ${dir.name}" to dir.path)
                }
            }.distinctBy { it.second }
        }
        AlertDialog(
            onDismissRequest = { pendingCopyOrMove = null },
            containerColor = dynamicGlassDialogContainerColor(),
            title = {
                Text(
                    text = if (isMove) "Move ${targets.size} Item(s) To..." else "Copy ${targets.size} Item(s) To...",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Select a destination folder:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (destinations.isEmpty()) {
                        Text(
                            text = "No destination folders available. Mount a SAF folder or create a subfolder first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GlassAmberTint
                        )
                    } else {
                        destinations.forEach { (label, destPath) ->
                            val destAccent = adaptiveGlassAccent(
                                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                                LocalLiquidGlassConfig.current.isDarkGlass
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(destAccent.copy(alpha = 0.12f))
                                    .clickable {
                                        onCopyOrMoveEntries(targets.map { it.path }, destPath, isMove)
                                        selectedPaths = emptySet()
                                        pendingCopyOrMove = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = BnjIcons.FilesSelected,
                                    contentDescription = null,
                                    tint = destAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = destPath,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pendingCopyOrMove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // File Details & Operations Bottom Sheet
    detailsTarget?.let { entry ->
        val isPinned = pinnedFilePaths.contains(entry.path) ||
            folderShortcuts.any { it.folderPath == entry.path }
        FileDetailsBottomSheet(
            entry = entry,
            isPinned = isPinned,
            isSelected = selectedPaths.contains(entry.path),
            onDismiss = { detailsTarget = null },
            onOpenOrPlay = {
                detailsTarget = null
                if (entry.isDirectory) {
                    onNavigateToFolder(entry.path)
                } else if (onOpenPlayableEntry != null) {
                    onOpenPlayableEntry(entry)
                } else {
                    val uriStr = if (entry.isSafUri || entry.path.startsWith("content://")) {
                        entry.path
                    } else {
                        Uri.fromFile(File(entry.path)).toString()
                    }
                    val isVideo = FileCategoryFilter.VIDEO.matches(entry)
                    onPlayMediaFile(
                        MediaEntity(
                            uri = uriStr,
                            title = entry.name.substringBeforeLast('.').ifBlank { entry.name },
                            artist = "Local File Manager",
                            album = entry.path.substringBeforeLast('/').substringAfterLast('/').ifBlank { "Folder" },
                            sizeBytes = entry.sizeBytes,
                            mimeType = entry.mimeType,
                            codecName = entry.codecBadge,
                            containerFormat = entry.extension,
                            isVideo = isVideo,
                            durationMs = 0L
                        )
                    )
                }
            },
            onShare = {
                detailsTarget = null
                shareEntries(listOf(entry))
            },
            onTogglePin = {
                togglePinEntry(entry)
                detailsTarget = null
            },
            onToggleSelect = {
                toggleSelectEntry(entry)
                detailsTarget = null
            },
            onCopy = {
                detailsTarget = null
                if (!entry.isDirectory) pendingCopyOrMove = listOf(entry) to false
            },
            onMove = {
                detailsTarget = null
                if (!entry.isDirectory) pendingCopyOrMove = listOf(entry) to true
            },
            onRename = {
                detailsTarget = null
                renameTarget = entry
                renameText = entry.name
            },
            onDelete = {
                detailsTarget = null
                deleteConfirmTargets = listOf(entry)
            }
        )
    }
}

@Composable
private fun FileBreadcrumbBar(
    currentPath: String,
    parentPath: String?,
    onNavigateToFolder: (String) -> Unit
) {
    val segments = remember(currentPath) {
        if (currentPath.startsWith("content://")) {
            val decoded = Uri.decode(currentPath)
            val tail = decoded.substringAfterLast(':').substringAfterLast('/').ifBlank { "SAF Folder" }
            listOf("SAF Drive" to currentPath, tail to currentPath)
        } else {
            val parts = currentPath.trim('/').split('/').filter { it.isNotBlank() }
            val result = mutableListOf<Pair<String, String>>()
            var running = ""
            for (part in parts) {
                running = "$running/$part"
                result.add(part to running)
            }
            result
        }
    }

    LiquidGlassCard(
        accentTint = ElectricIndigo,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val breadcrumbAccent = adaptiveGlassAccent(
                resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                LocalLiquidGlassConfig.current.isDarkGlass
            )
            if (parentPath != null) {
                LiquidGlassIconButton(
                    icon = BnjIcons.Back,
                    contentDescription = "Parent Folder",
                    onClick = { onNavigateToFolder(parentPath) },
                    tint = breadcrumbAccent,
                    size = 34.dp,
                    iconSize = 18.dp,
                    modifier = Modifier.testTag("file_manager_up_button")
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                segments.takeLast(4).forEachIndexed { idx, (label, fullPath) ->
                    val isLast = idx == segments.takeLast(4).lastIndex
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium,
                        color = if (isLast) breadcrumbAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onNavigateToFolder(fullPath) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    if (!isLast) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileEntryLeadingVisual(
    entry: FileSystemEntry,
    sizeDp: Int = 48
) {
    val isAudio = FileCategoryFilter.AUDIO.matches(entry)
    val isVideo = FileCategoryFilter.VIDEO.matches(entry)
    val isImage = FileCategoryFilter.IMAGES.matches(entry)
    val isDoc = FileCategoryFilter.DOCUMENTS.matches(entry)
    val isApk = FileCategoryFilter.APK.matches(entry)

    val glassConfig = LocalLiquidGlassConfig.current
    val resolvedPrimary = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    val resolvedSecondary = adaptiveGlassAccent(
        resolveContextualAccent(ElectricIndigo, glassConfig),
        glassConfig.isDarkGlass
    )

    when {
        entry.isDirectory -> {
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(resolvedSecondary.copy(alpha = 0.22f))
                    .border(1.dp, resolvedSecondary.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = BnjIcons.FilesSelected,
                    contentDescription = null,
                    tint = resolvedSecondary,
                    modifier = Modifier.size((sizeDp * 0.52f).dp)
                )
            }
        }
        isAudio || isVideo -> {
            val fileUri = remember(entry.path) {
                if (entry.isSafUri || entry.path.startsWith("content://")) {
                    entry.path
                } else {
                    Uri.fromFile(File(entry.path)).toString()
                }
            }
            BnjArtworkSurface(
                titleSeed = entry.name,
                mediaUri = fileUri,
                isVideo = isVideo,
                shape = RoundedCornerShape(12.dp),
                iconSize = (sizeDp * 0.48f).dp,
                modifier = Modifier.size(sizeDp.dp)
            )
        }
        else -> {
            val (icon, tint) = when {
                isImage -> Icons.Default.Image to resolvedPrimary
                isDoc -> Icons.Default.Description to GlassAmberTint
                isApk -> Icons.Default.Android to GlassEmeraldTint
                else -> Icons.AutoMirrored.Filled.InsertDriveFile to resolvedPrimary
            }
            Box(
                modifier = Modifier
                    .size(sizeDp.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.18f))
                    .border(1.dp, tint.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size((sizeDp * 0.5f).dp)
                )
            }
        }
    }
}

@Composable
private fun FileItemOverflowMenuButton(
    entry: FileSystemEntry,
    isSelected: Boolean,
    onOpenOrPlay: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetails: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = BnjIcons.MoreOptions,
                contentDescription = "File Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = dynamicGlassDialogContainerColor()
        ) {
            DropdownMenuItem(
                text = { Text(if (entry.isDirectory) "Open Folder" else "Play / Open") },
                leadingIcon = {
                    Icon(
                        imageVector = if (entry.isDirectory) BnjIcons.FilesSelected else BnjIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = {
                    expanded = false
                    onOpenOrPlay()
                }
            )
            if (!entry.isDirectory) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        expanded = false
                        onShare()
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("Rename") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    expanded = false
                    onRename()
                }
            )
            DropdownMenuItem(
                text = { Text(if (isSelected) "Deselect" else "Select Item") },
                onClick = {
                    expanded = false
                    onToggleSelect()
                }
            )
            DropdownMenuItem(
                text = { Text("Details & Properties") },
                onClick = {
                    expanded = false
                    onOpenDetails()
                }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = GlassRoseTint) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = GlassRoseTint, modifier = Modifier.size(18.dp)) },
                onClick = {
                    expanded = false
                    onDelete()
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileGridItemCard(
    entry: FileSystemEntry,
    isSelected: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val accent = if (entry.isDirectory) ElectricIndigo else LiquidCyan
    val selectedBorder = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) selectedBorder else Color.Transparent,
                shape = RoundedCornerShape(22.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        accentTint = if (isSelected) LiquidCyan else accent,
        contentPadding = PaddingValues(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileEntryLeadingVisual(entry = entry, sizeDp = 44)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Pinned",
                            tint = GlassEmeraldTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FileItemOverflowMenuButton(
                        entry = entry,
                        isSelected = isSelected,
                        onOpenOrPlay = onClick,
                        onShare = onMoreOptions,
                        onRename = onRename,
                        onToggleSelect = onLongClick,
                        onDelete = onDelete,
                        onOpenDetails = onMoreOptions
                    )
                }
            }
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (entry.isDirectory) "${entry.childCount} items" else "${entry.codecBadge} • ${entry.formattedSize}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileListItemCard(
    entry: FileSystemEntry,
    isSelected: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val accent = if (entry.isDirectory) ElectricIndigo else LiquidCyan
    val selectedBorder = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) selectedBorder else Color.Transparent,
                shape = RoundedCornerShape(22.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        accentTint = if (isSelected) LiquidCyan else accent,
        contentPadding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                FileEntryLeadingVisual(entry = entry, sizeDp = 46)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isPinned) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Pinned",
                                tint = GlassEmeraldTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Text(
                        text = if (entry.isDirectory) {
                            "${entry.childCount} items • ${entry.formattedDate}"
                        } else {
                            "${entry.codecBadge} • ${entry.formattedSize} • ${entry.formattedDate}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            FileItemOverflowMenuButton(
                entry = entry,
                isSelected = isSelected,
                onOpenOrPlay = onClick,
                onShare = onShare,
                onRename = onRename,
                onToggleSelect = onLongClick,
                onDelete = onDelete,
                onOpenDetails = onMoreOptions
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileCompactItemRow(
    entry: FileSystemEntry,
    isSelected: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val config = LocalLiquidGlassConfig.current
    val palette = resolveDynamicGlassPalette(
        config = config,
        tier = if (isSelected) GlassTier.ACTIVE else GlassTier.CLEAR,
        elevated = isSelected
    )
    val rowCorner = (config.glassCornerRadiusDp - 2f).coerceIn(8f, 24f).dp
    val rowBg = if (isSelected) palette.selectedSurfaceColor else palette.flatSurfaceColor
    val rowBorderColor = if (isSelected) palette.selectedBorderColor else palette.borderColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(rowCorner))
            .background(rowBg)
            .then(
                if (isSelected || config.borderGlowAlpha > 0.01f) {
                    Modifier.border(
                        width = 1.dp,
                        color = rowBorderColor,
                        shape = RoundedCornerShape(rowCorner)
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileEntryLeadingVisual(entry = entry, sizeDp = 34)
        Spacer(Modifier.width(10.dp))
        Text(
            text = entry.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (isPinned) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = "Pinned",
                tint = GlassEmeraldTint,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(15.dp)
            )
        }
        val compactSizeAccent = adaptiveGlassAccent(
            resolveContextualAccent(LiquidCyan, config),
            config.isDarkGlass
        )
        Text(
            text = if (entry.isDirectory) "${entry.childCount} items" else entry.formattedSize,
            style = MaterialTheme.typography.labelSmall,
            color = compactSizeAccent
        )
        FileItemOverflowMenuButton(
            entry = entry,
            isSelected = isSelected,
            onOpenOrPlay = onClick,
            onShare = onShare,
            onRename = onRename,
            onToggleSelect = onLongClick,
            onDelete = onDelete,
            onOpenDetails = onMoreOptions
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun FileDetailedItemCard(
    entry: FileSystemEntry,
    isSelected: Boolean,
    isPinned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMoreOptions: () -> Unit
) {
    val glassConfig = LocalLiquidGlassConfig.current
    val accent = if (entry.isDirectory) ElectricIndigo else LiquidCyan
    val selectedBorder = adaptiveGlassAccent(
        resolveContextualAccent(LiquidCyan, glassConfig),
        glassConfig.isDarkGlass
    )
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) selectedBorder else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        accentTint = if (isSelected) LiquidCyan else accent,
        elevated = true,
        contentPadding = PaddingValues(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    FileEntryLeadingVisual(entry = entry, sizeDp = 40)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isPinned) {
                                Icon(
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = "Pinned",
                                    tint = GlassEmeraldTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = entry.path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                FileItemOverflowMenuButton(
                    entry = entry,
                    isSelected = isSelected,
                    onOpenOrPlay = onClick,
                    onShare = onShare,
                    onRename = onRename,
                    onToggleSelect = onLongClick,
                    onDelete = onDelete,
                    onOpenDetails = onMoreOptions
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MetadataTagBadge("Type: ${entry.codecBadge.ifBlank { entry.extension }}", LiquidCyan)
                if (entry.isDirectory) {
                    MetadataTagBadge("Children: ${entry.childCount} items", ElectricIndigo)
                } else {
                    MetadataTagBadge("Size: ${entry.formattedSize}", GlassEmeraldTint)
                    if (entry.mimeType.isNotBlank()) {
                        MetadataTagBadge("MIME: ${entry.mimeType}", ElectricIndigo)
                    }
                }
                MetadataTagBadge("Modified: ${entry.formattedDate}", GlassAmberTint)
                MetadataTagBadge("Created: ${entry.formattedCreatedDate}", MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileDetailsBottomSheet(
    entry: FileSystemEntry,
    isPinned: Boolean,
    isSelected: Boolean,
    onDismiss: () -> Unit,
    onOpenOrPlay: () -> Unit,
    onShare: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleSelect: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = dynamicGlassDialogContainerColor()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FileEntryLeadingVisual(entry = entry, sizeDp = 56)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val sheetPrimary = adaptiveGlassAccent(
                        resolveContextualAccent(LiquidCyan, LocalLiquidGlassConfig.current),
                        LocalLiquidGlassConfig.current.isDarkGlass
                    )
                    Text(
                        text = if (entry.isDirectory) "${entry.childCount} items • Folder" else "${entry.codecBadge} • ${entry.formattedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = sheetPrimary
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))

            // Primary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = onOpenOrPlay,
                    selected = true,
                    accentColor = LiquidCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (entry.isDirectory) BnjIcons.FilesSelected else BnjIcons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (entry.isDirectory) "Open Folder" else "Open / Play",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                LiquidGlassPillButton(
                    onClick = onShare,
                    accentColor = GlassEmeraldTint,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share", style = MaterialTheme.typography.labelLarge)
                }
            }

            // Secondary Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LiquidGlassPillButton(
                    onClick = onTogglePin,
                    selected = isPinned,
                    accentColor = GlassEmeraldTint,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPinned) "Unpin" else "Pin", style = MaterialTheme.typography.labelMedium)
                }

                LiquidGlassPillButton(
                    onClick = onRename,
                    accentColor = LiquidCyan,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rename", style = MaterialTheme.typography.labelMedium)
                }

                LiquidGlassPillButton(
                    onClick = onToggleSelect,
                    selected = isSelected,
                    accentColor = ElectricIndigo,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (isSelected) "Deselect" else "Select",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                LiquidGlassPillButton(
                    onClick = onDelete,
                    accentColor = GlassRoseTint,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = GlassRoseTint)
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium, color = GlassRoseTint)
                }
            }

            if (!entry.isDirectory) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassPillButton(
                        onClick = onCopy,
                        accentColor = ElectricIndigo,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy To...", style = MaterialTheme.typography.labelLarge)
                    }
                    LiquidGlassPillButton(
                        onClick = onMove,
                        accentColor = GlassAmberTint,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Move To...", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))

            Text(
                text = "File Properties",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FilePropertyRow("Name", entry.name)
            FilePropertyRow("Type", if (entry.isDirectory) "Directory / Folder" else entry.codecBadge.ifBlank { entry.extension })
            if (!entry.isDirectory && entry.mimeType.isNotBlank()) {
                FilePropertyRow("MIME Type", entry.mimeType)
            }
            FilePropertyRow("Size", if (entry.isDirectory) "${entry.childCount} items" else entry.formattedSize)
            FilePropertyRow("Modified", entry.formattedDate)
            FilePropertyRow("Created", entry.formattedCreatedDate)
            FilePropertyRow("Path", entry.path)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilePropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
