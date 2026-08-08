# Дизайн-ревью компоновки JobCandidateEdit (CUBA 7.3, HRM HuntTech)

> Дата: 2026-08-03
> Роль: UI/UX-ревью компоновки (presentation-only, без изменения кода)
> Форма: `hunttech_JobCandidate.edit` — `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml` + `JobCandidateEdit.java`
> Эталон: `IteractionListEdit` (XML + `iteraction-list-visual-alignment.scss` — финальный SCSS-слой)
> Контракт: `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`, `docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md`

## 0. Что прочитано и проверено

- Контракт Edit-экранов (полностью) и UI/UX-концепция (полностью).
- Текущая форма: `job-candidate-edit.xml` (1680 строк), `JobCandidateEdit.java` (4371 строки, навигация/стили), `job-candidate-editor.scss` (945 строк), `hover-ext.scss` (порядок подключения).
- Эталон: `iteraction-list-edit.xml`, `iteraction-list-editor.scss`, `iteraction-list-flat-layout.scss`, `iteraction-list-visual-alignment.scss` (финальный слой), `iteraction-list-accordion-navigation.scss`.
- Shared API: `edit-screen-shared-styles.scss` (hover; по контракту 7 идентичных копий).
- Доки: `JobCandidateEdit_Spec.md`, `IteractionListEdit_Spec.md`, `IteractionListEdit_XmlLayout_2026-07-27.md`, `IteractionListEdit_VisualAlignment_2026-07-28.md`.
- Фактический порядок правил в собранном CSS: `modules/web/build/themes-tmp/VAADIN/themes/hover/styles.css` (ground truth для hover-темы).
- Ключи сообщений: `web/messages*.properties` + локальный пакет `screens/jobcandidate/messages*.properties`.

### Ключевой факт компиляции (важно для всех правок)

Во всех 7 темах `job-candidate-editor-theme` подключается **до** `edit-screen-shared-styles`
(в `hover`/`halo` — через `hover-ext.scss:1,551` / `halo-ext`; в modern-темах — напрямую в `styles.scss:6,29`).
Локальные правила JobCandidateEdit выигрывают у shared только за счёт повышенной специфичности
(вложенность в `.job-candidate-editor` → (0,3,0) против (0,2,0) у shared). Это нарушает контракт 6.4
(theme base → shared → screen-specific) и уже даёт видимые расхождения (см. P1-1, где shared-геометрия
24px не применилась к навигации).

---

## 1. Дефекты компоновки текущей формы

Приоритеты: **P1** — видимое нарушение контракта/эталона; **P2** — заметное расхождение или мёртвый код;
**P3** — мелочи/документация.

### P1-1. Label-навигация: геометрия пунктов не по эталону (38px вместо 27px)

- Локально: `job-candidate-editor.scss:382–405` задаёт пунктам `min-height: 38px`, `padding: 8px 10px`, `line-height: 18px` (компилируется в `styles.css` hover:4357).
- Эталон (финальный слой `iteraction-list-visual-alignment.scss:112–161`): `min-height: 27px`, `padding: 3px 10px`, `line-height: 20px`.
- Shared (`edit-screen-shared-styles.scss:98–114`): `min-height: 24px`, `padding: 3px 10px`, `line-height: 20px`.
- Контракт §3.1 + история 31.07.2026: локальные переопределения геометрии label-nav запрещены; JobCandidateEdit обязан повторять эталон 1:1 (27px/3px/20px). У JobCandidateEdit сейчас «вариант A» 38px/8px — отход от эталона на ~11px на пункт (7 пунктов ≈ 77px лишней высоты sidebar).
- Спека `JobCandidateEdit_Spec.md` (запись 08-03 «восстановлена до 38px/13px») противоречит контракту и эталону — см. P3-13.

### P1-2. Вкладка «Основное»: карточки рендерятся 50/50 рядом, а не вертикально

- `job-candidate-editor.scss:622–635`: `.job-candidate-accordion-open .job-candidate-accordion-content { display:flex }` + `> .v-slot { width: calc(50% - 8px) }`.
- Класс `job-candidate-accordion-open` стоит на `jobCandidateMainSection` (XML:674), значит правило применяется к контенту вкладки «Основное»: `personalDataBlock` и `professionalDataBlock` встают **рядом** по 50%.
- XML-комментарий (XML:686) и спека (запись 31.07, «переведена на вертикальную раскладку… одна над другой на всю ширину») требуют вертикального стека 100%.
- Эталон: flat-секции строго вертикальные, 100% ширины (`iteraction-list-flat-layout.scss:168–224`). Концепция §6.3.4: одна колонка при ограниченной ширине.
- Вывод: flex-правило — остаток старой hbox-раскладки; противоречит и коду-комментарию, и спеке, и эталону.

