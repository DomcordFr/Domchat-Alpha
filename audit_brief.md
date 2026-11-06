# Audit Brief — Task 183: Wire Liquid Glass Across Side Menus, Popovers, Action Buttons, Hoist Backdrop

## Intent Verification

Wired Convx-style liquid glass optics across chat side menus, popovers, action buttons, and hoisted the chat backdrop:

**ChatPage.kt**
- Hoisted `chatBackdrop = rememberLayerBackdrop()` at the top of `ChatPage` (after `drawerState`).
- Wrapped `ModalNavigationDrawer` with `CompositionLocalProvider(LocalAppBackdrop provides chatBackdrop)` so drawer, popovers, and dialogs can sample the backdrop.
- Removed the now-redundant local `rememberLayerBackdrop()` inside `ChatPageContent`; `chatBackdrop` is passed as a parameter to both `ChatPageContent` call sites (wide layout + modal layout).

**ChatDrawer.kt**
- Added `import me.lucid.domchat.ui.theme.liquidGlassSurface`.
- `DomChatModalDrawerSheet`: replaced `.domChatBlurEffect(containerColor = drawerBg, shape = drawerShape)` with `.liquidGlassSurface(shape = drawerShape)`.
- `PermanentPane` Surface: replaced `.domChatBlurEffect(containerColor = drawerBg, shape = RoundedCornerShape(32.dp))` with `.liquidGlassSurface(shape = drawerShape)`; shape unified to `drawerShape`.

**DomChatDrawerQuickAction.kt** (ui-core KMP)
- Replaced `Surface` with `Box` + `liquidGlassSurface(shape = cardShape, fillColor = cardBg, borderWidth = 0.5.dp, specularBrush = ...)` + `.clickable(...)` for haptics/click. Removed unused `Surface` import; added `clickable` and `LiquidGlassTokens` imports.

**DomChatDrawerAction.kt** (ui-core KMP)
- Replaced `Surface` with `Box` + `liquidGlassSurface(shape = shape, fillColor = containerColor, borderWidth = 0.5.dp, specularBrush = SpecularBorderSubtle)` + `.clickable(...)`. Removed `Surface`, `MaterialTheme` imports; added `clickable`, `LiquidGlassTokens` imports.

**MinimalChatInput.kt**
- Tool approval icon Surface → `Box` + `liquidGlassSurface(shape = CircleShape, fillColor = ..., borderWidth = 0.5.dp, specularBrush = SpecularBorderSubtle)`.
- Tool approval summary Surface → `Box` + `liquidGlassSurface(shape = RoundedCornerShape(18.dp), fillColor = ..., borderWidth = 1.dp, specularBrush = Brush.solid(outlineVariant))`.
- Minimal picker tabs Surface → `Box` + `liquidGlassSurface(shape = RoundedCornerShape(16.dp), fillColor = ..., borderWidth = 0.5.dp, specularBrush = Brush.solid(Color.White.copy(alpha = 0.08f)))`.
- ChatScrollToBottomButton Surface → `Box` + `liquidGlassSurface(shape = CircleShape, fillColor = blurredContainerColor(...), borderWidth = 1.dp, specularBrush = Brush.solid(outlineVariant))`.
- Added `import androidx.compose.ui.graphics.Brush`.

**LiquidGlass.kt**
- Default `blurRadius` for `Modifier.liquidGlass` changed from `20.dp` → `8.dp` (crisp Convx range 4dp-8dp; refraction physically visible).

## Call Chain & Architecture

- `chatBackdrop` (`rememberLayerBackdrop()`) is hoisted in `ChatPage` and provided via `CompositionLocalProvider(LocalAppBackdrop provides chatBackdrop)` wrapping `ModalNavigationDrawer`. All drawer/popover/dialog descendants can read `LocalAppBackdrop.current` to sample the chat backdrop through `Modifier.drawBackdrop(...)`.
- `liquidGlassSurface` maps `DisplaySetting` glass optics (blur, depth scale, specular intensity, tint) onto `Modifier.liquidGlass`, which uses `LocalAppBackdrop` + AGSL lens shader when available (API 33+, runtime shader support), falling back to `domChatBlurEffect` (Haze ultraThin) otherwise.
- Drawer actions and quick actions remain stateless composables emitting intents via `onClick`/`onHaptic` callbacks.
- Architectural boundary preserved: UI layer only; no ViewModel/repository/business-logic changes.

## Recomposition & Memory Safety

- `chatBackdrop` created once via `rememberLayerBackdrop()` — stable across recompositions.
- `liquidGlassSurface` reads `LocalSettings.current` (composition local) — recomputes correctly when settings change.
- `remember { MutableInteractionSource() }` in `DomChatDrawerAction` preserved — lifecycle-aware.
- No new remembered objects introduced beyond existing patterns.
- No `!!` force-unwraps introduced (grep-verified: only pre-existing ones in ChatPage.kt:2158/2176 and MinimalChatInput.kt:3673/3675 remain).
- Diff: 83 insertions / 64 deletions across 6 files — minimal, within scope.

## Threading & Dispatchers

- Pure UI/Compose drawing code on `Dispatchers.Main`; no coroutines, no `launch`, no `withContext`, no disk/network access.
- `liquidGlass` / `liquidGlassSurface` are frame-based composition modifiers; backdrop sampling happens during the draw phase.
- Invariant "ZERO background threading on Dispatchers.Main" satisfied.

## Offline & Error State Handling

- Fully offline-capable: purely graphical composition changes. No network calls.
- `liquidGlass` gracefully falls back to `domChatBlurEffect` when `LocalAppBackdrop.current == null` or runtime shaders unsupported (API < 33) — no crash path.
- `Brush.solid(...)` used for specular border when a solid color is needed; all color args are valid `Color` values.
- Error matrices unchanged from pre-existing behavior.
