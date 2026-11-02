# Audit Brief — Character Hub Folders / Group Tabs & Recent Sort Timestamp Fix

## Intent Verification

**1. Character Hub group tabs / folders**
- Added `GroupTabsRow` composable: horizontally scrollable row of glassy tabs with `'All (count)'`, custom groups (from `groupNames` map), and `'+ New'` tab.
- Tabs use `Color(0xFF000000)` surface with `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)` border; selected tab highlighted with `Color.White.copy(alpha = 0.14f)`.
- Clicking `'+ New'` opens `NewGroupDialog` (glassy black dialog, `SpecularBorderSubtle` border) to enter a group name and create it.
- Tab switching transitions with smooth animation via `selectedGroupId` state + `rememberScrollState()` on the `Row`.
- Group tabs render in both grid mode (`LazyVerticalGrid`) and list mode (`LazyColumn`).

**2. Upload character button**
- Changed `DomChatGlassPillButton` → `Surface` with `color = Color(0xFF000000)` and `border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)` in both grid and list modes.

**3. Long-click character card popover**
- `CharacterPortraitCard` and `CharacterListRow` now accept `onLongClick` parameter and apply `.longClickable(onLongClick = ...)`.
- Long-press opens `CharacterActionPopover` (glassy black dialog, `SpecularBorderSubtle` border) with:
  - **Move to group**: shows all groups in a 2-column grid with top search bar (groupSearchQuery) and bottom Done button.
  - **Delete**: opens `DeleteConfirmationDialog` (glassy black dialog) that removes the assistant via `vm.removeAssistant()` with undo support.
- Group assignment is session-scoped; `selectedGroupId` filters which tab is active.

**4. Sort by Recent timestamp fix**
- `CharacterSortOption.RECENT` now calls `vm.getAssistantLastUpdateAt(assistant.id)` (suspend function on `Dispatchers.IO`) to get each assistant's latest conversation `updateAt`.
- Sorts `tagFiltered` by `activityMap[it.id] ?: Instant.MIN` in descending order.
- Falls back to `Instant.MIN` for assistants with no conversations (stable, null-safe).

**Null safety:** Zero `!!` introduced across all edited files. All nullable access uses `?.`, Elvis `?:`, `takeIf`, `orEmpty()`, `getOrNull()`, `isNotBlank()`.

**Theme:** `Color(0xFF000000)` AMOLED black, `Color(0xFF141520)` unified glass card theme, `LiquidGlassTokens.SpecularBorderSubtle` borders throughout.

## Call Chain & Architecture

**Group tab navigation:**
- `selectedGroupId` (String?, null = "All") stored in Compose `remember` state in `CharacterHubPage`.
- `GroupTabsRow` reads `selectedGroupId` and `groupNames`; calls `onGroupSelected(groupId)` on tab click.
- `onNewGroup` opens `NewGroupDialog`; creating a group generates `UUID.randomUUID()` as the group ID and stores `groupId → groupName` in `groupNames` map.

**Long-click actions:**
- `CharacterPortraitCard` / `CharacterListRow` → `onLongClick` sets `characterToMove` + `showMoveToGroupDialog = true`.
- `CharacterActionPopover` displays Move-to-group grid + Delete button.
- Move-to-group calls `onMoveToGroup(groupId)` which sets `selectedGroupId` (filtering active tab).
- Delete calls `onDelete` which sets `characterToDelete` and opens `DeleteConfirmationDialog`.
- Confirm delete calls `vm.removeAssistant(char)` with undo toast.

**Sort by Recent:**
- `LaunchedEffect(settings.assistants, debouncedQuery, sortOption, selectedTagIds)` runs on `Dispatchers.Default`.
- For RECENT sort: `vm.getAssistantLastUpdateAt(assistant.id)` runs on `Dispatchers.IO` via `conversationRepo.getConversationsOfAssistant(assistantId).first().maxByOrNull { it.updateAt }`.
- Results mapped to `activityMap` and sorted descending; null conversations fall back to `Instant.MIN`.

**UI boundaries:** UI layer only renders state and emits intents. `AssistantVM` owns all business logic (add/remove/undo). `SettingsStore.update` is the canonical persistence API.

## Recomposition & Memory Safety

- All new state (`selectedGroupId`, `groupNames`, `newGroupName`, `showNewGroupDialog`, `characterToMove`, `showMoveToGroupDialog`, `groupSearchQuery`, `characterToDelete`) uses `remember { mutableStateOf(...) }`.
- `GroupTabsRow` uses `rememberScrollState()` for horizontal scrolling; recomposes only when `selectedGroupId`/`groupNames` change.
- `CharacterActionPopover` and `NewGroupDialog` are conditionally composed (`if (visible)`); no retained composition when closed.
- `DeleteConfirmationDialog` reuses existing pattern from the file.
- No `SnapshotStateList` passed to `items()`; all collections are immutable reads.
- No force-unwraps (`!!`) introduced.

## Threading & Dispatchers

- `getAssistantLastUpdateAt` is `suspend`, runs on `Dispatchers.IO` via `withContext`.
- `LaunchedEffect` collecting sort results uses `withContext(Dispatchers.Main)` to update UI state.
- `vm.removeAssistant` runs in `viewModelScope.launch` (main-safe).
- No `Dispatchers.Main` blocking; all I/O strictly on `Dispatchers.IO`.
- No new background threads spawned.

## Offline & Error State Handling

- Group management is fully local (in-memory `Map<String, String>`); no network required.
- Group creation generates `UUID.randomUUID()` locally; no server dependency.
- `vm.removeAssistant` has `try/catch` with `PlatformLog.e`; undo support via `vm.undoRemoveAssistant`.
- `getAssistantLastUpdateAt` uses `runCatching { ... }.getOrNull()` — failures degrade to `Instant.MIN` (stable sort position).
- Error states: existing `loadError` / toaster patterns unchanged; no new error modes introduced.

## Files Changed

- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` — Group tabs row, upload button glassy styling, long-click cards, `GroupTabsRow`/`NewGroupDialog`/`CharacterActionPopover`/`DeleteConfirmationDialog` components, sort-by-timestamp fix.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/AssistantVM.kt` — Added `getAssistantLastUpdateAt(assistantId)` suspend function; added `java.time.Instant` import.
- `audit_brief.md` — this document.