### P1-3. Toolbar рабочей области без заголовка и описания

- `jobCandidateTopBar` (XML:619–654, `edit-toolbar`) содержит только `popupButton` «Еще» (`edit-toolbar-actions`). Классы `edit-toolbar-title` / `edit-toolbar-description` не используются.
- Эталон: toolbar = `edit-toolbar-title` + `edit-toolbar-description` + actions (`iteraction-list-edit.xml:463–487`). Контракт §5.4: роль «Заголовок и actions».
- Визуально: шапка формы пустая слева, одинокая кнопка справа — не соответствует ритму эталона.

### P1-4. Фальшивый «аккордеон»: маркер ▼ без сворачивания

- `job-candidate-editor.scss:596–604`: `job-candidate-accordion-header:before { content: "\25BC" }` — декоративный маркер «раскрыто».
- Секции — обычные `vbox` (XML:671–674 и далее), без `groupBox`/`collapsable`, без Java-handler: клик по заголовку ничего не делает. Маркер вводит в заблуждение.
- Эталон: заголовки flat-секций **без маркера** (`iteraction-list-flat-layout.scss:194–211`), клик по заголовку ничего не сворачивает (XmlLayout §3). Концепция §6.3: аккордеон обязан быть честным (groupBox) — либо честным flat-заголовком.
- Рекомендация (без изменения Java): убрать ▼ и оформить заголовки как flat-section-header эталона.

### P2-5. Ширина sidebar: 312px локально против 270px контракта; media-поведение не совпадает с эталоном

- Факт: XML:255 `width="312px"`; SCSS `job-candidate-editor.scss:63–76` — 312px и у компонента, и у slot; `@media ≤1366px` — 286px (строки 892–900).
- Эталон: 312px (>1366), **296px (≤1366)**, **284px (≤1100)** (`iteraction-list-flat-layout.scss:319–346`; зафиксировано в IteractionListEdit_Spec §15).
- Контракт §4.2: базовая 270px, ≤1366 — 250px.
- Оценка: 312px у JobCandidateEdit **совпадает с эталоном** — отклонение от 270px уже документировано в JobCandidateEdit_Spec §3 («ширина — 312 px»). Оставляем 312px, но: (а) зафиксировать отклонение явно и в контракте/спеке; (б) привести media-тиры к эталону (296/284), сейчас 286px и нет тира ≤1100px.
- Нарушение контракта 4.2 «не сжимается содержимым» не выявлено: slot и компонент совпадают (312/312) — наложение sidebar/workspace отсутствует (подтверждено скомпилированным CSS: специфичность (0,3,0) побеждает shared 270px).

### P2-6. Порядок подключения SCSS нарушает контракт 6.4 (все 7 тем)

- Во всех темах `job-candidate-editor-theme` идёт ДО `edit-screen-shared-styles` (hover: `hover-ext.scss:1` + `styles.scss:4,9,35`; halo аналогично; modern-темы: `styles.scss:6,11,29,34`).
- Контракт §6.4: `theme base → edit-screen-shared-styles → screen-specific partial`. Из-за нарушения shared-геометрия label-nav (24px) не применилась к форме (см. P1-1) — прямое следствие.
- Правка безопасна визуально (вложенные локальные селекторы сохраняют специфичность), но устраняет хрупкость.

### P2-7. Подписи вкладок 12px — нарушение концепции (15–16px/600) и собственной спеки («14px»)

- `job-candidate-editor.scss:537`: `.job-candidate-tabs .v-caption { font-size: 12px }` (компилируется в hover styles.css). Концепция §5: подпись вкладок 15–16px/600. Спека JobCandidateEdit заявляет «компактные подписи 14 px».
- Также активная вкладка — синий `#0b63b6` (строка 553), тогда как акцент бренда `#ffb11b` (концепция §3); уточнить: для светлой рабочей области допустим theme-aware `$v-selection-color`, но разнобой 12px/14px/16px между кодом, спекой и концепцией нужно устранить.

### P2-8. Шрифт полей вкладки «Основное» 16px против 15px эталона/контракта 5.5

