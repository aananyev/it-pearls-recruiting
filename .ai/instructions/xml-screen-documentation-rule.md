# Hermes — проверка общего правила документирования XML-экранов

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/xml-screen-documentation-rule`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR
- Режим: проверка правил и документации без изменения кода

До запуска подтвердить:

- branch существует;
- branch HEAD = PR HEAD = переданный SHA;
- `base=master`;
- conflicts `NONE`.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Назначение

Проверить, что в HRM HuntTech введено обязательное правило для любого создаваемого или изменяемого XML-дескриптора экрана CUBA 7.3:

1. каждый открывающий XML-элемент получает смысловой inline-комментарий;
2. при изменении legacy descriptor аудируется весь файл;
3. одновременно обновляется `docs/ui/{FormName}_Spec.md`;
4. выполняются Data View Integrity, профильные тесты, сборка и runtime smoke;
5. comment-only изменения не меняют XML contracts.

## Разрешённый diff

- `.cursor/rules/xml-screen-documentation.mdc`;
- `.cursor/rules/living-ui-documentation.mdc`;
- `docs/architecture/XML_Screen_Documentation_Standard.md`;
- `docs/architecture/README.md`;
- эта инструкция.

Запрещены изменения:

- production Java;
- XML экранов приложения;
- entity, views, loaders и JPQL;
- SCSS;
- DB/Liquibase;
- build/infra/production.

## Статическая проверка правила

Проверить, что `.cursor/rules/xml-screen-documentation.mdc`:

- имеет `alwaysApply: true`;
- охватывает `browse`, `edit`, `view`, `lookup`, `window`, `fragment` и специальные XML-экраны;
- требует аудит всего изменяемого descriptor;
- требует комментарий перед каждым opening element;
- перечисляет data/loaders/query/layout/fields/actions/rows/columns;
- определяет исключения для declaration, closing tags, CDATA и attributes;
- запрещает формальные комментарии и домыслы;
- требует синхронизацию `docs/ui/{FormName}_Spec.md`;
- запрещает скрыто менять bindings, IDs, JPQL, actions и geometry;
- задаёт Data View Integrity, тесты, сборку и Hermes smoke;
- содержит Definition of Done и UI Diff-log.

Проверить, что `.cursor/rules/living-ui-documentation.mdc`:

- содержит ссылку на новый rule;
- активирует его при любом изменении XML;
- включает semantic comments audit в DoD;
- включает XML-комментарии в быстрый чеклист агента.

## Проверка документации

`docs/architecture/XML_Screen_Documentation_Standard.md` должен начинаться с:

1. Назначение и бизнес-смысл;
2. UI Context & Navigation;
3. Behavior Summary.

Далее должны быть описаны trigger, comment coverage, quality, UI Spec sync, safety contracts, checks и history.

`docs/architecture/README.md` должен содержать ссылку на стандарт и новую верхнюю строку истории `2026-07-28`.

## Команды

```bash
git diff --check
git diff --name-only master...HEAD

git diff master...HEAD -- \
  .cursor/rules/xml-screen-documentation.mdc \
  .cursor/rules/living-ui-documentation.mdc \
  docs/architecture/XML_Screen_Documentation_Standard.md \
  docs/architecture/README.md \
  .ai/instructions/xml-screen-documentation-rule.md

./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- diff scope: PASS;
- rule contract: PASS;
- documentation structure: PASS;
- compile: PASS;
- `ScreenViewIntegrityTest`: `8/8 PASS`;
- clean assemble: `BUILD SUCCESSFUL`.

Изменений XML/SCSS нет, поэтому отдельная visual regression не ожидается. После clean local deploy проверить:

- `/hrm/` = HTTP 200;
- Tomcat critical errors = NONE;
- smoke основных экранов без регрессии;
- P1=0;
- P2=0.

## Формат успешного отчёта

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/xml-screen-documentation-rule
PR: <number>
Base: master
Verified HEAD: <SHA>
HEAD match: PASS
Conflicts: NONE
Diff scope: PASS
XML documentation rule: PASS
Living UI integration: PASS
Architecture docs/history: PASS
ScreenViewIntegrityTest: 8/8 PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Tomcat errors: NONE
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

Hermes не меняет код или документацию, не делает commit, push, rebase, merge, не разрешает конфликты и не изменяет production.
