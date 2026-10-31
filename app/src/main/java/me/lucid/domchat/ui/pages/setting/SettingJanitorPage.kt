package me.lucid.domchat.ui.pages.setting

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import coil3.compose.AsyncImage
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import me.lucid.common.http.urlEncode
import me.lucid.common.platform.PlatformHttpClient
import me.lucid.common.platform.PlatformHttpRequest
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.model.Avatar
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
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("avatar") val avatarHash: String? = null,
    @SerialName("avatar_hash") val avatarHash2: String? = null,
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
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("systemPrompt") val systemPrompt: String? = null,
    @SerialName("alternateGreetings") val alternateGreetings: List<String> = emptyList(),
    @SerialName("avatar") val avatarHash: String? = null,
    @SerialName("avatar_hash") val avatarHash2: String? = null,
)

private fun resolveAvatarUrl(raw: String?): String =
    if (raw.isNullOrBlank()) "" else if (raw.startsWith("http")) raw else "$ELLA_CDN_BASE$raw"

sealed class JanitorLoadState {
    object Loading : JanitorLoadState()
    data class Success(val items: List<JanitorCharacter>) : JanitorLoadState()
    data class Error(val message: String) : JanitorLoadState()
}

@Serializable
private data class JanitorExtractRequest(
    @SerialName("character_id") val characterId: String,
)

