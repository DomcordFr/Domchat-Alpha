# Audit Brief — Task 119: Plus Icon Menu & Prompt Presets Sheet Redesign

## Intent Verification

All 4 requirements from task-119_directive.json implemented:

1. **Plus sub-sheets: drag handle + close (cross) icon in same place** — `MinimalChatInput.kt`:
   - Wallpaper sheet: Added close (X) `IconButton` next to title in header row. Uses `DomChatInWindowBottomSheet` which already provides `DomChatSheetDragHandle()` as default drag handle.
   - Model picker sheet: Added close (X) `IconButton` next to title in header row. Uses `DomChatInWindowBottomSheet` default drag handle.
   - Both sheets now have consistent close icon placement (top-right of header) and functioning drag handles.

2. **PromptPresetsBottomSheet.kt — Glass theme, full-width cards, search bar**:
   - Sheet container color changed from `Color(0xFF000000).copy(alpha = 0.85f)` to `Color(0xFF141520)` (unified DomChat black glass).
   - Added search bar `OutlinedTextField` with label "Search presets", search/clear icons, and `AppShapes.InputField` shape.
   - Added `presetSearchQuery` state and `filteredPresets` list that filters `allPresets` by name/description/content.
   - Preset cards use `.fillMaxWidth()` with `height(130.dp)` — cards don't touch screen edges (padding via grid spacing).
   - Glass theme applied throughout: `Color(0xFF141520)` surfaces with `LiquidGlassTokens.SpecularBorderSubtle` borders.

3. **MinimalChatInput.kt — Animate wallpaper content height when switching Full Image ↔ Fade**:
   - Replaced static `Column` + `AnimatedVisibility` for blur mode section with `AnimatedContent(targetState = blurModeDraft, ...)` using `expandVertically`/`shrinkVertically` + `fadeIn`/`fadeOut`.
   - Content height now animates smoothly when switching between Full Image and Bottom Fade modes (the latter adds the Blur Height Reach slider).
   - `AnimatedVisibility` removed in favor of `AnimatedContent` conditional rendering inside the branch.

4. **Plus icon menu background matched to chatbar at low tint (5-10%)**:
   - Changed `containerColor` from `Color(0xFF000000).copy(alpha = settings.displaySetting.glassAlpha.coerceIn(0.10f, 0.75f))` to `Color(0xFF000000).copy(alpha = settings.displaySetting.glassAlpha.coerceIn(0.05f, 0.10f))`.
   - Plus button now clamps between 5% and 10% opacity instead of 10%–75%, matching chatbar background at low tint.

**Constraints:** ZERO `!!` force-unwraps introduced (grep-confirmed across all modified files). Pre-existing `!!` in `MinimalChatInput.kt:3400-3402` (camera output file handling) are untouched. Unified glass card theme `0xFF141520` enforced. Existing code style preserved.

## Call Chain & Architecture

```
Plus button flow:
User taps Plus button (DomChatComposerAddButton)
  → showPicker = true
  → Plus sub-sheets open (Model Picker, Wallpaper, etc.)
  → Each sheet uses DomChatInWindowBottomSheet with default DomChatSheetDragHandle()
  → Close (X) IconButton in header calls onDismissRequest → showXxxSheet = false

Wallpaper mode switch:
User taps "Full Image" or "Bottom Fade" tab
  → blurModeDraft = mode
  → AnimatedContent(targetState = blurModeDraft) recomputes content
  → Bottom Fade branch adds Blur Height Reach slider
  → expandVertically/shrinkVertically animates height change

Prompt presets search:
User types in search bar
  → presetSearchQuery = it
  → filteredPresets = allPresets.filter { matches query }
  → LazyVerticalGrid recomputes with filtered items

Architecture boundaries:
- UI layer (sheets, composer): renders state + emits intents only
- ViewModel (ChatVM, AssistantVM): manages coroutines in viewModelScope
- DataStore/SettingsStore: single source of truth for settings/wallpapers
```

## Recomposition & Memory Safety

- `presetSearchQuery` is `remember { mutableStateOf("") }` — remembered UI state.
- `filteredPresets` is computed during composition from `allPresets` + `presetSearchQuery` — no allocation beyond the filtered list.
- `AnimatedContent(targetState = blurModeDraft)` — replaces content based on mode; `expandVertically`/`shrinkVertically` animate height without state loss for sliders (blurDraft, dimDraft, blurHeightFractionDraft are remembered outside).
- `DomChatSheetDragHandle()` default drag handle in `DomChatInWindowBottomSheet` — stateless, zero recomposition impact.
- Close icons are stateless `IconButton` composables — no state captured.
- All `Text` composables use bounded `maxLines` + `TextOverflow.Ellipsis` where applicable.
- No `SnapshotStateList` passed directly to `items()`. No naked objects in Compose.
- Zero `!!` force-unwraps introduced.

## Threading & Dispatchers

- Sheet UI rendering: `Dispatchers.Main` (correct — Compose UI layer).
- Wallpaper save: `scope.launch { settingsStore.update { ... } }` — viewModelScope, DataStore IO-bound internally.
- `wallpaperPicker.launch(...)` — ActivityResultLauncher, runs on Main (correct).
- No `Dispatchers.Main` violations introduced. No background work on Main dispatcher.
- `animateColorAsState` for tab backgrounds — runs on Main as part of Compose animation (correct).

## Offline & Error State Handling

- **Offline:** All changes are purely local UI — sheet navigation, search filtering, wallpaper mode switching. No network dependency.
- **Loading:** No loading states needed — all operations are instant local UI updates.
- **Success:** Wallpaper config saved via DataStore; preset search filters instantly; close icons dismiss sheets immediately.
- **Exception:** `settingsStore.update` wraps DataStore writes in try/catch (per existing patterns). `wallpaperPicker` result handled with null check on `uri`. `chatBgDraft` null-safe via Elvis operators.
- **Edge cases:** Empty search query shows all presets. `chatBgDraft` null → "Remove" button hidden. `blurModeDraft` defaults to existing config or global setting.
