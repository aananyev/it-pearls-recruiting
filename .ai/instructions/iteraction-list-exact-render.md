# Hermes — проверка точного render-контракта IteractionListEdit

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-exact-render`
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

Проверить фактическое совпадение XML, active-controller и SCSS с плоским дизайном `IteractionListEdit`:

- четыре обычных VBox-блока;
- все блоки постоянно видимы;
- четыре label-navigation target;
- focus/scroll и active highlight;
- ровно пять quick-action позиций;
- отсутствие accordion runtime;
- неизменность бизнес-логики формы.

## Разрешённый diff

- `IteractionListEdit.java` — только presentation navigation и построение quick-action positions;
- `iteraction-list-edit.xml` — GroupBox → VBox при сохранении business fields;
- три профильных core test;
- семь синхронных `iteraction-list-flat-layout.scss`;
- `docs/ui/IteractionListEdit_Spec.md`;
- `docs/ui/IteractionListEdit_XmlLayout_2026-07-27.md`;
- эта инструкция.

Запрещённый diff:

- entity;
- DB/Liquibase;
- services;
- JPQL;
- views.xml;
- loaders;
- business handlers;
- production/infra.

## Статическая проверка

1. XML parse PASS.
2. В XML отсутствуют:
   - `<groupBox`;
   - `collapsable=`;
   - `collapsed=`;
   - `showAsPanel=`;
   - `popularAccordion`.
3. В XML присутствуют четыре VBox с ID:
   - `participantsAccordion`;
   - `interactionAccordion`;
   - `resultAccordion`;
   - `commentAccordion`.
4. Сохранены все field ID, `dataContainer`, `property`, actions и invoke.
5. Active controller инъецирует четыре `VBoxLayout`.
6. Active controller не содержит `setExpanded` и expanded listeners.
7. Runtime navigation создаёт ровно четыре кнопки.
8. Quick actions строятся циклом по пяти позициям.
9. Placeholder disabled и не имеет listener.
10. Семь theme-local SCSS-файлов идентичны.
11. CSS ограничен `.iteraction-list-editor`.
12. Нет panel-collapse/nth-child compensation.

## Команды

```bash
git diff --check

git diff --name-only master...HEAD

git diff master...HEAD -- \
  modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java

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

- compile PASS;
- профильные tests PASS;
- `ScreenViewIntegrityTest`: `8/8 PASS`;
- SCSS семи тем PASS;
- `BUILD SUCCESSFUL`.

## Проверка идентичности SCSS

```bash
sha256sum modules/web/themes/{halo,havana,helium,hover,hunttech-modern,hunttech-modern-light,hunttech-modern-dark}/com.company.hunttech/iteraction-list-flat-layout.scss
```

Ожидается один одинаковый hash.

## Local deploy

Выполнить clean local deploy точного HEAD согласно инструкции проекта.

Проверить:

- `http://localhost:8080/hrm/` → HTTP 200;
- Tomcat critical errors = NONE;
- P1=0;
- P2=0.

## Browser smoke

### Базовый layout

1. Открыть новый `IteractionListEdit`.
2. Справа одновременно отображаются четыре блока.
3. Заголовки статичны и не сворачивают поля.
4. Quick actions находятся над scroll-area.
5. Footer постоянно доступен.
6. Horizontal scroll отсутствует.

### Label-navigation

Для каждого пункта:

1. нажать пункт;
2. проверить `label-nav-item-active`;
3. проверить accent border/shadow целевого блока;
4. проверить focus первого поля;
5. проверить автоматический vertical scroll;
6. убедиться, что остальные блоки остаются видимыми.

### Quick actions

Проверить пользователей/данные с:

- 0 популярных типов → 5 disabled `Нет данных`;
- 1–4 типами → реальные кнопки + placeholders до 5;
- 5+ типами → ровно 5 реальных кнопок.

Клик реальной кнопки должен установить соответствующий тип. Placeholder не должен менять поле.

### Sidebar

Проверить:

- open/closed vacancy;
- статус и приоритет после названия вакансии;
- `ЗАКРЫТА` без разрыва;
- alternative warning;
- company/project/cost;
- локальную sidebar-прокрутку.

### Business regression

- candidate/vacancy select;
- only-my-subscriptions;
- dynamic fields addType 1/2/3;
- rating/recruiter/communication;
- required comment;
- subscribe;
- save/cancel;
- повторное открытие;
- существующая запись без посещения всех блоков.

### Viewports и темы

Проверить:

- 1700×950;
- 1366×768;
- при возможности 1100 px;
- все семь тем.

## Формат успешного отчёта

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/iteraction-list-exact-render
PR: <number>
Base: master
Verified HEAD: <SHA>
HEAD match: PASS
Conflicts: NONE
Compile: PASS
Profile tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS 7 themes: PASS
SCSS identity: PASS
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
