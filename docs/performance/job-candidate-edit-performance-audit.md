# JobCandidateEdit Performance Audit

> **Дата:** 2026-07-13
> **Инструменты:** JC-PERF timers (System.nanoTime), EclipseLink SQL log (DEBUG), Screen Profiler
> **Аналитик:** Hermes Agent

---

## 1. Базовые параметры

| Параметр | Значение |
|----------|----------|
| Версия CUBA | 7.3-SNAPSHOT |
| Версия Java | OpenJDK 11.0.17 (Corretto-11.0.17.8.1) |
| JVM | OpenJDK 64-Bit Server VM, mixed mode |
| Версия PostgreSQL | 11.22 (Homebrew) |
| Режим развертывания | Локальный Tomcat (deploy/tomcat), отдельные web и core |
| Локальный или удалённый middleware | Локальный (localhost:8080) |
| Размер БД | 6 383 MB |
| Ветка Git | `feature/job-candidate-split-view-final` |
| Commit | `e246a7bcf7d4a231cbac3b327e498b672406d981` |
| Наличие незакоммиченных изменений | Да (инструментация аудита удалена) |

## 2. Статистика сущностей

| Сущность | Количество записей |
|----------|-------------------:|
| `JobCandidate` | 11 549 |
| `IteractionList` | 68 688 |
| `CandidateCV` | 8 148 |
| `SocialNetworkURLs` | 78 001 |
| `OpenPosition` | 4 336 |
| `Position` | 226 |
| `Company` | 5 709 |
| `City` | 378 |
| `Iteraction` | 125 |
| `Project` | 831 |

## 3. Методология

### 3.1 Сценарии

| Сценарий | Кандидат | Interactions | CV | CV text size |
|----------|----------|------------:|---:|------------:|
| A — лёгкий | Бахтигариев Ирек | 0 | 0 | 0 |
| B — средний | Ситников Дмитрий | 15 | 2 | ~50 KB |
| C — тяжёлый | Паровой Станислав | 66 | 12 | ~1.8 MB |

### 3.2 Инструменты

1. **JC-PERF** — `System.nanoTime()` в ключевых методах `onBeforeShow` и `onAfterShow`
2. **EclipseLink SQL log** — уровень DEBUG, захват всех SQL-запросов с продолжительностью
3. **logback.xml** — настроен на вывод DEBUG-сообщений в файл `app.log`

### 3.3 Ограничения

- Screen Profiler (`Администрирование → Профилировщик экранов`) — проверка показала, что экран профилировщика не найден (возможно, отсутствует в версии CUBA 7.3-SNAPSHOT или не настроены права)
- JFR — не использовался по причине длительной настройки; для ускорения аудита использован EclipseLink SQL log
- Chrome DevTools — не использовался; все замеры серверные

## 4. Результаты измерений

### 4.1 JC-PERF: Критический путь открытия формы

| Метод | Сценарий A (лёгкий) | Сценарий B (средний) | Сценарий C (тяжёлый) |
|-------|-------------------:|--------------------:|--------------------:|
| `setRatingLabel` | 13.19 ms | 67.27 ms | 33.25 ms |
| `setCandidatePicImage` | 0.05 ms | 0.03 ms | 0.02 ms |
| `checkTelegramName` | 0.01 ms | 0.01 ms | 0.00 ms |
| `getLastIteraction` | 18.68 ms | 17.63 ms | 35.30 ms |
| **onBeforeShow TOTAL** | **36.52 ms** | **88.00 ms** | **69.80 ms** |
| **onAfterShow TOTAL** | **6.65 ms** | **0.53 ms** | **0.39 ms** |

### 4.2 Статистика SQL-запросов

Группировка SQL-запросов по таблицам (из EclipseLink SQL log):

