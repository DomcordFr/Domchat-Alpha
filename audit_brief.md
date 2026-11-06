# Audit Brief — Task 182: Fix Backdrop Compiler Errors & Restore Convx Coordinate Transforms

## Intent Verification

Fixed compilation errors and restored Convx coordinate mapping across 4 backdrop files:

1. **Paint.kt** — Deleted invalid import `import androidx.compose.ui.graphics.asFrameworkPaint`. Replaced `this.asFrameworkPaint()` with `this.toFrameworkPaint()` in both `blur()` and `setRuntimeShader()` functions. The `asFrameworkPaint` extension does not exist in Compose Multiplatform 1.10.3; `toFrameworkPaint()` is the correct conversion API.

2. **RenderEffect.kt** — Deleted invalid import `import androidx.compose.ui.graphics.asAndroidRenderEffect`. Replaced `.asAndroidRenderEffect()` with `.toAndroidRenderEffect()` in all 3 usages (`chain()` at lines 24-25, `colorFilterEffect()` at line 51). Kept valid imports `asAndroidColorFilter` and `asComposeRenderEffect` which are used elsewhere in the file.

3. **DrawBackdropModifier.kt** — Wrapped `backdrop.drawBackdrop(...)` call inside `recordBackdropBlock` with `with(backdrop) { ... }` at line ~323, making the backdrop the receiver so `drawBackdrop` is called as a member function.

4. **LayerBackdrop.kt** — Added `DrawScope.withTransform()` helper that encapsulates the Convx coordinate mapping (scale by `1/sourceScale` around origin, then translate by `sourceOrigin - offset`). Replaced the manual `if/else` block with `scale`+`translate` calls with a single `withTransform(offset, origin, sourceScale) { drawLayer(graphicsLayer) }` call, preserving exact coordinate mapping behavior.

## Call Chain & Architecture

The backdrop rendering pipeline:
1. `DrawBackdropModifier` creates `DrawBackdropElement` → `DrawBackdropNode`
2. `DrawBackdropNode.draw()` → `drawBackdropLayer()` → `recordLayer()` → `recordBackdropBlock`
3. `recordBackdropBlock` calls `onDrawBackdrop { with(backdrop) { drawBackdrop(...) } }`
4. `LayerBackdrop.drawBackdrop()` computes offset/origin, then calls `withTransform { drawLayer(graphicsLayer) }`
5. `withTransform` applies Convx coordinate mapping (scale + translate) before drawing the layer

No architectural changes; all fixes are local to the backdrop rendering module. The `Backdrop` interface and `LayerBackdrop` class remain unchanged in their public API.

## Recomposition & Memory Safety

- No new state introduced; all existing `mutableStateOf`, `remember`, and snapshot reads unchanged.
- `withTransform` is a stateless `DrawScope` extension — no allocations, no remembered objects.
- `toFrameworkPaint()` and `toAndroidRenderEffect()` are pure conversion functions with no side effects.
- No `!!` force-unwraps introduced (grep-verified: only pre-existing `!!` in Outline.kt:18 and ShapeProvider.kt:48 remain, both untouched).
- Diff: 33 insertions / 22 deletions across 4 files — minimal, within scope.

## Threading & Dispatchers

- Pure UI/Compose drawing code executing on `Dispatchers.Main` during the draw phase.
- No coroutines, no `launch`, no `withContext`, no disk or network access.
- `withTransform`, `toFrameworkPaint()`, `toAndroidRenderEffect()` are synchronous, non-blocking operations.
- Invariant "ZERO background threading on Dispatchers.Main" satisfied.

## Offline & Error State Handling

- Fully offline-capable: purely graphical composition changes. No network calls, no resource lookups.
- No new error states introduced. `withTransform` preserves the existing try/catch-free logic (the original `try/catch` around `localPositionOf` remains intact).
- If `sourceScale == 1f`, `withTransform` skips scaling and only translates — same as original behavior.
- Error matrices unchanged from pre-existing behavior.
