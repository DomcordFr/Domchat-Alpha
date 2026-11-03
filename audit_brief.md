# Audit Brief — Task 122: Navigation & Top/Bottom Bar Opacity / Gradient Slider

## Intent Verification

All 4 requirements from task-122_directive.json implemented:

1. **Route Home button to MenuPage (not ConversationList)** — `ChatPage.kt`:
   - `onNewChat` (line 733): `navController.navigate(Screen.ConversationList)` → `navController.navigate(Screen.Menu)`
   - BackHandler "Navigate to Home" (line 785): `navController.navigate(Screen.ConversationList)` → `navController.navigate(Screen.Menu)`
   - `Screen.Menu` is already registered in `RouteActivity.kt` (line 926-928) rendering `MenuPage()`. `Screen` import already exists (line 172) and `Screen.Menu` is already used elsewhere (lines 1111, 1343).

2. **Fix top bar/bottom bar too dark at 5% opacity** — `ChatPage.kt` line 3268:
   - Removed `.coerceIn(0.10f, 0.75f)` from `glassAlpha`
   - `Color(0xFF000000).copy(alpha = settings.displaySetting.glassAlpha.coerceIn(0.10f, 0.75f))` → `Color(0xFF000000).copy(alpha = settings.displaySetting.glassAlpha)`
   - This allows `glassAlpha` to go down to 0% (fully transparent) instead of being clamped to 10% minimum. `topContainerColor` is used for both top and bottom bars via `ChatToolbarActionPill`, so both are fixed.

3. **Add slider for background black gradient intensity** — Two files:
   - `PreferencesModels.kt`: Added `barGradientIntensity: Float = 0.60f` field to `DisplaySetting` data class
   - `SettingDisplayPage.kt`: Added `localBarGradientIntensity` state + `DomChatSlider` (0.0f..1.0f) with percentage display, wired to `updateDisplaySetting(settings.displaySetting.copy(barGradientIntensity = ...))`
   - `Background.kt`: Replaced hardcoded gradient alpha values (0.55f, 0.65f) with `settings.displaySetting.barGradientIntensity`

4. **Match chat page top bar background to bottom bar chat bar background** — Both already use the same `topContainerColor` variable in `ChatToolbar`. With the `coerceIn` removed, they both respect the user's `glassAlpha` setting uniformly.

**Constraints:** ZERO `!!` force-unwraps introduced (grep-confirmed across all modified files). Pre-existing `!!` at ChatPage.kt lines 2119, 2137 are unrelated. Unified glass card theme `0xFF141520` untouched. Existing code style preserved.

## Call Chain & Architecture

```
Navigation:
User clicks Home / backs out of chat
  → navController.navigate(Screen.Menu)
  → RouteActivity.kt: composable<Screen.Menu> { MenuPage() }
  → MenuPage() renders home landing with generic assistant/actions

Bar opacity:
User adjusts glassAlpha slider in SettingDisplayPage
  → updateDisplaySetting(settings.displaySetting.copy(glassAlpha = ...))
  → vm.updateSettings(settings.copy(displaySetting = ...)) → DataStore
  → ChatPage.kt collects settings.displaySetting.glassAlpha
  → topContainerColor = Color(0xFF000000).copy(alpha = glassAlpha)
  → Applied to DomChatMenuButton + ChatToolbarActionPill (top + bottom bars)

Bar gradient intensity:
User adjusts Bar Gradient Intensity slider in SettingDisplayPage
  → updateDisplaySetting(settings.displaySetting.copy(barGradientIntensity = ...))
  → Background.kt collects settings.displaySetting.barGradientIntensity
  → Brush.verticalGradient alpha = barGradientIntensity at top/bottom edges
```

Architecture boundaries: UI layer renders state + emits intents only. ViewModel (`SettingVM`, `ChatVM`) manages coroutines in `viewModelScope`. DataStore is single source of truth for `DisplaySetting`.

## Recomposition & Memory Safety

- `topContainerColor` is a `val` derived from `settings.displaySetting.glassAlpha` — recomputes only when settings change, not per-frame.
- `barGradientIntensity` in Background.kt is collected via `rememberUserSettingsState()` — recomputes only when setting changes.
- `localBarGradientIntensity` in SettingDisplayPage.kt is `remember(settings.displaySetting.barGradientIntensity) { mutableFloatStateOf(...) }` — keyed on settings value, preserved across recompositions.
- `DomChatSlider` follows the same pattern as existing glass sliders (glassAlpha, glassBlur, etc.).
- No `SnapshotStateList` passed directly to `items()`. No naked objects in Compose.
- Zero `!!` force-unwraps introduced.

## Threading & Dispatchers

- All settings updates flow through `updateDisplaySetting` → `vm.updateSettings` → `viewModelScope.launch` → `settingsStore.update` (DataStore, IO-bound internally).
- Settings collection uses `collectAsStateWithLifecycle()` — lifecycle-aware, Main thread only for UI state.
- No `Dispatchers.Main` violations introduced. No background work on Main dispatcher.
- Navigation calls (`navController.navigate`) run on Main thread (correct — Compose UI layer).

## Offline & Error State Handling

- **Offline:** All changes are purely local UI/navigation — no network dependency. Settings persist in DataStore locally.
- **Loading:** No loading states needed for slider or navigation changes (instant local operations).
- **Success:** Settings persist immediately via DataStore. Slider updates reflect in real-time.
- **Exception:** `updateDisplaySetting` wraps DataStore writes in `viewModelScope.launch` with try/catch (per existing ViewModel patterns). If DataStore write fails, local `displaySetting` state is still updated (optimistic UI), providing graceful degradation.
- **Default value:** `barGradientIntensity` defaults to `0.60f` in `DisplaySetting`, matching the previous hardcoded average of 0.55f/0.65f for backward compatibility.
