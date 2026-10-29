package me.lucid.domchat.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Canon colors for every settings input field: solid black container, glass-grey
 * ladder, and a hairline specular white border that reads as glass on black.
 * Shared everywhere so focus/blur states stay consistent across the app.
 */
fun appFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF000000),
    unfocusedContainerColor = Color(0xFF000000),
    disabledContainerColor = Color(0xFF000000),
    focusedTextColor = Color(0xFFF2F2F7),
    unfocusedTextColor = Color(0xFFF2F2F7),
    disabledTextColor = Color(0xFF8A8A96),
    cursorColor = Color(0xFFF2F2F7),
    cursorErrorColor = Color(0xFFEF4444),
    focusedBorderColor = Color.White.copy(alpha = 0.30f),
    unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
    disabledBorderColor = Color.White.copy(alpha = 0.06f),
    errorBorderColor = Color(0xFFEF4444),
    focusedLabelColor = Color(0xFFF2F2F7),
    unfocusedLabelColor = Color(0xFF8A8A96),
    focusedPlaceholderColor = Color(0xFF8A8A96),
    unfocusedPlaceholderColor = Color(0xFF8A8A96),
    disabledPlaceholderColor = Color(0xFF8A8A96),
    focusedLeadingIconColor = Color(0xFF8A8A96),
    unfocusedLeadingIconColor = Color(0xFF8A8A96),
    focusedSupportingTextColor = Color(0xFF8A8A96),
    unfocusedSupportingTextColor = Color(0xFF8A8A96),
    errorSupportingTextColor = Color(0xFFEF4444),
)

/** Default input shape for settings fields (12dp corners, matches search bars). */
val AppFieldShape = RoundedCornerShape(12.dp)

/**
 * Canon colors for FilterChip-style selection buttons: black plate, glass
 * selected fill (white @ 16%), specular borders. Used by Chats/Files toggles,
 * Text/Image modality picks and any similar segmented selection.
 */
fun appFilterChipColors(selected: Boolean): androidx.compose.material3.SelectableChipColors =
    androidx.compose.material3.FilterChipDefaults.filterChipColors(
        containerColor = Color(0xFF000000),
        labelColor = Color(0xFF8A8A96),
        iconColor = Color(0xFF8A8A96),
        selectedContainerColor = Color.White.copy(alpha = 0.16f),
        selectedLabelColor = Color(0xFFF2F2F7),
        selectedIconColor = Color(0xFFF2F2F7),
        disabledContainerColor = Color(0xFF000000),
        disabledLabelColor = Color(0xFF8A8A96),
    )

fun appFilterChipBorder(selected: Boolean): androidx.compose.foundation.BorderStroke =
    androidx.compose.foundation.BorderStroke(
        0.5.dp,
        if (selected) Color.White.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.12f)
    )

/**
 * Canon colors for SegmentedButton selection groups (OpenAI/Google/Claude tabs,
 * Text/Image/... modality rows): black track, glass active segment, specular
 * border ladder.
 */
fun appSegmentedButtonColors() = androidx.compose.material3.SegmentedButtonDefaults.colors(
    containerColor = Color(0xFF000000),
    activeContainerColor = Color.White.copy(alpha = 0.16f),
    activeContentColor = Color(0xFFF2F2F7),
    activeBorderColor = Color.White.copy(alpha = 0.38f),
    inactiveContainerColor = Color(0xFF000000),
    inactiveContentColor = Color(0xFF8A8A96),
    inactiveBorderColor = Color.White.copy(alpha = 0.12f),
    disabledActiveContainerColor = Color(0xFF000000),
    disabledInactiveContainerColor = Color(0xFF000000),
)
