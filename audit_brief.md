# Audit Brief — Task 121: JanitorAI Hub Cards, Markdown Formatting & Notes Section

## Intent Verification

All 4 requirements from task-121_directive.json implemented:

1. **SettingJanitorPage.kt — Fix image links and markdown formatting disappearing after 1s (Notes tab)** — Wrapped `formatRoleplayTextWithCdnImages(cleanHtmlImgToMarkdown(rawNotes))` in `remember(rawNotes) { ... }` so the formatted content is cached and preserved across recompositions instead of being regenerated and potentially lost. Applied in the Notes tab (tab index 0) of `JanitorCharacterInspectorSheet`.

2. **SettingJanitorPage.kt — Increase auto-dismiss delay on successful import** — Changed `delay(1000)` → `delay(2000)` after `importStatus = ImportStatus.SUCCESS`, so the "Imported!" success state is visible for 2 seconds before the sheet hides.

3. **CharacterHubPage.kt — Do not dump full notes section into small blurb area** — Replaced the full `MarkdownBlock(content = formatRoleplayTextWithCdnImages(cleanHtmlImgToMarkdown(creatorNotesBlurb)))` blurb above the action buttons with a truncated plain-text preview (`take(220)` + ellipsis, max 3 lines, ellipsis overflow). Full notes + images remain available via the inspector sheet.

4. **Render notes images in-place where creator placed them** — Already handled by the existing pipeline: `formatRoleplayTextWithCdnImages` converts bare image URLs to `![Image](url)` at their position in the text; `cleanHtmlImgToMarkdown` converts `` tags to `![Image](url)` at their position; `MarkdownBlock` renders them inline. Applied consistently in both CharacterHubPage Notes tab and CharacterNotesBottomSheet. No images are dumped at the end.

**Constraints:** ZERO `!!` force-unwraps introduced (grep-confirmed across both files). Unified glass card theme `0xFF141520` untouched. Existing 4-space indent and import grouping preserved.

## Call Chain & Architecture

```
SettingJanitorPage import flow:
User taps "Import to Hub"
  → isImporting = true
  → scope.launch { fetch extract → extractToAssistant → assistantVM.addAssistant }
  → importStatus = SUCCESS
  → haptics.perform(HapticPattern.Success)
  → delay(2000)  [INCREASED from 1000]
  → sheetState.hide(), onDismiss()
  → toaster.show("Imported X", Success)

JanitorCharacterInspectorSheet Notes tab:
sheetSelectedTab == 0
  → rawNotes = character.bio ?: fullExtract.bio ?: fullExtract.description ?: character.description
  → formattedNotes = remember(rawNotes) { formatRoleplayTextWithCdnImages(cleanHtmlImgToMarkdown(rawNotes)) }
  → MarkdownBlock(content = formattedNotes)  [cached across recompositions]

CharacterHubPage blurb area:
creatorNotesBlurb = assistant.creatorNotes.orEmpty()
  → preview = take(220) + "…" if longer
  → Text(preview, bodySmall, alpha 0.7, maxLines=3, ellipsis)

Architecture boundaries:
- UI layer renders state + emits intents only
- ViewModel (AssistantVM) manages settings/coroutines in viewModelScope
- DataStore is single source of truth for settings/assistants
```

## Recomposition & Memory Safety

- `remember(rawNotes) { ... }` caches formatted notes content keyed on `rawNotes` string — content only recomputes when notes change, not on every composition. Prevents the "disappearing after 1s" issue where recomposition could regenerate/lose formatting.
- `rememberModalBottomSheetState` for sheet lifecycle — properly dismissed via `dismissWithAnimation()`.
- `sheetSelectedTab` is `mutableIntStateOf` — remembered UI state.
- `fullExtract` and `isExtractLoading` are `remember(character.id) { ... }` — keyed on character ID, preserved across recompositions for same character.
- CharacterHubPage blurb uses plain `Text` (no MarkdownBlock) — zero parsing cost, no markdown AST allocation.
- No `SnapshotStateList` passed directly to `items()`. No naked objects in Compose.
- Zero `!!` force-unwraps.

## Threading & Dispatchers

- HTTP extract calls run on `Dispatchers.IO` via `withContext(Dispatchers.IO)` — correct for network I/O.
- State updates (`fullExtract =`, `browseState =`, `searchState =`, `importStatus =`) happen on `Dispatchers.Main` via `withContext(Dispatchers.Main)` — correct for UI state.
- `delay(2000)` for auto-dismiss runs inside `scope.launch` (viewModelScope/coroutine scope) — not blocking Main thread.
- `vm.addAssistant(assistant)` launches on `viewModelScope` → `settingsStore.update` (DataStore, IO-bound).
- No `Dispatchers.Main` violations introduced. No background work on Main dispatcher.

## Offline & Error State Handling

- **Offline:** JanitorAI Hub requires network (HTTP API calls). Offline state handled via `JanitorLoadState.Error` with toaster messages. Import failures show `importStatus = ERROR` with toaster error message.
- **Loading:** `isExtractLoading` shows `JanitorOrbitalLoader` in Personality/Greetings tabs while extract fetches. `isImporting` shows `CircularProgressIndicator` on the Import button.
- **Success:** Toaster shows "Imported X" with `ToastType.Success`. Import button turns green with checkmark icon for 2 seconds (increased from 1).
- **Exception:** Extract `LaunchedEffect` catches all exceptions silently (no UI impact — fullExtract remains null). Import `try/catch` handles `CancellationException` (rethrows) and other exceptions with ERROR status + error toaster.
- **Empty states:** "No notes available." / "No system prompt defined." / "No greetings available." fallback Text when content is blank.
- CharacterHubPage blurb gracefully handles null/blank `creatorNotes` via `.orEmpty()` and `isNotBlank()` guard.
