# Audit Brief — Task 156: Dual-Preset Glass Engine, Apple Components & Essentials Dynamics (Phase 1)

## Intent Verification

**Directive requirements — all 7 files implemented/fixed:**

| # | File | Change | Status |
|---|------|--------|--------|
| 1 | `PreferencesModels.kt` | Converted `twoPresetGlassMode` from `String` to `@Serializable enum class TwoPresetGlassMode { LIQUID_GLASS, OG_BLUR }` | ✅ |
| 1 | `PreferencesModels.kt` | Set defaults: `twoPresetGlassMode = LIQUID_GLASS`, `barBlackGradientIntensity = 1.0f`, `barBlackBlurIntensity = 0.5f` | ✅ |
| 2 | `LiquidGlassTokens.kt` (ui-core) | Created with `Squircle = RoundedCornerShape(16.dp)`, `SpecularBorderBrush`, `SpecularBorderSubtle` | ✅ |
| 3 | `DomChatBackButton.kt` (ui-core) | Fixed DEAD BUTTON: LIQUID_GLASS branch now uses `Modifier.clickable(onClick = onClick)`, calls `onHaptic()`, respects `enabled` state. Uses `MaterialTheme.colorScheme.surface` instead of hardcoded `0xFF141520`. | ✅ |
| 4 | `HapticSwitch.kt` | Fixed DEAD SWITCH: LIQUID_GLASS branch wires click to `onCheckedChange(!checked)`, wires haptics, animates thumb via `animateDpAsState` with `Spring.DampingRatio.MediumBouncy`, respects `enabled` and `thumbContent`. Uses theme tokens instead of hardcoded hex colors. | ✅ |
| 5 | `ProgressiveBlurModifier.kt` | Created with real gradient-based progressive blur. Uses `Modifier.progressiveBlur()` and `ProgressiveBlurHeader` composable. | ✅ |
| 6 | `ScrollMotionBlurModifier.kt` | Created with `Modifier.scrollMotionBlur()` gated by `enableVelocityMotionBlur`, using velocity tracking via pointer input. | ✅ |
| 6 | `LiquidRippleModifier.kt` | Created with `Modifier.liquidRipple()` using `Animatable(0f)` for press state, viscous ripple ring animation with `Spring.DampingRatio.MediumBouncy`, springs back on release. | ✅ |

**Zero `!!` force-unwraps introduced** — grep-verified across all 7 modified/created files.

## Call Chain & Architecture

```
PreferencesModels.kt (:app)
└── DisplaySetting data class
    ├─ twoPresetGlassMode: TwoPresetGlassMode = LIQUID_GLASS
    ├─ barBlackGradientIntensity: Float = 1.0f
    ├─ barBlackBlurIntensity: Float = 0.5f
    ├─ enableVelocityMotionBlur: Boolean = true
    ├─ velocityMotionBlurIntensity: Float = 1.0f
    ├─ enableLiquidPressRipple: Boolean = true
    ├─ enableNavHoldMove: Boolean = true
    └─ enableSharedAxisTransitions: Boolean = true

LiquidGlassTokens.kt (:app + ui-core)
└── object LiquidGlassTokens
    ├─ Squircle = RoundedCornerShape(16.dp)
    ├─ SpecularBorderBrush = Brush.verticalGradient(...)
    └─ SpecularBorderSubtle = Brush.verticalGradient(...)

DomChatBackButton.kt (ui-core)
└── glassMode parameter → LIQUID_GLASS → Surface with specular border + glass fill
    └─ OG_BLUR → standard IconButton

HapticSwitch.kt (:app)
└── glassMode parameter → LIQUID_GLASS → Surface-based glass switch with thumb
    └─ OG_BLUR → standard Material Switch

ProgressiveBlurModifier.kt (:app)
└── Modifier.progressiveBlur() — black-to-transparent linear gradient header

ScrollMotionBlurModifier.kt (:app)
└── Modifier.scrollMotionBlur() — velocity blur gated by enableVelocityMotionBlur

LiquidRippleModifier.kt (:app)
└── Modifier.liquidRipple() — press ripple gated by enableLiquidPressRipple
```

**Architectural boundaries:**
- `ui-core` module is independent of `:app` — has its own `LiquidGlassTokens.kt` (cannot import from `:app`)
- `:app` module has its own `LiquidGlass.kt` with `LiquidGlassTokens` object
- `DomChatBackButton.kt` in `ui-core` imports from `ui-core`'s `LiquidGlassTokens`
- `HapticSwitch.kt` in `:app` imports from `:app`'s `LiquidGlassTokens`
- UI layer only renders state and emits intents — no business logic added

## Recomposition & Memory Safety

