# IteractionListEdit — контракт смысловых ID Box-компонентов

> Проект: **HRM HuntTech**  
> Screen ID: `hunttech_IteractionList.edit`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Дата: `2026-07-28`

## 1. Назначение и бизнес-смысл (What & Why)

XML-дескриптор `IteractionListEdit` содержит вложенную двухпанельную компоновку, sidebar-карточки и четыре вертикальных блока ввода. Без осмысленных `id` анонимные `VBoxLayout` и `HBoxLayout` трудно отличить при сопровождении, тестировании и анализе runtime DOM. Это повышает риск применить стиль, focus или presentation-логику не к тому контейнеру.

Контракт требует, чтобы каждый элемент типа Box имел уникальный `id`, отражающий его роль в бизнес-сценарии экрана. Изменение не затрагивает данные, loaders, actions или lifecycle.

## 2. UI Context & Navigation

Box-компоненты организованы в три уровня:

1. `iteractionListSidebar` — профиль кандидата, статус и приоритет вакансии, label-навигация, служебная и vacancy-card;
2. `iteractionListWorkspace` — toolbar, быстрые взаимодействия, scroll-area и footer;
3. `iteractionListSectionsBox` — четыре постоянных блока: участники, тип действия, оценка и комментарий.

Label-навигация продолжает работать через существующие controller-compatible ID `participantsAccordion`, `interactionAccordion`, `resultAccordion`, `commentAccordion`. Эти legacy ID не переименовывались.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть XML | требуется понять контейнер | `id` прямо указывает его назначение |
| добавить новый Box | создаётся `vbox`, `hbox`, `scrollBox` или `buttonsPanel` | разработчик обязан назначить уникальный semantic ID |
| изменить layout | переносится существующий Box | ID переносится вместе с назначением или переименовывается синхронно с tests/docs |
| выполнить runtime navigation | пользователь выбирает раздел | controller-compatible ID блоков остаются неизменными |
| запустить contract test | Box без ID, дубликат или generic ID | тест завершается ошибкой |

## 4. Полный контракт ID

### 4.1. Корневая компоновка

| ID | Тип | Назначение |
|---|---|---|
| `iteractionListMainLayout` | `hbox` | делит экран на sidebar и workspace |
| `iteractionListSidebar` | `vbox` | постоянный контекст кандидата и вакансии |
| `iteractionListWorkspace` | `vbox` | правая рабочая область экрана |
| `iteractionListToolbarBox` | `hbox` | верхний заголовок и описание формы |
| `iteractionListToolbarTitleBox` | `vbox` | объединяет title и context text |
| `iteractionListContentScrollBox` | `scrollBox` | прокручивает только блоки ввода |
| `iteractionListSectionsBox` | `vbox` | вертикально объединяет четыре раздела |

### 4.2. Sidebar

| ID | Тип | Назначение |
|---|---|---|
| `iteractionProfileSummaryBox` | `vbox` | изображения, ФИО, вакансия, status/priority |
| `iteractionIdentityImages` | `hbox` | фото кандидата и логотип проекта |
| `vacancyStateSummary` | `hbox` | горизонтальная пара status/priority |
| `vacancyStatusSummary` | `vbox` | статус вакансии |
| `vacancyStatusValueBox` | `hbox` | status value и warning action |
| `vacancyPrioritySummary` | `vbox` | приоритет вакансии |
| `vacancyPriorityValueBox` | `hbox` | traffic light и priority text |
| `iteractionListNavigation` | `vbox` | label-навигация разделов |
| `iteractionServiceCard` | `vbox` | номер и дата взаимодействия |
| `iteractionServiceFields` | `vbox` | read-only service fields |
| `iteractionVacancyCard` | `vbox` | подробный vacancy context |
| `sidebarVacancyNameRow` | `vbox` | наименование вакансии |
| `vacancyCompanyDepartmentBox` | `vbox` | компания и подразделение |
| `vacancyProjectBox` | `vbox` | проект вакансии |
| `outstaffingCostHBox` | `hbox` | условный блок ставки, ID сохранён для controller |
| `outstaffingCostContentBox` | `vbox` | caption и строка ставки |
| `outstaffingCostValueBox` | `hbox` | сумма и единица измерения |
| `vacancyRatingContextBox` | `hbox` | текст и изображение рейтинга |
| `iteractionListSidebarSpacer` | `vbox` | занимает свободную высоту sidebar |

