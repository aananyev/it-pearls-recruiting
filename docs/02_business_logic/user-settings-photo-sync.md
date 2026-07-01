# Технический проект: модуль администрирования пользователей и персональных настроек HRM HuntTech

> **Платформа:** CUBA 7.3 · **Область:** `ExtUser`, `UserSettings`, экраны `sec$User.edit` и `settings`  
> **Аудитория:** разработчики, бизнес-аналитики  
> **Cross-links:** [ExtUser](../entities/ExtUser.md) · [itpearls_ExtUserEdit_Spec](../ui/itpearls_ExtUserEdit_Spec.md) · [ImageProcessingService](../services/ImageProcessingService.md)

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-30 | Первичная версия TDD: архитектура сущностей, экраны, синхронизация фото, жизненный цикл сохранения |

---

## Аннотация

Документ описывает подсистему управления учётными записями рекрутёров и HR в HRM HuntTech: расширение стандартного пользователя CUBA (`sec$User` → `ExtUser`), персональные настройки (`UserSettings`), два административных и пользовательских экрана, а также сквозную логику загрузки, синхронизации и удаления фотографий профиля. Все утверждения основаны на фактическом коде репозитория.

---

## 1. ОБЗОР АРХИТЕКТУРЫ И СУЩНОСТЕЙ

### 1.1. Расширение пользователя CUBA

`ExtUser` (`itpearls_ExtUser`) наследует `com.haulmont.cuba.security.entity.User` через механизм `@Extends(User.class)` и маппится на ту же таблицу `SEC_USER` с дискриминатором `DTYPE = itpearls_ExtUser`.

```text
┌─────────────────────────────────────────────────────────────┐
│                      sec$User (CUBA)                        │
│  login, name, email, active, userRoles, substitutions, …    │
└───────────────────────────┬─────────────────────────────────┘
                            │ @Extends
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    itpearls_ExtUser (ExtUser)               │
│  officialPhoto, userAvatar, fileImageFace* (deprecated)     │
│  smtp*/imap*/pop3*, statistics, dashboards                  │
└───────────────────────────┬─────────────────────────────────┘
                            │ 1 : 1  (USER_ID, unique)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              itpearls_UserSettings (UserSettings)           │
│  user (ExtUser), fileImageFace, smtp*/imap*/pop3*           │
└─────────────────────────────────────────────────────────────┘
```

\* `fileImageFace` на `ExtUser` помечен `@Deprecated`; сохранён для миграции данных из колонки `IMAGE_ID`.

### 1.2. Связь ExtUser ↔ UserSettings

| Параметр | Значение |
|----------|----------|
| Кардинальность | 1 : 1 |
| Владелец связи | `UserSettings` (`@JoinColumn(name = "USER_ID", unique = true)`) |
| Поле связи | `UserSettings.user` → `ExtUser` |
| Политика удаления | `@OnDeleteInverse(DeletePolicy.DENY)` — нельзя удалить `ExtUser`, пока существует `UserSettings` |
| Автосоздание | `ExtUserChangedListener` создаёт запись `UserSettings` при изменении `ExtUser`, если её ещё нет (копирует почтовые поля с `ExtUser`, **без фото**) |

Fetch plan `userSettings-view` (`views.xml`):

```xml
<view entity="itpearls_UserSettings" name="userSettings-view" extends="_local">
    <property name="user" view="_local"/>
    <property name="fileImageFace" view="_local"/>
</view>
```

Fetch plan `extUser-view` для экранов:

```xml
<view entity="itpearls_ExtUser" name="extUser-view" extends="_local">
    <property name="officialPhoto" view="_local"/>
    <property name="userAvatar" view="_local"/>
    <property name="fileImageFace" view="_local"/>
    <!-- substitutions, userRoles, group … -->
</view>
```

### 1.3. Слоты фотографий: назначение и различия

В системе сосуществуют **четыре** логических точки хранения изображения лица; две из них — целевые, две — legacy.

