# UI Spec: `sec$User.edit` / `itpearls_ExtUserEdit`

Cross-link: сущность [ExtUser](../entities/ExtUser.md) (при наличии) · `UserAiConfiguration`

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-28 | Fix `userAiConfigsDs`: JPQL `e.user = :ds$userDs` (entity bind вместо `.id` + entity param) |
| 2026-06-27 | Динамический аватар: приоритет UserSettings.fileImageFace → ExtUser.fileImageFace → placeholder через FileDescriptorImageHelper |
| 2026-06-27 | Fix NPE: восстановлены `fieldGroupLeft` (passw/confirmPassw) и `fieldGroupRight` для UserEditor |

---

## Назначение и Бизнес-смысл (What & Why)

Экран редактирования пользователя HRM HuntTech (`ExtUser`) объединяет профиль рекрутера/HR, контактные и региональные настройки, параметры исходящей почты и персональные ключи AI-провайдеров. Администратор или сам пользователь (через админ-форму) управляет доступом, ролями, замещениями и интеграцией с LLM без отдельных справочников.

## Связи в интерфейсе и Навигация (UI Context & Navigation)

- **Screen id:** `sec$User.edit` (шаблон `ext-user-edit.xml`, контроллер-дополнение `itpearls_ExtUserEdit`)
- **Browse:** `sec$User.browse` → двойной клик / Edit
- **Дочерние модали:** `itpearls_UserAiConfiguration.edit` (CRUD AI-конфигураций)
- **Меню:** стандартный пункт Security → Users (CUBA)

## Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → загрузка `userDs` (`extUser-view`), обновление панели профиля (ФИО, логин, статус), refresh `userAiConfigsDs`
- «Сменить пароль» → показ/скрытие `passwordBox` (поля `passw` / `confirmPassw` в `fieldGroupLeft`, компоненты создаёт `UserEditor.createPasswordFields`)
- Новый пользователь → `passwordBox` автоматически видим (`PersistenceHelper.isNew`)
- AI: Create/Edit → модаль `UserAiConfigurationEdit` с автопривязкой `user`; Remove → `DataManager.remove`
- Сохранение → стандартный `UserEditor` + `editWindowActions`

---

## 1. Точка вызова и контекст

| Параметр | Значение |
|----------|----------|
| `@UiController` (дополнение) | `itpearls_ExtUserEdit` |
| Базовый редактор | `com.haulmont.cuba.gui.app.security.user.edit.UserEditor` |
| XML | `modules/web/.../extuser/ext-user-edit.xml` |
| `web-screens.xml` | `sec$User.edit` |

## 2. Связь с моделью данных

| Контейнер | Тип | View / Query |
|-----------|-----|--------------|
| `userDs` | instance | `extUser-view` |
| `fieldGroupLeft` | fieldGroup (UserEditor) | `passw`, `confirmPassw` (`custom="true"`) внутри `passwordBox` |
| `fieldGroupRight` | fieldGroup (UserEditor) | `language`, `timeZone`, `group` (`custom="true"`), `active`, … |
| `rolesDs` | group property | `userRoles` |
| `substitutionsDs` | collection property | `substitutions` |
| `userAiConfigsDs` | collectionDatasource | `userAiConfiguration-view`, `e.user = :ds$userDs` |

Критичные nested paths: `userRoles.role.*`, `substitutions.substitutedUser.*`, `fileImageFace` (`extUser-view`, `_local`), AI-таблица: `providerCode`, `defaultModelName`, `isActive` (без `apiKey`).

Аватар: отдельный JPQL-load `UserSettings` (`userSettings-view`, `fileImageFace` _local) по `user.id`; приоритет отображения — personal → admin → `icons/no-programmer.jpeg`.

## 3. Иерархия форм

```
sec$User.browse
  └── sec$User.edit (ExtUserEdit + UserEditor)
        └── itpearls_UserAiConfiguration.edit (modal)
```

## 4. Модель поведения

### 4.1 Lifecycle
- `ExtUserEdit.onInit` — listener на `userDs` для refresh профиля и AI-списка
- `onAfterShow` — первичное заполнение labels

### 4.2 Скрытые вычисления
- `buildFio()` — `name` или `lastName firstName middleName`
- `refreshAvatar()` / `resolveAvatarFileDescriptor()` — personal `UserSettings.fileImageFace` (экран «Обо мне» / `ExtSettingsWindow`) → admin `ExtUser.fileImageFace` → `defaultPic` (`icons/no-programmer.jpeg`); безопасная загрузка через `FileDescriptorImageHelper`
- SMTP password required validator через `@Install`

### 4.3 Валидация/сохранение
- Стандартный commit `UserEditor`; AI-записи коммитятся из модали `UserAiConfigurationEdit`

## 5. Actions & Buttons

| Элемент | Цепочка |
|---------|---------|
| `changePasswordBtn` | клик → `passwordBox` visible toggle |
| `aiConfigsCreateBtn` | клик → `ScreenBuilders` new `UserAiConfiguration` с `user` |
| `aiConfigsEditBtn` | клик → edit выбранной строки |
| `aiConfigsRemoveBtn` | клик → `dataManager.remove` + refresh |

## 6. Визуальная компоновка

```
layout
└── mainSplit (horizontal, 25%)
    ├── profilePanel (320px, well): avatar, fioLabel, loginLabel, statusLabel, changePasswordBtn, passwordBox
    └── settingsTabSheet
        ├── generalSettingsTab: contacts + regional vboxes, roles/subst split
        ├── emailSettingsTab: SMTP/POP3/IMAP grid
        └── aiSettingsTab: aiConfigsTable + buttonsPanel
└── editWindowActions fragment
```
