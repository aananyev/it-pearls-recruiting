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

## Metadata выполнения и колонка «Провайдер / Модель»

### Фактическая цепочка выполнения

Все точки запуска используют один orchestration-контракт:

| Путь | Вход в сервис | Что происходит с metadata |
|---|---|---|
| Фоновый worker | `CandidateSkillsEnrichmentWorker -> scanAndEnrich(..., true)` | Каждый уровень `MAIN/SECONDARY/TERTIARY`, а при пустом результате и `ALL`, получает собственный `SkillAnalysisResult`. Первый фактический `AiExecutionResult` с полными metadata выбирается для аудита; если полных metadata нет, сохраняется первый фактический AI-результат без выдумывания отсутствующих значений. |
| Ручное сканирование | `JobCandidateReestr -> scanAndEnrich(..., false)` | Использует ту же цепочку и тот же mapping, отличается только разрешённой fallback-политикой. |
| Проверка перед подбором вакансий | `CandidateVacancyMatchScreen -> scanAndEnrich(..., false, true)` | `forceScan` обходит только hash/version guard; способ получения и сохранения metadata не меняется. |
| Retry worker | запись `RETRY` переводится worker-ом в `PROCESSING`, затем вызывается тот же `scanAndEnrich` | До нового успешного результата прежние `providerCode`, `modelName` и `executionSource` не очищаются. Ошибка меняет статус/ошибку/backoff, но не provenance последнего успеха. |
| Повторный анализ из мониторинга | `reprocessCv()` ставит `NOT_ANALYZED`, затем worker вызывает тот же `scanAndEnrich` | Постановка в очередь не меняет metadata. Они заменяются только новым успешным AI-результатом либо новым фактически использованным dictionary fallback. |

`SkillAnalysisService` вызывает `AiExecutionService.executeText()`. Успешный внешний вызов возвращает `AiExecutionResult` с эффективными `providerCode` и `modelName` — с учётом provider/model failover и override. Ошибка именно внешнего AI-вызова может привести к dictionary fallback, если он разрешён. Ошибка после получения `AiExecutionResult` (парсинг, сопоставление со справочником, persistence) не должна маскироваться как dictionary fallback: она проходит в обычный `RETRY/ERROR`, а metadata предыдущего успешного анализа сохраняются.

### Mapping `AiExecutionResult -> entity -> DB -> view -> UI`

| Условие | `CandidateCvSkillAnalysis` | Колонки БД | Значение UI |
|---|---|---|---|
| Хотя бы один уровень реально завершён через AI, `providerCode` и `modelName` заполнены | `executionSource=AI`; сохраняются фактические значения выбранного `AiExecutionResult` | `EXECUTION_SOURCE`, `PROVIDER_CODE`, `MODEL_NAME`; токены из того же результата | Фактическая модель; при наличии провайдера — `provider / model`, например `deepseek / deepseek-v4-flash` |
| AI завершён, но metadata неполная | `executionSource=AI_METADATA_INCOMPLETE`; известное значение сохраняется, отсутствующее остаётся `NULL` | Те же колонки, без подстановки текущей конфигурации | Явная частичная metadata: `AI: provider / модель не зафиксирована`, `AI: провайдер не зафиксирован / model` либо `AI: модель не зафиксирована` |
| Ни один AI-результат не использован, применён прямой поиск по справочнику | `executionSource=DICTIONARY_FALLBACK`; provider/model/tokens для этого успешного результата равны `NULL` | `EXECUTION_SOURCE=DICTIONARY_FALLBACK` | `Fallback: справочник` |
| Историческая запись до появления provenance metadata | `executionSource`, provider и model равны `NULL` | Миграция не выполняет `UPDATE`/backfill | `Метаданные недоступны` |
| Retry/error после ранее успешного анализа | статус/error/backoff обновляются, provenance последнего успеха не меняется | provider/model/source остаются прежними | Колонка продолжает показывать последнюю успешно зафиксированную модель или fallback |

Запрещено восстанавливать старые metadata из текущей `AiFunctionConfiguration`: это не доказывает, какая модель выполнила исторический вызов. `candidateCvSkillAnalysis-browse-view` обязан загружать `providerCode`, `modelName` и `executionSource`; `analysesDl.load()` перечитывает их перед renderer-ом. Единственный источник строки «Fallback: справочник» — renderer мониторинга при явном `DICTIONARY_FALLBACK`, а не отсутствие model/provider.

Добавление `EXECUTION_SOURCE` выполняется миграцией `260922-1-addExecutionSourceToCandidateCvSkillAnalysis`.