Устаревший общий ID `labelHBox` заменён на `vacancyRatingContextBox`, поскольку он не использовался production controller и не отражал назначение.

### 4.3. Быстрые действия и рабочие разделы

| ID | Тип | Назначение |
|---|---|---|
| `mostPopularQuickActions` | `vbox` | карточка частых взаимодействий |
| `mostPopularIteractionHBox` | `hbox` | host runtime-кнопок |
| `mostPopularHbox` | `hbox` | пять кнопок/placeholder; legacy ID controller |
| `participantsAccordion` | `vbox` | раздел кандидата и вакансии |
| `participantsSectionHeaderBox` | `vbox` | заголовок раздела участников |
| `participantsAccordionContent` | `vbox` | picker-поля и subscription filter |
| `interactionAccordion` | `vbox` | раздел типа и действия |
| `interactionSectionHeaderBox` | `vbox` | заголовок раздела действия |
| `interactionSectionBodyBox` | `vbox` | тип и dynamic fields |
| `buttonsPanelCallAction` | `buttonsPanel` | controller-управляемая dynamic area |
| `dynamicActionFieldsBox` | `vbox` | action button и три дополнительных поля |
| `resultAccordion` | `vbox` | оценка и коммуникация |
| `resultSectionHeaderBox` | `vbox` | заголовок блока результата |
| `resultAccordionBody` | `vbox` | grid результата |
| `commentAccordion` | `vbox` | комментарий |
| `commentSectionHeaderBox` | `vbox` | заголовок комментария |
| `commentSectionBodyBox` | `vbox` | многострочное поле комментария |
| `editActions` | `hbox` | footer действий |
| `editActionsSpacer` | `vbox` | выравнивает actions вправо |
| `editActionsGroup` | `hbox` | subscribe/save/cancel |

## 5. Правила именования

1. ID пишется в `lowerCamelCase`.
2. Имя начинается с предметного назначения, а не с типа компонента.
3. Суффикс `Box` допустим для нового layout-контейнера; существующие controller-compatible legacy ID сохраняются.
4. Запрещены `box1`, `vbox`, `hbox`, `layout`, `container`, `panel` без предметного контекста.
5. ID уникален в пределах descriptor.
6. Переименование injected ID требует синхронного изменения controller, tests и документации и рассматривается как отдельный presentation-contract change.

## 6. Сохранённые CUBA-контракты

Не изменены:

- entity, БД и Liquibase;
- containers, loaders, JPQL и views;
- bindings `dataContainer` / `property` / `optionsContainer`;
- actions и `invoke`;
- required/visible/editable/enabled;
- width/height/expand/align/spacing/margin;
- stylename и SCSS;
- controller lifecycle и бизнес-side effects.

## 7. Проверки

Тест `IteractionListBoxIdContractTest` проверяет:

- наличие ID у каждого `vbox`, `hbox`, `scrollBox`, `buttonsPanel`;
- уникальность ID;
- отсутствие generic ID;
- наличие всех новых semantic ID;
- удаление `labelHBox`.

Hermes дополнительно выполняет XML parse, профильные тесты, `ScreenViewIntegrityTest 8/8`, `clean assemble`, local deploy, HTTP 200 и browser smoke.

## 8. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-28 | Всем Box-компонентам `iteraction-list-edit.xml` назначены уникальные смысловые ID; добавлен автоматический contract test. |
