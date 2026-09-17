# LlmChatScreen — разграничение AI-контуров

> Проект: **HRM HuntTech**  
> Screen ID: `hunttech_LlmChatScreen`  
> Controller: `modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java`  
> Descriptor: `modules/web/src/com/company/hunttech/web/screens/llmchat/llm-chat-screen.xml`  
> Платформа: CUBA Platform 7.3-SNAPSHOT

## 1. Назначение и бизнес-смысл (What & Why)

LLM-chat объединяет локальный AI-контур и Hermes. Локальный чат и будущий Hermes-контур, способный инициировать изменение HRM-данных, относятся к повышенным полномочиям. Поэтому они не должны автоматически становиться доступны любому пользователю, которому доступен сам экран.

Текущий этап вводит fail-closed контракт: привилегированные вкладки скрыты по умолчанию и регистрируются отдельные CUBA specific permissions. Точные production-роли категории «Менеджер/директор» и второй Hermes-контейнер должны быть определены Antigravity по фактической конфигурации, после чего controller/backend включает возможности только через CUBA security model.

## 2. UI Context & Navigation

Экран открывается из раздела «Управление AI» и через плавающий launcher.

```text
LLM-chat
├── localChatTab — «Локальный чат»
│   └── fail-closed: visible=false, целевое permission hunttech.ai.useLocalChat
├── hermesChatTab — «Hermes»
│   └── текущий общий/read-only контур (профиль hrm-viewer)
└── hermesManagerChatTab — «Hermes — управление HRM»
    └── открыта для коммуникации с Hermes (профиль hrm-operator в docker)
        └── безопасный server-side CUBA mutation gateway (запрет raw SQL/DELETE)
```

Размеры и общая UX-компоновка диалога не меняются.

## 3. Behavior Summary

| Действие | Условие | Результат |
|---|---|---|
| Открытие LLM-chat | обычный пользователь | вкладки Hermes (hrm-viewer) и Hermes — управление HRM (hrm-operator) доступны для коммуникации |
| Ввод в 3-й вкладке | пользователь вводит текст | поле ввода активно, сообщения отправляются в hermes-hrm-operator |
| CREATE/INSERT-смысл | Hermes формирует намерения MUTATION | операция выполняется через server-side CUBA mutation path при наличии прав |
| UPDATE | Hermes формирует намерения MUTATION | операция выполняется через server-side CUBA mutation path при наличии прав |
| DELETE | любое состояние | операция запрещается безусловно |

## 4. Security contract

В `web-permissions.xml` зарегистрированы:

- `hunttech.ai.useLocalChat`;
- `hunttech.ai.useManagerHermesWrite`.

Названия ролей не хардкодятся в Java/XML. Привязка выполняется по реальным CUBA roles после их проверки в базе настроек пользователей и ролей.

UI visibility не является достаточной security boundary. Финальный manager-Hermes backend обязан повторно проверять:

1. `hunttech.ai.useManagerHermesWrite`;
2. `Security.isEntityOpPermitted(..., EntityOp.CREATE/UPDATE)`;
3. permission на каждый изменяемый атрибут;
4. существующие domain validations/services.

## 5. Manager Hermes и работа с БД

На момент текущего коммита в Git отсутствуют подтверждённые сведения о втором production Hermes-контейнере: имя, endpoint, API и authentication. Поэтому соединение с ним намеренно не придумывается и не активируется.

Целевой поток:

```text
LlmChatScreen
→ HRM server-side gateway
→ второй Hermes container (structured mutation intent)
→ HRM core mutation service
→ CUBA Security текущего пользователя
→ DataManager / domain services
→ audit
```

Не допускается универсальный SQL gateway из Hermes напрямую в PostgreSQL, поскольку он не гарантирует применение entity/attribute/access-group permissions пользователя CUBA.

Строго запрещаются `DELETE`, soft-delete как обход запрета, `TRUNCATE`, `DROP`, destructive DDL и произвольный raw SQL.

## 6. Текущая реализация

В текущей ветке:

- добавлен `hermesManagerChatTab`;
- Local и manager tabs переведены в `visible=false` по умолчанию;
- manager input и Send disabled;
- добавлены specific permissions;
- добавлены сообщения и задания для Antigravity/Hermes;
- добавлен `LlmChatManagerAccessContractTest`.

Не изменены `LlmChatService`, квоты, история, AI routing, сущности и БД.

## 7. Следующий этап Antigravity

Antigravity должен выполнить read-only reconnaissance production:

- подтвердить `hrm-viewer`;
- определить второй Hermes-контейнер;
- выяснить endpoint/API/auth и наличие прямых DB credentials без публикации секретов;
- определить точные CUBA roles категории «Менеджер/директор»;
- реализовать runtime permission-gating controller;
- если возможно безопасно, реализовать CUBA-mediated mutation gateway с allowlist сущностей (первый сценарий — `OpenPosition`), CREATE/UPDATE и hard deny DELETE;
- если второй контейнер может писать только напрямую в DB в обход CUBA — manager вкладку не активировать.

## 8. Проверки

```bash
git diff --check
./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.LlmChatManagerAccessContractTest' \
  --tests 'com.company.hunttech.core.LlmChatFoundationContractTest' \
  --tests 'com.company.hunttech.core.LlmChatSecurityContractTest' \
  --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

После безопасного backend binding дополнительно обязательны negative tests на DELETE, запрещённый UPDATE/CREATE, закрытые attributes, неизвестную entity/operation и raw SQL.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-09-17 | Привилегированные Local/Manager Hermes tabs переведены в fail-closed режим; зарегистрированы CUBA specific permissions; manager write controls заблокированы до production discovery и CUBA-mediated gateway. |