| Слот | Сущность / поле | Колонка БД | Кто изменяет | Семантика |
|------|-----------------|------------|--------------|-----------|
| Официальное фото | `ExtUser.officialPhoto` | `OFFICIAL_PHOTO_ID` | Администратор (`ExtUserEdit`, upload `officialPhoto`) | Корпоративное фото сотрудника; отображается в админ-панели профиля (`fallbackImage` → `officialPhoto`) |
| Личный аватар | `ExtUser.userAvatar` | `USER_AVATAR_ID` | Сам пользователь (`ExtSettingsWindow`, upload `userAvatar`) | Персональное фото; **наивысший приоритет** при отображении через `resolveProfilePhoto()` |
| Legacy (пользователь) | `UserSettings.fileImageFace` | `IMAGE_ID` | Администратор (`ExtUserEdit`, при `syncToUserSettings=true`) | Исторический слот «личного фото»; **используется для детекции конфликта** в `resolvePersonalAvatar()` |
| Legacy (ExtUser) | `ExtUser.fileImageFace` | `IMAGE_ID` | — (deprecated) | Резервный fallback в `resolveProfilePhoto()` после `officialPhoto` |

#### Приоритет отображения (`ExtUser.resolveProfilePhoto()`)

```text
userAvatar  ──► если не null → отображается
     │ null
     ▼
officialPhoto ──► если не null → отображается
     │ null
     ▼
fileImageFace ──► legacy fallback
     │ null
     ▼
placeholder (ThemeResource / FallbackImage)
```

Метод `FileDescriptorImageHelper.setUserProfilePhoto()` делегирует выбор фото в `resolveProfilePhoto()` и безопасно проверяет физическое наличие файла через `fileLoader.fileExists()` до установки `FileDescriptorResource`.

### 1.4. Дублирование почтовых настроек

Почтовые поля SMTP/IMAP/POP3 дублируются на `ExtUser` и `UserSettings`:

- **Админ-экран** (`ExtUserEdit`) редактирует поля **на `ExtUser`** (вкладка «Настройки почты», `datasource="userDs"`).
- **Личные настройки** (`ExtSettingsWindow`) читают/пишут **преимущественно в `UserSettings`**, с fallback на `ExtUser` при инициализации (`setEmailSettings()`).

Это наследие эволюции модели: `ExtUser` — канон для администратора; `UserSettings` — персональные overrides пользователя.

### 1.5. Регистрация экранов (`web-screens.xml`)

| Screen id | Шаблон | Контроллер |
|-----------|--------|------------|
| `sec$User.edit` | `ext-user-edit.xml` | `UserEditor` + дополнение `itpearls_ExtUserEdit` |
| `settings` | `ext-settings-window.xml` | `ExtSettingsWindow` extends `SettingsWindow` |

---

## 2. ДЕТАЛЬНОЕ ОПИСАНИЕ РАБОТЫ ЭКРАННЫХ ФОРМ

### 2.1. ExtUserEdit — административное редактирование пользователя

#### 2.1.1. Назначение

Экран `sec$User.edit` предназначен для полного администрирования учётной записи: профиль, контакты, региональные параметры, роли, замещения, корпоративная почта, персональные AI-конфигурации и загрузка **официального** фото.

| Параметр | Значение |
|----------|----------|
| Базовый класс окна | `com.haulmont.cuba.gui.app.security.user.edit.UserEditor` |
| Дополнение | `@UiController("itpearls_ExtUserEdit")` extends `Screen` |
| Companion | `UserEditorCompanion` (стандарт CUBA) |

#### 2.1.2. Структура layout

```text
layout
└── mainSplit (horizontal, pos=25%)
    ├── profilePanel (well, 25%)
    │     ├── fallbackImage userPic → userDs.officialPhoto
    │     ├── officialPhotoUpload → userDs.officialPhoto (IMMEDIATE)
    │     └── profileLabelsVBox: fioLabel, loginLabel, statusLabel
    └── settingsTabSheet (75%)
          ├── generalSettingsTab: passwordBox, contacts, regional, roles/subst
          ├── emailSettingsTab: SMTP/POP3/IMAP grid → userDs
          └── aiSettingsTab: aiConfigsTable → userAiConfigsDs
└── bottomActionsBox: changePasswordBtn | editWindowActions (OK/Cancel)
```

#### 2.1.3. Контейнеры данных (`dsContext`)

| ID | Тип | Класс / view | Назначение |
|----|-----|--------------|------------|
| `userDs` | `datasource` | `User` / `extUser-view` | Основная сущность; вложенные `rolesDs`, `substitutionsDs` |
| `userAiConfigsDs` | `collectionDatasource` | `UserAiConfiguration` / `userAiConfiguration-view` | JPQL: `e.user = :ds$userDs` |

