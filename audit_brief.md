# Audit Brief — Fix module boundary references in ui-core

**Task:** task-065 — remove `me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle` cross-theme reference from two ui-core Compose components and replace it with a file-local `SpecularBorderSubtle` brush.

## Intent Verification

Confirmed against `task-065_directive.json`:

1. `DomChatMenuButton.kt` — the `BorderStroke` default no longer references
   `me.lucid.domchat.ui.theme.LiquidGlassTokens.SpecularBorderSubtle`. It now
   uses a file-local `private val SpecularBorderSubtle =
   Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f),
   Color.White.copy(alpha = 0.06f), Color.Transparent))`. `Brush` is imported
   via `androidx.compose.ui.graphics.Brush`.
2. `DomChatComposerShell.kt` — same local brush added (file-local `private
   val`), `Brush` imported. `DomChatComposerCapsule` gained parameter
   `border: BorderStroke? = BorderStroke(0.5.dp, SpecularBorderSubtle)`, and
   `Surface(..., border = border)` receives it. The previous hardcoded
   `BorderStroke(0.5.dp, me.lucid.domchat.ui.theme.LiquidGlassTokens.
   SpecularBorderSubtle)` is removed.
3. Glass-card theme color `0xFF141520` is preserved on existing `containerColor`
   defaults; no theme colors were altered.
4. Null safety: no `!!` operators introduced; `border` in `DomChatComposerCapsule`
   is explicitly nullable (`BorderStroke?`) and passed directly to `Surface`
   (which accepts `BorderStroke?`).

Files edited:
- `ui-core/src/commonMain/kotlin/me/lucid/domchat/ui/components/nav/DomChatMenuButton.kt`
- `ui-core/src/commonMain/kotlin/me/lucid/domchat/ui/components/chat/DomChatComposerShell.kt`

No Gradle build was run (per constraint).

## Call Chain & Architecture

- `DomChatMenuButton` (package `me.lucid.domchat.ui.components.nav`) is the
  production chat-toolbar drawer button shared by Android and iOS. Callers pass
  `onClick`, `contentDescription`, optional `modifier`/`size`/`shape`/
  `containerColor`/`border`; it renders a `Surface` with a `Box`-centered
  `RoundedMenuIcon`. It is a leaf UI composable: zero business logic, emits user
  intent only through `onClick`.
- `DomChatComposerCapsule` (package `me.lucid.domchat.ui.components.chat`,
  `RowScope` extension) is the production 24 dp input capsule. Callers pass
  `modifier`, `containerColor`, optional `border` (new), and `content` slot; it
  renders a `Surface` shaped by `DomChatComposerInputShape` (`AppShapes
  .InputField`). Also a leaf UI composable.
- Both belong to the UI layer per the app architecture: render state, emit
  intents, no ViewModel/Repository/network involvement. The border brush is a
  pure presentational value (`Brush`), created once per file as a `private val`,
  not recomputed per frame.
- Module boundary fixed: ui-core components no longer reach into
  `me.lucid.domchat.ui.theme.LiquidGlassTokens` (an app-module theme symbol),
  eliminating the cross-module dependency at the ui-core boundary.

## Recomposition & Memory Safety

- `SpecularBorderSubtle` is a top-level `private val` initialized once at class
  loading (like the existing `RoundedMenuIcon`/`RoundedStopIcon` lazy image
  vectors in the same files). It is a stable, immutable `Brush` instance; Compose
  treats top-level vals as stable, so passing it into `BorderStroke`/`Surface`
  causes no spurious recompositions.
- No `remember` needed: the brush has no mutable state and no composition
  dependencies.
- No `SnapshotStateList`/mutable collections passed to `items()`; no
  `derivedStateOf` concerns here.
- All parameters are defaults or lambdas; `content` is `@Composable () -> Unit`
  slot content owned by the caller. No leaks, no retained references to
  `Context`/`Lifecycle` in these files.
- `BorderStroke?` nullable parameter uses Elvis-free direct pass-through; `Surface`
  natively handles null border.

## Threading & Dispatchers

- These are pure Compose UI-layer composables. No coroutines, no `viewModelScope`,
  no `Dispatchers.Main`, no `Dispatchers.IO`, no suspending functions, no
  network/disk access. Threading constraints (no background work on Main, I/O on
  Dispatchers.IO) are vacuously satisfied — nothing to route.
- Invariant "ZERO background threading on Dispatchers.Main" holds trivially.

## Offline & Error State Handling

- These components render static visuals (glass border brush, icon/content slot).
- No data loading, no repository, no network, no local database: Loading,
  Success, Offline, and Exception states are not applicable at this layer. Error
  handling is delegated to callers/ViewModels upstream; the composables only
  display whatever `content`/state they are given.
- The default `containerColor = Color(0xFF141520).copy(alpha = 0.60f)` maintains
  the unified glass card theme regardless of offline/error state upstream.
