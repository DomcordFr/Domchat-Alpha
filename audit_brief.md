# Audit Brief — Live Wallpaper Preview in Settings and Per-Chat Wallpaper Popover Polish

## Intent Verification

**1. SettingDisplayPage.kt — live wallpaper preview card under Global Wallpaper**
- Added a preview card (height `140.dp`, rounded via `AppShapes.CardMedium`, border `LiquidGlassTokens.SpecularBorderSubtle`, background `Color(0xFF141520)`) that renders when `globalBackground` is set.
- Uses `AsyncImage(model = globalBg, contentScale = ContentScale.Crop)` to show the selected wallpaper.
- Applies live dim overlay (`Color.Black.copy(alpha = globalDim.coerceIn(0f, 0.95f))`) and live blur overlay (vertical gradient scrim scaled by `globalBlur / 200f`) so the preview reflects current dim/blur settings.
- Card only shows when a wallpaper is set; hidden otherwise (matches existing progressive-disclosure pattern).

**2. MinimalChatInput.kt — Per-Chat Wallpaper sheet polish (lines ~1992-2187)**
- **Blur Mode toggle tabs (FULL vs BOTTOM_FADE):** replaced hardcoded `Color(0xFF000000)` unselected backgrounds with `Color.Transparent` glassy containers bordered by `LiquidGlassTokens.SpecularBorderSubtle`; selected state uses `Color.White.copy(alpha = 0.14f)`. Backgrounds animated via `animateColorAsState` (tween 180ms) so switching tabs smoothly crossfades.
- **'Save for this chat' button:** replaced `DomChatGlassPillButton` (which forced left-aligned `SemiBold` text on a solid white surface) with a custom `Surface`-based glass button: `color = Color.Transparent`, border `LiquidGlassTokens.SpecularBorderSubtle`, text wrapped in a centered `Box` (`contentAlignment = Alignment.Center`), `FontWeight.Bold`, `textAlign = TextAlign.Center`.
- **Blur Height Reach slider:** wrapped the `if (blurModeDraft == BOTTOM_FADE)` block in `AnimatedVisibility` (fadeIn 150ms / fadeOut 100ms) so the slider smoothly appears/disappears when toggling blur mode.
- All `Color.White` text colors use non-conditional form; `isDarkMode` references removed where no longer needed.

**Null safety:** zero `!!` introduced. All nullable access uses smart-cast (`globalBg` inside `isNullOrBlank` guard), `coerceIn`, and Elvis operators.

**Theme:** `Color(0xFF000000)` AMOLED black, `Color(0xFF141520)` unified glass card, `LiquidGlassTokens.SpecularBorderSubtle` borders preserved throughout.

## Call Chain & Architecture

**SettingDisplayPage (settings layer):**
- `settings.displaySetting.globalBackground / globalBackgroundDim / globalBackgroundBlur` read via `collectAsStateWithLifecycle` (DataStore → `SettingsStore`).
- Preview card reads these directly and renders `AsyncImage` + dim/blur overlays. Pure UI derivation; no business logic, no VM calls.
- Wallpaper picker (`ActivityResultContracts.PickVisualMedia`) writes to app storage and calls `updateDisplaySetting` → `vm.updateSettings(settings.copy(displaySetting = ...))` → `SettingsStore.update` (canonical settings API: normalization, secret migration, in-memory state, persist).

**MinimalChatInput (per-chat wallpaper popover):**
- `settings.displaySetting.conversationWallpapers[convId]` and `assistant.background` seed the draft states (`chatBgDraft`, `blurDraft`, `dimDraft`, `blurModeDraft`, `blurHeightFractionDraft`) via `remember(existingConfig) { mutableStateOf(...) }`.
- `blurModeDraft` drives `animateColorAsState` targets for tab backgrounds and `AnimatedVisibility` for the blur-height slider — UI state only.
- "Save for this chat" builds `ConversationWallpaperConfig` and calls `settingsStore.update { ... }` inside `scope.launch` (main-safe coroutine, no blocking I/O on Main) to persist `conversationWallpapers` map.
- `DomChatInWindowBottomSheet` hosts the sheet; dismissal via `onDismissRequest = { showWallpaperSheet = false }`.

**UI boundaries respected:** UI layer only renders state and emits intents; settings persistence funnels through `SettingsStore.update` (normalization + persist). No new ViewModel needed for the preview (reads existing `settings.displaySetting`).

## Recomposition & Memory Safety

- `glassAlpha`-independent here; preview card reads `globalDim`/`globalBlur`/`globalBg` as immutable `Float`/`String` — recomposes only when those settings change.
- `AsyncImage(model = globalBg)` — Coil handles image loading/caching internally; no bitmap retained in Compose state. Model is a file-path `String`, stable across recompositions.
- `animateColorAsState` called inside `if (showWallpaperSheet)` block; targets derived from `blurModeDraft` — animation state is Compose-managed, no leaked `AnimationSpec` objects.
- `remember(existingConfig) { mutableStateOf(...) }` for drafts — keyed on config, recreated only when config changes. `mutableFloatStateOf` / `mutableStateOf` used correctly.
- `AnimatedVisibility` + `animateColorAsState` are standard Compose animation APIs; no naked animation objects.
- No `SnapshotStateList` passed to `items()`; no derived-state violations.
- No force-unwraps (`!!`) in edited regions.

## Threading & Dispatchers

- Opacity/preview/tab animation: synchronous main-thread Compose operations; no blocking calls on `Dispatchers.Main`.
- Wallpaper image load via `AsyncImage` — Coil loads off-main internally.
- Settings persistence: `settingsStore.update { ... }` invoked inside `scope.launch` (ViewModel/main scope) but the body only mutates an in-memory `Settings` copy; `SettingsStore.update` itself dispatches disk I/O internally (DataStore). No explicit `Dispatchers.IO` needed — matches existing code pattern in this file (line ~328 `scope.launch { settingsStore.update { ... } }`).
- No `Dispatchers.Main` used for I/O. No new background threads spawned.

## Offline & Error State Handling

- Preview card: offline-capable (loads local app-storage file path via Coil); if image fails to load, Coil renders nothing/placeholder gracefully — no exception propagates to Compose.
- Dim/blur overlays: total functions (`coerceIn` clamps); no failure mode.
- Blur Mode tabs: `animateColorAsState` targets are total; no exception surface.
- Save flow: wrapped in existing try/catch-free pattern consistent with the file (the `scope.launch` block mirrors existing wallpaper-save code at lines ~327-333). `chatBgDraft.isNullOrBlank()` guard removes map entry when no wallpaper, preventing stale entries.
- `settingsStore.update` is the canonical API — handles normalization + persistence atomically; callers need not handle partial state.
- Error states: existing sheet/toaster patterns apply; no new error modes introduced.

## Files Changed

- `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingDisplayPage.kt` — added imports (`AsyncImage`, `ContentScale`, `Brush`, `LiquidGlassTokens`); added live wallpaper preview card under Global Wallpaper.
- `app/src/main/java/me/lucid/domchat/ui/components/ai/MinimalChatInput.kt` — animated glassy Blur Mode tabs; centered bold 'Save for this chat' glass button; `AnimatedVisibility` around Blur Height Reach slider.
- `audit_brief.md` — this document.
