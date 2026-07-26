# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)  
> Визуальные референсы: [ExtSettingsWindow](ExtSettingsWindow_Spec.md), [CandidateCVEdit](CandidateCVEdit_Spec.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует экземпляр `IteractionList`: взаимодействие рекрутёра с кандидатом по конкретной вакансии. Через экран пользователь выбирает кандидата, вакансию и тип взаимодействия, вводит предусмотренные типом дополнительные данные, оценку, способ коммуникации, рекрутёра и комментарий.

Запись участвует в истории кандидата, подписках, уведомлениях и изменении статусов рекрутингового процесса. Поэтому визуальный слой обязан сохранять component ID, data bindings, actions, `invoke`, loaders, JPQL, views, validation и lifecycle CUBA Platform 7.3.

Изменение от 2026-07-27 устраняет дефекты, видимые на реальном экране:

- одинаковые по визуальному весу фотография кандидата и логотип проекта не формировали ясную иерархию;
- sidebar не показывала человекочитаемое наименование редактируемого экземпляра;
- номер и дата разрывали последовательность «образ → имя → навигация»;
- пять быстрых кнопок отображались отдельной строкой над формой, тогда как раздел «Частые взаимодействия» оставался пустым;
- зелёные pill-кнопки и увеличенные заголовки аккордеонов не соответствовали канонической геометрии HRM HuntTech;
- кнопки footer визуально расходились по всей ширине рабочей области.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания, копирования или редактирования взаимодействия.
- Picker кандидата сохраняет suggestion, lookup и open для `JobCandidate`; picker вакансии сохраняет lookup и open для `OpenPosition`.
- Постоянная sidebar содержит фотографию кандидата, логотип проекта, ФИО кандидата, название вакансии, индекс пяти разделов, служебную карточку номера/даты и карточку контекста вакансии.
- ФИО и название вакансии читаются непосредственно из `iteractionListDc` через `candidate.fullName` и `vacancy.vacansyName`. Новые loader, JPQL и Java-обработчики не добавляются.
- Индекс расположен в порядке: «Кандидат и вакансия» → «Тип и действие» → «Результат» → «Комментарий» → «Частые взаимодействия».
- Клик по пункту индекса раскрывает соответствующий `GroupBoxLayout`, сворачивает остальные блоки, выделяет активный пункт и переводит фокус в первое рабочее поле блока.
- Правая область содержит toolbar, штатный `TabSheet`, прокручиваемые полноширинные аккордеоны и компактную группу существующих действий справа в footer.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.

## Behavior Summary

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовый экземпляр `IteractionList`;
- открытие формы → в sidebar отображаются загруженные кандидат и вакансия → пользователь видит человекочитаемый контекст записи;
- выбор кандидата → binding `candidate` обновляет сущность → подпись `candidate.fullName` и фотография синхронизируются через тот же `iteractionListDc`;
- выбор вакансии → binding `vacancy` обновляет сущность → подпись `vacancy.vacansyName`, логотип и карточка контекста обновляются прежней логикой;
- выбор пункта sidebar → раскрывается ровно один связанный блок → данные сущности не изменяются;
- выбор типа взаимодействия → прежняя Java-логика управляет `buttonCallAction`, `addString`, `addDate` и `addInteger`;
- открытие «Частые взаимодействия» → внутри секции отображаются пять равных кнопок → названия и статистика формируются существующим контроллером;
- клик по заполненной быстрой кнопке → точный объект `Iteraction` устанавливается в `iteractionTypeField` → binding записывает тип в текущий `IteractionList`;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → бизнес-логика остаётся неизменной.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| `@UiController` | `hunttech_IteractionList.edit` |
| Java | `com.company.hunttech.web.screens.iteractionlist.IteractionListEdit` |
| Базовый класс | `StandardEditor<IteractionList>` |
| `@EditedEntityContainer` | `iteractionListDc` |
| Загрузка | `@LoadDataBeforeShow` |
| Root namespace | `.iteraction-list-editor` |
| Диалог | `width=1240`, `height=760`, `modal=true` |
| Темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

## 2. Data-контракты

| Контейнер / loader | View | Назначение | Статус |
|---|---|---|---|
| `iteractionListDc` / `iteractionListDl` | `iteractionList-edit-view` | редактируемое взаимодействие и подписи контекста | без изменения view |
| `iteractionTypesDc` / `iteractionTypesLc` | `iteraction-list-type-view` | типы взаимодействий | без изменений |
| `openPositionDc` / `openPositionsDl` | `openPosition-iteraction-list-picker-view` | вакансии с действующими conditions | без изменений |
| `usersDc` / `usersDl` | `_minimal` | активные пользователи | без изменений |

`iteractionList-edit-view` уже содержит `candidate.fullName`, а `openPosition-iteraction-list-picker-view` содержит `vacansyName`. Поэтому подписи sidebar соблюдают Data View Integrity и не требуют изменения `views.xml`.

Entity, поля, БД, Liquibase, definitions views, loaders и JPQL не изменяются. Агрегирующий запрос частых взаимодействий остаётся в существующем контроллере.

## 3. Компоновка

```text
main layout 100% × 100%
├─ sidebar 296 px / 276 px / 260 px
│  ├─ candidateImage 112 × 112 + projectLogoImage 80 × 80
│  ├─ candidate.fullName
│  ├─ vacancy.vacansyName
│  ├─ индекс пяти разделов
│  ├─ карточка взаимодействия: номер + дата
│  ├─ карточка вакансии: подразделение, проект, статус, приоритет, стоимость, рейтинг
│  └─ spacer
└─ workspace
   ├─ toolbar: заголовок + контекст
   ├─ TabSheet 48 px
   ├─ scrollable content
   │  ├─ Кандидат и вакансия [expanded]
   │  ├─ Тип и действие [collapsed]
   │  ├─ Результат [collapsed]
   │  ├─ Комментарий [collapsed]
   │  └─ Частые взаимодействия [collapsed, пять runtime-кнопок]
   └─ footer: spacer + subscribe/commit/cancel group
```

### 3.1. Sidebar

Порядок соответствует обязательному контракту Edit-форм HRM HuntTech:

1. визуальный образ экземпляра;
2. человекочитаемое наименование;
3. label-навигация;
4. детализация основных элементов;
5. служебные сведения.

`candidateImage` сохраняет размер `112 × 112 px`, binding `candidate.fileImageFace`, fallback `icons/no-programmer.jpeg` и `SCALE_DOWN`. `projectLogoImage` сохраняет legacy ID и Java-инъекцию, но получает отдельный локальный стиль и размер `80 × 80 px`, чтобы логотип не конкурировал с фотографией кандидата. Для логотипа применяется `object-fit: contain`.

`iteractionCandidateNameLabel` и `iteractionVacancyNameLabel` являются read-only отражением текущего `IteractionList`. Они не создают параллельного состояния и не записывают данные вне `iteractionListDc`.

### 3.2. Аккордеоны

Каждая секция остаётся штатным `GroupBoxLayout`. Каноническая геометрия:

- радиус `8 px`;
- тонкая theme-aware граница;
- интервал между секциями `10 px`;
- заголовок: `min-height 44 px`, padding `9 × 16 px`, `15 px`, weight `600`;
- контент: padding `16 × 18 × 18 px`;
- основная секция раскрыта, остальные свёрнуты;
- collapsed-состояние не очищает значения и не меняет validation.

### 3.3. Частые взаимодействия

`mostPopularHbox` перенесён внутрь `popularAccordion`. Это тот же runtime-хост, который контроллер очищает и заполняет пятью CUBA `Button`; ID и Java-контракт сохранены.

Кнопки:

- занимают по `20%`;
- имеют высоту `40 px`;
- используют радиус `8 px`, а не декоративный pill-радиус;
- используют theme-aware поверхность и `$v-selection-color`;
- явно сохраняют видимость `.v-button-wrap` и `.v-button-caption`;
- показывают disabled «Нет данных» без изменения геометрии.

### 3.4. Footer

`subscribeButton`, `windowCommitAndClose` и `windowClose` объединены в `editActionsGroup` справа. Порядок, actions, `invoke`, captions и enable-состояния не изменены. `editActionsSpacer` выполняет только функцию геометрического выравнивания.

## 4. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `candidateField`, `vacancyFiels` | bindings, lookup/open actions, query/optionsContainer |
| `iteractionTypeField` | binding, lookup и существующий value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime visible/required/caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer и option icon provider |
| `communicationMethodField` | binding и caption |
| `commentField` | binding, lazy reload, runtime required и автодополнение |
| `candidateImage` | legacy ID, `OvaFallbackImage`, binding и fallback |
| `projectLogoImage` | legacy ID, Java-инъекция, `OvaFallbackImage` и fallback |
| `mostPopularHbox` | пять равных быстрых кнопок; прямое присваивание `Iteraction` |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | subscribe → commit-and-close → cancel |

## 5. Локальный SCSS

Все правила ограничены корнем `.iteraction-list-editor`. Финальный `iteraction-list-reference-finish.scss` синхронизирован во всех семи темах.

Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` и `.v-panel` не изменяются. Вложенные Vaadin-селекторы применяются только внутри локального root-класса.

## 6. Ограничения изменений

- Java-контроллер и бизнес-логика не изменены;
- entity, поля, БД, Liquibase и definitions views не изменены;
- loader ID, conditions, JPQL, bindings, component ID, actions и `invoke` сохранены;
- validation, required, editable, visible и enable-состояния не изменены;
- навигация не записывает данные и не вмешивается в lifecycle;
- production не изменяется;
- merge допускается только после отчёта Hermes по точному HEAD SHA и прямой команды Алексея.

## 7. Обязательная проверка Hermes

1. HEAD branch и HEAD PR совпадают с SHA, указанным в PR.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. Профильные contract-тесты `IteractionListEdit` — PASS.
5. Compile web и core tests — PASS.
6. `ScreenViewIntegrityTest` — `8/8 PASS`.
7. Data View Integrity — PASS.
8. `:app-web:buildScssThemes` — PASS для семи тем.
9. `clean assemble` — `BUILD SUCCESSFUL`.
10. Local deploy и HTTP `/hrm/` = `200`.
11. Functional smoke: candidate/vacancy, lookup/open, тип, dynamic fields, rating, recruiter, communication, comment, subscription, save/cancel.
12. Navigation smoke: каждый пункт раскрывает связанный блок и фокусирует первое поле.
13. Frequent-actions smoke: кнопки находятся внутри секции, captions видимы, прямое присваивание типа работает, placeholders disabled.
14. Visual smoke семи тем: порядок sidebar, компактные аккордеоны, отсутствие пустой секции и верхней дублирующей строки, сгруппированный footer, отсутствие горизонтальной прокрутки.
15. Tomcat logs: новых critical errors NONE; P1 = 0; P2 = 0.

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Sidebar приведена к порядку «образ → имя → навигация → детали», частые взаимодействия перенесены в свой аккордеон, аккордеоны и footer уплотнены без изменения бизнес-логики |
| 2026-07-26 | `candidateImage` и `projectLogoImage` унифицированы как `OvaFallbackImage` |
| 2026-07-26 | Восстановлен видимый аккордеон «Кандидат и вакансия», добавлен блок пяти быстрых действий |
| 2026-07-26 | Компоновка синхронизирована с `CandidateCVEdit` и `ExtSettingsWindow` |
| 2026-07-25 | Добавлены двухколоночная строка кандидата и вакансии, кликабельный индекс и пять быстрых взаимодействий |
| 2026-07-25 | Исправлен сценарий «Копировать» для detached-вакансии |
| 2026-07-25 | Аккордеоны приведены к presentation-контракту `SettingsWindow` |
| 2026-07-25 | Выполнена двухпанельная визуальная адаптация с локальным namespace `.iteraction-list-editor` |
