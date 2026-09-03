# Дизайн адаптивной верстки тулбара реестра проектов

**Дата:** 2026-09-03  
**Экран:** project-reestr-browse.xml  
**Эталон:** open-position-reestr-browse.xml (реестр открытых вакансий)

---

## Контракт адаптивного тулбара (по эталону OpenPositionReestrBrowse)

### Структура XML (cssLayout-based)

```xml
<cssLayout id="tableFilterBar" width="100%" stylename="candidate-filter-bar edit-card">
    <!-- Левая группа: основные действия -->
    <cssLayout id="leftActionButtons" stylename="filter-buttons-panel left-action-buttons">
        <button id="createBtn" caption="Создать проект" icon="CREATE_ACTION" stylename="primary candidate-btn candidate-create-btn" action="projectsTable.create"/>
        <button id="editBtn" caption="Редактировать" icon="EDIT_ACTION" stylename="secondary candidate-btn candidate-edit-btn" action="projectsTable.edit" enabled="false"/>
        <button id="removeBtn" caption="Удалить" icon="REMOVE_ACTION" stylename="secondary candidate-btn candidate-remove-btn" action="projectsTable.remove" enabled="false"/>
    </cssLayout>

    <!-- Правая группа: фильтры и действия -->
    <cssLayout id="rightActionButtons" stylename="filter-buttons-panel right-action-buttons">
        <popupButton id="filterPopupButton" caption="С открытыми вакансиями" icon="FILTER" showActionIcons="true" stylename="secondary candidate-btn candidate-filter-scope-btn">
            <actions>
                <action id="filterWithOpenPositions" caption="С открытыми вакансиями" icon="FILTER"/>
                <action id="filterOnlyOpen" caption="Только открытые проекты" icon="ARCHIVE"/>
                <action id="filterAll" caption="Все проекты" icon="COMPASS"/>
            </actions>
        </popupButton>
        <popupButton id="actionsPopupButton" caption="Действия" icon="BARS" showActionIcons="true" stylename="primary candidate-btn">
            <actions>
                <action id="refreshAction" caption="Обновить данные" icon="REFRESH"/>
                <action id="excelExportAction" caption="Выгрузить в Excel" icon="FILE_EXCEL_O"/>
                <action id="generateShortDescriptionAction" caption="ИТ-генерация краткого описания" icon="font-icon:MAGIC"/>
            </actions>
        </popupButton>
    </cssLayout>
</cssLayout>
```

### Ключевые отличия от текущей версии

| Элемент | Текущее (hbox) | Новое (cssLayout + SCSS) |
|---------|----------------|---------------------------|
| Корневой контейнер | `hbox tableFilterBar` | `cssLayout tableFilterBar` + `candidate-filter-bar edit-card` |
| Левая группа | `hbox leftActionButtons` | `cssLayout leftActionButtons` + `filter-buttons-panel left-action-buttons` |
| Спейсер | `hbox toolbarSpacer width="100%"` | **УДАЛЕН** (flexbox сам распределяет) |
| Правая группа | `hbox rightActionButtons` | `cssLayout rightActionButtons` + `filter-buttons-panel right-action-buttons` |
| Кнопки | Без `candidate-btn` | Все кнопки с `candidate-btn` |
| Create кнопка | `stylename="primary"` | `stylename="primary candidate-btn candidate-create-btn"` |
| Edit/Remove | `stylename="secondary"` | `stylename="secondary candidate-btn candidate-edit-btn/remove-btn"` |
| PopupButton фильтр | Без спец. классов | `candidate-filter-scope-btn` |
| PopupButton действия | Без спец. классов | `candidate-btn` (primary) |

### Адаптивность (из job-candidate-editor.scss)

**Брейкпоинты:**
1. **1440px** — уменьшенные отступы и padding кнопок
2. **1240px** — правая группа под левую (flex-wrap), разделитель `border-top: dashed`
3. **900px** — компактные кнопки, уменьшенный шрифт

**SCSS поведение (уже готово):**
- `.candidate-filter-bar` — `display: flex`, `flex-wrap: wrap`, `justify-content: space-between`
- `.filter-buttons-panel` — `display: flex`, `flex-wrap: wrap`, `gap: 6px 8px`
- `.left-action-buttons` — `justify-content: flex-start`, `flex: 1 1 auto`
- `.right-action-buttons` — `justify-content: flex-end`, `flex: 0 1 auto`, `margin-left: auto`
- `@media (max-width: 1240px)` — правая группа `width: 100%`, `margin-left: 0`, `margin-top: 2px`, `border-top`

---

## Рекомендации по дизайну

1. **Иконки** — оставить текущие (CREATE_ACTION, EDIT_ACTION, REMOVE_ACTION, FILTER, BARS, REFRESH, FILE_EXCEL_O, MAGIC)
2. **Подписи кнопок** — оставить текущие ("Создать проект", "Редактировать", "Удалить", "С открытыми вакансиями", "Действия")
3. **Вложенные действия popupButton** — оставить текущие, добавить иконки через `showActionIcons="true"`
4. **Состояния disabled** — SCSS уже обрабатывает через `&.v-disabled` (opacity: 0.5)
5. **Hover/focus** — SCSS уже имеет переходы и цветовые изменения

---

## Ограничения (НЕ менять)

- ❌ Sidebar (левая панель 312px)
- ❌ Таблица проектов (projectsTable)
- ❌ Generic Filter
- ❌ Бизнес-логика в ProjectReestrBrowse.java
- ❌ Сущности Project, OpenPosition
- ❌ Другие экраны приложения