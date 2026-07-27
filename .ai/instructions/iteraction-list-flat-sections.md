# Hermes — проверка плоских блоков IteractionListEdit

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-flat-sections`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR
- Режим: проверка без изменения кода и документации

До проверки подтвердить:

- branch существует;
- branch HEAD = PR HEAD = SHA из PR;
- `base=master`;
- conflicts `NONE`.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Разрешённый diff

- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`;
- `modules/core/test/com/company/hunttech/core/IteractionListEditAccordionLayoutTest.java`;
- семь файлов `com.company.hunttech/iteraction-list-flat-layout.scss`;
- семь `styles.scss`;
- `docs/ui/IteractionListEdit_XmlLayout_2026-07-27.md`;
- `.ai/instructions/iteraction-list-flat-sections.md`.

Production Java, services, entity, views.xml, JPQL, БД и Liquibase отсутствуют в diff.

## Что проверить статически

1. Четыре рабочих GroupBox имеют `collapsable="false"` и `collapsed="false"`.
2. Каждый рабочий блок содержит `iteraction-list-flat-section`.
3. `edit-accordion-section` отсутствует.
4. Сохранены ID:
   - `participantsAccordion`;
   - `interactionAccordion`;
   - `resultAccordion`;
   - `commentAccordion`;
   - все business fields.
5. Сохранены bindings, picker actions и invoke.
6. Во всех семи темах есть реальная копия `iteraction-list-flat-layout.scss`.
7. Каждый `styles.scss` импортирует partial и включает mixin после accordion-navigation mixin.
8. CSS ограничен `.iteraction-list-editor`.
9. Нет глобальных `.v-panel`, `.v-label`, `.v-button`, `.v-tabsheet`.
10. `IteractionListEdit.java` и service/business code не изменены.

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
3. Справа одновременно видны четыре блока ввода.
4. Заголовки не сворачивают содержимое.
5. Нажать четыре пункта label-navigation.
6. Каждый клик переводит фокус в целевой блок.
7. Целевой блок подсвечивается border/shadow.
8. Пункт «Частые взаимодействия» в label-navigation не отображается.
9. Quick actions остаются над блоками.
10. Проверить status/priority закрытой вакансии в sidebar.
11. Проверить dynamic fields, rating, comment, subscribe, save/cancel.
12. Проверить все семь тем и viewport `<=1366px`.
13. Horizontal scroll отсутствует.
14. Tomcat critical errors = NONE.
15. P1=0; P2=0.

Hermes не меняет код/docs, не делает commit, push, rebase, merge, не разрешает конфликты и не изменяет production.
