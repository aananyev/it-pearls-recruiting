# OpenPositionReestrBrowse — Спецификация экрана реестра вакансий (Split-View)

> **Экран**: Реестр вакансий Split-View (Master-Detail)  
> **Контроллер**: `hunttech_OpenPositionReestrBrowse` (`OpenPositionReestrBrowse.java`)  
> **Дескриптор**: `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-reestr-browse.xml`  
> **Базовая концепция**: `JobCandidateReestr` (двухпанельный Layout, контрастный сайдбар 312px, адаптивная таблица, единый тулбар быстрых фильтров)  
> **Тема оформления**: CUBA Halo / HuntTech Hover / Modern Dark / Modern Light  

---

## 1. Бизнес-контекст и назначение (Business & Context)

### 1.1 Назначение (What & Why)
`OpenPositionReestrBrowse` — современный флагманский рабочий экран рекрутера и руководителя найма для работы с портфелем открытых и архивных позиций.
Экран объединяет богатую функциональность классического `OpenPositionBrowse` (дерево вакансий, подписки, светофор приоритетов, статусы, рейтинги, печатные формы, управление взаимодействиями) с эргономичной двухпанельной компоновкой **Split-View** в стиле `JobCandidateReestr`:
- **Левая панель (312px)**: Профильный интерактивный сайдбар выбранной вакансии (крупный логотип проекта/компании с фолбеком, ключевые реквизиты, статус готовности вакансии, быстрые действия, воронка кандидатов и стек технологий).
- **Правая панель**: Просторная рабочая область с адаптивным тулбаром быстрых действий, многофункциональной выпадающей кнопкой фильтрации позиций, фильтром по меткам, строкой поиска и деревом/таблицей вакансий.

### 1.2 Связи в интерфейсе и навигация
- **Главное меню**: Пункт «Реестр вакансий» (`font-icon:COMPASS`).
- **Связанные экраны и фрагменты**:
  - `hunttech_OpenPosition.edit` — полное редактирование вакансии.
  - `hunttech_SmartOpenPositionUploadScreen` — диалог «Умное создание вакансии» (AI-парсинг файла/текста и привязка проекта).
  - `hunttech_OpenPositionCommentsView` / `OpenPositionCommentEdit` — отзывы и рейтинг позиции.
  - `hunttech_RecrutiesTasksGroupSubscribeBrowse` — групповая подписка рекрутеров.
  - `hunttech_SuggestJobCandidate` — умный AI-подбор кандидатов под требования позиции.
  - `hunttech_JobCandidateSimpleBrowse` — список кандидатов, прикрепленных к позиции.
  - `hunttech_QuickViewOpenPositionDescription` — быстрый просмотр описания позиции.

---

## 2. Архитектура и UI/UX Концепция (Дизайн-проект)

