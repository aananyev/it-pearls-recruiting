# UserAiConfiguration — AI-конфигурация пользователя

> Cross-link: [пользовательская инструкция](../../integrations/ai/USER_AI_CONNECTION_GUIDE.md) · [документация изменений](../../integrations/ai/USER_AI_SETTINGS_IMPLEMENTATION.md) · [ExtSettingsWindow_Spec.md](../../ui/ExtSettingsWindow_Spec.md) · [UserAiConfigurationBrowse_Spec.md](../../ui/UserAiConfigurationBrowse_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Сущность хранит персональные ключи и настройки AI-провайдеров для пользователей HRM HuntTech. Один пользователь может иметь несколько сохранённых подключений, но системные кнопки AI-анализа всегда используют только одну текущую конфигурацию.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

- Персональная настройка API-ключей: стандартное пользовательское меню → **Настройки** → вкладка `AI` в `ExtSettingsWindow`.
- Административное редактирование конкретного пользователя: `sec$User.edit` → вкладка AI.
- Технические экраны `hunttech_UserAiConfiguration.browse/edit` сохранены, но отдельный пункт browse-экрана удалён из главного меню.
- Использование: `AiAnalysisServiceBean` → `HrmAiService.sendPromptUsingCurrentConfiguration()`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Создание конфигурации → `isActive=false` → работающий AI-провайдер не переключается автоматически.
- Выбор текущей конфигурации существующим действием → остальные конфигурации пользователя получают `isActive=false`, выбранная получает `isActive=true`.
- Нажатие AI-анализа → ищется единственная текущая конфигурация пользователя → запрос отправляется её провайдеру и модели.
- Открытие SettingsWindow → datasource фильтруется по текущему пользователю → чужие API-ключи не отображаются.

---

## 1. Архитектура Сущности (Data Model Layer)

| Поле | Тип | Описание |
|------|-----|----------|
| `user` | FK → `sec$User` | Владелец конфигурации. |
| `providerCode` | String(64) | Код реализации `AIProvider`. |
| `apiKey` | String(512) | Секретный API-ключ; не включается в browse-view. |
| `defaultModelName` | String(128) | Модель провайдера по умолчанию. |
| `isActive` | Boolean | Единственная текущая конфигурация для системного AI-анализа. |

Таблица: `HUNTTECH_USER_AI_CONFIGURATION`.

Частичный уникальный индекс `IDX_HUNTTECH_USER_AI_CFG_ONE_CURRENT` запрещает более одной неудалённой строки `IS_ACTIVE = TRUE` для одного `USER_ID`.

---

## 2. Интерфейсный Слой (UI & Layout)

| View | Назначение |
|------|------------|
| `userAiConfiguration-browse-view` | Список без `apiKey`; показывает текущий признак. |
| `userAiConfiguration-edit-view` | Редактор провайдера, ключа и модели. |
| `userAiConfiguration-view` | Полный `_local` + пользователь; используется персональным SettingsWindow. |

| Экран | Назначение |
|-------|------------|
| `settings` / `ExtSettingsWindow` | Основная пользовательская точка работы с персональными API-ключами. |
| `sec$User.edit` | Административная настройка подключений конкретного пользователя. |
| `hunttech_UserAiConfiguration.browse` | Сохранённый технический экран мониторинга и выбора текущей нейросети без пункта главного меню. |
| `hunttech_UserAiConfiguration.edit` | Редактирование подключения; текущий признак только для чтения. |

---

## 3. Бизнес-логика (Controller / Service Layer)

`UserAiConfigurationEdit` подставляет рекомендуемую модель и создаёт новую строку как нетекущую. `ExtSettingsWindow` переиспользует этот editor и `HrmAiService.testConnection`, не дублируя правила подключения к провайдерам. Core-сервис продолжает переключать текущую конфигурацию одной транзакцией.

Сценарии с явным `providerCode` могут использовать любую сохранённую конфигурацию. Признак `isActive` влияет только на системные кнопки AI-анализа.

---

## 4. Взаимодействие компонентов

`ExtSettingsWindow` / `sec$User.edit` → `UserAiConfigurationEdit` → `UserAiConfiguration`.

`OpenPositionEdit`, `CandidateCVEdit`, `IteractionListEdit` и другие экраны → `AiAnalysisHelper` → `AiAnalysisServiceBean` → `HrmAiServiceBean` → текущий `UserAiConfiguration` → `AIProvider`.

---

## 5. Инструкция по развертыванию (Deployment Guide)

Перенос навигации не меняет entity, таблицу, сервисы или миграции. Требуется пересобрать web-модуль, выполнить `ScreenViewIntegrityTest`, локальный deploy и проверить:

- открытие стандартного SettingsWindow;
- наличие вкладки AI;
- отсутствие отдельного пункта `UserAiConfiguration.browse` в главном меню;
- создание, редактирование и тестирование персонального подключения.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-22 | Персональные API-ключи закреплены в стандартном SettingsWindow; отдельный пункт `UserAiConfiguration.browse` удалён из главного меню без изменения сервисов и модели данных. |
| 2026-07-22 | `isActive` закреплён как признак единственной текущей конфигурации; добавлены транзакционное переключение и уникальный индекс. |
| 2026-07-21 | Личная вкладка AI переведена на таблицу конфигураций; добавлено тестирование подключения и DeepSeek. |
| 2026-06-27 | Созданы сущность, browse/edit views и административный экран мониторинга. |
