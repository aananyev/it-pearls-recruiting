# HrmMainScreen — тематический фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Сервис выбора ресурса: `MainScreenBackgroundService`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран является основной рабочей точкой dashboard HRM HuntTech. Фон персонализирует рабочую область, но не должен влиять на меню, widgets, уведомления, favicon, резервы и загрузку бизнес-данных.

Стандартные фоны являются частью визуальной темы и поставляются как статические JPG-файлы. Пользовательский фон хранится в FileStorage, имеет приоритет над тематическим каталогом и должен открываться через штатный механизм ресурсов Vaadin, а не через прямой `dispatch/download` URL.

Недоступность физического файла при сохранённом `FileDescriptor` не блокирует вход пользователя. Экран временно возвращается к системному фону, не удаляя metadata: после восстановления FileStorage персональный фон снова применяется автоматически.

## UI Context & Navigation

Экран создаётся через Screens API CUBA Platform 7.3:

- `cuba.web.mainScreenId=hrmMainScreen`;
- `@UiController("hrmMainScreen")`;
- `@UiDescriptor("hrm-main-screen.xml")`;
- legacy-регистрация `hrmMainScreen` в `web-screens.xml` отсутствует.

Персональный файл задаётся в `SettingsWindow` → «Интерфейс» → «Фон главного экрана». После успешного «ОК» текущая браузерная вкладка получает `MainScreenBackgroundChangedEvent` и обновляет фон без повторного входа.

## Behavior Summary

- вход → создаётся `HrmMainScreen` → наследуемая бизнес-логика `ExtMainScreen` выполняется без изменений;
- `AfterShowEvent` → Vaadin UI готов → `MainScreenBackgroundService` разрешает ресурс;
- `UserSettings.fileImageFace` содержит файл с префиксом `hrm-main-background-` → сервис открывает поток через `FileLoader.openStream()`;
- пользовательский файл доступен → поток полностью читается через `readAllBytes()` → создаётся `StreamResource` с исходным именем и MIME-типом;
- `StreamResource` получен → `HrmMainScreen` регистрирует его через скрытый `Image` в Vaadin connector tree → `ResourceReference` формирует URL;
- URL начинается с `app://APP` → внутренний префикс удаляется → CSS получает HTTP-путь ресурса;
- файл отсутствует или FileStorage возвращает ошибку → исключение перехватывается → descriptor сохраняется в БД → выбирается системный фон;
- пользовательского descriptor нет → выбирается случайный `1.jpg … 10.jpg` из `backgrounds/` активной темы;
- предыдущий индекс темы известен в `UserSession` → немедленное повторение исключается;
- ресурс разрешён → динамический локальный CSS назначает фон только `mainVBox`;
- dashboard получает прозрачный локальный стиль → фон остаётся видимым под рабочими widgets;
- SettingsWindow успешно сохранён → публикуется UI-scoped event → фон обновляется без перезахода;
- ошибка декоративного фона → записывается warning → открытие главного экрана не блокируется.

## Каталог тематических фонов

Для каждой поддерживаемой темы репозиторий содержит ровно десять JPG-файлов размером `1920 × 1080`:

```text
modules/web/themes/{theme}/backgrounds/
├── 1.jpg
├── 2.jpg
├── ...
└── 10.jpg
```

Поддерживаемые темы:

```text
halo
havana
helium
hover
hunttech-modern
hunttech-modern-light
hunttech-modern-dark
```

Изображения нейтральные, без текста и логотипов, с палитрой соответствующей темы. В Java-коде отсутствуют SVG-шаблоны, палитры и генераторы геометрии.

## Разрешение системного ThemeResource

`MainScreenBackgroundService` хранит индекс варианта в диапазоне `0..9`, а имя файла формирует как `variant + 1`:

```java
new ThemeResource("backgrounds/" + (variant + 1) + ".jpg")
```

`ThemeResource` разрешается относительно активной Vaadin-темы:

```text
VAADIN/themes/{activeTheme}/backgrounds/{1..10}.jpg
```

Allowlist поддерживаемых тем используется для нормализации имени и отдельного session-ключа. Неизвестное или отсутствующее имя темы нормализуется к `hover`.

Системные фоны не используют `StreamResource`, FileStorage или Vaadin connector registration и не изменяются исправлением пользовательского фона.

## Пользовательский фон

Цепочка сохранения не меняется:

```text
Upload → MainScreenBackgroundImageProcessor → FileStorage
→ UserSettings.fileImageFace
```

Контракт пользовательского файла:

- имя начинается с `hrm-main-background-`;
- файл хранится в FileStorage;
- файл имеет приоритет над тематическим ресурсом;
- legacy-фотография в `fileImageFace` без указанного префикса не считается фоном;
- при Cancel временно созданные файлы очищаются по существующему контракту SettingsWindow;
- файл открывается через `FileLoader.openStream()`;
- байты читаются через `InputStream.readAllBytes()`;
- создаётся `StreamResource(() -> new ByteArrayInputStream(bytes), fileName)`;
- MIME-тип назначается по расширению: PNG, JPEG или WebP;
- `cacheTime = -1`, поэтому Vaadin не добавляет ограничение времени кэширования ресурса;
- `FileStorageException` или `IOException` преобразуются в системный fallback;
- недоступный `FileDescriptor` не удаляется из БД;
- `ExternalResource` и `FileDescriptorImageHelper.buildDispatchDownloadUrl()` для пользовательского фона не применяются.

Загрузка в SettingsWindow ограничена `15 МБ` (`fileSizeLimit="15728640"`). Нормализованный результат дополнительно ограничивается процессором изображения, поэтому полное чтение потока в память не используется для неограниченных пользовательских файлов.