@Composable
fun SettingJanitorPage() {
    val httpClient: PlatformHttpClient = koinInject<PlatformHttpClient>()
    val assistantVM: AssistantVM = koinViewModel<AssistantVM>()
    val toaster = LocalToaster.current
    val haptics = rememberPremiumHaptics()
    val scope = rememberCoroutineScope()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val tabs = listOf("Trending 24h", "Trending", "Search")
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    var browseState by remember { mutableStateOf(JanitorLoadState.Loading) }
    var searchState by remember { mutableStateOf(JanitorLoadState.Loading) }

    var selectedCharacter by remember { mutableStateOf<JanitorCharacter?>(null) }
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isImporting by remember { mutableStateOf(false) }

    val activeState: JanitorLoadState = if (selectedTab == 2) searchState else browseState
    val currentItems: List<JanitorCharacter> = when (activeState) {
        is JanitorLoadState.Success -> activeState.items
        else -> emptyList()
    }
    val isLoading: Boolean = activeState is JanitorLoadState.Loading
    val loadError: String? = when (activeState) {
        is JanitorLoadState.Error -> activeState.message
        else -> null
    }

    fun loadBrowse(tab: String = "trending") {
        scope.launch {
            try {
                browseState = JanitorLoadState.Loading
                val response = withContext(Dispatchers.IO) {
                    httpClient.execute(
                        PlatformHttpRequest(
                            method = "GET",
                            url = "$JANITOR_BASE_URL/api/browse?tab=$tab",
                        )
                    )
                }
                if (response.statusCode in 200..299) {
                    val json = String(response.body, charset = Charsets.UTF_8)
                    val list = JsonInstant.decodeFromString<JanitorListResponse>(json)
                    withContext(Dispatchers.Main) {
                        browseState = JanitorLoadState.Success((list.bots + list.characters + list.results).distinctBy { it.id })
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        browseState = JanitorLoadState.Error("HTTP ${response.statusCode}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    browseState = JanitorLoadState.Error(e.message ?: e.toString())
                }
            }
        }
    }

    fun loadSearch() {
        scope.launch {
            try {
                searchState = JanitorLoadState.Loading
                val encodedQuery = searchQuery.urlEncode()
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
                        searchState = JanitorLoadState.Error("HTTP ${response.statusCode}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    searchState = JanitorLoadState.Error(e.message ?: e.toString())
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
                            body = JsonInstant.encodeToString(JanitorExtractRequest(characterId = character.id)).toByteArray(),
                            mediaType = "application/json",
                        )
                    )
                }
                if (response.statusCode in 200..299) {
                    val json = String(response.body, charset = Charsets.UTF_8)
                    val extract = JsonInstant.decodeFromString<JanitorExtractResponse>(json)
                    val assistant = extractToAssistant(extract)
                    withContext(Dispatchers.Main) {
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

    LaunchedEffect(selectedTab) {
        if (selectedTab != 2) {
            loadBrowse(if (selectedTab == 0) "24h" else "trending")
        } else if (searchQuery.isBlank()) {
            searchState = JanitorLoadState.Success(emptyList())
        }
    }

    LaunchedEffect(searchQuery) {
        if (selectedTab == 2 && searchQuery.isNotBlank()) {
            loadSearch()
        } else if (selectedTab == 2 && searchQuery.isBlank()) {
            searchState = JanitorLoadState.Success(emptyList())
        }
    }

    Scaffold(
        topBar = {
            OneUITopAppBar(
                title = "JanitorAI Hub",
                scrollBehavior = scrollBehavior,
                navigationIcon = { BackButton() },
                actions = {
                    Image(
                        painter = painterResource(id = R.drawable.janitorai_logo),
                        contentDescription = "JanitorAI Hub",
                        modifier = Modifier.size(32.dp),
                        contentScale = ContentScale.Fit,
                    )
                },
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        text = { Text(tab) },
                        selected = selectedTab == index,
                        onClick = {
                            haptics.perform(HapticPattern.Pop)
                            selectedTab = index
                        },
                    )
                }
            }

            if (selectedTab == 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search characters...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = AppShapes.SearchField,
                    colors = appFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(AppShapes.SearchField)
                        .border(
                            0.5.dp,
                            LiquidGlassTokens.SpecularBorderSubtle,
                            AppShapes.SearchField,
                        ),
                )
            }

            loadError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (currentItems.isEmpty() && !isLoading) {
                    item {
                        Text(
                            text = if (selectedTab == 2 && searchQuery.isNotBlank()) {
                                "No characters found"
                            } else {
                                "No characters available"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                        )
                    }
                } else {
                    items(currentItems) { character ->
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
                }
            }
        }
    }

    selectedCharacter?.let { character ->
        ModalBottomSheet(
            containerColor = Color(0xFF141520),
            shape = AppShapes.Dialog,
            modifier = Modifier.border(
                0.5.dp,
                LiquidGlassTokens.SpecularBorderBrush,
                AppShapes.Dialog,
            ),
            contentColor = Color(0xFFF2F2F7),
            onDismissRequest = {
                selectedCharacter = null
            },
            sheetState = bottomSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = character.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFFF2F2F7),
                )
                val displayCreator = character.creator ?: character.creatorName
                displayCreator?.let { creator ->
                    Text(
                        text = "by $creator",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.65f),
                    )
                }
                character.tags.filter { it.isNotBlank() }.take(5).joinToString(separator = " • ").let { tagsText ->
                    if (tagsText.isNotBlank()) {
                        Text(
                            text = tagsText,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF93C5FD),
                        )
                    }
                }
                val displayBio = character.bio ?: character.description
                displayBio?.orEmpty().takeIf { it.isNotBlank() }?.let { bio ->
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        importCharacter(character)
                    },
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Import to Hub")
                    }
                }
            }
        }
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
            .aspectRatio(1f)
            .background(Color(0xFF141520))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.default_generical_pfp),
                        fallback = painterResource(id = R.drawable.default_generical_pfp),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = Color.White.copy(alpha = 0.5f),
                    )
                }
            }
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF2F2F7),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            character.creator?.orEmpty().takeIf { it.isNotBlank() }?.let { creator ->
                Text(
                    text = "by $creator",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } ?: run {
                character.creatorName?.orEmpty().takeIf { it.isNotBlank() }?.let { creatorName ->
                    Text(
                        text = "by $creatorName",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            character.tags.take(3).joinToString(separator = ", ").let { tags ->
                if (tags.isNotBlank()) {
                    Text(
                        text = tags,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF93C5FD),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun extractToAssistant(extract: JanitorExtractResponse): Assistant {
    val name = extract.name ?: "Imported Character"
    val bio = extract.bio ?: extract.description ?: ""
    val creator = extract.creator ?: extract.creatorName
    val avatarUrl = resolveAvatarUrl(extract.avatarHash ?: extract.avatarHash2)
    return Assistant(
        name = name,
        avatar = if (avatarUrl.isBlank()) Avatar.Dummy else Avatar.Image(avatarUrl),
        systemPrompt = extract.systemPrompt ?: bio,
        alternateGreetings = extract.alternateGreetings,
        creator = creator,
        characterVersion = extract.id,
        creatorNotes = "Imported from JanitorAI Hub",
    )
}
