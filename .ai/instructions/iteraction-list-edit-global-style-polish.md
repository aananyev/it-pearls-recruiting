# Проверка глобального style API для Edit-экранов

PROJECT: HRM HuntTech

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-edit-design-polish`
- Base: `master`
- PR: указать номер после создания PR.
- Проверять только точный HEAD SHA из PR.
- Режим: проверка без изменения функционального кода и документации.
- Несовпадение branch HEAD, PR HEAD или переданного SHA -> `HEAD_MISMATCH`, проверку остановить.

## Область изменения

Проверяется визуальная доработка `IteractionListEdit` и преимущественное применение общего Edit style API:

- `edit-form-control` вынесен в `edit-screen-shared-styles.scss` для единых input/picker/textarea;
- `iteraction-list-edit.xml` использует `edit-form-control` для основных полей формы;
- локальный `iteraction-list-visual-alignment.scss` оставляет только screen-specific геометрию;
- `ExtSettingsWindow` получает глобальные `edit-*` и `label-*` классы поверх legacy namespace;
- документация синхронизирована с общим контрактом экранных форм.

Запрещено менять business logic, entity, views, loaders, JPQL, actions, `invoke`, БД и Liquibase.

## Предварительная проверка

```bash
git fetch --all --prune
git checkout agent/iteraction-list-edit-design-polish
git reset --hard origin/agent/iteraction-list-edit-design-polish

git rev-parse HEAD
git status --short
git diff --check
```

Подтвердить:

- branch существует;
- branch HEAD = PR HEAD = переданный SHA;
- PR открыт из `agent/iteraction-list-edit-design-polish`;
- base PR = `master`;
- conflicts = `NONE`;
- рабочее дерево чистое.

## Профильные тесты

```bash
./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionCssContractTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowRemainingNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowEmailNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowAiNavigationTest' \
          --no-daemon --stacktrace

./gradlew :app-core:test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ожидается:

- профильные тесты PASS;
- `ScreenViewIntegrityTest`: PASS;
- XML-дескрипторы парсятся;
- Data View Integrity PASS.

Примечание: если root-команда `./gradlew test --tests '*ScreenViewIntegrityTest*'` падает из-за `app-web:test` без подходящих тестов, проверять именно `:app-core:test`.

## SCSS и сборка

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- семь тем собраны;
- compiled CSS содержит `.edit-form-control`;
- compiled CSS содержит `.iteraction-list-participants-grid` и `.iteraction-list-result-grid`;
- глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` не добавлены;
- `BUILD SUCCESSFUL`.

## Local deploy и HTTP

1. Выполнить clean local deploy точного HEAD.
2. Проверить `http://localhost:8080/hrm/` -> HTTP 200.
3. Открыть `IteractionListBrowse`, затем существующий `IteractionListEdit` на записи `54 128` или аналогичной.

## Visual smoke

Проверить минимум темы `hover`, `halo`, `hunttech-modern-dark`:

- candidate и vacancy находятся в двух равных колонках и не вылезают за карточку;
- все input/picker/textarea выглядят единообразно по высоте, бордеру и радиусу;
- блоки «Оценка и коммуникация» и «Комментарий» не накладываются друг на друга;
- textarea комментария не обрезана и имеет читаемый внутренний отступ;
- фото кандидата и логотип проекта не накладываются, логотип проекта меньше фото;
- sidebar-навигация использует глобальные `label-*` стили;
- footer-actions не перекрывает контент.

## ExtSettingsWindow smoke

Проверить, что экран настроек сохранил старый namespace и одновременно применяет глобальные стили:

- sidebar использует `edit-sidebar`;
- навигация использует `label-navigation`, `label-nav-item`, `label-nav-item-active`;
- карточки используют `edit-card`, заголовки `edit-card-title`;
- runtime-переключение активных пунктов не потеряло legacy classes.

## Runtime logs

Проверить Tomcat logs после smoke. Недопустимы новые ошибки:

- XML binding/component ID;
- `Cannot get unfetched attribute`;
- detached/unfetched `IllegalStateException`;
- `NullPointerException` в изменённых экранах;
- SCSS/theme compilation errors.

## Отчёт

Сохранить отчёт в `.ai/reports/` со статусом:

- успех: `STATUS: READY_TO_MERGE`;
- ошибка: `STATUS: FAILED_VERIFICATION`.

В отчёте обязательно указать: Repo, Branch, PR, Base, проверенный HEAD, HEAD match, conflicts, тесты, SCSS, assemble, HTTP, smoke, docs/history synchronized.
