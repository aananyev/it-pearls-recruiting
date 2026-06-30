# ExtUser (`itpearls_ExtUser`)

> Расширение стандартного пользователя CUBA (`sec$User`) для HRM HuntTech: почтовые настройки, дашборды, AI-конфигурации и **два независимых слота фото**.

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

`ExtUser` — учётная запись рекрутёра или HR-специалиста в HRM HuntTech. Помимо стандартных полей CUBA (`login`, `name`, роли), сущность хранит корпоративные почтовые параметры (SMTP/IMAP/POP3), флаги доступа к статистике и дашбордам, персональные AI-ключи (`UserAiConfiguration`) и **два независимых фото**:

| Слот | Поле | Кто задаёт | Назначение |
|------|------|------------|------------|
| Официальное | `officialPhoto` | Администратор (`sec$User.edit`) | Корпоративное/админское фото сотрудника |
| Личный аватар | `userAvatar` | Сам пользователь (`ExtSettingsWindow`) | Персональное фото в «Обо мне» |

При отображении в интерфейсе приоритет: **userAvatar → officialPhoto → legacy `fileImageFace` → placeholder**.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

| Экран | Controller | Назначение |
|-------|------------|------------|
| Список пользователей | `sec$User.browse` → `ext-user-browse.xml` | Админский browse |
| Редактирование | `sec$User.edit` → `ext-user-edit.xml` / `ExtUserEdit` | Админ: профиль, роли, почта, AI, загрузка `officialPhoto` |
| Настройки | `ExtSettingsWindow` | Пользователь: интерфейс, почта, загрузка `userAvatar` |
| Виджет дашборда | `itpearls_MyPhotoWidget` | Миниатюра текущего пользователя |

Дочерние сущности: `UserSettings` (персональные почтовые overrides), `UserAiConfiguration` (AI-ключи). На `ExtUser` ссылаются рекрутёры в `IteractionList`, комментарии вакансий, внутренняя почта и др. (через `extUser-picker-view`).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- **Админ загружает фото** (`ExtUserEdit`) → если у пользователя уже есть `userAvatar`, показывается диалог: «только officialPhoto» или «оба слота»; если `userAvatar` пуст — фото пишется в `officialPhoto` и дублируется в `userAvatar`. Старый файл в слоте удаляется из хранилища.
- **Пользователь в настройках** (`ExtSettingsWindow`) → загрузка только в `userAvatar`; старый личный файл удаляется.
- **Обработка изображения** → перед сохранением в хранилище `AvatarImageUploadHelper` вызывает [ImageProcessingService](../services/ImageProcessingService.md) (лимит `targetImageSize` px, выходной формат `targetImageFormat`, по умолчанию PNG 1024); мелкие файлы не перекодируются.
- **Отображение** → `ExtUser.resolveProfilePhoto()` + `FileDescriptorImageHelper.setUserProfilePhoto()`.

---

## 1. Архитектура Сущности (Data Model Layer)

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.itpearls.entity.ExtUser` |
| **CUBA name** | `itpearls_ExtUser` |
| **Таблица** | `SEC_USER` (`@Extends(User.class)`) |
| **DTYPE** | `itpearls_ExtUser` |

### Фото-профиль (FK → `SYS_FILE`)

| Поле Java | Колонка БД | Fetch | Описание |
|-----------|------------|-------|----------|
| `officialPhoto` | `OFFICIAL_PHOTO_ID` | LAZY | Официальное фото (админ) |
| `userAvatar` | `USER_AVATAR_ID` | LAZY | Личный аватар (пользователь) |
| `fileImageFace` *(deprecated)* | `IMAGE_ID` | LAZY | Legacy; мигрируется в новые слоты |

Загрузка в оба слота проходит через [ImageProcessingService](../services/ImageProcessingService.md) (сжатие/масштабирование по `HunttechImageConfig`).

Миграция: `modules/core/db/update/postgres/26/260629-2-updateSecUser*.sql`

### Прочие поля ExtUser

Почта: `smtpServer`, `smtpPort`, `smtpPassword`, `imap*`, `pop3*`; флаги `statistics`, `dashboards`.

---

## 2. Интерфейсный Слой (UI & Layout)

### Views (`views.xml`)

| View | Ключевые поля фото |
|------|-------------------|
| `extUser-view` | `officialPhoto`, `userAvatar`, `fileImageFace` (_local) |
| `extUser-picker-view` | `userAvatar`, `officialPhoto`, `fileImageFace` (_minimal) |

### Экраны

- **ExtUserEdit** — upload `officialPhoto`; preview через `resolveProfilePhoto()`; диалог при конфликте с `userAvatar`.
- **ExtSettingsWindow** — upload `userAvatar` на `extUserDs`; commit `ExtUser` + `UserSettings`.
- **MyPhotoWidget** — `FileDescriptorImageHelper.setUserProfilePhoto()`.

---

## 3. Бизнес-логика (Controller Layer)

`ExtUser.resolveProfilePhoto()` — единая точка приоритета отображения.

`ExtUserChangedListener` — автосоздание `UserSettings` при создании пользователя (без фото).

---

## 4. Взаимодействие компонентов

- Browse/edit пользователей подменены в `web-screens.xml` (`sec$User.*` → `extuser/*`).
- Аватары рекрутёров в вакансиях, комментариях, внутренней почте — через `resolveProfilePhoto()` и обновлённый `extUser-picker-view`.

---

## 5. Инструкция по развертыванию (Deployment Guide)

После деплоя кода выполнить миграции БД (`updateDb` / Liquibase). Скрипты `260629-2-updateSecUser.sql` добавляют колонки и переносят данные из `IMAGE_ID` и `ITPEARLS_USER_SETTINGS.IMAGE_ID`.

Права: новые поля на `SEC_USER` покрываются существующими Read/Update на `sec$User`; Create на `sys$FileDescriptor` — без изменений.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-29 | Документация: cross-link на [ImageProcessingService](../services/ImageProcessingService.md) для обработки загружаемых фото |
| 2026-06-29 | Два слота фото: `officialPhoto` (админ) и `userAvatar` (личный); миграция с `fileImageFace` / `UserSettings.fileImageFace`; диалог в ExtUserEdit; ExtSettingsWindow → userAvatar; `resolveProfilePhoto()` и `FileDescriptorImageHelper.setUserProfilePhoto()` |
| 2026-06-27 | Динамический аватар в ExtUserEdit (legacy UserSettings.fileImageFace) |
