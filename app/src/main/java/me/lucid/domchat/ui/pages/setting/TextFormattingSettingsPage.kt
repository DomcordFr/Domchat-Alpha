package me.lucid.domchat.ui.pages.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.ui.components.nav.OneUITopAppBar
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import org.koin.androidx.compose.koinViewModel

@Composable
fun TextFormattingSettingsPage(vm: me.lucid.domchat.ui.pages.setting.SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val haptics = rememberPremiumHaptics()

    Scaffold(
        topBar = {
            OneUITopAppBar(
                title = "Text Formatting",
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
                navigationIcon = { BackButton() }
            )
        },
        modifier = Modifier.nestedScroll(TopAppBarDefaults.exitUntilCollapsedScrollBehavior().nestedScrollConnection)
    ) { innerPadding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Active Rule Card
            item {
                SettingsGroup(
                    title = "Roleplay Text Styling",
                    subtitle = "Configure formatting rules, font styling, and text decorators",
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                            shape = AppShapes.CardMedium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Active Formatting Rule",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Active rule card with rounded corners matching the card theme.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Preview active typography and glass card layout",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
