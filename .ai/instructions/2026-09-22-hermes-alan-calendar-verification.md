# Hermes verification: календарное подключение alan

TASK: проверить исправление `CAL-2026-001/002` и новые календарные автотесты
BASE_SHA: `0c1885abe4e677a3895a3d7b833956873f1e6132`
BRANCH: `agent/alan-calendar-connection-tests`
GOAL: подтвердить строгую диагностику CalDAV и изолированный mock CRUD
ALLOWED_SCOPE: сборка, тесты, read-only анализ логов, отчёт
FORBIDDEN_SCOPE: изменение кода/docs, реальные события `alan`, вывод секретов, TEST/PRODUCTION deploy, merge

После получения итогового SHA:

1. Зафиксировать `git rev-parse HEAD` и отсутствие незакоммиченных изменений.
2. Запустить через `scripts/agent-gradle.sh` последовательно:
   - `:app-core:compileJava :app-core:compileTestJava --no-daemon --stacktrace`;
   - `:app-core:test --tests 'com.company.hunttech.core.YandexCalendarConnectionTest' --no-daemon --stacktrace` — ожидается 5/5 PASS;
   - `:app-core:test --tests 'com.company.hunttech.core.YandexIntegrationContractTest' --tests 'com.company.hunttech.core.CorporateYandexCalendarContractTest' --no-daemon --stacktrace`;
   - `test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace` — ожидается 8/8 PASS;
   - `clean assemble --no-daemon --stacktrace`.
3. Проверить, что тесты не обращались к `caldav.yandex.ru`, TEST или Production и не вывели токены.
4. Выполнить только read-only SQL из диагностического отчёта на TEST, если datasource доступен; выводить лишь `has_*` признаки, статусы и даты.
5. Сохранить отчёт в `.ai/reports/2026-09-22-alan-calendar-connection-verification.md` с точным SHA, числом тестов, PASS/FAIL, stack trace при ошибке и перечнем непроверенных runtime-сценариев.

PASS: все Gradle-задачи успешны, 5/5 календарных тестов и 8/8 integrity-тестов прошли, секреты отсутствуют, внешних календарных записей не было. Иначе `FAILED_VERIFICATION`.
