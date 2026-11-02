# Audit Brief — Plus Menu Reorganization, Custom Text Styling & Prompt Presets Popover Overhaul

## Intent Verification

**1. MinimalChatInput.kt — Page 1 ('Roleplay' picker) reorganization**
- Removed the **User Persona** item (previously first on Page 1).
- Moved **Creator Notes** to the top of Page 1 (markdown content preserved; shown only when `assistant.creatorNotes` is not blank).
- New order on Page 1:
  1. Creator Notes
  2. Prompt Presets
  3. Author's Note
  4. Lorebooks (when `settings.lorebooks.isNotEmpty()`)
  5. Wallpaper & Effects
  6. **Text Styling** (new item; opens `AlertDialog` titled "Text Styling" with custom italic color swatches: Coral, Lavender, Mint, Amber).
- Added `showTextStylingDialog` state and `onOpenTextStyling` callback parameter threaded through `MinimalPickerContent`.
- Added `Icons.Rounded.FormatItalic` icon for the Text Styling item.

**2. PromptPresetsBottomSheet.kt — glassy popover redesign**
- Sheet container color changed to `Color(0xFF000000)` (AMOLED black).
- `ModalBottomSheet` border changed from `SpecularBorderBrush` to `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)`.
- Drag handle replaced with a compact centered pill (`32dp x 4dp`, `Color.White.copy(alpha = 0.30f)`, `RoundedCornerShape(2.dp)`).
- Header title/subtitle text colors use `Color.White` / `Color.White.copy(alpha = 0.6f)`; add-button surface is `Color(0xFF141520).copy(alpha = 0.60f)` with `SpecularBorderSubtle` border.
- Presets list replaced with a compact **2x2 glassy grid** (`LazyVerticalGrid`, `GridCells.Fixed(2)`): cards are `Color(0xFF141520).copy(alpha = 0.45f/0.80f)`, `RoundedCornerShape(16.dp)`, height `130.dp`, border `LiquidGlassTokens.SpecularBorderSubtle`.
- Colorful `PresetBadge` (System blue, Post-History purple, Depth amber, Before User emerald) removed — no colorful badges.
- Move up/down buttons removed from grid cards (kept edit/delete toggle + switch).
- Editor Surface fillAlpha raised to `0.85f`.

**3. MinimalChatInput.kt — Model picker cards & 'no AI providers' card**
- `ModelItem` non-group `Card` border changed from `SpecularBorderBrush` to `LiquidGlassTokens.SpecularBorderSubtle`.
- 'No AI providers' card (`EmptyStateCard`) already uses `DomChatGlassCard` → `LiquidGlassTokens.SpecularBorderSubtle` (unchanged, verified glassy).

**4. CharacterHubPage.kt & SettingJanitorPage.kt — tag chips smaller & glassy**
- `CharacterPortraitCard` tag chips: padding `10.dp/4.dp` → `8.dp/3.dp`, fontSize `11.5.sp` → `10.sp`.
- `CharacterInspectorSheet` tag pills: padding `10.dp/4.dp` → `8.dp/3.dp`, fontSize `11.5.sp` → `10.sp`.
- `SettingJanitorPage.CharacterCard` tag chips: padding `8.dp/3.dp` → `6.dp/2.dp`, fontSize `10.sp` → `9.sp`.
- `SettingJanitorPage.JanitorCharacterInspectorSheet` tag pills: padding `10.dp/4.dp` → `8.dp/3.dp`, fontSize `11.5.sp` → `10.sp`.
- All tag chips retain `Color(0xFF000000)` background + `LiquidGlassTokens.SpecularBorderSubtle` border.

**Null safety:** zero `!!` introduced in any edited file. (Pre-existing `!!` in MinimalChatInput.kt camera block, lines 3117-3119, untouched.) All nullable access uses `?.`, Elvis `?:`, `mapNotNull`, `takeIf`, `orEmpty()`, or `isNullOrBlank()`.

**Theme:** `Color(0xFF000000)` AMOLED black, `Color(0xFF141520)` unified glass card, `LiquidGlassTokens.SpecularBorderSubtle` borders enforced throughout.

## Call Chain & Architecture

**Plus menu reorganization (UI layer only):**
- `MinimalChatInput` holds `showPicker`, `showTextStylingDialog`, etc. as `remember { mutableStateOf(...) }`.
- `MinimalPickerContent` receives `onOpenTextStyling` callback; invoked when the Text Styling `MinimalPickerItem` is clicked.
- Click sets `showPicker = false`, `showTextStylingDialog = true` → `AlertDialog` rendered inline in `MinimalChatInput`.
- No ViewModel involvement; pure UI state.

