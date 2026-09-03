# CompanyEdit Footer Layout Fix — Design Report

**Задача:** Исправить верстку `company-edit.xml` по эталону `iteraction-list-edit.xml`, чтобы footer (кнопки OK/Cancel) всегда был виден при любых размерах окна, а TabSheet/вкладки прокручивались внутри своей области.

**Проблема:** В текущей верстке footer (`edit-footer-actions`) находится **внутри** `companyEditorWorkspace` (vbox) **после** `tabSheet` с `expand="mainTab"`. TabSheet потребляет всё пространство workspace, footer уходит за границы экрана.

**Эталон (IteractionListEdit):**
- `hbox iteractionListMainLayout` → `expand="iteractionListWorkspace"`
- `vbox iteractionListWorkspace` → `expand="iteractionListContentScrollBox"`
- Toolbar + QuickActions — **вне** scrollBox (фиксированные)
- `scrollBox iteractionListContentScrollBox` с `expand` — занимает пространство МЕЖДУ toolbar и footer
- Footer (`edit-footer-actions`) — **на уровне hbox iteractionListMainLayout**, НЕ внутри workspace

---

## 1. XML Changes — company-edit.xml

### 1.1 Переместить footer на уровень `companyEditorMainLayout` (hbox)

**Текущее (строки 213-853):**
```xml
<hbox id="companyEditorMainLayout" ... expand="companyEditorWorkspace">
    <vbox id="companyEditorSidebar" ...>...</vbox>
    <vbox id="companyEditorWorkspace" ... expand="mainTab">
        <hbox id="companyEditorToolbar" ...>...</hbox>
        <tabSheet id="mainTab" ... expand="mainTab">...</tabSheet>
        <hbox id="editActions" stylename="edit-footer-actions" ...>...</hbox>  <!-- ПРОБЛЕМА: внутри workspace -->
    </vbox>
</hbox>
```

**Должно быть:**
```xml
<hbox id="companyEditorMainLayout" ... expand="companyEditorWorkspace">
    <vbox id="companyEditorSidebar" ...>...</vbox>
    <vbox id="companyEditorWorkspace" ... expand="companyEditorContentScrollBox">
        <hbox id="companyEditorToolbar" ...>...</hbox>
        <scrollBox id="companyEditorContentScrollBox"
                   width="100%"
                   height="100%"
                   orientation="vertical"
                   scrollBars="vertical"
                   stylename="edit-workspace edit-workspace-scroll">
            <tabSheet id="mainTab"
                      stylename="edit-tabs"
                      width="100%"
                      height="100%">
                <!-- вкладки остаются без изменений -->
            </tabSheet>
        </scrollBox>
    </vbox>
    <!-- Footer ВНЕ workspace, на уровне hbox companyEditorMainLayout -->
    <hbox id="editActions"
          stylename="edit-footer-actions"
          width="100%"
          expand="bottomActionsSpacer"
          align="MIDDLE_RIGHT"
          spacing="false">
        <vbox id="bottomActionsSpacer" width="100%" height="1px"/>
        <hbox id="bottomActionsGroup"
              width="AUTO"
              align="MIDDLE_RIGHT"
              spacing="true">
            <button action="windowCommitAndClose" stylename="company-editor-primary-action"/>
            <button action="windowClose" stylename="company-editor-secondary-action"/>
        </hbox>
    </hbox>
</hbox>
```

### 1.2 Ключевые изменения в атрибутах

| Элемент | Было | Стало |
|---------|------|-------|
| `companyEditorWorkspace` (vbox) | `expand="mainTab"` | `expand="companyEditorContentScrollBox"` |
| `mainTab` (tabSheet) | `expand="mainTab"` + внутри workspace | **Убрать expand**, обернуть в scrollBox |
| Новый `companyEditorContentScrollBox` (scrollBox) | — | `width="100%" height="100%" expand` (на vbox workspace) |
| `editActions` (footer) | Внутри `companyEditorWorkspace` | **После** `companyEditorWorkspace`, внутри `companyEditorMainLayout` |

### 1.3 Вкладки (tabSheet) — не менять содержимое

Каждая вкладка уже имеет свой `scrollBox` внутри (например `companyDetailsScroll`, `companyRequisitesScroll` и т.д.). Внешний `companyEditorContentScrollBox` будет прокручивать весь TabSheet как единое целое, а внутренние scrollBox'ы вкладок — прокручивать контент внутри активной вкладки.

---

## 2. SCSS Recommendations

### 2.1 Общие стили (`edit-screen-shared-styles.scss`) — уже покрывают

Следующие правила из `edit-screen-shared-styles.scss` уже работают для новой структуры:

