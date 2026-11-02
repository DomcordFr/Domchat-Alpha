package me.lucid.domchat.ui.pages.assistant

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import me.lucid.domchat.Screen
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.datastore.Settings
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.utils.truncateCreator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import coil3.request.crossfade
import androidx.compose.ui.graphics.SolidColor
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.text.Regex
import me.lucid.domchat.data.model.Avatar
import me.lucid.domchat.ui.components.chat.CharacterNotesBottomSheet
import me.lucid.domchat.ui.components.chat.formatRoleplayTextWithCdnImages
import me.lucid.domchat.ui.components.chat.cleanHtmlImgToMarkdown
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.components.ui.AppToasterState
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.components.ui.Tag
import me.lucid.domchat.ui.components.ui.ToastType
import me.lucid.domchat.ui.modifier.domChatDialogContainerColor
import me.lucid.domchat.ui.modifier.domChatBlurEffect
import me.lucid.domchat.ui.modifier.domChatSheetContainerColor
import me.lucid.domchat.ui.components.ui.UIAvatar
import me.lucid.domchat.ui.context.LocalNavController
import me.lucid.domchat.ui.context.LocalToaster
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.ui.theme.LocalDarkMode
import me.lucid.domchat.data.ai.models.ModelCatalogService
import me.lucid.domchat.data.ai.models.ModelCatalogSnapshot
import org.koin.compose.koinInject
import me.lucid.domchat.utils.AssistantExportImport
import me.lucid.domchat.utils.navigateToChatPage
import androidx.compose.material.icons.rounded.RecordVoiceOver
import me.lucid.domchat.ui.components.ai.VoiceSelector
import me.lucid.tts.provider.TTSProviderSetting
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import kotlin.uuid.Uuid
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.hooks.HapticPattern

enum class CharacterSortOption {
    NAME, RECENT, CREATOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterHubPage(vm: AssistantVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val modelCatalogService: ModelCatalogService = koinInject()
    val catalogSnapshot by modelCatalogService.snapshotFlow.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val isDark = LocalDarkMode.current

    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(true) }
    var sortOption by remember { mutableStateOf(CharacterSortOption.RECENT) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val sortArrowRotation by animateFloatAsState(
        targetValue = if (sortMenuOpen) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = 400f
        ),
        label = "sort_arrow_rotation"
    )
    var filterMenuOpen by remember { mutableStateOf(false) }
    var selectedTagIds by remember { mutableStateOf(emptySet<String>()) }
    var inspectingAssistant by remember { mutableStateOf<Assistant?>(null) }
    var showingCreatorNotesFor by remember { mutableStateOf<Assistant?>(null) }
    var characterToDelete by remember { mutableStateOf<Assistant?>(null) }
    var showExportDialogFor by remember { mutableStateOf<Assistant?>(null) }

    // Group / folder state
    var selectedGroupId by remember { mutableStateOf<String?>(null) } // null = "All"
    var groupNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var newGroupName by remember { mutableStateOf("") }
    var showNewGroupDialog by remember { mutableStateOf(false) }
    var characterToMove by remember { mutableStateOf<Assistant?>(null) }
    var showMoveToGroupDialog by remember { mutableStateOf(false) }
    var groupSearchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toaster: AppToasterState? = LocalToaster.current
    val haptics = rememberPremiumHaptics()

    var pendingExportJson by remember { mutableStateOf("") }
    var pendingExportBytes by remember { mutableStateOf<ByteArray?>(null) }
    var loadError by remember { mutableStateOf<Throwable?>(null) }