Контейнер `userSettings` **не объявлен** в XML: `UserSettings` загружается императивно в `loadUserSettings()` при загрузке фото.

#### 2.1.4. profilePanel

- **Превью:** `fallbackImage` `userPic` привязан к `userDs.officialPhoto` (не к `resolveProfilePhoto()`).
- **Метки:** `refreshProfileLabels()` заполняет ФИО (`buildFio()`), логин, статус (активен/заблокирован) из `messages_ru.properties`.
- **Пароль:** `passwordBox` скрыт по умолчанию; виден для нового пользователя (`PersistenceHelper.isNew`) или по кнопке «Сменить пароль».

#### 2.1.5. settingsTabSheet

| Вкладка | ID | Содержимое |
|---------|-----|------------|
| Общие | `generalSettingsTab` | Контакты, региональные поля, роли (`rolesTable`), замещения (`substTable`) |
| Почта | `emailSettingsTab` | SMTP/POP3/IMAP серверы, порты, флаги `*PasswordRequired`, учётные данные — всё на `userDs` |
| ИИ | `aiSettingsTab` | CRUD `UserAiConfiguration` через `ScreenBuilders` → `UserAiConfigurationEdit` |

Валидатор `@Install` на `smtpPasswordRequired`: при `true` поле `smtpPassword` становится обязательным.

#### 2.1.6. Обработка загрузки officialPhoto

Цепочка вызывается сразу после успешной загрузки (`fileStoragePutMode="IMMEDIATE"`):

```text
onOfficialPhotoUploadSucceed
    → getExtUser()
    → processUploadedPhoto()  [AvatarImageUploadHelper + ImageProcessingService]
    → loadUserSettings(user)   [optional, view userSettings-view]
    → resolvePersonalAvatar()  [UserSettings.fileImageFace + fileExists]
    ├─ personalAvatar != null → showAdminPhotoChoiceDialog()
    └─ иначе → applyOfficialPhotoUpdate(..., syncToUserSettings = userSettings != null)
```

### 2.2. ExtSettingsWindow — персональные настройки пользователя

#### 2.2.1. Назначение

Окно `settings` (наследник CUBA `SettingsWindow`) позволяет **текущему** пользователю изменить интерфейс приложения, часовой пояс, язык, стартовый экран, пароль, почтовые параметры и **личный аватар** (`userAvatar`).

#### 2.2.2. Наследование и переопределения

`ExtSettingsWindow` переопределяет:

| Метод | Поведение |
|-------|-----------|
| `init()` | Загрузка `ExtUser`, `UserSettings`, `setEmailSettings()`, `refreshProfilePhoto()`, регистрация listeners upload |
| `commit()` | Сохранение почты в `UserSettings` + `dataManager.commit(extUserDs)` + `super.commit()` |
| `cancel()` | `super.cancel()` |

Остальные protected-методы (`initDefaultScreenField`, `collectScreens`, `saveLocaleSettings`, …) делегируют в базовый `SettingsWindow` без изменений.

#### 2.2.3. Контейнеры данных

| ID (XML) | Использование в Java |
|----------|----------------------|
| `extUserDs` | **Активно:** `@Inject Datasource<ExtUser> extUserDs`; загрузка в `loadExtUser()`, commit в `commit()` |
| `userSettingsDs` | **Объявлен в XML, не инжектируется** — логика работает через приватное поле `userSettings`, загружаемое `DataManager` |

#### 2.2.4. Вкладки

| Вкладка | ID | Содержимое |
|---------|-----|------------|
| Обо мне | `msgMyInfo` | `userPic` / `defaultPic`, `userAvatarUpload` → `extUserDs.userAvatar` |
| Интерфейс | `msgInterface` | Тема, язык, timezone, default screen, смена пароля, сброс настроек экранов (базовый CUBA) |
| Почта | `mailAccessTab` | SMTP/POP3/IMAP — отдельные `@Inject` поля, **не привязаны к datasource** |

#### 2.2.5. Отображение фото

`refreshProfilePhoto()`:

1. Сбрасывает `valueSource` у `userPic`.
2. Вызывает `user.resolveProfilePhoto()`.
3. При наличии файла — `FileDescriptorImageHelper.setUserProfilePhoto()`; иначе скрывает `userPic`, показывает `defaultPic` (`icons/no-programmer.jpeg`).

Загрузка аватара (`onUserAvatarUploaded`):

