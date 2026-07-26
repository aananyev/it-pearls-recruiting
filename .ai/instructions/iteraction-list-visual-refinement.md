# Проверка визуального уточнения IteractionListEdit

PROJECT: HRM HuntTech

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-visual-refinement`
- Base: `master`
- PR: указать номер после создания PR.
- Проверять только точный HEAD SHA из PR.
- Режим: проверка без изменения функционального кода и документации.
- Несовпадение branch HEAD, PR HEAD или переданного SHA → `HEAD_MISMATCH`, проверку остановить.

## Область изменения

Проверяется строго визуальная и XML-компоновочная доработка `IteractionListEdit`:

- sidebar: фотография кандидата, меньший логотип проекта, ФИО, название вакансии, индекс, номер/дата, контекст вакансии;
- перенос существующего `mostPopularHbox` внутрь `popularAccordion`;
- компактная theme-aware геометрия аккордеонов и пяти быстрых кнопок;
- группировка существующих footer-actions справа;
- contract-тесты и `docs/ui/IteractionListEdit_Spec.md`.

Java-контроллер, бизнес-логика, entity, views, loaders, JPQL, actions, `invoke`, БД и Liquibase не должны изменяться.

## Предварительная проверка

```bash
git fetch --all --prune
git checkout agent/iteraction-list-visual-refinement
git reset --hard origin/agent/iteraction-list-visual-refinement

git rev-parse HEAD
git status --short
git diff --check
```

Подтвердить:

- branch существует;
- branch HEAD = PR HEAD = переданный SHA;
- PR открыт из `agent/iteraction-list-visual-refinement`;
- base PR = `master`;
- conflicts = `NONE`;
- рабочее дерево чистое.

## Compile и профильные тесты

```bash
./gradlew :app-core:compileTestJava \
          :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
          --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionCssContractTest' \
          --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
          --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
          --tests 'com.company.hunttech.core.IteractionListRpcCompatibilityContractTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace
```

Ожидается:

- все профильные тесты PASS;
- `ScreenViewIntegrityTest`: `8/8 PASS`;
- XML-дескриптор парсится;
- Data View Integrity PASS:
  - `candidate.fullName` входит в `iteractionList-edit-view`;
  - `vacancy.vacansyName` входит в `openPosition-iteraction-list-picker-view`;
  - getters контроллера не читают незагруженные атрибуты.

## SCSS и сборка

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- семь тем собраны;
- compiled CSS каждой темы содержит локальные селекторы:
  - `.iteraction-list-editor .iteraction-list-accordion-section`;
  - `.iteraction-list-editor .iteraction-list-popular-button`;
  - `.iteraction-list-editor .iteraction-list-footer-actions`;
- глобальные Vaadin-селекторы не добавлены;
- `BUILD SUCCESSFUL`.

## Clean local deploy и HTTP

1. Выполнить clean local deploy точного HEAD.
2. Проверить `http://localhost:8080/hrm/` → HTTP 200.
3. Открыть `IteractionListBrowse`, затем создать и отредактировать `IteractionListEdit`.

## Functional smoke

Проверить новый и существующий экземпляр `IteractionList`:

1. Выбрать кандидата:
   - `candidateField` записывает `candidate`;
   - ФИО в sidebar обновляется;
   - фотография или fallback отображается.
2. Выбрать вакансию:
   - `vacancyFiels` записывает `vacancy`;
   - название вакансии в sidebar обновляется;
   - логотип проекта отображается без обрезки;
   - карточка подразделения, проекта, статуса и приоритета сохраняет прежние данные.
3. Проверить lookup/open кандидата и вакансии.
4. Последовательно открыть пункты sidebar:
   - «Кандидат и вакансия»;
   - «Тип и действие»;
   - «Результат»;
   - «Комментарий»;
   - «Частые взаимодействия».
5. Для каждого пункта:
   - раскрывается один связанный GroupBox;
   - активный пункт выделяется;
   - данные ранее заполненных секций не очищаются.
6. Выбрать тип взаимодействия:
   - dynamic fields и `buttonCallAction` работают по прежним условиям;
   - captions, required и visible не изменены.
7. В секции «Частые взаимодействия»:
   - отображаются ровно пять равных кнопок;
   - captions видимы;
   - заполненная кнопка устанавливает точный `Iteraction` в `iteractionTypeField`;
   - пустые позиции показывают disabled «Нет данных».
8. Проверить rating, recruiter, communication method и comment.
9. Проверить «Подписаться», OK и «Отмена».
10. Сохранить запись и повторно открыть её: все введённые значения сохранены.

## Visual smoke

Проверить темы:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Для каждой темы подтвердить:

- порядок sidebar: изображения → ФИО → вакансия → индекс → номер/дата → карточка вакансии;
- фотография кандидата `112 × 112`, логотип проекта `80 × 80`;
- аккордеоны имеют компактный заголовок около `44 px`, текст `15 px / 600`;
- отсутствуют прежние высокие пустые полосы;
- отсутствует отдельная строка зелёных pill-кнопок над аккордеонами;
- частые взаимодействия находятся внутри соответствующего аккордеона;
- captions кнопок не пустые;
- footer-actions собраны одной группой справа;
- горизонтальная прокрутка, наложения и обрезка подписей отсутствуют.

Сохранить screenshots минимум для `halo`, `hover` и `hunttech-modern-dark`.

## Runtime logs

Проверить Tomcat logs после всех сценариев. Недопустимы новые:

- `ClassCastException`;
- `IllegalArgumentException` в `ServerRpcManager`;
- `Cannot get unfetched attribute`;
- detached/unfetched `IllegalStateException`;
- `NullPointerException` в изменённых сценариях;
- ошибки XML binding или отсутствующие component ID.

## Отчёт

Сохранить отчёт:

```text
.ai/reports/2026-07-27-iteraction-list-visual-refinement.md
```

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/iteraction-list-visual-refinement
PR: <номер>
BASE: master
VERIFIED HEAD: <полный SHA>
HEAD MATCH: PASS
CONFLICTS: NONE
PROFILE TESTS: PASS
ScreenViewIntegrityTest: 8/8 PASS
DATA VIEW INTEGRITY: PASS
SCSS: PASS
BUILD: SUCCESSFUL
LOCAL DEPLOY: PASS
HTTP /hrm/: 200
FUNCTIONAL SMOKE: PASS
VISUAL SMOKE: PASS
TOMCAT ERRORS: NONE
P1: 0
P2: 0
MERGE: NOT PERFORMED
PRODUCTION: NOT CHANGED
```

Ошибка:

```text
PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
FAILED STEP: <шаг>
ROOT CAUSE: <причина>
VERIFIED HEAD: <полный SHA>
```

Указать выполненные и невыполненные проверки, релевантный log/stack trace и рекомендацию. Код и документацию не менять, commit/push/rebase/merge не выполнять, production не трогать.
