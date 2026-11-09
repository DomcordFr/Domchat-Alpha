# Audit Brief — Task-188: Fix InputPickerSheets Compilation Errors

## Intent Verification

Goal: Resolve the GitHub Actions compilation failures in
`app/src/main/java/me/lucid/domchat/ui/components/ai/InputPickerSheets.kt`:

1. `InputPickerSheets.kt:267:23` — "None of the following candidates is applicable" on the lorebook-id search expression.
2. `InputPickerSheets.kt:305:20` — "Unresolved reference 'lorebookGlassSurface'" on the sheet/row modifier call sites.

Root cause: `lorebookGlassSurface` was declared as a **local `@Composable` extension function** inside `LorebooksPickerSheet` (commit `2908093` changed the call sites to `Modifier.lorebookGlassSurface(...)` but left the function scoped locally), so the extension name was not resolvable at the `Modifier.` call sites. The id-search expression `it.id.toString().contains(...)` uses `kotlin.uuid.Uuid.toString()`, which is valid and already used across the module (`Assistant.id.toString()`, `Conversation.id.toString()`); it compiles cleanly once the enclosing function type-checks.

Fix applied (single file, +26/-20 lines):
- Promoted `Modifier.lorebookGlassSurface(shape, isFrosted)` from a broken local function to a **top-level `@Composable` extension function** in the same file.
- Both call sites now pass the resolved `isFrosted` boolean.
- Removed the now-unused local `borderBrush` value (inlined `LiquidGlassTokens.SpecularBorderSubtle`).
- No `!!` force-unwraps introduced; null safety preserved via existing Elvis/defensive patterns.
- Unified glass card theme preserved: reuses `frostedGlassSurface` / `liquidGlassSurface` with `LiquidGlassTokens.SpecularBorderSubtle` borders and `Color.Transparent` fill — consistent with the 0xFF141520 glass theme and the existing sheet/styling conventions.

Invariants honored: zero `!!`, zero background work on `Dispatchers.Main`, all state remembered/collected with lifecycle awareness, existing code style maintained.

## Call Chain & Architecture

```
UI layer (Compose)
  └─ LorebooksPickerSheet (InputPickerSheets.kt)
       ├─ remember settings.lorebooks / filteredLorebooks (search)
       ├─ isFrosted = settings.displaySetting.liquidGlassMode == SOFT_FROSTED
       ├─ ModalBottomSheet(modifier = Modifier.lorebookGlassSurface(sheetShape, isFrosted))
       │     └─ top-level @Composable Modifier.lorebookGlassSurface
       │           ├─ frostedGlassSurface(...)   (SOFT_FROSTED → Haze blur, no AGSL)
       │           └─ liquidGlassSurface(... BALANCED_BLEND) (AGSL backdrop glass)
       └─ lorebook rows: Box(modifier = Modifier.lorebookGlassSurface(shape, isFrosted).clickable(...))
             └─ onClick → onNavigateToLorebook(lorebook.id.toString())
```

Architectural boundaries respected:
- UI layer only renders state and emits intents (`onUpdateConversation`, `onNavigateToLorebook`, `onDismiss`). Zero business logic in the composable.
- `Settings`, `Assistant`, `Conversation` are passed in as immutable data parameters (no ViewModel/repo access inside this file).
- Glass-surface primitives (`frostedGlassSurface`/`liquidGlassSurface`) live in `ui/theme/LiquidGlass.kt` and read `LocalSettings.current` internally — consistent with the rest of the codebase (`LiquidGlass.kt:305`, `:483`).
- `Lorebook.id` is `kotlin.uuid.Uuid`; `.toString()` is the standard conversion (same as `Assistant.id.toString()`/`Conversation.id.toString()` used module-wide).

## Recomposition & Memory Safety

- All derived state is `remember`-scoped: `availableSkills`, `userInvocableSkills`, `filteredLorebooks`, `localEnabledIds`, `lorebookSearchQuery`, `sheetState`, `scope`, `density`, `configuration`, `maxSheetHeight`, `isFrosted`.
- `filteredLorebooks` re-runs only when `settings.lorebooks` or `lorebookSearchQuery` changes (no unstable lambdas passed to `remember` keys).
- `lorebookSearchQuery` is `remember { mutableStateOf("") }` — owned by the composition, released with the sheet.
- `Modifier.lorebookGlassSurface` is a top-level `@Composable` extension; it captures no unstable locals and reads settings only through the passed `isFrosted` flag plus the shared primitives' own `LocalSettings.current` reads (consistent with `liquidGlassSurface`/`frostedGlassSurface`).
- No `SnapshotStateList` passed directly to `items()`; `filteredLorebooks` (a `List`) is iterated with `forEachIndexed` inside a recomposable scope — no derived-state violation.
- No naked objects: `sheetState` via `rememberModalBottomSheetState`, `scope` via `rememberCoroutineScope()`, scroll state via `rememberScrollState()`.
- No `!!` anywhere in the edited file (grep-verified); all nullable access uses the existing safe patterns.

## Threading & Dispatchers

- `InputPickerSheets.kt` performs **no coroutine dispatching** and no direct `Dispatchers.Main`/`Dispatchers.IO` usage.
- The only coroutine handle is `rememberCoroutineScope()` (`scope`), which is declared but not used for async work in this file (kept for parity with sibling sheets); any future launch must route disk/network to `Dispatchers.IO` per project invariants.
- All state reads (`settings.displaySetting.liquidGlassMode`, `LocalSettings.current` inside the glass primitives) happen on the UI thread during composition — Compose-correct, no cross-thread reads.
- No suspend functions, no `flow`, no `withContext` in this file. Threading concerns (ViewModel/Repository/Dispatchers.IO) are owned by the calling layer, not this UI component.

## Offline & Error State Handling

- This file is a pure UI picker sheet; it has no network or disk access, so there are no offline/exception paths to handle directly.
- UI states are handled explicitly:
  - Empty search → `DomChatSearchBar` with placeholder; `filteredLorebooks.isEmpty()` renders a "none" text (`R.string.lorebooks_picker_none`).
  - Search filtering is client-side over `settings.lorebooks` (already local data), so it works offline with no fallback needed.
  - Dismiss → `onDismiss()`; navigation → `onNavigateToLorebook(id)`; toggles → `onUpdateConversation(conversation.copy(...))`.
- No `try/catch` needed (no blocking/suspend calls). Error propagation to the caller is via the passed-in callback lambdas, which the ViewModel layer handles per standard `StateFlow` error emission.

## Verification Notes

- Cannot run a local Gradle build per task constraints (`Do NOT run local gradle builds`).
- Fix scope: single file, minimal diff, no new imports (reuses existing `Shape`, `Color`, `dp`, `LiquidGlassTokens`, `frostedGlassSurface`, `liquidGlassSurface`, `LiquidGlassMode` imports).
- `git diff --stat`: 1 file changed, 26 insertions, 20 deletions.