- `job-candidate-editor.scss:734,757`: подписи и inputs `.job-candidate-form-grid`/`.job-candidate-form-row` — `font-size: 16px !important`.
- Контракт §5.5: `SuggestionPickerField` и `LookupPickerField` — одинаковые `font-size: 15px`. Эталон: 15px (`iteraction-list-visual-alignment.scss:382–393`).
- Внутри формы 16px единообразно (Suggestion == Lookup ✓), но отличается от эталона на 1px. Привести к 15px либо зафиксировать отклонение в спеке.

### P2-9. Фиксированная высота 560px у секции «Социальные сети»

- XML:1100: `contactSocialNetworksSection height="560px"`. Контракт/эталон: секции растут по содержимому (`height=AUTO`, flat-layout). Фиксированная высота даёт либо пустоту, либо обрезку в зависимости от числа строк таблицы.
- Внутри `dataGrid height="100%"` — при переводе секции на AUTO сетке нужен явный `min-height`, чтобы не схлопнуться.

### P2-10. Мёртвые stylename (XML) и мёртвые SCSS-правила

Не имеют ни одного правила в SCSS (проверено grep по `job-candidate-editor.scss`):

| Класс в XML | Где | Статус |
|---|---|---|
| `job-candidate-half-card` | XML:968 | спека заявляет «удалён» — фактически остался |
| `job-candidate-contact-card` | XML:968, 1025 | мёртвый |
| `job-candidate-positions-layout` | XML:1210 | мёртвый |
| `job-candidate-table-comments` | XML:1560 | мёртвый |
| `job-candidate-info-grid` | XML:364 | мёртвый |
| `job-candidate-sidebar-grid` | XML:364 | мёртвый |
| `job-candidate-name-row` | XML:715, 747, 777 | мёртвый (геометрию дают общие правила `.job-candidate-form-grid`) |

Мёртвый SCSS: `.job-candidate-audit-box` / `.job-candidate-audit-label` (`job-candidate-editor.scss:473–484`) — XML-компоненты удалены 31.07 (см. спеку), правила остались во всех 7 темах.

### P2-11. Захардкоженные русские подписи вместо ключей mainMsg

Прямые строки в XML: «Имя», «Отчество», «Фамилия», «Дата рождения», «Город», «Должность», «Компания», «Доп. позиции», «Рейтинг», «Карточка», «Email», «Телефон», «Telegram», «WhatsApp», «Viber», «Skype», «Способ связи», «Основные контакты», «Дополнительные контакты», «Разделы формы», подписи всех 7 вкладок, «Создать резюме», «Создать взаимодействие», «HR-Мастер», «Еще», «Сохранить и закрыть», «Отмена», «Создал», «Дата создания», «Данные записи», «Персональные данные», «Профессиональные данные».
Контракт (и практика гео-форм): подписи — ключи `msg://`/`mainMsg://` (ru+en). Часть ключей уже существует в локальном пакете (`msgBirthDate=Дата рождения`, `msgCandidateContacts=Контакты`, `msgCandidateIteration=Взаимодействия` и др.) — использовать их; недостающие добавить в оба файла.

### P2-12. Колонки таблиц без caption

- `socialNetworkTable.networkName` (XML:1137–1138) — нет `caption` → рендерится имя свойства.
- `jobCandidateIteractionListTable`: `vacancy` (1380), `iteractionType` (1387), `recrutier` (1394) — нет `caption`.
- Эталон: каждая колонка таблицы — ключ `msg://` (см. гео-формы и IteractionListEdit).

### P3-13. Расхождения спеки с кодом (обновить документацию после правок)

1. `JobCandidateEdit_Spec.md` §2 перечисляет вкладку `tabSocialNetworks` — в XML её нет (соцсети внутри `tabContactInfo`).
2. Спека §3 «верхняя панель: служебные данные и «Еще»» — audit-блок удалён 31.07, панель пуста (см. P1-3).
3. Спека §3 «пункты label-nav 38px — ритм эталона» — противоречит контракту §3.1 и эталону 27px (P1-1).
4. Спека §3 «компактные подписи вкладок 14px» — фактически 12px (P2-7).

### P3-14. Прочие мелкие расхождения

