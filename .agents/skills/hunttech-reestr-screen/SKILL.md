---
name: hunttech-reestr-screen
description: >-
  Руководство и стандарт разработки экранных форм Реестров (Browse/Reestr) в HRM HuntTech (CUBA 7.3 / Vaadin):
  Split-View с полновысотным сайдбаром 312px, тулбар с быстрыми фильтрами и действиями, мастер "Умная загрузка"
  с 3 вкладками (файлы, richtextarea, ссылка из интернета), таблица с индикатором приоритета, зарплатной вилкой и
  чипами навыков, пакетная загрузка Zero N+1 и адаптация под 7 SCSS-тем.
---

# Стандарт и руководство по разработке форм Реестров (HuntTech Reestr Screen)

Данный навык фиксирует проверенный стандарт проектирования, XML-разметки, Java-логики и визуального оформления для создания и модернизации экранов **Реестров** в системе **HuntTech HRM** (например, *Реестр кандидатов*, *Реестр открытых вакансий*).

---

## 1. Архитектурная концепция Split-View

Экран реестра строится по структуре Split-View с полновысотным левым сайдбаром:

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Меню приложения / Заголовок реестра (42px)                                                       │
├───────────────────────┬──────────────────────────────────────────────────────────────────────────┤
│ ЛЕВЫЙ САЙДБАР (312px) │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ (margin="true,true,true,false")                   │
│                       │ ┌──────────────────────────────────────────────────────────────────────┐ │
│ • Фото/Логотип 120px  │ │ Командный тулбар (candidate-filter-bar):                             │ │
│ • Должность (h2)      │ │ [Создать] [Умная загрузка] [Ред.] [Удал.]  ...  [Фильтр ▼] [Действия]│ │
│ • Проект (h4)         │ ├──────────────────────────────────────────────────────────────────────┤ │
│ • Быстрые действия    │ │ Generic Filter (collapsable="true" collapsed="true")                 │ │
│ • Условия / Реквизиты │ ├──────────────────────────────────────────────────────────────────────┤ │
│   - Вилка ТК / ИП     │ │ Карточка таблицы (candidate-table-card):                             │ │
│   - Опыт / Грейд      │ │ ┌───┬──────┬──────────────┬──────────────┬──────────────┬──────────┐ │ │
│ • Готовность & Рейтинг│ │ │ ! │ Лого │ Наименование │ Специализ-я  │ Зарплата     │ Навыки   │ │ │
│   - Чеклист           │ │ ├───┼──────┼──────────────┼──────────────┼──────────────┼──────────┤ │ │
│   - Звезды ★★★★☆      │ │ │ 🟢│  💼  │ Lead ML Eng. │ Разработка   │ до 350 000 ₽ │ ★ Python │ │ │
│ • Навыки (3 уровня)   │ │ └───┴──────┴──────────────┴──────────────┴──────────────┴──────────┘ │ │
│   - Обязательные (★)  │ │ RowsCount (компактный счетчик 24px)                                  │ │
│   - Желательные       │ └──────────────────────────────────────────────────────────────────────┘ │
│   - Прочие            │                                                                          │
└───────────────────────┴──────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Структура XML-дескриптора Реестра