```scss
// Workspace scrollBox — ограничивает v-scrollable шириной родителя
.edit-workspace-scroll > .v-scrollable,
.edit-workspace .v-scrollable {
  box-sizing: border-box !important;
  width: 100% !important;
  min-width: 0 !important;
  max-width: 100% !important;
  overflow-x: hidden !important;
}

// TabSheet panel — обрезает содержимое
.edit-workspace .v-tabsheet-tabsheetpanel {
  box-sizing: border-box !important;
  max-width: 100% !important;
  overflow: hidden !important;
}

// TabSheet content — flex: 1 1 auto, прокрутка внутри
.edit-tabs .v-tabsheet-content {
  flex: 1 1 auto !important;
  overflow: auto;
  min-height: 0 !important;
}
```

### 2.2 Локальные стили — `company-editor.scss` (7 тем)

**Добавить/обновить в миксине `company-editor-theme`:**

```scss
@mixin company-editor-theme {
  .company-editor {
    // ... существующие правила ...

    // Footer — зафиксирован внизу, не уходит за экран
    .edit-footer-actions {
      box-sizing: border-box;
      width: 100% !important;
      min-height: 62px;
      padding: 11px 20px;
      background: $v-panel-background-color;
      border-top: 1px solid rgba($v-font-color, 0.15);
      flex-shrink: 0 !important;  // Критично: не сжимается при нехватке места
    }

    .edit-footer-actions > .v-slot {
      width: 100% !important;
    }

    // Группа кнопок — прижата вправо
    .edit-footer-actions .v-horizontallayout {
      width: auto !important;
      white-space: nowrap;
    }

    .edit-footer-actions .v-horizontallayout > .v-slot {
      width: auto !important;
    }

    // Кнопки footer
    .edit-footer-actions .v-button {
      min-height: 40px;
      min-width: 100px;
      padding: 0 16px;
      font-size: 14px;
      font-weight: 600;
      border-radius: 4px;
      box-shadow: none !important;
    }

    .edit-footer-actions .v-button:focus {
      box-shadow: 0 0 0 2px rgba($v-selection-color, 0.20) !important;
      outline: 0;
    }

    // Primary (OK) — белый текст на primary
    .edit-footer-actions .company-editor-primary-action,
    .company-editor-primary-action {
      color: #ffffff !important;
      background: $v-selection-color !important;
      border-color: $v-selection-color !important;
    }

    // Secondary (Отмена) — прозрачная с рамкой
    .edit-footer-actions .company-editor-secondary-action,
    .company-editor-secondary-action {
      color: mix($v-font-color, $v-panel-background-color, 82%) !important;
      background: transparent !important;
      border: 1px solid rgba($v-font-color, 0.3) !important;
    }

    .edit-footer-actions .company-editor-secondary-action:hover,
    .company-editor-secondary-action:hover {
      background: rgba($v-font-color, 0.06) !important;
    }

    // Workspace с scrollBox — правильная flex-структура
    .company-editor-workspace {
      display: flex !important;
      flex-direction: column !important;
      height: 100% !important;
      min-height: 0 !important;
    }

    // Toolbar — фиксированный, не прокручивается
    .company-editor-toolbar {
      flex: 0 0 auto !important;
    }

    // ScrollBox с TabSheet — растягивается, прокручивается
    .company-editor-content-scroll {
      flex: 1 1 auto !important;
      min-height: 0 !important;
      overflow: hidden !important;  // прокрутка внутри .v-scrollable
    }

    // Адаптивность
    @media (max-width: 1366px) {
      .company-editor-workspace .edit-toolbar {
        padding: 8px 12px;
      }

      .company-editor-content-scroll {
        padding-right: 8px;
        padding-left: 8px;
      }

      .edit-footer-actions {
        padding: 8px 12px;
        min-height: 56px;
      }
    }
  }
}
```

### 2.3 Stylenames для XML — добавить в элементы

| XML Element | Добавить stylename |
|-------------|-------------------|
| `companyEditorWorkspace` (vbox) | `company-editor-workspace` |
| `companyEditorToolbar` (hbox) | `company-editor-toolbar` |
| `companyEditorContentScrollBox` (scrollBox) | `company-editor-content-scroll` |

---

## 3. Checklist для реализации

### XML (company-edit.xml)
- [ ] Перенести `editActions` (footer) из `companyEditorWorkspace` в `companyEditorMainLayout` (после `companyEditorWorkspace`)
- [ ] Добавить `scrollBox id="companyEditorContentScrollBox"` внутри `companyEditorWorkspace` с `width="100%" height="100%" orientation="vertical" scrollBars="vertical" stylename="edit-workspace edit-workspace-scroll"`
- [ ] Перенести `mainTab` (tabSheet) внутрь нового `companyEditorContentScrollBox`
- [ ] У `companyEditorWorkspace` изменить `expand="mainTab"` → `expand="companyEditorContentScrollBox"`
- [ ] У `mainTab` убрать `expand="mainTab"` (оставить `width="100%" height="100%"`)
- [ ] Добавить stylenames: `company-editor-workspace`, `company-editor-toolbar`, `company-editor-content-scroll`

