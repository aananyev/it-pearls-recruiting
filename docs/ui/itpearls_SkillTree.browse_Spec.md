# SkillTree Browse (`itpearls_SkillTree.browse`)

> Сущность: [SkillTree.md](../entities/SkillTree.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **SkillTree** HRM HuntTech: список / lookup сущности `SkillTree`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_SkillTree.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Дерево навыков. В списке — ссылки на Wikipedia и цвет приоритета; в форме — парсинг статьи Wiki в описание и превью картинки навыка.


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

### 4.1 Жизненный цикл

Edit: при инициализации — список приоритетов и слушатель имени; перед показом — дефолт notParsing; после показа — лого из Wiki-HTML.

### 4.2 Скрытые вычисления

Browse: колонка wikiPage (ссылка), prioritySkill (стиль по StandartPrioritySkills). Edit: стиль приоритета; upload превью skillPic.

### 4.3 Валидация и сохранение

Стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|---------|
| parseWikiToDescription | Нажатие → парсинг Wikipedia → заполнение RichTextArea + превью |
| Смена skillName | Диалог обновления Wiki URL |


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
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
