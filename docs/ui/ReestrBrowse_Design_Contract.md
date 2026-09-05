# КОНТРАКТ ДИЗАЙНА ТАБЛИЧНЫХ (BROWSE/REESTR) ФОРМ HRM HuntTech

> **Статус**: обязательный нормативный документ для всех browse/reestr-экранов HRM HuntTech (CUBA 7.3, Vaadin 8).
> **Эталоны**: `JobCandidateReestrBrowse` (`job-candidate-reestr.xml`) и `OpenPositionReestrBrowse` (`open-position-reestr-browse.xml`).
> **База обзора**: master, SHA `7c1c9b7a` (worktree hrm-hermes2, ветка agent/hermes2-dev).
> **Происхождение**: составлен субагентом-аналитиком (фактура «что есть в коде») и субагентом UI/UX-дизайнером (правила «как должно быть»); расхождения разрешены по факту кода. Каждое правило содержит ссылку `файл:строка` эталона.

---

## 1. Область действия

1.1. Контракт обязателен для **всех** browse/reestr-экранов приложения: любой новый экран
типа «реестр с детализацией» обязан следовать анатомии из п.2–7. Примеры действующих
реестров, уже построенных по этому контракту:
- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-reestr.xml` (эталон)
- `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-reestr-browse.xml` (эталон)
- `modules/web/src/com/company/hunttech/web/screens/city/city-reestr-browse.xml` (адаптирован, отклонения — п.8)
- `modules/web/src/com/company/hunttech/web/screens/person/person-reestr-browse.xml` (адаптирован, отклонения — п.8)
- `modules/web/src/com/company/hunttech/web/screens/position/position-reestr-browse.xml` (адаптирован, отклонения — п.8)

1.2. **Исключения** из контракта допускаются только с явным обоснованием в описании PR
(например, экран без сущности-детали: чистый lookup-список или дерево без Master-Detail).
Исключение оформляется комментарием в XML-дескрипторе экрана.

1.3. Контракт не распространяется на Edit-формы (для них действует
`docs/ui/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` и эталон IteractionListEdit),
но использует общую с ними SCSS-базу `edit-screen-shared-styles.scss`
(`modules/web/themes/halo/com.company.hunttech/edit-screen-shared-styles.scss:1-7`).

1.4. **Namespace-контракт (питфолл, задокументированный)**: все реестровые классы живут
под корневым классом `job-candidate-editor` (`job-candidate-editor.scss:13-16`) и/или
`edit-*` в shared-partial (`edit-screen-shared-styles.scss:7`). Новый реестр **обязан**
нести на layout эти классы (п.2.1); кастомные классы `*-reestr-*` без опоры на общие
SCSS-классы не дают ничего — стили не применятся ни в одной теме.

---

## 2. Анатомия экрана (обязательные блоки)

2.1. **Корневой layout** обязан нести `stylename="job-candidate-editor edit-screen-layout"`,
`expand="splitMainLayout"`, `spacing="false"`, `width/height="100%"`:
- `job-candidate-reestr.xml:51`
- `open-position-reestr-browse.xml:69`
- простые реестры-носители: `city-reestr-browse.xml:22`, `person-reestr-browse.xml:48`, `position-reestr-browse.xml:20`

2.2. **Split View (Master-Detail)**: горизальный `hbox id="splitMainLayout"` с
`stylename="job-candidate-main-layout edit-screen-layout"`, `spacing="true"` и
`expand=` на правой панели (workspace):
- `job-candidate-reestr.xml:53` (expand="candidatesTableBox")
- `open-position-reestr-browse.xml:72` (expand="positionsTableBox")

2.3. **Sidebar — ровно 312px**: `vbox width="312px" height="100%"
stylename="job-candidate-sidebar edit-sidebar"`, внутри — вертикальный `scrollBox`:
- `job-candidate-reestr.xml:56-58`
- `open-position-reestr-browse.xml:75-76`
- SCSS-закрепление ширины: `job-candidate-editor.scss:63-75` (`width/min/max: 312px !important`
  и для слота `.v-slot-job-candidate-sidebar`), тёмный градиентный фон сайдбара
  `#172638 → #0f1b28` (`job-candidate-editor.scss:68-69`), padding `24px 20px 18px`
  (`job-candidate-editor.scss:77`).
