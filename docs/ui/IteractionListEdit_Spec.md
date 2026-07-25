# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует конкретное взаимодействие рекрутёра с кандидатом по вакансии: участника процесса, вакансию, тип взаимодействия, рейтинг, способ коммуникации, рекрутёра, дополнительное значение и комментарий. Экран влияет на цепочку взаимодействий, статус кандидата, подписки, уведомления, сведения о трудоустройстве и историю вакансии, поэтому визуальная адаптация обязана сохранять все CUBA- и бизнес-контракты.

Редизайн от 2026-07-25 решает только задачу визуальной иерархии: постоянный контекст кандидата и вакансии вынесен в тёмную панель, рабочие поля сгруппированы в светлые карточки, а существующие действия закреплены в нижней панели. Java-контроллер, entity, БД, loaders, JPQL, views, bindings, actions и `invoke` не изменялись.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания/редактирования взаимодействия.
- Picker кандидата открывает lookup и editor `JobCandidate`.
- Picker вакансии открывает lookup и editor `OpenPosition`.
- Кнопка динамического действия может открыть экран класса, заданного типом взаимодействия.
- Подписка открывает editor `SubscribeCandidateAction`.
- Сохранение выполняется стандартным action `windowCommitAndClose`; отмена — `windowClose`.
- Экран остаётся модальным диалогом `1000 × 650`; размер не изменён.

## Behavior Summary

- открытие нового взаимодействия → контейнер получает новую entity → контроллер заполняет номер, дату и текущего рекрутёра;
- выбор кандидата → меняется `candidateField` → сохраняются прежние проверки владения кандидатом и source фотографии;
- выбор вакансии → меняется `vacancyFiels` → выполняются прежние проверки закрытия, соответствия кандидату, подписки, статуса, приоритета и логотипа проекта;
- выбор типа взаимодействия → меняется `iteractionTypeField` → контроллер переключает `buttonCallAction`, `addDate`, `addString` или `addInteger`, их caption и required;
- ввод дополнительного значения → срабатывает существующий listener → значение добавляется в комментарий;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → формируется цепочка, обновляются статусы, новости, подписки и email;
- смена темы → подключается локальный mixin `iteraction-list-editor-theme` → структура и поведение формы не меняются.

## 1. Точка вызова и технический контекст

| Параметр | Значение |
|---|---|
| `@UiController` | `hunttech_IteractionList.edit` |
| Java | `com.company.hunttech.web.screens.iteractionlist.IteractionListEdit` |
| Базовый класс | `StandardEditor<IteractionList>` |
| `@EditedEntityContainer` | `iteractionListDc` |
| Загрузка | `@LoadDataBeforeShow` |
| Root namespace | `.iteraction-list-editor` |
| Диалог | `width=1000`, `height=650`, `modal=true` |
| Эталонная тема | `halo` |
| Активная тема по умолчанию | `hover` |
| Поддерживаемые темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

## 2. Data-контракты

| Контейнер / loader | Тип / view | Назначение | Статус редизайна |
|---|---|---|---|
| `iteractionListDc` / `iteractionListDl` | `IteractionList`, `iteractionList-edit-view` | редактируемое взаимодействие | без изменений |
| `iteractionTypesDc` / `iteractionTypesLc` | `Iteraction`, `iteraction-list-type-view` | типы взаимодействий | без изменений |
| `openPositionDc` / `openPositionsDl` | `OpenPosition`, `openPosition-iteraction-list-picker-view` | вакансии с фильтрами | без изменений |
| `usersDc` / `usersDl` | `User`, `_minimal` | рекрутёры | без изменений |

Сохранены исходные JPQL, query conditions, параметры loaders, `cacheable`, views и защита `openPositionsDl` от ранней загрузки. `iteractionList-edit-view` не изменялся.

## 3. Карта component ID и сохранённых контрактов

