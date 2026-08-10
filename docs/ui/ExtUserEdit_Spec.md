# UI Spec: `sec$User.edit` / `ExtUserEdit`

> Канонический документ: `docs/ui/ExtUserEdit_Spec.md`  
> Legacy-зеркало: `docs/screens/ext-user/hunttech_ExtUserEdit_Spec.md`  
> Нормативы: [общий Edit-контракт](../architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md) · [UI/UX-концепция](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md)

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-10 | Блок «Профиль» sidebar получил заголовок-полосу 1:1 с заголовком «Разделы» (контракт §4.1: две inset-линии, `#ffb11b` 15px/700, min-height 36px; растянут на ширину карточки `margin: -10px -10px 12px`) и основную информацию из вкладки «Общие настройки»: статус (`msgStatusActive`/`msgStatusBlocked`), Email (`userDs.email`), должность (`userDs.position`) — подписи `ext-user-editor-profile-caption`/значения `-value` по эталону sidebar-caption/value. `ExtUserEditor` заполняет sidebar-лейблы (ФИО, login, статус, Email, должность) в `refreshProfileLabels()` из `userDs` (protected из `UserEditor`) при `postInit` и по `ItemChangeListener`; presentation-only, entity/datasource не мутируются. Добавлены `msgEmail`/`msgPosition` в messages (ru/en), стили `ext-user-editor-profile-*` во все 7 тем (sha256 идентичны). Селекторы — с удвоенным классом `edit-sidebar-summary .v-label.ext-user-editor-profile-*` (специфичность `(0,5,0)` против общего `.edit-sidebar-summary .v-label` `(0,4,0)` — иначе его `color`/`white-space` перекрывают полосу; проверено CDP). Контракт-тест расширен до 14 тестов (`profileBlockShowsMainUserInfoFromGeneralSettings`), `assertFalse(setValue)` заменён на проверку «setValue только presentation-лейблам». CDP-верификация (2026-08-10): полоса «Профиль» computed-стили 1:1 с «Разделы» (`#ffb11b`, 15px/700, min-height 36px, две inset-линии), лейблы заполнены (ФИО/login/статус/Email). |
| 2026-08-10 | Sidebar приведён к эталону OpenPositionEdit: растянут на всю высоту окна с вертикальной прокруткой. Root layout получил `width/height="100%"` и `spacing="false"`; у `profilePanel` убран `margin="true"`; `profilePanelSpacer` заменён с label на vbox `100%×100%`; footer `bottomActionsBox` перенесён из корневого layout внутрь `userWorkspace` (как в OpenPositionEdit). Причина: раньше footer + spacing корня съедали высоту `mainSplit`, sidebar обрывался на 678px вместо полных 747px (CDP-замер до/после). После фикса: sidebar top=56/bottom=803 — вся высота окна, `overflowY: auto`, `scrollHeight > clientHeight` (скролл активен). Контракт-тест `ExtUserEditSharedStyleContractTest` 13/13 PASS (footer-структура проверяется по файлу и не зависит от вложенности). |
| 2026-08-10 | Вкладка «Общие настройки» получила вертикальный скроллинг: `generalScrollBox` переведён с AUTO-высоты на `height="100%"` + `expand="generalScrollBox"` (единая прокрутка всей вкладки, паттерн email-вкладки); `rolesSubstSplit` перенесён внутрь scrollBox с фиксированной высотой `300px`. Причина: CDP-замеры на 1366×768/1280×720/1024×640 показали обрезку нижних полей ввода (`overflow: hidden` вкладки) и схлопывание split до 0px (scrollBox без height забирал весь expand). После фикса на всех разрешениях `overflowY: auto`, полоса прокрутки активна, split 300px виден и достижим прокруткой; на fullscreen 1440×812 все 16 полей ввода видны без прокрутки. Контракт-тест расширен до 13 тестов (`generalSettingsTabScrollsVertically`). |
| 2026-08-10 | Заголовки вкладок `settingsTabSheet` приведены 1:1 к эталону IteractionListEdit копированием финального слоя `iteraction-list-reference-finish.scss` (`.edit-tabs`): таб-полоса 48px, подписи 15px/600 через переменные темы Halo, прозрачный фон табов, выбранная вкладка — нижняя полоса-акцент `#ffb11b` 3px, hover `#ffb11b`, tabcontainer `padding: 0 20px` + `border-bottom: rgba($v-font-color,.16)`, content `calc(100% - 49px)`. Из stylename убран `framed` (вало-framed-рендер табов ломал эталонный стиль). Footer перестроен по структуре эталона (expand-спейсер `bottomActionsSpacer` + группа `bottomActionsGroup` AUTO/MIDDLE_RIGHT): кнопки ОК/Отмена прижаты в правый нижний угол экрана. Проверено CDP: вкладки 48px/15px/600, selected/hover `#ffb11b` + полоса 3px; footer right=29/bottom=20. Контракт-тест расширен до 12 тестов (tabsheet-эталон, footer-геометрия). |
| 2026-08-10 | По указанию пользователя: изображение заменено на `OvaFallbackImage` (180×180, круглый аватар, fallback `icons/no-programmer.jpeg` в 7 темах); единая label-навигация из трёх пунктов, повторяющих вкладки правого tabsheet — клик переключает вкладку (`setSelectedTab`), активный пункт = текущая вкладка (отступление от §3.5/§3.6 по прямому указанию); hover-стиль пунктов и палитра sidebar приведены 1:1 к эталону IteractionListEdit (белый текст на `rgba(255,255,255,.08)`, `border-top: rgba(255,255,255,.16)`); секционные focus-методы и их Java-инъекции удалены. |
| 2026-08-10 | Оформление кнопок формы по эталону IteractionListEdit: `changePasswordBtn` переведён с `friendly` на локальный `ext-user-editor-primary-action` (белый текст на `$v-selection-color`), footer-кнопка «ОК» (класс `c-primary-action` фрагмента `editWindowActions`) — primary-акцент, «Отмена» — secondary (прозрачный фон); единая геометрия кнопок в локальном SCSS (`min-height: 38px`, `padding: 0 16px`, `font-size: 14px`, без тени, hover `brightness(0.98)`, focus-ring). |
| 2026-07-29 | Экран приведён к общему `edit-*`/`label-*` контракту: фиксированная sidebar, вкладочная label-навигация, toolbar, карточки, почтовые accordion-секции и footer; bindings и бизнес-логика сохранены. |
| 2026-07-29 | Восстановлена стандартная смена пароля через `sec$User.changePassword`. |
| 2026-06-29 | `userPic` привязан к `userDs.officialPhoto`; удалено ручное переключение fallback. |

