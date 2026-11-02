# Audit Brief — Task 070: Jai Sort Rotating Dropdown, Remove Duplicate Presets Badge, Fix Post-History Overflow

## Intent Verification

| Requirement | File | Status |
|---|---|---|
| Jai hub sort button displays current `sortMode` (default 'Default') instead of static 'Sort' | `SettingJanitorPage.kt` | Done — `Text(text = sortMode, ...)` replaces `Text(text = "Sort", ...)` |
| Sort button has rotating arrow icon (`Icons.Rounded.KeyboardArrowDown` via `animateFloatAsState` based on `sortMenuOpen`) matching feedMode dropdown | `SettingJanitorPage.kt` | Done — `sortArrowRotation` state + `.rotate(sortArrowRotation)` on the arrow icon |
| Remove colorful `PresetBadge` from preset items in SettingPresetsPage (position shown by black glassy `posLabel` badge below) | `SettingPresetsPage.kt` | Done — `PresetBadge(...)` call removed from preset item header row |
| Remove unused `PresetBadge` composable | `SettingPresetsPage.kt` | Done — `PresetBadge` composable definition deleted |
| Injection position selector converted from 4-button row to dropdown with rotating chevron, current label, `DropdownMenu` | `SettingPresetsPage.kt` | Done — `Box` + `Surface` + `DropdownMenu` replaces `Row` of 4 pill buttons |
| Injection position selector converted from 4-button row to dropdown with rotating chevron, current label, `DropdownMenu` | `PromptPresetsBottomSheet.kt` | Done — same dropdown pattern applied to `formPosition` selector |
| AMOLED black background `Color(0xFF000000)` + `LiquidGlassTokens.SpecularBorderSubtle` | All 3 files | Done — dropdown surfaces use `Color(0xFF000000)` + `SpecularBorderSubtle` |
| All `when` branches on `PresetInjectionPosition` exhaustive | All 3 files | Done — all 4 enum values (SYSTEM_PROMPT, AT_DEPTH, BEFORE_LAST_USER, POST_HISTORY_INSTRUCTIONS) handled in every `when` |
| No `!!` force unwraps | All 3 files | Verified — grep returned zero matches |
| No broken Kotlin syntax / matching brackets | All 3 files | Verified — edits are syntactically balanced |

## Call Chain & Architecture

**SettingJanitorPage sort dropdown:**
- State: `sortMode` (`rememberSaveable { mutableStateOf("Default") }`), `sortMenuOpen`, `sortArrowRotation` (`animateFloatAsState` driven by `sortMenuOpen`)
- User taps sort `Surface` → `sortMenuOpen = true` → `DropdownMenu` expands
- Selecting an option → `sortMode = option`, `sortMenuOpen = false`, `loadBrowse(tab, page)` re-fetches with new sort
- Arrow rotates 0°→180° via `animateFloatAsState` when menu opens/closes, mirroring `feedArrowRotation` pattern
- Call depth: UI → `loadBrowse` → `withContext(Dispatchers.IO)` → `httpClient.execute` → JanitorAI API → `withContext(Dispatchers.Main)` → `browseState` update

**SettingPresetsPage injection position dropdown:**
- State: `draftPosition` (`PresetInjectionPosition`), `positionMenuOpen`, `positionArrowRotation`
- Tapping `Surface` → `positionMenuOpen = true` → `DropdownMenu` lists all 4 `PresetInjectionPosition.entries`
- Selecting → `draftPosition = pos`, `positionMenuOpen = false`
- Save → `saveDraft()` → `vm.updateSettings(...)` → ViewModel → Repository → DataStore

**PromptPresetsBottomSheet injection position dropdown:**
- Same pattern with `formPosition`, `positionMenuOpen`, `positionArrowRotation`
- Save → `savePreset()` → `settingsStore.update { ... }` → DataStore

**PresetBadge removal:**
- `SettingPresetsPage.kt`: `PresetBadge` call removed from item header; `PresetBadge` composable deleted (unused after removal)
- `PromptPresetsBottomSheet.kt`: `PresetBadge` retained (still used in preset list items); NOT removed per task scope (task #2 only targets `SettingPresetsPage.kt`)

**No business logic or network calls touched** — only UI presentation changes.

## Recomposition & Memory Safety

- All new state is `remember {}` / `rememberSaveable {}` / `animateFloatAsState` — lifecycle-aware, no naked objects
- `sortArrowRotation` / `positionArrowRotation` are `animateFloatAsState` — recompute only when `sortMenuOpen` / `positionMenuOpen` changes
- `DropdownMenu` is conditionally composed via `expanded = positionMenuOpen` / `expanded = sortMenuOpen` — when false, no menu nodes in composition
- `PresetBadge` removed from `SettingPresetsPage` — one fewer composable in preset item subtree, less recomposition work per item
- `PresetInjectionPosition.entries` is a static enum array — no per-frame allocation in dropdown item generation
- All `when` branches on `PresetInjectionPosition` are exhaustive (all 4 values handled) — no `else` fallthrough needed
- No `!!` force unwraps anywhere in the 3 files
- No `SnapshotStateList` passed to `LazyColumn.items` — `sortedFilteredItems` / `allPresets` are immutable `List` snapshots

## Threading & Dispatchers

- `SettingJanitorPage`: `loadBrowse` / `loadSearch` run in `scope.launch { ... }` (Main dispatcher from `rememberCoroutineScope`). Network IO via `withContext(Dispatchers.IO)` inside `httpClient.execute`. UI state updates (`browseState`, `searchState`, `page`, `isLoadingMore`) marshalled to `withContext(Dispatchers.Main)`. No `Dispatchers.Main` blocking, no `runBlocking`.
- `SettingPresetsPage` / `PromptPresetsBottomSheet`: dropdown interactions are pure UI state writes (`positionMenuOpen = false`, `draftPosition = pos`) inside `onClick` lambdas — no coroutines, no threading concerns
- Save flows: `vm.updateSettings` / `settingsStore.update` run inside existing `scope.launch` wrappers; `SettingsStore.update` performs DataStore IO internally on `Dispatchers.IO`
- No `java.util.concurrent` types used anywhere in the 3 files
- `animateFloatAsState` runs on the Compose UI thread (Main) — correct for animations

## Offline & Error State Handling

- **SettingJanitorPage**: Already handles offline/error via `JanitorLoadState.Error` with toast messages and existing-item retention on append failures. Sort dropdown change triggers `loadBrowse` which reuses the same error-handling path (HTTP non-2xx → Error state + toast; exception → Error state + toast). No new error paths introduced.
- **SettingPresetsPage / PromptPresetsBottomSheet**: Preset mutations write to `Settings` DataStore (local, always available) — fully offline-capable. `SettingsStore.update` is the canonical path (normalization + DataStore persist + in-memory state). Dropdown selection itself is local UI state — no persistence until Save is tapped.
- `PresetInjectionPosition` enum values are serialized with `@SerialName` — stable across versions; no migration concerns for adding `POST_HISTORY_INSTRUCTIONS`
- No new exception paths introduced; existing `try/catch` in network flows untouched
