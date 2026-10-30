package me.lucid.domchat.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Process-lifetime registry of entrances that already played, keyed by stable
 * element id. Guarantees: opening a chat animates its visible turns once,
 * scrolling back and forth never replays, recompositions don't replay.
 */
object EntrancePlayed {
    // Bounded: a long chat stream could otherwise grow this without limit.
    private const val MAX = 4096
    private val played = mutableSetOf<String>()

    fun markIfNew(key: String): Boolean {
        if (played.size >= MAX) played.clear()
        return played.add(key)
    }

    /** Clear the registry so entrances replay on the next conversation open. */
    fun reset() {
        played.clear()
    }
}

/** Screen-space entrance direction, chosen by the caller from element position. */
enum class EntranceDirection { UP, DOWN, LEFT, RIGHT, NONE }

/**
 * One-shot entrance: slide-in from [from] toward rest + alpha fade, staggered
 * via [delayMs]. All animation is transform/opacity (graphicsLayer) so it
 * stays on the GPU and never triggers layout or draw invalidation. Skipped
 * entirely under reduce motion and for elements whose entrance already
 * played (kept smooth by never re-animating).
 */
@Composable
fun Modifier.entrance(
    key: String,
    motionPolicy: MotionPolicy,
    from: EntranceDirection = EntranceDirection.UP,
    delayMs: Int = 0,
    distance: Dp = 30.dp,
    allow: Boolean = true,
): Modifier {
    if (
        !allow ||
        motionPolicy.reduceMotion ||
        !EntrancePlayed.markIfNew(key)
    ) {
        return this
    }
    val progress = remember(key) { Animatable(0f) }
    val distancePx = with(LocalDensity.current) { distance.toPx() }
    LaunchedEffect(key) {
        progress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = motionPolicy.duration(ENTRANCE_DURATION_MS),
                delayMillis = motionPolicy.duration(delayMs.coerceIn(0, ENTRANCE_MAX_STAGGER_MS)),
                easing = FastOutSlowInEasing
            )
        )
    }
    return this.graphicsLayer {
        val p = progress.value
        alpha = p
        val d = distancePx * (1f - p)
        when (from) {
            EntranceDirection.UP -> translationY = d
            EntranceDirection.DOWN -> translationY = -d
            EntranceDirection.LEFT -> translationX = -d
            EntranceDirection.RIGHT -> translationX = d
            EntranceDirection.NONE -> Unit
        }
    }
}

private const val ENTRANCE_DURATION_MS = 700
private const val ENTRANCE_MAX_STAGGER_MS = 400
