# DOMCHAT COMPREHENSIVE OVERHAUL — UNIFIED ARCHITECTURAL & HIGH-END UI SPECIFICATION

You are the Lead Android UI/UX & Graphics Engineer on DomChat (/storage/emulated/0/Opencode/domchat-clone).
You are working autonomously across the codebase in this persistent session.
You have full access to the codebase and the reference codebases:
- **Convx Reference**: `/storage/emulated/0/Opencode/reference_apps/Convx` (Liquid glass, refractive specular shaders, backdrop blur, Apple glass styling: see `com.convx.music.ui.component.GlassEffect.kt`, `GlassCircleButton.kt`, `IosGlass.kt`)
- **Essentials Reference**: `/storage/emulated/0/Opencode/reference_apps/essentials` (Smooth spring physics, item reordering, long-press contextual menus, fluid physics)
- **PrismalAGSL Reference**: `/storage/emulated/0/Opencode/reference_apps/PrismalAGSL` (AGSL runtime shaders, refractive optics)

---

## DESIGN DOCTRINE: HIGH-END MOBILE ERGONOMICS (ANTI-SLOP)

1. **Color & Hierarchy**:
   - AMOLED Jet Black background (`0xFF000000`) for all settings and root scaffold surfaces. No muddy grey cards.
   - Contrast: Pure white text (`0xFFFFFFFF`) for primary titles and active labels; `Color.White.copy(alpha = 0.65f)` for secondary descriptions; `Color.White.copy(alpha = 0.35f)` for disabled elements.
   - Destructive actions: Centered glassy confirmation dialog with white text/icon (never muddy red block).
2. **Spacing & Ergonomics**:
   - Small screen mobile first. Every popover, dialog, and sheet must be vertically and horizontally centered with `Modifier.widthIn(max = 420.dp)` so it never clips offscreen or at the bottom.
   - Generous touch targets (min 48dp). Generous internal padding (14dp..20dp).
   - Clear visual hierarchy: 1 primary action per screen/dialog, secondary actions muted.
3. **Liquid Glass Optics**:
   - Surface: Apple-inspired refractive glass surfaces with subtle alpha (`0x0DFFFFFF` to `0x26FFFFFF`).
   - Specular Border: Thin 0.5dp specular highlight rim `Brush.verticalGradient(listOf(Color.White.copy(0.40f), Color.White.copy(0.12f), Color.White.copy(0.03f), Color.Transparent))`.
   - Corners: Smooth squircle corners (20dp cards, 16dp inner containers, pill buttons).
   - Micro-motion: Springs (`dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow`) instead of rigid linear jumps.

---

## MODULE 1: THE GLASS & DISPLAY ENGINE

1. **Unify Sections Under Animation Speed (`SettingDisplayPage.kt`)**:
   - Delete the separate/duplicate glass section at the top of `SettingDisplayPage.kt`.
   - Place the unified glass section directly inside the **"Interface Appearance & Transparency"** group, **immediately below the "Animation Speed" toggle**.
2. **Two-Preset Architecture**:
   - Segmented toggle between **"Liquid Glass"** (Default) and **"OG Blur"**.
   - **Liquid Glass**: Convx-inspired refractive surfaces (`SpecularBorderSubtle`, translucent fill, subtle drop shadow) active out-of-the-box across all chat page menus, items, buttons, tabs, bottom nav islands, and search bars.
   - **OG Blur**: Classic translucent dark glass (`0xFF141520` background with subtle specular border).
3. **Advanced Glass Tuning Accordion**:
   - Hide fine-tuning sliders behind an animated toggle (`AnimatedVisibility` with `expandVertically` + `shrinkVertically`).
   - Sliders included:
     - **Bar Gradient Density**: Default 100%. Lowering it makes the bottom bar more transparent.
     - **Bar Gradient Blur**: Slider (0dp..60dp) to blur the bar gradient.
     - Interface opacity, blur, base luminance, and chat bubble sliders.
   - Note: In `DomChatSlider`, `onValueChangeFinished` is a no-argument lambda `(() -> Unit)`. Update via local state variable `persistDisplay { it.copy(...) }`.
4. **Progressive Header Blur**:
   - Settings page headers: Replace harsh solid black cutoffs with a progressive gradient fading from solid black at status bar to transparent at 72dp.

---

## MODULE 2: DIALOGS, POPUPS & NAVIGATION