## Назначение и бизнес-смысл (What & Why)

`ExtUserEdit` — административная форма HRM HuntTech для управления учётной записью пользователя: профилем, контактами, региональными параметрами, ролями, замещениями, email и персональными AI-подключениями. Единая компоновка снижает риск ошибки администратора: редактируемый пользователь постоянно идентифицируется в sidebar, а действия и данные размещены в предсказуемой рабочей области.

Визуальная миграция не меняет модель пользователя, права, password policy, алгоритм сохранения, роли, замещения и CRUD AI-конфигураций.

## UI Context & Navigation

- **Screen ID:** `sec$User.edit`.
- **Вход:** `sec$User.browse` → Create/Edit/двойной клик.
- **Контроллер:** `ExtUserEditor extends UserEditor`.
- **Дочерние окна:** `sec$User.changePassword`, `hunttech_UserAiConfiguration.edit`.
- **Вкладки:** общие настройки, email, персональный ИИ.
- **Sidebar:** visual → identity → navigation (вкладки правой части) → summary «Профиль» (полоса-заголовок §4.1 + статус, Email, должность).
- **Navigation:** единый набор из трёх пунктов, повторяющих вкладки правого tabsheet; клик по пункту переключает соответствующую вкладку.

## Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| открыть сохранённого пользователя | `userDs` valid | стандартный `UserEditor` загружает пользователя, роли и замещения; отображается общий layout; sidebar заполняется: ФИО, login, статус, Email, должность |
| сменить item пользователя | `userDs` item change | sidebar-лейблы обновляются (`refreshProfileLabels`); данные не меняются |
| открыть нового пользователя | `PersistenceHelper.isNew` | кнопка отдельной смены пароля скрыта; используются штатные `passw`/`confirmPassw` |
| сменить вкладку (клик по tab правой части) | выбран новый tab | активным становится пункт навигации текущей вкладки; данные не меняются |
| нажать navigation-пункт | пункт соответствует вкладке | `setSelectedTab` переключает вкладку правой части; `label-nav-item-active` переносится на выбранный пункт; entity/modified-state не меняются |
| сменить пароль | пользователь сохранён | открывается стандартный `sec$User.changePassword` |
| Save/Cancel | завершение редактирования | выполняются штатные actions `editWindowActions` и lifecycle `UserEditor` |

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| XML | `modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml` |
| Controller | `modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java` |
| Base editor | `com.haulmont.cuba.gui.app.security.user.edit.UserEditor` |
| Root namespace | `ext-user-editor` |
| Общий UI API | `edit-*`, `label-navigation`, `label-nav-*` |

