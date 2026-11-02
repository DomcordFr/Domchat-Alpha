# Audit Brief — Fix unresolved references in ChatPage delete confirmation dialog

**Task:** task-066 — fix two unresolved references in `ChatPageContent` in `ChatPage.kt`.

## Intent Verification

Confirmed against `task-066_directive.json`:

1. Declared `val haptics = rememberPremiumHaptics(enabled = setting.displaySetting.enableUIHaptics)` inside `ChatPageContent`, immediately after the `pendingDeleteMessage` state declaration (near line 1318, now line 1319). `rememberPremiumHaptics` is already imported at line 197 (`me.lucid.domchat.ui.hooks.rememberPremiumHaptics`); `setting` is in scope (`val setting by vm.settings.collectAsStateWithLifecycle()`, line 721).
2. In the `pendingDeleteMessage` confirmButton `onClick` (line 1893 → now line 1894), replaced `val backup = frameConversation` with `val backup = conversation`. `frameConversation` is scoped inside the `AnimatedContent` lambda (declared at line 1569 within `targetConversationId -> {...}`) and is out of scope at the delete-dialog confirm button, making it an unresolved reference. `conversation` (line 722) is in scope at the confirm button.

File edited:
- `app/src/main/java/me/lucid/domchat/ui/pages/chat/ChatPage.kt`

No Gradle build was run (per constraint).

Null safety: no `!!` operators introduced. `setting.displaySetting.enableUIHaptics` is a non-null Boolean read from a collected StateFlow. `conversation` is a non-null `Conversation` collected via `collectAsStateWithLifecycle()`. `rememberPremiumHaptics` returns a non-null `PremiumHaptics`.

Glass-card theme: `0xFF141520` theme color is untouched; no theme colors were modified.

## Call Chain & Architecture

- `ChatPageContent` is a Compose UI-layer composable in `ChatPage.kt` (`me.lucid.domchat.ui.pages.chat`). It renders the chat surface and emits user intents to the ViewModel (`vm`). Zero business logic lives here.
- `haptics` (new, line 1319) feeds `haptics.perform(HapticPattern.Error)` at the delete-dialog confirm button (line 1889). It is a UI-side haptics handle, created once via `rememberPremiumHaptics(...)` and gated by `setting.displaySetting.enableUIHaptics`.
- `conversation` (line 722) is the single source of truth for the active conversation, collected from `vm.conversation`. At the delete confirm button, `val removedIds = vm.deleteMessage(toDelete)` performs the deletion through the ViewModel/Repository boundary; `backup = conversation` captures the pre-deletion snapshot so the Undo toast action can call `vm.updateConversation(backup)` + `vm.markNodesAsRestored(removedIds)`.
- `scope.launch { ... }` (line 1893) uses `rememberCoroutineScope()` (line 711) — a UI-scope coroutine launched from Compose, not a ViewModel scope. It coordinates the delete + toast flow.
- Layering respected: UI renders state (`conversation`, `setting`, `haptics`), emits intents (`vm.deleteMessage`, `vm.updateConversation`, `vm.markNodesAsRestored`), no business logic in the composable.

## Recomposition & Memory Safety

- `haptics` is created with `rememberPremiumHaptics(...)`, which is Compose-lifecycle-aware (returns a remembered instance tied to the composition). No naked object; no per-recomposition allocation.
- `conversation` is collected via `collectAsStateWithLifecycle()` — lifecycle-aware, recomposes only when the value changes, no leaked collection.
- `remember { mutableStateOf<...>(null) }` for `pendingDeleteMessage` — standard Compose state, survives recomposition, cleared (`= null`) on dismiss and after confirm.
- `scope.launch {}` captures `conversation` by value at confirm time (`val backup = conversation`), so the Undo lambda holds a stable snapshot even if `conversation` changes later. No dangling references to a mutable StateFlow.
- No `SnapshotStateList`/mutable collections passed to `items()`. No `derivedStateOf` concerns here.
- `haptics` reads `setting.displaySetting.enableUIHaptics` at composition time; if the setting changes, `rememberPremiumHaptics` re-runs only when its `enabled` parameter changes (keyed by the factory), consistent with other `haptics` declarations elsewhere in the file (lines 2326, 2494, 2697, 3105, etc.).

## Threading & Dispatchers

- `haptics.perform(HapticPattern.Error)` runs on the Compose UI/Main thread — haptics are a UI-side, synchronous effect. This is correct; haptics must be performed on Main.
- `vm.deleteMessage(...)`, `vm.updateConversation(...)`, `vm.markNodesAsRestored(...)` are suspending ViewModel calls invoked inside `scope.launch {}`. Per architecture, the ViewModel routes network/disk work to `Dispatchers.IO` internally; the UI composable never calls `Dispatchers.Main` for I/O and never launches background work directly on Main beyond the UI effect itself.
- No `Dispatchers.IO` / `Dispatchers.Main` references are introduced or violated in this file at these edit points. No `withContext` needed here.
- `LaunchedEffect(vm)` (line 715) for error flow collection remains untouched; already lifecycle-aware.

## Offline & Error State Handling

- Delete confirmation is a user-initiated, local-database-driven action (message deletion via Repository → Room). It does not depend on network, so Offline is not a failure mode for the delete itself.
- Error states: `vm.errorFlow` is collected via `LaunchedEffect(vm)` (line 715) and surfaced through `toaster.show(...)` with `ToastType.Error` — generic error handling covers exceptions from `deleteMessage`/`updateConversation`/`markNodesAsRestored`.
- The Undo toast (`R.string.undo`) provides recovery: on click, `vm.updateConversation(backup)` restores the pre-deletion conversation and `vm.markNodesAsRestored(removedIds)` restores removed message nodes — explicit Success/rollback path.
- Loading state is implicit (the delete is fast/local); no separate Loading UI required for this dialog. `isGenerating` (line 1331) is unrelated to delete flow.
- All UI strings (`Delete message?`, `Are you sure...`, `message_deleted`, `undo`, `delete`) are resolved via `context.getString` / `stringResource` — no hardcoded strings, consistent with i18n setup.
