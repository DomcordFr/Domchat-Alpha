/*
 * Ported from Convx (com.convx.music.ui.component.backdrop.backdrops.LayerBackdrop)
 * Originally vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Adapted package name com.convx.music.ui.component.backdrop -> me.lucid.domchat.ui.component.backdrop.
 */
package me.lucid.domchat.ui.component.backdrop.backdrops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Density
import me.lucid.domchat.ui.component.backdrop.Backdrop

private val DefaultOnDraw: ContentDrawScope.() -> Unit = { drawContent() }

@Composable
fun rememberLayerBackdrop(
    graphicsLayer: GraphicsLayer = rememberGraphicsLayer(),
    onDraw: ContentDrawScope.() -> Unit = DefaultOnDraw
): LayerBackdrop {
    return remember(graphicsLayer, onDraw) {
        LayerBackdrop(graphicsLayer, onDraw)
    }
}

@Stable
class LayerBackdrop internal constructor(
    val graphicsLayer: GraphicsLayer,
    internal val onDraw: ContentDrawScope.() -> Unit
) : Backdrop {

    override val isCoordinatesDependent: Boolean = true

    internal var layerCoordinates: LayoutCoordinates? by mutableStateOf(null)

    /**
     * Changes after the source layer is recorded. Samplers can read this plain
     * field during drawing without creating a snapshot invalidation loop.
     * Their recorded layers reference the source RenderNode, so content updates
     * appear without re-recording each sampler.
     */
    internal var contentVersion: Int = 0
        private set

    /** Called by the recording node after a successful record. */
    internal fun contentRecorded() {
        if (contentVersion == 0) geometryChanged()
        contentVersion++
    }

    /**
     * Changes when a sampler's source geometry changes, so its layer must be
     * recorded again. Content redraws update the referenced RenderNode directly.
     */
    internal var geometryVersion: Int = 0
        private set

    internal fun geometryChanged() {
        geometryVersion++
    }

    /** True once the source has been recorded at least once. */
    internal val hasRecording: Boolean get() = contentVersion > 0

    /** Fraction of screen resolution [graphicsLayer] was recorded at. */
    internal var sourceScale: Float = 1f

    /** Where [graphicsLayer]'s top-left sits in the source's coordinate space (unscaled px). */
    internal var sourceOrigin: Offset = Offset.Zero

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        val coordinates = coordinates ?: return
        val layerCoordinates = layerCoordinates ?: return
        val offset =
            try {
                layerCoordinates.localPositionOf(coordinates)
            } catch (_: Exception) {
                // TODO: Account for outer transforms in this fallback calculation.
                coordinates.positionInWindow() - layerCoordinates.positionInWindow()
            }
        val origin = sourceOrigin
        if (sourceScale != 1f) {
            scale(1f / sourceScale, 1f / sourceScale, Offset.Zero) {
                translate(origin.x - offset.x, origin.y - offset.y) {
                    drawLayer(graphicsLayer)
                }
            }
        } else {
            translate(origin.x - offset.x, origin.y - offset.y) {
                drawLayer(graphicsLayer)
            }
        }
    }
}
