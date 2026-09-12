---
name: cuba-hrm-ui
description: >
  Use for reviewing, standardizing, refactoring, or creating UI screens
  in HRM HuntTech built on CUBA Platform. Improve layout, usability,
  visual consistency and reuse while preserving CUBA architecture,
  business logic, data bindings, actions, services and integrations.
---

# CUBA HRM UI

## Mission

Improve and standardize HRM HuntTech screens without changing business behavior.

For an existing screen:

**PRESERVE BEHAVIOR. IMPROVE PRESENTATION.**

For a new screen:

**FOLLOW EXISTING HRM/CUBA PATTERNS. DO NOT INVENT A PARALLEL UI ARCHITECTURE.**

This is a UI skill, not a migration, backend-refactoring or business-logic skill.

---

# 1. Project Instructions Are Authoritative

Before changing UI, inspect repository-local instructions when present:

- `AGENTS.md` and nested `AGENTS.md`;
- architecture documentation;
- Browse/Edit screen contracts;
- UI/design-system documentation;
- instructions near the target module or screen.

Repository-specific contracts override generic recommendations in this skill.

Prefer established HRM patterns over generic frontend conventions.

---

# 2. Detect And Preserve The CUBA Screen Model

Before editing, identify which API the target screen already uses.

CUBA 7+ examples:

- `Screen`;
- `StandardEditor`;
- `StandardLookup`;
- `MasterDetailScreen`;
- `ScreenFragment`;
- `@UiController`;
- `@UiDescriptor`;
- data containers/loaders.

Legacy CUBA examples:

- `AbstractWindow`;
- `AbstractEditor`;
- `AbstractLookup`;
- legacy datasources;
- `screens.xml` / `web-screens.xml`.

**Never migrate between legacy and newer CUBA screen APIs as part of a UI task.**

Preserve the screen/controller/XML/data/action architecture already used by the project.

---

# 3. Strict Scope Boundary

## Allowed

Prefer UI changes in this order:

1. **XML descriptor**
   - layout hierarchy;
   - grouping;
   - component order;
   - `VBox` / `HBox` / `GridLayout` / `Form`;
   - `GroupBox` / `TabSheet` / `ScrollBox` / `CssLayout`;
   - widths/heights;
   - spacing/margins;
   - expand ratios;
   - captions;
   - alignment;
   - `stylename`.

2. **Existing application theme**
   - SCSS/CSS;
   - reusable stylenames;
   - spacing;
   - typography;
   - borders/backgrounds;
   - table/form/button/status presentation.

3. **Java controller, presentation-only**
   - only when XML/theme cannot reasonably achieve the result;
   - minimal and localized.

## Forbidden Unless Explicitly Requested

Do not modify:

- entities or entity annotations;
- domain model;
- database schema;
- Liquibase;
- repositories / DAO;
- services or service interfaces;
- middleware;
- business rules/calculations;
- transactions;
- BPM;
- security, roles or permissions;
- REST/SOAP/external integrations;
- background jobs;
- application configuration;
- Gradle/build configuration;
- deployment configuration.

If a desired UI change appears to require forbidden scope, report the dependency instead of silently expanding the task.

---

# 4. Existing Screen Preservation Contract

For existing screens, assume current IDs, bindings and behavior are intentional.

Do not rename, remove, replace or repurpose without explicit authorization:

- component IDs;
- data container IDs;
- datasource IDs;
- loader IDs;
- property/value-source bindings;
- actions and action IDs;
- lookup/create/edit/remove actions;
- screen IDs;
- `@UiController` IDs;
- `@UiDescriptor` relationships;
- entity bindings;
- fetch plans/views;
- event subscriptions;
- lifecycle handlers;
- validation logic;
- save/commit logic;
- close/cancel logic;
- service calls;
- persistence operations;
- permission checks;
- business-event notifications.

Do not replace a working CUBA mechanism with custom Java merely to simplify styling.

Do not move business logic between XML, controller, services or entities.

After visual refactoring, preserve:

- what data is loaded;
- when it is loaded;
- edited entity semantics;
- validation;
- save/commit;
- cancel/close;
- create/edit/remove;
- lookup/selection/double-click behavior;
- permissions;
- service calls;
- navigation;
- side effects.

---

# 5. Mandatory Discovery Before Editing

For a non-trivial screen:

1. Identify screen type:
   - Browse;
   - Lookup;
   - Edit;
   - Master-detail;
   - Dialog;
   - Fragment;
   - Other.

2. Locate:
   - XML descriptor;
   - controller;
   - message bundle;
   - related fragments;
   - theme styles;
   - data containers/datasources;
   - loaders;
   - actions.

3. Inspect 2–3 closest HRM screens of the same type when available.

4. Identify existing conventions for:
   - headers;
   - toolbars;
   - filters;
   - forms;
   - tabs;
   - tables;
   - field widths;
   - spacing;
   - status display;
   - buttons;
   - dialogs.

5. Search for reusable styles/fragments/patterns before creating new ones.

Classify the target screen into:

- **A — Presentation:** may be redesigned;
- **B — CUBA plumbing:** preserve;
- **C — Business logic:** do not change.

---

# 6. Change Boundary

Before implementation, establish:

```text
SCREEN: <name>
TYPE: <Browse/Edit/...>
XML: <path>
CONTROLLER: <path>
DATA: <containers/datasources/loaders>
ACTIONS: <important actions>

PLANNED:
- layout
- grouping
- spacing
- typography
- stylenames

PRESERVE:
- business logic
- bindings
- loaders/data semantics
- actions
- persistence
- services
- permissions
```

Do not broaden a UI task into unrelated cleanup.

---

# 7. Browse / Lookup Contract

Preserve the existing CUBA browse/lookup model.

Preferred visual hierarchy:

```text
title/context
    ↓
filters/search
    ↓
toolbar/actions
    ↓
Table/DataGrid
    ↓
optional details/status
```

Preserve:

- `StandardLookup` / `AbstractLookup` behavior;
- collection containers/datasources;
- loaders;
- table/DataGrid bindings;
- lookup component semantics;
- action IDs;
- create/edit/remove;
- select/lookup actions;
- double-click;
- selection;
- permissions.

Prefer:

- one clear primary toolbar;
- compact readable filters;
- stable action placement;
- consistent table density;
- meaningful column order;
- predictable widths;
- visible business/status values.

Avoid decorative headers or cards that significantly reduce usable table area.

---

# 8. Edit Screen Contract

Preserve the existing CUBA editor model.

Preferred visual hierarchy:

```text
title/entity context
    ↓
primary information
    ↓
logical form sections
    ↓
secondary information/tabs
    ↓
save/cancel/actions
```

Preserve:

- `StandardEditor` / `AbstractEditor` behavior;
- edited entity/container/datasource;
- property bindings;
- validation;
- save/commit;
- cancel/close;
- lifecycle events;
- permissions;
- side effects;
- action IDs.

Do not convert an editor into a custom generic screen merely for visual reasons.

Prefer:

- primary business information first;
- grouping by business meaning;
- consistent labels/fields;
- tabs only for distinct or secondary information;
- stable Save/Cancel placement;
- compact enterprise density.

Avoid:

- a card around every field;
- excessive nested tabs;
- arbitrary columns unrelated to semantics;
- hiding critical fields in secondary tabs.

---

# 9. Master-Detail And Fragments

For master-detail screens:

- preserve selection-to-detail synchronization;
- preserve collection/editor bindings;
- preserve commit/discard semantics;
- change only presentation unless behavior changes are explicitly requested.

For fragments:

- preserve lifecycle;
- preserve host-screen contracts;
- reuse existing fragments when they already represent the required UI/business unit;
- do not duplicate fragment behavior in parent controllers for visual convenience.

---

# 10. Platform Fidelity

Prefer native CUBA/Vaadin components and project abstractions.

Do not introduce for ordinary screen work:

- React;
- Vue;
- Angular;
- Tailwind;
- Material UI;
- standalone frontend applications;
- iframe replacement screens;
- custom HTML replacements for standard CUBA widgets.

