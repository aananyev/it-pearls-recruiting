# UserAiConfiguration Browse (`itpearls_UserAiConfiguration.browse`)

> Сущность: [UserAiConfiguration.md](../entities/UserAiConfiguration.md) · edit: модаль `itpearls_UserAiConfiguration.edit` в [itpearls_ExtUserEdit_Spec.md](itpearls_ExtUserEdit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран мониторинга персональных AI-конфигураций пользователей: какой провайдер, модель и активность без отображения API-ключа. Дополняет редактирование ключей на вкладке «Персональный ИИ» в профиле пользователя.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Меню **Управление AI** → «Мониторинг ключей пользователей». Только просмотр (без create/edit/remove). Редактирование — через `sec$User.edit` / `UserAiConfigurationEdit`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

При открытии загружается read-only список всех `itpearls_UserAiConfiguration` с фильтром. API-ключ исключён из browse-view.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_UserAiConfiguration.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.useraiconfiguration.UserAiConfigurationBrowse` |
| **XML-дескриптор** | `user-ai-configuration-browse.xml` |
| **Базовый класс** | `StandardLookup` (без CRUD actions) |
| **Меню** | `web-menu.xml` → `aiAdministration` → `itpearls_UserAiConfiguration.browse` |

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `UserAiConfiguration` |
| **View** | `userAiConfiguration-browse-view` (без `apiKey`) |
| **Nested paths** | `user.login` (user `_minimal`) |
| **Data containers** | `userAiConfigurationsDc` (readOnly) |
| **Loader** | `userAiConfigurationsDl` |

### JPQL

```
select e from itpearls_UserAiConfiguration e
```

### Колонки таблицы

- `user.login`, `providerCode`, `isActive`, `defaultModelName`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран | Способ открытия |
|-------|-------|-----------------|
| Родитель | `aiAdministration` (menu) | menu |
| Редактирование ключей | `itpearls_UserAiConfiguration.edit` | модаль из ExtUser edit |

---

## 4. Модель поведения и интерактивность (Behavior Model)

Read-only browse; стандартная загрузка `@LoadDataBeforeShow`.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

CRUD-кнопки отсутствуют (мониторинг).

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- `filter` + `groupTable` `userAiConfigurationsTable`
- `dialogMode` 600×1000

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-27 | Создание read-only browse для мониторинга AI-конфигураций без apiKey в view |
