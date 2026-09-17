---
name: hunttech-edit-screen-design
description: >-
  Стандарт и руководство по разработке и рефакторингу экранных форм редактирования (Edit Screens)
  в HRM HuntTech (CUBA Platform 7.3 / Vaadin): двухпанельная композиция Split-View с сайдбаром 312px
  (scrollBox, аватар 120px, 4-уровневая типографика, быстрые действия, секции рейтингов и реквизитов,
  label-навигация), правая рабочая область (edit-toolbar, tabSheet с margin="false", аккордеон-секции
  edit-accordion-section, строки полей edit-form-control, постоянный footer действий), Data View Integrity
  и синхронизация 7 SCSS-тем.
---

# Стандарт и руководство по разработке и рефакторингу форм Edit (HuntTech Edit Screen Design)

Данный навык фиксирует проверенный промышленный стандарт проектирования, XML-дескрипторов, Java-контроллеров и SCSS-стилизации для создания и модернизации **форм редактирования (Edit-экранов)** в системе **HuntTech HRM** (на основе эталонов `JobCandidateEdit`, `OpenPositionEdit`, `IteractionListEdit`).

---

## 1. Архитектурная концепция Two-Pane Split View

Форма редактирования строится по двухпанельной схеме:
1. **Постоянный левый Сайдбар (312px)**: контекст редактируемой сущности, визуальный образ, ключевые параметры, статус, рейтинг, действия и секционная навигация.
2. **Правая рабочая область (Workspace)**: тулбар формы, вкладки (TabSheet), прокручиваемый контент с карточками полей (Accordion Sections) и подвал действий (Footer Actions).

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Меню приложения / Заголовок окна (DialogMode 1400×900 или Fullscreen)                            │
├───────────────────────┬──────────────────────────────────────────────────────────────────────────┤
│ ЛЕВЫЙ САЙДБАР (312px) │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ (edit-workspace)                                  │
│ (edit-sidebar)        │ ┌──────────────────────────────────────────────────────────────────────┐ │
│                       │ │ Toolbar (edit-toolbar): Заголовок и описание формы                   │ │
│ • scrollBox (100% h)  │ ├──────────────────────────────────────────────────────────────────────┤ │
│ • Аватар / Лого 120px │ │ Вкладки (TabSheet: edit-tabs, margin="false"):                       │ │
│ • 4 уровня шапки      │ │ [Основное]  [Детали]  [Файлы]  [Навыки]  [История]                   │ │
│ • Панель действий     │ │ ┌──────────────────────────────────────────────────────────────────┐ │ │
│ • Статус и Рейтинг ★  │ │ │ Скроллер контента (edit-workspace-scroll):                       │ │ │
│ • Сетка реквизитов    │ │ │ ┌──────────────────────────────────────────────────────────────┐ │ │ │
│ • Чипы навыков        │ │ │ │ Секция 1: «Наименование» (edit-accordion-section)            │ │ │ │
│ • Label-навигация     │ │ │ │ [ID: 110px] [Грейд: 143px] [Название: expandRatio=1] [Кнопка]│ │ │ │
│ • Spacer 16px         │ │ │ ├──────────────────────────────────────────────────────────────┤ │ │ │
│                       │ │ │ │ Ряд карточек 50/50 (edit-cards-row):                         │ │ │ │
│                       │ │ │ │ ┌──────────────────────────┐ ┌─────────────────────────────┐ │ │ │ │
│                       │ │ │ │ │ Карточка А               │ │ Карточка Б                  │ │ │ │ │
│                       │ │ │ │ │ [Поле 1]      [Поле 2]   │ │ [Поле 3]         [Поле 4]   │ │ │ │ │
│                       │ │ │ │ └──────────────────────────┘ └─────────────────────────────┘ │ │ │ │
│                       │ │ │ └──────────────────────────────────────────────────────────────┘ │ │ │
│                       │ │ └──────────────────────────────────────────────────────────────────┘ │ │
│                       │ ├──────────────────────────────────────────────────────────────────────┤ │
│                       │ │ Подвал действий (edit-footer-actions): [Сохранить]  [Отмена]         │ │
│                       │ └──────────────────────────────────────────────────────────────────────┘ │
└───────────────────────┴──────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Стандарт оформления левого Сайдбара (Sidebar 312px)

