package me.lucid.domchat.ui.pages.setting

import android.os.Build
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import me.lucid.domchat.ui.components.ui.DomChatSlider
import me.lucid.domchat.ui.components.ui.DomChatGlassPillButton
import me.lucid.domchat.ui.components.ui.HapticSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.lucid.domchat.data.datastore.AnimationSpeedLevel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.R
import me.lucid.domchat.data.datastore.DisplaySetting
import me.lucid.domchat.data.datastore.InterfaceStyle
import me.lucid.domchat.data.ai.models.ModelCatalogSource
import me.lucid.domchat.data.ai.models.ModelCatalogStatus
import me.lucid.domchat.ui.components.nav.BackButton
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
import kotlin.ranges.ClosedFloatingPointRange
import me.lucid.domchat.data.model.LiquidGlassMode
import me.lucid.domchat.data.model.BackgroundBlurMode
import me.lucid.domchat.ui.components.nav.OneUITopAppBar
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.components.ui.Select
import me.lucid.domchat.ui.components.ui.ToastType
import me.lucid.domchat.ui.components.ui.permission.PermissionManager
import me.lucid.domchat.ui.components.ui.permission.PermissionNotification
import me.lucid.domchat.ui.components.ui.permission.rememberPermissionState
import me.lucid.domchat.ui.context.LocalToaster
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberAmoledDarkMode
import me.lucid.domchat.ui.hooks.rememberColorMode
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.hooks.rememberSharedPreferenceBoolean
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.HorizontalDivider
import me.lucid.domchat.ui.theme.LocalDarkMode
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.ui.pages.setting.components.SettingsGroup
import me.lucid.domchat.ui.pages.setting.components.SettingGroupItem
import me.lucid.domchat.ui.pages.setting.components.SettingGroupInputItem
import me.lucid.domchat.ui.theme.ColorMode
import me.lucid.domchat.utils.plus
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.widthIn
import me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur

