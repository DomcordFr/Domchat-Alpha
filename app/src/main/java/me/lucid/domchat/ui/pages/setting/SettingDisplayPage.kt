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
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.material.icons.rounded.AutoAwesome
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
import me.lucid.domchat.data.datastore.TwoPresetGlassMode
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
            ProgressiveBlurHeader(
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
                content = {
                    OneUITopAppBar(
                        title = stringResource(R.string.setting_display_page_title),
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            BackButton()
                        }
                    )
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isDark = LocalDarkMode.current
                        var localGlassAlpha by remember(settings.displaySetting.glassAlpha) {
                            mutableFloatStateOf(settings.displaySetting.glassAlpha)
                        }
                        var localGlassBlur by remember(settings.displaySetting.glassBlur) {
                            mutableFloatStateOf(settings.displaySetting.glassBlur)
                        }
                        var localBaseLuminance by remember(settings.displaySetting.glassBaseLuminance) {
                            mutableFloatStateOf(settings.displaySetting.glassBaseLuminance)
                        }
                        var localBubbleAlpha by remember(settings.displaySetting.chatBubbleAlpha) {
                            mutableFloatStateOf(settings.displaySetting.chatBubbleAlpha)
                        }
                        var localBubbleBlur by remember(settings.displaySetting.chatBubbleBlur) {
                            mutableFloatStateOf(settings.displaySetting.chatBubbleBlur)
                        }
                        var localBubbleTone by remember(settings.displaySetting.chatBubbleTone) {
                            mutableFloatStateOf(settings.displaySetting.chatBubbleTone)
                        }
                        var localBarGradient by remember(settings.displaySetting.barBlackGradientIntensity) {
                            mutableFloatStateOf(settings.displaySetting.barBlackGradientIntensity)
                        }
                        var localBarBlurIntensity by remember(settings.displaySetting.barBlackBlurIntensity) {
                            mutableFloatStateOf(settings.displaySetting.barBlackBlurIntensity)
                        }

                        // --- SECTION 1: PANELS & SHEETS ---
                        Text(
                            text = "Interface & Navigation Panels",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.White
                        )
                        Text(
                            text = "Controls background blur, opacity, and base tone across sheets, popovers, and menus.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f)
                        )

                        // Glass Opacity / Transparency Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("UI Glass Opacity", style = MaterialTheme.typography.bodyMedium)
                            Text("${(localGlassAlpha * 100).roundToInt()}% (${((1f - localGlassAlpha) * 100).roundToInt()}% transparent)", style = MaterialTheme.typography.labelMedium, color = Color.White)
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
                            valueRange = 0f..1.0f
                        )

                        // Glass Blur Slider (0 to 100 dp)
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
                            valueRange = 0f..100f
                        )

                        // Glass Foundation Tone Slider (0.0 = OLED Black, 1.0 = Pure White)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Glass Foundation Tone", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when {
                                    localBaseLuminance <= 0.05f -> "Pitch Black (0%)"
                                    localBaseLuminance >= 0.95f -> "Pure White (100%)"
                                    else -> "${(localBaseLuminance * 100).roundToInt()}% Tint"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                        DomChatSlider(
                            value = localBaseLuminance,
                            onValueChange = {
                                val rounded = (it * 100f).roundToInt() / 100f
                                if (rounded != localBaseLuminance) {
                                    haptics.perform(HapticPattern.Tick)
                                    localBaseLuminance = rounded
                                }
                            },
                            onValueChangeFinished = {
                                updateDisplaySetting(settings.displaySetting.copy(glassBaseLuminance = localBaseLuminance))
                            },
                            valueRange = 0f..1.0f
                        )

                        // Bar black gradient intensity (top / bottom bar gradient strength)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bar Gradient Intensity", style = MaterialTheme.typography.bodyMedium)
                            Text("${(localBarGradient * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                        }
                        DomChatSlider(
                            value = localBarGradient,
                            onValueChange = {
                                val rounded = (it * 100f).roundToInt() / 100f
                                if (rounded != localBarGradient) {
                                    haptics.perform(HapticPattern.Tick)
                                    localBarGradient = rounded
                                }
                            },
                            onValueChangeFinished = {
                                updateDisplaySetting(settings.displaySetting.copy(barBlackGradientIntensity = localBarGradient))
                            },
                            valueRange = 0f..1.0f
                        )

                        // Connect side menu + chat popovers to the glass sliders above
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Connect Side Menu & Popovers", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Side menu and chat popovers follow the opacity, blur, and tone sliders. Off keeps them a fixed charcoal glass.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f)
                                )
                            }
                            HapticSwitch(
                                checked = settings.displaySetting.linkChromeToGlass,
                                onCheckedChange = {
                                    updateDisplaySetting(settings.displaySetting.copy(linkChromeToGlass = it))
                                }
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        )

                        // Dual-Preset Glass Engine
                        SettingsGroup(
                            title = "Dual-Preset Glass Engine",
                            subtitle = "Choose between AMOLED Black Glass and Original Blur modes"
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Glass Mode",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                                Select(
                                    value = settings.displaySetting.twoPresetGlassMode,
                                    onValueChange = {
                                        updateDisplaySetting(
                                            settings.displaySetting.copy(twoPresetGlassMode = it)
                                        )
                                    },
                                    options = listOf(
                                        TwoPresetGlassMode.LIQUID_GLASS,
                                        TwoPresetGlassMode.OG_BLUR
                                    ),
                                    optionToString = { it.name }
                                )
                            }
                        }

                        // Bar Black Gradient & Blur
                        SettingsGroup(
                            title = "Bar Black Gradient & Blur",
                            subtitle = "Control top/bottom bar appearance"
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Bar Gradient Intensity", style = MaterialTheme.typography.bodyMedium)
                                    Text("${(localBarGradient * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                                DomChatSlider(
                                    value = localBarGradient,
                                    onValueChange = {
                                        val rounded = (it * 100f).roundToInt() / 100f
                                        if (rounded != localBarGradient) {
                                            haptics.perform(HapticPattern.Tick)
                                            localBarGradient = rounded
                                        }
                                    },
                                    onValueChangeFinished = {
                                        updateDisplaySetting(settings.displaySetting.copy(barBlackGradientIntensity = localBarGradient))
                                    },
                                    valueRange = 0f..1.0f
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Bar Blur Intensity", style = MaterialTheme.typography.bodyMedium)
                                    Text("${(localBarBlurIntensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                }
                                DomChatSlider(
                                    value = localBarBlurIntensity,
                                    onValueChange = {
                                        val rounded = (it * 100f).roundToInt() / 100f
                                        if (rounded != localBarBlurIntensity) {
                                            haptics.perform(HapticPattern.Tick)
                                            localBarBlurIntensity = rounded
                                        }
                                    },
                                    onValueChangeFinished = {
                                        updateDisplaySetting(settings.displaySetting.copy(barBlackBlurIntensity = localBarBlurIntensity))
                                    },
                                    valueRange = 0f..1.0f
                                )
                            }
                        }

                        // Animation & Motion
                        SettingsGroup(
                            title = "Animation & Motion",
                            subtitle = "Essential motion effects for the UI"
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Velocity Motion Blur
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Velocity Motion Blur", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Blur while scrolling", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                                    }
                                    HapticSwitch(
                                        checked = settings.displaySetting.enableVelocityMotionBlur,
                                        onCheckedChange = {
                                            updateDisplaySetting(settings.displaySetting.copy(enableVelocityMotionBlur = it))
                                        },
                                        glassMode = settings.displaySetting.twoPresetGlassMode.name
                                    )
                                }

                                var localMotionBlurIntensity by remember(settings.displaySetting.velocityMotionBlurIntensity) {
                                    mutableFloatStateOf(settings.displaySetting.velocityMotionBlurIntensity)
                                }

                                if (settings.displaySetting.enableVelocityMotionBlur) {
                                    AnimatedVisibility(
                                        visible = settings.displaySetting.enableVelocityMotionBlur,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Motion Blur Intensity", style = MaterialTheme.typography.bodyMedium)
                                            Text("${(localMotionBlurIntensity * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = Color.White)
                                        }
                                        DomChatSlider(
                                            value = localMotionBlurIntensity,
                                            onValueChange = {
                                                val rounded = (it * 100f).roundToInt() / 100f
                                                if (rounded != localMotionBlurIntensity) {
                                                    haptics.perform(HapticPattern.Tick)
                                                    localMotionBlurIntensity = rounded
                                                }
                                            },
                                            onValueChangeFinished = {
                                                updateDisplaySetting(settings.displaySetting.copy(velocityMotionBlurIntensity = localMotionBlurIntensity))
                                            },
                                            valueRange = 0f..1.0f
                                        )
                                    }
                                }

                                // Advanced UI Glass Settings
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Advanced UI Glass Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Blur, refraction, specular, tint dials", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                                    }
                                    HapticSwitch(
                                        checked = settings.displaySetting.advancedUiGlassSettings,
                                        onCheckedChange = {
                                            updateDisplaySetting(settings.displaySetting.copy(advancedUiGlassSettings = it))
                                        },
                                        glassMode = settings.displaySetting.twoPresetGlassMode.name
                                    )
                                }

                                // Advanced Chat Glass Settings
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Advanced Chat Glass Settings", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Bubble blur, alpha, tone, tint dials", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                                    }
                                    HapticSwitch(
                                        checked = settings.displaySetting.advancedChatGlassSettings,
                                        onCheckedChange = {
                                            updateDisplaySetting(settings.displaySetting.copy(advancedChatGlassSettings = it))
                                        },
                                        glassMode = settings.displaySetting.twoPresetGlassMode.name
                                    )
                                }

                                // Liquid Press Ripples
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Liquid Press Ripples", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Ripple effect on buttons", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                                    }
                                    HapticSwitch(
                                        checked = settings.displaySetting.enableLiquidPressRipple,
                                        onCheckedChange = {
                                            updateDisplaySetting(settings.displaySetting.copy(enableLiquidPressRipple = it))
                                        },
                                        glassMode = settings.displaySetting.twoPresetGlassMode.name
                                    )
                                }

                                // Nav Hold-and-Move
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Nav Hold-and-Move", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Hold to move chat items", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                                    }
                                    HapticSwitch(
                                        checked = settings.displaySetting.enableNavHoldMove,
                                        onCheckedChange = {
                                            updateDisplaySetting(settings.displaySetting.copy(enableNavHoldMove = it))
                                        },
                                        glassMode = settings.displaySetting.twoPresetGlassMode.name
                                    )
                                }

                                // Shared-Axis Transitions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Shared-Axis Transitions", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text("Shared-axis page transitions", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                                    }
                                    HapticSwitch(
                                        checked = settings.displaySetting.enableSharedAxisTransitions,
                                        onCheckedChange = {
                                            updateDisplaySetting(settings.displaySetting.copy(enableSharedAxisTransitions = it))
                                        },
                                        glassMode = settings.displaySetting.twoPresetGlassMode.name
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Chat Bubbles & Reasoning Glass",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.White
                        )
                        Text(
                            text = "Controls transparency, tint, and blur for message bubbles and AI reasoning cards.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.65f)
                        )

                        // Chat Bubble Opacity Slider (0% to 100%)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bubble Opacity", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when {
                                    localBubbleAlpha <= 0.01f -> "Clear Glass (0%)"
                                    localBubbleAlpha >= 0.98f -> "Solid (100%)"
                                    else -> "${(localBubbleAlpha * 100).roundToInt()}%"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
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
                            valueRange = 0f..1.0f
                        )

                        // Chat Bubble Blur Slider (0 to 100 dp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bubble Blur Radius", style = MaterialTheme.typography.bodyMedium)
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
                            valueRange = 0f..100f
                        )

                        // Chat Bubble Base Tone Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bubble Base Tone", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                when {
                                    localBubbleTone <= 0.05f -> "Pure Black (0%)"
                                    localBubbleTone >= 0.95f -> "Pure White (100%)"
                                    else -> "${(localBubbleTone * 100).roundToInt()}%"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                        DomChatSlider(
                            value = localBubbleTone,
                            onValueChange = {
                                val rounded = (it * 100f).roundToInt() / 100f
                                if (rounded != localBubbleTone) {
                                    haptics.perform(HapticPattern.Tick)
                                    localBubbleTone = rounded
                                }
                            },
                            onValueChangeFinished = {
                                updateDisplaySetting(settings.displaySetting.copy(chatBubbleTone = localBubbleTone))
                            },
                            valueRange = 0f..1.0f
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
