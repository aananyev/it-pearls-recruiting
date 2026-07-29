# Hermes — проверка ExtUserEdit по общему Edit-контракту

PROJECT: HRM HuntTech  
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/ext-user-edit-shared-style-contract`
- Base: `master`
- Verified HEAD: точный HEAD PR
- Режим: проверка без изменения функционального кода и документации

До запуска подтвердить:

1. ветка существует;
2. HEAD ветки = HEAD PR = переданный SHA;
3. PR открыт напрямую в `master`;
4. conflicts `NONE`.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Разрешённый scope

- `ExtUserEdit` использует общий `edit-*`/`label-*` UI API;
- добавлены presentation-only navigation, toolbar, cards, email accordion и footer;
- локальный `ext-user-editor.scss` синхронизирован во всех семи темах;
- component ID, datasource/property, views, JPQL, actions, `invoke`, validators, save lifecycle, роли и замещения не изменяются.

Hermes не меняет Java, XML, SCSS, тесты или документацию, не делает commit, push, rebase или merge.

## Статические команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ExtUserEditSharedStyleContractTest' \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ExtUserChangePasswordContractTest' \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- compile: PASS;
- `ExtUserEditSharedStyleContractTest`: PASS;
- `ExtUserChangePasswordContractTest`: PASS;
- `ScreenViewIntegrityTest`: `8/8 PASS`;
- SCSS всех тем: PASS;
- `clean assemble`: `BUILD SUCCESSFUL`.

## Local deploy и browser smoke

После clean local deploy:

1. Проверить `http://localhost:8080/hrm/` → HTTP 200.
2. Открыть Administration → Users → существующего пользователя.
3. Проверить viewport: `1700×950`, `1366×768`, `1100×760`.
4. Проверить темы: `hover`, `halo`, `hunttech-modern-dark`.
5. Sidebar:
   - ширина стабильна, внутренние элементы не выходят за границы;
   - фото/fallback, ФИО, login и статус читаемы;
   - navigation меняется вместе с активной вкладкой;
   - одновременно активен один пункт текущего navigation-набора.
6. Общие настройки:
   - контакты, региональные настройки, роли и замещения доступны;
   - navigation переводит focus, не меняет значения и selection;
   - add/edit/remove ролей и замещений работают штатно.
7. Email:
   - пять полноширинных accordion-секций;
   - серверы раскрыты по умолчанию, остальные свёрнуты;
   - navigation раскрывает нужную секцию и фокусирует первое поле;
   - введённые значения не очищаются при сворачивании.
8. Персональный ИИ:
   - таблица и Create/Edit/Remove работают без регрессии;
   - API-ключ не отображается в таблице.
9. Смена пароля:
   - у существующего пользователя открывается `sec$User.changePassword`;
   - у нового пользователя отдельная кнопка скрыта, штатные password-поля доступны.
10. Save/Cancel, повторное открытие экрана и Tomcat logs — без ошибок.
11. Подтвердить, что роли, группа, замещения, contact/email/AI-данные не изменяются только от navigation/focus.

## Критерий успеха

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/ext-user-edit-shared-style-contract
PR: <number>
Base: master
Verified HEAD: <SHA>
HEAD match: PASS
Conflicts: NONE
Diff scope: PASS
Compile: PASS
ExtUserEditSharedStyleContractTest: PASS
ExtUserChangePasswordContractTest: PASS
ScreenViewIntegrityTest: 8/8 PASS
SCSS themes: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Visual smoke hover/halo/hunttech-modern-dark: PASS
ExtUser functional smoke: PASS
Tomcat errors: NONE
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

При ошибке: `STATUS: FAILED_VERIFICATION`, указать FAILED STEP, ROOT CAUSE, лог/stack trace и непроверенные шаги. Код, документацию, production и Git-history не изменять.
