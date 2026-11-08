package me.lucid.domchat.ui.components.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.lucid.domchat.ui.theme.LiquidGlassTokens
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete

/**
 * A reusable, smooth-opening destructive action confirmation dialog.
 *
 * Features:
 * - Centered on screen, capped at [widthIn(max = 420.dp)] so it never clips at the
 *   edges or gets cut off at the bottom on small screens.
 * - Single AMOLED black glass surface ([Color(0xFF000000)]) with a 0.5dp specular
 *   border ([LiquidGlassTokens.SpecularBorderSubtle]) — no nested/dual backgrounds.
 * - Smooth scale + fade entry/exit animations.
 * - Title and body message.
 * - Cancel button: glassy outlined, muted white text.
 * - Delete button: prominent glassy button with pure white text + icon and a specular border.
 *
 * @param visible Whether the dialog is currently shown.
 * @param title The dialog title (e.g. "Delete conversation?").
 * @param message The explanatory body text.
 * @param onConfirm Called when the user confirms the destructive action.
 * @param onDismiss Called when the dialog should be dismissed (cancel or backdrop press).
 * @param confirmLabel Label for the destructive button (default "Delete").
 * @param cancelLabel Label for the cancel button (default "Cancel").
 */
@Composable
fun UniversalDestructiveConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = "Delete",
    cancelLabel: String = "Cancel",
) {
    val dialogShape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)

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
            // Center the dialog both vertically and horizontally with generous padding
            // so it is never cut off at the bottom on small screens.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .padding(vertical = 28.dp, horizontal = 24.dp)
                        .background(Color(0xFF000000), shape = dialogShape)
                        .border(
                            0.5.dp,
                            LiquidGlassTokens.SpecularBorderSubtle,
                            dialogShape
                        )
                        .clip(dialogShape),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Body message
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Cancel button - glassy outlined, muted white text
                        OutlinedButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = dialogShape,
                            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.85f),
                                disabledContentColor = Color.White.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(text = cancelLabel)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Delete button - prominent glassy, pure white text + icon, specular border
                        Button(
                            onClick = {
                                onConfirm()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = dialogShape,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = Color.Red,
                                disabledContainerColor = Color.Transparent.copy(alpha = 0.2f),
                                disabledContentColor = Color.Red.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = confirmLabel,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
