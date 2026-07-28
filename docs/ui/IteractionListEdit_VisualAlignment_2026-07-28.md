# IteractionListEdit — точное визуальное выравнивание XML

> Проект: **HRM HuntTech**  
> Screen ID: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Дата: `2026-07-28`

## 1. Назначение и бизнес-смысл (What & Why)

Экран фиксирует взаимодействие рекрутёра с кандидатом по конкретной вакансии. В этой задаче изменяется только визуальная компоновка существующих компонентов: данные, обязательность полей, loaders, views, JPQL, lifecycle, сервисные вызовы и side effects сохраняются.

Цель — устранить расхождения между XML, поздними SCSS-слоями и предварительным рендером. Sidebar должна полностью помещать контекст, а правая рабочая область — показывать одинаково оформленные поля и естественную высоту блоков.

## 2. UI Context & Navigation

Экран открывается как editor взаимодействия из browse, карточки кандидата и связанных vacancy workflows. Слева постоянно отображаются:

1. фотография кандидата и меньший логотип проекта;
2. имя кандидата и выбранная вакансия;
3. горизонтальная пара «Статус вакансии» / «Приоритет»;
4. заголовок и пункты `label-navigation`;
5. карточка номера и даты;
6. карточка вакансии с её наименованием, компанией и проектом.

Справа находятся toolbar, пять быстрых взаимодействий, четыре постоянных блока ввода и footer. Label-навигация изменяет только active-style и focus target; скрытие блоков не выполняется.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть экран | всегда | два изображения имеют размер `96 × 96` |
| открыть sidebar | всегда | заголовок «Разделы формы» и четыре пункта навигации видимы и оформлены единым стилем |
| выбрать пункт навигации | клик пользователя | выбранный пункт и соответствующая карточка получают active-style, focus переводится в первое поле |
| выбрать кандидата или вакансию | поле доступно | оба picker-поля имеют одинаковую высоту, границу, фон и геометрию action-кнопок |
| заполнить результат | любое число строк/длина значений | блок «Оценка и коммуникация» растёт по `height=AUTO` |
| показать статус и приоритет | вакансия выбрана | значения располагаются горизонтально в двух равных ячейках |
| показать номер и дату | всегда | оба поля полностью помещаются в service-card |
| показать контекст вакансии | вакансия выбрана | карточка не выходит за sidebar и содержит наименование вакансии |
| показать поля результата и комментария | любое содержимое GridLayout | блоки не перекрываются, поля имеют единый визуальный контракт |

## 4. Точный XML-контракт

### 4.1. Изображения

`candidateImage` использует:

```xml
width="96px"
height="96px"
ovalWidth="96px"
ovalHeight="96px"
stylename="iteraction-list-context-image ..."
```

`projectLogoImage` остаётся `OvaFallbackImage`, но финальный SCSS задаёт логотипу `80 × 80`, чтобы он воспринимался вторичным визуальным контекстом и не конкурировал с фотографией кандидата.

### 4.2. Label-навигация

Контейнер и дочерние элементы используют одновременно канонические и compatibility-классы:

```text
label-navigation iteraction-list-navigation
label-nav-title iteraction-list-navigation-title
label-nav-item iteraction-list-nav-item
label-nav-item-active iteraction-list-nav-item-active
```

Первый компонент контейнера — `iteractionListNavigationTitle`. Активный controller сохраняет его при замене fallback labels runtime-кнопками.

### 4.3. Sidebar-карточки

- service-card: `iteractionServiceCard` → `iteractionServiceFields`;
- vacancy-card: `iteractionVacancyCard`;
- vacancy name: `sidebarVacancyNameLabel` → `vacancy.vacansyName`;
- все внутренние layout и fields имеют `width="100%"`, `height="AUTO"` и локальное ограничение `max-width: 100%`.

### 4.4. Статус и приоритет

`vacancyStateSummary` остаётся `HBoxLayout`. Дочерние `vacancyStatusSummary` и `vacancyPrioritySummary` имеют `width="50%"`. Финальный SCSS отменяет прежний `display:block` и восстанавливает flex-row `50/50`.

### 4.5. Picker-поля

`candidateField` и `vacancyFiels` имеют один stylename:

