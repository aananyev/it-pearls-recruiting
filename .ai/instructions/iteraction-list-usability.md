# Hermes — проверка навигации и частых взаимодействий IteractionListEdit

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-usability`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: проверка без изменения кода

## Точный HEAD

Полный SHA для проверки указан в описании PR в поле `VERIFIED HEAD TO CHECK`. Перед выполнением команд подтвердить:

1. ветка существует;
2. HEAD ветки равен указанному SHA;
3. PR открыт из `agent/iteraction-list-usability` прямо в `master`;
4. HEAD PR равен тому же SHA;
5. conflicts = NONE.

Несовпадение означает `HEAD_MISMATCH`; проверку остановить. Итоговый отчёт обязан содержать формулировку `проверен HEAD: <полный SHA>`.

## Область изменений

Разрешённый diff:

- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java`;
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEditAccordionNavigation.java`;
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml`;
- `modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit-accordion-navigation.xml`;
- семь локальных файлов `modules/web/themes/<theme>/com.company.hunttech/iteraction-list-accordion-navigation.scss`;
- профильные тесты `IteractionListEditAccordionLayoutTest`, `IteractionListAccordionNavigationTest`, `IteractionListMostPopularInteractionTest`;
- `docs/ui/IteractionListEdit_Spec.md`;
- эта инструкция.

`.github/workflows/`, entity, БД, Liquibase, `views.xml`, services, существующие loader definitions и production не изменяются.

## Команды

```bash
git diff --check

./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
  --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
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

- `IteractionListEditAccordionLayoutTest` — `5/5 PASS`;
- `IteractionListAccordionNavigationTest` — `4/4 PASS`;
- `IteractionListMostPopularInteractionTest` — `4/4 PASS`;
- `LeftSidebarAvatarComponentTest` — `2/2 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- Data View Integrity — PASS;
- SCSS семи тем — PASS;
- `clean assemble` — `BUILD SUCCESSFUL`;
- local deploy — PASS;
- HTTP `/hrm/` = `200`;
- Tomcat critical errors — NONE;
- P1 = 0; P2 = 0.

## Functional smoke

1. Открыть создание и редактирование `IteractionListEdit` через штатный screen ID `hunttech_IteractionList.edit`.
2. Убедиться, что кандидат и вакансия находятся в одной строке, имеют одинаковое оформление и не создают horizontal scroll.
3. Проверить lookup/open обоих picker-полей и checkbox подписанных вакансий.
4. Последовательно нажать пять пунктов левого индекса. Каждый пункт должен раскрывать ровно один блок, выделяться и переводить фокус в первое рабочее поле.
5. Раскрывать блоки штатными заголовками GroupBox и проверить синхронизацию индекса.
6. Убедиться, что отображаются ровно пять равных кнопок частых взаимодействий.
7. Сверить первые пять типов с агрегированной статистикой текущего пользователя за период один календарный год.
8. Нажать каждую заполненную кнопку и подтвердить прямую установку точного `Iteraction` в `iteractionTypeField` без разбора caption.
9. При недостатке статистики проверить disabled-позиции «Нет данных».
10. Проверить динамическое дополнительное поле, rating, recruiter, communication method, comment, subscription, save и cancel.
11. Повторить visual smoke в темах `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`.

Hermes не меняет функциональный код или документацию, не выполняет commit, push, rebase, merge, разрешение конфликтов и production-действия. Отчёт сохранить в `.ai/reports/` со статусом `READY_TO_MERGE` либо `FAILED_VERIFICATION`.