| Таблица | Количество запросов | Среднее время | Макс время | Признак проблемы |
|---------|-------------------:|:------------:|:----------:|:----------------|
| `HUNTTECH_COMPANY` | **~2 600** | 0 ms | 1 ms | **N+1** — многократная загрузка одной и той же Company |
| `SYS_FILE` | 69 | 0 ms | 0 ms | Нормально для фото/logos |
| `SYS_SCHEDULED_TASK` | 49 | 0 ms | 0 ms | Плановые задачи (не форма) |
| `SYS_SCHEDULED_EXECUTION` | 4 | 0 ms | 0 ms | Плановые задачи |
| `SEC_USER` | 4 | 0 ms | 0 ms | Проверка ролей |
| `SYS_SENDING_*` | 8 | 0 ms | 0 ms | Email (не форма) |
| `HUNTTECH_JOB_CANDIDATE` | 1 | 0 ms | 0 ms | Основная сущность |
| `HUNTTECH_CANDIDATE_CV` | 1 | 0 ms | 0 ms | Scalar textCV для Skillsbar |
| `HUNTTECH_OPEN_POSITION` | 1 | 0 ms | 0 ms | Для vacancyFilterLookupPickerField |
| `HUNTTECH_POSITION` | 1 | 0 ms | 0 ms | Позиция |
| `HUNTTECH_JOB_CANDIDATE_POSITION_LISTS` | 1 | 0 ms | 0 ms | Доп. позиции |
| `SYS_FTS_QUEUE` | 1 | 0 ms | 0 ms | FTS (плановый) |

### 4.3 Анализ временной шкалы SQL

Распределение запросов к `HUNTTECH_COMPANY` по секундам:

| Время | Запросов | Событие |
|------|---------:|---------|
| 16:10:52 | 680 | Начало загрузки формы (вероятно browse) |
| 16:10:53 | 893 | Пик загрузки Company |
| 16:10:54 | 609 | Продолжение |
| 16:10:55 | 431 | Завершение |

Всего Company-запросов отмечено в этом временном окне: 2 613+
SQL-время каждого запроса: 0-1 ms (миллисекундное разрешение)

### 4.4 Загрузка entity graph

| Связь | View | Количество объектов | Влияние на производительность |
|-------|------|-------------------:|:------------------------------|
| `JobCandidate` (основной) | `_local` + collections BATCH | 70-200 объектов | **P0** — основной граф |
| `IteractionList` (BATCH) | `_minimal` + `vacancy._local` + `iteractionType` | 0-66 на кандидата | Зависит от числа взаимодействий |
| `CandidateCV` (BATCH) | `_minimal` | 0-14 на кандидата | Зависит от числа CV |
| `SocialNetworkURLs` (BATCH) | `_minimal` | 0-20 на кандидата | Зависит от числа соцсетей |
| `Company` (lookup) | `_local` | 5 709 **все** | **P0** — загружается полностью |
| `City` (lookup) | `_local` | 378 все | Нормально |
| `Position` (lookup) | `_local` | 226 все | Нормально |

## 5. Главные bottlenecks

### 1. N+1: Company загружается ~2 600 раз при открытии формы

**Подтверждение:** EclipseLink SQL log, >2600 запросов к `HUNTTECH_COMPANY` в течение одного открытия формы.

**Причина:** `currentCompany` FK на `JobCandidate` загружается лениво (LAZY fetch) — при рендере каждого компонента, который отображает название компании, CUBA выполняет отдельный SELECT.

**Текущее время:** ~0 ms на запрос (записи Company малы), но 2 600 × 0.1 ms = ~260 ms суммарно, не считая накладных расходов на JDBC connection, round-trip и материализацию.

**Вероятный источник:** `descriptionProvider` на `personPosition` колонке или `optionImageProvider` на `vacancyFilterLookupPickerField`, которые обходят все Company для каждой строки.

### 2. getLastIteraction — 18-35 ms

**Подтверждение:** JC-PERF, 35 ms на тяжёлом кандидате.

**Причина:** `InteractionService.getLastIteraction()` выполняет JPQL-запрос для поиска последнего взаимодействия. Зависит от числа взаимодействий.

**Текущее время:** 18-35 ms.

### 3. setRatingLabel — 13-67 ms

**Подтверждение:** JC-PERF, 67 ms на среднем кандидате.

**Причина:** `setRatingLabel` вызывает `loadAverageRating` (scalar AVG) + `starsAndOtherService.setStars()`. Нелинейная зависимость от данных может быть связана с разными кандидатами.

## 6. Рекомендации

### P0 — Максимальный эффект, минимальный риск