- Footer: `job-candidate-bottom-bar` min-height 66px (SCSS:846) против контракта 58px (`edit-footer-actions`) и эталона 54px — допустимо, но зафиксировать.
- Строки контактов: label `width="100px"` в XML (строки 977, 988, 999, 1010, 1047, 1058, 1069) против slot `150px` в SCSS (`job-candidate-form-row > .v-slot:first-child`, строка 768) и `128px` в media ≤1366 — тройное противоречие; оставить одно значение.
- `birdhDateField width="AUTO"` (XML:810) при `edit-form-control` (shared `width:100% !important`) — атрибут не работает; убрать или поставить 100%.
- Поля комментариев `stylename="large edit-form-control"` (XML:1580, 1586) — shared перебивает `large` (высота/шрифт !important) → класс фактически no-op.
- `groupBox stylename="well"` (XML:1578, панель отправки комментария) — legacy-стиль, отход от канона карточек `edit-card`.
- Скрытый `lastProjects`/`dictionatysTavlesHBox` размером 1×1px — легитимный legacy-контракт @Inject, не трогать.
- Радио-группа «Способ связи» с `edit-form-control` — проверить при smoke, что shared-правила (рамки/высота 38px) не ломают radio (у эталона аналога нет).

---

## 2. Задание фронтенду (конкретные правки, presentation-only)

Ограничения: **не изменять** entity, views, data containers, properties, options containers, loaders, JPQL, actions, validators, `invoke`, lifecycle, Java-контроллер. Java-инъекции видимых контейнеров отсутствуют (инъецируются только `tabSheetSocialNetworks`, `blockCandidateCheckBox`, скрытые `skillBox`/`lastProjects`/`dictionatysTavlesHBox` — их ID и типы сохранить). Все правки — stylename, layout-атрибуты, SCSS, ключи сообщений.

### 2.1. `job-candidate-edit.xml`

1. **Навигация (P1-1):** оставить `borderless label-nav-item [label-nav-item-active]`; локальный класс `job-candidate-nav-item` можно оставить только как цветовой хук (см. 2.2), но геометрию он задавать не должен.
2. **Вкладка «Основное» (P1-2):** без изменения структуры (vbox уже вертикальный) — правка только SCSS (2.2.2).
3. **Toolbar (P1-3):** внутрь `jobCandidateTopBar` добавить слева вертикальный блок:
   ```xml
   <vbox id="jobCandidateToolbarTitleBox" width="100%" spacing="false" stylename="job-candidate-toolbar-title-box">
       <label value="msg://editorCaption" stylename="edit-toolbar-title" width="100%"/>
       <label value="mainMsg://msgCandidate" stylename="edit-toolbar-description" width="100%"/>
   </vbox>
   ```
   `expand` перевести на этот блок; `moreActionsPopUpButton` остаётся справа (`edit-toolbar-actions`). Ключи проверить в messages (ru/en).
4. **Секция «Социальные сети» (P2-9):** `contactSocialNetworksSection height="560px"` → `height="AUTO"`; `dataGrid socialNetworkTable` оставить `height="100%"`, минимальную высоту задать в SCSS (2.2.5).
5. **Мёртвые классы (P2-10):** удалить из stylename: `job-candidate-half-card`, `job-candidate-contact-card`, `job-candidate-positions-layout`, `job-candidate-table-comments`, `job-candidate-info-grid`, `job-candidate-sidebar-grid`, `job-candidate-name-row`. Канонические классы (`edit-card`, `edit-accordion-section`, `label-nav-*` и т.д.) сохранить.
6. **Captions колонок (P2-12):** добавить `caption="msg://..."` для `networkName`, `vacancy`, `iteractionType`, `recrutier` (ключи создать в ru/en).
7. **Мелочи (P3-14):** `birdhDateField` — убрать `width="AUTO"`; у labels строк контактов убрать `width="100px"` (управление из SCSS); у `chatMessageTextField`/`vacancyPopupPickerField` убрать `large`; `groupBox well` → `stylename="job-candidate-card edit-card"` (оставить id/контент).
8. **Ключи сообщений (P2-11):** перевести перечисленные в P2-11 подписи на `msg://`/`mainMsg://`; недостающие ключи добавить в `web/messages.properties` + `web/messages_ru.properties` (и/или локальный пакет формы) — оба файла синхронно.

### 2.2. `job-candidate-editor.scss` (7 идентичных копий: halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark)