Сайдбар обеспечивает непрерывный контекст и навигацию по длинной форме.

### 2.1. Контейнер и Скроллинг
- Сайдбар обязательно имеет ширину **312px** (`stylename="edit-sidebar"`).
- Все внутренние блоки размещаются в вертикальном скроллере:
  `<scrollBox id="...SidebarScroll" width="100%" height="100%" orientation="vertical">` с внутренней стопкой `<vbox width="100%" spacing="true">`.
- В конце скроллера размещается фиксированный спейсер:
  `<vbox id="...SidebarSpacer" width="100%" height="16px" stylename="edit-sidebar-spacer"/>` (без `height="100%"`).

### 2.2. Шапка профиля и визуальный образ (Profile Header)
- Контейнер: `<vbox align="TOP_CENTER" width="100%" spacing="true" stylename="edit-sidebar-visual">`
- Круглый аватар:
  `<ovaFallbackImage width="120px" height="120px" ovalWidth="120px" ovalHeight="120px" align="TOP_CENTER" stylename="job-candidate-avatar" scaleMode="SCALE_DOWN" fallbackThemePath="..."/>`
  (или парный `hbox` 96×96 для проектов/вакансий `open-position-editor-logo-box`).
- Четырёхуровневая типографика шапки (`edit-sidebar-identity`):
  - **Level 1 (Главный заголовок)**: `stylename="edit-sidebar-title h2 candidate-sidebar-fullname bold"` (ФИО / Название вакансии).
  - **Level 2 (Подзаголовок)**: `stylename="edit-sidebar-subtitle h4 candidate-sidebar-position bold"` (Должность / Проект).
  - **Level 3 (Компания / Город)**: `stylename="edit-help candidate-sidebar-city bold"` (Город / Компания).
  - **Level 4 (Локация / Формат)**: `stylename="edit-help candidate-sidebar-city"` (Формат работы).

### 2.3. Карточки данных и Секции
- **Заголовки секций**: `<label stylename="label-nav-title ...-section-title"/>` (полоса с фоном `rgba(255,255,255,0.045)`, нижней границей и двумя inset-линиями).
- **Сетка реквизитов**: `<grid spacing="true" width="100%" stylename="edit-sidebar-summary">` с 2 колонками:
  - Левая колонка: `<label value="Вилка по ТК:" stylename="bold"/>`
  - Правая колонка: `<label id="...Val" value="-"/>` (чистый текст через NVL без сырых HTML `<div>`).
- **Секция навыков**: 3-уровневые чипы (`candidate-skills-chips`) — Primary (золотые ★), Secondary (синие/серебряные), Other (серые).

### 2.4. Секционная навигация (Label Navigation)
- `<vbox id="...Navigation" width="100%" spacing="false" stylename="label-navigation">`
- Наборы кнопок по вкладкам: `<button id="..." caption="..." stylename="borderless label-nav-item"/>`.
- При клике: активируется класс `label-nav-item-active` (жёлтый акцент `#ffb11b` с левой полосой) и фокус переводится в первый элемент соответствующей секции.

---

## 3. Стандарт оформления правой Рабочей Области (Workspace)

### 3.1. Тулбар формы (Edit Toolbar)
```xml
<hbox id="editorToolbar" width="100%" spacing="true" align="MIDDLE_LEFT" expand="toolbarTitleBox" stylename="edit-toolbar">
    <vbox id="toolbarTitleBox" width="100%" spacing="false">
        <label id="editorToolbarTitle" value="msg://editorCaption" stylename="edit-toolbar-title"/>
        <label id="editorToolbarDescription" value="msg://editorDescription" stylename="edit-toolbar-description"/>
    </vbox>
</hbox>
```

### 3.2. Система вкладок (TabSheet) и Золотое правило Margin="false"
- На корневом `tabSheet`: `stylename="framed edit-tabs" width="100%" height="100%"`.
- **Критическое правило**: на ВСЕХ `<tab>` обязательно указывать `margin="false"`.
  - *Обоснование*: `margin="true"` в Vaadin TabSheet добавляет неотключаемый 12px отступ, который дублируется с padding контента и приводит к сдвигу аккордеон-секций на разных вкладках.