### Дескриптор реестра (`open-position-reestr-browse.xml` / `job-candidate-reestr.xml`)

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<window xmlns="http://schemas.haulmont.com/cuba/screen/window.xsd"
        caption="msg://browseCaption"
        focusComponent="entitiesTable"
        messagesPack="com.company.hunttech.web.screens.myentity">
    <data readOnly="true">
        <collection id="entitiesDc" class="com.company.hunttech.entity.MyEntity">
            <view extends="myEntity-browse-view">
                <!-- Data View Integrity: декларировать все вложенные поля, используемые в UI -->
                <property name="grade" view="_minimal"/>
                <property name="positionType" view="_minimal"/>
                <property name="projectName" view="_minimal">
                    <property name="projectLogo" view="_minimal"/>
                    <property name="projectDepartment" view="_minimal">
                        <property name="companyName" view="_minimal"/>
                    </property>
                    <property name="projectOwner" view="_minimal"/>
                </property>
            </view>
            <loader id="entitiesDl">
                <query><![CDATA[select e from hunttech_MyEntity e order by e.createTs desc]]></query>
            </loader>
        </collection>
    </data>

    <layout expand="splitMainLayout" spacing="false" margin="false" stylename="job-candidate-editor edit-screen-layout">
        <split id="splitMainLayout" orientation="horizontal" pos="312px" min="260px" max="500px" width="100%" height="100%">
            <!-- ЛЕВЫЙ САЙДБАР (312px) -->
            <vbox id="detailPane" width="100%" height="100%" spacing="true" margin="false" stylename="edit-sidebar job-candidate-sidebar">
                <scrollBox id="sidebarScrollBox" width="100%" height="100%" spacing="true" scrollBars="vertical">
                    <vbox width="100%" spacing="true">
                        <!-- Шапка профиля -->
                        <vbox id="profileHeader" width="100%" spacing="true" align="TOP_CENTER" stylename="job-candidate-profile-header edit-sidebar-visual">
                            <ovaFallbackImage id="logoPic" width="120px" height="120px" ovalWidth="120px" ovalHeight="120px"
                                              align="TOP_CENTER" stylename="job-candidate-avatar" fallbackThemePath="icons/briefcase.png" scaleMode="SCALE_DOWN"/>
                            <vbox width="100%" spacing="false" align="MIDDLE_CENTER" stylename="edit-sidebar-identity">
                                <label id="detailTitle" value="Выберите запись" width="100%" stylename="edit-sidebar-title h2 candidate-sidebar-fullname" align="MIDDLE_CENTER"/>
                                <label id="detailSubtitle" value="-" width="100%" stylename="edit-sidebar-subtitle h4 candidate-sidebar-position" align="MIDDLE_CENTER"/>
                                <label id="detailCompany" value="-" width="100%" stylename="edit-help candidate-sidebar-city" align="MIDDLE_CENTER"/>
                                <label id="detailLocation" value="-" width="100%" stylename="edit-help candidate-sidebar-city" align="MIDDLE_CENTER"/>
                            </vbox>
                        </vbox>

                        <!-- Кнопки действий сайдбара -->
                        <vbox id="sidebarActionsCard" width="100%" spacing="true" stylename="edit-sidebar-summary">
                            <button id="openEditCardBtn" caption="Открыть карточку" icon="EDIT_ACTION" stylename="primary" enabled="false" width="100%"/>
                            <button id="suggestBtn" caption="Подобрать подходящие" icon="font-icon:MAGIC" enabled="false" width="100%"/>
                        </vbox>

                        <!-- Секция: Условия и реквизиты -->
                        <vbox id="termsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                            <label value="УСЛОВИЯ И РЕКВИЗИТЫ" width="100%" stylename="label-nav-title job-candidate-section-title"/>
                            <grid id="termsGrid" spacing="true" width="100%" stylename="edit-sidebar-summary">
                                <columns count="2"/>
                                <rows>
                                    <row><label value="Вилка по ТК:" stylename="bold"/><label id="detailSalaryTk" value="-" stylename="bold"/></row>
                                    <row><label value="Ставка по ИП:" stylename="bold"/><label id="detailSalaryIe" value="-"/></row>
                                    <row><label value="Опыт работы:" stylename="bold"/><label id="detailExperience" value="-"/></row>
                                    <row><label value="Грейд:" stylename="bold"/><label id="detailGrade" value="-"/></row>
                                    <row><label value="Формат работы:" stylename="bold"/><label id="detailRemoteWork" value="-"/></row>
                                    <row><label value="Статус:" stylename="bold"/><label id="detailStatus" value="-"/></row>
                                </rows>
                            </grid>
                        </vbox>

                        <!-- Секция: Готовность и рейтинг -->
                        <vbox id="readinessCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                            <label value="ГОТОВНОСТЬ И РЕЙТИНГ" width="100%" stylename="label-nav-title job-candidate-section-title"/>
                            <label id="detailIndicators" htmlEnabled="true" width="100%" stylename="edit-help"/>
                            <label id="detailRating" htmlEnabled="true" width="100%" stylename="edit-help"/>
                        </vbox>

                        <!-- Секция: Требуемые навыки (3 категории) -->
                        <vbox id="skillsCard" width="100%" spacing="true" stylename="job-candidate-navigation label-navigation">
                            <label value="КЛЮЧЕВЫЕ НАВЫКИ" width="100%" stylename="label-nav-title job-candidate-section-title"/>
                            <label id="detailSkills" htmlEnabled="true" width="100%"/>
                        </vbox>
                    </vbox>
                </scrollBox>
            </vbox>

            <!-- ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ -->
            <vbox id="workspaceBox" width="100%" height="100%" spacing="true" stylename="edit-workspace candidate-reestr-workspace" expand="tableCard" margin="true,true,true,false">
                
                <!-- Командный тулбар (адаптивный Flexbox на cssLayout) -->
                <cssLayout id="tableFilterBar" width="100%" stylename="candidate-filter-bar edit-card">
                    <cssLayout id="leftActionButtons" stylename="filter-buttons-panel left-action-buttons">
                        <button id="createBtn" caption="Создать" icon="CREATE_ACTION" stylename="primary candidate-btn candidate-create-btn"/>
                        <button id="smartUploadBtn" caption="Умная загрузка" icon="font-icon:MAGIC" stylename="primary candidate-btn candidate-smartload-btn"/>
                        <button id="editBtn" caption="Редактировать" icon="EDIT_ACTION" stylename="secondary candidate-btn" enabled="false"/>
                        <button id="removeBtn" caption="Удалить" icon="REMOVE_ACTION" stylename="secondary candidate-btn" enabled="false"/>
                    </cssLayout>
                    <cssLayout id="rightActionButtons" stylename="filter-buttons-panel right-action-buttons">
                        <popupButton id="filterPopupButton" caption="Все записи" icon="FILTER" stylename="secondary candidate-btn"/>
                        <popupButton id="actionsPopupButton" caption="Действия" icon="BARS" stylename="primary candidate-btn"/>
                    </cssLayout>
                </cssLayout>

                <!-- Generic Filter -->
                <filter id="filter" applyTo="entitiesTable" dataLoader="entitiesDl" defaultMode="generic" width="100%" collapsable="true" collapsed="true" stylename="candidate-generic-filter">
                    <properties include=".*" exclude="id,version,createTs,createdBy,updateTs,updatedBy,deleteTs,deletedBy"/>
                </filter>

                <!-- Таблица с rowsCount -->
                <vbox id="tableCard" width="100%" height="100%" spacing="false" stylename="edit-card candidate-table-card" expand="entitiesTable">
                    <groupTable id="entitiesTable" width="100%" height="100%" dataContainer="entitiesDc" stylename="borderless grid candidate-browse-grid">
                        <columns>
                            <column id="priority" caption="Приоритет" width="50px" sortable="true"/>
                            <column id="logo" caption="Лого" width="50px" sortable="false"/>
                            <column id="name" caption="Наименование" expandRatio="1"/>
                            <column id="positionType" caption="Специализация" width="150px"/>
                            <column id="salary" caption="Зарплата" width="150px"/>
                            <column id="mainSkills" caption="Навыки" width="220px" sortable="false"/>
                            <column id="statusBadge" caption="Статус" width="110px" sortable="false"/>
                        </columns>
                        <rowsCount/>
                    </groupTable>
                </vbox>
            </vbox>
        </split>
    </layout>
