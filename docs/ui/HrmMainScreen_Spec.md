# HrmMainScreen — персональный фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран остаётся рабочей точкой входа в dashboard HRM HuntTech и получает персональный либо случайный тематический фон без изменения dashboard, уведомлений, резервов, favicon и меню.

## UI Context & Navigation

Экран создаётся через `cuba.web.mainScreenId=hrmMainScreen`. CUBA Platform 7.3 требует согласованной явной регистрации в `web-screens.xml`:

```xml
<screen id="hrmMainScreen"
        template="/com/company/hunttech/web/screens/mainscreen/hrm-main-screen.xml"/>
```

Персональный файл задаётся в `SettingsWindow` → «Интерфейс» → «Фон главного экрана».

## Behavior Summary

- вход → CUBA разрешает `hrmMainScreen` из `web-screens.xml`;
- `ExtMainScreen.BeforeShow` → выполняются прежние favicon и служебные проверки;
- `HrmMainScreen.AfterShow` → проверяются `UI.getCurrent()`, `currentUi.getPage()` и connector ownership;
- пользовательский маркированный файл → имеет абсолютный приоритет;
- файла нет → выбирается вариант `0..9` палитры активной темы;
- ресурс регистрируется Vaadin `Image` размером `0 × 0`;
- session CSS-класс назначается `mainVBox` и `mainDashboard`;
- `ExtMainScreen.AfterShow` → прежние уведомления и проверки резерва продолжают работать.

## Технический контракт

| Параметр | Значение |
|---|---|
| Screen ID | `hrmMainScreen` |
| Регистрация | `web-screens.xml` |
| Lifecycle | `AfterShowEvent` |
| Page | `UI.getCurrent().getPage()` |
| Поверхности | `mainVBox`, `mainDashboard` |
| Владелец ресурса | Vaadin `Image`, `0 × 0` |
| Каталог | 7 тем × 10 SVG |

`Page.getStyles().add()` сохраняется, поскольку connector URL динамический и персональный. Статический `@StyleSheet` не может содержать такой URL. Перед добавлением CSS проверяется принадлежность всех компонентов текущему UI и непустой `resourceUrl`.

## Наследование ExtMainScreen

`HrmMainScreen extends ExtMainScreen`; методы `publishMyNotification`, `checkPersonalReserveCandidates`, favicon и dashboard loaders не копируются и не переопределяются.

## Проверки Hermes

Обязательны `MainScreenBackgroundContractTest 10/10`, `ScreenViewIntegrityTest 8/8`, compile, SCSS семи тем, `clean assemble`, local deploy, HTTP 200, фактический controller `HrmMainScreen`, connector resource 200 и runtime smoke системного и персонального фона.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Явно зарегистрирован `hrmMainScreen`; добавлены проверки UI, Page и connector ownership |
| 2026-07-26 | Исправлена регистрация динамического ресурса после `AfterShow` |
| 2026-07-26 | Добавлен каталог 7 × 10 и приоритет персонального фона |
