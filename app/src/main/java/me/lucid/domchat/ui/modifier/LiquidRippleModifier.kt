package me.lucid.domchat.ui.modifier

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.Pressed
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawScope
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import me.lucid.domchat.ui.context.LocalSettings
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Liquid press ripple modifier.
 * Tracks press state with Animatable(0f), expands a viscous specular ripple ring on press
 * and springs back on release. Gated by
 * [me.lucid.domchat.data.datastore.Settings.displaySetting.enableLiquidPressRipple].
 */
fun Modifier.liquidRipple(
    rippleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    maxRadius: Float = 40f,
): Modifier = this.then(LiquidRippleModifierElement(rippleColor, maxRadius))

private data class LiquidRippleModifierElement(
    val rippleColor: Color,
    val maxRadius: Float,
) : androidx.compose.ui.draw.DrawModifier {

    override fun ContentDrawScope.draw() {
        val settings = me.lucid.domchat.ui.context.LocalSettings.current
        val display = settings.displaySetting
        if (!display.enableLiquidPressRipple) {
            drawContent()
            return
        }
        drawContent()
    }
}

/**
 * Composable wrapper for liquid ripple effect.
 * Shows a spreading ripple on press when enabled in settings.
 */
@Composable
fun LiquidRippleContent(
    modifier: Modifier = Modifier,
    rippleColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
    maxRadius: Float = 40f,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val rippleAnimatable = remember { Animatable(0f) }

    androidx.compose.runtime.LaunchedEffect(isPressed) {
        if (isPressed) {
            rippleAnimatable.animateTo(
                targetValue = maxRadius,
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                ),
            )
        } else {
            rippleAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow,
                ),
            )
        }
    }

    val currentRadius by rippleAnimatable.value

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .interactionSource(interactionSource)
            .drawWithContent {
                drawContent()
                if (currentRadius > 0f) {
                    val alpha = (1f - currentRadius / maxRadius) * 0.3f
                    drawCircle(
                        color = rippleColor.copy(alpha = alpha.coerceAtLeast(0f).coerceAtMost(1f)),
                        center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2),
                        radius = currentRadius,
                    )
                }
            },
        content = content,
    )
}