1. **Universal Centered Dialogs**:
   - All popups (Group creation, Delete confirmation, Card actions) must be centered on screen with `Modifier.widthIn(max = 420.dp)`.
   - Never cut off at the bottom on small mobile screens.
   - Eliminate dual backgrounds (nested surfaces drawing conflicting containers).
2. **Universal Destructive Action Dialog (`UniversalDestructiveConfirmDialog.kt`)**:
   - Centered glassy dialog with `Cancel` button and prominent glassy `Delete` button (white text, white icon, specular border).
3. **Back Navigation**:
   - Back button from active chat screen must navigate directly to app **Home** (`Screen.ConversationList`).
4. **Chat Side Menu Selection**:
   - When selecting a chat in the side menu, the popup that appears must have a black background, glassy specular border, and blurred backdrop (no grey background).

---

## MODULE 3: CHAT SCREEN & INPUT BAR CONTROLS

1. **Bottom Chat Bar (`MinimalChatInput.kt`)**:
   - Plus icon button: Transparent center with glassy specular rim (not a dark square on 5-10% tint).
   - Context circle icon: Transparent center with glassy border.
   - Stop response button: Rounded outer container with rounded inner stop shape (no sharp inner square).
   - Remove suggestion bar above chat input.
   - Message delete button: Clean white icon (not grey).
2. **Top Bar (`ChatPage.kt`)**:
   - Hamburger icon: Clean white strips matching new chat button.
   - Character picker tag: Glassy `Surface` with `SpecularBorderSubtle` and white text (no solid black box).
   - Plus icon: Clicking in active chat returns to Home page (does not duplicate chat).
3. **Response Swiping & Arrow Unification**:
   - Swiping right-to-left on bot message: If on the last roll, automatically generates a new response.
   - Initial Message: If on the last greeting, swiping right-to-left opens a glassy dialog to add a custom alternate greeting and save it to the bot permanently.
   - Unified Arrow: Right arrow navigates existing response rolls; when at the latest roll, tapping it triggers the next generation.
4. **Bot Identity**:
   - Distinguish `displayName` (cards, popovers, hub) vs `chatName` (chat message headers, chat bubbles).

---

## MODULE 4: PLUS ICON MENU & UNIFIED POPOVERS

1. **Header Unification Across All Popovers**:
   - Unify headers of all popovers to match the Creator Notes popover: transparent icon background, clean title, grab handle, close button.
2. **Menu Structure**:
   - Remove User Persona from plus menu (already accessible by clicking user avatar).
   - Order: (1) Creator Notes, (2) Prompt Presets, (3) Author's Notes, (4) Wallpaper, (5) Text Styling.
3. **Popover Redesigns**:
   - **Creator Notes Popover**: Moved to top; full Markdown, images, and text color support.
   - **Prompt Presets Popover**: Complete redesign. Remove tall transparent cards and rainbow badges. Compact mobile-friendly glassy 2x2 cards, rounded search bar, glassy "Enable All" and "Disable All" buttons.
   - **Author's Notes Popover**: Glassy specular border on input (use `OutlinedTextFieldDefaults.colors` focused/unfocused border, do NOT pass `border = BorderStroke` to `OutlinedTextField`).
   - **Per-Chat Wallpaper Popover**: Glassy buttons, animated fade toggle, bold centered save button, smooth content transitions.
   - **Text Styling Popover**: Mobile-optimized glassy cards with smooth animations.
   - **Model Picker Popover**: "No AI Provider" card styled with compact glass (`DomChatGlassCard`).

---

## MODULE 5: CHARACTER CREATION & EDITING OVERHAUL

1. **Profile**: Empty avatar styled with glassy border. Fix single-line overflow: display name, chat name, and tags properly spaced.
2. **Creator Notes**: Dedicated subpage/tab with expandable multi-line text editor (Expand/Collapse), full Markdown, text colors, and inline images. Tags styled with black background and glassy border.
3. **Personality**: Remove complicated "system prompt" terminology; reframe for roleplay chatbots. Replace grey background with glass surface, add example placeholder text, full Markdown support. Useful subtitle explanations like SillyTavern.
4. **Author's Notes**: Renamed from "Scenario". Expandable editor, useful explanation subtitles.
5. **Initial Messages / Greetings**: Single card with expandable Markdown input. Horizontal row of tabs above input: `Initial #1`, `+ Add` (creates `Initial #2`). Use `combinedClickable(onClick = { ... }, onLongClick = { ... })` with `@OptIn(ExperimentalFoundationApi::class)` to rename, delete, or reorder.
6. **Lorebooks**: Add lorebooks directly from this subpage with a compact search bar and selectable chips. Model uses `enabledLorebookIds: Set<Uuid>` (pass `lorebook.id`, not `toString()`).
7. **Cleanups**:
   - Delete "Preset Messages".
   - Remove lorebooks from Context page.
   - Custom and Global buttons: Black background, glassy border theme.

