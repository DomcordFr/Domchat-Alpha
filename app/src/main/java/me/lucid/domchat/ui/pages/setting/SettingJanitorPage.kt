package me.lucid.domchat.ui.pages.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import kotlin.text.Charsets
import kotlin.text.Regex
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import me.lucid.common.http.urlEncode
import me.lucid.common.platform.PlatformHttpClient
import me.lucid.common.platform.PlatformHttpRequest
import me.lucid.ai.core.MessageRole
import me.lucid.ai.ui.UIMessage
import me.lucid.ai.ui.UIMessagePart
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.model.Avatar
import me.lucid.domchat.data.model.Tag
import me.lucid.domchat.data.datastore.Settings
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.ui.components.nav.OneUITopAppBar
import me.lucid.domchat.ui.components.ui.ToastType
import me.lucid.domchat.ui.context.LocalToaster
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.pages.assistant.AssistantVM
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.ui.theme.appFieldColors
import me.lucid.domchat.utils.JsonInstant
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

private const val JANITOR_BASE_URL = "https://yuki.domcord.org"
private const val ELLA_CDN_BASE = "https://ella.janitorai.com/bot-avatars/"

@Serializable
data class JanitorCharacter(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("creator") val creator: String? = null,
    @SerialName("creator_name") val creatorName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("character_version") val characterVersion: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("avatar") val avatarHash: String? = null,
    @SerialName("avatar_hash") val avatarHash2: String? = null,
    @SerialName("systemPrompt") val systemPrompt: String? = null,
    @SerialName("firstMessage") val firstMessage: String? = null,
    @SerialName("alternateGreetings") val alternateGreetings: List<String> = emptyList(),
)

@Serializable
data class JanitorListResponse(
    @SerialName("bots") val bots: List<JanitorCharacter> = emptyList(),
    @SerialName("characters") val characters: List<JanitorCharacter> = emptyList(),
    @SerialName("results") val results: List<JanitorCharacter> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class JanitorExtractResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("creator") val creator: String? = null,
    @SerialName("creator_name") val creatorName: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("scenario") val scenario: String? = null,
    @SerialName("firstMessage") val firstMessage: String? = null,
    @SerialName("character_version") val characterVersion: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("embeddedImages") val embeddedImages: List<String> = emptyList(),
    @SerialName("systemPrompt") val systemPrompt: String? = null,
    @SerialName("alternateGreetings") val alternateGreetings: List<String> = emptyList(),
    @SerialName("avatar") val avatarHash: String? = null,
    @SerialName("avatar_hash") val avatarHash2: String? = null,
)

private fun resolveAvatarUrl(raw: String?): String =
    if (raw.isNullOrBlank()) "" else if (raw.startsWith("http")) raw else "$ELLA_CDN_BASE$raw"

/**
 * Extracts a JanitorAI bot UUID from a full bot URL (e.g. https://janitorai.com/bots/abc-123-def).
 * Returns null if no UUID pattern is found.
 */
private fun extractJanitorUuidFromUrl(url: String): String? {
    val uuidRegex = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    return uuidRegex.find(url)?.value
}

/**
 * Strips raw HTML tags and decodes common HTML entities so creator notes display as clean text.
 */
private fun sanitizeHtml(html: String?): String {
    val text = html ?: return ""
    var result = text
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&#39;", "'")
        .replace("&#x27;", "'")
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()?.let { it.toChar() }?.toString() ?: match.value
        }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { match ->
            match.groupValues[1].toIntOrNull(16)?.let { it.toChar() }?.toString() ?: match.value
        }
        .replace(Regex("\\s+"), " ")
        .trim()
    return result
}

/**
 * Normalizes a tag string for comparison by stripping emojis and non-alphanumeric characters,
 * then lowercasing. So "👩 Female" becomes "female", matching "female", "FEMALE", etc.
 */
private fun normalizeTag(tag: String): String {
    return tag
        .filter { c -> c.isLetterOrDigit() || c.isWhitespace() }
        .lowercase()
        .trim()
}

sealed class JanitorLoadState {
    object Loading : JanitorLoadState()
    data class Success(val items: List<JanitorCharacter>) : JanitorLoadState()
    data class Error(val message: String) : JanitorLoadState()
}

@Serializable
private data class JanitorExtractRequest(
    @SerialName("bot_id") val botId: String,
    @SerialName("character_id") val characterId: String,
)

