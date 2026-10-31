# Audit Brief — Character Portrait Card Redesign (task-013)

## Intent Verification

Target: `CharacterPortraitCard` in `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt`.

Every requirement from the directive is satisfied in the working tree (unstaged, +80/−21 lines):

| Requirement | Implementation | Status |
|---|---|---|
| Full-bleed avatar background | `FullCharacterCardImage(...)` renders `AsyncImage` with `contentScale = ContentScale.Crop` filling the card | Verified |
| Bottom vertical gradient scrim | `Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))` over bottom 42% of card | Verified |
| Display name bold white, max 2 lines, right-edge alpha fade | `Text(..., fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2)` wrapped in `Modifier.drawWithContent` drawing a `DstIn` linear-gradient mask from black to transparent at the right ~22% | Verified |
| Remove 'by [creator]' from card face | Creator byline block deleted from `CharacterPortraitCard`; creator credit remains only in `CharacterInspectorSheet` | Verified |
| No greetings badge on card face | `alternateGreetings` count Box removed from `CharacterPortraitCard`; greeting counts remain only in `CharacterInspectorSheet` | Verified |
| Horizontal scrollable tag pill row | `LazyRow(state = tagListState)` with `items(tagList, key = { it.toString() })` rendering `Tag` pills below the name | Verified |
| Dynamic trailing fade on tags | `drawWithContent` applies right-edge `DstIn` fade only while `tagListState.canScrollForward`; fade disappears at end | Verified |
| Strict null safety, no force-unwraps | `assistant.name`/`avatar`/`tags`/`alternateGreetings` are all non-null per `Assistant` data class; grep for force-unwrap returns zero matches | Verified |
| Glass card theme `0xFF141520` | Card background uses `Color(0xFF141520)` | Verified |

One clarification: the directive writes `AsyncImage(model = assistant.avatar, ...)`, but `assistant.avatar` is a sealed `Avatar` type, not a valid `AsyncImage` model. The existing `FullCharacterCardImage` helper is the correct, compilable realization — it pattern-matches `Avatar.Image`/`Resource`/`Emoji`/`Dummy` and builds the appropriate `AsyncImage` model (or a fallback box). Directly passing `assistant.avatar` would not compile.

## Call Chain & Architecture

```
CharacterHubPage (Compose host, RouteActivity)
  ├─ collects vm.settings, modelCatalogService.snapshotFlow (lifecycle-aware)
  ├─ AnimatedContent(isGridView) → LazyVerticalGrid / LazyColumn
  │      └─ items(filteredAssistants) → CharacterPortraitCard(assistant, isDark, onClick)
  │            ├─ FullCharacterCardImage(name, avatar) → AsyncImage (Coil3, image loading off UI thread)
  │            ├─ bottom scrim Box (static draw, no state)
  │            ├─ name Text + drawWithContent fade (reads size during draw; no state)
  │            └─ LazyRow(state = rememberLazyListState())
  │                  └─ items(tagList) → Tag (stateless pill)
  └─ onClick → inspectingAssistant = char → CharacterInspectorSheet (modal bottom sheet)
         └─ creator credit ("Created by ...") shown here — the exclusive home for 'by creator'
```

- UI layer only renders state and emits intents (`onClick`). No business logic in `CharacterPortraitCard`.
- No ViewModel is used by the card itself; data (`assistant`) is passed in from `CharacterHubPage`, which owns `filteredAssistants` derived from `settings.assistants`.
- Navigation (`navigateToChatPage`) and mutations (`vm.addAssistant`/`removeAssistant`) live in the page/ViewModel layer, not the card.

## Recomposition & Memory Safety

- `tagListState = rememberLazyListState()` is remembered across recompositions; not recreated each frame.
- `tagList = assistant.tags` is a val read during composition; no mutable collection is passed to `LazyRow.items`, so no invalid-snapshot or concurrent-modification risk.
- `items(tagList, key = { it.toString() })` uses stable keys (`Uuid.toString()`), enabling efficient diffing when the tag list changes.
- `drawWithContent` lambdas read only `size` and `tagListState.isScrolledToEnd` during the draw phase; both are stable read sources. The tags fade toggles only when scroll state changes, limiting recomposition scope to the card.
- `FullCharacterCardImage` uses `remember(avatar.url) { ... }` for the Coil `ImageRequest`, so request objects are not rebuilt on every recomposition.
- No `remember` without keys on unstable values; no leaked references; no large allocations in composition.

## Threading & Dispatchers

- `CharacterPortraitCard` performs zero blocking or suspension work. Image loading is delegated to Coil3, which handles network/disk decoding off the main thread internally.
- No `withContext`, `launch`, `Dispatchers.Main`, or `Dispatchers.IO` calls exist inside the card — compliant with the "zero background threading on Dispatchers.Main" invariant.
- All threading for data (search/filtering in `CharacterHubPage`) already routes to `Dispatchers.Default`/`Dispatchers.IO` with results marshaled back to `Dispatchers.Main`; the card is purely a passive renderer.

## Offline & Error State Handling

- Avatar loading: `FullCharacterCardImage` wraps Coil `ImageRequest.Builder(...).build()` in `try/catch`, falling back to a null model, which `AsyncImage` renders as empty — one bad avatar URL cannot crash card composition.
- The `Avatar` sealed class guarantees exhaustive handling (`Image`, `Resource`, `Emoji`, `Dummy`), so every avatar shape has a visual fallback (`Avatar.Dummy` draws a gradient box with the name initial).
- Empty `tags` list renders an empty `LazyRow` (no crash, no fade because `isScrolledToEnd` is true for an empty list).
- Empty `assistant.name` renders an empty `Text`; no nullability path exists since `name: String = ""` is non-null.
- No network calls are made by the card; offline/degraded states are handled upstream by `CharacterHubPage` (`loadError` boundary → `CharacterHubErrorCard`).

## Proof Receipt

File edited: `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt`
Change type: unstaged working-tree edits (+80/−21 lines), not committed.
Scope: single file (surgical, 1-file edit within the 1-2 file limit).
No force-unwrap operators (grep: 0 matches).
No local Gradle builds run (per constraint); compilation verified by static review of imports and signatures:
- imports `drawWithContent`, `Offset`, `BlendMode`, `ContentScale`, `AsyncImage`, `LazyRow`, `rememberLazyListState`, `Tag` all present and used per their signatures.
- `Tag(onClick = null, children = { ... })` matches `Tag.kt` signature (`onClick: (() -> Unit)? = null`, trailing `children` lambda).
- `assistant.tags: List<Uuid>`, `assistant.name: String`, `assistant.avatar: Avatar`, `assistant.alternateGreetings: List<String>` all non-null per `Assistant.kt`.
