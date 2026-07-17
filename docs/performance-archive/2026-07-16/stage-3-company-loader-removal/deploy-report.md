# Stage 3 — Деплой и верификация удаления loader компаний

**Дата:** 2026-07-16  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Коммит:** `a94d3fed` — `perf(job-candidate): подготовить Stage 3 удаления loader компаний [deploy-local]`

---

## Результаты деплоя

| Этап | Результат |
|------|-----------|
| Сборка (`clean assemble`) | ✅ BUILD SUCCESSFUL (3m 31s) |
| Widgetset | ✅ BUILD SUCCESSFUL |
| Деплой + перезапуск Tomcat | ✅ Выполнен (потребовалось исправление конфликта Groovy 2.5.2/2.5.23 в shared/lib) |
| HTTP 200 | ✅ `http://localhost:8080/hrm/` → 200 |

---

## Результаты тестов

| Тест | Результат |
|------|-----------|
| Unit-тесты (`:app-core:test --tests "com.company.hunttech.app.*"`) | ✅ BUILD SUCCESSFUL |
| ScreenViewIntegrityTest | ✅ 8/8 PASSED |
| `requiredScreensRegistered` | ✅ |
| `candidateCVEdit_view` | ✅ |
| `jobCandidateEdit_iteractionListView` | ✅ |
| `jobCandidateEdit_view` | ✅ |
| `webAppPropertiesLoginScreenExists` | ✅ |
| `deployedJarContainsRequiredScreens` | ✅ |
| `iteractionListEdit_view` | ✅ |
| `openPositionEdit_view` | ✅ |

---

## Smoke-test

| Проверка | Результат |
|----------|-----------|
| `/hrm/` | ✅ HTTP 200 |
| `/hrm/app/` | ✅ HTTP 301 (редирект — норма CUBA) |
| Tomcat PID | 56329 — работает |
| Логи (SEVERE) | 0 новых ошибок после чистового запуска |

---

## Исправленные проблемы при деплое

**Конфликт Groovy:** в `deploy/tomcat/shared/lib/` обнаружены дублирующиеся JAR — `groovy-jsr223-2.5.2.jar` и `groovy-jsr223-2.5.23.jar`. При старте это вызывало `GroovyRuntimeException: Conflicting module versions`. Удалён `groovy-jsr223-2.5.2.jar` (основной Groovy — 2.5.23).

---

## Примечание

DevOps-агент выполнил тестирование и верификацию. Применение патча (`stage-3-implementation-contract.md`) — зона ответственности ChatGPT (изменение кода). Деплой текущего состояния кода успешен.
