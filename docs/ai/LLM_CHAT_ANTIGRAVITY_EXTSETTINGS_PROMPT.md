# Prompt для Antigravity: checkbox согласия fallback в ExtSettingsWindow

Доработай принадлежащий тебе `ExtSettingsWindow` для функции плавающего LLM-чата HRM HuntTech.

Добавь в вкладку персональной информации отдельный checkbox `adminFallbackConsent`, связанный с `UserAiProfile.adminFallbackConsent`.

Требования:

- checkbox отдельный от `externalProcessingAllowed`;
- для новых и существующих пользователей значение по умолчанию `false`;
- при включении сохранять дату и версию согласия: `ADMIN_FALLBACK_CONSENT_AT` и `ADMIN_FALLBACK_CONSENT_VERSION`;
- при снятии checkbox очищать дату и версию;
- reset/отзыв согласий должен выключать fallback, не изменяя `externalProcessingAllowed`;
- текст рядом должен объяснять, что при недоступности личного AI API запрос может перейти на административный API;
- телефоны, пароли и API-ключи в этот checkbox и его обработчики не добавлять;
- сохранить текущую структуру и стиль ExtSettingsWindow, не менять соседние вкладки;
- добавить/обновить UI contract test и проверить открытие экрана.

Backend уже проверяет это согласие серверно для `LLM_CHAT`; без `true` административный fallback запрещён.
