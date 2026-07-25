# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует взаимодействие рекрутёра с кандидатом по вакансии: участника процесса, вакансию, тип взаимодействия, рейтинг, способ коммуникации, рекрутёра, дополнительное значение и комментарий. Экран участвует в формировании истории кандидата, статусов процесса, подписок, уведомлений и связанных действий, поэтому визуальные изменения не должны изменять lifecycle, data-контракты и бизнес-логику.

Компоновка от 2026-07-25 разделяет рабочее содержимое на сворачиваемые секции и переводит основные поля в одну вертикальную колонку. Такая структура уменьшает визуальную перегрузку, исключает конкуренцию подписей и picker-компонентов за ширину и сохраняет последовательность работы рекрутёра сверху вниз.

Оформление аккордеонов повторяет подтверждённую геометрию `SettingsWindow`. Фактический XML-класс `user-ai-profile-section` дополнительно оформляется локальным селектором внутри корня `.iteraction-list-editor`, поэтому внешний вид контролируется `IteractionListEdit` и не распространяется на другие экраны. Одинаковый CSS-контракт синхронизирован во всех семи темах.

Профильное изображение кандидата в левой панели сохраняет единый контракт HRM HuntTech: `candidateImage` отображается через legacy-компонент `OvaFallbackImage`, имеет стабильную круглую геометрию и показывает `icons/no-programmer.jpeg`, если фотография кандидата отсутствует. Java-контроллер и модель данных не изменены.

Сценарий «Копировать» создаёт новую сущность `IteractionList` из строки browse-экрана. Browse-контейнер намеренно использует компактный view, поэтому перед передачей вакансии в новый editor она перечитывается через `openPosition-iteraction-list-picker-view`. Это гарантирует загрузку `projectName.projectDepartment.companyName` и предотвращает `Cannot get unfetched attribute` при построении контекста компании и проекта.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания или редактирования взаимодействия.
- Команда «Копировать» в `IteractionListBrowse` создаёт новый `IteractionList`, сохраняет кандидата и передаёт перечитанную вакансию с полным editor-графом проекта.
- Picker кандидата сохраняет lookup и open для `JobCandidate`.
- Picker вакансии сохраняет lookup и open для `OpenPosition`.
- Выбор типа взаимодействия управляет существующими динамическими компонентами дополнительного действия.
- Основные секции расположены в рабочем порядке: «Взаимодействие» → «Комментарий» → «Популярные взаимодействия».
- Секция «Взаимодействие» раскрыта при открытии формы; остальные секции стартуют свёрнутыми, как в `SettingsWindow`.
- Кнопка подписки открывает существующий editor подписки.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.
- Экран остаётся модальным диалогом `1000 × 650`.

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
| Диалог | `width=1000`, `height=650`, `modal=true` |
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

```text
main layout 100% × 100%
├─ sidebar 252 px, full height
│  └─ context card
│     ├─ candidateImage: OvaFallbackImage 104 px + projectLogoImage: Image 76 px
│     ├─ numberIteractionField
│     ├─ dateIteractionField
│     ├─ closingDateVacancyLabel
│     ├─ companyLabel / projectLabel
│     ├─ vacancy status / priority / outstaffing
│     └─ rating context
└─ workspace, expanded
   ├─ toolbar 52 px
   ├─ TabSheet 42 px + scrollable content
   │  ├─ accordion: interaction data [expanded]
   │  │  ├─ candidate
   │  │  ├─ vacancy
   │  │  ├─ subscription filter
   │  │  ├─ interaction type
   │  │  ├─ dynamic action/value
   │  │  ├─ rating
   │  │  ├─ recruiter
   │  │  └─ communication method
   │  ├─ accordion: comment [collapsed]
   │  └─ accordion: popular interactions [collapsed]
   └─ footer 54 px
```

### Причины решений

- Сворачиваемые `GroupBox` позволяют временно скрыть неиспользуемый блок без удаления данных и компонентов.
- `gridIterationData` сохраняет тип `GridLayout` и legacy ID, но использует одну колонку.
- Все picker, lookup, checkbox и динамические компоненты располагаются последовательно друг под другом и занимают доступную ширину.
- Порядок соответствует сценарию рекрутёра: кандидат → вакансия → тип → дополнительное действие → рейтинг → рекрутёр → способ связи → комментарий.
- Sidebar остаётся непрерывным по высоте и не пересекается toolbar/footer.
- Фото кандидата сохраняет `OvaFallbackImage`; логотип проекта остаётся отдельным обычным `Image`.
- Комментарий остаётся отдельным блоком высотой `160 px`, чтобы его можно было быстро свернуть.

## 4. Канонический контракт аккордеона SettingsWindow

Каждая рабочая секция сохраняет штатный CUBA-контракт `GroupBoxLayout`:

```xml
<groupBox width="100%"
          spacing="true"
          margin="true"
          collapsable="true"
          collapsed="false|true"
          showAsPanel="true"
          stylename="user-ai-profile-section [iteraction-list-form-card|iteraction-list-comment-card]">
```

Состояния при открытии:

| Секция | Начальное состояние | Причина |
|---|---|---|
| `msgIteractionList` | раскрыта | основной рабочий сценарий |
| `msgComment` | свёрнута | дополнительная информация |
| `mshMostPopular` | свёрнута | вспомогательные быстрые действия |

Локальный CSS повторяет подтверждённые параметры `SettingsWindow`: радиус `7 px`, граница `1 px`, лёгкая поверхность, отступ между секциями `10 px`, вертикальный padding заголовка `9 px` и насыщенность `600`. Селекторы `.user-ai-profile-section` и `.v-panel-caption` применяются только внутри `.iteraction-list-editor`, поэтому изменение не влияет на `SettingsWindow` и другие экраны.

## 5. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `gridIterationData` | legacy ID и тип `GridLayout`; изменено только число колонок с 2 на 1 |
| `candidateField`, `vacancyFiels` | bindings, lookup/open actions и query |
| `iteractionTypeField` | binding, lookup и Java value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime visible/required/caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer и option icon provider |
| `communicationMethodField` | binding и caption |
| `commentField` | binding, lazy reload, runtime required и автодополнение |
| `candidateImage` | legacy ID, `iteractionListDc`, `candidate.fileImageFace`, Java-инъекция `Image`, runtime `setValueSource` / `setSource`; XML-тип — `OvaFallbackImage` |
| `projectLogoImage` | отдельный обычный `Image`, прежний source и Java-инъекция |
| `mostPopularHbox`, `mostPopularIteractionHBox` | отдельные XML-контейнеры |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | порядок subscribe → commit-and-close → cancel |

Component ID, bindings, actions, `invoke`, validators и runtime-управляемые состояния не изменены. `OvaFallbackImage` наследует базовый CUBA `Image`, поэтому существующее поле `private Image candidateImage` в контроллере остаётся совместимым.

## 6. Локальный SCSS

Во всех семи темах аккордеон оформляется одинаковым локальным mixin:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-accordion-navigation.scss
```

Mixin оформляет фактический XML-класс через локальный селектор `.iteraction-list-editor .user-ai-profile-section` и одновременно сохраняет собственный класс `.iteraction-list-accordion-section` для дальнейшего безопасного reflow. Параметры геометрии совпадают с `SettingsWindow`, но правила не выходят за root экрана. Собственные стили полей, footer, toolbar и sidebar остаются в `iteraction-list-editor.scss`. Глобальные Vaadin-селекторы не добавляются.

## 7. Ограничения изменений

- изменён только copy-initializer `IteractionListBrowse` и добавлен узкий reload вакансии;
- `IteractionListEdit.java`, XML-дескрипторы и бизнес-обработчики editor не изменены;
- entity, поля, БД, Liquibase не изменены;
- loaders, JPQL, conditions и определения views не изменены;
- component ID, captions существующих компонентов, actions и `invoke` не изменены;
- runtime `visible`, `required`, `editable`, caption и stylename не переопределены статически;
- production не изменяется в рамках разработки;
- merge допускается только после отчёта Hermes по точному HEAD SHA.

## 8. Обязательная проверка Hermes

1. HEAD branch и HEAD PR совпадают с переданным SHA.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. `IteractionListCopyProjectDepartmentTest` — `1/1 PASS`.
4. `IteractionListAccordionCssContractTest` — `1/1 PASS`.
5. `IteractionListEditAccordionLayoutTest` — `5/5 PASS`.
6. `LeftSidebarAvatarComponentTest` — `2/2 PASS`.
7. Compile web и core tests.
8. `ScreenViewIntegrityTest` — `8/8 PASS`.
<<<<<<< HEAD
9. Data View Integrity — `openPosition-iteraction-list-picker-view` содержит все getters цепочки `projectName.projectDepartment.companyName`.
10. `clean assemble` — `BUILD SUCCESSFUL`.
11. Local deploy и HTTP `/hrm/` = `200`.
12. Copy smoke: выбрать строку с вакансией, нажать «Копировать», убедиться, что editor открывается без `Cannot get unfetched attribute [projectDepartment]`.
13. Проверить копирование кандидата и вакансии, отображение компании, подразделения и проекта, затем сохранить новую запись.
14. Повторить для вакансии без проекта и без подразделения — editor должен открыться без исключения.
15. Tomcat logs: новых `IllegalStateException`, unfetched/detached errors и critical errors NONE; P1 = 0; P2 = 0.
9. Data View Integrity — getters контроллера входят в `iteractionList-edit-view`.
10. `:app-web:buildScssThemes` — PASS для семи тем.
11. `clean assemble` — `BUILD SUCCESSFUL`.
12. Local deploy и HTTP `/hrm/` = `200`.
13. Functional smoke: последовательно заполнить кандидата, вакансию, тип, dynamic fields, rating, рекрутёра, способ связи и комментарий; проверить подписку, save/cancel.
14. Accordion smoke: свернуть и раскрыть каждый из трёх блоков, убедиться в сохранении введённых значений и отсутствии пустых горизонтальных областей.
15. Visual smoke семи тем: радиус, граница, поверхность, заголовок и интервалы аккордеонов соответствуют `SettingsWindow`; поля идут одной колонкой, horizontal scroll отсутствует.
16. Tomcat logs: новых critical errors NONE; P1 = 0; P2 = 0.

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
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
