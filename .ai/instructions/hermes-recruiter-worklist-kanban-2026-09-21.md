# Hermes: проверка рабочего списка и названия Kanban

**Проект:** HRM HuntTech
**Репозиторий:** `aananyev/it-pearls-recruiting`
**Ветка:** `agent/recruiter-worklist-kanban-label`
**Base:** `master`
**Статус PR:** `WAITING_FOR_HERMES`
**Режим:** только проверка, без изменения кода, документации, коммита, push, merge и production.

В PR body перед началом проверки указан полный SHA. Сверьте его с HEAD ветки и SHA PR; при любом несовпадении остановитесь с `HEAD_MISMATCH`. Убедитесь, что base PR — `master` и конфликтов нет.

## Что проверить

1. Запустить профильные тесты:
   ```bash
   ./gradlew :app-core:test --tests '*ScreenViewIntegrityTest*' --tests '*RecruiterDashboardsContractTest*' --no-daemon --stacktrace
   ```
   Ожидается `ScreenViewIntegrityTest 8/8 PASS`.
2. Запустить чистую сборку:
   ```bash
   ./gradlew clean assemble --no-daemon --stacktrace
   ```
   Проверить Data View Integrity и результат `BUILD SUCCESSFUL`.
3. Для полной проверки тем запустить:
   ```bash
   ./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
   ```
4. Проверить diff: стартовый период `hunttech_RotatingCandidate.browse` — «За месяц» (`daysBetween = 30`); фильтр «Только мои» и JPQL не изменены; навигация Dashboard Add-on и widget name показывают «Kanban».
5. Проверить согласованность UI-спецификаций и deploy-отчёта в `.ai/reports/recruiter-worklist-kanban/`.
6. Выполнить локальный deploy штатным `scripts/start-app.sh` из worktree с проверяемым SHA; production не трогать.
   ```bash
   bash scripts/start-app.sh --branch <путь-к-worktree>
   ```
7. В локальном HRM войти под `eliberman` с локальными учетными данными, переданными пользователем; открыть «HR-мастер → Кандидаты в работе». Убедиться, что стартовые фильтры — «За месяц», «Только мои», кейсы «Все», и что строки из 30-дневной выборки отображаются. Дополнительно проверить, что в меню «Подбор → Дашборды рекрутера» имя пункта — «Kanban».
8. Проверить `/hrm/` = HTTP 200, Tomcat logs и smoke. В отчёте этой задачи уже зафиксированы HTTP 200 и повторяющаяся нефатальная запись Log4j2 о недостающей реализации; подтвердите, что нет ошибок, вызванных этим SHA.

## Формат результата

Верните отчёт с `PROJECT: HRM HuntTech`, `STATUS`, Repo/Branch/PR/Base/Verified HEAD; отдельно укажите совпадение HEAD, конфликты, профильные тесты, Data View Integrity, SCSS, assemble, local deploy, HTTP, Tomcat logs, smoke, синхронность docs/history и P1/P2. Рекомендуемый финальный статус — только по фактическим результатам; без merge и production.
