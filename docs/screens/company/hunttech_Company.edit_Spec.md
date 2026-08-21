# Company Edit (`hunttech_Company.edit`)

> Сущность: [Company.md](../../entities/company/Company.md)
>
> ⚠️ **Актуальная каноническая версия:** [docs/ui/CompanyEdit_Spec.md](../../ui/CompanyEdit_Spec.md) (2026-08-14, рефакторинг по контракту Edit-форм). Этот legacy-документ сохранён для истории; технические разделы ниже актуализированы по состоянию на 2026-08-14.

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Company** HRM HuntTech: редактирование записи сущности `Company`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `hunttech_Company.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Карточка клиента/работодателя. В списке — фильтры «только наши» и «только юрлицо»; в форме при смене города подставляются регион и страна; описание и логотип подгружаются пакетно для ускорения таблицы.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_Company.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.company.CompanyEdit ` |
| **XML-дескриптор** | `company-edit.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.company` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `companyDc` |
| **focusComponent** | `companyOwnershipField` |
| **dialogMode** | `height="100%" width="100%" modal="true"` (контракт §5.3, с 2026-08-14) |
| **Меню** | `web-menu.xml` → `screen="hunttech_Company.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Company** HRM HuntTech: редактирование записи сущности `Company`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Company` |
| **View** | `company-edit-view` |
| **Data containers** | `companyDc` (instance), `departmentOfCompanyDc` (collection), `companyOwnershipsDc` (collection), `companyDirectorsDc` (collection), `companyGroupDc` (collection), `cityOfCompaniesDc` (collection), `regionOfCompaniesDc` (collection), `countryOfCompaniesDc` (collection) |
| **Loader** | `companyOwnershipsLc` |

### JPQL (если задан)

```
select e from hunttech_Ownershup e
```

### Привязки property (form / table)

- `departmentOfCompany`
- `ourLegalEntity`
- `ourClient`
- `companyOwnership`
- `comanyName`
- `companyShortName`
- `legalEntityName`
- `companyGroup`
- `companyDirector`
- `cityOfCompany`
- `regionOfCompany`
- `countryOfCompany`
- `addressOfCompany`
- `fileCompanyLogo`
- `companyDescription`
- `workingConditions`
- `inn`
- `kpp`
- `ogrn`
- `okpo`
- `oktmo`
- `okved`
- `legalAddress`
- `actualAddress`
- `postalAddress`
- `bik`
- `bankName`
- `settlementAccount`
- `correspondentAccount`
- `phone`
- `email`
- `website`
- `departamentRuName`
- `departamentDirector`
- `departamentHrDirector`

### Колонки таблицы (browse)

- `departamentRuName`
- `departamentDirector`
- `departamentHrDirector`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `hunttech_Company.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |
| Smart Upload | `SmartCompanyRequisitesUploadScreen` | `smartUploadRequisitesBtn` диалог умной загрузки реквизитов |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл формы (Lifecycle)

| Экран | Что происходит при открытии |
|-------|----------------------------|
| Browse | Перед показом применяются фильтры «только наш клиент» / «только юрлицо»; после загрузки списка кэшируются текстовые описания для подсказок в колонках |
| Edit | Для новой записи `ourClient=false`; при первом открытии вкладок лениво подгружаются адрес, описание и департаменты; sidebar-навигация «Разделы» (4 вкладки) синхронизирует активный пункт с вкладкой; title sidebar — наименование компании; логотип — `WebOvaFallbackImage` (авто-fallback `icons/no-company.png`) |

### 4.2 Скрытые вычисления

| Что видит пользователь | Правило |
|------------------------|---------|
| Логотип с подсказкой | HTML-tooltip с описанием компании из кэша |
| Иконки ourClient / ourLegalEntity | Цвет и иконка по флагам записи |
| Каскадный выбор адреса | При выборе города автоматически заполняются регион и страна (на вкладке Основное и Официальные реквизиты) |

### 4.3 Валидация и сохранение

Стандартный commit editor'а; валидация обязательности наименования компании и города.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|---------|
| «Только наш клиент» / «Только юрлицо» | Включение чекбокса → перезагрузка списка с параметром loader |
| Смена города в edit | Выбор города → автозаполнение региона и страны |
| Кнопка «Умная загрузка реквизитов» | Открытие диалога мастера загрузки (из файлов PDF/DOCX/Pages/RTF, текста или URL) → AI-парсинг → автоматическое создание директора в справочнике Люди, автосоздание/привязка гео-справочников (Страна, Регион, Город) и заполнение всех полей реквизитов |

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Двухпанельная компоновка `edit-screen-layout`: sidebar 270px (логотип 176×176 + identity-title + навигация «Разделы» по 4 вкладкам + spacer + hint) и workspace (toolbar + tabSheet `edit-tabs` + footer `edit-footer-actions`)
- Вкладки:
  1. `tabConpanyDetails` (карточки «Общие сведения о компании», «Местонахождение и адрес» и «Контакты»)
  2. `companyRequisitesTab` (панель кнопки «Умная загрузка реквизитов», карточки «Государственная регистрация и коды», «Адрес и местонахождение организации» с гео-разбиением Страна/Регион/Город/Улица, «Банковские реквизиты», «Официальные контакты»)
  3. `companyDescriptionTab` (карточка описания с двумя RichTextArea)
  4. `tabCompanyDepartament` (карточка с dataGrid `departmentOfCompanyTable`)
- Footer: `windowCommitAndClose` (primary) / `windowClose` (secondary), правый нижний угол
- Фильтр: `filter` → `companyOwnershipsLc`
- Таблицы: `departmentOfCompanyTable`

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.hunttech.web.screens.company` |
| Иконка окна | атрибут `icon` в XML (`BUILDING`) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-21 | Добавлена вкладка «Официальные реквизиты», мастер «Умная загрузка реквизитов», разбиение адреса на гео-справочники Страна/Регион/Город/Улица и сквозная sidebar-навигация по 4 вкладкам |
| 2026-08-14 | Сверка с эталоном (контракт §3.1/§3.6): пункты навигации `height: auto`, wrap-правило удалено, на одноблочных вкладках `label-navigation` скрывается (`TABS_WITH_SIDEBAR_NAVIGATION`) |
| 2026-08-14 | Рефакторинг по контракту Edit-форм (эталон ProjectEdit): sidebar 270px, логотип `ovaFallbackImage` 176×176, навигация «Разделы», карточки `edit-card`+`showAsPanel`, footer primary/secondary, `dialogMode` 100%×100% modal; канон — [docs/ui/CompanyEdit_Spec.md](../../ui/CompanyEdit_Spec.md) |
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
