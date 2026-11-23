package me.lucid.domchat.ui.pages.setting

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    var isCheckingForUpdate by remember { mutableStateOf(false) }
    var manualUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showManualUpdateDialog by remember { mutableStateOf(false) }

    fun checkForUpdateManually() {
        if (isCheckingForUpdate) return
        isCheckingForUpdate = true
        scope.launch {
            try {
                updateChecker.checkUpdate().collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            isCheckingForUpdate = true
                        }
                        is UiState.Success -> {
                            isCheckingForUpdate = false
                            val info = state.data
                            val latestVersion = Version(info.version)
                            val currentVersion = Version(BuildConfig.VERSION_NAME)
                            if (latestVersion > currentVersion) {
                                manualUpdateInfo = info
                                showManualUpdateDialog = true
                            } else {
                                toaster.show(
                                    message = "You're on the latest version (v${BuildConfig.VERSION_NAME})",
                                    type = ToastType.Success
                                )
                            }
                        }
                        is UiState.Error -> {
                            isCheckingForUpdate = false
                            toaster.show(
                                message = state.throwable.message?.takeIf { it.isNotBlank() } ?: "Failed to check for updates",
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
                versionName = BuildConfig.VERSION_NAME,
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
                    DomChatAboutItem(
                        icon = Icons.Rounded.Refresh,
                        title = stringResource(R.string.setting_display_check_updates_title),
                        subtitle = stringResource(R.string.setting_display_check_updates_desc),
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
                    DomChatAboutItem(
                        icon = Icons.Rounded.SystemUpdateAlt,
                        title = "Software Update",
                        subtitle = if (isCheckingForUpdate) {
                            "Checking for updates..."
                        } else {
                            "Check for the latest release (Current: v${BuildConfig.VERSION_NAME})"
                        },
                        darkTheme = LocalDarkMode.current,
                        trailingContent = {
                            if (isCheckingForUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF9A9AA2),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        onHaptic = { haptics.perform(HapticPattern.Pop) },
                        onClick = if (!isCheckingForUpdate) {
                            { checkForUpdateManually() }
                        } else null
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
