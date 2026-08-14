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

Оформление аккордеонов повторяет подтверждённую геометрию `SettingsWindow`. Фактический XML-класс `user-ai-profile-section` дополнительно оформляется локальным селектором внутри корня `.iteraction-list-editor`, поэтому внешний вид контролируется `IteractionListEdit` и не распространяется на другие экраны. Одинаковый CSS-контракт синхронизирован во всех семи темах.

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

- открытие нового взаимодействия → контроллер заполняет номер, дату и текущего рекрутёра → пользователь получает готовую форму;
- открытие формы → секция «Взаимодействие» раскрыта, «Комментарий» и «Популярные взаимодействия» свёрнуты → пользователь сразу видит основные поля;
- раскрытие или сворачивание секции → меняется только presentation state `GroupBoxLayout` → значения и lifecycle не затрагиваются;
- выбор кандидата с фотографией → сохраняется прежний `ContainerValueSource` → `OvaFallbackImage` отображает фотографию круглой;
- выбор кандидата без фотографии → существующая Java-логика и `fallbackThemePath` указывают на `icons/no-programmer.jpeg` → sidebar не содержит пустого изображения;
- выбор вакансии → сохраняются проверки закрытия, подписки, статуса, приоритета и логотипа → sidebar обновляет вакансию;
- выбор типа взаимодействия → Java переключает `buttonCallAction`, `addString`, `addDate` или `addInteger` → дополнительное значение отображается следующей строкой под типом;
- изменение rating → Java сохраняет прежнее оформление и правила → оценка отображается в форме и sidebar;
- сохранение → выполняются прежние BeforeCommit/AfterCommit/BeforeClose обработчики → данные и связанные процессы изменяются как до reflow;
- смена темы → локальный mixin `iteraction-list-accordion-navigation-theme` применяет ту же геометрию, что и `SettingsWindow` → функциональные контракты не меняются.

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

Каждая рабочая секция сохраняет штатный CUBA-контракт `GroupBoxLayout`:

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

- найденный тип → активная зелёная кнопка с исторической подписью «N. Название»
  (`N` — номер частоты, `Название` — `Iteraction.iterationName`) и точным
  объектом `Iteraction` в listener;
- отсутствующий тип → disabled-кнопка `Нет данных` без listener.

Подпись кнопки всегда центрируется и принудительно остаётся видимой в пределах
своей равной ячейки. В disabled-позиции текст `Нет данных` имеет контрастный
цвет, а локальный стиль `.iteraction-list-popular-button` не меняет обработчик,
список или данные взаимодействия.

Цвет кнопок и их подписи зафиксированы по исторической реализации 2024 года:
зелёная поверхность `#008000` с белой подписью, скругление `10px` и светлая
рамка `rgba(81, 255, 0, 0.55)`; hover затемняет фон до `#006400`. Остальное
оформление (высота 64px, равные ячейки 5 × 20%, до трёх строк caption)
сохраняется без изменений.

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

Локальный CSS повторяет подтверждённые параметры `SettingsWindow`: радиус `7 px`, граница `1 px`, лёгкая поверхность, отступ между секциями `10 px`, вертикальный padding заголовка `9 px` и насыщенность `600`. Селекторы `.user-ai-profile-section` и `.v-panel-caption` применяются только внутри `.iteraction-list-editor`, поэтому изменение не влияет на `SettingsWindow` и другие экраны.

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

## 6. Локальный SCSS

Во всех семи темах аккордеон оформляется одинаковым локальным mixin:

```text
modules/web/themes/<theme>/com.company.hunttech/iteraction-list-accordion-navigation.scss
```

Mixin оформляет фактический XML-класс через локальный селектор `.iteraction-list-editor .user-ai-profile-section` и одновременно сохраняет собственный класс `.iteraction-list-accordion-section` для дальнейшего безопасного reflow. Параметры геометрии совпадают с `SettingsWindow`, но правила не выходят за root экрана. Собственные стили полей, footer, toolbar и sidebar остаются в `iteraction-list-editor.scss`. Глобальные Vaadin-селекторы не добавляются.

Controller выполняет цикл ровно по пяти индексам.

Для реального результата:

```java
iteractionTypeField.setValue(interaction);
iteractionTypeField.focus();
```

1. HEAD branch и HEAD PR совпадают с переданным SHA.
2. Base PR = `master`, conflicts = NONE.
3. `git diff --check`.
4. `IteractionListAccordionCssContractTest` — `1/1 PASS`.
5. `IteractionListEditAccordionLayoutTest` — `5/5 PASS`.
6. `LeftSidebarAvatarComponentTest` — `2/2 PASS`.
7. Compile web и core tests.
8. `ScreenViewIntegrityTest` — `8/8 PASS`.
9. Data View Integrity — getters контроллера входят в `iteractionList-edit-view`.
10. `:app-web:buildScssThemes` — PASS для семи тем.
11. `clean assemble` — `BUILD SUCCESSFUL`.
12. Local deploy и HTTP `/hrm/` = `200`.
13. Functional smoke: последовательно заполнить кандидата, вакансию, тип, dynamic fields, rating, рекрутёра, способ связи и комментарий; проверить подписку, save/cancel.
14. Accordion smoke: свернуть и раскрыть каждый из трёх блоков, убедиться в сохранении введённых значений и отсутствии пустых горизонтальных областей.
15. Visual smoke семи тем: радиус, граница, поверхность, заголовок и интервалы аккордеонов соответствуют `SettingsWindow`; поля идут одной колонкой, horizontal scroll отсутствует.
16. Tomcat logs: новых critical errors NONE; P1 = 0; P2 = 0.

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
- подписи «N. Название» (номер частоты + `iterationName`), зелёный стиль 2024 года;
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
| 2026-07-25 | В семи темах добавлено локальное CSS-оформление `.iteraction-list-editor .user-ai-profile-section`, визуально соответствующее `SettingsWindow`; XML и Java не изменены, добавлен `IteractionListAccordionCssContractTest` |
| 2026-07-25 | Основные рабочие блоки преобразованы в сворачиваемые секции; `gridIterationData` переведён на одну колонку, все поля расположены друг под другом без изменения business/data-контрактов |
| 2026-07-25 | По итогам аудита переработанных форм `candidateImage` в левой панели заменён на `OvaFallbackImage` 104×104 px с fallback `icons/no-programmer.jpeg`; ID, binding и Java-инъекция `Image` сохранены |
| 2026-07-25 | Улучшена компоновка: sidebar сделан непрерывным по высоте, toolbar и footer перенесены в workspace, ширина sidebar уменьшена, поля выстроены по сценарию рекрутёра, геометрия синхронизирована в семи темах |
| 2026-07-25 | Выполнена строго визуальная адаптация `IteractionListEdit`: двухпанельная композиция, локальный namespace `.iteraction-list-editor`, карточки и theme-aware состояния семи тем |
