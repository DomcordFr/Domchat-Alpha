package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupInputItem
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Personality sub-page - System Prompt only.
 * Dedicated modular sub-page for editing the character's system prompt / personality definition.
 */
@Composable
fun AssistantPersonalitySubPage(
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
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_personality)) {
            SettingGroupInputItem(
                title = "Character Personality",
                subtitle = "Defines how your character speaks, acts, and responds in roleplay. Written as plain text or Markdown, this is injected into every chat as the character's core definition.",
                content = {
                    Surface(
                        color = Color(0xFF141520),
                        border = androidx.compose.foundation.BorderStroke(
                            0.5.dp,
                            LiquidGlassTokens.SpecularBorderSubtle
                        ),
                        shape = AppShapes.CardMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val personalityValue = androidx.compose.runtime.key(assistant.id) {
                            androidx.compose.foundation.text.input.rememberTextFieldState(
                                initialText = assistant.systemPrompt
                            )
                        }

                        var isFocused by remember { mutableStateOf(false) }

                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                state = personalityValue,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 240.dp)
                                    .onFocusChanged {
                                        isFocused = it.isFocused
                                    },
                                trailingIcon = null,
                                lineLimits = TextFieldLineLimits.MultiLine(
                                    minHeightInLines = 10,
                                    maxHeightInLines = 24
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 20.sp
                                ),
                                shape = AppShapes.InputField,
                                placeholder = {
                                    Text(
                                        text = "Describe your character's personality, speech quirks, motivations, and how they behave in roleplay.",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White.copy(alpha = 0.45f)
                                        )
                                    )
                                },
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
                            if (!isFocused && personalityValue.text.toString() != assistant.systemPrompt) {
                                personalityValue.edit {
                                    replace(0, length, assistant.systemPrompt)
                                }
                            }
                        }

                        androidx.compose.runtime.LaunchedEffect(assistant.id) {
                            androidx.compose.runtime.snapshotFlow { personalityValue.text }
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
    }
}
