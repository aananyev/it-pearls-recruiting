# IteractionListEdit — спецификация экрана и бизнес-логики HRM HuntTech

> Screen ID: `hunttech_IteractionList.edit`  
> Entity: `hunttech_IteractionList` / `IteractionList`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`  
> Зарегистрированный controller: `IteractionListEdit`  
> Платформа: CUBA Platform 7.3-SNAPSHOT  
> Статус: каноническая living-спецификация

## 1. Назначение и бизнес-смысл (What & Why)

`IteractionListEdit` создаёт или редактирует факт взаимодействия рекрутёра с кандидатом в контексте вакансии. Экран объединяет ввод данных, контроль процесса подбора и ряд побочных бизнес-действий.

Запись фиксирует:

- кандидата;
- вакансию;
- ответственного рекрутёра;
- дату и номер взаимодействия;
- тип взаимодействия;
- дополнительное значение, зависящее от типа;
- оценку коммуникации;
- способ связи;
- комментарий;
- снимок приоритета и открытости вакансии;
- ссылку на предыдущее взаимодействие цепочки.

Сохранение может дополнительно:

1. изменить статус кандидата;
2. создать или обновить `Employee`;
3. создать автоматическую новость вакансии;
4. отправить email подписчику кандидата;
5. опубликовать UI-уведомление;
6. открыть форму подготовки письма кандидату;
7. открыть editor подписки на кандидата.

UI-рефакторинг не изменяет entity, JPQL, loaders, views, сервисные вызовы, required-правила и порядок lifecycle CUBA Platform.

## 2. UI Context & Navigation

### 2.1. Точки открытия

Экран открывается как editor `hunttech_IteractionList.edit` в сценариях:

- создание из списка взаимодействий;
- редактирование существующей записи;
- создание из карточки кандидата;
- создание с переданным `parentCandidate`;
- продолжение цепочки кандидат–вакансия;
- создание после копирования предыдущей вакансии кандидата.

### 2.2. Иерархия экрана

```text
IteractionListEdit
├── edit-sidebar
│   ├── candidateImage + projectLogoImage
│   ├── ФИО кандидата
│   ├── наименование вакансии
│   ├── статус вакансии
│   ├── приоритет вакансии
│   ├── label-navigation
│   ├── номер + дата
│   ├── компания / проект / стоимость / рейтинг
│   └── spacer
└── edit-workspace
    ├── toolbar
    ├── mostPopularQuickActions
    ├── vertical scroll
    │   ├── participantsAccordion — VBox
    │   ├── interactionAccordion — VBox
    │   ├── resultAccordion — VBox
    │   └── commentAccordion — VBox
    └── footer actions
