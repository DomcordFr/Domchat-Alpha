package me.lucid.domchat.ui.pages.chat

import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import me.lucid.domchat.ui.theme.LocalDarkMode
import me.lucid.domchat.ui.theme.frostedGlassSurface
import me.lucid.domchat.ui.theme.liquidGlassSurface
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.component.backdrop.LocalAppBackdrop
import me.lucid.domchat.ui.component.backdrop.backdrops.layerBackdrop
import me.lucid.domchat.ui.component.backdrop.backdrops.rememberLayerBackdrop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlin.OptIn
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.model.Avatar
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.R

import me.lucid.domchat.Screen
import me.lucid.domchat.data.model.Conversation
import me.lucid.domchat.ui.components.ui.Tooltip
import me.lucid.domchat.ui.components.chat.ConversationRowSurface
import me.lucid.domchat.ui.components.dialog.UniversalDestructiveConfirmDialog
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.components.nav.DomChatDrawerSearch
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.ui.text.style.TextAlign
import me.lucid.domchat.ui.modifier.domChatSoftEdgeBorder

import me.lucid.domchat.ui.modifier.domChatIconPillContainerColor
import me.lucid.domchat.ui.modifier.domChatChromeTextShadow
import me.lucid.domchat.ui.modifier.domChatCardContainerColor
import me.lucid.domchat.ui.context.LocalSettings
import me.lucid.domchat.data.model.LiquidGlassMode
import me.lucid.domchat.ui.context.LocalNavController
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.utils.toLocalString
import me.lucid.domchat.utils.toLocalTime
import java.time.LocalDate
import java.time.ZoneId
import kotlin.uuid.Uuid
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.ui.components.nav.OneUITopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.lucid.domchat.utils.navigateToChatPage

/**
 * Represents different types of items in the conversation list
 */
sealed class ConversationListItem {
    data class DateHeader(
        val date: LocalDate,
        val label: String
    ) : ConversationListItem()
    data object PinnedHeader : ConversationListItem()
    data class Item(
        val conversation: Conversation
    ) : ConversationListItem()
}

