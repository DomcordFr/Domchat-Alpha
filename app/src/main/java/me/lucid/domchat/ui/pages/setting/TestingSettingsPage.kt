package me.lucid.domchat.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

import me.lucid.domchat.data.model.Lorebook
import me.lucid.domchat.data.datastore.SettingsStore
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.ui.components.nav.OneUITopAppBar
import me.lucid.domchat.ui.components.ui.HapticSwitch
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

/**
 * Temporary testing page for QA / development.
 * Provides functional toggles to inject mock lorebooks and mock character cards
 * into live settings state so screens can be exercised without manual data entry.
 */
@Composable
fun TestingSettingsPage(vm: me.lucid.domchat.ui.pages.setting.SettingVM = koinViewModel()) {
    val settingsStore: SettingsStore = koinInject()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val haptics = rememberPremiumHaptics()
    val coroutineScope = rememberCoroutineScope()

    var mockLorebooks by remember { mutableStateOf(false) }
    var mockCards by remember { mutableStateOf(false) }

    val existingLorebookCount = settings.lorebooks.size
    val existingAssistantCount = settings.assistants.size

    fun injectMockLorebooks() {
        val mock = listOf(
            Lorebook(
                name = "Mock: Vampire Lore",
                description = "A sample lorebook for testing injection and retrieval.",
                entries = emptyList()
            ),
            Lorebook(
                name = "Mock: Cybernetic Implants",
                description = "Test lorebook entries with keyword activation.",
                entries = emptyList()
            )
        )
        coroutineScope.launch {
            settingsStore.update {
                it.copy(lorebooks = it.lorebooks + mock)
            }
        }
    }

    fun injectMockCharacterCards() {
        val mock = listOf(
            me.lucid.domchat.data.model.Assistant(
                name = "Mock: Tester Alpha",
                creator = "QA",
                creatorNotes = "This is a mock character card injected for testing."
            ),
            me.lucid.domchat.data.model.Assistant(
                name = "Mock: Tester Beta",
                creator = "QA",
                creatorNotes = "Second mock assistant for hub and chat flows."
            )
        )
        coroutineScope.launch {
            settingsStore.update {
                it.copy(assistants = it.assistants + mock)
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF000000),
        topBar = {
            OneUITopAppBar(
                title = "Testing",
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
                navigationIcon = { BackButton() }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                color = Color(0xFF141520),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Testing & Mock Data",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Inject mock data into live settings for QA flows. Changes persist to datastore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Inject Mock Lorebooks",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "($existingLorebookCount existing)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        HapticSwitch(
                            checked = mockLorebooks,
                            onCheckedChange = { checked ->
                                haptics.perform(HapticPattern.Pop)
                                mockLorebooks = checked
                                if (checked) injectMockLorebooks()
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Inject Mock Character Cards",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "($existingAssistantCount existing)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        HapticSwitch(
                            checked = mockCards,
                            onCheckedChange = { checked ->
                                haptics.perform(HapticPattern.Pop)
                                mockCards = checked
                                if (checked) injectMockCharacterCards()
                            }
                        )
                    }
                }
            }
        }
    }
}
