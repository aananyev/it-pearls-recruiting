# BL-2026-042 — повторное исправление AI-подбора из реестра вакансий

## Статус

`SUPERSEDED` (заменено актуальной реализацией BL-2026-042 от 2026-09-24: `.ai/tasks/2026-09-24-bl-2026-042-ai-candidate-matching-rework.md`)

## Git-контекст

- `BASE_SHA`: `a1f2cd2c0f2a6d0e9baa1195cf379304b69fdcb6`
- `BRANCH`: `agent/bl-2026-042-rework`
- Открытые PR не удалось проверить: GitHub API недоступен (`gh pr list` вернул network error).

## Заявленный дефект

Из sidebar `OpenPositionReestrBrowse` при запуске «Подобрать кандидата» появляется «Ошибка AI подбора. Не удалось выполнить анализ.» Подбор не работает, сообщение не объясняет причину и дальнейшее действие. Предыдущий результат BL-2026-042 не принят.

## Подтверждённая цепочка

`suggestCandidatesBtn` → `openCandidateVacancyMatchForSelected()` → `CandidateVacancyMatchScreen.setOpenPosition()` → vacancy-mode `startVacancyCandidateAnalysis()` → `CandidateVacancyMatchAiService.matchCandidatesForVacancy(openPositionId)` → AI `executeText()` → parser/fallback → UI report/error handling.

## Кодовый дефект, найденный архитектурным анализом

`CandidateVacancyMatchAiServiceBean.matchCandidatesForVacancy()` сперва ищет кандидатов с резюме, содержащим распознанный текст. Когда таких кандидатов нет, метод намеренно загружает до 25 незаблокированных кандидатов без обязательного резюме. Для каждого затем повторно загружает CV с распознанным текстом и без проверки пустого списка вызывает `buildCandidateResumeText(cvList)`. Этот helper безусловно выполняет `cvList.get(0)`. При пустом `cvList` выбрасывается `IndexOutOfBoundsException` до вызова `aiExecutionService.executeText()`. Верхний обработчик фоновой задачи показывает общий текст, соответствующий сообщению пользователя.

Это точная подтверждённая дефектная ветка кода и сильная гипотеза причины симптома. Лог именно неудачного пользовательского запуска в доступных локальных файлах не найден; наличие данной ветки среди кандидатов конкретного runtime не подтверждено логом. Не считать гипотезу runtime-причиной без повторного воспроизведения/корреляции.

## Архитектурная оценка

- Vacancy-to-candidates — последовательный вызов до 25 AI запросов, фоновая задача UI ограничена 240 секундами; per-candidate прогресс отсутствует.
- Некоторые ошибки до и после AI-вызова находятся вне локального `try/catch` и прекращают весь batch; AI-exception проглатывается и может привести к rule-based fallback.
- Логи в отдельных ветках содержат имя кандидата и/или только `e.getMessage()`. Не допускать логирования резюме, prompt, credentials и необязательных PII.
- Ранний отчёт PR #263 документирует ошибки локальных corporate credentials, но относится к старому runtime/SHA и не является текущим подтверждением.

## Решение/область исправления

- Сохранить существующую бизнес-логику пула: fallback на незаблокированных кандидатов без CV указывает на ожидаемый profile/skills-only путь; передавать пустой/явно маркированный контекст CV, когда распознанного текста нет.
- Добавить тест, доказывающий, что пустой CV список больше не обрывает обработку до AI-вызова.
- Ошибки подготовки кандидата, вызова AI, парсинга, fallback и связанного workflow должны быть безопасно диагностированы с operation/request ID, vacancy ID, candidate ID/номер, фазой и категорией ошибки; исключения одного кандидата не должны без объяснения скрыто прерывать остальных, если это допустимо архитектурой.
- Пользователь должен отличать пустой результат от сбоя AI. При необработанной операции видеть понятное безопасное сообщение с кодом обращения; подробная причина остаётся в серверном логе.
- Не менять кандидатский поиск, специализации и ранжирование вне необходимого поведения для кандидата без CV.

## Роли

- System analyst report obtained: taxonomy and expected behavior for no data/config, auth/quota/provider, timeout, malformed response, internal exception; correlation requirements.
- Application architect report obtained: source-level defect above; exact runtime attempt still needs log confirmation.
- Independent QA required after implementation.
- PCR review required before PR is ready.

## Acceptance criteria

1. No-CV fallback candidate reaches AI executor with defined profile/skills context instead of throwing before dispatch.
2. One candidate preparation error cannot silently abort entire vacancy analysis; completion/partial failure is explicit and traced.
3. Success, zero candidates, no readable CV, provider/configuration failure, timeout, malformed output and unexpected failure have distinct safe outcomes/messages.
4. Each attempted run can be correlated to backend diagnostic logs without logging credentials, resume text, full prompt/response or unnecessary PII.
5. UI recovers progress and actions on every terminal path and allows a safe retry.
6. Regression tests cover the exact empty-CV case and error paths; required CUBA screen integrity and relevant service tests pass.
7. PCR feedback is resolved; PR is opened against master. No deploy/merge.

## Verification gap

Available repo logs cover startup on 2026-09-22, not the user's reported click attempt. Exact runtime trace requires a fresh controlled repro or user-provided timestamp/log event; keep source-level confirmed facts distinct from that missing evidence.

An available local `app.log` line from `hrm-skills-enrichment-worker` says `hunttech.ai.encryptionKey` is not configured during background skill analysis. No `CandidateVacancyMatch` caller/request ID accompanies it. Treat this only as a separate local runtime configuration issue, not evidence for the vacancy-sidebar failure.

## Реализация и проверка (2026-09-23)

### Изменения