@Composable
fun ConversationList(
    current: Conversation,
    conversations: LazyPagingItems<ConversationListItem>,
    conversationJobs: Set<Uuid>,
    completedGenerationIds: Set<Uuid> = emptySet(),
    recentlyRestoredIds: Set<Uuid> = emptySet(),
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchExpanded: Boolean = false,
    onSearchExpandedChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    onClick: (Conversation) -> Unit = {},
    onDelete: (Conversation) -> Unit = {},
    onRegenerateTitle: (Conversation) -> Unit = {},
    onEditTitle: (Conversation, String) -> Unit = { _, _ -> },
    onPin: (Conversation) -> Unit = {},
    getAssistant: (Uuid) -> Assistant? = { null },
    quickActions: (@Composable () -> Unit)? = null,
    bottomContent: (@Composable () -> Unit)? = null,
) {
    val navController = LocalNavController.current
    Box(modifier = modifier) {
        val listState = rememberLazyListState()
        val canScrollBackward by remember {
            derivedStateOf { listState.canScrollBackward }
        }
        val canScrollForward by remember {
            derivedStateOf { listState.canScrollForward }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = if (isSearchExpanded || searchQuery.isNotBlank()) 88.dp else 64.dp,
                bottom = if (bottomContent != null) 58.dp else 8.dp,
            ),
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Quick action buttons (Imagine, Stats) - animated visibility
                    AnimatedVisibility(
                        visible = !isSearchExpanded && quickActions != null,
                        enter = fadeIn(animationSpec = spring(stiffness = 300f)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)),
                        exit = fadeOut(animationSpec = spring(stiffness = 500f)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f))
                    ) {
                        Column {
                            if (quickActions != null) {
                                quickActions()
                            }
                        }
                    }
                }
            }
            if (conversations.itemCount == 0) {
                item {
                    val emptyGlassFill = if (LocalDarkMode.current) Color(0xFFFFFFFF).copy(alpha = 0.08f) else Color.White.copy(alpha = 0.65f)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .liquidGlassSurface(
                                shape = RoundedCornerShape(20.dp),
                                fillColor = emptyGlassFill,
                                borderWidth = 0.5.dp,
                                specularBrush = LiquidGlassTokens.SpecularBorderSubtle,
                            ),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Transparent,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = stringResource(id = R.string.chat_page_no_conversations),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    shadow = domChatChromeTextShadow()
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Start a new conversation or select a character to begin",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    shadow = domChatChromeTextShadow(softText = true)
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(
                count = conversations.itemCount,
                key = conversations.itemKey { item ->
                    when (item) {
                        is ConversationListItem.DateHeader -> "date_${item.date}"
                        is ConversationListItem.PinnedHeader -> "pinned_header"
                        is ConversationListItem.Item -> item.conversation.id.toString()
                    }
                }
            ) { index ->
                AnimatedContent<Uuid>(
                    targetState = current.id,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) + slideInHorizontally(
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) { width -> width / 4 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(220, easing = FastOutSlowInEasing)) + slideOutHorizontally(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ) { width -> -width / 4 }
                            )
                    }
                ) {
                    when (val item = conversations[index]) {
                    is ConversationListItem.DateHeader -> {
                        DateHeaderItem(
                            label = item.label,
                            modifier = Modifier
                        )
                    }

                    is ConversationListItem.PinnedHeader -> {
                        PinnedHeader(
                            modifier = Modifier
                        )
                    }

                    is ConversationListItem.Item -> {
                        ConversationItem(
                            conversation = item.conversation,
                            selected = item.conversation.id == current.id,
                            loading = item.conversation.id in conversationJobs,
                            generationComplete = item.conversation.id in completedGenerationIds,
                            isRecentlyRestored = item.conversation.id in recentlyRestoredIds,
                            onClick = onClick,
                            onDelete = onDelete,
                            onRegenerateTitle = onRegenerateTitle,
                            onEditTitle = onEditTitle,
                            onPin = onPin,
                            searchQuery = searchQuery,
                            getAssistant = getAssistant,
                            modifier = Modifier
                        )
                    }

                    null -> {
                        // Placeholder for loading state
                    }
                }
            }
                }
        }

        if (canScrollBackward) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(112.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to MaterialTheme.colorScheme.surfaceContainerLow,
                                0.35f to MaterialTheme.colorScheme.surfaceContainerLow,
                                1f to Color.Transparent,
                            )
                        )
                    )
            )
        }

        if (canScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.78f to MaterialTheme.colorScheme.surfaceContainerLow,
                                1f to MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                        )
                    )
            )
        }

        DomChatDrawerSearch(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            expanded = isSearchExpanded,
            onExpandedChange = onSearchExpandedChange,
            placeholder = stringResource(id = R.string.chat_page_search_placeholder),
            hint = stringResource(R.string.chat_page_search_hint),
            containerColor = domChatIconPillContainerColor(),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
        )

        Box(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 8.dp)
        .zIndex(2f)
) {
    bottomContent?.invoke()
}
    }
}

