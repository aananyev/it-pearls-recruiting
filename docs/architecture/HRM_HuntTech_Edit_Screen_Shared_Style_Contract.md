# Общий контракт стилей Edit-экранов HRM HuntTech

> Область: экранные формы CUBA Platform 7.3 типа Edit и сопоставимые сложные формы с постоянной левой контекстной панелью.  
> Статус: **обязательное нормативное дополнение** к `HRM_HuntTech_UI_UX_Design_Concept.md`.  
> Эталон: `ExtSettingsWindow` (`SettingsWindow`) как подтверждённый пример sidebar, label-навигации и правой рабочей области.  
> Текущий этап: изменяются только инструкции и документация; Java, XML и SCSS приложения не изменяются.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Все существующие и будущие Edit-экраны HRM HuntTech должны использовать один визуальный язык и одинаковые семантические имена CSS-стилей. Пользователь не должен заново изучать расположение навигации, toolbar, карточек и аккордеонов при переходе между формами.

До утверждения этого контракта одинаковые элементы получали экранные имена: `settings-section-navigation`, `user-ai-profile-navigation`, `candidate-cv-navigation`, `iteraction-list-navigation` и другие. Такое копирование усложняет сопровождение семи тем, создаёт расхождения геометрии и заставляет каждый новый экран повторять SCSS `SettingsWindow`.

Контракт вводит общий UI API из нейтральных `stylename`. Экран сохраняет собственный корневой namespace, component ID и CUBA-контракты, но применяет единые классы к типовым визуальным ролям.

### UI Context & Navigation

Контракт применяется к:

- левой контекстной панели Edit-формы;
- вертикальной label-навигации внутри sidebar;
- правой рабочей области;
- toolbar и контейнеру существующих действий;
- карточкам, аккордеонам, пояснениям и footer-actions;
- вкладочным формам, где набор label-навигации зависит от активной вкладки.

`ExtSettingsWindow` используется как эталон геометрии и состояний. Его локальные имена не становятся зависимостью других экранов: подтверждённые решения переводятся в общие нейтральные stylename.

### Behavior Summary

- открытие Edit-формы → отображается постоянная sidebar и правая рабочая область → загрузка данных и lifecycle не меняются;
- нажатие пункта label-навигации → раскрывается или фокусируется связанная секция → entity, validation и save lifecycle не меняются;
- смена активного пункта → добавляется `label-nav-item-active` → размеры и порядок компонентов остаются стабильными;
- смена темы → общий theme-aware mixin сохраняет геометрию → меняются только допустимые цвета, прозрачность и контраст;
- создание нового Edit-экрана → используются общие stylename → отдельная копия CSS `SettingsWindow` не создаётся;
- миграция существующего экрана → legacy-stylename заменяются по отдельной задаче → component ID и бизнес-логика сохраняются.

## 1. Нормативный статус и приоритет

1. Этот документ обязателен совместно с `HRM_HuntTech_UI_UX_Design_Concept.md`.
2. При конфликте с ранее созданными локальными CSS-классами, копиями стилей или несогласованными изменениями Hermes приоритет имеет настоящий контракт и прямые указания Алексея.
3. Фактический код не переписывается автоматически: противоречащая реализация мигрирует отдельной задачей и отдельным PR.
4. Общие stylename определяют визуальную роль, а не принадлежность конкретному экрану.
5. Каждый экран сохраняет собственный корневой класс, например `.ext-settings-window`, `.job-candidate-editor`, `.candidate-cv-editor`.
6. Общие stylename являются управляемым UI API HRM HuntTech и могут использоваться несколькими экранами.
7. Неограниченные глобальные Vaadin-селекторы `.v-label`, `.v-button`, `.v-panel`, `.v-textfield`, `.v-tabsheet` и аналогичные запрещены.
8. Вложенные Vaadin-селекторы допускаются только внутри общего семантического класса или локального root namespace.
9. Документ не разрешает менять entity, datasource/dataContainer, property binding, loaders, JPQL, views, actions, `invoke`, validators и сохранение.

## 2. Правило именования

### 2.1. Stylename и component ID — разные контракты

Единое имя задаётся через `stylename`. XML `id` остаётся уникальным и отражает назначение компонента внутри конкретного экрана.

```xml
<vbox id="interfaceSettingsNavigation"
      width="100%"
      spacing="false"
      stylename="label-navigation">
```

Здесь:

- `interfaceSettingsNavigation` — локальный component ID `ExtSettingsWindow`;
- `label-navigation` — единый общий CSS stylename всех экранных форм.

Массово переименовывать legacy component ID ради унификации CSS запрещено. Такое изменение допускается только после проверки Java-инъекций, `@Subscribe`, `invoke`, тестов и расширяющих XML.