    // Export file launcher (JSON)
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingExportJson.isNotEmpty()) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { 
                        it.write(pendingExportJson.toByteArray())
                    }
                    toaster?.show("Character Card exported successfully")
                } catch (e: Exception) {
                    toaster?.show("Export failed: ${e.message}", ToastType.Error)
                }
                pendingExportJson = ""
            }
        }
    }

    // Export file launcher (PNG)
    val exportPngLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            val bytes = pendingExportBytes
            if (bytes == null) {
                toaster?.show("Export failed: no image data", ToastType.Error)
            } else {
                scope.launch {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { 
                            it.write(bytes)
                        }
                        toaster?.show("SillyTavern PNG Card exported successfully")
                    } catch (e: Exception) {
                        toaster?.show("Export failed: ${e.message}", ToastType.Error)
                    }
                    pendingExportBytes = null
                }
            }
        }
    }

    // Import file launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        AssistantExportImport.parseImport(uri, context)
                    }
                    when (result) {
                        is AssistantExportImport.ImportResult.Success -> {
                            vm.addAssistant(result.assistant)
                            toaster?.show("Imported ${result.assistant.name}")
                        }
                        is AssistantExportImport.ImportResult.Configurable -> {
                            val finalAssistant = result.exportV1?.let {
                                AssistantExportImport.finalizeLastChatImport(
                                    export = it,
                                    context = context,
                                    importMemories = result.hasMemories,
                                    importLorebooks = result.hasLorebooks
                                )
                            } ?: result.assistant
                            vm.addAssistant(finalAssistant)
                            toaster?.show("Imported ${finalAssistant.name}")
                        }
                        is AssistantExportImport.ImportResult.Error -> {
                            toaster?.show(result.message, ToastType.Error)
                        }
                    }
                } catch (e: Exception) {
                    toaster?.show("Import failed: ${e.message}", ToastType.Error)
                }
            }
        }
    }

    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    // 250ms debouncer prevents main-thread keyboard freeze on hundreds of characters
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(250)
            .distinctUntilChanged()
            .collect { debouncedQuery = it.trim().lowercase() }
    }

    var filteredAssistants by remember { mutableStateOf(settings.assistants) }

    // Fast indexed background search: searches name, creator without scanning multi-KB prompt blobs
    LaunchedEffect(settings.assistants, debouncedQuery, sortOption, selectedTagIds) {
        try {
            withContext(Dispatchers.Default) {
                val base = if (debouncedQuery.isEmpty()) {
                    settings.assistants
                } else {
                    settings.assistants.filter { char ->
                        char.name.lowercase().contains(debouncedQuery) ||
                            (char.creator?.lowercase()?.contains(debouncedQuery) == true)
                    }
                }
                val tagFiltered = if (selectedTagIds.isEmpty()) {
                    base
                } else {
                    base.filter { char -> char.tags.any { tagId -> selectedTagIds.contains(tagId.toString()) } }
                }
                val sorted = when (sortOption) {
                    CharacterSortOption.NAME -> tagFiltered.sortedBy { it.name.lowercase() }
                    CharacterSortOption.CREATOR -> tagFiltered.sortedBy { it.creator?.lowercase() ?: "" }
                    CharacterSortOption.RECENT -> {
                        val activityMap = tagFiltered.associateWith { assistant ->
                            runCatching {
                                vm.getAssistantLastUpdateAt(assistant.id)
                            }.getOrNull()
                        }
                        tagFiltered.sortedByDescending { activityMap[it.id] ?: Instant.MIN }
                    }
                }
                val distinctSorted = sorted.distinctBy { it.id }
                withContext(Dispatchers.Main) {
                    loadError = null
                    filteredAssistants = distinctSorted
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                loadError = e
                filteredAssistants = emptyList()
            }
        }
    }

    val collapseBehavior = androidx.compose.material3.TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            me.lucid.domchat.ui.components.nav.OneUITopAppBar(
                title = "Characters",
                scrollBehavior = collapseBehavior,
                navigationIcon = { BackButton() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        val newChar = Assistant(name = "New Character")
                        vm.addAssistant(newChar)
                        navController.navigate(Screen.AssistantDetail(id = newChar.id.toString()))
                    }
                },
                containerColor = Color.White,
                contentColor = Color.Black,
                shape = CircleShape,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Character",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = isGridView,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(90)) },
            contentAlignment = Alignment.TopCenter,
            label = "CharacterGallery",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { gridMode ->
            if (gridMode) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(collapseBehavior.nestedScrollConnection)
                ) {
                    // Header items: scroll away with content (only OneUITopAppBar stays sticky)
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(me.lucid.domchat.ui.theme.AppShapes.SearchField)
                                .border(0.5.dp, me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle, me.lucid.domchat.ui.theme.AppShapes.SearchField),
                            placeholder = { Text("Search by character or creator...", fontSize = 13.5.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF8A8A96)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF8A8A96))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = me.lucid.domchat.ui.theme.AppShapes.SearchField,
                            colors = me.lucid.domchat.ui.theme.appFieldColors()
                        )
                    }

                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF000000))
                                    .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${filteredAssistants.size} Characters",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sort dropdown pill
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF000000))
                                            .border(
                                                0.5.dp,
                                                LiquidGlassTokens.SpecularBorderSubtle,
                                                RoundedCornerShape(20.dp)
                                            ),
                                        color = Color.Transparent,
                                        onClick = { sortMenuOpen = true },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Sort,
                                                contentDescription = "Sort",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = when (sortOption) {
                                                    CharacterSortOption.RECENT -> "Recent"
                                                    CharacterSortOption.NAME -> "Name A-Z"
                                                    CharacterSortOption.CREATOR -> "Creator"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                                contentDescription = "Dropdown",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp).rotate(sortArrowRotation)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = sortMenuOpen,
                                        onDismissRequest = { sortMenuOpen = false },
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = Color(0xFF000000),
                                        modifier = Modifier
                                            .background(Color(0xFF000000))
                                            .border(
                                                0.5.dp,
                                                LiquidGlassTokens.SpecularBorderSubtle,
                                                RoundedCornerShape(20.dp)
                                            )
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Recent",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (sortOption == CharacterSortOption.RECENT) Color.White else Color(0xFFCCCCCC)
                                                )
                                            },
                                            onClick = {
                                                haptics.perform(HapticPattern.Pop)
                                                sortOption = CharacterSortOption.RECENT
                                                sortMenuOpen = false
                                            },
                                            trailingIcon = if (sortOption == CharacterSortOption.RECENT) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Name A-Z",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (sortOption == CharacterSortOption.NAME) Color.White else Color(0xFFCCCCCC)
                                                )
                                            },
                                            onClick = {
                                                haptics.perform(HapticPattern.Pop)
                                                sortOption = CharacterSortOption.NAME
                                                sortMenuOpen = false
                                            },
                                            trailingIcon = if (sortOption == CharacterSortOption.NAME) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Creator",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (sortOption == CharacterSortOption.CREATOR) Color.White else Color(0xFFCCCCCC)
                                                )
                                            },
                                            onClick = {
                                                haptics.perform(HapticPattern.Pop)
                                                sortOption = CharacterSortOption.CREATOR
                                                sortMenuOpen = false
                                            },
                                            trailingIcon = if (sortOption == CharacterSortOption.CREATOR) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                                // Filter pill button
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF000000))
                                        .border(
                                            0.5.dp,
                                            LiquidGlassTokens.SpecularBorderSubtle,
                                            RoundedCornerShape(20.dp)
                                        ),
                                    color = Color.Transparent,
                                    onClick = { filterMenuOpen = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FilterAlt,
                                            contentDescription = "Filter",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (selectedTagIds.isEmpty()) "Filter" else "Filter (${selectedTagIds.size})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "image/png", "image/webp", "*/*"))
                                },
                                shape = AppShapes.ButtonPill,
                                color = Color(0xFF000000),
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileUpload,
                                        contentDescription = "Upload Character",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Upload Character",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                onClick = { isGridView = !isGridView },
                                shape = AppShapes.ButtonPill,
                                color = Color(0xFF000000),
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                        contentDescription = "Toggle View",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isGridView) "List View" else "Grid View",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Group tabs row
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        GroupTabsRow(
                            selectedGroupId = selectedGroupId,
                            groupNames = groupNames,
                            allCount = filteredAssistants.size,
                            onGroupSelected = { groupId ->
                                selectedGroupId = groupId
                            },
                            onNewGroup = {
                                showNewGroupDialog = true
                                newGroupName = ""
                            }
                        )
                    }

                    loadError?.let { error ->
                        item {
                            CharacterHubErrorCard(
                                assistant = Assistant(name = "Character Hub"),
                                error = error
                            )
                        }
                    }

                    items(filteredAssistants, key = { it.id.toString() }) { char ->
                        CharacterPortraitCard(
                            assistant = char,
                            settings = settings,
                            isDark = isDark,
                            onClick = { inspectingAssistant = char },
                            onLongClick = {
                                characterToMove = char
                                showMoveToGroupDialog = true
                            }
                        )
                    }

                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(collapseBehavior.nestedScrollConnection)
                ) {
                    // Header items: scroll away with content (only OneUITopAppBar stays sticky)
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(me.lucid.domchat.ui.theme.AppShapes.SearchField)
                                .border(0.5.dp, me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle, me.lucid.domchat.ui.theme.AppShapes.SearchField),
                            placeholder = { Text("Search by character or creator...", fontSize = 13.5.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF8A8A96)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF8A8A96))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = me.lucid.domchat.ui.theme.AppShapes.SearchField,
                            colors = me.lucid.domchat.ui.theme.appFieldColors()
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF000000))
                                    .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${filteredAssistants.size} Characters",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sort dropdown pill
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF000000))
                                            .border(
                                                0.5.dp,
                                                LiquidGlassTokens.SpecularBorderSubtle,
                                                RoundedCornerShape(20.dp)
                                            ),
                                        color = Color.Transparent,
                                        onClick = { sortMenuOpen = true },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Sort,
                                                contentDescription = "Sort",
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = when (sortOption) {
                                                    CharacterSortOption.RECENT -> "Recent"
                                                    CharacterSortOption.NAME -> "Name A-Z"
                                                    CharacterSortOption.CREATOR -> "Creator"
                                                },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                                contentDescription = "Dropdown",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp).rotate(sortArrowRotation)
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = sortMenuOpen,
                                        onDismissRequest = { sortMenuOpen = false },
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = Color(0xFF000000),
                                        modifier = Modifier
                                            .background(Color(0xFF000000))
                                            .border(
                                                0.5.dp,
                                                LiquidGlassTokens.SpecularBorderSubtle,
                                                RoundedCornerShape(20.dp)
                                            )
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Recent",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (sortOption == CharacterSortOption.RECENT) Color.White else Color(0xFFCCCCCC)
                                                )
                                            },
                                            onClick = {
                                                haptics.perform(HapticPattern.Pop)
                                                sortOption = CharacterSortOption.RECENT
                                                sortMenuOpen = false
                                            },
                                            trailingIcon = if (sortOption == CharacterSortOption.RECENT) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Name A-Z",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (sortOption == CharacterSortOption.NAME) Color.White else Color(0xFFCCCCCC)
                                                )
                                            },
                                            onClick = {
                                                haptics.perform(HapticPattern.Pop)
                                                sortOption = CharacterSortOption.NAME
                                                sortMenuOpen = false
                                            },
                                            trailingIcon = if (sortOption == CharacterSortOption.NAME) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "Creator",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = if (sortOption == CharacterSortOption.CREATOR) Color.White else Color(0xFFCCCCCC)
                                                )
                                            },
                                            onClick = {
                                                haptics.perform(HapticPattern.Pop)
                                                sortOption = CharacterSortOption.CREATOR
                                                sortMenuOpen = false
                                            },
                                            trailingIcon = if (sortOption == CharacterSortOption.CREATOR) {
                                                {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                                // Filter pill button
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF000000))
                                        .border(
                                            0.5.dp,
                                            LiquidGlassTokens.SpecularBorderSubtle,
                                            RoundedCornerShape(20.dp)
                                        ),
                                    color = Color.Transparent,
                                    onClick = { filterMenuOpen = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.FilterAlt,
                                            contentDescription = "Filter",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (selectedTagIds.isEmpty()) "Filter" else "Filter (${selectedTagIds.size})",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "image/png", "image/webp", "*/*"))
                                },
                                shape = AppShapes.ButtonPill,
                                color = Color(0xFF000000),
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.FileUpload,
                                        contentDescription = "Upload Character",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Upload Character",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            Surface(
                                onClick = { isGridView = !isGridView },
                                shape = AppShapes.ButtonPill,
                                color = Color(0xFF000000),
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                                        contentDescription = "Toggle View",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isGridView) "List View" else "Grid View",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Group tabs row
                    item {
                        GroupTabsRow(
                            selectedGroupId = selectedGroupId,
                            groupNames = groupNames,
                            allCount = filteredAssistants.size,
                            onGroupSelected = { groupId ->
                                selectedGroupId = groupId
                            },
                            onNewGroup = {
                                showNewGroupDialog = true
                                newGroupName = ""
                            }
                        )
                    }

                    loadError?.let { error ->
                        item {
                            CharacterHubErrorCard(
                                assistant = Assistant(name = "Character Hub"),
                                error = error
                            )
                        }
                    }

                    items(filteredAssistants, key = { it.id.toString() }) { char ->
                        CharacterListRow(
                            assistant = char,
                            isDark = isDark,
                            onClick = { inspectingAssistant = char },
                            onLongClick = {
                                characterToMove = char
                                showMoveToGroupDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Filter Dialog - glassy tag filter sheet
    if (filterMenuOpen) {
        Dialog(onDismissRequest = { filterMenuOpen = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFF000000))
                    .border(0.5.dp, LiquidGlassTokens.SpecularBorderBrush, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter by Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { filterMenuOpen = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }

                    val allTags = settings.assistantTags
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allTags.forEach { tag ->
                            val isSelected = selectedTagIds.contains(tag.id.toString())
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                                    .border(
                                        0.5.dp,
                                        if (isSelected) LiquidGlassTokens.SpecularBorderBrush else LiquidGlassTokens.SpecularBorderSubtle,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        haptics.perform(HapticPattern.Pop)
                                        selectedTagIds = if (isSelected) {
                                            selectedTagIds - tag.id.toString()
                                        } else {
                                            selectedTagIds + tag.id.toString()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag.name,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = {
                            selectedTagIds = emptySet()
                            filterMenuOpen = false
                        }) {
                            Text("Reset", color = Color(0xFF8A8A96))
                        }
                        TextButton(onClick = { filterMenuOpen = false }) {
                            Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Quick Character Profile Sheet
    inspectingAssistant?.let { char ->
        CharacterInspectorSheet(
            assistant = char,
            settings = settings,
            isDark = isDark,
            ttsProviders = settings.ttsProviders,
            catalogSnapshot = catalogSnapshot,
            onUpdateVoice = { newVoiceId ->
                val updated = char.copy(ttsVoiceId = newVoiceId)
                vm.updateAssistant(updated)
                inspectingAssistant = updated
            },
            onDismiss = { inspectingAssistant = null },
            onStartChat = {
                inspectingAssistant = null
                scope.launch {
                    try {
                        vm.updateSettings(settings.copy(assistantId = char.id))
                        val conversation = withContext(Dispatchers.IO) {
                            vm.createConversation(char.id)
                        }
                        withContext(Dispatchers.Main) {
                            navigateToChatPage(navController, conversation.id)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            loadError = e
                            toaster?.show(
                                "Failed to start chat: ${e.message?.takeIf { it.isNotBlank() } ?: e.toString()}",
                                ToastType.Error
                            )
                        }
                    }
                }
            },
            onEdit = {
                inspectingAssistant = null
                navController.navigate(Screen.AssistantDetail(id = char.id.toString()))
            },
            onViewCreatorNotes = {
                showingCreatorNotesFor = char
            },
            onDuplicate = {
                val cloned = char.copy(
                    id = Uuid.random(),
                    name = "${char.name} (Copy)"
                )
                vm.addAssistant(cloned)
                inspectingAssistant = null
                toaster?.show("Cloned ${char.name}")
            },
            onExport = {
                showExportDialogFor = char
            },
            onDelete = {
                characterToDelete = char
            }
        )
    }

    // Delete confirmation dialog
    characterToDelete?.let { char ->
        AlertDialog(
            onDismissRequest = { characterToDelete = null },
            shape = me.lucid.domchat.ui.theme.AppShapes.Dialog,
            containerColor = me.lucid.domchat.ui.modifier.domChatDialogContainerColor(),
            modifier = Modifier.border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, AppShapes.Dialog),
            title = {
                me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur()
                Text(
                    text = "Delete ${char.name}?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will permanently remove this character from your library. Associated chat sessions will be preserved in your history.",
                    fontSize = 13.5.sp,
                    color = Color(0xFFAAAAAA)
                )
            },
            confirmButton = {
                DomChatGlassPillButton(
                    text = "Delete",
                    destructive = true,
                    onClick = {
                        val toDelete = char
                        characterToDelete = null
                        if (inspectingAssistant?.id == toDelete.id) {
                            inspectingAssistant = null
                        }
                        vm.removeAssistant(toDelete)
                        toaster?.show(
                            message = "Deleted ${toDelete.name}",
                            action = me.lucid.domchat.ui.components.ui.ToastAction(
                                label = "Undo",
                                onClick = { vm.undoRemoveAssistant(toDelete) }
                            )
                        )
                    }
                )
            },
            dismissButton = {
                DomChatGlassPillButton(
                    text = "Cancel",
                    onClick = { characterToDelete = null }
                )
            }
        )
    }

    // Export options dialog
    showExportDialogFor?.let { char ->
        AlertDialog(
            onDismissRequest = { showExportDialogFor = null },
            shape = me.lucid.domchat.ui.theme.AppShapes.Dialog,
            containerColor = me.lucid.domchat.ui.modifier.domChatDialogContainerColor(),
            title = {
                me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur()
                Text(
                    text = "Export ${char.name}",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DomChatGlassPillButton(
                        text = "SillyTavern Character Card (PNG)",
                        onClick = {
                            val target = char
                            showExportDialogFor = null
                            scope.launch {
                                val bytes = withContext(Dispatchers.IO) {
                                    AssistantExportImport.exportToCharacterCardPng(target, context)
                                }
                                if (bytes != null) {
                                    pendingExportBytes = bytes
                                    exportPngLauncher.launch("${target.name.replace(" ", "_")}.png")
                                } else {
                                    toaster?.show("Failed to create PNG card with avatar", ToastType.Error)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    DomChatGlassPillButton(
                        text = "Character Card V2 (JSON)",
                        onClick = {
                            val target = char
                            showExportDialogFor = null
                            scope.launch {
                                val jsonStr = withContext(Dispatchers.IO) {
                                    AssistantExportImport.exportToCharacterCardV2(target, context)
                                }
                                pendingExportJson = jsonStr
                                exportJsonLauncher.launch("${target.name.replace(" ", "_")}.json")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                DomChatGlassPillButton(
                    text = "Close",
                    onClick = { showExportDialogFor = null }
                )
            }
        )
    }

    // Creator Notes Modal
    showingCreatorNotesFor?.let { char ->
        CharacterNotesBottomSheet(
            characterName = char.name,
            creatorName = char.creator,
            characterVersion = char.characterVersion,
            notes = char.creatorNotes.orEmpty(),
            onDismissRequest = { showingCreatorNotesFor = null }
        )
    }

    // New Group Dialog
    NewGroupDialog(
        visible = showNewGroupDialog,
        onDismissRequest = {
            showNewGroupDialog = false
            newGroupName = ""
        },
        groupName = newGroupName,
        onGroupNameChange = { newGroupName = it },
        onCreate = { groupName ->
            val newGroupId = java.util.UUID.randomUUID().toString()
            groupNames = groupNames + (newGroupId to groupName)
            showNewGroupDialog = false
            newGroupName = ""
            toaster?.show("Created group '$groupName'", ToastType.Success)
        }
    )

    // Character Action Popover (long-click)
    characterToMove?.let { char ->
        CharacterActionPopover(
            visible = showMoveToGroupDialog,
            assistant = char,
            groupNames = groupNames,
            selectedGroupId = selectedGroupId,
            onDismiss = {
                showMoveToGroupDialog = false
                characterToMove = null
            },
            onDelete = {
                characterToDelete = char
                characterToMove = null
                showMoveToGroupDialog = false
            },
            onMoveToGroup = { groupId ->
                // Group assignment stored in-memory for session
                showMoveToGroupDialog = false
                characterToMove = null
                selectedGroupId = if (groupId.isBlank()) null else groupId
                toaster?.show("Moved '${char.effectiveDisplayName}' to group", ToastType.Success)
            }
        )
    }

    // Delete confirmation dialog
    characterToDelete?.let { char ->
        DeleteConfirmationDialog(
            visible = true,
            assistant = char,
            onDismiss = { characterToDelete = null },
            onConfirm = {
                vm.removeAssistant(char)
                characterToDelete = null
                toaster?.show(
                    message = "Deleted ${char.effectiveDisplayName}",
                    action = me.lucid.domchat.ui.components.ui.ToastAction(
                        label = "Undo",
                        onClick = { vm.undoRemoveAssistant(char) }
                    )
                )
            }
        )
    }
}

/**
 * 3:4 Portrait Character Card — optimized for anime/RP card art with liquid glass border.
 */
@Composable
fun FullCharacterCardImage(
    name: String,
    avatar: Avatar,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (avatar) {
            is Avatar.Image -> {
                val context = LocalContext.current
                // ImageRequest.build() can throw on a malformed avatar URL; never let one bad
                // avatar crash hub composition — fall back to a null model (AsyncImage is null-safe).
                val request = remember(avatar.url) {
                    try {
                        coil3.request.ImageRequest.Builder(context)
                            .data(avatar.url)
                            .size(360, 520)
                            .crossfade(true)
                            .build()
                    } catch (_: Exception) {
                        null
                    }
                }
                AsyncImage(
                    model = request,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.Crop
                )
            }
            is Avatar.Resource -> {
                AsyncImage(
                    model = avatar.id,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    alignment = Alignment.TopCenter,
                    contentScale = ContentScale.Crop
                )
            }
            is Avatar.Emoji -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF18181B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatar.content, fontSize = 48.sp)
                }
            }
            is Avatar.Dummy -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color(0xFF2C2C2E), Color(0xFF121214)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase().ifBlank { "?" },
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * Tall Vertical Portrait Card — full-bleed avatar with bottom glass scrim,
 * bold display name with right-edge alpha fade, and a horizontal scrollable
 * tag row with a dynamic trailing fade.
 */
@Composable
fun CharacterPortraitCard(
    assistant: Assistant,
    settings: Settings,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    val tagList = assistant.tags
    val tagListState = rememberLazyListState()

    // Resolve tag UUIDs to display names via settings.assistantTags; filter out raw UUIDs and missing names
    val resolvedTagNames = tagList.mapNotNull { tagId ->
        settings?.assistantTags?.find { it.id == tagId }?.name?.takeIf { name ->
            name.isNotBlank() && name.length <= 20 && !name.contains("-")
        }
    }.distinct()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141520))
            .border(
                0.5.dp,
                if (isDark) LiquidGlassTokens.SpecularBorderSubtle
                else Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)),
                RoundedCornerShape(18.dp)
            )
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .clickable(onClick = onClick)
            .longClickable(onLongClick = onLongClick)
    ) {
        // Full-bleed avatar background
        FullCharacterCardImage(
            name = assistant.effectiveDisplayName,
            avatar = assistant.avatar,
            modifier = Modifier.fillMaxSize()
        )

        // Bottom vertical gradient scrim: transparent -> dark glass (0xFF141520) for smooth fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF141520).copy(alpha = 0.85f),
                        )
                    )
                )
        )

        // Bottom content: display name + tag row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Character display name: bold white, max 2 lines, ellipsis overflow.
            val displayName = assistant.effectiveDisplayName
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal scrollable tag pill row
            LazyRow(
                state = tagListState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(resolvedTagNames, key = { it }) { tagName ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF000000))
                            .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                    Text(
                        text = tagName.replace("_", " "),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            }


        }
    }
}

@Composable
fun CharacterListRow(
    assistant: Assistant,
    isDark: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF000000))
            .border(
                width = 0.5.dp,
                brush = LiquidGlassTokens.SpecularBorderSubtle,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .longClickable(onLongClick = onLongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
        ) {
            FullCharacterCardImage(
                name = assistant.effectiveDisplayName,
                avatar = assistant.avatar,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = assistant.effectiveDisplayName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
            if (!assistant.creator.isNullOrBlank()) {
                Text(
                    text = "by ${assistant.creator.truncateCreator(10)}",
                    fontSize = 12.sp,
                    color = Color(0xFF8A8A96)
                )
            }
        }
        val introsCount = assistant.alternateGreetings.size + 1
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF000000))
                .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$introsCount greeting${if (introsCount > 1) "s" else ""}",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(0.92f)
            )
        }
    }
}

