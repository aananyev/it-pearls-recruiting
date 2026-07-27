# IteractionListEdit — спецификация экранной формы

> Controller: `hunttech_IteractionList.edit`  
> XML: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Entity: [IteractionList](../entities/iteraction-list/IteractionList.md)  
> Legacy-spec: [hunttech_IteractionList.edit_Spec.md](../screens/iteraction-list/hunttech_IteractionList.edit_Spec.md)  
> UI/UX-концепция: [HRM_HuntTech_UI_UX_Design_Concept.md](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)  
> Компонент изображений: [OvaFallbackImage](../screens/components/OvaFallbackImage.md)

## Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` фиксирует экземпляр `IteractionList`: взаимодействие рекрутёра с кандидатом по конкретной вакансии. Пользователь выбирает кандидата, вакансию и тип взаимодействия, вводит предусмотренные типом дополнительные данные, оценку, способ коммуникации, рекрутёра и комментарий.

Запись участвует в истории кандидата, подписках, уведомлениях и изменении статусов рекрутингового процесса. Поэтому исправление компоновки и изображений не должно менять component ID, data bindings, actions, `invoke`, loader conditions, JPQL, validation и lifecycle CUBA Platform 7.3.

Блок пяти частых взаимодействий является ускорителем ежедневного ввода. Он должен быть видим сразу после открытия вкладки, до первого аккордеона, чтобы пользователь мог выбрать наиболее вероятный тип взаимодействия без дополнительного раскрытия секции.

## UI Context & Navigation

- Экран открывается из `hunttech_IteractionList.browse`, карточки кандидата и связанных сценариев создания, копирования или редактирования взаимодействия.
- Picker кандидата сохраняет suggestion, lookup и open для `JobCandidate`; picker вакансии сохраняет lookup и open для `OpenPosition`.
- Постоянная sidebar сохраняет утверждённую последовательность: фотография кандидата и логотип проекта → ФИО → название вакансии → индекс разделов → номер/дата → карточка вакансии.
- Правая область сохраняет toolbar и штатный `TabSheet`; внутри вкладки перед первым рабочим аккордеоном постоянно отображается блок пяти быстрых взаимодействий.
- В раскрытом разделе «Кандидат и вакансия» оба picker-поля занимают одну строку одинаковой высоты; checkbox «Показывать только мои подписки» располагается отдельной строкой ниже.
- Клик по пункту sidebar раскрывает связанный аккордеон и переводит фокус в первое поле. Пункт «Частые взаимодействия» сохраняет legacy navigation-контракт, а сами кнопки остаются видимыми независимо от состояния аккордеонов.
- Сохранение выполняет `windowCommitAndClose`, отмена — `windowClose`.

## Behavior Summary

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовый экземпляр `IteractionList`;
- открытие вкладки → `mostPopularHbox` уже находится перед первым аккордеоном → пять быстрых кнопок видимы без дополнительного действия;
- статистика пользователя содержит менее пяти типов → недостающие позиции отображаются как disabled-кнопки «Нет данных» → геометрия блока остаётся стабильной;
- клик по заполненной быстрой кнопке → контроллер передаёт точный объект `Iteraction` в `iteractionTypeField` → зависимые runtime-поля обновляются штатным обработчиком;
- открытие существующего взаимодействия → `iteractionList-edit-view` загружает candidate/vacancy-контекст → sidebar и picker-поля отображают текущую запись;
- ввод в `candidateField` → suggestion query использует узкий `jobCandidate-iteraction-list-suggestion-view` → загружаются только поля, читаемые формой, включая `fileImageFace`, позицию и город;
- выбор кандидата → `candidate` записывается в `iteractionListDc` → ФИО и фотография отражают выбранный экземпляр;
- выбор вакансии → `vacancy` записывается в `iteractionListDc` → название, проект, логотип и контекст вакансии обновляются прежними обработчиками;
- доступный `FileDescriptor` → `FileLoader.openStream()` успешно открывает физический файл → стандартный `FileDescriptorResource` отображает изображение;
- descriptor отсутствует, поле unfetched или физический файл недоступен → `OvaFallbackImage` применяет theme fallback → форма продолжает открываться без ошибки UI-thread;
- раскрытие первого аккордеона → два picker-поля и checkbox занимают естественную высоту → следующий заголовок не перекрывает содержимое;
- выбор типа взаимодействия → прежняя Java-логика управляет `buttonCallAction`, `addString`, `addDate` и `addInteger`;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → бизнес-логика не меняется.

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

## 2. Data View Integrity и lazy loading

| Источник | View | Назначение |
|---|---|---|
| `iteractionListDc` / `iteractionListDl` | `iteractionList-edit-view` | редактируемый экземпляр и исходный candidate/vacancy-контекст |
| `candidateField` suggestion query | `jobCandidate-iteraction-list-suggestion-view` | узкий detached-граф кандидата для обработчиков формы |
| `openPositionDc` / `openPositionsDl` | `openPosition-iteraction-list-picker-view` | вакансии, проект, подразделение и логотип |
| `iteractionTypesDc` / `iteractionTypesLc` | `iteraction-list-type-view` | типы взаимодействий и динамические настройки |
| `usersDc` / `usersDl` | `_minimal` | активные пользователи |

