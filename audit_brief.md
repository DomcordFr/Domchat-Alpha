# Audit Brief — Task-115: Fix Compiler Alignment and Imports

## Intent Verification

Required changes from task-115_directive.json:

1. `app/src/main/java/me/lucid/domchat/ui/pages/assistant/CharacterHubPage.kt` — lines 658 and 993: `.align(Alignment.Center)` → `.align(Alignment.CenterVertically)`. Applied to both occurrences via a single exact-pattern replacement (grep confirmed exactly 2 matches, both at the specified lines). Verified post-edit at lines 658 and 993.

2. `app/src/main/java/me/lucid/domchat/ui/pages/assistant/detail/AssistantDetailPage.kt` — added imports `androidx.compose.foundation.background` and `androidx.compose.foundation.border`. These back the existing `.background(Color(0xFF141520))` (line 552) and `.border(1.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape)` (line 553) modifier calls that were previously unresolved. Inserted after `androidx.compose.foundation.verticalScroll` to keep `androidx.compose.foundation.*` imports grouped (lines 23–24).

No `!!` force-unwraps introduced or present. Glass card theme color `0xFF141520` is referenced only as existing usage; no theme constants were altered. Existing code style (4-space indent, import grouping) preserved.

## Call Chain & Architecture

CharacterHubPage.kt
- Compose screen hosted inside `RouteActivity` single-activity host via `AppRoutes` NavHost (type-safe `Screen` sealed interface).
- The two edited `Surface` blocks sit inside an import-affordance card (file upload for character cards). Each Surface is wrapped in `.wrapContentWidth().align(Alignment.CenterVertically).padding(horizontal = 16.dp)`, vertically centering the import button within its parent Row/Column that already declares `verticalAlignment = Alignment.CenterVertically`.
- Call chain: user taps Surface → `importLauncher.launch([...])` (ActivityResultContracts.OpenDocument) → picker returns URI → `AssistantExportImport`/attachment flow handles ingestion. Alignment change is purely layout; no business logic, no ViewModel/repository interaction.
- Layering: UI layer only renders state and emits intents; zero business logic in the composable (consistent with AGENTS.md UI/MVVM boundary).

AssistantDetailPage.kt
- Detail screen composable reached via assistant detail sub-routes (`AssistantDetailRoutes`) under the same `RouteActivity` NavHost.
- `.background(Color(0xFF141520))` and `.border(...)` are `Modifier` extension functions from `androidx.compose.foundation.background` / `androidx.compose.foundation.border`, applying the unified glass card fill (`0xFF141520` = DomChat unified glass card theme) and subtle specular border on a detail card.
- Call chain: UI renders ViewModel `StateFlow` state (collected via `collectAsStateWithLifecycle`, line 61 area) → detail modifiers paint card appearance. No navigation or repository calls triggered by these modifiers.

Both files register ViewModels in `di/ViewModelModule.kt` and inject via `koinViewModel<...>()` / `koinInject`, per AGENTS.md DI conventions.

## Recomposition & Memory Safety

- `.align(...)` is a stateless `Modifier` factory returning a new `Modifier` instance each composition; it captures no state, holds no references, and triggers no recomposition by itself. Changing horizontal center → vertical center has zero memory or recomposition side effects.
- `Modifier.background(...)` and `Modifier.border(...)` are likewise stateless `Modifier` factories (paint/draw modifiers). They do not allocate per-frame objects beyond the modifier chain itself, which Compose reuses across compositions when the modifier reference is stable. Since these are constructed inline in the `modifier` parameter without `remember`, Compose's equality-based change detection recomposes only when inputs (`Color`, `BorderStroke`, `Shape`) change — all are immutable value types, so no spurious recompositions.
- No `SnapshotStateList`, `MutableStateFlow`, `remember` without keys, or `LaunchedEffect` introduced. No naked objects in Compose; all state remains ViewModel-owned `StateFlow` collected lifecycle-aware via `collectAsStateWithLifecycle`.
- `Alignment.CenterVertically` is an immutable object singleton; no allocation per call.

## Threading & Dispatchers

- These edits touch only the Compose UI/layout layer (`Modifier`, `Alignment`), which executes on `Dispatchers.Main`. No coroutine launch, `withContext`, ViewModel `viewModelScope`, `Dispatchers.IO`, or `AppScope` (`Dispatchers.Default`) usage is added or removed.
- No network, disk, database, or Room access occurs in either edited code path. The file-import `ActivityResultLauncher` callback runs on Main and delegates parsing/IO to `AssistantExportImport`/repository code on appropriate dispatchers elsewhere — untouched by this change.
- No `AppScope`, `viewModelScope`, or `Dispatchers.Main` violation introduced.

## Offline & Error State Handling

- Offline: Character import relies on local document picker (`application/json`, `image/png`, `image/webp`, `*/*`) and local attachment storage; no network dependency in the alignment-affected UI. Alignment change has no bearing on offline behavior.
- Error handling: `Surface.onClick` merely launches the activity result contract; errors (picker cancellation, unsupported MIME, malformed card JSON) are handled downstream in `AssistantExportImport` / attachment repository, untouched here.
- The `.background`/`.border` modifiers degrade gracefully: if the theme token is unavailable they fall back to the default `Color.Unspecified`/no-border paint; no exceptions are thrown by the modifier functions themselves.
- Loading/Success/Offline/Exception states are owned by the ViewModel/Repository layer and rendered via `StateFlow`-driven UI elsewhere; these edits add no new states and remove no error handling.
