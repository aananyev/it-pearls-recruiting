# Review Stage 8 — синхронная загрузка последнего взаимодействия

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый HEAD:** `517c0c1fafb94111aa7abc2293cec99d86520e26`

## 1. Проверенный результат Hermes

Hermes сообщил: «Только документация. HTTP 200. Stage 8 baseline deployed».

Проверка репозитория показала:

- рабочая ветка осталась на базовом HEAD `517c0c1f`;
- новых коммитов Stage 8 в рабочей ветке нет;
- файл `stage-8-last-interaction-hermes-report.md` отсутствует;
- coordination не был переведён из `active` в `review` и по-прежнему содержит старый HEAD;
- Java, XML, сервисы, views и БД не изменялись.

HTTP 200 подтверждает запуск приложения, но не является измерением стоимости вызова `InteractionService.getLastIteraction()`.

## 2. Недостающие доказательства baseline

Не представлены:

- SQL MIN/MAX/AVG/P50/P95;
- `EXPLAIN (ANALYZE, BUFFERS, VERBOSE)`;
- длительность middleware-вызова;
- доля вызова во времени initial open;
- число SQL на одно открытие;
- сравнение cold/warm cache;
- проверка индексов;
- Data View Integrity matrix;
- один из предусмотренных Stage 8 вердиктов.

Поэтому выводы о необходимости индекса, фоновой задачи или сохранении синхронного вызова не считаются доказанными.

## 3. Статически подтверждённый call graph

При открытии существующего кандидата `JobCandidateEdit.onBeforeShow()` выполняет:

```java
lastIteraction = interactionService.getLastIteraction(getEditedEntity());
lastIteractionLoaded = true;
```

`InteractionServiceBean.getLastIteraction()` выполняет запрос:

```jpql
select e
from hunttech_IteractionList e
where e.candidate = :candidate
order by e.numberIteraction desc
```

с `maxResults(1)` и `iteractionList-picker-view`.

Результат `lastIteraction` используется в `copyIteractionJobCandidate()` только в сценарии:

```text
пользователь нажал «Копировать взаимодействие»
→ строка таблицы не выбрана
→ из последнего взаимодействия берётся vacancy
```

Если строка таблицы выбрана, используется выбранное взаимодействие и `lastIteraction` не требуется.

## 4. Вывод

Данные Stage 8 не подтверждают стоимость запроса, но статический call graph доказывает, что результат не нужен для первичного отображения карточки.

Безопасная стратегия следующего этапа:

```text
DEFER_LAST_INTERACTION
```

Запрос следует выполнять один раз при первом вызове `copyIteractionJobCandidate()` без выбранной строки, а не при каждом открытии `JobCandidateEdit`.

## 5. Вердикт

```text
STAGE_8_INCONCLUSIVE
STAGE_9_IMPLEMENTATION_ALLOWED
```

Stage 9 должен сохранить бизнес-поведение копирования взаимодействия, удалить middleware round-trip из `onBeforeShow`, добавить ленивое кеширование в пределах экрана и обязательную верификацию отсутствия SQL при простом открытии карточки.
