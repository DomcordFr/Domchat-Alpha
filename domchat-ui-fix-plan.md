# DomChat UI Redesign & Glass Enforcement Master Plan

> [!IMPORTANT]
> **Core Mandate**: Eliminate all Material Design 3 default colors, blue-grey tinted cards, and washed-out semi-transparent backgrounds across the **Settings page and all subpages**, **Character Hub**, **Onboarding**, and **Toasts**. 
> All surfaces, cards, input fields, pop-ups, dialogs, dropdowns, and bottom sheets must strictly match the **"No Lorebooks" reference pattern** (`DomChatGlassCard` / near-black glassy surface with solid/near-solid dark fill and specular borders).

---

## 1. Design System Reference & Canonical Tokens

### 1.1 The Canonical Card: `DomChatGlassCard`
- **Location**: `me.lucid.domchat.ui.components.ui.DomChatGlassCard.kt`
- **Canonical Fill Color**: `DOMCHAT_UNIFIED_CARD_COLOR = 0xFF141520`
- **Reference Pattern**: `EmptyStateCard.kt` ("No Lorebooks" card).
- **Standard Card Usage**:
```kotlin
DomChatGlassCard(
    modifier = modifier,
    shape = AppShapes.CardMedium, // 24.dp
    onClick = { /* optional */ }
) {
    // Content here
}
```

### 1.2 The Glass Specular Border Recipe
For any `Surface`, `Box`, `DropdownMenu`, or dialog container that cannot directly wrap `DomChatGlassCard`, use this exact border & surface configuration:
```kotlin
Surface(
    shape = AppShapes.CardMedium, // Or AppShapes.Dialog, AppShapes.InputField, etc.
    color = Color(0xFF141520),
    border = BorderStroke(
        width = 0.5.dp,
        brush = Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent
            )
        )
    )
) {
    // Content
}
```
*(Alternatively use `LiquidGlassTokens.SpecularBorderSubtle` or `LiquidGlassTokens.SpecularBorderBrush`)*

### 1.3 `AppShapes` Tokens
- `AppShapes.CardLarge` = `28.dp`
- `AppShapes.CardMedium` = `24.dp`
- `AppShapes.CardSmall` = `16.dp`
- `AppShapes.InputField` = `24.dp`
- `AppShapes.Dialog` = `28.dp`
- `AppShapes.BottomSheet` = `RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`
- `AppShapes.ButtonPill` = `RoundedCornerShape(50)` (Pill shape)

---

## 2. Global Sweep Rules (Applies to ALL Settings Pages)

Every implementing agent must apply these standardized rules across the entire settings module:

1. **`DropdownMenu` / Selection Popovers**:
   - Must set `containerColor = Color(0xFF141520)` (or wrap in a `Surface` with `AppShapes.CardSmall` and specular border).
   - Dropdown item text must be white/high-contrast.

2. **`OutlinedTextField` / `BasicTextField`**:
   - Replace default Material colors with:
   ```kotlin
   colors = OutlinedTextFieldDefaults.colors(
       focusedContainerColor = Color(0xFF141520),
       unfocusedContainerColor = Color(0xFF141520),
       focusedTextColor = Color(0xFFF2F2F7),
       unfocusedTextColor = Color(0xFFF2F2F7),
       focusedBorderColor = Color.White.copy(alpha = 0.30f),
       unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
       cursorColor = Color.White,
       focusedLabelColor = Color(0xFFB9A8FC),
       unfocusedLabelColor = Color(0xFF8A8A96)
   ),
   shape = AppShapes.InputField
   ```

3. **Material `Card`**:
   - Ban `CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer...)` or `errorContainer`.
   - Replace with `DomChatGlassCard` or `Surface(color = Color(0xFF141520), shape = AppShapes.CardMedium, border = specularBorder)`.

4. **`AlertDialog`**:
   - `containerColor = Color(0xFF141520)`
   - `shape = AppShapes.Dialog`
   - Confirm and Dismiss buttons must use glass pill style or `ButtonPill` shape.

5. **`ModalBottomSheet`**:
   - `containerColor = Color(0xFF141520)`
   - `sheetShape = AppShapes.BottomSheet`

