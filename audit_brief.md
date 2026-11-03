# Audit Brief — Task 117: Character Card Long-Press Popover Redesign & Group Reordering

## Intent Verification

All 4 requirements from task-117_directive.json implemented in `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt`:

1. **Popover positioning — vertically centered, no bottom cut-off** — Outer `Box` in `CharacterActionPopover` now has `contentAlignment = Alignment.Center` (previously defaulted to `TopStart`). The Surface content is anchored to the vertical center of the Dialog, preventing bottom-edge cut-off on small screens. Padding (`20.dp`) preserved as margin around the centered card.

2. **Groups in horizontally scrollable LazyRow** — Replaced the `Row` + `weight(1f)` custom group pills with a `LazyRow` (`horizontalArrangement = Arrangement.spacedBy(8.dp)`) using `items(groupList, key = { it.key })`. Each group pill is `wrapContentWidth` sized with a `widthIn(min = 120.dp)` Row for consistent touch targets. Horizontal scrolling only activates when groups exceed available width.

3. **Group delete + reorder actions** — Added `onDeleteGroup: (String) -> Unit` and `onReorderGroup: (String, Int) -> Unit` callbacks to `CharacterActionPopover`. Each group pill now shows:
   - Close (X) icon button → `onDeleteGroup(groupId)` (red tint)
   - KeyboardArrowUp icon button → `onReorderGroup(groupId, -1)` (disabled at index 0)
   - KeyboardArrowDown icon button → `onReorderGroup(groupId, 1)` (disabled at last index)
   
   Call site handles: deleting group removes it from `groupNames` and reassigns all characters with `groupId == deletedId` to `null` ("All"); reordering converts the `Map` to a mutable list, moves the entry by delta, and writes back as `toMap()`.

4. **Cleaned up ugly random spacing** — Removed `Spacer(modifier = Modifier.height(12.dp))` between sections; replaced with consistent `Arrangement.spacedBy(16.dp)` on the outer Column and `spacedBy(8.dp)` on inner sections. Removed `Modifier.weight(1f)` from group pills (caused uneven stretching). All pill heights unified at `vertical = 10.dp`.

**Constraints:** ZERO `!!` force-unwraps (grep-confirmed). Unified glass card theme `0xFF141520` untouched. Existing 4-space indent and import grouping preserved. `TextOverflow` already imported.

## Call Chain & Architecture

```
User long-presses character card
  → characterToMove = char, showMoveToGroupDialog = true
  → CharacterActionPopover opens (Dialog, vertically centered)
```

```
User taps group pill in LazyRow
  → onMoveToGroup(groupId)
    → updated = char.copy(groupId = targetGroupId)
    → scope.launch { vm.updateAssistant(updated) }   (viewModelScope → settingsStore.update → DataStore)
    → selectedGroupId = targetGroupId   (hub re-filters)
    → showMoveToGroupDialog = false
```

```
User taps Close (X) on group pill
  → onDeleteGroup(groupId)
    → groupNames = groupNames - groupId
    → scope.launch { settings.assistants.filter { it.groupId == groupId }.forEach { vm.updateAssistant(it.copy(groupId = null)) } }
    → if selectedGroupId == groupId: selectedGroupId = null
```

```
User taps Up/Down arrow on group pill
  → onReorderGroup(groupId, delta)
    → entries = groupNames.toMutableList()
    → move entry by delta, clamp to [0, lastIndex]
    → groupNames = entries.toMap()
```

```
User taps Delete (character)
  → onDelete() → characterToDelete = char → UniversalDestructiveConfirmDialog
    → onConfirm → vm.removeAssistant(char) → optimistic DataStore removal + deferred IO tombstone
```

Architecture: UI layer renders state and emits intents only. ViewModel (`AssistantVM`) owns `settings` StateFlow and coroutines in `viewModelScope`. DataStore is single source of truth. No business logic in composables.

## Recomposition & Memory Safety

- `contentAlignment = Alignment.Center` on the outer Box is a static layout property; zero state, zero recomposition impact.
- `LazyRow` with `items(groupList, key = { it.key })` provides stable item identity — Compose recomposes only changed group pills, not the whole row.
- `groupList = groupNames.entries.toList()` is derived inside the composable on each recomposition, but it's a small list (groups count is low) and not held in mutable state — acceptable.
- `currentIndex = groupList.indexOfFirst { it.key == groupId }` computed per item during composition — O(n) per item but n is small (groups count); no allocation beyond the index Int.
- `Modifier.wrapContentWidth()` on pills + `widthIn(min = 120.dp)` on inner Row ensures consistent touch targets without full-width stretching.
- `groupNames` is `remember { mutableStateOf<Map<String, String>> }` — remembered, lifecycle-aware UI state.
- All callbacks (`onDeleteGroup`, `onReorderGroup`, `onMoveToGroup`) launch coroutines via `scope.launch` (`rememberCoroutineScope()`) — not inline composition mutations.
- No `SnapshotStateList` passed to `items()`. No `!!` force-unwraps. No naked objects — all state `remember`ed or collected via `collectAsStateWithLifecycle`.

## Threading & Dispatchers

- Popover UI/layout: `Dispatchers.Main` (correct — Compose UI layer).
- `vm.updateAssistant(updated)` → `viewModelScope.launch` → `settingsStore.update` (DataStore, IO-bound) — not on Main.
- Group delete reassigns characters via `scope.launch { settings.assistants.filter {...}.forEach { vm.updateAssistant(...) } }` — viewModelScope, not Main-blocking.
- Deferred Room/database tombstone (from `vm.removeAssistant`) runs on `appScope.launch` → `Dispatchers.IO` — unchanged from existing behavior.
- No `Dispatchers.Main` violations introduced. No background work on Main dispatcher.

## Offline & Error State Handling

- **Offline:** Groups, `groupNames`, assistants, and settings all live in local DataStore — fully offline-capable. No network dependency for popover, group delete, or group reorder.
- **Loading:** No loading states needed — DataStore writes are asynchronous but optimistic UI updates (toaster + immediate state change) mask latency.
- **Success:** Toaster confirms "Moved X to group", "Group deleted".
- **Exception:** `vm.updateAssistant` catches exceptions internally and logs via `PlatformLog.e` — no crash propagation to UI. `groupNames = groupNames - groupId` is a local state mutation that succeeds regardless of DataStore outcome (eventual consistency via VM).
- **Empty state:** If `groupNames` is empty, the LazyRow block is skipped (`if (groupList.isNotEmpty())`), showing only "All" option and Delete character option.
- **Selection safety:** Up/down reorder arrows are `enabled = false` at boundaries (index 0 / lastIndex) — no out-of-bounds errors. `indexOfFirst` returns -1 if not found, guarded by `if (currentIndex >= 0)`.
