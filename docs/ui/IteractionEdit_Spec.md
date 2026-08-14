# IteractionEdit (`hunttech_Iteraction.edit`)

Cross-links: [docs/entities/iteraction/Iteraction.md](../entities/iteraction/Iteraction.md) · эталон компоновки: [IteractionListEdit_Spec.md](IteractionListEdit_Spec.md) · общий контракт: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md)

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Форма редактирует **тип взаимодействия** (`Iteraction`) — справочник HRM HuntTech, описывающий, каким образом рекрутёр взаимодействует с кандидатом (интервью, отправка в клиент, вывод на проект, резерв и т.п.). Каждый тип задаёт: место в дереве типов (родитель), обязательность, пиктограмму, номер, бизнес-признаки (что означает факт взаимодействия для кандидата/вакансии), аутстаффинговое поведение, кнопку действия (call-кнопка или форма), правила уведомлений, дополнительные поля, виджеты и проверку цепочки (какие дочерние типы допустимы следующим шагом). На эти признаки опираются browse-экраны и виджеты: фильтрация по `sign*`, вывод иконок и колонок.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из **IteractionTreeBrowse** (`hunttech_Iteraction._tree.browse`, меню «Тип взаимодействия с кандидатом») стандартным образом: создание новой записи или двойной клик/«Изменить» по узлу дерева. Иерархия типов строится через поле «Элемент верхнего уровня» (`iteractionTreeField`). Форма сама по себе — редактор справочника; из неё нет переходов на другие экраны, кроме picker-открытий (`iteractionTree`, `workStatus`). Внутри формы навигация по 8 вкладкам TabSheet дублируется пунктами label-навигации левой sidebar.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- **Открытие** → в sidebar показываются иконка, наименование, номер; активна первая вкладка «Тип взаимодействия»; тяжёлые справочники (дочерние типы, статусы штата) не грузятся до первого открытия своей вкладки (lazy). Если тип обязательный (`mandatoryIteraction`) — часть полей блокируется.
- **Вкладки и lazy-загрузка** → открыл вкладку «Аутстаффинг» → загрузился `workStatusDl`; «Дополнительно» → по необходимости подгружается LOB `textEmailToSend`; «Проверка цепочки» → грузится `iteractionElementDl` (дочерние типы для twin column).
- **Сохранение** → перед коммитом NULL-чекбоксы признаков приводятся к `false`; сохранение выполняется стандартными кнопками OK/Отмена в правом нижнем углу.
- **Бизнес-взаимодействия полей** (без изменения): включение календаря делает обязательным поле стиля; взаимоисключение признаков «добавить тип»/«форма вызова»; переключение типа уведомлений перерисовывает подписи; выбор режима отслеживания управляет twin column.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| Controller ID | `hunttech_Iteraction.edit` (`@UiController`) |
| Дескриптор | `modules/web/src/com/company/hunttech/web/screens/iteraction/iteraction-edit.xml` |
| Контроллер | `modules/web/src/com/company/hunttech/web/screens/iteraction/IteractionEdit.java` |
| Открытие | `IteractionTreeBrowse` (StandardLookup: create / edit по дереву) |
| Режим окна | `dialogMode` width/height 100%×100% (как эталон IteractionListEdit) |
| Фокус при открытии | `iterationNameField` |
| Иконка окна | `REFRESH_ACTION` |
| Сущность | `com.company.hunttech.entity.Iteraction`, view `iteraction-edit-view` |
| Права | стандартные CUBA CRUD на сущность |

## 2. Связь с моделью данных (Data & Entity Binding)

| Контейнер | Класс / view | Loader | Назначение |
|-----------|--------------|--------|------------|
| `iteractionDc` | `Iteraction` / `iteraction-edit-view` | `iteractionDl` | редактируемая запись |
| `workStatusDc` | `EmployeeWorkStatus` / `employeeWorkStatus-view` | `workStatusDl` (cacheable, lazy) | варианты picker «Рабочий статус» |
| `iteractionsTreeDc` | `Iteraction` / `iteraction-picker-view` | `iteractionsTreeDl` (cacheable) | корни дерева для «Элемент верхнего уровня» |
| `iteractionElementsDc` | `Iteraction` / `iteraction-picker-view` | `iteractionElementDl` (cacheable, lazy) | дочерние типы для twin column «Проверка цепочки» |

JPQL: `select e from hunttech_Iteraction e where e.iteractionTree is null order by e.number` (корни) и `select e from hunttech_Iteraction e where e.iteractionTree is not null order by e.iterationName` (дочерние).

