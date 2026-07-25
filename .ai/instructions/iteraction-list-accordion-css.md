# Проверка локального CSS аккордеонов IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-accordion-css`  
BASE: `master`  
MODE: проверка без изменения кода

## Правило точного HEAD

Точный полный SHA для проверки указывается в поле `VERIFIED HEAD TO CHECK` описания PR. Перед запуском любых проверок Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с переданным SHA;
3. PR открыт из этой ветки прямо в `master`;
4. HEAD PR совпадает с тем же SHA;
5. conflicts = NONE.

Несовпадение означает `HEAD_MISMATCH`; проверку необходимо остановить. Итоговый отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`.

## Область проверки

Изменяются только:

- семь файлов `modules/web/themes/<theme>/com.company.hunttech/iteraction-list-accordion-navigation.scss`;
- `modules/core/test/com/company/hunttech/core/IteractionListAccordionCssContractTest.java`;
- `docs/ui/IteractionListEdit_Spec.md`;
- эта инструкция.

Java-контроллеры, XML-дескрипторы, entity, views, JPQL, loaders, actions, bindings, БД и Liquibase не изменяются.

## Проверки

```bash
git diff --check

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListAccordionCssContractTest' \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидаемые результаты:

- `IteractionListAccordionCssContractTest` — `1/1 PASS`;
- `IteractionListEditAccordionLayoutTest` — `5/5 PASS`;
- `LeftSidebarAvatarComponentTest` — `2/2 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- Data View Integrity — PASS;
- SCSS всех семи тем — PASS;
- `clean assemble` — `BUILD SUCCESSFUL`;
- local deploy — PASS;
- HTTP `/hrm/` = `200`;
- critical Tomcat errors — NONE;
- P1 = 0; P2 = 0.

## Functional и visual smoke

1. Открыть создание и редактирование `IteractionListEdit`.
2. Проверить, что «Взаимодействие» раскрыто, а «Комментарий» и «Популярные взаимодействия» свёрнуты.
3. Сравнить с `SettingsWindow` радиус, границу, фон секции, заголовок и расстояние между аккордеонами.
4. Проверить темы `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`.
5. Последовательно заполнить кандидата, вакансию, тип, динамическое поле, рейтинг, рекрутёра, способ связи и комментарий.
6. Сворачивать и раскрывать секции; убедиться, что введённые значения сохраняются.
7. Проверить подписку, сохранение и отмену.
8. Убедиться в отсутствии горизонтальной прокрутки и визуальных сдвигов.
9. Подтвердить, что XML, Java и бизнес-поведение не изменились.

## Запреты

Hermes не изменяет код или документацию, не выполняет commit, push, rebase, merge, разрешение конфликтов и production-действия.

Отчёт сохранить в `.ai/reports/`. Merge и production не выполнять.
