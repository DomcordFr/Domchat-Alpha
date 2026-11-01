# Audit Brief — Task-058: Refactor SettingJanitorPage.kt

## Intent Verification

All six requirements from the DualBrain Lead Architect Directive are implemented:

1. **Sort & Filter button styling:** Both Sort and Filter pill buttons now use white text (`Color.White`), `FontWeight.Medium`, and white icon tint (`Color.White`), matching the Feed dropdown styling for visual consistency.

2. **Sort dropdown replacement:** Replaced the old `["Popular", "Trending", "Trending 24h", "Latest"]` options with `["Default", "Name A-Z", "Name Z-A"]`. Changed `sortMode` default from `"Popular"` to `"Default"`. Updated `sortedFilteredItems` when block to handle `"Name Z-A"` descending sort. Both Feed and Sort `ModalBottomSheet`/`DropdownMenu` components now use `RoundedCornerShape(20.dp)`, `containerColor = Color(0xFF000000)`, and `0.5.dp` `LiquidGlassTokens.SpecularBorderSubtle` border with `RoundedCornerShape(20.dp)`.

3. **CharacterHub popover drag handle:** Set `dragHandle = null` on `ModalBottomSheet` at line 1051, removing the default black pill drag handle above the hero image. `sheetState = bottomSheetState` is correctly passed for smooth open/dismiss animation.

4. **Import button with Tag Persistence:** Added `ImportStatus` enum (IDLE, IMPORTING, SUCCESS, ERROR) at line 238. Added `var importStatus by remember { mutableStateOf(ImportStatus.IDLE) }` at line 285. Added `Spacer(modifier = Modifier.height(14.dp))` above the Import button at line 1218. Import button background animates based on import status: `Color(0xFF2E7D32)` green with `Icons.Rounded.Check` and "Imported!" text on success; `Color(0xFFD32F2F)` red with `Icons.Rounded.Close` and "Failed" text on error. On success, `delay(750ms)` before hiding the bottom sheet via `bottomSheetState.hide()`.

5. **Smooth grid item transitions:** Added `Modifier.animateItem()` to grid character items at line 869 for smooth tag-filtering transitions.

6. **Offline error handling in loadBrowse/loadSearch:** When a network error occurs with existing items, the existing items are retained instead of wiping them out. An informative `ToastType.Error` is shown. Only when no existing items exist does the state transition to `JanitorLoadState.Error`. Applied to both `loadBrowse` (lines 344-356) and `loadSearch` (lines 416-447).

All changes maintain the unified glass card theme (`0xFF000000` backgrounds, `LiquidGlassTokens.SpecularBorderSubtle` borders, `0xFF141520` glass cards).

## Call Chain & Architecture

`SettingJanitorPage.kt` — single-activity route host via `RouteActivity`:
- `SettingJanitorPage()` composable — main JanitorAI Hub page with tabs (Trending 24h, Trending, Search)
- `feedMode` state (`mutableStateOf("Default")`) — current sort mode
- `sortMenuOpen`, `filterMenuOpen` — dropdown visibility state  
- `selectedTagIds` (`Set<String>`) — selected tag IDs for filtering
- `filterTags` (`MutableState<Set<String>>`) — selected tag IDs for filtering
- `selectedCharacter` state (`MutableState<JanitorCharacter?>`) — selected character for popover
- `viewerImageUrls`, `viewerOpen`, `viewerCurrentPage` — image viewer state
- `bottomSheetState` (`rememberModalBottomSheetState`) — controls popover visibility
- `importStatus` (`MutableState<ImportStatus.IDLE>`) — import button state
- `isImporting` (`MutableState<Boolean>`) — import loading state

UI layer only: renders state and emits user intents; zero business logic.
ViewModel: `AssistantVM` via `koinViewModel<AssistantVM>()`.

No new `Screen` routes, ViewModels, or DI changes — all changes are intra-composable UI refinements.

## Recomposition & Memory Safety

- All state variables properly `remember { mutableStateOf(...) }` or `rememberSaveable { ... }` — properly remembered
- `importStatus` state properly `remember { mutableStateOf(ImportStatus.IDLE) }` — survives recomposition
- `sortedFilteredItems` computed in `when (sortMode)` block — recomputes when dependencies change
- `animateItem()` modifier on grid items — Compose Foundation's item animation modifier, handles enter/exit transitions automatically
- `ImportStatus` enum — `IDLE`, `IMPORTING`, `SUCCESS`, `ERROR` states for import button state management
- Zero `!!` force-unwraps — verified by grep across the file
- All state is `remember`ed or collected with lifecycle awareness

## Threading & Dispatchers

- `loadBrowse()` and `loadSearch()` run in `scope.launch` on `Dispatchers.Main`
- Network I/O via `withContext(Dispatchers.IO)` — proper dispatcher for HTTP calls
- UI state updates via `withContext(Dispatchers.Main)` — proper dispatcher for state mutations
- `delay(750)` in `scope.launch` for delayed sheet hide on import success — runs on Main dispatcher via `scope.launch`, safe as Compose side-effect
- No blocking calls on Main dispatcher
- Existing `withContext(Dispatchers.IO)` / `withContext(Dispatchers.Main)` patterns unchanged

## Offline & Error State Handling

- Network errors handled via `try/catch` → `JanitorLoadState.Error(user-friendly message)` instead of raw HTTP status codes
- HTTP non-2xx handled gracefully — if existing items exist, they are retained and a toast is shown; otherwise `JanitorLoadState.Error` is set
- `loadSearch()` handles both URL/UUID extraction (POST to `/api/extract`) and normal search (GET to `/api/search`) with full error handling
- `loadBrowse()` and `loadSearch()` both preserve existing items on network errors instead of wiping them
- `importCharacter()` handles import errors gracefully with `importStatus = ImportStatus.ERROR`
- All error messages are user-friendly (e.g., "Failed to load more characters. Server returned HTTP 500. Please try again later.")
