# CompanyEdit: обязательный handoff Antigravity → Hermes-1

Дата: 2026-09-07

## Цель

Исключить потерю изменений при передаче PR и ложный вывод, что Hermes-1 развернул
старый код. Для CompanyEdit проверяется не название PR и не история ветки, а точный
`HEAD PR`, итоговый merge result и артефакт, собранный из этого merge result.

## Разбор текущего инцидента

- `master` `7f916f6c0` — merge PR #246.
- `agent/antigravity-dev` `a70f288d9` — HEAD PR #246.
- Дерево `a70f288d9` и дерево `7f916f6c0` совпадают: merge PR не потерял файлы.
- `git diff origin/master...origin/agent/antigravity-dev` для защищённой зоны PR #246
  показывает только семь `company-editor.scss`; `company-edit.xml` в этом PR не менялся.
- XML текущего `master` уже был унаследован из PR #245 (`14eb50fcc`). Поэтому PR #246
  не мог исправить XML-регрессию, которой не было в его diff.
- `8478ecd6` не является чистым состоянием `eb795a67 + 8f425f0f`: это синтетический
  гибрид. Полезные изменения из этих коммитов нельзя восстанавливать полным `revert`
  без сравнения итогового дерева.

Вывод: подтверждённой потери кода при merge PR #246 нет. Ошибка возникла раньше —
при сборке базового состояния rollback/PR #245 и при принятии PR без проверки того,
что заявленное исправление действительно находится в diff PR.

## Правило для Antigravity

1. Перед началом получить свежий `origin/master` и зафиксировать:

   ```text
   BASE_SHA=<git rev-parse origin/master>
   WORKTREE=<git status --short --branch>
   HEAD_SHA=<git rev-parse HEAD>
   ```

2. Для CompanyEdit использовать отдельную свежую topic-ветку от актуального
   `origin/master`. Долгоживущую `agent/antigravity-dev` не использовать как основу
   нового исправления, если в ней есть merge-коммиты старых PR.

3. В PR явно указать:

   ```text
   BASE_SHA: <SHA>
   VERIFIED_HEAD: <SHA>
   OWNER: Antigravity
   PROTECTED_FILES: <список>
   BASELINE: <какой коммит является эталоном и почему>
   ALLOWED_DELTAS: <точный список изменений>
   ```

4. Перед передачей проверить, что нужный файл реально входит в diff:

   ```bash
   git diff --name-status "$BASE_SHA...$HEAD_SHA"
   git diff --check "$BASE_SHA...$HEAD_SHA"
   ```

   Если PR заявляет исправление `company-edit.xml`, но XML отсутствует в diff,
   PR не считается исправлением CompanyEdit и не передаётся Hermes-1.

5. Приложить к PR результаты контрактных тестов и OCR-review. Незакоммиченные
   изменения, stash и ручное копирование файлов из другого worktree запрещены.

## Правило для Hermes-1

Перед merge Hermes-1 обязан зафиксировать точный head PR через GitHub и локальный Git:

```bash
git fetch origin --prune
PR_HEAD=$(gh pr view <PR> --json headRefOid --jq .headRefOid)
BASE_SHA=$(git rev-parse origin/master)
test "$(git rev-parse origin/<head-branch>)" = "$PR_HEAD"
git diff --name-status "$BASE_SHA...$PR_HEAD"
git diff --check "$BASE_SHA...$PR_HEAD"
```

Затем в отдельном временном worktree проверить merge result до GitHub merge:

```bash
git merge-tree --write-tree "$BASE_SHA" "$PR_HEAD"
```

Любой конфликт, `HEAD_MISMATCH`, расхождение ветки с PR или изменение защищённого
файла вне заявленного списка означает `FAILED_VERIFICATION`; merge и deploy
останавливаются. Конфликт CompanyEdit нельзя разрешать выбором `ours`, `theirs`,
копированием старого файла или полным revert другого PR.

После merge:

```bash
git pull --ff-only origin master
MERGE_SHA=$(git rev-parse HEAD)
git merge-base --is-ancestor "$PR_HEAD" "$MERGE_SHA"
git diff-tree --no-commit-id --name-status -r "$MERGE_SHA"
```

Сборка и deploy выполняются только из этого чистого `master`. В отчёт записываются
`BASE_SHA`, `PR_HEAD`, `MERGE_SHA`, версия приложения и SHA исходного XML/SCSS.
Проверка только исходной ветки PR не заменяет проверку merge result.

## Artifact provenance

Для каждого PR, затрагивающего CompanyEdit, в отчёте Hermes-1 должна быть цепочка:

```text
BASE_SHA
  → PR_HEAD
  → MERGE_SHA
  → SHA source company-edit.xml / company-editor.scss
  → SHA XML/SCSS внутри app-web JAR/WAR
  → SHA exploded Tomcat-файлов
```

Если SHA источника и SHA артефакта различаются, deploy считается неуспешным даже при
HTTP 200. В отчёте нельзя писать «версия актуальна» без этих идентификаторов.

## Правило откатов

Откат PR и восстановление рабочего UI — разные операции. Для CompanyEdit нельзя
использовать `revert` всего PR, если PR содержит layout, SCSS, локализацию, тесты и
другие изменения. Сначала формируется manifest разрешённых файлов и изменений,
затем создаётся новый узкий PR от свежего `master`. Коммиты `8478ecd6`, `ed5523b1`,
`8f425f0f3` и `eb795a67f` не использовать как готовую комбинацию без сравнения
итоговых blob/tree SHA.

## Статусы handoff

```text
READY_FOR_HERMES
BASE_SHA: ...
VERIFIED_HEAD: ...
PR: ...
FILES: ...
TESTS: ...
OCR: PASS
MERGE_TREE: CLEAN
```

Hermes-1 отвечает одним из двух статусов:

```text
MERGED_AND_DEPLOYED
PR_HEAD: ...
MERGE_SHA: ...
ARTIFACT_SHA: ...
HTTP: 200
REPORT: .ai/reports/...
```

или:

```text
FAILED_VERIFICATION
FAILED_STEP: ...
EXPECTED: ...
ACTUAL: ...
ACTION_REQUIRED: ...
```
