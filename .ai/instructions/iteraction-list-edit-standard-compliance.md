# Hermes — проверка соответствия IteractionListEdit общему Edit-стандарту

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

Проверить точный HEAD ветки `agent/iteraction-list-edit-standard-compliance` и PR №84 напрямую в `master`. Полный проверяемый SHA указан в описании PR после финального commit. До запуска обязательно подтвердить: branch HEAD = PR HEAD = указанному SHA. Несовпадение → `HEAD_MISMATCH`, проверку остановить.

Код и документацию не изменять; commit, push, rebase, merge и production не выполнять.

## Область изменения

- presentation-extension `IteractionListEditAccordionNavigation`;
- общий partial `modules/web/themes/common/edit-screen-shared-styles.scss`;
- подключение shared mixin в семи `styles.scss`;
- профильные contract tests;
- каноническая и legacy UI-спецификации.

## Неизменяемые контракты

- `InteractionService.getMostPolularIteraction(currentUser, 5)` и период последнего календарного месяца;
- текущий рекрутёр, group by type, count DESC;
- точный `Iteraction` в `iteractionTypeField`;
- entity, БД, Liquibase, loaders, JPQL, views;
- component ID, bindings, actions, `invoke`, required, validators;
- save/cancel, подписки, уведомления и динамические поля.

## Команды

```bash
git diff --check
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
  --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
  --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
  --tests 'com.company.hunttech.core.IteractionListRpcCompatibilityContractTest' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

## DOM и visual smoke

1. В DOM присутствуют `edit-screen-layout`, `edit-sidebar`, sidebar-роли, `label-navigation`, `edit-workspace`, `edit-toolbar`, `edit-card`, `edit-tabs`, `edit-workspace-scroll`, `edit-workspace-content`, `edit-accordion-section`, `edit-footer-actions`.
2. Каждый navigation-button содержит `label-nav-item`; одновременно один содержит `label-nav-item-active`.
3. Клик navigation раскрывает правильный GroupBox и переводит focus; entity и loaders не меняются.
4. Sidebar: 270 px, при viewport <=1366 px — 250 px.
5. Toolbar/footer: минимум 58 px; tabs: 48 px; однострочные поля: минимум 38 px.
6. Горизонтальная прокрутка формы отсутствует.
7. Фотография кандидата остаётся главным образом, логотип проекта отображается 80 × 80 и не обрезается.
8. Карточка пяти быстрых действий постоянно видима между toolbar и TabSheet.
9. Сценарии 0 / 1–4 / 5+ популярных типов сохраняют пять позиций; active и disabled-кнопки работают по контракту.
10. Проверить Halo и остальные шесть тем.

## Runtime

- clean local deploy;
- `http://localhost:8080/hrm/` = HTTP 200;
- candidate/vacancy, dynamic fields, rating, comment, subscription, save/cancel — PASS;
- Tomcat critical errors — NONE;
- P1=0, P2=0.

Успех: `STATUS: READY_TO_MERGE`, `проверен HEAD: <SHA>`. Новый commit аннулирует предыдущий отчёт.