Основные bindings: `mandatoryIteraction`, `iteractionTree`, `iterationName`, `number`, `pic`, `checkTrace`, `staffInteractionStatus`, `workStatus`, `outstaffingSign`, `signStartProject`, `signEndProject`, `textEmailToSend` (LOB, lazy reload), `addType`/`callForm`/`callDialog` и др. — по `property=` в XML.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

```
IteractionTreeBrowse (дерево типов)
 └─ IteractionEdit (fullscreen диалог)
     ├─ Sidebar: профиль (иконка, наименование) → номер → label-навигация → предупреждение
     ├─ Workspace: toolbar → TabSheet (8 вкладок) → footer OK/Отмена
     └─ Picker-открытия: корневой тип (iteractionsTreeDc), рабочий статус (workStatusDc)
```

Дочерних фрагментов и диалогов форма не содержит. Footer использует стандартные actions `windowCommitAndClose` / `windowClose` (screen `editWindowActions`).

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл формы (Lifecycle)

- `onInit` → строится программное содержимое вкладок (варианты типов уведомлений, периоды, режимы отслеживания), подключается sidebar-навигация; pre-load listeners запрещают загрузку тяжёлых loaders до активации вкладки.
- `onBeforeShow` → подпись предупреждения администратора, блокировка полей для обязательного типа, взаимоисключение addFlag/callForm, пересчёт видимости email-полей.
- `onAfterShow` → активация нужных радио-групп и состояния пиктограммы.
- `onTabSheetSelectedTabChange` → по первому открытию вкладок «Проверка цепочки» / «Аутстаффинг» загружаются `iteractionElementDl` / `workStatusDl`; для «Дополнительно» при необходимости reload LOB `textEmailToSend`.

### 4.2 Скрытые вычисления (без явного клика)

- Preview пиктограммы: изменение поля «Пиктограмма» (`iteractionFieldPic`) показывает картинку (`embeddedPict`) в карточке «Пиктограмма».
- `checkBoxCalendar` → делает обязательным `textFieldCalendarItemStyle`.
- Включение отправки уведомлений → показываются группы «когда/кому/период»; выбор периода «N дней до/после» показывает поле `dayBeforeAfterTextField`.
- Radio-переключения вкладок «Кнопка» и «Дополнительное поле» включают/отключают зависимые поля и переключают видимость `textEmailHBox`.
- Sidebar-навигация: клик по пункту → `tabSheet.setSelectedTab(...)`, активный пункт получает `label-nav-item-active`; контейнер `label-navigation` виден на **всех** 8 вкладках (решение 2026-08-11 — правило 3.6 контракта о скрытии на одноблочных вкладках не применяется). Это presentation-only — данные и lifecycle не затрагиваются.

### 4.3 Валидация и сохранение

