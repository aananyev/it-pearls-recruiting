## Описание изменений

В рамках данной задачи полностью удалена временная/дублирующая форма `CompanyReestrEdit` и все вызовы в проекте переведены на канонический экран редактирования компании `CompanyEdit` (`company-edit.xml`, `CompanyEdit.java`).

### 1. Удаление формы CompanyReestrEdit
- Удалены файлы формы `modules/web/src/com/company/hunttech/web/screens/company/CompanyReestrEdit.java` и `company-reestr-edit.xml`.
- Удален контрактный тест `CompanyReestrEditLayoutContractTest.java`.
- Удалена спецификация `docs/ui/CompanyReestrEdit_Spec.md`.
- Удалены неиспользуемые ключи локализации в `messages.properties` и `messages_ru.properties`.

### 2. Регистрация и аннотации CompanyEdit
- На контроллер `CompanyEdit` добавлена аннотация `@PrimaryEditorScreen(Company.class)`, регистрирующая `CompanyEdit` как главный экран редактирования сущности `Company` на уровне CUBA Platform.
- Файл `web-screens.xml` очищен от подмены дескриптора.

### 3. Маршрутизация вызовов редактора
- `modules/web/src/com/company/hunttech/web/screens/company/company-browse.xml`: действие `edit` и `create` настроены на `CompanyEdit`.
- `modules/web/src/com/company/hunttech/web/screens/company/company-reestr-browse.xml`: действия `edit` и `create` настроены на `CompanyEdit`.
- `modules/web/src/com/company/hunttech/web/screens/company/CompanyReestrBrowse.java`: в `setupTableActions` и `openEditCardBtn` действия переведены на `CompanyEdit.class`, устранены дублирующие подписчики `@Subscribe`.
- `modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java`: создание компании из формы кандидата переведено на `CompanyEdit.class`.

### 4. Тестирование и валидация
- Все 7 SCSS-тем идентичны master (0 расхождений).
- Контрактные тесты `:app-core:test --tests "com.company.hunttech.core.Company*"`: `BUILD SUCCESSFUL`.
- `ocr review --from master --to agent/antigravity-dev`: **0 замечаний** (`Review complete: 0 finding(s) across 8 selected item(s)`).
- Проверена локальная сборка и запуск (`start-app.sh --branch "$PWD"`): `HTTP 200` на `http://localhost:8080/hrm/`.