```text
processUploadedAvatar() → removeStoredFileIfUnreferenced(oldAvatar, officialPhoto, newAvatar)
→ user.setUserAvatar(newAvatar)
→ refreshProfilePhoto()
```

**`UserSettings.fileImageFace` при личной загрузке не изменяется.**

---

## 3. БИЗНЕС-ЛОГИКА СИНХРОНИЗАЦИИ ФОТОГРАФИЙ

### 3.1. Общий pipeline обработки изображения

Оба экрана используют единый web-helper:

```text
AvatarImageUploadHelper.processUploadedImage(descriptor, fileLoader, fileStorageService,
                                             dataManager, imageProcessingService, log)
    → fileLoader.openStream(descriptor)
    → imageProcessingService.process(bytes, fileName)   [HunttechImageConfig: size, format]
    → при processed=true: обновление extension/size, fileStorageService.saveFile, dataManager.commit(descriptor)
```

Параметры лимитов: `hunttech.image.resize.size` (по умолчанию 1024 px), `hunttech.image.resize.format` (по умолчанию `png`). См. [ImageProcessingService.md](../services/ImageProcessingService.md).

### 3.2. Сценарий 1: отсутствует личное фото в UserSettings

**Условие:** `resolvePersonalAvatar()` возвращает `null` — т.е. `UserSettings.fileImageFace` равен `null` **или** физический файл отсутствует в хранилище (`FileDescriptorImageHelper.fileExists`).

**Триггер:** `onOfficialPhotoUploadSucceed` → ветка `else` → `applyOfficialPhotoUpdate(user, userSettings, newPhoto, userSettings != null)`.

**Действия `applyOfficialPhotoUpdate`:**

1. Запоминаются `oldOfficial`, `oldAvatar`, `settingsAvatar`.
2. `user.setOfficialPhoto(newPhoto)` и `user.setUserAvatar(newPhoto)` — **оба слота ExtUser синхронизируются**.
3. Удаление устаревших файлов (`removeStoredFileIfUnreferenced`) с учётом ссылок между слотами.
4. Если `syncToUserSettings == true` (запись `UserSettings` существует):
   - `setUserSettingsAvatar(userSettings, newPhoto)` → `fileImageFace = newPhoto`
   - удаление старого `fileImageFace` из хранилища
   - **`dataManager.commit(userSettings)` — немедленный commit, не дожидаясь OK на экране**
5. `userDs.setItem(user)` — обновление UI datasource (commit `ExtUser` — при нажатии OK в `UserEditor`).

```text
Админ                    ExtUserEdit              DataManager           FileStorage
  │                          │                        │                      │
  │── upload officialPhoto ─►│                        │                      │
  │                          │── processUploadedPhoto │                      │
  │                          │── loadUserSettings ───►│                      │
  │                          │◄─ optional UserSettings│                      │
  │                          │── resolvePersonalAvatar│                      │
  │                          │   (null → сценарий 1)  │                      │
  │                          │── applyOfficialPhotoUpdate                     │
  │                          │   set officialPhoto,   │                      │
  │                          │   userAvatar = newPhoto│                      │
  │                          │── commit(userSettings)►│ (если запись есть)   │
  │                          │── removeStoredFile ────┼─────────────────────►│
  │                          │── userDs.setItem ──────│                      │
  │── OK (UserEditor) ──────►│── commit(ExtUser) ────►│                      │
```

### 3.3. Сценарий 2: конфликт — личное фото уже есть в UserSettings

**Условие:** `UserSettings.fileImageFace != null` и файл существует в хранилище.

**Триггер:** `showAdminPhotoChoiceDialog(user, userSettings, newPhoto, personalAvatar)`.

**UI диалога:**

| Элемент | Реализация |
|---------|------------|
| Caption | `messageBundle.getMessage("msgPhotoChangeCaption")` → «Изменение фото пользователя» |
| Message | HTML (`ContentMode.HTML`, ширина 450px): текст с ФИО + превью через `FileDescriptorImageHelper.buildCandidateFacePreviewHtml()` |
| «Да» (primary) | `applyOfficialPhotoUpdate(..., syncToUserSettings=true)` — обновить officialPhoto, userAvatar **и** UserSettings.fileImageFace |
| «Нет» | `applyOfficialPhotoUpdate(..., syncToUserSettings=false)` — обновить только `officialPhoto` и `userAvatar` на ExtUser; **личный слот UserSettings не трогается** |
| «Отмена» | `officialPhotoUpload.setValue(null)` — сброс upload без изменения сущностей |

