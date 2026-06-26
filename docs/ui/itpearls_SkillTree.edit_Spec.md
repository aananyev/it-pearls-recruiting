# SkillTree Edit (`itpearls_SkillTree.edit`)

> Сущность: [SkillTree.md](../entities/SkillTree.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **SkillTree** HRM HuntTech: редактирование записи сущности `SkillTree`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_SkillTree.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Подписки, actions и view контейнеры — §2–§5; Data View Integrity: атрибуты generators ⊆ view loader (см. [data-view-integrity.mdc](../../.cursor/rules/data-view-integrity.mdc)).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_SkillTree.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.skilltree.SkillTreeEdit ` |
| **XML-дескриптор** | `skill-tree-edit.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.skilltree` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `skillTreeDc` |
| **focusComponent** | `` |
| **Меню** | `web-menu.xml` → `screen="itpearls_SkillTree.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **SkillTree** HRM HuntTech: редактирование записи сущности `SkillTree`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `SkillTree` |
| **View** | `skillTree-edit-view` |
| **Data containers** | `skillTreeDc` (instance), `skillTreesDc` (collection), `specialisationDc` (collection) |
| **Loader** | `skillTreesLc` |

### JPQL (если задан)

```
select e from itpearls_SkillTree e
                    where e.skillTree is null
                    order by e.skillName
```

### Привязки property (form / table)

- `skillTree`
- `specialisation`
- `skillName`
- `prioritySkill`
- `notParsing`
- `wikiPage`
- `styleHighlighting`
- `fileImageLogo`
- `comment`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_SkillTree.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### Подписки и обработчики

| Событие / target | Метод | Логика |
|------------------|-------|--------|
| `screen` | `onInit` | см. Java |
| `fileImageSkillUpload` | `onFileImageSkillUploadFileUploadSucceed` | см. Java |
| `wikiPateField` | `onWikiPateFieldValueChange` | см. Java |
| `screen` | `onAfterShow` | см. Java |
| `screen` | `onBeforeShow` | см. Java |


### @Install (generators / providers)

| Target | Subject | Назначение |
|--------|---------|------------|
| `skillPriorityField` | `optionCaptionProvider` | см. Java |
| `skillPriorityField` | `optionStyleProvider` | см. Java |


---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Action / кнопка | id | Условие enable | Эффект |
|-----------------|-----|----------------|--------|
| `lookup` | standard CUBA action | — | CRUD / lookup |

Стандартные кнопки: `windowCommitAndClose`, `windowClose` (edit); lookup: `lookupSelectAction`, `lookupCancelAction`.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (``)
- Фильтр: `filter` → `skillTreesLc`
- Таблицы: —

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
