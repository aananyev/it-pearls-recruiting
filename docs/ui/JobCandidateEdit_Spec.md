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

- Открытие кандидата → загружается основной view → слева показывается профиль, справа рабочие вкладки.
- Первое открытие тяжёлой вкладки → устанавливаются обязательные параметры loaders → данные загружаются один раз.
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

Визуальная доработка не меняет Java-контроллер, сущности, component ID, data bindings, actions, invoke, loaders, views и JPQL.

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

- ленивое открытие тяжёлых вкладок сохраняется;
- обязательные параметры loaders не изменяются;
- визуальный слой не выполняет дополнительные запросы;
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
- колонка подписей уменьшена до 118 px, поэтому поля расположены ближе к labels;
- вторая колонка и корневые Vaadin-компоненты растягиваются на всю доступную ширину;
- поля `firstNameField`, `middleNameField`, `secondNameField`, `personPositionField` и `currentCompanyField` визуально увеличены более чем вдвое относительно прежней ширины по содержимому;
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

---

## 6. Профилирование открытия формы

Диагностический профайлер включается только системным свойством:

```text
-Dhrm.jobCandidateEdit.performance.enabled=true
```

При выключенном свойстве бизнес-логика и последовательность вызовов не меняются. При включении в журнал записываются строки `JOB_CANDIDATE_EDIT_PERF` с `openId`, кандидатом, фазой, длительностью, потоком и статусом.

Измеряемые участки:

- `onInit.loaderGuards` и регистрация listener вкладок;
- `framework.autoLoadGap` — `@LoadDataBeforeShow`, `jobCandidateDl` и основной entity view;
- каждый синхронный шаг `onBeforeShow`, включая рейтинг, фотографию, последнее взаимодействие и проверку ролей;
- инициализация каждой вкладки;
- шаги `onAfterShow` и полное время `screen.visible.total`;
- фоновая обработка навыков дополнительно анализируется через JFR.

Инструменты:

- `JobCandidateEditPerformanceProbeTest` — unit-тест структурированного профайлера;
- `test_job_candidate_edit_performance_report.py` — тест агрегатора;
- `collect_job_candidate_edit_profile.sh` — сбор JFR, журналов, CSV и Markdown;
- `job_candidate_edit_performance_report.py` — расчёт среднего, P50, P95, максимума и доли фазы от времени открытия.

Профилирование не должно использоваться постоянно на production: после получения отчёта system property необходимо удалить и перезапустить Tomcat.

---

## 7. Контроль качества и развертывание

Обязательные команды:

```bash
git diff --check
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ручная проверка:

- HTTP 200 для `/hrm`;
- открытие существующего и нового кандидата;
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
| 2026-07-14 | Добавлены диагностическое профилирование жизненного цикла JobCandidateEdit, unit-тесты, JFR-сборщик и генератор отчёта по времени открытия формы. |
| 2026-07-14 | Увеличены и приближены к подписям поля ФИО, должности и компании; шрифт SuggestionField синхронизирован с остальными полями вкладки «Основное» во всех темах. |
| 2026-07-14 | Исправлено фактическое отображение варианта 3: возвращены accordion-заголовки, растянуты GridLayout и поля, исправлены фон sidebar, повтор ФИО и кодировка звёзд рейтинга во всех темах. |
| 2026-07-14 | Реализована двухпанельная компоновка JobCandidateEdit, нижняя панель действий и локальный визуальный слой для подключённых тем. |
| 2026-07-14 | Сохранены XML-контракты, data bindings, actions, loaders и бизнес-логика экрана. |