</window>
```

---

## 3. Мастер «Умная загрузка» (3 вкладки: Файлы, Текст, Ссылка)

В обоих мастерах умной загрузки (`SmartCvUploadScreen` и `SmartOpenPositionUploadScreen`) на 1-м шаге используется `tabSheet` с тремя способами подачи исходных данных:

### XML-разметка вкладок
```xml
<tabSheet id="inputTabSheet" width="100%">
    <!-- Вкладка 1: Загрузка файла (drag-and-drop) -->
    <tab id="fileTab" caption="Загрузка файла" icon="CLOUD_UPLOAD">
        <vbox spacing="true" width="100%" margin="true,false,false,false">
            <upload id="uploadField" uploadButtonCaption="Загрузить файл..." uploadButtonIcon="font-icon:CLOUD_UPLOAD"
                    permittedExtensions=".pdf,.docx,.doc,.rtf,.pages,.txt" dropZone="uploadCard" stylename="primary" showFileName="true"/>
        </vbox>
    </tab>
    
    <!-- Вкладка 2: Вставка текста в richTextArea -->
    <tab id="textTab" caption="Вставка текста" icon="FILE_TEXT_O">
        <vbox spacing="true" width="100%" margin="true,false,false,false">
            <richTextArea id="rawRichTextArea" width="100%" height="200px"/>
            <hbox spacing="true" align="MIDDLE_LEFT">
                <button id="analyzeTextBtn" caption="Распознать введенный текст" icon="font-icon:MAGIC" stylename="primary candidate-btn"/>
                <button id="clearTextBtn" caption="Очистить" icon="ERASER" stylename="secondary candidate-btn"/>
            </hbox>
        </vbox>
    </tab>

    <!-- Вкладка 3: Загрузка по ссылке из интернета -->
    <tab id="urlTab" caption="Загрузка по ссылке" icon="GLOBE">
        <vbox spacing="true" width="100%" margin="true,false,false,false">
            <label value="Укажите интернет-ссылку (hh.ru, Хабр Карьера, SuperJob, LinkedIn, сайт):" stylename="edit-help"/>
            <hbox spacing="true" width="100%" expand="urlField">
                <textField id="urlField" width="100%" inputPrompt="https://..."/>
                <button id="loadFromUrlBtn" caption="Загрузить по ссылке" icon="font-icon:DOWNLOAD" stylename="primary candidate-btn"/>
                <button id="clearUrlBtn" caption="Очистить" icon="ERASER" stylename="secondary candidate-btn"/>
            </hbox>
        </vbox>
    </tab>
