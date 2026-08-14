# Country Edit (`hunttech_Country.edit`)

> Сущность: [Country.md](../../entities/country/Country.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Country** HRM HuntTech: редактирование записи сущности `Country`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `hunttech_Country.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник стран. Стандартный browse/edit без кастомной Java-логики.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_Country.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.country.CountryEdit ` |
| **XML-дескриптор** | `country-edit.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.country` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `countryDc` |
| **focusComponent** | `form` |
| **Меню** | `web-menu.xml` → `screen="hunttech_Country.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Country** HRM HuntTech: редактирование записи сущности `Country`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Country` |
| **View** | `country-edit-view` |
| **Data containers** | `countryDc` (instance), `countryCountryOfRegionsDc` (collection) |
| **Loader** | `` |

### JPQL (если задан)

```

```

### Привязки property (form / table)

- `countryOfRegion`
- `countryRuName`
- `countryShortName`
- `phoneCode`

### Колонки таблицы (browse)

- `regionRuName`
- `regionCode`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `hunttech_Country.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Стандартный CUBA.

### 4.2–4.3

Нет скрытых вычислений; стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Стандартный CUBA |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Двухпанельная композиция по контракту Edit-экранов (канон: [GeolocationEditForms_Spec.md](../../ui/GeolocationEditForms_Spec.md)):
  - корневой layout `country-editor` (`edit-screen-layout`), sidebar слева + рабочая область справа;
  - sidebar (`edit-sidebar` 270px, `#172638`): visual-блок с круглой иллюстрацией `ovalImage` 176×176, identity (`edit-sidebar-title` — название страны из `countryDc`, по центру по горизонтали; подпись типа записи убрана), label-навигация (`label-navigation` → «Разделы», пункты 27px), spacer, hint; контент имеет стандартные отступы от краёв (`padding: 14px 16px 12px`, правая граница и тень — эталон ProjectEdit);
  - рабочая область (`edit-workspace`): toolbar, scrollBox с карточками `edit-card` (`showAsPanel="true"`), footer `edit-footer-actions`;
- Footer: кнопки ОК/Отмена прижаты к правому нижнему углу (expand-спейсер + `align="MIDDLE_RIGHT"`), стили primary/secondary как у формы Project (`country-editor-primary-action`/`country-editor-secondary-action`, 40px/14px/600/radius 4px).

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.hunttech.web.screens.country` |
| Иконка окна | атрибут `icon` в XML (если задан) |
| Локальный SCSS | `geolocation-edit-forms.scss` (7 тем, md5-идентичны) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-14 | §6: CountryEdit sidebar доведён до эталона ProjectEdit — стандартные отступы контента sidebar (`padding: 14px 16px 12px` + граница + тень), подпись «Страна» удалена, название страны по центру по горизонтали, footer-кнопки ОК/Отмена в правом нижнем углу со стилем primary/secondary формы Project; тест `GeolocationEditFormsContractTest` дополнен |
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
