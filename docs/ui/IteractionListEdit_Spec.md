# IteractionListEdit — спецификация экранной формы

**PROJECT: HRM HuntTech**

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует взаимодействие рекрутёра с кандидатом по вакансии: кандидата, вакансию, тип взаимодействия, дополнительное действие, результат, способ коммуникации, рекрутёра и комментарий. Экран участвует в истории кандидата, подписках, уведомлениях и изменении статусов процесса, поэтому UI-изменения обязаны сохранять существующие data bindings, component ID, actions, `invoke`, loaders и lifecycle CUBA Platform 7.3.

Исправление от 2026-07-25 устраняет runtime-регрессию после предыдущей перестройки: `candidateField` мог визуально схлопываться из-за принудительного управления внутренними `DIV/TABLE` GridLayout, а селектор разделов находился под высокой карточкой в общей левой панели. Новый контракт переносит selector-host в правую рабочую область, сохраняет штатное динамическое создание кнопок контроллером и делает доступ к блоку «Частые взаимодействия» фактически видимым.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания или редактирования взаимодействия.
- Команда «Копировать» сохраняет существующий сценарий безопасной передачи кандидата и вакансии.
- Picker кандидата сохраняет suggestion search, lookup и open для `JobCandidate`; picker вакансии — lookup/open для `OpenPosition`.
- Глобальная левая панель шириной `228 px` содержит только контекст взаимодействия: изображения, номер, дату, компанию, проект, статус, приоритет и стоимость.
- Селектор пяти блоков находится внутри правой рабочей области, слева от прокручиваемых аккордеонов, по модели `SettingsWindow`.
- Порядок селектора: «Кандидат и вакансия» → «Тип и действие» → «Результат» → «Комментарий» → «Частые взаимодействия».
- Клик по пункту раскрывает соответствующий `GroupBoxLayout`, сворачивает остальные блоки, выделяет активный пункт и переводит фокус в первое рабочее поле.
- Клики по штатным заголовкам аккордеонов синхронизируют активный пункт селектора.
- Экран остаётся модальным диалогом `1100 × 650`.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.

## Behavior Summary

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовую форму;
- открытие формы → селектор блоков видим в рабочей области, раскрыт блок «Кандидат и вакансия» → `candidateField` и `vacancyFiels` доступны в одной строке;
- ввод минимум трёх символов в `candidateField` → выполняется существующий suggestion query → пользователь выбирает кандидата либо открывает lookup;
- выбор пункта селектора → раскрывается ровно один связанный блок → фокус переводится в `candidateField`, `iteractionTypeField`, `ratingField`, `commentField` или первую доступную быструю кнопку;
- выбор кандидата или вакансии → выполняются прежние обработчики изображения, статуса, подписок, приоритета и проекта → бизнес-поведение не меняется;
- выбор типа взаимодействия → прежняя Java-логика управляет `buttonCallAction`, `addString`, `addDate` и `addInteger`;
- открытие блока «Частые взаимодействия» через видимый selector → контроллер создаёт и показывает пять равных кнопок → captions и точные `Iteraction` берутся из статистики текущего пользователя;
- клик по заполненной быстрой кнопке → точный объект `Iteraction` устанавливается в `iteractionTypeField` → caption не разбирается и поиск по строке не выполняется;
- недостаточно пяти типов → свободные позиции остаются видимыми disabled-кнопками «Нет данных» → геометрия блока стабильна;
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

Entity, поля, БД, Liquibase, definitions views, существующие loaders и их JPQL не изменяются. Агрегирующий JPQL частых взаимодействий остаётся read-only запросом контроллера.

## 3. Компоновка

```text
main layout 100% × 100%
├─ context sidebar 228 px
│  └─ карточка контекста кандидата и вакансии
└─ workspace
   ├─ toolbar
   ├─ tab content
   │  ├─ section selector 210 px [fixed]
   │  └─ scrollable accordion content
   │     ├─ Кандидат и вакансия [expanded]
   │     ├─ Тип и действие [collapsed]
   │     ├─ Результат [collapsed]
   │     ├─ Комментарий [collapsed]
   │     └─ Частые взаимодействия [collapsed]
   └─ footer
```

`candidateField` и `vacancyFiels` располагаются в двух явных flex-колонках `GridLayout`. Это соответствует штатному контракту CUBA: свободная ширина распределяется самим контейнером, без вмешательства SCSS во внутренние `DIV` и `TABLE` GridLayout.

## 4. Контракт селектора блоков

XML содержит `iteractionListNavigation` с пятью fallback `Label` для CUBA Designer. На `InitEvent` базовый контроллер заменяет их доступными borderless-кнопками `participantsAccordionNav`, `interactionAccordionNav`, `resultAccordionNav`, `commentAccordionNav`, `popularAccordionNav`.

`iteractionListNavigation` находится внутри `iteractionListSectionLayout` в правой рабочей области и расположен до `iteractionListContentScrollBox`. Поэтому созданные контроллером кнопки остаются видимыми при вертикальной прокрутке аккордеонов.

Навигация меняет только expanded-state, style и focus. Она не запускает loaders, commit и не записывает поля редактируемой сущности.

Совместимый controller `hunttech_IteractionList.edit.accordion` остаётся тонким alias к основному экрану и использует тот же descriptor.

## 5. Частые взаимодействия за скользящий год

Один агрегирующий запрос:

- ограничивает выборку текущим `recrutier`;
- использует период от текущего момента минус один календарный год до текущего момента;
- исключает записи без `iteractionType`;
- группирует по `iteractionType`;
- сортирует по `count(iteractionType)` по убыванию.