## 2. Связь с моделью данных

| Datasource | Контракт |
|---|---|
| `userDs` | `User`, view `extUser-view` |
| `rolesDs` | `userRoles` |
| `substitutionsDs` | `substitutions` |
| `userAiConfigsDs` | `userAiConfiguration-view`, `e.user = :ds$userDs` |

Сохранены `fieldGroupLeft`, `fieldGroupRight`, все почтовые FieldGroup, `rolesTable`, `substTable`, `aiConfigsTable`, component ID, datasource/property, validators, actions и `invoke`. Новые entity getters не добавлены; `views.xml` и JPQL не изменены.

## 3. Иерархия форм

```text
sec$User.browse
└── sec$User.edit (ExtUserEditor → UserEditor)
    ├── sec$User.changePassword
    └── hunttech_UserAiConfiguration.edit
```

## 4. Модель поведения

### Lifecycle

1. `UserEditor.init()` создаёт штатные actions и custom controls.
2. `UserEditor.postInit()` применяет платформенные ограничения.
3. `ExtUserEditor.postInit()` скрывает password-dialog для новой сущности, назначает созданным полям `edit-form-control`, синхронизирует navigation активной вкладки и заполняет sidebar-лейблы профиля (`refreshProfileLabels`) из `userDs` (protected поле `UserEditor`); при смене item обновление повторяется по `userDs.addItemChangeListener`.
4. Commit/cancel полностью остаются в базовом редакторе.

### Presentation-навигация

Navigation-пункты объявлены в XML как keyboard-доступные borderless-кнопки и повторяют вкладки правого tabsheet («Общие настройки», «Настройки почты», «Персональный ИИ»). Контроллер:

- переключает вкладку вызовом `setSelectedTab(name)` (только `switchToTab` по клику пункта);
- не выполняет load, refresh или commit;
- не изменяет datasource, entity, selection и значения;
- добавляет/удаляет только `label-nav-item-active`, сохраняя `label-nav-item` у всех пунктов;
- активный пункт синхронизируется с выбранной вкладкой через `addSelectedTabChangeListener`.

Отступление от §3.5/§3.6 общего контракта («навигация не переключает TabSheet»; «набор из одного пункта не создаётся») выполнено по прямому указанию пользователя: навигация управляет вкладками правой части, AI-вкладка имеет собственный пункт навигации. Бизнес-логика не затронута.

## 5. Actions & Buttons Logic

| Элемент | Цепочка |
|---|---|
| `changePasswordBtn` | `invoke="changePassword"` → `sec$User.changePassword`; оформление `ext-user-editor-primary-action` (эталон IteractionListEdit: белый текст на `$v-selection-color`) |
| `generalTabNav` | click → `switchToTab("generalSettingsTab")` → `setSelectedTab` + активный пункт |
| `emailTabNav` | click → `switchToTab("emailSettingsTab")` → `setSelectedTab` + активный пункт |
| `aiTabNav` | click → `switchToTab("aiSettingsTab")` → `setSelectedTab` + активный пункт |
| role/substitution/AI buttons | существующие actions/listeners без изменения |
| `windowActions` | штатные Save/Cancel; «ОК» (класс `c-primary-action`) — primary-акцент, «Отмена» — secondary (прозрачный фон); footer-структура эталона IteractionListEdit — expand-спейсер + группа AUTO прижимают обе кнопки в правый нижний угол экрана |

