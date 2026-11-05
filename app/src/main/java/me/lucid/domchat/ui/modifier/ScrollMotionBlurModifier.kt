package me.lucid.domchat.ui.modifier

import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import me.lucid.domchat.ui.context.LocalSettings

/**
 * Scroll motion blur modifier.
 * Applies a velocity-based blur effect when scrolling fast, gated by
 * [me.lucid.domchat.data.datastore.Settings.displaySetting.enableVelocityMotionBlur].
 * The blur intensity scales with [velocityMotionBlurIntensity].
 */
fun Modifier.scrollMotionBlur(
    maxBlur: Float = 8f,
): Modifier = composed {
    val settings = me.lucid.domchat.ui.context.LocalSettings.current
    val display = settings.displaySetting
    val enabled = display.enableVelocityMotionBlur
    val intensity = display.velocityMotionBlurIntensity.coerceIn(0f, 1f)

    if (!enabled || intensity <= 0f) {
        this
    } else {
        var scrollVelocity by remember { mutableStateOf(0f) }

        this
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { change, offset ->
                        change.consumed
                    },
                    onDrag = { change, offset ->
                        scrollVelocity = change.positionChange.magnitude
                        change.consumed
                    },
                    onDragEnd = { change ->
                        scrollVelocity = 0f
                    },
                )
            }
            .graphicsLayer {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val dynamicBlur = (scrollVelocity * intensity).coerceIn(0f, maxBlur)
                    if (dynamicBlur > 0.5f) {
                        renderEffect = android.graphics.RenderEffect.createBlurEffect(
                            0f, dynamicBlur, android.graphics.Shader.TileMode.CLAMP
                        ).asComposeRenderEffect()
                    }
                }
            }
            .drawWithContent {
                drawContent()
            }
    }
}

/**
 * Composable wrapper for scroll motion blur.
 * Applies velocity-sensitive blur to scrollable content when enabled in settings.
 */
@Composable
fun ScrollMotionBlurContent(
    modifier: Modifier = Modifier,
    maxBlur: Float = 8f,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .scrollMotionBlur(maxBlur),
        content = content,
    )
}
