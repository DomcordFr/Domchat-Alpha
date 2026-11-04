package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.ui.theme.LocalDarkMode
import me.lucid.domchat.data.ai.models.ModelCatalogService
import me.lucid.domchat.data.ai.models.ModelCatalogSnapshot
import org.koin.compose.koinInject
import androidx.navigation.compose.rememberNavController
import me.lucid.domchat.R
import me.lucid.domchat.Screen
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.ui.components.ui.UIAvatar
import me.lucid.domchat.ui.hooks.heroAnimation
import me.lucid.domchat.ui.motion.LocalMotionPolicy
import me.lucid.domchat.ui.motion.hierarchicalEnterTransition
import me.lucid.domchat.ui.motion.hierarchicalExitTransition
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.pages.setting.LocalSettingsWideLayout
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import me.lucid.domchat.data.model.Tag as DataTag
import me.lucid.domchat.ui.pages.chat.ExportOptionsDialog
import me.lucid.domchat.ui.context.LocalNavController
import me.lucid.domchat.utils.AssistantExportImport
import me.lucid.domchat.ui.context.LocalToaster
import me.lucid.domchat.ui.components.ui.ToastAction


// Sub-routes within assistant detail
private object AssistantDetailRoutes {
    const val HOME = "home"
    const val CORE = "core"
    const val PROFILE = "profile"
    const val PERSONALITY = "personality"
    const val SCENARIO = "scenario"
    const val CREATOR_NOTES = "creator_notes"
    const val GREETINGS = "greetings"
    const val LOREBOOKS = "lorebooks"
    const val MODEL = "model"
    const val CONTEXT_MANAGEMENT = "context_management"
    const val UI = "ui"
    const val ADVANCED = "advanced"
    const val MEMORY = "memory"
    const val SKILLS = "skills"
    const val TOOLS = "tools"
}

