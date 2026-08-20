---
name: hunttech-browse-screen-design
description: >-
  Стандарт и руководство по разработке и оформлению экранных форм Browse и реестров в HRM HuntTech
  (CUBA Platform / Vaadin): Split-View с полновысотным сайдбаром 312px, командный тулбар,
  карточка Generic Filter, DataGrid, компактный счетчик rowsCount и адаптивная поддержка 7 SCSS-тем.
---

# Руководство по оформлению форм Browse и Реестров HRM HuntTech

Данный навык содержит архитектурный стандарт, шаблоны XML-дескрипторов, Java-контроллеров и SCSS-стилей для создания и модернизации экранов Browse (реестров) в системе **HuntTech HRM**.

---

## 1. Концепция макета Split-View

Современные экраны Browse строятся по схеме двухпанельного Split-View макета:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│ Главное меню приложения (App Menubar 42px)                                                  │
├───────────────────┬─────────────────────────────────────────────────────────────────────────┤
│ ЛЕВЫЙ САЙДБАР     │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ (margin="true,true,true,false")                  │
│ (312px, 100% h)   │                                                                         │
│                   │ ┌─────────────────────────────────────────────────────────────────────┐ │
│ • Фото / Логотип  │ │ Главный тулбар (tableFilterBar): [Действия слева]  [Фильтры справа] │ │
│ • Заголовок/Статус│ ├─────────────────────────────────────────────────────────────────────┤ │
│ • Быстрые кнопки  │ │ Generic Filter карточка (collapsable="true" collapsed="true")       │ │
│ • Условия / Грейд │ ├─────────────────────────────────────────────────────────────────────┤ │
│ • Кураторы / Отдел│ │ Карточка таблицы (TreeDataGrid / DataGrid с rowsCount 24px)         │ │
│ • Индикаторы      │ │                                                                     │ │
│ • Теги навыков    │ └─────────────────────────────────────────────────────────────────────┘ │
└───────────────────┴─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Структура XML-дескриптора

### Корневой контейнер и сайдбар
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
        <split id="splitMainLayout" orientation="horizontal" pos="312px" min="260px" max="500px" width="100%" height="100%">
            <!-- ЛЕВЫЙ САЙДБАР (312px на 100% высоты) -->
            <vbox id="detailPane" width="100%" height="100%" spacing="true" margin="false" stylename="edit-sidebar job-candidate-sidebar">
                <scrollBox id="sidebarScrollBox" width="100%" height="100%" spacing="true" scrollBars="vertical">
                    <!-- Фото/Логотип, Заголовок, Быстрые кнопки, Карточки атрибутов -->
                </scrollBox>
            </vbox>

            <!-- ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ -->
            <vbox id="workspaceBox" width="100%" height="100%" spacing="true" stylename="edit-workspace candidate-reestr-workspace" expand="tableCard" margin="true,true,true,false">
                
                <!-- 1. Главный командный тулбар -->
                <hbox id="tableFilterBar" width="100%" spacing="true" align="MIDDLE_LEFT" stylename="candidate-filter-bar edit-card" expand="toolbarSpacer">
                    <hbox id="leftActionButtons" spacing="true" align="MIDDLE_LEFT" stylename="filter-buttons-panel">
                        <button id="createBtn" caption="Создать" icon="CREATE_ACTION" stylename="primary candidate-btn"/>
                        <button id="editBtn" caption="Редактировать" icon="EDIT_ACTION" stylename="secondary candidate-btn" enabled="false"/>
                        <button id="removeBtn" caption="Удалить" icon="REMOVE_ACTION" stylename="secondary candidate-btn" enabled="false"/>
                    </hbox>
                    <hbox id="toolbarSpacer" width="100%"/>
                    <hbox id="rightActionButtons" spacing="true" align="MIDDLE_RIGHT" stylename="filter-buttons-panel">
                        <popupButton id="filterPopupButton" caption="Все записи" icon="FILTER" stylename="secondary candidate-btn"/>
                        <popupButton id="actionsPopupButton" caption="Действия" icon="BARS" stylename="primary"/>
                    </hbox>
                </hbox>

                <!-- 2. Generic Filter -->
                <filter id="filter"
                        applyTo="mainTable"
                        dataLoader="myEntitiesDl"
                        defaultMode="generic"
                        width="100%"
                        collapsable="true"
                        collapsed="true"
                        stylename="candidate-generic-filter">
                    <properties include=".*" exclude="id,version,createTs,createdBy,updateTs,updatedBy,deleteTs,deletedBy"/>
                </filter>

                <!-- 3. Карточка таблицы -->
                <vbox id="tableCard" width="100%" height="100%" spacing="false" stylename="edit-card candidate-table-card" expand="mainTable">
                    <treeDataGrid id="mainTable"
                                  width="100%"
                                  height="100%"
                                  bodyRowHeight="60px"
                                  dataContainer="myEntitiesDc"
                                  stylename="borderless grid candidate-browse-grid">
                        <actions>
                            <action id="create" type="create"/>
                            <action id="edit" type="edit"/>
                            <action id="remove" type="remove"/>
                            <action id="refresh" type="refresh"/>
                        </actions>
                        <columns>...</columns>
                        <rowsCount/>
                    </treeDataGrid>
                </vbox>
            </vbox>
        </split>
    </layout>
</window>
```

---

## 3. Требования к Java-контроллеру

1. **Иерархия и наследование**:
   - При создании специализированного реестра (например, `OpenPositionReestrBrowse`) наследоваться от базового Browse-контроллера (`OpenPositionBrowse`).
   - Поля сайдбара и метод `updateSidebarWithEntity(T entity)` объявлять как `protected`, обеспечивая повторное использование.
2. **Слушатель выбора строк**:
   - Обновлять сайдбар и доступность кнопок тулбара в `addSelectionListener`.
   - Безопасно обрабатывать выбор `null` (очистка сайдбара или вывод плейсхолдера «Выберите запись»).
3. **Data View Integrity**:
   - Запрещено вызывать геттеры у незагруженных атрибутов сущностей (`DetachedObjectException` / `UnfetchedAttributeException`).
   - Все поля, отображаемые в сайдбаре, обязаны быть включены во view загрузчика.

---

## 4. Стилизация и поддержка 7 SCSS-тем

Все стили оформляются через слои тем CUBA:
1. **Общий слой**: `modules/web/themes/*/com.company.hunttech/edit-screen-shared-styles.scss`
2. **Специализированный слой**: `job-candidate-editor.scss`
3. **Ключевые CSS-классы**:
   - `.candidate-reestr-workspace`: нативные отступы `padding-top: 10px; padding-right: 14px; padding-bottom: 10px;`
   - `.candidate-generic-filter`: карточка `border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); padding: 6px 12px;`
   - `.c-simplepagination` / `.c-rows-count`: контейнер `28px`, кнопки-стрелки `24x24px`, иконки `11px` без вертикального перекрытия соседних элементов.

---

## 5. Процедура верификации

1. **Компиляция и тесты**:
   ```bash
   bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-web:buildScssThemes :app-web:compileJava :app-core:test --tests "com.company.hunttech.core.ApplicationStartupContractTest" :app-core:test --tests "com.company.hunttech.core.ScreenViewIntegrityTest"
   ```
2. **Правила Git и деплоя**:
   - Коммит с русским понятным сообщением.
   - Отправка в `agent/antigravity-dev` и `master`.
   - **Не делать промежуточных деплоев** (деплой `start-app.sh` выполняется только по явной команде пользователя).
