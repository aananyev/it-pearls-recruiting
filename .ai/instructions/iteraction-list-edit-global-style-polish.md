# Проверка глобального style API для Edit-экранов

PROJECT: HRM HuntTech

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-edit-visual-fixes`
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
- документация синхронизирована с общим контрактом экранных форм;
- общий контракт содержит обязательный preflight для нейросети, правила новой Edit-формы и безопасного поэкранного рефакторинга.

Запрещено менять business logic, entity, views, loaders, JPQL, actions, `invoke`, БД и Liquibase.

## Предварительная проверка

```bash
git fetch --all --prune
git checkout agent/iteraction-list-edit-visual-fixes
git reset --hard origin/agent/iteraction-list-edit-visual-fixes

git rev-parse HEAD
git status --short
git diff --check
```

Подтвердить:

- branch существует;
- branch HEAD = PR HEAD = переданный SHA;
- PR открыт из `agent/iteraction-list-edit-visual-fixes`;
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
- семь `edit-screen-shared-styles.scss` идентичны и не ссылаются на несуществующий `themes/common`;
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
- vacancy остаётся полностью видимой при длинном значении candidate;
- candidate и vacancy имеют одинаковые `font-size`, `line-height`, высоту, фон и рамку; дополнительный левый отступ vacancy используется только под provider-пиктограмму;
- provider-пиктограммы vacancy/recruiter имеют размер `20 × 20`, а текст начинается справа от них без пересечения;
- checkbox «Показывать только мои подписки» и его подпись выровнены и не накладываются;
- все input/picker/textarea выглядят единообразно по высоте, бордеру и радиусу;
- rating и recruiter находятся в двух равных видимых колонках, communication — отдельной полноширинной строкой;
- блоки «Оценка и коммуникация» и «Комментарий» не накладываются друг на друга;
- textarea комментария не обрезана и имеет читаемый внутренний отступ;
- фото кандидата и логотип проекта имеют одинаковый размер `96 × 96`, не накладываются и симметрично центрированы в sidebar;
- fallback-содержимое гармонично заполняет круги без избыточного белого поля;
- sidebar-навигация использует глобальные `label-*` стили;
- date, calendar и time в служебной sidebar-card не выходят за правую границу карточки;
- служебная sidebar-card расположена сразу под профильным блоком кандидата;
- footer-actions не перекрывает контент.

## ExtSettingsWindow smoke

Проверить, что экран настроек сохранил старый namespace и одновременно применяет глобальные стили:

- sidebar использует `edit-sidebar`;
- навигация использует `label-navigation`, `label-nav-item`, `label-nav-item-active`;
- карточки используют `edit-card`, заголовки `edit-card-title`;
- runtime-переключение активных пунктов не потеряло legacy classes.

## Документационный контракт

Проверить `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`:

- обязательное обращение требует от нейросети прочитать UI/UX-концепцию, shared style contract, XML documentation standard и UI-спецификацию до изменений;
- общие `edit-*` и `label-*` stylename имеют приоритет над локальными аналогами;
- описаны sidebar, workspace, toolbar, tabs, cards, accordions, form controls, help и footer-actions;
- отдельно описаны состояния полей, защита от переполнения, создание новой формы и рефакторинг существующей;
- фактическая SCSS-архитектура CUBA 7.3 зафиксирована как семь синхронных theme-local partial;
- история документа и `docs/architecture/README.md` синхронизированы.

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
