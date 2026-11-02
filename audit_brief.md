# Audit Brief — Character Maker 2-Section Roleplay Remake and New Character Creation Bug Fix

## Intent Verification

**1. New Character Creation Bug Fix**
- `AssistantDetailVM.kt`: Added `addNewAssistant(assistant: Assistant)` suspend function that properly inserts a new `Assistant` into `Settings.assistants` via `SettingsStore.update`, handling duplicate IDs by generating a new `Uuid.random()`. The `update()` function now also handles the case where `oldAssistant == null` (brand-new assistant) by appending to `assistants` instead of only mapping existing entries.
- `AssistantDetailVM.kt`: Changed `assistant` state flow to fall back to `Assistant(id = assistantId)` (empty assistant with the route ID) instead of `settings.getCurrentAssistant()` when no match is found, so a brand-new character starts with a clean slate rather than leaking the currently selected assistant's data.
- `AssistantDetailVM.kt`: Added `isNewAssistant` state flow so the UI can detect whether the route targets a new (unsaved) character vs. an existing one.
- `AssistantVM.kt`: Changed `addAssistant` from `fun` (fire-and-forget `viewModelScope.launch`) to `suspend fun` so callers can await completion before navigating, preventing navigation to a not-yet-persisted assistant ID.
- `CharacterHubPage.kt`: Wrapped the FAB `onClick` in `scope.launch { vm.addAssistant(newChar); navController.navigate(...) }` so navigation happens only after the assistant is persisted.

