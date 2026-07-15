# JobCandidateEdit — спецификация экрана HRM HuntTech

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

`JobCandidateEdit` — основная рабочая карточка кандидата в HRM HuntTech. Экран объединяет персональные и профессиональные сведения, контакты, позиции и вакансии, взаимодействия, резюме и файлы, социальные сети, комментарии и историю записи.

Визуальная компоновка должна позволять рекрутеру постоянно видеть краткий профиль кандидата и одновременно работать с детальными данными, не переходя на отдельные экраны и не теряя контекст подбора.

### 2. Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается:

- из `JobCandidateBrowse` при создании или редактировании кандидата;
- из экранов подбора кандидатов;
- из связанных рекрутинговых сценариев и lookup-компонентов.

Основная навигация выполняется через существующий `tabSheetSocialNetworks`. Состав вкладок сохранён:

- `tabMain` — основные данные;
- `tabContactInfo` — контакты;
- `tabPositions` — позиции и вакансии;
- `tabIteraction` — взаимодействия;
- `tabResume` — резюме и файлы;
- `tabSocialNetworks` — социальные сети;
- `commentsTab` — комментарии;
- `tabHistory` — история.

Кнопка `openPositionMasterBrowseButton` открывает HR-Мастер для текущего кандидата. Кнопка `moreActionsPopUpButton` содержит существующие действия блокировки и подписки.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие кандидата → перед `InitEvent` из стартового view исключается коллекция `iteractionList` → слева показывается профиль, справа рабочие вкладки без предварительной материализации истории взаимодействий.
- Первое открытие вкладки «Взаимодействия» → существующий `ensureInteractionsLoaded()` выполняет узкий запрос → сущности merge-ятся в экранный `DataContext` и отображаются в штатной таблице.
- Изменение поля → данные остаются в штатном `DataContext` → перед сохранением выполняется существующая валидация.
- «Сохранить и закрыть» → выполняется `windowCommitAndClose` → кандидат сохраняется и экран закрывается.
- «Отмена» → выполняется `windowClose` → применяется стандартный сценарий закрытия CUBA.
- «Еще» → открывается существующее popup-меню → выполняются прежние handlers.

---

## 1. Технический контракт экрана

| Параметр | Значение |
|---|---|
| Screen ID | `hunttech_JobCandidate.edit` |
| Controller | `com.company.hunttech.web.screens.jobcandidate.JobCandidateEdit` |
| XML descriptor | `job-candidate-edit.xml` |
| Edited entity | `JobCandidate` |
| Edited container | `jobCandidateDc` |
| Платформа | CUBA Platform 7.3 |
| Корневой style name | `job-candidate-editor` |

Визуальная доработка не меняет сущности, component ID, data bindings, actions, invoke, loaders, views и JPQL.

Этап 1 прогрессивной загрузки не изменяет XML-дескриптор и контроллер. Компонент `JobCandidateInitialViewOptimizer` выполняется после штатной dependency injection и до `InitEvent`, копирует назначенный `jobCandidateDl` view со всеми fetch mode и вложенными views и исключает только свойство `iteractionList`.

Обязательные compatibility-компоненты контроллера:

| ID | XML-тип | Назначение |
|---|---|---|
| `lastProjects` | `groupBox` | сохранение существующего `@Inject`-контракта |
| `dictionatysTavlesHBox` | `grid` | сохранение существующего `@Inject`-контракта |
| `candidateNavigation` | `vbox` | сохранение существующих invoke-методов навигации |

---

## 2. Модель данных и загрузка

| Контейнер | Назначение |
|---|---|
| `jobCandidateDc` | редактируемый кандидат |
| `jobCandidateCandidateCvsDc` | резюме кандидата |
| `jobCandidateIteractionDc` | взаимодействия кандидата |
| `jobCandidateSocialNetworksDc` | социальные сети |
| `lastProjectDc` | история рассмотрения по вакансиям |
| `suggestOpenPositionDc` | подходящие вакансии |

Правила:

- `iteractionList` исключён только из runtime-view первичной загрузки `jobCandidateDl` и не материализуется до первого открытия `tabIteraction`;
- `JobCandidateInitialViewOptimizer` сохраняет остальные свойства исходного view, их fetch mode, вложенные views и режим partial entities;
- существующий `ensureInteractionsLoaded()` использует view `iteractionList-job-candidate`, merge-ит результат в экранный `DataContext` и заполняет прежний `jobCandidateIteractionDc`;
- `candidateCv` и `socialNetwork` на этом этапе оставлены в стартовом view для изолированного измерения эффекта;
- component ID, loader ID, actions, invoke, JPQL и бизнес-правила не изменены;
- таблицы продолжают использовать прежние dataContainer и actions.

---

## 3. Визуальная компоновка

```text
jobCandidateMainLayout
├── jobCandidateSidebar
│   ├── фотография
│   ├── ФИО
│   ├── рейтинг и процент заполнения
│   ├── город / компания / резюме
│   ├── email / телефон / Telegram
│   ├── растягиваемое свободное пространство
│   └── HR-Мастер
└── jobCandidateWorkspace
    ├── верхняя панель: служебные данные и «Еще»
    ├── горизонтальные вкладки
    ├── видимые accordion-заголовки разделов
    └── нижняя панель: «Сохранить и закрыть», «Отмена»
```

### Левая панель

- ширина — 312 px, на экранах до 1366 px — 286 px;
- тёмный фон задаётся самому `jobCandidateSidebar` и его Vaadin slot-обёртке;
- фотография отображается круглой и не искажается;
- ФИО имеет размер 24 px;
- текст карточек имеет размер 16 px и переносится по словам;
- HR-Мастер прижат к нижней границе;
- повторный вывод ФИО скрывается только стилем, component ID остаётся доступным контроллеру.

### Правая область

- workspace занимает всю оставшуюся ширину;
- «Еще» находится справа сверху;
- основные действия находятся справа снизу;
- вкладки сохраняют прежние ID и порядок;
- фон и границы одинаково определены для Halo, Hover, Havana и Helium.

### Accordion-заголовки

XML содержит контейнеры `job-candidate-accordion-header` и `job-candidate-accordion-content`. Заголовок раздела отображается над содержимым, слева выводится маркер раскрытого состояния, а заголовок и содержимое образуют единую карточку.

Accordion-слой не удаляет и не подменяет штатную навигацию `TabSheet`. Поведение полей, таблиц, loaders и действий не меняется.

### Вкладка «Основное»

- `personalDataBlock` и `professionalDataBlock` занимают по 50% доступной ширины;
- родительский `jobCandidateMainSectionContent` использует flex-компоновку;
- промежуток между блоками — 16 px;
- внутренний `GridLayout` принудительно растягивается на 100%;
- колонка подписей имеет ширину 118 px, поэтому поля расположены близко к labels;
- slot-компоненты `GridLayout` растягиваются единым локальным правилом без отдельных селекторов второй колонки;
- поля `firstNameField`, `middleNameField`, `secondNameField`, `personPositionField` и `currentCompanyField` занимают всю доступную ширину;
- шрифт `SuggestionField` ФИО установлен 16 px — такой же, как у `jobCityCandidateField` и остальных полей;
- высота полей — 38 px;
- component ID, properties, actions, queries и search executors не изменяются.

### Вкладка «Контакты»

- основные и дополнительные контакты занимают равные доли;
- подписи имеют фиксированную ширину;
- поля занимают оставшееся пространство;
- `radioButtonGroup` сохраняет прежнюю бизнес-логику и привязку `priorityContact`.

---

## 4. Actions и неизменяемые контракты

| Компонент | Контракт |
|---|---|
| `windowCommitAndCloseButton` | action `windowCommitAndClose` |
| кнопка отмены | action `windowClose` |
| `moreActionsPopUpButton` | прежний popup и handlers |
| `fileImageFaceUpload` | прежняя загрузка фотографии |
| поля ФИО | прежние properties, search executors и listeners |
| `currentCompanyField` | прежние lookup/open/create действия |
| `jobCandidateIteractionListTable` | прежние actions, columns и handlers |
| `jobCandidateCandidateCvTable` | прежние actions, columns и handlers |
| `socialNetworkTable` | прежний editor и generators |
| комментарии | прежние поле ввода, отправка и ответы |

