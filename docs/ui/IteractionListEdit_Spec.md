# IteractionListEdit — спецификация экрана и бизнес-логики HRM HuntTech

> Screen ID: `hunttech_IteractionList.edit`  
> Entity: `hunttech_IteractionList` / `IteractionList`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Зарегистрированный controller: `IteractionListEdit`  
> Незарегистрированный presentation-helper: `IteractionListEditAccordionNavigation`  
> Платформа: CUBA Platform 7.3-SNAPSHOT  
> Статус документа: каноническая living-спецификация экрана

## 1. Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` создаёт или изменяет один факт взаимодействия рекрутёра с кандидатом в контексте конкретной вакансии. Экран является узлом нескольких бизнес-процессов HRM HuntTech, а не только формой ввода.

Запись фиксирует:

- кандидата;
- вакансию;
- ответственного рекрутёра;
- дату и номер взаимодействия;
- тип контакта;
- дополнительное значение, зависящее от типа;
- оценку результата;
- способ коммуникации;
- комментарий;
- снимок приоритета и состояния вакансии;
- ссылку на предыдущее взаимодействие той же цепочки.

Сохранение взаимодействия может дополнительно:

1. изменить статус кандидата;
2. создать или обновить `Employee`;
3. создать автоматическую новость вакансии;
4. отправить email подписчику кандидата;
5. опубликовать UI-уведомление;
6. открыть форму подготовки письма кандидату;
7. создать подписку пользователя на кандидата.

Поэтому UI-изменение не вправе автоматически менять entity, JPQL, loaders, views, required-состояния, сервисные вызовы, side effects или порядок lifecycle CUBA Platform.

## 2. Фактический runtime-статус controller

### 2.1. Единственный зарегистрированный экран

В текущем `master` аннотации экрана находятся только на `IteractionListEdit`:

```java
@UiController("hunttech_IteractionList.edit")
@UiDescriptor("iteraction-list-edit.xml")
@EditedEntityContainer("iteractionListDc")
@LoadDataBeforeShow
```

Следовательно, именно `IteractionListEdit` создаётся CUBA Platform и владеет фактическим runtime lifecycle.

### 2.2. `IteractionListEditAccordionNavigation`

После commit Hermes `078ba63c4577c355142a49fcc31e5e775111a02f` из класса удалены:

- `@UiController`;
- `@UiDescriptor`;
- `@EditedEntityContainer`;
- `@LoadDataBeforeShow`.

Класс остался наследником `IteractionListEdit`, но не зарегистрирован как экран, Spring bean или иной вызываемый компонент. Прямых ссылок на его создание в коде нет.

Следствие: его методы `onAfterShowInitializePresentation()`, `initializeNavigation()` и `normalizePopularButtons()` автоматически не выполняются. Документация различает:

- **фактическое поведение** — методы `IteractionListEdit`;
- **неактивный helper-контракт** — код `IteractionListEditAccordionNavigation`, который сам по себе не участвует в runtime.

Это архитектурное расхождение должно быть устранено отдельной функциональной задачей. Документационная задача не меняет код.

## 3. UI Context & Navigation

### 3.1. Точки открытия

Экран использует legacy ID `hunttech_IteractionList.edit` и открывается в сценариях:

- создание из browse взаимодействий;
- редактирование существующей записи;
- создание из карточки кандидата;
- создание с переданным `parentCandidate`;
- копирование предыдущей вакансии кандидата;
- продолжение цепочки с теми же кандидатом и вакансией;
- связанные recruiter/vacancy workflows.

### 3.2. XML-компоновка

```text
IteractionListEdit
├── edit-sidebar
│   ├── candidateImage + projectLogoImage
│   ├── ФИО кандидата + название вакансии
│   ├── label-navigation
│   ├── номер + дата
│   ├── компания / проект / статус / приоритет / стоимость
│   └── spacer
└── edit-workspace
    ├── toolbar
    ├── mostPopularQuickActions
    ├── vertical scroll
    │   ├── participantsAccordion
    │   ├── interactionAccordion
    │   ├── resultAccordion
    │   └── commentAccordion
    └── footer actions
```

`mostPopularQuickActions` находится над scroll-area и не является аккордеоном.

### 3.3. Фактическая runtime-навигация

Базовый `onInitIteractionNavigation()` удаляет XML fallback labels и создаёт пять runtime-кнопок:

1. кандидат и вакансия;
2. тип взаимодействия;
3. результат;
4. комментарий;
5. частые взаимодействия.

Пятый пункт связан с `popularAccordion`, который в XML имеет `visible="false"`. Поэтому он является legacy compatibility-пунктом, а не полноценным видимым разделом.

Базовый controller использует legacy stylename:

```text
borderless iteraction-list-nav-item
borderless iteraction-list-nav-item iteraction-list-nav-item-active
```

Неактивный helper содержит целевой контракт четырёх `label-nav-item`, но он не применяется автоматически в текущем runtime.

### 3.4. Аккордеоны

| Раздел | Компоненты | Focus target | Default |
|---|---|---|---|
| Кандидат и вакансия | `candidateField`, `vacancyFiels`, `onlyMySubscribeCheckBox` | `candidateField` | открыт |
| Тип взаимодействия | `iteractionTypeField`, `buttonCallAction`, `addString`, `addDate`, `addInteger` | `iteractionTypeField` | закрыт |
| Результат | `ratingField`, `recrutierField`, `communicationMethodField` | `ratingField` | закрыт |
| Комментарий | `commentField` | `commentField` | закрыт |
| Legacy frequent | `popularAccordion`, `visible=false` | отсутствует | закрыт |

Клик navigation раскрывает один GroupBox и сворачивает остальные. Флаг `updatingAccordionState` предотвращает рекурсивные expanded events.

## 4. Behavior Summary

- новый объект → назначаются номер и дата → текущий пользователь назначается рекрутёром после показа;
- существующий объект → дата read-only → комментарий догружается узким view;
- выбран кандидат → обновляется фото → проверяется предыдущая активность;
- выбрана вакансия → проверяются позиция, локация, закрытие, подписка и начало новой цепочки;
- новая пара кандидат + вакансия → типы ограничиваются группой `001`;
- выбран тип → изменяются dynamic fields, required-комментарий и action-кнопка;
- введено дополнительное значение → оно дописывается в комментарий;
- quick action → точный `Iteraction` устанавливается в поле типа;
- before commit → snapshot вакансии, chain interaction и кадровый side effect;
- after commit → автоматическая новость вакансии;
- commit-and-close → статус кандидата, email/notification и письмо кандидату;
- `Подписаться` → сохранение новой записи при подтверждении → отдельный editor подписки;
- `Отмена` → стандартный `windowClose`.

## 5. Модель данных

### 5.1. Поля `IteractionList`

| Поле | Тип | Обязательность | Смысл |
|---|---|---:|---|
| `numberIteraction` | `BigDecimal` | нет | номер взаимодействия |
| `iteractionType` | `Iteraction` | UI required | тип и его бизнес-настройки |
| `dateIteraction` | `Date` | нет | дата события |
| `candidate` | `JobCandidate` | `@NotNull` | кандидат |
| `vacancy` | `OpenPosition` | `@NotNull` | вакансия |
| `communicationMethod` | `String` | нет | способ связи |
| `comment` | LOB `String` | условно | комментарий |
| `recrutier` | `ExtUser` | `@NotNull` | рекрутёр |
| `recrutierName` | `String` | нет | legacy текстовое поле |
| `addType` | `Integer` | нет | legacy тип дополнительного значения |
| `addDate` | `Date` | условно | дополнительная дата |
| `addString` | `String` | условно | дополнительный текст |
| `addInteger` | `Integer` | условно | дополнительное число |
| `rating` | `Integer` | UI required | оценка 0–4 |
| `currentPriority` | `Integer` | before commit | снимок приоритета вакансии |
| `currentOpenClose` | `Boolean` | before commit | снимок статуса вакансии |
| `chainInteraction` | `IteractionList` | before commit | предыдущее событие цепочки |

### 5.2. Рабочие bindings

| Component ID | Binding | Назначение |
|---|---|---|
| `candidateField` | `candidate` | кандидат |
| `vacancyFiels` | `vacancy` | вакансия |
| `iteractionTypeField` | `iteractionType` | тип |
| `dateIteractionField` | `dateIteraction` | дата |
| `numberIteractionField` | `numberIteraction` | номер |
| `recrutierField` | `recrutier` | рекрутёр |
| `ratingField` | `rating` | оценка |
| `communicationMethodField` | `communicationMethod` | способ связи |
| `commentField` | `comment` | комментарий |
| `addDate` | `addDate` | динамическая дата |
| `addString` | `addString` | динамический текст |
| `addInteger` | `addInteger` | динамическое число |