```

Legacy component ID с суффиксом `Accordion` сохранены ради совместимости имён, но компоненты являются обычными `VBoxLayout`. В XML отсутствуют `groupBox`, `collapsable`, `collapsed` и `showAsPanel`.

### 2.3. Постоянные блоки ввода

| ID блока | Заголовок | Основные компоненты | Focus target |
|---|---|---|---|
| `participantsAccordion` | Кандидат и вакансия | `candidateField`, `vacancyFiels`, `onlyMySubscribeCheckBox` | `candidateField` |
| `interactionAccordion` | Тип и действие | `iteractionTypeField`, `buttonCallAction`, `addString`, `addDate`, `addInteger` | `iteractionTypeField` |
| `resultAccordion` | Оценка и коммуникация | `ratingField`, `recrutierField`, `communicationMethodField` | `ratingField` |
| `commentAccordion` | Комментарий | `commentField` | `commentField` |

Все четыре блока:

- постоянно видимы;
- расположены вертикально;
- имеют естественную высоту `AUTO`;
- не меняют высоту при клике по заголовку;
- имеют статический title и отдельный body;
- используют локальный класс `iteraction-list-flat-section`.

### 2.3.1. Единый визуальный контракт полей

Основные поля ввода в правой рабочей области используют общий stylename `edit-form-control`:

- `iteractionTypeField`;
- `ratingField`;
- `recrutierField`;
- `communicationMethodField`;
- `commentField`.

`candidateField` и `vacancyFiels` сохраняют специализированный `iteraction-list-primary-picker`, но итоговая SCSS-геометрия совпадает с `edit-form-control`: высота `38px`, единая рамка, фон, focus-state и фиксированная ширина action-кнопок. Это устраняет расхождения между `SuggestionPickerField`, `LookupPickerField`, `LookupField`, `TextField` и `TextArea` без изменения bindings, actions и validators.

### 2.4. Label-navigation

`IteractionListEdit` заменяет XML fallback labels четырьмя keyboard-доступными кнопками.

Клик выполняет только presentation-действия:

1. назначает `label-nav-item-active` выбранной кнопке;
2. назначает `iteraction-list-flat-section-active` выбранному блоку;
3. снимает active-style с остальных блоков и кнопок;
4. переводит focus в первое рабочее поле;
5. ScrollBox прокручивает focus target в видимую область.

Навигация не вызывает `setExpanded()`, loaders, сервисы, commit и не записывает значения entity.

### 2.5. Частые взаимодействия

`mostPopularQuickActions` находится над scroll-area и не является пунктом label-navigation.

Controller всегда создаёт ровно пять визуальных позиций:

- найденный тип → активная кнопка с наименованием `Iteraction.iterationName` и точным объектом `Iteraction` в listener;
- отсутствующий тип → disabled-кнопка `Нет данных` без listener.

Подпись кнопки всегда центрируется и принудительно остаётся видимой в пределах
своей равной ячейки. В disabled-позиции текст `Нет данных` имеет контрастный
цвет, а локальный стиль `.iteraction-list-popular-button` не меняет обработчик,
список или данные взаимодействия.

Высота быстрых кнопок составляет `64px`; caption занимает не более трёх строк
(`48px`) и обрезается внутри собственной кнопки, а полный текст остаётся в
стандартной подсказке `description`. Это исключает выход длинных названий за
границы панели и наложение на следующий блок формы.

Рейтинг остаётся результатом исходного агрегирующего запроса за календарный
месяц. Перед созданием кнопок сервис догружает узкий `iteraction-picker-view`
для уже выбранных идентификаторов, чтобы `iterationName` был доступен caption;
порядок и набор типов не меняются.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| Открытие новой записи | entity новая | назначаются номер, дата и текущий рекрутёр |
| Открытие существующей записи | entity сохранена | дата read-only, комментарий догружается узким view |
| Клик label-navigation | выбран пункт | focus и active-state переходят к соответствующему VBox-блоку |
| Выбор кандидата | кандидат задан | обновляется фото, проверяется история контактов |
| Выбор вакансии | вакансия задана | проверяются соответствие, закрытие, подписка и начало цепочки |
| Новая пара кандидат–вакансия | взаимодействий нет | типы ограничиваются группой `001` |
| Выбор типа | тип задан | настраиваются dynamic fields, action и required-комментарий |
| Quick action | позиция содержит тип | точный `Iteraction` устанавливается в `iteractionTypeField` |
| Quick placeholder | данных нет | кнопка disabled и не меняет DataContext |
| Before commit | стандартное сохранение | snapshot вакансии, chain interaction, кадровый side effect |
| After commit | первый event | автоматическая новость вакансии |
| Commit-and-close | применимы флаги типа | статус кандидата, email, notification, письмо кандидату |
| Подписаться | новая запись | подтверждение сохранения и отдельный editor подписки |
| Отмена | стандартное закрытие | `windowClose` |

## 4. Модель данных и bindings

### 4.1. Основные поля `IteractionList`

| Поле | Тип | Обязательность | Назначение |
|---|---|---:|---|
| `numberIteraction` | `BigDecimal` | нет | номер взаимодействия |
| `dateIteraction` | `Date` | нет | дата события |
| `candidate` | `JobCandidate` | `@NotNull` | кандидат |
| `vacancy` | `OpenPosition` | `@NotNull` | вакансия |
| `iteractionType` | `Iteraction` | UI required | тип взаимодействия |
| `recrutier` | `ExtUser` | `@NotNull` | ответственный рекрутёр |
| `rating` | `Integer` | UI required | оценка 0–4 |
| `communicationMethod` | `String` | нет | способ связи |
| `comment` | LOB `String` | условно | комментарий |
| `addDate` | `Date` | условно | дополнительная дата |
| `addString` | `String` | условно | дополнительный текст |
| `addInteger` | `Integer` | условно | дополнительное число |
| `currentPriority` | `Integer` | before commit | снимок приоритета |
| `currentOpenClose` | `Boolean` | before commit | снимок статуса вакансии |
| `chainInteraction` | `IteractionList` | before commit | предыдущее событие цепочки |

### 4.2. UI bindings

| Component ID | Binding |
|---|---|
| `candidateField` | `candidate` |
| `vacancyFiels` | `vacancy` |
| `iteractionTypeField` | `iteractionType` |
| `dateIteractionField` | `dateIteraction` |
| `numberIteractionField` | `numberIteraction` |
| `recrutierField` | `recrutier` |
| `ratingField` | `rating` |
| `communicationMethodField` | `communicationMethod` |
| `commentField` | `comment` |
| `addDate` | `addDate` |
| `addString` | `addString` |
| `addInteger` | `addInteger` |

### 4.3. Sidebar

| Component | Источник / поведение |
|---|---|
| `candidateImage` | `candidate.fileImageFace`, fallback `icons/no-programmer.jpeg` |
| `projectLogoImage` | `vacancy.projectName.projectLogo`, fallback `icons/no-company.png` |
| `iteractionCandidateNameLabel` | `candidate.fullName` |
| `iteractionVacancyNameLabel` | `vacancy.vacansyName` |
| `statusOfVacansyLabel` | `ОТКРЫТА` / `ЗАКРЫТА` |
| `alternativeVacancyLinkButton` | открытые альтернативы того же типа позиции |
| `currentPriorityLabel` | текстовый приоритет |
| `trafficLighterImage` | иконка приоритета |
| `companyLabel` | компания и подразделение проекта |
| `projectLabel` | проект |
| `closingDateVacancyLabel` | дата закрытия и просрочка |
| `outstaffingCostHBox` | видим при непустом `outstaffingCost` |
| `iteractionListNavigationTitle` | заголовок «Разделы формы» навигации (полоса-заголовок) |
| vacancy-карточка `iteraction-list-sidebar-card-title` | заголовок «Вакансия» (полоса-заголовок) |

Заголовки содержательных секций sidebar — «Разделы формы» (`iteraction-list-navigation-title`) и «Вакансия» (`iteraction-list-sidebar-card-title`) — оформлены как полоса-заголовок по контракту `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` §4.1: две горизонтальные inset-линии (белая сверху `rgba(255,255,255,1) 0 1px 0 0 inset`, светлая снизу `rgba(244,244,244,1) 0 -1px 0 0 inset`), разделитель снизу `border-bottom: 1px solid rgba(255,255,255,.14)`, полоса `rgba(255,255,255,.045)`, текст `#ffb11b` 15px/700, `min-height: 36px`, `padding: 7px 11px`; заголовок «Вакансия» растянут на ширину карточки (`margin: -14px -14px 12px`, верхние углы скруглены). SCSS-правила: `iteraction-list-visual-alignment.scss` (навигация) и `iteraction-list-accordion-navigation.scss` (карточка), идентичны во всех 7 темах.