---

## MODULE 6: CHARACTER HUB & JANITOR AI HUB

1. **Folders / Group Tabs**:
   - Horizontally scrollable row below upload area: `All (count)`, `Custom (count)`, ..., `+ New`.
   - Tighten horizontal tab spacing (`spacedBy(4.dp)`).
   - `+ New` opens centered glassy dialog to create/name group.
   - Long-press group tab: Menu to rename, delete, or reorder (move forward/backward). Deleting group does NOT delete bots.
   - Long-press character card: Smooth menu (`Delete` with confirmation, `Move to Group` with searchable list and Done button).
   - Group switching uses smooth `AnimatedContent` transition.
2. **Layout & Sorting**:
   - Header area title: Scale title up to match other pages; reduce gap between title and search bar.
   - List/Grid toggle: Move to the far right. Fix list view horizontal compression and animation jank.
   - Bio / Blurb: Stop dumping full creator notes into the short card blurb area (show name + tags).
   - Recent sorting: Fix sorting to sort by `lastModified` / `lastChatTimestamp`.
3. **JAI Markdown & Image Preservation**:
   - Bio images: Render inline exactly where placed (between paragraphs, at start, end).
   - Initial messages: Render inline images.
   - Full Markdown, colors, dropdowns, and hyperlinks preserved on import without sanitization wipes.

---

## MODULE 7: ALL CHATS, SETTINGS & POLISH

1. **All Chats Page (`SettingChatsPage.kt`)**:
   - Chat card layout: Fix horizontal text overflow on mobile screens (`maxLines = 2`, ellipsis).
   - Selection management menu: Animate in smoothly with spring/slide (`slideInVertically` + `fadeIn`, `slideOutVertically` + `fadeOut` with `androidx.compose.animation.core.tween`).
2. **Settings Polish**:
   - **User Persona Page (`SettingPersonaPage.kt`)**: Edit button styled with glass. Add option to assign lorebooks (compact search + chips that don't overwhelm mobile).
   - **Text Styling Rules Page (`SettingRpOptimizationsPage.kt`)**: Active rule card rounded corners match cards above it (`AppShapes.CardMedium` / 20dp).
   - **Testing Settings Page (`TestingSettingsPage.kt`)**: Functional test toggles for mock lorebooks and character cards that actually inject test data into state. Chain padding (`.padding(horizontal = 16.dp).padding(top = 12.dp)`). Import `org.koin.androidx.compose.koinViewModel`.
   - **About Page Badge**: Black background, glassy border, reading "v1.0 • Maintained by Lucid". Anti-AI copy cleanup.

---

## COMPILATION & KOTLIN SAFETY INVARIANTS
- **No missing imports**: Verify `androidx.compose.animation.core.tween`, `androidx.compose.material.icons.rounded.KeyboardArrowUp/Down`, `combinedClickable`, `ExperimentalFoundationApi`.
- **BorderStroke Arity**: `BorderStroke(width: Dp, color: Color)` or `BorderStroke(width: Dp, brush: Brush)`. Both branches of `if (cond)` must evaluate to the same type (do NOT mix `Color` and `Brush`).
- **Slider Callbacks**: `onValueChangeFinished: (() -> Unit)?`. No parameters in callback lambda.
- **Lorebook IDs**: `assistant.enabledLorebookIds` is `Set<Uuid>`. Use `lorebook.id` directly.
- **OutlinedTextField**: Configure colors via `OutlinedTextFieldDefaults.colors()`. Do NOT pass `border = BorderStroke(...)`.
- **Padding Overload**: Do NOT pass `padding(horizontal = ..., top = ...)`. Chain `.padding(horizontal = ...).padding(top = ...)`.
- **LazyColumn**: Wrap all items in `item { }` or `items(...) { }`.
- **Duplicate Classes**: Do NOT declare `LiquidGlassTokens` in `:app`; `:ui-core` is the single source of truth.