XML содержит видимый host `mostPopularHbox` с `height="AUTO"`. На `BeforeShowEvent` контроллер очищает host, создаёт ровно пять CUBA `Button`, назначает каждой заполненной кнопке точную entity через click listener, а свободные позиции оставляет disabled «Нет данных». Caption не используется для обратного поиска.

В CUBA Platform 7.3 метод `HBoxLayout.expand(Component)` принимает один компонент за вызов. Контроллер последовательно вызывает `mostPopularHbox.expand(popularButton)` для каждой из пяти runtime-кнопок; SCSS задаёт host минимальную высоту `52 px` и ограничивает каждый slot двадцатью процентами ширины.

## 6. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `gridIterationData` | legacy ID и тип `GridLayout`; две flex-колонки |
| `candidateField`, `vacancyFiels` | IDs, bindings, suggestion query/optionsContainer, lookup/open actions |
| `iteractionTypeField` | binding, lookup и существующий value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime visible/required/caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer и option icon provider |
| `communicationMethodField` | binding и caption |
| `commentField` | binding, lazy reload, runtime required и автодополнение |
| `candidateImage` | legacy ID, `OvaFallbackImage`, binding и fallback |
| `projectLogoImage` | прежний source и Java-инъекция |
| `mostPopularHbox` | host пяти runtime-кнопок; прямое присваивание `Iteraction` |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | subscribe → commit-and-close → cancel |

## 7. Локальный SCSS

Все правила ограничены корнем `.iteraction-list-editor`. Одинаковый файл `iteraction-list-accordion-navigation.scss` используется во всех семи темах.

Запрещены селекторы `.iteraction-list-form-grid > div` и `.iteraction-list-form-grid table` с принудительной шириной. GridLayout управляет внутренней геометрией самостоятельно; SCSS ограничивает только внешний контейнер, slots и штатные input-компоненты.

Селектор блоков получает фиксированную ширину `210 px` (`190 px` при узком viewport), независимую вертикальную прокрутку и theme-aware фон/границу. Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` не изменяются.

## 8. Ограничения изменений

- entity, поля, БД, Liquibase и definitions views не изменены;
- loader ID, conditions, JPQL, actions, bindings, legacy component ID и `invoke` сохранены;
- сервисы и бизнес-обработчики сохранения не изменены;
- изменения ограничены XML-компоновкой, локальным SCSS, тестами и документацией;
- `.github/workflows/` не изменяются;
- production не изменяется;
- merge допускается только после отчёта Hermes по точному HEAD SHA и прямой команды Алексея.

## 9. Обязательная проверка Hermes

1. HEAD branch и HEAD PR совпадают с SHA, указанным в PR.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. `IteractionListEditAccordionLayoutTest` — `5/5 PASS`.
5. `IteractionListAccordionNavigationTest` — `5/5 PASS`.
6. `IteractionListMostPopularInteractionTest` — `5/5 PASS`.
7. `LeftSidebarAvatarComponentTest` — `2/2 PASS`.
8. Compile web и core tests.
9. `ScreenViewIntegrityTest` — `8/8 PASS`.
10. Data View Integrity — PASS.
11. `:app-web:buildScssThemes` — PASS для семи тем.
12. `clean assemble` — `BUILD SUCCESSFUL`.
13. Local deploy и HTTP `/hrm/` = `200`.
14. Candidate smoke: `candidateField` видим, принимает ввод, показывает suggestion, lookup/open работают; vacancy остаётся в той же строке.
15. Navigation smoke: пять пунктов видимы в правой рабочей области; каждый раскрывает один блок, выделяется и фокусирует целевой компонент; прокрутка контента не скрывает selector.
16. Popular smoke: после открытия блока видны ровно пять равных кнопок; заполненные назначают точный тип, пустые остаются disabled «Нет данных».
17. Functional smoke: dynamic fields, rating, recruiter, communication, comment, subscription, save/cancel без регрессии.
18. Visual smoke семи тем: отсутствуют горизонтальная прокрутка, схлопывание picker-полей и перекрытия.
19. Tomcat logs: новых critical errors NONE; P1 = 0; P2 = 0.

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Устранена runtime-регрессия: восстановлена видимость `candidateField`, selector-host пяти блоков перенесён в правую рабочую область, host частых взаимодействий получил гарантированную высоту |
| 2026-07-25 | Исправлена совместимость с CUBA 7.3: `HBoxLayout.expand` вызывается отдельно для каждой из пяти быстрых кнопок вместо передачи `Component[]` |
| 2026-07-25 | Добавлены двухколоночная строка кандидата и вакансии, кликабельный индекс пяти блоков и пять равных кнопок частых взаимодействий текущего пользователя за скользящий год |
| 2026-07-25 | Исправлен сценарий «Копировать»: вакансия перечитывается через `openPosition-iteraction-list-picker-view` до открытия нового `IteractionListEdit` |
| 2026-07-25 | Аккордеоны приведены к presentation-контракту `SettingsWindow`; первая секция раскрыта, остальные свёрнуты |
| 2026-07-25 | В семи темах добавлено локальное CSS-оформление аккордеонов внутри `.iteraction-list-editor` |
| 2026-07-25 | Основные рабочие блоки преобразованы в сворачиваемые секции без изменения data-контрактов |
| 2026-07-25 | `candidateImage` заменён на `OvaFallbackImage` 104×104 px с fallback `icons/no-programmer.jpeg` |
| 2026-07-25 | Sidebar, toolbar, footer и рабочая геометрия синхронизированы во всех семи темах |
| 2026-07-25 | Выполнена двухпанельная визуальная адаптация с локальным namespace `.iteraction-list-editor` |