## 5. Data containers, loaders и views

| ID | Тип | View / запрос |
|---|---|---|
| `iteractionListDc` | instance `IteractionList` | `iteractionList-edit-view` |
| `iteractionListDl` | instance loader | view контейнера |
| `iteractionTypesDc` | collection `Iteraction` | `iteraction-list-type-view` |
| `iteractionTypesLc` | collection loader | типы с динамическим параметром `number` |
| `openPositionDc` | collection `OpenPosition` | `openPosition-iteraction-list-picker-view` |
| `openPositionsDl` | collection loader | department + subscriber conditions |
| `usersDc` | collection `User` | `_minimal` |
| `usersDl` | collection loader | активные пользователи |

### 5.1. Защита загрузки вакансий

`@LoadDataBeforeShow` не должен загружать все вакансии до установки фильтров. `PreLoadListener` вызывает `preventLoad()`, пока `openPositionsReady=false`.

### 5.2. Data View Integrity

Обязательные views:

- `iteractionList-edit-view`;
- `jobCandidate-iteraction-list-suggestion-view`;
- `openPosition-iteraction-list-picker-view`;
- `iteraction-list-type-view`;
- `employee-view`;
- `subscribeCandidateAction-view`.

`ScreenViewIntegrityTest` должен завершаться результатом `8/8 PASS`.

