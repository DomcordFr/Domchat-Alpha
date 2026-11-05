package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Greetings sub-page - Initial message + alternate greetings list.
 * Dedicated modular sub-page for editing the character's intro messages.
 */
@Composable
fun AssistantGreetingsSubPage(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    val haptics = rememberPremiumHaptics()

    // Build greetings list from presetMessages (assistant role) + alternateGreetings
    val allGreetings = remember(assistant.presetMessages, assistant.alternateGreetings) {
        val list = mutableListOf<String>()
        assistant.presetMessages.filter { it.role == MessageRole.ASSISTANT }.forEach { list.add(it.toText()) }
        list.addAll(assistant.alternateGreetings)
        list
    }

    var selectedGreetingIndex by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newGreetingInput by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf(-1) }
    var editText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tab row section
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_initial_messages)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Greeting tabs row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allGreetings.forEachIndexed { index, greeting ->
                        val isSelected = index == selectedGreetingIndex
                        val tabLabel = "Initial #${index + 1}"

                        GlassGreetingTab(
                            label = tabLabel,
                            isSelected = isSelected,
                            preview = greeting.ifEmpty { "(empty)" },
                            onClick = {
                                haptics.perform(HapticPattern.Tick)
                                selectedGreetingIndex = index
                            },
                            onLongPress = {
                                haptics.perform(HapticPattern.Pop)
                                editIndex = index
                                editText = greeting
                                showEditDialog = true
                            }
                        )
                    }

                    // Add new greeting button
                    DomChatGlassPillButton(
                        text = "+ Add",
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            showAddDialog = true
                        },
                        modifier = Modifier.size(60.dp, 40.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable input with markdown preview toggle for selected greeting
            if (allGreetings.isNotEmpty() && selectedGreetingIndex in allGreetings.indices) {
                val currentGreeting = allGreetings[selectedGreetingIndex]
                var editorText by remember { mutableStateOf(currentGreeting) }
                var showPreview by remember { mutableStateOf(false) }

                Surface(
                    color = Color(0xFF141520),
                    border = BorderStroke(
                        0.5.dp,
                        LiquidGlassTokens.SpecularBorderSubtle
                    ),
                    shape = AppShapes.CardMedium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Initial #${selectedGreetingIndex + 1}",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "${currentGreeting.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Markdown preview toggle and editor
                        if (showPreview) {
                            MarkdownBlock(
                                content = editorText,
                                modifier = Modifier.fillMaxWidth().padding(12.dp)
                            )
                        } else {
                            DebouncedTextField(
                                value = editorText,
                                onValueChange = { editorText = it },
                                stateKey = "${assistant.id}_greeting_editor_$selectedGreetingIndex",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp),
                                minLines = 6,
                                maxLines = 20,
                                placeholder = "Write greeting in markdown...",
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 20.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview toggle + save row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showPreview = !showPreview }
                            ) {
                                Text(
                                    text = if (showPreview) "Hide Markdown" else "Show Markdown"
                                )
                            }
                            DomChatGlassPillButton(
                                text = stringResource(R.string.cancel),
                                onClick = {
                                    editorText = currentGreeting
                                },
                                modifier = Modifier.width(80.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            DomChatGlassPillButton(
                                text = stringResource(R.string.save),
                                accent = true,
                                onClick = {
                                    val newGreetings = allGreetings.toMutableList()
                                    newGreetings[selectedGreetingIndex] = editorText
                                    onUpdate(
                                        assistant.copy(
                                            presetMessages = listOf(
                                                UIMessage(
                                                    role = MessageRole.ASSISTANT,
                                                    parts = listOf(UIMessagePart.Text(newGreetings.first()))
                                                )
                                            ) + newGreetings.drop(1).mapIndexed { index, text ->
                                                UIMessage(
                                                    role = MessageRole.ASSISTANT,
                                                    parts = listOf(UIMessagePart.Text(text))
                                                )
                                            }
                                        )
                                    )
                                    showEditDialog = false
                                },
                                modifier = Modifier.width(80.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            modifier = Modifier.widthIn(max = 420.dp),
            title = { Text("Manage Greeting") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedGreetingIndex > 0) {
                        DomChatGlassPillButton(text = "Move Forward", onClick = {
                            val list = allGreetings.toMutableList()
                            val item = list.removeAt(selectedGreetingIndex)
                            list.add(selectedGreetingIndex - 1, item)
                            onUpdate(
                                assistant.copy(
                                    presetMessages = list.map {
                                        UIMessage(
                                            role = MessageRole.ASSISTANT,
                                            parts = listOf(UIMessagePart.Text(it))
                                        )
                                    }
                                )
                            )
                            selectedGreetingIndex -= 1
                            showEditDialog = false
                        })
                    }
                    if (selectedGreetingIndex < allGreetings.size - 1) {
                        DomChatGlassPillButton(text = "Move Backward", onClick = {
                            val list = allGreetings.toMutableList()
                            val item = list.removeAt(selectedGreetingIndex)
                            list.add(selectedGreetingIndex + 1, item)
                            onUpdate(
                                assistant.copy(
                                    presetMessages = list.map {
                                        UIMessage(
                                            role = MessageRole.ASSISTANT,
                                            parts = listOf(UIMessagePart.Text(it))
                                        )
                                    }
                                )
                            )
                            selectedGreetingIndex += 1
                            showEditDialog = false
                        })
                    }
                    if (allGreetings.size > 1) {
                        DomChatGlassPillButton(text = "Delete Greeting", onClick = {
                            val list = allGreetings.toMutableList()
                            list.removeAt(selectedGreetingIndex)
                            onUpdate(
                                assistant.copy(
                                    presetMessages = list.map {
                                        UIMessage(
                                            role = MessageRole.ASSISTANT,
                                            parts = listOf(UIMessagePart.Text(it))
                                        )
                                    }
                                )
                            )
                            selectedGreetingIndex = selectedGreetingIndex.coerceAtMost(list.size - 1)
                            showEditDialog = false
                        })
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Glassy greeting tab for the horizontal tab row.
 */
@Composable
private fun GlassyGreetingTab(
    label: String,
    isSelected: Boolean,
    preview: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val haptics = rememberPremiumHaptics()

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF141520),
        border = BorderStroke(
            0.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else LiquidGlassTokens.SpecularBorderSubtle
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .size(70.dp, 64.dp)
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectLongPressGesture(onLongPress = { onLongPress() })
            }
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preview.take(28),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}