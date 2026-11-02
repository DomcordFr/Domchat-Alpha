# Audit Brief — Task 069: Post-History Instructions, SillyTavern Presets System, Badges, and Reorder Animations

## Intent Verification

| Requirement | File | Status |
|---|---|---|
| Add `POST_HISTORY_INSTRUCTIONS` (@SerialName("post_history")) to `PresetInjectionPosition` enum | `PromptPreset.kt` | Done — fourth enum entry added |
| Wire `POST_HISTORY_INSTRUCTIONS` presets to inject at bottom of context before assistant response, with macro evaluation | `GenerationHandler.kt` | Done — `postHistoryPresets` filtered, `presetMessage()` applied via `RoleplayPromptAssembler.evaluateMacros`, appended at end of both message-building branches |
| Expand built-in presets with SillyTavern presets (Main Prompt, Post-History, Author's Note depth 3, Jailbreak/Auxiliary) | `PreferencesModels.kt` | Done — 4 new presets appended to `promptPresets` list |
| Glassy badges: [System] blue, [Post-History] purple/magenta, [Depth: N] amber, [Before User] emerald | `PromptPresetsBottomSheet.kt`, `SettingPresetsPage.kt` | Done — `PresetBadge` composable added to both files |
| Dropdown in create/edit dialog to choose `injectionPosition` with depth input when At Depth | `PromptPresetsBottomSheet.kt`, `SettingPresetsPage.kt` | Done — position pill row already existed; POST_HISTORY_INSTRUCTIONS added; SettingPresetsPage dialog now includes the row; depth slider shown conditionally |
| `Modifier.animateItem()` on LazyColumn items for add/move/delete | `PromptPresetsBottomSheet.kt`, `SettingPresetsPage.kt` | Done — `.animateItem()` added to both LazyColumns |
| Animate tab switches with `AnimatedContent` | `PromptPresetsBottomSheet.kt` | Done — `AnimatedVisibility` replaced with `AnimatedContent` using `expandVertically`/`shrinkVertically` + fade |

All enum values use `@SerialName` for serialization stability. `domChatDialogContainerColor()` = `Color(0xFF000000)` (pure black) per `Blur.kt:335`. `AppShapes.Tag` = `RoundedCornerShape(50)` per `DomChatDesignTokens.kt:80`.

## Call Chain & Architecture

**Preset definition:**
`PromptPreset` data class (`PromptPreset.kt:18`) carries `injectionPosition: PresetInjectionPosition` and `depth: Int`. `POST_HISTORY_INSTRUCTIONS` is the 4th enum value (`PromptPreset.kt:15`).

**Defaults seeding:**
`PreferencesModels.kt` `DisplaySetting.promptPresets` default list now includes 7 presets: 3 original (system prompts) + 4 SillyTavern-style (Main Prompt SYSTEM, Post-History POST_HISTORY_INSTRUCTIONS, Author's Note AT_DEPTH depth=3, Jailbreak BEFORE_LAST_USER).

**Injection at generation time:**
`GenerationHandler.buildMessages()` (`GenerationHandler.kt:900`) filters `activePresets` by `injectionPosition`. New `postHistoryPresets` are processed with `presetMessage()` which calls `RoleplayPromptAssembler.evaluateMacros(text, charName, userName, modelName)` for macro evaluation (`{{char}}`, `{{user}}`, `{{model_name}}`). They are appended at the **bottom** of `rawBuiltMessages` in both the `orderedSelectedMessages.isNotEmpty()` branch (after latest turn + dynamic context) and the empty-history branch — i.e., immediately before the assistant response.

**UI rendering:**
`PromptPresetsBottomSheet` and `SettingPresetsPage` list each preset with `PresetBadge(position, depth)` showing color-coded label. Edit/create dialogs render a pill-row dropdown over all 4 `PresetInjectionPosition.entries`, showing depth slider when `AT_DEPTH` is selected.

**Mutations:**
- Create/save → `settingsStore.update` (bottom sheet) / `vm.updateSettings` (settings page) → DataStore persist
- Move up/down → reorders list → same persist path
- Delete → confirmation dialog (from task-068) → `deletePreset` / `vm.removeAssistant` → DataStore

Call depth: UI → ViewModel → Repository → DataStore (EncryptedSharedPreferences / DataStore Preferences). No repository logic added; only UI wiring.

## Recomposition & Memory Safety

- All dialog-scoped state uses `remember { mutableStateOf(...) }` keyed to composable lifetime: `isEditingOrCreating`, `editingTargetId`, `formPosition`, `formDepth`, `draftPosition`, `draftDepth`.
- `PresetBadge` is a stateless `@Composable` — no state, no allocations beyond a `Color` pair per call (negligible).
- `AnimatedContent` replaces `AnimatedVisibility` for the edit form — content key is `Boolean`, lifecycle-aware, no naked objects.
- `Modifier.animateItem()` on both `LazyColumn`s — item animations keyed by `key = { it.id }` (already present), so add/move/delete reorders animate smoothly without recomposing unrelated items.
- No `!!` force-unwraps introduced anywhere in the 5 target files. Nullable access uses `?.`, Elvis `?:`, `firstOrNull()?.id`.
- `rememberCoroutineScope`, `rememberPremiumHaptics`, `rememberAmoledDarkMode`, `rememberModalBottomSheetState` — all lifecycle-aware `remember` calls.
- `PresetInjectionPosition.entries.forEach` inside dialog slots is stable — enum `entries` is a static array, no per-frame allocation.
- Writes to `draftPosition`/`draftDepth` occur only inside `onClick` lambdas — no side-effect-during-compose violations.

## Threading & Dispatchers

- All `settingsStore.update { … }` / `vm.updateSettings` calls run inside `scope.launch { … }` (from `rememberCoroutineScope`, inherits Main dispatcher). `SettingsStore.update` performs DataStore IO internally on `Dispatchers.IO` — UI coroutine does not block Main.
- `GenerationHandler.buildMessages()` is a `suspend fun` called from `GenerationHandler.generateText()` which runs on `Dispatchers.IO` (orchestrator). Preset macro evaluation (`RoleplayPromptAssembler.evaluateMacros`) is pure string manipulation — no blocking IO.
- No new coroutines introduced beyond existing `scope.launch` wrappers around pre-existing mutators.
- No `Dispatchers.Main` blocking, no `runBlocking`, no `java.util.concurrent` types.
- `presetMessage()` closure captures `charName`, `userName`, `effectiveModelName` — all `String` values, thread-safe.

## Offline & Error State Handling

- Preset mutations write to `Settings` DataStore (local, always available) — fully offline-capable; zero network dependency.
- `SettingsStore.update` is the canonical path (normalization + secret migration + DataStore persist + in-memory state). Crash-safe; normalization stages run inside the update block.
- `POST_HISTORY_INSTRUCTIONS` presets are filtered from `activePresets` which requires `it.content.isNotBlank()` — blank presets are skipped, no empty injection.
- `presetMessage()` applies `RoleplayPromptAssembler.evaluateMacros` — if macro references are unresolvable, they remain as literal text (no exception thrown).
- Delete gated behind confirmation dialog (task-068) — accidental taps cannot cause data loss.
- `depth` is `coerceAtLeast(0)` in `GenerationHandler` depth grouping; `formDepth`/`draftDepth` clamped by slider `0f..10f` range — no out-of-bounds access.
- No new exception paths introduced; existing `try/catch` in unrelated flows (import/export, chat start) untouched.