## 6. Визуальная компоновка

```text
layout.ext-user-editor (width/height 100%, spacing=false)
└── mainSplit.edit-screen-layout (100% × 100%)
    ├── profilePanel.edit-sidebar (270 px; 250 px ≤1366; растянут на всю высоту окна, вертикальная прокрутка при переполнении)
    │   ├── dropZone.edit-sidebar-visual (OvaFallbackImage 180×180 + upload)
    │   ├── profileLabelsVBox.edit-sidebar-identity
    │   ├── tabNavigation.label-navigation (заголовок — полоса-заголовок §4.1; 3 пункта = вкладки)
    │   ├── profileSummaryBox.edit-sidebar-summary («Профиль»: полоса-заголовок §4.1 + статус, Email, должность)
    │   └── profilePanelSpacer.edit-sidebar-spacer (vbox 100%×100%)
    └── userWorkspace.edit-workspace
        ├── userToolbar.edit-toolbar
        ├── settingsTabSheet.edit-tabs (заголовки вкладок — копия эталона IteractionListEdit)
        │   ├── general: единый vertical scrollBox `generalScrollBox` (height=100%, expand вкладки)
        │   │   ├── passwordBox.edit-card (новый пользователь)
        │   │   ├── contactsRegionalRow (contactsCard.edit-card + regionalCard.edit-card)
        │   │   └── rolesSubstSplit (split 300px: rolesPanel.edit-card + substPanel.edit-card)
        │   ├── email: пять edit-accordion-section
        │   └── AI: edit-card
        └── bottomActionsBox.edit-footer-actions (expand-спейсер bottomActionsSpacer + bottomActionsGroup AUTO → ОК/Отмена в правом нижнем углу; внутри workspace — эталон OpenPositionEdit, sidebar остаётся на всю высоту)
```

### Общие stylename

| Роль | Stylename |
|---|---|
| root/sidebar | `edit-screen-layout`, `edit-sidebar`, `edit-sidebar-visual`, `edit-sidebar-identity`, `edit-sidebar-title`, `edit-sidebar-subtitle`, `edit-sidebar-summary`, `ext-user-editor-profile-title`/`-status`/`-caption`/`-value`, `edit-sidebar-spacer` |
| navigation | `label-navigation`, `label-nav-title` (+ `ext-user-editor-navigation-title` — полоса §4.1), `label-nav-item`, `label-nav-item-active` |
| workspace | `edit-workspace`, `edit-toolbar`, `edit-toolbar-title`, `edit-toolbar-description`, `edit-toolbar-actions`, `edit-tabs` |
| buttons | `ext-user-editor-primary-action` (акцентная), `c-primary-action` (ОК footer-фрагмента), вторичная — `:not(.c-primary-action)` в footer; единая геометрия `.v-button` (38px, без тени, hover/focus) |
| content/footer | `edit-card`, `edit-card-title`, `edit-help`, `edit-form-control`, `edit-accordion-section`, `edit-footer-actions` |

### Email accordion

| Section | Начальное состояние |
|---|---|
| `propertiesEmailBox` — серверы | раскрыта |
| `emailPortsBox` — порты | свёрнута |
| `emailAuthenticationBox` — требование пароля | свёрнута |
| `emailAccountsBox` — учётные записи | свёрнута |
| `emailPasswordsBox` — пароли | свёрнута |

### Локальный SCSS

