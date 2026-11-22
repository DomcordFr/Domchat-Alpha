package me.lucid.domchat.ui.context

import androidx.compose.runtime.compositionLocalOf
import me.lucid.domchat.data.datastore.Settings

val LocalSettings = compositionLocalOf<Settings> {
    error("No SettingsStore provided")
}
