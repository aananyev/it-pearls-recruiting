# Stage 2: исключение полной загрузки компаний из JobCandidateEdit

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `d3558cb2370cfcf34c8f7e286d29ad69c9ffd13a`  
**Исполнитель:** ChatGPT  

## 1. Основание

Hermes подтвердил вердикт:

```text
FULL_COMPANY_LOADER_CONFIRMED
```

При каждом initial open `JobCandidateEdit` механизм `@LoadDataBeforeShow` запускал loader:

```text
currentCompaniesLc
```

Запрос:

```jpql
select e
from hunttech_Company e
order by e.comanyName
```

возвращал примерно 5 623 сущности `Company`. Чистое время SQL было небольшим, но web-tier материализовывал полный набор сущностей и включал его в экранный граф данных, хотя `currentCompanyField` уже работает как `SuggestionPickerField` и не использует полный options container.

## 2. Реализованное изменение

Добавлен компонент:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/
JobCandidateCompanyLoaderOptimizer.java
```

Компонент реализует `ControllerDependencyInjector` и выполняется до lifecycle экрана. Для `JobCandidateEdit` он получает `currentCompaniesLc` из `ScreenData` и подключает `PreLoadListener`:

```java
currentCompaniesLoader.addPreLoadListener(loadEvent -> loadEvent.preventLoad());
```

В результате автоматический вызов loader через `@LoadDataBeforeShow` должен завершаться до выполнения JPQL и загрузки сущностей.

## 3. Границы подэтапа

На этом подэтапе намеренно не изменялись:

- `JobCandidateEdit.java`;
- `job-candidate-edit.xml`;
- `currentCompanyField`;
- suggestion-query `%строка%`;
- `minSearchStringLength="2"`;
- `suggestionsLimit="50"`;
- actions `lookup` и `open`;
- action `createCompany`;
- возврат созданной компании из `CompanyEdit`;
- сущности, views, Liquibase и индексы.

`currentCompaniesDc/currentCompaniesLc` пока сохранены в XML и контроллере как compatibility-контракт create-company flow. После runtime-подтверждения отдельным этапом можно заменить возврат созданной компании на точечную загрузку через `DataManager` и удалить остаточный контейнер вместе с loader.

## 4. Ожидаемый эффект

При initial open формы должно измениться:

| Метрика | До | После Stage 2 |
|---|---:|---:|
| Вызов `currentCompaniesLc` | 1 | предотвращён |
| Загружено Company полным loader | ≈5 623 | 0 |
| Полный JPQL списка компаний | выполняется | отсутствует |
| Suggestion-поиск | по вводу | без изменений |
| Lookup/open/create | работает | должно работать без изменений |

Изменение не обещает ускорение самого substring suggestion-запроса. Оно устраняет только ненужную полную загрузку при открытии формы.

## 5. Риски и контроль

### 5.1 Порядок dependency injectors

Компонент зарегистрирован с `@Order(Ordered.LOWEST_PRECEDENCE)`. К моменту его выполнения штатная dependency injection должна быть завершена, а `ScreenData` и loader доступны. Listener подключается до `@LoadDataBeforeShow`.

### 5.2 Создание новой компании

Существующий `mergeCreatedCompany()` добавляет созданную компанию в пустой `currentCompaniesDc`. Поэтому блокировка initial load не должна мешать подстановке новой компании в `currentCompanyField`.

Обязательна ручная проверка:

```text
создать Company → сохранить CompanyEdit → новая Company установлена в currentCompanyField
```

### 5.3 Повторная загрузка

Loader блокируется при каждом вызове `load()`. В текущем экране он не нужен ни для suggestion, ни для lookup/open. Любой обнаруженный runtime-сценарий, который действительно требует полного `currentCompaniesDc`, должен быть описан как блокер удаления контейнера, а не обходиться снятием запрета без анализа.

## 6. Обязательное задание Hermes

### 6.1 Синхронизация

```bash
git fetch origin --prune
git show origin/coordination/active-work:.ai/active-work.yml

ACTIVE_BRANCH=$(git show origin/coordination/active-work:.ai/active-work.yml \
  | sed -n 's/^  branch: //p')

