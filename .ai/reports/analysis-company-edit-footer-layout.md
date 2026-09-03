# Анализ проблемы с footer (кнопками OK/Cancel) в CompanyEdit

## Проблема
В форме редактирования компании (`CompanyEdit`) кнопки «ОК» / «Отмена» (footer) уходят за нижнюю границу экрана и становятся недоступны пользователю.

## Затронутые файлы
1. `/modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml` (строки 213–852 — структура workspace)
2. `/modules/web/themes/hunttech-modern/com.company.hunttech/company-editor.scss` (строки 361–394 — стили `.edit-footer-actions`)
3. `/modules/web/themes/hunttech-modern/com.company.hunttech/edit-screen-shared-styles.scss` (общие стили `.edit-workspace`, `.edit-footer-actions`)

## Корневая причина
**Неправильное flex-распределение высоты в `companyEditorWorkspace` (vbox).**

### Текущая структура CompanyEdit (неправильная)
```xml
<vbox id="companyEditorWorkspace" expand="mainTab">
    <!-- toolbar -->
    <hbox id="companyEditorToolbar" .../>
    
    <!-- tabSheet РАСШИРЯЕТСЯ на всю доступную высоту -->
    <tabSheet id="mainTab" height="100%" ...>
        <tab>...<scrollBox height="100%"/>...</tab>
        ...
    </tabSheet>
    
    <!-- footer ПОСЛЕ tabSheet в том же vbox -->
    <hbox id="editActions" stylename="edit-footer-actions" ...>
        <button action="windowCommitAndClose"/>
        <button action="windowClose"/>
    </hbox>
</vbox>
```

- `companyEditorWorkspace` имеет `expand="mainTab"` → `tabSheet` получает **всё** свободное место (flex-grow: 1).
- `tabSheet` имеет `height="100%"` → он пытается занять 100% высоты workspace.
- `editActions` (footer) идёт **после** `tabSheet` в том же vbox → он выталкивается за экран, так как `tabSheet` уже съел всю высоту.

### Эталонная структура IteractionListEdit (правильная)
```xml
<vbox id="iteractionListWorkspace" expand="iteractionListContentScrollBox">
    <!-- toolbar -->
    <hbox id="iteractionListToolbarBox" .../>
    
    <!-- quickActions -->
    <vbox id="mostPopularQuickActions" .../>
    
    <!-- scrollBox РАСШИРЯЕТСЯ (содержит контент формы) -->
    <scrollBox id="iteractionListContentScrollBox" height="100%" ...>
        <vbox id="iteractionListSectionsBox" height="AUTO" ...>...</vbox>
    </scrollBox>
    
    <!-- footer ВНЕ scrollBox, НО В workspace vbox -->
    <hbox id="editActions" stylename="edit-footer-actions" height="AUTO" ...>...</hbox>
</vbox>
```

- `iteractionListWorkspace` имеет `expand="iteractionListContentScrollBox"` → **scrollBox** получает flex-grow: 1.
- `scrollBox` имеет `height="100%"` + внутренний контент `height="AUTO"` → прокручивает только контент.
- `editActions` (footer) идёт **после scrollBox** в workspace vbox → остаётся прижат к низу, фиксированная высота (`height="AUTO"`).

## Разбор различий

| Аспект | IteractionListEdit (эталон) | CompanyEdit (текущее) |
|--------|----------------------------|----------------------|
| `workspace` expand | `scrollBox` (контент) | `tabSheet` (вкладки) |
| Footer положение | После scrollBox, в workspace | После tabSheet, в workspace |
| Scroll-контейнер | Один общий scrollBox | scrollBox **внутри** каждого tab |
| TabSheet | Нет | Есть (mainTab) |
| Результат | Footer всегда виден | Footer уезжает за экран |

## Рекомендации по исправлению

### Вариант 1: Обёрнуть tabSheet в scrollBox (минимальные изменения, рекомендуется)
Сделать структуру workspace аналогичной эталону: общий scrollBox с expand, footer вне scrollBox.

```xml
<vbox id="companyEditorWorkspace" expand="companyEditorContentScrollBox">
    <hbox id="companyEditorToolbar" .../>
    
    <!-- НОВЫЙ: общий scrollBox для вкладок -->
    <scrollBox id="companyEditorContentScrollBox"
               width="100%"
               height="100%"
               orientation="vertical"
               scrollBars="vertical"
               stylename="edit-workspace edit-workspace-scroll">
        <tabSheet id="mainTab" width="100%" height="100%">
            <!-- табы с scrollBox height="100%" внутри оставляем как есть -->
        </tabSheet>
    </scrollBox>
    
    <!-- Footer ВНЕ scrollBox, НО В workspace -->
    <hbox id="editActions" stylename="edit-footer-actions" height="AUTO" ...>...</hbox>
</vbox>
```

**Почему это работает:**
- `workspace` отдаёт весь flex `companyEditorContentScrollBox`
- `scrollBox` растягивается, `tabSheet` внутри получает `height="100%"` от scrollBox
- `editActions` с `height="AUTO"` остаётся внизу workspace, не участвует в flex-распределении

### Вариант 2: Убрать expand у workspace, дать tabSheet flex: 0 0 auto, footer flex: 0 0 auto
Более хрупкий вариант, требует CSS-фиксов для tabSheet. Не рекомендуется.

### Что НЕЛЬЗЯ менять (по требованию задачи)
- ❌ Sidebar (`companyEditorSidebar`) — не трогать
- ❌ Элементы ввода внутри вкладок — не трогать
- ❌ Бизнес-логику в `CompanyEdit.java` — не трогать
- ❌ Сущности, контейнеры данных — не трогать
- ❌ Другие формы — не трогать

## План действий (для реализации)
1. В `company-edit.xml`:
   - Добавить `<scrollBox id="companyEditorContentScrollBox" ...>` вокруг `<tabSheet id="mainTab">`
   - У `companyEditorWorkspace` изменить `expand="mainTab"` → `expand="companyEditorContentScrollBox"`
   - У `editActions` добавить `height="AUTO"` (или убрать `height="100%"` если есть)
2. Проверить, что у `mainTab` остаётся `width="100%" height="100%"`
3. Проверить, что у внутренних scrollBox вкладок остаётся `height="100%"`
4. Пересобрать тему (gradle), открыть форму, проверить видимость кнопок

## Ожидаемый результат
- Footer (кнопки OK/Cancel) всегда виден внизу экрана
- Вкладки прокручиваются внутри своей области
- Sidebar не меняется
- Никаких изменений в Java-коде и бизнес-логике