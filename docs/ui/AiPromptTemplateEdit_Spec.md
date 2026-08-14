# AiPromptTemplateEdit — редактор системного промпта

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

Редактор позволяет администратору безопасно изменять название, код, класс сущности, текст системного промпта и список подстановок без изменения исходного кода HRM HuntTech.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из `AiPromptTemplateBrowse` действиями создания или редактирования. Успешное сохранение закрывает модальный редактор и возвращает пользователя в список системных промптов.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → `aiPromptTemplateDc` загружается с edit-view → поля формы получают значения сущности.
- Длинный текст промпта → прокручивается только содержимое формы → кнопки OK и Cancel остаются видимыми.
- Сохранение → обязательные поля проходят CUBA validation → изменения записываются и редактор закрывается.
- Отмена → изменения DataContext не коммитятся → редактор закрывается.

## 1. Data Container и View

| Элемент | Конфигурация | Назначение |
|---|---|---|
| `aiPromptTemplateDc` | `InstanceContainer<AiPromptTemplate>` | Редактируемая запись. |
| Loader | instance loader без собственного JPQL | Стандартная загрузка editor entity. |
| View | `aiPromptTemplate-edit-view` | Загружает все поля, используемые формой. |

## 2. Поля и Bindings

| Component | Property | Обязательность |
|---|---|---|
| `nameField` | `name` | Да |
| `codeField` | `code` | Да |
| `entityClassField` | `entityClass` | Да |
| `descriptionField` | `description` | Нет |
| `promptTextField` | `promptText` | Да |
| `availablePlaceholdersField` | `availablePlaceholders` | Нет |
| `activeField` | `active` | Нет |

## 3. Lifecycle и Validation

Экран использует стандартный `StandardEditor` lifecycle. До commit CUBA валидирует обязательные поля. Дополнительной Java-бизнес-логики и скрытых преобразований текста нет.

## 4. Actions

| Action | Результат |
|---|---|
| `windowCommitAndClose` | Валидирует, сохраняет и закрывает редактор. |
| `windowClose` | Закрывает редактор без commit. |

Панель `editActions` находится вне `contentScrollBox`, поэтому действия доступны независимо от длины промпта.

## 5. Layout и Локализация

- Диалог: 900×800 px.
- Корневой layout: 100% высоты и ширины, expand только `contentScrollBox`.
- Форма: `captionPosition="TOP"`, поэтому длинные подписи не уменьшают ширину контролов.
- Все captions берутся из локализованных screen messages.
- `promptTextField` имеет 14 строк, `availablePlaceholdersField` — 5 строк.

## 6. Проверки и Критерии приёмки

- Русские и английские подписи соответствуют текущей локали.
- Подписи полностью читаются и не перекрывают controls.
- OK и Cancel видны при открытии и после прокрутки формы.
- Создание, редактирование, сохранение и отмена работают.
- `ScreenViewIntegrityTest` проходит 8/8.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-22 | Переработана компоновка: верхние подписи, прокрутка содержимого, постоянно видимые OK и Cancel. |
