# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует взаимодействие рекрутёра с кандидатом по вакансии: кандидата, вакансию, тип взаимодействия, дополнительное действие, результат, способ коммуникации, рекрутёра и комментарий. Экран участвует в истории кандидата, подписках, уведомлениях и изменении статусов процесса, поэтому UI-изменения обязаны сохранять существующие data bindings, component ID, actions, `invoke`, loaders и lifecycle CUBA Platform 7.3.

Компоновка от 2026-07-25 разделяет рабочий сценарий на пять сворачиваемых блоков. Поля «Кандидат» и «Вакансия» находятся в одной строке, используют единый локальный CSS-контракт и не расширяют диалог по горизонтали. Левый индекс ускоряет переход между блоками. Блок «Частые взаимодействия» показывает пять наиболее часто используемых текущим пользователем типов за скользящий календарный год.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания или редактирования взаимодействия.
- Команда «Копировать» сохраняет существующий сценарий безопасной передачи кандидата и вакансии.
- Picker кандидата сохраняет lookup/open для `JobCandidate`; picker вакансии — lookup/open для `OpenPosition`.
- Левый индекс расположен в порядке: «Кандидат и вакансия» → «Тип и действие» → «Результат» → «Комментарий» → «Частые взаимодействия».
- Клик по пункту индекса раскрывает соответствующий `GroupBoxLayout`, сворачивает остальные блоки, выделяет активный пункт и переводит фокус в первое рабочее поле блока.
- Клики по штатным заголовкам аккордеонов синхронизируют активный пункт слева.
- Экран остаётся модальным диалогом `1100 × 650`; sidebar ограничен шириной `228 px`.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.

## Behavior Summary

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовую форму;
- открытие формы → раскрыт блок «Кандидат и вакансия», остальные четыре блока свёрнуты → основной контекст доступен первым;
- выбор пункта слева → раскрывается ровно один связанный блок → фокус переводится в `candidateField`, `iteractionTypeField`, `ratingField` или `commentField`;
- выбор кандидата или вакансии → выполняются прежние обработчики изображения, статуса, подписок, приоритета и проекта → бизнес-поведение не меняется;
- выбор типа взаимодействия → прежняя Java-логика управляет `buttonCallAction`, `addString`, `addDate` и `addInteger`;
- открытие блока частых взаимодействий → отображаются ровно пять равных кнопок → названия берутся из агрегированной статистики текущего пользователя за последний год;
- клик по заполненной быстрой кнопке → точный объект `Iteraction` устанавливается в `iteractionTypeField` → caption не разбирается и поиск по строке не выполняется;
- недостаточно пяти типов → свободные позиции показываются как disabled-кнопки «Нет данных» → геометрия блока остаётся стабильной;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → функциональные контракты сохраняются.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| `@UiController` | `hunttech_IteractionList.edit` |
| Java | `com.company.hunttech.web.screens.iteractionlist.IteractionListEdit` |
| Базовый класс | `StandardEditor<IteractionList>` |
| `@EditedEntityContainer` | `iteractionListDc` |
| Загрузка | `@LoadDataBeforeShow` |
| Root namespace | `.iteraction-list-editor` |
| Диалог | `width=1100`, `height=650`, `modal=true` |
| Темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

## 2. Data-контракты

| Контейнер / loader | View | Назначение | Статус |
|---|---|---|---|
| `iteractionListDc` / `iteractionListDl` | `iteractionList-edit-view` | редактируемое взаимодействие | без изменений |
| `iteractionTypesDc` / `iteractionTypesLc` | `iteraction-list-type-view` | типы взаимодействий | без изменений |
| `openPositionDc` / `openPositionsDl` | `openPosition-iteraction-list-picker-view` | вакансии с действующими conditions | без изменений |
| `usersDc` / `usersDl` | `_minimal` | активные пользователи | без изменений |

Entity, поля, БД, Liquibase, definitions views, существующие loaders и их JPQL не изменяются. Новый агрегирующий JPQL находится только в контроллере и читает статистику `IteractionList`.

## 3. Компоновка

```text
main layout 100% × 100%
├─ sidebar 228 px
│  ├─ карточка контекста кандидата и вакансии
│  └─ индекс пяти разделов
└─ workspace
   ├─ toolbar
   ├─ scrollable content
   │  ├─ Кандидат и вакансия [expanded]
   │  ├─ Тип и действие [collapsed]
   │  ├─ Результат [collapsed]
   │  ├─ Комментарий [collapsed]
   │  └─ Частые взаимодействия [collapsed]
   └─ footer
```

`candidateField` и `vacancyFiels` располагаются в двух колонках одной строки и используют `iteraction-list-primary-picker`. Workspace, GridLayout, picker-поля и их Vaadin-обёртки имеют локальные ограничения `min-width: 0` и `max-width: 100%`.

## 4. Контракт навигации

XML содержит пять fallback `Label`, чтобы структура оставалась читаемой в CUBA Designer. На `InitEvent` контроллер заменяет их borderless-кнопками. Навигация меняет только presentation state `GroupBoxLayout`; она не записывает entity, не вызывает `DataManager` и не выполняет commit.

Совместимый controller `hunttech_IteractionList.edit.accordion` остаётся тонким alias к основному экрану и использует тот же descriptor, чтобы не поддерживать две расходящиеся реализации.