1. **Навигация (P1-1):** скопировать 1:1 геометрию и цвета из `iteraction-list-visual-alignment.scss:112–161`: `min-height: 27px !important`, `height: auto !important`, `padding: 3px 10px !important`, `line-height: 20px !important`, `border-radius: 0 5px 5px 0 !important`, hover — белый на `rgba(255,255,255,.08)`, active — `#ffb11b` на `rgba(255,177,27,.12)` с левой границей. Обновить устаревший комментарий (строки 379–381, «вариант A»).
2. **Вкладка «Основное» (P1-2):** из правила `.job-candidate-accordion-open .job-candidate-accordion-content, .job-candidate-card-row { display:flex }` (строка 622) убрать селектор `.job-candidate-accordion-open .job-candidate-accordion-content` — flex и `width: calc(50% - 8px)` (строка 629) оставить **только** для `.job-candidate-card-row`. Карточки `personalDataBlock`/`professionalDataBlock` вернутся в вертикальный стек 100%.
3. **Заголовки секций (P1-4):** удалить `:before { content: "\25BC" }` (строки 596–604); заголовок оформить как flat-section-header эталона (фон, нижняя граница, `edit-card-title`-типографика уже есть в `.job-candidate-accordion-title`).
4. **Вкладки (P2-7):** `.job-candidate-tabs .v-caption` `font-size: 12px` → `14px` (и в media ≤1366 — единое 14px); активный цвет — согласовать: `#0b63b6` заменить на `$v-selection-color` (theme-aware) либо явно задокументировать синий акцент.
5. **Поля (P2-8):** `font-size: 16px !important` → `15px !important` для `.job-candidate-form-grid`/`.job-candidate-form-row` inputs и подписей (строки 734, 757). После правки проверить, что SuggestionField ФИО == LookupPicker == TextField по высоте/шрифту (38px/15px).
6. **Секция соцсетей (P2-9):** добавить `min-height` для `socialNetworkTable` (например `min-height: 320px`), секция — `height: auto`.
7. **Мёртвый SCSS (P2-10):** удалить `.job-candidate-audit-box` / `.job-candidate-audit-label` (строки 473–484).
8. **Sidebar media (P2-5):** `@media (max-width:1366px)` ширина `286px` → `296px` (и у slot — он наследует через общий селектор, проверить); добавить тир `@media (max-width:1100px)` → `284px` (компонент и slot одинаково). Убедиться, что slot и компонент всегда равны (правило эталона: «ширина sidebar = ширине Vaadin slot»).
9. **Строки контактов (P3-14):** `job-candidate-form-row > .v-slot:first-child` `150px` → `100px` (совпасть с фактическими подписями), media `128px` — синхронизировать.

### 2.3. `styles.scss` — 7 тем (P2-6)

Перенести подключение screen-specific слоя ПОСЛЕ shared:
- `@import "com.company.hunttech/job-candidate-editor.scss"` — после `@import "com.company.hunttech/edit-screen-shared-styles"`;
- `@include job-candidate-editor-theme;` — после `@include edit-screen-shared-styles;`.
- Для halo/havana/helium/hover: то же внутри `-ext` файлов (например, `hover-ext.scss:1,551` — вынести `@import`/`@include` в styles.scss после shared или перенести строки).
- Визуально правка no-op при сохранении вложенности селекторов, но возвращает порядок слоёв к контракту 6.4.

### 2.4. Документация (после правок)

- Обновить `JobCandidateEdit_Spec.md`: убрать расхождения P3-13 (вкладка tabSocialNetworks, 38px→27px, 14px→факт, audit-блок), зафиксировать: sidebar 312/296/284, отклонение 312px от контракта 270px, шрифт полей 15px, tab-подписи 14px, отсутствие маркера ▼.
- Проверить/обновить контракт-тест компоновки формы (`JobCandidateEditLayoutContractTest`), если он ассертит 38px/50-50-раскладку.

---

## 3. Критерии приёмки

### 3.1. Сборка и статика

1. `./gradlew :app-web:buildScssThemes --no-daemon --stacktrace` — успех; 7 копий `job-candidate-editor.scss` идентичны (diff).
2. `./gradlew :app-core:test --tests '*JobCandidateEditLayoutContractTest*' --no-daemon --stacktrace` и `./gradlew test --tests '*ScreenViewIntegrityTest*'` — зелёные.
3. Детерминированная проверка собранного CSS (`build/themes-tmp/VAADIN/themes/hover/styles.css`):
   - `.hover .job-candidate-editor .label-nav-item` → `min-height:27px`, `padding:3px 10px`;
   - отсутствует `display:flex` у `.job-candidate-accordion-open .job-candidate-accordion-content` (flex остаётся только у `.job-candidate-card-row`);
   - `.job-candidate-tabs .v-caption` → `font-size:14px`;
   - в DOM/CSS присутствуют `.edit-toolbar-title`/`.edit-toolbar-description` для JobCandidateEdit;
   - `job-candidate-editor.scss` содержит media-тиры 296px (≤1366) и 284px (≤1100);
   - отсутствуют `.job-candidate-audit-box`, `.job-candidate-half-card` и прочие мёртвые классы из P2-10.
