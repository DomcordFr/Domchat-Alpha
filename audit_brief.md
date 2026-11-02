# Audit Brief — Task 068: Redirect Persona Editing to Settings & Unify Glassy Black Destructive Confirmation Dialogs

## Intent Verification

| Requirement | File | Status |
|---|---|---|
| Remove cramped inline persona edit/create form from chat persona bottom sheet | `PersonaPickerBottomSheet.kt` | Done — `isEditingOrCreating`, `editingTargetId`, `form*`, `avatarGalleryLauncher`, `openCreateForm`, `openEditForm`, `savePersona`, and the `AnimatedVisibility` creator/editor block removed. Search field + personas list always visible. |
| Tap Edit (header "Manage Personas") → close sheet + `navController.navigate(Screen.SettingPersonas)` | `PersonaPickerBottomSheet.kt` | Done (header Edit surface already routed to `Screen.SettingPersonas`; preserved). |
| Tap Create Persona (+) → close sheet + `navController.navigate(Screen.SettingPersonas)` | `PersonaPickerBottomSheet.kt` | Done — `openCreateForm()` replaced with `onDismissRequest(); navController.navigate(Screen.SettingPersonas)`. |
| Tap Edit on any persona row → close sheet + `navController.navigate(Screen.SettingPersonas)` | `PersonaPickerBottomSheet.kt` | Done — per-row Edit surface now calls `onDismissRequest(); navController.navigate(Screen.SettingPersonas)` instead of `openEditForm(persona)`. |
| Delete persona shows `AlertDialog` with `containerColor = domChatDialogContainerColor()` (pure black 0xFF000000), `modifier = Modifier.border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, AppShapes.Dialog)`, `ApplyDialogWindowBlur()` | `PersonaPickerBottomSheet.kt` | Done — `personaToDelete` state + dialog at bottom-sheet scope. |
| Character delete confirmation dialog: pure black container + `modifier = Modifier.border(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, AppShapes.Dialog)` | `CharacterHubPage.kt` | Done — modifier added to existing `AlertDialog` (container already `domChatDialogContainerColor()`). |
| Delete preset shows `AlertDialog` confirmation (was inline-delete with no confirmation) | `PromptPresetsBottomSheet.kt` | Done — `presetToDelete` state, delete icon sets state instead of calling `deletePreset` directly, dialog added. |
| Delete preset shows `AlertDialog` confirmation (was inline-delete with no confirmation) | `SettingPresetsPage.kt` | Done — `presetToDelete` state, delete TextButton sets state, dialog added after the edit dialog. |

`Screen.SettingPersonas` exists as `data object SettingPersonas : Screen` at `RouteActivity.kt:1453`. `domChatDialogContainerColor()` = `Color(0xFF000000)` (pure black) per `Blur.kt:335`. `AppShapes.Dialog` = `RoundedCornerShape(28.dp)` per `DomChatDesignTokens.kt:82`.

## Call Chain & Architecture

**Persona edit/create (chat sheet → settings):**
`PersonaPickerBottomSheet` header Edit / (+) / per-row Edit icons → `onDismissRequest()` (sheet collapse callback owned by caller) → `navController.navigate(Screen.SettingPersonas)` → `RouteActivity` `NavHost` `composable<Screen.SettingPersonas>` → `SettingPersonasPage` (settings). UI layer only emits intents; no business logic in the sheet. Navigation uses the existing type-safe `Screen` route — no direct `navController.navigate(Screen.Chat(...))` violation.

**Persona delete:**
`Delete` icon (row, only when `currentPersonas.size > 1`) → `personaToDelete = persona` (remembered UI state) → `AlertDialog` enters composition over the sheet → confirm `Delete` → `deletePersona(id)` → `scope.launch { settingsStore.update { ... } }` → `SettingsStore.update` canonical API (normalization + secret migration + DataStore persist + in-memory state) → dismiss (`personaToDelete = null`). Cancel → `personaToDelete = null`.

