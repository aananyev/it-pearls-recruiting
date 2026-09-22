# Диагностика Yandex 360 Calendar пользователя alan

**Статус:** WAITING_FOR_HERMES
**BASE_SHA:** `0c1885abe4e677a3895a3d7b833956873f1e6132`
**Ветка:** `agent/yandex-calendar-alan-diagnostics`

## Цель

Проверить соответствие календарной интеграции HRM HuntTech официальным требованиям Yandex OAuth/Yandex 360 и протоколу CalDAV, выполнить read-only диагностику production для `alan`, устранить ложноположительную диагностику подключения и добавить автоматизированные проверки отказов.

## Разрешённая область

- `YandexIntegrationServiceBean` — только диагностика CalDAV и классификация ошибок;
- профильные unit/contract tests;
- документация интеграции и инструкция Hermes;
- read-only запросы к production без вывода секретов.

## Запрещённая область

- deploy, restart, migration и изменение production;
- изменение реальных событий и календарей;
- вывод, сохранение или расшифровка токенов в логах/артефактах;
- изменение бизнес-логики планирования встреч вне обработки ошибок подключения.

## Критерии приёмки

- успешный CalDAV discovery подтверждается только HTTP `207 Multi-Status`;
- `401`, `403`, `404` и сетевой сбой не превращаются в фиктивный список календарей;
- диагностический статус и `calendarConnected` отражают фактический результат;
- тестами покрыты success, invalid/expired auth, insufficient scope, unavailable calendar и network failure;
- документация фиксирует OAuth scope и отличие CalDAV от административного API Yandex 360;
- production остаётся неизменённым.

## Ограничение production-диагностики

Среда ChatGPT не имеет сетевого маршрута к `hr.hunttech.ru`/SSH (`Network is unreachable`). Фактические metadata и live CalDAV discovery для `alan` должен выполнить Hermes на сервере строго read-only по инструкции `.ai/instructions/2026-09-22-yandex-calendar-alan-verification.md`.
