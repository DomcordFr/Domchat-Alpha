# Audit Brief — Task-057: Character Hub Tag Filtering & Unified Sort Dropdown

## Intent Verification

All five requirements from the DualBrain Lead Architect Directive are implemented:

1. **Character count glass badge** (both Grid and List headers):
   - Changed padding from `padding(horizontal = 10.dp, vertical = 5.dp)` to `padding(horizontal = 12.dp, vertical = 8.dp)` to match Sort/Filter pill buttons
   - Badge styling confirmed: `RoundedCornerShape(20.dp)`, `Color(0xFF000000)` background, `0.5.dp` `LiquidGlassTokens.SpecularBorderSubtle` border, `11.5.sp` fontSize, `FontWeight.Medium`, `Color.White.copy(0.85f)` text

2. **Unified Sort Dropdown pills** (both Grid and List headers):
   - Both `DropdownMenu` calls now include `shape = RoundedCornerShape(20.dp)` and `containerColor = Color(0xFF000000)`
   - Existing `0.5.dp` `LiquidGlassTokens.SpecularBorderSubtle` with `RoundedCornerShape(20.dp)` border confirmed present

3. **CharacterInspectorSheet drag handle:**
   - Added `dragHandle = null` to `ModalBottomSheet` (line 1562), removing the default black pill handle at top of popover

4. **AnimatedContent transition:**
   - Added `contentAlignment = Alignment.TopCenter` (line 359)
   - Added `transitionSpec = { _, _ -> SizeTransform(clip = false) { _, _ -> snap() } }` (line 358)

5. **CharacterListRow card styling:**
   - Card background: `Color(0xFF000000)` (already present at line 1412)
   - Card border: `0.5.dp` `LiquidGlassTokens.SpecularBorderSubtle` with `RoundedCornerShape(16.dp)` (already present at lines 1418-1420)
   - Intros badge updated: `RoundedCornerShape(20.dp)`, black background, `0.5.dp` SpecularBorderSubtle border, `padding(horizontal = 10.dp, vertical = 4.dp)`, `11.5.sp` fontSize, `FontWeight.Medium`, `Color.White.copy(0.92f)` text

## Call Chain & Architecture

`CharacterHubPage.kt` — single-activity route host via `RouteActivity`:
- `CharacterHubPage()` composable — main Character Hub page with grid/list toggle and search
- `sortOption` state (`CharacterSortOption`) — current sort mode
- `sortMenuOpen`, `filterMenuOpen` — dropdown/sheet visibility state  
- `selectedTagIds` (`Set<String>`) — selected tag IDs for filtering
- `filteredAssistants` LaunchedEffect — combines search + tag filter + sort, runs on `Dispatchers.Default`, posts result to `Dispatchers.Main`
- `CharacterInspectorSheet()` — character popover modal sheet with `dragHandle = null`
- `CharacterPortraitCard()` / `CharacterListRow()` — character card composables
- `FullCharacterCardImage()` — avatar image composable with `Alignment.TopCenter` + `ContentScale.Crop`

UI layer only: renders state and emits user intents; zero business logic.
ViewModel: `AssistantVM` via `koinViewModel()`.

No new `Screen` routes, ViewModels, or DI changes — all changes are intra-composable UI refinements.

## Recomposition & Memory Safety

- All state variables properly `remember`ed — `sortOption`, `sortMenuOpen`, `filterMenuOpen`, `selectedTagIds`
- `filteredAssistants` recomputes when `settings.assistants`, `debouncedQuery`, `sortOption`, or `selectedTagIds` changes
- All `Text` styles use standard `MaterialTheme.typography.bodyMedium` without custom `platformStyle` — simpler recomposition
- Zero `!!` force-unwraps — verified by grep across both files
- All state is `remember`ed or collected with lifecycle awareness

## Threading & Dispatchers

- No new coroutines introduced beyond existing `LaunchedEffect` pattern
- Filtered assistant computation runs on `Dispatchers.Default` (CPU-bound filtering/sorting)
- UI state updates posted to `Dispatchers.Main`
- Existing `withContext(Dispatchers.IO)` / `withContext(Dispatchers.Main)` patterns unchanged

## Offline & Error State Handling

- Tag filtering is fully offline — uses in-memory `settings.assistantTags` and `char.tags`, no server dependency
- Empty `selectedTagIds` short-circuits to show all items (no filtering)
- `char.tags.any { tagId -> selectedTagIds.contains(tagId.toString()) }` — safe nullable access, empty tags list yields false
- All error states (loadError) remain unchanged and unaffected by tag filtering
- `AsyncImage` handles loading/error states internally via Coil3
