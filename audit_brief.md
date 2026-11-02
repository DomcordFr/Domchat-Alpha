# Audit Brief — Bot Display Name vs Chat Name Decoupling

## Intent Verification

**Assistant.kt**
- Added `@SerialName("display_name") val displayName: String? = null` and `@SerialName("chat_name") val chatName: String? = null` to the `Assistant` data class (nullable, default `null`).
- Added computed `val effectiveDisplayName: String` = `displayName?.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() } ?: "Character"`.
- Added computed `val effectiveChatName: String` = `chatName?.takeIf { it.isNotBlank() } ?: name.takeIf { it.isNotBlank() } ?: displayName?.takeIf { it.isNotBlank() } ?: "Character"`.
- All access null-safe (Elvis + `takeIf`), zero `!!`.

**CharacterHubPage.kt**
- Card display name (`CharacterPortraitCard`), hub list row name (`CharacterListRow`), inspector sheet title (`CharacterInspectorSheet`), `FullCharacterCardImage` name/content-description (portrait card, list row, inspector hero), and error card text now use `assistant.effectiveDisplayName`.

**ChatPage.kt**
- Chat toolbar title and top-bar character pill (`UIAvatar`) now use `currentAssistant.effectiveChatName`.
- Assistant message bubbles: `ChatMessageV2.kt` assistant-turn name label (`avatarName`) now uses `assistant.effectiveChatName`. (Chat bubbles are rendered by `ChatMessageV2`, called from `ChatPage` via `ChatList`; this is the physical location of the bubble name.)

**SettingJanitorPage.kt**
- `extractToAssistant` now sets `name = rawName`, `displayName = rawName`, `chatName = extractCleanChatName(rawName)`.
- New `extractCleanChatName(name)` splits on the first delimiter `|`, `-`, `—`, `[`, `(`, returns trimmed substring; null-safe, no `!!`.

## Call Chain & Architecture

**Model layer.** `Assistant` (`:app data/model/Assistant.kt`) is the Koin-serialized settings entity. `effectiveDisplayName`/`effectiveChatName` are pure computed properties (zero state, zero allocations beyond a `String`), consumed by UI as derived values.

**UI consumption (render only, no business logic).**
- `CharacterHubPage` → `CharacterPortraitCard` / `CharacterListRow` / `CharacterInspectorSheet` read `assistant.effectiveDisplayName` at composition time.
- `ChatPage` → `ChatToolbar` reads `currentAssistant.effectiveChatName` for toolbar title and pill; `ChatMessageV2` assistant-turn name label reads `assistant.effectiveChatName`.

**Import path.** `SettingJanitorPage.extractToAssistant` builds `Assistant` from `JanitorExtractResponse`; `chatName` populated from delimiter-split of raw name, `displayName` = raw name. Stored via `AssistantVM.addAssistant` → `PreferencesStore.update` (canonical settings API: normalization, secret migration, in-memory state, persist).

**Serialization.** New fields are nullable with defaults + `@SerialName`. Existing persisted `Assistant` JSON deserializes safely (defaults to null → computed fallbacks active). No Room/DataStore migration required.

## Recomposition & Memory Safety

- Computed properties are `val get()` — no mutable state, no self-triggering recomposition. They recompute only when the parent `Assistant` instance reference changes, which this feature does not alter.
- UI reads occur during composition; `assistant`/`currentAssistant` are collected via `collectAsStateWithLifecycle` (lifecycle-aware) in `CharacterHubPage` and `ChatPage`. Passing a derived immutable `String` into `Text`/`UIAvatar` is stable — no `SnapshotStateList` passed to `items`, no derived-state violations.
- `FullCharacterCardImage` receives `name` as a plain `String` parameter (content description + dummy avatar letter) — stable, no retained references, no memory leak.
- No new coroutines, jobs, or scoped state introduced. Existing `remember`/`collectAsStateWithLifecycle` usage unchanged.

## Threading & Dispatchers

- All name resolution is synchronous, main-thread-safe String computation. No blocking calls on `Dispatchers.Main`.
- `extractCleanChatName` is a pure function called while constructing the `Assistant` inside `extractToAssistant`, invoked from the import button handler inside `scope.launch { ... withContext(Dispatchers.IO) { ... } }`. The HTTP fetch (`/api/extract`) already runs on `Dispatchers.IO`; `Assistant` construction happens after the IO block returns, on `Dispatchers.Main` — safe because it is pure in-memory object construction, no I/O.
- No new dispatchers introduced. `Dispatchers.IO` used only for HTTP/JSON import, as before.

## Offline & Error State Handling

- `effectiveDisplayName`/`effectiveChatName` are total functions: every branch returns a non-null `String` (final fallback `"Character"`). No exception path; empty/blank inputs degrade gracefully to fallback.
- `extractCleanChatName`: no delimiter → full trimmed name; name starts with delimiter → empty string → `effectiveChatName` falls back through `name`/`displayName` to `"Character"`. Never throws.
- Import flow: `extractToAssistant` is wrapped in `try/catch` (import button handler); `fullExtract` fetch failures surface `ImportStatus.ERROR` + toast; parse failures caught. Name decoupling does not alter error handling.
- Offline: computed names require no network; fully local. Import requires network (pre-existing behavior) — failures handled by existing `JanitorLoadState.Error`/toast paths.
- Null safety: zero `!!` across all edited files; all nullable access uses `?.`, `?:`, `takeIf`, `orEmpty()`.

## Files Changed

- `app/src/main/java/me/lucid/domchat/data/model/Assistant.kt` — `displayName`, `chatName`, `effectiveDisplayName`, `effectiveChatName`.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` — card/list/inspector/error names → `effectiveDisplayName`.
- `app/src/main/java/me/lucid/domchat/ui/pages/chat/ChatPage.kt` — toolbar title + pill → `effectiveChatName`.
- `app/src/main/java/me/lucid/domchat/ui/components/chat/ChatMessageV2.kt` — assistant bubble name label → `effectiveChatName` (required for the "assistant message bubbles" requirement; bubbles render outside `ChatPage.kt` itself).
- `app/src/main/java/me/lucid/domchat/ui/pages/setting/SettingJanitorPage.kt` — `extractToAssistant` + `extractCleanChatName` helper.