- Базовая ширина `edit-sidebar` = 270px (`edit-screen-shared-styles.scss:32-38`) — реестр
  переопределяет её на 312px классом `job-candidate-sidebar`; оба класса обязательны в паре.

2.4. **Шапка профиля сайдбара** (`job-candidate-profile-header edit-sidebar-visual`,
`align="TOP_CENTER"`): аватар + блок идентичности (`edit-sidebar-identity`):
- `job-candidate-reestr.xml:62-80`
- `open-position-reestr-browse.xml:80-114`
- Аватар: компонент `ovaFallbackImage` **120×120px**, `ovalWidth/ovalHeight="120px"`,
  `stylename="job-candidate-avatar"`, `scaleMode="SCALE_DOWN"`, обязательный
  `fallbackThemePath` (`job-candidate-reestr.xml:64-70`, `open-position-reestr-browse.xml:81-87`).
  Геометрия задаётся SCSS и переопределять её в экране нельзя: `border-radius: 50%`,
  рамка `4px solid rgba(255,255,255,.96)`, тень `0 8px 24px rgba(0,0,0,.30)`,
  `object-fit: cover` (`job-candidate-editor.scss:183-209`).
  Контракт fallback-изображения кандидата: `docs/ui/ReestrBrowseFallbackNoCandidate_Spec.md:21-22,40,49`
  (силуэт человека `icons/no-candidate.png`, контроллер не перетирает fallback чужим путём).
- Иерархия подписей идентичности (три уровня типографики, все `align="MIDDLE_CENTER"`,
  `htmlEnabled="true"`, `width="100%"`):
  - заголовок: `stylename="edit-sidebar-title h2 candidate-sidebar-fullname bold"`
    (`job-candidate-reestr.xml:74`, `open-position-reestr-browse.xml:89-94`);
  - подзаголовок: `stylename="edit-sidebar-subtitle h4 candidate-sidebar-position bold"`
    (`job-candidate-reestr.xml:76`, `open-position-reestr-browse.xml:95-100`);
  - третья строка (город/локация): `stylename="edit-help candidate-sidebar-city [bold]"`
    (`job-candidate-reestr.xml:78`, `open-position-reestr-browse.xml:101-112`).
- **Логотип — ровно один раз** (в аватаре шапки). Дублирующая секция «ЛОГОТИП» в сайдбаре
  запрещена (задокументированное правило; нарушение — антипаттерн п.8.7).

2.5. **Панель действий сайдбара**: `vbox stylename="edit-sidebar-summary"` с кнопками
`width="100%"`, `enabled="false"` до выбора записи, обязательная первичная кнопка
«Открыть карточку» с `icon="EDIT_ACTION"`:
- `job-candidate-reestr.xml:83-90`
- `open-position-reestr-browse.xml:117-134`

2.6. **Секции сайдбара** — карточки `vbox stylename="job-candidate-navigation label-navigation"`
с заголовком `label stylename="label-nav-title job-candidate-section-title"`
(текст заголовка — ВЕРХНИМ РЕГИСТРОМ, `text-transform: uppercase` в SCSS
`edit-screen-shared-styles.scss:113-126`):
- `job-candidate-reestr.xml:93-99` (ГОТОВНОСТЬ И РЕЙТИНГ), `:102-134` (КОНТАКТЫ И РЕКВИЗИТЫ),
  `:137-159` (ПОСЛЕДНЯЯ АКТИВНОСТЬ), `:162-165` (ОСНОВНЫЕ НАВЫКИ)
- `open-position-reestr-browse.xml:137-142, 145-180, 183-202, 205-208, 211-215`
- Пары «ключ-значение» — `grid columns count="2"` со `stylename="edit-sidebar-summary"`;
  ключ — `label stylename="bold"`, значение — `label value="-" htmlEnabled="true"`
  (`job-candidate-reestr.xml:105-133`, `open-position-reestr-browse.xml:147-179`).
- Чипы навыков — `label stylename="candidate-skills-chips"`
  (`job-candidate-reestr.xml:164`, `open-position-reestr-browse.xml:207`).
- Разделитель секций — `border-top` у `.label-navigation`
  (`edit-screen-shared-styles.scss:108-111`).

2.7. **Workspace (правая панель)**: `vbox width/height="100%" spacing="true"
stylename="edit-workspace candidate-reestr-workspace"`, `expand=` на карточке таблицы,
`margin="true,true,true,false"`:
- `job-candidate-reestr.xml:171`
- `open-position-reestr-browse.xml:222`
- Отступы: `job-candidate-editor.scss:1395-1400` (padding-top 10px, right 14px, bottom 10px).

