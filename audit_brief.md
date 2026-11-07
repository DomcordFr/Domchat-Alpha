# Audit Brief — Task 185: Fix Cyclic GraphicsLayer Render Recursion Crash

## Intent Verification

Fixed a cyclic render loop crash in `AssistantChatTheme` composable inside `ChatPage.kt`.

**Root cause**: At line 1528, `Box(modifier = Modifier.fillMaxSize().layerBackdrop(chatBackdrop))` wrapped both the `AssistantBackground` composable and its fallback `Box`. The `AssistantBackground` (line 1533) calls `.domChatBlurSource()` which internally calls `layerBackdrop(chatBackdrop)` — recording into `chatBackdrop.graphicsLayer`. Because the parent `Box` already had `layerBackdrop(chatBackdrop)` applied, the parent graphicsLayer and child graphicsLayer were the **same RenderTexture**. When `drawBackdrop` ran inside the parent Box, it sampled `chatBackdrop.graphicsLayer` — the same RenderTexture currently being drawn — causing an infinite HWUI recursion and `RenderThread SIGSEGV` stack overflow crash.

**Fix applied**: Changed line 1528 from:
```kotlin
Box(modifier = Modifier.fillMaxSize().layerBackdrop(chatBackdrop)) {
```
to:
```kotlin
Box(modifier = Modifier.fillMaxSize()) {
```

This removes the parent graphicsLayer that was sampling itself. The `AssistantBackground` at line 1533 still has `.domChatBlurSource()` (which calls `layerBackdrop(chatBackdrop)` internally), so the backdrop is still captured correctly — just without the recursive parent-child graphicsLayer dependency.

## Call Chain & Architecture

**Call chain (before fix)**:
1. `ChatPage.kt` → `Compose UI frame`
2. `Box(modifier = Modifier.fillMaxSize().layerBackdrop(chatBackdrop))` → registers parent `GraphicsLayer`
3. Inside this Box, `AssistantBackground(...).modifier = Modifier.domChatBlurSource()` → calls `layerBackdrop(chatBackdrop)` → calls `drawBackdrop()`
4. `drawBackdrop()` calls `drawLayer(backdrop.graphicsLayer)` — samples the SAME graphicsLayer being drawn in step 2
5. HWUI detects render recursion → `RenderThread SIGSEGV`

**After fix**:
1. `ChatPage.kt` → `Compose UI frame`
2. `Box(modifier = Modifier.fillMaxSize())` — NO graphicsLayer registered for parent
3. Inside Box, `AssistantBackground(...).modifier = Modifier.domChatBlurSource()` → calls `layerBackdrop(chatBackdrop)` → records into `chatBackdrop.graphicsLayer`
4. `drawBackdrop()` samples `chatBackdrop.graphicsLayer` — a SEPARATE graphicsLayer, no recursion

**Architecture boundary**: `DomChatMenuButton` in `ui-core` module remains unchanged — it correctly uses `containerColor` and `border` parameters without referencing `LiquidGlassMode` (previously fixed in commit `a4b04b9`). All UI composition boundaries are respected: `ChatPage` owns the `chatBackdrop` `LocalAppBackdrop`, child composables consume it via `LocalAppBackdrop.current`.

## Recomposition & Memory Safety

- **No new state introduced**: Only removed a `.layerBackdrop(chatBackdrop)` modifier call from the parent Box. No new state objects allocated.
- **`chatBackdrop` is a `rememberLayerBackdrop()`**: Creates a single `LayerBackdrop` instance per `ChatPage` composition. The `LayerBackdropNode` holds a reference to this single instance — no new allocations per frame.
- **`surfaceDirty` flag**: `LayerBackdropNode` uses `surfaceDirty` flag to track when re-recording is needed. The parent no longer redundantly calls `layerBackdrop`, so `recordLayer` is called once per frame instead of twice (parent + child).
- **No `!!` force-unwraps introduced**: grep-verified zero new `!!` matches.

## Threading & Dispatchers

- **Pure UI thread Compose code**: `ChartPage.kt` composable functions run on `Dispatchers.Main`. All Compose drawing happens on the main thread during the draw phase.
- **No coroutines introduced**: No `launch`, `withContext`, or `async` calls added.
- **No blocking calls**: The `layerBackdrop` call happens within Compose's draw phase, which is designed for synchronous GPU command recording.
- **Thread safety**: `LayerBackdropNode` uses `mutableStateOf(null)` for `layoutCoordinates` with `neverEqualPolicy()` — this is the standard Compose pattern for tracking layout coordinates safely.

## Offline & Error State Handling

- **Fully offline-capable**: Purely graphical composition changes. No network calls, no remote resource lookups.
- **No new error states introduced**: The fix removes a crash path (RenderThread SIGSEGV) rather than introducing new ones.
- **Graceful degradation**: If `chatBackdrop` is `null` (when `LocalAppBackdrop.current` is null, e.g. on API < 33), `drawBackdrop` handles it gracefully — `LayerBackdropNode.draw()` calls `backdrop.graphicsLayer` which would be null, but this is pre-existing behavior not affected by this change.
- **Memory safety**: No memory leaks introduced. `LayerBackdrop` is `rememberLayerBackdrop()` — created once per composition and garbage-collected when the composable leaves composition.

## Files Modified

| File | Change |
|------|--------|
| `ChatPage.kt` | Line 1528: Removed `.layerBackdrop(chatBackdrop)` from parent Box modifier, leaving `Modifier.fillMaxSize()` |

**Diff**: 1 file changed, 1 insertion(+), 1 deletion(-)

**Commit**: `0e83afc` fix(ui): break graphicsLayer render cycle in ChatPage.kt

**Status**: Pushed to `origin/main`