### 5.3. Sidebar

| Component | Источник / поведение |
|---|---|
| `candidateImage` | `candidate.fileImageFace`, fallback `icons/no-programmer.jpeg` |
| `projectLogoImage` | `vacancy.projectName.projectLogo`, fallback `icons/no-company.png` |
| `iteractionCandidateNameLabel` | `candidate.fullName` |
| `iteractionVacancyNameLabel` | `vacancy.vacansyName` |
| `companyLabel` | компания и подразделение проекта |
| `projectLabel` | проект |
| `closingDateVacancyLabel` | дата закрытия и просрочка |
| `statusOfVacansyLabel` | `ОТКРЫТА` / `ЗАКРЫТА` |
| `alternativeVacancyLinkButton` | открытые альтернативы того же типа позиции |
| `currentPriorityLabel` | текстовый приоритет |
| `trafficLighterImage` | иконка приоритета |
| `outstaffingCostHBox` | видим при непустом `outstaffingCost` |

## 6. Data containers, loaders и views

### 6.1. Основной объект

| ID | Тип | View |
|---|---|---|
| `iteractionListDc` | instance `IteractionList` | `iteractionList-edit-view` |
| `iteractionListDl` | instance loader | view контейнера |

### 6.2. Типы взаимодействий

`iteractionTypesDc` использует `iteraction-list-type-view`.

Базовый запрос:

- entity `hunttech_Iteraction`;
- `iteractionTree is not null`;
- сортировка по `iterationName`.

Условие `e.iteractionTree.number like :number` включается при первой записи пары кандидат + вакансия.

- новый процесс → `number="001"`;
- существующий процесс → параметр удалён.

### 6.3. Вакансии

`openPositionDc` использует `openPosition-iteraction-list-picker-view`.

`openPositionsDl` поддерживает:

- фильтр подразделения `departament`;
- фильтр активной задачи текущего пользователя `subscriber`.

`PreLoadListener` блокирует преждевременную загрузку до установки параметров. Это предотвращает загрузку полного списка при `@LoadDataBeforeShow`.

### 6.4. Пользователи

`usersDc` загружает активных `sec$User` и сортирует по имени.

### 6.5. Внутренние запросы

| Запрос | Назначение |
|---|---|
| `QUERY_COUNT_BY_CANDIDATE_VACANCY` | первый ли контакт пары |
| `QUERY_CHAIN_LAST` | предыдущая запись цепочки |
| `QUERY_EMPLOYEE_BY_CANDIDATE` | поиск `Employee` |
| `QUERY_CHECK_CANDIDATE_EMPLOYEE` | проверка `workStatus.inStaff` |
| `QUERY_STATUS_OF_VACANCY` | открытые альтернативы |
| `RecrutiesTasks` count | активная подписка Researcher |
| `SubscribeCandidateAction` count | активная подписка на кандидата |

## 7. Screen options и внешний контекст

### `IteracionListScreenOptions`

`noSubscribers` переносится в controller-флаг `noSubscribe`. Это legacy-контракт callers; его нельзя удалять без поиска всех точек открытия.

### `parentCandidate`

`setParentCandidate()` позволяет внешнему экрану передать кандидата. В `BeforeShow` кандидат назначается entity.

### `transferFlag`

`setTransferFlag()` / `getTransferFlag()` сохраняют legacy-флаг передачи контекста.

## 8. Lifecycle

### 8.1. `InitEvent`

`onInit()`:

1. назначает fallback-изображения;
2. блокирует преждевременный `openPositionsDl`;
3. сбрасывает `newProject`, `askFlag`, `askFlag2`;
4. скрывает dynamic fields;
5. читает screen options;
6. настраивает rating options;
7. строит карту приоритетов.

Отдельный `onInitIteractionNavigation()` строит пять legacy navigation-кнопок.

### 8.2. Item change основного контейнера

Для новой entity:

- `number = InteractionService.getCountInteraction() + 1`;
- `date = new Date()`;
- вызывается `setCurrentUserName()`.

`setCurrentUserName()` получает имя пользователя, но не записывает его в entity. Это текущий legacy no-op.

