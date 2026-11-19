# DomChat Android Kotlin Compose: Comprehensive Design Consistency Audit Report

**Audit Target:** `/storage/emulated/0/Opencode/domchat-clone`  
**Scope:** Settings pages (`SettingProviderDetailPage`, `SettingChatsPage`, `SettingPersonaPage`, `SettingJanitorPage`, `SettingLorebooksPage`, `SettingModelPage`, `SettingDisplayPage`, `SettingTTSPage`, `SettingMcpPage`, etc.), `CharacterHubPage`, Assistant Detail subpages, Chat sheets, and core Dialog components.

---

## Executive Summary

Across the Kotlin Jetpack Compose UI codebase, the design doctrine dictates:
1. **AMOLED Jet Black (`0xFF000000`)** as the universal surface canvas;
2. **Canonical Card Pattern:** `DomChatGlassCard` / `DOMCHAT_UNIFIED_CARD_COLOR` (`0xFF000000`) with `AppShapes.CardMedium` (24.dp) or `AppShapes.CardLarge` (28.dp);
3. **Specular Border Highlights:** Razor-thin `0.5.dp` specular rim gradient (`LiquidGlassTokens.SpecularBorderSubtle` or `SpecularBorderBrush`), eliminating all solid grey/outline strokes and thick 1.0dp+ borders;
4. **Ergonomic Spacings:** Standardized `AppSpacing` scale with generous bottom clearance (80dp–120dp) for floating navigation/FABs;
5. **Standardized Bottom Sheets & Dialogs:** Black background, top-anchored `DomChatSheetDragHandle()`, explicit `shape = AppShapes.SettingBottomSheet` (16.dp) / `AppShapes.SettingDialog` (16.dp), and zero double-nested surfaces.

The audit revealed **5 distinct structural inconsistencies**:
- **547 hardcoded `RoundedCornerShape` instances** clashing with `AppShapes` and `LiquidGlassTokens` (with 12.dp, 16.dp, 18.dp, 20.dp, 24.dp, and 28.dp arbitrarily mixed even within single pages).
- **20+ lingering non-AMOLED slate/grey fills** (`0xFF141520`, `0xFF141522`, `0xFF18181B`, `0xFF1C1D26`, `0xFF2C1517`) and un-themed Material 3 `surfaceContainer*` backgrounds.
- **Widespread border thickness mismatches** (1.0dp, 1.5dp, 2.0dp) and missing borders on black cards causing invisible contrast boundaries.
- **Modifier vs. Parameter Shape Mismatches**: Several `ModalBottomSheet` and `AlertDialog` instances pass `shape` solely to `Modifier.border(...)` while omitting the composable's `shape` parameter, causing mismatched 28dp vs 16dp corner radiuses.
- **Missing drag handles** and **double-nesting of Surface inside Surface/Card**.

---

## 1. Corner Radiuses & Card Shape Consistency Audit

### 1.1 Architectural Token Clashes
The codebase currently contains three competing shape systems:
- `LiquidGlassTokens.CardShape` = `RoundedCornerShape(20.dp)`
- `DomChatGlassCard` default `shape = LiquidGlassTokens.CardShape` (20.dp)
- `AppShapes` (canonical tokens in `DomChatDesignTokens.kt`):
  - `CardLarge` = `RoundedCornerShape(28.dp)`
  - `CardMedium` = `RoundedCornerShape(24.dp)`
  - `CardSmall` = `RoundedCornerShape(16.dp)`
  - `SettingDialog` = `RoundedCornerShape(16.dp)`
  - `SettingBottomSheet` = `RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)`
  - `SettingControl` = `RoundedCornerShape(12.dp)`
  - `SettingBadge` = `RoundedCornerShape(10.dp)`
  - `InputField` = `RoundedCornerShape(24.dp)`

### 1.2 Page-by-Page Card & Container Radius Inconsistencies

1. **`SettingModelPage.kt` & `DomChatModelSettings.kt` (CRITICAL BUG)**
   - `DomChatModelSettings.kt:47`: `DomChatModelFeatureCard` (used across all model settings in `SettingModelPage.kt:1304`) hardcodes `val cardShape = RoundedCornerShape(18.dp)`.
   - `SettingModelPage.kt:1247-1252`: **Severe geometry mismatch on the same Row:**
     - Line 1247: `.clip(RoundedCornerShape(20.dp))`
     - Line 1252: `border(..., shape = AppShapes.CardMedium)` (which is **24.dp**!)
     - The background is clipped at 20.dp while the border curves at 24.dp.
   - Result: Feature cards on this page are 18.dp, helper cards are 20/24.dp, and bottom sheets are 16.dp.