`ext-user-editor.scss` синхронно присутствует в семи темах и подключается после `edit-screen-shared-styles`. Он ограничен `.ext-user-editor` и задаёт уникальную тёмную sidebar с палитрой IteractionListEdit (градиент `#172638→#132130→#0f1b28`), полосы-заголовки «Разделы» и «Профиль» (контракт §4.1: две inset-линии, `#ffb11b` 15px/700, min-height 36px; у «Профиль» растяжение на ширину карточки `margin: -10px -10px 12px`), статус/подписи/значения блока «Профиль» (`ext-user-editor-profile-status` 13px/600, `-caption` 10.5px/700 uppercase `rgba(248,250,252,.62)`, `-value` 13px/500 `#f8fafc` — 1:1 с sidebar-caption/value эталона; все селекторы — `edit-sidebar-summary .v-label.ext-user-editor-profile-*`, чтобы специфичность `(0,5,0)` побеждала общий `.edit-sidebar-summary .v-label` `(0,4,0)`), hover пунктов по эталону (белый текст на `rgba(255,255,255,.08)`), активный пункт `#ffb11b` на `rgba(255,177,27,.12)` с `border-left-color: #ffb11b`, круглый аватар `ext-user-editor-avatar` (180×180, `border-radius: 50%`, fallback `object-view-box: inset(8%)`), контрактные подписи полей 13px/600, защиту внутренних Vaadin-контейнеров от переполнения и заголовки вкладок `.edit-tabs` — точную копию финального слоя эталона `iteraction-list-reference-finish.scss` (полоса 48px, подписи 15px/600 через `$v-*`-переменные, selected/hover `#ffb11b` + нижняя полоса 3px, tabcontainer `padding: 0 20px`, content `calc(100% - 49px)`). Общие размеры cards/controls/toolbar/footer не копируются.

### Обоснованные отклонения

1. `OvaFallbackImage` сохранён размером 180×180 (эталон IteractionListEdit — 96×96): крупный аватар административной формы с зоной upload-фото, обрезка круглая как в эталоне.
2. Навигация повторяет вкладки правого tabsheet и переключает их `setSelectedTab` — отступление от §3.5 (запрет переключения TabSheet) и §3.6 (одиночный пункт AI) выполнено по прямому указанию пользователя; навигация остаётся presentation-only (loaders, данные, selection не затрагиваются).
3. `rolesSubstSplit` сохранён для одновременного сравнения ролей и замещений; перенесён внутрь единого вертикального `generalScrollBox` вкладки с фиксированной высотой `300px` — без этого (как expand-ребёнок вкладки рядом с scrollBox без height) он схлопывался в 0px, а нижние поля ввода обрезались на разрешениях ≤1366×768. Вся вкладка прокручивается вертикально (CUBA ScrollBoxLayout, политика VERTICAL по умолчанию).
4. Кнопка смены пароля перенесена в toolbar без изменения ID, caption, icon и `invoke`.
5. `contactsCard`/`regionalCard` используют заголовок-`label` (`edit-card-title`) внутри groupBox с `showAsPanel="true"` вместо caption groupBox — сохранение исходного рендера полей FieldGroup.

## 7. Проверки

`ExtUserEditSharedStyleContractTest` (14 тестов) защищает общий stylename API, XML bindings/actions, navigation по вкладкам (presentation-only), `OvaFallbackImage` и fallback-иконку в семи темах, пять accordion-секций, XML comments, одинаковый локальный SCSS семи тем, копию заголовков вкладок эталона IteractionListEdit (`.edit-tabs`, без `framed`, полоса 48px/15px/600, `#ffb11b`, content `calc(100% - 49px)`), footer-структуру правого нижнего угла (`bottomActionsSpacer` expand + `bottomActionsGroup` AUTO), вертикальный скроллинг вкладки «Общие настройки» (`generalScrollBox` height=100% + expand, `rolesSubstSplit` внутри с высотой 300px) и блок «Профиль» (`profileBlockShowsMainUserInfoFromGeneralSettings`: полоса-заголовок §4.1, лейблы статус/Email/должность, заполнение из `userDs`, messages ru/en). Sidebar-геометрия эталона OpenPositionEdit дополнительно проверена CDP (2026-08-10): `.edit-sidebar` занимает всю высоту root (top=56/bottom=803 при 812px окна), `overflowY: auto`, `scrollHeight > clientHeight` — вертикальный скролл активен. Дополнительно сохраняется `ExtUserChangePasswordContractTest`; Hermes выполняет `ScreenViewIntegrityTest 8/8`, SCSS build, clean assemble, local deploy, HTTP 200 и browser smoke.

---

Обязательные UI-документы прочитаны. Общие `edit-*` и `label-*` stylename использованы преимущественно. Локальные отклонения перечислены и обоснованы. Бизнес- и CUBA-контракты формы сохранены.