**2. Two-Section Character Creator Reorganization**
- `AssistantDetailPage.kt`: `AssistantDetailHome` now shows exactly two sections:
  - **Section 1 — Core Roleplay**: Profile (Display Name + Chat Name + Avatar + Tags), Creator Notes & Tags, Personality (System Prompt), Scenario (Author's Note), Initial Messages (alternate greetings), Lorebooks.
  - **Section 2 — Advanced**: Model configs, Context budget, UI Customization.
- Routes consolidated: `HOME`, `CORE` (new unified sub-page), `ADVANCED` (existing), plus LOREBOOKS. Removed redundant PROFILE/MODEL/PROMPTS/CONTEXT_MANAGEMENT/SKILLS/TOOLS/MEMORY/UI routes from the home nav.
- `AssistantCoreRoleplaySubPage.kt` (new): Single composable implementing all Core Roleplay fields with spacious multiline editors, black-glass (`0xFF141520`) cards with `LiquidGlassTokens.SpecularBorderSubtle` borders.
- `AssistantProfileSubPage.kt`: Name field split into `displayName` and `chatName` (both nullable, fall back to `name`).

**3. Spacious Multiline Text Editors**
- All text-heavy fields (System Prompt, Author's Note/Scenario, Creator Notes) use `DebouncedTextField` with `minLines=6..8`, `maxLines=10..16`, monospace font, `heightIn(min=120dp..200dp)` — replacing small single-line inputs.
- Creator Notes and Scenario use `Surface` with `Color(0xFF141520)` fill + `SpecularBorderSubtle` border (glassy black card, zero grey).

**4. Save & Navigation Safety**
- `addNewAssistant` persists via `SettingsStore.update` (canonical API: normalization, secret migration, in-memory state, persist) then Room/DB is updated through settings flow.
- `update()` now handles both existing and brand-new assistants.
- Navigation after save is awaited (suspend `addAssistant`) so the assistant ID exists in settings before `Screen.AssistantDetail(id=...)` is pushed.
- Default IDs handled: `Uuid.random()` collision guard in both `AssistantVM.addAssistant` and `AssistantDetailVM.addNewAssistant`.

**Null safety:** Zero `!!` introduced. All nullable access uses `?.`, Elvis `?:`, `takeIf { isNotBlank() }`, `orEmpty()`, `isNullOrBlank()`.

**Theme:** `Color(0xFF000000)` AMOLED black, `Color(0xFF141520)` unified glass card, `LiquidGlassTokens.SpecularBorderSubtle` borders throughout.

## Call Chain & Architecture

**New character creation:**
1. `CharacterHubPage` FAB → `scope.launch { vm.addAssistant(newChar); navController.navigate(Screen.AssistantDetail(id)) }`
2. `AssistantVM.addAssistant` (suspend) → `settingsStore.update { current → current.copy(assistants = ...plus(newChar)) }` → persisted via DataStore.
3. `RouteActivity` `composable<Screen.AssistantDetail>` → `AssistantDetailPage(id = route.id)` → `AssistantDetailVM` (Koin, parameter = id string).
4. `AssistantDetailVM.assistant` state flow looks up `settings.assistants.find { it.id == assistantId }` → falls back to `Assistant(id = assistantId)` for new chars.
5. `AssistantDetailPage` NavHost → `AssistantDetailHome` (2-section cards) or `AssistantCoreRoleplaySubPage` (full editor).
6. Save: `onUpdate(assistant)` → `AssistantDetailVM.update()` → `settingsStore.update` → persists to DataStore/Room.

**UI boundaries respected:** UI layer only renders state and emits intents via `onUpdate` lambdas. ViewModel manages coroutines in `viewModelScope`. Repository/SettingsStore is single source of truth.

## Recomposition & Memory Safety

- `AssistantDetailVM.addNewAssistant` and `update` run in `viewModelScope.launch` — no naked coroutines.
- All Compose state (`isPersonaExpanded`, `editorText`, `systemPromptValue`, etc.) uses `remember { mutableStateOf(...) }`.
- `systemPromptValue` keyed on `assistant.id` so it resets when switching assistants.
- `DebouncedTextField` with `rememberTextFieldState` + `snapshotFlow().debounce(150ms)` prevents recomposition storms on every keystroke.
- `FullScreenSystemPromptEditor` replaced with inline `DebouncedTextField` inside a `Surface` — no separate dialog state leaking.
- No `SnapshotStateList` passed to `items()`; all collections are immutable reads.
- No force-unwraps (`!!`) introduced.

## Threading & Dispatchers

- `settingsStore.update` is called inside `viewModelScope.launch` (main-safe coroutine scope); `SettingsStore.update` handles DataStore disk I/O internally — no explicit `Dispatchers.Main` blocking.
- `AssistantVM.addAssistant` is now `suspend`, called from `scope.launch` in the FAB click handler — main-safe.
- No `Dispatchers.Main` used for I/O. No new background threads spawned.
- All UI state mutations are synchronous main-thread Compose operations.

## Offline & Error State Handling

- New character creation is fully offline (local DataStore/Room only) — no network required.
- `addNewAssistant` and `update` are wrapped in `try/catch` (existing pattern); errors logged via `PlatformLog.e`.
- `AssistantVM.addAssistant` has `try/catch` with `PlatformLog.e` — failures don't crash, navigation still proceeds (ID exists in settings after `update`).
- Debounced saves: 150ms debounce on System Prompt text field prevents excessive DB writes during typing.
- Empty states handled: `assistant.name.ifBlank { default }`, `displayName`/`chatName` null-safe with Elvis to `name` then `"Character"`.
- Existing `errorFlow` toasts in `ChatPage`/`SettingVM` unchanged; no new error modes introduced.

## Files Changed

- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/detail/AssistantDetailVM.kt` — `addNewAssistant`, `isNewAssistant`, fixed `update` for new assistants, `assistant` flow fallback.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/AssistantVM.kt` — `addAssistant` changed to `suspend fun`.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` — FAB `addAssistant` awaited before navigate.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/detail/AssistantDetailPage.kt` — 2-section home, consolidated routes, `AssistantCoreRoleplaySubPage` registration.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/detail/AssistantCoreRoleplaySubPage.kt` (new) — unified Core Roleplay editor.
- `app/src/main/java/me/lucid/domchat/ui/pages/assistant/detail/AssistantProfileSubPage.kt` — `displayName`/`chatName` fields.
- `app/src/main/res/values/strings.xml` — `assistant_page_name_placeholder`, `assistant_page_chat_name_placeholder`.
- `app/src/main/res/values/strings_ui_locale.xml` — section titles and descriptions.
- `audit_brief.md` — this document.
