package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.ui.components.ui.DebouncedTextField
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Scenario sub-page - Author's Note only.
 * Dedicated modular sub-page for editing the character's scenario / author's note.
 */
@Composable
fun AssistantScenarioSubPage(
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
        SettingsGroup(title = stringResource(R.string.assistant_detail_section_scenario)) {
            SettingGroupInputItem(
                title = stringResource(R.string.assistant_page_authors_note),
                subtitle = stringResource(R.string.assistant_detail_scenario_desc),
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
                        DebouncedTextField(
                            value = assistant.authorsNote ?: "",
                            onValueChange = { onUpdate(assistant.copy(authorsNote = it)) },
                            stateKey = "${assistant.id}_scenario",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 160.dp),
                            minLines = 6,
                            maxLines = 12,
                            placeholder = stringResource(R.string.assistant_page_scenario_placeholder),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            )
        }
    }
}