## 6. Lifecycle

### 6.1. `InitEvent`

- строится label-navigation;
- устанавливаются fallback-изображения;
- блокируется преждевременная загрузка вакансий;
- сбрасываются параметры и флаги;
- скрываются dynamic fields;
- читаются screen options;
- формируются rating options и priority map.

### 6.2. Item change основного контейнера

Для новой entity:

- `number = InteractionService.getCountInteraction() + 1`;
- `date = new Date()`;
- вызывается legacy `setCurrentUserName()`.

`setCurrentUserName()` получает имя, но не сохраняет его в entity.

### 6.3. `BeforeShowEvent`

1. подключаются option providers вакансии;
2. скрывается action-кнопка;
3. запоминается кандидат;
4. включается фильтр подписок и загружаются вакансии;
5. восстанавливается состояние dynamic fields;
6. применяется `parentCandidate`;
7. строятся пять quick-action позиций;
8. дата разрешается к редактированию только новой entity.

### 6.4. `AfterShowEvent`

- новой entity назначается текущий рекрутёр;
- существующей entity один раз догружается `comment` узким view;
- listener кандидата запускает проверку истории контактов.

### 6.5. `BeforeCommitChangesEvent`

1. `comment=null` преобразуется в `""`;
2. сохраняется snapshot `currentPriority`;
3. сохраняется snapshot `currentOpenClose`;
4. определяется `chainInteraction`;
5. выполняется optional start/end employee side effect.

### 6.6. `AfterCommitChangesEvent`

Один раз:

- вызывается legacy `setSubscribe()`;
- создаётся автоматическая новость через `OpenPositionService`.

### 6.7. `BeforeCloseEvent`

При commit-and-close:

- числовой префикс `Iteraction.number` переносится в `candidate.status`;
- выполняются email/notification сценарии;
- при необходимости открывается `InternalEmailerEdit`.

## 7. Выбор кандидата

### 7.1. Фото

При наличии `fileImageFace` компонент привязывается к `candidate.fileImageFace`; иначе отображается fallback.

### 7.2. История контактов

`copyAndCheckCandidate()`:

- для истории текущего рекрутёра может предложить копирование предыдущей вакансии;
- при недавнем контакте другого рекрутёра показывает имя и дату предупреждением.

### 7.3. Копирование

`copyPrevionsItems()` загружает последнюю запись кандидата по максимальному номеру и переносит только вакансию.

## 8. Выбор вакансии

### 8.1. Проверка соответствия

Сравниваются:

- позиция кандидата и тип позиции вакансии;
- город кандидата и допустимые города;
- признак удалённой работы.

При расхождении пользователь может очистить вакансию либо продолжить.

### 8.2. Новый процесс

Для открытой вакансии считается количество взаимодействий пары:

- `0` → параметр типов `001` и предупреждение;
- больше `0` → ограничение снимается.

### 8.3. Закрытая вакансия

Для новой записи показывается диалог. При подтверждении выбора вакансия и визуальные индикаторы очищаются; отказ позволяет продолжить.

