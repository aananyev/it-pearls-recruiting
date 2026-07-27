# Проверка компоновки и FileStorage в IteractionListEdit

PROJECT: HRM HuntTech

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-layout-storage-fix`
- PR: `#71`
- Base: `master`
- Base SHA: `d87af0dff9aff0a8acbbc49409a81bfd1c9f32c1`
- Verified HEAD: использовать только полный SHA, зафиксированный в поле `VERIFIED HEAD` PR №71.
- Режим: проверка без изменения функционального кода и документации.
- Несовпадение branch HEAD, PR HEAD и SHA из PR → `HEAD_MISMATCH`, проверку остановить.

## Область изменения

Проверяется один этап:

1. устранение наложений в раскрытом разделе «Кандидат и вакансия»;
2. естественная высота аккордеонов и корректный focus-scroll;
3. узкий suggestion view кандидата;
4. проверка физической читаемости `FileDescriptor` через `FileLoader.openStream()`;
5. fallback `OvaFallbackImage` при unreadable storage и unfetched nested value;
6. синхронизация семи тем, contract-тестов и документации.

Не изменялись entity, поля, БД, Liquibase, бизнес-обработчики взаимодействия, loader conditions, JPQL коллекций, actions и `invoke`.

## Предварительная проверка

```bash
git fetch --all --prune
git checkout agent/iteraction-list-layout-storage-fix
git reset --hard origin/agent/iteraction-list-layout-storage-fix

git rev-parse HEAD
git status --short
git diff --check
git diff --name-status origin/master...HEAD
```

Подтвердить:

- branch существует;
- branch HEAD = PR HEAD = `VERIFIED HEAD` из PR №71;
- PR открыт из `agent/iteraction-list-layout-storage-fix` напрямую в `master`;
- conflicts = `NONE`;
- рабочее дерево чистое;
- diff не содержит entity, Liquibase, БД и несогласованных бизнес-изменений.

## Compile и профильные тесты

```bash
./gradlew :app-core:compileTestJava \
          :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListLayoutStorageContractTest' \
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

- compile web/core tests — PASS;
- профильные тесты — PASS;
- XML `iteraction-list-edit.xml` и `iteraction-list-views.xml` парсятся;
- `ScreenViewIntegrityTest` — `8/8 PASS`.

## Data View Integrity

Проверить:

1. `jobCandidate-iteraction-list-suggestion-view` зарегистрирован через `cuba.viewsConfig`.
2. View содержит:
   - `fullName`;
   - `fileImageFace`;
   - `personPosition`;
   - `cityOfResidence`;
   - поля имени, `email`, `status`, `blockCandidate`.
3. View не содержит тяжёлые коллекции `candidateCv`, `iteractionList`, `skillTree`, `jobHistory`, договоры.
4. Suggestion query `candidateField` использует именно этот view.
5. `openPosition-iteraction-list-picker-view` продолжает содержать `projectName.projectLogo` и `projectDepartment.companyName.fileCompanyLogo`.
6. В runtime нет чтения unfetched getter при выборе кандидата через suggestion, lookup и при открытии существующей записи.

## SCSS и сборка

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- семь тем собраны;
- compiled CSS содержит локальные селекторы:
  - `.iteraction-list-editor .v-slot-iteraction-list-accordion-section`;
  - `.iteraction-list-editor .iteraction-list-participants-section`;
  - `.iteraction-list-editor .iteraction-list-subscription-filter`;
- глобальные Vaadin-селекторы не добавлены;
- `BUILD SUCCESSFUL`.

## Clean local deploy и HTTP

1. Выполнить clean local deploy точного HEAD.
2. Проверить `http://localhost:8080/hrm/` → HTTP 200.
3. Выполнить hard reload браузера с отключённым cache.
4. Открыть существующий `IteractionList` и создать новый.

## Functional smoke

Проверить:

1. `candidateField`:
   - ввод suggestion;
   - lookup;
   - open;
   - очистка и повторный выбор;
   - ФИО и изображение обновляются.
2. `vacancyFiels`:
   - выбор из options;
   - lookup/open;
   - название вакансии, проект, подразделение и логотип обновляются.
3. Checkbox «Показывать только мои подписки»:
   - полностью виден;
   - кликабелен;
   - фильтр вакансий работает по прежнему listener;
   - переключение не сдвигает и не перекрывает следующий аккордеон.
4. Последовательно открыть все пять пунктов sidebar.
5. Проверить сохранение введённых значений после переходов между секциями.
6. Проверить тип взаимодействия и dynamic fields.
7. Проверить rating, recruiter, communication method, comment.
8. Проверить пять частых взаимодействий.
9. Проверить «Подписаться», OK и «Отмена».
10. Сохранить и повторно открыть запись.

## Visual smoke

Обязательные темы:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Для каждой темы подтвердить:

- концепция sidebar/toolbar/TabSheet/аккордеонов сохранена;
- candidate picker и vacancy picker находятся на одной горизонтальной линии;
- checkbox расположен отдельной строкой под ними;
- checkbox и его caption не обрезаны;
- заголовок «Тип и действие» начинается после полного содержимого первой секции;
- соседние `GroupBoxLayout` не перекрываются;
- focus из sidebar не прячет заголовок секции под вкладкой;
- collapsed-секции имеют стабильную высоту;
- горизонтальной прокрутки и наложений нет;
- footer не перекрывает scroll-content.

Сохранить screenshots минимум для `halo`, `hover`, `hunttech-modern-dark` при раскрытой первой секции.

## FileStorage smoke

Проверить отдельно фотографию кандидата и логотип проекта/компании:

1. Descriptor = `null` → theme fallback.
2. Descriptor и физический файл существуют → реальное изображение.
3. Metadata descriptor существует, физический файл временно переименован/недоступен:
   - форма открывается;
   - отображается fallback;
   - metadata не удаляется;
   - UI не получает InternalError.
4. Временно сделать FileStorage недоступным:
   - форма открывается с fallback;
   - после восстановления и повторного открытия отображается реальный файл.
5. Выбрать кандидата через suggestion и lookup с узкими browse-графами:
   - `Cannot get unfetched attribute` отсутствует;
   - fallback или реальное изображение отображается корректно.

## Runtime logs

После всех сценариев проверить Tomcat logs. Недопустимы новые:

- `Cannot get unfetched attribute`;
- detached/unfetched `IllegalStateException`;
- необработанный `FileStorageException`;
- `ClassCastException` / `ServerRpcManager` error;
- XML binding error;
- `NullPointerException` в сценариях выбора кандидата/вакансии;
- HTTP 500 при выдаче изображения.

Допустим диагностический warning helper о недоступном descriptor без stack trace, блокирующего UI.

## Отчёт Hermes

Сохранить отчёт:

```text
.ai/reports/2026-07-27-iteraction-list-layout-storage-fix.md
```

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/iteraction-list-layout-storage-fix
PR: 71
BASE: master
VERIFIED HEAD: <полный SHA из PR>
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
FILESTORAGE SMOKE: PASS
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

Указать выполненные и невыполненные проверки, релевантный log/stack trace и рекомендацию. Код и документацию не менять; commit, push, rebase, merge и production-действия не выполнять.