`jobCandidate-iteraction-list-suggestion-view` вынесен в `modules/global/src/com/company/hunttech/iteraction-list-views.xml` и подключён через `cuba.viewsConfig`. Он содержит:

- `fullName`, составные части имени, `email`, `status`, `blockCandidate`;
- `fileImageFace`;
- `personPosition`;
- `cityOfResidence`.

В view намеренно не входят `candidateCv`, `iteractionList`, `skillTree`, `jobHistory`, договоры и другие тяжёлые коллекции. Это исключает загрузку полного графа кандидата для фонового suggestion-поиска и одновременно предотвращает чтение unfetched-полей обработчиками формы.

`openPosition-iteraction-list-picker-view` сохраняется как существующий узкий граф вакансии и уже включает `projectName.projectLogo` и `projectDepartment.companyName.fileCompanyLogo`.

## 3. Компоновка

```text
main layout 100% × 100%
├─ sidebar 296 px / 276 px / 260 px
│  ├─ candidateImage 112 × 112 + projectLogoImage 80 × 80
│  ├─ candidate.fullName
│  ├─ vacancy.vacansyName
│  ├─ индекс пяти presentation-разделов
│  ├─ карточка взаимодействия: номер + дата
│  ├─ карточка вакансии
│  └─ spacer
└─ workspace
   ├─ toolbar
   ├─ TabSheet 48 px
   ├─ scrollBox
   │  ├─ mostPopularQuickActions: 5 кнопок [always visible]
   │  ├─ Кандидат и вакансия [expanded]
   │  │  ├─ GridLayout: candidateField | vacancyFiels
   │  │  └─ onlyMySubscribeCheckBox
   │  ├─ Тип и действие [collapsed]
   │  ├─ Результат [collapsed]
   │  ├─ Комментарий [collapsed]
   │  └─ popularAccordion [hidden compatibility component]
   └─ footer: subscribe / commit / cancel
```

### 3.1. Постоянный блок быстрых взаимодействий

`mostPopularQuickActions` находится внутри `iteractionListTab`, в прокручиваемой рабочей области, непосредственно перед `participantsAccordion`.

Компоненты `mostPopularIteractionHBox` и `mostPopularHbox` сохранены без переименования. Контроллер продолжает:

- строить пять кнопок;
- рассчитывать статистику текущего пользователя за скользящий год;
- сохранять ширину каждой позиции 20%;
- устанавливать точный `Iteraction` без разбора caption.

`popularAccordion` оставлен невидимым compatibility-компонентом, потому что существующий presentation-контроллер инъецирует его и синхронизирует пятый пункт sidebar. Он не содержит кнопок и не участвует в визуальной компоновке.

### 3.2. Естественная высота аккордеонов

Каждый рабочий `GroupBoxLayout`, его Vaadin slot, panel-content и внутренний `GridLayout` используют естественную высоту (`AUTO` / `height: auto`). Это необходимо, потому что CUBA/Vaadin рассчитывает размер соседних layout-slot по фактической высоте дочернего контейнера.

Для первой секции установлен минимальный запас по высоте, достаточный для caption секции, picker-полей, отдельной строки checkbox и нижнего внутреннего отступа.

SCSS не задаёт фиксированную высоту бизнес-полям и не изменяет их bindings, required или visible-состояния.

### 3.3. Поля кандидата и вакансии

`candidateField` и `vacancyFiels` остаются двумя explicit flex-колонками `GridLayout`. Правый picker больше не обёрнут в `VBox` вместе с checkbox, поэтому обе колонки имеют одинаковую вертикальную геометрию.

`onlyMySubscribeCheckBox` сохраняет legacy ID, caption, description и существующий listener контроллера. Изменилось только его расположение: отдельная полноширинная строка под picker-полями.

### 3.4. Фокус и прокрутка

Внутренний scroll-container использует `scroll-padding-top: 18px`. При переходе по label-навигации и вызове `focus()` браузер оставляет заголовок раскрытого аккордеона видимым, а не помещает его под строку вкладок.

## 4. FileStorage и fallback изображений

`FileDescriptor` хранит metadata файла, а бинарное содержимое находится в FileStorage. Поэтому безопасная цепочка изображения имеет вид:

```text
ValueSource / controller
→ FileDescriptor
→ FileLoader.openStream(descriptor)
→ readable: FileDescriptorResource
→ unavailable/unfetched/error: ThemeResource fallback
```

`FileDescriptorImageHelper.fileExists()` проверяет фактическую читаемость файла через `FileLoader.openStream()` в `try-with-resources`. Поток закрывается сразу после проверки; байты изображения не копируются в память этим helper.

