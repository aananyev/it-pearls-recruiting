# CandidateSkillEnrichmentService

## Назначение и бизнес-смысл (What & Why)

Сервис извлекает из резюме навыки стандартной AI-функцией HRM HuntTech, сопоставляет их со справочником и сохраняет результат в `CandidateSkill`. Это даёт подбору вакансий актуальный набор навыков кандидата и сохраняет защиту ручных данных.

## UI Context & Navigation

Диалог «Подобрать вакансию» вызывает сервис при открытии только тогда, когда у кандидата ещё нет навыков или дата последнего изменения резюме новее самой поздней даты создания/изменения навыков. Во всех остальных случаях AI-проверка навыков пропускается. Сервис также используется существующими ручными и фоновыми сценариями обработки резюме.

## Behavior Summary

- последнее изменение CV новее последней записи навыков или навыков нет → запускается стандартное извлечение навыков → дельта фиксируется в `CandidateSkill`;
- внешний критерий актуальности признал навыки устаревшими → вызывается overload с `forceScan=true` → внутренний ранний выход по совпавшим хэшу текста и версии функции обходится;
- навыки не устарели → экран пропускает вызов сервиса → сохранённые CandidateSkill используются при AI-сопоставлении вакансий;
- ошибка извлечения → сервис возвращает неуспешный результат → экран продолжает подбор по сохранённым навыкам и показывает частичный статус.

## API и правила актуальности

```java
CandidateSkillsScanResult scanAndEnrich(
        JobCandidate candidate, CandidateCV cv, String aiFunctionCode, boolean isBackground);

CandidateSkillsScanResult scanAndEnrich(
        JobCandidate candidate, CandidateCV cv, String aiFunctionCode,
        boolean isBackground, boolean forceScan);
```

Существующий четырёхпараметрический метод сохраняет прежнее поведение и делегирует вызов с `forceScan=false`. Новый overload обходит только внутренний ранний выход для статуса `FRESH`, совпавшего хэша CV и версии AI-функции. Нормализация, вызов `SkillAnalysisService`, сохранение дельты и защита ручных приоритетов остаются штатными.

Для диалога подбора timestamp определяется как более поздняя из `createTs` и `updateTs` последнего CV и CandidateSkill. Последний CV выбирается тем же порядком `datePost DESC, createTs DESC`, что и AI-сервис сопоставления, только среди резюме с распознанным непустым текстом. Навыки считаются свежими, если их максимальная дата не раньше даты последнего изменения CV. При отсутствии навыков они считаются устаревшими.

## Размещение

| Элемент | Путь |
|---|---|
| API | `modules/global/src/com/company/hunttech/service/CandidateSkillEnrichmentService.java` |
| Реализация | `modules/core/src/com/company/hunttech/service/CandidateSkillEnrichmentServiceBean.java` |
| Пользовательский сценарий | `modules/web/src/com/company/hunttech/web/screens/jobcandidate/CandidateVacancyMatchScreen.java` |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-21 | Добавлен принудительный вызов стандартного анализа навыков для timestamp-критерия устаревания; обычные callers сохраняют прежний hash/version guard |