| ID | Тип | Binding / action / invoke | Java-инъекция и обработчик | Динамическое состояние | Было → стало | Контракт |
|---|---|---|---|---|---|---|
| `numberIteractionField` | `TextField<BigDecimal>` | `iteractionListDc.numberIteraction` | `@Inject`; `setIteractionNumber()` | read-only | верхний groupBox → sidebar, служебный блок | сохранён |
| `dateIteractionField` | `DateField<Date>` | `iteractionListDc.dateIteraction` | `@Inject`; `setCurrentDate()`, `onBeforeShow()` | editable только для новой entity | верхний groupBox → sidebar | сохранён |
| `closingDateVacancyLabel` | `Label<String>` | — | `@Inject`; `setClosingDateLabel()` | `visible` и style меняет Java | верхний groupBox → sidebar | сохранён |
| `companyLabel` | `Label<String>` | HTML value | `@Inject`; `vacancyFieldValueChange()` | value меняет Java | верхний groupBox → sidebar | сохранён |
| `projectLabel` | `Label<String>` | — | `@Inject`; `vacancyFieldValueChange()` | value меняет Java | верхний groupBox → sidebar | сохранён |
| `ratingLabel` | `Label<String>` | — | `@Inject`; listener `setRatingField()` | `visible=false`, runtime style | верхний groupBox → sidebar rating block | сохранён |
| `ratingImage` | `Image` | runtime theme resource | `@Inject`; `setPriorityLabel()` | `visible=false`, source меняет Java | верхний groupBox → sidebar rating block | сохранён |
| `projectLogoImage` | `Image` | runtime `vacancy.projectName.projectLogo` | `@Inject`; `onInit()`, `onVacancyFielsValueChange()` | source/valueSource меняет Java | верхний groupBox → sidebar, отдельное изображение | сохранён |
| `candidateImage` | `Image` | `iteractionListDc.candidate.fileImageFace` | `@Inject`; `onInit()`, `onCandidateFieldValueChange()` | source/valueSource меняет Java | верхний groupBox → sidebar, отдельное изображение | сохранён |
| `mostPopularHbox` | `HBoxLayout` | динамические LinkButton | `@Inject`; `setMostPopularIteraction()` | Java добавляет компоненты | отдельный groupBox → карточка популярных | сохранён |
| `mostPopularIteractionHBox` | `HBoxLayout` | — | прямой injection отсутствует | остаётся отдельной XML-точкой | grid → та же карточка, отдельный контейнер | сохранён |
| `statusOfVacansyLabel` | `Label<String>` | — | `@Inject`; `setStatusOfVacancyLabel()` | value/style меняет Java | grid → sidebar | сохранён |
| `alternativeVacancyLinkButton` | `LinkButton` | click-логика/description Java | `@Inject`; `setStatusOfVacancyLabel()` | `visible=false`, description меняет Java | grid → sidebar рядом со статусом | сохранён |
| `trafficLighterImage` | `Image` | runtime theme resource | `@Inject`; `setPriorityLabel()` | source меняет Java | grid → sidebar | сохранён |
| `currentPriorityLabel` | `Label<String>` | — | `@Inject`; `setPriorityLabel()` | value меняет Java | grid → sidebar | сохранён |
| `outstaffingCostHBox` | `HBoxLayout` | `vacancy.outstaffingCost` | `@Inject`; `onVacancyFielsValueChange2()` | `visible=false`, Java включает по данным | grid → sidebar | сохранён |
| `ratingField` | `LookupField` | `iteractionListDc.rating` | `@Inject`; `setRatingField()`, `@Install optionStyleProvider` | `required=true` | grid → рабочая карточка | сохранён |
| `candidateField` | `SuggestionPickerField<JobCandidate>` | `iteractionListDc.candidate`; lookup/open; исходный query | `@Inject`; несколько value listeners | `required=true` | grid → первый ряд рабочей карточки | сохранён |
| `vacancyFiels` | `LookupPickerField<OpenPosition>` | `iteractionListDc.vacancy`; lookup/open | `@Inject`; два `@Subscribe`, три `@Install` | options, icons, styles и dialogs меняет Java | grid → первый ряд рабочей карточки | сохранён |
| `onlyMySubscribeCheckBox` | `CheckBox` | runtime loader parameter | `@Inject`; `setOnlyMySubscribeCheckBox()` | value/listener меняет Java | рядом с вакансией → рядом с вакансией | сохранён |
| `iteractionTypeField` | `LookupPickerField<Iteraction>` | `iteractionListDc.iteractionType`; lookup | `@Inject`; `onIteractionTypeFieldValueChange()` | `required=true`; управляет dynamic fields | grid → рабочая карточка | сохранён |
| `buttonsPanelCallAction` | `ButtonsPanel` | — | injection отсутствует | адаптивный host динамических вариантов | grid → рабочая карточка | сохранён |
| `buttonCallAction` | `Button` | `invoke="callActionEntity"` | `@Inject`; `changeField()` | Java меняет visible/caption | dynamic panel → общий dynamic panel | сохранён |
| `addString` | `TextField<String>` | `iteractionListDc.addString` | `@Inject`; `changeField()`, `@Subscribe` | Java меняет visible/required/caption | dynamic panel → общий dynamic panel | сохранён |
| `addDate` | `DateField<Date>` | `iteractionListDc.addDate` | `@Inject`; `changeField()`, `@Subscribe` | Java меняет visible/required/caption/value | dynamic panel → общий dynamic panel | сохранён |
| `addInteger` | `TextField<Integer>` | `iteractionListDc.addInteger` | `@Inject`; `changeField()`, `@Subscribe` | Java меняет visible/required/caption | dynamic panel → общий dynamic panel | сохранён |
| `communicationMethodField` | `TextField<String>` | `iteractionListDc.communicationMethod` | injection отсутствует | стандартное binding | grid → рабочая карточка | сохранён |
| `recrutierField` | `LookupPickerField<ExtUser>` | `iteractionListDc.recrutier`; lookup | `@Inject`; `onAfterShow()`, `@Install optionIconProvider` | value для новой entity задаёт Java | grid → рабочая карточка | сохранён |
| `commentField` | `TextArea<String>` | `iteractionListDc.comment` | `@Inject`; lazy load, required и автодополнение | required меняет Java | под grid → полноширинная карточка | сохранён |
| `subscribeButton` | `Button` | `invoke="onButtonSubscribeClick"` | публичный handler | доступность штатная | footer → локально оформленный footer | сохранён |
| `windowCommitAndClose` | standard action button | action | CUBA lifecycle | primary только presentation | footer → footer | сохранён |
| `windowClose` | standard action button | action | CUBA lifecycle | secondary только presentation | footer → footer | сохранён |

