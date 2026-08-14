# ProjectEdit (`hunttech_Project.edit`)

Cross-links: [docs/entities/project/Project.md](../entities/project/Project.md) · общий контракт: [HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md) · legacy-версия спецификации: [hunttech_Project.edit_Spec.md](../screens/project/hunttech_Project.edit_Spec.md)

---

## Бизнес-контекст (обязательный ввод)

### Назначение и Бизнес-смысл (What & Why)

Форма редактирует **проект** (`Project`) — контекстную единицу рекрутинга HRM HuntTech, объединяющую заказчика и организацию работы над группой вакансий. Проект задаёт наименование, принадлежность к департаменту, владельца (рекрутера/менеджера), даты старта и окончания, признак «проект по умолчанию», чаты для общения с заказчиком и для резюме кандидатов, логотип, развёрнутое описание и шаблон сопроводительного письма. Вкладка «Вакансии» показывает открытые позиции проекта; признак «Проект закрыт» управляет жизненным циклом: форма предлагает закрыть все открытые вакансии проекта и блокирует редактирование ключевых реквизитов.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из `hunttech_Project.browse` действиями создания и редактирования проекта как полноэкранный модальный диалог. Слева — sidebar: визуальный блок (логотип проекта 176×176 с загрузкой/очисткой), наименование и роль «Проект», label-навигация «Разделы» (пункты = вкладки TabSheet правой части — «Наименование проекта», «Описание проекта», «Вакансии», «Информация в сопроводительном письме»), подсказка. Справа — workspace: toolbar («Проект» + описание назначения), вкладки «Наименование проекта», «Описание проекта», «Вакансии», «Информация в сопроводительном письме» и footer-действия ОК/Отмена. Справочники департаментов, владельцев и родительских проектов выбираются через lookup-компоненты (picker_lookup).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие формы → проект существует → загружаются основные данные и справочники, запрос связанных вакансий блокируется до установки параметра проекта; в sidebar подставляется наименование проекта.
- Первое открытие вкладки «Вакансии» → проект сохранён → loader получает параметр `project`, выполняет JPQL и показывает вакансии проекта; вкладки «Описание проекта» и «Информация в сопроводительном письме» лениво догружают LOB-поля.
- Изменение признака «Проект закрыт» → есть открытые вакансии → форма предлагает закрыть их (диалог подтверждения) и блокирует редактирование ключевых реквизитов проекта; при закрытии проставляется дата окончания.
- Сохранение проекта → изменился статус открытия/закрытия → HRM HuntTech публикует системное уведомление («Закрыт проект»/«Открыт новый проект»).
- Ввод URL чатов → ссылки-кнопки «Перейти» активируются и открывают чаты в новой вкладке.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_Project.edit` |
| **@UiDescriptor** | `project-edit.xml` (modules/web/src/com/company/hunttech/web/screens/project/) |
| **Режим окна** | `dialogMode height="100%" width="100%" modal="true"` — полноэкранный модальный редактор (контракт §5.3) |
| **@EditedEntityContainer** | `projectDc` |
| **Загрузка** | `@LoadDataBeforeShow` |
| **focusComponent** | `projectNameField` |
| **messagesPack** | `com.company.hunttech.web.screens.project` |
| **Открытие** | `hunttech_Project.browse` → create/edit (StandardEditor) |

## 2. Связь с моделью данных (Data & Entity Binding)

| Контейнер | Сущность | View | Назначение |
|-----------|----------|------|------------|
| `projectDc` (instance) | `Project` | `project-edit-view` | главный редактируемый экземпляр |
| `projectOpenPositionsDc` (collection) | `OpenPosition` | `openPosition-project-tab-view` | вакансии проекта (JPQL по `projectName`, `order by createTs desc`) |
| `projectTreeDc` (collection) | `Project` | `project-tree-picker-view` | родительские проекты (не закрытые, без «(не использовать)») |
| `projectDepartmentsDc` (collection) | `CompanyDepartament` | `companyDepartament-picker-view` | департаменты (cacheable, без «(не использовать)») |
| `projectOwnersDc` (collection) | `Person` | `person-picker-view` | владельцы проектов (cacheable, order by secondName, firstName) |

`project-edit-view` несёт: `projectName`, `projectIsClosed`, `defaultProject`, `startProjectDate`, `endProjectDate`, `generalChat`, `chatForCV`, `projectLogo` (_minimal), `projectTree` (project-tree-picker-view), `projectDepartment` (companyDepartament-picker-view), `projectOwner` (person-owner-view). LOB-поля `projectDescription` и `templateLetter` в view отсутствуют — контроллер догружает их лениво через `dataManager.reload(...).view(ViewBuilder.of(Project.class).add(...))` при первом открытии соответствующих вкладок.

**View integrity (ProjectDetachedObjectTest 4/4):** все property формы доступны на detached-объекте; FK-граф (projectTree → projectName, projectDepartment → departamentRuName, projectOwner → secondName) читается без LazyInitializationException; LOB-reload + commit detached не бросает исключений.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

```
hunttech_Project.browse
└─ ProjectEdit (hunttech_Project.edit, модальный 100%×100%)
   ├─ sidebar (edit-sidebar 270px)
   │  ├─ visual: ovaFallbackImage projectLogoFileImage (176×176, ovalWidth/ovalHeight, fallback icons/no-company.png, SCALE_DOWN — эталон JobCandidateEdit) + upload projectLogoFileUpload (dropZone=sidebar visual)
   │  ├─ identity: label projectSidebarTitle (наименование проекта) + subtitle «Проект»
   │  ├─ label-navigation «Разделы»: projectEditorNavMain / projectEditorNavDescription / projectEditorNavVacancy / projectEditorNavTemplate (кнопки-пункты = вкладки TabSheet)
   │  ├─ spacer + hint
   ├─ workspace (edit-workspace)
   │  ├─ toolbar (edit-toolbar): title «Проект» + description
   │  ├─ tabSheet projectTab (edit-tabs)
   │  │  ├─ tabProject: scrollBox → projectMainCard («Основные данные») + projectChatCard («Чаты»)
   │  │  ├─ tabProjectDescription: projectDescriptionCard (richTextArea LOB)
   │  │  ├─ tabVacansy: projectVacancyCard (dataGrid projectOpenPositionTable)
   │  │  └─ tabTemplateLetter: projectTemplateCard (richTextArea LOB)
   │  └─ footer (edit-footer-actions): windowCommitAndClose (primary) + windowClose (secondary)
   └─ lookup-экраны: CompanyDepartament.lookup / Person.lookup / Project.lookup (picker_lookup)