6. **Tab Selectors / Segmented Buttons**:
   - Ban default Material `SingleChoiceSegmentedButtonRow` / `TabRow`.
   - Replace with pill-style segment row: outer container `Color(0xFF141520)` with specular border, selected item `Color.White.copy(alpha = 0.15f)` or primary glass fill, unselected item transparent.

7. **Buttons**:
   - Ban grey `FilledTonalButton` or default Material grey shapes.
   - Use `DomChatGlassPillButton` or `Button(shape = AppShapes.ButtonPill, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141520)))` with specular border.

---

## 3. Detailed File-by-File Implementation Plan

### 3.1 `res/values/strings.xml`
- **Issue**: Heading on the outside says "Display & Theme Settings", but inside page title resolved to "Advanced Settings".
- **Fix**:
  - Update `R.string.setting_display_page_title`: change value from `"Advanced Settings"` to `"Display & Theme Settings"`.

---

### 3.2 `SettingPage.kt`
- **Location**: `me.lucid.domchat.ui.pages.setting.SettingPage.kt`
- **Issues**:
  1. Live Search Bar (`OutlinedTextField`, lines 308–331) uses Material theming without solid dark background.
  2. `ProviderConfigWarningCard` (lines 434–469) uses Material `errorContainer` red card.
- **Fixes**:
  1. Add `colors = OutlinedTextFieldDefaults.colors(...)` to the search bar with `focusedContainerColor = Color(0xFF141520)` and `unfocusedContainerColor = Color(0xFF141520)`.
  2. Refactor `ProviderConfigWarningCard`:
     ```kotlin
     Surface(
         onClick = { navController.navigate(Screen.SettingProvider) },
         shape = AppShapes.CardLarge,
         color = Color(0xFF141520),
         border = BorderStroke(1.dp, Color(0xFFFF453A).copy(alpha = 0.45f)),
         modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
     ) {
         Row(
             modifier = Modifier.fillMaxWidth().padding(16.dp),
             verticalAlignment = Alignment.CenterVertically,
             horizontalArrangement = Arrangement.spacedBy(16.dp)
         ) {
             Icon(
                 imageVector = Icons.Rounded.Warning,
                 contentDescription = null,
                 tint = Color(0xFFFF453A)
             )
             Column(modifier = Modifier.weight(1f)) {
                 Text(
                     text = stringResource(R.string.setting_page_config_api_title),
                     style = MaterialTheme.typography.titleSmall,
                     color = Color.White
                 )
                 Text(
                     text = stringResource(R.string.setting_page_config_api_desc),
                     style = MaterialTheme.typography.bodySmall,
                     color = Color.White.copy(alpha = 0.7f)
                 )
             }
         }
     }
     ```

---

### 3.3 `SettingDisplayPage.kt`
- **Location**: `me.lucid.domchat.ui.pages.setting.SettingDisplayPage.kt`
- **Issues**:
  1. Title mismatch resolved via string resource.
  2. "Choose" wallpaper button (lines 253–264) is a plain grey `FilledTonalButton`.
  3. Slider responsiveness and theme consistency.
  4. Presets and Dropdown menus use translucent/Material surfaces.
- **Fixes**:
  1. Replace `FilledTonalButton` with `DomChatGlassPillButton(text = if (globalBg.isNullOrBlank()) "Choose" else "Change", onClick = { ... })` or a custom `Surface` with `Color(0xFF141520)` and specular border.
  2. Verify all sliders in the Interface Appearance & Chat Bubbles sections use `DomChatSlider` with haptic feedback on discrete steps.
  3. Ensure `Select` dropdown items and menus specify `containerColor = Color(0xFF141520)`.
  4. Ensure `GlassPresetSelector` uses solid `Color(0xFF141520)` with specular borders.

---

### 3.4 `SettingProviderDetailPage.kt` & `components/ProviderConfigure.kt`
- **Location**: `me.lucid.domchat.ui.pages.setting.components.ProviderConfigure.kt`
- **Issues**:
  1. `ProviderTypeSelector` (lines 260–351): "OpenAI", "Google", "Claude" tabs are left-aligned or unevenly stretched, using Material `secondaryContainer`.
  2. ComfyUI setup warning/status cards (lines 564–585, 605–630) use Material `secondaryContainer` and `errorContainer`.
  3. `SingleChoiceSegmentedButtonRow` for OpenAI compatibility mode uses default Material 3 colors.
  4. API path and Base URL input fields lack solid near-black background fill.
