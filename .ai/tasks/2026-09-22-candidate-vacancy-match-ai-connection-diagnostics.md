# Диагностика и восстановление AI-подбора вакансий

## Статус

`COMPLETE` — 2026-09-22

## Git-контекст

- Проект: HRM HuntTech
- Base SHA: `10567c422`
- Рабочая ветка: `agent/candidate-vacancy-match-ai-diagnostics`
- PR: #263, merge commit `350f2647a69f5a202b68030d27e2caf6d12309a7`
- Final feature SHA: `6fea97bdff9da97604cf3a4c5b87c35a7c647aa7`

## Цель

Воспроизвести ошибку подключения к AI-функции в диалоге «Подобрать вакансию», отличить потерю конфигурации при merge от проблем credential/runtime, добавить безопасную трассировку маршрута и восстановить переход к рабочей корпоративной модели.

## Подтверждённые факты на старте

- В локальной БД зарегистрирована миграция `260921-1-routeCandidateVacancyMatchToDeepSeek.sql`.
- `CANDIDATE_VACANCY_MATCH_ANALYZE` активно привязана к корпоративному DeepSeek `deepseek-v4-flash`.
- Локальный журнал вызовов содержит ошибки подбора, но без трассировки каждого кандидата маршрута.
- Код `AiExecutionServiceBean` расшифровывает admin credential до per-candidate `try/catch` в text/streaming/image путях.
- Исторический local handoff фиксирует невалидный личный DeepSeek key и нерасшифровываемый корпоративный credential.
- После удаления личного ключа локальный runtime продолжил корпоративный маршрут: B.AI отвечал HTTP 403/429, а часть корпоративных credentials не расшифровывалась. Это подтверждает, что merge не потерял привязку функции; проблема была в usable corporate credentials и в отсутствии изоляции ошибки decrypt.

## Область работ

- middleware AI resolver/execution;
- безопасные route logs и корреляция через `requestId`/operation id;
- regression tests;
- локальная сборка, миграция, deploy, restart и smoke через DevOps/Hermes;
- синхронизация `docs/` и `.ai/reports/`.

## Ограничения

- Не писать в production и не менять production credentials.
- Не логировать API keys, ciphertext, Authorization headers, CV/resume text или raw provider payloads.
- Не менять frontend/XML без подтверждённой необходимости.
- Merge PR выполнять только после успешной проверки и в рамках прямого запроса владельца.

## Критерии приёмки

1. При OpenRouter/первом credential error следующий рабочий DeepSeek действительно вызывается.
2. Ошибка расшифровки одной admin-конфигурации не обрывает общий fallback; для неё видны config UUID, provider, model и безопасная категория ошибки.
3. Ошибка невалидного личного ключа корректно переходит к admin fallback согласно политике функции.
4. В логах есть correlation id, function code, маршрут, номер кандидата, попытка, результат и финальная причина без секретов.
5. Для text, streaming и image путей ошибка расшифровки одного admin credential пропускает конфигурацию и передаёт управление следующему кандидату.
6. Форма завершается успехом, честным fallback или понятной повторяемой ошибкой, а не зависает в ожидании.
7. Core tests, ScreenViewIntegrityTest, SCSS build, local deploy/restart и HTTP/UI smoke пройдены на одном SHA.

## Проверка

- System analyst: требования, telemetry contract и acceptance scenarios.
- Java backend developer: исправление resolver и tests.
- QA: независимая проверка негативных и fallback-сценариев.
- DevOps/Hermes: exact SHA, migration status, local deploy/restart, logs and smoke.

## Финальная проверка

- `app-core-0.692-SNAPSHOT.jar`, локальный updateDb/deploy/restart — PASS.
- `/hrm/` HTTP 200, widgetset ready и endpoint — PASS.
- Core/contract/migration/screen tests: `24/24`, `11/11`, `2/2`, `8/8` — PASS.
- AI provider credentials остаются операционным блокером: DeepSeek/OpenRouter не расшифровываются, B.AI отвечает 403/429. Код fallback при этом продолжает маршрут и фиксирует причину в безопасных логах.
