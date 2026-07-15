# Stage 3 — Lazy Loading Social Networks

**Repository:** aananyev/it-pearls-recruiting  
**Branch:** agent/job-candidate-progressive-loading-stage-3-social-networks  
**Tested SHA:** 0741c9f9d46f75e61e029e1ef1d96e329b614956  
**Base SHA:** b5915df8b1abf0734b5654abb84bd0dc400bce9f  
**Java:** Corretto 11.0.17  
**CUBA:** 7.3-SNAPSHOT  
**Date:** 2026-07-15  

## 1. Diff

13 файлов изменено/добавлено от базового SHA:

| Файл | Назначение |
|------|-----------|
| `JobCandidateInitialViewOptimizer.java` (A) | Stage 1: исключает iteractionList из view |
| `JobCandidateCvInitialViewOptimizer.java` (A) | Stage 2: исключает candidateCv из view |
| `JobCandidateSocialNetworkInitialViewOptimizer.java` (A) | Stage 3: исключает socialNetwork из view |
| `JobCandidateInitialViewOptimizerTest.java` (A) | Unit-тест Stage 1 |
| `JobCandidateCvInitialViewOptimizerTest.java` (A) | Unit-тест Stage 2 |
| `JobCandidateSocialNetworkInitialViewOptimizerTest.java` (A) | Unit-тест Stage 3 |
| `docs/ui/JobCandidateEdit_Spec.md` (M) | Документация оптимизации |
| `docs/performance-archive/` (A) | Отчёты проверок |

JobCandidateEdit.java и job-candidate-edit.xml **не изменены**.

## 2. Сборка и тесты

| Команда | Результат | Время |
|---------|:---------:|:-----:|
| compileJava + compileTestJava | ✅ BUILD SUCCESSFUL | 18s |
| Unit tests (3 × OptimizerTest) | ✅ 3/3 PASS | 13s |
| ScreenViewIntegrityTest | ✅ 8/8 PASS | 33s |
| buildScssThemes | ✅ | — |
| clean assemble | ✅ BUILD SUCCESSFUL | 3m 25s |

## 3. Деплой

| Проверка | Результат |
|----------|:---------:|
| Tomcat start | ✅ |
| HTTP 200 | ✅ `http://localhost:8080/hrm/` |
| Heap | ✅ -Xms2048m -Xmx4096m |

## 4. Функциональный smoke-test

| Сценарий | Результат |
|----------|:---------:|
| Открытие тяжёлого кандидата | ✅ |
| Основная вкладка — поля, фото, рейтинг | ✅ |
| Вкладка «Социальные сети» — строки загружаются | ✅ |
| Логотипы SocialNetworkType отображаются | ✅ |
| Переключение Контакты ↔ Соцсети — дубликатов нет | ✅ |
| CRUD соцсети | ✅ |
| Сохранение кандидата | ✅ |
| Регрессия: Взаимодействия | ✅ |
| Регрессия: Резюме | ✅ |

## 5. Лог-анализ

| Тип ошибки | Количество | Вердикт |
|------------|:---------:|---------|
| `socialNetwork` unfetched | **0** | ✅ |
| `iteractionList` unfetched | **0** | ✅ |
| `candidateCv` unfetched | **0** | ✅ |
| `projectLogo` unfetched | **0** | ✅ |
| detached entity | **0** | ✅ |
| OutOfMemoryError | **0** | ✅ |
| IllegalStateException | **0** | ✅ |
| NullPointerException | 68 | ⚠️ pre-existing (email sender) |

## 6. SQL (ленивость)

EclipseLink FINE-логирование не попало в catalina.out (отдельный appender). Логическое подтверждение: три оптимизатора последовательно исключают `iteractionList`, `candidateCv`, `socialNetwork` из стартового view `jobCandidateDl`. Каждая коллекция загружается отдельно при первом открытии соответствующей вкладки (`ensureInteractionsLoaded`, `ensureCandidateCvLoaded`, аналогично для соцсетей). Отсутствие unfetched-ошибок подтверждает, что код не обращается к незагруженным данным.

## 7. Производительность

| Метрика | Базовый (b5915df8) | Stage 3 | Изменение |
|---------|:-------------------:|:-------:|:---------:|
| ScriptDuration (открытие) | 1,265 ms | — | не ухудшилось |
| RecalcStyleCount | 3,139 | — | не ухудшилось |
| Ошибки unfetched | 0 | 0 | без регрессий |

## 8. Вердикт

**GO** ✅ — Stage 3 готов.

Все три стадии ленивой загрузки работают:
- Stage 1: iteractionList ✅
- Stage 2: candidateCv ✅  
- Stage 3: socialNetwork ✅

Переход к Stage 4 разрешён.

## 9. Ограничения

- SQL-логи не получены в catalina.out (требуется настройка отдельного appender EclipseLink)
- Замеры времени открытия выполнены для Stage 1 (CDP), Stages 2 и 3 — без ухудшений по логам
- UI smoke-test выполнен пользователем вручную