@Composable
fun SettingDisplayPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val modelCatalogStatus by vm.modelCatalogStatus.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(
            settings.copy(
                displaySetting = setting
            )
        )
    }
    var uiIsGlass by remember(settings.displaySetting) {
        mutableStateOf(
            settings.displaySetting.liquidGlassMode != me.lucid.domchat.data.model.LiquidGlassMode.SOFT_FROSTED
        )
    }
    var bubbleIsGlass by remember(settings.displaySetting) {
        mutableStateOf(
            settings.displaySetting.bubbleInterfaceStyle == me.lucid.domchat.data.datastore.InterfaceStyle.GLASS
        )
    }
    var linkedStyles by remember(settings.displaySetting) {
        mutableStateOf(settings.displaySetting.linkUiAndBubbleStyle)
    }

    // Keep linked styles consistent with persisted settings (e.g. after a catalog merge).
    if (linkedStyles && bubbleIsGlass != uiIsGlass) {
        bubbleIsGlass = uiIsGlass
        updateDisplaySetting(
            settings.displaySetting.copy(
                liquidGlassMode = if (uiIsGlass) me.lucid.domchat.data.model.LiquidGlassMode.BALANCED_BLEND
                else me.lucid.domchat.data.model.LiquidGlassMode.SOFT_FROSTED,
                bubbleInterfaceStyle = if (uiIsGlass) InterfaceStyle.GLASS
                else InterfaceStyle.FROSTED,
            )
        )
    }

    var amoledDarkMode by rememberAmoledDarkMode()
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val haptics = rememberPremiumHaptics(enabled = settings.displaySetting.enableUIHaptics)

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val lazyListState = rememberLazyListState()

    val permissionState = rememberPermissionState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setOf(
            PermissionNotification
        ) else emptySet(),
    )
    PermissionManager(permissionState = permissionState)

    Scaffold(
        topBar = {
            OneUITopAppBar(
                title = stringResource(R.string.setting_display_page_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    BackButton()
                }
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Chat Message Styles with Visual Representation
            item {
                SettingsGroup(
                    title = "Chat Message Layout & Roleplay Style"
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        me.lucid.domchat.ui.components.chat.ChatMessageStyleSelector(
                            selectedStyle = settings.displaySetting.chatMessageStyle,
                            onStyleSelected = { newStyle ->
                                vm.updateSettings(
                                    settings.copy(
                                        displaySetting = settings.displaySetting.copy(chatMessageStyle = newStyle)
                                    )
                                )
                            }
                        )
                    }
                }
            }

            // Theme & Interface Customization
            item {
                val isDark = LocalDarkMode.current
                
                SettingsGroup(
                    title = stringResource(R.string.setting_page_theme_setting)
                ) {
                    SettingGroupItem(
                        title = "Themes",
                        subtitle = "OLED Liquid Glass",
                        trailing = {
                            Surface(
                                color = Color(0xFF000000),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.5.dp, me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle)
                            ) {
                                Text(
                                    text = "Coming Soon",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isDark) Color.White else Color(0xFFFFFFFF),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    )
                }
            }

            // Global Wallpaper & Atmosphere
            item {
                val globalBg = settings.displaySetting.globalBackground
                val globalDim = settings.displaySetting.globalBackgroundDim
                val globalBlur = settings.displaySetting.globalBackgroundBlur
                val globalWallpaperPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        val localPath = me.lucid.domchat.utils.ImageUtils.copyImageToAppStorage(
                            context = context,
                            sourceUri = uri,
                            subDir = "wallpapers",
                            fileName = "wp_global_${System.currentTimeMillis()}.jpg"
                        )
                        if (localPath != null) {
                            haptics.perform(HapticPattern.Pop)
                            updateDisplaySetting(
                                settings.displaySetting.copy(globalBackground = localPath)
                            )
                        }
                    }
                }

                SettingsGroup(
                    title = "Wallpaper & Atmosphere"
                ) {
                    SettingGroupItem(
                        title = "Global Wallpaper",
                        subtitle = if (globalBg.isNullOrBlank()) "No wallpaper set (pure OLED canvas)" else "Custom wallpaper active across all chats",
                        trailing = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!globalBg.isNullOrBlank()) {
                                    IconButton(
                                        onClick = {
                                            haptics.perform(HapticPattern.Pop)
                                            updateDisplaySetting(
                                                settings.displaySetting.copy(globalBackground = null)
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Remove Wallpaper",
                                            tint = Color(0xFFFF453A)
                                        )
                                    }
                                }
                                DomChatGlassPillButton(
                                    text = if (globalBg.isNullOrBlank()) "Choose" else "Change",
                                    onClick = {
                                        globalWallpaperPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                )
                            }
                        }
                    )

                    // Live Wallpaper Preview Card under Global Wallpaper
                    if (!globalBg.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
.clip(AppShapes.CardMedium)
                                    .border(
                                        BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                        AppShapes.CardMedium
                                    )
                                    .background(Color(0xFF141520))
                            ) {
                                AsyncImage(
                                    model = globalBg,
                                    contentDescription = "Global wallpaper preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                                // Live dim overlay applied to preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Color.Black.copy(
                                                alpha = globalDim.coerceIn(0f, 0.95f)
                                            )
                                        )
                                )
                                // Live blur overlay (bottom scrim) applied to preview
                                if (globalBlur > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(
                                                            alpha = (globalBlur / 200f).coerceIn(0f, 0.5f)
                                                        )
                                                    )
                                                )
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Progressive Disclosure: Dimming and Blur sliders only show when wallpaper is set
                    AnimatedVisibility(visible = !globalBg.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            var localDim by remember(globalDim) { mutableFloatStateOf(globalDim) }
                            var localBlur by remember(globalBlur) { mutableFloatStateOf(globalBlur) }
                            var localFadeHeight by remember(settings.displaySetting.globalBackgroundBlurHeightFraction) {
                                mutableFloatStateOf(settings.displaySetting.globalBackgroundBlurHeightFraction)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Wallpaper Dimming", style = MaterialTheme.typography.bodyMedium)
                                Text("${(localDim * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localDim,
                                onValueChange = {
                                    if ((it * 100).toInt() != (localDim * 100).toInt()) {
                                        haptics.perform(HapticPattern.Tick)
                                    }
                                    localDim = it
                                },
                                onValueChangeFinished = {
                                    updateDisplaySetting(settings.displaySetting.copy(globalBackgroundDim = localDim))
                                },
                                valueRange = 0f..0.95f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Wallpaper Blur", style = MaterialTheme.typography.bodyMedium)
                                Text("${localBlur.roundToInt()} dp", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localBlur,
                                onValueChange = {
                                    if (it.roundToInt() != localBlur.roundToInt()) {
                                        haptics.perform(HapticPattern.Tick)
                                    }
                                    localBlur = it
                                },
                                onValueChangeFinished = {
                                    updateDisplaySetting(settings.displaySetting.copy(globalBackgroundBlur = localBlur))
                                },
                                valueRange = 0f..100f
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Blur Style", style = MaterialTheme.typography.bodyMedium)
                                Select(
                                    options = BackgroundBlurMode.entries,
                                    selectedOption = settings.displaySetting.backgroundBlurMode,
                                    onOptionSelected = {
                                        haptics.perform(HapticPattern.Tick)
                                        updateDisplaySetting(settings.displaySetting.copy(backgroundBlurMode = it))
                                    },
                                    optionToString = { mode ->
                                        when (mode) {
                                            BackgroundBlurMode.FULL -> "Full Image"
                                            BackgroundBlurMode.BOTTOM_FADE -> "Bottom Fade"
                                        }
                                    },
                                    modifier = Modifier.wrapContentWidth()
                                )
                            }

                            if (settings.displaySetting.backgroundBlurMode == BackgroundBlurMode.BOTTOM_FADE) {
                                AnimatedContent<BackgroundBlurMode>(targetState = settings.displaySetting.backgroundBlurMode) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Fade Blur Height", style = MaterialTheme.typography.bodyMedium)
                                        Text("${(localFadeHeight * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                    }
                                    DomChatSlider(
                                        value = localFadeHeight,
                                        onValueChange = {
                                            if ((it * 100).toInt() != (localFadeHeight * 100).toInt()) {
                                                haptics.perform(HapticPattern.Tick)
                                            }
                                            localFadeHeight = it
                                        },
                                        onValueChangeFinished = {
                                            updateDisplaySetting(settings.displaySetting.copy(globalBackgroundBlurHeightFraction = localFadeHeight))
                                        },
                                        valueRange = 0.10f..1.0f
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Animation Speed
            item {
                SettingsGroup(
                    title = "Animation Speed"
                ) {
                    SettingGroupItem(
                        title = "Animation Speed",
                        subtitle = "${settings.displaySetting.animationSpeed.title} (${settings.displaySetting.animationSpeed.subtitle})",
                        trailing = {
                            Select(
                                options = AnimationSpeedLevel.entries,
                                selectedOption = settings.displaySetting.animationSpeed,
                                onOptionSelected = {
                                    haptics.perform(HapticPattern.Tick)
                                    updateDisplaySetting(settings.displaySetting.copy(animationSpeed = it))
                                },
                                optionToString = { it.title },
                                modifier = Modifier.wrapContentWidth()
                            )
                        }
                    )
                }
            }

            // =====================================================================
            // Section 1: UI Appearance
            // =====================================================================
            item {
                SettingsGroup(
                    title = "UI Appearance"
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Glass mode segmented tabs: Liquid / Frosted, each showing a "glass" subtext.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GlassModeTab(
                                selected = uiIsGlass,
                                label = "Liquid",
                                subtitle = "glass",
                                onClick = {
                                    haptics.perform(HapticPattern.Pop)
                                    uiIsGlass = true
                                    updateDisplaySetting(
                                        settings.displaySetting.copy(
                                            liquidGlassMode = me.lucid.domchat.data.model.LiquidGlassMode.BALANCED_BLEND
                                        )
                                    )
                                    if (linkedStyles) {
                                        bubbleIsGlass = true
                                        updateDisplaySetting(
                                            settings.displaySetting.copy(
                                                bubbleInterfaceStyle = me.lucid.domchat.data.datastore.InterfaceStyle.GLASS
                                            )
                                        )
                                    }
                                },
                            )
                            GlassModeTab(
                                selected = !uiIsGlass,
                                label = "Frosted",
                                subtitle = "glass",
                                onClick = {
                                    haptics.perform(HapticPattern.Pop)
                                    uiIsGlass = false
                                    updateDisplaySetting(
                                        settings.displaySetting.copy(
                                            liquidGlassMode = me.lucid.domchat.data.model.LiquidGlassMode.SOFT_FROSTED
                                        )
                                    )
                                    if (linkedStyles) {
                                        bubbleIsGlass = false
                                        updateDisplaySetting(
                                            settings.displaySetting.copy(
                                                bubbleInterfaceStyle = me.lucid.domchat.data.datastore.InterfaceStyle.FROSTED
                                            )
                                        )
                                    }
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var uiAdvanced by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable(onClick = { uiAdvanced = !uiAdvanced }),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Advanced Appearance",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (uiAdvanced) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = uiAdvanced,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                var localGlassAlpha by remember(settings.displaySetting.glassAlpha) {
                                    mutableFloatStateOf(settings.displaySetting.glassAlpha)
                                }
                                glassSliderRow(
                                    label = "UI Glass Opacity",
                                    valueText = "${(localGlassAlpha * 100).roundToInt()}%",
                                    value = localGlassAlpha,
                                    onValueChange = {
                                        val rounded = (it * 100f).roundToInt() / 100f
                                        if (rounded != localGlassAlpha) {
                                            haptics.perform(HapticPattern.Tick)
                                            localGlassAlpha = rounded
                                        }
                                    },
                                    onFinished = {
                                        updateDisplaySetting(settings.displaySetting.copy(glassAlpha = localGlassAlpha))
                                    },
                                )

                                var localGlassBlur by remember(settings.displaySetting.glassBlur) {
                                    mutableFloatStateOf(settings.displaySetting.glassBlur)
                                }
                                glassSliderRow(
                                    label = "UI Glass Blur Radius",
                                    valueText = "${localGlassBlur.roundToInt()} dp",
                                    value = localGlassBlur,
                                    onValueChange = {
                                        val rounded = it.roundToInt().toFloat()
                                        if (rounded != localGlassBlur) {
                                            haptics.perform(HapticPattern.Tick)
                                            localGlassBlur = rounded
                                        }
                                    },
                                    onFinished = {
                                        updateDisplaySetting(settings.displaySetting.copy(glassBlur = localGlassBlur))
                                    },
                                    valueRange = 0f..100f,
                                )

                                var localDepthScale by remember(settings.displaySetting.glassDepthScale) {
                                    mutableFloatStateOf(settings.displaySetting.glassDepthScale)
                                }
                                if (uiIsGlass) {
                                glassSliderRow(
                                    label = "UI Glass Depth / Refraction",
                                    valueText = "${localDepthScale.roundToInt()}x",
                                    value = localDepthScale,
                                    onValueChange = { localDepthScale = it },
                                    onFinished = {
                                        val rounded = (localDepthScale * 10f).roundToInt() / 10f
                                        localDepthScale = rounded
                                        updateDisplaySetting(settings.displaySetting.copy(glassDepthScale = localDepthScale))
                                    },
                                    valueRange = 0f..2.5f,
                                    steps = 25,
                                )
                                }

                                var localGlassSpecular by remember(settings.displaySetting.glassSpecularIntensity) {
                                    mutableFloatStateOf(settings.displaySetting.glassSpecularIntensity)
                                }
                                if (uiIsGlass) {
                                glassSliderRow(
                                    label = "UI Glass Specular Highlight",
                                    valueText = "${(localGlassSpecular * 100).roundToInt()}%",
                                    value = localGlassSpecular,
                                    onValueChange = { localGlassSpecular = it },
                                    onFinished = {
                                        val rounded = (localGlassSpecular * 100f).roundToInt() / 100f
                                        localGlassSpecular = rounded
                                        updateDisplaySetting(settings.displaySetting.copy(glassSpecularIntensity = localGlassSpecular))
                                    },
                                    valueRange = 0f..1.0f,
                                    steps = 100,
                                )
                                }

                                var localTint by remember(settings.displaySetting.glassTintColor) {
                                    mutableStateOf(settings.displaySetting.glassTintColor)
                                }
                                var uiTintPickerOpen by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Glass Tint Color",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Tint wash over glass surfaces",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.wrapContentWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(localTint))
                                        )
                                        Text(
                                            text = "Pick color",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        IconButton(
                                            onClick = { uiTintPickerOpen = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                if (uiTintPickerOpen) {
                                    TintColorPickerDialog(
                                        open = true,
                                        onDismiss = { uiTintPickerOpen = false },
                                        initialColor = localTint,
                                        onPick = { picked ->
                                            uiTintPickerOpen = false
                                            localTint = picked
                                            updateDisplaySetting(
                                                settings.displaySetting.copy(glassTintColor = picked)
                                            )
                                        },
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color.White.copy(alpha = 0.08f)
                                )

                                var localBarGradientDensity by remember(settings.displaySetting.barGradientDensity) {
                                    mutableFloatStateOf(settings.displaySetting.barGradientDensity)
                                }
                                glassSliderRow(
                                    label = "Bar Gradient Density",
                                    valueText = "${(localBarGradientDensity * 100).roundToInt()}%",
                                    value = localBarGradientDensity,
                                    onValueChange = { localBarGradientDensity = it },
                                    onFinished = {
                                        val rounded = (localBarGradientDensity * 100f).roundToInt() / 100f
                                        localBarGradientDensity = rounded
                                        updateDisplaySetting(settings.displaySetting.copy(barGradientDensity = localBarGradientDensity))
                                    },
                                    valueRange = 0f..1.0f,
                                    steps = 100,
                                )

                                var localBarGradientBlur by remember(settings.displaySetting.barGradientBlur) {
                                    mutableFloatStateOf(settings.displaySetting.barGradientBlur)
                                }
                                glassSliderRow(
                                    label = "Bar Gradient Blur",
                                    valueText = "${localBarGradientBlur.roundToInt()} dp",
                                    value = localBarGradientBlur,
                                    onValueChange = { localBarGradientBlur = it },
                                    onFinished = {
                                        val rounded = (localBarGradientBlur * 100f).roundToInt() / 100f
                                        localBarGradientBlur = rounded
                                        updateDisplaySetting(settings.displaySetting.copy(barGradientBlur = localBarGradientBlur))
                                    },
                                    valueRange = 0f..60f,
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = Color.White.copy(alpha = 0.08f)
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Link Chrome to Glass",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "Side menu and popovers follow glass opacity, blur, and tone.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                    HapticSwitch(
                                        checked = settings.displaySetting.linkChromeToGlass,
                                        onCheckedChange = {
                                            updateDisplaySetting(settings.displaySetting.copy(linkChromeToGlass = it))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Link UI & Bubbles switch (placed between the two appearance sections)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Link UI & Bubbles", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "When on, changing UI or Bubble glass style applies to both.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                    HapticSwitch(
                        checked = linkedStyles,
                        onCheckedChange = {
                            linkedStyles = it
                            updateDisplaySetting(settings.displaySetting.copy(linkUiAndBubbleStyle = it))
                        }
                    )
                }
            }

            // =====================================================================
            // Section 2: Chat Bubbles Appearance
            // =====================================================================
            item {
                SettingsGroup(
                    title = "Chat Bubbles Appearance"
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Glass mode segmented tabs: Liquid / Frosted, each showing a "glass" subtext.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GlassModeTab(
                                selected = bubbleIsGlass,
                                label = "Liquid",
                                subtitle = "glass",
                                onClick = {
                                    haptics.perform(HapticPattern.Pop)
                                    if (linkedStyles) {
                                        uiIsGlass = true
                                        bubbleIsGlass = true
                                        updateDisplaySetting(
                                            settings.displaySetting.copy(
                                                liquidGlassMode = me.lucid.domchat.data.model.LiquidGlassMode.BALANCED_BLEND,
                                                bubbleInterfaceStyle = me.lucid.domchat.data.datastore.InterfaceStyle.GLASS,
                                            )
                                        )
                                    } else {
                                        bubbleIsGlass = true
                                        updateDisplaySetting(
                                            settings.displaySetting.copy(
                                                bubbleInterfaceStyle = me.lucid.domchat.data.datastore.InterfaceStyle.GLASS
                                            )
                                        )
                                    }
                                },
                            )
                            GlassModeTab(
                                selected = !bubbleIsGlass,
                                label = "Frosted",
                                subtitle = "glass",
                                onClick = {
                                    haptics.perform(HapticPattern.Pop)
                                    if (linkedStyles) {
                                        uiIsGlass = false
                                        bubbleIsGlass = false
                                        updateDisplaySetting(
                                            settings.displaySetting.copy(
                                                liquidGlassMode = me.lucid.domchat.data.model.LiquidGlassMode.SOFT_FROSTED,
                                                bubbleInterfaceStyle = me.lucid.domchat.data.datastore.InterfaceStyle.FROSTED,
                                            )
                                        )
                                    } else {
                                        bubbleIsGlass = false
                                        updateDisplaySetting(
                                            settings.displaySetting.copy(
                                                bubbleInterfaceStyle = me.lucid.domchat.data.datastore.InterfaceStyle.FROSTED
                                            )
                                        )
                                    }
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var bubbleAdvanced by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .clickable(onClick = { bubbleAdvanced = !bubbleAdvanced }),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Advanced Appearance",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (bubbleAdvanced) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = bubbleAdvanced,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                var localBubbleAlpha by remember(settings.displaySetting.chatBubbleAlpha) {
                                    mutableFloatStateOf(settings.displaySetting.chatBubbleAlpha)
                                }
                                glassSliderRow(
                                    label = "Bubble Opacity",
                                    valueText = "${(localBubbleAlpha * 100).roundToInt()}%",
                                    value = localBubbleAlpha,
                                    onValueChange = {
                                        val rounded = (it * 100f).roundToInt() / 100f
                                        if (rounded != localBubbleAlpha) {
                                            haptics.perform(HapticPattern.Tick)
                                            localBubbleAlpha = rounded
                                        }
                                    },
                                    onFinished = {
                                        updateDisplaySetting(settings.displaySetting.copy(chatBubbleAlpha = localBubbleAlpha))
                                    },
                                )

                                var localBubbleBlur by remember(settings.displaySetting.chatBubbleBlur) {
                                    mutableFloatStateOf(settings.displaySetting.chatBubbleBlur)
                                }
                                glassSliderRow(
                                    label = "Bubble Blur Radius",
                                    valueText = "${localBubbleBlur.roundToInt()} dp",
                                    value = localBubbleBlur,
                                    onValueChange = {
                                        val rounded = it.roundToInt().toFloat()
                                        if (rounded != localBubbleBlur) {
                                            haptics.perform(HapticPattern.Tick)
                                            localBubbleBlur = rounded
                                        }
                                    },
                                    onFinished = {
                                        updateDisplaySetting(settings.displaySetting.copy(chatBubbleBlur = localBubbleBlur))
                                    },
                                    valueRange = 0f..100f,
                                )

                                var localBubbleTone by remember(settings.displaySetting.chatBubbleTone) {
                                    mutableFloatStateOf(settings.displaySetting.chatBubbleTone)
                                }
                                glassSliderRow(
                                    label = "Bubble Tone / Luminance",
                                    valueText = "${(localBubbleTone * 100).roundToInt()}%",
                                    value = localBubbleTone,
                                    onValueChange = {
                                        val rounded = (it * 100f).roundToInt() / 100f
                                        if (rounded != localBubbleTone) {
                                            haptics.perform(HapticPattern.Tick)
                                            localBubbleTone = rounded
                                        }
                                    },
                                    onFinished = {
                                        updateDisplaySetting(settings.displaySetting.copy(chatBubbleTone = localBubbleTone))
                                    },
                                    valueRange = 0f..1.0f,
                                    steps = 100,
                                )

                                var localBubbleDepth by remember(settings.displaySetting.bubbleDepthScale) {
                                    mutableFloatStateOf(settings.displaySetting.bubbleDepthScale)
                                }
                                if (bubbleIsGlass) {
                                glassSliderRow(
                                    label = "Bubble Depth / Refraction",
                                    valueText = "${localBubbleDepth.roundToInt()}x",
                                    value = localBubbleDepth,
                                    onValueChange = { localBubbleDepth = it },
                                    onFinished = {
                                        val rounded = (localBubbleDepth * 10f).roundToInt() / 10f
                                        localBubbleDepth = rounded
                                        updateDisplaySetting(settings.displaySetting.copy(bubbleDepthScale = localBubbleDepth))
                                    },
                                    valueRange = 0f..2.5f,
                                    steps = 25,
                                )
                                }

                                var localBubbleSpecular by remember(settings.displaySetting.bubbleSpecularIntensity) {
                                    mutableFloatStateOf(settings.displaySetting.bubbleSpecularIntensity)
                                }
                                if (bubbleIsGlass) {
                                glassSliderRow(
                                    label = "Bubble Specular Intensity",
                                    valueText = "${(localBubbleSpecular * 100).roundToInt()}%",
                                    value = localBubbleSpecular,
                                    onValueChange = { localBubbleSpecular = it },
                                    onFinished = {
                                        val rounded = (localBubbleSpecular * 100f).roundToInt() / 100f
                                        localBubbleSpecular = rounded
                                        updateDisplaySetting(settings.displaySetting.copy(bubbleSpecularIntensity = localBubbleSpecular))
                                    },
                                    valueRange = 0f..1.0f,
                                    steps = 100,
                                )
                                }

                                var localBubbleTint by remember(settings.displaySetting.chatBubbleTintHue) {
                                    mutableStateOf(settings.displaySetting.chatBubbleTintHue)
                                }
                                var bubbleTintPickerOpen by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Bubble Tint Color",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Tint wash over chat bubbles",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.wrapContentWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val tintColor = Color.hsv(localBubbleTint.coerceIn(0f, 360f), 0.75f, 1.0f)
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(tintColor)
                                        )
                                        Text(
                                            text = "Pick color",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        IconButton(
                                            onClick = { bubbleTintPickerOpen = true }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                if (bubbleTintPickerOpen) {
                                    TintColorPickerDialog(
                                        open = true,
                                        onDismiss = { bubbleTintPickerOpen = false },
                                        initialColor = Color.hsv(localBubbleTint.coerceIn(0f, 360f), 0.75f, 1.0f).toArgb().toLong(),
                                        onPick = { picked ->
                                            bubbleTintPickerOpen = false
                                            val hsv = FloatArray(3)
                                            android.graphics.Color.colorToHSV(picked.toInt(), hsv)
                                            localBubbleTint = hsv[0]
                                            updateDisplaySetting(
                                                settings.displaySetting.copy(
                                                    chatBubbleTintHue = localBubbleTint,
                                                    chatBubbleTintAmount = 1.0f
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }



            // Basic Settings
            item {
                var createNewConversationOnStart by rememberSharedPreferenceBoolean(
                    "create_new_conversation_on_start",
                    true
                )
                SettingsGroup(
                    title = stringResource(R.string.setting_page_basic_settings)
                ) {
                    SettingGroupItem(
                        title = stringResource(R.string.setting_display_page_create_new_conversation_on_start_title),
                        subtitle = stringResource(R.string.setting_display_page_create_new_conversation_on_start_desc),
                        trailing = {
                            HapticSwitch(
                                checked = createNewConversationOnStart,
                                onCheckedChange = {
                                    createNewConversationOnStart = it
                                }
                            )
                        }
                    )
                    SettingGroupItem(
                        title = stringResource(R.string.setting_display_page_notification_message_generated),
                        subtitle = stringResource(R.string.setting_display_page_notification_message_generated_desc),
                        trailing = {
                            HapticSwitch(
                                checked = displaySetting.enableNotificationOnMessageGeneration,
                                onCheckedChange = {
                                    if (it && !permissionState.allPermissionsGranted) {
                                        permissionState.requestPermissions()
                                    }
                                    updateDisplaySetting(displaySetting.copy(enableNotificationOnMessageGeneration = it))
                                }
                            )
                        }
                    )
                    SettingGroupItem(
                        title = stringResource(R.string.setting_display_check_updates_title),
                        subtitle = stringResource(R.string.setting_display_check_updates_desc),
                        trailing = {
                            HapticSwitch(
                                checked = displaySetting.checkForUpdates,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(checkForUpdates = it))
                                }
                            )
                        }
                    )
                }
            }

//            item {
//                ListItem(
//                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
//                    headlineContent = {
//                        Text(stringResource(R.string.setting_display_page_developer_mode))
//                    },
//                    supportingContent = {
//                        Text(stringResource(R.string.setting_display_page_developer_mode_desc))
//                    },
//                    trailingContent = {
//                        HapticSwitch(
//                            checked = settings.developerMode,
//                            onCheckedChange = {
//                                vm.updateSettings(settings.copy(developerMode = it))
//                            }
//                        )
//                    },
//                )
//            }

        }
    }
}

/**
 * Segmented tab for Liquid / Frosted glass mode with a small centered "glass" subtext.
 * 48dp minimum touch target; never cramps horizontally on mobile.
 */
@Composable
private fun RowScope.GlassModeTab(
    selected: Boolean,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val fill = if (selected) Color(0xFF000000) else Color.White.copy(alpha = 0.04f)
    val border: BorderStroke = if (selected) {
        BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
    } else {
        BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
    }
    Surface(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = AppShapes.CardMedium,
        color = fill,
        border = border,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) Color.White else Color.White.copy(alpha = 0.66f),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = if (selected) 0.85f else 0.45f),
            )
        }
    }
}

/**
 * Label + value row paired with [DomChatSlider] for glass appearance controls.
 */
@Composable
private fun glassSliderRow(
    label: String,
    valueText: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(valueText, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
    DomChatSlider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onFinished,
        valueRange = valueRange,
        steps = steps,
    )
}

/**
 * HSV / Hex color picker dialog for glass and bubble tint washes
 * (Convx-style color wheel replacement: sliders + hex input + preview swatch).
 */
@Composable
private fun TintColorPickerDialog(
    open: Boolean,
    onDismiss: () -> Unit,
    initialColor: Long,
    onPick: (Long) -> Unit,
) {
    if (!open) return
    var pickerColor by remember { mutableStateOf(initialColor) }
    LaunchedEffect(initialColor) { pickerColor = initialColor }

    val hsv = FloatArray(3)
    fun syncSliders() {
        android.graphics.Color.colorToHSV(pickerColor.toInt(), hsv)
    }
    syncSliders()
    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1] * 100f) }
    var brightness by remember { mutableFloatStateOf(hsv[2] * 100f) }
    var alpha by remember { mutableFloatStateOf(Color(pickerColor).alpha * 100f) }
    var hex by remember { mutableStateOf(String.format("%08X", pickerColor.toInt())) }

    fun derive() {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation / 100f, brightness / 100f))
        val a = (alpha / 100f * 255f).toInt().coerceIn(0, 255)
        val r = android.graphics.Color.red(argb)
        val g = android.graphics.Color.green(argb)
        val b = android.graphics.Color.blue(argb)
        pickerColor = (android.graphics.Color.argb(a, r, g, b).toLong() and 0xFFFFFFFFL)
        hex = String.format("%08X", pickerColor.toInt())
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = AppShapes.CardMedium,
            color = Color(0xFF141520),
            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
            modifier = Modifier.padding(16.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp).width(340.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(pickerColor))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = hex,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            tintSliderRow("Hue", "${hue.roundToInt()}°", hue, 0f..360f, 360) {
                hue = it
                derive()
            }
            tintSliderRow("Saturation", "${saturation.roundToInt()}%", saturation, 0f..100f, 100) {
                saturation = it
                derive()
            }
            tintSliderRow("Brightness", "${brightness.roundToInt()}%", brightness, 0f..100f, 100) {
                brightness = it
                derive()
            }
            tintSliderRow("Alpha", "${alpha.roundToInt()}%", alpha, 0f..100f, 100) {
                alpha = it
                derive()
            }

            OutlinedTextField(
                value = hex,
                onValueChange = { input ->
                    hex = input
                    try {
                        val raw = input.trimStart()
                        val parsed = android.graphics.Color.parseColor(if (raw.startsWith("#")) raw else "#$raw")
                        pickerColor = parsed.toLong()
                        val hsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(parsed, hsv)
                        hue = hsv[0]
                        saturation = hsv[1] * 100f
                        brightness = hsv[2] * 100f
                        alpha = Color(parsed).alpha * 100f
                    } catch (_: IllegalArgumentException) {
                    }
                },
                singleLine = true,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hex") },
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = { onDismiss() },
                ) {
                    Text(text = "Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onPick(pickerColor)
                        onDismiss()
                    },
                ) {
                    Text(text = "Apply")
                }
            }
        }
        }
    }
}

/**
 * Label + value row paired with [DomChatSlider] inside the tint picker dialog.
 */
@Composable
private fun tintSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(valueText, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
    DomChatSlider(
        value = value,
        onValueChange = onChange,
        valueRange = valueRange,
        steps = steps,
    )
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.InterfaceStyleCard(
    selected: Boolean,
    label: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val fill = if (selected) Color(0xFF000000) else Color.White.copy(alpha = 0.04f)
    val border: BorderStroke = if (selected) {
        BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
    } else {
        BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))
    }
    val titleColor = if (selected) Color.White else Color.White.copy(alpha = 0.66f)
    val subtitleColor = Color.White.copy(alpha = if (selected) 0.85f else 0.45f)

    Surface(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = fill,
        border = border,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = titleColor,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun buildModelCatalogSubtitle(
    context: android.content.Context,
    status: ModelCatalogStatus,
): String {
    val sourceLabel = when (status.source) {
        ModelCatalogSource.BUNDLED -> context.getString(R.string.setting_display_model_catalog_source_bundled)
        ModelCatalogSource.DOWNLOADED -> context.getString(R.string.setting_display_model_catalog_source_downloaded)
    }
    val lastRefreshLabel = status.lastSuccessfulRefreshAt?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
    }
    return if (status.source == ModelCatalogSource.DOWNLOADED && lastRefreshLabel != null) {
        context.getString(
            R.string.setting_display_model_catalog_subtitle_downloaded,
            sourceLabel,
            status.entryCount,
            lastRefreshLabel,
        )
    } else {
        context.getString(
            R.string.setting_display_model_catalog_subtitle_bundled,
            sourceLabel,
            status.entryCount,
        )
    }
}
