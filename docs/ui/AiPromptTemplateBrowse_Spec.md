# AiPromptTemplateBrowse — системные промпты AI

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

Экран позволяет администратору управлять системными промптами HRM HuntTech, используемыми кнопками AI-анализа резюме, вакансий и взаимодействий. Локализованные заголовки таблицы исключают неоднозначность при работе пользователей в русской и английской локали.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из административного меню AI-настроек. Из списка доступны создание, редактирование и удаление `AiPromptTemplate`; действия создания и редактирования открывают `AiPromptTemplateEdit`.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие списка → CUBA определяет текущую локаль → captions колонок читаются из `messages.properties` или `messages_ru.properties`.
- Создание/изменение → открывается редактор системного промпта → после сохранения таблица получает актуальные данные.
- Загрузка списка → loader сортирует записи по `code` → системные промпты отображаются в стабильном порядке.

## 1. Data Containers и Views

| Элемент | Конфигурация | Назначение |
|---|---|---|
| `aiPromptTemplatesDc` | `CollectionContainer<AiPromptTemplate>` | Набор системных промптов. |
| View | `aiPromptTemplate-browse-view` | Загружает `name`, `code`, `entityClass`, `active` без тяжёлых текстовых полей. |
| Таблица | `aiPromptTemplatesTable` | Отображает коллекцию и предоставляет стандартные CRUD actions. |

## 2. Loaders и Query

`aiPromptTemplatesDl` выполняет:

```jpql
select e from hunttech_AiPromptTemplate e order by e.code
```

Параметры отсутствуют. Стабильная сортировка по коду упрощает сопоставление записей с `RESUME_ANALYSIS`, `VACANCY_ANALYSIS` и `INTERACTION_ANALYSIS`.

## 3. Lifecycle и Controller

Экран использует стандартный lifecycle CUBA browse-экрана и не содержит собственного Java-контроллера. Данные загружаются перед показом стандартным механизмом screen data.

## 4. Actions и Бизнес-поведение

| Action | Результат |
|---|---|
| `create` | Создаёт новый системный промпт в `AiPromptTemplateEdit`. |
| `edit` | Открывает выбранный промпт в редакторе. |
| `remove` | Выполняет стандартное удаление выбранной записи. |

## 5. Layout и Локализация

Колонки имеют явные captions:

- `msg://AiPromptTemplate.name`;
- `msg://AiPromptTemplate.code`;
- `msg://AiPromptTemplate.entityClass`;
- `msg://AiPromptTemplate.active`.

Русская локаль получает русские заголовки, английская — английские. Экран не зависит от смешанной legacy-локализации entity metadata.

## 6. Проверки и Критерии приёмки

- В русской локали отображаются «Название», «Код», «Класс сущности», «Активен».
- В английской локали отображаются `Name`, `Code`, `Entity class`, `Active`.
- CRUD actions открываются и выполняются без XML/lifecycle ошибок.
- `ScreenViewIntegrityTest` проходит 8/8.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | Добавлены явные локализованные заголовки таблицы и сортировка по коду. |
