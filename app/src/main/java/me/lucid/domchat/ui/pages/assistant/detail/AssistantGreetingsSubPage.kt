package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
                            LiquidGlassTokens.SpecularBorderSubtle
                        ),
                        shape = AppShapes.CardMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for ((index, intro) in intros.withIndex()) {
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
                                        var editorText by remember { mutableStateOf(intro) }
                                        Surface(
                                            color = Color(0xFF141520),
                                            border = androidx.compose.foundation.BorderStroke(
                                                0.5.dp,
                                                LiquidGlassTokens.SpecularBorderSubtle
                                            ),
                                            shape = AppShapes.CardMedium,
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
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
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
                                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
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
    }
}
