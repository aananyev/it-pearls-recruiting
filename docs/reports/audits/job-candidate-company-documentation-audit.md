# Аудит документации: JobCandidate.currentCompany -> Company

Дата: 2026-07-13.

## Найденные документы

| Документ | Статус | Что описывает |
| --- | --- | --- |
| `docs/entities/JobCandidate.md` | актуален | Сущность `hunttech_JobCandidate`, таблица `HUNTTECH_JOB_CANDIDATE`, связь `currentCompany`, экран `hunttech_JobCandidate.edit`, lazy-загрузку справочников. |
| `docs/entities/Company.md` | актуален | Справочник `hunttech_Company`, таблица `HUNTTECH_COMPANY`, поля `comanyName`, `companyShortName`, индексы и browse/edit-экраны. |
| `docs/ui/hunttech_JobCandidate.edit_Spec.md` | частично актуален | Структуру редактора кандидата и вкладку `tabCandidate`; до изменения не описывал создание компании из поля. |
| `docs/ui/JobCandidateEdit_Spec.md` | частично актуален | Общую спецификацию экрана кандидата; требует сверки с фактическим XML. |
| `docs/ui/hunttech_Company.edit_Spec.md` | актуален | Редактор компании `hunttech_Company.edit`, обязательные поля UI и вкладки. |
| `docs/ui/hunttech_Company.browse_Spec.md` | актуален | Список компаний и сценарии открытия редактора. |
| `docs/README.md`, `docs/ui/README.md` | актуальны как каталог | Содержат ссылки на сущности и UI-спеки. |
| `deployment/database-migration/*`, `Рефакторинг/Миграция данных/*` | справочные | Описывают legacy-миграцию данных, не задают бизнес-логику поля компании. |

## Противоречия и пробелы

- Документы фиксируют `currentCompany` как текущее место работы кандидата, но до этой задачи не описывали сценарий создания новой `Company` прямо из `JobCandidateEdit`.
- В документации `Company` указаны индексы для поиска/сортировки, но не зафиксировано актуальное бизнес-правило уникальности названий. В коде Java `Company.comanyName` не имеет `unique=true`; исторические SQL-скрипты содержат старые unique-индексы, поэтому правило дублей требует отдельного анализа данных перед введением новых ограничений.
- UI-документация требовала обновления разделом про commit/cancel дочернего редактора.

## Официальные механизмы CUBA

Для проекта CUBA 7.3-SNAPSHOT применим новый Screen API:

- `LookupPickerField` / `PickerField` со стандартными `picker_lookup` и `picker_open` для выбора и открытия существующей сущности.
- `ScreenBuilders.editor(Company.class, origin)` с `.newEntity()`, `.withScreenId("hunttech_Company.edit")`, `.withOpenMode(OpenMode.DIALOG)`.
- `EditorBuilder.withField(...)`: после успешного commit редактора CUBA устанавливает закоммиченную сущность в поле.
- `EditorBuilder.withTransformation(...)`: позволяет перед установкой результата выполнить `DataContext.merge(...)` и добавить созданную компанию в options container.
- `StandardOutcome.COMMIT`: используется для отличия успешного сохранения от cancel/discard/close.
- `DataContext.merge(...)`: возвращает экземпляр, отслеживаемый текущим экраном кандидата; связь `JobCandidate.currentCompany` сохраняется только при последующем сохранении кандидата.

## Документы к обновлению

- `docs/screens/job-candidate-edit.md`
- `docs/screens/company-edit.md`
- `docs/business-rules/job-candidate-company-selection.md`
- итоговый отчёт `docs/reports/job-candidate-company-implementation-report.md`