```text
Админ                    ExtUserEdit                    UserSettings
  │                          │                              │
  │── upload ───────────────►│                              │
  │                          │── resolvePersonalAvatar ────►│ fileImageFace exists
  │◄── OptionDialog HTML ────│                              │
  │── [Да] ─────────────────►│ applyOfficialPhotoUpdate     │
  │                          │   sync=true → commit ───────►│ fileImageFace=newPhoto
  │── [Нет] ────────────────►│ applyOfficialPhotoUpdate     │
  │                          │   sync=false (US не меняется)│
  │── [Отмена] ─────────────►│ upload.clear()               │
```

**Важно:** диалог **не** проверяет `ExtUser.userAvatar` напрямую. Конфликт определяется исключительно по `UserSettings.fileImageFace`. Если пользователь загрузил аватар только в `userAvatar` (через личные настройки), а `fileImageFace` пуст — диалог **не показывается**, но `userAvatar` всё равно перезаписывается в сценарии 1.

### 3.4. Очистка officialPhoto (админ)

`onOfficialPhotoUploadBeforeValueClear`:

```java
removeStoredFileIfUnreferenced(user.getOfficialPhoto(), null, user.getUserAvatar());
user.setOfficialPhoto(null);
```

Файл удаляется из хранилища только если на него не ссылается `userAvatar`.

### 3.5. Личная загрузка (ExtSettingsWindow)

`onUserAvatarUploaded` / `onUserAvatarCleared` изменяют **только** `ExtUser.userAvatar`. Слоты `officialPhoto` и `UserSettings.fileImageFace` не затрагиваются.

При очистке:

```java
removeStoredFileIfUnreferenced(user.getUserAvatar(), user.getOfficialPhoto(), null);
user.setUserAvatar(null);
```

### 3.6. removeStoredFileIfUnreferenced — предотвращение orphan-файлов

Две реализации с разной сигнатурой, общий принцип: удалить `FileDescriptor` из `FileStorageService`, если он не совпадает с `replacement` и его UUID не встречается среди «ещё используемых» ссылок.

**ExtUserEdit** (varargs):

```java
private void removeStoredFileIfUnreferenced(FileDescriptor oldFile,
                                            FileDescriptor replacement,
                                            FileDescriptor... stillReferenced)
```

Проверяет все переданные `stillReferenced` по `id`; при совпадении — выход без удаления. Ошибки `FileStorageException` логируются (`log.warn`), не прерывают UX.

**ExtSettingsWindow** (два аргумента ссылок):

```java
private void removeStoredFileIfUnreferenced(FileDescriptor oldFile,
                                            FileDescriptor stillReferenced,
                                            FileDescriptor replacement)
```

Эквивалентная логика для одного дополнительного держателя ссылки (`officialPhoto` при удалении `userAvatar`).

---

## 4. ЖИЗНЕННЫЙ ЦИКЛ И СОХРАНЕНИЕ ДАННЫХ

### 4.1. ExtUserEdit — двухфазное сохранение

| Фаза | Что сохраняется | Механизм |
|------|-----------------|----------|
| Немедленно (при upload фото) | `UserSettings` (при `syncToUserSettings=true`) | `dataManager.commit(userSettings)` внутри `applyOfficialPhotoUpdate` |
| Немедленно (при upload фото) | `ExtUser` в памяти datasource | `userDs.setItem(user)` — без DB flush |
| По OK | `ExtUser` (все поля формы, роли, замещения) | Стандартный `UserEditor` commit через `editWindowActions` |
| По AI CRUD | `UserAiConfiguration` | `dataManager.remove` / commit из модали `UserAiConfigurationEdit` |

```text
Событие                    ExtUser (БД)    UserSettings (БД)    FileStorage
─────────────────────────────────────────────────────────────────────────
Upload фото (сценарий 1)   pending OK      commit сразу         new file saved (IMMEDIATE)
Upload фото (сценарий 2)   pending OK      commit или skip      depends on Да/Нет
OK на форме                commit          (уже сохранён)       —
Cancel                     rollback ds       (US уже в БД*)       —
```

\* Отмена формы **не откатывает** ранее закоммиченный `UserSettings` при upload — потенциальная несогласованность UX.

