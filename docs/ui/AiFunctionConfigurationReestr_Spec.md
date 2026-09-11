# AiFunctionConfigurationReestr (Спецификация экранной формы «Реестр функций AI»)

## Роли разработки
- **Аналитик**: предпроектная аналитика, спецификация атрибутов сущности и бизнес-логики навигации.
- **UI/UX-дизайнер**: проектирование двухпанельной компоновки Split-View (312px сайдбар), типографики, статусных индикаторов и адаптивности.

---

## 1. Назначение и бизнес-смысл (What & Why)

Экран **«Реестр функций AI»** (`hunttech_AiFunctionConfiguration.reestr`) предназначен для удобного администрирования, мониторинга и быстрого анализа бизнес-функций искусственного интеллекта в HRM HuntTech.
В отличие от базового табличного Browse, Реестр предоставляет современный двухпанельный интерфейс **Split-View Master-Detail**:
- Мгновенный детальный обзор параметров функции в фиксированном левом сайдбаре (312px) при смене строки в таблице.
- Исключение необходимости открывать модальный редактор для просмотра технических реквизитов, лимитов токенов, температуры, используемой модели и политики маршрутизации.
- Быстрые действия: переход в редактор, копирование machine code функции в буфер, фильтрация журнала вызовов (`AiCallLog`) по выбранной функции.

---

## 2. Навигация и размещение в меню

- **Родительский пункт**: Главное меню $\rightarrow$ «Искусственный интеллект» (`menu_config.hunttech_AiMenu`).
- **Идентификатор экрана**: `hunttech_AiFunctionConfiguration.reestr`.
- **Контроллер**: `com.company.hunttech.web.screens.aifunctionconfiguration.AiFunctionConfigurationReestr`.
- **XML-дескриптор**: `com/company/hunttech/web/screens/aifunctionconfiguration/ai-function-configuration-reestr.xml`.
- **Иконка**: `TASKS` / `SLIDERS`.
- **Связанные экраны**:
  - `hunttech_AiFunctionConfiguration.edit` (редактор функции);
  - `hunttech_AiCallLog.browse` (журнал вызовов AI с фильтрацией по функции).

---

## 3. Архитектура данных и Data View Integrity

- **Контейнер**: `collection id="aiFunctionsDc" class="com.company.hunttech.entity.ai.AiFunctionConfiguration" view="ai-function-configuration-browse-view"`.
- **Загрузчик**: `loader id="aiFunctionsDl"`.
- **JPQL**: `select e from hunttech_AiFunctionConfiguration e order by e.name asc`.
- **Data View Integrity**: Представление `ai-function-configuration-browse-view` содержит все используемые в UI атрибуты:
  `code`, `name`, `description`, `capability`, `privacyPolicyVersion`, `temperature`, `maxTokens`, `defaultMonthlyTokenQuota`, `adminConfiguration` (с `admin-ai-configuration-browse-view`), `adminModelName`, `executionPolicy`, `fallbackPolicy`, `allowModelOverride`, `active`, `includeUserContext`, `configurationVersion`.
  Большие prompt LOB (`systemPrompt`, `promptTemplate`) и секреты исключены из выборки для максимальной производительности Zero N+1.

---

## 4. Компоновка интерфейса (Split-View 312px)

### Левый сайдбар (312px, `edit-sidebar`, `scrollBox id="detailScroll"`)
1. **Шапка профиля (`edit-sidebar-visual`, `TOP_CENTER`)**:
   - `ovaFallbackImage id="detailPic"` (120×120px, `fallbackThemePath="icons/ai/ai-function-configuration.png"`, `scaleMode="SCALE_DOWN"`).
   - 4-уровневая типографика (`edit-sidebar-identity`):
     - Уровень 1 (`h2 bold`): Наименование функции (`name`).
     - Уровень 2 (`h4 bold`): Machine code функции (`code`).
     - Уровень 3 (`edit-help bold`): Возможность (`capability`: TEXT, CHAT, EMBEDDING, IMAGE).
     - Уровень 4 (`edit-help`): Статус функции («🟢 Активна» / «⚪ Отключена»).
2. **Панель быстрых действий (`edit-sidebar-summary`)**:
   - Кнопка `editBtn`: «Открыть карточку» (`icon="EDIT_ACTION"`, `stylename="primary"`).
   - Кнопка `copyCodeBtn`: «Копировать код» (`icon="COPY"`).
   - Кнопка `callLogsBtn`: «Журнал вызовов» (`icon="HISTORY"`).
3. **Секция «Маршрутизация и исполнение» (`label-navigation`)**:
   - Двухколоночный `grid`:
     - Политика: `executionPolicy` (ADMIN_ONLY, USER_ONLY, HYBRID).
     - Fallback: `fallbackPolicy` (FAIL_FAST, ADMIN_FALLBACK).
     - Подключение: `adminConfiguration.name` (NVL: «Не привязано»).
     - Модель: `adminModelName` (NVL: «По умолчанию»).
     - Пользовательский контекст: `includeUserContext` (Да / Нет).
     - Переопределение: `allowModelOverride` (Разрешено / Запрещено).
4. **Секция «Параметры LLM и лимиты» (`label-navigation`)**:
   - Температура: `temperature` (0.0 - 2.0).
   - Максимум токенов: `maxTokens`.
   - Квота в месяц: `defaultMonthlyTokenQuota` (с форматированием тыс. токенов).
   - Версия конфигурации: `configurationVersion`.
5. **Секция «Описание и назначение» (`label-navigation`)**:
   - Текст `detailDescription` с поддержкой переносов строк.

### Правая рабочая область
1. **Командный тулбар (`lookup-toolbar`)**:
   - Кнопки `create`, `edit`, `remove`.
   - Кнопка `refresh` (Обновить).
   - Кнопка `toggleActiveBtn` (Быстрое переключение активности).
2. **Generic Filter (`collapsable="true" collapsed="true"`)**:
   - Стандартный фильтр CUBA над загрузчиком `aiFunctionsDl`.
3. **DataGrid (`aiFunctionsTable`, 100% width, expand)**:
   - Колонки: `activeStatus` (иконка/бейдж), `code`, `name`, `capability`, `executionPolicy`, `adminConfiguration`, `adminModelName`, `maxTokens`, `defaultMonthlyTokenQuota`.
   - Компактный пагинатор `rowsCount` (24px).
