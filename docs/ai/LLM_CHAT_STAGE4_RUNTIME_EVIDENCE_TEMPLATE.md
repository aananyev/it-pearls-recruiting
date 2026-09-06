# Evidence-шаблон этапа 4: безопасность и данные LLM-чата

Статус: подготовлен для authenticated staging. Не является распоряжением на production.

## Обязательные реквизиты прогона

- Дата и часовой пояс:
- Commit и версия приложения:
- Staging URL:
- Reverse proxy/балансировщик и тайм-ауты:
- Ответственный за прогон:
- Идентификаторы двух тестовых пользователей:
- Идентификатор тестовой административной роли:
- Идентификатор sandbox/mock provider:
- Идентификатор backup перед rehearsal:

В этот документ не записываются пароли, API-ключи, токены, plaintext-секреты или полные тексты персональных данных.

## Синтетический тестовый набор

Использовать только специально созданные записи:

1. Пользователь A: имя `Stage4 User A`, синтетическая профессия и предпочтения, отдельная персональная AI-конфигурация.
2. Пользователь B: имя `Stage4 User B`, другой синтетический профиль без доступа к данным пользователя A.
3. Администратор: отдельная тестовая роль с `hunttech.ai.viewChatHistoryAdmin`, `hunttech.ai.reconcileChatQuota` и `hunttech.ai.manageCorporateCredentials`.
4. Legacy fixture: синтетические записи с plaintext `API_KEY` только в изолированной staging-копии; значение не выводить и не включать в evidence.
5. Rotation fixture: ciphertext, созданный предыдущим тестовым server-side key; значения текущего и предыдущего ключа в evidence не фиксировать.
6. Privacy fixture: synthetic prompt с маркерами `TEST_SECRET`, `TEST_BEARER` и `TEST_CANDIDATE_DATA`; это не реальные данные кандидата.

## Матрица runtime-проверок

### Encryption и rotation

- [ ] Новая персональная конфигурация сохраняется только в ciphertext.
- [ ] Legacy migration переносит контролируемое число записей, очищает plaintext и не создаёт дубли.
- [ ] Повторный запуск migration возвращает нулевое число новых переносов.
- [ ] `rotateSecrets()` пере-шифровывает fixture предыдущим ключом и сохраняет доступ к AI.
- [ ] Повторный запуск rotation не изменяет уже актуальные ciphertext.
- [ ] После проверки previous key удалён из runtime-конфигурации.
- Результат/counts/checksums без раскрытия секретов:

### Secret leakage

- [ ] API-ключи, Bearer/Basic/Api-Key значения отсутствуют в UI.
- [ ] Секреты отсутствуют в server log, error response и `AiCallLog`.
- [ ] В диагностическом сообщении secret-like значения заменены `[REDACTED]`.
- [ ] Privacy fixture не раскрывается через ошибку, push-событие или audit.
- Ссылки на log/error evidence:

### Consent и privacy version

- [ ] Новый пользователь видит отдельное согласие admin fallback, независимое от `externalProcessingAllowed`.
- [ ] Fallback без `true + version + timestamp` отклоняется до provider call.
- [ ] Fallback с актуальным consent выполняется только после ошибки личного API.
- [ ] Согласие и privacy policy version фиксируются для операции/диалога по утверждённому контракту.
- Проверенная версия consent/privacy:

### Owner isolation и permissions

- [ ] Пользователь A не читает диалог, сообщения, streaming snapshot или push-событие пользователя B.
- [ ] Пользователь B не читает данные пользователя A.
- [ ] Административная история доступна только роли с отдельным permission.
- [ ] Пользователь не может удалить бессрочную историю.
- Идентификаторы проверенных conversation/request без содержимого:

### Retention и восстановление

- [ ] История сохраняется бессрочно и не удаляется пользовательским действием.
- [ ] In-memory streaming snapshot удаляется после завершения согласно runtime policy.
- [ ] После reconnect/polling нет повторного provider call и дублирования assistant message.
- [ ] Незавершённый запрос после перезапуска остаётся в ожидаемом reservation/status и не запускается повторно.
- Результаты и timestamps:

### Квота, fallback и unknown outcome

- [ ] Общая месячная квота применяется по календарному месяцу.
- [ ] Индивидуальный override с `effectiveTo` и причиной имеет приоритет.
- [ ] Personal API не расходует admin quota при успешном вызове.
- [ ] Fallback без согласия запрещён.
- [ ] Timeout/cancel списывают только подтверждённый usage; неизвестный исход остаётся `UNKNOWN_PENDING`.
- [ ] Ручная reconciliation не вызывает provider повторно и оставляет audit trail.
- Counts/reservation/request IDs без текстов prompt и секретов:

## Итог gate

- Статус: `PASS` / `REWORK` / `BLOCKED`
- Невыполненные пункты:
- Дефекты с приоритетом:
- Ссылки на логи, скриншоты и отчёты:
- Решение о переходе к этапу 5: `разрешён` / `запрещён`

Пока не заполнены authenticated staging evidence по всем разделам и не устранены блокеры, этап 4 остаётся `REWORK`, а этап 5 не запускается.