2.8. **Тулбар** (`tableFilterBar`) — см. п.4/5. **Фильтр** — см. п.5. **Карточка таблицы** —
`vbox stylename="edit-card candidate-table-card" spacing="false" expand=<table>`
(`job-candidate-reestr.xml:250`, `open-position-reestr-browse.xml:302`); карточка
`edit-card` = белый фон, border-radius 8px, тень (`edit-screen-shared-styles.scss:405-416`).

2.9. **Пагинация/rowsCount**: внутри таблицы обязателен `<rowsCount/>`
(`job-candidate-reestr.xml:281`, `open-position-reestr-browse.xml:326`,
`city-reestr-browse.xml:130`); стиль счётчика/стрелок — общий
(`edit-screen-shared-styles.scss:806-908`: высота 28px, стрелки 24×24px, radius 4px).

2.10. **Прочие обязательные атрибуты окна**: `data readOnly="true"`
(`job-candidate-reestr.xml:10`), `<dialogMode height="700" width="1100"/>`
(`job-candidate-reestr.xml:48`, `open-position-reestr-browse.xml:66`,
`city-reestr-browse.xml:20`), иконка окна `icon="TH_LIST"` для списочных реестров
(`job-candidate-reestr.xml:8`, `open-position-reestr-browse.xml:8`),
`focusComponent=` на таблицу (`open-position-reestr-browse.xml:6`, `city-reestr-browse.xml:4`).

---

## 3. Табличная часть

3.1. **Таблица** — `groupTable` (или `treeDataGrid` для иерархий) со
`stylename="borderless grid candidate-browse-grid"`, `width/height="100%"`,
привязка `dataContainer`:
- `job-candidate-reestr.xml:252-256`
- `open-position-reestr-browse.xml:303-306`
- `city-reestr-browse.xml:109-113`, `person-reestr-browse.xml:134-138`, `position-reestr-browse.xml:103-107`

3.2. **Строка 38px + word-break** (определяется дважды — в реестровом и shared-partial,
копии синхронны):
- `job-candidate-editor.scss:1518-1536`: ячейка `padding 6px 0`, `min-height: 38px`,
  `vertical-align: middle`; wrapper `white-space: normal`, `line-height: 1.35`,
  `word-break: break-word`; строки `min-height: 38px`
- `edit-screen-shared-styles.scss:1017-1051` — то же + `height: auto` (динамическая высота
  многострочных ячеек) + стили групповых строк GroupTable (`font-weight: 600`, 13px).
- Правило: высота строки **минимум** 38px и растёт по содержимому; перенос длинных слов
  обязателен (без обрезки/`nowrap`).

3.3. **Колонки**:
- одна «резиновая» колонка с `expandRatio` (ФИО/название): `job-candidate-reestr.xml:266`
  (`fullName expandRatio="1"`), `open-position-reestr-browse.xml:319` (`vacansyName`);
- фиксированные ширины кратны смыслу: аватар/лого 50–60px
  (`job-candidate-reestr.xml:264` — `avatar width="50px" sortable="false"`;
  `open-position-reestr-browse.xml:318` — `logo width="55px"`), счётчики 90–95px,
  статусы 110–135px, навыки 220px (`job-candidate-reestr.xml:268-278`,
  `open-position-reestr-browse.xml:316-324`);
- генерируемые/HTML-колонки (чипы, светофоры, бейджи) — `sortable="false"`
  (`job-candidate-reestr.xml:264,274,276,278`; `open-position-reestr-browse.xml:318,322-324`);
- числовые/короткие поля — `align="CENTER"` (`open-position-reestr-browse.xml:316-317,324`,
  `city-reestr-browse.xml:125-127`).

3.4. **Действия таблицы**: стандартные `edit`/`refresh` (+`create`/`remove` где уместно)
объявляются в `<actions>` таблицы; переход в карточку — `openMode="NEW_TAB"` для
сложных Edit-форм (`job-candidate-reestr.xml:257-260`, `position-reestr-browse.xml:108-113`).

3.5. **Пустые состояния и fallback без кандидата**:
- сайдбар до выбора записи показывает плейсхолдер в заголовке
  (`value="Выберите кандидата"` — `job-candidate-reestr.xml:74`;
  `value="Выберите вакансию"` — `open-position-reestr-browse.xml:90`;
  `msg://msgSelectPosition` — `position-reestr-browse.xml:31`), значения полей — `"-"`;
