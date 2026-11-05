package me.lucid.domchat.ui.pages.setting

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.Screen
import me.lucid.domchat.data.datastore.Settings
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.model.Lorebook
import me.lucid.domchat.data.model.LorebookEntry
import me.lucid.domchat.data.model.Avatar
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.ui.components.nav.OneUITopAppBar
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.components.ui.HapticSwitch
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random

@Composable
fun TestingSettingsPage(vm: me.lucid.domchat.ui.pages.setting.SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val haptics = rememberPremiumHaptics()

    var mockLorebooks by remember(settings.displaySetting) { mutableStateOf(settings.displaySetting.mockLorebooks) }
    var mockCards by remember(settings.displaySetting) { mutableStateOf(settings.displaySetting.mockCards) }

    Scaffold(
        topBar = {
            OneUITopAppBar(
                title = "Testing & Mock Data",
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
            // Mock Data Section
            item {
                SettingsGroup(
                    title = "Enable Mock Data",
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Inject synthetic lorebooks and character cards into settings store",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Mock Lorebooks toggle
                        SettingGroupItem(
                            title = "Mock Lorebooks",
                            subtitle = "Inject 5 global lorebooks with 25 keyword-activated entries",
                            trailing = {
                                HapticSwitch(
                                    checked = mockLorebooks,
                                    onCheckedChange = { checked ->
                                        mockLorebooks = checked
                                        if (checked) {
                                            injectMockLorebooks(vm)
                                        } else {
                                            removeMockLorebooks(vm)
                                        }
                                    }
                                )
                            }
                        )

                        // Mock Cards toggle
                        SettingGroupItem(
                            title = "Mock Character Cards",
                            subtitle = "Inject 5 rich character cards with diverse system prompts and tags",
                            trailing = {
                                HapticSwitch(
                                    checked = mockCards,
                                    onCheckedChange = { checked ->
                                        mockCards = checked
                                        if (checked) {
                                            injectMockCards(vm)
                                        } else {
                                            removeMockCards(vm)
                                        }
                                    }
                                )
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Status info
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                            shape = AppShapes.CardMedium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Development Only",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${settings.lorebooks.size} lorebooks • ${settings.assistants.size} character cards",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun injectMockLorebooks(vm: me.lucid.domchat.ui.pages.setting.SettingVM) {
    val mockLorebooks = listOf(
        Lorebook(
            name = "Cyberpunk 2077 Lore",
            description = "Cyberpunk setting lore including gangs, corps, and slang",
            entries = listOf(
                LorebookEntry(
                    name = "Arasaka Corporation",
                    prompt = "Megacorporation that dominates Night City. Known for cyberware and security.",
                    keywords = listOf("arasaka", "koroshi", "security"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Night City",
                    prompt = "Free City of Night City - a dystopian metropolis in California.",
                    keywords = listOf("night city", "nightcity"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Johnny Silverhand",
                    prompt = "Rockerboy and revolutionary. Former lead singer of Samurai.",
                    keywords = listOf("johnny", "silverhand", "samurai"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Braindance",
                    prompt = "Recording technology that allows experiencing another person's memories and sensations.",
                    keywords = listOf("braindance", "bd"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Cyberpsychosis",
                    prompt = "Mental illness caused by excessive cyberware implantation.",
                    keywords = listOf("cyberpsychosis", "cyberpsycho"),
                    activationType = LorebookActivationType.KEYWORDS
                )
            )
        ),
        Lorebook(
            name = "Cyberpunk Edgerunners",
            description = "Characters and lore from the Cyberpunk: Edgerunners anime",
            entries = listOf(
                LorebookEntry(
                    name = "David Martinez",
                    prompt = "Edgerunner from Night City. Uses a cyberdeck and fights corp corruption.",
                    keywords = listOf("david", "martinez", "edgerunner"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Lucy",
                    prompt = "Netrunner and edgerunner. Skilled in hacking and cyberware.",
                    keywords = listOf("lucy", "netrunner"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Rebecca",
                    prompt = "Edgerunner with a love for heavy weapons and action.",
                    keywords = listOf("rebecca"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Maine",
                    prompt = "Edgerunner crew leader. Heavy cyberware user.",
                    keywords = listOf("maine"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Falco",
                    prompt = "Edgerunner weapons expert.",
                    keywords = listOf("falco"),
                    activationType = LorebookActivationType.KEYWORDS
                )
            )
        ),
        Lorebook(
            name = "Cyberpunk Slang",
            description = "Common slang terms used in Night City",
            entries = listOf(
                LorebookEntry(
                    name = "Choomba",
                    prompt = "Friend or companion. Slang term used in Night City.",
                    keywords = listOf("choomba", "choom"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Choom",
                    prompt = "Short for choomba. Friend.",
                    keywords = listOf("choom"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Choomah",
                    prompt = "Derogatory term for choomba.",
                    keywords = listOf("choomah"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Choomery",
                    prompt = "Derogatory term.",
                    keywords = listOf("choomery"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Choomba",
                    prompt = "Friend.",
                    keywords = listOf("choomba"),
                    activationType = LorebookActivationType.KEYWORDS
                )
            )
        ),
        Lorebook(
            name = "Cyberware",
            description = "Common cyberware implants in Night City",
            entries = listOf(
                LorebookEntry(
                    name = "Sandevistan",
                    prompt = "Military-grade cyberware that slows down time for the user.",
                    keywords = listOf("sandevistan"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Kerenzikov",
                    prompt = "Military-grade cyberware that slows down time.",
                    keywords = listOf("kerenzikov"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Berserk",
                    prompt = "Cyberware that increases strength and aggression.",
                    keywords = listOf("berserk"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Gorilla Arms",
                    prompt = "Cyberware that replaces arms with enhanced strength.",
                    keywords = listOf("gorilla arms"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Monowire",
                    prompt = "Cyberware wire weapon that can cut through almost anything.",
                    keywords = listOf("monowire"),
                    activationType = LorebookActivationType.KEYWORDS
                )
            )
        ),
        Lorebook(
            name = "Night City Districts",
            description = "Different districts of Night City",
            entries = listOf(
                LorebookEntry(
                    name = "Watson",
                    prompt = "District in Night City. Home to many immigrants and workers.",
                    keywords = listOf("watson"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Westbrook",
                    prompt = "Wealthy district in Night City.",
                    keywords = listOf("westbrook"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "City Center",
                    prompt = "Central district of Night City.",
                    keywords = listOf("city center"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Heywood",
                    prompt = "District in Night City.",
                    keywords = listOf("heywood"),
                    activationType = LorebookActivationType.KEYWORDS
                ),
                LorebookEntry(
                    name = "Pacifica",
                    prompt = "Abandoned district in Night City.",
                    keywords = listOf("pacifica"),
                    activationType = LorebookActivationType.KEYWORDS
                )
            )
        )
    )

    vm.updateSettings(
        settings.copy(lorebooks = settings.lorebooks + mockLorebooks)
    )
}

private fun removeMockLorebooks(vm: me.lucid.domchat.ui.pages.setting.SettingVM) {
    val mockNames = listOf(
        "Cyberpunk 2077 Lore",
        "Cyberpunk Edgerunners",
        "Cyberpunk Slang",
        "Cyberware",
        "Night City Districts"
    )
    vm.updateSettings(
        settings.copy(lorebooks = settings.lorebooks.filter { !mockNames.contains(it.name) })
    )
}

private fun injectMockCards(vm: me.lucid.domchat.ui.pages.setting.SettingVM) {
    val mockAssistants = listOf(
        Assistant(
            name = "Cyberpunk Edgerunner",
            systemPrompt = "You are an edgerunner from Night City. Speak with street slang and attitude.",
            alternateGreetings = listOf(
                "Hey choomba, what's happening?",
                "You looking for work?",
                "Watch your back in Night City."
            ),
            tags = emptyList(),
            creatorNotes = "Cyberpunk themed assistant",
            characterVersion = "1.0"
        ),
        Assistant(
            name = "Corpo Executive",
            systemPrompt = "You are a high-ranking corpo executive. Speak formally and professionally.",
            alternateGreetings = listOf(
                "Good day. How may I assist you?",
                "Welcome to Arasaka Corporation.",
                "What can I do for you today?"
            ),
            tags = emptyList(),
            creatorNotes = "Corporate themed assistant"
        ),
        Assistant(
            name = "Netrunner",
            systemPrompt = "You are a netrunner who specializes in hacking and cyberspace.",
            alternateGreetings = listOf(
                "Jacking in...",
                "Welcome to the net.",
                "What do you need hacked?"
            ),
            tags = emptyList(),
            creatorNotes = "Netrunner themed assistant"
        ),
        Assistant(
            name = "Street Doc",
            systemPrompt = "You are an underground doctor who helps people without asking questions.",
            alternateGreetings = listOf(
                "What happened?",
                "Come in, don't tell anyone.",
                "I can fix that."
            ),
            tags = emptyList(),
            creatorNotes = "Street doc themed assistant"
        ),
        Assistant(
            name = "Fixer",
            systemPrompt = "You are a fixer who connects people with jobs.",
            alternateGreetings = listOf(
                "Got a job for you.",
                "What do you need?",
                "I know a guy."
            ),
            tags = emptyList(),
            creatorNotes = "Fixer themed assistant"
        )
    )

    vm.updateSettings(
        settings.copy(assistants = settings.assistants + mockAssistants)
    )
}

private fun removeMockCards(vm: me.lucid.domchat.ui.pages.setting.SettingVM) {
    val mockNames = listOf(
        "Cyberpunk Edgerunner",
        "Corpo Executive",
        "Netrunner",
        "Street Doc",
        "Fixer"
    )
    vm.updateSettings(
        settings.copy(assistants = settings.assistants.filter { !mockNames.contains(it.name) })
    )
}