## Регистрация StreamResource в HrmMainScreen

Архитектура контроллера:

```text
mainVBox (динамический CSS-класс фона)
├── Image 0 × 0 (владелец StreamResource в connector tree)
└── mainDashboard (локальный прозрачный стиль)
```

Контроллер разрешает два штатных типа:

1. `ThemeResource` → `VAADIN/themes/{theme}/backgrounds/{n}.jpg`;
2. `StreamResource` → `registerBackgroundResource()` → Vaadin connector URL.

`registerBackgroundResource()`:

1. удаляет предыдущий скрытый holder из родительского контейнера;
2. создаёт `Image` размером `0 × 0` с новым `StreamResource`;
3. добавляет holder в `mainVBox`, чтобы ресурс был зарегистрирован в connector tree;
4. получает URL через `ResourceReference.create(resource, holder, "src").getURL()`;
5. заменяет префикс `app://APP` на пустую строку;
6. отклоняет пустой URL как ошибку регистрации.

После разрешения URL контроллер:

1. создаёт уникальный локальный CSS-класс;
2. назначает `background-image`, `background-position`, `background-repeat` и `background-size` на `mainVBox`;
3. устанавливает `background-size: 100% 100%`;
4. оставляет dashboard прозрачным.

## Масштабирование на дисплеях

Фон растягивается независимо по ширине и высоте контейнера `mainVBox`. Контракт сознательно использует `100% 100%`, а не `cover`:

- вся рабочая область главного экрана заполнена;
- пустые полосы отсутствуют;
- изображение не обрезается по краям;
- при несовпадении соотношения сторон допускается изменение визуальных пропорций;
- логика выбора тематического или пользовательского ресурса не зависит от разрешения дисплея.

## Runtime-маркеры

| Элемент | Маркер | Назначение |
|---|---|---|
| `mainVBox` | `data-hrm-main-background="applied"` | подтверждает выполнение background lifecycle |
| `mainVBox` | `data-hrm-main-controller="HrmMainScreen"` | подтверждает фактический root controller |

Назначенные через `HtmlAttributes` значения проверяются integration-тестом. Реальный computed style, HTTP-статус connector-ресурса и screenshot проверяются Hermes после clean deploy.

## Выбор системного варианта

Для каждой из семи тем сохраняется последний индекс `0..9` в `UserSession`. Следующий индекс выбирается из девяти остальных вариантов. Состояние относится только к текущей пользовательской сессии и не записывается в БД.

## Событие обновления

`MainScreenBackgroundChangedEvent` реализует `UiEvent`. Событие:

- публикуется только после успешной commit-цепочки SettingsWindow;
- доставляется синхронно в текущую браузерную вкладку;
- не обновляет чужие UI-сеансы;
- не публикуется при Cancel или неуспешной загрузке.

## Автоматические проверки

`MainScreenBackgroundContractTest` проверяет:

1. использование `ThemeResource` с путём `backgrounds/{1..10}.jpg`;
2. наличие allowlist всех семи тем и десяти JPG-файлов в каждой теме;
3. применение `background-size: 100% 100%` и отсутствие `cover`;
4. перехват ошибок `FileLoader.openStream()` и системный fallback;
5. чтение пользовательского файла через `readAllBytes()`;
6. создание `StreamResource` и назначение MIME-типа;
7. вызов `registerBackgroundResource()`;
8. регистрацию через `ResourceReference` и holder `backgroundResourceHolder`;
9. преобразование `app://APP`;
10. отсутствие `ExternalResource` в `HrmMainScreen`;
11. неизменность SettingsWindow/FileStorage-цепочки.

`HrmMainScreenIntegrationTest` не изменяется этой задачей и продолжает проверять фактический root screen, `mainVBox`, dashboard, DOM-маркеры и обновление системного URL по `MainScreenBackgroundChangedEvent`.

## Проверки Hermes

Hermes проверяет точный HEAD PR:

- `git diff --check`;
- compile web/core tests;
- `MainScreenBackgroundContractTest`;
- `HrmMainScreenIntegrationTest` без изменения его исходного кода;
- `ScreenViewIntegrityTest 8/8`;
- сборку SCSS семи тем;
- `clean assemble`;
- clean local deploy;
- `/hrm/` = HTTP 200;
- системный фон для каждой из семи тем;
- пользовательский ресурс имеет connector URL и не содержит `/dispatch/download?f=`;
- пользовательский ресурс отвечает HTTP 200 и корректным `image/*` MIME-типом;
- пользовательский фон отображается после сохранения SettingsWindow и после повторного входа;
- отсутствующий физический файл не блокирует вход и включает системный fallback;
- descriptor не удаляется при временной недоступности FileStorage;
- отсутствуют Tomcat critical errors, P1 и P2.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Пользовательский фон переведён на `StreamResource` с байтами FileStorage; URL регистрируется через Vaadin connector, `app://APP` преобразуется в HTTP-путь; системные `ThemeResource` не изменены |
| 2026-07-26 | Пользовательский фон временно переводился на прямой FileStorage dispatch URL; ошибка чтения файла включала системный fallback без удаления descriptor |
| 2026-07-26 | Фон главного экрана переведён с `cover` на точное растягивание `100% × 100%` |
| 2026-07-26 | Стандартные фоны вынесены из Java runtime-SVG в каталоги семи тем: 7 × 10 JPG |
| 2026-07-26 | Добавлены UI-scoped refresh, антиповтор системного варианта и screen-level integration test |
| 2026-07-26 | Синхронизированы оба `mainScreenId`; удалена несовместимая legacy-регистрация |
