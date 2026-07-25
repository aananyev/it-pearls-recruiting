# Проверка правил оформления аккордеонов

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/ui-ux-accordion-rules`  
BASE: `master`  
MODE: проверка без изменения кода

## Правило точного HEAD

Точный полный SHA для проверки указывается в поле `VERIFIED HEAD TO CHECK` описания PR. До проверки Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с переданным SHA;
3. PR открыт из этой ветки прямо в `master`;
4. HEAD PR совпадает с тем же SHA;
5. conflicts = NONE.

Несовпадение означает `HEAD_MISMATCH`; проверку необходимо остановить. Итоговый отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`.

## Область проверки

Изменяется только документация:

- `docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md`;
- `.ai/instructions/ui-ux-accordion-rules.md`.

Java, XML экранов, SCSS, entity, views, JPQL, сервисы, БД и Liquibase не изменяются.

## Проверки

1. `git diff --check` — PASS.
2. Base PR = `master`, conflicts = NONE.
3. В разделе `6.3` присутствуют:
   - обязательный XML-контракт `GroupBoxLayout`;
   - правила `width`, `spacing`, `margin`, `collapsable`, `collapsed`, `showAsPanel`;
   - визуальная геометрия по эталону `SettingsWindow`;
   - правила заголовка и содержимого;
   - локальный SCSS namespace и запрет глобальных Vaadin-селекторов;
   - запрещённые решения;
   - критерии visual smoke.
4. История изменений содержит новую строку первой.
5. Markdown-кодовые блоки, заголовки, нумерация и ссылки не повреждены.
6. Изменения не затрагивают функциональные файлы.
7. `./gradlew clean assemble --no-daemon --stacktrace` — `BUILD SUCCESSFUL`.
8. Local deploy — PASS.
9. HTTP `/hrm/` = 200.
10. Critical Tomcat errors — NONE.
11. P1 = 0; P2 = 0.

Для документационного PR `ScreenViewIntegrityTest`, Data View Integrity и SCSS build могут быть отмечены `N/A`, поскольку код, XML экранов, views и SCSS не изменялись.

## Запреты

Hermes не изменяет код или документацию, не выполняет commit, push, rebase, merge, разрешение конфликтов и production-действия.

Отчёт сохранить в `.ai/reports/`. Merge и production не выполнять.