### 4.2. ExtSettingsWindow — единый commit по OK

`commit()`:

1. Копирует значения почтовых полей из UI в `userSettings`.
2. `dataManager.commit(userSettings)`.
3. `dataManager.commit(extUserDs.getItem())` — включает `userAvatar`, изменённый при upload.
4. `super.commit()` — настройки интерфейса CUBA (тема, locale, timezone, default screen).

Upload аватара (`IMMEDIATE`) сохраняет файл в хранилище сразу; привязка к `ExtUser.userAvatar` — в памяти `extUserDs` до нажатия OK.

### 4.3. setEmailSettings — fallback ExtUser → UserSettings

Метод `setEmailSettings()` вызывается в `init()` и при создании новой записи `createNewUserSetting()`. Алгоритм для каждого почтового поля:

```text
1. Если userSettings.<поле> != null → установить в UI из UserSettings
2. Иначе если UI-поле ещё null → установить из currentUser (ExtUser).<поле>
3. Для портов: fallback 0, если и ExtUser, и UI null
```

Порядок приоритета при **чтении**: `UserSettings` (если not null) → `ExtUser`.

При **записи** (`commit()`): значения UI целиком перезаписывают `UserSettings`; поля `ExtUser` для почты в этом окне **не обновляются**.

### 4.4. Автосоздание UserSettings (ExtUserChangedListener)

При любом `EntityChangedEvent` для `ExtUser` (кроме DELETE) listener проверяет наличие `UserSettings` для пользователя. Если записи нет — создаёт с копией почтовых полей с `ExtUser`. Фото не копируется.

**Замечание по реализации:** listener загружает **все** записи `UserSettings` (`dataManager.load(...).list()`) и сравнивает в цикле — потенциальный N+1 при масштабировании.

### 4.5. Диаграмма жизненного цикла сессии настроек

```text
                    ┌──────────────────┐
                    │  User opens      │
                    │  settings window │
                    └────────┬─────────┘
                             │
                    ┌────────▼─────────┐
                    │ init():          │
                    │ loadExtUser()    │
                    │ getUserSettings  │
                    │ setEmailSettings │
                    │ refreshProfile   │
                    └────────┬─────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
     ┌────────▼────┐  ┌──────▼──────┐  ┌───▼────┐
     │ Edit mail   │  │ Upload      │  │ Edit   │
     │ fields      │  │ userAvatar  │  │ UI prefs│
     └────────┬────┘  └──────┬──────┘  └───┬────┘
              │              │              │
              └──────────────┼──────────────┘
                             │
                    ┌────────▼─────────┐
              ┌─────┤ OK: commit()     ├─────┐
              │     │  US + ExtUser    │     │
              │     │  + super.commit  │     │
              │     └──────────────────┘     │
              │                              │
     ┌────────▼────────┐            ┌───────▼───────┐
     │ Cancel:         │            │ Avatar/file   │
     │ super.cancel()  │            │ already in    │
     │ (US unchanged   │            │ storage if    │
     │  if not OK)     │            │ uploaded)     │
     └─────────────────┘            └───────────────┘
```

---

## 5. ВСПОМОГАТЕЛЬНЫЕ КОМПОНЕНТЫ

### 5.1. FileDescriptorImageHelper

| Метод | Назначение в контексте пользователей |
|-------|--------------------------------------|
| `fileExists(fileLoader, fd)` | Проверка перед отображением и в `resolvePersonalAvatar()` |
| `setUserProfilePhoto(image, fileLoader, user)` | `ExtSettingsWindow.refreshProfilePhoto()`, виджеты |
| `buildCandidateFacePreviewHtml(fileLoader, fd)` | HTML-превью в диалоге конфликта `ExtUserEdit` |
| `buildDispatchDownloadUrl(fd)` | URL `/dispatch/download?f=` для `<img>` в диалоге |

### 5.2. AvatarImageUploadHelper

Единая точка post-upload обработки для `ExtUserEdit.processUploadedPhoto()` и `ExtSettingsWindow.processUploadedAvatar()`.

### 5.3. ImageProcessingService

Серверный bean `hunttech_ImageProcessingService`; вызывается из web-слоя через `AvatarImageUploadHelper`. Не привязан к конкретному экрану.

---

## 6. МАТРИЦА ОТВЕТСТВЕННОСТИ ПО ПОЛЯМ

