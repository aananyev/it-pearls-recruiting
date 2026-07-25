# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует взаимодействие рекрутёра с кандидатом по вакансии: участника процесса, вакансию, тип взаимодействия, рейтинг, способ коммуникации, рекрутёра, дополнительное значение и комментарий. Экран участвует в формировании истории кандидата, статусов процесса, подписок, уведомлений и связанных действий, поэтому визуальные изменения не должны изменять lifecycle, data-контракты и бизнес-логику.

Компоновка от 2026-07-25 разделяет рабочее содержимое на пять сворачиваемых секций. Поля «Кандидат» и «Вакансия» размещены в одной адаптивной строке, используют общий локальный CSS-контракт и не могут расширять рабочую область за пределы диалога.

Оформление аккордеонов повторяет подтверждённую геометрию `SettingsWindow`. Фактический XML-класс `user-ai-profile-section` дополнительно оформляется локальным селектором внутри корня `.iteraction-list-editor`, поэтому внешний вид контролируется `IteractionListEdit` и не распространяется на другие экраны. Одинаковый CSS-контракт синхронизирован во всех семи темах.

Профильное изображение кандидата в левой панели сохраняет единый контракт HRM HuntTech: `candidateImage` отображается через legacy-компонент `OvaFallbackImage`, имеет стабильную круглую геометрию и показывает `icons/no-programmer.jpeg`, если фотография кандидата отсутствует. Java-контроллер и модель данных не изменены.

Сценарий «Копировать» создаёт новую сущность `IteractionList` из строки browse-экрана. Browse-контейнер намеренно использует компактный view, поэтому перед передачей вакансии в новый editor она перечитывается через `openPosition-iteraction-list-picker-view`. Это гарантирует загрузку `projectName.projectDepartment.companyName` и предотвращает `Cannot get unfetched attribute` при построении контекста компании и проекта.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания или редактирования взаимодействия.
- Команда «Копировать» в `IteractionListBrowse` создаёт новый `IteractionList`, сохраняет кандидата и передаёт перечитанную вакансию с полным editor-графом проекта.
- Picker кандидата сохраняет lookup и open для `JobCandidate`.
- Picker вакансии сохраняет lookup и open для `OpenPosition`.
- Выбор типа взаимодействия управляет существующими динамическими компонентами дополнительного действия.
- Левый индекс расположен в порядке: «Кандидат и вакансия» → «Тип и действие» → «Результат» → «Комментарий» → «Частые взаимодействия».
- Секция «Кандидат и вакансия» раскрыта при открытии; остальные четыре секции стартуют свёрнутыми. Клик по пункту слева раскрывает один блок и переводит фокус в его первое поле.
- Кнопка подписки открывает существующий editor подписки.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.
- Экран остаётся модальным диалогом `1100 × 650`; sidebar ограничен шириной `228 px`.

## Behavior Summary

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовую форму;
- нажатие «Копировать» в browse → выбранная строка содержит сокращённый detached-граф вакансии → `IteractionListBrowse` перечитывает вакансию через `openPosition-iteraction-list-picker-view` и только затем передаёт её в новый editor;
- открытие формы → секция «Взаимодействие» раскрыта, «Комментарий» и «Популярные взаимодействия» свёрнуты → пользователь сразу видит основные поля;
- раскрытие или сворачивание секции → меняется только presentation state `GroupBoxLayout` → значения и lifecycle не затрагиваются;
- выбор кандидата с фотографией → сохраняется прежний `ContainerValueSource` → `OvaFallbackImage` отображает фотографию круглой;
- выбор кандидата без фотографии → существующая Java-логика и `fallbackThemePath` указывают на `icons/no-programmer.jpeg` → sidebar не содержит пустого изображения;
- выбор вакансии → сохраняются проверки закрытия, подписки, статуса, приоритета и логотипа → sidebar обновляет вакансию;
- выбор типа взаимодействия → Java переключает `buttonCallAction`, `addString`, `addDate` или `addInteger` → дополнительное значение отображается следующей строкой под типом;
- изменение rating → Java сохраняет прежнее оформление и правила → оценка отображается в форме и sidebar;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → данные и связанные процессы изменяются как до reflow;
- смена темы → локальный mixin `iteraction-list-accordion-navigation-theme` применяет ту же геометрию, что и `SettingsWindow` → функциональные контракты не меняются.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| `@UiController` | `hunttech_IteractionList.edit` |
| Java | `com.company.hunttech.web.screens.iteractionlist.IteractionListEdit` |
| Источник копирования | `com.company.hunttech.web.screens.iteractionlist.IteractionListBrowse#onButtonCopyClick` |
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
| copy initializer `IteractionListBrowse` | `openPosition-iteraction-list-picker-view` | перечитывание вакансии перед передачей в новую сущность | добавлен безопасный reload |
| `usersDc` / `usersDl` | `_minimal` | активные пользователи | без изменений |

`openPosition-iteraction-list-picker-view` содержит `projectName.projectDepartment.departamentRuName` и `projectName.projectDepartment.companyName.companyShortName`, которые читает `IteractionListEdit#vacancyFieldValueChange`. JPQL, query conditions, параметры loaders, `cacheable`, entity, БД, Liquibase и `views.xml` не изменялись.


## 3. Компоновка

Sidebar ограничен шириной `228 px`; workspace, GridLayout и picker-поля используют `min-width: 0` и `max-width: 100%`. Поля `candidateField` и `vacancyFiels` размещены в двух колонках одной строки и используют общий класс `iteraction-list-primary-picker`.