```

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл формы (Lifecycle)

- `onInit` — добавляет `preLoadListener` на `projectOpenPositionsDl`: автозагрузка вакансий блокируется (`preventLoad`), пока не установлен параметр проекта.
- `onBeforeShow` — сохраняет `beforeEdit`-снимок; для новой записи проставляет дату старта; устанавливает дату окончания по признаку «Закрыт»; загружает открытые вакансии проекта (для диалога закрытия); активирует/деактивирует ссылки чатов.
- `onBeforeShowSidebar` — подставляет наименование проекта в title sidebar (или общий заголовок «Проект» для новой записи).
- `onProjectTabSelectedTabChange` — ленивая загрузка: первое открытие вкладки «Описание проекта» → reload LOB `projectDescription`; «Информация в сопроводительном письме» → reload LOB `templateLetter`; «Вакансии» → установка параметра `project` и единственная отложенная загрузка коллекции.
- `onProjectTabSelectedTabChangeNav` — синхронизирует sidebar-навигацию: активный пункт (карта `TAB_TO_NAV_BUTTON`: имя вкладки → кнопка). Навигация видна на всех вкладках (указание владельца; правило контракта §3.6 не применяется).

### 4.2 Скрытые вычисления (без явного клика)

- `onBeforeShow1` — логотип отображается единым `OvaFallbackImage` (сам читает `projectLogo` из `projectDc` и показывает fallback `icons/no-company.png` при отсутствии файла); ручное переключение видимости/источника не требуется (эталон JobCandidateEdit).
- `setEndDateProject` — при «Проект закрыт» проставляет текущую дату окончания, иначе очищает её.
- `getOpenedPosition` — запрашивает открытые позиции проекта (`openClose = false`) для диалога подтверждения закрытия.
- `setButtonsForChats` — активирует/деактивирует ссылки чатов по наличию URL в полях `generalChat`/`chatForCV`.
- `sendGlobalEventsMessage` — сравнивает статус закрытия с `beforeEdit` и публикует `UiNotificationEvent` («Закрыт проект»/«Открыт новый проект»).

### 4.3 Валидация и сохранение

- Обязательные поля: `projectName`, `projectDepartment`, `projectOwner` (`required="true"`).
- `BeforeCommitChanges` → `sendGlobalEventsMessage`: для новой записи дефолтит `projectIsClosed=false`; при смене статуса (в том числе у существующей записи) публикует системное уведомление.
- Закрытие проекта с открытыми вакансиями: диалог «Закрыть вакансии на этом проекте?» → YES закрывает каждую открытую позицию (`openClose=true`), коммитит и публикует уведомления по каждой.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

- **«Наименование проекта» (projectEditorNavMain)** → клик → пункт активен, вкладка `tabProject` (`projectTab.setSelectedTab`).
- **«Описание проекта» (projectEditorNavDescription)** → клик → пункт активен, вкладка `tabProjectDescription` (ленивая загрузка LOB).
- **«Вакансии» (projectEditorNavVacancy)** → клик → пункт активен, вкладка `tabVacansy` (ленивая загрузка списка).
- **«Информация в сопроводительном письме» (projectEditorNavTemplate)** → клик → пункт активен, вкладка `tabTemplateLetter` (ленивая загрузка LOB).
- **«Проект закрыт» (checkBoxProjectIsClosed)** → включили + есть открытые вакансии → диалог подтверждения закрытия вакансий; в любом случае — проставляется/очищается дата окончания и блокируются/разблокируются ключевые поля (наименование, даты, департамент, владелец).
- **Ссылки чатов (generalChatLink/chatForCVLink)** → ввод URL в поле → ссылка активируется; клик открывает чат в новой вкладке (target=_blank).
- **Загрузка логотипа (projectLogoFileUpload)** → файл IMMEDIATE сохраняется в `projectLogo` (dataContainer `projectDc`), `OvaFallbackImage` автоматически показывает загруженное изображение; «Очистить» возвращает fallback `icons/no-company.png`.
- **Footer**: «Сохранить и закрыть» (windowCommitAndClose, primary) → валидация required + BeforeCommitChanges-обработчики; «Отмена» (windowClose, secondary) → закрытие без сохранения.

## 6. Визуальная компоновка элементов (Visual Layout Schema)

```
layout (project-editor)
└─ hbox (edit-screen-layout)
   ├─ vbox edit-sidebar (270px; ≤1366px → 250px через shared)
   │  ├─ edit-sidebar-visual: ovaFallbackImage 176×176 (border-radius 50%, без рамки/тени) + пара кнопок «Загрузить»/«Очистить» 96×36
   │  ├─ edit-sidebar-identity: title (#ffb11b 18px/700, центрировано) + subtitle (12px/400)
   │  ├─ label-navigation: полоса-заголовок «Разделы» (project-editor-navigation-title — 36px, #ffb11b 15px/700, inset-линии) + пункты 27px/13px/600 (active #ffb11b на rgba(255,177,27,.12), hover rgba(255,255,255,.08))
   │  ├─ edit-sidebar-spacer (100%×100%)
   │  └─ edit-sidebar-hint (12px/18px)
   └─ vbox edit-workspace
      ├─ edit-toolbar: title 20px/700 + description 12px/18px
      ├─ tabSheet edit-tabs (48px строка): вкладки «Наименование проекта»/«Описание проекта»/«Вакансии»/«Информация в сопроводительном письме»
      │  └─ Стили вкладок — ОБЩИЕ для Edit-форм (.edit-tabs в edit-screen-shared-styles.scss, эталон OpenPositionEdit): полоса 0 12px на панельном фоне с нижней линией; подпись 48px/14px/600 (#26384c, hover #1264b5, активная — $v-selection-color #4d7ab2 с линией 3px); контент вкладки padding 14px 16px 18px на mix(86%); горизонтальный скролл полосы при нехватке ширины
      │  └─ карточки edit-card (showAsPanel, 7–8px радиус): projectMainCard, projectChatCard, projectDescriptionCard, projectVacancyCard, projectTemplateCard
      └─ edit-footer-actions (62px, верхняя тень): primary project-editor-primary-action (#4d7ab2), secondary project-editor-secondary-action (прозрачная)

SCSS: themes/{7 тем}/com.company.hunttech/project-editor.scss (sha256-идентичны) +
@import/@include project-editor-theme в styles.scss; тёмный sidebar #172638→#132130→#0f1b28;
:before halo-темы у nav-кнопок отключён (display:none; content:none) — иначе подсветка выше текста.
```

### Стили и сообщения

- Локальные стили: `project-editor.scss` (7 тем), root-класс `project-editor`.
- Сообщения: `messages.properties` / `messages_ru.properties` пакета `com.company.hunttech.web.screens.project`; новые ключи: `msgProjectSidebarSubtitle`, `msgProjectToolbarDescription`, `msgProjectSidebarHint`, `msgProjectMainSection`, `msgProjectChatSection`.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-12 | Логотип sidebar приведён к размеру 176×176 (width/height/ovalWidth/ovalHeight в XML `projectLogoFileImage`) — в точности как `candidatePic` эталона JobCandidateEdit; контракт-тест обновлён (176px). |
| 2026-08-12 | Стили вкладок TabSheet — в ОБЩИЕ стили тем (эталон OpenPositionEdit): блок .edit-tabs в edit-screen-shared-styles.scss (полоса 0 12px, подпись 48px/14px/600 #26384c, hover #1264b5, активная $v-selection-color с линией 3px, контент 14px 16px 18px mix(86%)); локальный дубль из project-editor.scss убран; контракт-тест + новый тест-метод tabsStylesLiveInSharedThemeStyles (8/8) |
| 2026-08-12 | Label-навигация «Разделы» переведена на вкладки TabSheet правой части экрана (указание владельца): 4 пункта `projectEditorNavMain`/`projectEditorNavDescription`/`projectEditorNavVacancy`/`projectEditorNavTemplate`; клик по пункту → `projectTab.setSelectedTab(...)`; активный пункт синхронизируется по SelectedTabChange; навигация видна на всех вкладках (правило §3.6 не применяется) |
| 2026-08-12 | Рефакторинг по контракту Edit-форм: полноэкранный модальный диалог 100%×100%; sidebar 270px (визуал логотипа 96×96 с upload-кнопками 96×36, identity, label-навигация «Разделы» с полосой-заголовком, spacer, hint); workspace с toolbar и tabSheet edit-tabs; пять карточек edit-card (showAsPanel); edit-form-control на всех полях; footer edit-footer-actions с primary/secondary; presentation-only Java: навигация по карточкам вкладки «Проект», видимость навигации по правилу §3.6, динамический title sidebar; SCSS partial project-editor.scss во всех 7 темах; тесты ProjectEditLayoutContractTest (7/7) и ProjectDetachedObjectTest (4/4); Spec перенесён в docs/ui/ProjectEdit_Spec.md |
