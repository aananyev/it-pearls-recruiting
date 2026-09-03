# Task for ChatGPT — 2026-07-25

## 🐛 UNFETCHED ATTRIBUTE: Project.projectDepartment

**Где:** IteractionListEdit, при открытии через кнопку "Копировать"
**Статус:** ✅ Closed 2026-09-03 (фактически исправлено 2026-07-25)

### Стек
```
Cannot get unfetched attribute [projectDepartment] from detached object Project
  at IteractionListEdit.vacancyFieldValueChange(IteractionListEdit.java:316)
  at IteractionListEdit.onVacancyFielsValueChange(IteractionListEdit.java:1489)
  at StandardEditor.setupEntityToEdit
```

### Причина
При открытии IteractionListEdit через `onButtonCopyClick` в `IteractionListBrowse.java:266`, экран загружает `Project` без атрибута `projectDepartment` в view. Но `vacancyFieldValueChange()` (строка 316) пытается читать `project.getProjectDepartment()`.

### Требуется
1. Добавить `projectDepartment` в view сущности `Project`, которым грузится edit-экран
2. Либо переписать `vacancyFieldValueChange()` так, чтобы не обращаться к `projectDepartment` без проверки

### Affected files
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java` (строка 316)
- Возможно `modules/global/src/com/company/hunttech/views.xml` (view для Project)

---

## Закрытие (2026-09-03)

Задача устарела — исправление выполнено и проверено 2026-07-25:

- Коммит `64cf6a7c` «fix(iteraction-list): загрузить подразделение проекта при копировании» (branch `agent/iteraction-list-copy-project-department`): `onButtonCopyClick` перечитывает вакансию узким view через `loadVacancyForCopy()` → `dataManager.reload(vacancy, openPosition-iteraction-list-picker-view)` до открытия editor.
- View `openPosition-iteraction-list-picker-view` (views.xml:1235) содержит весь граф, читаемый `vacancyFieldValueChange()`: `projectName → projectDepartment (_minimal: departamentRuName) → companyName (_minimal: companyShortName)` — Data View Integrity соблюдена.
- По коммит-сообщению также добавлен регрессионный тест unfetched-сценария и обновлена спецификация IteractionListEdit.
- Проверено при закрытии (2026-09-03): код `loadVacancyForCopy` присутствует в IteractionListBrowse.java:265-281, view задекларирован; файл компилируется (все сборки :app-web:compileJava в этой сессии зелёные).
- Задача оставалась помеченной Open ошибочно.

Закрыл: Antigravity (hrm-dev, agent/antigravity-dev).
