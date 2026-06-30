# UI Spec: `sec$User.edit` / `itpearls_ExtUserEdit`

Cross-link: [ExtUser](../entities/ExtUser.md) · `UserAiConfiguration`

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-29 | Аватар: `fallbackImage` `userPic` с `datasource="userDs"` / `property="officialPhoto"`; удалены `defaultPic` и ручной `refreshAvatar()` / `resolveProfilePhoto()` |
| 2026-06-29 | Profile panel: `profilePanel`/`dropZone` width/height 100%; `userDs.setItem` после `applyOfficialPhoto` |
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
- «Сменить пароль» (кнопка в нижней панели) → показ/скрытие `passwordBox` на вкладке «Общие» (поля `passw` / `confirmPassw` в `fieldGroupLeft`)
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

Аватар: `fallbackImage` `userPic` привязан к `userDs.officialPhoto`; заглушка через `HunttechImageConfig.defaultFallbackImagePath` (`images/hunttech-placeholder.svg`). Upload `officialPhoto`; диалог при наличии `userAvatar`; удаление старых файлов через FileStorageService.

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
- Аватар — declarative binding `fallbackImage` → `officialPhoto`; placeholder при null через `WebFallbackImage`
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
    ├── profilePanel (100%×100%, well): dropZone (TOP_CENTER), fallbackImage userPic + officialPhoto upload, profileLabelsVBox (TOP_CENTER)
    └── settingsTabSheet
        ├── generalSettingsTab: passwordBox (card), contacts + regional hbox (50/50 flex), roles/subst split
        ├── emailSettingsTab: SMTP/POP3/IMAP grid
        └── aiSettingsTab: aiConfigsTable + buttonsPanel
└── bottomActionsBox: changePasswordBtn | spacer | editWindowActions fragment
```
