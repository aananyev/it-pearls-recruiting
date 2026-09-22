# Hermes: проверка Yandex 360 Calendar пользователя alan

## TASK

Проверить точный HEAD ветки `agent/yandex-calendar-alan-diagnostics`, профильные тесты и read-only состояние production для пользователя `alan`.

## CURRENT_SHA / BASE_SHA / BRANCH

- `CURRENT_SHA`: взять после push и подтвердить полным SHA;
- `BASE_SHA`: `0c1885abe4e677a3895a3d7b833956873f1e6132`;
- `BRANCH`: `agent/yandex-calendar-alan-diagnostics`.

## ALLOWED_SCOPE

- checkout/fetch ветки;
- compile и тесты локально;
- read-only SQL и чтение журналов production;
- один live CalDAV `PROPFIND` для `alan`, выполняемый на production-сервере без вывода токена;
- отчёт в `.ai/reports/2026-09-22-yandex-calendar-alan-verification.md`.

## FORBIDDEN_SCOPE

- любые `INSERT/UPDATE/DELETE`, deploy, restart, migration и изменение production;
- создание, изменение или удаление реальных событий/календарей;
- печать, логирование, хеширование или копирование OAuth/refresh токенов;
- изменение Java/XML/SCSS/tests/docs; commit/push/merge/rebase.

## VERIFICATION_REQUIRED

1. Подтвердить отсутствие открытых PR, меняющих `YandexIntegrationServiceBean`.
2. Запустить:
   - `git diff --check origin/master...HEAD`;
   - `./gradlew :app-core:compileJava :app-core:compileTestJava --no-daemon --stacktrace`;
   - `./gradlew :app-core:test --tests 'com.company.hunttech.core.YandexCalendarConnectionDiagnosticsTest' --no-daemon --stacktrace`;
   - `./gradlew :app-core:test --tests 'com.company.hunttech.core.YandexIntegrationContractTest' --no-daemon --stacktrace`;
   - `./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace` (ожидается 8/8 PASS);
   - `./gradlew clean assemble --no-daemon --stacktrace`.
3. Production SQL только SELECT: для `alan` вывести исключительно boolean-признаки наличия account email, encrypted OAuth/refresh token, personal/client calendar path, `calendarBaseUrl`, `calendarConnected`, `lastVerifiedAt`, безопасный текст `lastVerificationMessage`. Значения токенов не выбирать.
4. Проверить активные корпоративные календари: название, active/default, base URL и boolean-признаки наличия path/account/token. Не выводить секреты.
5. Live read-only CalDAV: расшифровать токен только внутри процесса на production, не передавать его в argv и не печатать; выполнить `PROPFIND` principal/home-set и зафиксировать только HTTP status, количество доступных календарей и факт наличия требуемых прав. Любой временный файл — `0600`, cleanup через trap. Если безопасно выполнить нельзя, отметить `NOT RUN`, не ослаблять ограничения.
6. Проверить production-журналы с 2026-09-01 по `CalDAV|YandexIntegration|календар`, предварительно редактируя любые credential-like значения. Зафиксировать коды ошибок и время, без персональных данных/секретов.
7. CRUD production не выполнять. Опциональный CRUD допускается только на отдельном тестовом контуре/календаре: уникальный UID → PUT → GET → PUT update → GET → DELETE → GET=404, cleanup в `finally`.

## ACCEPTANCE_CRITERIA

- профильный тест: 6/6 PASS;
- `YandexIntegrationContractTest`: PASS;
- ScreenViewIntegrityTest: 8/8 PASS;
- build: PASS;
- production unchanged: YES;
- отчёт содержит `VERIFIED_HEAD`, фактический статус `alan`, HTTP/scopes/paths без секретов, ограничения и P1/P2/P3.
