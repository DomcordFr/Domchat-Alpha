# Audit Brief — Task 172: Wire Centered Tab Context Menu Dialog

## Intent Verification

The missing `AlertDialog` for the Greetings tab context menu has been rendered in `AssistantGreetingsSubPage.kt`.
- Placed immediately after the `Column` body and before the closing brace of `AssistantGreetingsSubPage`, so it sits as a sibling overlay (not inside the scrollable Column) and precedes `GlassyGreetingTab`.
- Gated by `if (showEditDialog)` — the flag is already set to `true` on long-press of a greeting tab (existing line 121) and reset to `false` by each action and by `onDismissRequest`.
- `modifier = Modifier.widthIn(max = 420.dp)` enforces the mobile-first max-width constraint.
- Title "Manage Greeting"; three conditional pill buttons: Move Forward (guard `selectedGreetingIndex > 0`), Move Backward (guard `selectedGreetingIndex < allGreetings.size - 1`), Delete Greeting (guard `allGreetings.size > 1`). Dismiss button "Cancel".
- Each action copies `allGreetings` to a mutable list, mutates the copy, rebuilds `presetMessages` as `UIMessage(role = ASSISTANT, parts = [Text(...)])`, calls `onUpdate(assistant.copy(...))`, adjusts `selectedGreetingIndex` defensively (`coerceAtMost` on delete), then dismisses.
- Matches the existing `presetMessages`-based greeting model already used by the save button (lines 232-245), not a nonexistent `greetings` field.

Correction applied vs. directive: `DomChatGlassPillButton(text = ..., onClick = { ... })` uses an explicit `onClick` named argument. The component signature is `(text, onClick, modifier = ..., icon, ...)` where `onClick` has no default and is not the trailing-lambda target (`content` is last). A trailing lambda would bind to `content` and leave required `onClick` unset, causing a compile error. Existing usage at line 127 confirms the explicit `onClick = { ... }` style.

## Call Chain & Architecture

UI layer only:
1. User long-presses a `GlassyGreetingTab` → `onLongPress` sets `editIndex`, `editText`, `showEditDialog = true` (existing code).
2. Recomposition: `showEditDialog == true` adds the `AlertDialog` to composition. Material3 `AlertDialog` presents as an overlay via `Dialog`.
3. User taps Move Forward / Move Backward / Delete Greeting → `DomChatGlassPillButton.onClick` mutates a local copy of `allGreetings`, calls `onUpdate(assistant.copy(presetMessages = ...))`, clamps `selectedGreetingIndex`, sets `showEditDialog = false`.
4. `onUpdate` is the caller-provided lambda (ViewModel-level), which persists the `Assistant` through the repository/DAO. The UI emits intent only; zero business logic here.
5. User taps Cancel or dismisses → `showEditDialog = false`, dialog leaves composition.

Architectural boundary respected: UI renders state and emits intents via `onUpdate`; persistence lives downstream in the ViewModel/repository, not in this composable.

## Recomposition & Memory Safety

- All state vars are `remember { mutableStateOf(...) }`: `selectedGreetingIndex`, `showEditDialog`, `editIndex`, `editText`. No naked objects; no `rememberSaveable` needed (dialog is ephemeral UI state).
- `allGreetings` is `remember(assistant.presetMessages, assistant.alternateGreetings) { ... }`; dialog reads it and copies via `.toMutableList()` before mutating, avoiding mutation of the remembered list during composition.
- `selectedGreetingIndex` adjustments (`-= 1`, `+= 1`, `coerceAtMost`) are guarded by the preceding `if` conditions, so no out-of-bounds index.
- `showEditDialog = false` on every action and `onDismissRequest` ensures the dialog leaves composition after a single frame, preventing stale dialog state.
- No `!!` force-unwraps (grep-verified: zero matches in the file). All nullable access uses guards/Elvis where applicable; this edit introduces no nullable types.
- Diff is 76 insertions in a single file — within scope, no bloat.

## Threading & Dispatchers

- This is purely UI-thread Compose code (`@Composable`, invoked on `Dispatchers.Main`). No coroutines, no `launch`, no `withContext`, no dispatchers used directly in this file.
- No disk or network access occurs here; `onUpdate` hands the updated `Assistant` to the caller (ViewModel), which is responsible for offloading persistence to `Dispatchers.IO` per existing repository patterns.
- No background threading introduced; invariant "ZERO background threading on Dispatchers.Main" trivially satisfied.

## Offline & Error State Handling

- Fully offline-capable: all operations are in-memory list copies + local `onUpdate` callback. No network, no remote fetch, no cached fallback needed at this layer.
- Error states: none introduced by this dialog. If `onUpdate` throws downstream, that is the caller's responsibility; the dialog itself has no try/catch boundary because it performs no fallible I/O.
- Guard conditions prevent invalid index operations (Move Forward requires index > 0; Move Backward requires index < size - 1; Delete requires size > 1). Delete clamps index via `coerceAtMost(list.size - 1)`.
- Empty greetings list: dialog shows no buttons (all guards false) plus Cancel — a valid no-op state.
