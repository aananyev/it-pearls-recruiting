# HrmMainScreen — персональный фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Главный экран остаётся рабочей точкой входа в dashboard HRM HuntTech, но получает спокойный нейтральный фон. Фон персонализирует рабочее место без изменения dashboard, уведомлений, резервов кандидатов, favicon, меню или иных бизнес-сценариев.

Для каждой из семи зарегистрированных тем сервис формирует десять самостоятельных SVG-композиций светлого нейтрального оттенка. Если пользователь не выбрал собственный файл, одна композиция выбирается случайно при каждом новом входе.

### UI Context & Navigation

Экран создаётся после успешной аутентификации через `cuba.web.mainScreenId=hrmMainScreen`. `HrmMainScreen` наследует действующий `ExtMainScreen`, а его descriptor наследует существующий dashboard `recruiting-dashboard`.

Персональный файл задаётся в `SettingsWindow` на вкладке «Интерфейс». Других входов и переходов функция не добавляет.

### Behavior Summary

- вход пользователя → создаётся `HrmMainScreen` → сервис проверяет персональный фон;
- сохранён пользовательский файл с маркером `hrm-main-background-` → файл регистрируется как Vaadin resource → отображается только он;
- персональный файл отсутствует, очищен или недоступен → определяется активная тема → случайно выбирается вариант `0..9`;
- неизвестная тема → используется безопасная палитра `hover`;
- ошибка декоративного ресурса → записывается warning → открытие главного экрана продолжается;
- уведомления, dashboard и проверки резервов → выполняются наследованным `ExtMainScreen` без изменений.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| Screen ID | `hrmMainScreen` |
| Базовый controller | `ExtMainScreen` |
| Рабочий контейнер фона | `mainVBox` |
| Сервис | `MainScreenBackgroundService` |
| Каталог | 7 тем × 10 SVG-вариантов |
| Формат встроенных изображений | SVG 1920 × 1080 |
| Пользовательский формат | PNG, JPG, JPEG, WEBP до 15 МБ |

## 2. Связь с моделью данных

Новых entity, полей, таблиц, Liquibase-скриптов и views нет. Сервис читает существующую запись `hunttech_UserSettings` запросом:

```jpql
select e from hunttech_UserSettings e where e.user = :currentUser
```

Пользовательский файл хранится в уже существующей связи `UserSettings.fileImageFace`. Чтобы не принять историческую legacy-фотографию за фон, пользовательским считается только `FileDescriptor`, имя которого начинается с `hrm-main-background-`.

## 3. Иерархия экрана

```text
HrmMainScreen
└─ ExtMainScreen
   └─ ext-main-screen.xml
      └─ workArea
         └─ mainVBox [динамический background resource]
            └─ mainDashboard (recruiting-dashboard)
```

## 4. Модель поведения

`MainScreenBackgroundService.resolveForUser()` сначала пытается загрузить маркированный пользовательский файл. Случайный индекс вычисляется только при отсутствии корректного персонального ресурса. Это обеспечивает строгий приоритет пользовательского выбора.

Встроенные SVG не сохраняются в БД и не создают файлов в `fileStorage`: они формируются в памяти как `StreamResource`. Для активной сессии ресурс регистрируется через `ResourceReference`, а URL применяется только к уникальному CSS-классу текущего `mainVBox`.

## 5. Ограничения

- не изменяется `ExtMainScreen.java`;
- не изменяются dashboard code, timers, loaders и notification queries;
- не меняются entity, БД, Liquibase и production;
- фон является presentation-only и не должен блокировать открытие экрана;
- случайный выбор выполняется один раз при создании главного экрана после входа.

## 6. Проверки Hermes

Обязательны профильный `MainScreenBackgroundContractTest`, `ScreenViewIntegrityTest 8/8`, `buildScssThemes`, `clean assemble`, local deploy, HTTP 200 и visual smoke семи тем. Для каждой темы проверить несколько повторных входов, пользовательский файл и возврат к каталогу после очистки.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Создано расширение главного экрана с приоритетным пользовательским фоном и каталогом 7 × 10 нейтральных SVG-композиций |