### 8.4. Researcher без подписки

При отсутствии активной `RecrutiesTasks` предлагается открыть `RecrutiesTasksEdit` с текущим пользователем, вакансией, текущей датой и датой будущего понедельника.

### 8.5. Sidebar-контекст

Обновляются компания, подразделение, проект, логотип, closing date, просрочка, статус, альтернативы, приоритет, иконка и стоимость аутстаффинга.

## 9. Фильтр «только мои подписки»

По умолчанию включён.

- включён → `subscriber=currentUser`;
- выключен → параметр удаляется;
- loader перезапускается;
- пустой список вызывает warning.

Фильтр меняет только options вакансии.

## 10. Тип взаимодействия и dynamic fields

| Настройка `Iteraction` | Влияние |
|---|---|
| `addFlag` | включает дополнительное значение |
| `addType` | дата / строка / число |
| `addCaption` | caption дополнительного поля |
| `callForm` | видимость action-кнопки |
| `callButtonText` | caption кнопки |
| `callClass` | динамически открываемая meta-class |
| `findToDic` | legacy-ветка открытия |
| `setDateTime` | автозаполнение `addDate` |
| `signComment` | required-комментарий |
| `number` | статус кандидата |
| `signStartProject` | начало кадрового проекта |
| `signEndProject` | завершение кадрового проекта |
| `workStatus` | статус сотрудника |
| `needSendLetter` | подготовка письма |
| `textEmailToSend` | шаблон письма |
| `notificationNeedSend` | включение notification |
| `notificationWhenSend` | момент notification |
| `notificationType` | аудитория |
| `signPriorityNews` | приоритет vacancy news |

### 10.1. Матрица add-fields

| `addFlag` | `addType` | Компонент | Required | Action |
|---:|---:|---|---:|---|
| true | 1 | `addDate` | да | скрыта |
| true | 2 | `addString` | да | скрыта |
| true | 3 | `addInteger` | да | скрыта |
| false | — | add-fields скрыты | нет | видима при `callForm=true` |

`setDateTime=true` заполняет пустой `addDate` текущим временем.

`signComment=true` делает `commentField` обязательным.

Изменение add-value дописывает в комментарий строку `<Название типа>: <значение>`.

## 11. Быстрые взаимодействия

### 11.1. Источник

```java
InteractionService.getMostPolularIteraction(userSession.getUser(), 5)
```

Сервис использует:

```text
текущий пользователь
→ текущая дата минус один календарный месяц
→ iteractionType is not null
→ group by iteractionType
→ order by count DESC
→ до пяти результатов
```

### 11.2. Визуальный контракт

Controller выполняет цикл ровно по пяти индексам.

Для реального результата:

```java
iteractionTypeField.setValue(interaction);
iteractionTypeField.focus();
```

Для пустого результата создаётся disabled-кнопка `Нет данных` без click listener.

Ряд имеет пять одинаковых expand ratio и визуально соответствует сетке `5 × 20%`.

## 12. Рейтинг

`ratingField` хранит значения 0–4:

| Значение | Отображение |
|---:|---|
| 0 | 1 звезда — Полный негатив |
| 1 | 2 звезды — Сомнительно |
| 2 | 3 звезды — Нейтрально |
| 3 | 4 звезды — Положительно |
| 4 | 5 звёзд — Отлично |

## 13. Подписка на кандидата

`subscribeButton` вызывает `onButtonSubscribeClick()`.

Для новой записи пользователь подтверждает сохранение, после чего открывается новая `SubscribeCandidateAction`. Для существующей записи editor подписки открывается сразу.

## 14. Commit и side effects

### 14.1. Snapshot вакансии

Перед commit:

- `currentPriority = vacancy.priority`;
- `currentOpenClose = vacancy.openClose`.

### 14.2. Chain interaction

Ищется последняя запись той же пары candidate–vacancy с непустым типом. Для первой записи `chainInteraction=null`.

### 14.3. Start/end project

