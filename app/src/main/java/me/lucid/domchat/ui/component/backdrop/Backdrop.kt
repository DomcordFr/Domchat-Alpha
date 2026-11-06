/*
 * Ported from Convx (com.convx.music.ui.component.backdrop.Backdrop)
 * Originally vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Adapted package name com.convx.music.ui.component.backdrop -> me.lucid.domchat.ui.component.backdrop.
 */
package me.lucid.domchat.ui.component.backdrop

import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density

interface Backdrop {

    val isCoordinatesDependent: Boolean

    fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)? = null
    )
}

/** Application-wide backdrop captured from the chat wallpaper + content. */
val LocalAppBackdrop = androidx.compose.runtime.staticCompositionLocalOf<Backdrop?> { null }
