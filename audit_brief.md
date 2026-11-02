# Audit Brief — Fix Jai Hub Popover, Import Button Animation, Character Hub Images, and Dropdown Arrows

**Task:** task-067 — seven coordinated fixes across three files.

## Intent Verification

Confirmed against `task-067_directive.json`:

1. **Jai Popover Animation** — Refactored the inline `selectedCharacter?.let { ... ModalBottomSheet(...) }` block in `SettingJanitorPage.kt` into a self-contained `@Composable private fun JanitorCharacterInspectorSheet(...)`. It owns `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, calls `sheetState.show()` via `LaunchedEffect(character)` for smooth bottom-sheet entry, and exposes `dismissWithAnimation()` (guarded by `isDismissing`) that calls `sheetState.hide()` then `onDismiss()`. Page-level `bottomSheetState`, `viewerImageUrls`, `viewerOpen`, `viewerCurrentPage` removed; all state now lives inside the composable.

2. **Import Button Animation** — `buttonBgColor` changed from a plain `val` to `val buttonBgColor by animateColorAsState(targetValue = ..., animationSpec = tween(300), label = "ImportButtonBg")`. Button content wrapped in `AnimatedContent(targetState = importStatus, transitionSpec = { fadeIn(tween(150)) + fadeOut(tween(150)) }, label = "ImportButtonContent")` so the transition to the green "Imported!" checkmark is smooth.

3. **Import Coroutine Cancellation** — In the import `catch (e: Exception)` block, added `if (e is CancellationException) throw e` before setting error state, so sheet dismissal (cancellation) does not surface a false error toast.

4. **Preserve Embedded Images** — `embeddedImageUrls` now merges textBlobs from BOTH `character` and `fullExtract` (bio, description, firstMessage, alternateGreetings from each) instead of eliding one side with `?:`, so images never disappear when `fullExtract` finishes loading. Notes tab: `creatorNotes` containing `![` or `<img` takes precedence over plain extracted descriptions.

5. **Character Hub Popover Images** — In `CharacterInspectorSheet` (`CharacterHubPage.kt`), the Embedded Images gallery block was removed from between the hero image and the content area, and re-inserted below the Secondary Character Management buttons (Export, Clone, Delete) inside the content `Column` that already carries `.padding(horizontal = 20.dp)`, so it never touches screen edges.

6. **Unified Dropdown Chevron** — `Icons.Rounded.ArrowDropDown` → `Icons.Rounded.KeyboardArrowDown` in all three files; imports updated accordingly (`ArrowDropDown` import removed, `KeyboardArrowDown` import added where absent). `SettingLorebookDetailPage.kt` already imported `KeyboardArrowDown`; the unused `ArrowDropDown` import was removed.

7. **Jai Search respects feedMode** — `loadSearch()` normal query URL now appends `&sort=$sortParam` where `sortParam` maps `Popular → popular`, `Trending → trending`, `Trending 24h → 24h`, `Latest → latest`. Added `LaunchedEffect(feedMode)` that re-triggers `loadSearch()` when `feedMode` changes and `searchQuery` is not blank.

Files edited:
- `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt`
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt`
- `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingLorebookDetailPage.kt`

No Gradle build was run (per constraint).

Null safety: no `!!` introduced. All nullable reads use Elvis (`?:`), safe calls (`?.`), or `orEmpty()`. `conversation`/`character`/`assistant` references are non-null in their scopes.

Glass-card theme: `0xFF141520` preserved on image box backgrounds; no theme colors altered.

## Call Chain & Architecture

**SettingJanitorPage.kt**
- `SettingJanitorPage` (UI layer) renders the JanitorAI hub grid. On character card click, sets `selectedCharacter = character`; the `selectedCharacter?.let { JanitorCharacterInspectorSheet(...) }` call composes the sheet.
- `JanitorCharacterInspectorSheet` owns `ModalBottomSheet` with internal `sheetState`. `LaunchedEffect(character)` calls `sheetState.show()` on selection for smooth bottom-up animation. `dismissWithAnimation()` hides sheet then invokes `onDismiss()` (which clears `selectedCharacter`).
- Import flow: click → `scope.launch { ... }` → `withContext(Dispatchers.IO) { httpClient.execute(...) }` → `extractToAssistant(...)` → `assistantVM.addAssistant(...)`. ViewModel/Repository boundary handles persistence.
- `loadSearch()` executes search via `/api/search?q=...&sort=$sortParam` on `Dispatchers.IO`; results posted to `Dispatchers.Main`.
- `loadBrowse()` similar pattern for browse tabs.

**CharacterHubPage.kt**
- `CharacterHubPage` renders the character gallery; `inspectingAssistant` triggers `CharacterInspectorSheet(...)`. The sheet owns its own `sheetState` and `dismissWithAnimation()`.
- Embedded Images gallery now lives inside the content `Column(.padding(horizontal = 20.dp))` below Start Chat / Edit / Export / Clone / Delete buttons.
- `onStartChat` → `vm.createConversation(char.id)` on `Dispatchers.IO` → `navigateToChatPage(...)` on Main.

**SettingLorebookDetailPage.kt**
- Chevron-only change; no logic impact.

Layering respected: UI renders state and emits intents; ViewModel owns business logic and coroutine management; Repository handles data.

## Recomposition & Memory Safety

- `sheetState`, `viewerImageUrls`, `viewerOpen`, `viewerCurrentPage`, `isImporting`, `importStatus`, `fullExtract`, `isExtractLoading`, `sheetSelectedTab` all use `remember(...)` / `remember(character.id)` / `mutableStateOf` — Compose-lifecycle-aware, no naked objects.
- `buttonBgColor by animateColorAsState(...)` — recomputes only when `importStatus` changes; tween(300) is bounded.
- `AnimatedContent` with `fadeIn`/`fadeOut` tween(150) — bounded transitions, no infinite recomposition.
- `LaunchedEffect(character)` for `sheetState.show()` — runs once per character selection, keyed on `character`, no repeated execution.
- `LaunchedEffect(feedMode)` — runs only when `feedMode` changes, guarded by `searchQuery.isNotBlank()`.
- No `SnapshotStateList`/mutable collections passed to `items(...)`. All `items(...)` use stable keys (`it.id`, `it`).
- `rememberPremiumHaptics()` — Compose-lifecycle-aware haptics handle (consistent with other declarations in the file).
- No `!!` force-unwraps anywhere in the edited regions.

## Threading & Dispatchers

- Network (`httpClient.execute`) strictly on `Dispatchers.IO` via `withContext(Dispatchers.IO) {}`.
- State updates (`searchState = ...`, `browseState = ...`, `fullExtract = ...`, `isExtractLoading = ...`) marshalled back to `Dispatchers.Main` via `withContext(Dispatchers.Main) {}`.
- `scope.launch {}` (UI `rememberCoroutineScope()`) used for import coroutine and sheet show/hide — UI-scope, not `Dispatchers.Main` for I/O.
- `sheetState.show()` / `sheetState.hide()` are suspending ModalBottomSheetState calls, correctly invoked inside `scope.launch {}` / `LaunchedEffect`.
- No `Dispatchers.Main` used for disk/network; no `withContext(Dispatchers.Main)` for compute.
- `CancellationException` rethrow prevents spurious error toasts when the coroutine is cancelled by sheet dismissal.

## Offline & Error State Handling

- `JanitorLoadState` sealed class (`Loading` / `Success(items)` / `Error(message)`) explicitly models all states; UI renders loader, error text, or cards accordingly.
- Browse errors retain existing items when available (toaster shows "Failed to load more...") rather than wiping state.
- Search errors: `searchState = JanitorLoadState.Error("...")`; catch block shows generic unexpected-error toast.
- Import: `ImportStatus.IDLE / IMPORTING / SUCCESS / ERROR`; ERROR state renders red "Import Failed" button; catch block shows `toaster.show("Import error: ...", ToastType.Error)`.
- CancellationException rethrown — not swallowed as error — so dismissal never produces a false error toast.
- Undo pattern exists elsewhere in the codebase (delete flows) but import is fire-and-forget; no partial state to roll back on failure since `assistantVM.addAssistant` only runs on success.
- Offline: search/browse are network-dependent; errors surface via `ToastType.Error` and `JanitorLoadState.Error`. Local state (`currentItems`) is retained across transient failures.
