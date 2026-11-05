package me.lucid.domchat.ui.pages.setting

import android.os.Build
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.lucid.domchat.R
import me.lucid.domchat.data.datastore.DisplaySetting
import me.lucid.domchat.data.ai.models.ModelCatalogSource
import me.lucid.domchat.data.ai.models.ModelCatalogStatus
import me.lucid.domchat.ui.components.nav.BackButton
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt
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
    var amoledDarkMode by rememberAmoledDarkMode()
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val haptics = rememberPremiumHaptics(enabled = settings.displaySetting.enableUIHaptics)

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(
            settings.copy(
                displaySetting = setting
            )
        )
    }

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

            // Appearance: Animation Speed Controls
            item {
                SettingsGroup(
                    title = "Interface Appearance & Transparency"
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

                    // Decoupled UI & Bubble glass style toggles (directly below Animation Speed)
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

                    // When linked, bubble mirrors UI style.
                    if (linkedStyles && bubbleIsGlass != uiIsGlass) {
                        bubbleIsGlass = uiIsGlass
                        updateDisplaySetting(
                            settings.displaySetting.copy(
                                bubbleInterfaceStyle = if (uiIsGlass) me.lucid.domchat.data.datastore.InterfaceStyle.GLASS
                                else me.lucid.domchat.data.datastore.InterfaceStyle.FROSTED
                            )
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        InterfaceStyleCard(
                            selected = uiIsGlass,
                            label = "UI Style",
                            subtitle = if (uiIsGlass) "Refractive liquid surface" else "Soft frosted blur",
                            icon = {
                                Icon(
                                    imageVector = if (uiIsGlass) Icons.Rounded.AutoAwesome else Icons.Rounded.BlurOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                if (!uiIsGlass) {
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
                                } else {
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
                                }
                            },
                        )
                        InterfaceStyleCard(
                            selected = bubbleIsGlass,
                            label = "Bubble Style",
                            subtitle = if (bubbleIsGlass) "Refractive liquid bubbles" else "Soft frosted bubbles",
                            icon = {
                                Icon(
                                    imageVector = if (bubbleIsGlass) Icons.Rounded.AutoAwesome else Icons.Rounded.BlurOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color.White
                                )
                            },
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                if (!linkedStyles) {
                                    val newBubbleIsGlass = !bubbleIsGlass
                                    bubbleIsGlass = newBubbleIsGlass
                                    updateDisplaySetting(
                                        settings.displaySetting.copy(
                                            bubbleInterfaceStyle = if (newBubbleIsGlass) me.lucid.domchat.data.datastore.InterfaceStyle.GLASS
                                            else me.lucid.domchat.data.datastore.InterfaceStyle.FROSTED
                                        )
                                    )
                                }
                            },
                        )
                    }

                    // Link UI & Bubbles switch
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

                    // Fine-tune sliders accordion                    }

                    // Fine-tune sliders accordion
                    var accordionExpanded by remember(settings.displaySetting) {
                        mutableStateOf(false)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clickable(onClick = { accordionExpanded = !accordionExpanded }),
                        contentAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Fine-tune Sliders",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (accordionExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = accordionExpanded,
                        enter = expandVertically(animationSpec = tween(durationMillis = 220)) + fadeIn(animationSpec = tween(durationMillis = 220)),
                        exit = shrinkVertically(animationSpec = tween(durationMillis = 220)) + fadeOut(animationSpec = tween(durationMillis = 220)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            var localBarGradientDensity by remember(settings.displaySetting.barGradientDensity) {
                                mutableFloatStateOf(settings.displaySetting.barGradientDensity)
                            }
                            var localBarGradientBlur by remember(settings.displaySetting.barGradientBlur) {
                                mutableFloatStateOf(settings.displaySetting.barGradientBlur)
                            }
                            var localGlassAlpha by remember(settings.displaySetting.glassAlpha) {
                                mutableFloatStateOf(settings.displaySetting.glassAlpha)
                            }
                            var localGlassBlur by remember(settings.displaySetting.glassBlur) {
                                mutableFloatStateOf(settings.displaySetting.glassBlur)
                            }
                            var localBubbleAlpha by remember(settings.displaySetting.chatBubbleAlpha) {
                                mutableFloatStateOf(settings.displaySetting.chatBubbleAlpha)
                            }
                            var localBubbleBlur by remember(settings.displaySetting.chatBubbleBlur) {
                                mutableFloatStateOf(settings.displaySetting.chatBubbleBlur)
                            }

                            // Bar Gradient Density (0% to 100%, default 100%, step 0.01f)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bar Gradient Density", style = MaterialTheme.typography.bodyMedium)
                                Text("${(localBarGradientDensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localBarGradientDensity,
                                onValueChange = { localBarGradientDensity = it },
                                onValueChangeFinished = {
                                    val rounded = (it * 100f).roundToInt() / 100f
                                    if (rounded != localBarGradientDensity) {
                                        haptics.perform(HapticPattern.Tick)
                                        localBarGradientDensity = rounded
                                    }
                                    updateDisplaySetting(settings.displaySetting.copy(barGradientDensity = localBarGradientDensity))
                                },
                                valueRange = 0f..1.0f,
                                steps = 100,
                            )

                            // Bar Gradient Blur (0dp to 60dp, default 0dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bar Gradient Blur", style = MaterialTheme.typography.bodyMedium)
                                Text("${localBarGradientBlur.roundToInt()} dp", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localBarGradientBlur,
                                onValueChange = { localBarGradientBlur = it },
                                onValueChangeFinished = {
                                    val rounded = it.roundToInt().toFloat()
                                    if (rounded != localBarGradientBlur) {
                                        haptics.perform(HapticPattern.Tick)
                                        localBarGradientBlur = rounded
                                    }
                                    updateDisplaySetting(settings.displaySetting.copy(barGradientBlur = localBarGradientBlur))
                                },
                                valueRange = 0f..60f,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            )

                            // UI Glass Opacity & Blur Radius
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("UI Glass Opacity", style = MaterialTheme.typography.bodyMedium)
                                Text("${(localGlassAlpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localGlassAlpha,
                                onValueChange = {
                                    val rounded = (it * 100f).roundToInt() / 100f
                                    if (rounded != localGlassAlpha) {
                                        haptics.perform(HapticPattern.Tick)
                                        localGlassAlpha = rounded
                                    }
                                },
                                onValueChangeFinished = {
                                    updateDisplaySetting(settings.displaySetting.copy(glassAlpha = localGlassAlpha))
                                },
                                valueRange = 0f..1.0f,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("UI Glass Blur Radius", style = MaterialTheme.typography.bodyMedium)
                                Text("${localGlassBlur.roundToInt()} dp", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localGlassBlur,
                                onValueChange = {
                                    val rounded = it.roundToInt().toFloat()
                                    if (rounded != localGlassBlur) {
                                        haptics.perform(HapticPattern.Tick)
                                        localGlassBlur = rounded
                                    }
                                },
                                onValueChangeFinished = {
                                    updateDisplaySetting(settings.displaySetting.copy(glassBlur = localGlassBlur))
                                },
                                valueRange = 0f..100f,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            )

                            // Chat Bubble Opacity & Blur Radius
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Chat Bubble Opacity", style = MaterialTheme.typography.bodyMedium)
                                Text("${(localBubbleAlpha * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localBubbleAlpha,
                                onValueChange = {
                                    val rounded = (it * 100f).roundToInt() / 100f
                                    if (rounded != localBubbleAlpha) {
                                        haptics.perform(HapticPattern.Tick)
                                        localBubbleAlpha = rounded
                                    }
                                },
                                onValueChangeFinished = {
                                    updateDisplaySetting(settings.displaySetting.copy(chatBubbleAlpha = localBubbleAlpha))
                                },
                                valueRange = 0f..1.0f,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Chat Bubble Blur Radius", style = MaterialTheme.typography.bodyMedium)
                                Text("${localBubbleBlur.roundToInt()} dp", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localBubbleBlur,
                                onValueChange = {
                                    val rounded = it.roundToInt().toFloat()
                                    if (rounded != localBubbleBlur) {
                                        haptics.perform(HapticPattern.Tick)
                                        localBubbleBlur = rounded
                                    }
                                },
                                onValueChangeFinished = {
                                    updateDisplaySetting(settings.displaySetting.copy(chatBubbleBlur = localBubbleBlur))
                                },
                                valueRange = 0f..100f,
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = Color.White.copy(alpha = 0.08f)
                            )

                            // Glass Tint Color presets
                            Text(
                                text = "Glass Tint Color",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tintPresets = listOf(
                                    Pair("Pure Clear", 0x00FFFFFFL),
                                    Pair("Ice Blue", 0xFFB7E4FFL),
                                    Pair("Emerald", 0xFF7FD6A4L),
                                    Pair("Rose Gold", 0xFFE8B7C4L),
                                    Pair("Sunset Amber", 0xFFFFCC80L),
                                    Pair("Violet", 0xFFC39BD3L)
                                )
                                var localTint by remember(settings.displaySetting.glassTintColor) {
                                    mutableStateOf(settings.displaySetting.glassTintColor)
                                }
                                val isTintClear = localTint == 0x00FFFFFFL
                                tintPresets.forEachIndexed { index, (label, color) ->
                                    val selected = localTint == color || (index == 0 && isTintClear)
                                    InterfaceStyleCard(
                                        selected = selected,
                                        label = label,
                                        subtitle = "",
                                        icon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(color)),
                                                contentAlignment = Alignment.Center
                                            )
                                        },
                                        onClick = {
                                            haptics.perform(HapticPattern.Pop)
                                            localTint = color
                                            updateDisplaySetting(
                                                settings.displaySetting.copy(glassTintColor = color)
                                            )
                                        },
                                    )
                                }
                            }

                            // Glass Depth / Refraction Scale slider (0.0x..2.5x)
                            var localDepthScale by remember(settings.displaySetting.glassDepthScale) {
                                mutableFloatStateOf(settings.displaySetting.glassDepthScale)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Glass Depth / Refraction", style = MaterialTheme.typography.bodyMedium)
                                Text("${localDepthScale.roundToInt()}x", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localDepthScale,
                                onValueChange = { localDepthScale = it },
                                onValueChangeFinished = {
                                    val rounded = (it * 10f).roundToInt() / 10f
                                    if (rounded != localDepthScale) {
                                        haptics.perform(HapticPattern.Tick)
                                        localDepthScale = rounded
                                    }
                                    updateDisplaySetting(settings.displaySetting.copy(glassDepthScale = localDepthScale))
                                },
                                valueRange = 0f..2.5f,
                                steps = 25,
                            )

                            // Specular Highlight Intensity slider (0%..100%)
                            var localSpecularIntensity by remember(settings.displaySetting.glassSpecularIntensity) {
                                mutableFloatStateOf(settings.displaySetting.glassSpecularIntensity)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Specular Highlight Intensity", style = MaterialTheme.typography.bodyMedium)
                                Text("${(localSpecularIntensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                            }
                            DomChatSlider(
                                value = localSpecularIntensity,
                                onValueChange = { localSpecularIntensity = it },
                                onValueChangeFinished = {
                                    val rounded = (it * 100f).roundToInt() / 100f
                                    if (rounded != localSpecularIntensity) {
                                        haptics.perform(HapticPattern.Tick)
                                        localSpecularIntensity = rounded
                                    }
                                    updateDisplaySetting(settings.displaySetting.copy(glassSpecularIntensity = localSpecularIntensity))
                                },
                                valueRange = 0f..1.0f,
                                steps = 100,
                            )
                        }
                    }

                    // Connect side menu + chat popovers to the glass sliders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Connect Side Menu & Popovers", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Side menu and chat popovers follow the opacity, blur, and tone sliders. Off keeps them a fixed charcoal glass.",
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

@Composable
private fun InterfaceStyleCard(
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
