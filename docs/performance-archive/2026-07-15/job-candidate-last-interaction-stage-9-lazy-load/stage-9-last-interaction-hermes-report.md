# Stage 9 — Ленивая загрузка последнего взаимодействия

**Branch:** agent/job-candidate-progressive-loading-stage-3-social-networks  
**SHA:** facbd44b02599225269ba223468e11a9f3ce9a45  
**Base SHA:** 20f5e8e1a88cf467bbf7378b8d13ce0bce6c483d  
**Date:** 2026-07-15  

## 1. Цель

Удалить синхронный middleware-вызов `InteractionService.getLastIteraction()` из критического пути открытия `JobCandidateEdit` и выполнять его только при нажатии «Копировать взаимодействие» без выбранной строки.

## 2. Изменения

| Файл | Изменение |
|------|-----------|
| `JobCandidateEdit.java` | удалён вызов `getLastIteraction` из `onBeforeShow` |
| `JobCandidateEdit.java` | добавлен `ensureLastInteractionLoaded()` с кешированием |
| `JobCandidateEdit.java` | добавлен `invalidateLastInteractionCache()` |
| `JobCandidateEdit.java` | `reloadInteractions()` инвалидирует кеш |
| `JobCandidateEdit.java` | `copyIteractionJobCandidate()` вызывает lazy-метод |
| `JobCandidateEdit.java` | удалён `QUERY_GET_LAST_ITERACTION` |
| `JobCandidateEdit.java` | удалён закомментированный legacy-метод |

## 3. SQL/runtime verification

| Сценарий | Ожидание | Результат |
|----------|:---------:|:---------:|
| Открытие существующего кандидата | 0 вызовов `getLastIteraction` | ✅ |
| Открытие нового кандидата | 0 вызовов | ✅ |
| Copy при выбранной строке | 0 вызовов | ✅ |
| Первый Copy без строки | 1 вызов | ✅ |
| Повторный Copy без изменений | 0 дополнительных | ✅ |
| После создания взаимодействия | следующий Copy — 1 новый | ✅ |

## 4. Тесты

| Проверка | Результат |
|----------|:---------:|
| compileJava | ✅ BUILD SUCCESSFUL |
| HTTP 200 | ✅ |
| Ручной smoke-test | ✅ PASS |

## 5. Вердикт

**STAGE_9_ACCEPTED**
