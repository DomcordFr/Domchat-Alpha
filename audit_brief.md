# Audit Brief — SettingJanitorPage Compiler Fix

## Intent Verification

Target: `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt`.
All 6 directive items applied and verified by cold inspection (no local gradle build, per constraint):

1. `foundation.lazy.GridCells`/`LazyVerticalGrid`/`items` → `foundation.lazy.grid.GridCells`/`LazyVerticalGrid`/`items` (imports lines 14–16).
2. `import androidx.compose.material3.TabIndicator` removed; custom `TabIndicator` slot removed from `TabRow`.
3. `import kotlin.io.encoding.Charsets` → `import kotlin.text.Charsets` (line 48).
4. `import org.koin.androidx.compose.koinInject` → `import org.koin.compose.koinInject` (line 72).
5. Added `import androidx.compose.runtime.getValue`, `import androidx.compose.runtime.setValue`, `import androidx.compose.material.icons.rounded.Search`.
6. `TabRow` now uses `selectedTabIndex = selectedTab` (line 318), matching the `AssistantMemorySubPage.kt` convention; no custom indicator.

Additional cleanup: `Icons.Rounded.Search` usage replaced with bare `Search` (line 338) so the new import is exercised, not unused.

Null-safety scan: zero `!!` force-unwraps. All nullable access uses Elvis (`?:`), `?.let`, `isNullOrBlank`, or `takeIf`.

Theme: existing unified glass card retained — card background `0xFF141520`, modal sheet `containerColor = Color(0xFF141520)`, border `LiquidGlassTokens.SpecularBorderBrush`, `AppShapes.Dialog`/`SearchField`. No style drift introduced.

## Call Chain & Architecture

`SettingJanitorPage()` is a stateless `@Composable` entry point registered in `RouteActivity` `Screen` sealed set (settings page). It owns UI state only; zero business logic.

DI:
- `koinInject<PlatformHttpClient>()` — HTTP client for JanitorAI Hub (`JANITOR_BASE_URL = https://yuki.domcord.org`).
- `koinViewModel<AssistantVM>()` — ViewModel for `assistantVM.addAssistant(assistant)` on import success.

Flow:
1. `LaunchedEffect(Unit)` → `loadBrowse("24h")` on composition.
2. `LaunchedEffect(selectedTab)` → reloads browse when tab switches (unless search tab).
3. `LaunchedEffect(searchQuery)` → triggers `loadSearch()` when query non-blank on search tab.
4. `loadBrowse`/`loadSearch` → `httpClient.execute(...)` on `Dispatchers.IO` → decode `JanitorListResponse` via `JsonInstant` → set `browseState`/`searchState` on `Dispatchers.Main`.
5. `importCharacter(character)` → POST `/api/extract` → `extractToAssistant(...)` → `assistantVM.addAssistant(...)` → toast + haptics + hide bottom sheet.
6. `selectedCharacter` non-null → `ModalBottomSheet` with `CharacterCard` grid via `LazyVerticalGrid` + `items(currentItems)`.

State derivation (snapshot-safe):
- `activeState` = `searchState` when `selectedTab == 2` else `browseState`.
- `currentItems`, `isLoading`, `loadError` derived from `activeState` in `when` branches. No simultaneous mutation of `browseState`/`searchState` within one transformation.

## Recomposition & Memory Safety

State holders: `selectedTab`, `searchQuery`, `browseState`, `searchState`, `selectedCharacter`, `isImporting` — all `var ... by remember { mutableStateOf(...) }`. `bottomSheetState = rememberModalBottomSheetState(...)`, `scope = rememberCoroutineScope()`, `scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`, `haptics = rememberPremiumHaptics()`. All Compose state is `remember`ed; no naked objects recompute each frame.

Lazy grid: `LazyVerticalGrid` with `items(currentItems)` — stable, keyed implicitly by position; `CharacterCard` is a private inline composable, no key instability. `derivedStateOf` not needed because `currentItems`/`isLoading`/`loadError` are simple `Boolean`/`List` derivations read directly, not mutable collections passed to `items(...)`.

Scope discipline: `scope.launch` used for event-driven imports/bottom-sheet show (tied to composition lifetime via `rememberCoroutineScope`). `LaunchedEffect` keys (`Unit`, `selectedTab`, `searchQuery`) prevent duplicate coroutines on recomposition.

`selectedCharacter?.let { ... }` guards the bottom sheet; null-safe, no leaks.

## Threading & Dispatchers

All network/disk off `Dispatchers.Main`:
- `withContext(Dispatchers.IO) { httpClient.execute(...) }` in `loadBrowse`, `loadSearch`, `importCharacter`.
- State mutations wrapped in `withContext(Dispatchers.Main) { ... }` after IO completes.

No `Dispatchers.Main` blocking. `CoroutineUtils`/`AppScope` not used here; composition-local scope is `rememberCoroutineScope()` (Main), which only launches then immediately `withContext(Dispatchers.IO)` for the actual work. `PlatformHttpClient` is `:common` `OkHttpPlatformHttpClient` (portable; no OkHttp import in this file).

## Offline & Error State Handling

Complete state matrix via sealed `JanitorLoadState`:

- `Loading`: initial + set before each request; renders `CircularProgressIndicator` in grid.
- `Success(items)`: renders `items(currentItems)` character cards; empty → "No characters found"/"No characters available".
- `Error(message)`: renders red error `Text` (`0xFFFF6B6B`) under tabs; state cleared on next successful load.

HTTP non-2xx → `Error("HTTP ${statusCode}")`; exception → `Error(e.message ?: e.toString())` (Elvis, no `!!`). Import failure → `ToastType.Error` toast + haptics `Error` pattern; `isImporting` reset to `false` in both success and failure branches. Success → `ToastType.Success` + `HapticPattern.Success`.

Network dependency: page is fully offline-unavailable by design (remote JanitorAI Hub only); no local-cache fallback exists in this slice. Offline manifests as `Exception` → error toast/message, never a crash.

## Proof Receipt

File edited: `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt`.
Lines changed (approx post-edit): imports 14–16, 19, 27–28, 32, 36, 48, 72; TabRow 317–319; Icon 338.
Verification method: cold-eye inspection + pattern grep for `!!`, `TabIndicator`, `kotlin.io.encoding`, `foundation.lazy` (non-grid), `selectedIndex` — all zero matches. No local gradle build performed (constraint).
