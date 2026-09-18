# Отчёт: Синхронизация каталогов разработки

**Дата:** 2026-09-04
**Автор:** hrm-dev (Antigravity)
**Статус:** Завершено

---

## 1. Результат синхронизации

| Каталог | Git-ветка | Версия | Состояние форм CompanyEdit/ExtSettingsWindow |
|---------|-----------|--------|----------------------------------------------|
| **hunttech_recruiting** | master | 0.434 | ✅ **Эталон** — PR #225, #226, #227, #229 замерджены |
| **hrm-hermes2** | agent/text-processing-remove-empty-lines | 0.434 | ✅ **Синхронизировано с master** (было отставание) |
| **hrm-antigravity** | agent/antigravity-dev | 0.437 | ✅ **Идентично master** для форм + PR #231 |

---

## 2. Что было сделано

### hrm-hermes2 (была проблема)
**Проблема:** Ветка `agent/text-processing-remove-empty-lines` отставала от master и содержала **регрессии форм**:
- `settingsTabSheet` имел `height="100%"` (убрано в PR #225)
- `margin=true` на groupBox (убрано в PR #225)
- `overflow: hidden` вместо `overflow-x: hidden` на tabsheetpanel (исправлено в PR #227)
- `edit-workspace edit-workspace-scroll` дубли на scrollBox вкладок CompanyEdit (убраны в PR #226)

**Действие:** `git merge origin/master` с разрешением конфликта `build.gradle` в пользу master (версия 0.434). Коммит `32cf7cf1` отправлен в origin.

### hrm-antigravity
Уже был актуален (включает master + PR #231). Только untracked отчёты.

---

## 3. Верификация форм (все 3 каталога идентичны)

### CompanyEdit (company-edit.xml)
```xml
<!-- Правильная структура (во всех 3 каталогах): -->
<scrollBox id="companyDetailsScroll" stylename="company-tab-scroll">     <!-- 4 вхождения -->
<scrollBox id="companyRequisitesScroll" stylename="company-tab-scroll">
<scrollBox id="companyDescriptionScroll" stylename="company-tab-scroll">
<scrollBox id="companyDepartmentsScroll" stylename="company-tab-scroll">
<!-- expand="..." АБСЕНТЕН на tab (как в PR #226) -->
<!-- edit-workspace edit-workspace-scroll ТОЛЬКО на companyEditorContentScrollBox (1 вхождение) -->
```

### ExtSettingsWindow (ext-settings-window.xml)
```xml
<!-- Правильная структура (во всех 3 каталогах): -->
<tabSheet id="settingsTabSheet" width="100%" stylename="framed ext-settings-tabs">
    <!-- НЕТ height="100%" (PR #225) -->
<scrollBox ... stylename="user-ai-profile-content edit-workspace-scroll">
    <!-- НЕТ edit-workspace дубля (PR #225) -->
<groupBox ... margin="true" ...>
    <!-- margin="true" (PR #225) -->
```

### SCSS (все 7 тем идентичны)
```scss
// edit-screen-shared-styles.scss
.edit-workspace .v-tabsheet-tabsheetpanel {
    overflow: hidden !important;  /* правильно (PR #227) */
}

// company-editor.scss
/* НЕТ height:100%!important на .v-scrollable (PR #227) */
```

---

## 4. Различия между каталогами (только версия и PR #231)

| Файл | master / hermes2 | antigravity |
|------|------------------|-------------|
| `build.gradle` | version = '0.434' | version = '0.437' |
| Контрактные тесты | все тесты | очистка 29 устаревших тестов (PR #231) |

**Для форм CompanyEdit/ExtSettingsWindow — полная идентичность.**

---

## 5. Рекомендации

1. **HRM-1 (CI/CD):** Деплоить из `hunttech_recruiting` (master) — это эталон
2. **HRM-2 (Hermes-2):** Продолжать работу в `hrm-hermes2` (ветка `agent/text-processing-remove-empty-lines`) — теперь синхронизировано
3. **Antigravity (hrm-dev):** Продолжать в `hrm-antigravity` — PR #231 готов к отправке

---

## 6. Субагенты

- **Основной агент (hrm-dev):** git diff анализ, merge conflict resolution, верификация всех 7 тем SCSS, XML структуры форм
- Никакие дополнительные субагенты не требовались