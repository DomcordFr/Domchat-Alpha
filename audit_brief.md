# Audit Brief — Settings Header Fade, JAI Tile Logo & Janitor Hub Complete Redesign (task-027)

## Intent Verification

All six directive requirements are implemented across the three target files:

1. **OneUITopAppBar.kt** — Added a smooth vertical gradient fade `Box` at the bottom of the sticky header (`48.dp` height, `Brush.verticalGradient` from `barColor` to `barColor.copy(alpha = 0f)`), so all 20+ setting subpages have seamless bottom transparency when scrolling.

2. **SettingPage.kt** — JanitorAI Hub tile now uses `iconPainter = painterResource(id = R.drawable.janitorai_logo)` instead of `Icons.Rounded.AutoStories`. Added `iconPainter: Painter?` field to `SettingNavItem` and updated both icon-rendering lambdas (categorized layout + search results) to prefer `iconPainter` when non-null, falling back to `ImageVector` icon.

3. **SettingJanitorPage.kt**:
   - **(a)** Removed the 32dp logo `Image` from `topBar.actions` slot — actions block is now empty, eliminating the random circle in the top right.
   - **(b)** Tab bar now has `Modifier.fillMaxWidth().background(Color(0xFF141520).copy(alpha = 0.92f)).zIndex(1f)` so cards scroll cleanly underneath with proper z-layering.
   - **(c)** Bot cards unified to Character Hub design: `aspectRatio(0.68f)`, full-bleed portrait, `Color(0xFF141520)` background, `0.5dp` `SpecularBorderSubtle` border, `CompositingStrategy.Offscreen` graphicsLayer, bottom glass scrim (`Color(0xFF141520).copy(alpha = 0.85f)`), display name with `?: "Character"` fallback, dark glass tag pills (`0xFF141520` + `0.5dp` subtle border) in a `FlowRow`.
   - **(d)** Popover sheet unified to CharacterInspectorSheet structure: hero image with multi-stop vertical gradient fade, title/creator, `FlowRow` badges (scenario count + version), bio blurb, primary "Import to Hub" action button (white glass, bold text).
   - **(e)** Stock `CircularProgressIndicator` replaced with custom `JanitorOrbitalLoader` — pulsing dark-glass orbital ring (`Color(0xFF141520)` gradient border, rotating `rotationZ`, alpha pulse), used both for grid loading state and import button loading state.
   - **(f)** `JanitorExtractRequest` now sends both `bot_id` and `character_id` (both set to `character.id`).

Strict null safety: no `!!` in any modified file. `sanitizeHtml`-style nullable handling uses `?:`, `takeIf`, `orEmpty()`. Unified glass theme `0xFF141520` enforced as card background, tab bar fill, tag pill fill, FAB-like import button, and orbital loader gradient.

## Call Chain & Architecture

**OneUITopAppBar:** Used as `topBar` in `Scaffold` across all setting subpages (`SettingPage`, `SettingJanitorPage`, `SettingDisplayPage`, etc.). Reads `collapsedFraction` from scroll behavior to interpolate height and bar color. New bottom gradient fade Box is purely presentational — no state, no ViewModel involvement.

**SettingPage:** `SettingVM` via `koinViewModel()` exposes `settings` StateFlow. `allSettings` list built in `remember {}` includes `SettingNavItem` for JanitorAI Hub with `iconPainter`. Icon rendering lambda checks `item.iconPainter != null` → `Image(painter = ...)` else `Icon(imageVector = ...)`. Navigation via `navController.navigate(item.target)`.

**SettingJanitorPage:** `AssistantVM` via `koinViewModel()` for `addAssistant(...)`. `PlatformHttpClient` via `koinInject()` for JanitorAI API calls. Flow:
- `loadBrowse(tab)` / `loadSearch()` → `withContext(Dispatchers.IO)` HTTP request → parse JSON on `Dispatchers.IO` → update `browseState`/`searchState` on `Dispatchers.Main`
- `importCharacter(character)` → POST to `/api/extract` with `JanitorExtractRequest(botId, characterId)` → parse `JanitorExtractResponse` → `extractToAssistant()` → `assistantVM.addAssistant()` → toast + hide sheet
- `CharacterCard` click → `selectedCharacter = character` → `bottomSheetState.show()` → `ModalBottomSheet` with unified design
- Bottom sheet "Import to Hub" → `importCharacter(character)` → `assistantVM.addAssistant()` → navigate flow

UI Layer: only renders state and emits intents. Zero business logic in composables.

## Recomposition & Memory Safety

- **No `!!`**: Verified zero force-unwraps across all three files. Nullable access uses `?:`, `takeIf`, `orEmpty()`, `isNullOrBlank()`.
- **Remember scoping**: `selectedTab`, `searchQuery`, `browseState`, `searchState`, `selectedCharacter`, `bottomSheetState`, `isImporting` all use `remember` / `rememberModalBottomSheetState`. `JanitorOrbitalLoader` uses `animateFloatAsState` with `infiniteRepeatable` — tied to composition lifecycle, stops when composition leaves.
- **FlowRow**: Replaces rigid `Row` — Compose foundation layout, stable across recompositions, responsive wrapping.
- **graphicsLayer `{ compositingStrategy = Offscreen }`**: Isolates offscreen rendering for alpha fade, preventing black square glitch on text overflow. No layout invalidation from the layer.
- **Tab bar zIndex**: `zIndex(1f)` on tab bar ensures it paints above scrolling cards — no z-fighting during scroll.
- **No naked objects**: All state is `remember`-scoped or `collectAsStateWithLifecycle`-collected. `animateFloatAsState` is properly scoped within `JanitorOrbitalLoader`.

## Threading & Dispatchers

- All UI animation, color, and layout work runs on `Dispatcher.Main` via Compose composition — correct and unchanged.
- `animateFloatAsState` in `JanitorOrbitalLoader` runs on Main thread — standard Compose animation API, no blocking.
- Network I/O strictly on `Dispatchers.IO` (`httpClient.execute` inside `withContext(Dispatchers.IO)`). JSON parsing on `Dispatchers.IO`. UI state updates marshalled to `Dispatchers.Main` via `withContext(Dispatchers.Main)`.
- `scope.launch` (Main dispatcher via `rememberCoroutineScope`) orchestrates the import flow, hopping to IO for the request and back to Main for state updates.
- No `Dispatchers.Default` misuse — CPU-light JSON parsing happens on IO which is acceptable for small payloads.

## Offline & Error State Handling

- **Fully local UI**: Header fade, tile logo, card design, orbital loader, and sheet styling work entirely offline. No network dependency in UI rendering.
- **Network errors**: `loadBrowse`/`loadSearch` catch exceptions → `JanitorLoadState.Error(message)` → displayed as `Text` in hub grid. `importCharacter` catches exceptions → `LocalToaster.current` `ToastType.Error`.
- **HTTP errors**: Non-2xx status codes → `JanitorLoadState.Error("HTTP ${response.statusCode}")` for browse/search; import shows `"Import failed: HTTP ${statusCode}"`.
- **Malformed JSON**: `JsonInstant.decodeFromString` wrapped in try/catch → error state.
- **Missing avatar**: `resolveAvatarUrl` returns `""` for blank/null → fallback to `default_generical_pfp` placeholder + fallback painter.
- **Null character fields**: `character.name.takeIf { it.isNotBlank() } ?: "Character"` fallback for display name. `character.creator ?: character.creatorName` nullable chains. `character.tags.forEach` safe on empty list.
- **Backward compatible**: `JanitorExtractRequest` still serializes both fields — server receives both `bot_id` and `character_id`. No breaking changes to existing API contract.