`FallbackImageResourceDelegate` перехватывает `RuntimeException` при чтении bound `ValueSource` и при разрешении ресурса. Это покрывает detached/unfetched-атрибуты и ошибки presentation-цепочки: компонент отображает fallback вместо прекращения открытия экрана.

Metadata `FileDescriptor` не удаляется и не изменяется. При восстановлении физического файла стандартное отображение снова доступно.

## 5. Сохранённые component-контракты

| Компоненты | Сохранённый контракт |
|---|---|
| `candidateField`, `vacancyFiels` | bindings, lookup/open actions, query/optionsContainer |
| `onlyMySubscribeCheckBox` | ID, caption, description, value-change listener и loader filtering |
| `iteractionTypeField` | binding, lookup и value-change |
| `buttonCallAction` | `invoke="callActionEntity"` |
| `addString`, `addDate`, `addInteger` | bindings и runtime visible/required/caption |
| `ratingField` | binding, required, option style provider |
| `recrutierField` | binding, optionsContainer и option icon provider |
| `communicationMethodField` | binding и caption |
| `commentField` | binding, lazy reload и runtime required |
| `candidateImage` | legacy ID, `OvaFallbackImage`, candidate binding и fallback |
| `projectLogoImage` | legacy ID, Java-инъекция, `OvaFallbackImage` и fallback |
| `mostPopularHbox` | пять равных быстрых кнопок; прямое присваивание `Iteraction`; расположен перед первым аккордеоном |
| `popularAccordion` | скрытый compatibility-компонент для существующей Java-инъекции и sidebar navigation |
| footer | subscribe → commit-and-close → cancel |

## 6. Локальный SCSS

Все правила ограничены корнем `.iteraction-list-editor`. Существующие правила `.iteraction-list-popular-host`, `.iteraction-list-popular-buttons` и `.iteraction-list-popular-button` продолжают обеспечивать равную геометрию и читаемость пяти кнопок во всех семи темах.

Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` и `.v-panel` не изменяются.

## 7. Ограничения изменений

- бизнес-обработчики `IteractionListEdit.java` не изменены;
- entity, поля, БД и Liquibase не изменены;
- loader ID, conditions и JPQL коллекций сохранены;
- component ID, bindings, actions и `invoke` сохранены;
- изменено только расположение runtime-host пяти кнопок в XML;
- production не изменяется;
- merge допускается только после отчёта Hermes по точному HEAD SHA и прямой команды Алексея.

## 8. Обязательная проверка Hermes

1. Branch HEAD = PR HEAD = SHA, указанный в PR.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. `IteractionListEditAccordionLayoutTest`, `IteractionListMostPopularInteractionTest`, `IteractionListAccordionNavigationTest`, `IteractionListRpcCompatibilityContractTest` — PASS.
5. `ScreenViewIntegrityTest` — `8/8 PASS`.
6. `:app-web:buildScssThemes` — PASS для семи тем.
7. `clean assemble` — `BUILD SUCCESSFUL`.
8. Clean local deploy и HTTP `/hrm/` = `200`.
9. Visual smoke: внутри вкладки над первым аккордеоном видны ровно пять кнопок; кнопки не исчезают при переключении аккордеонов.
10. Functional smoke: клик по заполненной кнопке устанавливает тип взаимодействия; candidate/vacancy, dynamic fields, rating, comment, subscription, save/cancel работают без регрессии.
11. Tomcat logs: новых `Cannot get unfetched attribute`, `ClassCastException`, P1 и P2 нет.

До отчёта Hermes статус задачи: `WAITING_FOR_HERMES`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Блок пяти быстрых взаимодействий возвращён внутрь вкладки и размещён перед первым аккордеоном; `mostPopularHbox` и Java-логика сохранены |
| 2026-07-27 | Устранено наложение первой секции: picker-поля выровнены в одной строке, checkbox вынесен ниже GridLayout, аккордеоны переведены на естественную высоту; suggestion кандидата получил узкий view, а изображения — безопасную проверку FileStorage и fallback |
| 2026-07-27 | Sidebar приведена к порядку «образ → имя → навигация → детали», частые взаимодействия перенесены в свой аккордеон, аккордеоны и footer уплотнены |
| 2026-07-26 | `candidateImage` и `projectLogoImage` унифицированы как `OvaFallbackImage` |
| 2026-07-26 | Восстановлен видимый аккордеон «Кандидат и вакансия», добавлен блок пяти быстрых действий |
| 2026-07-26 | Компоновка синхронизирована с `CandidateCVEdit` и `ExtSettingsWindow` |
| 2026-07-25 | Добавлены двухколоночная строка кандидата и вакансии, кликабельный индекс и пять быстрых взаимодействий |
| 2026-07-25 | Исправлен сценарий «Копировать» для detached-вакансии |
| 2026-07-25 | Выполнена двухпанельная визуальная адаптация с локальным namespace `.iteraction-list-editor` |