- `CandidateVacancyMatchAiServiceBean`: пустой список читаемых CV теперь формирует явный контекст о том, что AI должен использовать только профиль и сохранённые навыки, без неподтверждённых выводов. Существующий резервный пул незаблокированных кандидатов сохранён.
- Запуск и вызовы кандидатов коррелируются через operation/request ID. Безопасная диагностика содержит UUID вакансии/кандидата, фазу и тип ошибки; имена, тексты резюме, prompts/responses, токены, credentials и `exception.getMessage()` не журналируются.
- Ошибка подготовки/обработки отдельного кандидата учитывается и не прерывает оставшийся batch. Итог отмечает предварительную оценку и частичный сбой, когда они возникли; верхнеуровневая ошибка возвращает безопасный код обращения.
- `CandidateVacancyMatchScreen`: статус fallback/частичного результата отображается из отчёта и не перезаписывается общим текстом. Операционная ошибка с кодом обращения показывается как ERROR; пустые/валидационные исходы без кода остаются WARNING. Завершение фоновой задачи сбрасывает busy state, снова активируя кнопку повтора.
- `docs/ui/CandidateVacancyMatchScreen_Spec.md` описывает вход из реестра вакансий, кандидатов без читаемого CV, диагностику и поведение при fallback/ошибках.

### Тесты

- `./gradlew :app-core:test --tests 'com.company.hunttech.service.CandidateVacancyMatchNoResumeTest' --no-daemon --stacktrace` — PASS, 2/2. Сервисный interaction test подтверждает, что vacancy-mode с пустым readable-CV pool выбирает разрешённого fallback кандидата и вызывает `AiExecutionService.executeText` с no-resume marker.
- `./gradlew :app-core:test --tests '*CandidateVacancyMatch*Test' --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace` — 26 тестов AI-подбора прошли; `ScreenViewIntegrityTest` не смог инициализировать общий CUBA test container, потому что в окружении отсутствует `HUNTTECH_LOCAL_DB_PASSWORD`. Это env prerequisite, не failure assertions кода.
- `./gradlew :app-web:test --tests '*AllXmlScreensIntegrityTest' --no-daemon --stacktrace` — PASS; web production/test code скомпилирован, XML integrity test прошёл.
- `git diff --check` — PASS.

### Оставшиеся подтверждения

Exact trace исходного пользовательского запуска по-прежнему недоступен; причина подтверждена как дефект исходной no-CV ветки, а interaction regression test подтверждает её исправление на уровне сервиса. Локальный `app.log` с `hunttech.ai.encryptionKey` относится к `hrm-skills-enrichment-worker`, без CandidateVacancyMatch request/caller ID; это отдельная конфигурационная запись и не используется как доказательство причины этой ошибки.

Нет запуска приложения, деплоя или изменения данных. PR/PCR остаются следующими обязательными действиями.

### Проверка после PCR-замечания

- `./gradlew :app-core:test --tests '*CandidateVacancyMatch*Test' --no-daemon --stacktrace` — BUILD SUCCESSFUL, 26 тестов; включая interaction regression для пустого CV и сквозной operation ID в request ID.
- `./gradlew :app-web:test --tests '*AllXmlScreensIntegrityTest' --no-daemon --stacktrace` — BUILD SUCCESSFUL, 1/1; web-код экрана скомпилирован.
- Повторная комбинированная проверка с `ScreenViewIntegrityTest` показывает прежнее ограничение: класс инициализации test container требует `HUNTTECH_LOCAL_DB_PASSWORD`, которого нет в этом окружении. Остальные 26 тестов в команде прошли.
- PCR сначала отметил P2: незащищённый task exception handler vacancy-mode логировал throwable без correlation ID. Исправлено: web создаёт operation ID, передаёт его в service overload; timeout и uncaught task exception логируются только по operation ID и классу исключения; тот же ID показывается пользователю как код обращения.
- PCR повторно проверил исправленный участок и подтвердил, что P2 закрыт. Остаточных замечаний в рамках повторной проверки нет. Старый raw-throwable лог candidate-to-vacancies не входит в scope BL-2026-042.
- Последний `git diff --check` — PASS.

**Текущая готовность:** код, профильные тесты и PCR готовы к PR; интеграционный `ScreenViewIntegrityTest` остаётся непроверенным из-за отсутствия обязательной переменной БД. Exact production/runtime click trace не получен. Приложение не запускалось и не развёртывалось. PR ещё не открыт.

### Итог фиксации на 2026-09-23

- Итоговый кодовый commit находится в `agent/bl-2026-042-rework`. Commit hook автоматически поднимает patch version в `build.gradle`; проверки повторно запускаются после последнего изменения commit.
- Последняя проверка: `:app-core:test --tests '*CandidateVacancyMatch*Test'` — BUILD SUCCESSFUL (26 tests); `:app-web:test --tests '*AllXmlScreensIntegrityTest'` — BUILD SUCCESSFUL (1/1); `git diff --check` — PASS.
- `ScreenViewIntegrityTest` не может пройти инициализацию в этом окружении без `HUNTTECH_LOCAL_DB_PASSWORD`; отдельная попытка дала `IllegalStateException` именно на отсутствующей переменной.
- PCR: initial P2 закрыт; повторная независимая проверка подтвердила сквозной operation ID и безопасный vacancy-mode exception/timeout logging. P2 считается разрешённым. Вне scope остаётся похожий старый raw-throwable handler candidate-to-vacancies.
- PR создать и ветку отправить не удалось: `gh auth status` сообщает invalid token для активного `aananyev`; `git credential fill` не нашёл GitHub credential; SSH fallback завершился `Could not resolve hostname github.com` (DNS/network недоступен). PR не создан; commit SHA передаётся в handoff-отчёте.
- Деплой не выполнялся; приложение не запускалось.
