# Project Edit (`hunttech_Project.edit`)

> **ВНИМАНИЕ (2026-08-12):** форма отрефакторена по контракту Edit-форм (sidebar 270px, label-навигация «Разделы», карточки edit-card, полноэкранный модальный режим). Актуальная спецификация: [docs/ui/ProjectEdit_Spec.md](../../ui/ProjectEdit_Spec.md). Этот legacy-файл сохранён для истории.

> Сущность: [Project.md](../../entities/project/Project.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран проекта в HRM HuntTech объединяет реквизиты заказчика и проекта, описание, шаблон письма и связанные вакансии. Он используется для ведения проектного контекста рекрутинга и управления жизненным циклом вакансий проекта.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из `hunttech_Project.browse` действиями создания и редактирования проекта. Из вкладки «Вакансии» пользователь просматривает позиции, связанные с текущим проектом; справочники подразделений, владельцев и родительских проектов выбираются через lookup-компоненты.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие формы → проект существует → загружаются основные данные и справочники, а запрос связанных вакансий блокируется до установки параметра проекта.
- Первое открытие вкладки «Вакансии» → проект сохранён → loader получает параметр `project`, выполняет JPQL и показывает вакансии проекта.
- Изменение признака «Проект закрыт» → есть открытые вакансии → форма предлагает закрыть их и блокирует редактирование ключевых реквизитов проекта.
- Сохранение проекта → изменился статус открытия или закрытия → HRM HuntTech публикует системное уведомление.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_Project.edit` |
| **Java-класс** | `com.company.hunttech.web.screens.project.ProjectEdit ` |
| **XML-дескриптор** | `project-edit.xml` |
| **messagesPack** | `com.company.hunttech.web.screens.project` |
| **Базовый класс** | `StandardEditor` |
| **Lookup-компонент** | `` |
| **EditedEntityContainer** | `projectDc` |
| **focusComponent** | `projectNameField` |
| **Меню** | `web-menu.xml` → `screen="hunttech_Project.edit"` (если есть пункт) |
| **Загрузка данных** | `@LoadDataBeforeShow`; `projectOpenPositionsDl` защищён `PreLoadListener` и загружается отложенно |

### Назначение

Экран редактирует проект, его организационный контекст, коммуникационные ссылки, описание, шаблон письма и связанные вакансии.

---

## 2. Связь с моделью данных (Data & Entity Binding)

| Параметр | Значение |
|----------|----------|
| **Entity** | `Project` |
| **View** | `project-edit-view` |
| **Data containers** | `projectDc` (instance), `projectOpenPositionsDc` (collection), `projectTreeDc` (collection), `projectDepartmentsDc` (collection), `projectOwnersDc` (collection) |
| **Loader** | `projectOpenPositionsDl` |

### JPQL (если задан)

```jpql
select e from hunttech_OpenPosition e
where e.projectName = :project
order by e.createTs desc
```

Обязательный параметр `:project` устанавливается методом `loadOpenPositions()` непосредственно перед вызовом `projectOpenPositionsDl.load()`. `PreLoadListener`, зарегистрированный в `onInit()`, отменяет автоматическую загрузку этого loader до установки параметра и предотвращает ошибку `Query argument project not found`.

### Привязки property (form / table)

- `projectIsClosed`
- `defaultProject`
- `projectTree`
- `projectName`
- `startProjectDate`
- `endProjectDate`
- `projectDepartment`
- `projectOwner`
- `generalChat`
- `chatForCV`
- `projectLogo`
- `projectDescription`
- `openClose`
- `vacansyName`
- `numberPosition`
- `positionType`
- `createTs`
- `templateLetter`

### Колонки таблицы (browse)

- `openClose`
- `vacansyName`
- `numberPosition`
- `positionType`

---

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

| Связь | Экран / фрагмент | Способ открытия |
|-------|------------------|-----------------|
| Родитель | `hunttech_Project.browse` | действия create / edit |
| Парный экран | `hunttech_Project.browse` | создание или редактирование выбранного проекта |
| Lookup targets | справочники проекта, подразделения и владельца | `picker_lookup` |
| Дочерние данные | `projectOpenPositionsDc` | вкладка `tabVacansy` текущей формы |

---

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Жизненный цикл

1. `@LoadDataBeforeShow` запускает стандартную загрузку данных формы и справочников.
2. В `onInit()` для `projectOpenPositionsDl` регистрируется `PreLoadListener`. До установки флага готовности listener вызывает `preventLoad()`, поэтому JPQL с обязательным `:project` не выполняется при общей автозагрузке.
3. При первом выборе `tabProjectDescription` или `tabTemplateLetter` контроллер отдельно перечитывает соответствующее LOB-поле через минимальный динамический view.
4. При первом выборе `tabVacansy` метод `loadOpenPositions()` задаёт `projectOpenPositionsDl.setParameter("project", getEditedEntity())`, разрешает загрузку и выполняет loader один раз.
5. Для нового, ещё не сохранённого проекта вкладка вакансий не выполняет запрос, поскольку связь с проектом ещё не может существовать.

### 4.2 Скрытые вычисления

Кэшируются признаки первой загрузки описания, шаблона письма и вакансий. Контроллер также формирует preview логотипа, управляет ссылками чатов и загружает отдельный список открытых вакансий для сценария закрытия проекта.

### 4.3 Валидация и сохранение

Перед сохранением нормализуется флаг `projectIsClosed`. При изменении статуса открытия или закрытия проекта публикуется глобальное уведомление. Закрытие проекта устанавливает дату окончания и может запустить закрытие связанных открытых вакансий после подтверждения пользователя.

### 4.4 Обработка загружаемого логотипа (автоматическая)

Загрузка логотипа через `projectLogoFileUpload` перехватывается кастомным загрузчиком `WebProjectLogoFileUploadField` (зарегистрирован в `cuba-ui-component.xml` под именем `upload` — XML формы не менялся). Обработка выполняется **только** для полей, привязанных к свойству `projectLogo`:

1. Конвертация в PNG (любой растровый формат на входе).
2. Если сторона больше 300px — пропорциональное уменьшение до 300px.
3. Удаление белого фона (flood-fill от краёв, порог 235) — белые элементы внутри логотипа сохраняются.
4. Вписывание в круг: квадратный канвас со стороной, равной диагонали логотипа (с запасом 5%), логотип центрируется — при отображении в круглом аватаре `ovaFallbackImage` углы не обрезаются.

Обработанный файл записывается в файловое хранилище, дескриптор получает расширение `png`. При ошибке обработки (например, загружен не-растровый файл) сохраняется исходный файл. Настройки — `HunttechProjectLogoConfig` (`hunttech.projectLogo.*`). Сервис: `ProjectLogoImageProcessingService`.

---

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Элемент | Цепочка |
|---------|---------|
| Вкладка «Описание проекта» | Первое открытие → точечная загрузка `projectDescription` → повторные переходы не выполняют запрос |
| Вкладка «Шаблон письма» | Первое открытие → точечная загрузка `templateLetter` → повторные переходы не выполняют запрос |
| Вкладка «Вакансии» | Сохранённый проект + первое открытие → установка `:project` → загрузка `projectOpenPositionsDl` |
| «Проект закрыт» | Включение → установка даты окончания и блокировка ключевых полей → при наличии открытых вакансий показывается диалог закрытия |
| Ссылки чатов | URL заполнен → ссылка активна; URL отсутствует → ссылка отключена |

---

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Структура layout

- Корневой layout разворачивает `projectTab` на доступную область формы.
- `tabProject` содержит основные реквизиты, ссылки чатов и блок логотипа.
- `tabProjectDescription` содержит `projectDescriptionRichTextArea`.
- `tabVacansy` содержит `projectOpenPositionTable`.
- `tabTemplateLetter` содержит `templateLetterRichTextArea`.

### Стили и сообщения

| Элемент | Источник |
|---------|----------|
| Caption | `msg://` / `mainMsg://` из `com.company.hunttech.web.screens.project` |
| Иконка окна | атрибут `icon="ARCHIVE"` в XML |

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-12 | Автоматическая обработка загружаемого логотипа проекта (PNG, ресайз ≤300px, удаление белого фона, вписывание в круг) через кастомный загрузчик `WebProjectLogoFileUploadField` + `ProjectLogoImageProcessingService`; XML формы и Java-логика экрана не менялись |
| 2026-07-14 | Исправлена отложенная загрузка связанных вакансий: обязательный параметр `project` устанавливается до выполнения JPQL, преждевременная автозагрузка блокируется `PreLoadListener` |
| 2026-06-26 | §4–5: поведение из Java простым языком (batch modernization) |
| 2026-06-26 | Business & Context Intro (Living Documentation standard) |
| 2026-06-26 | Первая версия UI Spec (автогенерация из XML/Java) |
