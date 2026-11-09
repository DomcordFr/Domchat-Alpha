package me.lucid.domchat.ui.components.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.lucid.domchat.R
import me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur
import me.lucid.domchat.ui.modifier.domChatDialogContainerColor
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

private const val DELETE_RED = 0xFFE53935
private const val DIALOG_HEIGHT = 48.dp
private const val BUTTON_CORNER = 16.dp

/**
 * Single irreversible-confirm pattern: floating rounded dialog, clear title,
 * one-line consequence, Cancel + solid red confirm (Delete by default).
 *
 * The dialog is gated by [visible] and animates in/out with scale + fade so the
 * popup never appears or disappears abruptly.
 */
@Composable
fun DomChatDestructiveConfirmDialog(
    visible: Boolean,
    title: String,
    consequence: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = stringResource(R.string.delete),
    cancelLabel: String = stringResource(R.string.cancel),
) {
    val dialogShape = RoundedCornerShape(20.dp)
    val containerColor = domChatDialogContainerColor()
    ApplyDialogWindowBlur()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
            initialScale = 0.9f
        ),
        exit = fadeOut(animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f)) + scaleOut(
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
            targetScale = 0.9f
        ),
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = dialogShape,
                    color = containerColor,
                    border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                    modifier = Modifier.widthIn(min = 280.dp, max = 380.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )

                        // Body message
                        Text(
                            text = consequence,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            // Cancel button: translucent dark surface, subtle white
                            // border, centered white text.
                            Surface(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(BUTTON_CORNER),
                                color = Color(0xFF000000).copy(alpha = 0.15f),
                                border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                                tonalElevation = 0.dp,
                                modifier = Modifier.weight(1f).height(DIALOG_HEIGHT),
                            ) {
                                Text(
                                    text = cancelLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                )
                            }

                            // Confirm button: solid red fill, never just a tint.
                            Surface(
                                onClick = {
                                    onConfirm()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(BUTTON_CORNER),
                                color = Color(DELETE_RED),
                                tonalElevation = 0.dp,
                                modifier = Modifier.weight(1f).height(DIALOG_HEIGHT),
                            ) {
                                Text(
                                    text = confirmLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
