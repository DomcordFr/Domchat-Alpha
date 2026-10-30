package me.lucid.domchat.ui.components.chat

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import me.lucid.domchat.ui.context.LocalSettings
import me.lucid.domchat.ui.modifier.ApplyDialogWindowBlur
import me.lucid.domchat.ui.modifier.domChatBlurEffect
import me.lucid.domchat.ui.theme.LocalDarkMode
import me.lucid.domchat.ui.theme.AppShapes
import me.lucid.domchat.ui.theme.LiquidGlassTokens

/**
 * Unified sleek drag handle pill for all bottom sheets and popovers across DomChat.
 * Eliminates arrows/pointers and provides a consistent glassy touch affordance.
 */
@Composable
fun DomChatSheetDragHandle(
    modifier: Modifier = Modifier,
) {
    val isDark = LocalDarkMode.current
    Box(
        modifier = modifier
            .padding(top = 10.dp, bottom = 8.dp)
            .size(width = 38.dp, height = 4.5.dp)
            .clip(CircleShape)
            .background(if (isDark) Color.White.copy(alpha = 0.28f) else Color.Black.copy(alpha = 0.20f))
    )
}

/**
 * An in-window bottom sheet that shares the main Activity Compose canvas.
 * Inherits full hardware Haze blur over chat messages and wallpaper.
 * Supports smooth 120Hz entry/exit spring animations and interactive drag-down-to-dismiss.
 */
@Composable
fun DomChatInWindowBottomSheet(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.BottomSheet,
    dragHandle: @Composable (() -> Unit)? = {
        DomChatSheetDragHandle()
    },
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return

    ApplyDialogWindowBlur()

    val scope = rememberCoroutineScope()
    val motionPolicy = me.lucid.domchat.ui.motion.LocalMotionPolicy.current
    val isDark = LocalDarkMode.current
    val sheetBg = Color(0xFF000000)
    val borderBrush = LiquidGlassTokens.SpecularBorderSubtle

    var sheetHeightPx by remember { mutableFloatStateOf(1000f) }
    val offsetY = remember { Animatable(1200f) }
    val scrimAlpha = remember { Animatable(0f) }
    var isDismissing by remember { mutableStateOf(false) }

    fun dismissWithAnimation() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            launch {
                scrimAlpha.animateTo(
                    0f,
                    animationSpec = spring(dampingRatio = 0.88f, stiffness = 450f)
                )
            }
            offsetY.animateTo(
                targetValue = sheetHeightPx.coerceAtLeast(1200f),
                animationSpec = spring(dampingRatio = 0.84f, stiffness = 420f)
            )
            onDismissRequest()
        }
    }

    LaunchedEffect(Unit) {
        launch {
            scrimAlpha.animateTo(
                1f,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f)
            )
        }
        offsetY.animateTo(
            0f,
            animationSpec = spring(dampingRatio = 0.82f, stiffness = 400f)
        )
    }

    BackHandler(enabled = !isDismissing) {
        dismissWithAnimation()
    }

    val dragModifier = Modifier.pointerInput(sheetHeightPx) {
        detectVerticalDragGestures(
            onDragStart = {},
            onDragEnd = {
                scope.launch {
                    if (offsetY.value > sheetHeightPx * 0.20f || offsetY.value > 150f) {
                        dismissWithAnimation()
                    } else {
                        offsetY.animateTo(
                            0f,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = motionPolicy.pressStiffness(480f))
                        )
                    }
                }
            },
            onDragCancel = {
                scope.launch {
                    offsetY.animateTo(
                        0f,
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = motionPolicy.pressStiffness(480f))
                    )
                }
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                scope.launch {
                    val nextOffset = if (offsetY.value + dragAmount >= 0) {
                        offsetY.value + dragAmount
                    } else {
                        offsetY.value + dragAmount * 0.2f
                    }
                    offsetY.snapTo(nextOffset)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(200f)
    ) {
        // Scrim with interactive fade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val dragProgress = (1f - (offsetY.value / sheetHeightPx).coerceIn(0f, 1f))
                    this.alpha = scrimAlpha.value * dragProgress
                }
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { dismissWithAnimation() }
                )
        )

        // Bottom sheet card with 120Hz translation and drag-to-dismiss
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .onSizeChanged {
                        if (it.height > 0) {
                            sheetHeightPx = it.height.toFloat()
                        }
                    }
                    .graphicsLayer {
                        translationY = offsetY.value.coerceAtLeast(0f)
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .domChatBlurEffect(containerColor = sheetBg, shape = shape),
                shape = shape,
                color = Color.Transparent,
                border = BorderStroke(0.5.dp, borderBrush)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (dragHandle != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(dragModifier)
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            dragHandle()
                        }
                    }
                    content()
                }
            }
        }
    }
}

/**
 * A modal bottom sheet with hardware window blur and Apple-grade frosted container.
 * Creates an Android Window dialog that reliably fills the screen and never gets clipped by parent layouts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DomChatGlassBottomSheet(
    visible: Boolean = true,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    shape: Shape = AppShapes.BottomSheet,
    dragHandle: @Composable (() -> Unit)? = {
        DomChatSheetDragHandle()
    },
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!visible) return
    val isDark = LocalDarkMode.current
    val sheetBg = Color(0xFF000000)
    val contentColor = if (isDark) Color.White else Color.Black

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = sheetBg,
        contentColor = contentColor,
        shape = shape,
        scrimColor = Color.Black.copy(alpha = 0.28f),
        sheetGesturesEnabled = sheetGesturesEnabled,
        dragHandle = dragHandle,
        modifier = modifier,
    ) {
        ApplyDialogWindowBlur()
        content()
    }
}

    onDismissRequest = onDismissRequest,
    modifier = modifier,
    shape = shape,
    dragHandle = dragHandle,
    content = content
)
