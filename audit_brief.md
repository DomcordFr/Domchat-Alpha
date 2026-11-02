# Audit Brief — Multi-Lorebook Architecture & Glassy Lorebook Popovers

## Intent Verification

**1. UserPersona.kt — multi-lorebook support**
- Added `@SerialName("enabled_lorebook_ids") val enabledLorebookIds: Set<String> = emptySet()` to the `UserPersona` data class, enabling personas to attach multiple lorebooks.
- Added `kotlinx.serialization.SerialName` import.

**2. SettingLorebooksPage.kt & SettingLorebookDetailPage.kt — sheet height + close button**
- `LorebookCreatorSheet` (SettingLorebooksPage.kt): Constrained max height to 85% of screen (`heightIn(max = maxSheetHeight)` where `maxSheetHeight = configuration.screenHeightDp.dp * 0.85f`). Added top-right close `IconButton` (`Icons.Rounded.Close`). Drag handle pill retained. Border changed from `SpecularBorderBrush` to `SpecularBorderSubtle`.
- `EntryEditorSheet` (SettingLorebookDetailPage.kt): Same max-height constraint, close `IconButton`, `SpecularBorderSubtle` border.
- `LorebookEditorSheet` (SettingLorebookDetailPage.kt): Same max-height constraint, close `IconButton`, `SpecularBorderSubtle` border.

**3. SettingLorebooksPage.kt — lorebook cards black + SpecularBorderSubtle**
- `LorebookCard`: border changed from grey gradient (`borderBrush`) to `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)`. Card fill remains `Color(0xFF000000)`.
- Empty state card: border changed from `borderBrush` to `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)`. Removed now-unused `borderBrush` and `isDark` declarations in empty-state branch.

**4. MinimalChatInput.kt (via InputPickerSheets.kt LorebooksPickerSheet) — glassy popover**
- `LorebooksPickerSheet`: container color changed to `Color(0xFF000000)` (AMOLED black). Border changed from `domChatSheetContainerColor()` to `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)`. Added max-height constraint (85% of screen). Added top-right close `IconButton`. Lorebook cards changed from `Card` with `surfaceContainerHighest` fill to `Surface` with `Color(0xFF000000)` fill and `SpecularBorderSubtle` border — zero grey.

**Null safety:** zero `!!` introduced across all edited files. All nullable access uses `?.`, Elvis `?:`, `isNullOrBlank()`, `orEmpty()`, or `mapNotNull`.

**Theme:** `Color(0xFF000000)` AMOLED black, `Color(0xFF141520)` unified glass card theme, `LiquidGlassTokens.SpecularBorderSubtle` borders enforced throughout.

## Call Chain & Architecture

**UserPersona (data model):**
- `UserPersona.enabledLorebookIds` is a plain data field with `@SerialName("enabled_lorebook_ids")` for serialization. No business logic impact — it's a storage field consumed by the assistant/lorebook association layer.

**Sheet presentation (UI layer only):**
- `LorebookCreatorSheet`, `EntryEditorSheet`, `LorebookEditorSheet` are modal bottom sheets presented conditionally via `if (showX)` blocks in their respective composables. State is `remember { mutableStateOf(...) }` — UI-only, no ViewModel involvement for sheet presentation.
- Sheet dismissal flows through `onDismiss` callbacks → parent sets `showX = false`.
- Max height computed from `LocalConfiguration.current.screenHeightDp` × 0.85 — pure Compose read, no async.

**Lorebook cards (display only):**
- `LorebookCard` and `LorebooksPickerSheet` cards read `Lorebook` data and render it. Borders/fills are static theme values. No state mutation.

**Persistence:**
- Lorebook CRUD (`save`, `delete`, `update`) funnels through `vm.updateSettings(settings.copy(lorebooks = ...))` → `SettingsStore.update` (canonical settings API: normalization, secret migration, in-memory state, persist). Unchanged by this task.

## Recomposition & Memory Safety

- `maxSheetHeight` computed once per composition from `LocalDensity.current` + `LocalConfiguration.current` — stable, no per-frame allocation.
- `heightIn(max = maxSheetHeight)` bounds sheet content; `rememberScrollState()` + `verticalScroll` used where content may exceed the max height (EntryEditorSheet, LorebookEditorSheet, LorebooksPickerSheet).
- `rememberModalBottomSheetState(skipPartiallyExpanded = true)` — lifecycle-aware sheet state.
- Close `IconButton` and drag handle are stateless Compose controls.
- All card styling uses inline `Color(0xFF000000)` and `LiquidGlassTokens.SpecularBorderSubtle` — no retained objects, no allocations beyond Compose's normal node creation.
- No force-unwraps (`!!`) introduced.
- No `SnapshotStateList` passed to `items()`; all `itemsIndexed`/`forEachIndexed` use stable keys (`lorebook.id`, `entry.id`).

## Threading & Dispatchers

- All changes are synchronous main-thread Compose operations. No `Dispatchers.Main` blocking.
- No `Dispatchers.IO` usage introduced in this task.
- Sheet open/close and dismiss are main-thread state updates (`showX = false`), consistent with existing patterns.
- Lorebook persistence (`vm.updateSettings`) already runs on appropriate dispatchers inside `SettingsStore.update` — untouched by this task.

## Offline & Error State Handling

- Sheet presentation and dismissal are fully local (no network). Offline-capable.
- Lorebook card rendering uses local data only — no error surface.
- `LorebookCreatorSheet` save path: `onSave(lorebook)` → `vm.updateSettings` — existing error handling (toasts) in parent scope unchanged.
- `EntryEditorSheet`/`LorebookEditorSheet` auto-save: `onAutoSave?.invoke(...)` — existing try/catch for embedding generation unchanged.
- Empty lorebook state renders a black glass card (`Color(0xFF000000)` + `SpecularBorderSubtle`) instead of grey — no crash, graceful offline display.
- Error states: existing `errorFlow` toasts in `SettingVM`/callers unchanged; no new error modes introduced.

## Files Changed

- `app/src/main/java/me/lucid/domchat/data/model/UserPersona.kt` — added `enabledLorebookIds` field + `SerialName` import.
- `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingLorebooksPage.kt` — LorebookCard + empty card borders → `SpecularBorderSubtle`; removed grey `borderBrush`; `LorebookCreatorSheet` max height (85%) + close button + `LocalConfiguration` import.
- `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingLorebookDetailPage.kt` — `EntryEditorSheet` + `LorebookEditorSheet` max height (85%) + close button + `SpecularBorderSubtle` border + `LocalConfiguration` import.
- `app/src/main/java/me/lucid/domchat/ui/components/ai/InputPickerSheets.kt` — `LorebooksPickerSheet` glassy (`0xFF000000` container, `SpecularBorderSubtle` border), max height, close button, glassy black lorebook cards + `BorderStroke`/`LocalConfiguration`/`LocalDensity` imports.
- `audit_brief.md` — this document.
