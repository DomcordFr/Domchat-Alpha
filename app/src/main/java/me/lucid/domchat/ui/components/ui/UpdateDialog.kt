package me.lucid.domchat.ui.components.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.lucid.domchat.BuildConfig
import me.lucid.domchat.ui.components.richtext.MarkdownBlock
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import me.lucid.domchat.utils.DownloadProgress
import me.lucid.domchat.utils.UpdateChecker
import me.lucid.domchat.utils.UpdateDownloadStatus
import me.lucid.domchat.utils.UpdateInfo

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.1f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    updateChecker: UpdateChecker,
    onDismiss: () -> Unit,
    onLater: () -> Unit,
    onIgnore: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberPremiumHaptics()
    val currentVersionName = BuildConfig.VERSION_NAME
    var downloadId by remember(info.version) { mutableStateOf<Long?>(null) }
    val progress by remember(downloadId) {
        val id = downloadId
        if (id != null && id != -1L) {
            updateChecker.observeDownload(context, id)
        } else {
            flowOf(DownloadProgress())
        }
    }.collectAsState(initial = DownloadProgress())

    val primaryDownload = info.downloads.firstOrNull()
    val isDownloading = progress.status == UpdateDownloadStatus.RUNNING
    val isDownloaded = progress.status == UpdateDownloadStatus.SUCCESSFUL && progress.localUri != null
    val downloadFailed = progress.status == UpdateDownloadStatus.FAILED
    val needsInstallPermission = isDownloaded && !updateChecker.canInstallDownloadedUpdate(context)
    val buttonShape = RoundedCornerShape(50)

    ApplyDialogWindowBlur()
    val dialogShape = RoundedCornerShape(26.dp)

    Dialog(
        onDismissRequest = {
            if (!isDownloading) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isDownloading,
            dismissOnClickOutside = !isDownloading,
        )
    ) {
        Surface(
            shape = dialogShape,
            color = Color(0xFF000000),
            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderBrush),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .widthIn(max = 440.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF000000))
                            .border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isDownloaded -> Icons.Rounded.CheckCircle
                                downloadFailed -> Icons.Rounded.Refresh
                                else -> Icons.Rounded.SystemUpdateAlt
                            },
                            contentDescription = null,
                            tint = when {
                                isDownloaded -> Color(0xFF34C759)
                                downloadFailed -> MaterialTheme.colorScheme.error
                                else -> Color.White
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                needsInstallPermission -> "Allow Installation"
                                isDownloaded -> "Ready to Install"
                                downloadFailed -> "Download Failed"
                                isDownloading -> "Downloading Update"
                                else -> "Update Available"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = Color.White
                        )
                        AnimatedContent(
                            targetState = Triple(progress.status, needsInstallPermission, downloadFailed),
                            label = "update_dialog_status_text"
                        ) { (status, permissionNeeded, failed) ->
                            Text(
                                text = when {
                                    permissionNeeded -> "Allow installs for Domchat to continue."
                                    failed -> progress.errorMessage ?: "Download failed. Please try again."
                                    status == UpdateDownloadStatus.RUNNING -> "Downloading the latest version..."
                                    status == UpdateDownloadStatus.SUCCESSFUL -> "Update verified and ready to install."
                                    else -> "A new version of Domchat is ready."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.65f)
                            )
                        }
                    }
                }

                // Version & Metadata Card
                Surface(
                    color = Color(0xFF000000),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "v$currentVersionName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.50f)
                                )
                                Text(
                                    text = "→",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.40f)
                                )
                                Text(
                                    text = "v${info.version}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }

                            if (primaryDownload != null && primaryDownload.size.isNotBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
                                ) {
                                    Text(
                                        text = primaryDownload.size,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }

                        if (primaryDownload != null) {
                            Text(
                                text = primaryDownload.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.50f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Download Progress
                if (downloadId != null) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress.progress.coerceIn(0f, 1f),
                        animationSpec = spring(stiffness = Spring.StiffnessLow),
                        label = "update_download_progress"
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(50)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.12f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDownloaded) "Download complete" else "${(progress.progress * 100).toInt().coerceIn(0, 100)}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.70f)
                            )
                            if (progress.totalBytes > 0) {
                                Text(
                                    text = "${formatBytes(progress.bytesDownloaded)} / ${formatBytes(progress.totalBytes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.45f)
                                )
                            }
                        }
                    }
                }

                // Release Notes
                Text(
                    text = "Release Notes",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White.copy(alpha = 0.85f)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF000000))
                        .border(BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle), RoundedCornerShape(16.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    MarkdownBlock(
                        content = info.changelog.ifBlank { "No release notes provided." }
                    )
                }

                // Action Buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isDownloading) {
                        Button(
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                when {
                                    isDownloaded && progress.localUri != null -> {
                                        val localUri = progress.localUri ?: return@Button
                                        scope.launch {
                                            updateChecker.installDownloadedUpdateAsync(context, localUri, primaryDownload?.sha256)
                                        }
                                    }
                                    primaryDownload != null -> {
                                        downloadId = updateChecker.downloadUpdate(context, primaryDownload)
                                    }
                                }
                            },
                            enabled = (isDownloaded && progress.localUri != null) || primaryDownload != null,
                            shape = buttonShape,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF000000),
                                disabledContainerColor = Color.White.copy(alpha = 0.3f),
                                disabledContentColor = Color(0xFF000000).copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = when {
                                    isDownloaded && needsInstallPermission -> "Allow & Install"
                                    isDownloaded -> "Install Now"
                                    downloadFailed -> "Try Again"
                                    else -> "Update Now"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF000000)
                            )
                        }
                    }

                    if (!isDownloading && !isDownloaded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptics.perform(HapticPattern.Pop)
                                    onIgnore()
                                },
                                shape = buttonShape,
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White.copy(alpha = 0.75f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Ignore")
                            }
                            OutlinedButton(
                                onClick = {
                                    haptics.perform(HapticPattern.Pop)
                                    onLater()
                                },
                                shape = buttonShape,
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White.copy(alpha = 0.75f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Later")
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                haptics.perform(HapticPattern.Pop)
                                onLater()
                            },
                            shape = buttonShape,
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.White.copy(alpha = 0.75f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(if (isDownloading) "Hide (Background)" else "Later")
                        }
                    }
                }
            }
        }
    }
}