@Composable
fun AssistantDetailPage(
    id: String,
    startRoute: String? = null,
    initialMemoryTab: Int? = null,
    scrollToMemoryId: Int? = null
) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val modelCatalogService: ModelCatalogService = koinInject()
    val catalogSnapshot by modelCatalogService.snapshotFlow.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val rootNavController = LocalNavController.current
    val motionPolicy = LocalMotionPolicy.current
    val toaster = LocalToaster.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val settings by vm.settings.collectAsStateWithLifecycle()
    val mcpServerConfigs by vm.mcpServerConfigs.collectAsStateWithLifecycle()
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val memories by vm.memories.collectAsStateWithLifecycle()
    val providers by vm.providers.collectAsStateWithLifecycle()
    val tags by vm.tags.collectAsStateWithLifecycle()
    val snackbarMessage by vm.snackbarMessage.collectAsStateWithLifecycle()

    val hasMemories by vm.hasMemories.collectAsStateWithLifecycle()
    val hasLorebooks by vm.hasLorebooks.collectAsStateWithLifecycle()

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    // Export state
    var showExportMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showExportOptionsDialog by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pendingExportContent by remember { androidx.compose.runtime.mutableStateOf("") }
    var pendingExportBytes by remember { androidx.compose.runtime.mutableStateOf<ByteArray?>(null) }

    // Export file launcher (JSON)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingExportContent.isNotEmpty()) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(pendingExportContent.toByteArray())
                    }
                    toaster.show(context.getString(R.string.export_success))
                } catch (e: Exception) {
                    e.printStackTrace()
                    toaster.show(
                        context.getString(
                            R.string.export_failed_message,
                            e.message ?: context.getString(R.string.backup_page_unknown_error)
                        )
                    )
                }
                pendingExportContent = ""
            }
        }
    }

    // Export file launcher (PNG)
    val pngExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null && pendingExportBytes != null) {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use {
                        it.write(pendingExportBytes ?: ByteArray(0))
                    }
                    toaster.show(context.getString(R.string.export_success))
                } catch (e: Exception) {
                    e.printStackTrace()
                    toaster.show(
                        context.getString(
                            R.string.export_failed_message,
                            e.message ?: context.getString(R.string.backup_page_unknown_error)
                        )
                    )
                }
                pendingExportBytes = null
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            vm.clearSnackbarMessage()
        }
    }

    // Auto-navigate to start route if specified (e.g., for deep linking to memory)
    LaunchedEffect(startRoute) {
        if (startRoute == AssistantDetailRoutes.MEMORY) {
            navController.navigate(AssistantDetailRoutes.MEMORY) {
                popUpTo(AssistantDetailRoutes.HOME) { inclusive = false }
            }
        }
    }

    fun onUpdate(assistant: Assistant) {
        vm.update(assistant)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = currentRoute != null && currentRoute != AssistantDetailRoutes.HOME,
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(0))
                    ) {
                        Text(
                            text = assistant.name.ifBlank {
                                stringResource(R.string.assistant_page_default_assistant)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    AnimatedVisibility(
                        visible = currentRoute == AssistantDetailRoutes.HOME,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(
                                    imageVector = Icons.Rounded.Upload,
                                    contentDescription = stringResource(R.string.assistant_detail_export)
                                )
                            }

                            val isDark = LocalDarkMode.current
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false },
                                shape = RoundedCornerShape(18.dp),
                                containerColor = if (isDark) Color(0xFF0E0F18).copy(alpha = 0.97f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.assistant_detail_export_lastchat_bundle)) },
                                    onClick = {
                                        showExportMenu = false
                                        if (hasMemories || hasLorebooks) {
                                            showExportOptionsDialog = true
                                        } else {
                                            scope.launch {
                                                try {
                                                    val content = AssistantExportImport.exportToLastChatBundle(
                                                        assistant = assistant,
                                                        context = context,
                                                        includeMemories = false,
                                                        includeLorebooks = false
                                                    )
                                                    pendingExportContent = content
                                                    val fileName = AssistantExportImport.getSuggestedFileName(assistant, "lastchat")
                                                    exportLauncher.launch(fileName)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    toaster.show(
                                                        context.getString(
                                                            R.string.export_failed_message,
                                                            e.message ?: context.getString(R.string.backup_page_unknown_error)
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.assistant_detail_export_card_json)) },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            try {
                                                val content = AssistantExportImport.exportToCharacterCardV2(assistant, context)
                                                pendingExportContent = content
                                                val fileName = AssistantExportImport.getSuggestedFileName(assistant, "card_v2")
                                                exportLauncher.launch(fileName)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                toaster.show(
                                                    context.getString(
                                                        R.string.export_failed_message,
                                                        e.message ?: context.getString(R.string.backup_page_unknown_error)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.assistant_detail_export_card_png)) },
                                    onClick = {
                                        showExportMenu = false
                                        scope.launch {
                                            try {
                                                val bytes = AssistantExportImport.exportToCharacterCardPng(assistant, context)
                                                if (bytes != null) {
                                                    pendingExportBytes = bytes
                                                    val fileName = AssistantExportImport.getSuggestedFileName(assistant, "card_v2_png")
                                                    pngExportLauncher.launch(fileName)
                                                } else {
                                                    toaster.show(context.getString(R.string.export_failed_create_png))
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                toaster.show(
                                                    context.getString(
                                                        R.string.export_failed_message,
                                                        e.message ?: context.getString(R.string.backup_page_unknown_error)
                                                    )
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AssistantDetailRoutes.HOME,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            enterTransition = {
                hierarchicalEnterTransition(
                    direction = AnimatedContentTransitionScope.SlideDirection.Left,
                    motionPolicy = motionPolicy
                )
            },
            exitTransition = {
                hierarchicalExitTransition(
                    direction = AnimatedContentTransitionScope.SlideDirection.Left,
                    motionPolicy = motionPolicy
                )
            },
            popEnterTransition = {
                hierarchicalEnterTransition(
                    direction = AnimatedContentTransitionScope.SlideDirection.Right,
                    motionPolicy = motionPolicy
                )
            },
            popExitTransition = {
                hierarchicalExitTransition(
                    direction = AnimatedContentTransitionScope.SlideDirection.Right,
                    motionPolicy = motionPolicy
                )
            }
        ) {
            composable(AssistantDetailRoutes.HOME) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantDetailHome(
                        assistant = assistant,
                        navController = navController,
                    )
                }
            }

            // Profile (avatar, display & chat name, creator notes, tags)
            composable(AssistantDetailRoutes.PROFILE) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantProfileSubPage(
                        assistant = assistant,
                        tags = tags,
                        onUpdate = { onUpdate(it) },
                        vm = vm
                    )
                }
            }

            // Personality (system prompt)
            composable(AssistantDetailRoutes.PERSONALITY) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantPersonalitySubPage(
                        assistant = assistant,
                        onUpdate = { onUpdate(it) }
                    )
                }
            }

            // Scenario (author's note)
            composable(AssistantDetailRoutes.SCENARIO) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantScenarioSubPage(
                        assistant = assistant,
                        onUpdate = { onUpdate(it) }
                    )
                }
            }

            // Greetings (initial message + alternate greetings)
            composable(AssistantDetailRoutes.GREETINGS) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantGreetingsSubPage(
                        assistant = assistant,
                        onUpdate = { onUpdate(it) }
                    )
                }
            }

            // Example Dialogues & SillyTavern settings
            composable(AssistantDetailRoutes.EXAMPLE_DIALOGUES) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantExampleDialoguesSubPage(
                        assistant = assistant,
                        onUpdate = { onUpdate(it) }
                    )
                }
            }

            // Lorebooks (nested under Context Management)
            composable(AssistantDetailRoutes.LOREBOOKS) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantLorebooksSubPage(
                        assistant = assistant,
                        onUpdate = { onUpdate(it) },
                        vm = vm
                    )
                }
            }

            // Core Roleplay shell (retained for route compatibility)
            composable(AssistantDetailRoutes.CORE) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantCoreRoleplaySubPage(
                        assistant = assistant,
                        tags = tags,
                        providers = providers,
                        settings = settings,
                        onUpdate = { onUpdate(it) },
                        vm = vm,
                        catalogSnapshot = catalogSnapshot,
                        onNavigateToLorebooks = { navController.navigate(AssistantDetailRoutes.LOREBOOKS) },
                        onNavigateToSummarizerSettings = { rootNavController.navigate(Screen.SettingModels) }
                    )
                }
            }

            // Advanced
            composable(AssistantDetailRoutes.ADVANCED) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantAdvancedSubPage(
                        assistant = assistant,
                        onUpdate = { onUpdate(it) }
                    )
                }
            }

            // Model Configuration
            composable(AssistantDetailRoutes.MODEL) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantModelSubPage(
                        assistant = assistant,
                        providers = providers,
                        ttsProviders = settings.ttsProviders,
                        catalogSnapshot = catalogSnapshot,
                        onUpdate = { onUpdate(it) }
                    )
                }
            }

            // Context & Budget / Context Management
            composable(AssistantDetailRoutes.CONTEXT_MANAGEMENT) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantContextManagementSubPage(
                        assistant = assistant,
                        hasSummarizerModelConfigured = false,
                        onUpdate = { onUpdate(it) },
                        onNavigateToLorebooks = { navController.navigate(AssistantDetailRoutes.LOREBOOKS) },
                        onNavigateToSummarizerSettings = { rootNavController.navigate(Screen.SettingModels) }
                    )
                }
            }

            // UI Customization
            composable(AssistantDetailRoutes.UI) {
                AnimatedContent<String?>(targetState = currentRoute) {
                    AssistantUISubPage(
                        assistant = assistant,
                        onUpdate = { onUpdate(it) }
                    )
                }
            }
        }
    }

    if (showExportOptionsDialog) {
        ExportOptionsDialog(
            title = stringResource(R.string.assistant_detail_export_options),
            onDismissRequest = { showExportOptionsDialog = false },
            showMemoriesOption = hasMemories,
            showLorebooksOption = hasLorebooks,
            onConfirm = { includeMemories, includeLorebooks ->
                showExportOptionsDialog = false
                scope.launch {
                    try {
                        val content = AssistantExportImport.exportToLastChatBundle(
                            assistant = assistant,
                            context = context,
                            includeMemories = includeMemories,
                            includeLorebooks = includeLorebooks
                        )
                        pendingExportContent = content
                        val fileName = AssistantExportImport.getSuggestedFileName(assistant, "lastchat")
                        exportLauncher.launch(fileName)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        toaster.show(
                            message = context.getString(
                                R.string.export_failed_message,
                                e.message ?: context.getString(R.string.backup_page_unknown_error)
                            )
                        )
                    }
                }
            }
        )
    }
}

