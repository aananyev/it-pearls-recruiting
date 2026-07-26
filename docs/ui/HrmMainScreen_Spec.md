# HrmMainScreen — устойчивый фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран остаётся основной рабочей точкой dashboard HRM HuntTech. Фоновое изображение персонализирует рабочую область, но не должно влиять на меню, widgets, уведомления, favicon, резервы и загрузку бизнес-данных. Отдельный декоративный слой устраняет двойную отрисовку и делает runtime-контракт проверяемым.

## UI Context & Navigation

Экран создаётся через новый Screens API CUBA Platform 7.3:

- `cuba.web.mainScreenId=hrmMainScreen`;
- `@UiController("hrmMainScreen")`;
- `@UiDescriptor("hrm-main-screen.xml")`;
- legacy-регистрация `hrmMainScreen` в `web-screens.xml` отсутствует.

Персональный файл задаётся в `SettingsWindow` → «Интерфейс» → «Фон главного экрана». После успешного «ОК» текущая браузерная вкладка получает `MainScreenBackgroundChangedEvent` и обновляет фон без повторного входа.

## Behavior Summary

- вход → создаётся фактический `HrmMainScreen` → наследуемая бизнес-логика `ExtMainScreen` выполняется без изменений;
- `AfterShowEvent` → Vaadin UI и connector tree готовы → ресурс фона регистрируется скрытым `Image`;
- пользовательский маркированный файл существует → используется нормализованный JPEG;
- пользовательского файла нет → выбирается SVG активной темы;
- системный SVG выбран → предыдущий вариант темы известен в `UserSession` → немедленное повторение исключается;
- ресурс зарегистрирован → inline `background-*` назначается только `mainScreenBackgroundLayer`;
- layer применён → dashboard остаётся прозрачным и располагается выше по `z-index`;
- SettingsWindow успешно сохранён → публикуется UI-scoped event → layer получает новый ресурс без перезахода;
- ошибка декоративного слоя → записывается warning → открытие главного экрана не блокируется.

## Структура background layer

```text
mainVBox (position: relative; overflow: hidden)
├── mainScreenBackgroundLayer (absolute; z-index: 0; pointer-events: none)
├── mainDashboard (relative; z-index: 1; transparent)
└── backgroundResourceHolder внутри layer (Vaadin Image 0 × 0)
```

`mainVBox` и `mainDashboard` не получают `background-image`. Единственный владелец изображения — `mainScreenBackgroundLayer`.

## Runtime-маркеры

| Элемент | Маркер | Назначение |
|---|---|---|
| `mainScreenBackgroundLayer` | `data-hrm-main-background="applied"` | подтверждает выполнение background lifecycle |
| `mainScreenBackgroundLayer` | `data-hrm-main-background-resource="<connector URL>"` | позволяет проверить фактический ресурс |
| `mainVBox` | `data-hrm-main-controller="HrmMainScreen"` | подтверждает фактический root controller |

Назначенные через `HtmlAttributes` значения проверяются screen-level integration тестом. Реальный computed style браузера, HTTP-статус connector URL и screenshot проверяются Hermes после clean deploy.

## Выбор системного варианта

Для каждой из семи тем сохраняется последний индекс `0..9` в `UserSession`. Следующий индекс выбирается из девяти остальных вариантов. Состояние относится только к текущей пользовательской сессии и не записывается в БД.

## Событие обновления

`MainScreenBackgroundChangedEvent` реализует `UiEvent`. Событие:

- публикуется только после успешной commit-цепочки SettingsWindow;
- доставляется синхронно в текущую браузерную вкладку;
- не обновляет чужие UI-сеансы;
- не публикуется при Cancel или неуспешной загрузке.

## Интеграционные проверки

`HrmMainScreenIntegrationTest` запускается в штатном CUBA `TestContainer` и проверяет:

1. разрешение screen ID `hrmMainScreen` в класс `HrmMainScreen`;
2. наличие выделенного layer в реальном component tree;
3. assigned DOM-маркеры и CSS-контракт `layer/dashboard`;
4. отсутствие background-image на `mainVBox` и dashboard;
5. повторное применение после `MainScreenBackgroundChangedEvent`;
6. работу нормализатора изображения и регистрацию WEBP reader.

Тест не эмулирует реальный браузерный computed style и HTTP connector request. Эти проверки обязательны в runtime smoke Hermes.

## Наследование ExtMainScreen

`HrmMainScreen extends ExtMainScreen`. Методы `publishMyNotification`, `checkPersonalReserveCandidates`, favicon, dashboard loaders и notification events не копируются и не переопределяются.

## Проверки Hermes

Hermes проверяет точный HEAD PR:

- compile web/core tests;
- `MainScreenBackgroundContractTest 10/10`;
- `HrmMainScreenIntegrationTest`;
- `ScreenViewIntegrityTest 8/8`;
- SCSS семи тем;
- `clean assemble`;
- clean local deploy;
- `/hrm/` = HTTP 200;
- фактический controller, DOM-маркеры, computed style;
- connector resource HTTP 200 и MIME;
- screenshot системного и пользовательского режима;
- мгновенное обновление после «ОК»;
- отсутствие Tomcat critical errors, P1 и P2.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Добавлен единый background layer, UI-scoped refresh, исключение повтора SVG и screen-level integration test |
| 2026-07-26 | Синхронизированы оба `mainScreenId`; динамический фон переведён на inline CSS через `HtmlAttributes` |
| 2026-07-26 | Удалена несовместимая legacy-регистрация `hrmMainScreen` |
| 2026-07-26 | Добавлен каталог 7 × 10 и приоритет персонального фона |
