# UserAiConfiguration — AI-конфигурация пользователя

> Cross-link: [пользовательская инструкция](../../integrations/ai/USER_AI_CONNECTION_GUIDE.md) · [документация изменений](../../integrations/ai/USER_AI_SETTINGS_IMPLEMENTATION.md) · [hunttech_UserAiConfiguration.browse_Spec.md](../../screens/user-ai-configuration/hunttech_UserAiConfiguration.browse_Spec.md) · edit в [hunttech_ExtUserEdit_Spec.md](../../screens/ext-user/hunttech_ExtUserEdit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Персональные ключи и настройки AI-провайдеров (OpenAI, YandexGPT, GigaChat) для каждого пользователя HRM HuntTech.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

- Редактирование: вкладка «Персональный ИИ» в `sec$User.edit` → модаль `hunttech_UserAiConfiguration.edit`
- Мониторинг: меню **Управление AI** → `hunttech_UserAiConfiguration.browse`

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Пользователь/админ задаёт providerCode, apiKey, модель и флаг isActive; `HrmAiService` выбирает активную конфигурацию текущего пользователя при вызове AI.

---

## 1. Архитектура Сущности (Data Model Layer)

| Поле | Тип | Описание |
|------|-----|----------|
| user | FK → sec$User | Владелец |
| providerCode | String(64) | Код провайдера |
| apiKey | String(512) | API-ключ |
| defaultModelName | String(128) | Модель по умолчанию |
| isActive | Boolean | Активна |

Таблица: `HUNTTECH_USER_AI_CONFIGURATION`. Миграция: `260627-2-createUserAiConfiguration`.

---

## 2. Интерфейсный Слой (UI & Layout)

| View | Назначение |
|------|------------|
| `userAiConfiguration-browse-view` | Browse без apiKey |
| `userAiConfiguration-edit-view` | Edit (модаль) |
| `userAiConfiguration-view` | Полный _local + user |

| Экран | Controller |
|-------|------------|
| Browse (мониторинг) | `hunttech_UserAiConfiguration.browse` |
| Edit | `hunttech_UserAiConfiguration.edit` |

---

## 3. Бизнес-логика (Controller Layer)

`UserAiConfigurationEdit` — lookup провайдеров, автопривязка `user` из ExtUser edit.

---

## 4. Взаимодействие компонентов

`ExtUserEdit` и личное окно `settings` → коллекция AI configs; `HrmAiServiceBean` → чтение активной конфигурации и тестирование выбранного подключения.

---

## 5. Инструкция по развертыванию (Deployment Guide)

Миграция Liquibase `260627-1-addAiEntities.xml`; пересборка модулей.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-27 | Browse мониторинга без apiKey; view `userAiConfiguration-browse-view`; меню aiAdministration |
| 2026-06-27 | Edit-модаль в ExtUser; сущность и миграции AI |
| 2026-07-21 | Личная вкладка AI в пользовательских настройках переведена на таблицу конфигураций; добавлено тестирование подключения |
| 2026-07-21 | В личные AI-настройки добавлен провайдер DeepSeek с моделью `deepseek-v4-flash` |