2. **`CharacterHubPage.kt`**
   - Lines 1660, 1666: `CharacterPortraitCard` hardcodes `RoundedCornerShape(18.dp)`.
   - Lines 1920, 1922: `CharacterHubErrorCard` hardcodes `RoundedCornerShape(18.dp)`.
   - Lines 2284, 2396, 2439, 2488, 2565, 2598, 2626, 2808: Inspector detail section cards use hardcoded `RoundedCornerShape(16.dp)`.
   - Line 2345: Tab row bar uses `RoundedCornerShape(14.dp)`.
   - Line 2357: Active tab indicator pill uses `RoundedCornerShape(10.dp)`.
   - Lines 3176, 3201, 3229, 3809: Batch selection cards use `RoundedCornerShape(20.dp)`.
   - Lines 3271, 3572, 3656: Group dialogs (`NewGroupDialog`, `MoveToGroupDialog`, `GroupActionMenu`) use hardcoded `RoundedCornerShape(24.dp)` instead of `AppShapes.Dialog` (28.dp) or `AppShapes.SettingDialog` (16.dp).

3. **`SettingJanitorPage.kt`**
   - Lines 575, 603, 655, 690, 728: Filter dropdowns and buttons hardcode `RoundedCornerShape(20.dp)`.
   - Lines 1745, 1750: `CharacterCard` uses hardcoded `RoundedCornerShape(18.dp)`.

4. **`SettingChatsPage.kt`**
   - Lines 283, 445, 467, 742, 795, 834: Hardcodes `RoundedCornerShape(18.dp)`.
   - Line 573: Conversation items use `val cardShape = RoundedCornerShape(18.dp)`.
   - Lines 397, 423, 508, 848, 869, 908, 934, 953: Card controls and buttons use `RoundedCornerShape(12.dp)`.
   - Line 684: Chat badges use `RoundedCornerShape(6.dp)` instead of `AppShapes.SettingBadge` (10.dp).

5. **`SettingPersonaPage.kt`**
   - Lines 331, 353, 636: Persona cards use `AppShapes.CardSmall` (16.dp) instead of `CardMedium` (24.dp).
   - Line 412: Active status badge uses `RoundedCornerShape(8.dp)` instead of `AppShapes.SettingBadge` (10.dp).
   - Line 585: Avatar edit surface uses `RoundedCornerShape(12.dp)`.
   - Line 600: Text input fields use `val inputShape = RoundedCornerShape(14.dp)` instead of `AppShapes.InputField` (24.dp).
   - Line 768: Dialog action buttons use `RoundedCornerShape(14.dp)` instead of `AppShapes.SettingControl` (12.dp).

6. **`SettingLorebooksPage.kt` & `SettingLorebookDetailPage.kt`**
   - `SettingLorebooksPage.kt:298`: Empty state card uses `RoundedCornerShape(20.dp)`.
   - `SettingLorebooksPage.kt:491`: `LorebookCard` default uses `RoundedCornerShape(18.dp)`.
   - `SettingLorebooksPage.kt:513-524`: Cover shapes hardcode asymmetric corners (`topStart = 16.dp, topEnd = 6.dp, bottomStart = 16.dp, bottomEnd = 6.dp`) and `6.dp`.
   - `SettingLorebooksPage.kt:667`: Cover preview uses `RoundedCornerShape(10.dp)`.
   - `SettingLorebooksPage.kt:751`: Dialog buttons use `RoundedCornerShape(14.dp)`.
   - `SettingLorebookDetailPage.kt:436, 707`: Uses `AppShapes.CardLarge` (28.dp), conflicting directly with `SettingLorebooksPage`'s 18.dp cards.
   - `SettingLorebookDetailPage.kt:1375, 1527`: Surfaces use `RoundedCornerShape(8.dp)`.

7. **`SettingProviderDetailPage.kt`**
   - Line 637: Floating segmented bar uses custom `RoundedCornerShape(32.dp)`.
   - Lines 665, 706: Inner tabs use `RoundedCornerShape(24.dp)`.
   - Lines 773, 811, 831, 1141, 1415, 1811, 2060, 2386: Cards use `AppShapes.CardLarge` (28.dp).

