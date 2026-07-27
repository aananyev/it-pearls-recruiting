# Hermes — проверка подробной бизнес-документации IteractionListEdit

PROJECT: HRM HuntTech
STATUS: WAITING_FOR_HERMES

## Git-контракт

- Repository: `aananyev/it-pearls-recruiting`
- Branch: `agent/iteraction-list-business-documentation`
- Base: `master`
- Проверяемый HEAD: точный HEAD PR, указанный в описании PR
- Режим: проверка документации без изменения кода и `docs/`

До начала проверки подтвердить:

1. ветка существует;
2. branch HEAD = PR HEAD = SHA из описания PR;
3. `base=master`;
4. conflicts `NONE`.

Несовпадение означает `HEAD_MISMATCH` и остановку проверки.

## Разрешённый diff

- `docs/ui/IteractionListEdit_Spec.md`;
- `docs/screens/iteraction-list/hunttech_IteractionList.edit_Spec.md`;
- `.ai/instructions/iteraction-list-business-documentation.md`.

Функциональный код, XML, SCSS, entity, views, tests, БД и Liquibase изменяться не должны.

## Цель проверки

Подтвердить, что документация точно описывает фактический экран `hunttech_IteractionList.edit` после merge PR №85, theme-fix `21761d84...` и controller-fix Hermes `078ba63c4577c355142a49fcc31e5e775111a02f`.

## Обязательная содержательная проверка

Сопоставить документацию с:

- `IteractionListEdit.java`;
- `IteractionListEditAccordionNavigation.java`;
- `iteraction-list-edit.xml`;
- `IteractionList.java`;
- `InteractionServiceBean.java`;
- `views.xml`;
- theme-local `edit-screen-shared-styles.scss` всех семи тем.

Проверить разделы:

1. What & Why;
2. фактический runtime-controller;
3. UI Context & Navigation;
4. Behavior Summary;
5. entity fields и bindings;
6. containers/loaders/views/JPQL;
7. screen options;
8. Init/BeforeShow/AfterShow/BeforeCommit/AfterCommit/BeforeClose;
9. кандидат и история контактов;
10. вакансия, mismatch, закрытие и Researcher subscription;
11. фильтр вакансий;
12. dynamic fields и `Iteraction` flags;
13. быстрые взаимодействия;
14. rating;
15. подписка на кандидата;
16. chain interaction;
17. Employee start/end;
18. vacancy news;
19. candidate status;
20. notifications/email;
21. Data View Integrity;
22. performance/technical debt;
23. regression matrix;
24. история новой строкой сверху.

## Особо проверить

- только `IteractionListEdit` содержит `@UiController("hunttech_IteractionList.edit")`;
- `IteractionListEditAccordionNavigation` не содержит `@UiController`, `@UiDescriptor`, `@EditedEntityContainer`, `@LoadDataBeforeShow`;
- прямых runtime-ссылок на создание helper нет;
- фактический базовый controller создаёт пять legacy navigation-пунктов, включая скрытый `popularAccordion`;
- базовый controller создаёт только фактически найденные быстрые кнопки;
- `normalizePopularButtons()` и placeholders `Нет данных` существуют в helper, но автоматически не исполняются;
- период быстрых взаимодействий — один календарный месяц;
- active quick-action хранит точный `Iteraction`;
- Employee коммитится отдельно в `BeforeCommitChangesEvent`;
- start/end error notification не отменяет commit;
- `candidate.status` вычисляется из числового префикса `Iteraction.number`;
- notification types 0–5 не выполняют действия;
- письмо кандидату открывает `InternalEmailerEdit`, а не отправляется автоматически;
- `setSubscribe()` и `setCurrentUserName()` — legacy no-op;
- after commit создаётся vacancy news;
- `themes/common` и symlink не используются, имеются семь реальных theme-local копий.

## Команды

```bash
git diff --check

git diff --name-only master...HEAD

grep -n "@UiController\|class IteractionListEditAccordionNavigation" \
  modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit*.java

grep -R "new IteractionListEditAccordionNavigation\|IteractionListEditAccordionNavigation(" \
  modules --exclude='IteractionListEditAccordionNavigation.java' || true

grep -n "getMostPolularIteraction\|Calendar.MONTH\|group by e.iteractionType\|order by count" \
  modules/core/src/com/company/hunttech/core/InteractionServiceBean.java

grep -n "onBeforeCommitChanges\|checkEmployyementStatus\|setChainInteraction\|onAfterCommitChanges1\|onBeforeClose1" \
  modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java

grep -n "normalizePopularButtons\|EMPTY_POPULAR_CAPTION\|setEnabled(false)" \
  modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEditAccordionNavigation.java
```

Поскольку diff документационный, compile, `ScreenViewIntegrityTest`, SCSS build, deploy, HTTP и browser smoke допускается отметить `N/A`, если подтверждено отсутствие кодовых файлов в diff. Допустимо дополнительно выполнить `./gradlew clean assemble --no-daemon --stacktrace`.

## Формат результата

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/iteraction-list-business-documentation
PR: <number>
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS
Conflicts: NONE
Allowed diff: PASS
Business documentation accuracy: PASS
Runtime-controller distinction: PASS
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
проверен HEAD: <full SHA>
```

Ошибка:

```text
PROJECT: HRM HuntTech
STATUS: FAILED_VERIFICATION
FAILED STEP: <step>
ROOT CAUSE: <cause>
Code/docs changed by Hermes: NO
Merge: NOT PERFORMED
Production: NOT CHANGED
```

Hermes не меняет код или документацию, не делает commit, push, rebase, merge, не разрешает конфликты и не изменяет production.
