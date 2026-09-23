# UserAiConfiguration Browse (`hunttech_UserAiConfiguration.browse`)

> Сущность: [UserAiConfiguration.md](../../entities/user-ai-configuration/UserAiConfiguration.md) · edit: модаль `hunttech_UserAiConfiguration.edit` в [hunttech_ExtUserEdit_Spec.md](../ext-user/hunttech_ExtUserEdit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран мониторинга персональных AI-конфигураций пользователей: какой провайдер, модель и активность без отображения API-ключа. Дополняет редактирование ключей на вкладке «Персональный ИИ» в профиле пользователя.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Меню **Управление AI** → «Мониторинг ключей пользователей». Создание из реестра открывает `UserAiConfigurationEdit` и назначает владельцем пользователя текущей сессии до commit.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие экрана → загружается список всех `hunttech_UserAiConfiguration` без API-ключа.
- Нажатие «Создать» → текущая сессия содержит пользователя → новый объект получает `user` до открытия редактора.
- Сохранение без владельца → защитная проверка редактора → commit блокируется до обращения к БД.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_UserAiConfiguration.browse` |
| **Java-класс** | `com.company.hunttech.web.screens.useraiconfiguration.UserAiConfigurationBrowse` |
| **XML-дескриптор** | `user-ai-configuration-browse.xml` |
| **Базовый класс** | `StandardLookup` |
| **Меню** | `web-menu.xml` → `aiAdministration` → `hunttech_UserAiConfiguration.browse` |

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `UserAiConfiguration` |
| **View** | `userAiConfiguration-browse-view` (без `apiKey`) |
| **Nested paths** | `user.login` (user `_minimal`) |
| **Data containers** | `userAiConfigurationsDc` |
| **Loader** | `userAiConfigurationsDl` |

### JPQL

```
select e from hunttech_UserAiConfiguration e
```

### Колонки таблицы

- `user.login`, `providerCode`, `isActive`, `defaultModelName`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран | Способ открытия |
|-------|-------|-----------------|
| Родитель | `aiAdministration` (menu) | menu |
| Создание/редактирование | `hunttech_UserAiConfiguration.edit` | из реестра, ExtUser edit или личных настроек |

---

## 4. Модель поведения и интерактивность (Behavior Model)

Стандартная загрузка `@LoadDataBeforeShow`. Создание выполнено отдельным обработчиком, поскольку generic CUBA create-action не знает бизнес-владельца обязательной связи.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

- `Создать` — создаёт персональную конфигурацию владельца текущей сессии.
- `Редактировать`, `Удалить`, `Обновить` — штатные действия таблицы.
- `Открыть карточку`, `Тест AI` — действия выбранной конфигурации.

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- `filter` + `groupTable` `userAiConfigurationsTable`
- `dialogMode` 600×1000

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-09-23 | BL-2026-021: В `UserAiConfigurationBrowse` и `UserAiConfigurationEdit` внедрена гарантированная привязка текущего пользователя сессии (`UserSessionSource`) при создании конфигурации, добавлена pre-commit валидация и визуализация владельца (`user.login`) в сайдбаре редактора. Подтверждено тестом `UserAiConfigurationOwnerContractTest`. |
| 2026-09-22 | BL-2026-021: generic create-action заменён владельческим сценарием; `user` назначается из текущей сессии до commit |
| 2026-06-27 | Создание read-only browse для мониторинга AI-конфигураций без apiKey в view |
