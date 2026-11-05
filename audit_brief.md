# Audit Brief — Task 175: Phase 5A Character Hub Group Tabs & Sheet Polish

## Intent Verification

**CharacterHubPage.kt**
- Added 4 group context-menu state vars in `CharacterHubPage` composable:
  - `selectedGroupForMenu` (`String?`, nullable, Elvis-safe)
  - `showGroupMenuDialog` (`Boolean`)
  - `showRenameGroupDialog` (`Boolean`)
  - `renameGroupText` (`String`)
  All via `remember { mutableStateOf(...) }`.
- `GroupTabsRow` gained an `onGroupLongClick: (String) -> Unit = {}` parameter. Custom group tab `Surface` now uses `Modifier.combinedClickable(onClick = ..., onLongClick = { onGroupLongClick(groupId) })`. The Surface's standalone `onClick` was removed to avoid double-fire (combinedClickable already handles click). The "All" and "+ New" tabs are unchanged (no long-click menu).
- Both `GroupTabsRow` call sites (grid + list views) wire `onGroupLongClick` to perform `HapticPattern.Buildup`, set `selectedGroupForMenu = groupId`, `showGroupMenuDialog = true`.
- **Group management `AlertDialog`** (gated by `showGroupMenuDialog`): `Modifier.widthIn(max = 420.dp)`, title `Manage Group: ${selectedGroupForMenu ?: ""}`, "Rename Group" pill (opens rename dialog) and "Delete Group" pill. Delete does `selectedGroupForMenu?.let { grp -> groupNames = groupNames - grp; charGroupAssignments = charGroupAssignments.filterValues { it != grp }; persistGroups(); toaster?.show(...) }` then dismisses. Null-safe via `?.let`.
- **Rename `AlertDialog`** (gated by `showRenameGroupDialog`): `Modifier.widthIn(max = 420.dp)`, title "Rename Group", `OutlinedTextField` bound to `renameGroupText`, Confirm "Save" (guarded by `isNotBlank()`, renames via `groupNames = groupNames + (grp to renameGroupText.trim())`, persists, resets state) and "Cancel" dismiss.
- **CharacterActionPopover**: `widthIn(max = 460.dp)` → `widthIn(max = 420.dp)` for consistent dialog width.

**MinimalChatInput.kt**
- AI Streaming Animation sheet header Close `IconButton`: added `colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)`.
- Author's Note sheet header Close `IconButton`: added `colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)`.
- Samplers sheet header already had transparent colors (no change needed).
- `IconButtonDefaults` import already present (added in task 174).

Architectural note: group names live in a separate SharedPreferences file (`character_hub_groups`) managed locally in `CharacterHubPage` via `persistGroups()`, NOT in `Settings`/`AssistantVM`. There is no `vm.updateGroups` on `AssistantVM` (VM owns Settings only). Therefore delete/rename persist inline via `groupNames` + `charGroupAssignments` + `persistGroups()`, rather than calling a non-existent `vm.updateGroups(...)`. This matches existing group CRUD patterns (create at line ~1479, move at line ~1494, delete at line ~1501).

## Call Chain & Architecture

UI layer only:
1. User long-presses a custom group tab → `combinedClickable.onLongClick` → haptics Buildup → `selectedGroupForMenu = groupId`, `showGroupMenuDialog = true`.
2. Recomposition adds the "Manage Group" `AlertDialog` overlay (`widthIn(max = 420.dp)`).
3. "Rename Group" → `renameGroupText = selectedGroupForMenu ?: ""`, close first dialog, open rename dialog.
4. Rename dialog "Save" (enabled only when name non-blank) → `groupNames = groupNames + (grp to trimmedName)`, `persistGroups()` (writes `character_hub_groups` SharedPreferences off Main via `scope.launch { withContext(Dispatchers.IO) { ... } }`), toast, reset state, dismiss.
5. "Delete Group" → `selectedGroupForMenu?.let { grp -> groupNames = groupNames - grp; charGroupAssignments = charGroupAssignments.filterValues { it != grp }; persistGroups(); toast }`, dismiss. Also unassigns any characters in the deleted group.
6. Cancel / dismiss → corresponding `show*Dialog = false`.

Architectural boundary: composables render state and emit intents via `onGroupSelected`/`onGroupLongClick` callbacks and direct state mutation + `persistGroups()`. No business logic in UI; persistence in `persistGroups()` (IO dispatcher). ViewModel (`AssistantVM`) is not involved in group ownership (correct — groups aren't in Settings).

## Recomposition & Memory Safety

- All four state vars are `remember { mutableStateOf(...) }` — no naked objects.
- Dialogs are ephemeral: `showGroupMenuDialog = false` / `showRenameGroupDialog = false` on every action and `onDismissRequest`, so they leave composition after one frame.
- `selectedGroupForMenu` is nullable; all access uses `?: ""` (title) or `?.let { ... }` (delete/rename) — no `!!`.
- `groupNames` and `charGroupAssignments` are reassigned immutably (`groupNames - grp`, `groupNames + (grp to name)`, `filterValues`) — no mutation of remembered maps during composition.
- `renameGroupText` reset to `""` after save/cancel.
- `combinedClickable` is the existing helper (used elsewhere in the file); `onClick` removed from Surface to avoid double-invocation.
- No `!!` force-unwraps introduced (grep-verified: zero matches in CharacterHubPage.kt).
- Diff: 109 insertions / 5 deletions across 2 files — within scope.

## Threading & Dispatchers

- Composable bodies run on `Dispatchers.Main` (UI thread) only; no coroutines launched in the edited composable code.
- `persistGroups()` launches in `scope.launch { withContext(Dispatchers.IO) { saveGroupNames(...) } }` — disk I/O correctly off the Main thread, matching the existing pattern (lines 275-284).
- `toaster?.show(...)` is UI-thread safe (existing usage).
- No network access introduced.
- Invariant "ZERO background threading on Dispatchers.Main" satisfied.

## Offline & Error State Handling

- Fully offline-capable: all operations are in-memory map copies + local SharedPreferences persistence. No network calls.
- Error handling: `saveGroupNames` already swallows exceptions ("best-effort persistence" try/catch), so persist failures don't crash the UI. Toaster shows success messages on successful save.
- Empty/null group: title falls back to `""` via Elvis (`selectedGroupForMenu ?: ""`). Delete/rename guarded by `selectedGroupForMenu?.let` (no-op if null). Rename confirm guarded by `renameGroupText.isNotBlank()`.
- Deleting a group also reassigns member characters to ungrouped (`filterValues { it != grp }`), preventing dangling references.