8. **`SettingDisplayPage.kt`**
   - Line 1247: `GlassModeTab` uses `AppShapes.CardMedium` (24.dp).
   - Line 1498: `InterfaceStyleCard` (right next to it!) uses `RoundedCornerShape(20.dp)`.
   - Line 241: Badge uses `RoundedCornerShape(8.dp)`.
   - Lines 724, 1105: Color chips use `RoundedCornerShape(6.dp)`.
   - Line 1342: Dialog surface uses `RoundedCornerShape(24.dp)`.
   - Line 1407: Hex field uses `RoundedCornerShape(12.dp)`.

9. **`AssistantDetailPage.kt` & Detail Subpages**
   - `AssistantDetailPage.kt:273`: Export DropdownMenu uses `RoundedCornerShape(18.dp)`.
   - `AssistantLorebooksSubPage.kt:101`: Card uses `AppShapes.CardLarge` (28.dp).
   - `AssistantLorebooksSubPage.kt:240-256`: Re-invents grouped shape logic with `cornerRadius = 28.dp, smallCorner = 8.dp` instead of using canonical `AppShapes.ListItemFirst`, `ListItemMiddle`, `ListItemLast`.
   - `AssistantProfileSubPage.kt:187`: Notes card uses `AppShapes.CardMedium` (24.dp).

10. **Confirmation Dialogs**
    - `UniversalDestructiveConfirmDialog.kt:144, 230, 333`: `dialogShape = RoundedCornerShape(20.dp)`
    - `UniversalDestructiveConfirmDialog.kt:44`: `BUTTON_CORNER = 16.dp`
    - `DomChatDestructiveConfirmDialog.kt:62`: `dialogShape = RoundedCornerShape(20.dp)`
    - `DomChatDestructiveConfirmDialog.kt:43`: `BUTTON_CORNER = 16.dp`

---

## 2. Background Colors Audit

### 2.1 Lingering Non-AMOLED Grey/Slate Container Colors
Per the design doctrine, all container cards, scaffolds, sheets, and popovers must use AMOLED Jet Black (`0xFF000000`) or unified glass. The following containers still use legacy slate/grey hex values:

| File Path | Line | Offending Color | Context |
|---|---|---|---|
| `SettingJanitorPage.kt` | 1739 | `Color(0xFF141522)` | `CharacterCard` background (`cardBg`) |
| `SettingJanitorPage.kt` | 1015 | `Color(0xFF10111A)` | Filter sheet container background |
| `SettingJanitorPage.kt` | 1016 | `Color(0xFF181926)` | Filter card container background |
| `SettingJanitorPage.kt` | 1476 | `Color(0xFF121212)` | Dark container fill |
| `CharacterHubPage.kt` | 1661 | `Color(0xFF141520)` | `CharacterPortraitCard` background |
| `CharacterHubPage.kt` | 1690 | `Color(0xFF141520).copy(0.85f)` | Avatar bottom vertical gradient scrim |
| `CharacterHubPage.kt` | 1608 | `Color(0xFF18181B)` | Header background |
| `CharacterHubPage.kt` | 1618 | `listOf(0xFF2C2C2E, 0xFF121214)` | Gradient background |
| `CharacterHubPage.kt` | 1921 | `Color(0xFF141520)` | `CharacterHubErrorCard` background |
| `CharacterHubPage.kt` | 2710 | `Color(0xFF141520)` | Bottom action containerColor |
| `CharacterHubPage.kt` | 3771 | `Color(0xFF1A0A0A)` | Delete group banner container |
| `SettingContextBudgetPage.kt` | 206 | `Color(0xFF1C1D26)` | Context budget bar background |
| `SettingWebPage.kt` | 523 | `Color(0xFF2C1517)` | Reset configuration warning card |
| `CustomIconSelector.kt` | 131 | `Color(0xFF2C1517)` | Reset icon button background |
| `WebViewPage.kt` | 115 | `Color(0xFF141418)` | WebView containerColor |
| `AssistantDetailPage.kt` | 274 | `Color(0xFF0E0F18).copy(0.97f)` | Export dropdown containerColor |
| `AssistantDetailPage.kt` | 594 | `Color(0xFF141520)` | Avatar background container |
| `AssistantLorebooksSubPage.kt` | 96 | `Color(0xFF141520)` | Description card header Surface |
| `AssistantLorebooksSubPage.kt` | 260 | `Color(0xFF141520)` | Lorebook list item Card |
| `AssistantProfileSubPage.kt` | 182 | `Color(0xFF141520)` | Creator notes expandable Surface |
| `AssistantExampleDialoguesSubPage.kt` | 57, 83 | `Color(0xFF141520)` | Example dialogues container Surfaces |
| `AssistantPersonalitySubPage.kt` | 73 | `Color(0xFF141520)` | Personality details card Surface |
| `AssistantScenarioSubPage.kt` | 59 | `Color(0xFF141520)` | Scenario details card Surface |
| `ChatMessageStyleSelector.kt` | 155 | `Color(0xFF141524).copy(0.70f)` | Style preview card container |
| `ChatMessageStyleSelector.kt` | 202 | `Color(0xFF222436).copy(0.85f)` | Chat bubble preview background |
| `NewChatContent.kt` | 291 | `Color(0xFF1E1E24).copy(0.65f)` | Welcome suggestions card fill |
| `DomChatInWindowBottomSheet.kt` | 293 | `Color(0xFF141520).copy(0.35f)` | Bottom sheet container background |
| `McpPicker.kt` | 109 | `Color(0xFF141520).copy(0.9f)` | MCP badge container |
| `ChatMessageV2.kt` | 1515 | `Color(0xFF141520).copy(0.6f)` | Message card background wash |

