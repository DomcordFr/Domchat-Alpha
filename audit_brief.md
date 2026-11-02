# Audit Brief — Character Hub Attached Lorebooks Tab & In-Sheet Management

## Intent Verification

**Task:** Add an attached lorebooks management tab to `CharacterInspectorSheet` in `CharacterHubPage.kt`.

**Implemented:**

1. **Lorebooks tab appears when lorebooks are attached:**
   - `hasAttachedLorebooks` computed from `settings.lorebooks.filter { it.id in assistant.enabledLorebookIds }`
   - Tab bar becomes horizontally scrollable (`horizontalScroll(rememberScrollState())`)
   - Tab titles list dynamically includes `"Lorebooks"` when `hasAttachedLorebooks` is true
   - `selectedTab` state is clamped to valid range via `LaunchedEffect(tabTitles)` when tab list changes

2. **Lorebooks tab content (tab index 3):**
   - Displays attached lorebook cards in a column
   - Each card shows lorebook name, description, entry count, enable/disable `HapticSwitch`, and detach `OutlinedButton`
   - Cards use black `Color(0xFF141520)` background with `LiquidGlassTokens.SpecularBorderSubtle` border (glassy theme, zero grey)
   - Toggle updates `enabledLorebookIds` via `onUpdateLorebookIds` callback
   - Detach opens a confirmation dialog

3. **Attach new lorebooks:**
   - "Attach Lorebook" button opens `Attach Lorebook Dialog` (glassy black, `SpecularBorderSubtle` border)
   - Dialog lists all available lorebooks (those not yet attached) with enable toggles
   - "Done" button closes the dialog

4. **Detach confirmation dialog:**
   - Shows confirmation before detaching a lorebook
   - Confirmed removal updates `enabledLorebookIds` via `onUpdateLorebookIds`

**Null safety:** Zero `!!` introduced. All nullable access uses `?.`, Elvis `?:`, `ifEmpty`, `isNotEmpty`, `in` checks.

**Theme:** `Color(0xFF000000)` AMOLED black surfaces, `Color(0xFF141520)` unified glass card theme, `LiquidGlassTokens.SpecularBorderSubtle` borders on cards and dialogs.

## Call Chain & Architecture

**UI → ViewModel → SettingsStore:**

1. `CharacterHubPage` renders `CharacterInspectorSheet` when `inspectingAssistant` is set
2. `CharacterInspectorSheet` receives `onUpdateLorebookIds: ((Set<Uuid>) -> Unit)?` callback
3. User toggles lorebook enable/disable → `onUpdateLorebookIds(newIds)` called
4. Caller (main `CharacterHubPage` composable) calls `vm.updateAssistant(updated)` with `char.copy(enabledLorebookIds = newIds)`
5. `AssistantVM.updateAssistant()` → `settingsStore.update { settings.copy(assistants = ...)}` → DataStore persistence

**Detach flow:**
1. User clicks detach on a lorebook card → `lorebookToDetach` set, `showDetachConfirm = true`
2. Detach confirmation dialog shows
3. Confirm → `onUpdateLorebookIds(assistant.enabledLorebookIds - lorebook.id)` → `vm.updateAssistant()` → persist

**Attach flow:**
1. User clicks "Attach Lorebook" → `showAttachLorebookDialog = true`
2. Attach dialog lists `settings.lorebooks.filter { it.id !in assistant.enabledLorebookIds }`
3. User toggles a lorebook on → `onUpdateLorebookIds(assistant.enabledLorebookIds + lorebook.id)`, dialog closes
4. "Done" button also closes dialog

**ViewModel boundary:** UI layer only renders state and emits intents via callbacks. `AssistantVM` (`vm.updateAssistant()`) is the single writer to `SettingsStore`.

## Recomposition & Memory Safety

- All new state (`showAttachLorebookDialog`, `lorebookToDetach`, `showDetachConfirm`, `attachedLorebookList`, `selectedTab`) uses `remember { mutableStateOf(...) }` or `remember(settings, assistant) { ... }`
- `attachedLorebookList` is keyed on `settings.lorebooks` and `assistant.enabledLorebookIds` — recomputes only when these change
- `LaunchedEffect(tabTitles)` clamps `selectedTab` when tab list changes — prevents out-of-range index
- Tab bar uses `horizontalScroll(rememberScrollState())` — stable scroll state
- No `SnapshotStateList` passed to `items()`; all collections are immutable reads
- Dialogs conditionally composed (`if (showAttachLorebookDialog)`, `lorebookToDetach?.let`) — no retained composition when closed
- No force-unwraps (`!!`) introduced

## Threading & Dispatchers

- All lorebook UI state mutations are synchronous main-thread Compose operations
- `vm.updateAssistant()` runs in `viewModelScope.launch` (main-safe coroutine scope); `SettingsStore.update` handles DataStore disk I/O internally
- No `Dispatchers.Main` blocking; all disk I/O strictly internal to `SettingsStore.update`
- No new background threads spawned

## Offline & Error State Handling

- Lorebook management is fully local (local DataStore/Room only) — no network required
- Attach/detach are in-memory set operations on `Set<Uuid>`, persisted via `SettingsStore.update`
- Empty state handled: "No lorebooks attached to this character." when list is empty
- `vm.updateAssistant()` has existing `try/catch` with `PlatformLog.e` — failures don't crash
- `onUpdateLorebookIds` is nullable with safe-call (`?.invoke`) — no crash if callback missing
- Existing `errorFlow` toasters unchanged; no new error modes introduced

## Files Changed

- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` — Added lorebook management state, scrollable tab bar with Lorebooks tab, Lorebooks tab content (toggle/detach/attach cards), `Attach Lorebook` dialog, `Detach Confirmation` dialog, `onUpdateLorebookIds` parameter on `CharacterInspectorSheet`, wired callback in caller
- `audit_brief.md` — this document
