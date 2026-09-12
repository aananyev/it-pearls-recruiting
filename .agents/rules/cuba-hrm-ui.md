---
description: >
  Persistent HRM HuntTech guardrail. For any task that creates, changes,
  reviews, standardizes, or fixes visible CUBA Platform screens, layouts,
  forms, tabs, tables, dialogs, fragments, XML descriptors, theme/SCSS,
  or presentation-only controller code, load and follow the workspace
  skill cuba-hrm-ui before editing.
trigger: always_on
---

# HRM HuntTech — CUBA UI Guardrail

For any task involving visible UI in HRM HuntTech:

1. Load and follow `.agents/skills/cuba-hrm-ui/SKILL.md` before editing.
2. Treat repository-specific UI/Browse/Edit contracts as authoritative.
3. For existing screens: **PRESERVE BEHAVIOR. IMPROVE PRESENTATION.**
4. Prefer changes in this order: **XML descriptor → existing SCSS/theme → minimal presentation-only Java**.
5. Do not modify business logic, entities, services, repositories, database/Liquibase,
   security, permissions, integrations, BPM, Gradle/build configuration, or deployment
   configuration unless the user explicitly requests that separate scope.
6. Preserve existing component IDs, screen IDs, data containers/datasources, loaders,
   actions, bindings, validation, persistence semantics, lifecycle behavior, service calls,
   permissions, lookup/selection behavior, and side effects.
7. Do not migrate between legacy and newer CUBA screen APIs during a UI task.
8. Do not introduce React, Vue, Angular, Tailwind, Material UI, or another frontend stack
   as a replacement for normal CUBA/Vaadin UI.
9. Before non-trivial edits, inspect the target XML/controller/styles and analogous HRM
   screens. Establish the change boundary before implementation.
10. Before finishing, inspect `git diff` and revert accidental out-of-scope changes.

For tasks unrelated to visible UI, do not force this skill into the task. This rule's purpose
is to protect the UI boundary, not to change how backend tasks are performed.
