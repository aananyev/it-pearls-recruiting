# Диагностика подключения AI для «Подобрать вакансию»

## Purpose and Business Meaning (What & Why)

Диалог подбора вакансий должен использовать корпоративный AI после удаления личных ключей кандидата и завершать операцию понятным результатом. Сбой одной корпоративной конфигурации не должен останавливать резервный маршрут или оставлять форму в неопределённом состоянии.

## UI Context & Navigation

Проверяется экран «Кандидаты → Подобрать вакансию». Локальный runtime: `http://localhost:8080/hrm/`. Диагностика относится к middleware-маршруту функции `CANDIDATE_VACANCY_MATCH_ANALYZE` и к обновлению навыков перед подбором.

## Проверенный контекст

- Кодовая база перед исправлением: `origin/master` `10567c422`.
- Локальный runtime в журнале: `app-core-0.690-SNAPSHOT.jar`.
- Changeset `260921-1-routeCandidateVacancyMatchToDeepSeek.sql` зарегистрирован локально.
- Активная функция `CANDIDATE_VACANCY_MATCH_ANALYZE` в локальной БД привязана к корпоративному DeepSeek `deepseek-v4-flash`; активного пользовательского override этой функции не найдено.

## Наблюдения после удаления личного ключа

В журнале локального runtime корпоративный маршрут действительно запускался. B.AI `glm-5.2` возвращал HTTP 403 с требованием пополнить депозит и HTTP 429. Параллельно часть корпоративных credentials завершалась ошибкой расшифровки. Поэтому сообщение «AI подключение недоступно» было честным результатом: рабочий корпоративный ключ не был доказан, а fallback мог прерываться на credential preparation.

До удаления личного ключа наблюдался отдельный HTTP 401 DeepSeek для личного подключения. Это больше не является основанием считать, что корпоративная привязка потеряна.

## Корневая причина

1. В `AiExecutionServiceBean` decrypt корпоративного API key выполнялся до `try/catch` внутри перебора admin-кандидатов. Нерасшифровываемый OpenRouter/DeepSeek credential прекращал перебор и маскировал следующую модель.
2. На локальной конфигурации после перехода к корпоративному маршруту B.AI недоступен из-за provider-side 403/429. Это требует пополнения/замены модели или отключения нерабочей конфигурации.
3. Если ciphertext создан с другим `hunttech.ai.encryptionKey`, непустое поле в БД не означает пригодный credential. Требуется повторно сохранить действующий корпоративный ключ через текущий runtime.

Миграция и привязка функции присутствуют; потеря при merge не подтверждена.

## Исправление

`AiExecutionServiceBean` изолирует подготовку credential для каждого admin-кандидата в text, streaming и image путях. При decrypt failure кандидат пропускается, в журнал попадает только техническая категория, а следующий кандидат продолжает обработку.

Route logs используют безопасные поля: `functionCode`, `requestId`, provider, model, configuration UUID, `candidateIndex/total`, `attempt/max`, `stage`, `category`, `action`. Секреты, ciphertext, authorization headers, CV и provider payload не логируются.

Экран перед подбором использует correlation operation id в контексте middleware, поэтому локальный маршрут можно сопоставить с конкретным запуском.

## Acceptance / verification

- `undecryptable corporate OpenRouter → valid DeepSeek` для text продолжает работу через DeepSeek.
- Такой же fallback проверен для streaming; image покрывается тем же per-candidate контрактом.
- Необходимы `ScreenViewIntegrityTest`, core AI tests, SCSS build и локальный deploy/restart одного SHA.

## Remediation options

1. Повторно сохранить рабочий корпоративный DeepSeek key через экран управления AI на текущем runtime, чтобы ciphertext был создан текущим master key.
2. Проверить баланс/доступность B.AI `glm-5.2`; отключить конфигурацию, если она не должна участвовать в fallback.
3. Проверить OpenRouter gateway `127.0.0.1:8119` и его ключ, либо отключить его из admin pool до восстановления.
4. После redeploy использовать `requestId` из `AI_ROUTE_*` для проверки полного маршрута и финального `AiCallLog`.

## History

| Date | Change |
|---|---|
| 2026-09-22 | Зафиксированы результаты диагностики после удаления личных ключей, причины provider/decrypt failures и контракт безопасного fallback. |