---

## 5. Стили и поддержка тем

Общий mixin:

```scss
@mixin job-candidate-editor-theme
```

Все правила ограничены:

```scss
.job-candidate-editor
```

Одинаковый файл `job-candidate-editor.scss` используется в темах:

- Halo;
- Hover;
- Havana;
- Helium.

Глобальные `.v-table`, `.v-label`, `.v-button` и `.v-tabsheet` вне `.job-candidate-editor` не изменяются.

Для рейтинга используются CSS Unicode escapes (`\2605`, `\2606`), чтобы звёзды не повреждались при сборке SCSS.

### Оптимизация локального SCSS

По результатам завершённого анализа клиентского рендеринга сохранён безопасный рефакторинг локального визуального слоя:

- универсальное правило для всех потомков `.job-candidate-editor` заменено перечнем локальных layout-классов;
- substring selector рейтинга заменён явным списком rating-классов;
- правила sidebar сведены к `job-candidate-sidebar-card`;
- для `job-candidate-form-grid` удалены селекторы `td:nth-child(2)` и глубокие цепочки до внутренних полей;
- slot-компоненты основной формы растягиваются единым локальным правилом;
- повторяющиеся комбинации `width/min-width/max-width` сокращены там, где геометрия уже задаётся XML или flex layout;
- критические `!important` сохранены только для inline-размеров Vaadin и конфликтующих правил тем.

Временные performance-пробы, JFR-сборщики и performance-тесты после завершения анализа удалены. Штатный lifecycle экрана не зависит от диагностического system property.

---

## 6. Контроль качества и развертывание

Обязательные команды:

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ручная проверка:

- HTTP 200 для `/hrm`;
- открытие существующего и нового кандидата;
- до первого открытия `tabIteraction` отсутствует загрузка коллекции взаимодействий через initial view;
- первое открытие `tabIteraction` загружает все штатные строки, повторное переключение не создаёт дубликаты;
- создание, редактирование и удаление взаимодействия сохраняют прежнее поведение;
- сохранение и отмена;
- меню «Еще»;
- фотография и HR-Мастер;
- accordion-заголовки на каждой вкладке;
- одинаковый шрифт полей ФИО, города, должности и компании;
- ширина полей на вкладках «Основное» и «Контакты»;
- рейтинг без повреждённых символов;
- все таблицы, actions и ленивые loaders;
- темы Halo, Hover, Havana и Helium.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-15 | Этап 1 прогрессивной загрузки: runtime-view `jobCandidateDl` копируется без `iteractionList`; взаимодействия загружаются существующим методом при первом открытии вкладки, а XML и контроллер сохранены без изменений. |
| 2026-07-15 | Завершено performance-тестирование: удалены временные runtime-пробы, performance-тесты, JFR/лог-скрипты и диагностическое system property; оптимизированный SCSS сохранён. |
| 2026-07-15 | Выполнен этап 1 клиентской оптимизации: удалён универсальный selector потомков, упрощены цепочки Vaadin-селекторов и сокращены принудительные CSS-ограничения без изменения XML и бизнес-логики. |
| 2026-07-14 | Добавлены диагностическое профилирование жизненного цикла JobCandidateEdit, unit-тесты, JFR-сборщик и генератор отчёта по времени открытия формы. |
| 2026-07-14 | Увеличены и приближены к подписям поля ФИО, должности и компании; шрифт SuggestionField синхронизирован с остальными полями вкладки «Основное» во всех темах. |
| 2026-07-14 | Исправлено фактическое отображение варианта 3: возвращены accordion-заголовки, растянуты GridLayout и поля, исправлены фон sidebar, повтор ФИО и кодировка звёзд рейтинга во всех темах. |
| 2026-07-14 | Реализована двухпанельная компоновка JobCandidateEdit, нижняя панель действий и локальный визуальный слой для подключённых тем. |
| 2026-07-14 | Сохранены XML-контракты, data bindings, actions, loaders и бизнес-логика экрана. |