Do not migrate CUBA screens to another frontend stack during UI work.

Prefer normal layout containers over absolute positioning.

---

# 11. XML → SCSS → Java

For visual changes:

**XML first. SCSS/theme second. Java last.**

Do not change a controller if XML/theme can reasonably achieve the same visual result.

If Java is unavoidable:

- modify the smallest possible block;
- do not refactor unrelated code;
- do not rename unrelated methods;
- do not reorder lifecycle handlers;
- do not alter subscriptions;
- do not change persistence;
- do not change service calls;
- do not change validation;
- do not introduce business decisions into presentation code.

---

# 12. HuntTech Visual Language

HRM HuntTech is an enterprise business application.

Target:

- modern;
- professional;
- restrained;
- information-dense but readable;
- consistent;
- predictable;
- suitable for daily operational use.

Prefer:

- light background;
- clear content surfaces;
- subtle borders;
- restrained shadows if consistent with the current theme;
- consistent field/button heights;
- consistent spacing;
- clear section hierarchy;
- compact tables;
- semantic status styling.

Avoid:

- landing-page aesthetics;
- excessive gradients;
- oversized typography;
- decorative animations;
- glassmorphism;
- excessive shadows;
- huge empty spaces;
- novelty UI patterns;
- arbitrary icons;
- consumer-SaaS redesign that reduces business information density.

The result should look like a professional evolution of HRM HuntTech, not a different application.

---

# 13. Theme And Reuse Rules

Reusable presentation belongs in the existing CUBA theme/theme extension.

Preferred hierarchy:

```text
HuntTech theme
    ↓
shared SCSS variables/mixins/rules
    ↓
shared CUBA stylenames
    ↓
component-family styles
    ↓
screen-specific style only when necessary
```

Prefer `stylename` and reusable selectors for repeated visual roles.

Avoid:

- programmatic Java styling;
- repeated hardcoded colors;
- duplicate SCSS across screens;
- repeated magic spacing/width values;
- screen-local copies of shared styles.

Before adding a visual pattern ask:

- Does HRM already have it?
- Is there an existing stylename?
- Is there an existing fragment?
- Is there an existing toolbar/form/table pattern?
- Is this truly a new semantic role?

Do not over-engineer a global component system for a one-screen problem.

---

# 14. Layout Rules

Use structural relationships rather than local nudges.

Prefer:

- shared containers;
- `Form`/grid alignment for business fields;
- consistent section gaps;
- consistent insets;
- predictable field widths;
- sensible expand ratios;
- relative sizing where appropriate.

Avoid:

- arbitrary offsets;
- absolute positioning for normal business UI;
- unrelated magic widths;
- unnecessary fixed heights;
- `100%` sizing without understanding parent layout behavior.

When changing sizing, consider:

- parent layout dimensions;
- expand ratios;
- scrolling;
- tab contents;
- Table/DataGrid behavior;
- dialog dimensions.

---

# 15. Typography And Content

Use the existing application typography.

Create hierarchy with:

- size;
- weight;
- spacing;
- semantic color;
- grouping.

Do not introduce a new font family for one screen.

Preserve existing domain terminology unless the task explicitly includes copy/content changes.

If a label is long, improve layout before truncating meaningful business text.

---

# 16. Interaction And State

Preserve existing action semantics.

Keep presentation clear for:

- required fields;
- validation errors;
- disabled/read-only components;
- selected rows;
- empty states;
- permission-driven visibility;
- error notifications.

Do not add/remove confirmations, validation or behavioral states merely for visual polish.

---

# 17. New Screen Workflow

When creating a new screen:

1. Find the closest existing HRM screen of the same semantic type.
2. Determine the CUBA API used in that module.
3. Reuse its architectural pattern.
4. Follow repository Browse/Edit contracts.
5. Reuse HuntTech theme styles.
6. Use native CUBA components.
7. Prefer XML layout.
8. Use data containers/datasources/loaders/actions consistent with neighboring screens.
9. Keep controller code focused on screen behavior.
10. Do not invent a new architecture when a project pattern exists.
11. Extract fragments/shared styles only when justified by real reuse.
12. Make the result look native to HRM HuntTech.