**Preset delete (PromptPresetsBottomSheet / SettingPresetsPage):**
Delete icon / TextButton → `presetToDelete = preset` → `AlertDialog` → confirm → `deletePreset(id)` → `settingsStore.update` (bottom sheet) / `vm.updateSettings` (settings page, ViewModel → Repository) → DataStore persist → dismiss. In `PromptPresetsBottomSheet`, `deletePreset` also syncs `conversation.enabledPromptPresetIds` when `onUpdateConversation` is provided (per-chat override path preserved).

**Character delete (CharacterHubPage):**
`CharacterInspectorSheet.onDelete` → `characterToDelete = char` → existing `AlertDialog` → confirm → `vm.removeAssistant(toDelete)` with undo toast → `characterToDelete = null`.

Call depth: UI → ViewModel (`AssistantVM`/`SettingVM`) → Repository → DataStore (EncryptedSharedPreferences / DataStore Preferences). No repository logic added; only UI confirmation gating introduced before existing mutators.

## Recomposition & Memory Safety

- All dialog-scoped state is `remember { mutableStateOf<...?>(null) }` keyed to the composable lifetime: `personaToDelete` (PersonaPicker), `presetToDelete` (both preset files), `characterToDelete` (CharacterHub, pre-existing). Dialogs compose conditionally via `?.let { }` — when null, no `AlertDialog` nodes enter the composition, so no leaked dialog windows.
- No `!!` anywhere. Nullable handled via safe-call `?.let`, Elvis `?:`, and `firstOrNull()?.id` in `deletePersona`.
- `rememberModalBottomSheetState`, `rememberCoroutineScope`, `rememberPremiumHaptics`, `rememberAmoledDarkMode` — all lifecycle-aware remember calls; no naked objects.
- `AlertDialog` uses `properties = DialogProperties(usePlatformDefaultWidth = false)` + `Modifier.widthIn(max = 320.dp)` to avoid oversized dialogs on small screens; `shape = AppShapes.Dialog` clamps the container to the 28.dp token.
- `ApplyDialogWindowBlur()` is invoked inside `titleContent` (a `@Composable` slot) so the `LaunchedEffect` inside it runs once per dialog instance, keyed on `LocalView.current` + blur radius + blur-active flag — no per-frame recomposition.
- No `SnapshotStateList` passed to `LazyColumn.items`; `filteredPersonas` / `allPresets` are immutable `List` snapshots derived via `remember(…, …)` — safe for `items(…, key = { it.id })`.
- `personaToDelete`/`presetToDelete` writes happen only inside `onClick` lambdas, never during composition — no side-effect-during-compose violations.

## Threading & Dispatchers

- All `settingsStore.update { … }` / `vm.updateSettings` / `vm.removeAssistant` calls run inside `scope.launch { … }` (coroutineScope from `rememberCoroutineScope`, inherits Main dispatcher). `SettingsStore.update` performs DataStore IO internally (Dispatchers.IO inside the datastore) — UI coroutine does not block Main.
- `CharacterHubPage` already uses `withContext(Dispatchers.IO)` for file import/export and `withContext(Dispatchers.Default)` for search filtering, then `withContext(Dispatchers.Main)` to write UI state — unchanged by this task.
- No new coroutines introduced beyond the existing `scope.launch` wrappers around the pre-existing mutators (`deletePersona`, `deletePreset`, `vm.removeAssistant`). No `Dispatchers.Main` blocking, no `runBlocking`.
- No `java.util.concurrent` types used.

## Offline & Error State Handling

- Persona/preset mutations write to `Settings` DataStore (local, always available) — fully offline-capable; no network dependency. `SettingsStore.update` is the canonical path and is crash-safe (normalization stages run inside the update block).
- `deletePersona` guards active-persona reassignment: if the deleted persona is active, falls back to `remaining.firstOrNull()?.id` — null-safe via `?.id`, so deletion of the last active persona leaves `activePersonaId` unchanged rather than throwing.
- Delete is gated behind confirmation, so accidental taps cannot cause data loss. Character delete keeps an Undo toast with `vm.undoRemoveAssistant(toDelete)` (pre-existing safety net).
- No exception paths were introduced; existing `try/catch` in unrelated flows (import/export, character start chat) untouched. Errors from DataStore writes propagate through the normal `SettingsStore.update` mechanism and are not swallowed by this change.