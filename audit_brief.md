# Audit Brief — Cinematic Apple-Spring Entrance Animation (task-023)

## Intent Verification

All three directive requirements are implemented in the target files:

1. **SplashReveal.kt** — `overlayAlpha.animateTo(0f)` is a suspending call that completes before `visible = false` is assigned, so the splash overlay is never unmounted mid-fade. Fade duration calibrated to **350ms** (`SPLASH_FADE_MS` changed 400 → 350). The logo exit drift shares the same 350ms spec, so logo and overlay settle together.
2. **Entrance.kt** — The sluggish `tween(2000ms, FastOutSlowInEasing)` is replaced with an organic Apple physics spring (`dampingRatio = 0.72f`, `stiffness = 320f`) applied via `animateTo(...)` with per-element stagger delay preserved. A scale pop (`0.93f → 1.0f`, derived as `0.93f + 0.07f * progress`) is composited with alpha in the same `graphicsLayer`, all transform/opacity — no layout or draw invalidation. Unused `FastOutSlowInEasing`/`tween` imports and the now-dead `ENTRANCE_DURATION_MS = 2000` constant are removed.
3. **ChatPage.kt** — `startupWallpaperPauseMs` calibrated 700 → **350ms**. This single constant gates the wallpaper pause (`delay` before `chatAnimationsEnabled = true`) and the stagger offsets for top bar (`+0`), bottom bar input (`+2 * step`), temp-chat overlay (`+step`), and new-chat content (`+step`), so the user sees the background before chrome springs in.

Strict null safety: no `!!` introduced or removed; all existing nullable handling (`?:`, safe calls, Elvis) preserved. Glass card theme color `0xFF141520` is untouched (these files do not set card colors; they use `MaterialTheme.colorScheme` / `Compose` layer alpha). Existing code style (4-space kt, KDoc, const placement) preserved.

## Call Chain & Architecture

**Splash path:** `RouteActivity` → `SplashRevealOverlay()` (Compose) → `LaunchedEffect(Unit)` runs once per process (`SplashShown.once` guard) → logo alpha/scale springs → `delay(SPLASH_HOLD_MS)` → `overlayAlpha.animateTo(0f)` (suspends 350ms) → `visible = false` → early-return `if (!visible) return` unmounts the overlay. No ViewModel/Repository involvement; purely presentational startup overlay.

**Entrance path:** `ChatPage` → `ChatPageContent` → `StartupChromeReveal(key, delayMs, ...)` / `Modifier.entrance(key, ...)` applied to top bar, bottom input, temp-chat overlay, new-chat content. Each `entrance(...)` calls `EntrancePlayed.markIfNew(key)` (process-lifetime `HashSet<String>`, capped 4096) to guarantee one-shot per conversation open. Inside, `LaunchedEffect(key)` drives `progress.animateTo(1f, spring(0.72f, 320f), delayMillis=stagger)`; `graphicsLayer` reads `progress.value` each frame for `alpha`, `scaleX/Y`, and `translation`. No business logic in UI layer; `ChatVM` owns conversation state via `collectAsStateWithLifecycle()`.

**ChatPage timing:** `startupWallpaperPauseMs` (350ms) → `delay(...)` → `chatAnimationsEnabled = true` → provided via `LocalChatAnimationsEnabled` to `ChatList`, gating per-message entrance animations. Top bar reveal starts at `delayMs = startupWallpaperPauseMs`; input at `startupWallpaperPauseMs + 2*step`; overlays at `+step`. `startupStaggerStepMs` remains 120ms.

## Recomposition & Memory Safety

- **No `!!`**: All edits preserve null safety. `entrance(...)` early-returns `this` when `!allow`, `reduceMotion`, or `markIfNew` returns false — no nullable dereference.
- **Remember scoping**: `progress = remember(key) { Animatable(0f) }` ties the Animatable to the element key; `LaunchedEffect(key)` cancels/restarts only when the key changes. `EntrancePlayed` set is process-lifetime but bounded (clear at 4096 entries) — no unbounded growth.
- **Graphics layer only**: `alpha`, `scaleX/Y`, `translationX/Y` are transform/opacity properties; no `Modifier.size`/`padding` changes during animation, so no layout pass or measurement invalidation per frame. The optional blur path (`blurPx > 0`) reads `progress.value` in composition and only recomposes during the entrance; callers opt in.
- **Splash**: `visible` is `remember { mutableStateOf(true) }`; once `false`, the composable returns early and skips the `Box`/`Image` subtree entirely — zero recomposition cost after unmount. `Animatable` instances are `remember`-scoped and disposed with the composition.
- **Wallpaper pause**: `chatAnimationsEnabled` is `remember(conversation.id) { mutableStateOf(false) }`, set once after the delay — single write, stable thereafter.

## Threading & Dispatchers

All animation work runs on `Dispatcher.Main` via Compose `LaunchedEffect`/`Animatable.animateTo` — correct for UI animation; no `withContext(Dispatchers.IO)` or `Dispatchers.Default` introduced in these files. No blocking calls. The `delay(...)` in SplashReveal and ChatPage are suspending, non-blocking, and Main-dispatcher-safe.

`EntrancePlayed.markIfNew` mutates a mutable `HashSet` from the Main thread (called during composition side-effect within `entrance()` modifier). This matches existing code and is single-threaded (Compose Main dispatcher); no concurrent access introduced by this change.

No new coroutines, scopes, or dispatchers added. Existing `scope.launch` calls elsewhere in ChatPage.kt (file import deletion, scroll) are unchanged and remain on `rememberCoroutineScope()` (Main).

## Offline & Error State Handling

- **SplashReveal**: Fully local, asset-backed (`painterResource(R.drawable.domchat_setup_icon)`). No network, no disk, no error states possible. `reduceMotion` or `SplashShown.once` short-circuits to `visible = false` immediately — graceful degrade.
- **Entrance**: Purely local UI animation. `reduceMotion` short-circuits to no animation (`return this`), elements render at rest state instantly. Offline/online status irrelevant. `markIfNew` returning false (already-played) skips animation gracefully — element shows at rest.
- **ChatPage wallpaper pause**: Purely local timing. No network dependency. If conversation initialization fails, `vm.errorFlow` is collected at the top of `ChatPage` and surfaced via `LocalToaster.current` (`ToastType.Error`) — existing behavior, untouched. `chatAnimationsEnabled` gating only affects visual entrance; chat list and input remain functional regardless.
- No `try/catch` needed: animation APIs do not throw for offline/error states. All `collectAsStateWithLifecycle` flows handle emission lifecycle safely.

---
Changes are source-only. No Gradle build executed (per constraint). Verified by reading target files before/after edit; no `!!`, no `FastOutSlowInEasing`/`tween`/`ENTRANCE_DURATION_MS` remnants in Entrance.kt, `SPLASH_FADE_MS = 350`, `startupWallpaperPauseMs = 350`.
