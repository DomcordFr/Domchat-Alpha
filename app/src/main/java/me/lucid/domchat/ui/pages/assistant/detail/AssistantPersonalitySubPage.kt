package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.ui.theme.appFilterChipBorder
import me.lucid.domchat.ui.theme.appFilterChipColors

/**
 * Personality sub-page - Dedicated modular workspace for character personality & system prompt.
 * Supports Markdown text authoring with live preview, word counts, and AMOLED black glassy aesthetics.
 */
@Composable
fun AssistantPersonalitySubPage(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    val haptics = rememberPremiumHaptics()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Write, 1: Preview
    var personalityText by remember(assistant.id) { mutableStateOf(assistant.systemPrompt) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // HEADER / MODE SEGMENT (Write vs Preview)
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        selectedTab = 0
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Write") },
                    shape = AppShapes.Chip,
                    colors = appFilterChipColors(selectedTab == 0),
                    border = appFilterChipBorder(selectedTab == 0)
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = {
                        haptics.perform(HapticPattern.Tick)
                        selectedTab = 1
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Preview") },
                    shape = AppShapes.Chip,
                    colors = appFilterChipColors(selectedTab == 1),
                    border = appFilterChipBorder(selectedTab == 1)
                )
            }

            val wordCount = remember(personalityText) {
                if (personalityText.isBlank()) 0 else personalityText.trim().split(Regex("\\s+")).size
            }
            Text(
                text = "${personalityText.length} chars • $wordCount words",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // WORKSPACE CONTAINER (Black glass with specular border)
        // ═══════════════════════════════════════════════════════════════════
        Surface(
            color = Color(0xFF000000),
            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
            shape = AppShapes.CardLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "personalityTabs"
            ) { tab ->
                if (tab == 0) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = personalityText,
                            onValueChange = {
                                personalityText = it
                                onUpdate(assistant.copy(systemPrompt = it))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 320.dp),
                            minLines = 14,
                            placeholder = {
                                Text(
                                    text = "Describe your character's personality, speaking style, behavioral guidelines, thoughts, quirks, and lore in Markdown or plain text...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White.copy(alpha = 0.45f)
                                    )
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 22.sp,
                                color = Color.White
                            ),
                            shape = AppShapes.InputField,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.White.copy(alpha = 0.30f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White.copy(alpha = 0.9f)
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        if (personalityText.isNotBlank()) {
                            MarkdownBlock(
                                content = personalityText,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "No personality prompt defined yet. Switch to 'Write' to create your character's persona.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
