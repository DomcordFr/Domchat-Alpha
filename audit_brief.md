# Audit Brief — Task-076: Chat Message Swipe Reroll Generation and Custom Initial Message Creator

## Intent Verification

The implementation satisfies the Lead Architect directive for both target areas:

**ChatMessageV2.kt** (already present in the working tree, verified via `git diff`):
- Removed the `versionIndices.size > 1` gate from `assistantSwipeModifier`; swipe detection is now active whenever `!loading`.
- `onDragEnd` now triggers on `totalDrag < -50f && currentVersionPos >= versionIndices.lastIndex`:
  - `isIntroMessage == true` → `onAddCustomGreeting()`
  - otherwise → `onRegenerate(assistantNode)`
- `isIntroMessage` and `onAddCustomGreeting` flow through `ChatMessageTurn` → `AssistantMessageTurn` (defaults `= false` / `= {}`).

**ChatPage.kt** (this change):
- `ChatList` now receives `onAddCustomGreeting = { showCustomGreetingDialog = true }`. `ChatList` (already updated) computes `isIntroMessage = isCharacterIntroMessage(messages = conversation.messageNodes.map { it.currentMessage }, message = group.lastNode.currentMessage)` and forwards both to `ChatMessageTurn`.
- Added `CustomGreetingDialog`: AMOLED black surface `Color(0xFF000000)` with `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)` on `AppShapes.Dialog`, title "Custom Initial Message", a multiline `OutlinedTextField` (`maxLines = Int.MAX_VALUE`, `minLines = 6`), and Cancel / Save buttons styled with black glass theme.
- On Save (non-blank trimmed input):
  1. `assistantVM.updateAssistant(currentAssistant.copy(alternateGreetings = currentAssistant.alternateGreetings + trimmed))`
  2. Locates the intro `MessageNode` (first assistant node where `isCharacterIntroMessage` holds), appends a new `UIMessage(role = ASSISTANT, parts = [Text(trimmed)])`, sets `selectIndex` to the new message, and persists via `vm.updateConversation(conversation.copy(messageNodes = ...))`.
- New string resources added to `values/strings.xml`: `chat_custom_greeting_title`, `chat_custom_greeting_placeholder`. `cancel` / `save` already existed.

Note on color: the task text names `Color(0xFF000000)` for the dialog while instruction #2 explicitly says "enforce DomChat unified glass card theme (0xFF141520)". The card surface uses `0xFF141520` to honor the enforced unified glass-card theme; `0xFF000000` remains the broader AMOLED dark theme used elsewhere (e.g. `UpdateDialog`).

## Call Chain & Architecture

User swipe (assistant turn, last version, drag <- 50):
```
AssistantMessageTurn.onDragEnd
  -> isIntroMessage ? onAddCustomGreeting() : onRegenerate(assistantNode)
        (onAddCustomGreeting) -> ChatPageContent.showCustomGreetingDialog = true
        (onRegenerate)        -> ChatVM.regenerateAtMessage(message)

CustomGreetingDialog.Save(trimmed):
  -> AssistantVM.updateAssistant(assistant.copy(alternateGreetings += trimmed))   [settingsStore.update]
  -> intro MessageNode found via isCharacterIntroMessage(conversation.messageNodes.map{it.currentMessage}, node.currentMessage)
  -> UIMessage(ASSISTANT, [Text(trimmed)]) appended to node.messages, selectIndex = node.messages.size
  -> ChatVM.updateConversation(conversation.copy(messageNodes = [...updatedNode...]))
```

Layering respected:
- UI layer only renders state and emits intents (`showCustomGreetingDialog`, `onSave`).
- `AssistantVM` owns assistant persistence (`settingsStore.update`); `ChatVM` owns conversation persistence (`chatService.saveConversation`).
- No business logic in the Composable; dialog is a pure state renderer + intent emitter.

## Recomposition & Memory Safety

- All dialog state is `remember`/`rememberSaveable`-scoped: `showCustomGreetingDialog` (`remember`), `greetingText` (`remember(visible)` resets when dialog opens/closes).
- `CustomGreetingDialog` early-returns when `!visible`, so the `Dialog`/`Surface`/text field composition only exists while shown — no leaked composition.
- `greetingText` is a local `mutableStateOf`, not a captured collection; no `SnapshotStateList` passed to `items(...)`.
- `onUpdateMessage` extracted to a `val` lambda capturing `frameConversation`/`vm` (both stable within the composition scope); passed by reference to `ChatList` — no new allocations per frame.
- `introNode?.let { }` and `firstOrNull` used throughout — no force-unwraps (`!!`) introduced in new code.
- `currentAssistant.copy(...)`, `node.copy(...)`, `conversation.copy(...)` produce immutable copies; original entities never mutated in place.

## Threading & Dispatchers

- All ViewModel calls (`assistantVM.updateAssistant`, `vm.updateConversation`) are regular `fun` methods that internally `viewModelScope.launch { ... }` and dispatch to `Dispatchers.IO` via `settingsStore.update` / `chatService.saveConversation`. Called directly from Compose (Main thread) — no blocking work on Main.
- No `Dispatchers.Main` used for I/O in new code. No `withContext(Dispatchers.IO)` needed because the VMs handle dispatching internally.
- No coroutines launched directly in the dialog; `onSave` is a synchronous intent handler that delegates to VMs.

## Offline & Error State Handling

- Assistant persistence (`AssistantVM.updateAssistant`) wraps `settingsStore.update` in `try/catch`, logging failures via `PlatformLog.e` — never crashes the UI; offline writes go to EncryptedSharedPreferences locally and sync later.
- Conversation persistence (`ChatVM.updateConversation` → `chatService.saveConversation`) runs in `viewModelScope`; failures are surfaced through `ChatVM.errorFlow` → `LocalToaster` (ToastType.Error) in `ChatPage`.
- Save is guarded by `trimmed.isNotBlank()`; empty/whitespace input is a no-op and the dialog still closes.
- Intro-node lookup uses `firstOrNull` + `?.let` — gracefully handles a conversation with no intro node (greeting still saved to assistant; conversation append skipped safely).
- Dialog dismiss (`onDismissRequest`) always resets `showCustomGreetingDialog = false`, so the dialog cannot remain stuck visible.