</tabSheet>
```

### Загрузка по ссылке через Jsoup (Java)
```java
private String fetchTextFromUrl(String urlString) throws Exception {
    if (urlString == null || urlString.trim().isEmpty()) return null;
    String cleanUrl = urlString.trim();
    if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
        cleanUrl = "https://" + cleanUrl;
    }

    org.jsoup.nodes.Document doc = org.jsoup.Jsoup.connect(cleanUrl)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .referrer("https://www.google.com")
            .timeout(20000)
            .followRedirects(true)
            .get();

    doc.select("script, style, noscript, svg, nav, footer, header, .cookie-banner, .advertisement").remove();

    String title = doc.title();
    String mainContent = "";
    org.jsoup.nodes.Element contentEl = doc.selectFirst("[data-qa='vacancy-description'], [data-qa='resume-block-container'], main, article, .vacancy-section, .content, #content, body");
    if (contentEl != null) {
        mainContent = contentEl.text();
    } else if (doc.body() != null) {
        mainContent = doc.body().text();
    } else {
        mainContent = doc.text();
    }

    StringBuilder result = new StringBuilder();
    if (title != null && !title.isEmpty()) {
        result.append(title).append("\n\n");
    }
    result.append(mainContent);
    return result.toString();
}
```

---

## 4. Zero N+1 и Data View Integrity

1. **Пакетная загрузка связанных коллекций**:
   В `onEntitiesDlPostLoad(CollectionLoader.PostLoadEvent event)` собираем `positions` / `candidates` и одним JPQL-запросом `where e.entity in :entities` загружаем:
   - Навыки (`skillsByPositionId`)
   - Средний рейтинг комментариев (`avgRatingByPositionId`)
2. **Безопасность геттеров**:
   Все свойства, читаемые в генераторах колонок или сайдбаре (`grade`, `projectName.projectDepartment.companyName`), декларируются во `<view>`.
3. **Генераторы колонок**:
   - `priority` — цветная иконка `Image` (18x18px) без текста с `setDescription(...)`.
   - `salary` — максимум по ТК (`до 250 000 ₽`), либо `По запросу кандидата`, либо `Не определено`.
   - `mainSkills` — цветные чипы (до 3 шт. + бейдж `+N`).
   - `statusBadge` — бейдж `Открыта` (зеленый) / `Закрыта` (красный).

---

## 5. SCSS Стили и Темы

Стили подключаются через `edit-screen-shared-styles.scss` и адаптированы под все 7 тем (Modern, Modern-Dark, Modern-Light, Hover, Havana, Halo, Helium):
- Сайдбар: `.candidate-sidebar-fullname`, `.candidate-sidebar-position` (`text-align: center`).
- Командный тулбар: `.candidate-filter-bar.edit-card` (фоновый радиус 8px, акцентные кнопки `candidate-btn`).
- Таблица: `.candidate-table-card`, `.candidate-browse-grid`.
- Счетчик строк: компактный `rowsCount` (24px, серый текст 11.5px).
