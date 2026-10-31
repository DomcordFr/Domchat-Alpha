# Audit Brief — Character Inspector Sheet Polish & HTML Sanitization (task-026)

## Intent Verification

All five directive requirements are implemented in `CharacterHubPage.kt` (CharacterInspectorSheet):

1. **HTML Sanitization** — Added `sanitizeHtml(html: String?): String` helper that strips all raw HTML tags (`<p>`, `</p>`, `<br>`, `<span>`, `</span>`, `<div>`, etc.) via regex `<[^>]*>` and decodes named entities (`&nbsp;`, `&amp;`, `&lt;`, `&gt;`, `&quot;`, `&apos;`, `&#39;`, `&#x27;`) plus decimal/hex numeric entities (`&#NNN;`, `&#xHH;`). Whitespace is collapsed (`\\s+` → single space). `notesBlurb` now flows through `sanitizeHtml(assistant.creatorNotes)` before line-splitting and truncation.

2. **Badge Layout** — Replaced rigid horizontal `Row` (badge area) with `FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp))` so badges wrap responsively on thin screens. Each badge box uses `cardBgColor` (`0xFF141520`) + `0.5dp` `cardBorderColor` (`SpecularBorderSubtle`).

3. **Purged JAI/web links & redundant label** — Badge area now only contains scenario count + version chips. No raw links or redundant chat name label remain.

4. **Scroll Jitter Fix** — Removed `CompositionLocalProvider(LocalOverscrollConfiguration provides null)` overscroll suppression that caused bottom-clamp stutter. The vertical `Column` now uses plain `verticalScroll(scrollState)` so gestures hand off smoothly to the bottom sheet without clamping.

5. **Unified Dark Glass Styling** — Character Voice box and Tabs both use `cardBgColor = Color(0xFF141520)` with `0.5dp` `cardBorderColor` (`SpecularBorderSubtle`) border. No raw black boxes remain in badge/voice/tab areas.

Strict null safety: no `!!` in the file. `sanitizeHtml` uses Elvis (`?: ""`) for nullable input. Unified glass theme `0xFF141520` enforced across cards, voice box, tabs, and badges.

## Call Chain & Architecture

**CharacterInspectorSheet** is a modal bottom sheet composable invoked from `CharacterHubPage` when `inspectingAssistant != null`. It receives `assistant`, `ttsProviders`, `catalogSnapshot`, and callbacks (`onDismiss`, `onStartChat`, `onEdit`, `onViewCreatorNotes`, `onDuplicate`, `onExport`, `onDelete`).

Flow:
- `assistant.creatorNotes` → `sanitizeHtml(...)` → line sequence → first 3 lines → `notesBlurb` → rendered as `Text` (maxLines=3, ellipsis)
- `assistant.alternateGreetings.size + 1` → scenario count badge
- `assistant.characterVersion` → version badge (conditionally shown)
- Both badges rendered in `FlowRow` for responsive wrapping
- Character Voice box shown when `ttsProviders.isNotEmpty()`, bound to `VoiceSelector`
- Tabs (Notes/Personality/Greetings) driven by `selectedTab` state, content via `AnimatedContent`
- `onStartChat` → `vm.createConversation(char.id)` on `Dispatchers.IO` → `navigateToChatPage` on `Dispatchers.Main`

No ViewModel/Repository business logic inside the sheet — UI only renders state and emits intents via callbacks.

## Recomposition & Memory Safety

- **No `!!`**: Verified zero force-unwraps. Nullable `html` parameter handled with `?: ""`. `assistant.creatorNotes` nullable handled by `sanitizeHtml`.
- **Remember scoping**: `selectedTab` via `remember { mutableIntStateOf(0) }`. `notesBlurb` via `remember(assistant.creatorNotes) { ... }` — recomputes only when notes change. `scrollState` via `rememberScrollState()`.
- **FlowRow**: Replaces `Row` — Compose foundation layout, stable across recompositions, no state retention issues.
- **sanitizeHtml**: Pure function, no side effects, no allocations beyond String operations. Regexes are compiled per call (acceptable for infrequent notes rendering).
- **Removed CompositionLocalProvider**: Eliminates an unnecessary recomposition boundary that was forcing the overscroll config down the tree.
- **No naked objects**: All state is `remember`-scoped or `collectAsStateWithLifecycle`-collected (in parent `CharacterHubPage`).

## Threading & Dispatchers

- `sanitizeHtml` runs on `Dispatcher.Main` during composition — pure string manipulation, no blocking, no I/O.
- `FlowRow` layout is Main-thread-only Compose measurement — correct.
- `verticalScroll(scrollState)` on `Column` is Main-thread gesture handling — correct.
- No `withContext`, `Dispatchers.IO`, or `Dispatchers.Default` introduced in this sheet.
- Parent `CharacterHubPage` handles background work: assistant search on `Dispatchers.Default`, conversation creation on `Dispatchers.IO`, UI updates marshalled to `Dispatchers.Main`. Unchanged by this task.

## Offline & Error State Handling

- **Fully local**: HTML sanitization, badge layout, scroll behavior, and dark glass styling work entirely offline. No network dependency.
- **Error resilience**: `sanitizeHtml` handles null input (`html ?: return ""`), malformed entities (regex fallback returns original match), and empty strings gracefully — never throws.
- **Notes missing**: `assistant.creatorNotes == null` → `sanitizeHtml` returns `""` → `notesBlurb` is `""` → blurb section skipped (`notesBlurb.isNotBlank()` guard).
- **Badges missing**: `FlowRow` with zero items renders nothing — no crash.
- **Tabs**: `AnimatedContent` with `selectedTab` state — no error states possible.
- **Graceful degradation**: If `FlowRow` measurement fails on rare device configurations, Compose falls back to standard layout without crashing the sheet.
