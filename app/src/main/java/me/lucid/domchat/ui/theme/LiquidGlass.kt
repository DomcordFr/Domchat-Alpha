package me.lucid.domchat.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.lucid.domchat.ui.context.LocalSettings
import me.lucid.domchat.data.model.LiquidGlassMode

/**
 * Liquid Glass Material Tokens.
 * Optical characteristics:
 * 1. Razor-sharp 0.5dp precision specular edge highlight catching ambient bounce light.
 * 2. Translucent surface luminance diffusion (children are kept razor-sharp).
 * 3. Gradient sheen simulating glass refraction and depth.
 * 4. Ambient ground occlusion.
 */
object LiquidGlassTokens {
    val CardShape = RoundedCornerShape(20.dp)
    val BubbleShape = RoundedCornerShape(18.dp)
    val PillShape = RoundedCornerShape(100.dp)

    // Specular border gradients - light reflects from top, disperses cleanly towards bottom
    val SpecularBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.40f), // Top specular rim catch
            Color.White.copy(alpha = 0.12f), // Mid-body glass refraction
            Color.White.copy(alpha = 0.03f), // Lower falloff
            Color.Transparent               // Clean ground falloff
        )
    )

    val SpecularBorderSubtle = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.22f),
            Color.White.copy(alpha = 0.06f),
            Color.Transparent
        )
    )

    val LightSpecularBorder = Brush.verticalGradient(
        listOf(
            Color.Black.copy(alpha = 0.12f),
            Color.Black.copy(alpha = 0.04f),
            Color.Transparent
        )
    )

    // Legacy static fallbacks (used when no settings composition local is available,
    // e.g. preview or unit composition). Clear glass anchor: unified DomChat glass card 0xFF141520.
    val GlassSurfaceDark = Color(0xFF141520).copy(alpha = 0.22f)
    val GlassSurfaceLight = Color(0xFFFFFFFF).copy(alpha = 0.45f)
    val BubbleGlassDark = Color(0xFFFFFFFF).copy(alpha = 0.06f)
    val BubbleGlassLight = Color(0xFF000000).copy(alpha = 0.03f)
}

/**
 * Dynamic liquid glass surface color derived from glassAlpha and glassBaseLuminance.
 * Returns Color.Transparent when glassAlpha <= 0.001f (0% opacity).
 */
@Composable
fun liquidGlassSurfaceColor(): Color {
    val settings = LocalSettings.current
    val display = settings.displaySetting
    val alpha = display.glassAlpha.coerceIn(0f, 1f)
    if (alpha <= 0.001f) return Color.Transparent
    var base = androidx.compose.ui.graphics.lerp(Color.Black, Color.White, display.glassBaseLuminance.coerceIn(0f, 1f))
    // Depth / refraction scale modulates how much of the background the glass surface retains.
    val depth = display.glassDepthScale.coerceIn(0f, 2.5f)
    base = base.copy(alpha = (alpha * depth).coerceIn(0f, 1f))
    // Non-clear tint applies a wash over the glass surface (tint alpha = wash strength).
    val tint = display.glassTintColor
    val tintColor = Color(tint)
    val strength = tintColor.alpha.coerceIn(0f, 1f)
    if (strength > 0.02f) {
        base = androidx.compose.ui.graphics.lerp(base, tintColor, strength)
    }
    return base
}

/**
 * Dynamic specular border brush for glass surfaces. Clear Glass uses a 0.18f white specular rim;
 * DARK and FROSTED use subtler rim catches scaled to their surface brightness.
 */
@Composable
fun liquidGlassBorderBrush(): Brush {
    val settings = LocalSettings.current
    val display = settings.displaySetting
    // Specular rim catch and depth scale jointly drive rim brightness.
    val specular = display.glassSpecularIntensity.coerceIn(0f, 1f)
    val depth = display.glassDepthScale.coerceIn(0f, 2.5f)
    val intensity = (specular * depth).coerceIn(0f, 1f)
    return Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = 0.40f * intensity),
            Color.White.copy(alpha = 0.12f * intensity),
            Color.White.copy(alpha = 0.03f * intensity),
            Color.Transparent
        )
    )
}

