# Audit Brief — Character Hub Polish & Card Overflow Fix (task-025)

## Intent Verification

All four directive requirements are implemented in the target files:

1. **OneUITopAppBar.kt** — Removed the `drawLine` bottom divider (lines 118-130) that appeared when `collapseProgress > 0.1f`. The `.then(Modifier.drawBehind { drawLine(...) })` block is deleted; the Column modifier chain is now just `.fillMaxWidth().height(currentHeight).background(barColor)` with zero separator line.

2. **CharacterHubPage.kt** — Bottom boundary in `CharacterPortraitCard` changed from harsh `Color.Black.copy(alpha = 0.85f)` to a smooth `Brush.verticalGradient` fading from `Color.Transparent` to `Color(0xFF141520).copy(alpha = 0.85f)`. Search bar pinned to top: the Column is split into a non-scrollable header (search bar + count + sort + actions) and a scrollable gallery Column with `weight(1f)` + `nestedScroll(collapseBehavior.nestedScrollConnection)`.

3. **CharacterHubPage.kt FAB** — `FloatingActionButton` restyled as circular glass: `containerColor = Color(0xFF141520).copy(alpha = 0.75f)`, 0.5dp white specular border via a vertical gradient brush (`Color.White.copy(alpha = 0.9f)` to `0.6f`), white `Icons.Rounded.Add` icon inside a 20.dp circle, `shape = CircleShape`.

4. **CharacterPortraitCard** — `graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` added to the container Box before `drawWithContent` alpha fade, eliminating the black square glitch on text overflow. Display name rendered with fallback: `assistant.name.takeIf { it.isNotBlank() } ?: "Character"`. Tag pills restyled with dark glass `Color(0xFF141520)` background and `0.5dp` `SpecularBorderSubtle` border, replacing the `Tag` component.

Strict null safety: no `!!` in either file. Unified glass theme `0xFF141520` enforced as FAB fill, card background, bottom scrim, and tag pill fill. Existing code style (4-space kt, KDoc, import ordering) preserved.

## Call Chain & Architecture

**OneUITopAppBar:** Used as the `topBar` in `Scaffold` within `CharacterHubPage`. Receives `scrollBehavior` from `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`. Reads `collapsedFraction` from scroll state to interpolate height, title alpha, and bar color. No divider is drawn on scroll. No ViewModel/Repository involvement — purely presentational.

**CharacterHubPage:** `AssistantVM` via `koinViewModel()` exposes `settings` (StateFlow) and assistant CRUD operations. `ModelCatalogService` snapshot collected for TTS/catalog data. Flow:
- `settings.assistants` → debounced search (250ms) on `Dispatchers.Default` → filtered/sorted on `Dispatchers.Main` → `filteredAssistants` state
- `CharacterPortraitCard(assistant, isDark, onClick)` → `inspectingAssistant = char` → `CharacterInspectorSheet` modal bottom sheet
- FAB `onClick` → `vm.addAssistant(Assistant(name = "New Character"))` → `navController.navigate(Screen.AssistantDetail(...))`
- Search bar text flows into `searchQuery` → debounced → `debouncedQuery` → filters assistants

**CharacterPortraitCard:** Pure UI component. Reads `assistant.tags`, renders `FullCharacterCardImage`, bottom glass scrim, `displayName` text with `drawWithContent` alpha fade, and horizontal `LazyRow` of tag pills. `CompositingStrategy.Offscreen` on the container graphicsLayer isolates offscreen rendering for the alpha fade, preventing the black square glitch.

## Recomposition & Memory Safety

- **No `!!`**: Verified zero force-unwraps across both files. Nullable access uses `?.`, `?:`, `takeIf`, `isNullOrBlank`, `orEmpty()`.
- **Remember scoping**: `searchQuery`, `debouncedQuery`, `isGridView`, `sortOption`, `inspectingAssistant`, `loadError`, `tagListState`, `gridState`, `listState` all use `remember` / `rememberLazyListState` / `rememberLazyGridState`. `displayName` is a local `val` derived from `assistant.name` during composition — no state retention needed.
- **Graphics layer isolation**: `CompositingStrategy.Offscreen` on `CharacterPortraitCard` container ensures the `drawWithContent` alpha fade renders to an offscreen buffer, preventing bleed/glitch when text overflows. No layout invalidation from the layer.
- **Tag pill replacement**: Removed `Tag` component usage (potential external styling side-effects) in favor of inline `Box` with `Color(0xFF141520)` background + `SpecularBorderSubtle` border — fully self-contained, no external dependencies.
- **FAB glass styling**: Inline `Box` with vertical gradient brush for the specular highlight, white icon — no black square artifact. `CircleShape` ensures circular clipping.
- **Scroll pinning**: Header Column (search bar + controls) is outside the scrollable `nestedScroll` Column — it never scrolls, so its state (`searchQuery`, `sortOption`) is stable across gallery scrolls.

## Threading & Dispatchers

- All UI animation, color, and layout work runs on `Dispatcher.Main` via Compose composition — correct and unchanged.
- Background work remains on proper dispatchers: import parsing on `Dispatchers.IO` (`AssistantExportImport.parseImport`), assistant search/filter on `Dispatchers.Default`, UI state updates marshalled back to `Dispatchers.Main`. No new dispatchers introduced or misused.
- `withContext(Dispatchers.IO)` / `withContext(Dispatchers.Main)` pattern preserved for export/import and conversation creation.
- No blocking calls on Main thread. The 250ms debouncer prevents keyboard freeze on large assistant lists.

## Offline & Error State Handling

- **Fully local UI**: Glass colors, FAB styling, card layout, and search filtering work entirely offline. No network dependency in the modified components.
- **Error boundary**: `loadError` state captures exceptions from assistant search/filter (try/catch around `withContext(Dispatchers.Default)` block) and conversation creation (try/catch around `vm.createConversation`). Errors surface via `CharacterHubErrorCard` (pinned in header) or `LocalToaster.current` (`ToastType.Error`).
- **Import/export errors**: Caught in `try/catch` blocks, surfaced via toaster. `pendingExportJson`/`pendingExportBytes` cleared after use.
- **Null safety on Assistant fields**: `assistant.name` uses `takeIf { it.isNotBlank() } ?: "Character"` fallback. `assistant.creator` uses `isNullOrBlank()` checks. `assistant.tags` iterated safely (empty list default).
- **Graceful degradation**: If `CompositingStrategy.Offscreen` is unsupported (unlikely on API 28+), Compose falls back to default strategy without crashing. If `assistant.tags` is empty, the LazyRow renders zero items — no crash.