---

# 18. Existing Screen Improvement Workflow

## Phase 0 — Discover

- read project instructions;
- detect CUBA screen API;
- find XML/controller/messages/styles;
- identify bindings/loaders/actions;
- inspect analogous screens.

## Phase 1 — Diagnose

Identify only relevant UI problems:

- weak hierarchy;
- inconsistent spacing;
- poor grouping;
- inefficient width usage;
- excessive nesting;
- confusing tabs;
- inconsistent actions;
- poor table density;
- duplicated styling.

## Phase 2 — Bound Scope

Separate presentation from CUBA plumbing and business logic.

## Phase 3 — Implement Minimally

Use:

1. XML;
2. shared/existing SCSS;
3. minimal UI-only controller code if necessary.

## Phase 4 — Check Consistency

If the issue is shared, prefer fixing/reusing the shared pattern where task scope permits.

## Phase 5 — Inspect Diff

Verify no behavior or architecture escaped the UI boundary.

---

# 19. Build And Runtime Policy

Do not start the application, run a full Gradle build, modify Gradle files or troubleshoot
the runtime environment unless the task explicitly assigns that responsibility.

For ordinary UI work:

- static code/XML inspection is mandatory;
- final diff inspection is mandatory.

If runtime verification is explicitly requested, use the project's established CUBA/Gradle
workflow.

An unrelated build/environment failure is not permission to repair backend, Gradle or infrastructure.

---

# 20. Mandatory Git Diff Safety Gate

Before finishing:

```bash
git status --short
git diff --name-only
git diff --stat
git diff
```

Verify no accidental changes to:

- entities;
- services;
- repositories;
- migrations;
- security;
- BPM;
- integrations;
- build/configuration;
- unrelated screens.

For changed existing controllers verify:

- persistence unchanged;
- service calls unchanged;
- actions unchanged;
- lifecycle semantics unchanged;
- data loading semantics unchanged;
- validation unchanged;
- permissions unchanged.

If an out-of-scope change exists only because of UI work, revert it.

---

# 21. Review Checklist

## Architecture

- Correct CUBA API identified?
- Existing screen/controller model preserved?
- No legacy/new API migration?
- No new frontend framework?

## Business Safety

- Bindings preserved?
- Loaders/data semantics preserved?
- Actions preserved?
- Persistence/service calls preserved?
- Validation preserved?
- Permissions preserved?
- Lifecycle preserved?

## UI Quality

- Similar HRM screens inspected?
- Existing styles/patterns reused?
- Hierarchy clearer?
- Fields grouped by business meaning?
- Spacing/alignment consistent?
- Tables/forms compact and readable?
- Tabs justified?
- Result professional and native to HRM?

## Implementation

- XML preferred over Java?
- Shared theme preferred over local styling?
- No unrelated cleanup?
- Final `git diff` inspected?

---

# 22. Anti-Patterns

Do not:

- clean up business code while polishing UI;
- rename old IDs merely because names look untidy;
- remove loaders/actions because they appear unused;
- migrate CUBA APIs during visual work;
- move XML behavior into Java for convenience;
- rebuild standard CUBA widgets in custom HTML;
- introduce another frontend framework;
- create unique styles for every screen;
- use absolute positioning for ordinary forms;
- add huge decorative cards/headers;
- reduce enterprise information density to imitate consumer SaaS;
- change the data model because it would simplify display;
- modify Gradle/infrastructure because verification failed.

---

# 23. Completion Summary

For non-trivial work, finish with:

```text
Changed:
- <XML/layout>
- <theme/style>
- <UI-only controller changes, if any>

Preserved:
- business logic
- data bindings/loaders
- actions
- persistence/services
- permissions

Validation:
- final diff inspected
- no out-of-scope backend/build changes detected
```

If behavior could not be confidently preserved, say so explicitly.

---

# Core Rule

**CUBA architecture is a constraint to preserve, not an obstacle to remove.**

Make HRM HuntTech screens clearer, more consistent and more professional while preserving
the existing platform model and business behavior.
