package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.pages.setting.components.SettingGroupInputItem
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Example Dialogues & SillyTavern settings sub-page.
 * Dedicated modular sub-page for preset messages / example dialogues.
 */
@Composable
fun AssistantExampleDialoguesSubPage(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsGroup(title = stringResource(R.string.assistant_page_preset_messages)) {
            SettingGroupInputItem(
                title = stringResource(R.string.assistant_page_preset_messages),
                subtitle = stringResource(R.string.assistant_page_preset_messages_desc),
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
                        val presetMessages = assistant.presetMessages
                            .filter { it.role == MessageRole.ASSISTANT }
                            .map { it.toText() }

                        if (presetMessages.isEmpty()) {
                            Text(
                                text = "No example dialogues configured.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                presetMessages.forEachIndexed { index, message ->
                                    Surface(
                                        color = Color(0xFF141520),
                                        border = androidx.compose.foundation.BorderStroke(
                                            0.5.dp,
                                            LiquidGlassTokens.SpecularBorderSubtle
                                        ),
                                        shape = AppShapes.CardSmall,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Example #${index + 1}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = message,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    lineHeight = 20.sp
                                                ),
                                                maxLines = 6,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
