# IteractionListEdit — спецификация экранной формы

> Screen ID: `hunttech_IteractionList.edit`  
> Presentation-controller: `IteractionListEditAccordionNavigation` → `IteractionListEdit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Общий стандарт: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` регистрирует взаимодействие рекрутёра с кандидатом по конкретной вакансии. Форма объединяет выбор кандидата, вакансии и типа взаимодействия, зависимые дополнительные значения, оценку, способ связи, рекрутёра и комментарий.

Экран участвует в истории кандидата, подписках, уведомлениях, статусах рекрутингового процесса и последующих действиях. Поэтому визуальная унификация выполняется только в presentation-слое и не изменяет entity, bindings, loaders, JPQL, views, validation, actions, `invoke` и save lifecycle CUBA Platform 7.3.

Пять частых взаимодействий являются персональным ускорителем ввода. Пользователь всегда видит пять равных позиций: фактически найденные типы активны, отсутствующие позиции отображаются как disabled `Нет данных`.

## UI Context & Navigation

- Экран открывается из browse взаимодействий, карточки кандидата и связанных сценариев создания, копирования и редактирования.
- Постоянная sidebar показывает фотографию кандидата, логотип проекта, ФИО, вакансию, label-навигацию, номер и дату взаимодействия, компанию, проект, статус, приоритет, стоимость и rating context.
- Обязательный порядок sidebar: визуальный образ → наименование → `label-navigation` → детализация → spacer.
- Правая рабочая область содержит toolbar, постоянно видимую карточку быстрых действий, штатный TabSheet, прокручиваемый поток аккордеонов и footer-actions.
- Label-навигация раскрывает существующий `GroupBoxLayout`, обновляет только `label-nav-item-active` и переводит keyboard focus в первое рабочее поле.
- Кнопка «Частые взаимодействия» фокусирует первую доступную быструю кнопку; entity и loaders при навигации не изменяются.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`, подписка — существующий `onButtonSubscribeClick`.

## Behavior Summary

- открытие формы → базовый контроллер загружает данные и выполняет прежний lifecycle → presentation-extension добавляет общие semantic stylename;
- открытие нового взаимодействия → заполняются номер, дата и текущий рекрутёр → пользователь получает готовый экземпляр `IteractionList`;
- открытие экрана → создаются пять доступных с клавиатуры пунктов label-навигации → активен раздел «Кандидат и вакансия»;
- клик по пункту navigation → раскрывается соответствующий аккордеон и меняется только active-state → данные сущности не изменяются;
- сервис возвращает 0–4 популярных типа → недостающие позиции дополняются disabled `Нет данных` → пользователь всегда видит пять мест;
- сервис возвращает 5+ типов → отображаются первые пять в порядке сервиса → логика ранжирования не меняется;
- клик по активной быстрой кнопке → точный `Iteraction` устанавливается в `iteractionTypeField` → штатный value-change handler управляет required, дополнительными полями и action-кнопкой;
- выбор кандидата или вакансии → прежние handlers обновляют контекст sidebar → component ID и bindings сохранены;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → бизнес-логика не меняется.

## 1. Неизменяемый бизнес-контракт быстрых взаимодействий

1. Источник — `InteractionService.getMostPolularIteraction(User, int)`.
2. Пользователь — текущий рекрутёр из `UserSession`.
3. Период — последний календарный месяц.
4. Ранжирование — `group by iteractionType`, `count DESC`.
5. Лимит фактических результатов — до пяти.
6. Активная кнопка передаёт точный объект `Iteraction`; разбор caption запрещён.
7. Disabled-placeholder не имеет click listener и не изменяет сущность.
8. Локальная JPQL-агрегация в UI-контроллере запрещена.

Контракт защищён `IteractionListMostPopularInteractionTest`.

## 2. Общий контракт Edit-экранов

### 2.1. Локальный namespace и semantic stylename

Локальный root `.iteraction-list-editor` сохраняется. `IteractionListEditAccordionNavigation` рекурсивно добавляет общие классы поверх существующих локальных классов:

| Локальная роль | Общий stylename |
|---|---|
| `iteraction-list-main-layout` | `edit-screen-layout` |
| `iteraction-list-sidebar` | `edit-sidebar` |
| `iteraction-list-identity-images` | `edit-sidebar-visual` |
| `iteraction-list-profile-header` | `edit-sidebar-identity` |
| `iteraction-list-profile-title` | `edit-sidebar-title` |
| `iteraction-list-profile-subtitle` | `edit-sidebar-subtitle` |
| `iteraction-list-sidebar-card` | `edit-sidebar-summary` |
| `iteraction-list-sidebar-warning` | `edit-sidebar-warning` |
| `iteraction-list-sidebar-spacer` | `edit-sidebar-spacer` |
| `iteraction-list-workspace` | `edit-workspace` |
| `iteraction-list-toolbar` | `edit-toolbar` |
| `iteraction-list-toolbar-title` | `edit-toolbar-title` |
| `iteraction-list-toolbar-context` | `edit-toolbar-description` |
| `iteraction-list-quick-actions` | `edit-card` |
| `iteraction-list-quick-actions-title` | `edit-card-title` |
| `iteraction-list-tabs` | `edit-tabs` |
| `iteraction-list-scroll` | `edit-workspace-scroll` |
| `iteraction-list-content` | `edit-workspace-content` |
| `iteraction-list-accordion-section` | `edit-accordion-section` |
| `iteraction-list-footer` | `edit-footer-actions` |

### 2.2. Label-навигация

Контейнер использует только `label-navigation`, заголовок — `label-nav-title`, каждый runtime-пункт — `borderless label-nav-item`. Активность задаётся дополнительным `label-nav-item-active`; базовый класс не заменяется.

Runtime-кнопки создаются presentation-extension после базовой инициализации. Они используют прежние message keys, существующие GroupBox и focus targets. Навигационные методы не содержат `DataManager`, `InteractionService`, loaders, commit или `setValue()`.

### 2.3. Shared SCSS

Единый источник: `modules/web/themes/common/edit-screen-shared-styles.scss`.

Все семь `styles.scss` импортируют shared partial и последним вызывают `edit-screen-shared-styles`. Независимые копии общих правил в локальных файлах не создаются.

Геометрический контракт:

- sidebar — `270px`, при viewport до `1366px` — `250px`;
- toolbar и footer — минимум `58px`;
- tabs — `48px`;
- однострочные поля и кнопки — минимум `38px`;
- card/accordion radius — `8px`;
- workspace и внутренние контейнеры — `min-width: 0`;
- горизонтальная прокрутка формы запрещена;
- Vaadin-селекторы ограничены semantic-классами.

## 3. Специфика формы

### 3.1. Визуальная иерархия sidebar

Фотография кандидата остаётся главным образом `112 × 112`. Логотип проекта сохраняет отдельный component ID и fallback, но presentation-extension возвращает ему стиль `iteraction-list-project-image` и размер `80 × 80`, чтобы он не конкурировал с кандидатом.

### 3.2. Постоянная карточка быстрых действий

`mostPopularQuickActions` расположена между toolbar и TabSheet. Такое положение соответствует специфике формы: быстрые действия видимы независимо от выбранной вкладки и состояния аккордеонов. Контейнер получает общий класс `edit-card`, заголовок — `edit-card-title`.

### 3.3. Рабочие аккордеоны

1. `participantsAccordion` — кандидат, вакансия, фильтр подписок; раскрыт по умолчанию.
2. `interactionAccordion` — тип и динамические значения/action.
3. `resultAccordion` — рейтинг, рекрутёр, способ связи.
4. `commentAccordion` — комментарий.
5. `popularAccordion` — скрытый compatibility-компонент для legacy presentation-контракта.

Каждый рабочий GroupBox получает `edit-accordion-section`, остаётся полноширинным и использует естественную высоту.

## 4. Data View Integrity

| Источник | View | Назначение |
|---|---|---|
| `iteractionListDc` | `iteractionList-edit-view` | редактируемая запись и candidate/vacancy-контекст |
| `candidateField` | `jobCandidate-iteraction-list-suggestion-view` | узкий suggestion-граф кандидата |
| `openPositionDc` | `openPosition-iteraction-list-picker-view` | вакансия, проект, подразделение, логотип |
| `iteractionTypesDc` | `iteraction-list-type-view` | тип взаимодействия и динамические настройки |
| `usersDc` | `_minimal` | активные пользователи |

Контроллер не должен читать unfetched-атрибуты detached-сущностей. Тяжёлые коллекции кандидата в suggestion-view не включаются.

## 5. Сохранённые component-контракты

| Компоненты | Контракт |
|---|---|
| `candidateField`, `vacancyFiels` | bindings, suggestion/lookup/open, optionsContainer |
| `onlyMySubscribeCheckBox` | ID, caption, description, loader filtering |
| `iteractionTypeField` | binding, lookup, value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | binding и runtime visible/required/caption |
| `ratingField`, `recrutierField`, `communicationMethodField` | binding и прежние providers |
| `commentField` | binding, lazy reload, runtime required |
| `candidateImage`, `projectLogoImage` | legacy ID, source/fallback и Java-инъекция |
| `mostPopularHbox` | пять равных позиций и точный `Iteraction` |
| footer | subscribe → commit-and-close → cancel |

## 6. Обязательная проверка Hermes

1. Branch HEAD = PR HEAD = переданный SHA; base = `master`; conflicts = NONE.
2. `git diff --check`.
3. Compile web и core tests.
4. `IteractionListEditAccordionLayoutTest`, `IteractionListAccordionNavigationTest`, `IteractionListSidebarContextPanelTest`, `IteractionListMostPopularInteractionTest`, `IteractionListRpcCompatibilityContractTest` — PASS.
5. `ScreenViewIntegrityTest` — `8/8 PASS`.
6. `:app-web:buildScssThemes` — PASS для семи тем.
7. `clean assemble` — `BUILD SUCCESSFUL`.
8. Clean local deploy, HTTP `/hrm/` = `200`.
9. DOM smoke: общие semantic classes присутствуют, одновременно активен один navigation-item.
10. Visual smoke Halo + остальные темы: sidebar 270/250, toolbar/footer 58, tabs 48, fields 38, горизонтального scroll нет.
11. Functional smoke: candidate/vacancy, пять быстрых кнопок, dynamic fields, rating, comment, subscription, save/cancel без регрессии.
12. Tomcat logs: новых critical errors нет; P1=0; P2=0.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Форма приведена к `HRM_HuntTech_Edit_Screen_Shared_Style_Contract`: общие semantic stylename, shared SCSS семи тем, корректный active-state и нормативная геометрия без изменения бизнес-логики |
| 2026-07-27 | Восстановлены пять постоянно видимых быстрых позиций; пустые позиции disabled |
| 2026-07-27 | Закреплён исторический месячный контракт `InteractionService` |
| 2026-07-27 | Аккордеоны, sidebar и fallback изображений приведены к актуальной UI/UX-концепции |
| 2026-07-25 | Выполнен первоначальный двухпанельный UI/UX-редизайн |
