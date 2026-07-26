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
- `AfterShow` → Vaadin connector уже присоединён к UI → ресурс регистрируется скрытым `Image` по ключу `src`;
- сохранён пользовательский файл с маркером `hrm-main-background-` → файл регистрируется как Vaadin resource → отображается только он;
- персональный файл отсутствует, очищен или недоступен → определяется активная тема → случайно выбирается вариант `0..9`;
- неизвестная тема → используется безопасная палитра `hover`;
- зарегистрированный URL → один локальный CSS-класс назначается `mainVBox` и `mainDashboard` → dashboard не перекрывает фон родителя;
- ошибка декоративного ресурса → записывается warning → открытие главного экрана продолжается;
- уведомления, dashboard и проверки резервов → выполняются наследованным `ExtMainScreen` без изменений.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| Screen ID | `hrmMainScreen` |
| Базовый controller | `ExtMainScreen` |
| Рабочие поверхности фона | `mainVBox`, `mainDashboard` |
| Владелец динамического ресурса | скрытый Vaadin `Image` |
| Ключ ресурса connector | `src` |
| Lifecycle применения | `AfterShowEvent` |
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
         └─ mainVBox [background + hidden Image resource holder]
            └─ mainDashboard [тот же локальный background class]
```

## 4. Регистрация ресурса и причина исправления

`ResourceReference` формирует рабочий connector URL только после того, как владелец ресурса присоединён к текущему UI. Первоначальная реализация выполнялась в `BeforeShow` и передавала ресурс напрямую в `ResourceReference`, не закрепляя его за компонентом. Исключение подавлялось защитным `catch`, поэтому главный экран открывался, но системный SVG и пользовательский файл не отображались.

Исправленная последовательность:

1. `AfterShowEvent` подтверждает присоединение экранных компонентов к UI.
2. Скрытый Vaadin `Image` получает `StreamResource`.
3. `Image` добавляется в `mainVBox`, регистрируя ресурс по ключу `src`.
4. `ResourceReference` получает обслуживаемый URL.
5. Локальный CSS-класс с этим URL назначается `mainVBox` и `mainDashboard`.

## 5. Генерация каталога

`MainScreenBackgroundService.resolveForUser()` сначала пытается загрузить маркированный пользовательский файл. Случайный индекс вычисляется только при отсутствии корректного персонального ресурса. Это обеспечивает строгий приоритет пользовательского выбора.

Каталог содержит палитры:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Для каждой палитры доступны варианты `0..9`: окружности, диагональные полосы, волны, угловые дуги, мягкая сетка, плавающие карточки, точечный поток, слоистые холмы, пересекающиеся линии и сбалансированные формы. Каждый ресурс формируется в памяти как SVG `1920 × 1080`, MIME `image/svg+xml`, с `viewBox` и `preserveAspectRatio="xMidYMid slice"`.

## 6. Ограничения

- не изменяется `ExtMainScreen.java`;
- не изменяются dashboard code, timers, loaders и notification queries;
- не меняются entity, БД, Liquibase и production;
- фон является presentation-only и не должен блокировать открытие экрана;
- случайный выбор выполняется один раз при создании главного экрана после входа.

## 7. Проверки Hermes

Обязательны профильный `MainScreenBackgroundContractTest 7/7`, `ScreenViewIntegrityTest 8/8`, `buildScssThemes`, `clean assemble`, local deploy, HTTP 200 и visual smoke семи тем. Для каждой темы проверить несколько повторных входов, пользовательский файл, сетевой ответ connector resource и возврат к каталогу после очистки.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Исправлена регистрация динамического ресурса после `AfterShow`; фон назначается `mainVBox` и `mainDashboard`, проверены семь палитр и десять SVG-вариантов |
| 2026-07-26 | Создано расширение главного экрана с приоритетным пользовательским фоном и каталогом 7 × 10 нейтральных SVG-композиций |
