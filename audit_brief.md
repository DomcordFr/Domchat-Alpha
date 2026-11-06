# Audit Brief — Task 184: Decouple Frosted Glass and Liquid Glass into Standalone Pipelines

## Intent Verification

Decoupled Convx-style liquid glass and frosted glass into two distinct, standalone modifier pipelines across 6 files:

1. **LiquidGlass.kt** — Added `Modifier.frostedGlassSurface()` as a standalone modifier separate from `Modifier.liquidGlassSurface()`:
   - `frostedGlassSurface`: Pure Haze blur (`domChatBlurEffect`) + alpha + tone + border. No AGSL backdrop refraction or lens effects.
   - `liquidGlassSurface`: Pure Convx AGSL backdrop refraction + lens + specular rim + depth (existing, unchanged).
   - Both share the same parameter shape (shape, borderWidth, specularBrush, fillColor, shadowElevation) for API consistency.

2. **ChatMessageV2.kt** — Routed `InterfaceStyle.FROSTED` → `frostedGlassSurface()` and `InterfaceStyle.GLASS` → `liquidGlassSurface()` in all 3 bubble rendering sites (user message turn, assistant message turn, assistant thinking dropdown). Removed `domChatFrostedContainerColor` import (no longer needed).

3. **ChatDrawer.kt** — Modal drawer sheet and Permanent pane now conditionally render `frostedGlassSurface(shape = drawerShape)` when `liquidGlassMode == SOFT_FROSTED`, otherwise `liquidGlassSurface(shape = drawerShape, glassMode = ...)`.

4. **MinimalChatInput.kt** — Plus button, composer capsule, STT overlay surface, think surface, think summary box, picker tabs, and scroll-to-bottom button all conditionally route to `frostedGlassSurface` (SOFT_FROSTED) or `liquidGlassSurface` (BALANCED_BLEND/FULL_WATER_LENS).

5. **ChatPage.kt** — Toolbar icon button, search mode bar, share mode bar, overflow menu, action pill, update pill, menu button, and context meter button all conditionally route based on `liquidGlassMode`. Removed unused `domChatBlurEffect`/`domChatBlurSource` imports.

6. **SettingDisplayPage.kt** — Cleanly decoupled Liquid vs Frosted settings:
   - UI Appearance card: Liquid shows Depth/Specular/Tint/BarDensity/BarBlur; Frosted shows Opacity/BlurRadius/Tint/BarDensity/BarBlur
   - Chat Bubbles Appearance card: Liquid shows Depth/Specular/Tint; Frosted shows Opacity/BlurRadius/Tone/Tint
   - Tint color, Bar Gradient Density, and Bar Gradient Blur are shared between both modes

## Call Chain & Architecture

- `frostedGlassSurface` and `liquidGlassSurface` are both `Modifier` extension functions in `LiquidGlass.kt`, called from UI composables.
- `frostedGlassSurface` reads `LocalSettings.current` for `chatBubbleAlpha`, `chatBubbleTone`, `chatBubbleBlur` — uses Haze `domChatBlurEffect` for blur.
- `liquidGlassSurface` reads `LocalSettings.current` for `glassBlur`, `glassDepthScale`, `glassTintColor`, `glassSpecularIntensity`, `liquidGlassMode` — uses `Modifier.drawBackdrop` with AGSL lens shader via `LocalAppBackdrop`.
- `SettingDisplayPage.kt` maintains two independent `remember` states (`uiIsGlass` from `liquidGlassMode`, `bubbleIsGlass` from `bubbleInterfaceStyle`) with a `linkUiAndBubbleStyle` toggle that syncs them when enabled.
- Architectural boundary preserved: UI layer only; no ViewModel/repository/business-logic changes.

## Recomposition & Memory Safety

- `frostedGlassSurface` and `liquidGlassSurface` are stateless modifiers — no allocations beyond existing Compose pattern.
- All `remember` blocks in SettingDisplayPage.kt are keyed on their respective `Settings.displaySetting` values — stable across recompositions.
- No new remembered objects introduced beyond existing patterns.
- No `!!` force-unwraps introduced (grep-verified: zero matches across all 6 edited files).
- Diff: 226 insertions / 54 deletions across 6 files — minimal, within scope.

## Threading & Dispatchers

- Pure UI/Compose drawing code on `Dispatchers.Main`; no coroutines, no `launch`, no `withContext`, no disk/network access.
- `domChatBlurEffect` (Haze) and `drawBackdrop` (AGSL) are frame-based composition modifiers running during the draw phase.
- Invariant "ZERO background threading on Dispatchers.Main" satisfied.

## Offline & Error State Handling

- Fully offline-capable: purely graphical composition changes. No network calls, no remote resource lookups.
- `frostedGlassSurface` gracefully degrades when `glassBlur <= 0f` — falls back to solid `background()` color without blur.
- `liquidGlassSurface` gracefully falls back to `domChatBlurEffect` when `LocalAppBackdrop.current == null` or runtime shaders unsupported (API < 33) — no crash path.
- `frostedGlassSurface` gracefully degrades when `effectiveBlur <= 0f` — falls back to solid `background()` color.
- Error matrices unchanged from pre-existing behavior.
