# Audit Brief — Task-075: In-Place Markdown and Inline Image Positioning

## Intent Verification

All three directive requirements are implemented:

1. **SettingJanitorPage.kt Greetings tab** — Replaced `parseContentWithImages(greetingText)` + 160dp `Box`/`AsyncImage` per-segment rendering with `MarkdownBlock(content = formatRoleplayTextWithCdnImages(cleanHtmlImgToMarkdown(greetingText)))`. Greetings now render full markdown (bold, italic, hyperlinks) with images inline in the text flow.

2. **SettingJanitorPage.kt & CharacterHubPage.kt** — Removed the redundant `LazyRow` "Embedded Images" gallery that dumped all images at the top/end of inspector sheets. Images now render inline via `MarkdownBlock` instead of a separate gallery.

3. **ChatMessageV2.kt** — Piped `displayContent` through `cleanHtmlImgToMarkdown()` then `formatRoleplayTextWithCdnImages()` at all three `MarkdownBlock` call sites (user message turn, assistant bubbles mode, assistant no-bubbles mode). Bare image URLs and `<img src=...>` tags in chat messages now render as rich inline images.

## Call Chain & Architecture

### SettingJanitorPage.kt
- `SettingJanitorPage()` → `JanitorCharacterInspectorSheet()` (modal bottom sheet)
  - Tab 2 (Greetings): `allGreetings` carousel → `AnimatedContent` → `MarkdownBlock(content = formatRoleplayTextWithCdnImages(cleanHtmlImgToMarkdown(greetingText)))`
  - `formatRoleplayTextWithCdnImages` (CharacterNotesBottomSheet.kt:54): wraps bare `https?://...image` URLs in `![Image](url)` markdown
  - `cleanHtmlImgToMarkdown` (CharacterNotesBottomSheet.kt:66): converts `<img src="url">` → `![Image](url)`
  - `MarkdownBlock` (Markdown.kt:716): parses markdown AST, renders images inline via Coil3 `AsyncImage`

### CharacterHubPage.kt
- `CharacterHubPage()` → `CharacterInspectorSheet()` (modal bottom sheet)
  - Tab 0 (Notes): `MarkdownBlock(formatRoleplayTextWithCdnImages(cleanHtmlImgToMarkdown(assistant.creatorNotes)))` — already inline
  - Tab 1 (Personality): `MarkdownBlock(formatRoleplayTextWithCdnImages(assistant.systemPrompt))` — already inline
  - Tab 2 (Greetings): `MarkdownBlock(formatRoleplayTextWithCdnImages(cleanHtmlImgToMarkdown(greetingText)))` — already inline
  - Removed: `embeddedImageUrls` `remember` block + `LazyRow` gallery + `viewerImageUrls`/`viewerOpen`/`viewerCurrentPage` state + Image Viewer Dialog

### ChatMessageV2.kt
- `ChatMessageTurn()` → `UserMessageTurn()` / `AssistantMessageTurn()`
  - Each text bubble computes `displayContent` via `remember { ... }`
  - Pipeline: `part.text` → `replacePersonaPlaceholders` → `replaceRegexes` → `cleanHtmlImgToMarkdown` → `formatRoleplayTextWithCdnImages` → `MarkdownBlock(content=...)`
- `formatRoleplayTextWithCdnImages` / `cleanHtmlImgToMarkdown` are in the same package (`me.lucid.domchat.ui.components.chat`), no import needed.

## Recomposition & Memory Safety

- **`remember` keys**: All `displayContent` computations use `remember(text, assistant, userNickname)` — recompute only when inputs change. No unstable keys.
- **No `SnapshotStateList` passed to `items()`**: Removed `items(embeddedImageUrls, ...)` LazyRow galleries that could trigger unnecessary recompositions on URL list changes.
- **No `!!`**: All nullable access uses Elvis operators (`?:`) or safe calls (`?.`). Verified across all three files.
- **`MarkdownBlock` synchronous parse**: Content < 4000 chars parses synchronously on UI thread (memoized via `markdownAstCache`), larger content defers to `Dispatchers.Default`. No 0-height flash.
- **`AnimatedContent` in greetings carousel**: Unchanged — still uses `tween(240)`/`slideInHorizontally` transitions, now wrapping `MarkdownBlock` instead of per-segment `Column`.

## Threading & Dispatchers

- **No `Dispatchers.Main` for I/O**: All network/disk operations in these files already route to `Dispatchers.IO` (e.g., `httpClient.execute` in `loadBrowse`/`loadSearch`/`extract` LaunchedEffects). The MarkdownBlock rendering is UI-thread only (local text transformation, no I/O).
- **`formatRoleplayTextWithCdnImages` / `cleanHtmlImgToMarkdown`**: Pure string transformations, synchronous, safe on UI thread.
- **Coil3 `AsyncImage`**: Image loading is handled internally by Coil3 on its own dispatcher, not blocking Compose UI thread.
- **No new coroutines launched**: Changes are purely synchronous UI transformations within existing composables.

## Offline & Error State Handling

- **Offline images**: `AsyncImage` in `MarkdownBlock` uses Coil3 with placeholder/fallback support. If image URL is unreachable, Coil3 shows the fallback painter (no crash).
- **Malformed HTML/img tags**: `cleanHtmlImgToMarkdown` uses regex replacement; non-matching HTML passes through unchanged. No exceptions thrown on malformed input.
- **Empty/null content**: `formatRoleplayTextWithCdnImages` and `cleanHtmlImgToMarkdown` handle null/empty strings safely (regex `.replace` on empty string is a no-op). `MarkdownBlock` renders empty content gracefully.
- **No try/catch needed**: The pipeline is pure string manipulation — no network, file, or parsing exceptions possible at these call sites.
- **Greetings dummy guard**: `isDummyGreeting()` filters placeholder greetings (".", "-", "none", etc.) before rendering — unchanged.