- Внутри каждой вкладки:
  `<scrollBox orientation="vertical" spacing="false" height="100%" width="100%" stylename="edit-workspace-scroll">`
  `<vbox spacing="false" stylename="edit-workspace-content">`

### 3.3. Карточки полей и Аккордеон-секции (Accordion Sections)
- Все логические блоки оборачиваются в `<groupBox>`:
  `stylename="edit-accordion-section" showAsPanel="true" collapsable="true" collapsed="false"`
- **Строки полей (Field Rows)**:
  - Строка 50/50: `<hbox width="100%" spacing="true" stylename="...-field-row ...-row-half">` (поля с `box.expandRatio="1"` и `width="100%"`).
  - Строка с растяжением названия: `<hbox width="100%" spacing="true" stylename="...-field-row ...-row-title">` (ID `width="110px"`, Грейд `width="143px"`, Название `width="100%" box.expandRatio="1"`, Кнопка `width="110px"`).
  - Широкая строка на всю ширину: `<hbox width="100%" spacing="true" expand="..." stylename="...-field-row ...-row-wide">`.
- **Поля ввода**: ко всем `textField`, `lookupField`, `lookupPickerField`, `dateField`, `richTextArea` применяется единый класс `stylename="edit-form-control"`.

### 3.4. Компоновка полей с учетом длины данных (Data-length Adaptive Layout)
При проектировании и рефакторинге форм редактирования запрещается слепо растягивать все контролы на 50% или 100% ширины только ради заполнения сетки. Компоновка обязана быть адаптивной к реальной длине данных:

1. **Компактные числовые и оценочные значения (оценки 1..5, рейтинг, дни, число позиций, проценты)**:
   - Ширина от `120px` до `160px` (`width="140px"`), либо при нахождении в фиксированной сетке — `width="100%" maxWidth="240px" align="MIDDLE_LEFT"`.
   - Исключаются широкие пустые поля на пол-экрана для одного-двух символов.
2. **Короткие структурированные строковые данные (даты/время, телефоны, мессенджеры, способы связи, коды)**:
   - Дата со временем: `width="220px"` (ровно под 16 символов даты-времени).
   - Выпадающие списки справочников/календарей: `width="280px"`–`width="320px"`.
   - Способы коммуникации (телефон, Telegram, email): `width="100%" maxWidth="540px"`. Это предотвращает визуальный разрыв между подписью поля и контролом на Full HD / 4K мониторах.
