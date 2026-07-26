# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)  
> Визуальные референсы: [ExtSettingsWindow](ExtSettingsWindow_Spec.md), [CandidateCVEdit](CandidateCVEdit_Spec.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует взаимодействие рекрутёра с кандидатом по вакансии: кандидата, вакансию, тип взаимодействия, дополнительное действие, результат, способ коммуникации, рекрутёра и комментарий. Экран участвует в истории кандидата, подписках, уведомлениях и изменении статусов процесса, поэтому визуальный рефакторинг обязан сохранять data bindings, component ID, actions, `invoke`, loaders, JPQL, views и lifecycle CUBA Platform 7.3.

Компоновка от 2026-07-26 объединяет два подтверждённых паттерна HRM HuntTech:

- `CandidateCVEdit` задаёт постоянную тёмную профильную sidebar с изображениями и контекстными карточками;
- `ExtSettingsWindow` задаёт вертикальный индекс разделов и полноширинные аккордеоны в светлой рабочей области.

Цель изменений — устранить перегруженную карточку слева, освободить полезную ширину справа и выстроить предсказуемый сценарий заполнения без изменения функциональности формы.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания или редактирования взаимодействия.
- Команда «Копировать» сохраняет существующий сценарий безопасной передачи кандидата и вакансии.
- Picker кандидата сохраняет suggestion, lookup и open для `JobCandidate`; picker вакансии — lookup и open для `OpenPosition`.
- Постоянная sidebar содержит изображения кандидата и проекта, расположенную сразу под ними карточку номера/даты, индекс пяти разделов и карточку вакансии.
- Статические подписи «Заголовок», «Список взаимодействий», «Заголовок» в sidebar удалены как дублирующие контекст формы.
- Индекс расположен в порядке: «Кандидат и вакансия» → «Тип и действие» → «Результат» → «Комментарий» → «Частые взаимодействия».
- Клик по пункту индекса раскрывает соответствующий `GroupBoxLayout`, сворачивает остальные блоки, выделяет активный пункт и переводит фокус в первое рабочее поле блока.
- Клики по штатным заголовкам аккордеонов синхронизируют активный пункт sidebar.
- Правая область содержит toolbar, штатный `TabSheet`, прокручиваемые полноширинные аккордеоны и footer действий.
- Диалог имеет размер `1240 × 760 px`; sidebar использует `296 px`, при сужении — `276 px` и `260 px`.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.

## Behavior Summary

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовую форму;
- открытие формы → перед аккордеонами виден блок пяти быстрых действий, а секция «Кандидат и вакансия» раскрыта и имеет видимый заголовок → основной контекст доступен первым;
- выбор пункта sidebar → раскрывается ровно один связанный блок → фокус переводится в `candidateField`, `iteractionTypeField`, `ratingField` или `commentField`;
- выбор кандидата или вакансии → выполняются прежние обработчики изображения, статуса, подписок, приоритета и проекта → бизнес-поведение не меняется;
- выбор типа взаимодействия → прежняя Java-логика управляет `buttonCallAction`, `addString`, `addDate` и `addInteger`;
- динамическое действие → отображается под типом взаимодействия в одной вертикальной последовательности → runtime `visible`, `required` и captions остаются под управлением контроллера;
- открытие формы → над аккордеонами отображаются ровно пять равных зелёных кнопок с полукруглыми краями → названия берутся из статистики текущего пользователя за последний год;
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
| Диалог | `width=1240`, `height=760`, `modal=true` |
| Темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

## 2. Data-контракты

| Контейнер / loader | View | Назначение | Статус |
|---|---|---|---|
| `iteractionListDc` / `iteractionListDl` | `iteractionList-edit-view` | редактируемое взаимодействие | без изменений |
| `iteractionTypesDc` / `iteractionTypesLc` | `iteraction-list-type-view` | типы взаимодействий | без изменений |
| `openPositionDc` / `openPositionsDl` | `openPosition-iteraction-list-picker-view` | вакансии с действующими conditions | без изменений |
| `usersDc` / `usersDl` | `_minimal` | активные пользователи | без изменений |

Entity, поля, БД, Liquibase, definitions views, существующие loaders и их JPQL не изменяются. Агрегирующий запрос частых взаимодействий остаётся в существующем контроллере и читает статистику `IteractionList`.

## 3. Компоновка

```text
main layout 100% × 100%
├─ sidebar 296 px / 276 px / 260 px
│  ├─ профиль: candidateImage + projectLogoImage (оба OvaFallbackImage)
│  ├─ карточка взаимодействия: номер + дата
│  ├─ индекс пяти разделов
│  ├─ карточка вакансии: подразделение, проект, статус, приоритет, стоимость, рейтинг
│  └─ spacer
└─ workspace
   ├─ toolbar: заголовок + контекст
   ├─ TabSheet 48 px
   ├─ scrollable content
   │  ├─ блок пяти быстрых действий 5 × 20%
   │  ├─ Кандидат и вакансия [expanded, caption visible]
   │  ├─ Тип и действие [collapsed]
   │  ├─ Результат [collapsed]
   │  ├─ Комментарий [collapsed]
   │  └─ Частые взаимодействия [collapsed]
   └─ footer
```

### 3.1. Sidebar по модели CandidateCVEdit

- внешний фон сохраняет градиент `#172638 → #132130 → #0f1b28`;
- основная ширина — `296 px`, совпадающая с профильной панелью `CandidateCVEdit`;
- `candidateImage` остаётся `OvaFallbackImage`, размер увеличен до `112 × 112 px`;
- `projectLogoImage` — `OvaFallbackImage` 80 × 80 px с fallback `icons/no-company.png`;
- три дублирующие статические подписи удалены;
- карточка номера и даты расположена непосредственно под изображениями; индекс идёт после неё и до карточки вакансии;
- служебные значения разделены на две внутренние карточки вместо одного длинного `GroupBoxLayout`;
- карточки имеют радиус `8 px`, тонкую полупрозрачную границу и локальные заголовки;
- `companyLabel`, `projectLabel`, `statusOfVacansyLabel` и `currentPriorityLabel` переносят длинные значения;
- HTML внутри `companyLabel` наследует светлый цвет sidebar;
- статус и приоритет используют строки с `expand` текстового компонента, поэтому warning/icon не вытесняют подпись;
- `outstaffingCostHBox` сохраняет ID и binding, но caption и значение стоимости разделены;
- вертикальная прокрутка разрешена только sidebar, горизонтальная прокрутка запрещена.

### 3.2. Индекс по модели SettingsWindow

`iteractionListNavigation` располагается после карточки номера/даты и до карточки вакансии. XML содержит пять fallback `Label`, чтобы структура оставалась читаемой в CUBA Designer. На `InitEvent` контроллер заменяет их borderless-кнопками.

Навигация меняет только presentation state `GroupBoxLayout`; она не записывает entity, не вызывает `DataManager` и не выполняет commit. Активный пункт использует акцент `#ffb11b`, левую границу `3 px` и полупрозрачный фон.

### 3.3. Workspace

- toolbar имеет минимальную высоту `58 px` и вертикальную пару «заголовок + контекст»;
- штатный `TabSheet` сохранён, высота строки вкладки — `48 px`;
- содержимое прокручивается внутри workspace и не двигает sidebar;
- удалена дополнительная колонка навигации шириной `210 px`, поэтому аккордеоны используют всю доступную ширину;
- content padding — преимущественно `18–20 px`;
- footer имеет высоту не менее `62 px`, существующие действия выровнены справа.

### 3.4. Аккордеоны

Каждая секция остаётся штатным `GroupBoxLayout`, а локальный финальный слой копирует подтверждённую геометрию `SettingsWindow`: поверхность `$v-panel-background-color`, граница `1 px rgba($v-font-color, 0.15)`, радиус `8 px`, тень `0 2px 8px rgba(15, 23, 42, 0.05)` и интервал `10 px`. Caption всегда видим, имеет `min-height: 50 px`, padding `12 × 16 px`, размер `17 px`, насыщенность `700` и фон `mix($v-app-background-color, $v-panel-background-color, 68%)`. Контент имеет padding `20 × 22 × 22 px`.

`participantsAccordion` явно видим и раскрыт при старте. Нажатие «Кандидат и вакансия» в sidebar раскрывает его, сворачивает остальные секции и переводит фокус в `candidateField`.

### 3.5. Поля и колонки

- `candidateField` и `vacancyFiels` остаются в двух explicit flex-колонках одной строки;
- оба picker-поля используют `iteraction-list-primary-picker`;
- `interactionAccordion` использует одну колонку: тип взаимодействия сверху, динамический блок ниже;
- `resultAccordion` сохраняет две колонки для рейтинга и рекрутёра, способ коммуникации занимает всю ширину;
- `commentField` занимает всю ширину секции;
- внутренние `DIV` и `TABLE` штатного CUBA `GridLayout` не переопределяются SCSS.

## 4. Частые взаимодействия за скользящий год

Один существующий агрегирующий запрос:

- ограничивает выборку текущим `recrutier`;
- использует период от текущего момента минус один календарный год до текущего момента;
- исключает записи без `iteractionType`;
- группирует по `iteractionType`;
- сортирует по `count(iteractionType)` по убыванию.

Контроллер всегда создаёт пять CUBA `Button`. `mostPopularHbox` расположен в полноширинной карточке перед аккордеонами; каждый slot занимает 20%. Кнопки зелёные, высотой 46 px, с радиусом 999 px. Номер позиции определяется порядком результата запроса, но не включается в caption и не используется для обратного поиска.

В CUBA Platform 7.3 метод `HBoxLayout.expand(Component)` принимает ровно один компонент за вызов. Поэтому контроллер отдельно вызывает `mostPopularHbox.expand(popularButton)` для каждой позиции.

## 5. Сохранённые component-контракты

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
| `projectLogoImage` | legacy ID, Java-инъекция, `OvaFallbackImage` и fallback `icons/no-company.png` |
| `mostPopularHbox` | пять равных быстрых кнопок; прямое присваивание `Iteraction` |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | subscribe → commit-and-close → cancel |

Совместимый controller `hunttech_IteractionList.edit.accordion` остаётся тонким alias к основному экрану и использует тот же descriptor.

## 6. Локальный SCSS

Все правила ограничены корнем `.iteraction-list-editor`. Финальный `iteraction-list-reference-finish.scss` подключён последним во всех семи темах и фиксирует TabSheet CandidateCVEdit, GroupBox SettingsWindow, зелёные кнопки и овальный логотип.

Запрещённые глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet`, `.v-panel` не изменяются. Vaadin-селекторы применяются только как потомки локального root-класса. Структура и размеры во всех темах одинаковы.

## 7. Ограничения изменений

- Java-контроллер и существующая бизнес-логика не изменены;
- entity, поля, БД, Liquibase и definitions views не изменены;
- loader ID, conditions, JPQL, bindings, component ID, actions и `invoke` сохранены;
- validation, required, editable, visible и enable-состояния не изменены;
- навигация не изменяет данные и lifecycle;
- production не изменяется;
- merge допускается только после отчёта Hermes по точному HEAD SHA и прямой команды Алексея.

## 8. Обязательная проверка Hermes

1. HEAD branch и HEAD PR совпадают с SHA, указанным в PR.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. `IteractionListEditAccordionLayoutTest` — PASS.
5. `IteractionListAccordionNavigationTest` — PASS.
6. `IteractionListSidebarContextPanelTest` — PASS.
7. `IteractionListAccordionCssContractTest` — PASS.
8. `IteractionListMostPopularInteractionTest` — PASS.
9. `LeftSidebarAvatarComponentTest` — PASS.
10. Compile web и core tests — PASS.
11. `ScreenViewIntegrityTest` — `8/8 PASS`.
12. Data View Integrity — PASS.
13. `:app-web:buildScssThemes` — PASS для семи тем.
14. `clean assemble` — `BUILD SUCCESSFUL`.
15. Local deploy и HTTP `/hrm/` = `200`.
16. Functional smoke: candidate/vacancy, lookup/open, тип, dynamic fields, rating, recruiter, communication, comment, subscription, save/cancel.
17. Navigation smoke: каждый пункт sidebar раскрывает один блок, выделяется и фокусирует первое поле; заголовки GroupBox синхронизируют индекс.
18. Popular smoke: ровно пять равных кнопок, top-5 текущего пользователя за год, прямое присваивание типа, disabled placeholders.
19. Visual smoke семи тем: геометрия sidebar, карточки, индекс, toolbar, вкладка 48 px, полноширинные аккордеоны, отсутствие горизонтальной прокрутки и наложений.
20. Tomcat logs: новых critical errors NONE; P1 = 0; P2 = 0.

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Восстановлен видимый аккордеон «Кандидат и вакансия», добавлен блок пяти зелёных кнопок, номер/дата подняты под изображения, логотип проекта переведён на OvaFallbackImage |
| 2026-07-26 | Удалены три дублирующие подписи sidebar; индекс пяти разделов перемещён непосредственно под изображения кандидата и проекта |
| 2026-07-26 | Компоновка синхронизирована с `CandidateCVEdit` и `ExtSettingsWindow`: профильная sidebar 296/276/260 px, раздельные контекстные карточки, индекс пяти разделов слева и полноширинные аккордеоны справа |
| 2026-07-25 | Контекстная панель расширена до 272/252 px, captions полей отделены от заголовка карточки, длинные значения и стоимость получили читаемую компоновку без наложений |
| 2026-07-25 | Исправлена совместимость с CUBA 7.3: `HBoxLayout.expand` вызывается отдельно для каждой из пяти быстрых кнопок вместо передачи `Component[]` |
| 2026-07-25 | Добавлены двухколоночная строка кандидата и вакансии, кликабельный индекс пяти блоков и пять равных кнопок частых взаимодействий текущего пользователя за скользящий год |
| 2026-07-25 | Исправлен сценарий «Копировать»: вакансия перечитывается через `openPosition-iteraction-list-picker-view` до открытия нового `IteractionListEdit` |
| 2026-07-25 | Аккордеоны приведены к presentation-контракту `SettingsWindow`; первая секция раскрыта, остальные свёрнуты |
| 2026-07-25 | `candidateImage` заменён на `OvaFallbackImage` с fallback `icons/no-programmer.jpeg` |
| 2026-07-25 | Выполнена двухпанельная визуальная адаптация с локальным namespace `.iteraction-list-editor` |
