# Hermes — проверка компоновки правой части IteractionListEdit

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES  
Repository: `aananyev/it-pearls-recruiting`  
Branch: `agent/iteraction-list-edit-right-layout-fix`  
Base: `master`

## Режим

Проверка без изменения функционального кода и документации. Не выполнять merge, rebase, исправления, production deploy или изменения БД.

Точный `PR` и полный `HEAD SHA` передаются отдельным комментарием `Передача Hermes` в PR после создания итогового коммита. Перед началом проверки Hermes обязан сопоставить эти значения с GitHub.

Несовпадение любого из значений:

- branch HEAD;
- PR HEAD;
- переданный HEAD SHA;
- `base=master`;

означает `HEAD_MISMATCH`; проверку остановить.

## Область изменения

Разрешённый diff задачи:

- новый локальный `iteraction-list-workspace-layout.scss` во всех 7 темах;
- подключение этого partial после `iteraction-list-visual-alignment` в `styles.scss` семи тем;
- `IteractionListWorkspaceLayoutContractTest`;
- связанная UI-документация и индекс.

Не должны быть изменены:

- `IteractionListEdit.java`;
- `iteraction-list-edit.xml`;
- sidebar;
- entity/views/loaders/JPQL/actions/data bindings;
- сервисы;
- БД/Liquibase.

## Preflight

```bash
git fetch --all --prune
git checkout agent/iteraction-list-edit-right-layout-fix
git reset --hard origin/agent/iteraction-list-edit-right-layout-fix

git rev-parse HEAD
git status --short
git diff --check
```

Подтвердить:

- branch существует;
- рабочее дерево чистое;
- branch HEAD = PR HEAD = SHA из комментария `Передача Hermes`;
- PR открыт из `agent/iteraction-list-edit-right-layout-fix` напрямую в `master`;
- conflicts = `NONE`.

## Профильные тесты

```bash
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListWorkspaceLayoutContractTest' \
  --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
  --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
  --no-daemon --stacktrace
```

Ожидается `PASS` для всех перечисленных классов.

## Screen View Integrity

```bash
./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace
```

Ожидается: `8/8 PASS`.

## SCSS

```bash
./gradlew :app-web:buildScssThemes \
  --no-daemon --stacktrace
```

Проверить:

- сборку всех семи тем;
- отсутствие SCSS error/warning, связанных с новым partial;
- семь `iteraction-list-workspace-layout.scss` побайтово идентичны;
- новый partial включён после `iteraction-list-visual-alignment`;
- глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` задачей не добавлены.

## Полная сборка

```bash
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается: `BUILD SUCCESSFUL`.

## Local deploy и HTTP

Выполнить штатный local deploy HRM HuntTech для проверяемого HEAD.

Проверить:

```text
http://localhost:8080/hrm/
```

Ожидается: HTTP 200.

## Visual smoke IteractionListEdit

Проверить минимум темы:

- `halo`;
- `hover`;
- `hunttech-modern-dark`.

Сценарий:

1. Открыть существующую запись `IteractionListEdit` с заполненным кандидатом/вакансией.
2. Проверить верх формы при стандартной ширине рабочего окна.
3. Уменьшить ширину окна до области около `1100px` и повторить проверку.
4. Проверить форму с пустыми/короткими значениями и с длинными подписями кандидата/вакансии.

Обязательные критерии:

- sidebar визуально не изменён;
- верх правого workspace совпадает с верхом sidebar — workspace не «уехал» вниз;
- блок «Частые взаимодействия» находится непосредственно под toolbar;
- кандидат и вакансия расположены в одной строке 50/50;
- соседние строки единой карточки имеют одинаковый читаемый вертикальный интервал;
- `Тип взаимодействия` не накладывается на `Рекрутер`;
- `Рейтинг кандидата` и `Рекрутер` находятся в одной строке;
- `Способ связи` находится ниже отдельной полноширинной строкой;
- `Комментарий` расположен ниже предыдущих полей и не перекрывается ими;
- горизонтальный scroll/overflow отсутствует;
- footer actions видимы и доступны;
- сохранение без изменения бизнес-данных выполняется штатно.

## Runtime logs

После smoke проверить Tomcat logs. Недопустимы новые ошибки изменённого сценария, включая:

- `IllegalStateException`;
- `NullPointerException`;
- unfetched/detached errors;
- SCSS/theme errors;
- layout/runtime exceptions.

## Документация

Проверить синхронизацию:

- `docs/ui/IteractionListEdit_WorkspaceLayout_2026-09-17.md`;
- `docs/ui/iteraction-list/README.md`.

## Формат результата

При успехе:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/iteraction-list-edit-right-layout-fix
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
Tomcat errors: NONE
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

При ошибке:

```text
PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
FAILED STEP: ...
ROOT CAUSE: ...
Verified HEAD: ...
Выполненные проверки: ...
Неисполненные проверки: ...
Рекомендация: ...
Код не менялся.
Commit/push/merge не выполнялись.
Production не изменён.
```
