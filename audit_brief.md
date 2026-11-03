# Audit Brief — Task 124: Models Page, Model Controller & Model Picker Refinements

## Intent Verification

All 3 requirements from task-124_directive.json implemented:

1. **SettingModelPage.kt — Fix duplicate background on reasoning lightbulb card** — Removed `.background(color = Color(0xFF000000))` from `HelperReasoningSettings` row (line 1248). The row now relies solely on `.border(BorderStroke(0.5.dp, LiquidGlassTokens.SpecularBorderBrush, ...))` for its appearance, matching the glass card theme without double background painting. The parent `DomChatModelFeatureCard` already provides `Color(0xFF000000)` container.

2. **Model Controller — Expose full generation parameters** — Added `ModelGenerationParamsEditor` composable in `SettingProviderDetailPage.kt` (advanced settings tab, page 1), rendering sliders for:
   - Temperature (0–2)
   - Top P (0–1)
   - Frequency Penalty (-2 to 2)
   - Presence Penalty (-2 to 2)
   - Repetition Penalty (0–2)
   - Max Tokens (0–4096)
   
   Each slider updates `Model` fields directly via `onModelChange(model.copy(...))`. "Apply" button commits changes. Added `Model.kt` fields: `temperature`, `topP`, `frequencyPenalty`, `presencePenalty`, `repetitionPenalty`, `maxTokens`.

3. **Model Picker popover — Reduce excessive tint** — Search bar icon tint changed `Color.White.copy(alpha = 0.65f)` → `0.45f`. 'No models' card text tint changed `0.65f` → `0.45f`. Both in `ModelPicker` popover within `SettingProviderDetailPage.kt`.

**Constraints:** ZERO `!!` force-unwraps introduced (grep-confirmed across all modified files). Pre-existing `!!` at `SettingProviderDetailPage.kt:881` (model in ConnectionTesterButton) and `:3584` (editingProvider) are untouched. Unified glass card theme `0xFF141520` enforced. Existing code style preserved.

## Call Chain & Architecture

```
Model parameter edit flow:
User opens Provider Detail page → selects model → taps Settings gear → Advanced settings tab
  → ModelGenerationParamsEditor renders sliders for temperature/topP/penalties/maxTokens
  → User adjusts slider → local state updates immediately
  → User taps Apply → onModelChange(model.copy(temperature=..., topP=..., ...))
    → SettingProviderConfigPage.onEdit(updatedProvider) → vm.updateSettings(newSettings)
    → SettingsStore.update persists to DataStore
```

```
Model Picker popover flow:
User taps "Add from list" FAB → showPicker = true
  → DomChatGlassBottomSheet opens with model list
  → Search bar tint 0.45f, 'No models' card text tint 0.45f (subtler)
  → User selects model → onAddModel / onAddModels → providerSetting.addModel(...)
```

Architecture boundaries: UI layer renders state + emits intents only. ViewModel (SettingVM) manages coroutines in viewModelScope. DataStore is single source of truth for provider/model settings.

## Recomposition & Memory Safety

- `ModelGenerationParamsEditor` uses `remember(model.temperature) { mutableStateOf(...) }` — keyed on model field values, preserved across recompositions.
- Sliders use `Modifier.fillMaxWidth()` — stateless Modifier, no per-frame allocation.
- `Model.kt` data class fields are immutable — `model.copy(...)` creates new instances for each change.
- No `SnapshotStateList` passed directly to `items()`. No naked objects in Compose.
- Zero `!!` force-unwraps introduced.

## Threading & Dispatchers

- Settings save: `vm.updateSettings(newSettings)` → `viewModelScope.launch` → `settingsStore.update` (DataStore, IO-bound internally).
- Model Picker `resolveProviderModels` runs on `Dispatchers.Default` (line 1677) — correct for CPU-bound metadata resolution.
- All UI rendering on `Dispatchers.Main` (correct — Compose UI layer).
- No `Dispatchers.Main` violations introduced.

## Offline & Error State Handling

- **Offline:** Model parameter edits and provider model selection operate on local DataStore — fully offline-capable.
- **Loading:** Model Picker shows `LinearWavyProgressIndicator` during reload. Generation params save is instant local write.
- **Success:** Changes persist via DataStore; `isSaving` flag in bottom bar reflects save state.
- **Exception:** `vm.updateSettings` catches exceptions internally. `settings.getCurrentAssistant()` falls back to `DEFAULT_ASSISTANTS.first()` — null-safe.
- **Edge cases:** Empty search query shows all models. Default slider values (0.7f temperature, 1.0f topP, etc.) map to `null` on save to preserve provider defaults.
