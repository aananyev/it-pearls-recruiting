# Диагностика календарей пользователя `alan`

**Дата:** 2026-09-22
**BASE_SHA:** `0c1885abe4e677a3895a3d7b833956873f1e6132`
**Режим:** read-only для Production; без деплоя и изменения реальных событий

## 1. Статус подключения

Фактический runtime-статус OAuth/CalDAV пользователя `alan` из текущего окружения **не подтверждён**: SSH/DNS-доступ к Production отсутствует, TEST `192.168.1.135` недоступен из внешней сети. Реальные токены, БД и пользовательские события не читались и не изменялись.

Статически подтверждено, что проект предусматривает:

| Интеграция | Источник | Состояние по коду |
|---|---|---|
| Персональный CalDAV | `UserYandexConfiguration` | Поддерживается; путь и OAuth-токен задаются для пользователя |
| «Hunttech у заказчика» | seed `260914-3` | Предусмотрен активный корпоративный календарь по умолчанию для `alan@hunttech.ru` |
| «Мои события (alan@hunttech.ru)» | seed `260914-3` | Предусмотрен второй активный корпоративный календарь |
| OAuth Яндекс 360 | `oauthTokenEncrypted`, `AiSecretService` | Поддерживается в зашифрованном виде; фактическая валидность токена не подтверждена |
| Refresh token | `refreshTokenEncrypted` | Хранится, но автоматическое обновление access token в календарном сервисе не обнаружено |
| IAM | календарный сервис | Отдельная IAM-аутентификация не реализована |
| Синхронизация | `IteractionList.calendarSyncState` | `SYNCED`/`FAILED`; фактическое состояние событий `alan` не проверялось |

## 2. Проверенные источники и компоненты

- GitHub `master`, SHA `0c1885abe4e677a3895a3d7b833956873f1e6132`; открытые PR через публичный API не обнаружены.
- `AGENTS.md`, `.cursorrules`, `.ai/tasks/`, `.ai/reports/` и обязательные инструкции Git-протокола.
- `YandexIntegrationService` / `YandexIntegrationServiceBean`.
- `UserYandexConfiguration`, `CorporateYandexCalendar`, `IteractionList`.
- `ExtSettingsWindow`, `ExtUserEdit`, `CorporateYandexCalendarBrowse/Edit`, `IteractionListEdit`.
- Liquibase `260913-3`, `260914-2`, `260914-3` и production migration plan.
- Документация `YANDEX_CALENDAR_INTERACTION_INTEGRATION.md` и `YANDEX_360_CALENDAR_LLM_ORCHESTRATION.md`.
- Существующие `YandexIntegrationContractTest` и `CorporateYandexCalendarContractTest`.

## 3. Воспроизводимость read-only проверки

На Production допускаются только запросы, возвращающие признаки без значений секретов:

```sql
SELECT u.login,
       c.account_email,
       (c.oauth_token_encrypted IS NOT NULL AND c.oauth_token_encrypted <> '') AS has_oauth_token,
       (c.refresh_token_encrypted IS NOT NULL AND c.refresh_token_encrypted <> '') AS has_refresh_token,
       c.calendar_connected,
       c.last_verified_at,
       c.last_verification_message
FROM sec_user u
LEFT JOIN hunttech_user_yandex_config c ON c.user_id = u.id AND c.delete_ts IS NULL
WHERE lower(u.login) = 'alan' AND u.delete_ts IS NULL;

SELECT name, account_email, calendar_path, is_default, active,
       (oauth_token_encrypted IS NOT NULL AND oauth_token_encrypted <> '') AS has_oauth_token
FROM hunttech_corp_yandex_cal
WHERE delete_ts IS NULL
ORDER BY is_default DESC, name;
```

Запрещено выводить поля токенов целиком или выполнять CalDAV `PUT`/`DELETE` на Production.

## 4. Автоматизированные тесты

Добавлен `YandexCalendarConnectionTest`:

1. `calendarDiagnosticsRejectsMissingToken`;
2. `calendarDiagnosticsRejectsUnauthorizedCaldavResponse`;
3. `calendarDiagnosticsRejectsNetworkFailure`;
4. `calendarDiagnosticsAcceptsSuccessfulCaldavDiscovery`;
5. `mockCaldavSupportsCreateReadUpdateDeleteAndCleanup`.

Тесты используют только фиктивный домен `.invalid`, placeholder-токен и in-memory события. Сборка и Gradle-прогон переданы Hermes согласно правилам проекта; до отчёта по точному SHA результат отмечается как **НЕ ВЕРИФИЦИРОВАН**.

## 5. Дефекты и приоритеты

| ID | Приоритет | Дефект | Исправление |
|---|---:|---|---|
| CAL-2026-001 | P1 | Диагностика считала fallback-календарь доказательством успешного подключения после 401/403/сети | Для диагностики введён строгий режим без fallback; статус подключения сбрасывается при ошибке |
| CAL-2026-002 | P1 | Discovery искал только `<resourcetype>` и не распознавал обычный namespace-префикс `<d:resourcetype>` | Добавлен namespace-aware шаблон |
| CAL-2026-003 | P2 | `refreshTokenEncrypted` хранится, но автоматическое обновление OAuth access token не обнаружено | Спроектировать отдельный refresh flow с тестами истечения/отзыва токена |
| CAL-2026-004 | P2 | Runtime-конфигурация `alan` не контролируется автоматическим безопасным health-check | Добавить TEST smoke с отдельным календарём и редактированием только событий с префиксом автотеста |

## 6. Рекомендованный TEST smoke

Создать отдельный календарь `HRM HuntTech Calendar Integration Test`. Для каждого запуска использовать UID `hrm-calendar-test-<uuid>`, выполнить `PUT → GET/REPORT → PUT → GET/REPORT → DELETE`, а удаление повторить в `finally`. После теста отдельным `REPORT` подтвердить отсутствие UID. При любой ошибке cleanup должен выполняться повторно; Production в этом сценарии не используется.
