# UserSettings — индивидуальные настройки пользователя

> Сущность HRM HuntTech: `com.company.hunttech.entity.UserSettings`.  
> Entity name: `hunttech_UserSettings`.  
> Таблица: `HUNTTECH_USER_SETTINGS`.  
> Связанный экран: [ExtSettingsWindow](../../ui/ExtSettingsWindow_Spec.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`UserSettings` хранит персональные настройки, которые относятся к конкретной учётной записи HRM HuntTech и не должны смешиваться с глобальной конфигурацией администратора. Помимо почтовых параметров сущность хранит предпочтения пользователя по источнику API и применению личных промптов.

Флаги не содержат API-ключи и тексты промптов. Персональные ключи, модели и активность подключений по-прежнему хранятся в `UserAiConfiguration`; содержимое личных и системных промптов хранится в профильных сущностях и сервисах.

### UI Context & Navigation

Обе настройки доступны в стандартном окне «Параметры» → вкладка AI. Checkbox расположены в карточке личных предпочтений над таблицей персональных AI-подключений и связаны с `userSettingsDs`.

Связи:

- `UserSettings.user` → владелец настроек `ExtUser`;
- `UserSettings.preferPersonalAiApiSettings` → предпочтение пользователя по источнику API;
- `UserSettings.preferPersonalPrompts` → предпочтение пользователя по применению личных промптов;
- `UserAiConfiguration` → конкретные персональные подключения к провайдерам;
- административные API-настройки и системные промпты → отдельные контуры, которые этой задачей не изменяются.

### Behavior Summary

- открытие SettingsWindow → загружается запись `UserSettings` текущего пользователя;
- запись отсутствует → создаётся несохранённый экземпляр, оба AI-предпочтения равны `true`;
- значение отсутствует в legacy-записи → контроллер применяет безопасное `true`;
- пользователь меняет checkbox → значение изменяется в `userSettingsDs`;
- сохранение окна → оба флага фиксируются в общей транзакции `CommitContext` вместе с остальными пользовательскими настройками;
- повторное открытие → checkbox восстанавливаются из `HUNTTECH_USER_SETTINGS`;
- вызов AI-сервиса → флаги доступны для выбора приоритета, но маршрутизация API и разрешение промптов остаются ответственностью профильных сервисов.

## 1. Поля

| Поле | Колонка | Тип | Назначение |
|---|---|---|---|
| `user` | `USER_ID` | `ExtUser`, one-to-one | владелец персональных настроек |
| `preferPersonalAiApiSettings` | `PREFER_PERSONAL_AI_API_SETTINGS` | `Boolean`, not null | предпочитать личные настройки API; по умолчанию `true` |
| `preferPersonalPrompts` | `PREFER_PERSONAL_PROMPTS` | `Boolean`, not null | предпочитать личные промпты; по умолчанию `true` |
| `fileImageFace` | `IMAGE_ID` | `FileDescriptor` | legacy-фотография пользователя |
| `smtp*`, `pop3*`, `imap*` | соответствующие колонки | почтовые параметры | существующий контракт персональной почты |

## 2. Семантика AI-предпочтений

Значение `true` означает, что при наличии подходящего персонального ресурса система должна рассматривать его раньше административного или системного аналога. Значение `false` разрешает вызывающему сервису применять штатный системный маршрут без приоритета личной настройки.

Флаги являются сохраняемыми предпочтениями, но сами по себе:

- не выбирают конкретного провайдера или модель;
- не проверяют наличие и корректность API-ключа;
- не определяют алгоритм fallback;
- не разрешают конфликт между личным и системным промптом;
- не изменяют права доступа к AI-функциям.

## 3. Представления и загрузка

SettingsWindow использует `userSettings-view`, расширяющий `_local`. Оба Boolean-поля включаются в view как локальные скалярные атрибуты без отдельного расширения графа сущности.

Запись загружается JPQL:

```jpql
select e from hunttech_UserSettings e where e.user = :currentUser
```

После загрузки `ExtSettingsWindow.loadOrCreateUserSettings()` нормализует legacy-null в `true` и устанавливает сущность в `userSettingsDs`.

## 4. Сохранение

Оба checkbox имеют XML-binding:

```text
preferPersonalAiApiSettingsField → userSettingsDs.preferPersonalAiApiSettings
preferPersonalPromptsField → userSettingsDs.preferPersonalPrompts
```

При нажатии `okBtn` контроллер добавляет `userSettings` в общий `CommitContext`:

```java
context.addInstanceToCommit(userSettings);
```

Поэтому оба значения сохраняются атомарно вместе с почтовыми настройками и восстанавливаются при повторном открытии окна.

## 5. База данных

Историческая миграция поля API остаётся неизменной:

- PostgreSQL: `modules/core/db/update/postgres/26/260723-1-addPreferPersonalAiApiSettings.sql`;
- Liquibase: `modules/core/db/changelog/260723-1-addPreferPersonalAiApiSettings.xml`.

Изменение нового поведения выполнено отдельной миграцией:

- PostgreSQL: `modules/core/db/update/postgres/26/260724-1-enablePersonalAiPreferences.sql`;
- Liquibase: `modules/core/db/changelog/260724-1-enablePersonalAiPreferences.xml`;
- `PREFER_PERSONAL_AI_API_SETTINGS` получает default `TRUE`, существующие строки переводятся в `TRUE`;
- добавляется `PREFER_PERSONAL_PROMPTS BOOLEAN NOT NULL DEFAULT TRUE`.

Отдельная миграция не изменяет уже зарегистрированный changeSet и сохраняет корректную историю обновления CUBA.

## 6. Проверки

`UserSettingsAiApiPreferenceTest` проверяет:

1. наличие двух Boolean-свойств в CUBA Metadata;
2. значение `true` у нового экземпляра;
3. binding обоих checkbox к `userSettingsDs`;
4. включение `userSettings` в `CommitContext`;
5. наличие PostgreSQL- и Liquibase-миграций с default `TRUE`;
6. null-safe контракт предпросмотра AI-контекста.

Дополнительно обязательны `ScreenViewIntegrityTest`, Data View Integrity для SettingsWindow, `updateDb` и общая сборка проекта.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-24 | Оба AI-предпочтения включены по умолчанию; добавлен `preferPersonalPrompts`, миграции, binding и проверки сохранения через `UserSettings` |
| 2026-07-23 | Добавлен `preferPersonalAiApiSettings`, checkbox во вкладке AI, безопасный default `false`, миграции и автотесты; маршрутизация API намеренно не изменена |
