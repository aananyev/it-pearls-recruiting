# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует взаимодействие рекрутёра с кандидатом по вакансии: участника процесса, вакансию, тип взаимодействия, рейтинг, способ коммуникации, рекрутёра, дополнительное значение и комментарий. Экран участвует в формировании истории кандидата, статусов процесса, подписок, уведомлений и связанных действий, поэтому визуальные изменения не должны изменять lifecycle, data-контракты и бизнес-логику.

Уточнение компоновки от 2026-07-25 устраняет визуальный разрыв двухпанельной формы: тёмная контекстная панель теперь занимает полную высоту диалога, а toolbar, вкладка и footer относятся только к светлой рабочей области. Рабочие поля выстроены в последовательности фактической работы рекрутёра.

Профильное изображение кандидата в левой панели приведено к единому контракту HRM HuntTech: `candidateImage` отображается через legacy-компонент `OvaFallbackImage`, имеет стабильную круглую геометрию и показывает `icons/no-programmer.jpeg`, если фотография кандидата отсутствует. Java-контроллер и модель данных не изменены.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания или редактирования взаимодействия.
- Picker кандидата сохраняет lookup и open для `JobCandidate`.
- Picker вакансии сохраняет lookup и open для `OpenPosition`.
- Выбор типа взаимодействия управляет существующими динамическими компонентами дополнительного действия.
- Кнопка подписки открывает существующий editor подписки.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.
- Экран остаётся модальным диалогом `1000 × 650`.

## Behavior Summary

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовую форму;
- выбор кандидата с фотографией → сохраняется прежний `ContainerValueSource` → `OvaFallbackImage` отображает фотографию круглой;
- выбор кандидата без фотографии → существующая Java-логика и `fallbackThemePath` указывают на `icons/no-programmer.jpeg` → sidebar не содержит пустого изображения;
- выбор вакансии → сохраняются проверки закрытия, подписки, статуса, приоритета и логотипа → sidebar обновляет вакансию;
- выбор типа взаимодействия → Java переключает `buttonCallAction`, `addString`, `addDate` или `addInteger` → дополнительное значение остаётся в соседней колонке;
- изменение rating → Java сохраняет прежнее оформление и правила → оценка отображается в форме и sidebar;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → данные и связанные процессы изменяются как до reflow;
- смена темы → локальный mixin `.iteraction-list-editor` применяет presentation-слой → функциональные контракты не меняются.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| `@UiController` | `hunttech_IteractionList.edit` |
| Java | `com.company.hunttech.web.screens.iteractionlist.IteractionListEdit` |
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
| `usersDc` / `usersDl` | `_minimal` | активные пользователи | без изменений |

JPQL, query conditions, параметры loaders, `cacheable` и views сохранены. `IteractionListEdit.java`, entity, БД, Liquibase и `views.xml` не изменялись.

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
   │  ├─ popular interactions card
   │  ├─ form card
   │  │  ├─ candidate + vacancy / subscription filter
   │  │  ├─ interaction type + dynamic action/value
   │  │  ├─ rating + recruiter
   │  │  └─ communication method, full width
   │  └─ comment card, full width
   └─ footer 54 px