### 2.2. Формат общих имён

- нижний регистр;
- слова разделяются дефисом;
- имя описывает визуальную роль;
- имя не содержит название сущности, экрана или вкладки;
- состояние оформляется дополнительным классом, а не отдельным компонентом.

## 3. Обязательный контракт label-навигации

### 3.1. Единственное имя блока

Контейнер вертикальной label-навигации во всех существующих и будущих экранных формах называется строго:

```text
label-navigation
```

Имена `settings-section-navigation`, `user-ai-profile-navigation`, `ai-settings-navigation`, `candidate-cv-navigation`, `iteraction-list-navigation` и аналогичные считаются legacy-stylename. Они не используются в новых формах и подлежат поэкранной миграции отдельными задачами.

### 3.2. Утверждённый набор классов

| Stylename | Назначение | Обязательный контракт |
|---|---|---|
| `label-navigation` | контейнер вертикального индекса разделов | ширина 100%, единый внутренний ритм, визуальное отделение от соседних sidebar-блоков |
| `label-nav-title` | заголовок набора навигации | вторичная типографика, uppercase, не является кликабельным пунктом |
| `label-nav-item` | обычный пункт навигации | label-подобная поверхность, перенос текста, hover и keyboard focus |
| `label-nav-item-active` | активное состояние пункта | применяется только совместно с `label-nav-item`, акцентный цвет и левая граница без изменения размеров |

Другие имена для тех же четырёх ролей запрещены в новых реализациях.

### 3.3. XML-пример по модели SettingsWindow

```xml
<vbox id="interfaceSettingsNavigation"
      width="100%"
      spacing="false"
      stylename="label-navigation">
    <label value="msg://interfaceSettingsNavigationTitle"
           width="100%"
           stylename="label-nav-title"/>

    <button id="interfaceWindowNav"
            caption="msg://interfaceSettingsWindowSection"
            width="100%"
            stylename="borderless label-nav-item label-nav-item-active"
            invoke="focusInterfaceWindowSection"/>

    <button id="interfaceAppearanceNav"
            caption="msg://interfaceSettingsAppearanceSection"
            width="100%"
            stylename="borderless label-nav-item"
            invoke="focusInterfaceAppearanceSection"/>
</vbox>
```

Статический информационный пункт допустимо оставить `<label>`, если он не выполняет действие. Пункт, который меняет focus, раскрывает секцию или прокручивает рабочую область, должен быть доступным с клавиатуры компонентом, обычно borderless-кнопкой.

### 3.4. Active-state

- активный пункт всегда содержит `label-nav-item label-nav-item-active`;
- контроллер добавляет и удаляет только `label-nav-item-active`;
- базовый `label-nav-item` не заменяется;
- active-state не меняет высоту, ширину, padding и положение соседних пунктов;
- одновременно активен один пункт текущего navigation-набора, если UI-спецификация не описывает иной подтверждённый сценарий;
- при переключении вкладки показывается релевантный `label-navigation` либо обновляется его состав без изменения данных.

### 3.5. Разрешённое поведение

Разрешено:

- раскрыть соответствующий `GroupBoxLayout`;
- перевести keyboard focus к заголовку или первому полю секции;
- прокрутить правую рабочую область к секции;
- обновить active-state.

Запрещено без отдельной функциональной задачи:

- менять entity;
- запускать loader;
- переключать `TabSheet` вместо фокусировки раздела текущей вкладки;
- выполнять save/commit;
- менять required, validators или editable;
- создавать новый бизнес-action.

## 4. Единый набор sidebar-stylename

| Stylename | Роль |
|---|---|
| `edit-sidebar` | корневая поверхность левой контекстной панели |
| `edit-sidebar-visual` | область фотографии, логотипа, иконки или иного визуального образа |
| `edit-sidebar-identity` | контейнер имени экземпляра и краткого описания |
| `edit-sidebar-title` | основное человекочитаемое имя экземпляра или раздела |
| `edit-sidebar-subtitle` | должность, тип, статус или краткий вторичный контекст |
| `edit-sidebar-summary` | компактная карточка ключевых атрибутов |
| `edit-sidebar-hint` | нейтральная подсказка |
| `edit-sidebar-warning` | предупреждение или ограничение без изменения бизнес-логики |
| `edit-sidebar-spacer` | служебный expand-компонент, отделяющий нижние подсказки |

Обязательный порядок:

```text
визуальный образ → наименование → label-navigation → детализация → прочее
```

Если отдельная роль отсутствует в модели экрана, соответствующий блок пропускается. Порядок оставшихся блоков сохраняется.

## 5. Единый набор stylename правой части Edit-экранов

### 5.1. Структура рабочей области