- **Fixes**:
  1. Center and re-style `ProviderTypeSelector`:
     - Wrap in a centered container (`Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth())`).
     - Style each tab using near-black glass pills (`Color(0xFF141520)` container, selected: `Color.White.copy(alpha = 0.18f)` fill, specular border).
  2. Convert ComfyUI info cards to `DomChatGlassCard` or `Surface(color = Color(0xFF141520))` with specular border. For errors, use red border tint (`0xFFFF453A`).
  3. In `OpenAICompatibilityModeSetting`, replace `SegmentedButton` with custom glass pill row.
  4. Ensure all text fields have `focusedContainerColor = Color(0xFF141520)` and `unfocusedContainerColor = Color(0xFF141520)`.

---

### 3.5 `components/ApiKeyPoolBottomSheet.kt` & `ApiKeyPoolSettingsDialog.kt`
- **Location**:
  - `me.lucid.domchat.ui.pages.setting.components.ApiKeyPoolBottomSheet.kt`
  - `me.lucid.domchat.ui.pages.setting.components.ApiKeyPoolSettingsDialog.kt`
- **Issues**:
  1. Bottom sheet uses translucent/Material background.
  2. `ApiKeyCard` (lines 406–425) uses Material `surfaceContainer` and `surfaceContainerHigh`.
  3. `ApiKeyPoolSettingsDialog` uses Material `AlertDialog` and `StrategyOptionCard` uses `surfaceContainer`.
- **Fixes**:
  1. In `ApiKeyPoolBottomSheet`, set `containerColor = Color(0xFF141520)`.
  2. In `ApiKeyCard`, replace `CardDefaults.cardColors(...)` with solid `Color(0xFF141520)` and specular border (`LiquidGlassTokens.SpecularBorderSubtle`).
  3. In `ApiKeyPoolSettingsDialog`, set `containerColor = Color(0xFF141520)`. In `StrategyOptionCard`, replace `surfaceContainer` with `Color(0xFF141520)` and specular border.

---

### 3.6 `components/SecureOutlinedTextField.kt`
- **Location**: `me.lucid.domchat.ui.pages.setting.components.SecureOutlinedTextField.kt`
- **Fix**:
  - Update lines 50–59: change `focusedContainerColor` and `unfocusedContainerColor` to solid `Color(0xFF141520)`, with white specular border highlights (`Color.White.copy(alpha = 0.25f)`). This fixes all secure password/API key fields globally.

---

### 3.7 `MinimalChatInput.kt` (Per Chat Wallpaper Sheet)
- **Location**: `me.lucid.domchat.ui.components.ai.MinimalChatInput.kt` (lines 1999–2170)
- **Issues**:
  1. "Choose Wallpaper" button (`DomChatGlassPillButton`, line 2032) has `Modifier.weight(1f)`, stretching across the sheet awkwardly.
  2. "No wallpaper set" chip (`DomChatGlassChip`, line 2023) is too small.
  3. "Save for this chat" button (line 2147) is tucked in the bottom right corner as a tiny chip instead of a full primary action.
- **Fixes**:
  1. Restructure the wallpaper action row:
     - Wrap "Choose Wallpaper" in a comfortable width constraint (e.g. `Modifier.widthIn(max = 200.dp)` or `wrapContentWidth()`).
     - Center or space evenly with the "Remove" button.
  2. Increase typography and padding on `DomChatGlassChip`:
     - Text style `MaterialTheme.typography.labelMedium` with `padding(horizontal = 14.dp, vertical = 6.dp)`.
  3. Place "Save for this chat" as a prominent, full-width glass pill button at the bottom of the column:
     ```kotlin
     DomChatGlassPillButton(
         text = "Save for this chat",
         accent = true,
         modifier = Modifier.fillMaxWidth().height(48.dp),
         onClick = { ... }
     )
     ```

---