```
+----------------------------------------------------------------------------------------------------+
|  TOP TOOLBAR (edit-toolbar): Заголовок "Реестр вакансий" + CUBA Generic Filter                     |
+------------------------------------+---------------------------------------------------------------+
| САЙДБАР ВАКАНСИИ (312px)           | РАБОЧАЯ ОБЛАСТЬ РЕЕСТРА (expand=1)                            |
| (edit-sidebar / candidate-sidebar) |                                                               |
|                                    | ТУЛБАР (tableFilterBar):                                      |
| +--------------------------------+ | [Создать] [Умная вакансия 🪄] [Быстрая загрузка ▾]            |
| | [ Логотип Проекта / Компании ] | | [Редактировать] [Удалить]                                     |
| |          (120x120px)           | | [Все вакансии ▾] [Фильтр по меткам ▾] [Метки ▾] [Действия ▾] |
| |                                | +-------------------------------------------------------------+
| | Название позиции (+30% bold)   | ТАБЛИЦА / TREE-GRID ВАКАНСИЙ:                                 |
| | Проект: SSP "Актирование..."   | > Название вакансии | Проект | Заказчик | Город | ЗП | Статус   |
| | Компания: ООО "Сбытовая комп." | ------------------------------------------------------------- |
| | Локация: Москва / Удаленно     |   [●] Java Senior Developer | SSP... | ООО... | РФ | 300к | Открыта |
| +--------------------------------+ |   [●] React Frontend Lead   | Банк.. | ПАО... | Мск| 350к | Открыта |
|                                    |   [○] QA Automation Python  | Финтех | ООО... | СПБ| 220к | Пауза   |
| БЫСТРЫЕ ДЕЙСТВИЯ:                  |                                                               |
| [ Открыть карточку вакансии ]      |                                                               |
| [ Подобрать кандидатов (AI) ]      |                                                               |
| [ Подписаться на вакансию ]        |                                                               |
| [ Закрыть / Открыть позицию ▾]     |                                                               |
|                                    |                                                               |
| 1. УСЛОВИЯ И РЕКВИЗИТЫ             |                                                               |
| • Зарплатная вилка: 250k - 350k ₽  |                                                               |
| • Опыт: От 3 до 6 лет (Senior)     |                                                               |
| • Формат: Удаленная работа         |                                                               |
| • Позиций: 2 шт. (Тип: Аутстафф)   |                                                               |
|                                    |                                                               |
| 2. ПРОЕКТ И ЗАКАЗЧИК               |                                                               |
| • Проект: SSP Лейсан Шестаковой    |                                                               |
| • Куратор: Шестакова Ляйсан        |                                                               |
| • Владелец: Кожевникова Ольга      |                                                               |
|                                    |                                                               |
| 3. ИНДИКАТОРЫ ГОТОВНОСТИ           |                                                               |
| [✓] Описание [✓] Тестовое [✓] Инфо |                                                               |
| Рейтинг: ★★★★☆ (4.5 / 8 отзывов)   |                                                               |
|                                    |                                                               |
| 4. ТРЕБУЕМЫЕ НАВЫКИ                |                                                               |
| [Java 17] [Spring Boot] [Kafka]... |                                                               |
+------------------------------------+---------------------------------------------------------------+
```

---

## 3. Детализация UI-компонентов

### 3.1 Левый сайдбар вакансии (`openPositionDetailPane` - 312px)
1. **Шапка визуальной идентичности (`openPositionProfileHeader`)**:
   - `ovaFallbackImage` (`id="projectLogoPic"`, 120x120px, oval, fallback: `icons/no-company.png` / `icons/project-default.png`) — отображает официальный логотип проекта или компании-заказчика.
   - `label` (`id="detailVacancyName"`, стиль `edit-sidebar-title h2 candidate-sidebar-fullname`) — крупное контрастное наименование позиции с автопереносом строк.
   - `label` (`id="detailProjectName"`, стиль `edit-sidebar-subtitle h4 candidate-sidebar-position`) — наименование связанного проекта.
   - `label` (`id="detailCompanyName"`, стиль `edit-help candidate-sidebar-city`) — наименование компании-клиента.
   - `label` (`id="detailLocationAndFormat"`, стиль `edit-help`) — локация и формат работы (например: `Москва / Remote`).

2. **Панель быстрых действий (`sidebarActionsBox`)**:
   - Кнопка **«Открыть карточку»** (`id="openEditCardBtn"`, icon `EDIT_ACTION`, stylename `primary`, width `100%`).
   - Кнопка **«Подобрать кандидатов»** (`id="suggestCandidatesBtn"`, icon `font-icon:MAGIC`, stylename `secondary`, width `100%`).
   - Кнопка **«Подписаться»** (`id="subscribeBtn"`, icon `font-icon:BELL`, stylename `secondary`, width `100%`).
   - Popup-кнопка **«Статус позиции»** (`id="changeStatusPopupButton"`, icon `CHECK_CIRCLE`, stylename `secondary`, width `100%`) с действиями: «Закрыть позицию», «Закрыть с комментарием», «Приостановить», «Возобновить».

3. **Секция «Условия и реквизиты» (`label-navigation`)**:
   - Заголовок `УСЛОВИЯ И РЕКВИЗИТЫ` (`label-nav-title job-candidate-section-title`).
   - Сетка `detailsGrid`:
     - Зарплата: `label id="detailSalary"` (вилка min-max).
     - Грейд / Опыт: `label id="detailExperience"`.
     - Формат: `label id="detailRemoteWork"`.
     - Количество мест: `label id="detailNumberPositions"`.
     - Тип вакансии: `label id="detailPositionType"`.

