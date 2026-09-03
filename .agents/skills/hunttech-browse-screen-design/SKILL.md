---
name: hunttech-browse-screen-design
description: >-
  Стандарт и руководство по разработке и оформлению экранных форм Browse и реестров в HRM HuntTech
  (CUBA Platform / Vaadin): Split-View с полновысотным сайдбаром 312px (scrollBox, аватар 120px,
  4-уровневая типографика, чипы навыков, рейтинг и готовность), командный тулбар, карточка Generic Filter,
  DataGrid с rowsCount 24px и адаптивная поддержка 7 SCSS-тем.
---

# Руководство по оформлению форм Browse и Реестров HRM HuntTech

Данный навык содержит архитектурный стандарт, шаблоны XML-дескрипторов, Java-контроллеров и SCSS-стилей для создания и модернизации экранов Browse (реестров) в системе **HuntTech HRM** (на базе эталонов `JobCandidateReestr` и `OpenPositionReestrBrowse`).

---

## 1. Концепция макета Split-View и Сайдбара (312px)

Современные экраны Browse строятся по схеме двухпанельного Split-View макета:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ Главное меню приложения (App Menubar 42px)                                                  │
├───────────────────┬─────────────────────────────────────────────────────────────────────────┤
│ ЛЕВЫЙ САЙДБАР     │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ (margin="true,true,true,false")                  │
│ (312px, 100% h)   │                                                                         │
│                   │ ┌─────────────────────────────────────────────────────────────────────┐ │
│ • Аватар 120px    │ │ Главный тулбар (tableFilterBar): [Действия слева]  [Фильтры справа] │ │
│ • 4 уровня шапки  │ ├─────────────────────────────────────────────────────────────────────┤ │
│ • Быстрые кнопки  │ │ Generic Filter карточка (collapsable="true" collapsed="true")       │ │
│ • Готовность/Рейт.│ ├─────────────────────────────────────────────────────────────────────┤ │
│ • Условия/Контакты│ │ Карточка таблицы (TreeDataGrid / DataGrid с rowsCount 24px)         │ │
│ • Теги навыков    │ └─────────────────────────────────────────────────────────────────────┘ │
└───────────────────┴─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Стандарт оформления Сайдбара

1. **Разметка и скроллинг**:
   - Сайдбар размещается в `<vbox width="312px" height="100%" spacing="true" stylename="job-candidate-sidebar edit-sidebar">` с обязательным внутренним `<scrollBox id="detailScroll" width="100%" height="100%" orientation="vertical">`.
2. **Аватар / Логотип (120×120px)**:
   - `<ovaFallbackImage width="120px" height="120px" ovalWidth="120px" ovalHeight="120px" align="TOP_CENTER" stylename="job-candidate-avatar" scaleMode="SCALE_DOWN"/>`
   - Fallback-пути: `icons/no-programmer.jpeg` (кандидаты), `icons/briefcase.png` (вакансии), `icons/no-candidate.png` (резюме/взаимодействия), `icons/no-company.png` (компании).
3. **Иерархия типографики шапки профиля**:
   - `h2 candidate-sidebar-fullname bold` — Заголовок (ФИО / Вакансия).
   - `h4 candidate-sidebar-position bold` — Подзаголовок (Должность / Проект).
   - `edit-help candidate-sidebar-city bold` — Город / Компания.
   - `edit-help candidate-sidebar-city` — Локация и формат работы.
4. **Секции карточек**:
   - Заголовки: `<label stylename="label-nav-title job-candidate-section-title"/>`
   - «Готовность и рейтинг»: индикаторы светофоров (🟢🟡⚪) и рейтинг ★.
   - «Условия и реквизиты»: `<grid stylename="edit-sidebar-summary">` с парами ключ (bold) — значение (NVL).
   - «Ключевые навыки»: 3-уровневые чипы (`stylename="candidate-skills-chips"`).

---

