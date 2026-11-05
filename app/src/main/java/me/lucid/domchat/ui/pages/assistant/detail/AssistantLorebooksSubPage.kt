package me.lucid.domchat.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.rounded.Check
import androidx.compose.foundation.layout.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.R
import me.lucid.domchat.data.model.Assistant
import me.lucid.domchat.data.model.Lorebook
import me.lucid.domchat.ui.components.ui.EmptyStateCard
import me.lucid.domchat.ui.components.ui.HapticSwitch
import me.lucid.domchat.ui.context.LocalNavController
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LocalDarkMode
import me.lucid.domchat.Screen
import kotlin.uuid.Uuid

@Composable
fun AssistantLorebooksSubPage(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit,
    vm: me.lucid.domchat.ui.pages.assistant.detail.AssistantDetailVM
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val lorebooks = settings.lorebooks
    val navController = LocalNavController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (lorebooks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateCard(
                    icon = Icons.Rounded.Book,
                    title = stringResource(R.string.assistant_lorebooks_empty),
                    description = stringResource(R.string.assistant_lorebooks_empty_hint),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Description card header
            Surface(
                color = Color(0xFF141520),
                border = androidx.compose.foundation.BorderStroke(
                    0.5.dp,
                    me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
                ),
                shape = AppShapes.CardLarge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.assistant_lorebooks_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.assistant_lorebooks_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Search bar
            var lorebookSearch by remember { mutableStateOf("") }
            OutlinedTextField(
                value = lorebookSearch,
                onValueChange = { lorebookSearch = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search lorebooks...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (lorebookSearch.isNotEmpty()) {
                        IconButton(onClick = { lorebookSearch = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = AppShapes.InputField,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.White.copy(alpha = 0.30f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White.copy(alpha = 0.9f),
                )
            )

            Spacer(Modifier.height(12.dp))

            // Compact selectable chips (uses lorebook.id directly, Set<Uuid>)
            val filtered = lorebooks.filter { it.name.contains(lorebookSearch, ignoreCase = true) }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.forEach { lorebook ->
                    val isEnabled = assistant.enabledLorebookIds.contains(lorebook.id)
                    val selected = isEnabled
                    Surface(
                        onClick = {
                            haptics.perform(HapticPattern.Tick)
                            val newIds = if (selected) {
                                assistant.enabledLorebookIds - lorebook.id
                            } else {
                                assistant.enabledLorebookIds + lorebook.id
                            }
                            onUpdate(assistant.copy(enabledLorebookIds = newIds))
                        },
                        color = if (selected) Color(0xFF000000) else Color.Transparent,
                        border = BorderStroke(
                            if (selected) 0.5.dp else 0.5.dp,
                            if (selected) LiquidGlassTokens.SpecularBorderSubtle else Color.White.copy(alpha = 0.14f)
                        ),
                        shape = AppShapes.Tag,
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .height(36.dp)
                            .clickable { /* handled by onClick above */ }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lorebook.name.ifEmpty { stringResource(R.string.lorebooks_page_unnamed) },
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = if (selected) Color.White else Color.White.copy(alpha = 0.75f)
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Lorebook detail navigation kept (card tap opens detail)
@Composable
private fun LorebookSelectionCard(
    lorebook: Lorebook,
    isEnabled: Boolean,
    position: me.lucid.domchat.ui.components.ui.ItemPosition,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val cornerRadius = 28.dp
    val smallCorner = 8.dp
    val shape = when (position) {
        me.lucid.domchat.ui.components.ui.ItemPosition.ONLY -> RoundedCornerShape(cornerRadius)
        me.lucid.domchat.ui.components.ui.ItemPosition.FIRST -> RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomStart = smallCorner,
            bottomEnd = smallCorner
        )
        me.lucid.domchat.ui.components.ui.ItemPosition.MIDDLE -> RoundedCornerShape(smallCorner)
        me.lucid.domchat.ui.components.ui.ItemPosition.LAST -> RoundedCornerShape(
            topStart = smallCorner,
            topEnd = smallCorner,
            bottomStart = cornerRadius,
            bottomEnd = cornerRadius
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141520)),
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle
        ),
        shape = shape,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bookShape = when (position) {
                me.lucid.domchat.ui.components.ui.ItemPosition.ONLY -> RoundedCornerShape(
                    topStart = 16.dp, topEnd = 6.dp,
                    bottomStart = 16.dp, bottomEnd = 6.dp
                )
                me.lucid.domchat.ui.components.ui.ItemPosition.FIRST -> RoundedCornerShape(
                    topStart = 16.dp, topEnd = 6.dp,
                    bottomStart = 6.dp, bottomEnd = 6.dp
                )
                me.lucid.domchat.ui.components.ui.ItemPosition.MIDDLE -> RoundedCornerShape(6.dp)
                me.lucid.domchat.ui.components.ui.ItemPosition.LAST -> RoundedCornerShape(
                    topStart = 6.dp, topEnd = 6.dp,
                    bottomStart = 16.dp, bottomEnd = 6.dp
                )
            }
            Surface(
                shape = bookShape,
                color = Color(0xFF000000),
                modifier = Modifier.size(width = 50.dp, height = 70.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (val cover = lorebook.cover) {
                        is me.lucid.domchat.data.model.Avatar.Image -> {
                            coil3.compose.AsyncImage(
                                model = cover.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        is me.lucid.domchat.data.model.Avatar.Emoji -> {
                            Text(
                                text = cover.content,
                                fontSize = 24.sp
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Rounded.Book,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = lorebook.name.ifEmpty { stringResource(R.string.lorebooks_page_unnamed) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White
                )
                if (lorebook.description.isNotEmpty()) {
                    Text(
                        text = lorebook.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.lorebooks_page_entries_count, lorebook.entries.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            }

            HapticSwitch(
                checked = isEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
