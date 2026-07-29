# UI Spec: `sec$User.edit` / `hunttech_ExtUserEdit`

Cross-link: [ExtUser](../../entities/ext-user/ExtUser.md) · `UserAiConfiguration`

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-29 | Исправлена кнопка «Сменить пароль пользователя»: добавлен минимальный `ExtUserEditor`, наследующий штатный `UserEditor`, и вызов стандартного диалога `sec$User.changePassword`; другая логика формы не изменена |
| 2026-06-29 | Аватар: `fallbackImage` `userPic` с `datasource="userDs"` / `property="officialPhoto"`; удалены `defaultPic` и ручной `refreshAvatar()` / `resolveProfilePhoto()` |
| 2026-06-29 | Profile panel: `profilePanel`/`dropZone` width/height 100%; `userDs.setItem` после `applyOfficialPhoto` |
| 2026-06-28 | Fix `userAiConfigsDs`: JPQL `e.user = :ds$userDs` (entity bind вместо `.id` + entity param) |
| 2026-06-27 | Динамический аватар: приоритет UserSettings.fileImageFace → ExtUser.fileImageFace → placeholder через FileDescriptorImageHelper |
| 2026-06-27 | Fix NPE: восстановлены `fieldGroupLeft` (passw/confirmPassw) и `fieldGroupRight` для UserEditor |

---

## Назначение и Бизнес-смысл (What & Why)

Экран редактирования пользователя HRM HuntTech (`ExtUser`) объединяет профиль рекрутера/HR, контактные и региональные настройки, параметры исходящей почты и персональные ключи AI-провайдеров. Администратор или сам пользователь (через админ-форму) управляет доступом, ролями, замещениями и интеграцией с LLM без отдельных справочников.

## Связи в интерфейсе и Навигация (UI Context & Navigation)

- **Screen id:** `sec$User.edit` (шаблон `ext-user-edit.xml`, legacy-контроллер `ExtUserEditor`)
- **Browse:** `sec$User.browse` → двойной клик / Edit
- **Дочерние модали:** `sec$User.changePassword`, `hunttech_UserAiConfiguration.edit`
- **Меню:** стандартный пункт Security → Users (CUBA)

## Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → стандартный `UserEditor` загружает `userDs` (`extUser-view`), роли и замещения
- «Сменить пароль пользователя» для сохранённой учётной записи → открывается штатный модальный экран CUBA `sec$User.changePassword` с параметром `user` → пароль изменяется стандартным сервисом платформы
- Новый пользователь → кнопка отдельной смены пароля скрыта → пароль задаётся штатными полями `passw` / `confirmPassw` и обрабатывается `UserEditor.preCommit()`
- AI: Create/Edit → модаль `UserAiConfigurationEdit` с автопривязкой `user`; Remove → `DataManager.remove`
- Сохранение → стандартный `UserEditor` + `editWindowActions`

---

## 1. Точка вызова и контекст

| Параметр | Значение |
|----------|----------|
| Legacy controller | `com.company.hunttech.web.screens.extuser.ExtUserEditor` |
| Базовый редактор | `com.haulmont.cuba.gui.app.security.user.edit.UserEditor` |
| XML | `modules/web/.../extuser/ext-user-edit.xml` |
| `web-screens.xml` | `sec$User.edit` |

`ExtUserEditor` не заменяет алгоритм сохранения пользователя. Класс наследует стандартный `UserEditor` и добавляет только запуск штатного диалога смены пароля из нижней панели.

## 2. Связь с моделью данных

| Контейнер | Тип | View / Query |
|-----------|-----|--------------|
| `userDs` | instance | `extUser-view` |
| `fieldGroupLeft` | fieldGroup (UserEditor) | `passw`, `confirmPassw` (`custom="true"`) внутри `passwordBox` |
| `fieldGroupRight` | fieldGroup (UserEditor) | `language`, `timeZone`, `group` (`custom="true"), `active`, … |
| `rolesDs` | group property | `userRoles` |
| `substitutionsDs` | collection property | `substitutions` |
| `userAiConfigsDs` | collectionDatasource | `userAiConfiguration-view`, `e.user = :ds$userDs` |

Критичные nested paths: `userRoles.role.*`, `substitutions.substitutedUser.*`, `fileImageFace` (`extUser-view`, `_local`), AI-таблица: `providerCode`, `defaultModelName`, `isActive` (без `apiKey`).

Аватар: `fallbackImage` `userPic` привязан к `userDs.officialPhoto`; заглушка через `HunttechImageConfig.defaultFallbackImagePath` (`images/hunttech-placeholder.svg`). Upload `officialPhoto`; диалог при наличии `userAvatar`; удаление старых файлов через FileStorageService.

## 3. Иерархия форм

```
sec$User.browse
  └── sec$User.edit (ExtUserEditor → UserEditor)
        ├── sec$User.changePassword (стандартный диалог CUBA)
        └── hunttech_UserAiConfiguration.edit (modal)
```

## 4. Модель поведения

### 4.1 Lifecycle
- `UserEditor.init()` — создаёт штатные password-поля, actions ролей/замещений и listener сохранения
- `ExtUserEditor.postInit()` — после стандартной инициализации скрывает `changePasswordBtn` только для новой несохранённой учётной записи

### 4.2 Смена пароля

Для существующего пользователя кнопка содержит `invoke="changePassword"`. Метод открывает:

```text
screen: sec$User.changePassword
open type: DIALOG
parameter: user = текущий userDs item
```

Пароль не вычисляется и не коммитится пользовательским кодом HRM HuntTech. Валидация, password policy, хэширование и запись выполняются стандартной реализацией CUBA Platform.

### 4.3 Валидация/сохранение
- Новый пользователь: пароль и подтверждение обрабатываются стандартным `UserEditor.preCommit()`
- Существующий пользователь: смена пароля выполняется отдельным диалогом и не отмечает остальные поля редактора изменёнными
- Роли, замещения и остальные свойства сохраняются штатным `UserEditor`

## 5. Actions & Buttons

| Элемент | Цепочка |
|---------|---------|
| `changePasswordBtn` | `invoke="changePassword"` → `sec$User.changePassword` (`DIALOG`, параметр `user`) |
| `aiConfigsCreateBtn` | клик → `ScreenBuilders` new `UserAiConfiguration` с `user` |
| `aiConfigsEditBtn` | клик → edit выбранной строки |
| `aiConfigsRemoveBtn` | клик → `dataManager.remove` + refresh |

## 6. Визуальная компоновка

```
layout
└── mainSplit (horizontal, 25%)
    ├── profilePanel (100%×100%, well): dropZone (TOP_CENTER), fallbackImage userPic + officialPhoto upload, profileLabelsVBox (TOP_CENTER)
    └── settingsTabSheet
        ├── generalSettingsTab: passwordBox (только новый пользователь), contacts + regional hbox (50/50 flex), roles/subst split
        ├── emailSettingsTab: SMTP/POP3/IMAP grid
        └── aiSettingsTab: aiConfigsTable + buttonsPanel
└── bottomActionsBox: changePasswordBtn | spacer | editWindowActions fragment
```

## 7. Регрессионная проверка

`ExtUserChangePasswordContractTest` фиксирует следующие инварианты:

- XML использует `ExtUserEditor`, наследующий стандартный `UserEditor`;
- кнопка связана с `invoke="changePassword"`;
- открывается именно `sec$User.changePassword` с параметром текущего пользователя;
- пользовательский контроллер не выполняет собственное хэширование, commit пользователя, изменение ролей или замещений.
