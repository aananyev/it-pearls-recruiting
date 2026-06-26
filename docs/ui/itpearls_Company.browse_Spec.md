# Company Browse (`itpearls_Company.browse`)

> Сущность: [Company.md](../entities/Company.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Company** HRM HuntTech: список / lookup сущности `Company`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `itpearls_Company.browse`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Карточка клиента/работодателя. В списке — фильтры «только наши» и «только юрлицо»; в форме при смене города подставляются регион и страна; описание и логотип подгружаются пакетно для ускорения таблицы.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `itpearls_Company.browse` |
| **Java-класс** | `com.company.itpearls.web.screens.company.CompanyBrowse ` |
| **XML-дескриптор** | `company-browse.xml` |
| **messagesPack** | `com.company.itpearls.web.screens.company` |
| **Базовый класс** | `StandardLookup` |
| **Lookup-компонент** | `companiesTable` |
| **EditedEntityContainer** | `` |
| **focusComponent** | `companiesTable` |
| **Меню** | `web-menu.xml` → `screen="itpearls_Company.browse"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Company** HRM HuntTech: список / lookup сущности `Company`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Company` |
| **View** | `company-browse-view` |
| **Data containers** | `companiesDc` (collection) |
| **Loader** | `companiesDl` |

### JPQL (если задан)

```
select e from itpearls_Company e
order by e.comanyName
```

### Привязки property (form / table)

- `companyGroup`
- `comanyName`
- `companyShortName`
- `countryOfCompany`
- `regionOfCompany`
- `cityOfCompany`

### Колонки таблицы (browse)

- `companyGroup`
- `companyLogoColumn`
- `ourCompanyIconColumn`
- `ourClientIconColumn`
- `comanyName`
- `companyShortName`
- `countryOfCompany`
- `regionOfCompany`
- `cityOfCompany`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `itpearls_Company.edit` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл формы (Lifecycle)

| Экран | Что происходит при открытии |
|-------|----------------------------|
| Browse | Перед показом применяются фильтры «только наш клиент» / «только юрлицо»; после загрузки списка кэшируются текстовые описания для подсказок в колонках |
| Edit | Для новой записи `ourClient=false`; при первом открытии вкладок лениво подгружаются адрес, описание и департаменты |

### 4.2 Скрытые вычисления

| Что видит пользователь | Правило |
|------------------------|---------|
| Логотип с подсказкой | HTML-tooltip с описанием компании из кэша |
| Иконки ourClient / ourLegalEntity | Цвет и иконка по флагам записи |

### 4.3 Валидация и сохранение

Стандартный commit editor'а; дополнительных BeforeCommit в Java нет.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|---------|
| «Только наш клиент» / «Только юрлицо» | Включение чекбокса → перезагрузка списка с параметром loader |
| Смена города в edit | Выбор города → автозаполнение региона и страны |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `expand` на основную таблицу / форму (`companiesTable`)
- Фильтр: `filter` → `companiesDl`
- Таблицы: `companiesTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.itpearls.web.screens.company` |
| Иконка окна | атрибут `icon` в XML (если задан) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