### 2.2 Material Theme `surfaceContainer*` Without AMOLED Overrides
The following sheets and dialogs still consume stock Material 3 tinted purple/grey surface tokens:
- `ChatMessageCopySheet.kt:45`: `containerColor = MaterialTheme.colorScheme.surfaceContainerLow`
- `ChatMessageTranslation.kt:98`: `containerColor = MaterialTheme.colorScheme.surfaceContainerLow`
- `Mermaid.kt:337`: `containerColor = MaterialTheme.colorScheme.surfaceContainerLow`
- `TextSelectionSheet.kt:147, 231, 249, 334, 517`: Uses `surfaceContainerLow`, `surfaceContainerHigh`, and `surfaceContainerHighest` across cards, input bars, and dialogs.
- `ContextSourcesSheet.kt:215, 322, 437, 452`: Uses `MaterialTheme.colorScheme.surfaceContainerHighest` and `tertiaryContainer` on all item cards.
- `UsedLorebooksSheet.kt:136, 231`: Uses `MaterialTheme.colorScheme.surfaceContainerHighest`.
- `ActivityTimelineSheet.kt`: 17 separate occurrences of `surfaceContainerHigh` and `surfaceContainerHighest`.
- `PersonaPickerBottomSheet.kt:336`: `color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)`.

### 2.3 Milky White Fills Without Backdrop Blur (`Color.White.copy(alpha > 0.05f)`)
- `SettingChatStoragePage.kt:457, 516`: `containerColor = Color.White.copy(alpha = 0.12f)` on action buttons.
- `SettingProviderDetailPage.kt:665, 706`: `Color.White.copy(alpha = 0.12f)` on inactive tab buttons without blur.
- `CharacterHubPage.kt:2066`: `.background(Color.White.copy(alpha = 0.35f))` indicator line.
- `CharacterHubPage.kt:2360`: `.background(if (isSelected) Color.White.copy(alpha = 0.16f) else Color.Transparent)` tab button.
- `SettingLorebookDetailPage.kt:716`: `Surface(color = Color.White.copy(alpha = 0.65f))` entry number circle.
- `DomChatModelSettings.kt:77`: `.background(Color.White.copy(alpha = 0.08f))` icon container.
- `ApiKeyPoolSettingsDialog.kt:170`: `containerColor = if (selected) Color.White.copy(alpha = 0.18f) else Color(0xFF000000)` selection card.
- `TTSController.kt:118`: `Color.White.copy(alpha = 0.45f)` floating pill fill.
- `MinimalChatInput.kt:1917, 1962, 1983, 2156, 2294, 2403, 3498, 4173`: Repeated use of `0.08f`–`0.16f` white fills for buttons and containers.

---

## 3. Border Strokes Audit

