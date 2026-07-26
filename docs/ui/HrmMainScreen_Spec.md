# HrmMainScreen — персональный фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран остаётся рабочей точкой входа в dashboard HRM HuntTech и получает персональный либо случайный тематический фон без изменения dashboard, уведомлений, резервов, favicon и меню.

## UI Context & Navigation

Экран создаётся через новый Screens API CUBA Platform 7.3:

- `cuba.web.mainScreenId=hrmMainScreen` выбирает ID главного экрана;
- `@UiController("hrmMainScreen")` регистрирует Java-контроллер;
- `@UiDescriptor("hrm-main-screen.xml")` связывает controller с descriptor.

Значение `cuba.web.mainScreenId` должно совпадать в обоих проектных источниках:

- `modules/web/src/com/company/hunttech/web-app.properties`;
- `com/company/hunttech/web-app.properties`.

`hrmMainScreen` нельзя одновременно регистрировать в legacy `web-screens.xml`. Такая запись подменяет новый screen legacy-описанием и при входе приводит к `DevelopmentException: Unable to create screen hrmMainScreen with type FRAGMENT`.

Персональный файл задаётся в `SettingsWindow` → «Интерфейс» → «Фон главного экрана».

## Behavior Summary

- вход → оба `web-app.properties` выбирают `hrmMainScreen`;
- `@UiController("hrmMainScreen")` создаёт фактический controller `HrmMainScreen`;
- `ExtMainScreen.BeforeShow` → выполняются прежние favicon и служебные проверки;
- `HrmMainScreen.AfterShow` → проверяется `UI.getCurrent()` и connector ownership;
- пользовательский маркированный файл → имеет абсолютный приоритет;
- файла нет → выбирается вариант `0..9` палитры активной темы;
- ресурс регистрируется Vaadin `Image` размером `0 × 0`;
- CUBA `HtmlAttributes` назначает inline `background-*` фактическим DOM-элементам `mainVBox` и `mainDashboard`;
- DOM получает маркер `data-hrm-main-background="applied"` для runtime smoke;
- `ExtMainScreen.AfterShow` → прежние уведомления и проверки резерва продолжают работать;
- наличие `hrmMainScreen` в legacy `web-screens.xml` → конфигурационная ошибка и блокировка входа.

## Технический контракт

| Параметр | Значение |
|---|---|
| Screen ID | `hrmMainScreen` |
| Web-module property | `cuba.web.mainScreenId=hrmMainScreen` |
| App-component property | `cuba.web.mainScreenId=hrmMainScreen` |
| Регистрация controller | `@UiController("hrmMainScreen")` |
| Descriptor | `@UiDescriptor("hrm-main-screen.xml")` |
| Legacy `web-screens.xml` | запись `hrmMainScreen` отсутствует |
| Lifecycle | `AfterShowEvent` |
| CSS API | CUBA `HtmlAttributes.setCssProperty()` |
| Поверхности | `mainVBox`, `mainDashboard` |
| Владелец ресурса | Vaadin `Image`, `0 × 0` |
| Каталог | 7 тем × 10 SVG |

`Page.getStyles().add()` не используется. Vaadin 7/8 может добавить динамическое правило в `Page`, но не применить его к уже отрисованным компонентам без дополнительного обновления. `HtmlAttributes` является штатным API CUBA для программного назначения DOM/CSS-атрибутов и передаёт inline-style через connector конкретного компонента.

`ResourceReference` сохраняется только для получения URL динамического `StreamResource`. Сначала скрытый `Image` добавляется в `mainVBox`, затем проверяется его принадлежность текущему UI, после чего URL передаётся в `HtmlAttributes`.

## Наследование ExtMainScreen

`HrmMainScreen extends ExtMainScreen`; методы `publishMyNotification`, `checkPersonalReserveCandidates`, favicon и dashboard loaders не копируются и не переопределяются. Исправление затрагивает только конфигурацию выбора root screen и декоративный CSS-слой.

## Проверки Hermes

Обязательны `MainScreenBackgroundContractTest 10/10`, `ScreenViewIntegrityTest 8/8`, compile, SCSS семи тем, `clean assemble`, local deploy, HTTP 200 и новый login smoke. После входа должен быть создан фактический controller `HrmMainScreen`; в DOM `mainVBox` и dashboard присутствует `data-hrm-main-background="applied"`, inline `background-image` содержит connector URL, ресурс возвращает HTTP 200, системный или персональный фон отображается.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Синхронизированы оба `mainScreenId`; динамический фон переведён с `Page.getStyles().add()` на inline CSS через `HtmlAttributes` |
| 2026-07-26 | Удалена несовместимая legacy-регистрация `hrmMainScreen`; закреплён контракт `mainScreenId + @UiController + @UiDescriptor` |
| 2026-07-26 | Добавлены проверки UI и connector ownership |
| 2026-07-26 | Исправлена регистрация динамического ресурса после `AfterShow` |
| 2026-07-26 | Добавлен каталог 7 × 10 и приоритет персонального фона |
