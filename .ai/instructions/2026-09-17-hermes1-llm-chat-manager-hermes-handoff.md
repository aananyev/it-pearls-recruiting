# HERMES-1 — довести LLM-chat manager/write Hermes до merge и локального deploy

Дата: 2026-09-17
Репозиторий: `aananyev/it-pearls-recruiting`
Исходная ветка ChatGPT: `agent/llm-chat-manager-hermes-access`

## Что уже сделано ChatGPT
- добавлен UI-каркас третьей вкладки `hermesManagerChatTab`;
- добавлены specific permissions `hunttech.ai.useLocalChat` и `hunttech.ai.useManagerHermesWrite`;
- добавлены сообщения/предупреждения;
- write controls намеренно disabled до завершения безопасного backend binding;
- Antigravity получил отдельный implementation prompt `.ai/prompts/2026-09-17-antigravity-llm-chat-hermes-role-access.md`.

## Твоя роль
Следуй своему профилю `.ai/instructions/hermes1-profile.md`: master/CI/CD/merge/deploy. Продуктовый код за Antigravity не переписывай.

## До merge
1. Дождись PR Antigravity, который включает изменения ChatGPT и завершает реализацию.
2. Проверь, что Antigravity реально установил точные CUBA роли менеджеров/директоров и не хардкодил captions ролей как security boundary.
3. Проверь результат read-only reconnaissance production Docker:
   - `hrm-viewer` подтверждён;
   - второе фактическое имя Hermes container зафиксировано;
   - container/API/port/auth описаны без секретов.
4. Критический security gate: write-Hermes НЕ должен выполнять прямой произвольный SQL от имени общего DB-пользователя. CREATE/UPDATE должны проходить через CUBA server-side permission checks текущего пользователя.
5. DELETE должен быть запрещён безусловно. Не должно быть delete-tool, DELETE SQL, TRUNCATE, DDL или универсального raw SQL gateway.
6. Проверь deny-by-default для неизвестных entities/fields/operations.
7. Проверь профильные тесты, `ScreenViewIntegrityTest` и сборку.

## Merge и локальный deploy
После успешной проверки:
```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
git pull --ff-only origin master
# merge PR штатным способом согласно профилю Hermes-1
git pull --ff-only origin master
bash scripts/start-app.sh
curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/hrm/
```
Ожидаемый HTTP status: `200`.

## Smoke test локально
Проверить минимум три профиля пользователей:
1. обычный локальный пользователь — не видит `Локальный чат` и `Hermes — управление HRM`;
2. пользователь с ролью менеджера — видит обе вкладки;
3. пользователь с ролью директора — видит обе вкладки.

Для manager/write Hermes:
- запрос чтения работает;
- CREATE вакансии разрешается только при соответствующем CUBA EntityOp.CREATE;
- UPDATE разрешается только при CUBA EntityOp.UPDATE + attribute permission;
- попытка DELETE отклоняется;
- попытка менять недоступное поле отклоняется;
- ссылка на созданную/изменённую сущность открывается только если пользователь имеет READ.

## Финальный отчёт
Создай `.ai/reports/2026-09-17-PR<номер>-llm-chat-manager-hermes-deploy.md` с:
- merge SHA;
- названием второго Hermes production container;
- краткой схемой security delegation;
- результатами тестов;
- HTTP 200 после local deploy;
- подтверждением DELETE-deny;
- подтверждением, что production не изменялся без отдельного согласования.