### 3.1 Inconsistent Stroke Widths (1.0dp, 1.5dp, 2.0dp vs canonical 0.5dp)
The design doctrine specifies razor-thin `0.5.dp` specular edge highlights. The following components violate this:
- **`SettingRpOptimizationsPage.kt:140, 215, 287, 343, 426`**: All 5 setting group cards apply `.border(1.dp, cardBorder, AppShapes.CardLarge)`.
- **`SettingMcpPage.kt:631`**: `.border(1.dp, presetBorder, shape)`.
- **`SettingProviderPage.kt:689`**: `.border(1.dp, emptyBorderColor, AppShapes.CardLarge)`.
- **`SettingChatsPage.kt:398, 424`**: `BorderStroke(if (isSelected) 1.dp else 0.5.dp, ...)`.
- **`SettingChatsPage.kt:508`**: `BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f))`.
- **`SettingChatsPage.kt:592`**: `BorderStroke(1.5.dp, Color.White)`.
- **`SettingAndroidIntegrationPage.kt:189`**: `BorderStroke(1.dp, ...)`.
- **`ApiKeyPoolBottomSheet.kt:415`**: `BorderStroke(1.5.dp, Color(0xFFFF453A))`.
- **`ApiKeyPoolButton.kt:121`**: `BorderStroke(1.5.dp, Color(0xFFFF453A))`.
- **`ApiKeyPoolSettingsDialog.kt:176`**: `BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f))`.
- **`WaveOriginPicker.kt:96`**: `.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))`.
- **`WaveOriginPicker.kt:146`**: `.border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape)`.
- **`DomChatAssistantPickerSheet.kt:202`**: `BorderStroke(1.dp, MaterialTheme.colorScheme.primary)`.
- **`DomChatProvidersBottomBar.kt:79`**: `.border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)`.
- **`AssistantDetailPage.kt:595`**: `.border(1.dp, LiquidGlassTokens.SpecularBorderSubtle, CircleShape)`.

### 3.2 Missing Borders on AMOLED Black Cards (Contrast Loss)
Black cards on black scaffolds without a specular border become completely invisible:
- **`SettingProviderDetailPage.kt:830`**: Tags card uses `Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF000000)))` with **no border** (while line 810 has `border = BorderStroke(0.5.dp, ...)`).
- **`SettingProviderDetailPage.kt:2385, 3465, 3533`**: Settings cards missing `border` parameter.
- **`UpdateDialog.kt:77`**: Dialog `Surface` has **no border**.
- **`WaveOriginPicker.kt:70`**: Dialog `Surface` has **no border**.
- **`PermissionRationaleDialog.kt:51`**: Outer `Card` has **no border**.
- **`PermissionRationaleDialog.kt:173`**: Inner `PermissionItem` `Card` has **no border**.
- **`SettingLocalLlmPage.kt:845`**: Bottom sheet info `Surface` has **no border**.
- **`ContextSourcesSheet.kt:212, 322, 437`**: All item `Surface` elements have **no border**.
- **`UsedLorebooksSheet.kt:136`**: Item `Surface` has **no border**.
- **`ApiKeyPoolBottomSheet.kt:117`**: `ModalBottomSheet` has **no border**.

### 3.3 Non-Specular Borders (Solid colors or arbitrary gradients)
- `CharacterHubPage.kt:1665`: Fallback border `Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent))` instead of `LiquidGlassTokens.SpecularBorderSubtle`.
- `CharacterHubPage.kt:1922`: `border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.6f), ...)`.
- `SettingLorebooksPage.kt:530, 669`: `BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))`.
- `SettingDisplayPage.kt:1240, 1489`: `BorderStroke(0.5.dp, Color.White.copy(alpha = 0.14f))` when unselected.
- `SettingPersonaPage.kt:413`: `BorderStroke(0.5.dp, Color(0xFF10B981).copy(alpha = 0.45f))`.
- `SettingPersonaPage.kt:587`: `BorderStroke(0.5.dp, Color.White.copy(alpha = 0.18f))`.

---

## 4. Spacing and Paddings Audit

### 4.1 Content Padding Discrepancies in Settings Lists
Settings pages must provide balanced horizontal padding (`16.dp`) and bottom navigation/FAB clearance (`80.dp`–`120.dp`). The current values vary drastically:

