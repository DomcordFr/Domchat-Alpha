# Audit Brief — SettingJanitorPage Fix

## Intent Verification

Target: `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt`

All three directive items applied:

1. Imports added (line 36): `import androidx.compose.runtime.LaunchedEffect`
   Imports added (line 45): `import androidx.compose.ui.input.nestedscroll.nestedScroll`
   Both symbols were already referenced at lines 278+ (`LaunchedEffect`) and 314 (`Modifier.nestedScroll(...)`) without imports — the file would not compile cold. Now resolved.

2. Explicit type parameters (lines 148–149):
   `var browseState by remember { mutableStateOf<JanitorLoadState>(JanitorLoadState.Loading) }`
   `var searchState by remember { mutableStateOf<JanitorLoadState>(JanitorLoadState.Loading) }`
   Removes inference ambiguity across the `JanitorLoadState` sealed hierarchy (`Loading` / `Success` / `Error`).

3. Line 459–466 bio block rewritten to strict null-safe form:
   ```kotlin
   val displayBio = (character.bio ?: character.description).orEmpty().trim()
   if (displayBio.isNotBlank()) {
       Text(
           text = displayBio,
           style = MaterialTheme.typography.bodyMedium,
           color = Color.White.copy(alpha = 0.9f),
       )
   }
   ```
   Replaces the `?.orEmpty().takeIf { }.?.let { }` chain. No `!!`, no nullable leakage into `Text`.

Invariants held: zero `!!` (grep confirmed no matches), glass card `0xFF141520` retained on ModalBottomSheet and CharacterCard backgrounds, existing code style preserved.

## Call Chain & Architecture

`SettingJanitorPage()` is a stateless `@Composable` rendered inside `RouteActivity`'s `NavHost` (`Screen` sealed interface) via `composable<Screen.X>` registration. UI layer only:

- Injects `PlatformHttpClient` (Koin `koinInject`) and `AssistantVM` (`koinViewModel`).
- Local UI state: `selectedTab`, `searchQuery`, `browseState`, `searchState`, `selectedCharacter`, `isImporting`, `bottomSheetState`.
- Network fetch functions `loadBrowse(tab)`, `loadSearch()`, `importCharacter(character)` launch coroutines via `rememberCoroutineScope()` and write to local `MutableState` (browseState/searchState) — no ViewModel involvement; this page is a UI-only widget per the local state pattern.
- `LaunchedEffect(Unit)` triggers initial browse load ("24h"); `LaunchedEffect(selectedTab)` / `LaunchedEffect(searchQuery)` re-trigger loads on tab/query changes.
- `selectedCharacter` opens `ModalBottomSheet` with `CharacterCard`-style detail row and `importCharacter` action, which POSTs to `.../api/extract` and hands the result to `assistantVM.addAssistant(assistant)`.

Call chain: user tab tap → `selectedTab = index` → `LaunchedEffect(selectedTab)` → `loadBrowse` → `PlatformHttpClient.execute(...)` (IO) → JSON decode via `JsonInstant` → `browseState = Success(items)` → `LazyVerticalGrid` recomposes → `CharacterCard` click → `bottomSheetState.show()` → import → `assistantVM.addAssistant` → `Conversation` repository write.

## Recomposition & Memory Safety

- All mutable UI state held in `remember { mutableStateOf<...>() }` — survives recomposition, recreated only on composition disposal.
- `rememberCoroutineScope()` bound to composition lifecycle; `scope.launch` jobs are not auto-cancelled on disposal but composition scope disposal ends the coroutine scope.
- `rememberModalBottomSheetState(skipPartiallyExpanded = true)` — sheet state remembered.
- `activeState` / `currentItems` / `isLoading` / `loadError` are derived val computed during composition from `browseState`/`searchState` — cheap, no allocations in hot paths beyond small `when` branches.
- `LazyVerticalGrid` `items(currentItems)` — stable `List<JanitorCharacter>`; `@Serializable` data class is immutable.
- `displayBio` is a local `String` val; `if (displayBio.isNotBlank())` guard prevents empty `Text` nodes.
- No snapshot reads of mutable collections; `JanitorLoadState` instances are immutable sealed objects/data classes — safe for concurrent assignment from `withContext(Dispatchers.Main)` blocks.
- `selectedCharacter?.let { ... }` at bottom-sheet call site — nullable access, no `!!`.

## Threading & Dispatchers

- All network/disk work confined to `withContext(Dispatchers.IO)` inside `scope.launch` coroutines (`CoroutineScope(SupervisorJob() + Dispatchers.Main + ...)` via `rememberCoroutineScope()` — Main dispatcher for UI launches).
- State mutations (`browseState = ...`, `searchState = ...`, `isImporting = ...`, `selectedCharacter = ...`) performed inside `withContext(Dispatchers.Main) {}` after IO — Compose state touched only on Main thread.
- `httpClient.execute(...)` is blocking Ktor/OkHttp call on IO — never on Main.
- `assistantVM.addAssistant(assistant)` called on Main inside import success block (VM responsible for its own `viewModelScope` threading).
- No `GlobalScope`, no `launch(Dispatchers.IO)` top-level, no `withContext(Dispatchers.Main)` around non-UI work.

## Offline & Error State Handling

- `JanitorLoadState` sealed: `Loading`, `Success(items)`, `Error(message)` — all four phases represented.
- Loading: initial state `Loading` for both `browseState` and `searchState`; `CircularProgressIndicator` shown while `isLoading`.
- Success: `currentItems` populates `LazyVerticalGrid`; empty-state text when `currentItems.isEmpty() && !isLoading` ("No characters found" / "No characters available").
- HTTP error: non-2xx status → `JanitorLoadState.Error("HTTP ${response.statusCode}")`, rendered as red `Text` (`Color(0xFFFF6B6B)`).
- Exception: caught in `catch (e: Exception)` → `JanitorLoadState.Error(e.message ?: e.toString())` — Elvis chain, no `!!`.
- Import failure: HTTP error and exception paths both show `ToastType.Error` via `LocalToaster.current` and reset `isImporting = false` on Main.
- `importCharacter` sheet dismissal and `selectedCharacter = null` on success; bottom sheet hides on import completion.
- Offline manifest: any `IOException`/timeout from `httpClient.execute` lands in the catch branch → Error state shown; UI never crashes on network failure.
- Null safety across optional fields: `creator.bio ?: creator.description`, `creator.creator ?: creator.creatorName`, `extract.name ?: character.name`, `extract.bio ?: extract.description ?: ""`, `extract.avatarHash ?: extract.avatarHash2` — all Elvis operators, no `!!`.

## Edit Receipt

File: `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt`
- Line 36: inserted `import androidx.compose.runtime.LaunchedEffect`
- Line 45: inserted `import androidx.compose.ui.input.nestedscroll.nestedScroll`
- Lines 148–149: explicit `mutableStateOf<JanitorLoadState>(...)` on `browseState` and `searchState`
- Lines 459–466: replaced nullable chain with `val displayBio = (character.bio ?: character.description).orEmpty().trim()` + `if (displayBio.isNotBlank())` Text block

No local Gradle build performed (constraint). File read-verified: imports present, explicit type params present, zero `!!` occurrences.
