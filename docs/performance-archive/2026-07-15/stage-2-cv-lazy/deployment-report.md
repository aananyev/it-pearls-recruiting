# Отчёт развёртывания — Stage 2: CV Lazy Loading

**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-2-cv`  
**Коммит:** `a7447ead` — `docs(job-candidate): описать исправление projectLogo`  
**Триггер:** `[deploy-local]` от ChatGPT

---

## ✅ Сводка

| Этап | Результат |
|------|-----------|
| Обнаружение | 3 новых коммита, последний с `[deploy-local]` |
| Получение кода | `git checkout -B` → `a7447ead` |
| Сборка (`clean assemble -x test`) | **BUILD SUCCESSFUL** (3m 46s, 37 tasks) |
| Деплой (widgetset + Tomcat) | **BUILD SUCCESSFUL × 5**, виджетсет развёрнут |
| HTTP-проверка | **HTTP 200** (`http://localhost:8080/hrm/`) |
| Тесты | ✅ 10/10 пройдено (0 ошибок) |
| Логи (ошибки) | ✅ Нет `unfetched attribute`, `projectLogo`, `candidateCv`, `detached`, `OOM` |

---

## 🧪 Результаты тестов

| Тест | Пройдено | Ошибок | Время |
|------|----------|--------|-------|
| `ScreenViewIntegrityTest` | **8/8** | 0 | 3.656s |
| `JobCandidateCvInitialViewOptimizerTest` | **2/2** | 0 | 0.159s |

---

## 🔍 Проверка логов

Проверены последние 500 строк `catalina.out` после развёртывания:

- ❌ **Нет** `Cannot get unfetched attribute projectLogo`
- ❌ **Нет** `Cannot get unfetched attribute candidateCv`
- ❌ **Нет** `detached entity passed to persist`
- ❌ **Нет** `NullPointerException` (кроме Emailer — `sendingMessage.caption is null`, не связано с CV)
- ❌ **Нет** `OutOfMemoryError`
- ⚠️ LuceneIndexer: `No description for entity itpearls_CandidateCV` — FTS-предупреждение, не ошибка

---

## 🌐 HTTP-проверка

| URL | Код | Описание |
|-----|-----|----------|
| `http://localhost:8080/hrm/` | 200 | Главная страница |
| `http://localhost:8080/hrm/app/` | 301 | Редирект на логин |
| `http://localhost:8080/hrm/` (Basic Auth) | 200 | С аутентификацией |

---

## ⚠️ Ограничения

- UI-верификация через браузер **не выполнена** — окно Safari скрыто/недоступно для захвата экрана (cua-driver). Требуется ручная проверка: открыть кандидата с CV и проектами, проверить логотипы, CRUD CV, сохранение.

---

## 📋 Итого

Развёртывание успешно. Все автотесты пройдены. Критических ошибок в логах нет. Требуется ручная UI-верификация сценариев из промпта.