### 8.3. `BeforeShowEvent`

Порядок:

1. option providers вакансии;
2. скрытие action-кнопки;
3. запоминание кандидата;
4. включение «только мои подписки» и загрузка вакансий;
5. восстановление dynamic fields;
6. назначение `parentCandidate`;
7. построение фактически найденных быстрых кнопок;
8. дата editable только у новой entity.

### 8.4. `AfterShowEvent`

- новой entity назначается текущий рекрутёр;
- существующей entity один раз догружается `comment` узким runtime view;
- добавляется listener проверки кандидата.

Неактивный helper также содержит `@Subscribe AfterShowEvent`, но без регистрации класса этот handler автоматически не выполняется.

### 8.5. `BeforeCommitChangesEvent`

1. `comment=null` → `""`;
2. snapshot `currentPriority`;
3. snapshot `currentOpenClose`;
4. поиск `chainInteraction`;
5. start/end project side effect.

### 8.6. `AfterCommitChangesEvent`

Один раз:

- вызывается пустой legacy `setSubscribe()`;
- создаётся автоматическая новость вакансии через `OpenPositionService`.

### 8.7. `BeforeCloseEvent`

`onBeforeClose1()`:

- преобразует числовой префикс `Iteraction.number` в `candidate.status`;
- при commit-and-close запускает email/notification;
- при необходимости открывает `InternalEmailerEdit`;
- защищён флагом повторного выполнения.

Изменение `candidate.status` выполняется до проверки close action. При cancel его фактическая персистентность зависит от DataContext и должна проверяться smoke-тестом.

## 9. Выбор кандидата

### Изображение

При наличии `fileImageFace` изображение привязывается к `candidate.fileImageFace`, иначе используется fallback.

### История контактов

`copyAndCheckCandidate()`:

- при истории текущего рекрутёра может предложить скопировать предыдущую вакансию;
- при недавнем контакте другого рекрутёра показывает имя и дату предупреждением.

### Копирование

`copyPrevionsItems()` загружает последнее взаимодействие кандидата по максимальному номеру и переносит только вакансию. Тип, оценка, комментарий и add-values не копируются.

## 10. Выбор вакансии

### 10.1. Соответствие кандидату

Сравниваются:

- позиция кандидата и тип позиции вакансии;
- город кандидата и допустимые города;
- признак удалённой работы.

При расхождении warning предлагает очистить вакансию либо оставить выбор.

### 10.2. Новый процесс

Для открытой вакансии считается количество взаимодействий пары.

- `0` → параметр типов `001`, warning о начале процесса;
- больше `0` → ограничение снимается.

### 10.3. Закрытая вакансия

Для новой записи диалог предлагает отменить выбор. При подтверждении поле и визуальные индикаторы очищаются. Отказ позволяет продолжить с закрытой вакансией.

### 10.4. Researcher без подписки

Проверяется активная `RecrutiesTasks`. При отсутствии предлагается открыть `RecrutiesTasksEdit` со значениями:

- текущий пользователь;
- выбранная вакансия;
- текущая дата;
- дата будущего понедельника через `SubscribeDateService`.

Диалог показывается один раз за lifecycle.

### 10.5. Sidebar-контекст

Обновляются компания, подразделение, проект, логотип, closing date, просрочка, открытость, альтернативы, приоритет, иконка и outstaffing cost.

## 11. Фильтр «только мои подписки»

По умолчанию включён.

- включён → `subscriber=currentUser`;
- выключен → параметр удалён;
- после изменения loader перезапускается;
- пустой список вызывает warning.

Фильтр меняет только options вакансии.

## 12. Тип взаимодействия и dynamic fields

### 12.1. Используемые настройки `Iteraction`

| Поле типа | Влияние |
|---|---|
| `addFlag` | включает add-value |
| `addType` | дата / строка / число |
| `addCaption` | caption add-field |
| `callForm` | видимость action-кнопки |
| `callButtonText` | caption кнопки |
| `callClass` | динамически открываемая meta-class |
| `findToDic` | legacy-ветка открытия |
| `setDateTime` | автозаполнение addDate |
| `signComment` | required-комментарий |
| `number` | статус кандидата |
| `signStartProject` | start employee side effect |
| `signEndProject` | end employee side effect |
| `workStatus` | статус сотрудника |
| `needSendLetter` | подготовка письма |
| `textEmailToSend` | шаблон письма |
| `notificationNeedSend` | включение notification |
| `notificationWhenSend` | момент notification |
| `notificationType` | аудитория |
| `signPriorityNews` | приоритет vacancy news |