- кнопки сайдбара и зависимые popup-кнопки тулбара — `enabled="false"` до выбора
  (`job-candidate-reestr.xml:85-89, 208`; `open-position-reestr-browse.xml:122,127,132`);
- аватар без фото — fallback-силуэт (`docs/ui/ReestrBrowseFallbackNoCandidate_Spec.md:10,21-22,56-61`);
- TreeDataGrid-реестры не должны схлопываться: `min-height: 320px` для
  `.candidate-table-card .v-treegrid` (`job-candidate-editor.scss:1143-1148`).

---

## 4. Кнопки/иконки

4.1. **Состав тулбара** (эталон): левая группа — Создать (primary, `CREATE_ACTION`),
Умная загрузка (primary, `font-icon:MAGIC`), Редактировать (secondary, `EDIT_ACTION`,
`enabled="false"`), Удалить (secondary, `REMOVE_ACTION`, `enabled="false"`); правая группа —
popup-фильтры области просмотра и «Действия»:
- `job-candidate-reestr.xml:176-181` / `:184-230`
- `open-position-reestr-browse.xml:228-233` / `:236-283`

4.2. **Обязательная иконочная пара**: каждая кнопка несёт `icon` + текстовый `caption`
(см. все кнопки п.4.1); голые иконочные кнопки в тулбаре реестра запрещены.
`popupButton` — `showActionIcons="true"` и иконка на каждой action
(`job-candidate-reestr.xml:185-195, 212-229`).

4.3. **Стилевые классы кнопок**: `candidate-btn` + семантический суффикс
(`candidate-create-btn`, `candidate-edit-btn`, `candidate-remove-btn`,
`candidate-smartload-btn`, `candidate-filter-scope-btn`) + `primary|secondary`:
- `job-candidate-reestr.xml:177-180`, `open-position-reestr-browse.xml:229-232`
- `city-reestr-browse.xml:79-81` (минимальный набор без smartload допустим).

4.4. **Размеры/состояния** (определяются тулбар-блоком SCSS, переопределять в экране
запрещено): `job-candidate-editor.scss:1259-1328` — высота **34px** (все Button и
PopupButton), radius 6px, font 12.5px/600, padding 0 10px, иконка `margin-right: 5px`,
13px; `white-space: nowrap`.
- primary: фон `#4d7ab2`, hover `#3d689e` (`job-candidate-editor.scss:1330-1356`);
- secondary: фон `#f1f5f9`, border `#e2e8f0`, текст `#475569`, hover `#e2e8f0`/`#0f172a`
  (`job-candidate-editor.scss:1358-1391`);
- disabled: `opacity: .5`, `pointer-events: none` (у обоих вариантов,
  `job-candidate-editor.scss:1351-1355, 1386-1390`).
- Кнопки создания вне тулбара: 32px (`job-candidate-editor.scss:1216-1223`).

4.5. **Кнопки сайдбара** — не `candidate-btn`, а полноширинные `width="100%"`
в `edit-sidebar-summary` (см. п.2.5); стилизуются темой как обычные кнопки.

---

## 5. Фильтр

5.1. **Collapsable-паттерн** (обязателен): компонент `<filter>` со
`defaultMode="generic"`, `width="100%"`, `collapsable="true"`, `collapsed="true"`,
`stylename="candidate-generic-filter"`, `applyTo=<таблица>`, `dataLoader=<загрузчик>`:
- `job-candidate-reestr.xml:235-247`
- `open-position-reestr-browse.xml:287-299`
- `city-reestr-browse.xml:95-105`, `person-reestr-browse.xml:120-130`, `position-reestr-browse.xml:87-99`
- Расположение: между тулбаром и карточкой таблицы (workspace сверху вниз: тулбар → фильтр → таблица).

5.2. **Состав полей**: `<properties include=".*" exclude="id" excludeRecursively="true"
excludeProperties="version,createTs,createdBy,updateTs,updatedBy,deleteTs,deletedBy[,сервисные поля сущности]"/>`
— служебные и аудиторные поля исключаются всегда; дополнительно исключаются
непереносимые/избыточные связи (`fileImageFace,priorityContact` —
`job-candidate-reestr.xml:243-246`; `openPosition,cityRegion.regionOfCity` —
`city-reestr-browse.xml:103-104`; `sendResumeToEmail,fileImageFace,birdhDate` —
`person-reestr-browse.xml:128-129`).

