# Task for ChatGPT — 2026-07-25

## 🐛 UNFETCHED ATTRIBUTE: Project.projectDepartment

**Где:** IteractionListEdit, при открытии через кнопку "Копировать"
**Статус:** 🔴 Open

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
