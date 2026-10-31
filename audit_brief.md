# Audit Brief — Task-017 (BorderStroke / @OptIn / Janitor API)

## Intent Verification
- **MinimalChatInput.kt:860** — `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape)` → `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)`. Verified: `androidx.compose.foundation.BorderStroke` ctor takes `(width: Dp, brush: Brush)` only; 3-arg form does not compile. The 2-arg form matches existing usages at lines 2289, 2358, 2519, 3490, 3526, 3569.
- **CharacterHubPage.kt:1017-1018** — Two separate `@OptIn` annotations on `CharacterInspectorSheet` merged into one non-repeatable `@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)`. Verified: both imports present (lines 11, 52). `@OptIn` is not repeatable in Kotlin; separate annotations caused compiler failure.
- **SettingJanitorPage.kt** — **BLOCKER: file does not exist.** Repo-wide grep for `Janitor` returns zero matches except an unrelated comment at `Assistant.kt:332`. No `JanitorListResponse`, `JanitorExtractResponse`, `JanitorCharacter`, or `extractToAssistant()` anywhere in the codebase. Cannot edit a file that is absent; will not fabricate a settings page + repository + network contract blindly (violates surgical scope + no-hallucination).
- **Assistant model mismatch** (relevant to blocked item 3): `Assistant` has `systemPrompt: String = ""`, `background: String? = null` (no `scenario` field), `alternateGreetings: List<String>`, `tags: List<Uuid>`. `firstMessage` is a **computed** `val` derived from `presetMessages` (Assistant.kt:134-137), not settable. Mapping `scenario`/`firstMessage` into `Assistant` requires model changes first (`scenario` field or `background` reuse; `presetMessages` population for `firstMessage`).

## Call Chain & Architecture
- **MinimalChatInput** (`ui/components/ai/`): Compose input row inside `DomChatComposerCapsule`. `border` param is read by the capsule's border-drawing logic; no ViewModel/repository involvement. Pure UI edit, zero call-chain impact.
- **CharacterHubPage** (`ui/pages/assistant/`): `CharacterInspectorSheet` is a bottom-sheet composable invoked from the hub gallery (`CharacterHubPage.kt:511`). `@OptIn` is an annotation-only marker; no runtime call-chain. Edit is annotation-surface only.
- **SettingJanitorPage** (absent): would have sat in `ui/pages/setting/`, wired via `SettingsDestination` + `AppRoutes`, owned by a `ViewModel` exposing `StateFlow`, repository for network, `SecretKeyManager` for any keys. Cannot verify without the file.

## Recomposition & Memory Safety
- `BorderStroke` edit: value object, immutable, recreated per recomposition; no allocations in hot path beyond existing. No `remember`/`derivedStateOf` involved. Memory neutral.
- `@OptIn` merge: annotation only; zero runtime, zero recomposition, zero memory impact.
- No `!!` introduced in edited regions (grep clean for `CharacterHubPage`; `MinimalChatInput` `!!` at 3032/3034 are pre-existing camera code, out of scope).
- Glass card theme color `0xFF141520` preserved (used in `CharacterHubErrorCard` background at line 976; untouched).

## Threading & Dispatchers
- Both edits are pure Compose UI/Material annotation changes. No coroutines, no dispatchers, no `Dispatchers.Main`/`IO` involved. No threading risk.
- If `SettingJanitorPage` were created later, network/disk must route to `Dispatchers.IO`; ViewModel must use `viewModelScope`; never suspend on `Main`.

## Offline & Error State Handling
- `BorderStroke`/`@OptIn` edits: no error branches, no network, no offline handling applicable.
- `CharacterHubErrorCard` (CharacterHubPage.kt:967) already renders an error fallback card with red border (`0xFFEF4444` alpha 0.6) on dark glass `0xFF141520` — existing error-state UI preserved.
- Blocked item 3 would need explicit Loading/Success/Offline/Exception matrix per repo invariants; cannot assess without the file.

## Proof Receipt (exact line changes)
- `MinimalChatInput.kt:860`: `border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape),` → `border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle),`
- `CharacterHubPage.kt:1017-1018`: two `@OptIn` lines → single `@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)`
- `SettingJanitorPage.kt`: **not edited — file absent.**

No local Gradle build run (constraint). Compiler verification limited to static inspection of constructor signatures and imports above.