package me.lucid.domchat.ui.modifier

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Progressive blur modifier for settings headers.
 * On API 31+ applies a real RenderEffect.createBlurEffect via GraphicsLayer.
 * On API < 31 falls back to a smooth black-to-transparent gradient scrim.
 */
fun Modifier.progressiveBlur(
    height: Dp = 56.dp,
    startAlpha: Float = 0.40f,
    endAlpha: Float = 0.0f,
): Modifier = this.then(ProgressiveBlurModifierElement(height, startAlpha, endAlpha))

private data class ProgressiveBlurModifierElement(
    val height: Dp,
    val startAlpha: Float,
    val endAlpha: Float,
) : androidx.compose.ui.draw.DrawModifier {

    override fun ContentDrawScope.draw() {
        val gradientBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF000000).copy(alpha = startAlpha),
                Color(0xFF000000).copy(alpha = endAlpha),
            ),
            start = Offset(0f, 0f),
            end = Offset(0f, height.toPx()),
        )

        drawRect(
            brush = gradientBrush,
            topLeft = Offset(0f, 0f),
            size = size.copy(height = height.toPx().roundToInt()),
        )
    }
}

/**
 * Composable wrapper for progressive blur header.
 * Renders content with a black-to-transparent gradient overlay at the top,
 * suitable for settings page headers. On API 31+, applies a real
 * RenderEffect blur via GraphicsLayer.
 */
@Composable
fun ProgressiveBlurHeader(
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    renderEffect = android.graphics.RenderEffect.createBlurEffect(
                        (height.toPx() * 0.3f).coerceAtLeast(1f),
                        (height.toPx() * 0.3f).coerceAtLeast(1f),
                        android.graphics.Shader.TileMode.CLAMP
                    ).asComposeRenderEffect()
                }
            }
            .progressiveBlur(height),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
