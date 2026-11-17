# See AGENTS.md for canonical invariant contracts and laws.

# DOMCHAT MASTER ARCHITECTURE & INVARIANT CONTRACT (V2)

## 1. HOST & WORKFLOW LAWS (TERMUX MOBILE RIG)
1. **NO LOCAL GRADLE:** Never invoke `./gradlew`, `gradle`, or `kotlinc` locally. Always verify via `python3 scripts/domchat_preflight.py`.
2. **NO FULL-FILE READS:** Never read more than 120 lines of any `.kt` file at once. Use `sed -n 'X,Yp' <file>` or `rg -n -C 5 <pattern> app/src/`.
3. **NO CI POLLING LOOPS:** Never poll GitHub Actions in 15s–60s loops. Use `scripts/domchat_rig.sh ci-wait` which sleeps silently for 5 minutes before checking status once.
4. **NO SILENT FEATURE ERASURE:** When fixing a UI or state bug, you are strictly forbidden from deleting unrelated existing callbacks, `HapticPattern` triggers, `creatorNotes` popovers, drag-and-drop modifiers, or `DisposableEffect` cleanup blocks.
5. **NO LEGACY RESURRECTION:** The package namespace `me.rerere.rikkahub` (from the legacy LastChat fork) is extinct. Always use `me.lucid.domchat.*`. Never mix deprecated `Kyant0` or `FletchMcKee` blur modifiers into `liquidGlassSurface`.

---

## 2. THE 3-TIER LIQUID GLASS BACKDROP DAG (`me.lucid.domchat.ui.component.backdrop.*`)
- **Tier 1 (`wallpaperBackdrop`):** Root wallpaper/canvas (`Background.kt`). Consumed ONLY by `ChatMessageV2`, `GroupedMessageBubble`, and cold-start center action pills.
  - *Cold-Start Invariant:* Both `AsyncImage.onState(State.Success)` AND the blank default canvas branch (`SideEffect { onWallpaperLoaded?.invoke() }`) MUST call `wallpaperBackdrop.geometryChanged()` on Frame 0.
- **Tier 2 (`chatBackdrop`):** Composite of wallpaper + chat messages. Consumed by top bar and bottom composer bar.
- **Tier 3 (`overlayBackdrop`):** Full-page viewport. Consumed ONLY by transient overlays (`DomChatInWindowBottomSheet`, side drawer, floating context menus).
  - *Route Guard Invariant:* Always guard with `if (chatListReady) overlayBackdrop else null` in `ChatPage.kt`.
  - *Anti-Mirroring Law:* Never bind `ChatMessageV2` or `GroupedMessageBubble` to `overlayBackdrop`.
- **Sampler Lifecycle Law:** `DrawBackdropNode.onDetach()` MUST NEVER mutate or set `(backdrop as? LayerBackdrop)?.layerCoordinates = null`. If `currentBackdropOffset()` is `null` during a 120 FPS `LazyColumn` fling, fall back to translating the cached layer at `lastDrawOffset`.
- **Corner Clipping Law:** Always apply `.clip(shape)` BEFORE drawing shader/backdrop layers so AGSL refraction never bleeds into sharp rectangular corners.

---

## 3. COMPOSE UI, MODIFIER ORDER & WINDOW INSETS LAWS
1. **Container Surface Order:** `Modifier.width(...).then(surfaceModifier).padding(horizontal = 8.dp, vertical = 8.dp)` — Never place `.padding()` before `surfaceModifier`.
2. **Interactive Row Order:** `Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).background(...).clickable(...).padding(horizontal = 14.dp, vertical = 8.dp)` — Never place `.padding()` before `.clip()` or `.clickable()`. Icon-to-text spacing inside rows must be `Arrangement.spacedBy(14.dp)`.
3. **Single Scaffold & IME Insets:** Never nest a `Scaffold` inside another `Scaffold` in `ChatPage.kt`. Coordinate `imePadding()` and `navigationBarsPadding()` on the bottom composer bar so opening the keyboard never creates a black gap.
4. **Back Gesture Hierarchy:** `BackHandler(enabled = ...)` must pop open `DomChatInWindowBottomSheet` instances, side drawers, or active search bar focus BEFORE allowing the activity to exit.
5. **Sequential Drawer Navigation:** Always suspend until `drawerState.close()` completes before invoking route navigation (`navController.navigate`) to prevent 120 FPS frame drops.
6. **Popup Coordinate Space:** Context menus inside `ConversationList` must anchor using `coordinates.positionInParent()` (local container space), never raw `positionInWindow()`.
7. **In-Window Bottom Sheets:** Never use Material3 `ModalBottomSheet`. Always use `DomChatInWindowBottomSheet` capped at `heightIn(max = maxHeight * 0.60f)` with vertical drag gestures wired on both header and scrim.

---

## 4. DATA LAYER, PARSING, LOREBOOKS & AUDIO INVARIANTS
1. **SillyTavern V2 & Room `CursorWindow` Safety:**
   - All `kotlinx.serialization` Character Card DTO fields MUST declare default values (`= ""`, `= emptyList()`) with `ignoreUnknownKeys = true`.
   - Room DAO list queries (`getAllCharacters()`, `getConversations()`) must NEVER select massive raw PNG base64 chunks or 100k-token prompt columns into list cursors; fetch lightweight summary projections for lists and load full card blobs lazily by ID.
   - Keep `character.displayName` strictly decoupled from `conversation.title`.
2. **JanitorAI (JAI) Markdown & HTML Sanitizer:**
   - Strip raw HTML (`<font>`, `<div>`, `<marquee>`, raw `<img>`) before Compose text rendering.
   - Sanitize unclosed roleplay asterisks (`*action*`) per paragraph so stray asterisks never italicize entire multi-paragraph responses.
3. **Lorebook Recursion & Boundary Guard:**
   - Keyword matching must enforce word boundaries (`\b`) so short keys (`cat`) do not match inside substrings (`caterpillar`).
   - Enforce `MAX_RECURSION_DEPTH = 2` with a `visitedEntryIds: MutableSet<String>` guard to prevent circular Entry A <-> Entry B infinite loops.
4. **Context Budget Safety Margin:**
   - Reserve a `256-token` safety buffer below the provider's hard `max_context_length` to account for local-vs-remote tokenizer drift (Claude/DeepSeek/Llama tokenizers).
5. **Audio & TTS Hardware Lifecycle:**
   - Every `AudioTrack`, `MediaPlayer`, or `sherpa-onnx` instance MUST be registered inside a `DisposableEffect(conversationId)` that invokes `stop()` and `release()` on dispose or chat switch, and properly abandons audio focus so background music resumes.
6. **Atomic Settings Persistence:**
   - Every UI toggle or slider mutation (glass mode, blur radius, per-chat wallpaper) must write through to Room/DataStore inside a viewModelScope coroutine—never mutate ephemeral Compose `mutableStateOf` alone.

---

