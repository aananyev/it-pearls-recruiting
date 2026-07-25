# Проверка эталонной компоновки IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-reference-layout`  
BASE: `master`  
MODE: проверка точного HEAD PR без изменения функционального кода, документации и production.

Точный полный HEAD SHA указывается в PR после завершения всех коммитов. Перед началом Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `HEAD SHA для проверки` из PR;
3. PR открыт из этой ветки напрямую в `master`;
4. HEAD PR совпадает с проверяемым SHA;
5. `base=master`;
6. conflicts = NONE.

Несовпадение — `HEAD_MISMATCH`, проверку остановить. Отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`.

## Смысл проверки

Компоновка `IteractionListEdit` объединяет подтверждённые паттерны:

- профильная тёмная sidebar и контекстные карточки — по `CandidateCVEdit`;
- индекс разделов и полноширинные вертикальные аккордеоны — по `ExtSettingsWindow`;
- Java-контроллер, entity, views, loaders, JPQL, bindings, actions, `invoke`, validation и lifecycle не изменяются.

## Обязательные команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
          --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionCssContractTest' \
          --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
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

Ожидается:

- compile web/core tests — PASS;
- все шесть профильных test class — PASS;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- Data View Integrity — PASS;
- SCSS всех семи тем — PASS;
- `BUILD SUCCESSFUL`.

## Local deploy

Развернуть точный HEAD локально, перезапустить Tomcat и проверить:

- `http://localhost:8080/hrm/` — HTTP `200`;
- Tomcat critical errors — NONE;
- XML loader/binding errors — NONE;
- `NullPointerException`, `ClassCastException`, detached/unfetched errors — NONE;
- P1 = 0;
- P2 = 0.

## Functional smoke

1. Открыть создание нового `IteractionListEdit`.
2. Проверить suggestion кандидата после трёх символов, lookup и open.
3. Проверить lookup/open вакансии и фильтр «только мои подписки».
4. Выбрать тип взаимодействия и проверить прежнее управление `buttonCallAction`, `addString`, `addDate`, `addInteger`.
5. Проверить rating, recruiter, communication method и comment.
6. Проверить подписку, save и cancel.
7. Открыть существующее взаимодействие и повторить сохранение без изменения значений.
8. Проверить сценарий «Копировать» без detached/unfetched ошибки.
9. В «Частых взаимодействиях» проверить ровно пять равных кнопок, top-5 текущего пользователя за год, прямую установку `Iteraction` и disabled «Нет данных».

## Navigation smoke

1. Индекс пяти разделов виден в постоянной sidebar после контекстных карточек.
2. Клик по каждому пункту раскрывает связанный аккордеон и сворачивает остальные.
3. Active-state синхронизирован с раскрытым разделом.
4. Фокус переводится в первое поле рабочего блока.
5. Клик по штатному заголовку GroupBox синхронизирует active-state индекса.
6. Вертикальная прокрутка sidebar позволяет добраться до индекса при небольшой высоте viewport.

## Visual smoke

Проверить `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`:

1. Диалог имеет полезную геометрию `1240 × 760 px`.
2. Sidebar имеет ширину `296 px`, адаптивно `276 px` и `260 px`.
3. Фотография кандидата `112 × 112 px`, логотип проекта `80 × 80 px`.
4. Профильный заголовок, карточка взаимодействия и карточка вакансии не накладываются друг на друга.
5. Подразделение, проект, статус, приоритет и стоимость переносятся и остаются читаемыми.
6. Индекс разделов оформлен в тёмной sidebar; активный пункт имеет оранжевый акцент.
7. Правая область не содержит отдельную колонку навигации и не сжимается искусственно.
8. Toolbar имеет высоту не менее `58 px`; вкладка — `48 px`.
9. Пять аккордеонов занимают 100% полезной ширины, имеют радиус `7 px` и стабильные интервалы.
10. «Кандидат» и «Вакансия» находятся в одной строке и имеют одинаковый стиль.
11. «Тип и действие» располагает динамические поля последовательно сверху вниз.
12. Горизонтальная прокрутка, обрезание picker-полей, перекрытия текста и скачки ширины отсутствуют.
13. Footer и три существующих действия видимы и не перекрывают контент.

Hermes не меняет Java, XML, SCSS, tests или docs; не делает commit, push, rebase или merge и не изменяет production. Отчёт сохранить в `.ai/reports/`.