| Stylename | Роль |
|---|---|
| `edit-screen-layout` | двухпанельная композиция Edit-экрана |
| `edit-workspace` | корневой контейнер правой рабочей области |
| `edit-workspace-scroll` | вертикально прокручиваемая область контента |
| `edit-workspace-content` | внутренний поток карточек и аккордеонов |
| `edit-tabs` | TabSheet рабочей области с единым визуальным контрактом |
| `edit-toolbar` | верхняя поверхность заголовка и существующих действий |
| `edit-toolbar-title` | основной заголовок рабочей области |
| `edit-toolbar-description` | поясняющий текст toolbar |
| `edit-toolbar-actions` | контейнер существующих действий справа |
| `edit-card` | постоянно видимая тематическая карточка |
| `edit-card-title` | заголовок карточки |
| `edit-help` | поясняющий текст внутри рабочей области |
| `edit-accordion-section` | полноширинный `GroupBoxLayout` по эталону SettingsWindow |
| `edit-footer-actions` | нижняя панель штатных действий save/cancel и других существующих actions |

### 5.2. Базовая XML-композиция

```xml
<hbox width="100%" height="100%" expand="workspaceScroll" stylename="edit-screen-layout">
    <vbox width="270px" height="100%" stylename="edit-sidebar">
        <!-- visual → identity → label-navigation → summary → hint -->
    </vbox>

    <scrollBox id="workspaceScroll"
               width="100%"
               height="100%"
               orientation="vertical"
               scrollBars="vertical"
               stylename="edit-workspace edit-workspace-scroll">
        <vbox width="100%" spacing="true" stylename="edit-workspace-content">
            <hbox width="100%" stylename="edit-toolbar">
                <!-- title, description, existing actions -->
            </hbox>

            <groupBox width="100%"
                      collapsable="true"
                      collapsed="false"
                      showAsPanel="true"
                      stylename="edit-accordion-section">
                <!-- existing fields and bindings -->
            </groupBox>
        </vbox>
    </scrollBox>
</hbox>
```

`edit-screen-layout` не отменяет локальный root style конкретного экрана.

### 5.3. Геометрический контракт по эталону SettingsWindow

- sidebar: 270 px, при viewport до 1366 px — 250 px;
- toolbar: минимальная высота 58 px;
- tabs: высота строки 48 px;
- однострочные поля: минимальная высота 38 px;
- карточки и аккордеоны: радиус 7–8 px;
- toolbar/card shadow: лёгкая, без тяжёлого web-эффекта;
- основной внутренний padding карточки: 16–22 px;
- аккордеон занимает 100% полезной ширины;
- рабочая область имеет `min-width: 0`;
- основная горизонтальная прокрутка считается дефектом, кроме специализированных таблиц.

## 6. Архитектура SCSS и семи тем

### 6.1. Единый источник

При последующей реализации общие классы выносятся в один shared SCSS partial/mixin, подключаемый каждой из семи тем:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Рекомендуемая роль файла:

```text
common/edit-screen-shared-styles.scss
```

Точный путь определяется отдельной задачей после аудита структуры тем. Семь независимых копий одинаковых правил в `*-ext.scss` запрещены.

### 6.2. Theme-aware требования

Рабочая область использует переменные темы:

```scss
$v-app-background-color
$v-panel-background-color
$v-font-color
$v-selection-color
$v-border-radius
```

Фирменная sidebar может сохранять палитру HRM HuntTech:

```text
#172638 → #132130 → #0f1b28
```

Акцент активной label-навигации определяется единым shared mixin. Нельзя задавать разные геометрию и active-state для разных экранов.

### 6.3. Область общих селекторов

Разрешено:

```scss
.label-navigation { ... }
.label-nav-item { ... }
.edit-toolbar { ... }
.edit-accordion-section .v-panel-caption { ... }
.edit-workspace .v-textfield { ... }
```

Запрещено без ограничения:

```scss
.v-label { ... }
.v-button { ... }
.v-panel-caption { ... }
.v-textfield { ... }
```

## 7. Пример миграции ExtSettingsWindow

`ExtSettingsWindow` остаётся эталоном поведения и геометрии, но его текущие локальные stylename являются источником общей модели, а не именами для копирования.

