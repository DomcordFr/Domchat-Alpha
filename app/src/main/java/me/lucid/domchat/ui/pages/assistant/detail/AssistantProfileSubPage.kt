package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.components.ui.TagsInput
import me.lucid.domchat.ui.components.ui.UIAvatar
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
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
            UIAvatar(
                value = assistant.avatar,
                name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                onUpdate = { avatar ->
                    onUpdate(assistant.copy(avatar = avatar))
                },
                modifier = Modifier.size(96.dp)
            )

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
        // TAGS
        // ═══════════════════════════════════════════════════════════════════
        SettingsGroup(title = stringResource(R.string.assistant_page_tags)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
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
