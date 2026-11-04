# Audit Brief — Task 173: Wire Greetings Route + Search & Direct Add in Lorebooks

## Intent Verification

**AssistantDetailPage.kt**
- `composable(AssistantDetailRoutes.GREETINGS) { AssistantGreetingsSubPage(assistant = assistant, onUpdate = { onUpdate(it) }) }` — present and unchanged (lines 437-444).
- `composable(AssistantDetailRoutes.CREATOR_NOTES) { AssistantCreatorNotesSubPage(assistant = assistant, onUpdate = { onUpdate(it) }) }` — added (lines 446-454). Import `me.lucid.domchat.ui.components.ui.AssistantCreatorNotesSubPage` already present at line 79.
- Orphaned `composable(AssistantDetailRoutes.EXAMPLE_DIALOGUES) { ... }` block — removed (previously lines 446-454).
- `AssistantDetailRoutes.CREATOR_NOTES = "creator_notes"` constant already exists in the routes object (line 108).

**AssistantLorebooksSubPage.kt**
- Added state: `searchQuery`, `showAddDialog`, `newLorebookName`, `newLorebookDesc` — all `remember { mutableStateOf(...) }`.
- Added `filteredLorebooks = remember(lorebooks, searchQuery) { ... }` filtering by `name` or `description` containing `searchQuery` (ignoreCase).
- List now renders `filteredLorebooks` instead of `lorebooks`.
- Added search row (`OutlinedTextField` with leading search icon, trailing clear icon, placeholder "Search lorebooks...") + `DomChatGlassPillButton("+ Create Lorebook")` opening `showAddDialog`, above the lorebooks list inside the non-empty branch.
- Added centered `AlertDialog` (`widthIn(max = 420.dp)`, title "Create Lorebook", Name + Description optional fields) gated by `showAddDialog`. Confirm button "Create & Attach" (enabled only when `newLorebookName.isNotBlank()`) builds a `Lorebook`, calls `vm.updateSettings(settings.copy(lorebooks = settings.lorebooks + newBook))` then `onUpdate(assistant.copy(enabledLorebookIds = assistant.enabledLorebookIds + newBook.id))`, resets fields and dismisses. Dismiss button "Cancel".

**AssistantDetailVM.kt**
- Added `fun updateSettings(settings: Settings)` wrapping `settingsStore.update(settings = settings)` so `vm.updateSettings(...)` referenced in the dialog resolves. Follows the existing `settingsStore.update(settings = ...)` pattern used by `update(assistant)`, `updateTags`, etc.

## Call Chain & Architecture

UI layer (AssistantLorebooksSubPage):
1. User taps "+ Create Lorebook" → `showAddDialog = true`.
2. Recomposition adds the `AlertDialog` overlay (sibling of the `Column`, not inside the scrollable body).
3. User edits Name/Description → `newLorebookName`/`newLorebookDesc` state. Confirm button enables only when name non-blank.
4. User taps "Create & Attach" → builds `Lorebook(name, description)` (id defaults to `Uuid.random()`), calls `vm.updateSettings(settings.copy(lorebooks = settings.lorebooks + newBook))` (persists to settings via `SettingsStore`), then `onUpdate(assistant.copy(enabledLorebookIds = assistant.enabledLorebookIds + newBook.id))` (attaches to the assistant). Resets fields, `showAddDialog = false`.
5. `onUpdate` → `AssistantDetailVM.update(assistant)` → `settingsStore.update(...)` persists the assistant.
6. Cancel / dismiss → `showAddDialog = false`.

Architectural boundary: UI renders state and emits intents via `onUpdate` + `vm.updateSettings`; persistence lives in the ViewModel (`viewModelScope`) and `SettingsStore`. No business logic in the composable.

## Recomposition & Memory Safety

- All four state vars are `remember { mutableStateOf(...) }` — no naked objects.
- `filteredLorebooks` uses `remember(lorebooks, searchQuery)`; derives an immutable filtered list each recomposition; never mutates `lorebooks`/`settings.lorebooks`.
- `.toMutableList()` not used here (no list mutation in composition); `settings.lorebooks + newBook` creates a new list.
- `newLorebookName = ""`, `newLorebookDesc = ""`, `showAddDialog = false` reset after create, so dialog leaves composition cleanly.
- Confirm button `enabled = newLorebookName.isNotBlank()` guards empty-name submits; inner `if (newLorebookName.isNotBlank())` is a redundant defensive guard.
- No `!!` force-unwraps (grep-verified: zero matches in all three edited files).
- Diff: 120 insertions / 4 deletions across 3 files — within scope, no bloat.

## Threading & Dispatchers

- Composable code runs on `Dispatchers.Main` (UI thread) only; no coroutines launched in the UI.
- `vm.updateSettings(...)` launches inside `viewModelScope` (Dispatcher default for ViewModel = Main; the `settingsStore.update` suspending call is responsible for offloading disk I/O per `SettingsStore`/`DataStore` internals, which dispatch to `Dispatchers.IO`).
- No network or direct disk access in the edited files; `onUpdate` → `vm.update` → `settingsStore.update` follows existing offloading conventions.
- Invariant "ZERO background threading on Dispatchers.Main" satisfied: no `withContext(Dispatchers.IO)` / `launch` on Main in the UI layer.

## Offline & Error State Handling

- Fully offline-capable: all operations are in-memory list copies + local `SettingsStore`/`DataStore` persistence. No network calls.
- Error states: none introduced at this layer. If `settingsStore.update` throws downstream, that is the ViewModel/repository's responsibility; the dialog performs no fallible I/O itself.
- Empty lorebooks list: the `else` branch (with search + create) only renders when `lorebooks` is non-empty; when empty, the `EmptyStateCard` shows. The create dialog is still reachable only via the "+ Create Lorebook" button in the non-empty branch per the directive's placement ("above the lorebooks list").
- `newBook.id` defaults to `Uuid.random()` via the `Lorebook` data class default, so no null id risk.
