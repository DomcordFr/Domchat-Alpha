# Audit Brief — Janitor Hub Scrollable Header, Real Dropdown, Sort/Filter Actions & Glassy Tags (task-035)

## Intent Verification

All four directive requirements are implemented in `SettingJanitorPage.kt`:

1. **Header controls moved inside LazyVerticalGrid** — The feed selector, sort button, filter button, and search bar are now inside `LazyVerticalGrid` as `item(span = { GridItemSpan(maxLineSpan) })`. Only `OneUITopAppBar` remains sticky; all header controls scroll away with the card grid.

2. **Feed Dropdown (real DropdownMenu)** — Replaced the static column/row of feed modes with `DropdownMenu`. A single glassy pill button shows the active mode (e.g., "Trending 24h ▾") that toggles a popup with options: [Popular, Trending, Trending 24h, Latest].

3. **Sort & Filter actions** — Sort opens a `DropdownMenu` (Trending 24h, Trending, Popular, Latest, Name A-Z) that sorts the character list. Filter opens a dialog/sheet with tag chips (Female, Male, Smut, Dead Dove, Fluff, Angst, Fantasy, Sci-Fi) that filters displayed items.

4. **Tags in CharacterCard** — Tag pills use black background `Color(0xFF000000)`, glassy `0.5dp` `SpecularBorderSubtle` border, `RoundedCornerShape(20.dp)` pill shape, full words via `.replace("_", " ")` with no truncation.

Strict null safety: no `!!` in the file. All nullable access uses `?:`, `takeIf`, `isNullOrBlank()`, `orEmpty()`. Unified glass theme `0xFF141520` enforced on tag pills and load more button.

## Call Chain & Architecture

**SettingJanitorPage** is a setting page composable reached via `Screen.SettingJanitor`. State:
- `settings` — collected via `assistantVM.settings.collectAsStateWithLifecycle()`
- `selectedTab` — 0=Trending 24h, 1=Trending, 2=Search
- `browseState`/`searchState` — `JanitorLoadState` (Loading/Success/Error)
- `feedMode` — current feed mode selection
- `page` — pagination counter (starts at 1)
- `isLoadingMore` — load-more-in-progress flag
- `sortMode` — current sort selection
- `filterTags` — set of filter tags applied to filter items
- `selectedCharacter` — currently inspected character
- `bottomSheetState` — `rememberModalBottomSheetState`
- `isImporting` — extraction in progress flag
- `isTabSwitching` — tab transition loading flag

Flow:
1. `loadBrowse(tab, pageNum, append)` → `withContext(Dispatchers.IO)` HTTP request → parse JSON → update state on `Dispatchers.Main`. When `append=true`, merges new items with existing via `distinctBy { it.id }`.
2. Tab/feed mode switch → `LaunchedEffect(selectedTab, feedMode)` sets `isTabSwitching = true`, resets `page = 1`, loads data, then `isTabSwitching = false`.
3. Search bar `onCommit` → extracts UUID from URL via regex or performs text search → updates `browseState`.
4. 'Load More' → increments `page`, calls `loadBrowse(tab, page, append = true)`.
5. CharacterCard click → `selectedCharacter = character` → `bottomSheetState.show()` → `ModalBottomSheet`.
6. Sheet "Import to Hub" → uses `fullExtract` (pre-fetched via `LaunchedEffect(character.id)`) → registers new tags → `extractToAssistant(extract, updatedSettings)` → `assistantVM.addAssistant(assistant)` → toast + hide sheet.
7. Sheet embedded images → extracted from markdown URLs in `bio`/`description`/`firstMessage` + `extract.embeddedImages` via URL regex extraction.

UI Layer: only renders state and emits intents. Zero business logic in composables.

## Recomposition & Memory Safety

- **No `!!`**: Verified zero force-unwraps across the file.
- **Remember scoping**: `selectedTab`, `searchQuery`, `browseState`, `searchState`, `isTabSwitching`, `feedMode`, `page`, `isLoadingMore`, `sortMode`, `filterTags`, `selectedCharacter`, `bottomSheetState`, `isImporting`, `fullExtract`, `isExtractLoading` all use `remember` / `rememberModalBottomSheetState` / `mutableIntStateOf`.
- **DropdownMenu state**: `feedMenuOpen`, `sortMenuOpen`, `filterMenuOpen` are local `remember` states inside the grid item header, scoped to that composition.
- **LazyVerticalGrid**: items(key = character.id) ensures stable identity across pagination appends.
- **Sorted/filtered items**: `sortedFilteredItems` is a derived value computed during composition from `activeState.items` with filter and sort applied — no state mutation during composition.
- **No naked objects**: All state is `remember`-scoped or `collectAsStateWithLifecycle`-collected.

## Threading & Dispatchers

- UI composition, animation, and layout on `Dispatcher.Main` — correct.
- `JanitorOrbitalLoader` uses `rememberInfiniteTransition` + `animateFloat` — Main thread Compose animation API.
- Network I/O strictly on `Dispatchers.IO` (`httpClient.execute` inside `withContext(Dispatchers.IO)`). JSON parsing on `Dispatchers.IO`. UI state updates marshalled to `Dispatchers.Main` via `withContext(Dispatchers.Main)`.
- `scope.launch` (Main dispatcher) orchestrates import flow, hopping to IO for requests and back to Main for state updates.
- Tab/feed mode loading state (`isTabSwitching`) set/reset in `LaunchedEffect` with try/finally for guaranteed reset.

## Offline & Error State Handling

- **Fully local UI**: Header controls, pagination, orbital loader, tag design, and sheet styling work entirely offline.
- **Network errors**: `loadBrowse`/`loadSearch` catch exceptions → `JanitorLoadState.Error(message)` → displayed as error Text. Import catches exceptions → `LocalToaster.current` `ToastType.Error`.
- **HTTP errors**: Non-2xx status → `JanitorLoadState.Error("HTTP ${statusCode}")`.
- **Malformed JSON**: `JsonInstant.decodeFromString` wrapped in try/catch → error state.
- **Missing avatar**: `resolveAvatarUrl` returns `""` for blank/null → Coil3 placeholder/fallback.
- **Null character fields**: `character.name.takeIf { it.isNotBlank() } ?: "Character"` fallback. `character.creator ?: character.creatorName` nullable chains.
- **Embedded images**: Empty list skips gallery section. URL regex safely returns empty list on no matches.
- **Backward compatible**: `embeddedImages` field defaults to `emptyList()`, existing serialized responses deserialize without migration.
