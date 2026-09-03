# SkillTreeReestrBrowse (`hunttech_SkillTreeReestr.browse`) — Архитектурная спецификация

> Сущность: [SkillTree.md](../entities/skill-tree/SkillTree.md) · эталон реестров: [hunttech-reestr-screen](../../.agents/skills/hunttech-reestr-screen/SKILL.md) · контракт Edit-экранов: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **SkillTree** HRM HuntTech: **Реестр компетенций** — древовидное
представление справочника «Дерево компетенций» вместо плоского списка legacy-Browse
(`hunttech_SkillTree.browse`). Позволяет куратору/рекрутеру:

- обозревать иерархию компетенций (родитель → дочерние навыки) в одном экране;
- мгновенно видеть реквизиты, описание, Wiki-ссылку и логотип выбранного узла
  в сайдбаре без открытия Edit-формы;
- создавать дочерние навыки «на месте» (контекст родителя подставляется);
- выгружать плоский список в Excel, разворачивать/сворачивать дерево целиком.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

- Меню: «Справочники» (`application-hadbook`) → «Реестр компетенций» (рядом с legacy
  `hunttech_SkillTree.browse`, сохранён для совместимости lookup-выборов).
- Edit-форма узла: `hunttech_SkillTree.edit` (диалог, через `screenBuilders.editor`).
- Lookup-использование: экран наследует `StandardLookup<SkillTree>`,
  `@LookupComponent("skillTreesTreeTable")` — готов к выбору навыка из других форм.

---

## Архитектура экрана

### Композиция (Split View, контракт реестров HRM)

```
┌────────────────────────────────────────────────────────────────────────┐
│ ЛЕВЫЙ САЙДБАР (312px)         │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ                 │
│ edit-sidebar + scrollBox      │ edit-workspace (expand=tableCard)      │
│                               │ ┌────────────────────────────────────┐ │
│ • ovaFallbackImage 120px      │ │ cssLayout candidate-filter-bar     │ │
│ • 4 уровня типографики:      │ │ [Создать][Ред.][Удал.] [Действия ▾]│ │
│   название / специализация /  │ ├────────────────────────────────────┤ │
│   родитель / приоритет        │ │ Generic Filter (collapsable)       │ │
│ • Быстрые действия:           │ ├────────────────────────────────────┤ │
│   «Открыть карточку»,         │ │ treeDataGrid skillTreesTreeTable   │ │
│   «Создать дочерний навык»    │ │ (hierarchyProperty=skillTree)      │ │
│ • РЕКВИЗИТЫ НАВЫКА (grid 2к)  │ │ rowsCount                          │ │
│ • ОПИСАНИЕ НАВЫКА (comment)   │ └────────────────────────────────────┘ │
│ • ДОЧЕРНИЕ НАВЫКИ (чипы)      │                                        │
│ • ЛОГОТИП НАВЫКА (140px)      │                                        │
└───────────────────────────────┴────────────────────────────────────────┘
```

### Данные и View Integrity

- Контейнер `skillTreesDc` (view **`skillTree-reestr-browse-view`**, `cacheable=true`):
  `skillName`, `skillTree→picker`, `specialisation→picker`, `wikiPage`,
  `prioritySkill`, `notParsing`, `styleHighlighting`, `comment`, `fileImageLogo→_minimal`.
  Все геттеры, вызываемые контроллером в `updateSidebarDetails`, декларированы во view
  (правило `.cursor/rules/data-view-integrity.mdc`) — без Unfetched Attribute Access.
- JPQL загрузчика без `join fetch` (CUBA view уже подгружает needed-ассоциации;
  `left join fetch` в collection-запросе CUBA не используется).
- **Zero N+1**: чипы дочерних навыков строятся из уже загруженной
  `skillTreesDc.getItems()` (фильтр по `skillTree.id`) — дополнительных запросов нет.

### Поведение контроллера (`SkillTreeReestrBrowse.java`)

| Сценарий | Реализация |
|---|---|
| Выбор узла | `addSelectionListener` → `updateSidebarDetails` / `clearSidebarDetails` (плейсхолдеры, кнопки disabled) |
| Первый пост-загрузочный проход | `onSkillTreesDlPostLoad`: сохранить текущий выбор либо выделить первую запись |
| Открыть карточку | `screenBuilders.editor(table).editEntity(selected).withOpenMode(DIALOG)` |
| Создать дочерний | `metadata.create(SkillTree.class)` + `setSkillTree(parent)` → editor newEntity DIALOG |
| Развернуть/Свернуть всё | `treeDataGrid.expandAll()/collapseAll()` из popupButton «Действия» |
| Excel | делегирование встроенному экшену `excel` таблицы |
| Логотип 24px в колонке | `addGeneratedColumn("skillLogoColumn")` c FileDescriptorResource / fallback `icons/no-programmer.jpeg` (эталон CityReestrBrowse) |
| HTML-инъекции | `escapeHtml()` для всех значений, попадающих в htmlEnabled-label; Wiki-ссылка — экранированный href |

### Тулбар (адаптивный, эталон ProjectReestrBrowse/OpenPositionReestrBrowse)

`cssLayout candidate-filter-bar edit-card` + flexbox-группы
`left-action-buttons` / `right-action-buttons` (брейкпоинты 1440/1240/900px в
`candidate-filter-bar` уже во всех 7 темах); кнопки с классами
`candidate-btn candidate-create-btn/candidate-edit-btn/candidate-remove-btn`,
`popupButton showActionIcons=true`.

### Темизация

Экран использует только общие контрактные классы тем (`edit-sidebar*`,
`label-navigation`, `candidate-browse-grid`, `candidate-filter-bar`,
`job-candidate-avatar`, `dictionary-logo-image`) — новых SCSS-правил не требуется;
все 7 тем (hunttech-modern, -dark, -light, halo, havana, helium, hover) покрывают
композицию существующими mixin-ами.

---

## Тесты и приёмка

- `:app-web:compileJava` — зелёная.
- `ScreenViewIntegrityTest` / контрактные тесты Company/SkillTree — зелёные.
- Проверка UI: `scripts/start-app.sh --branch "$PWD"` → меню «Справочники» →
  «Реестр компетенций»: дерево разворачивается, сайдбар обновляется на выбор,
  «Создать дочерний» открывает Edit с предзаполненным родителем.

## Открытые вопросы / эволюция

- Виртуальный корень «(без родителя)» как узел дерева — не делается: CUBA
  `hierarchyProperty` сама группирует null-родителей на верхнем уровне.
- Drag&drop перестановки узлов — вне объёма v1.