| Текущий stylename SettingsWindow | Утверждённый общий stylename |
|---|---|
| `settings-section-sidebar`, `user-ai-profile-sidebar`, `ai-settings-sidebar` | `edit-sidebar` |
| `settings-section-navigation`, `user-ai-profile-navigation`, `ai-settings-navigation` | `label-navigation` |
| `settings-section-navigation-title`, `user-ai-profile-navigation-title`, `ai-settings-navigation-title` | `label-nav-title` |
| `settings-section-nav-item`, `user-ai-profile-nav-item`, `ai-settings-nav-item` | `label-nav-item` |
| `settings-section-nav-item-active`, `user-ai-profile-nav-item-active`, `ai-settings-nav-item-active` | `label-nav-item-active` |
| `settings-section-content`, `user-ai-profile-content`, `ai-settings-content` | `edit-workspace` / `edit-workspace-content` по роли контейнера |
| `settings-section-toolbar`, `user-ai-profile-toolbar`, `ai-settings-toolbar` | `edit-toolbar` |
| `settings-section-toolbar-actions`, `user-ai-profile-toolbar-actions`, `ai-settings-toolbar-actions` | `edit-toolbar-actions` |
| `settings-section-card`, `ai-settings-card` | `edit-card` |
| `settings-section-card-title`, `ai-settings-card-title` | `edit-card-title` |
| `user-ai-profile-section` и почтовые accordion-классы | `edit-accordion-section` |
| `ext-settings-footer` | `edit-footer-actions` |

В текущей документационной задаче XML и SCSS `ExtSettingsWindow` не меняются. Таблица определяет целевое состояние отдельного рефакторинга.

## 8. Применение к существующим и будущим формам

### 8.1. Новые формы

Новый Edit-экран сразу использует:

- локальный root namespace;
- `edit-screen-layout`;
- sidebar-классы из раздела 4;
- точный блок `label-navigation` и три дочерних класса;
- workspace-классы из раздела 5;
- существующие CUBA components, actions и bindings.

Создание новых экранных аналогов общих классов запрещено.

### 8.2. Существующие формы

Миграция выполняется по одной форме на отдельную ветку и PR:

1. зафиксировать актуальный `master` и UI-спецификацию;
2. проверить Java-инъекции и расширяющие XML;
3. добавить общие stylename, временно сохранив legacy-класс при необходимости;
4. перевести active-state на `label-nav-item-active`;
5. подключить shared SCSS без копирования;
6. выполнить профильные tests, `ScreenViewIntegrityTest 8/8`, SCSS build, clean assemble и visual smoke семи тем;
7. удалить legacy stylename только после подтверждения отсутствия зависимостей;
8. обновить UI-спецификацию и историю.

Массовая замена всех экранов одним непроверенным коммитом запрещена.

## 9. Критерии приёмки будущей реализации

Hermes подтверждает:

1. в DOM присутствует релевантный контейнер `label-navigation`;
2. заголовок использует `label-nav-title`;
3. каждый пункт содержит `label-nav-item`;
4. активный пункт дополнительно содержит `label-nav-item-active`;
5. legacy navigation-stylename отсутствуют либо оставлены как временная совместимость;
6. sidebar использует общие классы без потери контекста;
7. правая часть использует утверждённые workspace-классы;
8. compiled CSS содержит общие правила во всех семи темах;
9. геометрия соответствует `ExtSettingsWindow` в Halo и сохраняется в остальных темах;
10. navigation-клики не меняют entity и не запускают незапланированные loaders;
11. component ID, bindings, actions, `invoke`, required, validators и save/cancel не изменены;
12. горизонтальная прокрутка формы отсутствует;
13. Tomcat critical errors отсутствуют; P1=0; P2=0.

## 10. Запрещённые решения

- создавать контейнер `<screen>-navigation` вместо `label-navigation`;
- использовать `label-nav` как имя контейнера;
- применять `label-nav-item-active` без `label-nav-item`;
- копировать SCSS `SettingsWindow` в каждый экран;
- использовать namespace `ExtSettingsWindow` как зависимость другой формы;
- объявлять семь независимых копий общих правил;
- менять component ID, bindings или бизнес-логику под видом CSS-унификации;
- использовать неограниченные глобальные Vaadin-селекторы;
- считать реализацию принятой только по compile, HTTP 200 или `BUILD SUCCESSFUL` без visual smoke.

## 11. Текущее состояние на 2026-07-27

В `master` Hermes уже добавил частичные общие классы `label-nav-title`, `label-nav-item` и `label-nav-item-active` во все семь theme extension. Эти изменения не заменяют настоящий контракт:

- отсутствует утверждённая реализация контейнера `label-navigation`;
- правила продублированы по theme extension вместо shared partial/mixin;
- существующие формы продолжают использовать локальные screen-specific stylename;
- набор sidebar и workspace-классов пока существует только как нормативный контракт;
- цвет и геометрия существующих частичных классов должны быть приведены к этому документу при отдельной реализации.

Следующий этап — отдельная задача на shared SCSS-архитектуру и поэкранную миграцию. Настоящий этап не изменяет код приложения.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Утверждены единое имя блока `label-navigation`, классы `label-nav-title`, `label-nav-item`, `label-nav-item-active`, общий набор sidebar/workspace-stylename и целевое соответствие на примере `ExtSettingsWindow` |