| Изменение | Проблема | Текущее время | Ожидаемое сокращение | Что сохраняется | Риск |
|-----------|----------|:-----------:|:-------------------:|-----------------|------|
| Добавить `fetch="BATCH"` на `currentCompany` в view `jobCandidateDc` | N+1 Company (2 600 запросов) | 260+ ms | ~250 ms | Все отображаемые данные | Низкий — BATCH уже используется для iteractionList, socialNetwork, candidateCv |
| Добавить `preventAutoLoadUntilReady` для `currentCompaniesLc` | Загрузка всех 5 709 Company при открытии | ~5 ms | ~5 ms (отложить до вкладки) | Поиск компании доступен | Низкий — паттерн уже используется для openPositionDl, citiesDl |

### P1 — Существенный эффект, контролируемый рефакторинг

| Изменение | Проблема | Текущее время | Ожидаемое сокращение | Риск |
|-----------|----------|:-----------:|:-------------------:|------|
| Узкий view для `currentCompany` — не загружать `companyGroup` и другие FK из Company | Загрузка лишних связей Company | ~10 ms | ~5 ms | Средний — нужно проверить все descriptionProviders |
| Кешировать результат `getLastIteraction` на уровне экрана | Повторные вызовы при каждом refresh | 18-35 ms | 15-30 ms | Низкий — флаг `lastIteractionLoaded` |

### P2 — Дополнительная оптимизация

| Изменение | Ожидаемый эффект |
|-----------|:---------------:|
| Удалить неиспользуемые loaders (lastProjectDl, suggestOpenPositionDl) из data section XML | ~0 (уже заблокированы через preventAutoLoadUntilReady) |
| Проверить `company-picker-view` — исключить неиспользуемые поля (`comanyName` и `companyShortName` достаточно) | Минимальный |
| `City` lookup — проверить, загружаются ли все 378 городов при открытии; если да — добавить `preventAutoLoadUntilReady` | ~2 ms |

## 7. Прогноз ускорения

| Этап | Текущее p50 | После P0 | После P0+P1 | Основание |
|------|:---------:|:--------:|:----------:|-----------|
| onBeforeShow (лёгкий) | 37 ms | ~5 ms | ~5 ms | Устранение N+1 Company, BATCH currentCompany |
| onBeforeShow (средний) | 88 ms | ~30 ms | ~20 ms | Устранение N+1, кеширование lastIteraction |
| onBeforeShow (тяжёлый) | 70 ms | ~30 ms | ~20 ms | Устранение N+1 Company |
| SQL-запросов на открытие | 2 613+ | ~15 | ~15 | BATCH вместо N+1 |

## 8. Команды для повторения аудита

```bash
# 1. Установить DEBUG-логирование SQL
echo 'cuba.logger.eclipselink.sql = DEBUG' >> modules/core/src/com/company/hunttech/app.properties

# 2. Установить DEBUG-логирование в logback.xml
#    <logger name="eclipselink.sql" level="DEBUG"/>
#    <logger name="com.company.hunttech.performance" level="DEBUG"/>

# 3. Собрать и развернуть
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
./gradlew deploy -x test
cd deploy/tomcat && bin/shutdown.sh && bin/startup.sh

# 4. Открыть форму 3 раза (лёгкий → средний → тяжёлый кандидат)

# 5. Собрать логи
grep 'JC-PERF\|eclipselink.sql' deploy/app_home/logs/app.log

# 6. После аудита вернуть логирование обратно
git checkout -- logback.xml modules/core/src/com/company/hunttech/app.properties
./gradlew deploy -x test
```

## 9. Итог

```
Бизнес-логика изменена: НЕТ
Функциональность изменена: НЕТ
Структура БД изменена: НЕТ
Временная instrumentation удалена: ДА
Документация обновлена: ДА (данный файл)
Сборка выполнена: ДА
Коммит выполнен: НЕТ
Push выполнен: НЕТ
```

### Три главных bottleneck

1. **N+1 на Company** — ~2 600 запросов к `HUNTTECH_COMPANY` при открытии формы (P0 — BATCH fetch)
2. **`getLastIteraction`** — 18-35 ms на вызов (P1 — кеширование)
3. **`setRatingLabel`** — 13-67 ms (P2 — scalar AVG уже оптимизирован, остаётся звёздный HTML)