| File Path | Line | Configured `contentPadding` | Defect |
|---|---|---|---|
| `SettingPersonaPage.kt` | 302 | `innerPadding + PaddingValues(horizontal = 16.dp, vertical = 12.dp)` | Bottom padding is only 12.dp; FAB covers the last persona card. |
| `SettingChatsPage.kt` | 567 | `PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)` | Insufficient bottom padding (32.dp); obscured by navigation island. |
| `SettingLorebooksPage.kt` | 292 | `contentPadding + PaddingValues(16.dp) + PaddingValues(bottom = 40.dp)` | Adds 16.dp to TOP and SIDES, causing excessive top gap below top app bar. |
| `SettingLorebookDetailPage.kt` | 379 | `contentPadding + PaddingValues(16.dp) + PaddingValues(bottom = 135.dp)` | Adds 16.dp to top in addition to top app bar insets. |
| `SettingProviderDetailPage.kt` | 1134 | `contentPadding + PaddingValues(horizontal = 16.dp)` | Zero bottom clearance; bottom model cards clip behind the floating action bar. |
| `SettingProviderDetailPage.kt` | 2350 | `PaddingValues(8.dp)` | Grid inside sheet is cramped with only 8.dp padding. |
| `SettingRpOptimizationsPage.kt` | 129 | `contentPadding + PaddingValues(16.dp)` | Zero bottom clearance for navigation bar. |
| `SettingFontsPage.kt` | 195 | `contentPadding + PaddingValues(bottom = 16.dp)` | Missing horizontal padding (`16.dp`) on LazyColumn. |
| `SettingPresetsPage.kt` | 303 | `contentPadding = contentPadding` | Missing horizontal padding (`16.dp`). |
| `CharacterHubPage.kt` | 878 | `PaddingValues(horizontal = 16.dp, vertical = 8.dp)` | Bottom padding is only 8.dp in list mode. |

### 4.2 Spacing Scale (`Arrangement.spacedBy`) Jumps
Instead of standardized `AppSpacing.xs` (8.dp), `sm` (12.dp), `md` (16.dp), screens use arbitrary values:
- `SettingDisplayPage.kt:205`: `spacedBy(0.dp)` on main LazyColumn.
- `SettingLorebooksPage.kt:293` & `SettingLorebookDetailPage.kt:380`: `spacedBy(4.dp)` between major cards.
- `SettingPersonaPage.kt:564`: `spacedBy(14.dp)` in dialog.
- `SettingPersonaPage.kt:728`: `spacedBy(1.dp)` between lorebook rows.
- `SettingPresetsPage.kt:368`: `spacedBy(10.dp)`.
- `SettingRpOptimizationsPage.kt:219`: `spacedBy(10.dp)`.
- `CharacterHubPage.kt:2350`: `spacedBy(4.dp)` in tab container; line 3282: `spacedBy(14.dp)` in dialog.
- `DomChatDestructiveConfirmDialog.kt:125`: `spacedBy(10.dp)`.

### 4.3 Accidental Double-Paddings
- **`ChatMessageTools.kt:362-363`**:
  ```kotlin
  modifier = Modifier
      .fillMaxHeight(0.8f)
      .padding(16.dp)
      .padding(16.dp), // ACCIDENTAL DUPLICATE PADDING (32.dp)
  ```
- **`CharacterHubPage.kt:2767, 2775`**: Outer Box has `.padding(20.dp)` and inner Surface modifier adds another `.padding(20.dp)`.
- **`CharacterHubPage.kt:2896, 2904`**: Identical double 20.dp padding in detach confirm dialog.

---

## 5. Popups & Bottom Sheets Audit

### 5.1 Missing Drag Handles
- **`CharacterHubPage.kt:2010`**: `DomChatInWindowBottomSheet` explicitly sets `dragHandle = null` on the character inspector sheet!
- **`ChatMessageCopySheet.kt:49`**: `ModalBottomSheet` explicitly sets `dragHandle = null`!
- **`Mermaid.kt:343`**: `ModalBottomSheet` sets `dragHandle = {}` (empty lambda; no visual drag pill is rendered)!
- **`AssistantPage.kt:563`**: `dragHandle = {}` (empty lambda).

### 5.2 ModalBottomSheet Shape Parameter Omissions (Shape Mismatch with Border)
The following sheets pass `AppShapes.SettingBottomSheet` (16.dp) to `Modifier.border(...)` but **fail to pass `shape` to `ModalBottomSheet` itself**. As a result, Material 3 renders the container with its default 28.dp top corners while the specular border curves at 16.dp:
- **`CustomIconSelector.kt:249-253`**:
  ```kotlin
  ModalBottomSheet(
      containerColor = Color(0xFF000000),
      dragHandle = { DomChatSheetDragHandle() },
      modifier = Modifier.border(0.5.dp, SpecularBorderBrush, AppShapes.SettingBottomSheet).imePadding(),
      // MISSING: shape = AppShapes.SettingBottomSheet
  )
  ```
