# Baseline загрузки компаний в JobCandidateEdit

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Рабочая ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Исходный HEAD:** `b68e27d85af056fab2a1ec72079df82d362c5b0d`  
**Этап:** 1 — фиксация текущего поведения, без изменения кода и БД

## 1. Цель этапа

Зафиксировать фактический baseline поля компании в `JobCandidateEdit` перед дальнейшей оптимизацией.

На этом этапе запрещено изменять:

- `JobCandidateEdit.java`;
- `job-candidate-edit.xml`;
- сущности и views;
- JPQL;
- Liquibase и структуру БД;
- индексы PostgreSQL;
- бизнес-логику выбора, открытия и создания компании.

Результат этапа должен ответить на один основной вопрос:

> Выполняется ли запрос полного списка компаний `currentCompaniesLc` при initial open `JobCandidateEdit`, несмотря на то что поле уже переведено на `SuggestionPickerField` и не использует `optionsContainer`?

## 2. Зафиксированное текущее состояние

### 2.1 Поле компании уже использует серверные подсказки

В актуальном XML поле `currentCompanyField` уже имеет тип `suggestionPickerField`:

```xml
<suggestionPickerField id="currentCompanyField"
                       dataContainer="jobCandidateDc"
                       property="currentCompany"
                       width="100%"
                       minSearchStringLength="2"
                       suggestionsLimit="50"
                       asyncSearchDelayMs="300"
                       inputPrompt="msg://msgPressEnter"
                       required="true">
    <actions>
        <action id="lookup" type="picker_lookup"/>
        <action id="open" type="picker_open" icon="EDIT_ACTION"/>
    </actions>
    <query entityClass="com.company.hunttech.entity.Company"
           escapeValueForLike="true"
           searchStringFormat="%$searchString%">
        select e from hunttech_Company e
        where lower(e.comanyName) like lower(:searchString) escape '\'
        order by e.comanyName, e.companyShortName
    </query>
</suggestionPickerField>
```

Текущий контракт:

| Параметр | Значение |
|---|---|
| Минимальная длина | 2 символа |
| Лимит подсказок | 50 |
| Задержка поиска | 300 мс |
| Тип поиска | по любой части строки: `%строка%` |
| Действия | `lookup`, `open` |
| Связь | `jobCandidateDc.currentCompany` |

Следовательно, отдельная замена `LookupPickerField` на `SuggestionPickerField` больше не требуется: она уже выполнена в текущей ветке.

### 2.2 В XML остаётся полный loader компаний

В data-секции экрана по-прежнему объявлены:

```xml
<collection id="currentCompaniesDc"
            class="com.company.hunttech.entity.Company">
    <view extends="company-picker-view"/>
    <loader id="currentCompaniesLc" cacheable="true">
        <query>
            select e from hunttech_Company e order by e.comanyName
        </query>
    </loader>
</collection>
```

`currentCompanyField` не содержит `optionsContainer="currentCompaniesDc"`. Поэтому полный контейнер не требуется для отображения suggestion-результатов.

### 2.3 Loader не защищён от автоматической загрузки

Контроллер помечен:

```java
@LoadDataBeforeShow
```

В `onInit()` блокируется автоматическая загрузка следующих loaders:

- `openPositionDl`;
- `citiesDl`;
- `personPositionsLc`;
- `lastProjectDl`;
- `suggestOpenPositionDl`.

`currentCompaniesLc` в список `preventAutoLoadUntilReady(...)` не включён.

Статический анализ даёт высоковероятную гипотезу:

> `currentCompaniesLc` остаётся доступным для автоматической загрузки механизмом `@LoadDataBeforeShow` и может выполнять полный запрос компаний при каждом initial open формы.

Это пока гипотеза. Она должна быть подтверждена либо опровергнута runtime-логами Hermes.

### 2.4 Остаточные Java-зависимости

В контроллере остаются injections:

```java
@Inject
private CollectionLoader<Company> currentCompaniesLc;

@Inject
private CollectionContainer<Company> currentCompaniesDc;
```

До следующего этапа Hermes обязан найти все фактические обращения к ним. Особое внимание:

- обработчику создания новой компании;
- возврату из `CompanyEdit`;
- добавлению созданной компании в контейнер;
- повторному открытию редактора компании;
- listeners и actions `currentCompanyField`.

Удаление контейнера без карты зависимостей запрещено.

## 3. Исторические показатели для сравнения

Предыдущий локальный аудит HRM HuntTech зафиксировал:

| Метрика | Значение |
|---|---:|
| Количество компаний в полном справочнике | около 5 623 |
| SQL полного списка компаний | около 7,6 мс |
| Характер нагрузки | материализация тысяч entity и передача в web/UI тяжелее самого SQL |

Последний аудит текущей ветки после Stages 1–3 зафиксировал:

| Метрика | Значение |
|---|---:|
| Server-side lifecycle | около 78 мс |
| Совокупное SQL-время initial open | менее 15 мс |
| Vaadin UIDL scripting | около 1,26 с |
| Style recalculation | около 305 мс, примерно 3 139 перерасчётов |

Эти значения являются исторической опорой, но не доказывают наличие или отсутствие `currentCompaniesLc` в initial open. Требуется отдельная трассировка loader ID и SQL.

## 4. Обязательный runtime baseline для Hermes