/**
 * Crash-proof fallback card rendered when a single character fails to compose.
 * Isolates per-card failures so one bad assistant never takes down the whole hub gallery.
 */
@Composable
fun CharacterHubErrorCard(
    assistant: Assistant,
    error: Throwable
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141520))
            .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f), RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Error,
                contentDescription = null,
                tint = Color(0xFFEF4444).copy(alpha = 0.8f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = assistant.effectiveDisplayName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Failed to load card",
                fontSize = 10.sp,
                color = Color(0xFF8A8A96),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = error.message?.takeIf { it.isNotBlank() } ?: error.toString(),
                fontSize = 10.sp,
                color = Color(0xFFEF4444).copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CharacterInspectorSheet(
    assistant: Assistant,
    settings: Settings? = null,
    isDark: Boolean,
    ttsProviders: List<TTSProviderSetting> = emptyList(),
    catalogSnapshot: ModelCatalogSnapshot? = null,
    onUpdateVoice: ((Uuid?) -> Unit)? = null,
    onDismiss: () -> Unit,
    onStartChat: () -> Unit,
    onEdit: () -> Unit,
    onViewCreatorNotes: () -> Unit,
    onDuplicate: () -> Unit = {},
    onExport: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var isDismissing by remember { mutableStateOf(false) }

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    val sheetShape = me.lucid.domchat.ui.theme.AppShapes.BottomSheet
    // Charter: character popups are always a black glass plate, independent of glass/luminance settings.
    val sheetBgColor = me.lucid.domchat.ui.modifier.domChatDialogContainerColor()
    val contentTextColor = Color(0xFFF2F2F7)
    val cardBgColor = Color(0xFF000000)
    val cardBorderColor = me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
    val haptics = rememberPremiumHaptics()
    var viewerImageUrls by remember { mutableStateOf(emptyList<String>()) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerCurrentPage by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = { dismissWithAnimation() },
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        containerColor = sheetBgColor,
        contentColor = contentTextColor,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0.dp) },
        shape = sheetShape,
        modifier = Modifier.border(0.5.dp, me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderBrush, sheetShape),
    ) {
        me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Hero Image with smooth multi-stop cubic vertical gradient fade into sheet background
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        FullCharacterCardImage(
                            name = assistant.effectiveDisplayName,
                            avatar = assistant.avatar,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            sheetBgColor.copy(alpha = 0.15f),
                                            sheetBgColor.copy(alpha = 0.50f),
                                            sheetBgColor.copy(alpha = 0.85f),
                                            sheetBgColor.copy(alpha = 1f)
                                        )
                                    )
                                )
                            )
                    }
                    // Drag handle overlaid at top center over hero image
                    Box(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 36.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.35f))
                        )
                    }
                }


                // Content area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    // Character Title and Creator
                    Text(
                        text = assistant.effectiveDisplayName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentTextColor
                    )
                    if (!assistant.creator.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Created by ${assistant.creator.truncateCreator(10)}",
                            fontSize = 13.5.sp,
                            color = Color(0xFFA0A0A8)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    val introsCount = assistant.alternateGreetings.size + 1
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(cardBgColor)
                                .border(0.5.dp, cardBorderColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$introsCount scenario greeting${if (introsCount > 1) "s" else ""}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = contentTextColor.copy(alpha = 0.9f)
                            )
                        }
                        if (!assistant.characterVersion.isNullOrBlank()) {
                            val version = assistant.characterVersion ?: ""
                            val isUuidLike = version.length > 20 || version.contains("-")
                            if (!isUuidLike) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cardBgColor)
                                        .border(0.5.dp, cardBorderColor, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "v${assistant.characterVersion}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFBBBBBB)
                                    )
                                }
                            }
                        }
                    }

                    // Horizontal scrollable tag pill row — resolves names from settings.assistantTags
                    val resolvedSheetTags = assistant.tags.mapNotNull { tagId ->
                        settings?.assistantTags?.find { it.id == tagId }?.name?.takeIf { name ->
                            name.isNotBlank() && name.length <= 20 && !name.contains("-")
                        }
                    }.distinct()
                    if (resolvedSheetTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tags",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentTextColor.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val tagRowState = rememberLazyListState()
                        LazyRow(
                            state = tagRowState,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(resolvedSheetTags, key = { it }) { tagName ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF000000))
                                        .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp),
                                ) {
                                    Text(
                                        text = tagName.replace("_", " "),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.92f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }

                    // Top few lines of creator notes as a blurb (HTML-sanitized)
                    val notesBlurb = remember(assistant.creatorNotes) {
                        sanitizeHtml(assistant.creatorNotes)
                            .lineSequence()
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() }
                            ?.take(3)
                            ?.joinToString("\n")
                            .orEmpty()
                    }
                    if (notesBlurb.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = notesBlurb,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color(0xFFC0C0C8),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Primary Action Buttons: Start Chat & Edit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                onStartChat()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Chat, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Chat", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(0.5.dp, cardBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = contentTextColor
                            )
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Secondary Character Management Buttons (Export, Clone, Delete)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExport,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, cardBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentTextColor)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export", fontSize = 12.5.sp)
                        }

                        OutlinedButton(
                            onClick = onDuplicate,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, cardBorderColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = contentTextColor)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clone", fontSize = 12.5.sp)
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.height(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }

                    // Character Voice (TTS) Quick Selector
                    if (ttsProviders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBgColor)
                                .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Character Voice",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = contentTextColor
                                        )
                                        Text(
                                            text = "TTS voice binding for audio",
                                            fontSize = 11.sp,
                                            color = Color(0xFF8A8A96)
                                        )
                                    }
                                }
                                VoiceSelector(
                                    voiceId = assistant.ttsVoiceId,
                                    providers = ttsProviders,
                                    allowClear = true,
                                    onClear = { onUpdateVoice?.invoke(null) },
                                    onSelect = { onUpdateVoice?.invoke(it.id) },
                                    catalogSnapshot = catalogSnapshot
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 3 TABS: 0: Notes, 1: Personality, 2: Greetings
                    var selectedTab by remember { mutableIntStateOf(0) }
                    val tabTitles = listOf("Notes", "Personality", "Greetings")

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBgColor)
                            .border(0.5.dp, cardBorderColor, RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabTitles.forEachIndexed { index, title ->
                            val isSelected = selectedTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) {
                                            Color.White.copy(alpha = 0.16f)
                                        } else Color.Transparent
                                    )
                                    .clickable { selectedTab = index }
                                    .padding(horizontal = 4.dp, vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) contentTextColor else contentTextColor.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            val isForward = targetState > initialState
                            (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { width -> if (isForward) width / 4 else -width / 4 }) togetherWith
                                (fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { width -> if (isForward) -width / 4 else width / 4 })
                        },
                        label = "CharacterHubTabsContent"
                    ) { tab ->
                        when (tab) {
                            0 -> {
                                // Creator Notes Tab
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(cardBgColor)
                                        .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    val notes = assistant.creatorNotes?.trim().orEmpty()
                                    if (notes.isNotBlank()) {
                                        MarkdownBlock(
                                            content = formatRoleplayTextWithCdnImages(
                                                cleanHtmlImgToMarkdown(notes)
                                            )
                                        )
                                    } else {
                                        Text(
                                            text = "No creator notes provided for this character.",
                                            fontSize = 13.sp,
                                            color = Color(0xFF8A8A96)
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Personality Tab
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val promptExcerpt = assistant.systemPrompt.trim()
                                    if (promptExcerpt.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(cardBgColor)
                                                .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Character Definition & System Prompt",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                MarkdownBlock(
                                                    content = formatRoleplayTextWithCdnImages(promptExcerpt)
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "No system prompt or definition configured for this character.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF88888A)
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Opening Greetings Tab with animated carousel
                                val allGreetings = remember(assistant.firstMessage, assistant.alternateGreetings) {
                                    val list = mutableListOf<String>()
                                    if (assistant.firstMessage.isNotBlank() && !isDummyGreeting(assistant.firstMessage)) {
                                        list.add(assistant.firstMessage)
                                    }
                                    list.addAll(assistant.alternateGreetings.filter { !isDummyGreeting(it) })
                                    list
                                }

                                if (allGreetings.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cardBgColor)
                                            .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "No scenario opening greeting set. Starting a chat will open into a freeform conversation.",
                                            fontSize = 12.5.sp,
                                            color = Color(0xFF8A8A96)
                                        )
                                    }
                                } else {
                                    var activeGreetingIndex by remember { mutableIntStateOf(0) }

                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        if (allGreetings.size > 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF000000))
                                                    .border(0.5.dp, cardBorderColor, RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                LazyRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    items(allGreetings.size) { idx ->
                                                        val isGreetingSelected = activeGreetingIndex == idx
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (isGreetingSelected) {
                                                                        Color.White.copy(alpha = 0.35f)
                                                                    } else {
                                                                        Color.White.copy(alpha = 0.05f)
                                                                    }
                                                                )
                                                                .border(
                                                                    0.5.dp,
                                                                    if (isGreetingSelected) {
                                                                        Brush.verticalGradient(listOf(Color.White, Color.White))
                                                                    } else cardBorderColor,
                                                                    RoundedCornerShape(8.dp)
                                                                )
                                                                .clickable { activeGreetingIndex = idx }
                                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                        ) {
                                                            Text(
                                                                text = if (idx == 0) "Intro #1 (Default)" else "Greeting #${idx + 1}",
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isGreetingSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isGreetingSelected) Color.White else contentTextColor.copy(alpha = 0.75f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        val safeIndex = activeGreetingIndex.coerceIn(0, allGreetings.lastIndex)
                                        AnimatedContent(
                                            targetState = safeIndex,
                                            transitionSpec = {
                                                val direction = if (targetState > initialState) 1 else -1
                                                (slideInHorizontally(animationSpec = tween(240)) { width -> width / 3 * direction } + fadeIn(animationSpec = tween(240))) togetherWith
                                                    (slideOutHorizontally(animationSpec = tween(200)) { width -> -width / 3 * direction } + fadeOut(animationSpec = tween(200)))
                                            },
                                            label = "greetings_carousel"
                                        ) { currentIdx ->
                                            val greetingText = allGreetings.getOrNull(currentIdx).orEmpty()
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(cardBgColor)
                                                    .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                                    .padding(16.dp)
                                            ) {
                                                if (greetingText.isNotBlank()) {
                                                    MarkdownBlock(
                                                        content = formatRoleplayTextWithCdnImages(
                                                            cleanHtmlImgToMarkdown(greetingText)
                                                        )
                                                    )
                                                } else {
                                                    Text(
                                                        text = "No greeting content.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(36.dp))
                }
            }

            // Close button floating at top-right with safe margin
            IconButton(
                onClick = { dismissWithAnimation() },
                modifier = Modifier
                    .padding(top = 16.dp, end = 16.dp)
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.50f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Image Viewer Dialog - Swipeable Embedded Image Viewer
    if (viewerOpen && viewerImageUrls.isNotEmpty()) {
        Dialog(
            onDismissRequest = { viewerOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val viewerPagerState = rememberPagerState(initialPage = viewerCurrentPage.coerceIn(0, (viewerImageUrls.size - 1).coerceAtLeast(0))) { viewerImageUrls.size }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f)),
            ) {
                HorizontalPager(
                    state = viewerPagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AsyncImage(
                            model = viewerImageUrls[page],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            alignment = Alignment.Center,
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
                // Page counter indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 40.dp),
                ) {
                    Text(
                        text = "${viewerPagerState.currentPage + 1} / ${viewerImageUrls.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                // Close (X) button top-end corner
                IconButton(
                    onClick = { viewerOpen = false },
                    modifier = Modifier
                        .padding(top = 40.dp, end = 20.dp)
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.50f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/**
 * Strips raw HTML tags and decodes common HTML entities so creator notes display as clean text.
 * Handles <p>, </p>, <br>, <span>, &nbsp; and numeric/named entities without external dependencies.
 */
private fun sanitizeHtml(html: String?): String {
    val text = html ?: return ""
    var result = text
        // Remove opening/closing tags (<p>, </p>, <br>, <span>, </span>, <div>, etc.)
        .replace(Regex("<[^>]*>"), "")
        // Decode named entities
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&#x27;", "'")
        // Decode decimal numeric entities (&#NNN;)
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.let { it.toChar() }?.toString() ?: match.value
        }
        // Decode hex numeric entities (&#xHH;)
        .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            match.groupValues[1].toIntOrNull(16)?.let { it.toChar() }?.toString() ?: match.value
        }
        // Strip raw URLs (http/https) to remove JAI links
        .replace(Regex("https?://[^\\s<>\"]+"), "")
        // Collapse multiple whitespace introduced by tag removal
        .replace(Regex("\\s+"), " ")
        .trim()
    return result
}

/**
 * Represents a segment of content that can be either plain text or an embedded image URL.
 * Used to render text with inline images at their exact positions in the flow.
 */
private sealed interface ContentSegment {
    data class Text(val text: String) : ContentSegment
    data class Image(val url: String) : ContentSegment
}

/**
 * Parses a text string into a list of [ContentSegment]s, extracting markdown images
 * (![alt](url)), HTML images (<img src='url'>), and raw image URLs. Text between images
 * is preserved as [ContentSegment.Text], and each image URL becomes a [ContentSegment.Image].
 */
private fun parseContentWithImages(text: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()

    val markdownRegex = Regex("!\\[([^\\]]*)\\]\\(([^)]+)\\)")
    val htmlImgRegex = Regex("<img[^>]+src=['\"]([^'\"]+)['\"]", RegexOption.IGNORE_CASE)
    val rawUrlRegex = Regex("https?://[^\\s<>\"]+\\.(?:png|jpg|jpeg|webp|gif)", RegexOption.IGNORE_CASE)

    val allMatches = mutableListOf<Triple<Int, Int, String>>()

    markdownRegex.findAll(text).forEach { m ->
        if (m.groupValues.size > 2) {
            allMatches.add(Triple(m.range.first, m.range.last, m.groupValues[2]))
        }
    }
    htmlImgRegex.findAll(text).forEach { m ->
        if (m.groupValues.size > 1) {
            allMatches.add(Triple(m.range.first, m.range.last, m.groupValues[1]))
        }
    }
    rawUrlRegex.findAll(text).forEach { m ->
        allMatches.add(Triple(m.range.first, m.range.last, m.value))
    }

    allMatches.sortBy { it.first }

    val filtered = mutableListOf<Triple<Int, Int, String>>()
    var lastEnd = -1
    for ((start, end, url) in allMatches) {
        if (start > lastEnd) {
            filtered.add(Triple(start, end, url))
            lastEnd = end
        }
    }

    var textPos = 0
    for ((start, end, url) in filtered) {
        if (start > textPos) {
            segments.add(ContentSegment.Text(text.substring(textPos, start)))
        }
        segments.add(ContentSegment.Image(url))
        textPos = end + 1
    }

    if (textPos < text.length) {
        segments.add(ContentSegment.Text(text.substring(textPos)))
    }

    return segments.filter { segment ->
        when (segment) {
            is ContentSegment.Text -> segment.text.isNotBlank()
            is ContentSegment.Image -> segment.url.isNotBlank()
        }
    }
}

/**
 * Returns true if a greeting text is a dummy placeholder (single dot, dash, underscore, "none"/"None"/"null", empty).
 */
private fun isDummyGreeting(text: String?): Boolean {
    val t = text?.trim() ?: return true
    return when (t) {
        "", ".", "..", "...", "-", "_", "none", "None", "null" -> true
        else -> false
    }
}

/**
 * Horizontally scrollable row of glassy group tabs: 'All (count)', custom groups, and '+ New'.
 */
@Composable
private fun GroupTabsRow(
    selectedGroupId: String?,
    groupNames: Map<String, String>,
    allCount: Int,
    onGroupSelected: (String?) -> Unit,
    onNewGroup: () -> Unit
) {
    val groups = groupNames.keys.toList()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" tab
        val allLabel = "All ($allCount)"
        Surface(
            onClick = { onGroupSelected(null) },
            shape = RoundedCornerShape(20.dp),
            color = if (selectedGroupId == null) Color.White.copy(alpha = 0.14f) else Color.Transparent,
            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = allLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selectedGroupId == null) FontWeight.Bold else FontWeight.Normal,
                    color = Color.White
                )
            }
        }

        // Custom group tabs
        groups.forEach { groupId ->
            val label = groupNames[groupId] ?: "Group"
            Surface(
                onClick = { onGroupSelected(groupId) },
                shape = RoundedCornerShape(20.dp),
                color = if (selectedGroupId == groupId) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selectedGroupId == groupId) FontWeight.Bold else FontWeight.Normal,
                        color = Color.White
                    )
                }
            }
        }

        // "+ New" tab
        Surface(
            onClick = onNewGroup,
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "New Group",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "New",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Glassy dialog to create a new group/folder.
 */
@Composable
private fun NewGroupDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    groupName: String,
    onGroupNameChange: (String) -> Unit,
    onCreate: (String) -> Unit
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(Color(0xFF000000))
                    .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "New Group",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Enter a name for this character group.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { onGroupNameChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Group name") },
                        shape = AppShapes.InputField,
                        colors = me.lucid.domchat.ui.theme.appFieldColors()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onDismissRequest() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (groupName.isNotBlank()) {
                                    onCreate(groupName)
                                    onDismissRequest()
                                }
                            },
                            enabled = groupName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF000000),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF000000).copy(alpha = 0.4f),
                                disabledContentColor = Color.White.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
                        ) {
                            Text(text = "Create", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Popover shown on long-press of a character card, with 'Delete' and 'Move to group' options.
 */
@Composable
private fun CharacterActionPopover(
    visible: Boolean,
    assistant: Assistant,
    groupNames: Map<String, String>,
    selectedGroupId: String?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onMoveToGroup: (String) -> Unit
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .background(Color(0xFF000000))
                    .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(28.dp))
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = assistant.effectiveDisplayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Choose an action for this character.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )

                    // Move to group section
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Move to group",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // "All" option
                            Surface(
                                onClick = { onMoveToGroup("") },
                                shape = RoundedCornerShape(20.dp),
                                color = if (selectedGroupId == null) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "All",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            // Custom groups
                            groupNames.forEach { (groupId, groupName) ->
                                Surface(
                                    onClick = { onMoveToGroup(groupId) },
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (selectedGroupId == groupId) Color.White.copy(alpha = 0.14f) else Color.Transparent,
                                    border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = groupName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Delete option
                    Surface(
                        onClick = { onDelete() },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                        border = BorderStroke(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog to confirm deletion of a character.
 */
@Composable
private fun DeleteConfirmationDialog(
    visible: Boolean,
    assistant: Assistant,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = me.lucid.domchat.ui.theme.AppShapes.Dialog,
        containerColor = me.lucid.domchat.ui.modifier.domChatDialogContainerColor(),
        modifier = Modifier.border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, me.lucid.domchat.ui.theme.AppShapes.Dialog),
        title = {
            me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur()
            Text(
                text = "Delete ${assistant.effectiveDisplayName}?",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Text(
                text = "This will permanently remove this character from your library. Associated chat sessions will be preserved in your history.",
                fontSize = 13.5.sp,
                color = Color(0xFFAAAAAA)
            )
        },
        confirmButton = {
            DomChatGlassPillButton(
                text = "Delete",
                destructive = true,
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            )
        },
        dismissButton = {
            DomChatGlassPillButton(
                text = "Cancel",
                onClick = onDismiss
            )
        }
    )
}