- **`SettingChatStoragePage.kt:767, 804`**: `shape` parameter missing on `ModalBottomSheet`.
- **`SettingMcpPage.kt:880, 1406`**: `shape` parameter missing on `ModalBottomSheet`.
- **`ApiKeyPoolBottomSheet.kt:117`**: `shape` and `border` parameters missing entirely.
- **`ChatMessageCopySheet.kt:44`**: `shape` and `border` missing.
- **`ChatMessageTranslation.kt:97`**: `shape` and `border` missing.
- **`Mermaid.kt:336`**: `shape` and `border` missing.
- **`ShareSheet.kt:59`**: `shape` and `border` missing.
- **`UIAvatar.kt:249`**: `shape` and `border` missing.
- **`AssistantPage.kt:563`**: `shape` and `border` missing.
- **`AssistantToolsSubPage.kt:355`**: `shape` and `border` missing.
- **`AssistantToolsSubPage.kt:465`**: Uses non-standard `RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)`.
- **`Export.kt:180`**: `shape` and `border` missing.
- **`ImgGenPage.kt:931`**: `shape` and `border` missing.
- **`WebViewPage.kt:153`**: `shape` and `border` missing.

### 5.3 Dialogs Missing Container Color & Specular Borders
The following `AlertDialog` instances omit `containerColor`, falling back to default Material 3 light/tinted styling:
- **`ActivityTimelineSheet.kt:733, 769`**: Edit and Delete memory dialogs omit `containerColor`, `shape`, and `border`.
- **`ChatDrawer.kt:597`**: Delete chat dialog omits `containerColor` and `border`.
- **`ChatPage.kt:2004`**: Confirmation dialog omits `containerColor` and `shape`.
- **`ExportDialog.kt:25` & `ExportOptionsDialog.kt:34`**: Omit `containerColor`, `shape`, and `border`.
- **`TagList.kt:105`**: Tag deletion dialog omits `containerColor`, `shape`, and `border`.
- **`AssistantPromptSubPage.kt:588`**: `AlertDialog` uses `shape = AppShapes.BottomSheet` (a bottom sheet shape on a centered dialog!) and omits `containerColor`.
- **`DomChatMemoryUi.kt:213`**: Memory delete confirmation dialog omits `containerColor`.
- **`ApiKeyPoolBottomSheet.kt:562`**: `AlertDialog` passes `shape = AppShapes.SettingDialog` to `.border(...)` on Modifier, but omits the `shape` parameter on `AlertDialog`!

### 5.4 Double-Nesting Surfaces & Redundant Wrappers
- **`PermissionRationaleDialog.kt:48, 51, 173`**:
  `Dialog` -> `Card` (`0xFF000000`, no border) -> Column -> `PermissionItem` -> nested `Card` (`0xFF000000`, no border). Double-nested black surfaces with no specular rims.
- **`BackupPage.kt:1381, 1392`**:
  `AlertDialog` (`0xFF000000`) -> Column -> nested `Card` (`0xFF000000.copy(0.55f)`).
- **`WaveOriginPicker.kt:66, 70`**:
  `Dialog` -> `Surface(shape = RoundedCornerShape(28.dp), color = 0xFF000000, no border)` -> inner phone frame Box with `.border(1.dp, outlineVariant, 26.dp)`.
- **`UpdateDialog.kt:76, 77`**:
  `Dialog` -> `Surface(shape = AppShapes.CardLarge, tonalElevation = 6.dp, no border)`.
- **`CharacterHubPage.kt:2760, 2769` & `2886, 2898`**:
  `Dialog` -> Box -> `Surface` where `shape` parameter is omitted from `Surface` and `.border(0.5.dp, ..., RoundedCornerShape(28.dp))` is attached to Modifier, creating an unrounded surface with a rounded border.
- **`SettingChatStoragePage.kt:1192, 1197`**:
  `Card(shape = AppShapes.CardMedium)` wrapping an inner `Surface(shape = AppShapes.CardMedium)`.

---

## 6. Prioritized Action Plan / Remediation Roadmap