@Composable
fun SettingJanitorPage() {
    val httpClient: PlatformHttpClient = koinInject<PlatformHttpClient>()
    val assistantVM: AssistantVM = koinViewModel<AssistantVM>()
    val settings by assistantVM.settings.collectAsStateWithLifecycle()
    val toaster = LocalToaster.current
    val haptics = rememberPremiumHaptics()
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val tabs = listOf("Trending 24h", "Trending", "Search")
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var browseState by remember { mutableStateOf<JanitorLoadState>(JanitorLoadState.Loading) }
    var searchState by remember { mutableStateOf<JanitorLoadState>(JanitorLoadState.Loading) }
    var isTabSwitching by remember { mutableStateOf(false) }
    var feedMode by rememberSaveable { mutableStateOf("Popular") }
    var page by rememberSaveable { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var feedMenuOpen by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var sortMode by rememberSaveable { mutableStateOf("Trending 24h") }
    var filterTags by remember { mutableStateOf(emptySet<String>()) }

    var selectedCharacter by remember { mutableStateOf<JanitorCharacter?>(null) }
    var viewerImageUrls by remember { mutableStateOf(emptyList<String>()) }
    var viewerOpen by remember { mutableStateOf(false) }
    var viewerCurrentPage by remember { mutableIntStateOf(0) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isImporting by remember { mutableStateOf(false) }

    val activeState: JanitorLoadState = if (searchQuery.isNotBlank()) searchState else browseState
    val currentItems: List<JanitorCharacter> = when (activeState) {
        is JanitorLoadState.Success -> activeState.items
        else -> emptyList()
    }
    val isLoading: Boolean = isTabSwitching || activeState is JanitorLoadState.Loading
    val loadError: String? = when (activeState) {
        is JanitorLoadState.Error -> activeState.message
        else -> null
    }

    val sortedFilteredItems: List<JanitorCharacter> = when (activeState) {
        is JanitorLoadState.Success -> {
            val normalizedFilterTags = filterTags.map { normalizeTag(it) }.toSet()
            val items = activeState.items.filter { character ->
                normalizedFilterTags.isEmpty() || character.tags.any { tag ->
                    normalizedFilterTags.contains(normalizeTag(tag))
                }
            }
            when (sortMode) {
                "Name A-Z" -> items.sortedBy { it.name.lowercase() }
                else -> items
            }
        }
        else -> emptyList()
    }

    fun loadBrowse(tab: String = "trending", pageNum: Int = 1, append: Boolean = false) {
        scope.launch {
            try {
                if (!append) {
                    browseState = JanitorLoadState.Loading
                } else {
                    isLoadingMore = true
                }
                val response = withContext(Dispatchers.IO) {
                    httpClient.execute(
                        PlatformHttpRequest(
                            method = "GET",
                            url = "$JANITOR_BASE_URL/api/browse?tab=$tab&page=$pageNum",
                        )
                    )
                }
                if (response.statusCode in 200..299) {
                    val json = String(response.body, charset = Charsets.UTF_8)
                    val list = JsonInstant.decodeFromString<JanitorListResponse>(json)
                    val newItems = (list.bots + list.characters + list.results).distinctBy { it.id }
                    withContext(Dispatchers.Main) {
                        if (append) {
                            val existing = (browseState as? JanitorLoadState.Success)?.items ?: emptyList()
                            browseState = JanitorLoadState.Success((existing + newItems).distinctBy { it.id })
                            isLoadingMore = false
                        } else {
                            browseState = JanitorLoadState.Success(newItems.distinctBy { it.id })
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        if (append) isLoadingMore = false
                        browseState = JanitorLoadState.Error("HTTP ${response.statusCode}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (append) isLoadingMore = false
                    browseState = JanitorLoadState.Error(e.message ?: e.toString())
                }
            }
        }
    }

    fun loadSearch() {
        scope.launch {
            try {
                searchState = JanitorLoadState.Loading
                val trimmedQuery = searchQuery.trim()
                val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}").find(trimmedQuery)?.value

                if (uuid != null) {
                    // UUID present (raw UUID or pasted character URL): POST to /api/extract
                    val body = JsonInstant.encodeToString(JanitorExtractRequest(botId = uuid, characterId = uuid)).toByteArray()
                    val response = withContext(Dispatchers.IO) {
                        httpClient.execute(
                            PlatformHttpRequest(
                                method = "POST",
                                url = "$JANITOR_BASE_URL/api/extract",
                                body = body,
                                mediaType = "application/json",
                            )
                        )
                    }
                    if (response.statusCode in 200..299) {
                        val json = String(response.body, charset = Charsets.UTF_8)
                        val extract = JsonInstant.decodeFromString<JanitorExtractResponse>(json)
                        withContext(Dispatchers.Main) {
                            searchState = JanitorLoadState.Success(listOf(
                                JanitorCharacter(
                                    id = extract.id ?: "",
                                    name = extract.name ?: "Imported Character",
                                    creator = extract.creator ?: extract.creatorName,
                                    bio = extract.bio,
                                    description = extract.description,
                                    characterVersion = extract.characterVersion,
                                    tags = extract.tags,
                                    avatarHash = extract.avatarHash ?: extract.avatarHash2,
                                    systemPrompt = extract.systemPrompt,
                                    firstMessage = extract.firstMessage,
                                    alternateGreetings = extract.alternateGreetings,
                                )
                            ))
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            searchState = JanitorLoadState.Error("Failed to load character. Server returned HTTP ${response.statusCode}. Please try again later.")
                        }
                    }
                } else {
                    // Normal query: search bots and creators via /api/search
                    val encodedQuery = trimmedQuery.urlEncode()
                    val response = withContext(Dispatchers.IO) {
                        httpClient.execute(
                            PlatformHttpRequest(
                                method = "GET",
                                url = "$JANITOR_BASE_URL/api/search?q=$encodedQuery",
                            )
                        )
                    }
                    if (response.statusCode in 200..299) {
                        val json = String(response.body, charset = Charsets.UTF_8)
                        val list = JsonInstant.decodeFromString<JanitorListResponse>(json)
                        withContext(Dispatchers.Main) {
                            searchState = JanitorLoadState.Success((list.bots + list.characters + list.results).distinctBy { it.id })
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            searchState = JanitorLoadState.Error("Search failed. Server returned HTTP ${response.statusCode}. Please try again later.")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    searchState = JanitorLoadState.Error("An unexpected error occurred. Please try again later.")
                }
            }
        }
    }

    fun importCharacter(character: JanitorCharacter) {
        scope.launch {
            try {
                isImporting = true
                val response = withContext(Dispatchers.IO) {
                    httpClient.execute(
                        PlatformHttpRequest(
                            method = "POST",
                            url = "$JANITOR_BASE_URL/api/extract",
                            body = JsonInstant.encodeToString(JanitorExtractRequest(botId = character.id, characterId = character.id)).toByteArray(),
                            mediaType = "application/json",
                        )
                    )
                }
                if (response.statusCode in 200..299) {
                    val json = String(response.body, charset = Charsets.UTF_8)
                    val extract = JsonInstant.decodeFromString<JanitorExtractResponse>(json)
                    withContext(Dispatchers.Main) {
                        // Register new tags into settings.assistantTags first, so UUIDs are stable
                        val newTagNames = extract.tags.filter { tagName ->
                            tagName.isNotBlank() && settings.assistantTags.none { tag -> tag.name == tagName }
                        }
                        val updatedSettings = if (newTagNames.isNotEmpty()) {
                            val newTags = newTagNames.map { Tag(id = Uuid.random(), name = it) }
                            assistantVM.updateSettings(
                                settings.copy(
                                    assistantTags = settings.assistantTags + newTags
                                )
                            )
                            settings.copy(assistantTags = settings.assistantTags + newTags)
                        } else {
                            settings
                        }
                        val assistant = extractToAssistant(extract, updatedSettings)
                        assistantVM.addAssistant(assistant)
                        isImporting = false
                        selectedCharacter = null
                        bottomSheetState.hide()
                        toaster.show(
                            message = "Imported '${extract.name ?: character.name}' to Hub",
                            type = ToastType.Success,
                        )
                        haptics.perform(HapticPattern.Success)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isImporting = false
                    }
                    toaster.show(
                        message = "Import failed: HTTP ${response.statusCode}",
                        type = ToastType.Error,
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isImporting = false
                }
                toaster.show(
                    message = "Import failed: ${e.message ?: e.toString()}",
                    type = ToastType.Error,
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBrowse("24h")
    }

    LaunchedEffect(selectedTab, feedMode) {
        isTabSwitching = true
        page = 1
        try {
            if (selectedTab != 2) {
                val tab = when (feedMode) {
                    "Popular" -> "popular"
                    "Trending" -> "trending"
                    "Trending 24h" -> "24h"
                    else -> "latest"
                }
                loadBrowse(tab, page)
            } else if (searchQuery.isBlank()) {
                searchState = JanitorLoadState.Success(emptyList())
            }
        } finally {
            isTabSwitching = false
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank()) {
            delay(350)
            if (searchQuery.isNotBlank()) {
                loadSearch()
            }
        } else {
            searchState = JanitorLoadState.Success(emptyList())
        }
    }

    Scaffold(
        topBar = {
            OneUITopAppBar(
                title = "JanitorAI Hub",
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton() },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header items will go inside LazyVerticalGrid as item(span = { GridItemSpan(maxLineSpan) })
        }

            loadError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Content area: loader centered, or single unified grid (header controls scroll away with cards)
            if (isLoading && currentItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    JanitorOrbitalLoader()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header items: scroll away with cards (only OneUITopAppBar stays sticky)
                    item(
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Top row: Feed mode dropdown + Sort & Filter buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Feed mode dropdown pill
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF000000))
                                            .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp)),
                                        color = Color.Transparent,
                                        onClick = { feedMenuOpen = true },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = feedMode,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White,
                                                fontWeight = FontWeight.Medium,
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowDropDown,
                                                contentDescription = "Dropdown",
                                                tint = Color(0xFF8A8A96),
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = feedMenuOpen,
                                        onDismissRequest = { feedMenuOpen = false },
                                        modifier = Modifier
                                            .background(Color(0xFF000000))
                                            .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(8.dp))
                                    ) {
                                        listOf("Popular", "Trending", "Trending 24h", "Latest").forEach { option ->
                                            val isSelected = feedMode == option
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = option,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                                    )
                                                },
                                                onClick = {
                                                    haptics.perform(HapticPattern.Pop)
                                                    feedMode = option
                                                    page = 1
                                                    feedMenuOpen = false
                                                    val tab = when (option) {
                                                        "Popular" -> "popular"
                                                        "Trending" -> "trending"
                                                        "Trending 24h" -> "24h"
                                                        else -> "latest"
                                                    }
                                                    loadBrowse(tab, page)
                                                },
                                                trailingIcon = if (isSelected) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Check,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                        )
                                                    }
                                                } else null,
                                            )
                                        }
                                    }
                                }

                                // Right side: Sort and Filter buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sort button
                                    Box {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(Color(0xFF000000))
                                                .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp)),
                                            color = Color.Transparent,
                                            onClick = { sortMenuOpen = true },
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Sort,
                                                    contentDescription = "Sort",
                                                    tint = Color(0xFF8A8A96),
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = "Sort",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF8A8A96),
                                                )
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = sortMenuOpen,
                                            onDismissRequest = { sortMenuOpen = false },
                                            modifier = Modifier
                                                .background(Color(0xFF000000))
                                                .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(8.dp))
                                        ) {
                                            listOf("Trending 24h", "Trending", "Popular", "Latest", "Name A-Z").forEach { option ->
                                                val isSelected = sortMode == option
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = option,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isSelected) Color.White else Color(0xFFCCCCCC),
                                                        )
                                                    },
                                                    onClick = {
                                                        haptics.perform(HapticPattern.Pop)
                                                        sortMode = option
                                                        sortMenuOpen = false
                                                    },
                                                    trailingIcon = if (isSelected) {
                                                        {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                            )
                                                        }
                                                    } else null,
                                                )
                                            }
                                        }
                                    }

                                    // Filter button
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF000000))
                                            .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp)),
                                        color = Color.Transparent,
                                        onClick = { filterMenuOpen = true },
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FilterAlt,
                                                contentDescription = "Filter",
                                                tint = if (filterTags.isNotEmpty()) Color.White else Color(0xFF8A8A96),
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Text(
                                                text = if (filterTags.isEmpty()) "Filter" else "Filter (${filterTags.size})",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontSize = 12.sp,
                                                color = if (filterTags.isNotEmpty()) Color.White else Color(0xFF8A8A96),
                                            )
                                        }
                                    }
                                }
                            }

                            // Search bar (full width, text queries and Janitor bot URLs)
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        "Search characters or paste link...",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF8A8A96)) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF8A8A96))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = AppShapes.SearchField,
                                colors = appFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppShapes.SearchField)
                                    .border(
                                        0.5.dp,
                                        LiquidGlassTokens.SpecularBorderSubtle,
                                        AppShapes.SearchField,
                                    ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onSearch = {
                                        if (searchQuery.isNotBlank()) {
                                            scope.launch {
                                                loadSearch()
                                            }
                                        }
                                    },
                                ),
                            )
                        }
                    }

                    // Character items
                    items(sortedFilteredItems, key = { it.id }) { character ->
                        CharacterCard(
                            character = character,
                            onClick = {
                                selectedCharacter = character
                                scope.launch {
                                    bottomSheetState.show()
                                }
                            },
                        )
                    }

                    // Load more button spanning both columns
                    if (!isLoadingMore && currentItems.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .widthIn(max = 200.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFF000000))
                                        .border(
                                            0.5.dp,
                                            LiquidGlassTokens.SpecularBorderSubtle,
                                            RoundedCornerShape(20.dp)
                                        ),
                                    color = Color.Transparent,
                                    onClick = {
                                        haptics.perform(HapticPattern.Pop)
                                        page++
                                        val tab = when (feedMode) {
                                            "Popular" -> "popular"
                                            "Trending" -> "trending"
                                            "Trending 24h" -> "24h"
                                            else -> "latest"
                                        }
                                        loadBrowse(tab, page, append = true)
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Load More",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Dialog
    if (filterMenuOpen) {
        Dialog(onDismissRequest = { filterMenuOpen = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF000000))
                    .border(0.5.dp, LiquidGlassTokens.SpecularBorderBrush, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter by Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        IconButton(onClick = { filterMenuOpen = false }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    val filterTagList = listOf("👩 Female", "👨 Male", "❤️‍🔥 Smut", "🕊🗡 Dead Dove", "❤️‍🩹 Fluff", "💔 Angst", "🐉 Fantasy", "🚀 Sci-Fi", "⛓️ Dominant", "👩 FemPov", "🧑‍🎨 OC", "😂 Comedy", "🔦 Horror", "🎲 RPG")
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filterTagList.forEach { tag ->
                            val isSelected = filterTags.contains(tag)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color.White.copy(alpha = 0.20f) else Color.Transparent)
                                    .border(
                                        0.5.dp,
                                        if (isSelected) LiquidGlassTokens.SpecularBorderBrush else LiquidGlassTokens.SpecularBorderSubtle,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        haptics.perform(HapticPattern.Pop)
                                        filterTags = if (isSelected) filterTags - tag else filterTags + tag
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { filterTags = emptySet() }) {
                            Text("Reset", color = Color(0xFF8A8A96))
                        }
                        TextButton(onClick = { filterMenuOpen = false }) {
                            Text("Apply", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    selectedCharacter?.let { character ->
        val sheetBgColor = Color(0xFF000000)
        val cardBorderColor = LiquidGlassTokens.SpecularBorderSubtle
        val contentTextColor = Color(0xFFF2F2F7)
        val imageUrl = resolveAvatarUrl(character.avatarHash ?: character.avatarHash2)
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

        var sheetSelectedTab by remember { mutableIntStateOf(0) }
        val sheetTabTitles = listOf("Notes", "Personality", "Greetings")

        var fullExtract by remember(character.id) { mutableStateOf<JanitorExtractResponse?>(null) }
        var isExtractLoading by remember(character.id) { mutableStateOf(false) }

        LaunchedEffect(character.id) {
            isExtractLoading = true
            fullExtract = null
            try {
                val response = withContext(Dispatchers.IO) {
                    httpClient.execute(
                        PlatformHttpRequest(
                            method = "POST",
                            url = "$JANITOR_BASE_URL/api/extract",
                            body = JsonInstant.encodeToString(JanitorExtractRequest(botId = character.id, characterId = character.id)).toByteArray(),
                            mediaType = "application/json",
                        )
                    )
                }
                if (response.statusCode in 200..299) {
                    val json = String(response.body, charset = Charsets.UTF_8)
                    fullExtract = JsonInstant.decodeFromString<JanitorExtractResponse>(json)
                }
            } catch (_: Exception) {} finally {
                isExtractLoading = false
            }
        }

        ModalBottomSheet(
            containerColor = sheetBgColor,
            shape = sheetShape,
            sheetGesturesEnabled = false,
            modifier = Modifier.border(
                0.5.dp,
                LiquidGlassTokens.SpecularBorderBrush,
                sheetShape,
            ),
            contentColor = contentTextColor,
            onDismissRequest = {
                scope.launch {
                    bottomSheetState.hide()
                }.invokeOnCompletion {
                    selectedCharacter = null
                }
            },
            sheetState = bottomSheetState,
        ) {
            me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                val sheetScrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(sheetScrollState)
                ) {
                    // Full-bleed edge-to-edge hero image matching Character Hub
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(340.dp)
                        ) {
                            if (imageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = character.name,
                                    modifier = Modifier.fillMaxSize(),
                                    alignment = Alignment.TopCenter,
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(id = R.drawable.default_generical_pfp),
                                    fallback = painterResource(id = R.drawable.default_generical_pfp),
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF000000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = Color.White.copy(alpha = 0.5f),
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                sheetBgColor.copy(alpha = 0.15f),
                                                sheetBgColor.copy(alpha = 0.50f),
                                                sheetBgColor.copy(alpha = 0.85f),
                                                sheetBgColor.copy(alpha = 1f),
                                            )
                                        )
                                    )
                                )
                            // Drag handle overlaid at top center over hero image
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .align(Alignment.TopCenter)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color.White.copy(alpha = 0.35f))
                                )
                            }
                        }
                        // Close/cross button top-end corner over hero image
                        IconButton(
                            onClick = {
                                scope.launch {
                                    bottomSheetState.hide()
                                }.invokeOnCompletion {
                                    selectedCharacter = null
                                }
                            },
                            modifier = Modifier
                                .padding(top = 12.dp, end = 12.dp)
                                .align(Alignment.TopEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.50f))
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Content below hero image with 20.dp horizontal padding
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = character.name.takeIf { it.isNotBlank() } ?: "Character",
                            style = MaterialTheme.typography.headlineSmall,
                            color = contentTextColor,
                        )

                        val displayCreator = character.creator ?: character.creatorName
                        displayCreator?.let { creator ->
                            Text(
                                text = "Created by $creator",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.65f),
                            )
                        }

                        // Tags row
                        if (character.tags.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                items(character.tags, key = { it }) { tagId ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF000000))
                                            .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = tagId.replace("_", " ").trim(),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.92f),
                                        )
                                    }
                                }
                            }
                        }

                        // Embedded images gallery (from bio, description, greetings)
                        val embeddedImageUrls = remember(character, fullExtract) {
                            val list = mutableListOf<String>()
                            fullExtract?.embeddedImages?.let { list.addAll(it) }
                            val textBlobs = listOf(
                                fullExtract?.bio ?: character.bio.orEmpty(),
                                fullExtract?.description ?: character.description.orEmpty(),
                                fullExtract?.firstMessage ?: character.firstMessage.orEmpty(),
                            )
                            val imgRegex = Regex("https?://[^\\s<>\"]+\\.(?:png|jpg|jpeg|webp|gif)", RegexOption.IGNORE_CASE)
                            for (text in textBlobs) {
                                imgRegex.findAll(text).forEach { m -> list.add(m.value) }
                            }
                            list.filter { it.isNotBlank() }.distinct()
                        }
                        if (embeddedImageUrls.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Embedded Images",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentTextColor.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(embeddedImageUrls, key = { it }) { imageUrl ->
                                    Box(
                                        modifier = Modifier
                                            .size(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF141520))
                                            .border(0.5.dp, cardBorderColor, RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewerImageUrls = embeddedImageUrls.toList()
                                                viewerCurrentPage = embeddedImageUrls.indexOf(imageUrl).coerceAtLeast(0)
                                                viewerOpen = true
                                            }
                                    ) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            alignment = Alignment.Center,
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }

                        // Primary Action: Import to Hub
                        Button(
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                val extractToUse = fullExtract
                                if (extractToUse != null) {
                                    val newTagNames = extractToUse.tags.filter { tagName ->
                                        tagName.isNotBlank() && settings.assistantTags.none { tag -> tag.name == tagName }
                                    }
                                    val updatedSettings = if (newTagNames.isNotEmpty()) {
                                        val newTags = newTagNames.map { Tag(id = Uuid.random(), name = it) }
                                        assistantVM.updateSettings(
                                            settings.copy(assistantTags = settings.assistantTags + newTags)
                                        )
                                        settings.copy(assistantTags = settings.assistantTags + newTags)
                                    } else {
                                        settings
                                    }
                                    val assistant = extractToAssistant(extractToUse, updatedSettings)
                                    assistantVM.addAssistant(assistant)
                                    selectedCharacter = null
                                    toaster.show(
                                        message = "Imported '${assistant.name}' to Hub",
                                        type = ToastType.Success,
                                    )
                                    haptics.perform(HapticPattern.Success)
                                } else {
                                    importCharacter(character)
                                }
                            },
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color.Black,
                            ),
                        ) {
                            if (isImporting) {
                                JanitorOrbitalLoader(modifier = Modifier.size(20.dp))
                            } else {
                                Text("Import to Hub", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 3 Inspection Tabs (Notes, Personality, Greetings)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(sheetBgColor)
                                .border(0.5.dp, cardBorderColor, RoundedCornerShape(14.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            sheetTabTitles.forEachIndexed { index, title ->
                                val isSelected = sheetSelectedTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                Color.White.copy(alpha = 0.16f)
                                            } else Color.Transparent
                                        )
                                        .clickable { sheetSelectedTab = index }
                                        .padding(horizontal = 4.dp, vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) contentTextColor else contentTextColor.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AnimatedContent<Int>(
                            targetState = sheetSelectedTab,
                            transitionSpec = {
                                val isForward = targetState > initialState
                                (fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220)) { width -> if (isForward) width / 4 else -width / 4 }) togetherWith
                                    (fadeOut(animationSpec = tween(180)) + slideOutHorizontally(animationSpec = tween(180)) { width -> if (isForward) -width / 4 else width / 4 })
                            },
                            label = "JanitorSheetTabsContent"
                        ) { tab ->
                            when (tab) {
                                0 -> {
                                    // Notes Tab — creator notes / bio (HTML sanitized)
                                    val rawNotes = fullExtract?.description ?: character.bio ?: character.description
                                    val displayNotes = sanitizeHtml(rawNotes)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(sheetBgColor)
                                            .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                    ) {
                                        if (displayNotes.isNotBlank()) {
                                            Text(
                                                text = displayNotes,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = contentTextColor.copy(alpha = 0.9f),
                                            )
                                        } else {
                                            Text(
                                                text = "No notes available.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.5f),
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    // Personality Tab — system prompt / definition
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(sheetBgColor)
                                            .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                    ) {
                                        if (isExtractLoading) {
                                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                                JanitorOrbitalLoader(modifier = Modifier.size(36.dp))
                                            }
                                        } else {
                                            val systemPrompt = fullExtract?.systemPrompt ?: character.systemPrompt ?: ""
                                            if (systemPrompt.isNotBlank()) {
                                                Text(
                                                    text = systemPrompt,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = contentTextColor.copy(alpha = 0.9f),
                                                )
                                            } else {
                                                Text(
                                                    text = "No system prompt defined.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                )
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Greetings Tab — first message + alternate greetings
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        if (isExtractLoading) {
                                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                                JanitorOrbitalLoader(modifier = Modifier.size(36.dp))
                                            }
                                        } else {
                                            val firstMessage = fullExtract?.firstMessage ?: character.firstMessage.orEmpty()
                                            if (firstMessage.isNotBlank() && !isDummyGreeting(firstMessage)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(sheetBgColor)
                                                        .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                                        .padding(16.dp),
                                                ) {
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            text = "First Message",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = contentTextColor.copy(alpha = 0.7f),
                                                        )
                                                        Text(
                                                            text = firstMessage,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = contentTextColor.copy(alpha = 0.9f),
                                                        )
                                                    }
                                                }
                                            }

                                            val altGreetings = fullExtract?.alternateGreetings ?: character.alternateGreetings
                                            altGreetings.filter { !isDummyGreeting(it) }.forEachIndexed { i, alt ->
                                                if (alt.isNotBlank()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(sheetBgColor)
                                                            .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                                            .padding(16.dp),
                                                    ) {
                                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                            Text(
                                                                text = "Alternate Greeting #${i + 1}",
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = contentTextColor.copy(alpha = 0.7f),
                                                            )
                                                            Text(
                                                                text = alt,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = contentTextColor.copy(alpha = 0.9f),
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (firstMessage.isBlank() && altGreetings.isEmpty()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(sheetBgColor)
                                                        .border(0.5.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                                        .padding(16.dp),
                                                ) {
                                                    Text(
                                                        text = "No greetings available.",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = Color.White.copy(alpha = 0.5f),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

/**
 * Custom pulsing dark-glass orbital loader. Replaces the stock circular progress spinner
 * with a rotating ring that pulses alpha, matching the unified 0xFF141520 glass theme.
 */
@Composable
private fun JanitorOrbitalLoader(modifier: Modifier = Modifier.size(54.dp)) {
    val infiniteTransition = rememberInfiniteTransition(label = "janitorOrbital")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalRotation"
    )
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalCounterRotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbitalPulse"
    )
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing spinning aperture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = rotation
                    alpha = 0.7f + 0.3f * pulse
                }
                .border(
                    1.5.dp,
                    Brush.sweepGradient(
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.25f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.75f),
                        )
                    ),
                    CircleShape
                )
        )
        // Inner counter-rotating ring
        Box(
            modifier = Modifier
                .fillMaxSize(0.60f)
                .graphicsLayer {
                    rotationZ = counterRotation
                    alpha = 0.5f + 0.5f * pulse
                }
                .border(
                    1.dp,
                    Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.85f),
                            Color.Transparent,
                        )
                    ),
                    CircleShape
                )
        )
        // Center pulsing beacon
        Box(
            modifier = Modifier
                .fillMaxSize(0.22f)
                .graphicsLayer {
                    scaleX = 0.8f + 0.35f * pulse
                    scaleY = 0.8f + 0.35f * pulse
                    alpha = 0.75f + 0.25f * pulse
                }
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
private fun CharacterCard(
    character: JanitorCharacter,
    onClick: () -> Unit,
) {
    val imageUrl = resolveAvatarUrl(character.avatarHash ?: character.avatarHash2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF141520))
            .border(
                0.5.dp,
                LiquidGlassTokens.SpecularBorderSubtle,
                RoundedCornerShape(18.dp)
            )
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .clickable(onClick = onClick),
    ) {
        // Full-bleed avatar background
        if (imageUrl.isNotBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = character.name,
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.default_generical_pfp),
                fallback = painterResource(id = R.drawable.default_generical_pfp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF141520)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White.copy(alpha = 0.5f),
                )
            }
        }

        // Bottom vertical gradient scrim: transparent -> dark glass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.42f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF141520).copy(alpha = 0.85f),
                        )
                    )
                )
        )

        // Bottom content: display name + tag row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            val displayName = character.name.takeIf { it.isNotBlank() } ?: "Character"
            Text(
                text = displayName,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Horizontally scrollable tag pills — black glass, full words
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(character.tags, key = { it }) { tagId ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF000000))
                            .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = tagId.toString().replace("_", " "),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

