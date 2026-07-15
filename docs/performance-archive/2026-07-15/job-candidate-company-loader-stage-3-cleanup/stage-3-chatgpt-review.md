# Проверка Stage 3 cleanup справочника компаний

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Проверенный HEAD:** `fd9dc5eafb7d6a9d9ffa58b51b5aec0a88847309`

## 1. Проверенный diff

Stage 3 изменяет только согласованный контур работы с компаниями в `JobCandidateEdit`:

- удалены injections `currentCompaniesLc` и `currentCompaniesDc`;
- из `job-candidate-edit.xml` удалены `currentCompaniesDc/currentCompaniesLc`;
- удалён временный `JobCandidateCompanyLoaderOptimizer`;
- удалён его регрессионный тест;
- create-company flow переведён на точечную загрузку созданной `Company` по идентификатору через `DataManager` и `company-picker-view`;
- добавлен `JobCandidateCreatedCompanyResolverTest` с тремя сценариями.

Не изменены:

- suggestion-поиск `%строка%`;
- `minSearchStringLength=2`;
- лимит 50 результатов;
- actions `lookup`, `open`, `createCompany`;
- сущности, views, Liquibase, БД и SCSS.

## 2. Оценка реализации

Метод `mergeCreatedCompany()` после сохранения `CompanyEdit` выполняет один точечный запрос:

```jpql
select e
from hunttech_Company e
where e.id = :companyId
```

Запрос использует `company-picker-view`, после чего сущность merge-ится в текущий экранный `DataContext`. Полный справочник компаний больше не присутствует в XML data-контракте и не может быть автоматически загружен через `@LoadDataBeforeShow`.

Метод `resolveCreatedCompany()` корректно обрабатывает:

1. `null` после отмены;
2. сущность без идентификатора без SQL и merge;
3. сохранённую сущность: загрузка по ID → merge → возврат значения полю.

Статический вердикт по коду:

```text
CODE_REVIEW_PASS
```

## 3. Подтверждённые проверки Hermes

По сообщению Hermes и коммиту Stage 3 подтверждены:

- unit-тест `JobCandidateCreatedCompanyResolverTest`: 3/3 PASS;
- `ScreenViewIntegrityTest`: BUILD SUCCESSFUL;
- `clean assemble`: BUILD SUCCESSFUL;
- локальный deploy: успешно;
- HTTP 200 для `http://localhost:8080/hrm/`.

## 4. Оставшийся ручной smoke-test

ChatGPT не имеет доступа к локальному браузеру и адресу `localhost` пользователя. До окончательной приёмки Алексей или Hermes с доступным computer-use должен проверить:

1. Открыть существующего кандидата с выбранной компанией — значение отображается сразу.
2. Ввести 0–1 символ — suggestion SQL не выполняется.
3. Ввести 2 и более символов — появляется не более 50 результатов.
4. Выбрать компанию из suggestion — значение устанавливается.
5. Нажать `lookup` — выбрать компанию из штатного списка.
6. Нажать `open` — открыть и закрыть выбранную компанию.
7. Выполнить `createCompany` — сохранить новую компанию; она должна автоматически установиться в `currentCompanyField`.
8. Сохранить кандидата, закрыть и открыть повторно — связь с новой компанией сохранена.
9. Отменить создание компании — action снова доступен и повторно открывает редактор.
10. В runtime-логах отсутствуют `IllegalStateException`, unfetched/detached errors, `NullPointerException` и `OutOfMemoryError`.

## 5. Обнаруженные расхождения документации

`docs/ui/JobCandidateEdit_Spec.md` на проверенном HEAD остаётся несинхронизированным с кодом Stage 3:

- описывает уже удалённый `JobCandidateCompanyLoaderOptimizer`;
- указывает, что `currentCompaniesDc/currentCompaniesLc` временно сохранены;
- содержит `currentCompaniesDc` в таблице контейнеров;
- история изменений описывает только блокировку loader, а не его окончательное удаление и точечный `DataManager` create-flow.

Согласно правилам HRM HuntTech, код экрана и XML без синхронного обновления спецификации не считается завершённым этапом.

## 6. Итоговый статус

```text
CODE_REVIEW_PASS
RUNTIME_BUILD_PASS
MANUAL_SMOKE_PENDING
DOCUMENTATION_SYNC_REQUIRED
```

До исправления спецификации и фиксации ручного smoke-test окончательный `STAGE_3_CONFIRMED` не выставляется.