3. **Длинные текстовые значения (наименования вакансий, компаний, ФИО кандидатов с action-кнопками picker'а)**:
   - Ширина 50% (~600–700px в стандартном окне) либо 100% с `box.expandRatio="1"`, что дает комфортное пространство без сокращения текста.
4. **Полноширинные многострочные текстовые поля (комментарии, заметки, протоколы)**:
   - Полноширинный `textArea` (`width="100%"`), с фиксированной комфортной высотой (`height="170px"`, `rows="7"`), обеспечивающей обзор без избыточного скроллинга.
5. **Полноширинные чекбоксы с длинным текстом**:
   - Чекбоксы с длинными наименованиями («Только мои подписки», «Добавить напоминание в календарь» и т.п.) размещаются отдельной строкой с `width="100%"`, исключая сжатие лейбла и перенос слов на новую строку.
6. **Сохранение контрактных сеток тестов ядра**:
   - Если тесты ядра (`...VisualAlignmentTest.java`) проверяют фиксированную структуру колонок (например, ровно 6 колонок `<column width="50%"/>`), запрещено менять разбивку колонок на 3 или 4 (20/40/40). Ограничение по длине данных достигается исключительно через атрибуты `maxWidth="..."` и `align="MIDDLE_LEFT"` внутри ячейки 50%.

### 3.5. Панель быстрых действий (Quick Actions) и контрастная типографика
Для форм с частыми типовыми сценариями (например, кнопки популярных типов взаимодействий, статусов, быстрых шаблонов):
- **Фиксированная сетка**: панель разбивается на равные ячейки (например, 5 ячеек по 20% с `height="64px"`).
- **Контрастная типографика**: подпись кнопки `.v-button-caption` обязана иметь принудительный высокий контраст (белый `#ffffff !important` на зеленом/акцентном фоне `#008000`, полужирный `font-weight: 600`, размер `13px`).
- **Многострочный текст**: поддержка переноса текста до 2–3 строк с вертикальным центрированием. Полный текст обязательно дублируется в стандартный tooltip (`description`).
- **Устранение каскадных конфликтов**: стили кнопок не должны переопределяться и конфликтовать между несколькими SCSS-файлами одной темы; каскад должен быть строго централизован.

### 3.6. Постоянный подвал действий (Footer Actions)
```xml
<hbox id="editorFooterActions" width="100%" spacing="true" align="MIDDLE_RIGHT" stylename="edit-footer-actions">
    <button id="windowCommitAndCloseButton" caption="mainMsg://actions.Ok" icon="CHECK" stylename="primary" action="windowCommitAndClose"/>
    <button id="windowCloseButton" caption="mainMsg://actions.Cancel" icon="BAN" stylename="secondary" action="windowClose"/>
</hbox>
```

---

## 4. Эталонный XML-дескриптор Edit-экрана

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<window xmlns="http://schemas.haulmont.com/cuba/screen/window.xsd"
        caption="msg://editorCaption"
        focusComponent="mainTabScrollBox"
        messagesPack="com.company.hunttech.web.screens.myentity">
    <data>
        <instance id="myEntityDc" class="com.company.hunttech.entity.MyEntity">
            <view extends="myEntity-edit-view">
                <!-- Data View Integrity: обязательное декларирование всех вложенных полей -->
                <property name="department" view="_minimal"/>
                <property name="city" view="_minimal"/>
            </view>
            <loader id="myEntityDl"/>
        </instance>
    </data>

    <dialogMode height="900px" width="1400px"/>

    <layout expand="mainLayout" spacing="false" width="100%" height="100%" stylename="edit-screen-layout my-entity-editor">
        <hbox id="mainLayout" width="100%" height="100%" spacing="false" expand="workspaceBox" stylename="edit-screen-layout">
            
            <!-- ===== 1. ЛЕВЫЙ САЙДБАР (312px) ===== -->
            <vbox id="sidebarPane" width="312px" height="100%" spacing="false" stylename="edit-sidebar">
                <scrollBox id="sidebarScroll" width="100%" height="100%" orientation="vertical">
                    <vbox width="100%" spacing="true">
                        
                        <!-- Шапка профиля: Аватар 120px + 4 уровня типографики -->
                        <vbox id="profileHeader" width="100%" spacing="true" align="TOP_CENTER" stylename="edit-sidebar-visual">
                            <ovaFallbackImage id="pic" width="120px" height="120px" ovalWidth="120px" ovalHeight="120px"
                                              align="TOP_CENTER" stylename="job-candidate-avatar" scaleMode="SCALE_DOWN"
                                              fallbackThemePath="icons/no-programmer.jpeg"/>
                            <vbox width="100%" spacing="false" align="MIDDLE_CENTER" stylename="edit-sidebar-identity">
                                <label id="headerTitle" value="-" stylename="edit-sidebar-title h2 candidate-sidebar-fullname bold" width="100%" align="MIDDLE_CENTER"/>
                                <label id="headerSubtitle" value="-" stylename="edit-sidebar-subtitle h4 candidate-sidebar-position bold" width="100%" align="MIDDLE_CENTER"/>
                                <label id="headerCity" value="-" stylename="edit-help candidate-sidebar-city bold" width="100%" align="MIDDLE_CENTER"/>
                            </vbox>
                        </vbox>

                        <!-- Label-навигация разделов -->
                        <vbox id="sidebarNavigation" width="100%" spacing="false" stylename="label-navigation">
                            <label value="РАЗДЕЛЫ ФОРМЫ" width="100%" stylename="label-nav-title my-entity-section-title"/>
                            <button id="navMain" caption="Основное" width="100%" stylename="borderless label-nav-item label-nav-item-active"/>
                            <button id="navDetails" caption="Параметры" width="100%" stylename="borderless label-nav-item"/>
                        </vbox>

                        <!-- Секция: Реквизиты и сводка -->
                        <vbox id="summarySection" width="100%" spacing="true" stylename="label-navigation">
                            <label value="РЕКВИЗИТЫ" width="100%" stylename="label-nav-title my-entity-section-title"/>
                            <grid id="summaryGrid" spacing="true" width="100%" stylename="edit-sidebar-summary">
                                <columns count="2"/>
                                <rows>
                                    <row><label value="Статус:" stylename="bold"/><label id="statusVal" value="-"/></row>
                                    <row><label value="Телефон:" stylename="bold"/><label id="phoneVal" value="-"/></row>
                                </rows>
                            </grid>
                        </vbox>

                        <!-- Фиксированный спейсер отступа внизу -->
                        <vbox id="sidebarSpacer" width="100%" height="16px" stylename="edit-sidebar-spacer"/>
                    </vbox>
                </scrollBox>
            </vbox>

            <!-- ===== 2. ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ ===== -->
            <vbox id="workspaceBox" width="100%" height="100%" spacing="false" expand="mainTabSheet" stylename="edit-workspace">
                
                <!-- Toolbar формы -->
                <hbox id="toolbar" width="100%" spacing="true" align="MIDDLE_LEFT" expand="toolbarTitleBox" stylename="edit-toolbar">
                    <vbox id="toolbarTitleBox" width="100%" spacing="false">
                        <label id="toolbarTitle" value="msg://editorCaption" stylename="edit-toolbar-title"/>
                        <label id="toolbarDescription" value="msg://editorDescription" stylename="edit-toolbar-description"/>
                    </vbox>
                </hbox>

                <!-- Вкладки (margin="false") -->
                <tabSheet id="mainTabSheet" stylename="framed edit-tabs" width="100%" height="100%">
                    <tab id="mainTab" caption="msg://mainTab" margin="false" icon="USER">
                        <scrollBox id="mainTabScrollBox" orientation="vertical" spacing="false" height="100%" width="100%" stylename="edit-workspace-scroll">
                            <vbox id="mainTabContent" spacing="false" stylename="edit-workspace-content">
                                
                                <!-- Секция «Наименование» -->
                                <groupBox id="nameGroupBox" caption="msg://nameSection" showAsPanel="true" collapsable="true" collapsed="false" stylename="edit-accordion-section" width="100%">
                                    <hbox id="nameRow" spacing="true" width="100%" stylename="my-entity-field-row">
                                        <textField id="codeField" dataContainer="myEntityDc" property="code" caption="msg://code" width="110px" stylename="edit-form-control"/>
                                        <textField id="nameField" dataContainer="myEntityDc" property="name" caption="msg://name" width="100%" box.expandRatio="1" required="true" stylename="edit-form-control"/>
                                    </hbox>
                                </groupBox>

                                <!-- Секция «Параметры» (50/50) -->
                                <groupBox id="paramsGroupBox" caption="msg://paramsSection" showAsPanel="true" collapsable="true" collapsed="false" stylename="edit-accordion-section" width="100%">
                                    <hbox id="paramsRow" spacing="true" width="100%" stylename="my-entity-field-row">
                                        <lookupField id="typeField" dataContainer="myEntityDc" property="type" caption="msg://type" width="100%" box.expandRatio="1" stylename="edit-form-control"/>
                                        <lookupPickerField id="cityField" dataContainer="myEntityDc" property="city" caption="msg://city" width="100%" box.expandRatio="1" stylename="edit-form-control"/>
                                    </hbox>
                                </groupBox>
                            </vbox>
                        </scrollBox>
                    </tab>
                </tabSheet>

                <!-- Footer действий -->
                <hbox id="footerActions" width="100%" spacing="true" align="MIDDLE_RIGHT" stylename="edit-footer-actions">
                    <button id="commitAndCloseBtn" caption="mainMsg://actions.Ok" icon="CHECK" stylename="primary" action="windowCommitAndClose"/>
                    <button id="closeBtn" caption="mainMsg://actions.Cancel" icon="BAN" stylename="secondary" action="windowClose"/>
                </hbox>
            </vbox>
        </hbox>
    </layout>
</window>
```

---

## 5. Требования к Java-контроллеру и Data View Integrity

1. **Безопасное управление классами стилей**:
   - Никогда не вызывать `setStyleName("new-class")` для динамического изменения цвета/состояния, так как это стирает все базовые CSS-классы.
   - Использовать методы: `removeStyleName("old-state")` и `addStyleName("new-state")`.
2. **Строгое соблюдение Data View Integrity**:
   - Любой вызов геттера (`entity.getDepartment().getName()`) требует обязательного декларирования свойства в fetch-плане контейнера `<view extends="...">`, исключая `UnfetchedAttributeException` и `LazyInitializationException`.
3. **Null-safe вывод значений (NVL)**:
   ```java
   private String nvl(String val) {
       return (val != null && !val.trim().isEmpty()) ? val.trim() : "-";
   }
   ```

---

## 6. Синхронизация SCSS и 7 тем

- Все стили формы инкапсулируются в `@mixin <screen>-editor-theme` внутри namespace `.<screen>-editor`.
- Файл стилей обязан быть **побайтово идентичен** во всех 7 темах:
  `modules/web/themes/{halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark}/com.company.hunttech/<screen>-editor.scss`.
- Проверка через контрактный тест `...LayoutContractTest` и `ocr review --audience agent`.

---

## 7. Основные принципы и чек-лист рефакторинга Edit-форм

При получении задачи на рефакторинг формы редактирования (Edit-экрана) строго следовать следующему регламенту:

1. **Предварительный аудит и сохранение контрактов**:
   - [ ] Зафиксировать границы изменений (Preserve Behavior, Improve Presentation). Не изменять бизнес-логику Java-контроллера, JPQL, views, загрузчики и имена существующих ID компонентов.
   - [ ] Проверить существующие автоматические тесты ядра и веба формы (`*Test.java`). Если тесты проверяют точное количество колонок 50% или определенные XML-структуры, не ломать их геометрию, а адаптивность реализовывать через `maxWidth="..."` и выравнивания.
2. **Эталонный левый Сайдбар (312px)**:
   - [ ] Обернуть все карточки сайдбара в полновысотный вертикальный скроллер `<scrollBox id="...SidebarScroll" width="100%" height="100%" orientation="vertical">`.
   - [ ] Использовать внутренний карточный контейнер `<vbox id="...SidebarContentBox" width="100%" height="AUTO" spacing="true">`.
   - [ ] Разместить в самом низу скроллера фиксирующий спейсер `<vbox id="...SidebarSpacer" width="100%" height="16px" stylename="edit-sidebar-spacer"/>`.
3. **Компоновка полей с учетом длины данных (Data-length Adaptive)**:
   - [ ] Короткие числовые/оценочные поля (оценки 1..5, рейтинг, проценты): компактная ширина `width="140px"` или `maxWidth="240px"` с `align="MIDDLE_LEFT"`.
   - [ ] Короткие форматированные поля (даты/время, телефоны, способы связи, Lookup-списки): `width="220px"`–`280px"` или `maxWidth="540px"`.
   - [ ] Длинные строковые поля (названия вакансий, компаний, ФИО): 50% (~600–700px) или 100% с `box.expandRatio="1"`.
   - [ ] Чекбоксы с длинным текстом: отдельная строка с `width="100%"`, без сокращений и переносов.
   - [ ] Поле развернутого комментария/протокола: полноширинный `textArea` (`width="100%"`, `height="170px"`, `rows="7"`).
4. **Быстрые действия и кнопки (Quick Actions)**:
   - [ ] Обеспечить принудительный контрастный белый цвет подписи `.v-button-caption` (`#ffffff !important`, полужирный 600, вертикальное центрирование над акцентным фоном).
   - [ ] Предотвратить обрезку или выпадение названий кнопок за пределы плашек, дублировать полное название в tooltip (`description`).
   - [ ] Устранить дублирование селекторов в разных SCSS-файлах темы.
5. **Синхронизация стилей и проверка**:
   - [ ] Проверить побайтовую идентичность SCSS во всех 7 темах оформления (`halo`, `havana`, `helium`, `hover`, `hunttech-modern-dark`, `hunttech-modern-light`, `hunttech-modern`).
   - [ ] Запустить тесты ядра и веба формы через `bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test ... :app-web:test ...`.
   - [ ] Актуализировать проектную спецификацию экрана (`docs/ui/..._Spec.md`).

