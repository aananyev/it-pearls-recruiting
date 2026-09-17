# Hermes — проверка адаптивности IteractionListEdit

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES  
Repository: `aananyev/it-pearls-recruiting`  
Branch: `agent/iteraction-list-responsive-workspace-polish`  
Base: `master`

## Режим

Проверка без изменения функционального кода и документации. Не выполнять merge, rebase, исправления, production deploy или изменения БД.

Точный `PR` и полный `HEAD SHA` передаются отдельным комментарием `Передача Hermes` в PR. Перед началом проверки Hermes обязан подтвердить:

- branch существует;
- branch HEAD = PR HEAD = переданный SHA;
- PR открыт напрямую в `master`;
- conflicts = `NONE`.

Любое несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Разрешённая область diff

Только:

- `iteraction-list-workspace-layout.scss` во всех 7 темах;
- `IteractionListWorkspaceLayoutContractTest.java`;
- связанная UI-документация правой рабочей области;
- эта инструкция.

Не должны быть изменены:

- `IteractionListEdit.java`;
- `iteraction-list-edit.xml`;
- размеры, breakpoints, содержимое и визуальная геометрия sidebar;
- entity/views/loaders/JPQL/data bindings/actions/invoke;
- сервисы;
- БД/Liquibase;
- другие экраны.

## Preflight

```bash
git fetch --all --prune
git checkout agent/iteraction-list-responsive-workspace-polish
git reset --hard origin/agent/iteraction-list-responsive-workspace-polish
git rev-parse HEAD
git status --short
git diff --check
```

## Профильные тесты

```bash
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListWorkspaceLayoutContractTest' \
  --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
  --no-daemon --stacktrace
```

Ожидается PASS.

## Screen View Integrity

```bash
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ожидается `8/8 PASS`.

## SCSS

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
```

Проверить:

- все 7 тем PASS;
- 7 копий `iteraction-list-workspace-layout.scss` идентичны;
- отсутствуют новые глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet`;
- финальный partial не задаёт `width: 312px`, `width: 252px` и не меняет sidebar breakpoints.

## Полная сборка

```bash
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается `BUILD SUCCESSFUL`.

## Local deploy / HTTP

Выполнить штатный local deploy проверяемого HEAD и проверить:

```text
http://localhost:8080/hrm/
```

Ожидается HTTP 200.

## Visual smoke IteractionListEdit

Проверить минимум темы `halo`, `hover`, `hunttech-modern-dark` на широком, среднем и узком viewport.

Критерии PASS:

1. sidebar по размеру и внешнему виду идентичен `master` до задачи;
2. workspace начинается на одной верхней линии с sidebar;
3. workspace плавно занимает всё оставшееся пространство при resize браузера;
4. нет горизонтального overflow/scroll;
5. на нормальной ширине `Кандидат | Вакансия` и `Рейтинг | Рекрутер` — две адаптивные колонки;
6. на viewport `<=960px` только правая форма переходит в одну колонку, sidebar не меняется этим правилом;
7. каждый caption расположен строго над собственным input/picker/textarea и совпадает с ним по левому краю;
8. активные зелёные кнопки «Частые взаимодействия» имеют белую жирную подпись;
9. disabled `Нет данных` сохраняет нейтральный стиль;
10. `Способ связи`, `Комментарий` и footer не перекрываются;
11. save/cancel работают штатно;
12. Tomcat logs — новых ошибок изменённого сценария нет.

## Формат результата

При успехе:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/iteraction-list-responsive-workspace-polish
PR: <номер>
Base: master
Verified HEAD: <полный SHA>
HEAD match: PASS
Conflicts: NONE
Profile tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS: PASS
clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200 PASS
Visual smoke: PASS
Sidebar unchanged: PASS
Tomcat errors: NONE
Docs synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

При ошибке: `STATUS: FAILED_VERIFICATION`, указать FAILED STEP, ROOT CAUSE, лог/stack trace, выполненные и неисполненные проверки. Код, commit, merge и production не менять.