### 4.1 Подготовка

```bash
git fetch origin --prune
git show origin/coordination/active-work:.ai/active-work.yml

git switch agent/job-candidate-progressive-loading-stage-3-social-networks
git pull --ff-only origin agent/job-candidate-progressive-loading-stage-3-social-networks

git status --short --branch
git rev-parse HEAD
git rev-parse origin/agent/job-candidate-progressive-loading-stage-3-social-networks
```

Перед тестированием должны совпасть:

```text
local HEAD = origin branch HEAD = work.head_sha
```

Незакоммиченные изменения являются блокером. Не применять `stash`, `reset --hard`, force push или переключение на параллельную ветку.

### 4.2 Тестовые сценарии

Проверить три сценария:

1. тяжёлый существующий кандидат, использованный в предыдущем аудите (`ee1cd239...`);
2. существующий кандидат без выбранной компании;
3. создание нового кандидата.

Для каждого сценария:

- один прогревочный запуск;
- пять измерительных запусков;
- одинаковая JVM и локальная БД;
- без открытия выпадающего списка компании;
- без ввода текста в `currentCompanyField`;
- без перехода на другие вкладки до фиксации initial open.

### 4.3 Что логировать

Обязательно разделить:

```text
Screen construction
InitEvent
LoadDataBeforeShow
BeforeShowEvent
AfterShowEvent
first UIDL response
```

Для каждого запуска записать:

- полное время открытия формы;
- server-side lifecycle;
- число SQL-запросов;
- суммарное SQL-время;
- наличие запроса:

```sql
select e
from hunttech_Company e
order by e.comanyName
```

- число возвращённых строк Company;
- факт срабатывания loader ID `currentCompaniesLc`;
- объём ответа middleware/web, когда доступен;
- ошибки и предупреждения CUBA/Vaadin.

### 4.4 Контроль suggestion-поиска

После baseline initial open отдельно проверить ввод в поле:

| Ввод | Ожидаемое поведение |
|---|---|
| 0–1 символ | suggestion SQL не выполняется |
| 2 символа | выполняется поиск `%строка%` |
| 3 и более | выполняется поиск `%строка%` |
| любой запрос | возвращается не более 50 сущностей |

Зафиксировать SQL и время для строк:

```text
ян
сб
ооо
тех
```

На этапе 1 запрос, лимит и параметры не менять.

## 5. Формат результатов

Итоговая таблица:

| Сценарий | Run | Initial open, мс | Server lifecycle, мс | SQL count | SQL total, мс | `currentCompaniesLc` | Company rows |
|---|---:|---:|---:|---:|---:|---|---:|
| тяжёлый кандидат | warm-up |  |  |  |  | YES/NO |  |
| тяжёлый кандидат | 1 |  |  |  |  | YES/NO |  |
| тяжёлый кандидат | 2 |  |  |  |  | YES/NO |  |
| тяжёлый кандидат | 3 |  |  |  |  | YES/NO |  |
| тяжёлый кандидат | 4 |  |  |  |  | YES/NO |  |
| тяжёлый кандидат | 5 |  |  |  |  | YES/NO |  |

Отдельно вычислить:

- MIN;
- MAX;
- AVG;
- P50;
- P95.

## 6. Критерии завершения этапа 1

Этап считается завершённым только когда:

- подтверждён точный branch HEAD;
- зафиксировано, выполняется ли `currentCompaniesLc` при initial open;
- указано реальное количество загружаемых Company;
- выполнено пять измерений после прогрева;
- измерены P50 и P95;
- suggestion SQL отделён от initial-open SQL;
- исходный код, XML и БД не изменялись;
- результат сохранён рядом с этим документом отдельным Hermes-отчётом.

Допустимые вердикты:

```text
FULL_COMPANY_LOADER_CONFIRMED
FULL_COMPANY_LOADER_NOT_TRIGGERED
BASELINE_BLOCKED
```

## 7. Решение для следующего этапа

### При `FULL_COMPANY_LOADER_CONFIRMED`

Следующий этап должен:

1. построить карту Java/XML-зависимостей `currentCompaniesLc/currentCompaniesDc`;
2. сохранить сценарии lookup/open/create;
3. исключить полный loader из initial open;
4. после проверки удалить неиспользуемый контейнер либо оставить только минимальную совместимость;
5. повторить те же замеры до/после.

### При `FULL_COMPANY_LOADER_NOT_TRIGGERED`

Не заявлять ускорение от удаления loader без доказательств. Следующий этап должен сосредоточиться на:

- стоимости substring-поиска `%строка%`;
- необходимости повысить `minSearchStringLength` с 2 до 3;
- уменьшении `suggestionsLimit` с 50 до 30;
- сравнении prefix-поиска `строка%` с текущим contains-поиском;
- сохранении lookup/open/create без изменения бизнес-логики.

## 8. Статус ChatGPT

Статический baseline выполнен. Обнаружено, что рекомендуемый `SuggestionPickerField` уже присутствует в текущей рабочей ветке, но старый полный loader компаний не удалён и не защищён в `onInit()`.

Runtime-замеры в среде HRM HuntTech не выполнялись ChatGPT, поскольку локальная рабочая копия, PostgreSQL и Tomcat пользователя недоступны из текущего окружения. Фактическое подтверждение передано Hermes как обязательная read-only проверка.
