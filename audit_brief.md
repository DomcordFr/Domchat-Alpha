# Audit Brief — task-018: Cloud JanitorAI Backend Endpoint

## Intent Verification
Target: `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt`.
Change applied: `JANITOR_BASE_URL` reassigned from `http://127.0.0.1:8899` to `https://yuki.domcord.org` at line 73. The exact URL `https://yuki.domcord.org` is used verbatim, with no trailing slash. All downstream call sites (`/api/browse?tab=`, `/api/search?q=`, `/api/extract`) concatenate against the constant, so they now resolve against the cloud endpoint.

Verified references after edit:
- Line 73: declaration.
- Line 167: `loadBrowse` GET browse.
- Line 199: `loadSearch` GET search.
- Line 230: `importCharacter` POST extract.

No `!!` operators were introduced or remain in the touched region; nullable handling uses `?:`, `isNullOrBlank()`, `?.let`, and `takeIf { }?.let { }` guards only.

## Call Chain & Architecture
UI layer (`SettingJanitorPage`) is a `@Composable` function. It owns three `remember { mutableStateOf(...) }` load states (`browseState`, `searchState`) and one import flag (`isImporting`).

Call chain per action:
1. `LaunchedEffect(Unit)` → `loadBrowse("24h")` on page entry.
2. `LaunchedEffect(selectedTab)` → `loadBrowse("24h" | "trending")` on tab switch (non-search tabs).
3. `LaunchedEffect(searchQuery)` → `loadSearch()` when search tab active and query non-blank; otherwise resets to `Success(emptyList())`.
4. Character card click → `selectedCharacter = character` → bottom sheet opens → `importCharacter(character)` → POST `/api/extract` → `extractToAssistant(...)` → `assistantVM.addAssistant(assistant)`.

Network transport: `koinInject<PlatformHttpClient>()` → `httpClient.execute(PlatformHttpRequest(...))` inside `withContext(Dispatchers.IO)`. JSON (de)serialization via `JsonInstant` (`ignoreUnknownKeys=true, encodeDefaults=true, coerceInputValues=true`; no snake_case strategy, field mapping manual via `@SerialName`).

ViewModel boundary: `AssistantVM` (injected via `koinViewModel`) receives only the final `Assistant` domain object on `Dispatchers.Main`; the page performs zero business logic beyond state transitions and intent emission.

Glass card theme: `Color(0xFF141520)` used for `ModalBottomSheet.containerColor` (line 418) and `CharacterCard` background (line 497), matching the DomChat unified glass card theme. Border uses `LiquidGlassTokens.SpecularBorderBrush` / `SpecularBorderSubtle`.

## Recomposition & Memory Safety
All mutable UI state is scoped with `remember { mutableStateOf(...) }`:
- `selectedTab`, `searchQuery`, `browseState`, `searchState`, `selectedCharacter`, `isImporting`.
- `bottomSheetState` via `rememberModalBottomSheetState(skipPartiallyExpanded = true)`.
- `scope` via `rememberCoroutineScope()`.
- `haptics` via `rememberPremiumHaptics()`.
- `scrollBehavior` via `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`.

`LazyVerticalGrid` consumes `currentItems: List<JanitorCharacter>` (immutable `List`) derived from `activeState`; no `SnapshotStateList` is passed to `items(...)`, avoiding unstable collection recompositions. State writes (`browseState = ...`, `searchState = ...`) occur inside `withContext(Dispatchers.Main)` blocks, so recomposition is triggered on the main thread only after the IO coroutine suspends and resumes.

No naked objects in Compose: `httpClient`, `assistantVM`, `toaster`, `haptics`, `scope` are all obtained via `koinInject` / `koinViewModel` / `LocalToaster.current` / `rememberPremiumHaptics()` / `rememberCoroutineScope()`.

## Threading & Dispatchers
- Network I/O: `withContext(Dispatchers.IO)` wraps every `httpClient.execute(...)` call (lines 163-170, 195-202, 226-235).
- State mutation: `withContext(Dispatchers.Main)` wraps all `browseState` / `searchState` / `isImporting` / `selectedCharacter` assignments and `toaster.show` / `haptics.perform` calls.
- Coroutine launch: `scope.launch { ... }` (Composition-local scope from `rememberCoroutineScope()`) — lifecycle-aware, cancelled when composition leaves the composition.
- No `Dispatchers.Main` used for disk/network; no `Dispatchers.IO` used for UI state writes.
- `AssistantVM.addAssistant(assistant)` is invoked on `Dispatchers.Main`; the VM is responsible for its own internal dispatcher discipline.

## Offline & Error State Handling
Load states are explicit and exhaustive via sealed class `JanitorLoadState`:
- `Loading` → `CircularProgressIndicator` shown in grid (lines 375-383).
- `Success(items)` → grid renders `items`; empty non-loading state shows "No characters available" / "No characters found" (lines 384-398).
- `Error(message)` → error text rendered in red `Color(0xFFFF6B6B)` (lines 358-365).

HTTP non-2xx branches produce `JanitorLoadState.Error("HTTP ${response.statusCode}")` for browse/search, and a toast `Import failed: HTTP ${response.statusCode}` for import.
Exception branches produce `JanitorLoadState.Error(e.message ?: e.toString())` (Elvis operator, no `!!`) for browse/search, and toast `Import failed: ${e.message ?: e.toString()}` for import.
Import success shows `ToastType.Success`; import failure shows `ToastType.Error`; haptics `HapticPattern.Success` on success only.

Import in-flight guard: `Button(enabled = !isImporting)` prevents duplicate submissions; `isImporting` reset to `false` in both success and failure branches on `Dispatchers.Main`.
Offline (no network) surfaces as caught `Exception` → `Error` state / error toast, with no crash and no partial state leak.
