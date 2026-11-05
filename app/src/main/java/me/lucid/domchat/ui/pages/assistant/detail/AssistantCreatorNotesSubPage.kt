package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Creator Notes sub-page - Dedicated page for character creator notes.
 * Supports markdown rendering, inline image insertion, and expandable text area.
 */
@Composable
fun AssistantCreatorNotesSubPage(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    val haptics = rememberPremiumHaptics()
    var showMarkdownPreview by remember { mutableStateOf(false) }
    var creatorNotesEditorText by remember(assistant.creatorNotes) { mutableStateOf(assistant.creatorNotes ?: "") }
    var showImageInsertDialog by remember { mutableStateOf(false) }
    var imageUrlInput by remember { mutableStateOf("") }

    val currentNotes = assistant.creatorNotes ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_creator_notes)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.assistant_detail_creator_notes_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )

                // Formatting chips row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassyFormattingChip(
                        label = "Bold",
                        isSelected = false,
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            val current = creatorNotesEditorText
                            creatorNotesEditorText = "**$current**"
                        }
                    )
                    GlassyFormattingChip(
                        label = "Italic",
                        isSelected = false,
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            val current = creatorNotesEditorText
                            creatorNotesEditorText = "*$current*"
                        }
                    )
                    GlassyFormattingChip(
                        label = "Insert Image",
                        isSelected = false,
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            imageUrlInput = ""
                            showImageInsertDialog = true
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color(0xFF141520),
                    border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DebouncedTextField(
                        value = creatorNotesEditorText,
                        onValueChange = { creatorNotesEditorText = it },
                        stateKey = "${assistant.id}_creator_notes",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp),
                        minLines = 8,
                        maxLines = 20,
                        placeholder = stringResource(R.string.assistant_page_creator_notes_placeholder),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DomChatGlassPillButton(
                        text = stringResource(R.string.cancel),
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            creatorNotesEditorText = assistant.creatorNotes ?: ""
                        },
                        modifier = Modifier.width(80.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DomChatGlassPillButton(
                        text = stringResource(R.string.save),
                        accent = true,
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            onUpdate(assistant.copy(creatorNotes = creatorNotesEditorText))
                        },
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }
    }

    // Image Insert Dialog
    if (showImageInsertDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImageInsertDialog = false },
            containerColor = Color(0xFF000000),
            title = {
                Text(
                    text = "Insert Image",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter image URL (markdown ![Image](url) will be inserted)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    DebouncedTextField(
                        value = imageUrlInput,
                        onValueChange = { imageUrlInput = it },
                        stateKey = "${assistant.id}_image_url",
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = "https://example.com/image.png",
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 20.sp
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (imageUrlInput.isNotBlank()) {
                            val markdown = "\n![Image]($imageUrlInput)\n"
                            onUpdate(
                                assistant.copy(
                                    creatorNotes = creatorNotesEditorText + markdown
                                )
                            )
                            showImageInsertDialog = false
                        }
                    }
                ) {
                    Text(text = "Insert")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImageInsertDialog = false }
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

/**
 * Glassy formatting chip for creator notes formatting options.
 */
@Composable
private fun GlassyFormattingChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF141520),
        border = BorderStroke(
            0.5.dp,
            if (isSelected) Brush.solidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else LiquidGlassTokens.SpecularBorderSubtle
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.9f)
        )
    }
}
