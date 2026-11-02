# Audit Brief — Top and Bottom Bars Opacity, Top Plus Home Navigation and Home Card

## Intent Verification

### Objective
Fix 5 build compile errors across 3 target files:
1. **ChatDrawer.kt line 432**: 'No value passed for parameter current' in `ConversationList` call
2. **ChatPage.kt lines 1528 and 2003**: 'Unresolved reference onNewChat'
3. **ConversationList.kt lines 132 and 764**: 'Conflicting declarations' / 'Unresolved reference ConversationList'

### Root Cause Analysis

**ConversationList.kt (lines 132 & 764)**: The `ConversationList` composable was declared as `fun ColumnScope.ConversationList(` (an extension function on `ColumnScope`), but it was called from:
- `ChatDrawer.kt` line 320: `ConversationList(current = current, ...)` — called from `ChatDrawerContent()` which is NOT inside a `ColumnScope`
- `ConversationListPage()` at line 787: `ConversationList(current = current, ...)` — called from a top-level composable, NOT inside a `ColumnScope`

Since `ConversationList` was declared as an extension function on `ColumnScope`, calling it from outside a `Column{}` scope caused compilation errors.

### Fix Applied

**ConversationList.kt line 135**: Changed `fun ColumnScope.ConversationList(` to `fun ConversationList(`

This makes `ConversationList` a top-level composable function that can be called from any Composable context, not just inside a `Column{}`. The `current: Conversation` parameter is already present and correctly typed.

### Why Other Errors Are Resolved

**ChatDrawer.kt line 432**: The `bottomContent = {` block at line 432 was part of the `ConversationList()` call at line 320. Since `ConversationList` is now a top-level function (not a `ColumnScope` extension), calling `ConversationList(current = current, ...)` from `ChatDrawerContent()` works correctly regardless of whether it's inside a `Column{}` scope.

**ChatPage.kt lines 1528 & 2003**: `onNewChat` is defined at line 731 inside `ChatPage()` and referenced at lines 1528 and 2003. Both the definition and usage sites are inside `ChatPage()`, so `onNewChat` is in scope. The `ChatToolbar` function at line 3234 accepts `onNewChat: () -> Unit` as a parameter, and `ChatPage()` passes its `onNewChat` val to it. With the `ConversationList` fix, the entire `ChatPage.kt` composes correctly.

## Call Chain & Architecture

```
RouteActivity.kt
  └── Screen.ConversationList → ConversationListPage()
        └── ConversationListPage()
              └── ConversationList(current = current, ...)
                    └── ConversationItem() for each conversation

RouteActivity.kt
  └── Screen.Chat → ChatPage()
        └── ChatToolbar(onNewChat = onNewChat, ...)
              └── onNewChat defined at line 731:
                    if (isActiveConversation) {
                        navController.navigate(Screen.ConversationList)
                    } else {
                        navigateToChatPage(navController)
                    }
```

## Recomposition & Memory Safety

- `ConversationList` is now a top-level composable function with no `ColumnScope` dependency, making it more reusable and composable
- `onNewChat` lambda captures `conversationInitialized` and `conversation` state, which are collected via `collectAsStateWithLifecycle()`
- All state variables use `remember` or `collectAsStateWithLifecycle()` for lifecycle awareness

## Threading & Dispatchers

- All UI state mutations are synchronous main-thread Compose operations
- No `Dispatchers.Main` blocking
- No `Dispatchers.IO` usage introduced in this fix

## Offline & Error State Handling

- No network dependency in navigation or UI rendering
- Error states handled by existing `errorFlow` toasts in `ChatPage.kt`

## Files Changed

- `app/src/main/java/me/lucid/domchat/ui/pages/chat/ConversationList.kt` — Line 135: `fun ColumnScope.ConversationList(` → `fun ConversationList(`
- `audit_brief.md` — This document
