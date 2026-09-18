# Спецификация адаптивного командного тулбара реестра взаимодействий (IteractionListReestrBrowse)

**Экран**: `hunttech_IteractionListReestr.browse`  
**Контроллер**: `com.company.hunttech.web.screens.iteractionlist.IteractionListReestrBrowse`  
**Дескриптор**: `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-reestr-browse.xml`  
**Дата актуализации**: 2026-09-18  
**Статус**: Реализовано и верифицировано контрактными тестами (`IteractionListReestrLayoutContractTest`)

---

## 1. Контекст и проблема

В экранной форме «Реестр взаимодействий» (`IteractionListReestrBrowse`) при различных разрешениях экранов и изменении ширины окна браузера кнопки правой группы тулбара (`filterPopupButton` — «Все взаимодействия» и `actionsPopupButton` — «Действия») вели себя нестабильно:
- Совершали визуальные рывки («прыгали»);
- Неожиданно переносились на новую строку при экранах шире 1240px или прижимались к левому краю;
- Входили в циклический layout thrashing между движком Vaadin Layout и CSS Flexbox.

### Коренные причины
1. **Vaadin HBox vs CSS Flexbox**: `tableFilterBar` был объявлен как `<hbox expand="toolbarSpacer">` со спейсером `<hbox id="toolbarSpacer" width="100%"/>`. При этом класс `.candidate-filter-bar` в SCSS задавал `display: flex !important`. Внутренние контейнеры Vaadin `v-slot` получали расчётные inline-ширины от JavaScript-движка Vaadin, что приводило к конфликту с flexbox.
2. **Отсутствие эталонных классов выравнивания**: Группы кнопок не имели классов `left-action-buttons` и `right-action-buttons`, из-за чего правая группа не имела правила `margin-left: auto !important`.
3. **Ложный медиа-запрос 1240px**: Глобальное правило `@media (max-width: 1240px)` принудительно переносило правую группу на вторую строку с `width: 100%; margin-left: 0; justify-content: flex-start`, хотя для 5 компактных кнопок реестра взаимодействий суммарной шириной ~705px места достаточно на экранах вплоть до 960px.

---

## 2. Архитектурное решение

### 2.1. XML-дескриптор (`iteraction-list-reestr-browse.xml`)
Тулбар переведён на `cssLayout` по эталону `OpenPositionReestrBrowse` и `ProjectReestrBrowse`:
```xml
<!-- Командный тулбар (адаптивный cssLayout без рывков и скачков) -->
<cssLayout id="tableFilterBar" width="100%" stylename="candidate-filter-bar edit-card iteraction-reestr-filter-bar">
    <cssLayout id="leftActionButtons" stylename="filter-buttons-panel left-action-buttons">
        <button id="createBtn" caption="Создать взаимодействие" icon="CREATE_ACTION" stylename="primary candidate-btn candidate-create-btn" action="iteractionListsTable.create"/>
        <button id="editBtn" caption="Редактировать" icon="EDIT_ACTION" stylename="secondary candidate-btn candidate-edit-btn" action="iteractionListsTable.edit"/>
        <button id="removeBtn" caption="Удалить" icon="REMOVE_ACTION" stylename="secondary candidate-btn candidate-remove-btn" action="iteractionListsTable.remove"/>
    </cssLayout>
    <cssLayout id="rightActionButtons" stylename="filter-buttons-panel right-action-buttons">
        <popupButton id="filterPopupButton" caption="Все взаимодействия" icon="FILTER" showActionIcons="true" stylename="secondary candidate-btn candidate-filter-scope-btn">
            <actions>
                <action id="filterAll" caption="Все взаимодействия"/>
                <action id="filterMyOnly" caption="Только мои взаимодействия"/>
                <action id="filterOutstaffingOnly" caption="Только аутстаффинг"/>
                <action id="filterLast30Days" caption="За 30 дней"/>
                <action id="filterLast90Days" caption="За 90 дней"/>
            </actions>
        </popupButton>
        <popupButton id="actionsPopupButton" caption="Действия" icon="BARS" showActionIcons="true" stylename="primary candidate-btn">
            <actions>
                <action id="refreshAction" caption="Обновить данные" icon="REFRESH"/>
                <action id="excelExportAction" caption="Выгрузить в Excel" icon="FILE_EXCEL_O"/>
            </actions>
        </popupButton>
    </cssLayout>
</cssLayout>
```

### 2.2. SCSS во всех 7 темах оформления
В файлах `job-candidate-editor.scss` для тем `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`:
1. Добавлен селектор `.candidate-filter-bar.iteraction-reestr-filter-bar`:
   - `.filter-buttons-panel`: `display: inline-flex !important; flex-wrap: wrap !important; align-items: center !important; gap: 6px 8px !important; min-width: 0 !important; width: auto !important;`
   - `.left-action-buttons`: `display: inline-flex !important; justify-content: flex-start !important; flex: 0 1 auto !important; width: auto !important;`
   - `.right-action-buttons`: `display: inline-flex !important; justify-content: flex-end !important; flex: 0 0 auto !important; margin-left: auto !important; margin-top: 0 !important; padding-top: 0 !important; border-top: none !important; width: auto !important;`
   - Кнопки тулбара: `flex-shrink: 0 !important; white-space: nowrap !important;`
   - Адаптивный перенос при `<= 960px`: только на узких экранах правая панель плавно переносится на новую строку.
2. Медиа-запрос 1240px изолирован: `.candidate-filter-bar:not(.iteraction-reestr-filter-bar)` исключает тулбар взаимодействий от ложного разрыва строки.

---

## 3. Верификация и тесты

1. **Контрактный тест**: `IteractionListReestrLayoutContractTest` проверяет:
   - Использование `cssLayout` для `tableFilterBar`, `leftActionButtons`, `rightActionButtons`.
   - Отсутствие промежуточного `toolbarSpacer`.
   - Сохранность всех идентификаторов кнопок (`createBtn`, `editBtn`, `removeBtn`, `filterPopupButton`, `actionsPopupButton`) и их привязок.
   - Синхронизацию SCSS-правил во всех 7 темах.
2. **Code Review**: `ocr review --audience agent` — 0 замечаний по всем 8 файлам.
3. **Сборка SCSS-тем**: `buildScssThemes` завершилась успешно (`BUILD SUCCESSFUL`).
4. **Тесты**: Все тесты пакета `iteractionlist` и `ScreenViewIntegrityTest` успешно пройдены.
