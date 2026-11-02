package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.input.TextFieldLineLimits
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
import me.lucid.domchat.ui.components.ui.FormItem
import me.lucid.domchat.ui.components.ui.HapticSwitch
import me.lucid.domchat.ui.components.ui.TagsInput
import me.lucid.domchat.ui.components.ui.UIAvatar
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Unified Core Roleplay sub-page combining Profile (Display Name, Chat Name, Avatar, Tags),
 * Creator Notes & Tags (black background, glassy borders), Personality (System Prompt - spacious
 * multiline), Scenario (Author's Note - spacious multiline), Initial Messages (alternate greetings),
 * and Lorebooks into a single cohesive view.
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

    var isPersonaExpanded by remember { mutableStateOf(true) }
    var isNotesExpanded by remember { mutableStateOf(true) }
    var isPersonalityExpanded by remember { mutableStateOf(true) }
    var isScenarioExpanded by remember { mutableStateOf(true) }
    var isIntrosExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ═══════════════════════════════════════════════════════════════════════════
        // SECTION: PERSONA (Display Name, Chat Name, Avatar, Tags)
        // ═══════════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_persona)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                UIAvatar(
                    value = assistant.avatar,
                    name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                    onUpdate = { avatar ->
                        onUpdate(assistant.copy(avatar = avatar))
                    },
                    modifier = Modifier.size(96.dp)
                )
            }

            SettingGroupItem(
                title = stringResource(R.string.assistant_page_name),
                subtitle = stringResource(R.string.assistant_profile_name_desc),
                trailing = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DebouncedTextField(
                            value = assistant.displayName ?: assistant.name,
                            onValueChange = { onUpdate(assistant.copy(displayName = it.takeIf { it.isNotBlank() })) },
                            stateKey = "${assistant.id}_display",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.assistant_page_name_placeholder)) }
                        )
                        DebouncedTextField(
                            value = assistant.chatName ?: assistant.name,
                            onValueChange = { onUpdate(assistant.copy(chatName = it.takeIf { it.isNotBlank() })) },
                            stateKey = "${assistant.id}_chat",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.assistant_page_chat_name_placeholder)) }
                        )
                    }
                }
            )

            TagsInput(
                value = assistant.tags,
                tags = tags,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { tagIds, updatedTags ->
                    vm.updateTags(tagIds, updatedTags)
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // SECTION: CREATOR NOTES & TAGS (black background, glassy borders)
        // ═══════════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_creator_notes)) {
            SettingGroupItem(
                title = stringResource(R.string.assistant_page_creator_notes),
                subtitle = stringResource(R.string.assistant_detail_creator_notes_desc),
                trailing = {
                    Surface(
                        color = Color(0xFF141520),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                        ),
                        shape = me.lucid.domchat.ui.theme.AppShapes.CardMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DebouncedTextField(
                            value = assistant.creatorNotes,
                            onValueChange = { onUpdate(assistant.copy(creatorNotes = it)) },
                            stateKey = "${assistant.id}_notes",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            minLines = 6,
                            maxLines = 10,
                            placeholder = { Text(stringResource(R.string.assistant_page_creator_notes_placeholder)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // SECTION: PERSONALITY (System Prompt - spacious multiline)
        // ═══════════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_personality)) {
            SettingGroupItem(
                title = stringResource(R.string.assistant_page_system_prompt),
                subtitle = stringResource(R.string.assistant_detail_personality_desc),
                trailing = {
                    Surface(
                        color = Color(0xFF141520),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                        ),
                        shape = me.lucid.domchat.ui.theme.AppShapes.CardMedium,
                        modifier = Modifier.fillMaxWidth()
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

        // ═══════════════════════════════════════════════════════════════════════════
        // SECTION: SCENARIO (Author's Note - spacious multiline)
        // ═══════════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_scenario)) {
            SettingGroupItem(
                title = stringResource(R.string.assistant_page_authors_note),
                subtitle = stringResource(R.string.assistant_detail_scenario_desc),
                trailing = {
                    Surface(
                        color = Color(0xFF141520),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                        ),
                        shape = me.lucid.domchat.ui.theme.AppShapes.CardMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DebouncedTextField(
                            value = assistant.authorsNote,
                            onValueChange = { onUpdate(assistant.copy(authorsNote = it)) },
                            stateKey = "${assistant.id}_scenario",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            minLines = 6,
                            maxLines = 10,
                            placeholder = { Text(stringResource(R.string.assistant_page_scenario_placeholder)) },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // SECTION: INITIAL MESSAGES (alternate greetings)
        // ═══════════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_initial_messages)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val intros = remember(assistant.presetMessages, assistant.alternateGreetings) {
                    val list = mutableListOf<String>()
                    assistant.presetMessages.filter { it.role == MessageRole.ASSISTANT }.forEach { list.add(it.toText()) }
                    list.addAll(assistant.alternateGreetings)
                    list
                }

                val updateIntros = { newIntros: List<String> ->
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
                }

                if (intros.isEmpty()) {
                    DomChatGlassPillButton(
                        text = stringResource(R.string.assistant_detail_add_intro),
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            updateIntros(listOf(""))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Surface(
                        color = Color(0xFF141520),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                        ),
                        shape = AppShapes.CardMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            intros.forEachIndexed { index, intro ->
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
                                            me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
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
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                IconButton(
                                                    onClick = {
                                                        haptics.perform(HapticPattern.Tick)
                                                        val newList = intros.toMutableList()
                                                        if (index in newList.indices) {
                                                            newList.removeAt(index)
                                                            updateIntros(newList)
                                                        }
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
                                        // Inline full-screen editor for the intro
                                        var editorText by remember { mutableStateOf(intro) }
                                        Surface(
                                            color = Color(0xFF141520),
                                            border = androidx.compose.foundation.BorderStroke(
                                                0.5.dp,
                                                me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                                            ),
                                            shape = me.lucid.domchat.ui.theme.AppShapes.CardMedium,
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Edit Intro #${index + 1}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color.White
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
                                                    ),
                                                    shape = AppShapes.InputField,
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
                                                            val newList = intros.toMutableList()
                                                            newList[index] = editorText
                                                            updateIntros(newList)
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
                                        updateIntros(intros + "")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════════════
        // SECTION: LOREBOOKS
        // ═══════════════════════════════════════════════════════════════════════════
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
