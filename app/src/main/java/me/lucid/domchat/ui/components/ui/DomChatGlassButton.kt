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
private val DomChatAccent = Color.White

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
    containerColor: Color? = null,
    contentColor: Color? = null,
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
        containerColor = containerColor,
        contentColor = contentColor,
        content = content,
        text = text,
    )
}

/**
 * Dark glassy pill button.
 * Default (no containerColor) is a translucent frosted glass surface for chat pages
 * (no solid white, no solid black). Settings pages can pass `containerColor = Color(0xFF000000)`
 * for a black glass + specular border variant.
 */
@Composable
private fun DomChatGlassPillButtonContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    accent: Boolean = false,
    contentPadding: PaddingValues,
    containerColor: Color? = null,
    contentColor: Color? = null,
    text: String,
    content: (@Composable RowScope.() -> Unit)? = null,
) {
    // Default chat-page style: translucent frosted glass (never white, never opaque black)
    val bgColor = containerColor.takeIf { it != null } ?: Color(0xFF000000).copy(alpha = 0.15f)
    // Default text/icons are white on dark glass; destructive overrides to red.
    val resolvedTextColor = contentColor.takeIf { it != null } ?: when {
        destructive -> Color(DOMCHAT_DESTRUCTIVE)
        else -> Color.White
    }
    val resolvedIconTint = when {
        destructive -> Color(DOMCHAT_DESTRUCTIVE)
        else -> resolvedTextColor
    }
    val borderBrush = when {
        destructive -> BorderStroke(0.5.dp, Color(DOMCHAT_DESTRUCTIVE).copy(alpha = 0.6f)).brush
        else -> LiquidGlassTokens.SpecularBorderSubtle
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppShapes.ButtonPill,
        color = if (enabled) bgColor else bgColor.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, borderBrush),
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
                    tint = resolvedIconTint
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = when {
                    !enabled -> resolvedTextColor.copy(alpha = 0.4f)
                    else -> resolvedTextColor
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
        color = Color(DOMCHAT_UNIFIED_CARD_COLOR),
        shape = AppShapes.Tag,
        border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),
        tonalElevation = 0.dp,
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (accent) DomChatAccent else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            maxLines = 1,
        )
    }
}
