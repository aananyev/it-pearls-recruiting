# Проверка аккордеонов IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-settings-accordion-design`  
PR: `#38`  
BASE: `master`  
MODE: проверка без изменения кода

## Правило точного HEAD

Точный полный SHA для проверки указан в поле `VERIFIED HEAD TO CHECK` описания PR #38. Перед запуском любых проверок Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с SHA из PR;
3. PR открыт из этой ветки прямо в `master`;
4. HEAD PR совпадает с тем же SHA;
5. conflicts = NONE.

Любое несовпадение означает `HEAD_MISMATCH`; проверку необходимо остановить. Формулировка итогового отчёта: `проверен HEAD: <полный SHA>`.

## Запреты

Hermes не изменяет Java, XML, SCSS, тесты или документацию, не выполняет commit, push, rebase, merge, разрешение конфликтов и production-действия.

## Проверки

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидаемые результаты:

- `IteractionListEditAccordionLayoutTest` — 5/5 PASS;
- `LeftSidebarAvatarComponentTest` — 2/2 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS;
- SCSS всех семи тем — PASS;
- clean assemble — `BUILD SUCCESSFUL`;
- local deploy — PASS;
- HTTP `/hrm/` = 200;
- critical Tomcat errors — NONE;
- P1 = 0; P2 = 0.

## Functional и visual smoke

1. Открыть создание и редактирование взаимодействия.
2. Проверить, что «Взаимодействие» раскрыто, а «Комментарий» и «Популярные взаимодействия» свёрнуты.
3. Сравнить геометрию, фон, границы, заголовок и интервалы с аккордеонами `SettingsWindow`.
4. Проверить темы `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`.
5. Последовательно заполнить кандидата, вакансию, тип, динамическое поле, рейтинг, рекрутёра, способ связи и комментарий.
6. Сворачивать и раскрывать секции; убедиться, что значения не очищаются.
7. Проверить подписку, сохранение и отмену.
8. Убедиться в отсутствии горизонтальной прокрутки и пустых двухколоночных областей.

Отчёт сохранить в `.ai/reports/` по принятой структуре проекта. Merge и production не выполнять.
