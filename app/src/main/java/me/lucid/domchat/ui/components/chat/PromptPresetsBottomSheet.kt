package me.lucid.domchat.ui.components.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import me.lucid.domchat.ui.theme.LocalDarkMode
import java.util.Locale
import me.lucid.domchat.ui.components.ui.HapticSwitch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Psychology
import kotlin.math.roundToInt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import me.lucid.domchat.data.datastore.Settings
import me.lucid.domchat.data.datastore.SettingsStore
import me.lucid.domchat.data.model.Conversation
import me.lucid.domchat.data.model.LiquidGlassMode
import me.lucid.domchat.data.model.PresetInjectionPosition
import me.lucid.domchat.data.model.PromptPreset
import me.lucid.domchat.ui.context.LocalToaster
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberAmoledDarkMode
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.ui.theme.frostedGlassSurface
import me.lucid.domchat.ui.theme.liquidGlassSurface
import me.lucid.domchat.ui.components.ui.DomChatGlassChip
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.components.ui.DomChatSlider
import me.lucid.domchat.ui.components.ui.ToastType
import me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur
import me.lucid.domchat.ui.modifier.domChatBlurEffect
import me.lucid.domchat.ui.modifier.domChatCardContainerColor
import me.lucid.domchat.ui.modifier.domChatDialogContainerColor
import me.lucid.domchat.ui.modifier.domChatSheetContainerColor
import org.koin.compose.koinInject

/**
 * Single selectable position button for the 2x2 injection-position grid.
 */