В sidebar расположен кликабельный индекс пяти блоков: кандидат и вакансия; тип и действие; результат; комментарий; частые взаимодействия. В последнем блоке всегда отображаются пять равных кнопок по 20% доступной ширины.


## 4. Канонический контракт навигации SettingsWindow

XML `Label` служат fallback-разметкой. На `InitEvent` они заменяются borderless-кнопками: клик раскрывает ровно один `GroupBoxLayout`, синхронизирует active-style и переводит фокус в первое поле. Клики по штатным заголовкам аккордеонов синхронизируют левый индекс.

## 5. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `gridIterationData` | legacy ID и тип `GridLayout`; две адаптивные колонки для кандидата и вакансии |
| `candidateField`, `vacancyFiels` | bindings, lookup/open actions и query; единый класс `iteraction-list-primary-picker` |
| `iteractionTypeField` | binding, lookup и Java value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime visible/required/caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer и option icon provider |
| `communicationMethodField` | binding и caption |
| `commentField` | binding, lazy reload, runtime required и автодополнение |
| `candidateImage` | legacy ID, `iteractionListDc`, `candidate.fileImageFace`, Java-инъекция `Image`, runtime `setValueSource` / `setSource`; XML-тип — `OvaFallbackImage` |
| `projectLogoImage` | отдельный обычный `Image`, прежний source и Java-инъекция |
| `mostPopularHbox`, `mostPopularIteractionHBox` | пять равных быстрых кнопок; выбор устанавливает точный `Iteraction` без парсинга caption |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | порядок subscribe → commit-and-close → cancel |

Component ID, bindings, actions, `invoke`, validators и runtime-управляемые состояния не изменены. `OvaFallbackImage` наследует базовый CUBA `Image`, поэтому существующее поле `private Image candidateImage` в контроллере остаётся совместимым.


## 6. Частые взаимодействия за скользящий год

Один агрегирующий JPQL-запрос выбирает типы текущего пользователя за период от текущей даты минус один календарный год до текущего момента, группирует по `iteractionType` и сортирует по количеству по убыванию. Первые пять типов отображаются как равные CUBA `Button`; клик напрямую устанавливает объект в `iteractionTypeField`. Недостающие позиции — disabled-кнопки «Нет данных».

## 7. Локальный SCSS и responsive-контракт

Все правила ограничены корнем `.iteraction-list-editor`. Локальные селекторы ограничивают ширину picker, запрещают horizontal overflow и фиксируют пять slots по 20% во всех семи темах.


## 8. Ограничения изменений

- entity, БД, Liquibase и `views.xml` не изменены;
- loaders, их JPQL, conditions и views не изменены;
- component ID, bindings, actions и `invoke` сохранены;
- новый JPQL используется только для агрегирования статистики текущего пользователя за год;
- navigation не записывает entity и не выполняет commit;
- merge и production запрещены до отдельной команды.


## 9. Обязательная проверка Hermes

Проверить точный HEAD/base/conflicts; `git diff --check`; профильные тесты `5/5`, `4/4`, `4/4`, avatar `2/2`; compile; `ScreenViewIntegrityTest 8/8`; Data View Integrity; SCSS семи тем; `clean assemble`; local deploy; HTTP `/hrm/` 200. Smoke обязан подтвердить одинаковый CSS кандидата и вакансии, отсутствие horizontal scroll, пять фокусирующих пунктов слева и пять равных годовых кнопок с установкой точного типа.

До отчёта Hermes статус: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Добавлены responsive-компоновка кандидата и вакансии, кликабельный индекс пяти блоков и пять равных кнопок частых взаимодействий текущего пользователя за скользящий год |
<<<<<<< HEAD
| 2026-07-25 | Исправлен сценарий «Копировать»: вакансия перечитывается через `openPosition-iteraction-list-picker-view` до открытия нового `IteractionListEdit`, что гарантирует загрузку `projectDepartment` и предотвращает unfetched-ошибку |
| 2026-07-25 | Аккордеоны `IteractionListEdit` приведены к точному presentation-контракту `SettingsWindow`: `showAsPanel`, `margin`, первая секция раскрыта, остальные свёрнуты, переиспользованы эталонные theme-aware стили `user-ai-profile-section`; конкурирующий accordion-класс исключён |
=======
| 2026-07-25 | В семи темах добавлено локальное CSS-оформление `.iteraction-list-editor .user-ai-profile-section`, визуально соответствующее `SettingsWindow`; XML и Java не изменены, добавлен `IteractionListAccordionCssContractTest` |
>>>>>>> origin/agent/iteraction-list-accordion-css
| 2026-07-25 | Основные рабочие блоки преобразованы в сворачиваемые секции; `gridIterationData` переведён на одну колонку, все поля расположены друг под другом без изменения business/data-контрактов |
| 2026-07-25 | По итогам аудита переработанных форм `candidateImage` в левой панели заменён на `OvaFallbackImage` 104×104 px с fallback `icons/no-programmer.jpeg`; ID, binding и Java-инъекция `Image` сохранены |
| 2026-07-25 | Улучшена компоновка: sidebar сделан непрерывным по высоте, toolbar и footer перенесены в workspace, ширина sidebar уменьшена, поля выстроены по сценарию рекрутёра, геометрия синхронизирована в семи темах |
| 2026-07-25 | Выполнена строго визуальная адаптация `IteractionListEdit`: двухпанельная композиция, локальный namespace `.iteraction-list-editor`, карточки и theme-aware состояния семи тем |