5.3. **Сброс**: сброс фильтра — штатными средствами generic filter (кнопка «Сбросить»
компонента); кастомные кнопки сброса в тулбар не добавляются. Быстрая смена области
просмотра («Все/Мои») — отдельным `popupButton` с классом
`candidate-filter-scope-btn` (`job-candidate-reestr.xml:185-195`,
`open-position-reestr-browse.xml:237-248`).

5.4. **Внешний вид**: белая карточка radius 8px, padding 6px 12px
(`job-candidate-editor.scss:1403-1413`); компактные поля внутри — 32px, radius 6px,
12.5px (`job-candidate-editor.scss:1416-1435`).

---

## 6. Производительность (Zero N+1 и Data View Integrity)

6.1. **Пакетные кэши контроллера**: все sidebar-колонки/доп. поля, требующие
обращений к связям, загружаются ОДНИМ пакетным запросом на страницу данных и
складываются в `Map<UUID, …>`/`Set<UUID>` поля контроллера, заполняемые в
`PostLoadEvent` загрузчика коллекции:
- `JobCandidateReestr.java:211-233` — 8 кэшей (метки, навыки, счётчик отправленных,
  Employee, CV-флаг, дата последнего взаимодействия, ЗОТ, последнее взаимодействие);
- `JobCandidateReestr.java:329` — `onJobCandidatesDlPostLoad`;
- `JobCandidateReestr.java:339,354,370,387,405,423,443,459` — пакетные JPQL
  `… where e.candidate in :candidates`.
Правило: **запрещено** обращаться к lazy-связям построчно в render-колбэках таблицы
или в обработчике выделения строки.

6.2. **FetchGroup BATCH для колонок**: связи, отображаемые в колонках, объявляются
в inline-view экрана с `fetch="BATCH"`:
- `open-position-reestr-browse.xml:14-32` (`projectName`, `positionType`, `owner`,
  `openPositionComments`, `someFiles`, `cities`, `skillsList` — все `fetch="BATCH"`);
- `person-reestr-browse.xml:11-39` (inline view с вложенными `_minimal`-вью).

6.3. **maxResults загрузчика**: коллекция реестра обязана иметь ограничение выборки:
`maxResults="200"` (`job-candidate-reestr.xml:16`) или `maxResults="100"`
(`open-position-reestr-browse.xml:33`). Справочники малого объёма допускают
`cacheable="true"` без maxResults (`job-candidate-reestr.xml:41`,
`city-reestr-browse.xml:11`) — но только при заведомо ограниченной кардинальности.

6.4. **Data View Integrity** (правило `.cursor/rules/data-view-integrity.mdc`): каждый
геттер, вызываемый из Java-кода экрана (рендер колонок, сайдбар), обязан быть
задекларирован во view контейнера/XML — иначе Unfetched Attribute Access /
LazyInitializationException на рантайме. Проверка — обязательный пункт чеклиста QA (п.9).

6.5. **readOnly**: секция `<data readOnly="true">` для browse-экранов обязательна
(`job-candidate-reestr.xml:10`, `open-position-reestr-browse.xml:11`).

---

## 7. Темизация

7.1. **Семь синхронных копий partial'ов**. CUBA 7.3 компилирует темы из изолированного
themes-tmp, поэтому каждая тема содержит реальную копию partial'а; **все копии
изменяются вместе** (шапка файла: `edit-screen-shared-styles.scss:1-6`). Копии
`job-candidate-editor.scss` существуют в темах:
`halo`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`,
`hunttech-modern-dark`, `havana`
(`modules/web/themes/<тема>/com.company.hunttech/job-candidate-editor.scss`).

7.2. **Проверенная инвентаризация (grep по всем 7 темам)**: классы
`candidate-browse-grid`, `job-candidate-sidebar`, `candidate-sidebar-fullname`,
`candidate-reestr-workspace`, `candidate-table-card`, `candidate-btn`,
`candidate-filter-bar`, `candidate-generic-filter` определены в **каждой** теме
(job-candidate-editor.scss + edit-screen-shared-styles.scss). Правило: новая правка
любого из этих классов обязана попасть во все 7 копий; PR, изменивший partial только
в части тем, отклоняется QA.
- **Механическая проверка синхронности**: на master `7c1c9b7a` все 7 копий каждого
  partial'а sha256-идентичны (проверено: `shasum -a 256 */com.company.hunttech/{job-candidate-editor,edit-screen-shared-styles}.scss`
  даёт один хеш на partial). После правки хеши обязаны остаться идентичными —
  это пункт чеклиста QA (п.9.14).

7.3. **Цвета**: правила привязаны к Vaadin-переменным темы (`$v-font-color`,
`$v-selection-color`, `$v-panel-background-color`) — см. `edit-screen-shared-styles.scss:84,92,98,408-409,442`.
Хардкод цветов вне SCSS (в inline-стилях экранов, Java-строках HTML) запрещён;
допустимый хардкод — только внутри partial'ов SCSS (акцент `#4d7ab2` кнопок
`job-candidate-editor.scss:1336`, градиент сайдбара `:68-69`, цвета identity-подписей
`:1124-1141`) и он обязан быть продублирован во всех копиях (п.7.1).

