# Hermes — проверка XML-компоновки IteractionListEdit

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-xml-layout`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR
- Режим: проверка без изменения кода и документации

До проверки подтвердить branch HEAD = PR HEAD = SHA из PR, `base=master`, conflicts `NONE`. Несовпадение → `HEAD_MISMATCH`.

## Разрешённый diff

- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`;
- `modules/core/test/com/company/hunttech/core/IteractionListEditAccordionLayoutTest.java`;
- `modules/core/test/com/company/hunttech/core/IteractionListSidebarContextPanelTest.java`;
- `docs/ui/IteractionListEdit_Spec.md`;
- `docs/screens/iteraction-list/hunttech_IteractionList.edit_Spec.md`;
- `.ai/instructions/iteraction-list-xml-layout.md`.

Java production code, services, entity, views.xml, SCSS, БД и Liquibase отсутствуют в diff.

## Что проверить

1. `statusOfVacansyLabel`, `alternativeVacancyLinkButton`, `trafficLighterImage`, `currentPriorityLabel` находятся после `iteractionVacancyNameLabel` и до `iteractionListNavigation`.
2. Эти component ID отсутствуют в нижней `iteraction-list-vacancy-card`.
3. `participantsAccordion` имеет `collapsed="false"`.
4. `interactionAccordion`, `resultAccordion`, `commentAccordion` имеют `collapsed="true"`.
5. Все рабочие GroupBox имеют `height="AUTO"`.
6. Сохранены все component ID, bindings, actions, invoke, loaders, JPQL и views.
7. Business-controller `IteractionListEdit.java` не изменён.
8. SCSS семи тем не изменён.

## Команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
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

- профильные тесты PASS;
- `ScreenViewIntegrityTest`: `8/8 PASS`;
- SCSS семи тем PASS;
- `BUILD SUCCESSFUL`.

## Runtime smoke

После clean local deploy точного HEAD:

1. `http://localhost:8080/hrm/` → HTTP 200.
2. Открыть новый и существующий `IteractionListEdit`.
3. Под названием вакансии отображаются статус и приоритет.
4. Они находятся выше `РАЗДЕЛЫ ФОРМЫ`.
5. Первый раздел «Кандидат и вакансия» открыт сразу после загрузки.
6. Поля candidate/vacancy находятся в двух равных колонках.
7. Остальные три раздела закрыты и раскрываются штатно.
8. Проверить candidate/vacancy, dynamic fields, rating, comment, subscribe, save/cancel.
9. Проверить Halo и остальные шесть тем.
10. Tomcat critical errors = NONE; P1=0; P2=0.

Hermes не меняет код/docs, не делает commit, push, rebase, merge и не изменяет production.
