# Compose Migration Guide

## Scope
This project keeps existing XML screens and business logic as-is while introducing a Compose foundation.

## XML Screen Migration Plan
- `signup_layout.xml` -> `SignupScreen`
- `fragment_home.xml` -> `HomeScreen`
- `applist_layout.xml` + `item_app.xml` -> `AppListScreen` + `AppListItem`
- `blocked_app_list_layout.xml` + `item_app_blocked.xml` -> `BlockedAppsScreen` + `BlockedAppItem`
- `fragment_edit_profile.xml` -> `EditProfileScreen`
- `activity_block_app.xml` -> `BlockAppScreen`
- `activity_blocked.xml` -> `BlockedActivityScreen`
- Dialog XMLs (`permission_layout.xml`, `notification_dialog.xml`, etc.) -> Compose `AlertDialog`/`Dialog` equivalents
- Overlay (`overlay.xml`) -> ComposeView-backed overlay wrapper (last phase)

## Naming Conventions
- Screen composables: `FeatureScreen` (e.g., `HomeScreen`)
- Reusable UI: `LimitrXxx` prefix (e.g., `LimitrPrimaryButton`)
- State models: `FeatureUiState`
- Events/actions: `FeatureUiEvent`

## State Handling Pattern
- Keep ViewModel/repository logic unchanged.
- Expose immutable UI state (`StateFlow<UiState>` preferred, LiveData acceptable during transition).
- Collect in Compose with lifecycle-aware APIs.
- Keep one-way data flow: UI event -> ViewModel -> state update -> UI render.

## Folder Structure
- `ui/theme/` -> Compose design tokens (`Color`, `Type`, `Shape`, `Theme`)
- `ui/components/` -> reusable Compose components
- `ui/navigation/` -> Compose route definitions and future NavHost
- Existing XML/Fragment/Activity packages remain active until each screen is migrated.