@Composable
private fun PositionButton(
    label: String,
    position: PresetInjectionPosition,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        color = if (selected) selectedColor.copy(alpha = 0.20f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = if (selected) {
            BorderStroke(0.5.dp, selectedColor)
        } else {
            BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
        },
        modifier = modifier.then(Modifier.liquidGlassSurface(shape = RoundedCornerShape(12.dp))),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 12.5.sp
                ),
                color = if (selected) selectedColor else Color.White,
                maxLines = 1,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = selectedColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptPresetsBottomSheet(
    settings: Settings,
    conversation: Conversation? = null,
    onUpdateConversation: ((Conversation) -> Unit)? = null,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settingsStore: SettingsStore = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val haptics = rememberPremiumHaptics(enabled = settings.displaySetting.enableUIHaptics)
    val amoledMode by rememberAmoledDarkMode()
    val isDark = LocalDarkMode.current

    val borderBrush = LiquidGlassTokens.SpecularBorderSubtle
    val isFrosted = settings.displaySetting.liquidGlassMode == LiquidGlassMode.SOFT_FROSTED
    @Composable
    fun glassSurface(shape: Shape) = if (isFrosted) {
        Modifier.frostedGlassSurface(
            shape = shape,
            borderWidth = 0.5.dp,
            specularBrush = borderBrush,
            fillColor = Color.Transparent,
        )
    } else {
        Modifier.liquidGlassSurface(
            shape = shape,
            borderWidth = 0.5.dp,
            specularBrush = borderBrush,
            fillColor = Color.Transparent,
            glassMode = LiquidGlassMode.BALANCED_BLEND,
        )
    }

    val overscrollAbsorber = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                return available
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return available
            }
        }
    }

    val allPresets = settings.displaySetting.promptPresets

    // If conversation is provided and has specific overrides, use those; otherwise use global settings
    val effectiveEnabledIds = remember(conversation?.enabledPromptPresetIds, settings.displaySetting.globalEnabledPromptPresetIds) {
        conversation?.enabledPromptPresetIds ?: settings.displaySetting.globalEnabledPromptPresetIds
    }

    val isChatSpecific = conversation?.enabledPromptPresetIds != null

    var isEditingOrCreating by remember { mutableStateOf(false) }
    var presetSearchQuery by remember { mutableStateOf("") }
    var debouncedPresetQuery by remember { mutableStateOf("") }
    var editingTargetId by remember { mutableStateOf<String?>(null) }

    // 250ms debouncer for preset search
    LaunchedEffect(Unit) {
        snapshotFlow { presetSearchQuery }
            .debounce(250)
            .distinctUntilChanged()
            .collect { debouncedPresetQuery = it.trim().lowercase() }
    }

    val filteredPresets = allPresets.filter { preset ->
        debouncedPresetQuery.isEmpty() ||
            preset.name.lowercase().contains(debouncedPresetQuery) ||
            preset.description.lowercase().contains(debouncedPresetQuery) ||
            preset.content.lowercase().contains(debouncedPresetQuery)
    }
    var presetToDelete by remember { mutableStateOf<PromptPreset?>(null) }
    var formName by remember { mutableStateOf("") }
    var formDescription by remember { mutableStateOf("") }
    var formContent by remember { mutableStateOf("") }
    var formPosition by remember { mutableStateOf(PresetInjectionPosition.SYSTEM_PROMPT) }
    var formDepth by remember { mutableStateOf(0) }

    fun openCreate() {
        editingTargetId = null
        formName = ""
        formDescription = ""
        formContent = ""
        formPosition = PresetInjectionPosition.SYSTEM_PROMPT
        formDepth = 0
        isEditingOrCreating = true
    }

    fun openEdit(preset: PromptPreset) {
        editingTargetId = preset.id
        formName = preset.name
        formDescription = preset.description
        formContent = preset.content
        formPosition = preset.injectionPosition
        formDepth = preset.depth
        isEditingOrCreating = true
    }

    fun savePreset() {
        val targetName = formName.trim().ifBlank { "Custom Preset" }
        scope.launch {
            settingsStore.update { cur ->
                val display = cur.displaySetting
                val updatedList = if (editingTargetId == null) {
                    val newP = PromptPreset(
                        name = targetName,
                        content = formContent.trim(),
                        description = formDescription.trim(),
                        injectionPosition = formPosition,
                        depth = formDepth
                    )
                    display.promptPresets + newP
                } else {
                    display.promptPresets.map { p ->
                        if (p.id == editingTargetId) {
                            p.copy(
                                name = targetName,
                                content = formContent.trim(),
                                description = formDescription.trim(),
                                injectionPosition = formPosition,
                                depth = formDepth
                            )
                        } else p
                    }
                }
                cur.copy(displaySetting = display.copy(promptPresets = updatedList))
            }
            isEditingOrCreating = false
        }
    }

    fun deletePreset(id: String) {
        scope.launch {
            settingsStore.update { cur ->
                val display = cur.displaySetting
                val updatedList = display.promptPresets.filterNot { it.id == id }
                val updatedGlobal = display.globalEnabledPromptPresetIds - id
                cur.copy(
                    displaySetting = display.copy(
                        promptPresets = updatedList,
                        globalEnabledPromptPresetIds = updatedGlobal
                    )
                )
            }
            if (conversation != null && onUpdateConversation != null) {
                val curSet = conversation.enabledPromptPresetIds ?: settings.displaySetting.globalEnabledPromptPresetIds
                onUpdateConversation(conversation.copy(enabledPromptPresetIds = curSet - id))
            }
        }
    }

    fun togglePreset(id: String, enabled: Boolean) {
        haptics.perform(HapticPattern.Tick)
        val newSet = if (enabled) effectiveEnabledIds + id else effectiveEnabledIds - id

        if (conversation != null && onUpdateConversation != null) {
            // Per-chat mode
            onUpdateConversation(conversation.copy(enabledPromptPresetIds = newSet))
        } else {
            // Global mode
            scope.launch {
                settingsStore.update { cur ->
                    cur.copy(
                        displaySetting = cur.displaySetting.copy(globalEnabledPromptPresetIds = newSet)
                    )
                }
            }
        }
    }

    fun toggleAll(enableAll: Boolean) {
        haptics.perform(HapticPattern.Pop)
        val targetSet = if (enableAll) allPresets.map { it.id }.toSet() else emptySet()

        if (conversation != null && onUpdateConversation != null) {
            onUpdateConversation(conversation.copy(enabledPromptPresetIds = targetSet))
        } else {
            scope.launch {
                settingsStore.update { cur ->
                    cur.copy(
                        displaySetting = cur.displaySetting.copy(globalEnabledPromptPresetIds = targetSet)
                    )
                }
            }
        }
    }

    fun resetToGlobal() {
        haptics.perform(HapticPattern.Pop)
        if (conversation != null && onUpdateConversation != null) {
            onUpdateConversation(conversation.copy(enabledPromptPresetIds = null))
        }
    }

    fun movePreset(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in allPresets.indices || toIndex !in allPresets.indices) return
        val mutable = allPresets.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        scope.launch {
            settingsStore.update { cur ->
                cur.copy(displaySetting = cur.displaySetting.copy(promptPresets = mutable))
            }
        }
    }

    ApplyDialogWindowBlur()

    DomChatInWindowBottomSheet(
        visible = true,
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .nestedScroll(overscrollAbsorber),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                DomChatSheetHeader(
                    title = "Prompt Presets",
                    actions = {
                    if (!isEditingOrCreating) {
                        Surface(
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                openCreate()
                            },
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "New Preset",
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { isEditingOrCreating = false },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cancel",
                                tint = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                },
            )
        }

        item {
            // Search bar at top
            DomChatSearchBar(
                value = presetSearchQuery,
                onValueChange = { presetSearchQuery = it },
                placeholder = "Search presets...",
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        item {
            // Action pills: Enable All / Disable All / Reset to Default, plus the
            // active scope badge ("Using Chat" or "Using Global"), all glass pills.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                DomChatGlassPillButton(
                    text = "Enable All",
                    onClick = { toggleAll(true) }
                )
                DomChatGlassPillButton(
                    text = "Disable All",
                    onClick = { toggleAll(false) }
                )
                if (isChatSpecific) {
                    DomChatGlassPillButton(
                        text = "Reset to Default",
                        icon = Icons.Rounded.Refresh,
                        onClick = { resetToGlobal() }
                    )
                }
                if (conversation != null) {
                    DomChatGlassChip(
                        text = if (isChatSpecific) "Using Chat" else "Using Global",
                        modifier = Modifier.padding(horizontal = 6.dp),
                    )
                }
            }
        }

        item {
            // Inline Creator / Editor
            AnimatedContent(
                targetState = isEditingOrCreating,
                transitionSpec = {
                    (expandVertically(spring(dampingRatio = 0.75f, stiffness = 350f)) + fadeIn()) togetherWith
                        (shrinkVertically(spring(dampingRatio = 0.75f, stiffness = 350f)) + fadeOut())
                },
                contentAlignment = Alignment.TopCenter,
            ) { visible ->
                if (visible) {
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .then(glassSurface(RoundedCornerShape(20.dp)))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (editingTargetId == null) "Create Prompt Preset" else "Edit Preset",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = formName,
                            onValueChange = { formName = it },
                            label = { Text("Preset Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.White.copy(alpha = 0.30f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White.copy(alpha = 0.9f),
                                focusedLabelColor = Color.White.copy(alpha = 0.8f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = formDescription,
                            onValueChange = { formDescription = it },
                            label = { Text("Short Description / Tagline") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.White.copy(alpha = 0.30f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White.copy(alpha = 0.9f),
                                focusedLabelColor = Color.White.copy(alpha = 0.8f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = formContent,
                            onValueChange = { formContent = it },
                            label = { Text("Instructions (supports {{char}}, {{user}})") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.White.copy(alpha = 0.30f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White.copy(alpha = 0.9f),
                                focusedLabelColor = Color.White.copy(alpha = 0.8f),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.45f),
                            )
                        )

                        val formEstTokens = (formContent.length / 3.8).roundToInt()
                        Surface(
                            color = Color.Transparent,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).then(glassSurface(RoundedCornerShape(12.dp)))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Psychology,
                                        contentDescription = "Tokens",
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Estimated tokens",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Text(
                                    text = "~$formEstTokens · ${formContent.length} chars",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Injection Position",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PositionButton(
                                    label = "System",
                                    position = PresetInjectionPosition.SYSTEM_PROMPT,
                                    selected = formPosition == PresetInjectionPosition.SYSTEM_PROMPT,
                                    onClick = {
                                        haptics.perform(HapticPattern.Pop)
                                        formPosition = PresetInjectionPosition.SYSTEM_PROMPT
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                PositionButton(
                                    label = "Post-History",
                                    position = PresetInjectionPosition.POST_HISTORY_INSTRUCTIONS,
                                    selected = formPosition == PresetInjectionPosition.POST_HISTORY_INSTRUCTIONS,
                                    onClick = {
                                        haptics.perform(HapticPattern.Pop)
                                        formPosition = PresetInjectionPosition.POST_HISTORY_INSTRUCTIONS
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PositionButton(
                                    label = "Before User",
                                    position = PresetInjectionPosition.BEFORE_LAST_USER,
                                    selected = formPosition == PresetInjectionPosition.BEFORE_LAST_USER,
                                    onClick = {
                                        haptics.perform(HapticPattern.Pop)
                                        formPosition = PresetInjectionPosition.BEFORE_LAST_USER
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                PositionButton(
                                    label = "At Depth",
                                    position = PresetInjectionPosition.AT_DEPTH,
                                    selected = formPosition == PresetInjectionPosition.AT_DEPTH,
                                    onClick = {
                                        haptics.perform(HapticPattern.Pop)
                                        formPosition = PresetInjectionPosition.AT_DEPTH
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = formPosition == PresetInjectionPosition.AT_DEPTH,
                            enter = expandVertically(animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessLow
                            )) + fadeIn(animationSpec = tween(400)),
                            exit = shrinkVertically(animationSpec = spring(
                                dampingRatio = 0.8f,
                                stiffness = Spring.StiffnessLow
                            )) + fadeOut(animationSpec = tween(300))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Depth (Messages from end)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$formDepth",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DomChatSlider(
                                    value = formDepth.toFloat(),
                                    onValueChange = { formDepth = it.toInt() },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            DomChatGlassPillButton(
                                text = "Cancel",
                                onClick = { isEditingOrCreating = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            DomChatGlassPillButton(
                                text = "Save Preset",
                                accent = true,
                                onClick = {
                                    savePreset()
                                }
                            )
                        }
                    }
                }
            } // end if (visible)
        }
    } // end editor item

    itemsIndexed(filteredPresets, key = { _, it: PromptPreset -> it.id }) { index: Int, preset: PromptPreset ->
                    val isEnabled = effectiveEnabledIds.contains(preset.id)
                    val snippet = preset.description.ifBlank { preset.content.take(56) }
                    val estTokens = (preset.content.length / 3.8).roundToInt()
                    val positionLabel = when (preset.injectionPosition) {
                        PresetInjectionPosition.SYSTEM_PROMPT -> "System"
                        PresetInjectionPosition.POST_HISTORY_INSTRUCTIONS -> "Post-History"
                        PresetInjectionPosition.BEFORE_LAST_USER -> "Before User"
                        PresetInjectionPosition.AT_DEPTH -> "At Depth ${preset.depth}"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 82.dp, max = 110.dp)
                            // frostedGlassSurface in SOFT_FROSTED mode, stable liquidGlassSurface
                            // in BALANCED_BLEND mode — neither freezes nor drops out while the
                            // LazyColumn scrolls (no per-card AGSL lens pass during scroll).
                            .then(glassSurface(RoundedCornerShape(16.dp))),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    // Token estimate pill
                                    Surface(
                                        color = Color.White.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp),
                                    ) {
                                        Text(
                                            text = "~$estTokens tokens",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                            color = Color.White.copy(alpha = 0.8f),
                                        )
                                    }
                                    // Injection position tag
                                    Text(
                                        text = positionLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                        color = Color.White.copy(alpha = 0.6f),
                                        maxLines = 1,
                                    )
                                }
                                Text(
                                    text = snippet,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HapticSwitch(
                                    checked = isEnabled,
                                    onCheckedChange = { enabled ->
                                        haptics.perform(HapticPattern.Tick)
                                        togglePreset(preset.id, enabled)
                                        toaster.show(
                                            message = if (enabled) "Enabled: ${preset.name}" else "Disabled: ${preset.name}",
                                            type = ToastType.Info,
                                        )
                                    },
                                )

                                IconButton(
                                    onClick = { openEdit(preset) },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }

                                IconButton(
                                    onClick = { presetToDelete = preset },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        }
                    }
        }

        item {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    // Preset delete confirmation dialog (glassy black, destructive)
    presetToDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { presetToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .widthIn(max = 320.dp)
                .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, AppShapes.Dialog),
            shape = AppShapes.Dialog,
            containerColor = Color(0xFF000000),
            title = {
                ApplyDialogWindowBlur()
                Text(
                    text = "Delete ${preset.name}?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This preset will be permanently removed from your presets list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.perform(HapticPattern.Pop)
                        deletePreset(preset.id)
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        disabledContainerColor = Color.Transparent.copy(alpha = 0.2f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Delete", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { presetToDelete = null }) {
                    Text(text = "Cancel", color = Color.White.copy(alpha = 0.85f))
                }
            }
        )
    }
}
}