### Priority 1: Visual Defect & Broken Geometry Fixes (Immediate)
1. **Fix Shape Parameter Mismatches on Sheets & Dialogs**:
   - `SettingModelPage.kt:1247`: Harmonize clip and border to `AppShapes.CardMedium` (24.dp).
   - `CustomIconSelector.kt:249`, `SettingChatStoragePage.kt:767, 804`, `SettingMcpPage.kt:1406`, `ApiKeyPoolBottomSheet.kt:562`: Add explicit `shape = AppShapes.SettingBottomSheet` / `AppShapes.SettingDialog` to the composables, matching their border modifier.
   - `ChatMessageTools.kt:362`: Remove duplicate `.padding(16.dp)`.
2. **Add Missing Drag Handles**:
   - `CharacterHubPage.kt:2010`: Replace `dragHandle = null` with `dragHandle = { DomChatSheetDragHandle() }`.
   - `ChatMessageCopySheet.kt:49` & `Mermaid.kt:343`: Replace `null` / `{}` with `DomChatSheetDragHandle()`.
3. **Harmonize Border Stroke Widths**:
   - In `SettingRpOptimizationsPage.kt:140..426`, change `.border(1.dp, ...)` to `border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)` on the `Card` composable directly.
   - Update `ApiKeyPoolBottomSheet.kt`, `ApiKeyPoolSettingsDialog.kt`, and `SettingChatsPage.kt` to replace `1.0dp` / `1.5dp` strokes with `0.5.dp`.

### Priority 2: AMOLED Jet Black & Non-AMOLED Grey Fills Purge
1. **Convert Lingering Slates to Pure Black (`0xFF000000`)**:
   - Replace `Color(0xFF141520)`, `Color(0xFF141522)`, and `Color(0xFF181926)` with `Color(0xFF000000)` in `SettingJanitorPage.kt`, `CharacterHubPage.kt`, `AssistantDetailPage.kt`, and all `Assistant*SubPage.kt` files.
   - Update `CharacterHubPage.kt:1690` avatar gradient scrim from `0xFF141520` to `Color.Black`.
2. **Replace M3 Stock Surfaces with Black Plate + Specular Rim**:
   - In `ChatMessageCopySheet.kt`, `ChatMessageTranslation.kt`, `Mermaid.kt`, `ContextSourcesSheet.kt`, `UsedLorebooksSheet.kt`, and `TextSelectionSheet.kt`, change `surfaceContainer*` to `Color(0xFF000000)` (or `domChatSheetContainerColor()`) with `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)`.
3. **Eliminate Milky White Fills**:
   - In `SettingChatStoragePage.kt`, `SettingProviderDetailPage.kt`, `DomChatModelSettings.kt`, and `CharacterHubPage.kt`, replace `Color.White.copy(alpha > 0.05f)` with solid dark glassy fills + specular borders or dynamic glass primitives.

### Priority 3: Corner Radius Token Normalization
1. **Enforce Canonical Card Radiuses**:
   - Adopt `AppShapes.CardMedium` (24.dp) as the standard for primary cards in settings and character hub (or `DomChatGlassCard` with unified tokens).
   - Deprecate random `18.dp` in `SettingChatsPage.kt`, `SettingJanitorPage.kt`, `SettingLorebooksPage.kt`, and `DomChatModelSettings.kt` in favor of `AppShapes.CardMedium`.
   - Normalize dialogs to `AppShapes.SettingDialog` (16.dp) for settings and `AppShapes.Dialog` (28.dp) for modal alert dialogs.
   - Use `AppShapes.ListItemFirst`, `ListItemMiddle`, and `ListItemLast` in `AssistantLorebooksSubPage.kt` instead of custom 28dp/8dp calculations.

### Priority 4: Padding & Bottom Clearance Normalization
1. **Add Proper Bottom Insets to Settings LazyColumns**:
   - In `SettingPersonaPage.kt`, `SettingChatsPage.kt`, `SettingProviderDetailPage.kt`, `SettingRpOptimizationsPage.kt`, and `CharacterHubPage.kt`, adjust `contentPadding` bottom to at least `80.dp`–`96.dp` (`AppSpacing.xl`..`AppSpacing.xxl` + navigation insets) so cards and FABs never collide or clip offscreen.
2. **Standardize `spacedBy` Scale**:
   - Standardize column arrangements to `Arrangement.spacedBy(AppSpacing.sm)` (12.dp) for card lists and `AppSpacing.xs` (8.dp) for inner card elements.

---
*End of Audit Report.*