7.4. **Responsive** (готовые брейкпоинты partial'ов, переопределять нельзя):
- ≤1440px: уплотнение тулбара (`job-candidate-editor.scss:1438-1463`);
- ≤1366px (реестровый partial): сайдбар 296px + уплотнение горизонтальных padding
  (`job-candidate-editor.scss:1036-1048`);
- ≤1240px: группы тулбара складываются в две строки, правая — с пунктирным разделителем
  (`job-candidate-editor.scss:1465-1487`);
- ≤1100px: сайдбар 284px (`job-candidate-editor.scss:1538-1547`);
- ≤900px: кнопки тулбара 11.5px/padding 6px (`job-candidate-editor.scss:1489-1505`);
- ≤1366px (shared, для базовых edit-форм): `edit-sidebar` 250px
  (`edit-screen-shared-styles.scss:672-678`).

---

## 8. Запреты (антипаттерны, найденные сравнением с простыми реестрами)

8.1. **Тулбар без flex-классов**: `city-reestr-browse.xml:77-92` и
`person-reestr-browse.xml:102-117` строят `tableFilterBar` на `hbox` с костылём
`toolbarSpacer` (`city-reestr-browse.xml:83`) и без классов `left-action-buttons` /
`right-action-buttons`. Запрещено: тулбар обязан быть `cssLayout` c классами
`filter-buttons-panel left-action-buttons` / `filter-buttons-panel right-action-buttons`
(эталон `job-candidate-reestr.xml:174-231`), иначе flex-правила
`job-candidate-editor.scss:1240-1257` и брейкпоинты п.7.4 не работают.

8.2. **Нет popup-фильтра области просмотра**: в city/person/position отсутствует
аналог `candidate-filter-scope-btn` («Все/Мои») — новый реестр обязан предоставить
быструю смену scope через popupButton (эталон `job-candidate-reestr.xml:185-195`).

8.3. **Отсутствие ограничения выборки**: `person-reestr-browse.xml:40` и
`position-reestr-browse.xml:12` — загрузчик без `maxResults` и без `cacheable`.
Запрещено для реестров с неограниченной кардинальностью (п.6.3).

8.4. **Разнородный состав кнопок**: `position-reestr-browse.xml:78-83` держит
«Обновить» отдельной кнопкой в левой группе и не имеет правой группы; в эталоне
сервисные действия собраны в popup «Действия» с `icon="BARS"`
(`job-candidate-reestr.xml:212-230`).

8.5. **Неполный excludeProperties**: `city-reestr-browse.xml:104` не исключает
бинарные/сервисные поля по общей конвенции (в эталоне — полный аудиторный набор +
сервисные поля сущности, п.5.2).

8.6. **Действия таблицы не привязаны к кнопкам тулбара** (дублирование
`action="citiesTable.edit"` на кнопке при наличии popup-меню) — выбирать один
механизм: либо `action=`-привязка, либо программные слушатели как в эталоне
(`job-candidate-reestr.xml:179-180` + контроллер).

8.7. **Дубль логотипа**: вторая секция с изображением логотипа в сайдбаре запрещена
(логотип — ровно один раз в аватаре шапки, п.2.4). Примечание: содержательное
второе изображение (герб города в `city-reestr-browse.xml:62-68`) — данные, а не
логотип; исключение допустимо с комментарием в XML.

8.8. **Свои цвета/размеры inline** (`color`, `backgroundColor` в Java, inline-style в
XML) вместо stylename'ов контракта — запрещено (п.7.3).

8.9. **Построчные обращения к lazy-связям** в рендере колонок — запрещено (п.6.1).

---

## 9. Чеклист PR для QA (механическая проверка)

- [ ] 1. Layout несёт `job-candidate-editor edit-screen-layout`; hbox — `job-candidate-main-layout edit-screen-layout` (п.2.1–2.2).
- [ ] 2. Sidebar: `width="312px"` + `stylename="job-candidate-sidebar edit-sidebar"` + scrollBox (п.2.3).
- [ ] 3. Аватар `ovaFallbackImage` 120×120, `ovalWidth/Height`, `scaleMode="SCALE_DOWN"`, `fallbackThemePath` (п.2.4).
- [ ] 4. Identity-подписи: `edit-sidebar-title h2 candidate-sidebar-fullname`, `edit-sidebar-subtitle h4 candidate-sidebar-position`, `edit-help candidate-sidebar-city` (п.2.4).
- [ ] 5. Логотип один раз; нет дубль-секции «ЛОГОТИП» (п.2.4, 8.7).
- [ ] 6. Секции сайдбара: `job-candidate-navigation label-navigation` + `label-nav-title job-candidate-section-title`; пары «ключ-значение» в grid 2 колонки `edit-sidebar-summary` (п.2.6).
- [ ] 7. Workspace: `edit-workspace candidate-reestr-workspace`, `margin="true,true,true,false"`, expand на tableCard (п.2.7).
- [ ] 8. Тулбар: `cssLayout` + `candidate-filter-bar edit-card`; группы `left-action-buttons`/`right-action-buttons`; кнопки `candidate-btn` с иконками; Редактировать/Удалить `enabled="false"` (п.4, 8.1).
- [ ] 9. Фильтр: `collapsable="true" collapsed="true" stylename="candidate-generic-filter"`, `excludeProperties` с полным аудиторным набором (п.5).
- [ ] 10. Таблица: `borderless grid candidate-browse-grid`, одна `expandRatio`-колонка, `sortable="false"` на генерируемых, `<rowsCount/>` (п.3).
- [ ] 11. `data readOnly="true"`, `dialogMode 700×1100`, `icon="TH_LIST"`, `focusComponent` (п.2.10).
- [ ] 12. Zero N+1: пакетные кэши/BATCH-fetch на все связи из колонок и сайдбара; `maxResults` у загрузчика (п.6.1–6.3).
- [ ] 13. Data View Integrity: каждый геттер Java-кода декларирован во view контейнеров (п.6.4).
- [ ] 14. Темизация: если правились SCSS-partial'ы — изменены ВСЕ 7 копий идентично; новых хардкод-цветов вне partial'ов нет (п.7).
- [ ] 15. ScreenViewIntegrityTest + контрактные тесты зелёные; OCR review PASS в описании PR.

---

## 10. Приложение: таблица «класс SCSS → где определён → где используется в эталоне»

Определения даны для halo-копии; все 7 тем содержат синхронные копии (п.7.1–7.2).
Пути SCSS относительно `modules/web/themes/halo/com.company.hunttech/`.

| Класс | Определён (SCSS) | Использован в эталоне (XML) |
|---|---|---|
| `job-candidate-editor` (корень) | job-candidate-editor.scss:13-23 | job-candidate-reestr.xml:51; open-position-reestr-browse.xml:69 |
| `edit-screen-layout` | edit-screen-shared-styles.scss:8-30 | job-candidate-reestr.xml:51,53 |
| `job-candidate-main-layout` | job-candidate-editor.scss:26,56-60 | job-candidate-reestr.xml:53 |
| `job-candidate-sidebar` / `edit-sidebar` | job-candidate-editor.scss:63-93; edit-screen-shared-styles.scss:32-44 | job-candidate-reestr.xml:56 |
| `job-candidate-profile-header` / `edit-sidebar-visual` | job-candidate-editor.scss:109-119; edit-screen-shared-styles.scss:46-54 | job-candidate-reestr.xml:62 |
| `job-candidate-avatar` | job-candidate-editor.scss:203-209 | job-candidate-reestr.xml:64-70 |
| `edit-sidebar-identity` | edit-screen-shared-styles.scss:46-54 | job-candidate-reestr.xml:72 |
| `edit-sidebar-title` + `candidate-sidebar-fullname` | edit-screen-shared-styles.scss:61-86; job-candidate-editor.scss:1124-1129 | job-candidate-reestr.xml:74 |
| `edit-sidebar-subtitle` + `candidate-sidebar-position` | edit-screen-shared-styles.scss:63-94; job-candidate-editor.scss:1131-1135 | job-candidate-reestr.xml:76 |
| `candidate-sidebar-city` | edit-screen-shared-styles.scss:96-99; job-candidate-editor.scss:1137-1141 | job-candidate-reestr.xml:78 |
| `edit-sidebar-summary` | edit-screen-shared-styles.scss:48-54,72-77 | job-candidate-reestr.xml:83,105,140 |
| `job-candidate-navigation` / `label-navigation` | job-candidate-editor.scss:29; edit-screen-shared-styles.scss:101-111 | job-candidate-reestr.xml:93,102,137,162 |
| `label-nav-title` / `job-candidate-section-title` | edit-screen-shared-styles.scss:113-126 | job-candidate-reestr.xml:94,103,138,163 |
| `candidate-skills-chips` | SCSS-определения нет (stylename-маркер; чипы рендерятся HTML из Java-контроллера) | job-candidate-reestr.xml:164; open-position-reestr-browse.xml:207 |
| `edit-workspace` / `candidate-reestr-workspace` | edit-screen-shared-styles.scss:9,22-26,195-231; job-candidate-editor.scss:1395-1400 | job-candidate-reestr.xml:171 |
| `candidate-filter-bar` | job-candidate-editor.scss:1169-1177,1229-1238 | job-candidate-reestr.xml:174 |
| `filter-buttons-panel` (+`left-/right-action-buttons`) | job-candidate-editor.scss:1180-1182,1240-1257 | job-candidate-reestr.xml:176,184 |
| `candidate-btn` (+`candidate-create/edit/remove/smartload/filter-scope-btn`) | job-candidate-editor.scss:1259-1392,1216-1223 | job-candidate-reestr.xml:177-189,201,205,217 |
| `candidate-generic-filter` | job-candidate-editor.scss:1403-1435 | job-candidate-reestr.xml:242 |
| `edit-card` / `candidate-table-card` | edit-screen-shared-styles.scss:14,405-416; job-candidate-editor.scss:1145-1148 | job-candidate-reestr.xml:174,250 |
| `candidate-browse-grid` | job-candidate-editor.scss:1518-1536; edit-screen-shared-styles.scss:1017-1051 | job-candidate-reestr.xml:256; open-position-reestr-browse.xml:307 |
| `borderless grid` (платформенные) | тема CUBA | job-candidate-reestr.xml:256 |
| `c-rows-count` / `c-simplepagination` (rowsCount) | edit-screen-shared-styles.scss:806-908 | job-candidate-reestr.xml:281 |
| `v-treegrid` в tableCard | job-candidate-editor.scss:1145-1148 | SkillTreeReestrBrowse (прецедент) |

---

## Источники

**Эталоны (изучены полностью):**
- modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-reestr.xml (288 стр.)
- modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateReestr.java (кэши :211-233, PostLoad :329-472)
- modules/web/src/com/company/hunttech/web/screens/openposition/open-position-reestr-browse.xml (332 стр.)
- modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionReestrBrowse.java

**SCSS-база (halo-копии; синхронные копии в helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark, havana):**
- modules/web/themes/halo/com.company.hunttech/job-candidate-editor.scss (1548 стр.)
- modules/web/themes/halo/com.company.hunttech/edit-screen-shared-styles.scss (1092 стр.)

**Сравнительные простые реестры:**
- modules/web/src/com/company/hunttech/web/screens/city/city-reestr-browse.xml
- modules/web/src/com/company/hunttech/web/screens/person/person-reestr-browse.xml
- modules/web/src/com/company/hunttech/web/screens/position/position-reestr-browse.xml

**Существующие спеки (сверка формулировок):**
- docs/ui/OpenPositionReestrBrowse_Spec.md
- docs/ui/ProjectReestrBrowse_Spec.md
- docs/ui/ReestrBrowseFallbackNoCandidate_Spec.md
- docs/ui/SkillTreeReestrBrowse_Design.md
- docs/ui/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md

**Версия кода обзора**: master `7c1c9b7a` (05.09.2026, деплой 0.435).