### 3.8 `OnboardingPage.kt`
- **Location**: `me.lucid.domchat.ui.pages.onboarding.OnboardingPage.kt`
- **Issues**:
  - "Skip" and "Too many options?" buttons (lines 670–679, 1259–1306) appear washed out / semi-transparent.
  - Setup buttons in dark mode have low opacity.
- **Fixes**:
  - In `SetupButton` (lines 1269–1270, 1294–1295):
    - Replace container color with solid `Color(0xFF141520)` or `Color(0xFF0D0E16)`.
    - Apply specular border: `BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)`.
    - Ensure high-contrast white text (`Color(0xFFF2F2F7)`).

---

### 3.9 `AppToast.kt`
- **Location**: `me.lucid.domchat.ui.components.ui.AppToast.kt`
- **Issues**:
  - At 0% UI glass opacity, toast containers become transparent/washed out, causing text to blend into the background.
  - Settings page toasts should follow the "No Lorebooks" solid dark glass design.
- **Fixes**:
  - In `getToastColors(type: ToastType)` (lines 434–472):
    - Set the toast plate base color to solid near-black with an opacity floor:
      `val base = Color(0xFF0D0E16).copy(alpha = 0.95f)`
    - Ensure border is visible with specular highlight:
      `border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.20f))`

---

### 3.10 `CharacterHubPage.kt` (Header & Controls Layout Rework)
- **Location**: `me.lucid.domchat.ui.pages.assistant.CharacterHubPage.kt` (lines 295–428)
- **Issues**:
  - Header layout is inconsistent with Settings pages: Back button, title, upload icon, and grid/list icon are crammed into a single 52dp bar.
  - User requested:
    1. Back button standalone at top-left.
    2. Large "Characters" title below the back button.
    3. Search bar.
    4. Sorting options ("Recent", "Name", "Creator").
    5. **Upload Character and Grid/List toggle buttons placed BELOW the sorting options row**.
- **Fixes**:
  - Replace lines 300–428 with the specified layout hierarchy:
  ```kotlin
  Column(
      modifier = Modifier
          .fillMaxSize()
          .padding(padding)
          .statusBarsPadding()
  ) {
      // 1. Standalone Top Row with Back Button
      Row(
          modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically
      ) {
          BackButton()
      }

      // 2. Large Page Title
      Text(
          text = "Characters",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = if (isDark) Color.White else Color.Black,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
      )

      // 3. Search Bar (Solid near-black fill)
      OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 6.dp),
          placeholder = { Text("Search by character or creator...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color(0xFF888888)) },
          trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                      Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = Color(0xFF888888))
                  }
              }
          },
          singleLine = true,
          shape = AppShapes.InputField,
          colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color(0xFF141520),
              unfocusedContainerColor = Color(0xFF141520),
              focusedBorderColor = Color.White.copy(0.3f),
              unfocusedBorderColor = Color.White.copy(0.12f),
              focusedTextColor = Color.White,
              unfocusedTextColor = Color.White
          )
      )

      // 4. Sorting Options Row
      Row(
          modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
      ) {
          Text(
              text = "${filteredAssistants.size} characters loaded",
              fontSize = 12.sp,
              color = Color(0xFF888888)
          )
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              CharacterSortOption.entries.forEach { option ->
                  val selected = sortOption == option
                  Surface(
                      onClick = { sortOption = option },
                      color = if (selected) Color.White.copy(alpha = 0.18f) else Color(0xFF141520),
                      shape = RoundedCornerShape(10.dp),
                      border = BorderStroke(0.5.dp, if (selected) LiquidGlassTokens.SpecularBorderBrush else LiquidGlassTokens.SpecularBorderSubtle)
                  ) {
                      Text(
                          text = when (option) {
                              CharacterSortOption.RECENT -> "Recent"
                              CharacterSortOption.NAME -> "Name"
                              CharacterSortOption.CREATOR -> "Creator"
                          },
                          fontSize = 11.sp,
                          fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                          color = if (selected) Color.White else Color(0xFF888888),
                          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                      )
                  }
              }
          }
      }

      // 5. Actions Row (BELOW Sorting Options): Upload Button + Grid/List Toggle
      Row(
          modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
      ) {
          DomChatGlassPillButton(
              text = "Upload Character",
              icon = Icons.Rounded.FileUpload,
              onClick = {
                  importLauncher.launch(arrayOf("application/json", "image/png", "image/webp", "*/*"))
              }
          )

          Surface(
              onClick = { isGridView = !isGridView },
              shape = AppShapes.ButtonPill,
              color = Color(0xFF141520),
              border = BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderSubtle)
          ) {
              Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                  Icon(
                      imageVector = if (isGridView) Icons.Rounded.ViewList else Icons.Rounded.GridView,
                      contentDescription = "Toggle View",
                      tint = Color.White,
                      modifier = Modifier.size(18.dp)
                  )
                  Text(
                      text = if (isGridView) "List View" else "Grid View",
                      style = MaterialTheme.typography.labelMedium,
                      color = Color.White
                  )
              }
          }
      }
      
      // 6. Character Gallery Grid / List ...
  }
  ```

