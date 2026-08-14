# Review Stage 7 — baseline загрузки городов JobCandidateEdit

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Проверяющий:** ChatGPT  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`

## 1. Ожидаемый результат

По контракту Stage 7 Hermes должен был сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-city-loader-stage-7-baseline/
stage-7-city-loader-hermes-report.md
```

Отчёт должен был содержать численные данные по `citiesDl`, SQL-планы, runtime timings, размер коллекции, heap/payload и один технический вердикт.

## 2. Фактический результат

На момент review:

- рабочая ветка не содержит новых коммитов относительно `ff58d3d155f093447cf974a6564da44a94956779`;
- `stage-7-city-loader-hermes-report.md` отсутствует;
- coordination содержит только статус `review` и сообщение `Stage 7 baseline deployed. HTTP 200`;
- изменения Java, XML, views и БД отсутствуют.

HTTP 200 подтверждает доступность приложения, но не измеряет:

- длительность `citiesDl.load()`;
- SQL P50/P95;
- число строк `City`;
- размер `citiesDc`;
- heap delta;
- размер сетевого payload;
- долю loader во времени открытия экрана;
- эффективность `cacheable="true"`.

## 3. Выводы, которые нельзя делать

Без измерений запрещено утверждать, что требуется:

- заменить `LookupPickerField` города на `SuggestionPickerField`;
- отложить `citiesDl` до первого взаимодействия;
- менять JPQL;
- уменьшать состав `city-picker-view`;
- создавать индекс;
- включать дополнительное кэширование.

## 4. Вердикт

```text
STAGE_7_INCONCLUSIVE
```

Stage 7 закрыт организационно по сообщению Hermes о готовности продолжать, но не принят как измерительный источник.

`citiesDl`, `citiesDc`, `jobCityCandidateField`, `city-picker-view`, JPQL и БД остаются без изменений.

## 5. Разрешённое продолжение

Следующий этап должен исследовать другой синхронный участок критического пути, для которого контракт виден непосредственно в коде и который можно измерить изолированно.

Выбран `InteractionService.getLastIteraction(JobCandidate)`, вызываемый синхронно из `JobCandidateEdit.onBeforeShow()` до отображения формы.