### 12.2. Матрица add-fields

| `addFlag` | `addType` | Компонент | Required | Action |
|---:|---:|---|---:|---|
| true | 1 | `addDate` | да | скрыта |
| true | 2 | `addString` | да | скрыта |
| true | 3 | `addInteger` | да | скрыта |
| false | — | add-fields скрыты | нет | видима при `callForm=true` |

`setDateTime=true` заполняет пустой `addDate` текущим временем.

### 12.3. Комментарий

`signComment=true` делает `commentField` обязательным.

Изменение add-value дописывает:

```text
<Название взаимодействия>: <значение>
```

Дата форматируется как `dd.MM.yyyy hh.mm`.

### 12.4. Динамическая форма

`callActionEntity()` строит screen ID `<callClass>.edit` и открывает новую entity в новой вкладке. `callClass` должен соответствовать существующей meta-class и editor screen.

## 13. Быстрые взаимодействия

### 13.1. Источник

```java
InteractionService.getMostPolularIteraction(userSession.getUser(), 5)
```

Алгоритм сервиса:

```text
текущий пользователь
→ текущая дата минус один календарный месяц
→ iteractionType is not null
→ group by iteractionType
→ order by count DESC
→ первые пять
```

### 13.2. Фактическое runtime-поведение

Базовый controller создаёт только фактически найденные кнопки. Каждая замыкает точный объект:

```java
iteractionTypeField.setValue(interaction);
iteractionTypeField.focus();
```

Если найдено меньше пяти типов, в текущем runtime отображается меньше пяти кнопок.

### 13.3. Неактивный helper-контракт

`IteractionListEditAccordionNavigation.normalizePopularButtons()` способен дополнять позиции disabled-кнопками `Нет данных`, но handler не выполняется автоматически, поскольку класс не зарегистрирован.

Следовательно, утверждение «всегда пять позиций» является целевым, но не подтверждённым текущим runtime-контрактом после commit `078ba63c...`.

## 14. Рейтинг

`ratingField` хранит 0–4:

| Значение | Отображение |
|---:|---|
| 0 | 1 звезда — Полный негатив |
| 1 | 2 звезды — Сомнительно |
| 2 | 3 звезды — Нейтрально |
| 3 | 4 звезды — Положительно |
| 4 | 5 звёзд — Отлично |

Listener обновляет label и stylename `rating_<color>_<1..5>`.

## 15. Подписка на кандидата

`subscribeButton` вызывает `onButtonSubscribeClick()`.

### Новая запись

1. вопрос о сохранении;
2. `commitChanges()` при подтверждении;
3. открытие новой `SubscribeCandidateAction`;
4. candidate, subscriber и startDate заполняются автоматически.

### Существующая запись

Editor подписки открывается сразу.

Подписка создаётся отдельной формой, а не напрямую кнопкой.

## 16. Commit и side effects

### 16.1. Snapshot вакансии

Перед commit:

- `currentPriority = vacancy.priority`;
- `currentOpenClose = vacancy.openClose`.

### 16.2. Chain interaction

Ищется последняя запись той же пары с непустым типом. Для первой записи `chainInteraction=null`.

### 16.3. Start project

Если `signStartProject=true`:

- ищется `Employee`;
- при отсутствии создаётся;
- устанавливаются candidate, employeeDate=`addDate`, vacancy, workStatus;
- выполняется `DataManager.commit(Employee)`.

Если сотрудник уже в штате, error notification не предотвращает commit.

### 16.4. End project

Если `signEndProject=true`:

- ищется либо создаётся `Employee`;
- устанавливаются candidate, dissmisalDate=`addDate`, vacancy, workStatus;
- выполняется commit.

Если кандидат не в штате, error notification не предотвращает commit.

### 16.5. Транзакционная особенность

`Employee` коммитится отдельно внутри `BeforeCommitChangesEvent`, до завершения стандартного commit `IteractionList`. Это зона возможной рассинхронизации и требует отдельной сервисной задачи для рефакторинга.

### 16.6. Vacancy news

