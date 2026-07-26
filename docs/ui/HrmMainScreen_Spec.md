# HrmMainScreen — тематический фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Сервис выбора ресурса: `MainScreenBackgroundService`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран является основной рабочей точкой dashboard HRM HuntTech. Фон персонализирует рабочую область, но не должен влиять на меню, widgets, уведомления, favicon, резервы и загрузку бизнес-данных.

Стандартные фоны являются частью визуальной темы и поставляются как статические JPG-файлы. Это отделяет дизайн темы от Java-кода, исключает runtime-генерацию SVG и позволяет проверять полноту каталога на этапе сборки. Пользовательский фон остаётся отдельным персональным ресурсом в FileStorage и имеет приоритет над тематическим каталогом.

## UI Context & Navigation

Экран создаётся через Screens API CUBA Platform 7.3:

- `cuba.web.mainScreenId=hrmMainScreen`;
- `@UiController("hrmMainScreen")`;
- `@UiDescriptor("hrm-main-screen.xml")`;
- legacy-регистрация `hrmMainScreen` в `web-screens.xml` отсутствует.

Персональный файл задаётся в `SettingsWindow` → «Интерфейс» → «Фон главного экрана». После успешного «ОК» текущая браузерная вкладка получает `MainScreenBackgroundChangedEvent` и обновляет фон без повторного входа.

## Behavior Summary

- вход → создаётся фактический `HrmMainScreen` → наследуемая бизнес-логика `ExtMainScreen` выполняется без изменений;
- `AfterShowEvent` → Vaadin UI готов → `MainScreenBackgroundService` разрешает ресурс;
- `UserSettings.fileImageFace` содержит существующий файл с префиксом `hrm-main-background-` → используется пользовательский `StreamResource`;
- пользовательского файла нет → выбирается случайный `1.jpg … 10.jpg` из `backgrounds/` активной темы;
- предыдущий индекс темы известен в `UserSession` → немедленное повторение исключается;
- ресурс разрешён → скрытый Vaadin `Image` регистрирует URL → динамический локальный CSS назначает фон только `mainVBox`;
- dashboard получает прозрачный локальный стиль → фон остаётся видимым под рабочими widgets;
- SettingsWindow успешно сохранён → публикуется UI-scoped event → фон обновляется без перезахода;
- пользовательский файл повреждён или отсутствует → сервис возвращается к тематическому каталогу;
- ошибка декоративного фона → записывается warning/error → открытие главного экрана не должно блокироваться.

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

## Разрешение theme-ресурса

`MainScreenBackgroundService` хранит индекс варианта в диапазоне `0..9`, а имя файла формирует как `variant + 1`. Для стандартного режима возвращается:

```java
new ThemeResource("backgrounds/" + (variant + 1) + ".jpg")
```

`ThemeResource` разрешает путь относительно активной Vaadin-темы:

```text
VAADIN/themes/{activeTheme}/backgrounds/{1..10}.jpg
```

Allowlist поддерживаемых тем используется для нормализации имени и отдельного session-ключа. Неизвестное или отсутствующее имя темы нормализуется к `hover`.

## Пользовательский фон

Пользовательская цепочка не меняется:

```text
Upload → MainScreenBackgroundImageProcessor → FileStorage
→ UserSettings.fileImageFace
```

Контракт пользовательского файла:

- имя начинается с `hrm-main-background-`;
- файл хранится в FileStorage;
- файл имеет приоритет над тематическим ресурсом;
- legacy-фотография в `fileImageFace` без указанного префикса не считается фоном;
- при Cancel временно созданные файлы очищаются по существующему контракту SettingsWindow.

## Применение ресурса в HrmMainScreen

Архитектура контроллера не меняется:

```text
mainVBox
├── backgroundResourceHolder (скрытый Vaadin Image 0 × 0)
└── mainDashboard (локальный прозрачный стиль)
```

После регистрации ресурса контроллер:

1. получает URL через `ResourceReference`;
2. удаляет несовместимый префикс `app://APP`, если он присутствует;
3. создаёт уникальный локальный CSS-класс;
4. назначает `background-image`, `background-position`, `background-repeat` и `background-size` на `mainVBox`;
5. оставляет dashboard прозрачным.

## Runtime-маркеры

| Элемент | Маркер | Назначение |
|---|---|---|
| `mainVBox` | `data-hrm-main-background="applied"` | подтверждает выполнение background lifecycle |
| `mainVBox` | `data-hrm-main-controller="HrmMainScreen"` | подтверждает фактический root controller |

Назначенные через `HtmlAttributes` значения проверяются integration-тестом. Реальный computed style, HTTP-статус theme-ресурса и screenshot проверяются Hermes после clean deploy.

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

1. отсутствие `createSvg`, `createPalettes`, `Palette` и MIME `image/svg+xml`;
2. использование `ThemeResource` с путём `backgrounds/{1..10}.jpg`;
3. наличие allowlist всех семи тем;
4. наличие ровно десяти файлов в каждом `theme/backgrounds`;
5. имена `1.jpg … 10.jpg`;
6. декодирование каждого файла через `ImageIO`;
7. размер каждого изображения `1920 × 1080`;
8. сохранение приоритета пользовательского фона и session-антиповтора;
9. неизменность интеграционного контракта `HrmMainScreen`;
10. неизменность SettingsWindow/FileStorage-цепочки.

`HrmMainScreenIntegrationTest` продолжает проверять создание экрана, DOM-маркеры, регистрацию ресурса и обновление по `MainScreenBackgroundChangedEvent`.

## Проверки Hermes

Hermes проверяет точный HEAD PR:

- `git diff --check`;
- compile web/core tests;
- `MainScreenBackgroundContractTest 10/10`;
- `HrmMainScreenIntegrationTest`;
- `ScreenViewIntegrityTest 8/8`;
- сборку SCSS семи тем;
- `clean assemble`;
- clean local deploy;
- `/hrm/` = HTTP 200;
- системный фон для каждой из семи тем;
- отсутствие `app://APP` в итоговом CSS URL;
- HTTP 200 и MIME `image/jpeg` для theme-ресурса;
- отсутствие немедленного повтора после обновления/перезахода;
- приоритет пользовательского фона;
- отсутствие Tomcat critical errors, P1 и P2.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Стандартные фоны вынесены из Java runtime-SVG в каталоги семи тем: 7 × 10 JPG; контрактный тест проверяет состав и формат файлов |
| 2026-07-26 | Добавлен единый background layer, UI-scoped refresh, исключение повтора SVG и screen-level integration test |
| 2026-07-26 | Синхронизированы оба `mainScreenId`; динамический фон переведён на inline CSS через `HtmlAttributes` |
| 2026-07-26 | Удалена несовместимая legacy-регистрация `hrmMainScreen` |
| 2026-07-26 | Добавлен каталог 7 × 10 и приоритет персонального фона |