## 4. Новая визуальная композиция

```text
toolbar 58 px
└─ main layout
   ├─ sidebar 270 px
   │  ├─ candidateImage + projectLogoImage
   │  ├─ number/date/closingDate
   │  ├─ company/project
   │  ├─ vacancy status/priority/outstaffing
   │  └─ rating context
   └─ workspace
      ├─ штатный TabSheet 48 px
      └─ vertical ScrollBox
         ├─ popular interactions card
         ├─ primary interaction card
         │  ├─ candidate + vacancy
         │  ├─ rating
         │  ├─ interaction type + dynamic host
         │  └─ communication + recruiter
         └─ full-width comment card
footer 58 px: subscribe → commit-and-close → cancel
```

Выбран двухпанельный layout, поскольку исходный диалог шириной 1000 px оставляет около 700 px полезной рабочей области после sidebar. Рабочая сетка сохраняет две колонки, а комментарий вынесен в отдельную полноширинную карточку. Аккордеоны для основных полей не использованы: кандидат, вакансия, рейтинг и тип взаимодействия должны быть доступны без дополнительного раскрытия. Штатно collapsable остаётся только существующий блок популярных взаимодействий и контекстный groupBox.

## 5. Локальный SCSS

Для каждой темы создан файл:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-editor.scss
```

Корень и mixin:

```scss
@mixin iteraction-list-editor-theme {
  .iteraction-list-editor {
    // только локальные правила формы
  }
}
```

SCSS оформляет toolbar, sidebar, карточки, TabSheet, поля, picker actions, checkbox, link button, focus, hover, disabled, read-only, validation error, required и footer. Все Vaadin-селекторы вложены в `.iteraction-list-editor`. Зависимости от `.job-candidate-editor` и `.ext-settings-window` нет.

## 6. Неизменённые функциональные контракты

- `IteractionListEdit.java` не изменён;
- entity, поля, Liquibase, SQL и БД не изменены;
- `iteractionList-edit-view` и `views.xml` не изменены;
- все loaders, JPQL и query conditions идентичны исходным;
- component ID, типы компонентов, bindings, actions, `invoke`, validators и captions существующих компонентов сохранены;
- `Image` не заменены на другие типы;
- `candidateImage` и `projectLogoImage` остаются отдельными компонентами;
- runtime `visible`, `required`, `editable`, caption и styles контроллера не переопределены XML;
- порядок footer-действий сохранён;
- производительность data-слоя не изменяется: loaders, сервисы, background tasks и изображения не добавлены.

## 7. Адаптивность и ограничения

- `dialogMode` сохранён `1000 × 650`;
- sidebar имеет базовую ширину 270 px и локальное уменьшение до 242 px при узком viewport;
- основной horizontal scroll не предусматривается;
- рабочие контейнеры используют `min-width: 0`;
- скрытые dynamic components остаются в одном контейнере и не получают статических `visible`/`required`;
- фактическая геометрия, отсутствие пустых slot и читаемость captions должны быть подтверждены visual smoke после локального deploy.

## 8. Проверки

| Проверка | Статус до Hermes |
|---|---|
| XML well-formed | PASS, локальная структурная проверка |
| Состав изменённых файлов | PASS: XML, локальный SCSS, theme imports, docs |
| Java изменён | NO |
| component ID / binding static audit | PASS |
| `git diff --check` | NOT VERIFIED |
| compile / compileTestJava | NOT VERIFIED |
| `ScreenViewIntegrityTest` 8/8 | NOT VERIFIED |
| Data View Integrity | NOT VERIFIED |
| `buildScssThemes` | NOT VERIFIED |
| `clean assemble` | NOT VERIFIED |
| local deploy / HTTP 200 | NOT VERIFIED |
| visual smoke семи тем | NOT VERIFIED |
| Tomcat logs / P1 / P2 | NOT VERIFIED |

До проверки Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Выполнена строго визуальная адаптация `IteractionListEdit`: двухпанельная композиция, локальный namespace `.iteraction-list-editor`, карточки, theme-aware состояния семи тем и сохранение всех Java/data/CUBA-контрактов |
