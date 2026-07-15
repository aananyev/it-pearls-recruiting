# Hermes Report: Baseline загрузчика компаний

**Repository:** aananyev/it-pearls-recruiting  
**Branch:** agent/job-candidate-progressive-loading-stage-3-social-networks  
**SHA:** 84af3294d082f43ed9ce337d850dd06d8fb7b9e1  
**Date:** 2026-07-15  

## Вердикт

**FULL_COMPANY_LOADER_CONFIRMED**

## Доказательство

### 1. `@LoadDataBeforeShow` + отсутствие в preventAutoLoadUntilReady

Контроллер помечен `@LoadDataBeforeShow` (строка 57 `JobCandidateEdit.java`).  
В `onInit()` (строка 1288) блокируются автозагрузки:

```java
preventAutoLoadUntilReady(openPositionDl, ...);
preventAutoLoadUntilReady(citiesDl, ...);
preventAutoLoadUntilReady(personPositionsLc, ...);
preventAutoLoadUntilReady(lastProjectDl, ...);
preventAutoLoadUntilReady(suggestOpenPositionDl, ...);
```

**`currentCompaniesLc` в список НЕ включён.**

Механизм `@LoadDataBeforeShow` CUBA гарантирует: все `CollectionLoader`, не приостановленные через `preventAutoLoadUntilReady`, выполняются до `BeforeShowEvent`.

### 2. XML-декларация loader

```xml
<collection id="currentCompaniesDc" class="com.company.hunttech.entity.Company">
    <view extends="company-picker-view"/>
    <loader id="currentCompaniesLc" cacheable="true">
        <query>
            select e from hunttech_Company e order by e.comanyName
        </query>
    </loader>
</collection>
```

Loader загружает **полный список** всех компаний (≈5 623 строки), сортировка по `comanyName`.

### 3. Java-зависимости currentCompaniesDc

Единственное использование контейнера — в методе возврата из создания компании (строки 1892-1895):

```java
if (currentCompaniesDc != null && !currentCompaniesDc.containsItem(mergedCompany)) {
    currentCompaniesDc.getMutableItems().add(mergedCompany);
} else if (currentCompaniesDc != null) {
    currentCompaniesDc.replaceItem(mergedCompany);
}
```

Сценарии **lookup** и **open** НЕ используют контейнер — они работают через `picker_lookup`/`picker_open` actions поля.

## Карта зависимостей

| Ресурс | Используется | Для чего | Можно удалить? |
|--------|:------------:|----------|:--------------:|
| `currentCompaniesLc` | ✅ auto-load | Полный список компаний при initial open | ✅ — добавить в preventAutoLoadUntilReady |
| `currentCompaniesDc` | ✅ create flow | `containsItem/add/replaceItem` после создания компании | ✅ — заменить на DataManager |
| `currentCompanyField` | ✅ suggestion | Поиск по `%строка%`, лимит 50 | ❌ — основной функционал |
| `currentCompanyField` actions lookup/open | ✅ | Выбор/открытие компании | ❌ — сохранить |

## Suggestion-поиск

| Параметр | Значение |
|----------|----------|
| Минимальная длина | 2 символа |
| Лимит | 50 |
| Задержка | 300 мс |
| Поиск | `%строка%` (contains) |

Ввод 0–1 символа: SQL **не** выполняется.  
Ввод 2+ символов: выполняется `SELECT ... FROM HUNTTECH_COMPANY WHERE LOWER(COMANY_NAME) LIKE LOWER('%строка%') ORDER BY COMANY_NAME, COMPANY_SHORT_NAME LIMIT 50`.

## Рекомендация для Stage 2

1. Добавить `currentCompaniesLc` в `preventAutoLoadUntilReady()` в `onInit()`
2. Заменить `currentCompaniesDc.containsItem/add/replaceItem` на работу через `DataManager`
3. Удалить `currentCompaniesDc` и `currentCompaniesLc` из XML и Java-injections
4. Сохранить lookup/open actions поля и suggestion-поиск
5. Выполнить замеры до/после

Ожидаемый эффект: исключение материализации ≈5 623 Company-сущностей из initial open, снижение server-side lifecycle с ~78 мс.
