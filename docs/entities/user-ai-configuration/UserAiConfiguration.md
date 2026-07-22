# UserAiConfiguration — AI-конфигурация пользователя

> Cross-link: [пользовательская инструкция](../../integrations/ai/USER_AI_CONNECTION_GUIDE.md) · [документация изменений](../../integrations/ai/USER_AI_SETTINGS_IMPLEMENTATION.md) · [UserAiConfigurationBrowse_Spec.md](../../ui/UserAiConfigurationBrowse_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Сущность хранит персональные ключи и настройки AI-провайдеров для пользователей HRM HuntTech. Один пользователь может иметь несколько сохранённых подключений, но системные кнопки AI-анализа всегда используют только одну текущую конфигурацию.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

- Редактирование: вкладка «Персональный ИИ» пользователя или модаль `hunttech_UserAiConfiguration.edit`.
- Мониторинг и выбор текущей нейросети: меню **Управление AI** → `hunttech_UserAiConfiguration.browse`.
- Использование: `AiAnalysisServiceBean` → `HrmAiService.sendPromptUsingCurrentConfiguration()`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Создание конфигурации → `isActive=false` → работающий AI-провайдер не переключается автоматически.
- Выбор строки в browse → действие «Использовать для AI-анализа» → остальные конфигурации пользователя получают `isActive=false`, выбранная получает `isActive=true`.
- Нажатие AI-анализа → ищется единственная текущая конфигурация пользователя → запрос отправляется её провайдеру и модели.

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
| `userAiConfiguration-view` | Полный `_local` + пользователь. |

| Экран | Назначение |
|-------|------------|
| `hunttech_UserAiConfiguration.browse` | Мониторинг, тест и выбор текущей нейросети. |
| `hunttech_UserAiConfiguration.edit` | Редактирование подключения; текущий признак только для чтения. |

---

## 3. Бизнес-логика (Controller / Service Layer)

`UserAiConfigurationEdit` подставляет рекомендуемую модель и создаёт новую строку как нетекущую. `UserAiConfigurationBrowse` передаёт выбор в `HrmAiService.setCurrentConfiguration(UUID)`. Core-сервис переключает строки одной транзакцией.

Сценарии с явным `providerCode` могут использовать любую сохранённую конфигурацию. Признак `isActive` влияет только на системные кнопки AI-анализа.

---

## 4. Взаимодействие компонентов

`OpenPositionEdit`, `CandidateCVEdit`, `IteractionListEdit` и другие экраны → `AiAnalysisHelper` → `AiAnalysisServiceBean` → `HrmAiServiceBean` → текущий `UserAiConfiguration` → `AIProvider`.

---

## 5. Инструкция по развертыванию (Deployment Guide)

При обновлении применяется `270722-002-enforceCurrentAiConfiguration.sql`: существующие активные строки нормализуются до одной на пользователя, затем создаётся частичный уникальный индекс. После миграции требуется пересборка core/web и локальный smoke-test выбора провайдера.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-22 | `isActive` закреплён как признак единственной текущей конфигурации; добавлены транзакционное переключение и уникальный индекс. |
| 2026-07-21 | Личная вкладка AI переведена на таблицу конфигураций; добавлено тестирование подключения и DeepSeek. |
| 2026-06-27 | Созданы сущность, browse/edit views и административный экран мониторинга. |
