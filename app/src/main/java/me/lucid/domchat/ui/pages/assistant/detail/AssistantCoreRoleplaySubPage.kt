package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.input.TextFieldLineLimits
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.debounce
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.domchat.R
import me.lucid.domchat.data.ai.models.ModelCatalogSnapshot
import me.lucid.domchat.data.datastore.Settings
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.model.Tag as DataTag
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.components.ui.TagsInput
import me.lucid.domchat.ui.components.ui.UIAvatar
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.pages.setting.components.SettingGroupInputItem
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

private val GlassCardModifier: Modifier = Modifier
    .fillMaxWidth()
    .background(Color(0xFF141520))
    .border(
        0.5.dp,
        LiquidGlassTokens.SpecularBorderSubtle,
        AppShapes.CardMedium
    )

/**
 * Core Roleplay sub-page split into dedicated separate menus (tabs):
 * Profile, Personality, Scenario, Initial Messages, Example Dialogues.
 *
 * Each tab is a self-contained menu with strict exclusivity — no duplicated
 * items across tabs. All cards use unified black background (0xFF141520)
 * with glassy borders. Lorebooks navigates to a dedicated sub-page.
 */
@Composable
fun AssistantCoreRoleplaySubPage(
    assistant: Assistant,
    tags: List<DataTag>,
    providers: List<me.lucid.ai.provider.ProviderSetting>,
    settings: Settings,
    onUpdate: (Assistant) -> Unit,
    vm: AssistantDetailVM,
    catalogSnapshot: ModelCatalogSnapshot? = null,
    onNavigateToLorebooks: (() -> Unit)? = null,
    onNavigateToSummarizerSettings: (() -> Unit)? = null
) {
    val haptics = rememberPremiumHaptics()

    val tabTitles = listOf(
        stringResource(R.string.assistant_detail_section_profile),
        stringResource(R.string.assistant_detail_section_personality),
        stringResource(R.string.assistant_detail_section_scenario),
        stringResource(R.string.assistant_detail_section_initial_messages),
        stringResource(R.string.assistant_detail_section_example_dialogues)
    )

    var selectedTab by remember { mutableIntStateOf(0) }

    // Profile tab: local edit state for display/chat name
    var localDisplayName by remember(assistant.id) { mutableStateOf(assistant.displayName ?: assistant.name) }
    var localChatName by remember(assistant.id) { mutableStateOf(assistant.chatName ?: assistant.name) }

    // Initial Messages tab: local edit state
    var localIntros by remember(assistant.id) {
        mutableStateOf(
            assistant.presetMessages
                .filter { it.role == MessageRole.ASSISTANT }
                .map { it.toText() }
                .plus(assistant.alternateGreetings)
                .filter { it.isNotBlank() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ═════════════════════════════════════════════════════════════════════════
        // TAB ROW — Dedicated menus with strict exclusivity
        // ═════════════════════════════════════════════════════════════════════════
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(text = title, style = MaterialTheme.typography.labelMedium) },
                    icon = {
                        when (index) {
                            0 -> Icon(imageVector = Icons.Rounded.Person, contentDescription = null)
                            1 -> Icon(imageVector = Icons.Rounded.Psychology, contentDescription = null)
                            2 -> Icon(imageVector = Icons.Rounded.DataObject, contentDescription = null)
                            3 -> Icon(imageVector = Icons.Rounded.Chat, contentDescription = null)
                            else -> Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ═════════════════════════════════════════════════════════════════════════
        // TAB CONTENT — Strict exclusivity per tab
        // ═════════════════════════════════════════════════════════════════════════
        when (selectedTab) {
            0 -> ProfileTab(
                assistant = assistant,
                tags = tags,
                localDisplayName = localDisplayName,
                localChatName = localChatName,
                onUpdateDisplayName = { localDisplayName = it; onUpdate(assistant.copy(displayName = it.takeIf { it.isNotBlank() })) },
                onUpdateChatName = { localChatName = it; onUpdate(assistant.copy(chatName = it.takeIf { it.isNotBlank() })) },
                onUpdate = onUpdate,
                assistantId = assistant.id,
                vm = vm
            )
            1 -> PersonalityTab(
                assistant = assistant,
                onUpdate = onUpdate
            )
            2 -> ScenarioTab(
                assistant = assistant,
                onUpdate = onUpdate
            )
            3 -> InitialMessagesTab(
                assistant = assistant,
                localIntros = localIntros,
                onUpdateIntros = { newIntros ->
                    localIntros = newIntros
                    if (newIntros.isEmpty()) {
                        onUpdate(assistant.copy(presetMessages = emptyList(), alternateGreetings = emptyList()))
                    } else {
                        onUpdate(
                            assistant.copy(
                                presetMessages = listOf(UIMessage(role = MessageRole.ASSISTANT, parts = listOf(UIMessagePart.Text(newIntros.first())))),
                                alternateGreetings = newIntros.drop(1)
                            )
                        )
                    }
                },
                haptics = haptics
            )
            4 -> ExampleDialoguesTab(
                assistant = assistant,
                onUpdate = onUpdate
            )
        }

        // ═════════════════════════════════════════════════════════════════════════
        // LOREBOOKS — Navigation to dedicated sub-page (not a tab, strict exclusivity)
        // ═════════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_lorebooks)) {
            SettingGroupItem(
                title = stringResource(R.string.assistant_page_lorebooks),
                subtitle = stringResource(R.string.assistant_detail_lorebooks_desc),
                trailing = {
                    DomChatGlassPillButton(
                        text = stringResource(R.string.assistant_detail_manage_lorebooks),
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            onNavigateToLorebooks?.invoke()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

/**
 * Profile Tab — Display Name, Chat Name, Avatar, Creator Notes, Tags.
 * Strictly exclusive: only profile-related fields here.
 */
@Composable
private fun ProfileTab(
    assistant: Assistant,
    tags: List<DataTag>,
    localDisplayName: String,
    localChatName: String,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateChatName: (String) -> Unit,
    onUpdate: (Assistant) -> Unit,
    assistantId: kotlin.uuid.Uuid,
    vm: AssistantDetailVM
) {
    val haptics = rememberPremiumHaptics()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Avatar
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UIAvatar(
                value = assistant.avatar,
                name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                onUpdate = { avatar ->
                    haptics.perform(HapticPattern.Pop)
                    onUpdate(assistant.copy(avatar = avatar))
                },
                modifier = Modifier.size(96.dp)
            )
        }

        // Display Name + Chat Name
        SettingGroupInputItem(
            title = stringResource(R.string.assistant_page_name),
            subtitle = stringResource(R.string.assistant_profile_name_desc),
            content = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DebouncedTextField(
                        value = localDisplayName,
                        onValueChange = onUpdateDisplayName,
                        stateKey = "${assistantId}_display",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = stringResource(R.string.assistant_page_name_placeholder)
                    )
                    DebouncedTextField(
                        value = localChatName,
                        onValueChange = onUpdateChatName,
                        stateKey = "${assistantId}_chat",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = stringResource(R.string.assistant_page_chat_name_placeholder)
                    )
                }
            }
        )

        // Creator Notes (black glass card)
        SettingGroupInputItem(
            title = "Creator Notes",
            subtitle = "Creator notes",
            content = {
                Surface(
                    modifier = GlassCardModifier,
                    shape = AppShapes.CardMedium
                ) {
                    DebouncedTextField(
                        value = assistant.creatorNotes ?: "",
                        onValueChange = { onUpdate(assistant.copy(creatorNotes = it)) },
                        stateKey = "${assistantId}_notes",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        minLines = 6,
                        maxLines = 10,
                        placeholder = "Write creator notes...",
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    )
                }
            }
        )

        // Tags
        TagsInput(
            value = assistant.tags,
            tags = tags,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { tagIds, updatedTags ->
                vm.updateTags(tagIds, updatedTags)
            }
        )
    }
}

/**
 * Personality Tab — System Prompt only. Strictly exclusive.
 */
@Composable
private fun PersonalityTab(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    SettingGroupInputItem(
        title = stringResource(R.string.assistant_page_system_prompt),
        subtitle = stringResource(R.string.assistant_detail_personality_desc),
        content = {
            Surface(
                modifier = GlassCardModifier,
                shape = AppShapes.CardMedium
            ) {
                val systemPromptValue = androidx.compose.runtime.key(assistant.id) {
                    androidx.compose.foundation.text.input.rememberTextFieldState(
                        initialText = assistant.systemPrompt
                    )
                }

                var isFocused by remember { mutableStateOf(false) }

                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.OutlinedTextField(
                        state = systemPromptValue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .onFocusChanged {
                                isFocused = it.isFocused
                            },
                        trailingIcon = null,
                        lineLimits = TextFieldLineLimits.MultiLine(
                            minHeightInLines = 8,
                            maxHeightInLines = 16
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        ),
                        shape = AppShapes.InputField,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedBorderColor = Color.White.copy(alpha = 0.30f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White.copy(alpha = 0.9f)
                        ),
                    )
                }

                androidx.compose.runtime.LaunchedEffect(assistant.systemPrompt, isFocused) {
                    if (!isFocused && systemPromptValue.text.toString() != assistant.systemPrompt) {
                        systemPromptValue.edit {
                            replace(0, length, assistant.systemPrompt)
                        }
                    }
                }

                androidx.compose.runtime.LaunchedEffect(assistant.id) {
                    androidx.compose.runtime.snapshotFlow { systemPromptValue.text }
                        .drop(1)
                        .debounce(150L)
                        .collect {
                            if (it.toString() != assistant.systemPrompt) {
                                onUpdate(
                                    assistant.copy(
                                        systemPrompt = it.toString()
                                    )
                                )
                            }
                        }
                }
            }
        }
    )
}

/**
 * Scenario Tab — Author's Note only. Strictly exclusive.
 */
@Composable
private fun ScenarioTab(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    SettingGroupItem(
        title = stringResource(R.string.assistant_page_authors_note),
        subtitle = stringResource(R.string.assistant_detail_scenario_desc),
        trailing = {
            Surface(
                modifier = GlassCardModifier,
                shape = AppShapes.CardMedium
            ) {
                DebouncedTextField(
                    value = assistant.authorsNote ?: "",
                    onValueChange = { onUpdate(assistant.copy(authorsNote = it)) },
                    stateKey = "${assistant.id}_scenario",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    minLines = 6,
                    maxLines = 10,
                    placeholder = stringResource(R.string.assistant_page_scenario_placeholder),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    )
}

/**
 * Initial Messages Tab — Alternate greetings only. Strictly exclusive.
 */
@Composable
private fun InitialMessagesTab(
    assistant: Assistant,
    localIntros: List<String>,
    onUpdateIntros: (List<String>) -> Unit,
    haptics: me.lucid.domchat.ui.hooks.PremiumHaptics
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (localIntros.isEmpty()) {
            DomChatGlassPillButton(
                text = stringResource(R.string.assistant_detail_add_intro),
                onClick = {
                    haptics.perform(HapticPattern.Pop)
                    onUpdateIntros(listOf(""))
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Surface(
                modifier = GlassCardModifier,
                shape = AppShapes.CardMedium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for ((index, intro) in localIntros.withIndex()) {
                        var isEditing by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF141520),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.5.dp,
                                    LiquidGlassTokens.SpecularBorderSubtle
                                ),
                                shape = AppShapes.CardSmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        haptics.perform(HapticPattern.Pop)
                                        isEditing = true
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Intro #${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                haptics.perform(HapticPattern.Tick)
                                                val newList = localIntros.toMutableList()
                                                newList.removeAt(index)
                                                onUpdateIntros(newList)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Delete,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = intro.ifEmpty { "Tap to write intro..." },
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (intro.isEmpty())
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        else
                                            MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            if (isEditing) {
                                var editorText by remember { mutableStateOf(intro) }
                                Surface(
                                    modifier = GlassCardModifier.padding(vertical = 8.dp),
                                    shape = AppShapes.CardMedium
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Edit Intro #${index + 1}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        DebouncedTextField(
                                            value = editorText,
                                            onValueChange = { editorText = it },
                                            stateKey = "${assistant.id}_intro_$index",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 160.dp),
                                            minLines = 8,
                                            maxLines = 12,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 20.sp
                                            )
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            DomChatGlassPillButton(
                                                text = stringResource(R.string.cancel),
                                                onClick = { isEditing = false },
                                                modifier = Modifier.width(80.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            DomChatGlassPillButton(
                                                text = stringResource(R.string.save),
                                                accent = true,
                                                onClick = {
                                                    val newList = localIntros.toMutableList()
                                                    newList[index] = editorText
                                                    onUpdateIntros(newList)
                                                    isEditing = false
                                                },
                                                modifier = Modifier.width(80.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        DomChatGlassPillButton(
                            text = stringResource(R.string.assistant_detail_add_intro),
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                onUpdateIntros(localIntros + "")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Example Dialogues Tab — Dedicated section for example dialogues.
 * Strictly exclusive: no overlap with other tabs.
 */
@Composable
private fun ExampleDialoguesTab(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    SettingGroupInputItem(
        title = stringResource(R.string.assistant_page_example_dialogues),
        subtitle = stringResource(R.string.assistant_detail_example_dialogues_desc),
        content = {
            Surface(
                modifier = GlassCardModifier,
                shape = AppShapes.CardMedium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.assistant_detail_example_dialogues_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    DomChatGlassPillButton(
                        text = stringResource(R.string.assistant_detail_add_example_dialogue),
                        onClick = {
                            // Placeholder — example dialogues editing handled in dedicated flow
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    )
}
