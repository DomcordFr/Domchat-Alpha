package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.ExperimentalFoundationApi
import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.motion.ExpandableContent
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Greetings sub-page - Initial messages.
 * Dedicated modular sub-page for editing the character's intro messages.
 *
 * Design:
 * - Horizontal tab row above a single expandable Markdown editor ("Initial #1", "+ Add" ...).
 * - Long-press a tab to open a glassy menu (Rename, Delete, Move Forward, Move Backward).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AssistantGreetingsSubPage(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    val haptics = rememberPremiumHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

                var selectedIndex by remember(intros.size) { mutableStateOf(if (intros.isEmpty()) -1 else 0) }
                var menuIndex by remember { mutableStateOf<Int?>(null) }

                // ═══════════════════════════════════════════════════════════
                // TAB ROW — Initial #1, Initial #2, ..., + Add
                // ═══════════════════════════════════════════════════════════
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    intros.forEachIndexed { index, _ ->
                        val isSelected = selectedIndex == index
                        val tabLabel = "Initial #${index + 1}"
                        Box(
                            modifier = Modifier
                                .widthIn(min = 72.dp)
                                .height(40.dp)
                                .combinedClickable(
                                    onClick = {
                                        haptics.perform(HapticPattern.Tick)
                                        selectedIndex = index
                                        menuIndex = null
                                    },
                                    onLongClick = {
                                        haptics.perform(HapticPattern.Pop)
                                        menuIndex = index
                                        renameDraft = tabLabel
                                    },
                                    role = androidx.compose.foundation.selection.SelectableRole
                                )
                                .clip(AppShapes.ButtonPill)
                                .background(
                                    if (isSelected) Color(0xFF000000) else Color.Transparent,
                                    shape = AppShapes.ButtonPill
                                )
                                .border(
                                    if (isSelected) BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
                                    else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f)),
                                    shape = AppShapes.ButtonPill
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                    // "+ Add" tab — creates "Initial #2"
                    Box(
                        modifier = Modifier
                            .widthIn(min = 72.dp)
                            .height(40.dp)
                            .clickable {
                                haptics.perform(HapticPattern.Pop)
                                selectedIndex = intros.size
                                updateIntros(intros + "")
                                menuIndex = null
                            }
                            .clip(AppShapes.ButtonPill)
                            .background(Color.Transparent, shape = AppShapes.ButtonPill)
                            .border(
                                BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                shape = AppShapes.ButtonPill
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                if (intros.isEmpty()) {
                    DomChatGlassPillButton(
                        text = stringResource(R.string.assistant_detail_add_intro),
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            selectedIndex = 0
                            updateIntros(listOf(""))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ════════════════════════════════════════════════════════════
                // SINGLE EXPANDABLE MARKDOWN EDITOR CARD
                // ════════════════════════════════════════════════════════════
                if (selectedIndex in intros.indices) {
                    val currentIntro = intros[selectedIndex]
                    var editorText by remember(selectedIndex) { mutableStateOf(currentIntro) }
                    var editorExpanded by remember { mutableStateOf(true) }

                    // Re-sync when switching tabs
                    if (editorText != currentIntro) {
                        editorText = currentIntro
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedIndex == 0) "Initial #1" else "Initial #${selectedIndex + 1}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                        IconButton(
                            onClick = { editorExpanded = !editorExpanded },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (editorExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    ExpandableContent(visible = editorExpanded) {
                        Surface(
                            color = Color(0xFF000000),
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                            shape = AppShapes.CardMedium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MarkdownBlock(
                                    content = editorText,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                DebouncedTextField(
                                    value = editorText,
                                    onValueChange = { editorText = it },
                                    stateKey = "${assistant.id}_greeting_$selectedIndex",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 140.dp),
                                    minLines = 8,
                                    maxLines = 16,
                                    placeholder = "Write the initial message...",
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        lineHeight = 20.sp
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DomChatGlassPillButton(
                                        text = stringResource(R.string.cancel),
                                        onClick = {
                                            editorText = currentIntro
                                        },
                                        modifier = Modifier.width(80.dp)
                                    )
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                                    DomChatGlassPillButton(
                                        text = stringResource(R.string.save),
                                        accent = true,
                                        onClick = {
                                            val newList = intros.toMutableList()
                                            newList[selectedIndex] = editorText
                                            updateIntros(newList)
                                            selectedIndex = selectedIndex.coerceAtLeast(0).coerceAtMost(newList.lastIndex)
                                        },
                                        modifier = Modifier.width(80.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // ════════════════════════════════════════════════════════════
                // LONG-PRESS TAB MENU — Rename / Delete / Move Forward / Move Backward
                // ═════════════════════════════════════════════════════════════
                menuIndex?.let { index ->
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { menuIndex = null },
                        shape = AppShapes.Dialog,
                        containerColor = Color(0xFF000000),
                        border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = Color.White.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(18.dp)) },
                            onClick = { menuIndex = null },
                            enabled = false
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.White.copy(alpha = 0.9f)) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                haptics.perform(HapticPattern.Error)
                                val newList = intros.toMutableList()
                                if (index in newList.indices) {
                                    newList.removeAt(index)
                                    updateIntros(newList)
                                    selectedIndex = if (newList.isEmpty()) -1 else (index.coerceAtMost(newList.lastIndex))
                                }
                                menuIndex = null
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Move Forward", color = Color.White) },
                            leadingIcon = { Icon(Icons.Rounded.ArrowForward, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                if (index < intros.lastIndex) {
                                    val newList = intros.toMutableList()
                                    val temp = newList.removeAt(index)
                                    newList.add(index + 1, temp)
                                    updateIntros(newList)
                                    selectedIndex = index + 1
                                }
                                menuIndex = null
                            },
                            enabled = index < intros.lastIndex
                        )
                        DropdownMenuItem(
                            text = { Text("Move Backward", color = Color.White) },
                            leadingIcon = { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp)) },
                            onClick = {
                                if (index > 0) {
                                    val newList = intros.toMutableList()
                                    val temp = newList.removeAt(index)
                                    newList.add(index - 1, temp)
                                    updateIntros(newList)
                                    selectedIndex = index - 1
                                }
                                menuIndex = null
                            },
                            enabled = index > 0
                        )
                    }
                }
            }
        }
    }
}