После commit вызывается:

```java
OpenPositionService.setOpenPositionNewsAutomatedMessage(
    vacancy,
    iteractionType.iterationName,
    comment,
    dateIteraction,
    candidate,
    recrutier,
    iteractionType.signPriorityNews
)
```

`deleteTwiceEvent` защищает от дубля.

## 17. Статус кандидата

Из `Iteraction.number` берётся часть до точки и преобразуется в `Integer`.

```text
003.02 → 3
```

Нечисловой префикс вызывает `NumberFormatException`. Формат номера типа является бизнес-контрактом справочника.

## 18. Уведомления и email

### 18.1. Email текущему подписчику

При commit-and-close проверяется активная подписка текущего пользователя на кандидата. Если она есть, `EmailService.sendEmailAsync()` отправляет письмо текущему пользователю по шаблону `iteraction.html`.

### 18.2. UI notification

Реализован сценарий:

- `notificationNeedSend=true`;
- `notificationWhenSend=1`;
- `notificationType=6`.

Публикуется `UiNotificationEvent` для всех. Ветки типов 0–5 присутствуют, но не выполняют действий.

### 18.3. Письмо кандидату

При `needSendLetter=true`, заполненном шаблоне и email кандидата:

1. тип узко перезагружается с `textEmailToSend`;
2. `EmailGenerationService` формирует текст;
3. открывается `InternalEmailerEdit`;
4. получатель и body заполняются.

Автоматическая отправка кандидату не выполняется.

## 19. Дополнительные действия

### `addNewIteraction()`

Сохраняет текущую запись, открывает новую и переносит candidate + vacancy. Тип, оценка и комментарий не переносятся.

### Option providers вакансии

- image зависит от priority;
- icon показывает open/closed;
- style различает открытые и закрытые позиции.

## 20. Валидация

### Постоянно обязательны

- `candidateField`;
- `iteractionTypeField`;
- `ratingField`;
- entity: candidate, vacancy, recrutier.

### Условно обязательны

- comment при `signComment=true`;
- один add-field по `addFlag/addType`.

### Мягкие предупреждения

Не всегда блокируют сохранение:

- закрытая вакансия;
- mismatch позиции/города;
- отсутствие подписки Researcher;
- недавний контакт другого рекрутёра;
- already in staff / not in staff.

## 21. Data View Integrity

| View | Требуемые данные |
|---|---|
| `iteractionList-edit-view` | bindings и отношения основного controller |
| `jobCandidate-iteraction-list-suggestion-view` | ФИО, фото, позиция, город |
| `openPosition-iteraction-list-picker-view` | проект, логотип, компания, status, priority, cities, remoteWork, closingDate, cost, positionType |
| `iteraction-list-type-view` | add/call/comment/status/notification/email/work fields |
| `employee-view` | кадровый side effect |
| `subscribeCandidateAction-view` | проверка подписки |

Правила:

- detached getter не читается без loaded-state;
- comment существующей записи догружается один раз;
- `ScreenViewIntegrityTest` должен давать `8/8 PASS`.

## 22. Производительность

### Реализовано

- преждевременная загрузка вакансий блокируется;
- comment догружается узким view;
- популярные типы — один агрегирующий service query;
- альтернативы — только для закрытой вакансии;
- chain query — один раз перед commit;
- after-commit side effects защищены флагами.

### Риски

- номер `max + 1` подвержен гонке;
- при выборе вакансии выполняется несколько запросов;
- `Employee` имеет отдельный commit;
- legacy null-handling неоднороден;
- notification types 0–5 не реализованы;
- `setSubscribe()` и `setCurrentUserName()` — no-op;
- presentation-helper не зарегистрирован и его UI-нормализация не работает автоматически.

## 23. SCSS

После исправления Hermes `themes/common` и symbolic links не используются. Shared-набор хранится в семи реальных файлах:

```text
modules/web/themes/<theme>/com.company.hunttech/edit-screen-shared-styles.scss
```

Темы:

- halo;
- havana;
- helium;
- hover;
- hunttech-modern;
- hunttech-modern-light;
- hunttech-modern-dark.

Все копии должны меняться синхронно.

Геометрия:

- sidebar 270 px;
- при viewport `<=1366px` — 250 px;
- toolbar/footer не менее 58 px;
- controls не менее 38 px;
- GroupBox `height=AUTO`;
- horizontal scroll запрещён.

