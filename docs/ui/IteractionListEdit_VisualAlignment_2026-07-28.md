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

1. фотография кандидата и логотип проекта одинакового размера;
2. имя кандидата и выбранная вакансия;
3. карточка номера и даты непосредственно под профильным блоком кандидата;
4. горизонтальная пара «Статус вакансии» / «Приоритет»;
5. заголовок и пункты `label-navigation`;
6. карточка вакансии с её наименованием, компанией и проектом.

Справа находятся toolbar, пять быстрых взаимодействий, четыре постоянных блока ввода и footer. Label-навигация изменяет только active-style и focus target; скрытие блоков не выполняется.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть экран | всегда | два изображения имеют размер `96 × 96` |
| открыть sidebar | всегда | заголовок «Разделы формы» и четыре пункта навигации видимы и оформлены единым стилем |
| выбрать пункт навигации | клик пользователя | выбранный пункт и соответствующая карточка получают active-style, focus переводится в первое поле |
| выбрать кандидата или вакансию | поле доступно | оба picker-поля имеют одинаковую высоту, границу, фон и геометрию action-кнопок |
| показать выбранную вакансию или рекрутёра | option icon/image задан provider-ом | пиктограмма имеет размер `20 × 20`, а текст начинается после зарезервированной области `40px` |
| показать фильтр подписок | всегда | checkbox и подпись находятся в одной строке, вертикально выровнены и не перекрываются |
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

`candidateImage` и `projectLogoImage` остаются `OvaFallbackImage`. Финальный SCSS задаёт обоим размер `96 × 96`; две равные половины `iteractionListIdentityImagesBox` и `align="MIDDLE_CENTER"` обеспечивают симметричное центрирование в sidebar.

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
- service-card расположен после `iteractionVacancyNameLabel` и до `vacancyStateSummary`;
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
`c-suggestionfield` использует те же `15px`, `38px` и горизонтальные отступы, что `v-filterselect-input`.
Общий `edit-form-control` увеличивает provider-пиктограмму до `20 × 20` и резервирует перед текстом `40px`.

### 4.6. Оценка и коммуникация

- `resultAccordion`: `height="AUTO"`, class `iteraction-list-result-section`;
- `resultAccordionBody`: `height="AUTO"`, class `iteraction-list-result-body`;
- `resultAccordionGrid`: `height="AUTO"`, class `iteraction-list-result-grid`.

У CUBA 7.3 ячейки `GridLayout` рендерятся как absolute-positioned `.v-gridlayout-slot`. Поэтому локальный SCSS:

- сохраняет вычисленную высоту корня `59px` для candidate/vacancy и `126px` для двухстрочного result;
- закрепляет ширину slot-ов первой строки как `50%` и позицию правого slot как `left: 50%`;
- оставляет communication в следующей строке с `width: 100%` и `left: 0`;
- сохраняет верхние `19px` каждого slot для caption, чтобы подпись не накладывалась на control;
- не применяет `height:auto` к корню GridLayout: это схлопывает контейнер до `0px` и вызывает наложение следующей секции.

## 5. SCSS и темы

Финальный partial:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-visual-alignment.scss
```

Он подключается после общего `edit-screen-shared-styles`, `iteraction-list-reference-finish` и прочих legacy/reference partials. Порядок нужен, чтобы общий Edit-контракт работал как базовый слой, а локальный экранный слой финально ограничивал карточки, GridLayout, поля, action-кнопки и размеры изображений.

`iteraction-list-visual-alignment.scss` дополнительно закрепляет:

- одинаковый размер `96 × 96` и симметричное центрирование `candidateImage`/`projectLogoImage`;
- `edit-form-control` для `iteractionTypeField`, `ratingField`, `recrutierField`, `communicationMethodField`, `commentField`;
- одинаковую высоту `38px` и фиксированные action-кнопки picker-полей;
- одинаковую типографику `15px` у candidate/vacancy, отдельную область под provider-пиктограммы и выровненный checkbox подписок;
- `min-width: 0` для Vaadin GridLayout wrappers и локальную геометрию absolute-positioned slot-ов;
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
- фотография кандидата и логотип проекта одинаковы по размеру `96 × 96`, не обрезаются и симметрично центрированы;
- заголовок и стили navigation видимы;
- status/priority находятся в одной строке;
- service-card и vacancy-card не выходят за sidebar;
- локальные `312px` у sidebar и его Vaadin slot совпадают, поэтому workspace не перекрывает правую часть карточек;
- date, calendar и time служебной карточки остаются внутри её правой границы;
- карточка номера и даты расположена сразу под ФИО/вакансией кандидата; её field slots имеют высоту под фактический control `38px`;
- vacancy-card содержит название вакансии;
- candidate/vacancy controls одинаковы и занимают две видимые колонки `50/50`;
- result/recruiter/communication/comment controls используют единый визуальный контракт;
- rating/recruiter занимают две видимые колонки `50/50`, communication располагается отдельной полноширинной строкой;
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
| 2026-07-28 | Поле кандидата выровнено с вакансией по типографике `15px`; provider-пиктограммы вакансии/рекрутёра увеличены до `20 × 20` и отделены от текста областью `40px`; fallback-изображения масштабированы внутри кругов; checkbox подписок выровнен с подписью; service-card перенесена под профиль кандидата, а date/time-контролу закреплена непротекающая геометрия. |
| 2026-07-28 | Колонки candidate/vacancy и rating/recruiter закреплены как `50/50` с учётом absolute-positioned slot-ов CUBA GridLayout; корням сеток возвращена ненулевая высота, communication оставлен полноширинной второй строкой; ширина sidebar синхронизирована с его Vaadin slot, date/time layout ограничен шириной карточки; оба верхних `OvaFallbackImage` выровнены до `96 × 96`. |
| 2026-07-28 | Shared Edit SCSS переведён в базовый слой, локальный final partial ограничивает GridLayout, picker/action-кнопки, `edit-form-control`, рабочий scroll overflow и одинаковый размер изображений `96 × 96`; добавлены критерии отсутствия перекрытия result/comment. |
| 2026-07-28 | Унифицированы `OvaFallbackImage`, восстановлены title/styles label-навигации, выровнены picker-поля, status/priority, sidebar-карточки и AUTO-геометрия блока результата; в vacancy-card добавлено наименование вакансии. |