### SCSS (company-editor.scss × 7 тем)
- [ ] Добавить блок `.edit-footer-actions` с `flex-shrink: 0`, min-height, padding
- [ ] Добавить стили для `.company-editor-primary-action` / `.company-editor-secondary-action`
- [ ] Добавить flex-структуру для `.company-editor-workspace` / `.company-editor-toolbar` / `.company-editor-content-scroll`
- [ ] Добавить `@media (max-width: 1366px)` адаптивность

### Вертикальная прокрутка
- [ ] Внешний `companyEditorContentScrollBox` прокручивает весь TabSheet при нехватке высоты
- [ ] Внутренние scrollBox'ы вкладок (`companyDetailsScroll`, `companyRequisitesScroll` и др.) продолжают прокручивать контент внутри активной вкладки
- [ ] Toolbar остаётся фиксированным сверху workspace
- [ ] Footer остаётся фиксированным снизу окна (на уровне hbox)

### Горизонтальная прокрутка
- [ ] Общие стили `edit-screen-shared-styles.scss` уже ограничивают `.v-scrollable`, `.v-tabsheet-tabsheetpanel`, `.v-slot` шириной 100%
- [ ] Проверить: нет ли горизонтального скролла на 1366px и 1920px

---

## 4. Результат после изменений

```
window company-editor (100%×100%)
└─ layout (expand=companyEditorMainLayout)
   └─ hbox companyEditorMainLayout (edit-screen-layout)
      ├─ vbox companyEditorSidebar (edit-sidebar, 270px) — НЕ МЕНЯТЬ
      ├─ vbox companyEditorWorkspace (edit-workspace, company-editor-workspace)
      │   ├─ hbox companyEditorToolbar (edit-toolbar, company-editor-toolbar) — фиксирован
      │   └─ scrollBox companyEditorContentScrollBox (company-editor-content-scroll, expand)
      │       └─ tabSheet mainTab (edit-tabs)
      │           ├─ tab tabConpanyDetails → scrollBox companyDetailsScroll → content
      │           ├─ tab companyRequisitesTab → scrollBox companyRequisitesScroll → content
      │           ├─ tab companyDescriptionTab → scrollBox companyDescriptionScroll → content
      │           └─ tab tabCompanyDepartament → scrollBox companyDepartmentsScroll → content
      └─ hbox editActions (edit-footer-actions) — НОВОЕ МЕСТО: на уровне hbox, ВНЕ workspace
          ├─ vbox bottomActionsSpacer (expand)
          └─ hbox bottomActionsGroup → OK (primary) / Отмена (secondary)
```

---

## 5. Риски и митигация

| Риск | Митигация |
|------|-----------|
| TabSheet внутри scrollBox может не растянуться на 100% высоты | У scrollBox `height="100%"`, у tabSheet `height="100%"`, у workspace `flex: 1 1 auto` + `min-height: 0` |
| Внутренние scrollBox вкладок конфликтуют с внешним | Внешний прокручивает TabSheet как единый блок, внутренние — контент вкладки. Вложенные скроллы работают в браузере (Vaadin WebComponents) |
| Footer перекрывает контент при очень маленьком окне | `flex-shrink: 0` на footer + `min-height: 0` на scrollBox гарантируют, что scrollBox сожмётся до 0, footer останется видимым |
| Горизонтальный оверфлоу на мобильных | Общие CDP-фиксы в `edit-screen-shared-styles.scss` уже ограничивают `.v-scrollable`, `.v-slot`, `.v-tabsheet-tabsheetpanel` |

---

## 6. Файлы для изменения

1. **XML:** `modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml`
2. **SCSS (7 тем):**
   - `modules/web/themes/hunttech-modern-dark/com.company.hunttech/company-editor.scss`
   - `modules/web/themes/hunttech-modern-light/com.company.hunttech/company-editor.scss`
   - `modules/web/themes/hunttech-modern/com.company.hunttech/company-editor.scss`
   - `modules/web/themes/hover/com.company.hunttech/company-editor.scss`
   - `modules/web/themes/helium/com.company.hunttech/company-editor.scss`
   - `modules/web/themes/havana/com.company.hunttech/company-editor.scss`
   - `modules/web/themes/halo/com.company.hunttech/company-editor.scss`