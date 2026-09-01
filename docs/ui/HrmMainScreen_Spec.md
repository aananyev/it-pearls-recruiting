# HrmMainScreen — главный dashboard и тематический фон

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Базовый экран: `ExtMainScreen`.  
> Dashboard: persistent dashboard с кодом `recruiting-dashboard`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран является основной рабочей областью HRM HuntTech. Он объединяет стандартное меню CUBA Platform, `WorkArea`, персональный dashboard рекрутера и тематический фон. Dashboard должен визуально соответствовать общему контракту Edit-форм: использовать ту же геометрию карточек, типографику заголовков, высоту управляющих элементов и состояния focus/hover, но не переносить тёмную sidebar Edit-форм в рабочую область главного экрана.

Фон персонализирует интерфейс, но не должен влиять на меню, widgets, уведомления, резервы, favicon и загрузку бизнес-данных. Dashboard остаётся прозрачным относительно фонового слоя, а читаемость обеспечивают theme-aware поверхности отдельных виджетов.

## UI Context & Navigation

Экран создаётся через Screens API CUBA Platform 7.3:

- `cuba.web.mainScreenId=hrmMainScreen`;
- `@UiController("hrmMainScreen")`;
- `@UiDescriptor("hrm-main-screen.xml")`;
- legacy-регистрация `hrmMainScreen` в `web-screens.xml` отсутствует.

`ext-main-screen.xml` сохраняет стандартную `WorkArea` и в `initialLayout` размещает `mainDashboard` с кодом `recruiting-dashboard`. Открытие browse/edit-экранов из меню или виджета выполняется стандартными механизмами CUBA и не меняется визуальной задачей.

Путь настройки персонального изображения:

```text
SettingsWindow → Интерфейс → Фон главного экрана
```

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| Открытие главного экрана | Пользовательский descriptor отсутствует | Выбирается случайный системный `ThemeResource` активной темы |
| Открытие главного экрана | Существует пользовательский фон | Файл читается из FileStorage и применяется как `data:` URI |
| Создание dashboard | Найден persistent dashboard `recruiting-dashboard` | Dashboard отображает сохранённую модель и существующие widgets |
| Применение темы | Активна одна из семи тем | Подключается идентичный `recruiter-dashboard-shared-styles.scss` с theme-aware цветами |
| Отображение widget | Корень использует `widget-border` или `widget-border-line` | Поверхность получает геометрию `edit-card` без изменения fragment lifecycle |
| Уменьшение viewport | Ширина не более 1366 px | Сокращаются внешние и внутренние отступы, глобальный horizontal scroll не создаётся |
| Обновление dashboard | Срабатывает существующий timer/update event | Данные обновляются прежним кодом; SCSS не запускает loaders или service calls |
| Сохранение SettingsWindow | Commit успешен | Публикуется UI-scoped событие, фон обновляется без повторного входа |

## 1. Точка вызова и контекст

- Root screen: `hrmMainScreen`.
- Наследование Java: `HrmMainScreen → ExtMainScreen → MainScreen`.
- Наследование XML: `hrm-main-screen.xml → ext-main-screen.xml → main-screen.xml`.
- Начальный dashboard: `mainDashboard`, code `recruiting-dashboard`, `timerDelay="60"`.
- Поддерживаемые темы: `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`.

## 2. Связь с моделью данных

Визуальный контракт не добавляет data containers, loaders, JPQL, views, entity или сервисы. Фактический состав dashboard и параметры widgets продолжают определяться persistent model в таблицах Dashboard Add-on.

Не изменяются:

- код `recruiting-dashboard`;
- dashboard parameters;
- frame IDs существующих widgets;
- `timerDelay`;
- права доступа;
- запросы и lifecycle `ScreenFragment`;
- бизнес-действия внутри widgets.

## 3. Иерархия и взаимосвязь форм

```text
HrmMainScreen
└── ExtMainScreen
    └── WorkArea
        └── initialLayout
            └── mainVBox
                └── mainDashboard (recruiting-dashboard)
                    └── существующие Dashboard Add-on widgets
```