---

### 3.11 `SettingPersonaPage.kt`
- **Location**: `me.lucid.domchat.ui.pages.setting.SettingPersonaPage.kt`
- **Issue**: "Use in Chat" button is a plain white solid button.
- **Fix**:
  - Replace with dark glass pill button (`Color(0xFF141520)` container, specular border, white text and icon).

---

### 3.12 `SettingWebPage.kt` & `SettingAndroidIntegrationPage.kt`
- **Location**: `me.lucid.domchat.ui.pages.setting.SettingWebPage.kt`
- **Issues**:
  1. "Domchat in a browser" banner card uses blue-grey Material Card.
  2. WebDAV backup and Port input fields use default Material colors.
- **Fixes**:
  1. Replace "Domchat in a browser" Card with `DomChatGlassCard(shape = AppShapes.CardLarge)`.
  2. Apply `Color(0xFF141520)` container color to all port, URL, and credential text fields.

---

### 3.13 Global Sweep of Remaining Settings Subpages
The following subpages must be inspected to ensure no `Card(colors = ...)` or default `OutlinedTextField` remain:
- `SettingAboutPage.kt`: App info card → `DomChatGlassCard`.
- `SettingChatStoragePage.kt`: Storage stats cards → `DomChatGlassCard`.
- `SettingChatsPage.kt`: Batch action dialogs → `Color(0xFF141520)`.
- `SettingContextBudgetPage.kt`: Budget slider and preview card → `DomChatGlassCard`.
- `SettingFontsPage.kt`: Font preview item cards → `DomChatGlassCard`.
- `SettingLorebookDetailPage.kt`: Entry cards, keyword input fields → `Color(0xFF141520)`.
- `SettingLorebooksPage.kt`: Lorebook list items → `DomChatGlassCard`.
- `SettingMcpPage.kt`: Server configuration cards & dialogs → `DomChatGlassCard`.
- `SettingModelPage.kt`: Model item cards & filter chip row → Near-black glass pills.
- `SettingPresetsPage.kt`: Preset selector cards → `DomChatGlassCard`.
- `SettingPromptInjectionsPage.kt`: Prompt item cards & text editors → `Color(0xFF141520)`.
- `SettingRpOptimizationsPage.kt`: Optimization toggles & cards → `DomChatGlassCard`.
- `SettingSearchPage.kt`: Search provider cards & API inputs → `Color(0xFF141520)`.
- `SettingSkillsPage.kt`: Skill cards & parameters → `DomChatGlassCard`.
- `SettingTTSPage.kt` & `SettingTTSProviderDetailPage.kt`: Voice selection & engine cards → `DomChatGlassCard`.
- `SettingUICustomizationPage.kt`: Color picker dialogs & preview cards → `Color(0xFF141520)`.
- `SettingLocalLlmPage.kt`: Model download cards & hardware specs → `DomChatGlassCard`.

---

## 4. Verification & Build Command

After implementing the changes above, verify that the Android project compiles cleanly with 0 errors:

```bash
cd /storage/emulated/0/gemini/Outputs/domchat-clone
./gradlew :app:assembleDebug
```
Ensure all modified composables preview and render without runtime crashes or missing symbols.