git switch "$ACTIVE_BRANCH"
git pull --ff-only origin "$ACTIVE_BRANCH"

git status --short --branch
git rev-parse HEAD
git rev-parse "origin/$ACTIVE_BRANCH"
```

При несовпадении `HEAD`, `origin/<branch>` и `work.head_sha` остановиться с `STAGE_2_BLOCKED`. Не применять `stash`, `reset --hard`, cherry-pick и force push.

### 6.2 Статические проверки

```bash
git diff --check
./gradlew :app-web:compileJava :app-web:compileTestJava --no-daemon --stacktrace
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Требования:

- `JobCandidateCompanyLoaderOptimizer` обнаруживается Spring component scan;
- `ScreenData.getLoader("currentCompaniesLc")` успешно разрешается;
- `ScreenViewIntegrityTest`: 8/8 PASS;
- `BUILD SUCCESSFUL`.

### 6.3 Runtime baseline после изменения

Проверить три сценария:

1. тяжёлый существующий кандидат `ee1cd239...`;
2. существующий кандидат без компании;
3. новый кандидат.

Для каждого:

- один прогревочный запуск;
- пять измерительных запусков;
- не вводить текст в поле компании до окончания initial-open замера;
- записать MIN, MAX, AVG, P50 и P95;
- записать SQL count и SQL total;
- отдельно отметить количество загруженных Company.

Критическое доказательство:

```text
select e from hunttech_Company e order by e.comanyName
```

не должен присутствовать в initial-open SQL.

### 6.4 Функциональная регрессия поля компании

Проверить:

- текущая компания существующего кандидата видна сразу;
- кандидат без компании открывается без ошибок;
- ввод 0–1 символа не запускает suggestion SQL;
- ввод 2 и более символов возвращает до 50 результатов;
- выбор suggestion устанавливает компанию;
- `lookup` открывает штатный экран выбора;
- `open` открывает выбранную компанию;
- `createCompany` открывает `CompanyEdit`;
- сохранённая новая компания подставляется в кандидата;
- отмена создания не блокирует повторный запуск action;
- сохранение и повторное открытие кандидата сохраняет связь;
- отсутствуют unfetched/detached ошибки, `IllegalStateException`, `NullPointerException` и `OutOfMemoryError`.

### 6.5 Runtime приложения

- развернуть локальный `/hrm`;
- проверить HTTP 200;
- проверить логи web/core;
- production не использовать и не изменять.

## 7. Формат отчёта Hermes

Сохранить результат:

```text
docs/performance-archive/2026-07-15/job-candidate-company-loader-stage-2/
job-candidate-company-loader-stage-2-hermes-report.md
```

Допустимые вердикты:

```text
STAGE_2_CONFIRMED
STAGE_2_REGRESSION
STAGE_2_BLOCKED
```

Для `STAGE_2_CONFIRMED` обязательно привести:

- точный SHA;
- результаты сборки и 8/8 ScreenViewIntegrityTest;
- таблицу пяти запусков;
- P50/P95 до и после;
- SQL-доказательство отсутствия полного Company loader;
- результаты suggestion/lookup/open/create.

## 8. Следующий этап

Только после `STAGE_2_CONFIRMED`:

1. изменить `mergeCreatedCompany()` на точечную загрузку сохранённой `Company` через `DataManager` с `company-picker-view`;
2. удалить injections `currentCompaniesLc/currentCompaniesDc` из `JobCandidateEdit.java`;
3. удалить `currentCompaniesDc/currentCompaniesLc` из XML;
4. удалить временный `JobCandidateCompanyLoaderOptimizer`, так как loader перестанет существовать;
5. повторить те же функциональные и performance-проверки;
6. синхронно обновить `docs/ui/JobCandidateEdit_Spec.md`.

## 9. Статус ChatGPT

Код и документация Stage 2 опубликованы в рабочей ветке. Runtime-сборка, локальный PostgreSQL, Tomcat и браузерный профиль в среде пользователя недоступны ChatGPT, поэтому фактическая приёмка передана Hermes.
