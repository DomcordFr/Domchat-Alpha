package me.lucid.domchat.ui.components.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.ui.theme.appFieldColors
import me.lucid.domchat.ui.theme.appFilterChipBorder
import me.lucid.domchat.ui.theme.appFilterChipColors
import me.lucid.domchat.ui.theme.liquidGlassSurface

/**
 * Standard Markdown Text Editor for Character Creation & Editing.
 * Provides:
 * - Write / Preview tabs with smooth crossfade
 * - Markdown formatting toolbar (Bold, Italic, Code, Quote, Image embedding)
 * - {{char}} and {{user}} macro pills
 * - Word and character count indicator
 * - AMOLED liquid glass aesthetics
 */
@Composable
fun DomChatMarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Write markdown here...",
    minLines: Int = 10,
    showMacros: Boolean = true,
    allowImageEmbed: Boolean = true,
) {
    val haptics = rememberPremiumHaptics()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Write, 1: Preview

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            haptics.perform(HapticPattern.Pop)
            val imageMarkdown = "\n![Image]($uri)\n"
            onValueChange(value + imageMarkdown)
        }
    }

    val wordCount = remember(value) {
        if (value.isBlank()) 0 else value.trim().split("\\s+".toRegex()).size
    }
    val charCount = value.length

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tab Row & Counters
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

            // Word / Char count pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF000000).copy(alpha = 0.55f),
                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
            ) {
                Text(
                    text = "$wordCount words • $charCount chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Markdown Toolbar (visible in Write mode)
        if (selectedTab == 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlassSurface(
                        shape = RoundedCornerShape(12.dp),
                        borderWidth = 0.5.dp,
                        fillColor = Color(0xFF000000).copy(alpha = 0.40f)
                    ),
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    EditorToolbarButton(
                        icon = Icons.Rounded.FormatBold,
                        tooltip = "Bold",
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onValueChange("$value**text**")
                        }
                    )
                    EditorToolbarButton(
                        icon = Icons.Rounded.FormatItalic,
                        tooltip = "Italic",
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onValueChange("$value*text*")
                        }
                    )
                    EditorToolbarButton(
                        icon = Icons.Rounded.Code,
                        tooltip = "Code",
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onValueChange("$value`code`")
                        }
                    )
                    EditorToolbarButton(
                        icon = Icons.Rounded.FormatQuote,
                        tooltip = "Quote",
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onValueChange("$value\n> ")
                        }
                    )

                    if (allowImageEmbed) {
                        EditorToolbarButton(
                            icon = Icons.Rounded.Image,
                            tooltip = "Embed Image",
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }

                    if (showMacros) {
                        Spacer(Modifier.width(4.dp))
                        MacroPill(
                            label = "{{char}}",
                            onClick = {
                                haptics.perform(HapticPattern.Tick)
                                onValueChange("$value{{char}}")
                            }
                        )
                        MacroPill(
                            label = "{{user}}",
                            onClick = {
                                haptics.perform(HapticPattern.Tick)
                                onValueChange("$value{{user}}")
                            }
                        )
                    }
                }
            }
        }

        // Editor Body
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "markdown_editor_content"
        ) { tab ->
            if (tab == 0) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (minLines * 24).dp)
                        .liquidGlassSurface(
                            shape = AppShapes.CardLarge,
                            borderWidth = 0.5.dp,
                            fillColor = Color(0xFF000000).copy(alpha = 0.45f)
                        ),
                    colors = appFieldColors(),
                    placeholder = {
                        Text(
                            text = placeholder,
                            color = Color.White.copy(alpha = 0.45f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    shape = AppShapes.CardLarge,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        lineHeight = 22.sp
                    )
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = (minLines * 24).dp)
                        .liquidGlassSurface(
                            shape = AppShapes.CardLarge,
                            borderWidth = 0.5.dp,
                            fillColor = Color(0xFF000000).copy(alpha = 0.45f)
                        ),
                    shape = AppShapes.CardLarge,
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        if (value.isNotBlank()) {
                            MarkdownBlock(
                                content = value,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = "Nothing to preview yet. Switch to 'Write' to compose.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorToolbarButton(
    icon: ImageVector,
    tooltip: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(34.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = tooltip,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun MacroPill(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF000000).copy(alpha = 0.55f),
        border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