## 5. Частые взаимодействия за скользящий год

Один агрегирующий запрос:

- ограничивает выборку текущим `recrutier`;
- использует период от текущего момента минус один календарный год до текущего момента;
- исключает записи без `iteractionType`;
- группирует по `iteractionType`;
- сортирует по `count(iteractionType)` по убыванию.

Контроллер всегда создаёт пять CUBA `Button`. HBox получает одинаковый expand ratio для всех пяти компонентов; SCSS ограничивает каждый slot двадцатью процентами доступной ширины. Номер позиции определяется порядком результата запроса, но не включается в caption и не используется для обратного поиска.

В CUBA Platform 7.3 метод `HBoxLayout.expand(Component)` принимает ровно один компонент за вызов. Поэтому после создания кнопок контроллер проходит по `List<Button>` и отдельно вызывает `mostPopularHbox.expand(popularButton)` для каждой позиции. Передача массива `Component[]` запрещена контрактным тестом, а одинаковый expand ratio сохраняет геометрию `5 × 20%`.

## 6. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `gridIterationData` | legacy ID и тип `GridLayout`; две колонки для кандидата и вакансии |
| `candidateField`, `vacancyFiels` | bindings, lookup/open actions, query/optionsContainer |
| `iteractionTypeField` | binding, lookup и существующий value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime visible/required/caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer и option icon provider |
| `communicationMethodField` | binding и caption |
| `commentField` | binding, lazy reload, runtime required и автодополнение |
| `candidateImage` | legacy ID, `OvaFallbackImage`, binding и fallback |
| `projectLogoImage` | прежний source и Java-инъекция |
| `mostPopularHbox` | пять равных быстрых кнопок; прямое присваивание `Iteraction` |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | subscribe → commit-and-close → cancel |

## 7. Локальный SCSS

Все правила ограничены корнем `.iteraction-list-editor`. Одинаковый файл `iteraction-list-accordion-navigation.scss` используется во всех семи темах. Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` не изменяются; Vaadin-селекторы применяются только как потомки локального root-класса.

## 8. Ограничения изменений

- entity, поля, БД, Liquibase и definitions views не изменены;
- существующие loader ID, conditions, actions, bindings, component ID и `invoke` сохранены;
- новый JPQL используется только для read-only статистики текущего пользователя;
- навигация не изменяет данные и lifecycle;
- CI/CD workflow не изменяются;
- production не изменяется;
- merge допускается только после отчёта Hermes по точному HEAD SHA и прямой команды Алексея.

## 9. Обязательная проверка Hermes

1. HEAD branch и HEAD PR совпадают с SHA, указанным в PR.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. `IteractionListEditAccordionLayoutTest` — `5/5 PASS`.
5. `IteractionListAccordionNavigationTest` — `4/4 PASS`.
6. `IteractionListMostPopularInteractionTest` — `4/4 PASS`.
7. `LeftSidebarAvatarComponentTest` — `2/2 PASS`.
8. Compile web и core tests.
9. `ScreenViewIntegrityTest` — `8/8 PASS`.
10. Data View Integrity — PASS.
11. `:app-web:buildScssThemes` — PASS для семи тем.
12. `clean assemble` — `BUILD SUCCESSFUL`.
13. Local deploy и HTTP `/hrm/` = `200`.
14. Functional smoke: candidate/vacancy, lookup/open, тип, dynamic fields, rating, recruiter, communication, comment, subscription, save/cancel.
15. Navigation smoke: каждый пункт раскрывает один блок, выделяется и фокусирует первое поле; заголовки GroupBox синхронизируют индекс.
16. Popular smoke: ровно пять равных кнопок, top-5 текущего пользователя за год, прямое присваивание типа, disabled placeholders.
17. Visual smoke семи тем: горизонтальная прокрутка отсутствует, candidate/vacancy имеют одинаковое оформление.
18. Tomcat logs: новых critical errors NONE; P1 = 0; P2 = 0.

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Исправлена совместимость с CUBA 7.3: `HBoxLayout.expand` вызывается отдельно для каждой из пяти быстрых кнопок вместо передачи `Component[]` |
| 2026-07-25 | Добавлены двухколоночная строка кандидата и вакансии, кликабельный индекс пяти блоков и пять равных кнопок частых взаимодействий текущего пользователя за скользящий год |
| 2026-07-25 | Исправлен сценарий «Копировать»: вакансия перечитывается через `openPosition-iteraction-list-picker-view` до открытия нового `IteractionListEdit` |
| 2026-07-25 | Аккордеоны приведены к presentation-контракту `SettingsWindow`; первая секция раскрыта, остальные свёрнуты |
| 2026-07-25 | В семи темах добавлено локальное CSS-оформление аккордеонов внутри `.iteraction-list-editor` |
| 2026-07-25 | Основные рабочие блоки преобразованы в сворачиваемые секции без изменения data-контрактов |
| 2026-07-25 | `candidateImage` заменён на `OvaFallbackImage` 104×104 px с fallback `icons/no-programmer.jpeg` |
| 2026-07-25 | Sidebar, toolbar, footer и рабочая геометрия синхронизированы во всех семи темах |
| 2026-07-25 | Выполнена двухпанельная визуальная адаптация с локальным namespace `.iteraction-list-editor` |
