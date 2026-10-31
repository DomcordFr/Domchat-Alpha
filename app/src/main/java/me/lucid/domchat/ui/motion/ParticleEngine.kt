package me.lucid.domchat.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Particle text reveal styles, persisted as an Int in DataStore.
 *  OFF     — old behavior, text simply appears via the fade machinery.
 *  GLASS   — per-glyph glow: fade + sink + sharpen, left-to-right wave (SpanStyle only).
 *  FIREFLY — one glowing mote per glyph flying in from the corner onto its letter.
 *  SWARM   — a micro-particle swarm per glyph; letters emerge from the arriving cloud.
 */
enum class ParticleTextMode(val id: Int) {
    OFF(0),
    GLASS(1),
    FIREFLY(2),
    SWARM(3);

    companion object {
        fun fromId(id: Int): ParticleTextMode = entries.firstOrNull { it.id == id } ?: GLASS
    }
}

fun Int.toParticleTextMode(): ParticleTextMode = ParticleTextMode.fromId(this)

/**
 * Message nodes present in the conversation at open time. Anything in this set
 * is history — it never animates. Only nodes that arrive during the session
 * (the user's new message and the bot's reply) play their one-shot entrance.
 */
val LocalPreexistingMessageNodes = androidx.compose.runtime.compositionLocalOf<Set<String>> { emptySet() }

/** Tunables for a swarm overlay. Small radii + high counts = "extremely small, a lot of them". */
data class SwarmSpec(
    val particlesPerTarget: Int,
    val radiusMinDp: Float,
    val radiusMaxDp: Float,
    // Scatter distance from the target, in dp.
    val scatterMinDp: Float,
    val scatterMaxDp: Float,
    // Per-particle curve bend (perpendicular offset of the bezier control point), dp.
    val bendMinDp: Float,
    val bendMaxDp: Float,
    // Sinusoidal wander amplitude along the path, dp.
    val wanderAmplitudeDp: Float,
    // 0 = converge from all directions; 1 = all launch from the overlay's top-left corner.
    val cornerBias: Float,
    val tint: Color = Color(0xFFEAF2FF),
)

/** Firefly: fewer, slightly larger motes that travel from the corner. */
val FireflySwarmSpec = SwarmSpec(
    particlesPerTarget = 1,
    radiusMinDp = 1.4f,
    radiusMaxDp = 2.2f,
    scatterMinDp = 36f,
    scatterMaxDp = 80f,
    bendMinDp = 10f,
    bendMaxDp = 34f,
    wanderAmplitudeDp = 6f,
    cornerBias = 1f,
)

/** Swarm: many micro particles converging from all directions with wandering paths. */
val MicroSwarmSpec = SwarmSpec(
    particlesPerTarget = 7,
    radiusMinDp = 0.5f,
    radiusMaxDp = 1.2f,
    scatterMinDp = 14f,
    scatterMaxDp = 52f,
    bendMinDp = 8f,
    bendMaxDp = 40f,
    wanderAmplitudeDp = 4f,
    cornerBias = 0f,
)

/** Hard cap so pathological streaming bursts never balloon the draw cost. */
private const val MAX_ACTIVE_PARTICLES = 420

/** One particle's full trajectory, precomputed per (key, target set) so drawing is cheap. */
private class SwarmParticle(
    val target: Offset,
    val start: Offset,
    val control: Offset,
    val phase: Float,          // 0..1 — launch stagger inside the cycle
    val wanderFreq: Float,
    val wanderPhase: Float,
    val wanderAmpPx: Float,
    val radiusPx: Float,
    val durationFactor: Float, // 0.75..1.15 — each particle takes its own time
)

private fun buildSwarm(
    targets: List<Offset>,
    spec: SwarmSpec,
    densityPxPerDp: Float,
    seed: Int,
): List<SwarmParticle> {
    if (targets.isEmpty()) return emptyList()
    val perTarget = spec.particlesPerTarget.coerceAtLeast(1)
    val budget = (MAX_ACTIVE_PARTICLES / targets.size).coerceAtLeast(1)
    val effectivePerTarget = perTarget.coerceAtMost(budget)
    val random = Random(seed)
    val corner = Offset(0f, 0f)
    val particles = ArrayList<SwarmParticle>(targets.size * effectivePerTarget)
    for (target in targets) {
        repeat(effectivePerTarget) {
            val scatterDp = spec.scatterMinDp + random.nextFloat() * (spec.scatterMaxDp - spec.scatterMinDp)
            val scatterPx = scatterDp * densityPxPerDp
            val angle = random.nextFloat() * 2f * PI.toFloat()
            var start = target + Offset(cos(angle) * scatterPx, sin(angle) * scatterPx)
            if (spec.cornerBias > 0f && random.nextFloat() < spec.cornerBias) {
                // Launch from the overlay corner instead: pull the start toward it.
                start = corner + (start - corner) * 0.35f
            }
            val mid = (start + target) * 0.5f
            val dir = target - start
            val perp = Offset(-dir.y, dir.x)
            val bendDp = spec.bendMinDp + random.nextFloat() * (spec.bendMaxDp - spec.bendMinDp)
            val control = mid + perp * (bendDp * densityPxPerDp) * (if (random.nextBoolean()) 1f else -1f) * 0.5f
            particles.add(
                SwarmParticle(
                    target = target,
                    start = start,
                    control = control,
                    phase = random.nextFloat() * 0.35f,
                    wanderFreq = 1.5f + random.nextFloat() * 2.5f,
                    wanderPhase = random.nextFloat() * 2f * PI.toFloat(),
                    wanderAmpPx = spec.wanderAmplitudeDp * densityPxPerDp,
                    radiusPx = (spec.radiusMinDp + random.nextFloat() * (spec.radiusMaxDp - spec.radiusMinDp)) * densityPxPerDp,
                    durationFactor = 0.75f + random.nextFloat() * 0.4f,
                )
            )
        }
    }
    return particles
}