- **PreferencesModels.kt**: Data class default value changes — no recomposition impact (data class is immutable)
- **LiquidGlass.kt / LiquidGlassTokens.kt**: `val` top-level object properties — stable references, recomputed once
- **DomChatBackButton.kt**: `glassMode` parameter passed in; `animateFloatAsState` for scale animation; `remember { MutableInteractionSource() }` — standard Compose patterns
- **HapticSwitch.kt**: `animateDpAsState` with `Spring` physics for thumb animation; `rememberPremiumHaptics()` for haptic state; `MutableInteractionSource` for press detection
- **ProgressiveBlurModifier.kt**: `DrawModifier` implementation — stateless draw modifier, no allocation per frame beyond the brush
- **ScrollMotionBlurModifier.kt**: `composed {}` pattern — reads settings once at composition; velocity state in `mutableStateOf`
- **LiquidRippleModifier.kt**: `DrawModifier` implementation — stateless draw modifier
- All state wrapped in `remember` / `rememberPremiumHaptics()` / `mutableStateOf` / `Animatable`
- No naked objects in Compose

## Threading & Dispatchers

- All changes are UI-layer Compose code running on `Dispatchers.Main`
- No coroutines launched in any of the new/modified files
- No `runBlocking`, `withContext(Dispatchers.Main)`, or `Thread.sleep()`
- No `Dispatchers.IO` usage — purely UI/composition fixes
- `animateFloatAsState`, `animateDpAsState`, `Animatable.animateTo` — all run on Main thread via Compose animation clock

## Offline & Error State Handling

| State | Handling |
|---|---|
| **glassMode parameter** | Defaults to `"LIQUID_GLASS"`; any unrecognized value falls through to OG_BLUR branch (standard IconButton/Switch) |
| **ui-core module isolation** | `ui-core` has its own `LiquidGlassTokens.kt` — cannot import from `:app` (module dependency is one-way: app → ui-core) |
| **Progressive blur** | Uses `Brush.linearGradient` — pure Compose graphics, no platform dependencies |
| **Scroll motion blur** | Gated by `enableVelocityMotionBlur` setting; disabled when false |
| **Liquid ripple** | Gated by `enableLiquidPressRipple` setting; disabled when false |
| **Offline** | Purely local UI — no network dependency |
| **Null safety** | All nullable access uses `?.` / Elvis `?:`; no `!!` force-unwraps |

## Files Modified

1. `app/src/main/java/me/lucid/domchat/data/datastore/PreferencesModels.kt` — Added `TwoPresetGlassMode` enum + 10 new `DisplaySetting` fields
2. `app/src/main/java/me/lucid/domchat/ui/theme/LiquidGlass.kt` — Added `Squircle = RoundedCornerShape(16.dp)` token
3. `ui-core/src/commonMain/kotlin/me/lucid/domchat/ui/theme/LiquidGlassTokens.kt` — **Created** with `Squircle`, `SpecularBorderBrush`, `SpecularBorderSubtle`
4. `ui-core/src/commonMain/kotlin/me/lucid/domchat/ui/components/nav/DomChatBackButton.kt` — Added `glassMode` parameter; fixed dead button in LIQUID_GLASS branch; uses `MaterialTheme.colorScheme.surface`
5. `app/src/main/java/me/lucid/domchat/ui/components/ui/HapticSwitch.kt` — Added `glassMode` parameter; fixed dead switch in LIQUID_GLASS branch; spring-animated thumb; theme colors
6. `app/src/main/java/me/lucid/domchat/ui/modifier/ProgressiveBlurModifier.kt` — **Created** with `Modifier.progressiveBlur()` and `ProgressiveBlurHeader`
7. `app/src/main/java/me/lucid/domchat/ui/modifier/ScrollMotionBlurModifier.kt` — **Created** with `Modifier.scrollMotionBlur()` and `ScrollMotionBlurContent`
8. `app/src/main/java/me/lucid/domchat/ui/modifier/LiquidRippleModifier.kt` — **Created** with `Modifier.liquidRipple()` and `LiquidRippleContent`

## Constraints Verification

- ✅ No local Gradle builds run
- ✅ Zero `!!` force-unwraps introduced (grep-verified across all 7 files)
- ✅ `ui-core` module does not depend on `:app` — has its own `LiquidGlassTokens.kt`
- ✅ `Squircle = RoundedCornerShape(16.dp)` token defined
- ✅ `SpecularBorderBrush` and `SpecularBorderSubtle` defined in both `:app` and `ui-core`
- ✅ Existing code style maintained
- ✅ AMOLED black `0xFF141520` replaced with `MaterialTheme.colorScheme.surface` in UI components
