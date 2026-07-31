# UI Spec: `sec$User.edit` / `ExtUserEdit`

> Канонический документ: `docs/ui/ExtUserEdit_Spec.md`  
> Legacy-зеркало: `docs/screens/ext-user/hunttech_ExtUserEdit_Spec.md`  
> Нормативы: [общий Edit-контракт](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md) · [UI/UX-концепция](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-29 | Экран приведён к общему `edit-*`/`label-*` контракту: фиксированная sidebar, вкладочная label-навигация, toolbar, карточки, почтовые accordion-секции и footer; bindings и бизнес-логика сохранены. |
| 2026-07-29 | Восстановлена стандартная смена пароля через `sec$User.changePassword`. |
| 2026-06-29 | `userPic` привязан к `userDs.officialPhoto`; удалено ручное переключение fallback. |

## Назначение и бизнес-смысл (What & Why)

`ExtUserEdit` — административная форма HRM HuntTech для управления учётной записью пользователя: профилем, контактами, региональными параметрами, ролями, замещениями, email и персональными AI-подключениями. Единая компоновка снижает риск ошибки администратора: редактируемый пользователь постоянно идентифицируется в sidebar, а действия и данные размещены в предсказуемой рабочей области.

Визуальная миграция не меняет модель пользователя, права, password policy, алгоритм сохранения, роли, замещения и CRUD AI-конфигураций.

## UI Context & Navigation

- **Screen ID:** `sec$User.edit`.
- **Вход:** `sec$User.browse` → Create/Edit/двойной клик.
- **Контроллер:** `ExtUserEditor extends UserEditor`.
- **Дочерние окна:** `sec$User.changePassword`, `hunttech_UserAiConfiguration.edit`.
- **Вкладки:** общие настройки, email, персональный ИИ.
- **Sidebar:** visual → identity → navigation активной вкладки → summary.
- **Navigation:** фокусирует существующую секцию; почтовую секцию предварительно раскрывает.

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть сохранённого пользователя | `userDs` valid | стандартный `UserEditor` загружает пользователя, роли и замещения; отображается общий layout |
| открыть нового пользователя | `PersistenceHelper.isNew` | кнопка отдельной смены пароля скрыта; используются штатные `passw`/`confirmPassw` |
| сменить вкладку | выбран новый tab | показывается только соответствующий navigation-набор; данные не меняются |
| нажать navigation-пункт | секция принадлежит активной вкладке | меняется keyboard focus и `label-nav-item-active`; tab/entity/modified-state не меняются |
| выбрать почтовую секцию | accordion свёрнут | выполняется `setExpanded(true)`, затем focus первого поля |
| сменить пароль | пользователь сохранён | открывается стандартный `sec$User.changePassword` |
| Save/Cancel | завершение редактирования | выполняются штатные actions `editWindowActions` и lifecycle `UserEditor` |

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| XML | `modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml` |
| Controller | `modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java` |
| Base editor | `com.haulmont.cuba.gui.app.security.user.edit.UserEditor` |
| Root namespace | `ext-user-editor` |
| Общий UI API | `edit-*`, `label-navigation`, `label-nav-*` |

## 2. Связь с моделью данных

| Datasource | Контракт |
|---|---|
| `userDs` | `User`, view `extUser-view` |
| `rolesDs` | `userRoles` |
| `substitutionsDs` | `substitutions` |
| `userAiConfigsDs` | `userAiConfiguration-view`, `e.user = :ds$userDs` |

Сохранены `fieldGroupLeft`, `fieldGroupRight`, все почтовые FieldGroup, `rolesTable`, `substTable`, `aiConfigsTable`, component ID, datasource/property, validators, actions и `invoke`. Новые entity getters не добавлены; `views.xml` и JPQL не изменены.

## 3. Иерархия форм

```text
sec$User.browse
└── sec$User.edit (ExtUserEditor → UserEditor)
    ├── sec$User.changePassword
    └── hunttech_UserAiConfiguration.edit
```

## 4. Модель поведения

### Lifecycle

1. `UserEditor.init()` создаёт штатные actions и custom controls.
2. `UserEditor.postInit()` применяет платформенные ограничения.
3. `ExtUserEditor.postInit()` скрывает password-dialog для новой сущности, назначает созданным полям `edit-form-control` и синхронизирует navigation активной вкладки.
4. Commit/cancel полностью остаются в базовом редакторе.

### Presentation-навигация

Navigation-пункты объявлены в XML как keyboard-доступные borderless-кнопки. Контроллер:

- не вызывает `setSelectedTab()`;
- не выполняет load, refresh или commit;
- не изменяет datasource, entity, selection и значения;
- раскрывает только выбранный email accordion;
- переводит focus;
- добавляет/удаляет только `label-nav-item-active`, сохраняя `label-nav-item`.

## 5. Actions & Buttons Logic

| Элемент | Цепочка |
|---|---|
| `changePasswordBtn` | `invoke="changePassword"` → `sec$User.changePassword` |
| `generalContactsNav` | focus login |
| `generalRegionalNav` | focus language lookup |
| `generalRolesNav` | focus `rolesTable` |
| `generalSubstitutionsNav` | focus `substTable` |
| `email*Nav` | раскрыть секцию → focus первого поля |
| `aiConfigurationsNav` | focus `aiConfigsTable` |
| role/substitution/AI buttons | существующие actions/listeners без изменения |
| `windowActions` | штатные Save/Cancel |

## 6. Визуальная компоновка

```text
layout.ext-user-editor
├── mainSplit.edit-screen-layout
│   ├── profilePanel.edit-sidebar (270 px; 250 px ≤1366)
│   │   ├── dropZone.edit-sidebar-visual
│   │   ├── profileLabelsVBox.edit-sidebar-identity
│   │   ├── general/email/ai navigation.label-navigation
│   │   ├── profileSummaryBox.edit-sidebar-summary
│   │   └── profilePanelSpacer.edit-sidebar-spacer
│   └── userWorkspace.edit-workspace
│       ├── userToolbar.edit-toolbar
│       └── settingsTabSheet.edit-tabs
│           ├── general: edit-card
│           ├── email: пять edit-accordion-section
│           └── AI: edit-card
└── bottomActionsBox.edit-footer-actions
```

### Общие stylename

| Роль | Stylename |
|---|---|
| root/sidebar | `edit-screen-layout`, `edit-sidebar`, `edit-sidebar-visual`, `edit-sidebar-identity`, `edit-sidebar-title`, `edit-sidebar-subtitle`, `edit-sidebar-summary`, `edit-sidebar-hint`, `edit-sidebar-spacer` |
| navigation | `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active` |
| workspace | `edit-workspace`, `edit-toolbar`, `edit-toolbar-title`, `edit-toolbar-description`, `edit-toolbar-actions`, `edit-tabs` |
| content/footer | `edit-card`, `edit-card-title`, `edit-help`, `edit-form-control`, `edit-accordion-section`, `edit-footer-actions` |

### Email accordion

| Section | Начальное состояние |
|---|---|
| `propertiesEmailBox` — серверы | раскрыта |
| `emailPortsBox` — порты | свёрнута |
| `emailAuthenticationBox` — требование пароля | свёрнута |
| `emailAccountsBox` — учётные записи | свёрнута |
| `emailPasswordsBox` — пароли | свёрнута |

### Локальный SCSS

`ext-user-editor.scss` синхронно присутствует в семи темах и подключается после `edit-screen-shared-styles`. Он ограничен `.ext-user-editor` и задаёт только уникальную тёмную sidebar, контраст navigation и защиту внутренних Vaadin-контейнеров от переполнения. Общие размеры cards/controls/toolbar/tabs/footer не копируются.

### Обоснованные отклонения

1. `fallbackImage` 180×180 сохранён, чтобы не смешивать style-only миграцию с заменой image-компонента и upload-контракта.
2. Три вкладки сохранены как разные административные контексты; navigation работает только внутри активной вкладки.
3. `rolesSubstSplit` сохранён для одновременного сравнения ролей и замещений.
4. Кнопка смены пароля перенесена в toolbar без изменения ID, caption, icon и `invoke`.

## 7. Проверки

`ExtUserEditSharedStyleContractTest` защищает общий stylename API, XML bindings/actions, presentation-only navigation, пять accordion-секций, XML comments и одинаковый локальный SCSS семи тем. Дополнительно сохраняется `ExtUserChangePasswordContractTest`; Hermes выполняет `ScreenViewIntegrityTest 8/8`, SCSS build, clean assemble, local deploy, HTTP 200 и browser smoke.

---

Обязательные UI-документы прочитаны. Общие `edit-*` и `label-*` stylename использованы преимущественно. Локальные отклонения перечислены и обоснованы. Бизнес- и CUBA-контракты формы сохранены.