/**
 * Dynamic chat bubble surface color derived from chatBubbleAlpha and chatBubbleTone.
 * Returns Color.Transparent when chatBubbleAlpha <= 0.001f (0% opacity).
 */
@Composable
fun liquidBubbleSurfaceColor(): Color {
    val settings = LocalSettings.current
    val display = settings.displaySetting
    val alpha = display.chatBubbleAlpha.coerceIn(0f, 1f)
    if (alpha <= 0.001f) return Color.Transparent
    val base = androidx.compose.ui.graphics.lerp(Color.Black, Color.White, display.chatBubbleTone.coerceIn(0f, 1f))
    return base.copy(alpha = alpha)
}

/**
 * Specular rim alpha for Clear Glass (0.18f white specular rim per design charter).
 */
val LiquidGlassSpecularRimAlpha = 0.18f

/**
 * Modifier that applies clean liquid glass surface optics without blurring foreground children.
 */
fun Modifier.liquidGlassSurface(
    shape: Shape = LiquidGlassTokens.CardShape,
    borderWidth: Dp = 0.5.dp,
    specularBrush: Brush = LiquidGlassTokens.SpecularBorderBrush,
    fillColor: Color = Color.Transparent,
    shadowElevation: Dp = 0.dp,
    glassMode: LiquidGlassMode = LiquidGlassMode.BALANCED_BLEND,
    isFloatingControl: Boolean = false
): Modifier {
    val withShadow = if (shadowElevation > 0.dp) {
        this.shadow(
            elevation = shadowElevation,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.25f),
            spotColor = Color.Black.copy(alpha = 0.35f)
        )
    } else {
        this
    }
    val withBg = if (fillColor != Color.Transparent && fillColor != Color.Unspecified) {
        withShadow.background(
            color = fillColor,
            shape = shape
        )
    } else {
        withShadow
    }
    return withBg
        .border(
            width = borderWidth,
            brush = specularBrush,
            shape = shape
        )
        .clip(shape)
}

/**
 * Liquid Glass Card Container. Keeps content, avatars, and text razor-sharp while providing optical glass depth.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassTokens.CardShape,
    borderWidth: Dp = 0.5.dp,
    specularBrush: Brush = LiquidGlassTokens.SpecularBorderBrush,
    fillColor: Color = Color.Transparent,
    glassMode: LiquidGlassMode = LiquidGlassMode.BALANCED_BLEND,
    isFloatingControl: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.liquidGlassSurface(
            shape = shape,
            borderWidth = borderWidth,
            specularBrush = specularBrush,
            fillColor = fillColor,
            glassMode = glassMode,
            isFloatingControl = isFloatingControl
        ),
        content = content
    )
}

/**
 * Fluid touch modifier that tracks press position and paints a subtle radial specular bloom
 * behind the user's thumb, evoking Convx-style liquid glass tactility.
 *
 * The bloom is a white radial gradient (alpha up to 0.15f) centered on the last press point,
 * fading out on release via a spring animation. It is drawn behind content so it never
 * obscures interactive elements.
 */
fun Modifier.convxFluidTouch(
    bloomAlpha: Float = 0.15f,
    bloomRadiusDp: Float = 40f
): Modifier = composed {
    val density = LocalDensity.current
    val bloomRadiusPx = with(density) { bloomRadiusDp.dp.toPx() }
    var touchX by remember { mutableFloatStateOf(-1f) }
    var touchY by remember { mutableFloatStateOf(-1f) }
    var bloomAlphaAnim by remember { mutableFloatStateOf(0f) }

    this
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    touchX = offset.x
                    touchY = offset.y
                    bloomAlphaAnim = bloomAlpha
                    tryAwaitRelease()
                    bloomAlphaAnim = 0f
                }
            )
        }
        .drawWithContent {
            drawContent()
            if (touchX >= 0f && touchY >= 0f && bloomAlphaAnim > 0f) {
                val bloomBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = bloomAlphaAnim),
                        Color.White.copy(alpha = bloomAlphaAnim * 0.3f),
                        Color.Transparent
                    ),
                    center = Offset(touchX, touchY),
                    radius = bloomRadiusPx
                )
                drawCircle(
                    brush = bloomBrush,
                    center = Offset(touchX, touchY),
                    radius = bloomRadiusPx
                )
            }
        }
}
