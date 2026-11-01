# Audit Brief — Janitor Hub Redesign with Controls, Pagination, Custom Loader & Popover Cross (task-033)

## Intent Verification

All seven directive requirements are implemented in `SettingJanitorPage.kt`:

1. **Header Controls** — Below the JanitorAI Hub heading:
   - Left-aligned glassy feed mode dropdown selector: [Popular, Trending, Trending 24h, Latest] with clickable options that update `feedMode` state and reset `page = 1`
   - Right side: two glassy option buttons — 'Sort' (sort icon) and 'Filter' (filter list icon), both in `0xFF000000` glass cards with `0.5dp` `SpecularBorderSubtle` borders
   - Top-right: search bar supporting text queries AND full Janitor bot URLs; pasting a URL triggers `extractJanitorUuidFromUrl()` regex extraction and direct bot lookup via `/api/bots/{uuid}`

2. **Pagination** — `page` counter (mutableIntStateOf starting at 1), `loadBrowse(tab, pageNum, append)` appends fetched bots to the current list via `distinctBy { it.id }`. Glassy 'Load More' button at bottom of grid increments page and calls `loadBrowse(tab, page, append = true)`.

3. **Custom Centered Animated Loader** — `JanitorOrbitalLoader()` uses concentric glowing orbital rings: rotating `Box` with `2.5dp` border in `Color(0xFF141520)` gradient (0.95f → 0.35f alpha), `infiniteRepeatable` animation for rotation (1200ms) and pulse alpha (1000ms reverse). Centered via `Box(fillMaxSize, contentAlignment = Alignment.Center)` when loading with empty items.

4. **Tag design** — Black background `Color(0xFF000000)`, glassy `0.5dp` `SpecularBorderSubtle` border, pill shape `RoundedCornerShape(20.dp)`, full words via `.replace("_", " ")` with no truncation.

5. **Popover Sheet** — Smooth `ModalBottomSheet` animation with `skipPartiallyExpanded = true`; circular glassy Close (X) button in top-end corner over hero image (`0.50f` black scrim + `0.25f` white border); embedded images gallery renders from `bio`, `description`, `firstMessage`, and `embeddedImages` fields via URL regex extraction; hero avatar alignment = `Alignment.TopCenter` with `contentScale = ContentScale.Crop`.

6. **Removed** — The `'___ characters loaded'` text indicator has been removed from the Janitor Hub page.

7. **Schema** — `JanitorExtractResponse` now includes `@SerialName("embeddedImages") val embeddedImages: List<String> = emptyList()`.

Strict null safety: no `!!` in the file. All nullable access uses `?:`, `takeIf`, `isNullOrBlank()`, `orEmpty()`. Unified glass theme `0xFF141520` enforced across cards, tag pills, load more button, and embedded image cards.

## Call Chain & Architecture

**SettingJanitorPage** is a setting page composable reached via `Screen.SettingJanitor`. State:
- `settings` — collected via `assistantVM.settings.collectAsStateWithLifecycle()`
- `selectedTab` — 0=Trending 24h, 1=Trending, 2=Search
- `browseState`/`searchState` — `JanitorLoadState` (Loading/Success/Error)
- `feedMode` — current feed mode selection
- `page` — pagination counter (starts at 1)
- `isLoadingMore` — load-more-in-progress flag
- `selectedCharacter` — currently inspected character
- `bottomSheetState` — `rememberModalBottomSheetState`
- `isImporting` — extraction in progress flag
- `isTabSwitching` — tab transition loading flag

Flow:
1. `loadBrowse(tab, pageNum, append)` → `withContext(Dispatchers.IO)` HTTP request → parse JSON → update state on `Dispatchers.Main`. When `append=true`, merges new items with existing.
2. Tab/feed mode switch → `LaunchedEffect(selectedTab, feedMode)` sets `isTabSwitching = true`, resets `page = 1`, loads data, then `isTabSwitching = false`.
3. Search bar `onCommit` → extracts UUID from URL via regex or performs text search → updates `browseState`.
4. 'Load More' → increments `page`, calls `loadBrowse(tab, page, append = true)`.
5. CharacterCard click → `selectedCharacter = character` → `bottomSheetState.show()` → `ModalBottomSheet`.
6. Sheet "Import to Hub" → uses `fullExtract` (pre-fetched via `LaunchedEffect(character.id)`) → registers new tags → `extractToAssistant(extract, updatedSettings)` → `assistantVM.addAssistant(assistant)` → toast + hide sheet.
7. Sheet embedded images → extracted from markdown URLs in `bio`/`description`/`firstMessage` + `extract.embeddedImages` via `urlRegex()`.

UI Layer: only renders state and emits intents. Zero business logic in composables.

## Recomposition & Memory Safety

- **No `!!`**: Verified zero force-unwraps across the file.
- **Remember scoping**: `selectedTab`, `searchQuery`, `browseState`, `searchState`, `isTabSwitching`, `feedMode`, `page`, `isLoadingMore`, `selectedCharacter`, `bottomSheetState`, `isImporting`, `fullExtract`, `isExtractLoading`, `embeddedImageUrls` all use `remember` / `rememberModalBottomSheetState` / `mutableIntStateOf`.
- **LazyVerticalGrid**: items(key = character.id) ensures stable identity across pagination appends.
- **LaunchedEffect(key)**: `selectedTab` + `feedMode` key ensures reload only when either changes; `character.id` key ensures extract re-fetch only per character.
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
