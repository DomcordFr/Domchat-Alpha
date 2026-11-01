# Audit Brief — JanitorAI Hub Redesign, 3-Tab Pre-Import Sheet & Full Data Mapping (task-029)

## Intent Verification

All four directive requirements are implemented in `SettingJanitorPage.kt`:

1. **CharacterCard** — Replaced multi-row `FlowRow` tags with a single horizontally scrollable `LazyRow` with compact tag pills (`0xFF141520` background, `0.5dp` `SpecularBorderSubtle` border, trailing fade gradient). Card uses `aspectRatio(0.68f)`, full-bleed portrait, `CompositingStrategy.Offscreen`, bottom glass scrim, display name with `?: "Character"` fallback — matching `CharacterPortraitCard` design exactly.

2. **Detail Sheet** — Overhauled to match `CharacterInspectorSheet` structure:
   - Hero image with bottom vertical gradient fade into pure black (`0xFF000000`)
   - Character title & creator displayed below hero
   - Primary "Import to Hub" button positioned right below title (matching "Start Chat" placement)
   - 3 inspection tabs (Notes, Personality, Greetings) with smooth `AnimatedContent` transitions — full inspection of definition/scenario/greetings BEFORE importing
   - Pure jet black background (`Color(0xFF000000)`) with `0.5dp` `SpecularBorderSubtle` specular glass borders on all cards/chips/tabs

3. **Import Mapping** — `JanitorExtractResponse` now parses `firstMessage`, `description`, `systemPrompt`, `scenario`, `tags`. `extractToAssistant` maps:
   - `firstMessage` → `presetMessages` (as `UIMessage` with `MessageRole.ASSISTANT`)
   - `description` → `creatorNotes`
   - `systemPrompt` → `systemPrompt`
   - String tags → registered into `settings.assistantTags` (new `Tag` entries with random UUIDs), UUIDs assigned to `assistant.tags`

4. **Controls** — `JanitorOrbitalLoader` (pulsing dark-glass orbital ring) shown during tab switching (`isTabSwitching` flag) and extraction (`isImporting` flag). Clean sort options without dividers.

Strict null safety: no `!!` in the file. Pure black `0xFF000000` popover enforced.

## Call Chain & Architecture

**SettingJanitorPage** is a setting page composable reached via `Screen.SettingJanitor`. State:
- `settings` — collected via `assistantVM.settings.collectAsStateWithLifecycle()`
- `selectedTab` — 0=Trending 24h, 1=Trending, 2=Search
- `browseState`/`searchState` — `JanitorLoadState` (Loading/Success/Error)
- `selectedCharacter` — currently inspected character
- `bottomSheetState` — `rememberModalBottomSheetState`
- `isImporting` — extraction in progress flag
- `isTabSwitching` — tab transition loading flag

Flow:
1. `loadBrowse(tab)` / `loadSearch()` → `withContext(Dispatchers.IO)` HTTP request → parse JSON → update state on `Dispatchers.Main`
2. Tab switch → `LaunchedEffect(selectedTab)` sets `isTabSwitching = true`, loads data, then `isTabSwitching = false`
3. `CharacterCard` click → `selectedCharacter = character` → `bottomSheetState.show()` → ModalBottomSheet
4. Sheet 3 tabs → `AnimatedContent` switches Notes/Personality/Greetings content
5. "Import to Hub" → `importCharacter(character)` → POST `/api/extract` with `JanitorExtractRequest(botId, characterId)` → parse `JanitorExtractResponse` → register new tags → `extractToAssistant(extract, updatedSettings)` → `assistantVM.addAssistant(assistant)` → toast + hide sheet

UI Layer: only renders state and emits intents. Zero business logic in composables.

## Recomposition & Memory Safety

- **No `!!`**: Verified zero force-unwraps. Nullable access uses `?:`, `takeIf`, `isNullOrBlank()`, `orEmpty()`.
- **Remember scoping**: `selectedTab`, `searchQuery`, `browseState`, `searchState`, `isTabSwitching`, `selectedCharacter`, `bottomSheetState`, `isImporting`, `sheetSelectedTab` all use `remember` / `rememberModalBottomSheetState` / `mutableIntStateOf`.
- **AnimatedContent**: Crossfades between `LazyVerticalGrid` and `LazyColumn` in main view, and between 3 sheet tabs — smooth transitions with `slideInHorizontally`/`slideOutHorizontally`.
- **LazyRow tag row**: Clean horizontally scrollable row for tag pills, avoiding any offscreen GPU artifacts.
- **CompositingStrategy.Offscreen**: On `CharacterCard` container, isolates offscreen rendering for alpha fade.
- **Tag UUID stability**: New tag UUIDs generated once in `importCharacter`, registered into `settings.assistantTags`, then `extractToAssistant` uses the updated settings so UUIDs match between assistant.tags and settings.assistantTags.

## Threading & Dispatchers

- UI composition, animation, and layout on `Dispatcher.Main` — correct.
- `JanitorOrbitalLoader` uses `rememberInfiniteTransition` + `animateFloat` — Main thread Compose animation API.
- Network I/O strictly on `Dispatchers.IO` (`httpClient.execute` inside `withContext(Dispatchers.IO)`). JSON parsing on `Dispatchers.IO`. UI state updates marshalled to `Dispatchers.Main` via `withContext(Dispatchers.Main)`.
- `scope.launch` (Main dispatcher) orchestrates the import flow, hopping to IO for the request and back to Main for state updates.
- Tab switching loading state (`isTabSwitching`) set/reset in `LaunchedEffect(selectedTab)` with try/finally to ensure reset even on error.

## Offline & Error State Handling

- **Fully local UI**: Card layout, sheet styling, tag registration, orbital loader all work offline.
- **Network errors**: `loadBrowse`/`loadSearch` catch exceptions → `JanitorLoadState.Error(message)` → displayed as error Text in hub grid. `importCharacter` catches exceptions → `LocalToaster.current` `ToastType.Error`.
- **HTTP errors**: Non-2xx status codes → `JanitorLoadState.Error("HTTP ${statusCode}")` for browse/search; import shows `"Import failed: HTTP ${statusCode}"`.
- **Malformed JSON**: `JsonInstant.decodeFromString` wrapped in try/catch → error state.
- **Missing avatar**: `resolveAvatarUrl` returns `""` for blank/null → fallback to `default_generical_pfp`.
- **Null character fields**: `character.name.takeIf { it.isNotBlank() } ?: "Character"` fallback. `character.creator ?: character.creatorName` nullable chains. `character.tags.forEach` safe on empty list. `extract.firstMessage`, `extract.description`, `extract.systemPrompt` all nullable with Elvis fallbacks.
- **Tag registration**: New tags only added if not already in `settings.assistantTags` (checked by name). Empty tag names filtered with `isNotBlank()`.
- **Atomic settings update**: `settingsStore.update { current -> ... }` in `AssistantVM.addAssistant` prevents lost updates when multiple assistants are added in quick succession.
