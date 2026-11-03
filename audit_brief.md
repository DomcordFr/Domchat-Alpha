# Audit Brief — Task 116: Character Groups Spacing, Upload Button, and Group Filter Isolation

## Intent Verification

All 6 requirements from task-116_directive.json implemented in `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` (plus one supporting field in `Assistant.kt`):

1. **Tighten GroupTabsRow spacing** — `Arrangement.spacedBy(8.dp)` → `Arrangement.spacedBy(4.dp)` in `GroupTabsRow` Row. Applied once; verified at `GroupTabsRow` definition.

2. **Left-align upload button** — `.align(Alignment.CenterVertically)` → `.align(Alignment.CenterStart)` on both grid-view (line ~658) and list-view (line ~993) Upload Character Surface modifiers. Two occurrences replaced via `replaceAll`; both verified.

3. **Group filtering isolation** — Added `val groupId: String? = null` field to `Assistant` data class. Added `selectedGroupId` to `LaunchedEffect` dependency list. New `groupFiltered` step filters `tagFiltered` by `char.groupId == selectedGroupId` (null = show all). Sort sources updated from `tagFiltered` → `groupFiltered`. Persisted group moves via `vm.updateAssistant(char.copy(groupId = targetGroupId))`.

4. **Eliminate duplicate delete popups** — Removed inline `AlertDialog` delete confirmation block (was between Inspector Sheet and Export Dialog). Kept single `UniversalDestructiveConfirmDialog` (animated, vertical-centered Dialog, no cut-off). Dialog message now states "Associated chat sessions will be preserved in your history."

5. **'All' full-width distortion** — Added `.wrapContentWidth()` to all three tab Surfaces in `GroupTabsRow` ("All", custom groups, "+ New") so tabs size to content instead of forcing full width when no extra groups exist. Changed `allCount` from `filteredAssistants.size` (dynamic/shrinking per group) to `settings.assistants.size` (stable total) in both grid and list GroupTabsRow call sites.

6. **New group creation popup bottom cut-off** — `NewGroupDialog` outer Box `contentAlignment = Alignment.Center` → `Alignment.TopCenter`, preventing content from being vertically centered and cut off at the bottom edge.

**Constraints:** ZERO `!!` force-unwraps (grep-confirmed across all touched files). Unified glass card theme `0xFF141520` unchanged — no theme constants altered. Existing 4-space indent and import grouping preserved.

## Call Chain & Architecture

```
User taps GroupTabsRow tab
  → onGroupSelected(groupId) → selectedGroupId = groupId
  → LaunchedEffect(settings.assistants, ..., selectedGroupId) recomputes
      withContext(Dispatchers.Default) {
        base = settings.assistants (search-filtered)
        tagFiltered = base (tag-filtered)
        groupFiltered = tagFiltered.filter { char.groupId == selectedGroupId }
        sorted = groupFiltered.sortedBy(...)
        filteredAssistants = distinctSorted  (Main dispatcher)
      }
  → LazyVerticalGrid/LazyColumn items(filteredAssistants) render only group members
```

```
User long-presses character card
  → characterToMove = char, showMoveToGroupDialog = true
  → CharacterActionPopover onMoveToGroup(groupId)
      → updated = char.copy(groupId = targetGroupId)
      → scope.launch { vm.updateAssistant(updated) }  (viewModelScope → settingsStore.update → DataStore)
      → selectedGroupId = targetGroupId (re-filter hub)
```

```
User taps delete in popover / inspector
  → characterToDelete = char
  → UniversalDestructiveConfirmDialog (single instance)
  → onConfirm → vm.removeAssistant(char) → optimistic settings removal
      → deferred tombstone on Dispatchers.IO after undo window
      → toaster shows "Deleted X" with Undo action
```

Architectural boundaries per AGENTS.md: UI layer only renders state + emits intents; ViewModel (`AssistantVM`) exposes `settings` StateFlow and manages coroutines in `viewModelScope`; Repository/DataStore is single source of truth. Group persistence routes through `settingsStore.update { current.copy(assistants = ...) }` — same path as all assistant mutations.

## Recomposition & Memory Safety

- `GroupTabsRow` spacing change is stateless Modifier geometry; no state captured, zero recomposition side effects.
- `.align(Alignment.CenterStart)` on upload button is a stateless Modifier; no allocation per frame beyond the modifier chain itself.
- `groupFiltered` is computed inside `LaunchedEffect` on `Dispatchers.Default`, then assigned to `filteredAssistants` (a `mutableStateOf`) on `Dispatchers.Main`. Snapshot rule followed: value computed off-main, then `withContext(Dispatchers.Main) { filteredAssistants = distinctSorted }`.
- `filteredAssistants` read via `remember { mutableStateOf(...) }`; Lazy grids/columns use `items(..., key = { it.id.toString() })` for stable item identity — no `SnapshotStateList` passed directly to `items()`.
- `selectedGroupId` is `remember { mutableStateOf<String?>(null) }` — remembered, lifecycle-aware UI state.
- Group tabs use `.wrapContentWidth()` — no full-width distortion; tabs size to content, eliminating wasted space when no extra groups exist.
- `NewGroupDialog` now `Alignment.TopCenter` — content anchored to top, no bottom cut-off; dialog Surface has `.widthIn(max = 420.dp)` capped width.
- No `!!` force-unwraps anywhere; all nullable access uses safe calls, Elvis, or defensive guards.
- No naked objects in Compose — all state `remember`ed or collected via `collectAsStateWithLifecycle`.

## Threading & Dispatchers

- All Compose UI/layout edits execute on `Dispatchers.Main` (correct for UI layer).
- Group filter computation (`tagFilter` → `groupFilter` → `sorted`) runs on `Dispatchers.Default` inside `LaunchedEffect`, then results posted to Main — no Main-thread blocking on hundreds of characters.
- `vm.updateAssistant(updated)` launches on `viewModelScope` → `settingsStore.update` (DataStore, IO-bound) — not on Main.
- `vm.removeAssistant(char)` optimistic removal on `viewModelScope`; deferred Room/database tombstone on `appScope.launch` → `Dispatchers.IO` (per `loadDeleteImpact`/`removeAssistant` in AssistantVM).
- Import/export parsers use `withContext(Dispatchers.IO)` — unchanged, no Main-thread disk access.
- No `Dispatchers.Main` violations introduced; no background work on Main dispatcher.

## Offline & Error State Handling

- **Offline:** Groups, assistants, and settings live entirely in local DataStore/Room — no network dependency for group filtering, tab rendering, or delete. File import uses local document picker (`OpenDocument`) and local `AssistantExportImport.parseImport` — fully offline-capable.
- **Loading:** `loadError` state is captured; `CharacterHubErrorCard` renders when `loadError != null` (isolates per-load failure without taking down the gallery).
- **Success:** Toaster confirms create/move/delete via `toaster?.show(...)`; undo available for delete via `vm.undoRemoveAssistant`.
- **Exception:** `LaunchedEffect` catch block sets `loadError = e` and `filteredAssistants = emptyList()` on Main. `vm.updateAssistant`/`vm.removeAssistant` catch exceptions internally and log via `PlatformLog.e` — no crash propagation.
- **Empty state:** `allCount = settings.assistants.size` is stable (not filtered), so the "All" tab label doesn't shrink/distort when a group is selected and has no members. Group tabs width-collapse via `.wrapContentWidth()` when only "All" exists.
