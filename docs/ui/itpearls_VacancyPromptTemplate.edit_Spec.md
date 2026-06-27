# VacancyPromptTemplate Edit (`itpearls_VacancyPromptTemplate.edit`)

> Сущность: [VacancyPromptTemplate.md](../entities/VacancyPromptTemplate.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Форма редактирования шаблона промпта для стандартизации вакансий через AI (`HrmAiService`). Администратор задаёт код, название, температуру, системный контекст и основной текст промпта.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из `itpearls_VacancyPromptTemplate.browse` (create/edit) или программно.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

При открытии загружается сущность с view `vacancyPromptTemplate-edit-view`. Сохранение → стандартный commit `StandardEditor`.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_VacancyPromptTemplate.edit` |
| **Java-класс** | `com.company.itpearls.web.screens.vacancyprompttemplate.VacancyPromptTemplateEdit` |
| **XML-дескриптор** | `vacancy-prompt-template-edit.xml` |
| **EditedEntityContainer** | `vacancyPromptTemplateDc` |
| **focusComponent** | `codeField` |
| **dialogMode** | 800×960 |

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `VacancyPromptTemplate` |
| **View** | `vacancyPromptTemplate-edit-view` (extends `_local`) |
| **Data containers** | `vacancyPromptTemplateDc` (instance) |

### Привязки property

| Компонент | property | caption key | description key |
|-----------|----------|-------------|-------------------|
| codeField | code | (entity) | `VacancyPromptTemplate.code.description` |
| nameField | name | (entity) | `VacancyPromptTemplate.name.description` |
| temperatureField | temperature | (entity) | `VacancyPromptTemplate.temperature.description` |
| systemContextField | systemContext | `systemContext.caption` | `systemContext.description` |
| promptTextField | promptText | `promptText.caption` | `promptText.description` |

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран | Способ открытия |
|-------|-------|-----------------|
| Родитель | `itpearls_VacancyPromptTemplate.browse` | create / edit action |

---

## 4. Модель поведения и интерактивность (Behavior Model)

Стандартный `StandardEditor` без кастомных `@Subscribe`.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Кнопка | Действие |
|--------|----------|
| commitAndCloseBtn | `windowCommitAndClose` |
| closeBtn | `windowClose` |

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- Верхняя `form` (3 колонки): code, name, temperature — у каждого поля атрибут `description="msg://VacancyPromptTemplate.*.description"`; подсказка температуры — многострочная с тремя диапазонами (точность / баланс / креативность)
- `systemContextField`: textArea, caption `systemContext.caption`, description `systemContext.description`, width 100%, height 120px, rows 5
- `promptTextField`: textArea, caption `promptText.caption`, description `promptText.description`, width 100%, rows 15, expand layout
- `editActions`: commit / close

Локализация подсказок: `messages.properties` / `messages_ru.properties` пакета экрана (`com.company.itpearls.web.screens.vacancyprompttemplate`).

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-28 | `systemContextField` / `promptTextField`: отдельные caption и description (`systemContext.*`, `promptText.*`) вместо entity-ключей |
| 2026-06-28 | Многострочная подсказка `temperatureField`: диапазоны 0.0–0.3 / 0.4–0.7 / 0.8–1.0 с примерами использования (RU/EN) |
| 2026-06-28 | Контекстные description для всех полей edit-формы (RU/EN) |
| 2026-06-27 | Создание edit-экрана с крупными textArea для systemContext и promptText |
