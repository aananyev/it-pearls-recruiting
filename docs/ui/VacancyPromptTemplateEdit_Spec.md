# VacancyPromptTemplateEdit (`hunttech_VacancyPromptTemplate.edit`)

> Cross-link: [VacancyPromptTemplate.md](../entities/vacancy-prompt-template/VacancyPromptTemplate.md) · legacy: [hunttech_VacancyPromptTemplate.edit_Spec.md](../screens/vacancy-prompt-template/hunttech_VacancyPromptTemplate.edit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Форма редактирования шаблона промпта — справочника, который задаёт, «как ИИ думает» при стандартизации описаний вакансий через `HrmAiService`. Администратор задаёт уникальный технический код (по нему сервис находит шаблон), человекочитаемое название, температуру креативности и два текстовых блока: роль ИИ (системный контекст) и основную задачу (сам промпт). Правильно заполненный шаблон — залог стабильного, предсказуемого результата AI-обработки.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из `hunttech_VacancyPromptTemplate.browse` (меню **Управление AI** → «Шаблоны промптов») действиями create/edit. Родительский экран — browse; дочерних диалогов и фрагментов нет. Sidebar-навигация «Разделы» переключает фокус между двумя секциями рабочей области.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

При открытии загружается сущность с view `vacancyPromptTemplate-edit-view` (`@LoadDataBeforeShow`). Сохранение — стандартный commit `StandardEditor` (`windowCommitAndClose`), отмена — `windowClose`. Кнопки sidebar-навигации только перемещают фокус (`focusMainSection` → поле кода, `focusPromptSection` → поле системного контекста) и меняют активный пункт навигации; данные не изменяют.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_VacancyPromptTemplate.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.vacancyprompttemplate.VacancyPromptTemplateEdit` |
| **XML-дескриптор** | `vacancy-prompt-template-edit.xml` |
| **EditedEntityContainer** | `vacancyPromptTemplateDc` |
| **focusComponent** | `codeField` |
| **dialogMode** | 100%×100%, modal |

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `VacancyPromptTemplate` |
| **View** | `vacancyPromptTemplate-edit-view` (extends `_local`) |
| **Data containers** | `vacancyPromptTemplateDc` (instance, `<loader/>` без запроса) |

### Привязки property

| Компонент | property | caption | description |
|-----------|----------|---------|-------------|
| codeField | code | `templateCodeCaption` | `VacancyPromptTemplate.code.description` |
| nameField | name | `templateName.caption` | `templateNameDescription` |
| temperatureField | temperature | `templateTemperatureCaption` | `VacancyPromptTemplate.temperature.description` |
| systemContextField | systemContext | `aiRole.caption` | `aiRoleDescription` |
| promptTextField | promptText | `mainTask.caption` | `mainTaskDescription` |

Ключи `constraintsDescription`, `outputFormatDescription`, `exampleDescription` зарезервированы в `messages*.properties` пакета экрана для будущих полей формы.

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран | Способ открытия |
|-------|-------|-----------------|
| Родитель | `hunttech_VacancyPromptTemplate.browse` | create / edit action |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### §4.1 Lifecycle

Стандартный `StandardEditor<VacancyPromptTemplate>`: `@LoadDataBeforeShow` грузит сущность по id из editor-контекста. Sidebar-заголовок (label `vacancyPromptTemplateSidebarTitle`) отображает живое значение `name` из контейнера.

### §4.2 Скрытые вычисления

Нет генераторов и провайдеров. Единственная скрытая логика — управление активным пунктом навигации (presentation-only, см. §5).

### §4.3 Валидация и сохранение

Штатный commit `StandardEditor`; обязательные поля (`code`, `name` — `@NotNull` в entity) валидируются CUBA. Кастомных PreCommit/PostCommit нет.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Действие → Условие → Результат |
|---------|---------------------------------|
| `vacancyPromptTemplateMainNav` (sidebar «Основные данные») | Клик → без условий → фокус на `codeField`, пункт получает `label-nav-item-active`, у второго пункта класс снимается |
| `vacancyPromptTemplatePromptNav` (sidebar «Текст промпта») | Клик → без условий → фокус на `systemContextField`, активный класс переключается на этот пункт |
| `commitAndCloseBtn` (`windowCommitAndClose`) | Клик → валидация пройдена → сущность коммитится, форма закрывается |
| `closeBtn` (`windowClose`) | Клик → без условий → изменения отменяются, форма закрывается |

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

Двухпанельная композиция по контракту Edit-экранов HRM HuntTech:

- **Sidebar 270px** (`edit-sidebar`, тёмный `#172638→#0f1b28`): identity (`edit-sidebar-subtitle` «Шаблон промпта» + `edit-sidebar-title` из `name`) → `label-navigation` с полосой-заголовком «Разделы» (`vacancy-prompt-template-navigation-title`, §4.1 — inset-линии, `#ffb11b` 15px/700) и двумя пунктами → spacer → hint
- **Workspace** (`edit-workspace`): toolbar (`edit-toolbar-title` «Карточка шаблона» + `edit-toolbar-description`) → scrollBox (`edit-workspace-scroll`) с двумя карточками `edit-card` (`showAsPanel="true"`, радиус 8px):
  - «Основные данные» — форма `vacancyPromptTemplateForm` с 3 полями `edit-form-control` (code / name / temperature)
  - «Текст промпта» — 2 textArea `edit-form-control` (systemContext 120px/rows 5, promptText rows 15)
- **Footer** `edit-footer-actions`: кнопки commit/close прижаты к правому краю spacer-ом

Локальный SCSS partial `vacancy-prompt-template-editor.scss` подключён во всех 7 темах (sha256-идентичные копии), тёмная sidebar и каноническая навигация — по эталону `iteraction-list-visual-alignment.scss`.

---

Sidebar-иллюстрация: `ovaFallbackImage` отображается 176×176 и использует отдельный theme asset `icons/ai/vacancy-prompt-template.png` размером 200×200. Графика в фирменной чёрно-серо-бело-красной палитре HRM HuntTech объединяет HuntTech-монограмму, AI/network, prompt document и pencil, чтобы визуально показывать назначение редактора шаблона промпта.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-13 | Общий fallback `icons/hunttech-logo.png` заменён на тематическую иллюстрацию prompt template `icons/ai/vacancy-prompt-template.png`: исходный asset 200×200, отображение `ovaFallbackImage` сохранено 176×176 |
| 2026-08-13 | Размер sidebar-логотипа `ovaFallbackImage` приведён к эталону JobCandidateEdit: 176×176 (было 96×96) |
| 2026-08-12 | Рефакторинг по контракту Edit-экранов: sidebar 270px, label-навигация «Разделы» с полосой-заголовком, toolbar, карточки edit-card (showAsPanel), footer edit-footer-actions, presentation-only Java-навигация (focusMainSection/focusPromptSection), msg-ключи templateCodeCaption/templateTemperatureCaption, локальный SCSS partial в 7 темах, контракт-тест VacancyPromptTemplateEditContractTest |
| 2026-06-29 | Добавлены стандартные всплывающие подсказки (description) для полей формы на понятном для пользователей языке. |
| 2026-06-28 | `systemContextField` / `promptTextField`: отдельные caption и description (`systemContext.*`, `promptText.*`) вместо entity-ключей |
| 2026-06-28 | Многострочная подсказка `temperatureField`: диапазоны 0.0–0.3 / 0.4–0.7 / 0.8–1.0 с примерами использования (RU/EN) |
| 2026-06-28 | Контекстные description для всех полей edit-формы (RU/EN) |
| 2026-06-27 | Создание edit-экрана с крупными textArea для systemContext и promptText |