При `signStartProject=true` создаётся либо обновляется `Employee` с датой начала, вакансией и `workStatus`.

При `signEndProject=true` создаётся либо обновляется `Employee` с датой завершения, вакансией и `workStatus`.

`Employee` коммитится отдельно внутри `BeforeCommitChangesEvent`. Это существующая транзакционная особенность и не изменяется UI-задачей.

### 14.4. Vacancy news

После commit вызывается `OpenPositionService.setOpenPositionNewsAutomatedMessage(...)`. Флаг `deleteTwiceEvent` предотвращает дубль.

### 14.5. Email и уведомления

Поддерживаются:

- email текущему пользователю при активной подписке на кандидата;
- `UiNotificationEvent` для `notificationType=6`;
- открытие `InternalEmailerEdit` при `needSendLetter=true` и наличии шаблона/email.

Ветки notification types 0–5 остаются legacy no-op.

## 15. SCSS и render-контракт

Локальный partial:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-flat-layout.scss
```

синхронно хранится в темах:

- halo;
- havana;
- helium;
- hover;
- hunttech-modern;
- hunttech-modern-light;
- hunttech-modern-dark.

Контракт:

- sidebar 312 px;
- `<=1366px` — 296 px;
- `<=1100px` — 284 px;
- status/priority — полноширинные строки;
- четыре VBox-карточки с header/body;
- active section — border и shadow цвета selection;
- controls не менее 38 px;
- horizontal scroll запрещён;
- глобальные `.v-panel`, `.v-label`, `.v-button`, `.v-tabsheet` не переопределяются.

## 16. Производительность и известные риски

### Реализовано

- преждевременная загрузка вакансий блокируется;
- comment догружается узким view;
- популярные типы — один агрегирующий service query;
- alternatives загружаются только для закрытой вакансии;
- chain query выполняется один раз перед commit;
- after-commit side effects защищены флагами.

### Технический долг

| Область | Состояние |
|---|---|
| Номер | `max + 1`, возможна гонка |
| Employee | отдельный commit до основного commit |
| Notification 0–5 | no-op branches |
| `setSubscribe()` | no-op |
| `setCurrentUserName()` | читает имя, не сохраняет |
| `Iteraction.number` | требуется числовой префикс |
| `callClass` | runtime зависимость от meta-class/screen |
| `IteractionListEditAccordionNavigation` | незарегистрированный legacy helper, runtime не участвует |

## 17. Regression matrix

### Открытие и layout

- новый / существующий / parent candidate;
- повторное открытие;
- все четыре VBox-блока видимы одновременно;
- заголовки не сворачивают содержимое;
- четыре пункта label-navigation;
- active nav и active section синхронны;
- focus/scroll для каждого блока;
- viewport 1700×950 и 1366×768;
- семь тем;
- отсутствие horizontal scroll.
- `candidateImage` и `projectLogoImage` имеют одинаковый размер `96 × 96`, не обрезаются и симметрично центрированы в верхней части sidebar;
- fallback-изображения визуально заполняют круги без избыточных внутренних полей;
- карточка номера и даты расположена сразу под профильным блоком кандидата, а date/time control целиком помещается внутри sidebar;
- candidate/vacancy и rating/recruiter сохраняют две видимые колонки `50/50` при длинном значении слева;
- candidate/vacancy используют одинаковый шрифт `15px`; option icon/image вакансии и рекрутёра имеют `20 × 20` и не пересекаются с текстом;
- checkbox «Показывать только мои подписки» выровнен с подписью и допускает безопасный перенос текста;
- `resultAccordion` и `commentAccordion` не перекрываются при любом содержимом GridLayout;
- `iteractionTypeField`, `ratingField`, `recrutierField`, `communicationMethodField`, `commentField`, `candidateField` и `vacancyFiels` имеют единую геометрию.

### Quick actions

- 0 результатов → пять disabled placeholders;
- 1–4 результата → реальные кнопки + placeholders;
- 5+ результатов → пять реальных кнопок;
- клик устанавливает точный `Iteraction`;
- placeholders не имеют listener.

### Кандидат и вакансия

- select/clear candidate;
- фото/fallback;
- open/closed vacancy;
- alternatives;
- mismatch position/city;
- remote vacancy;
- Researcher subscribed/unsubscribed;
- only-my-subscriptions on/off.

### Тип и commit

- addType 1/2/3;
- callForm;
- setDateTime;
- signComment;
- start/end project;
- chain first/subsequent;
- vacancy news;
- subscriber email;
- notification type 6;
- candidate email preparation;
- save/cancel.

## 18. Обязательные проверки

```bash
git diff --check
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
  --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Runtime:

