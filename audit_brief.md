# Audit Brief — Character Hub Polish, Black Box Fix & Pure Black Popover (task-028)

## Intent Verification

All five directive requirements are implemented:

1. **CharacterPortraitCard** — Removed `drawWithContent` with `BlendMode.DstIn` on the display name `Text`. Now uses plain `maxLines = 2` + `TextOverflow.Ellipsis` with `Modifier.fillMaxWidth()`. The black rectangular box artifact next to the name is eliminated. The `graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` on the card container remains.

2. **CharacterHubPage header** — Only the `OneUITopAppBar` (back arrow + "Characters" title) remains sticky via `Scaffold.topBar`. The search bar, sort chips ('Name', 'Recent', 'Creator'), and 'Upload Character'/'List View' buttons are now header `item`s inside both `LazyVerticalGrid` and `LazyColumn` branches of `AnimatedContent`, so they scroll away with content. The outer sticky `Column` wrapper around the header was removed; `nestedScroll` is now applied directly to each lazy container.

3. **FAB** — Changed to pure white background (`containerColor = Color.White`) with black content (`contentColor = Color.Black`) and a plain black `Icons.Rounded.Add` icon (no gradient box, no glass tint).

4. **CharacterInspectorSheet** — `cardBgColor` changed from `Color(0xFF141520)` to pure jet black `Color(0xFF000000)`. Applied to Character Voice card, tab bar, tab content cards (Notes/Personality/Greetings), and intro chips — all framed with `0.5.dp` `SpecularBorderSubtle` border. `sanitizeHtml` now also strips raw URLs (`https?://[^\s<>"]+`) to remove JAI links from the blurb.

5. **AssistantVM.addAssistant** — Changed from read-then-write (`settings.value` → `settingsStore.update(settings.copy(--))`) to atomic `settingsStore.update { current -> -- }`, so concurrent `addAssistant` calls never overwrite each other's assistants.

Strict null safety: no `!!` in either file. All nullable access uses `?:`, `takeIf`, `isNullOrBlank()`, `orEmpty()`. Unified glass theme `0xFF141520` preserved for cards where specified; pure black `0xFF000000` used for sheet cards per directive.

## Call Chain & Architecture

**CharacterHubPage:** Single composable owned by `RouteActivity`. State: `searchQuery`, `debouncedQuery`, `isGridView`, `sortOption`, `inspectingAssistant`, `filteredAssistants`, `loadError`. Flow:
- `settings.assistants` → debounced search (250ms) on `Dispatchers.Default` → filtered/sorted on `Dispatchers.Main`
- `CharacterPortraitCard` click → `inspectingAssistant = char` → `CharacterInspectorSheet` modal bottom sheet
- FAB click → `vm.addAssistant(newChar)` → `navController.navigate(Screen.AssistantDetail(--))`
- Header items (search, sort, upload) now scroll with grid/list content inside `AnimatedContent` switching between `LazyVerticalGrid` and `LazyColumn`.

**CharacterInspectorSheet:** Modal bottom sheet showing character details. Uses `sheetBgColor` (pure black via `domChatDialogContainerColor()`), `cardBgColor` (now `Color(0xFF000000)`), `cardBorderColor` (`SpecularBorderSubtle`). Primary action "Import to Hub" / "Start Chat" buttons.

**AssistantVM:** ViewModel exposing `settings: StateFlow<Settings>`. `addAssistant` now uses `settingsStore.update { current -> -- }` for atomic read-modify-write. `updateAssistant`, `removeAssistant`, `undoRemoveAssistant`, `copyAssistant` all use `settingsStore.update { -- }` blocks.

## Recomposition & Memory Safety

- **No `!!`**: Verified zero force-unwraps across both files.
- **Remember scoping**: `searchQuery`, `debouncedQuery`, `isGridView`, `sortOption`, `inspectingAssistant`, `loadError`, `bottomSheetState`, `gridState`, `listState` all use `remember`. `animateFloatAsState` in loaders is scoped to composition lifecycle.
- **AnimatedContent**: Crossfades between `LazyVerticalGrid` and `LazyColumn` — header items duplicated in both branches but identical content, crossfade is visually seamless.
- **Header items in lazy scope**: `item { OutlinedTextField }`, `item { Row }`, `item { Spacer }`, conditional `loadError?.let { item { -- } }` — all valid Compose lazy DSL. `loadError` is read in composition; when null, no item is emitted.
- **No naked objects**: All state is `remember`-scoped or `collectAsStateWithLifecycle`-collected.

## Threading & Dispatchers

- UI composition, animation, and layout on `Dispatcher.Main` — correct.
- Assistant search/filter: `withContext(Dispatchers.Default)` for filtering, `withContext(Dispatchers.Main)` for state updates.
- `addAssistant` runs inside `viewModelScope.launch` (Main) and calls `settingsStore.update` which persists to DataStore on the appropriate internal dispatcher.
- No blocking calls on Main. `withContext(Dispatchers.IO)` used for conversation creation in `onStartChat`.
- No `Dispatchers.Default` misuse.

## Offline & Error State Handling

- **Fully local UI**: Card layout, header restructuring, FAB styling, sheet styling all work offline.
- **Error boundary**: `loadError` state captured from search/filter try/catch, displayed via `CharacterHubErrorCard` as a grid/list header item.
- **Conversation creation errors**: Caught in `onStartChat` try/catch, surfaced via `LocalToaster.current` (`ToastType.Error`).
- **Null safety**: `assistant.name.takeIf { it.isNotBlank() } ?: "Character"` fallback. `assistant.creator` nullable chains with `isNullOrBlank()`. `assistant.tags` iterated safely.
- **URL stripping**: `sanitizeHtml` strips `https?://--` patterns, so raw JAI URLs in creator notes never render as clickable links or break layout.
- **Atomic settings update**: `settingsStore.update { current -> -- }` prevents lost updates when multiple assistants are added in quick succession.
