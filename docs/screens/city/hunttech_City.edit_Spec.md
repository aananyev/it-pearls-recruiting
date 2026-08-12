# City Edit (`hunttech_City.edit`)

> Сущность: [City.md](../../entities/city/City.md)

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

`CityEdit` поддерживает ведение городов, которые используются в адресах кандидатов, компаний и вакансий HRM HuntTech. Форма должна позволять быстро идентифицировать город, проверить телефонный код и назначить корректный регион без изменения стандартного CUBA lifecycle редактора.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из `hunttech_City.browse` стандартными действиями создания и редактирования, а также может использоваться как связанный editor справочника городов. Регион выбирается через штатный `picker_lookup` из контейнера `cityRegionsDc`.

Левая контекстная панель содержит название редактируемого города, label-навигацию по разделам «Наименование» и «Регион и связь», сводку по выбранному региону и служебную подсказку. Контент формы прокручивается только в правой рабочей области.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- открытие формы → CUBA загружает `cityDc` и кэшируемый справочник регионов → пользователь получает текущие реквизиты города;
- выбор «Наименование» в sidebar → active-state переключается на первый пункт → фокус устанавливается в `cityRuNameField`;
- выбор «Регион и связь» → active-state переключается на второй пункт → фокус устанавливается в `cityRegionField`;
- выбор региона → используется прежний `picker_lookup` и binding `cityRegion` → связанная сущность сохраняется штатно;
- сохранение или отмена → выполняются `windowCommitAndClose` или `windowClose` → presentation-слой не вмешивается в lifecycle.

## 1. Технический контракт

| Параметр | Значение |
|---|---|
| `@UiController` | `hunttech_City.edit` |
| Java-класс | `com.company.hunttech.web.screens.city.CityEdit` |
| XML | `city-edit.xml` |
| Базовый класс | `StandardEditor<City>` |
| Edited entity container | `cityDc` |
| View | `city-edit-view` |
| Lookup container | `cityRegionsDc` |
| Loader | `cityRegionsLc`, `cacheable=true` |
| JPQL | `select e from hunttech_Region e` |
| Focus component | `cityRuNameField` |

## 2. Информационная архитектура

### Левая панель

Порядок элементов соответствует обязательному контракту Edit-форм HRM HuntTech:

1. наименование типа объекта и экземпляра;
2. label-навигация;
3. детализация выбранного региона;
4. служебная подсказка.

### Правая рабочая область

1. toolbar «Карточка города»;
2. карточка «Наименование» — `cityRuName`, `cityPhoneCode`;
3. карточка «Регион и связь» — `cityRegion`;
4. footer-actions сохранения и отмены.

Разделение выполнено только на уровне визуальных контейнеров. Property binding, loaders, views, JPQL и actions не изменены.

## 3. Presentation-навигация

| Метод | Результат |
|---|---|
| `focusMainSection()` | совместимый вход, делегирует фокус разделу наименования |
| `focusIdentitySection()` | активирует `cityIdentityNav`, снимает active-state с `cityRegionNav`, фокусирует `cityRuNameField` |
| `focusRegionSection()` | активирует `cityRegionNav`, снимает active-state с `cityIdentityNav`, фокусирует `cityRegionField` |

Методы не изменяют entity, не запускают loaders и не инициируют сохранение.

## 4. Data View Integrity

Контроллер не читает дополнительные атрибуты сущности. XML sidebar использует `cityRegion.regionRuName`, который должен оставаться доступным через `city-edit-view`. Изменения fetch plan в этом этапе не выполнялись.

## 5. Визуальный контракт

- локальный namespace: `.city-editor`;
- sidebar: `270px`, тёмная поверхность HRM HuntTech;
- navigation: `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active`;
- карточки: `edit-card`;
- поля: `edit-form-control`;
- рабочая область и footer используют общий контракт `edit-workspace` и `edit-footer-actions`;
- поддерживаются все темы, подключающие `geolocation-edit-forms.scss`.

## 6. Проверки

- `CityEditRedesignContractTest`;
- `GeolocationEditFormsContractTest`;
- `ScreenViewIntegrityTest` — ожидается `8/8 PASS`;
- `buildScssThemes`;
- `clean assemble`;
- local deploy `/hrm/` и visual smoke на 1920×1080 и 1366×768.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-05 | Применён отдельный редизайн CityEdit: два логических раздела, синхронная label-навигация, active-state и контрактный тест |
| 2026-07-28 | Форма приведена к общему двухпанельному контракту геолокационных Edit-экранов |
| 2026-06-26 | Добавлены Business & Context Intro и первая версия UI-спецификации |