4. Порядок слоёв: в 7 `styles.scss` `@include job-candidate-editor-theme` стоит после `@include edit-screen-shared-styles`.

### 3.2. Визуальный smoke (под пользователем alan)

Форма открывается как editor кандидата; сравнение 1:1 с эталоном `IteractionListEdit` на тех же viewports: **1700×950, 1366×768, 1100×760**, темы: **hover** (основная), **halo** (эталонная проверка), **hunttech-modern-dark**.

- Sidebar: 312px (>1366), 296px (≤1366), 284px (≤1100); панель и её slot одной ширины; карточки, кнопки и длинные ФИО не выходят за правую границу sidebar; нет горизонтального скролла формы.
- Label-навигация: пункты высотой 27px (визуально ≈ как в IteractionListEdit), активный пункт — жёлтый `#ffb11b` на `rgba(255,177,27,.12)` с жёлтой левой границей, hover — белый текст на `rgba(255,255,255,.08)`; маркер по центру текста; переключение пунктов не меняет размеры соседних.
- Вкладка «Основное»: «Персональные данные» и «Профессиональные данные» — одна над другой на 100% ширины.
- Toolbar: заголовок + описание слева, «Еще» справа, высота ≥58px.
- Секции: заголовки без маркера ▼, выглядят как flat-заголовки эталона; содержимое не обрезается.
- Вкладки: подписи 14px/600, активная вкладка с нижней границей 3px; подписи видны полностью (без ellipsis) — горизонтальная прокрутка строки вкладок допустима только внутри tabcontainer.
- Поля: единая высота 38px и шрифт 15px (ФИО/город/должность/компания/контакты/комментарии).
- «Контакты»: две карточки 50/50; секция «Социальные сети» без фиксированной 560px, таблица видна (min-height), колонки с русскими captions.
- «Позиции и вакансии», «Взаимодействия», «Резюме и файлы», «Комментарии», «История»: таблицы не выходят за границы, footer («Сохранить и закрыть»/«Отмена») не перекрывает контент.
- Hard reload (кеш браузера off) перед проверкой; критерий: отличия видны на скриншоте без инспектора.
- P1=0, P2=0; Tomcat critical errors отсутствуют.

---

## Приложение: карта «элемент → эталон/контракт → текущее состояние»

| Роль (контракт §5.4) | Эталон IteractionListEdit | JobCandidateEdit (факт) | Статус |
|---|---|---|---|
| `edit-screen-layout` | `iteractionListMainLayout` | root layout + `jobCandidateMainLayout` (дважды) | ⚠️ класс на двух уровнях — оставить один (на hbox), убрать с `<layout>` либо наоборот |
| `edit-sidebar` 270px | 312/296/284 (локально, задокументировано) | 312/286 (локально) | ⚠️ см. P2-5 |
| `edit-sidebar-visual/identity/title/subtitle` | 96×96 пара, title/subtitle | 176×176 одиночное фото, title/subtitle | ✅ (176px подтверждён концепцией §11) |
| `edit-sidebar-summary` | service-card после identity, до навигации | `candidateProfileSummary` после identity, до навигации | ✅ порядок по контракту |
| `label-navigation` + `label-nav-*` | 27px/3px/20px, желтый active | 38px/8px/18px | ❌ P1-1 |
| `edit-toolbar` + `edit-toolbar-title/-description` | title + description + actions | только actions («Еще») | ❌ P1-3 |
| `edit-tabs` | — (форма без табов) | 7 вкладок, caption 12px | ⚠️ P2-7 (концепция 15–16px) |
| `edit-card` / секции | flat-section header+body, вертикально, 100% | vbox + кастомный header + ▼, вкладка «Основное» — 50/50 | ❌ P1-2, P1-4 |
| `edit-form-control` | 38px/15px, provider 20×20/40px | 38px/16px | ⚠️ P2-8 |
| `edit-footer-actions` | 54px | 66px | ⚠️ P3-14 |