4. **Секция «Проект и ответственные» (`label-navigation`)**:
   - Заголовок `ПРОЕКТ И ЗАКАЗЧИК`.
   - Проект: `label id="detailProjectTitle"`.
   - Ответственный со стороны заказчика: `label id="detailClientContact"`.
   - Ведущий рекрутер / Владелец: `label id="detailOwnerName"`.

5. **Секция «Статус и готовность» (`label-navigation`)**:
   - Заголовок `ИНДИКАТОРЫ ГОТОВНОСТИ`.
   - Наличие материалов: Описание, Тестовое задание, Памятка кандидату, Шаблон письма.
   - Рейтинг и отзывы: `label id="detailRatingStars"`.

6. **Секция «Требуемые навыки» (`label-navigation`)**:
   - Заголовок `ТРЕБУЕМЫЕ НАВЫКИ`.
   - Чипсы навыков `detailSkillsLabels` (стиль `candidate-skills-chips`).

---

### 3.2 Верхний тулбар рабочей области (`tableFilterBar`)

Содержит сгруппированные элементы управления:
1. **Группа создания и загрузки (Primary Actions)**:
   - `createPositionBtn`: «Создать вакансию» (`icon="CREATE_ACTION"`, `stylename="primary candidate-btn"`).
   - `smartUploadPositionBtn`: «Умная вакансия» (`icon="font-icon:MAGIC"`, `stylename="primary candidate-btn candidate-smartload-btn"`).
   - `quickLoadPosition`: Popup-кнопка «Быстрая загрузка» (`icon="CLOUD_UPLOAD"`) со списком источников:
     - `smartLoad`: «Умная AI-загрузка описания вакансии...» (`font-icon:MAGIC`)
     - `loadFromPdf`: «Из файла PDF / DOCX...» (`FILE_PDF_O`)
     - `loadFromClipboard`: «Из буфера обмена / текста...» (`CLIPBOARD`)
2. **Группа действий над выбранной позицией**:
   - `editPositionToolbarBtn`: «Редактировать» (`EDIT_ACTION`, `secondary candidate-btn`).
   - `removePositionToolbarBtn`: «Удалить» (`REMOVE_ACTION`, `secondary candidate-btn`).
3. **Выпадающая кнопка фильтрации позиций (`positionsFilterPopupButton`)**:
   - Заголовок и иконка динамически меняются:
     - **Все вакансии** (`USERS` / `LIST`) — полный каталог.
     - **Мои вакансии** (`USER`) — вакансии, где текущий пользователь является владельцем (`owner`).
     - **Мои подписки** (`font-icon:BELL` / `STAR`) — вакансии с активной подпиской рекрутера.
     - **Срочные вакансии** (`font-icon:FLASH` / `EXCLAMATION_TRIANGLE`) — позиции с наивысшим приоритетом.
     - **Только открытые** (`CHECK_CIRCLE`) — исключая закрытые и архивные.
4. **Фильтры по значкам и меткам**:
   - `signFilterButton`: «Фильтр по меткам» (`FILTER`).
   - `signIconsButton`: «Метки» (`TAGS`).
5. **Группа отчетов и действий**:
   - `actionsPopupButton`: «Действия» (`GEARS`).
   - `totalCountLabel`: «Вакансий: 54» (`candidate-count-badge bold`).

---

## 4. Сохранение и преемственность бизнес-логики

В `OpenPositionReestrBrowse` полностью сохраняется функциональность классического `OpenPositionBrowse`:
1. **Иерархия дерева вакансий (`TreeDataGrid` / `parentOpenPosition`)**:
   - Поддержка дочерних позиций в составе мастер-вакансии.
   - Проверка зависимостей при закрытии (запрет закрытия родительской позиции при незакрытых дочерних).
2. **Управление подписками (`RecrutiesTasks`)**:
   - Индивидуальная и групповая подписка рекрутеров на вакансии.
3. **Рейтинги и комментарии (`OpenPositionComment`)**:
   - Просмотр средней оценки, ленты комментариев и добавление экспертного отзыва.
4. **Светофор приоритетов и статусы**:
   - Цветовая дифференциация строк и бейджей по критичности, зарплатным ожиданиям и датам обновления.
5. **View Integrity**:
   - Все геттеры и вложенные ассоциации (`projectName`, `projectDepartment.companyName`, `projectOwner`, `cities`, `positionType`, `owner`) строго задекларированы в XML View контейнера `openPositionsDc`.
