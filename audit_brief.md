# Audit Brief — Character Hub Crash + Chat Bar Plus Icon Glass Border

## Intent Verification

| Requirement | Status | Evidence |
|---|---|---|
| Zero `verticalScroll` on Column wrapping `LazyVerticalGrid` | Done | `CharacterHubPage.kt:325-330` root `Column` no longer has `.verticalScroll(rememberScrollState())`; only `.fillMaxSize().nestedScroll(...).padding(padding)` remains. |
| `Modifier.weight(1f)` on `AnimatedContent` gallery | Done | `CharacterHubPage.kt:462-463` `AnimatedContent(..., modifier = Modifier.weight(1f).fillMaxWidth())`. Gallery now takes finite remaining height instead of infinite. |
| Plus button border `0.5.dp LiquidGlassTokens.SpecularBorderSubtle` | Done | `MinimalChatInput.kt:860` `border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape)` on `DomChatComposerAddButton`. `DomChatComposerShell.kt:79` added backward-compatible `border: BorderStroke? = null` param; `ui-core` cannot import app tokens, so the app layer supplies the token. |
| Strict null safety, no `!!` | Done | `git diff` contains no `!!`. New param is nullable (`BorderStroke?`) and consumed via `border ?: when { ... }`. |
| No local Gradle builds | Done | Edits applied by inspection only; no `./gradlew` invoked. |
| Search bar + filter chips pinned at top | Unchanged | They precede the weighted `AnimatedContent` in the same `Column`, so they keep natural height above the scrolling gallery. |

## Call Chain & Architecture

**Character Hub crash fix**
- `CharacterHubPage` (UI, `@Composable`) renders a `Scaffold` whose content `Column` previously carried `.verticalScroll(rememberScrollState())`. That gave the `Column` infinite height constraints, under which the nested `LazyVerticalGrid` (grid mode) / `LazyColumn` (list mode) threw `IllegalStateException` ("infinite height constraints").
- Fix removes `verticalScroll` from the `Column` and weights the `AnimatedContent` gallery container (`weight(1f).fillMaxWidth()`), giving the gallery finite remaining height. The lazy list/grid inside receives bounded constraints. Search bar, count/sort row, actions row, and error card stay above and keep natural size.
- `LazyVerticalGrid`/`LazyColumn` still own their own scroll state (`gridState`/`listState`) and scroll independently. `inspectingAssistant?.let` (sheet/dialogs) and `CharacterInspectorSheet` are siblings of the `Scaffold`, unaffected.

**Plus button glass border fix**
- `DomChatComposerAddButton` (`ui-core/DomChatComposerShell.kt`) is the shared add/record pill used by Android (`MinimalChatInput.kt:843`) and iOS (`iosApp/.../DomChatIosApp.kt:577`). It internally drew `BorderStroke(SoftEdgeWidth=0.5.dp, Color.White.copy(alpha=0.40f))` when `containerColor == Color.Transparent` — a high-contrast opaque white rim.
- Added `border: BorderStroke? = null` (default preserves existing behavior for all current callers, incl. iOS). `MinimalChatInput.kt` now passes `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape)`, which wins via `border ?: when { ... }`. Width unchanged (`0.5.dp = AppSurface.SoftEdgeWidth`); only the brush goes from opaque white to the subtle vertical-gradient specular border.

## Recomposition & Memory Safety

- `CharacterHubPage`: all local UI state (`searchQuery`, `isGridView`, `sortOption`, `inspectingAssistant`, etc.) is held in `remember { mutableStateOf(...) }`. `filterAssistants` is `remember`-backed and updated on `Main` after `withContext(Dispatchers.Default)` filtering. No naked mutable collections passed to `LazyVerticalGrid`/`LazyColumn` `items(...)` — `filteredAssistants` is read as a value snapshot at recomposition. `gridState`/`listState` are `rememberLazyGridState()`/`rememberLazyListState()`.
- `AnimatedContent` with `weight(1f)` recomposes only when `isGridView` changes; `LazyVerticalGrid`/`LazyColumn` reuse items via keys (`it.id.toString()`), so character cards are stable across scroll/recomposition.
- `MinimalChatInput` / `DomChatComposerAddButton`: `border` is a plain immutable `BorderStroke?` parameter — zero recomposition overhead, no memory retention. `SpecularBorderSubtle` is a stateless `Brush` object (module-level `val`), reused across recompositions.
- No `!!`, no `lateinit` introduced, no leaked coroutine scopes. `rememberScrollState`/`verticalScroll` imports remain used elsewhere (`CharacterInspectorSheet` at `CharacterHubPage.kt:1014`), so no dangling imports.

## Threading & Dispatchers

- No new coroutines introduced in this change. Existing `CharacterHubPage` search filtering already routes CPU work to `Dispatchers.Default` and state writes to `Dispatchers.Main`; DB/import/export work routes to `Dispatchers.IO`.
- `DomChatComposerAddButton` and `MinimalChatInput` border change is purely compositional (no dispatchers, no suspend functions).
- No `Dispatchers.Main` used for I/O; no `withContext(Dispatchers.Main)` misuse. `AppScope`/`viewModelScope` untouched.

## Offline & Error State Handling

- Character Hub load errors: `LaunchedEffect(settings.assistants, ...)` catches exceptions and sets `loadError = e` on `Main`, rendering `CharacterHubErrorCard` (fallback card, `error.message?.takeIf { it.isNotBlank() } ?: error.toString()`). Crash on open is eliminated because the `LazyVerticalGrid` no longer receives infinite height constraints.
- `ChatAttachmentManager.importChatFile`/imports, PNG/JSON export (`AssistantExportImport`) all wrap `runCatching`/try-catch with `ToastType.Error` toasts; `openOutputStream(uri)?.use { }` is null-safe (elvis `?`).
- Plus button: purely visual; no network/disk/error handling involved. `containerColor = Color.Transparent` remains valid because the explicit `border` parameter overrides the `when` fallback.
- Offline: none of the touched code paths depend on network; catalog/settings flows are unchanged.

## Files Changed

- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` — removed `verticalScroll`; added `Modifier.weight(1f).fillMaxWidth()` to `AnimatedContent`.
- `app/src/main/java/me/lucid/domchat/ui/components/ai/MinimalChatInput.kt` — plus button now passes `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape)`.
- `ui-core/src/commonMain/kotlin/me/lucid/domchat/ui/components/chat/DomChatComposerShell.kt` — `DomChatComposerAddButton` gained `border: BorderStroke? = null` (backward compatible).