@Composable
private fun DateHeaderItem(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                shadow = domChatChromeTextShadow()
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun PinnedHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.pinned_chats),
            style = MaterialTheme.typography.labelLarge.copy(
                shadow = domChatChromeTextShadow()
            ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItem(
    conversation: Conversation,
    selected: Boolean,
    loading: Boolean,
    generationComplete: Boolean,
    isRecentlyRestored: Boolean = false,
    modifier: Modifier = Modifier,
    onDelete: (Conversation) -> Unit = {},
    onRegenerateTitle: (Conversation) -> Unit = {},
    onEditTitle: (Conversation, String) -> Unit = { _, _ -> },
    onPin: (Conversation) -> Unit = {},
    searchQuery: String = "",
    getAssistant: (Uuid) -> Assistant? = { null },
    onClick: (Conversation) -> Unit
) {
    val haptics = rememberPremiumHaptics()
    val loadingDescription = stringResource(R.string.loading)
    val isDark = LocalDarkMode.current

    // Fade-in animation for recently restored items
    var hasAnimated by remember { mutableStateOf(!isRecentlyRestored) }
    val restoredAlpha by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "restored_alpha"
    )
    LaunchedEffect(isRecentlyRestored) {
        if (isRecentlyRestored && !hasAnimated) {
            kotlinx.coroutines.delay(50) // Small delay to ensure item is in layout
            hasAnimated = true
        }
    }
    
    var showMenu by remember { mutableStateOf(false) }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var editedTitle by remember(conversation.id) { mutableStateOf(conversation.title) }
    val messageOnlyMatch = searchQuery.isNotBlank() &&
        !conversation.title.contains(searchQuery, ignoreCase = true)

    val assistant = getAssistant(conversation.assistantId)

    Box(modifier = modifier.fillMaxWidth()) {
        ConversationRowSurface(
            selected = selected,
            restingAlpha = restoredAlpha,
            modifier = Modifier.fillMaxWidth().height(72.dp),
            onClick = {
                haptics.perform(HapticPattern.Tick)
                onClick(conversation)
            },
            onLongClick = {
                haptics.perform(HapticPattern.Buildup)
                showMenu = true
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 0.5.dp,
                        brush = if (selected) LiquidGlassTokens.SpecularBorderSubtle else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(
                        if (selected) Color(0xFF000000) else Color.Transparent
                    )
            ) {
                // Edge-to-edge character pfp on the right, showing 35% of the image smoothly blending into the left
                if (assistant != null) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .fillMaxWidth(0.35f)
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                    ) {
                        when (val av = assistant.avatar) {
                            is Avatar.Image -> {
                                AsyncImage(
                                    model = av.url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(0f, -0.14f),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    0.0f to Color.Transparent,
                                                    0.35f to Color.Black.copy(alpha = 0.45f),
                                                    1.0f to Color.Black
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                )
                            }
                            is Avatar.Resource -> {
                                AsyncImage(
                                    model = av.id,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alignment = BiasAlignment(0f, -0.14f),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                        .drawWithContent {
                                            drawContent()
                                            drawRect(
                                                brush = Brush.horizontalGradient(
                                                    0.0f to Color.Transparent,
                                                    0.35f to Color.Black.copy(alpha = 0.45f),
                                                    1.0f to Color.Black
                                                ),
                                                blendMode = BlendMode.DstIn
                                            )
                                        }
                                )
                            }
                            else -> {}
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (conversation.isFork) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = me.lucid.domchat.R.drawable.ic_fork_left),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(20.dp),
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (assistant != null) 48.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = conversation.title.ifBlank { stringResource(id = R.string.chat_page_new_message) },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                shadow = domChatChromeTextShadow()
                            ),
                            color = Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                        AnimatedVisibility(visible = messageOnlyMatch) {
                            Text(
                                text = stringResource(R.string.chat_page_search_message_match),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Count only the bot's messages (assistant turns), per design.
                        val msgCount = conversation.messageNodes.count { it.currentMessage.role == me.lucid.ai.core.MessageRole.ASSISTANT }
                        val zdt = conversation.updateAt.atZone(ZoneId.systemDefault())
                        val hour12 = if (zdt.hour % 12 == 0) 12 else zdt.hour % 12
                        val amPm = if (zdt.hour < 12) "AM" else "PM"
                        val timeStr = "%d:%02d %s".format(hour12, zdt.minute, amPm)
                        Text(
                            text = "$msgCount messages · $timeStr",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                shadow = domChatChromeTextShadow(softText = true)
                            ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    AnimatedVisibility(conversation.isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = stringResource(R.string.chat_message_pinned),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    AnimatedVisibility(loading) {
                        LoadingIndicator(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(18.dp)
                                .semantics {
                                    contentDescription = loadingDescription
                                },
                            color = LocalContentColor.current,
                        )
                    }
                    AnimatedVisibility(!loading && generationComplete) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(CircleShape)
                                .background(LocalContentColor.current)
                                .size(7.dp)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(animationSpec = tween(120)) + scaleIn(
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
                initialScale = 0.92f,
            ),
            exit = fadeOut(animationSpec = tween(100)) + scaleOut(
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
                targetScale = 0.92f,
            ),
        ) {
            // Tap anywhere on the row to dismiss the in-tree overlay menu
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .combinedClickable(
                        onLongClick = {},
                        onClick = { showMenu = false },
                    )
            )

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .zIndex(2f),
            ) {
                ConversationActionMenu(
                    visible = true,
                    onDismissRequest = { showMenu = false },
                    conversation = conversation,
                    onPin = onPin,
                    onEditTitle = {
                        editedTitle = conversation.title
                        showEditTitleDialog = true
                    },
                    onRegenerateTitle = onRegenerateTitle,
                    onDelete = onDelete,
                )
            }
        }
    }

    if (showEditTitleDialog) {
        Dialog(
            onDismissRequest = { showEditTitleDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF000000),
                    border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                    modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.chat_page_edit_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                        )
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            singleLine = true,
                            shape = AppShapes.InputField,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.White.copy(alpha = 0.35f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color.White.copy(alpha = 0.8f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
                                cursorColor = Color.White.copy(alpha = 0.9f),
                            )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DomChatGlassPillButton(
                                text = stringResource(R.string.chat_page_cancel),
                                onClick = { showEditTitleDialog = false },
                                modifier = Modifier.weight(1f).height(48.dp),
                                containerColor = Color(0xFF000000),
                                contentColor = Color.White,
                            )
                            DomChatGlassPillButton(
                                text = stringResource(R.string.chat_page_save),
                                onClick = {
                                    onEditTitle(conversation, editedTitle)
                                    showEditTitleDialog = false
                                },
                                modifier = Modifier.weight(1f).height(48.dp),
                                containerColor = Color(0xFF000000),
                                contentColor = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
    }

/**
 * In-tree long-press action menu for a conversation row.
 * Stays in the main window coordinate space, so [LocalAppBackdrop] sampling
 * works without DropdownMenu's isolated PopupWindow coordinate distortion.
 * Surface has no opaque fill — transparent glass with a specular rim only.
 */
@Composable
private fun ConversationActionMenu(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    conversation: Conversation,
    onPin: (Conversation) -> Unit,
    onEditTitle: () -> Unit,
    onRegenerateTitle: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
) {
    if (!visible) return

    val haptics = rememberPremiumHaptics()
    val settings = LocalSettings.current
    val isFrosted = settings.displaySetting.liquidGlassMode == LiquidGlassMode.SOFT_FROSTED
    val borderBrush = LiquidGlassTokens.SpecularBorderSubtle

    // SOFT_FROSTED -> Haze frosted glass; BALANCED_BLEND (and anything else)
    // -> liquid glass refraction surface. Both with the specular border.
    // Strictly no grey container: transparent fill only.
    val surfaceModifier: Modifier = if (isFrosted) {
        Modifier.frostedGlassSurface(
            shape = RoundedCornerShape(16.dp),
            borderWidth = 0.5.dp,
            specularBrush = borderBrush,
            fillColor = Color.Transparent,
        )
    } else {
        Modifier.liquidGlassSurface(
            shape = RoundedCornerShape(16.dp),
            borderWidth = 0.5.dp,
            specularBrush = borderBrush,
            fillColor = Color.Transparent,
            glassMode = LiquidGlassMode.BALANCED_BLEND,
        )
    }

    Column(
        modifier = Modifier
            .width(220.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .then(surfaceModifier),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ConversationMenuItem(
            icon = Icons.Rounded.PushPin,
            label = stringResource(if (conversation.isPinned) R.string.unpin_chat else R.string.pin_chat),
            onClick = {
                haptics.perform(HapticPattern.Pop)
                onPin(conversation)
                onDismissRequest()
            },
        )

        ConversationMenuItem(
            icon = Icons.Rounded.Edit,
            label = stringResource(R.string.chat_page_edit_title),
            onClick = {
                haptics.perform(HapticPattern.Pop)
                onEditTitle()
            },
        )

        ConversationMenuItem(
            icon = Icons.Rounded.Refresh,
            label = stringResource(R.string.chat_page_regenerate_title),
            onClick = {
                haptics.perform(HapticPattern.Tick)
                onRegenerateTitle(conversation)
                onDismissRequest()
            },
        )

        ConversationMenuItem(
            icon = Icons.Rounded.Delete,
            label = stringResource(R.string.chat_page_delete),
            destructive = true,
            onClick = {
                haptics.perform(HapticPattern.Error)
                onDelete(conversation)
                onDismissRequest()
            },
        )
    }
}

/**
 * Single glass pill row inside [ConversationActionMenu].
 * Rounded corners (16.dp), pressed state overlay, icon + label.
 */
@Composable
private fun ConversationMenuItem(
    icon: ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val color = if (destructive) MaterialTheme.colorScheme.error else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = color.copy(alpha = 0.9f),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
            ),
            color = color.copy(alpha = 0.95f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ConversationListPage() {
    val vm: ConversationListVM = koinViewModel()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val haptics = rememberPremiumHaptics()
    val homeBackdrop = rememberLayerBackdrop()

    val conversations = vm.conversations.collectAsLazyPagingItems()
    val conversationJobs by vm.conversationJobsSet.collectAsStateWithLifecycle(emptySet())
    val completedGenerationIds by vm.completedGenerationIds.collectAsStateWithLifecycle(emptySet())
    val recentlyRestoredIds by vm.recentlyRestoredIds.collectAsStateWithLifecycle(emptySet())
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var showConfirmDelete by remember { mutableStateOf(false) }
    var conversationToDelete by remember { mutableStateOf<Conversation?>(null) }

    val assistants by vm.assistants.collectAsStateWithLifecycle(emptyList())
    val assistantMap = remember(assistants) { assistants.associateBy { it.id } }

    val current = remember { Conversation.ofId(Uuid.random()) }

    Scaffold(
        topBar = {
            OneUITopAppBar(
                title = "Conversations",
                scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
                navigationIcon = { BackButton() },
            )
        },
        modifier = Modifier.fillMaxSize().layerBackdrop(homeBackdrop),
    ) { padding ->
        CompositionLocalProvider(LocalAppBackdrop provides homeBackdrop) {
            ConversationList(
            current = current,
            conversations = conversations,
            conversationJobs = conversationJobs,
            completedGenerationIds = completedGenerationIds,
            recentlyRestoredIds = recentlyRestoredIds,
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            isSearchExpanded = isSearchExpanded,
            onSearchExpandedChange = { isSearchExpanded = it },
            modifier = Modifier.fillMaxSize().padding(padding),
            onClick = { conversation ->
                haptics.perform(HapticPattern.Tick)
                navigateToChatPage(navController, conversation.id)
            },
            onDelete = { conversation ->
                conversationToDelete = conversation
                showConfirmDelete = true
            },
            onRegenerateTitle = { conversation ->
                vm.generateTitle(conversation, true)
            },
            onEditTitle = { conversation, title ->
                vm.updateConversationTitle(conversation, title)
            },
            onPin = { conversation ->
                vm.updatePinnedStatus(conversation)
            },
            getAssistant = { assistantMap[it] },
        )
        }
    }

    conversationToDelete?.let { conversation ->
        UniversalDestructiveConfirmDialog(
            visible = showConfirmDelete,
            title = "Delete conversation?",
            message = "This will permanently remove \"${conversation.title.ifBlank { "Unnamed conversation" }}\" from your history.",
            onConfirm = {
                vm.deleteConversation(conversation)
                conversationToDelete = null
                showConfirmDelete = false
            },
            onDismiss = {
                conversationToDelete = null
                showConfirmDelete = false
            }
        )
    }
}