```text
iteraction-list-primary-picker
```

Финальный SCSS нормализует `SuggestionPickerField` и `LookupPickerField`, включая input и action buttons.

### 4.6. Оценка и коммуникация

- `resultAccordion`: `height="AUTO"`, class `iteraction-list-result-section`;
- `resultAccordionBody`: `height="AUTO"`, class `iteraction-list-result-body`;
- `resultAccordionGrid`: `height="AUTO"`, class `iteraction-list-result-grid`.

SCSS устанавливает `height:auto`, `min-height:0` и `overflow:visible` для CUBA/Vaadin GridLayout wrappers.

## 5. SCSS и темы

Финальный partial:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-visual-alignment.scss
```

Он подключается после общего `edit-screen-shared-styles`, `iteraction-list-reference-finish` и прочих legacy/reference partials. Порядок нужен, чтобы общий Edit-контракт работал как базовый слой, а локальный экранный слой финально ограничивал карточки, GridLayout, поля, action-кнопки и размеры изображений.

`iteraction-list-visual-alignment.scss` дополнительно закрепляет:

- `projectLogoImage` `80 × 80`;
- `edit-form-control` для `iteractionTypeField`, `ratingField`, `recrutierField`, `communicationMethodField`, `commentField`;
- одинаковую высоту `38px` и фиксированные action-кнопки picker-полей;
- `table-layout: fixed` и `min-width: 0` для Vaadin GridLayout wrappers;
- `overflow-x: hidden` для рабочей scroll-area;
- отсутствие перекрытия `resultAccordion` и `commentAccordion`.

Поддерживаемые темы:

- halo;
- havana;
- helium;
- hover;
- hunttech-modern;
- hunttech-modern-light;
- hunttech-modern-dark.

Содержимое partial во всех темах идентично. Все селекторы ограничены `.iteraction-list-editor`.

## 6. Сохранённые CUBA-контракты

Не изменены:

- screen ID;
- entity и БД;
- Liquibase;
- containers/loaders;
- JPQL;
- views;
- field component ID;
- `dataContainer` / `property`;
- picker actions;
- `invoke`;
- `InteractionService`;
- dynamic fields;
- subscriptions;
- Employee side effects;
- notifications/email;
- save/cancel lifecycle.

## 7. Предварительный render-review

Проверяется точная XML-иерархия и итоговая геометрия после последнего SCSS-слоя:

- `1700 × 950`;
- `1366 × 768`;
- `1100 × 760`.

Критерии PASS:

- горизонтальный overflow отсутствует;
- фотография кандидата крупнее логотипа проекта, логотип не обрезается;
- заголовок и стили navigation видимы;
- status/priority находятся в одной строке;
- service-card и vacancy-card не выходят за sidebar;
- vacancy-card содержит название вакансии;
- candidate/vacancy controls одинаковы;
- result/recruiter/communication/comment controls используют единый визуальный контракт;
- result-card растёт по содержимому;
- comment-card начинается ниже result-card и не перекрывает его поля;
- footer не перекрывает рабочую область.

Статический render-review не заменяет CUBA/Vaadin browser smoke Hermes.

## 8. Regression matrix

- новая и существующая запись;
- candidate select/clear;
- vacancy select/clear;
- открытая и закрытая вакансия;
- длинное название вакансии;
- длинная компания/департамент/проект;
- пустой и заполненный priority/status;
- 0/1–4/5 популярных взаимодействий;
- dynamic fields addType 1/2/3;
- rating/recruiter/communication;
- comment;
- subscribe;
- save/cancel;
- семь тем;
- viewports `1700`, `1366`, `1100`.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Shared Edit SCSS переведён в базовый слой, локальный final partial ограничивает GridLayout, picker/action-кнопки, `edit-form-control`, рабочий scroll overflow и размер логотипа проекта `80 × 80`; добавлены критерии отсутствия перекрытия result/comment. |
| 2026-07-28 | Унифицированы `OvaFallbackImage`, восстановлены title/styles label-навигации, выровнены picker-поля, status/priority, sidebar-карточки и AUTO-геометрия блока результата; в vacancy-card добавлено наименование вакансии. |