```

### Причины решений

- Sidebar больше не пересекается верхней и нижней светлыми панелями и воспринимается как единая контекстная область.
- Ширина sidebar уменьшена с `270` до `252 px`, поэтому рабочая область получила дополнительное пространство без потери читаемости.
- Фото кандидата остаётся главным визуальным объектом и использует `OvaFallbackImage`; логотип проекта остаётся отдельным обычным `Image`, поскольку не является фотографией человека.
- `ovalWidth`, `ovalHeight`, `width` и `height` фотографии равны `104 px`; `SCALE_DOWN` исключает искажение, а `fallbackThemePath` задаёт штатный placeholder.
- Номер и дата расположены вертикально: captions и значения не сжимаются в узкой панели.
- Компания, проект, статус и приоритет оформлены как `caption → value`, а не как длинные горизонтальные строки.
- `ratingField` больше не занимает две колонки; он расположен рядом с `recrutierField`.
- `communicationMethodField` занимает всю ширину и не конкурирует с picker рекрутёра.
- `onlyMySubscribeCheckBox` выровнен по левой границе поля вакансии.
- Комментарий уменьшен до `160 px`, чтобы основные поля и footer были доступны при меньшем объёме прокрутки.

## 4. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `candidateField`, `vacancyFiels` | bindings, lookup/open actions и query |
| `iteractionTypeField` | binding, lookup и Java value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime visible/required/caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer и option icon provider |
| `commentField` | binding, lazy reload, runtime required и автодополнение |
| `candidateImage` | legacy ID, `iteractionListDc`, `candidate.fileImageFace`, Java-инъекция `Image`, runtime `setValueSource` / `setSource`; XML-тип — `OvaFallbackImage` |
| `projectLogoImage` | отдельный обычный `Image`, прежний source и Java-инъекция |
| `mostPopularHbox`, `mostPopularIteractionHBox` | отдельные XML-контейнеры |
| `subscribeButton` | `invoke="onButtonSubscribeClick"` |
| footer | порядок subscribe → commit-and-close → cancel |

Component ID, bindings, actions, `invoke`, validators и runtime-управляемые состояния не изменены. `OvaFallbackImage` наследует базовый CUBA `Image`, поэтому существующее поле `private Image candidateImage` в контроллере остаётся совместимым.

## 5. Аудит переработанных левых панелей

| Форма | Профильная левая панель | Компонент | Результат |
|---|---|---|---|
| `JobCandidateEdit` | `jobCandidateSidebar` | `candidatePic: OvaFallbackImage` | соответствует |
| `ExtSettingsWindow` | `userAiProfileSidebar` | `userPic: OvaFallbackImage` | соответствует |
| `IteractionListEdit` | `.iteraction-list-sidebar` | `candidateImage: OvaFallbackImage` | приведено в соответствие |
| `CandidateCVEdit` | отдельной левой профильной панели нет; фото находится справа | `candidatePic: Image` | правило левой панели не применяется |

Проектные логотипы, индикаторы рейтинга и светофор приоритета не являются фотографиями пользователя, кандидата, рекрутёра или сотрудника и сохраняют свои исходные типы.

## 6. Локальный SCSS

Во всех семи темах используется одинаковый файл:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-editor.scss
```

Правила вложены только в `.iteraction-list-editor`. Локальный слой оформляет sidebar, toolbar, TabSheet, карточки, picker actions, checkbox, dynamic panel, comment, footer, focus, hover, disabled, read-only, required и validation error.

Адаптивная геометрия при viewport до `1100 px`:

- sidebar: `232 px`;
- candidate image: `94 px` визуального пространства при сохранении XML-контракта компонента;
- project image: `68 px`;
- уменьшенные горизонтальные padding рабочей области.

Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` вне namespace не изменялись.

## 7. Ограничения изменений

- бизнес-логика и Java handlers не изменены;
- entity, поля, БД, Liquibase не изменены;
- loaders, JPQL, conditions и views не изменены;
- component ID, captions существующих компонентов, actions и `invoke` не изменены;
- замена затронула только XML-тип профильного `candidateImage`, совместимый с базовым `Image`;
- runtime `visible`, `required`, `editable`, caption и stylename не переопределены статически;
- production не изменяется в рамках разработки;
- merge допускается только после отчёта Hermes по точному HEAD SHA.

## 8. Обязательная проверка Hermes

1. HEAD branch и HEAD PR совпадают с переданным SHA.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. `LeftSidebarAvatarComponentTest` — `2/2 PASS`.
5. Compile web и core tests.
6. `ScreenViewIntegrityTest` — `8/8 PASS`.
7. Data View Integrity — getters контроллера входят в `iteractionList-edit-view`.
8. `:app-web:buildScssThemes` — PASS для семи тем.
9. `clean assemble` — `BUILD SUCCESSFUL`.
10. Local deploy и HTTP `/hrm/` = `200`.
11. Functional smoke: кандидат с фото, кандидат без фото, вакансия, тип, dynamic fields, rating, рекрутёр, подписка, save/cancel.
12. Visual smoke семи тем: фотография круглая, fallback виден, sidebar непрерывный, toolbar/footer только справа, нет horizontal scroll и пустых dynamic slots.
13. Tomcat logs: новых critical errors NONE; P1 = 0; P2 = 0.

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | По итогам аудита переработанных форм `candidateImage` в левой панели заменён на `OvaFallbackImage` 104×104 px с fallback `icons/no-programmer.jpeg`; ID, binding и Java-инъекция `Image` сохранены |
| 2026-07-25 | Улучшена компоновка: sidebar сделан непрерывным по высоте, toolbar и footer перенесены в workspace, ширина sidebar уменьшена, поля выстроены по сценарию рекрутёра, геометрия синхронизирована в семи темах |
| 2026-07-25 | Выполнена строго визуальная адаптация `IteractionListEdit`: двухпанельная композиция, локальный namespace `.iteraction-list-editor`, карточки и theme-aware состояния семи тем |