Переход из widget в `OpenPositionEdit`, `JobCandidateEdit`, browse взаимодействий или другой существующий экран остаётся ответственностью конкретного fragment. Главный экран не заменяет рабочие формы и не меняет их CUBA-контракты.

## 4. Модель поведения и интерактивность

### 4.1. Фон

Системные фоны находятся в каталогах:

```text
modules/web/themes/{theme}/backgrounds/{1..10}.jpg
```

Пользовательский фон загружается через `SettingsWindow`, хранится в FileStorage и имеет приоритет. `HrmMainScreen` применяет:

```css
background-position: center center;
background-repeat: no-repeat;
background-size: 100% 100%;
```

`horizontalWrap`, `mainVBox` и `mainDashboard` занимают 100% доступной ширины и высоты. `mainDashboard` получает runtime-класс `hrm-dashboard-transparent`, поэтому фоновое изображение остаётся видимым между карточками.

### 4.2. Обновление

- `AfterShowEvent` вызывает первичное применение фона;
- `MainScreenBackgroundChangedEvent` обновляет фон после успешного сохранения SettingsWindow;
- `timerDelay="60"` сохраняет существующий контракт обновления dashboard;
- SCSS не меняет частоту обновления и не выполняет серверных операций.

## 5. Логика управляющих элементов

В этой задаче не добавляются новые кнопки, фильтры или actions. Существующие управляющие компоненты внутри widgets получают только визуальные параметры:

- высота текстовых и date/filter controls — 38 px;
- радиус — 5 px;
- theme-aware border;
- заметный keyboard focus с акцентом HRM HuntTech `#ffb11b`;
- кнопки link/borderless не преобразуются в крупные toolbar-кнопки.

## 6. Визуальная компоновка элементов

## Визуальный контракт персонального dashboard

### 6.1. Корневые стили

`mainDashboard` получает:

```xml
stylename="edit-workspace recruiter-dashboard-root"
```

- `edit-workspace` связывает главный экран с общим UI API Edit-форм;
- `recruiter-dashboard-root` ограничивает все dashboard-specific селекторы;
- глобальные переопределения `.v-label`, `.v-button`, `.v-table`, `.v-panel` запрещены;
- тёмная `edit-sidebar` на главном экране не создаётся.

### 6.2. Соответствие общему Edit-контракту

| Dashboard role | Источник общего контракта | Результат |
|---|---|---|
| `widget-border`, `widget-border-line` | `edit-card` | panel background, border 1 px, radius 8 px, padding 16×20 px, лёгкая тень |
| `widget-table-header` | `edit-card-title` | 15 px, 700, line-height 22 px, перенос длинного текста |
| вложенные `Panel` | `edit-accordion-section` | radius 8 px, caption 17 px, min-height 50 px |
| filter/date/text controls | `edit-form-control` | высота 38 px, radius 5 px, локальный focus ring |
| таблицы и DataGrid | общая поверхность workspace | theme-aware header, спокойные zebra rows, явный hover/selected |
| responsive root | правила Edit-форм до 1366 px | уменьшенные отступы без горизонтальной прокрутки страницы |

### 6.3. Theme-aware реализация

Файл `recruiter-dashboard-shared-styles.scss` хранится синхронной копией во всех семи темах. Геометрия, размеры, порядок правил и responsive-breakpoints идентичны. Различия цвета и контраста формируются только через переменные активной темы:

- `$v-font-color`;
- `$v-panel-background-color`;
- `$v-app-background-color`;
- `$v-selection-color`.

Фирменный акцент `#ffb11b` используется для focus, hover/selected emphasis и критически важных визуальных маркеров, но не заменяет текстовое описание статуса.

### 6.4. Responsive

При ширине до 1366 px:

- padding dashboard уменьшается с 16 до 10 px;
- padding карточки уменьшается до 12×14 px;
- заголовок виджета уменьшается до 14 px;
- текст строк таблицы может уменьшаться до 12 px;
- высота controls остаётся 38 px;
- horizontal scroll всего главного экрана запрещён.

