# UI Spec: `sec$User.edit` / `ExtUserEdit`

> Каноническая living-спецификация: [docs/ui/ExtUserEdit_Spec.md](../../ui/ExtUserEdit_Spec.md)  
> Нормативы: [общий Edit-контракт](../../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md) · [UI/UX-концепция](../../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-10 | Блок «Профиль» sidebar: заголовок-полоса 1:1 с «Разделы» (контракт §4.1) + основная информация из вкладки «Общие настройки» — статус, Email, должность; `ExtUserEditor` заполняет sidebar-лейблы (ФИО, login, статус, Email, должность) из `userDs` (`refreshProfileLabels`, presentation-only). Контракт-тест 14/14. См. канонический `docs/ui/ExtUserEdit_Spec.md`. |
| 2026-08-10 | Sidebar приведён к эталону OpenPositionEdit (растянут на всю высоту окна с вертикальной прокруткой): root layout `spacing="false"` + `width/height="100%"`, у `profilePanel` убран `margin="true"`, `profilePanelSpacer` — vbox `100%×100%`, footer перенесён внутрь `userWorkspace`. См. канонический `docs/ui/ExtUserEdit_Spec.md`. |
| 2026-07-29 | Legacy-спецификация синхронизирована с каноническим документом: `ExtUserEdit` приведён к общему `edit-*`/`label-*` контракту без изменения бизнес- и CUBA-контрактов. |
| 2026-07-29 | Восстановлена стандартная смена пароля через `sec$User.changePassword`. |
| 2026-06-29 | `userPic` привязан к `userDs.officialPhoto`. |

## Назначение и бизнес-смысл (What & Why)

`ExtUserEdit` — административная форма HRM HuntTech для управления профилем пользователя, ролями, замещениями, почтовыми параметрами и персональными AI-подключениями. Постоянная sidebar удерживает контекст редактируемого пользователя, а правая область организует данные по единому визуальному контракту Edit-экранов.

Визуальная миграция не меняет entity, права, password policy, загрузку, сохранение, роли, замещения и CRUD AI-конфигураций.

## UI Context & Navigation

- **Screen ID:** `sec$User.edit`.
- **Вход:** `sec$User.browse` → Create/Edit.
- **Controller:** `ExtUserEditor extends UserEditor`.
- **Дочерние окна:** `sec$User.changePassword`, `hunttech_UserAiConfiguration.edit`.
- **Sidebar navigation:** отдельный `label-navigation` для общей, почтовой и AI-вкладки.
- **Каноническое техническое описание:** [ExtUserEdit_Spec.md](../../ui/ExtUserEdit_Spec.md).

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть экран | пользователь загружен `userDs` | отображается sidebar и workspace, стандартный lifecycle не меняется; sidebar «Профиль» заполняется: ФИО, login, статус, Email, должность |
| сменить вкладку | выбран новый tab | показывается соответствующий navigation-набор |
| нажать navigation | секция активной вкладки | accordion при необходимости раскрывается, focus переводится к control, данные не меняются |
| сменить пароль | пользователь сохранён | открывается стандартный `sec$User.changePassword` |
| сохранить/отменить | завершение работы | выполняются штатные actions `UserEditor` |

## 1. Точка вызова и контекст

- XML: `modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml`.
- Java: `modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java`.
- Root stylename: `ext-user-editor`.
- Общий API: `edit-*`, `label-navigation`, `label-nav-*`.

## 2. Связь с моделью данных

Сохранены `userDs`, `rolesDs`, `substitutionsDs`, `userAiConfigsDs`, views, JPQL, component ID, datasource/property bindings, validators, actions и `invoke`. Новые entity getters и изменения `views.xml` отсутствуют.

## 3. Иерархия форм

```text
sec$User.browse
└── sec$User.edit
    ├── sec$User.changePassword
    └── hunttech_UserAiConfiguration.edit
```

## 4. Модель поведения

`ExtUserEditor.postInit()` выполняет только presentation-задачи: применяет `edit-form-control` к созданным стандартным контроллером полям и синхронизирует видимость navigation с вкладкой. Navigation не вызывает `setSelectedTab`, load, refresh, commit и не меняет entity или selection. Почтовая секция раскрывается через `setExpanded(true)` перед focus.

## 5. Actions & Buttons Logic

- `changePasswordBtn` сохраняет `invoke="changePassword"`.
- navigation-кнопки меняют focus и `label-nav-item-active`.
- role/substitution actions и AI listeners сохранены.
- Save/Cancel выполняет `editWindowActions`.

## 6. Визуальная компоновка

```text
sidebar.edit-sidebar (270/250 px)
├── edit-sidebar-visual
├── edit-sidebar-identity
├── label-navigation
└── edit-sidebar-summary

workspace.edit-workspace
├── edit-toolbar
├── edit-tabs
│   ├── edit-card
│   ├── edit-accordion-section × 5
│   └── edit-card
└── edit-footer-actions
```

Локальный `ext-user-editor.scss` одинаков во всех семи темах, ограничен `.ext-user-editor` и подключён после общего `edit-screen-shared-styles`. Обоснованные отклонения: сохранены `fallbackImage`, три вкладки и `rolesSubstSplit`; кнопка смены пароля только визуально перенесена в toolbar.

---

Обязательные UI-документы прочитаны. Общие `edit-*` и `label-*` stylename использованы преимущественно. Локальные отклонения перечислены и обоснованы. Бизнес- и CUBA-контракты формы сохранены.