/**
 * Home screen with header and navigation cards
 */
@Composable
private fun AssistantDetailHome(
    assistant: Assistant,
    navController: NavHostController,
) {
    val useWideSettingsLayout = LocalSettingsWideLayout.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ════════════════════════════════════════════════════════════════════
        // HEADER SECTION - Avatar, Name, System Prompt Preview (2 lines, centered)
        // ════════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF141520))
                    .border(1.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape)
                    .let { modifier ->
                        if (useWideSettingsLayout) {
                            modifier
                        } else {
                            modifier.heroAnimation(key = "assistant_avatar_${assistant.id}")
                        }
                    }
            ) {
                UIAvatar(
                    value = assistant.avatar,
                    name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                    onUpdate = null,
                    modifier = Modifier.size(96.dp)
                )
            }

            Text(
                text = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (assistant.systemPrompt.isNotBlank()) {
                Text(
                    text = assistant.systemPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ════════════════════════════════════════════════════════════════════
        // NAVIGATION CARDS - Dedicated modular sub-pages
        // ═══════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_core_roleplay)) {
            NavigationCard(
                icon = Icons.Rounded.Person,
                title = stringResource(R.string.assistant_detail_profile),
                description = stringResource(R.string.assistant_detail_core_profile_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.PROFILE) }
            )

            NavigationCard(
                icon = Icons.Rounded.Psychology,
                title = stringResource(R.string.assistant_detail_section_personality),
                description = stringResource(R.string.assistant_detail_personality_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.PERSONALITY) }
            )

            NavigationCard(
                icon = Icons.Rounded.DataObject,
                title = stringResource(R.string.assistant_detail_section_scenario),
                description = stringResource(R.string.assistant_detail_scenario_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.SCENARIO) }
            )

            NavigationCard(
                icon = Icons.AutoMirrored.Rounded.Chat,
                title = stringResource(R.string.assistant_detail_section_initial_messages),
                description = stringResource(R.string.assistant_detail_add_intro),
                onClick = { navController.navigate(AssistantDetailRoutes.GREETINGS) }
            )

            NavigationCard(
                icon = Icons.Rounded.DataObject,
                title = stringResource(R.string.assistant_detail_section_lorebooks),
                description = stringResource(R.string.assistant_detail_core_lorebooks_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.LOREBOOKS) }
            )

            NavigationCard(
                icon = Icons.Rounded.Upload,
                title = stringResource(R.string.assistant_page_preset_messages),
                description = stringResource(R.string.assistant_page_preset_messages_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.EXAMPLE_DIALOGUES) }
            )
        }

        SettingsGroup(title = stringResource(R.string.assistant_detail_section_advanced)) {
            NavigationCard(
                icon = Icons.Rounded.Build,
                title = stringResource(R.string.assistant_detail_models),
                description = stringResource(R.string.assistant_detail_advanced_models_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.MODEL) }
            )

            NavigationCard(
                icon = Icons.Rounded.Tune,
                title = "Context & Budget",
                description = stringResource(R.string.assistant_detail_advanced_context_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.CONTEXT_MANAGEMENT) }
            )

            NavigationCard(
                icon = Icons.Rounded.Palette,
                title = stringResource(R.string.assistant_detail_ui_customization),
                description = stringResource(R.string.assistant_detail_advanced_ui_desc),
                onClick = { navController.navigate(AssistantDetailRoutes.UI) }
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Navigation card component with proper styling
 */
@Composable
private fun NavigationCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    SettingGroupItem(
        title = title,
        subtitle = description,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        onClick = onClick
    )
}
