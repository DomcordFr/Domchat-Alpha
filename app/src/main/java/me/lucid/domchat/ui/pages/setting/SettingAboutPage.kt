package me.lucid.domchat.ui.pages.setting

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.lucid.domchat.BuildConfig
import me.lucid.domchat.R
import me.lucid.domchat.ui.components.nav.BackButton
import me.lucid.domchat.ui.components.settings.DomChatAboutContent
import me.lucid.domchat.ui.components.settings.DomChatAboutItem
import me.lucid.domchat.ui.components.ui.HapticSwitch
import me.lucid.domchat.ui.components.ui.ToastType
import me.lucid.domchat.ui.components.ui.UpdateDialog
import me.lucid.domchat.ui.context.LocalToaster
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.theme.LocalDarkMode
import me.lucid.domchat.utils.UiState
import me.lucid.domchat.utils.UpdateChecker
import me.lucid.domchat.utils.UpdateInfo
import me.lucid.domchat.utils.Version
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingAboutPage(settingVm: SettingVM = koinViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPremiumHaptics()
    val toaster = LocalToaster.current
    val updateChecker = koinInject<UpdateChecker>()
    val settings by settingVm.settings.collectAsStateWithLifecycle()
    val currentVersionName = BuildConfig.VERSION_NAME

    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showManualUpdateDialog by remember { mutableStateOf(false) }

    fun checkForUpdateManually(openDialogRegardless: Boolean = false, notifyUpToDate: Boolean = true) {
        if (isCheckingForUpdate) return
        isCheckingForUpdate = true
        scope.launch {
            try {
                updateChecker.checkUpdate().collect { state ->
                    when (state) {
                        is UiState.Idle -> Unit
                        is UiState.Loading -> {
                            isCheckingForUpdate = true
                        }
                        is UiState.Success -> {
                            isCheckingForUpdate = false
                            val info = state.data
                            manualUpdateInfo = info
                            val latestVersion = Version(info.version)
                            val currentVersion = Version(currentVersionName)
                            if (openDialogRegardless || latestVersion > currentVersion) {
                                showManualUpdateDialog = true
                            } else if (notifyUpToDate) {
                                toaster.show(
                                    message = "You're on the latest version (v$currentVersionName)",
                                    type = ToastType.Success
                                )
                            }
                        }
                        is UiState.Error -> {
                            isCheckingForUpdate = false
                            toaster.show(
                                message = state.error.message?.takeIf { it.isNotBlank() } ?: "Failed to check for updates",
                                type = ToastType.Error
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is kotlin.coroutines.cancellation.CancellationException) throw e
                isCheckingForUpdate = false
                toaster.show(
                    message = e.message?.takeIf { it.isNotBlank() } ?: "Failed to check for updates",
                    type = ToastType.Error
                )
            }
        }
    }

    // Automatically check on mount to populate the latest version line
    LaunchedEffect(Unit) {
        checkForUpdateManually(openDialogRegardless = false, notifyUpToDate = false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.setting_page_about)) },
                navigationIcon = { BackButton() },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            DomChatAboutContent(
                appName = stringResource(R.string.app_name),
                versionName = currentVersionName,
                darkTheme = LocalDarkMode.current,
                platformTitle = "Android Version",
                platformSubtitle = "${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})",
                platformIcon = Icons.Rounded.Android,
                deviceSubtitle = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                deviceIcon = Icons.Rounded.PhoneAndroid,
                architectureSubtitle = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown",
                architectureIcon = Icons.Rounded.Memory,
                onSourceCode = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/DomcordFr/Domchat"),
                        ),
                    )
                },
                extraProjectContent = {
                    // 1. Check for Updates switch card (full multiline description)
                    DomChatAboutItem(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(R.string.setting_display_check_updates_title),
                        subtitle = stringResource(R.string.setting_display_check_updates_desc),
                        subtitleMaxLines = Int.MAX_VALUE,
                        darkTheme = LocalDarkMode.current,
                        trailingContent = {
                            HapticSwitch(
                                checked = settings.displaySetting.checkForUpdates,
                                onCheckedChange = { checked ->
                                    settingVm.updateDisplaySetting(settings.displaySetting.copy(checkForUpdates = checked))
                                }
                            )
                        }
                    )

                    // 2. Version Update card (current version, latest version, and install button)
                    DomChatAboutItem(
                        icon = Icons.Rounded.SystemUpdateAlt,
                        title = "Version Update",
                        darkTheme = LocalDarkMode.current,
                        subtitleContent = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Line 1: Current version
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Current Version:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF9A9AA2)
                                    )
                                    Text(
                                        text = "v$currentVersionName",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White
                                    )
                                }

                                // Line 2: Latest version & status badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Latest Version:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF9A9AA2)
                                    )
                                    val info = manualUpdateInfo
                                    val isNewer = info != null && Version(info.version) > Version(currentVersionName)
                                    Text(
                                        text = when {
                                            isCheckingForUpdate -> "Checking..."
                                            info != null -> "v${info.version}"
                                            else -> "v$currentVersionName"
                                        },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isNewer) Color(0xFF34C759) else Color.White
                                    )

                                    if (!isCheckingForUpdate && info != null) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (isNewer) Color(0xFF34C759).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                            border = BorderStroke(
                                                0.5.dp,
                                                if (isNewer) Color(0xFF34C759).copy(alpha = 0.40f) else Color.White.copy(alpha = 0.15f)
                                            )
                                        ) {
                                            Text(
                                                text = if (isNewer) "Update Available" else "Up to Date",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = if (isNewer) Color(0xFF34C759) else Color.White.copy(alpha = 0.70f),
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(4.dp))

                                // Button to get and install the latest version even if already on it
                                val installBtnInteraction = remember { MutableInteractionSource() }
                                val isInstallPressed by installBtnInteraction.collectIsPressedAsState()
                                val installScale by animateFloatAsState(
                                    targetValue = if (isInstallPressed) 0.96f else 1f,
                                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                                    label = "about_install_btn_press"
                                )

                                Button(
                                    onClick = {
                                        haptics.perform(HapticPattern.Pop)
                                        val info = manualUpdateInfo
                                        if (info != null) {
                                            showManualUpdateDialog = true
                                        } else {
                                            checkForUpdateManually(openDialogRegardless = true, notifyUpToDate = false)
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF000000)
                                    ),
                                    interactionSource = installBtnInteraction,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp)
                                        .graphicsLayer {
                                            scaleX = installScale
                                            scaleY = installScale
                                        }
                                ) {
                                    if (isCheckingForUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Checking...",
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.Download,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        val info = manualUpdateInfo
                                        val btnText = when {
                                            info != null && Version(info.version) > Version(currentVersionName) ->
                                                "Install Update (v${info.version})"
                                            info != null ->
                                                "Install Latest (v${info.version})"
                                            else ->
                                                "Get Latest Version"
                                        }
                                        Text(
                                            text = btnText,
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(
                                onClick = {
                                    haptics.perform(HapticPattern.Pop)
                                    checkForUpdateManually(openDialogRegardless = false, notifyUpToDate = true)
                                },
                                enabled = !isCheckingForUpdate
                            ) {
                                if (isCheckingForUpdate) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Check for updates",
                                        tint = Color(0xFF9A9AA2),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    )
                },
                onVersionLongPress = {
                    updateChecker.forceUpdateCheck()
                    toaster.show(
                        message = "Update banner forced for testing.",
                        type = ToastType.Info,
                    )
                },
                onHaptic = { haptics.perform(HapticPattern.Pop) },
            )
        }
    }

    val currentManualUpdate = manualUpdateInfo
    if (showManualUpdateDialog && currentManualUpdate != null) {
        UpdateDialog(
            info = currentManualUpdate,
            updateChecker = updateChecker,
            onDismiss = {
                showManualUpdateDialog = false
            },
            onLater = {
                updateChecker.dismissVersion(context, currentManualUpdate.version)
                showManualUpdateDialog = false
            },
            onIgnore = {
                updateChecker.ignoreUpdate(context, currentManualUpdate.version)
                showManualUpdateDialog = false
            }
        )
    }
}