## 24. Известный технический долг

| Область | Фактическое состояние |
|---|---|
| Presentation helper | наследник существует, но не зарегистрирован и не вызывается |
| Navigation | базовый controller создаёт пять legacy-пунктов |
| Quick placeholders | helper-код есть, но runtime handler не запускается |
| Номер | `max + 1`, не sequence |
| Employee | отдельный commit до основного commit |
| Notification 0–5 | no-op branches |
| `setSubscribe()` | no-op |
| `setCurrentUserName()` | читает имя, не сохраняет |
| `Iteraction.number` | требуется числовой префикс |
| `callClass` | runtime зависимость от meta-class/screen |
| Shared SCSS | семь реальных синхронных копий |

## 25. Полный сценарий commit

```text
Заполнение формы
→ validation
→ BeforeCommitChanges
   → comment null → ""
   → snapshot vacancy
   → chain interaction
   → optional Employee commit
→ StandardEditor/DataContext commit
→ AfterCommitChanges
   → vacancy news
→ BeforeClose commit-and-close
   → candidate.status
   → subscriber email
   → optional UiNotificationEvent
   → optional InternalEmailerEdit
→ close
```

## 26. Матрица side effects

| Триггер | Условие | Результат |
|---|---|---|
| candidate change | есть история | copy/warning dialog |
| vacancy change | новая пара | type filter `001` |
| vacancy change | Researcher без task | `RecrutiesTasksEdit` |
| vacancy change | closed | warning + alternatives |
| type change | setDateTime | addDate now |
| add-value | value not null | append comment |
| before commit | signStartProject | create/update Employee |
| before commit | signEndProject | update/create Employee |
| after commit | первый event | vacancy news |
| commit-and-close | candidate subscription | email current user |
| commit-and-close | notification type 6 | UI event |
| commit-and-close | needSendLetter | InternalEmailerEdit |
| subscribe button | confirmed | SubscribeCandidateAction editor |

## 27. Regression matrix

### Открытие

- new / existing / parent candidate;
- повторное открытие;
- отсутствующая фотография/логотип/FileStorage.

### Кандидат и вакансия

- candidate select/clear;
- история текущего/другого рекрутёра;
- open/closed vacancy;
- mismatch position/city;
- remote vacancy;
- Researcher subscribed/unsubscribed;
- only-my-subscriptions on/off.

### Тип

- addType 1/2/3;
- callForm;
- setDateTime;
- signComment;
- popular result 0/1–4/5+;
- проверить фактическое отсутствие placeholders в текущем runtime;
- проверить пятый legacy navigation-пункт.

### Commit

- normal;
- start/end project;
- Employee exists/missing;
- chain first/subsequent;
- vacancy news;
- subscriber email;
- notification type 6;
- candidate email preparation;
- cancel;
- save с закрытыми аккордеонами.

## 28. Обязательные проверки

```bash
git diff --check
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew test --tests '*IteractionList*Test' --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Runtime:

- `restart`, включающий `web-toolkit:deploy`;
- `/hrm/` = HTTP 200;
- functional smoke;
- visual smoke семи тем;
- Tomcat critical errors NONE;
- P1=0, P2=0.

## 29. Сохранённые контракты

Без отдельной задачи запрещено менять:

- entity / DB / Liquibase;
- screen ID;
- containers/loaders/JPQL/views;
- component ID/bindings/validators;
- `InteractionService` и месячный период;
- exact-object quick listener;
- dynamic field rules;
- Employee side effects;
- vacancy news;
- notifications/email;
- standard save/cancel.

## 30. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Документация синхронизирована с commit Hermes `078ba63c...`: `IteractionListEdit` зафиксирован как единственный зарегистрированный screen-controller, а `IteractionListEditAccordionNavigation` — как неактивный helper. Скорректированы фактические navigation и quick-action сценарии. |
| 2026-07-27 | Добавлена подробная бизнес-логика: lifecycle, loaders, candidate/vacancy rules, dynamic fields, subscriptions, Employee, chain interaction, notifications, email, риски и regression matrix. |
| 2026-07-27 | Экран перепроектирован: двухпанельный XML, аккордеоны и блок быстрых действий. |
| 2026-07-27 | Форма приведена к общему UI-контракту HRM HuntTech. |