private fun DrawScope.drawSwarm(particles: List<SwarmParticle>, globalProgress: Float, spec: SwarmSpec) {
    if (globalProgress <= 0f || globalProgress >= 1f) return
    for (particle in particles) {
        val localRaw = ((globalProgress - particle.phase) / particle.durationFactor).coerceIn(0f, 1f)
        if (localRaw <= 0f || localRaw >= 1f) continue
        // Slow ease-out so arrivals feel soft, never snappy.
        val eased = 1f - (1f - localRaw) * (1f - localRaw)
        // Quadratic bezier: curved, natural approach path.
        val inv = 1f - eased
        var pos = particle.start * (inv * inv) + particle.control * (2f * inv * eased) + particle.target * (eased * eased)
        // Perpendicular wander: particles fly around, not along the straight line.
        if (particle.wanderAmpPx > 0f) {
            val dir = particle.target - particle.start
            val len = dir.getDistance()
            if (len > 1f) {
                val perp = Offset(-dir.y / len, dir.x / len)
                val envelope = sin(localRaw * PI.toFloat()) // 0 at both ends
                pos += perp * (sin(localRaw * particle.wanderFreq * 2f * PI.toFloat() + particle.wanderPhase) * envelope * particle.wanderAmpPx)
            }
        }
        // Fade in fast, fade out as the particle "lands" and becomes the glyph.
        val alpha = when {
            localRaw < 0.15f -> localRaw / 0.15f
            localRaw > 0.82f -> (1f - localRaw) / 0.18f
            else -> 1f
        } * 0.9f
        if (alpha <= 0.01f) continue
        // Soft halo + bright core.
        drawCircle(
            color = spec.tint.copy(alpha = alpha * 0.22f),
            radius = particle.radiusPx * 3f,
            center = pos,
        )
        drawCircle(
            color = spec.tint.copy(alpha = alpha),
            radius = particle.radiusPx,
            center = pos,
        )
    }
}

/**
 * Canvas overlay that plays a swarm converging onto [targets] (in the overlay's own
 * coordinate space). Plays ONCE per [key] (EntrancePlayed registry), then never again.
 * Speed comes from [durationMs] (already scaled by the user's speed setting).
 */
@Composable
fun ParticleSwarmOverlay(
    key: String,
    active: Boolean,
    targets: List<Offset>,
    spec: SwarmSpec,
    durationMs: Int,
    modifier: Modifier = Modifier,
) {
    if (!active || targets.isEmpty()) return
    if (!EntrancePlayed.markIfNew("swarm:$key")) return
    val density = LocalDensity.current
    val particles = remember(key, targets) {
        buildSwarm(
            targets = targets,
            spec = spec,
            densityPxPerDp = density.density,
            seed = key.hashCode(),
        )
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(
            1f,
            animationSpec = tween(
                durationMillis = durationMs.coerceIn(300, 6000),
                easing = FastOutSlowInEasing,
            ),
        )
    }
    Canvas(modifier = modifier) {
        drawSwarm(particles, progress.value, spec)
    }
}

/**
 * Wraps arbitrary content (avatar, name, toolbar, token stats) with a one-shot
 * micro-swarm reveal: particles converge onto the content's area while the
 * content itself fades in underneath. Plays once per [key], never again.
 */
@Composable
fun ParticleRevealBox(
    key: String,
    enabled: Boolean,
    durationMs: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled || !EntrancePlayed.markIfNew("preveal:$key")) {
        Box(modifier = modifier) { content() }
        return
    }
    val density = LocalDensity.current
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    // Evenly distributed targets across the box area (not glyph-accurate — this is chrome).
    val targets = remember(key, boxSize) {
        val w = boxSize.width.toFloat()
        val h = boxSize.height.toFloat()
        if (w <= 0f || h <= 0f) {
            emptyList()
        } else {
            val cell = 10f * density.density
            val cols = (w / cell).toInt().coerceIn(3, 12)
            val rows = (h / cell).toInt().coerceIn(2, 6)
            buildList {
                for (r in 0 until rows) {
                    for (c in 0 until cols) {
                        add(Offset((c + 0.5f) * w / cols, (r + 0.5f) * h / rows))
                    }
                }
            }
        }
    }
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        progress.animateTo(
            1f,
            animationSpec = tween(durationMs.coerceIn(300, 6000), easing = FastOutSlowInEasing),
        )
    }
    val particles = remember(key, targets) {
        buildSwarm(
            targets = targets,
            spec = MicroSwarmSpec,
            densityPxPerDp = density.density,
            seed = key.hashCode(),
        )
    }
    Box(modifier = modifier.onSizeChanged { boxSize = it }) {
        Box(modifier = Modifier.graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) }) {
            content()
        }
        Canvas(modifier = Modifier.matchParentSize()) {
            drawSwarm(particles, progress.value, MicroSwarmSpec)
        }
    }
}
