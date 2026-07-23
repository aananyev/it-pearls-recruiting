# UserSettings — индивидуальные настройки пользователя

> Сущность HRM HuntTech: `com.company.hunttech.entity.UserSettings`.  
> Entity name: `hunttech_UserSettings`.  
> Таблица: `HUNTTECH_USER_SETTINGS`.  
> Связанный экран: [ExtSettingsWindow](../../ui/ExtSettingsWindow_Spec.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`UserSettings` хранит персональные настройки, которые относятся к конкретной учётной записи HRM HuntTech и не должны смешиваться с глобальной конфигурацией администратора. Сущность уже используется для почтовых параметров пользователя; новый флаг `preferPersonalAiApiSettings` фиксирует намерение пользователя предпочитать собственные настройки API нейросетей.

Флаг не является API-ключом и не определяет конкретного провайдера. Персональные ключи, модели и активность подключений по-прежнему хранятся в `UserAiConfiguration`.

### UI Context & Navigation

Настройка доступна в стандартном окне «Параметры» → вкладка AI. Checkbox расположен над таблицей персональных AI-подключений и связан с `userSettingsDs`.

Связи:

- `UserSettings.user` → владелец настроек `ExtUser`;
- `UserSettings.preferPersonalAiApiSettings` → глобальное предпочтение пользователя по источнику API;
- `UserAiConfiguration` → конкретные персональные подключения к провайдерам;
- административные API-настройки → отдельный системный контур, который этой задачей не изменяется.

### Behavior Summary

- открытие SettingsWindow → загружается запись `UserSettings` текущего пользователя;
- запись отсутствует → создаётся несохранённый экземпляр со значением `false`;
- значение в legacy-записи отсутствует → контроллер применяет безопасное `false`;
- checkbox включён → в `UserSettings` сохраняется `true`;
- checkbox выключен → сохраняется `false`;
- сохранение окна → флаг фиксируется в общей транзакции SettingsWindow;
- вызов AI-сервиса → текущей задачей не изменяется, алгоритм маршрутизации будет реализован отдельно.

## 1. Поля

| Поле | Колонка | Тип | Назначение |
|---|---|---|---|
| `user` | `USER_ID` | `ExtUser`, one-to-one | владелец персональных настроек |
| `preferPersonalAiApiSettings` | `PREFER_PERSONAL_AI_API_SETTINGS` | `Boolean`, not null | предпочитать личные настройки API; по умолчанию `false` |
| `fileImageFace` | `IMAGE_ID` | `FileDescriptor` | legacy-фотография пользователя |
| `smtp*`, `pop3*`, `imap*` | соответствующие колонки | почтовые параметры | существующий контракт персональной почты |

## 2. Семантика флага API

`false` сохраняет действующее поведение и не требует наличия персональных AI-конфигураций. Значение `true` означает только предпочтение пользователя. Правила выбора провайдера, fallback на административный аккаунт и недоступность отдельных функций при отсутствии личных ключей в эту задачу не входят.

До реализации отдельного алгоритма сервисы не должны интерпретировать поле самостоятельно или частично менять маршрутизацию вызовов.

## 3. Представления и загрузка

SettingsWindow использует `userSettings-view`, расширяющий `_local`. Поэтому `preferPersonalAiApiSettings` включается в view как локальный скалярный атрибут без отдельного расширения графа сущности.

Запись загружается JPQL:

```jpql
select e from hunttech_UserSettings e where e.user = :currentUser
```

## 4. База данных

- PostgreSQL update script: `modules/core/db/update/postgres/26/260723-1-addPreferPersonalAiApiSettings.sql`;
- Liquibase: `modules/core/db/changelog/260723-1-addPreferPersonalAiApiSettings.xml`;
- колонка: `BOOLEAN NOT NULL DEFAULT FALSE`;
- существующие строки получают `false`, поэтому поведение до внедрения алгоритма маршрутизации не меняется.

## 5. Проверки

`UserSettingsAiApiPreferenceTest` проверяет:

1. наличие Boolean-свойства в CUBA Metadata;
2. значение `false` у нового экземпляра;
3. binding checkbox к `userSettingsDs`;
4. наличие PostgreSQL- и Liquibase-миграций.

Дополнительно обязательны `ScreenViewIntegrityTest`, Data View Integrity для SettingsWindow и общая сборка проекта.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-23 | Добавлен `preferPersonalAiApiSettings`, checkbox во вкладке AI, безопасный default `false`, миграции и автотесты; маршрутизация API намеренно не изменена |
