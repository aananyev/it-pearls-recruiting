# SkillTree Browse (`itpearls_SkillTree.browse`)

> Сущность: [SkillTree.md](../entities/SkillTree.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **SkillTree** HRM HuntTech: список / lookup сущности `SkillTree`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_SkillTree.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_SkillTree.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.skilltree.SkillTreeBrowse ` |
| **XML-дескриптор** | `skill-tree-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.skilltree` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `skillTreesTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `skillTreesTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_SkillTree.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **SkillTree** HRM HuntTech: список / lookup сущности `SkillTree`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `SkillTree` |
| **View** | `skillTree-browse-view` |
| **Data containers** | `skillTreesDc` (collection) |
| **Loader** | `skillTreesDl` |

### JPQL (если задан)

```
select e from itpearls_SkillTree e
```

### Привязки property (form / table)

- см. XML

### Колонки таблицы (browse)

- `skillTree`
- `skillName`
- `specialisation`
- `wikiPage`
- `prioritySkill`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_SkillTree.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| — | — | Стандартное поведение CUBA (`StandardLookup` / `StandardEditor`) |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `skillTreesTable.wikiPage` | `columnGenerator` | см. Java |
| `skillTreesTable.prioritySkill` | `columnGenerator` | см. Java |
| `skillTreesTable.skillName` | `descriptionProvider` | см. Java |
| `skillTreesTable.isComment` | `columnGenerator` | см. Java |
| `skillTreesTable.isComment` | `styleProvider` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `create` | standard CUBA action | — | CRUD / lookup |
| `edit` | standard CUBA action | — | CRUD / lookup |
| `remove` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`skillTreesTable`)
- Фильтр: `filter` → `skillTreesDl`
- Таблицы: `skillTreesTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.skilltree` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