| Поле | ExtUserEdit | ExtSettingsWindow | Отображение (resolveProfilePhoto) |
|------|-------------|-------------------|-----------------------------------|
| `officialPhoto` | R/W (upload) | R (protect on avatar delete) | 2-й приоритет |
| `userAvatar` | W (синхр. при upload official) | R/W (upload) | 1-й приоритет |
| `UserSettings.fileImageFace` | R/W (при syncToUserSettings) | — | Не участвует в resolveProfilePhoto |
| `ExtUser.fileImageFace` | — | — | 3-й приоритет (legacy) |

---

## 7. ССЫЛКИ НА ИСХОДНЫЙ КОД

| Компонент | Путь |
|-----------|------|
| ExtUser | `modules/global/src/com/company/itpearls/entity/ExtUser.java` |
| UserSettings | `modules/global/src/com/company/itpearls/entity/UserSettings.java` |
| ExtUserEdit | `modules/web/src/com/company/itpearls/web/screens/extuser/ExtUserEdit.java` |
| ext-user-edit.xml | `modules/web/src/com/company/itpearls/web/screens/extuser/ext-user-edit.xml` |
| ExtSettingsWindow | `modules/web/src/com/company/itpearls/web/screens/extsettingswindow/ExtSettingsWindow.java` |
| ext-settings-window.xml | `modules/web/src/com/company/itpearls/web/screens/extsettingswindow/ext-settings-window.xml` |
| views | `modules/global/src/com/company/itpearls/views.xml` (`extUser-view`, `userSettings-view`) |
| ExtUserChangedListener | `modules/core/src/com/company/itpearls/listeners/ExtUserChangedListener.java` |
| FileDescriptorImageHelper | `modules/web/src/com/company/itpearls/web/util/FileDescriptorImageHelper.java` |
| AvatarImageUploadHelper | `modules/web/src/com/company/itpearls/web/util/AvatarImageUploadHelper.java` |

---

## 8. ИЗВЕСТНЫЕ РАСХОЖДЕНИЯ И ПРОБЕЛЫ (код vs документация)

| # | Наблюдение | Источник |
|---|------------|----------|
| 1 | Детекция конфликта фото использует `UserSettings.fileImageFace`, а не `ExtUser.userAvatar` | `resolvePersonalAvatar()` в `ExtUserEdit.java` |
| 2 | `ExtSettingsWindow` не обновляет `UserSettings.fileImageFace` при загрузке аватара | `onUserAvatarUploaded()` |
| 3 | `userSettingsDs` объявлен в `ext-settings-window.xml`, но не используется в Java | XML vs `ExtSettingsWindow.java` |
| 4 | UI Spec `itpearls_ExtUserEdit_Spec.md` (2026-06-27) упоминает приоритет `UserSettings.fileImageFace` для аватара; актуальный код `ExtUserEdit` привязывает preview к `officialPhoto` | UI Spec vs `ext-user-edit.xml` |
| 5 | `docs/ui/ExtSettingsWindow_Spec.md` отсутствует (упоминается в `ImageProcessingService.md` как «при наличии») | docs gap |
| 6 | При upload фото в `ExtUserEdit` `UserSettings` коммитится до OK; Cancel формы не откатывает | `applyOfficialPhotoUpdate` |
| 7 | Если `UserSettings` не существует в момент upload (listener ещё не отработал), `syncToUserSettings=false` — фото не попадёт в `fileImageFace` | `loadUserSettings().optional()` |
| 8 | `ExtUserChangedListener` загружает полный список `UserSettings` вместо JPQL по user | performance smell |

---

## 9. РЕКОМЕНДАЦИИ ДЛЯ РАЗРАБОТКИ (не реализовано)

1. Унифицировать детекцию конфликта: учитывать `ExtUser.userAvatar` наряду с `UserSettings.fileImageFace`, либо отказаться от `fileImageFace` как отдельного слота.
2. Синхронизировать `userAvatar` и `fileImageFace` в `ExtSettingsWindow` или формально deprecate `fileImageFace`.
3. Создать `docs/ui/ExtSettingsWindow_Spec.md` по GLOBAL UI TRIGGER.
4. Перенести commit `UserSettings` из upload-handler в общий save формы или добавить откат при Cancel.
5. Заменить full-scan в `ExtUserChangedListener` на параметризованный запрос `where e.user = :user`.