- clean local deploy;
- `/hrm/` = HTTP 200;
- functional smoke;
- visual smoke семи тем;
- Tomcat critical errors NONE;
- P1=0, P2=0.

## 19. История изменений

| Дата | Изменение |
|---|---|
| 2026-08-08 | Заголовкам разделов sidebar «Разделы формы» (`iteractionListNavigationTitle`, класс `iteraction-list-navigation-title`) и «Вакансия» (`iteraction-list-sidebar-card-title`) добавлены две горизонтальные inset-линии полосы (белая сверху, светлая снизу) + разделитель `border-bottom`, как у заголовков секций OpenPositionEdit / caption инфокарточки — контракт §4.1; SCSS: `iteraction-list-visual-alignment.scss` и `iteraction-list-accordion-navigation.scss` во всех 7 темах; добавлен контрактный тест `IteractionListVisualAlignmentTest.sectionTitlesHaveTwoInsetLinesLikeInfoCaption`. |
| 2026-07-29 | Реальный visual smoke под `okozhevnikova` выявил вертикальное переполнение длинных caption: высота quick-action увеличена до `64px`, caption ограничен тремя строками внутри кнопки. |
| 2026-07-29 | Усилен контраст disabled-подписей `Нет данных` в семи темах, чтобы пустая позиция не выглядела бесконтентной. |
| 2026-07-29 | Реальная локальная проверка выявила пустые captions: после агрегирующего запроса сервис догружает только `iteraction-picker-view`, сохраняя порядок рейтинга и обработчик quick action. |
| 2026-07-29 | Подписи пяти частых взаимодействий зафиксированы как видимые captions во всех локальных темах; Java-обработчик и бизнес-контракт не изменялись. |
| 2026-07-28 | Унифицирована типографика candidate/vacancy, provider-пиктограммы отделены от текста, fallback-изображения увеличены внутри кругов, checkbox подписок выровнен; service-card перенесена под профиль кандидата и защищена от переполнения date/time-контролом. |
| 2026-07-28 | Двухколоночные GridLayout закреплены как `50/50` на уровне absolute-positioned slot-ов CUBA 7.3, корням сеток сохранена ненулевая высота, локальная ширина sidebar синхронизирована с Vaadin slot, внутренний date/time layout служебной карточки ограничен шириной родителя, оба верхних `OvaFallbackImage` выровнены до `96 × 96`; добавлены regression-критерии видимости vacancy/recruiter и отсутствия переполнения. |
| 2026-07-28 | Уточнён визуальный слой: общий `edit-screen-shared-styles` применяется как базовый SCSS-контракт, локальный `iteraction-list-visual-alignment` финально ограничивает карточки, GridLayout, picker/action-кнопки, поля результата и комментарий; добавлен `edit-form-control` для однотипных полей. |
| 2026-07-28 | XML, active-controller и SCSS синхронизированы с точным render-контрактом: четыре обычных VBox-блока, четыре navigation target, отсутствие expanded state и ровно пять quick-action позиций. |
| 2026-07-27 | Зафиксирована подробная бизнес-логика: lifecycle, loaders, candidate/vacancy rules, dynamic fields, subscriptions, Employee, chain interaction, notifications, email и риски. |
| 2026-07-27 | Экран переведён на двухпанельную компоновку HRM HuntTech. |
