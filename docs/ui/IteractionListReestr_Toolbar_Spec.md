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
   - **Строго однострочное размещение**: `display: flex !important; flex-wrap: nowrap !important; align-items: center !important; justify-content: space-between !important; width: 100% !important; min-width: 0 !important; gap: 6px !important; overflow-x: auto !important; overflow-y: hidden !important;`
   - `.filter-buttons-panel`: `display: inline-flex !important; flex-wrap: nowrap !important; align-items: center !important; min-width: 0 !important; width: auto !important; gap: 5px !important;`
   - `.left-action-buttons`: `display: inline-flex !important; justify-content: flex-start !important; flex: 1 1 auto !important; min-width: 0 !important;`
   - `.right-action-buttons`: `display: inline-flex !important; justify-content: flex-end !important; flex: 0 0 auto !important; margin-left: auto !important; min-width: 0 !important;`
   - **Масштабируемость кнопок (Fluid Scalable Buttons)**:
     - Кнопки: `flex: 0 1 auto !important; min-width: 32px !important; max-width: 100% !important; white-space: nowrap !important;`
     - Подписи кнопок (`.v-button-caption`): `min-width: 0 !important; overflow: hidden !important; text-overflow: ellipsis !important; white-space: nowrap !important;`
     - Иконки и индикаторы popup: `flex-shrink: 0 !important;`
   - **Плавная градация размеров при уменьшении ширины экрана**:
     - `> 1440px`: высота 34px (min-height/max-height: 34px), line-height: 32px, паддинг 0 10px, шрифт 12.5px, gap 6px.
     - `<= 1440px`: паддинг 0 8px, шрифт 12px, gap 5px.
     - `<= 1240px`: высота 32px (min-height/max-height: 32px), line-height: 30px, паддинг 0 6px, шрифт 11.5px, gap 4px.
     - `<= 1024px`: высота 30px (min-height/max-height: 30px), line-height: 28px, паддинг 0 4px, шрифт 11px, min-width 28px, gap 3px.
     - `<= 900px`: высота 28px (min-height/max-height: 28px), line-height: 26px, паддинг 0 3px, шрифт 10.5px, min-width 28px, gap 2px.
2. Исключен перенос на вторую строку: `iteraction-reestr-filter-bar` исключен из общих правил переноса через `:not(.iteraction-reestr-filter-bar)`. Тулбар сохраняет монолитное однострочное представление без наложений и выпадений. При экстремально узких окнах доступен горизонтальный скролл (`overflow-x: auto`), гарантируя доступность всех действий.

---

## 3. Верификация и тесты

1. **Контрактный тест**: `IteractionListReestrLayoutContractTest` проверяет:
   - Использование `cssLayout` для `tableFilterBar`, `leftActionButtons`, `rightActionButtons`.
   - Отсутствие промежуточного `toolbarSpacer`.
   - Сохранность всех идентификаторов кнопок (`createBtn`, `editBtn`, `removeBtn`, `filterPopupButton`, `actionsPopupButton`) и их привязок.
   - Синхронизацию SCSS-правил во всех 7 темах: однострочность `flex-wrap: nowrap`, эллипсис `text-overflow: ellipsis`, выравнивание `justify-content: space-between` и `margin-left: auto`.
2. **Code Review**: `ocr review --audience agent` — 0 замечаний.
3. **Сборка SCSS-тем**: `buildScssThemes` завершилась успешно (`BUILD SUCCESSFUL`).
4. **Тесты**: Все тесты пакета `iteractionlist` и `ScreenViewIntegrityTest` успешно пройдены.
