# Проверка исправления copy-сценария IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-copy-project-department`  
BASE: `master`  
MODE: проверка без изменения кода

## Правило точного HEAD

Точный полный SHA указывается в поле `VERIFIED HEAD TO CHECK` описания PR. До запуска проверок Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с переданным SHA;
3. PR открыт из этой ветки прямо в `master`;
4. HEAD PR совпадает с тем же SHA;
5. conflicts = NONE.

Несовпадение означает `HEAD_MISMATCH`; проверку остановить. Итоговый отчёт должен содержать: `проверен HEAD: <полный SHA>`.

## Причина исправления

`IteractionListBrowse#onButtonCopyClick` ранее передавал в новую сущность vacancy непосредственно из `iteractionList-browse-view`. Этот detached-граф не гарантирует загрузку `vacancy.projectName.projectDepartment`. При инициализации `IteractionListEdit` метод `vacancyFieldValueChange()` читает подразделение и компанию, что приводило к `Cannot get unfetched attribute [projectDepartment]`.

Исправление перечитывает выбранную вакансию через `openPosition-iteraction-list-picker-view` до передачи в новый editor. View уже содержит `projectName.projectDepartment.departamentRuName` и `projectName.projectDepartment.companyName.companyShortName`.

## Запреты

Hermes не изменяет Java, XML, views, тесты или документацию, не выполняет commit, push, rebase, merge, разрешение конфликтов и production-действия.

## Команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListCopyProjectDepartmentTest' \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `IteractionListCopyProjectDepartmentTest` — `1/1 PASS`;
- `IteractionListEditAccordionLayoutTest` — `5/5 PASS`;
- `LeftSidebarAvatarComponentTest` — `2/2 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- Data View Integrity — PASS;
- `BUILD SUCCESSFUL`;
- local deploy — PASS;
- HTTP `/hrm/` = `200`;
- critical Tomcat errors — NONE;
- `Cannot get unfetched attribute [projectDepartment]` — NONE;
- detached/unfetched errors в copy-сценарии — NONE;
- P1 = 0; P2 = 0.

## Functional smoke

1. Открыть `hunttech_IteractionList.browse`.
2. Выбрать строку с кандидатом и вакансией, у которой заполнены проект, подразделение и компания.
3. Нажать «Копировать».
4. Подтвердить, что новый `IteractionListEdit` открывается без исключения.
5. Проверить отображение компании, подразделения и проекта.
6. Проверить, что кандидат и вакансия скопированы, а новая запись сохраняется.
7. Повторить для вакансии без проекта и для проекта без подразделения — экран должен открываться без исключения.
8. Проверить Tomcat logs на `IllegalStateException`, `unfetched`, `detached`, `projectDepartment`.

Отчёт сохранить в `.ai/reports/`. Merge и production не выполнять.
