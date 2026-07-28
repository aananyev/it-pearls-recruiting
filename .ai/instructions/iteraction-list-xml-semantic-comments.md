# Hermes — проверка смысловых комментариев IteractionListEdit XML

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-xml-semantic-comments`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR
- Режим: проверка без изменения кода и документации

До запуска подтвердить:

- branch существует;
- branch HEAD = PR HEAD = переданный SHA;
- `base=master`;
- conflicts `NONE`.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Назначение

Проверить, что перед каждым открывающим XML-элементом `iteraction-list-edit.xml` находится отдельный смысловой комментарий, а структура и business contracts экрана не изменены.

## Разрешённый diff

- `iteraction-list-edit.xml` — только XML comments;
- `IteractionListXmlSemanticCommentsTest.java`;
- `docs/ui/IteractionListEdit_XmlSemanticComments_2026-07-28.md`;
- `docs/ui/iteraction-list/README.md`;
- эта инструкция.

Запрещённый diff:

- Java production controller;
- entity;
- DB/Liquibase;
- services;
- JPQL text;
- views;
- loaders и их параметры;
- component ID;
- bindings/actions/invoke;
- SCSS;
- production/infra.

## Статическая проверка

1. XML parse PASS.
2. Каждый открывающий тег имеет непосредственно перед собой отдельный `<!-- ... -->`.
3. Проверяются также:
   - `loader`;
   - `query`;
   - `condition`;
   - `and`;
   - `c:jpql`;
   - `c:where`;
   - `columns` / `column`;
   - `rows` / `row`;
   - `actions` / `action`;
   - все UI fields, layouts и buttons.
4. CDATA и closing tags отдельного комментария не требуют.
5. Комментарии объясняют смысл, а не повторяют имя тега.
6. Нет `TODO`, `Элемент vbox`, `Элемент label` и аналогичных заглушек.
7. Diff XML не меняет ни одного существовавшего непустого тега, атрибута, JPQL-фрагмента или порядка компонентов.

## Команды

```bash
git diff --check
git diff --name-only master...HEAD

git diff --word-diff=porcelain master...HEAD -- \
  modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListXmlSemanticCommentsTest' \
          --tests 'com.company.hunttech.core.IteractionListVisualAlignmentTest' \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
          --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
          --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- compile PASS;
- semantic comments test PASS;
- профильные regression tests PASS;
- `ScreenViewIntegrityTest`: `8/8 PASS`;
- SCSS PASS;
- `BUILD SUCCESSFUL`.

## Diff-аудит XML

Сравнить XML до и после удаления комментариев. После нормализации comments оба XML должны быть эквивалентны по:

- element tree;
- tag names;
- attributes;
- text/CDATA;
- element order.

Любое отличие кроме XML comments → `FAILED_VERIFICATION`.

## Local deploy и smoke

Выполнить clean local deploy точного HEAD.

Проверить:

- `http://localhost:8080/hrm/` → HTTP 200;
- экран открывается;
- sidebar и четыре блока отображаются как до задачи;
- label-navigation работает;
- candidate/vacancy selection работает;
- save/cancel/subscribe без регрессии;
- Tomcat critical errors = NONE;
- P1=0;
- P2=0.

## Формат успешного отчёта

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/iteraction-list-xml-semantic-comments
PR: <number>
Base: master
Verified HEAD: <SHA>
HEAD match: PASS
Conflicts: NONE
XML parse: PASS
XML comments coverage: PASS
XML normalized equivalence: PASS
Profile tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Browser smoke: PASS
Tomcat errors: NONE
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

Hermes не меняет код/docs, не делает commit, push, rebase, merge, не разрешает конфликты и не изменяет production.