При ширине до 1280 px padding root уменьшается до 8 px, карточки — до 10×12 px. Перекомпоновка самих widgets остаётся ответственностью responsive layout persistent dashboard и не имитируется CSS-перестановкой DOM.

### 6.5. Состояния

- hover не изменяет размеры и не вызывает layout shift;
- selected использует фон с прозрачным `#ffb11b`, а не только цвет текста;
- focus отображает outline/ring и доступен с клавиатуры;
- disabled/read-only сохраняют нативную семантику CUBA;
- пустые/error/loading states конкретного widget не подменяются общим SCSS.

### 6.6. Осознанные отклонения от Edit-форм

1. Sidebar отсутствует, поскольку dashboard является обзорной рабочей поверхностью, а не формой редактирования одной сущности.
2. Корень dashboard остаётся прозрачным для сохранения тематического фона.
3. Карточные поверхности применяются к widgets, а не к единой правой workspace-карточке.
4. Persistent model и responsive layout Dashboard Add-on не изменяются CSS-задачей.

## Runtime-маркеры

| Элемент | Маркер | Назначение |
|---|---|---|
| `mainVBox` | `data-hrm-main-background="applied"` | подтверждает выполнение background lifecycle |
| `mainVBox` | `data-hrm-main-controller="HrmMainScreen"` | подтверждает фактический root controller |
| `mainDashboard` | `recruiter-dashboard-root` | ограничивает visual contract главного dashboard |

## Автоматические проверки

`MainScreenBackgroundContractTest` продолжает проверять:

- конфигурацию root screen;
- 7 × 10 тематических JPG;
- `StreamResource` и `data:` URI пользовательского фона;
- fullscreen-геометрию;
- UI-scoped refresh;
- отсутствие legacy connector/background layer.

`MainScreenDashboardSharedStyleContractTest` проверяет:

- сохранение `recruiting-dashboard` и `timerDelay="60"`;
- наличие `edit-workspace recruiter-dashboard-root`;
- идентичность SCSS partial во всех семи темах;
- import/include mixin в каждом `styles.scss`;
- геометрию 8 px / 38 px / лёгкую тень;
- responsive breakpoint 1366 px;
- отсутствие неограниченных глобальных Vaadin-селекторов;
- синхронизацию настоящей UI-спецификации.

## Проверка Hermes

Hermes проверяет точный HEAD PR:

- branch HEAD = PR HEAD;
- `base=master`, conflicts `NONE`;
- `git diff --check`;
- профильный `MainScreenDashboardSharedStyleContractTest`;
- `MainScreenBackgroundContractTest`;
- `ScreenViewIntegrityTest 8/8`;
- `:app-web:buildScssThemes` для всех семи тем;
- `clean assemble` → `BUILD SUCCESSFUL`;
- clean local deploy;
- `http://localhost:8080/hrm/` → HTTP 200;
- browser smoke в семи темах на 1366×768, 1920×1080, 1920×1200 и ultrawide;
- фон виден между карточками;
- widgets, таблицы, фильтры и действия работают без регрессии;
- global horizontal scroll отсутствует;
- Tomcat critical errors `NONE`, P1=0, P2=0.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-02 | `recruiting-dashboard` приведён к общему визуальному контракту Edit-форм через `edit-workspace recruiter-dashboard-root`; добавлен идентичный shared SCSS для семи тем без изменения persistent model, widgets и бизнес-логики |
| 2026-07-29 | Fullscreen-геометрия `horizontalWrap`, `mainVBox` и `mainDashboard` закреплена в XML и контрактном тесте |
| 2026-07-27 | Пользовательский фон закреплён как `StreamResource` с MIME и `data:` URI; системные `ThemeResource` не изменены |
| 2026-07-26 | Фон переведён на точное растягивание `100% × 100%`; системные фоны вынесены в каталоги семи тем |
