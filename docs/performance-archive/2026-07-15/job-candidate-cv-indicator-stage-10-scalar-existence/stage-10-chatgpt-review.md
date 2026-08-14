# Stage 10 — review скалярной проверки наличия резюме

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Итоговый HEAD:** `8ec2b26f3ba7f84c25f05a3c46ba85a1304c5a42`

## 1. Проверенный scope

В рабочей ветке относительно базового SHA `afba35ef2a1bcef2e58f6e27d3bcfcea1637fe06` изменены:

- `JobCandidateEdit.java`;
- `JobCandidateEdit_Spec.md`;
- отчёты Stage 9 и Stage 10.

XML, views, entities, сервисы, Liquibase, индексы и SCSS не изменялись.

## 2. Проверка реализации

`hasCandidateCv()`:

- для нового кандидата возвращает `false` без SQL;
- передаёт в запрос UUID кандидата, а не detached entity;
- выполняет скалярный `count`;
- не вызывает `getEditedEntity().getCandidateCv()`;
- не материализует `CandidateCV`, `textCV`, файлы и связанные entity-графы.

Фактический запрос:

```jpql
select count(e)
from hunttech_CandidateCV e
where e.candidate.id = :candidateId
  and e.deleteTs is null
```

Явное условие `deleteTs is null` функционально соответствует требованию учитывать только неудалённые резюме. CUBA soft deletion при JPQL также должна быть проверена на отсутствие двойной или некорректной фильтрации, но текущий запрос не меняет данные и не загружает entity.

## 3. Проверки Hermes

Пользователь передал результат:

- `VERIFICATION: PASS` — 5/6;
- один false negative относится к автоматической проверке `deleteTs`, наличие фильтра подтверждено кодом;
- compile и deploy успешны;
- `/hrm` отвечает HTTP 200.

Отчёт Hermes содержит только compileJava и HTTP 200 и использует нераскрытый placeholder `$(git rev-parse HEAD)`. Поэтому этот отчёт недостаточен как самостоятельное доказательство полного acceptance gate. Принятие основано на полном diff, фактическом коде и переданной пользователем верификации.

## 4. Вердикт

```text
STAGE_10_ACCEPTED
```

Stage 10 устраняет риск `Cannot get unfetched attribute [candidateCv]` в initial open и сохраняет ленивую загрузку полной коллекции только при открытии вкладки «Резюме».

## 5. Ограничения для следующих этапов

- Не возвращать чтение `candidateCv` в `onBeforeShow`.
- Не объединять индикатор CV с полной загрузкой резюме.
- Не менять `ensureCandidateCvLoaded()` без отдельного контракта и Data View Integrity проверки.
- В итоговых отчётах указывать реальный SHA, а не shell-placeholder.