- `onBeforeCommitChanges` → NULL-значения чекбоксов признаков (`signEndCase`, `signOurInterview*`, `signSentToClient`, `statistics`, `signPriorityNews`, `signViewOnlyManagers`) приводятся к `false`, чтобы не ломать фильтры.
- Дополнительных бизнес-валидаторов нет; сохранение — стандартное commit + close.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Действие → Условие → Результат |
|---------|-------------------------------|
| Пункты label-навигации (8 шт.: `typeTabNav` … `checkTraceNav`) | Нажал пункт → переключается соответствующая вкладка TabSheet → пункт подсвечивается жёлтым (#ffb11b), данные вкладки загружаются штатным lazy-механизмом |
| Вкладка TabSheet | Клик по вкладке → `onTabSheetSelectedTabChange` → lazy-загрузка справочников вкладки при первом открытии |
| Кнопка OK (`windowCommitAndClose`) | Нажал OK → проверка обязательных полей → commit (с нормализацией NULL-чекбоксов) → закрытие |
| Кнопка Отмена (`windowClose`) | Нажал Отмена → закрытие без сохранения |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

```
window iteraction-editor (100%×100%)
└─ layout (expand=iteractionMainLayout)
   └─ hbox iteractionMainLayout (edit-screen-layout)
      ├─ vbox iteractionSidebar (edit-sidebar, 270px, тёмная #172638→#0f1b28)
      │   ├─ visual: круглая иллюстрация типа 176×176 (ovalImage, theme-ресурс icons/dictionaries/iteraction.png, dictionary-logo-image)
      │   ├─ identity: labelItercationName (18px/700 белый) + подзаголовок «Тип взаимодействия» (12px)
      │   ├─ label-navigation: заголовок-полоса «Разделы формы» (36px, #ffb11b, inset-линии) + 8 пунктов (27px/13px/600, active #ffb11b)
      │   ├─ labelWarning (iteraction-editor-warning, янтарный блок)
      │   └─ spacer
      └─ vbox iteractionWorkspace (edit-workspace)
          ├─ toolbar (edit-toolbar): «Редактировать взаимодействие» (19px/700) + описание (12px)
          ├─ tabSheet (iteraction-tabs edit-tabs): 8 вкладок, 48px/15px/600, активная — нижняя полоса #ffb11b
          │   └─ контент: карточки-панели (groupBox showAsPanel + edit-card, заголовки 17px/700/50px)
          │       · tabType: Основные параметры (номер + дерево в одной строке, mandatory, наименование) + Пиктограмма
          │       · tabSigns: 4 карточки признаков попарно
          │       · outstaffingTab: чекбокс + 2 карточки (статус штата, признаки проекта)
          │       · tabIcons: карточка «Кнопка» (call-кнопка/форма/диалог)
          │       · tabNotifictions: карточка «Уведомления» с 3 подкарточками (когда/кому/период)
          │       · tabSetup: карточки «Дополнительное поле», «Календарь», «Email и памятки» (2 LOB)
          │       · setupWidgets: чекбокс + карточка с accordion виджетов
          │       · checkTrace: карточка «Тип отслеживания» + «Следующее взаимодействие» (twin column)
          └─ footer editActions (edit-footer-actions): spacer + группа AUTO (iteraction-primary-action / iteraction-secondary-action)
```

Стили: локальный partial `iteraction-editor.scss` (7 тем, идентичные копии) поверх общего `edit-screen-shared-styles.scss` (`edit-sidebar`, `edit-toolbar`, `edit-tabs`, `edit-form-control`, `edit-card`, `edit-footer-actions`); поля форм — `edit-form-control` (38px/15px, фокус-кольцо), подписи 13px/600. Сообщения: `messages.properties`/`messages_ru.properties` пакета `iteraction` (ключи `msgToolbarDescription`, `msgNavigationTitle`, `msgNav*` — новые, 2026-08-11).

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-13 | Sidebar-иконка типа взаимодействия заменена на круглую статичную иллюстрацию `ovalImage` 176×176 (theme-ресурс `icons/dictionaries/iteraction.png`, stylename `dictionary-logo-image`) — эталон JobCandidateEdit; убраны стили `iteraction-editor-icon-preview` 96px из `iteraction-editor.scss` (7 тем), добавлен блок `.dictionary-logo-image` 176px; контрактный тест `DictionaryEditFormIllustrationContractTest` |
| 2026-08-13 | Чекбоксы формы переведены на общие стили темы CUBA Platform (Valo): из локального партиала `iteraction-editor.scss` (7 тем) удалена кастомная стилизация квадратика/подписи (`padding: 3px 0` + подпись 14px/1.4) — штатные отступы темы исключают наезд чекбокса на элементы под ним; контрактный тест `DictionaryEditFormsCheckboxContractTest` |
| 2026-08-11 | Поле «Номер» (`numberField`) возвращено из sidebar во вкладку 1 «Тип взаимодействия» — строка «номер + корневой элемент дерева» в карточке «Основные параметры» (как до рефакторинга). Служебная карточка `iteraction-service-card` из sidebar удалена (пуста) |
| 2026-08-11 | Label-навигация теперь видна на **всех** 8 вкладках: убрано скрытие контейнера на одноблочных вкладках «Кнопка», «Всплывающие сообщения», «Настройки виджетов» (правило 3.6 не применяется); активный пункт подсвечивается на каждой вкладке. CDP-подтверждение: navVisible=true + корректный активный пункт на вкладках 4/5/7 |
| 2026-08-11 | CDP-верификация соответствия эталону IteractionListEdit (тема halo, 1440×812): sidebar 312px/#172638, полоса-заголовок 15px/700/36px + inset-линии, навигация активный 13px/600 #ffb11b / неактивные rgba(248,250,252,.82), поля ввода 15px/38px, подписи полей 13px/600 rgb(108,118,128), чекбоксы 14px — идентичны эталону; активный пункт навигации переключается по всем 8 вкладкам (по одному), скрытие навигации на одноблочных вкладках «Кнопка»/«Уведомления»/«Виджеты» (правило 3.6) подтверждено; карточка «Настройки виджетов» получила заголовок 17px/700/50 |
| 2026-08-11 | Полный визуальный рефакторинг по эталону IteractionListEdit: sidebar + workspace, label-навигация по 8 вкладкам (presentation-only Java), карточки-панели (`showAsPanel` + `edit-card`), toolbar/footer по общему Edit-контракту, локальный `iteraction-editor.scss` × 7 тем, контрактный тест `IteractionEditLayoutContractTest` (11/11). Бизнес-логика и поведение элементов не изменялись |