private fun extractToAssistant(extract: JanitorExtractResponse, settings: Settings): Assistant {
    val name = extract.name ?: "Imported Character"
    val creator = extract.creator ?: extract.creatorName
    val avatarUrl = resolveAvatarUrl(extract.avatarHash ?: extract.avatarHash2)
    val systemPrompt = extract.systemPrompt ?: ""
    val creatorNotes = extract.description ?: ""

    // Map firstMessage to presetMessages as an assistant greeting
    val presetMessages = extract.firstMessage?.takeIf { it.isNotBlank() }?.let { message ->
        listOf(
            UIMessage(
                role = MessageRole.ASSISTANT,
                parts = listOf(UIMessagePart.Text(message)),
            )
        )
    } ?: emptyList()

    // Register string tags into settings.assistantTags, assigning tag UUIDs to assistant.tags
    val registeredTagIds = extract.tags.filter { it.isNotBlank() }.map { tagName ->
        settings.assistantTags.find { it.name == tagName }?.id ?: Uuid.random()
    }

    return Assistant(
        name = name,
        avatar = if (avatarUrl.isBlank()) Avatar.Dummy else Avatar.Image(avatarUrl),
        systemPrompt = systemPrompt,
        presetMessages = presetMessages,
        alternateGreetings = extract.alternateGreetings,
        creator = creator,
        creatorNotes = creatorNotes,
        characterVersion = extract.characterVersion ?: "",
        tags = registeredTagIds,
    )
}

/**
 * Returns true if a greeting text is a dummy placeholder (single dot, dash, underscore, "none"/"None"/"null", empty).
 */
private fun isDummyGreeting(text: String?): Boolean {
    val t = text?.trim() ?: return true
    return when (t) {
        "", ".", "..", "...", "-", "_", "none", "None", "null" -> true
        else -> false
    }
}
