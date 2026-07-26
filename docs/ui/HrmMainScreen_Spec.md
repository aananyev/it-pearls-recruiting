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

`hrmMainScreen` нельзя одновременно регистрировать в legacy `web-screens.xml`. Такая запись подменяет новый screen legacy-описанием и при входе приводит к `DevelopmentException: Unable to create screen hrmMainScreen with type FRAGMENT`.

Персональный файл задаётся в `SettingsWindow` → «Интерфейс» → «Фон главного экрана».

## Behavior Summary

- вход → `cuba.web.mainScreenId` разрешается через `@UiController("hrmMainScreen")`;
- `ExtMainScreen.BeforeShow` → выполняются прежние favicon и служебные проверки;
- `HrmMainScreen.AfterShow` → проверяются `UI.getCurrent()`, `currentUi.getPage()` и connector ownership;
- пользовательский маркированный файл → имеет абсолютный приоритет;
- файла нет → выбирается вариант `0..9` палитры активной темы;
- ресурс регистрируется Vaadin `Image` размером `0 × 0`;
- session CSS-класс назначается `mainVBox` и `mainDashboard`;
- `ExtMainScreen.AfterShow` → прежние уведомления и проверки резерва продолжают работать;
- наличие `hrmMainScreen` в legacy `web-screens.xml` → конфигурационная ошибка и блокировка входа.

## Технический контракт

| Параметр | Значение |
|---|---|
| Screen ID | `hrmMainScreen` |
| Регистрация controller | `@UiController("hrmMainScreen")` |
| Descriptor | `@UiDescriptor("hrm-main-screen.xml")` |
| Legacy `web-screens.xml` | запись `hrmMainScreen` отсутствует |
| Lifecycle | `AfterShowEvent` |
| Page | `UI.getCurrent().getPage()` |
| Поверхности | `mainVBox`, `mainDashboard` |
| Владелец ресурса | Vaadin `Image`, `0 × 0` |
| Каталог | 7 тем × 10 SVG |

`Page.getStyles().add()` сохраняется, поскольку connector URL динамический и персональный. Статический `@StyleSheet` не может содержать такой URL. Перед добавлением CSS проверяется принадлежность всех компонентов текущему UI и непустой `resourceUrl`.

## Наследование ExtMainScreen

`HrmMainScreen extends ExtMainScreen`; методы `publishMyNotification`, `checkPersonalReserveCandidates`, favicon и dashboard loaders не копируются и не переопределяются. Удаление legacy-регистрации не изменяет эту цепочку наследования.

## Проверки Hermes

Обязательны `MainScreenBackgroundContractTest 10/10`, `ScreenViewIntegrityTest 8/8`, compile, SCSS семи тем, `clean assemble`, local deploy, HTTP 200 и новый login smoke. После входа должен быть создан фактический controller `HrmMainScreen`; ошибки `type FRAGMENT` и упоминания legacy `screens.xml` отсутствуют; connector resource возвращает 200, системный или персональный фон отображается.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Удалена несовместимая legacy-регистрация `hrmMainScreen`; закреплён контракт `mainScreenId + @UiController + @UiDescriptor` |
| 2026-07-26 | Добавлены проверки UI, Page и connector ownership |
| 2026-07-26 | Исправлена регистрация динамического ресурса после `AfterShow` |
| 2026-07-26 | Добавлен каталог 7 × 10 и приоритет персонального фона |