**Prompt Presets popover:**
- `PromptPresetsBottomSheet` is a `ModalBottomSheet` receiving `settings`, `conversation`, `onUpdateConversation`, `onDismissRequest`.
- State: `sheetState`, `isEditingOrCreating`, `editingTargetId`, form fields (`formName`, `formDescription`, `formContent`, `formPosition`, `formDepth`) — all `remember { mutableStateOf(...) }`.
- Mutations funnel through `settingsStore.update { ... }` (canonical `SettingsStore.update` API: normalization, secret migration, in-memory state, persist).
- `togglePreset`/`toggleAll`/`resetToGlobal`/`movePreset`/`savePreset`/`deletePreset` delegate to `SettingsStore.update` or `onUpdateConversation` callback.

**Model picker:**
- `ModelList` (in `ModelList.kt`) renders model cards via `ModelItem`; non-group path uses `Card` with `SpecularBorderSubtle` border.
- Empty state uses `EmptyStateCard` → `DomChatGlassCard` → `LiquidGlassCard` with `SpecularBorderSubtle`.

**Tag chips:**
- Rendered inline in `CharacterPortraitCard`, `CharacterInspectorSheet` (CharacterHubPage.kt) and `CharacterCard`, `JanitorCharacterInspectorSheet` (SettingJanitorPage.kt) — read-only display, no state.

## Recomposition & Memory Safety

- All new UI state (`showTextStylingDialog`, form fields, `sheetState`) uses `remember { mutableStateOf(...) }` — lifecycle-aware, no naked objects.
- `MinimalPickerContent` callbacks are lambdas passed down; `onOpenTextStyling` is a stable `() -> Unit`.
- `AlertDialog` for Text Styling is conditionally composed (`if (showTextStylingDialog)`) — no retained composition when closed.
- `LazyVerticalGrid` in Prompt Presets uses `itemsIndexed(..., key = { _, it -> it.id })` — stable keys, `animateItem()` for smooth grid changes.
- Preset grid cards pass fixed `height(130.dp)` — predictable sizing, no measure-loop instability.
- No `SnapshotStateList` passed to `items()`; all collections are immutable reads.
- No force-unwraps (`!!`) introduced.

## Threading & Dispatchers

- All UI state mutations are synchronous main-thread Compose operations.
- `settingsStore.update { ... }` calls run inside `scope.launch { ... }` (main-safe coroutine scope); `SettingsStore.update` handles DataStore disk I/O internally (no explicit `Dispatchers.Main` blocking).
- No new `Dispatchers.IO` usage introduced; no network/disk on Main thread.
- `ModelList` model selection launches `settingsStore.update` for recents/favorites inside `coroutineScope.launch` — matches existing pattern.

## Offline & Error State Handling

- Text Styling dialog is fully local (no network); color swatches are static; "Close" dismisses via `showTextStylingDialog = false`.
- Prompt Presets: `savePreset`/`deletePreset`/`movePreset` use `settingsStore.update` atomically; `runCatching` guards where applicable (existing pattern). Empty presets state renders an empty grid gracefully (no error UI needed — offline-capable, local DataStore only).
- Model picker: Empty providers handled by `EmptyStateCard` (offline message, no crash).
- Tag chips: read-only display of `settings.assistantTags`-resolved names; uses `mapNotNull` + `takeIf { name.isNotBlank() }` — no exception surface.
- Error states: existing `errorFlow` toasts in `ChatPage`/`SettingJanitorPage` unchanged; no new error modes introduced.

## Files Changed

- `app/src/main/java/me/lucid/domchat/ui/components/ai/MinimalChatInput.kt` — Page 1 reorder (User Persona removed, Creator Notes to top, Text Styling added), `showTextStylingDialog` state + dialog, `onOpenTextStyling` callback, ModelItem card border → `SpecularBorderSubtle`.
- `app/src/main/java/me/lucid/domchat/ui/components/chat/PromptPresetsBottomSheet.kt` — glassy sheet redesign (black container, `SpecularBorderSubtle` border, drag handle pill, close button, 2x2 grid, no colorful badges).
- `app/src/main/java/me/lucid/domchat/ui/components/ai/ModelList.kt` — `ModelItem` Card border `SpecularBorderBrush` → `SpecularBorderSubtle`.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` — tag chips smaller (`10.sp`, `8.dp/3.dp` padding) in PortraitCard + InspectorSheet.
- `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt` — tag chips smaller (`9.sp`/`10.sp`, `6.dp/2.dp`/`8.dp/3.dp` padding) in CharacterCard + inspector sheet.
- `audit_brief.md` — this document.
