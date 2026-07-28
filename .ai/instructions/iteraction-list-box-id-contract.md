# Hermes — проверка смысловых Box ID IteractionListEdit

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-box-ids`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR
- Режим: проверка без изменения кода и документации

До запуска подтвердить branch HEAD = PR HEAD = переданный SHA, `base=master`, conflicts `NONE`. Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Назначение

Проверить, что каждый элемент типа Box в `iteraction-list-edit.xml` имеет уникальный `id`, отражающий назначение контейнера, без изменения бизнес-логики и runtime behavior.

Под Box понимаются:

- `vbox`;
- `hbox`;
- `scrollBox`;
- `buttonsPanel`.

## Разрешённый diff

- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml` — только добавление/уточнение Box ID и форматирование XML;
- `modules/core/test/com/company/hunttech/core/IteractionListBoxIdContractTest.java`;
- `docs/ui/IteractionListEdit_BoxIdContract_2026-07-28.md`;
- `docs/ui/iteraction-list/README.md`;
- эта инструкция.

Запрещены изменения production Java, entity, DB/Liquibase, services, loaders, JPQL, views, bindings, actions, validators, SCSS и production/infra.

## XML contract

1. Каждый `vbox`, `hbox`, `scrollBox`, `buttonsPanel` имеет непустой `id`.
2. Все Box ID уникальны.
3. Новые ID используют `lowerCamelCase` и предметное назначение.
4. Запрещены generic ID: `box1`, `vbox`, `hbox`, `layout`, `container`, `panel`.
5. Устаревший `labelHBox` отсутствует; вместо него `vacancyRatingContextBox`.
6. Controller-compatible legacy ID сохранены:
   - `participantsAccordion`;
   - `interactionAccordion`;
   - `resultAccordion`;
   - `commentAccordion`;
   - `mostPopularHbox`;
   - `outstaffingCostHBox`;
   - `editActions`.
7. XML parse PASS.
8. Смысловые comments перед элементами сохранены.

## Обязательные новые ID

```text
iteractionProfileSummaryBox
vacancyStatusValueBox
vacancyPriorityValueBox
vacancyCompanyDepartmentBox
vacancyProjectBox
outstaffingCostContentBox
outstaffingCostValueBox
vacancyRatingContextBox
iteractionListToolbarBox
iteractionListSectionsBox
participantsSectionHeaderBox
interactionSectionHeaderBox
interactionSectionBodyBox
dynamicActionFieldsBox
resultSectionHeaderBox
commentSectionHeaderBox
commentSectionBodyBox
```

## Проверка неизменности business contracts

Сравнить `master...HEAD` и подтвердить, что не изменены:

- `dataContainer`, `property`, `optionsContainer`;
- loader/query/condition text;
- views;
- actions и `invoke`;
- required/visible/editable/enabled;
- width/height/expand/align/spacing/margin;
- stylename;
- порядок XML-элементов;
- production controller.

Допускается только добавление ID, замена `labelHBox` на `vacancyRatingContextBox` и форматирование атрибутов без изменения значений.

## Команды

```bash
git diff --check
git diff --name-only master...HEAD

git diff master...HEAD -- \
  modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListBoxIdContractTest' \
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
- Box ID contract PASS;
- semantic comments PASS;
- профильные regression tests PASS;
- `ScreenViewIntegrityTest 8/8 PASS`;
- SCSS PASS;
- `BUILD SUCCESSFUL`.

## Local deploy и browser smoke

После clean local deploy проверить:

- `/hrm/` = HTTP 200;
- экран открывается без XML injection errors;
- sidebar, toolbar, четыре раздела и footer отображаются как до задачи;
- label-navigation, quick actions, candidate/vacancy selection работают;
- save/cancel/subscribe без регрессии;
- Tomcat critical errors = NONE;
- P1=0;
- P2=0.

Hermes не меняет код/docs, не делает commit, push, rebase, merge, не разрешает конфликты и не изменяет production.
