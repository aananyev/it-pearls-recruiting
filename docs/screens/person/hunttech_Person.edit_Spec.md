# Person Edit (`hunttech_Person.edit`)

> Сущность: [Person.md](../../entities/person/Person.md)
> Канонический UI Spec: [PersonEdit_Spec.md](../../ui/PersonEdit_Spec.md) (рефакторинг 2026-08-14 по контракту Edit-экранов)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран подсистемы **Person** HRM HuntTech: редактирование записи сущности `Person`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Контроллер `hunttech_Person.edit`; навигация и дочерние формы — §3 «Иерархия и взаимосвязь форм».

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Справочник персон (контакты). В списке — миниатюра фото с подсказкой ФИО; в форме — переключение placeholder и загруженного фото.


---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_Person.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.person.PersonEdit ` |
| **XML-дескриптор** | `person-edit.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.person` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `personDc` |
| **focusComponent** | `firstNameField` |
| **Меню** | `web-menu.xml` → `screen="hunttech_Person.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow` |

### Назначение

Экран подсистемы **Person** HRM HuntTech: редактирование записи сущности `Person`.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Person` |
| **View** | `person-edit-view` |
| **Data containers** | `personDc` (instance), `positionCityDc` (collection), `positionCountriesDc` (collection), `personPositionsDc` (collection) |
| **Loader** | `positionCityLc` |

### JPQL (если задан)

```
select e from hunttech_City e order by e.cityRuName
```

### Привязки property (form / table)

- `firstName`
- `middleName`
- `secondName`
- `companyDepartment`
- `birdhDate`
- `email`
- `phone`
- `mobPhone`
- `skypeName`
- `telegramName`
- `wiberName`
- `watsupName`
- `cityOfResidence`
- `positionCountry`
- `personPosition`
- `fileImageFace`

### Колонки таблицы (browse)

- см. XML

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `web-menu.xml` / opener | menu / lookup |
| Парный экран | `hunttech_Person.browse` | create / edit action |
| Lookup targets | picker_lookup на FK-полях | `screenBuilders.lookup()` |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

Browse: только генератор колонки аватара. Edit: `onAfterShow` — при отсутствии `fileImageFace` fallback-аватар `applyFallback` (ovaFallbackImage 176×176); upload/clear обновляет preview; label-навигация sidebar «Разделы» переводит фокус к первому полю раздела и подсвечивает активный пункт (presentation-only).

### 4.2 Скрытые вычисления

Миниатюра 20px, fallback `no-programmer.jpeg`, HTML-tooltip с ФИО.

### 4.3 Валидация и сохранение

Стандартный commit.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| CRUD | Стандартный CUBA |


---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout: `person-editor` (namespace) + `edit-screen-layout` — двухпанельная композиция sidebar 270px + workspace (эталон SkillTreeEdit/гео-форм, контракт Edit-экранов)
- Sidebar (`edit-sidebar`): фото `ovaFallbackImage` 176×176 + upload (dropZone) → title по центру (без подписи типа записи) → label-навигация «Разделы» (3 пункта, полоса-заголовок) → spacer → hint
- Workspace (`edit-workspace`): toolbar («Карточка человека» + описание) → scrollBox → карточки `edit-card` («Основные данные», «Контакты», «Местоположение и должность»; поля `edit-form-control`) → footer `edit-footer-actions` (primary/secondary)

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Локальный SCSS | `person-editor.scss` (7 тем, идентичны; sidebar #172638, аватар 176px, nav 27px, карточки 8px, footer 40px) |
| Caption | `msg://` / `mainMsg://` из `com.company.hunttech.web.screens.person` |
| Иконка окна | атрибут `icon` в XML (`USER`) |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-14 | Рефакторинг по контракту Edit-экранов (эталон SkillTreeEdit/гео-форм): sidebar 270px + ovaFallbackImage 176×176 + upload, title по центру, label-навигация «Разделы», карточки, footer primary/secondary; актуализированы §1/§4/§6, канон — PersonEdit_Spec.md |
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