## 3. Структура XML-дескриптора

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<window xmlns="http://schemas.haulmont.com/cuba/screen/window.xsd"
        caption="msg://browseCaption"
        focusComponent="mainTable"
        messagesPack="com.company.hunttech.web.screens.myentity">
    <data readOnly="true">
        <collection id="myEntitiesDc" class="com.company.hunttech.entity.MyEntity">
            <view extends="myEntity-browse-view">
                <!-- Data View Integrity: декларировать все вложенные поля, используемые в сайдбаре и таблице -->
                <property name="department" view="_minimal"/>
                <property name="skillsList" view="_minimal"/>
            </view>
            <loader id="myEntitiesDl">
                <query><![CDATA[select e from hunttech_MyEntity e order by e.createTs desc]]></query>
            </loader>
        </collection>
    </data>
    <layout expand="splitMainLayout" spacing="false" margin="false" stylename="job-candidate-editor edit-screen-layout">
        <hbox id="splitMainLayout" width="100%" height="100%" spacing="true" expand="workspaceBox" stylename="job-candidate-main-layout edit-screen-layout">
            <!-- ЛЕВЫЙ САЙДБАР (312px на 100% высоты) -->
            <vbox id="detailPane" width="312px" height="100%" spacing="true" margin="false" stylename="edit-sidebar job-candidate-sidebar">
                <scrollBox id="detailScroll" width="100%" height="100%" orientation="vertical">
                    <vbox width="100%" spacing="true">
                        <!-- Шапка профиля -->
                        <vbox id="profileHeader" width="100%" spacing="true" align="TOP_CENTER" stylename="job-candidate-profile-header edit-sidebar-visual">
                            <ovaFallbackImage id="logoPic" width="120px" height="120px" ovalWidth="120px" ovalHeight="120px"
                                              align="TOP_CENTER" stylename="job-candidate-avatar" fallbackThemePath="icons/briefcase.png" scaleMode="SCALE_DOWN"/>
                            <vbox width="100%" spacing="false" align="MIDDLE_CENTER" stylename="edit-sidebar-identity">
                                <label id="detailTitle" value="Выберите запись" stylename="edit-sidebar-title h2 candidate-sidebar-fullname bold" width="100%" align="MIDDLE_CENTER"/>
                                <label id="detailSubtitle" value="" stylename="edit-sidebar-subtitle h4 candidate-sidebar-position bold" width="100%" align="MIDDLE_CENTER"/>
                                <label id="detailCompany" value="" stylename="edit-help candidate-sidebar-city bold" width="100%" align="MIDDLE_CENTER"/>
                                <label id="detailLocation" value="" stylename="edit-help candidate-sidebar-city" width="100%" align="MIDDLE_CENTER"/>
                            </vbox>
                        </vbox>

                        <!-- Кнопки действий сайдбара -->
                        <vbox id="sidebarActionsCard" width="100%" spacing="true" stylename="edit-sidebar-summary">
                            <button id="openEditCardBtn" caption="Открыть карточку" icon="EDIT_ACTION" stylename="primary" enabled="false" width="100%"/>
                            <button id="suggestBtn" caption="Подобрать подходящие" icon="font-icon:MAGIC" enabled="false" width="100%"/>
                        </vbox>

                        <!-- Готовность и рейтинг -->
                        <vbox id="readinessCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                            <label value="ГОТОВНОСТЬ И РЕЙТИНГ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>
                            <label id="detailReadiness" width="100%" stylename="job-candidate-readiness edit-help"/>
                            <label id="detailRating" width="100%" stylename="job-candidate-rating edit-help"/>
                        </vbox>

                        <!-- Условия и реквизиты -->
                        <vbox id="termsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                            <label value="УСЛОВИЯ И РЕКВИЗИТЫ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>
                            <grid id="termsGrid" spacing="true" width="100%" stylename="edit-sidebar-summary">
                                <columns count="2"/>
                                <rows>
                                    <row><label value="Статус:" stylename="bold"/><label id="detailStatus" value="-"/></row>
                                    <row><label value="Зарплата:" stylename="bold"/><label id="detailSalary" value="-"/></row>
                                </rows>
                            </grid>
                        </vbox>

                        <!-- Навыки -->
                        <vbox id="skillsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                            <label value="ОСНОВНЫЕ НАВЫКИ" width="100%" stylename="label-nav-title job-candidate-section-title" align="MIDDLE_CENTER"/>
                            <label id="detailSkillsLabels" width="100%" htmlEnabled="true" stylename="candidate-skills-chips"/>
                        </vbox>
                    </vbox>
                </scrollBox>
            </vbox>

            <!-- ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ -->
            <vbox id="workspaceBox" width="100%" height="100%" spacing="true" stylename="edit-workspace candidate-reestr-workspace" expand="tableCard" margin="true,true,true,false">
                <!-- Главный командный тулбар -->
                <cssLayout id="tableFilterBar" width="100%" stylename="candidate-filter-bar edit-card">
                    <cssLayout id="leftActionButtons" stylename="filter-buttons-panel left-action-buttons">
                        <button id="createBtn" caption="Создать" icon="CREATE_ACTION" stylename="primary candidate-btn"/>
                        <button id="editBtn" caption="Редактировать" icon="EDIT_ACTION" stylename="secondary candidate-btn" enabled="false"/>
                        <button id="removeBtn" caption="Удалить" icon="REMOVE_ACTION" stylename="secondary candidate-btn" enabled="false"/>
                    </cssLayout>
                    <cssLayout id="rightActionButtons" stylename="filter-buttons-panel right-action-buttons">
                        <popupButton id="filterPopupButton" caption="Все записи" icon="FILTER" stylename="secondary candidate-btn"/>
                        <popupButton id="actionsPopupButton" caption="Действия" icon="BARS" stylename="primary"/>
                    </cssLayout>
                </cssLayout>

                <!-- Generic Filter -->
                <filter id="filter" applyTo="mainTable" dataLoader="myEntitiesDl" defaultMode="generic" width="100%" collapsable="true" collapsed="true" stylename="candidate-generic-filter"/>

                <!-- Карточка таблицы с rowsCount -->
                <vbox id="tableCard" width="100%" height="100%" spacing="false" stylename="edit-card candidate-table-card" expand="mainTable">
                    <treeDataGrid id="mainTable" width="100%" height="100%" dataContainer="myEntitiesDc" stylename="borderless grid candidate-browse-grid">
                        <columns>...</columns>
                        <rowsCount/>
                    </treeDataGrid>
                </vbox>
            </vbox>
        </hbox>
    </layout>
</window>
```

---

## 4. Требования к Java-контроллеру

1. **Слушатель выбора строк (`SelectionListener`)**:
   - При выборе строки обновлять сайдбар и разблокировать кнопки действий (`enabled="true"`).
   - При сбросе выбора очищать поля сайдбара и блокировать кнопки.
2. **Data View Integrity**:
   - Все поля сущности, читаемые в методе `updateSidebarWithEntity(T entity)`, обязаны быть декларированы во view загрузчика.
3. **Безопасный NVL-вывод строк**:
   - Использовать `nvl(String val)` без сырых HTML-тегов `<div>`, исключая деградацию стилей в темах.
