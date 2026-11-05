package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.components.ui.TagsInput
import me.lucid.domchat.ui.components.ui.UIAvatar
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.motion.ExpandableContent
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.pages.setting.components.SettingGroupInputItem
import me.lucid.domchat.data.model.Tag as DataTag

/**
 * Profile sub-page - Display & chat name, avatar, creator notes, tags.
 * Dedicated modular sub-page for character identity and identity-related settings.
 */
@Composable
fun AssistantProfileSubPage(
    assistant: Assistant,
    tags: List<DataTag>,
    onUpdate: (Assistant) -> Unit,
    vm: me.lucid.domchat.ui.pages.assistant.detail.AssistantDetailVM
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ═════════════════════════════════════════════════════════════════════
        // AVATAR SECTION (prominent, centered)
        // ═════════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF000000))
                    .border(
                        BorderStroke(0.5.dp, me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
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

            Text(
                text = stringResource(R.string.assistant_profile_tap_change_avatar),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // IDENTITY GROUP (display name, chat name)
        // ═════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_profile_identity)) {
            SettingGroupInputItem(
                title = stringResource(R.string.assistant_page_name),
                subtitle = stringResource(R.string.assistant_profile_name_desc),
                content = {
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
                            placeholder = stringResource(R.string.assistant_page_name_placeholder)
                        )
                        DebouncedTextField(
                            value = assistant.chatName ?: assistant.name,
                            onValueChange = { onUpdate(assistant.copy(chatName = it.takeIf { it.isNotBlank() })) },
                            stateKey = "${assistant.id}_chat",
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = stringResource(R.string.assistant_page_chat_name_placeholder)
                        )
                    }
                }
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // CREATOR NOTES (black glass card)
        // ════════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_creator_notes)) {
            Column(modifier = Modifier.fillMaxWidth()) {
                var notesExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.assistant_detail_section_creator_notes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    IconButton(
                        onClick = { notesExpanded = !notesExpanded },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (notesExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                ExpandableContent(visible = notesExpanded) {
                    Surface(
                        color = Color(0xFF141520),
                        border = BorderStroke(
                            0.5.dp,
                            me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                        ),
                        shape = me.lucid.domchat.ui.theme.AppShapes.CardMedium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            MarkdownBlock(
                                content = assistant.creatorNotes ?: "",
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            DebouncedTextField(
                                value = assistant.creatorNotes ?: "",
                                onValueChange = { onUpdate(assistant.copy(creatorNotes = it)) },
                                stateKey = "${assistant.id}_notes",
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
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // TAGS
        // ═══════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_page_tags)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF000000),
                border = BorderStroke(
                    0.5.dp,
                    me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.assistant_page_tags),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.assistant_profile_tags_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TagsInput(
                        value = assistant.tags,
                        tags = tags,
                        onValueChange = { tagIds, updatedTags ->
                            vm.updateTags(tagIds, updatedTags)
                        },
                    )
                }
            }
        }
    }
}
