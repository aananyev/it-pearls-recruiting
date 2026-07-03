# VacancyPromptTemplate Browse (`hunttech_VacancyPromptTemplate.browse`)

> Сущность: [VacancyPromptTemplate.md](../entities/VacancyPromptTemplate.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Административный список шаблонов промптов для AI-обработки вакансий в HRM HuntTech. Администратор видит код, название и температуру каждого шаблона и управляет CRUD-операциями.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Меню **Управление AI** → «Шаблоны промптов». Переход в `hunttech_VacancyPromptTemplate.edit` по create/edit.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

При открытии загружается список `hunttech_VacancyPromptTemplate`. Create → новая запись в edit; Edit → выбранная строка; Remove → удаление с подтверждением CUBA.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_VacancyPromptTemplate.browse` |
| **Java-класс** | `com.company.hunttech.web.screens.vacancyprompttemplate.VacancyPromptTemplateBrowse` |
| **XML-дескриптор** | `vacancy-prompt-template-browse.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.vacancyprompttemplate` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `vacancyPromptTemplatesTable` |
| **Меню** | `web-menu.xml` → `aiAdministration` → `hunttech_VacancyPromptTemplate.browse` |
| **Загрузка данных** | `@LoadDataBeforeShow` |

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `VacancyPromptTemplate` |
| **View** | `vacancyPromptTemplate-browse-view` (`code`, `name`, `temperature`) |
| **Data containers** | `vacancyPromptTemplatesDc` (collection, readOnly) |
| **Loader** | `vacancyPromptTemplatesDl` |

### JPQL

```
select e from hunttech_VacancyPromptTemplate e
```

### Колонки таблицы

- `code`, `name`, `temperature`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран | Способ открытия |
|-------|-------|-----------------|
| Родитель | `aiAdministration` (menu) | menu |
| Парный экран | `hunttech_VacancyPromptTemplate.edit` | create / edit action |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Стандартный CUBA browse без кастомной Java-логики.

### 4.2 Скрытые вычисления

Нет.

### 4.3 Валидация и сохранение

CRUD через стандартные actions таблицы; commit в edit-экране.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Кнопка | Действие |
|--------|----------|
| createBtn | `vacancyPromptTemplatesTable.create` → edit |
| editBtn | `vacancyPromptTemplatesTable.edit` → edit |
| removeBtn | `vacancyPromptTemplatesTable.remove` |

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- `filter` → `vacancyPromptTemplatesDl`
- `groupTable` `vacancyPromptTemplatesTable` с `buttonsPanel` (create/edit/remove)
- `dialogMode` 600×900

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-27 | Создание browse-экрана AI-администрирования шаблонов промптов |
