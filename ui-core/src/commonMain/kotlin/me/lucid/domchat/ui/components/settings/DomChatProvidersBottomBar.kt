package me.lucid.domchat.ui.components.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class DomChatProviderTab(
    val id: String,
    val icon: ImageVector,
)

/** Android's production compact Models/Search/TTS bottom navigation. */
@Composable
fun DomChatProvidersBottomBar(
    tabs: List<DomChatProviderTab>,
    selectedId: String,
    useWideLayout: Boolean,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onHaptic: () -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier = modifier.fillMaxWidth().navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        if (!useWideLayout) {
            val selectedIndex = tabs.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
            val targetOffsetDp = (selectedIndex * 52).dp // 48dp item size + 4dp space
            val animatedIndicatorOffset by animateDpAsState(
                targetValue = targetOffsetDp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF141520),
                border = BorderStroke(
                    0.5.dp,
                    Brush.verticalGradient(listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent,
                    ))
                ),
                shadowElevation = 8.dp,
            ) {
                Box {
                    // Animated sliding puck behind icons
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .offset(x = animatedIndicatorOffset)
                            .size(48.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    )
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        tabs.forEach { tab ->
                            val selected = tab.id == selectedId
                            val iconTint by animateColorAsState(
                                targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.65f),
                                animationSpec = tween(200)
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (tab.id != selectedId) {
                                                onHaptic()
                                                onSelect(tab.id)
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier.align(Alignment.BottomEnd),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = actions,
        )
    }
}
