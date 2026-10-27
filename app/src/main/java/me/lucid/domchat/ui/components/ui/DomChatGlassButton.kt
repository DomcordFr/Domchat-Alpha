package me.lucid.domchat.ui.components.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import me.lucid.domchat.ui.hooks.HapticPattern
import me.lucid.domchat.ui.hooks.rememberPremiumHaptics
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

private const val DOMCHAT_DESTRUCTIVE = 0xFFFF6B6B.toInt()
private val DomChatAccent = Color(0xFFA78BFA)

/**
 * Dark glassy pill button (enable/disable actions, saves, resets).
 * Replaces stock Material TextButtons/Buttons/Badges that render grey or red.
 */
@Composable
fun DomChatGlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    accent: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    val haptics = rememberPremiumHaptics()
    DomChatGlassPillButtonContent(
        onClick = {
            haptics.perform(HapticPattern.Pop)
            onClick()
        },
        modifier = modifier,
        icon = icon,
        destructive = destructive,
        accent = accent,
        enabled = enabled,
        contentPadding = contentPadding,
        content = content,
        text = text,
    )
}

@Composable
private fun DomChatGlassPillButtonContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    accent: Boolean = false,
    contentPadding: PaddingValues,
    text: String,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.ButtonPill,
        color = Color(DOMCHAT_UNIFIED_CARD_COLOR).copy(alpha = if (enabled) 0.75f else 0.45f),
        border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = when {
                        destructive -> Color(DOMCHAT_DESTRUCTIVE)
                        accent -> DomChatAccent
                        else -> Color.White.copy(alpha = 0.9f)
                    }
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = when {
                    !enabled -> Color.White.copy(alpha = 0.4f)
                    destructive -> Color(DOMCHAT_DESTRUCTIVE)
                    accent -> DomChatAccent
                    else -> Color.White.copy(alpha = 0.92f)
                },
                maxLines = 1,
            )
            content?.invoke(this)
        }
    }
}

/** Small dark glassy badge/pill for status text ("Chat Specific", model tags, etc.). */
@Composable
fun DomChatGlassChip(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        color = Color(DOMCHAT_UNIFIED_CARD_COLOR).copy(alpha = 0.75f),
        shape = AppShapes.Tag,
        border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (accent) DomChatAccent else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}
