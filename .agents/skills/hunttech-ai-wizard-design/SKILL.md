---
name: hunttech-ai-wizard-design
description: >-
  Стандарт и руководство по проектированию и разработке мастеров умного ввода и автозаполнения (Smart AI Wizards)
  в HRM HuntTech (CUBA Platform / Vaadin / AI Control Plane): архитектурная схема 4 уровней (DTO, Core Service,
  Seed-миграция AI-функции, Web-диалог 960x760px с выбором кандидатов и предпросмотром), DataContext integrity,
  автотесты и валидация через Alibaba OCR.
---

# Стандарт и руководство по разработке умных мастеров автозаполнения (HuntTech AI Wizard Design)

Данный документ фиксирует проверенный стандарт проектирования, реализации, сидирования AI-функций, XML-компоновки и валидации для создания мастеров умного автозаполнения (Smart AI Wizards) в системе **HRM HuntTech** (например, *Умная загрузка резюме*, *Умная загрузка вакансии*, *Мастер умного поиска и автозаполнения компании*).

---

## 1. Архитектурная схема 4 уровней (End-to-End Pipeline)

Каждый умный мастер в HRM HuntTech строится строго по 4-уровневой сквозной архитектуре:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ 1. GLOBAL TIER (DTO & Service Interface)                                              │
│    • DTO: ParsedData (Serializable, POJO с геттерами/сеттерами)                        │
│    • Service Interface: MyEntitySearchAiService (String NAME = "hunttech_...")          │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 2. CORE TIER (Business Logic, AI Execution, Registry Seed)                             │
│    • Core Bean: MyEntitySearchAiServiceBean implements MyEntitySearchAiService         │
│    • Интеграция с AiExecutionService (вызов функции + fallback)                        │
│    • Безопасный парсинг JSON (обработка arrays, candidates, items, data, flat)         │
│    • Seed-миграции AI-функции (SQL + Liquibase XML c CDATA, dbms, preConditions)       │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 3. CLIENT TIER (Web Spring Proxy & Wizard UI)                                          │
│    • Web Remote Proxy: регистрация <entry> в web-spring.xml                            │
│    • UI Screen Dialog (960×760px, modal, scrollBox, tabSheet, candidate selection)    │
│    • Интерактивный предпросмотр (previewGrid) + проверка дубликатов в БД               │
│    • Интеграция в форму редактирования EntityEdit (тулбар, кнопка MAGIC, DataContext)  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ 4. QUALITY ASSURANCE & VERIFICATION                                                    │
│    • Контрактный тест мастера (*ContractTest.java)                                     │
│    • Архитектурный тест регистрации сервисов (WebServiceProxyRegistryContractTest)    │
│    • Обязательный Code Review через Alibaba OCR (`ocr review --audience agent`)        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Пошаговый чеклист разработки визарда

| № | Этап | Что необходимо сделать | Обязательные требования |
|---|------|------------------------|-------------------------|
| **1** | **DTO** | Создать/расширить `MyEntityParsedData` в `modules/global/` | Поля сущности, описание деятельности, условия, сниппеты, геттеры/сеттеры |
| **2** | **Interface** | Создать `MyEntitySearchAiService` в `modules/global/` | Константа `NAME = "hunttech_..."`, константа `FUNCTION_...`, методы поиска и применения |
| **3** | **Core Service** | Создать `MyEntitySearchAiServiceBean` в `modules/core/` | `@Service(NAME)`, вызов `aiExecutionService.executeText`, fallback, безопасный парсинг JSON |
| **4** | **AI Seed SQL** | Создать SQL-миграцию в `modules/core/db/update/postgres/26/` | Вставка в `HUNTTECH_AI_FUNCTION_CONFIGURATION` с системным промптом и шаблоном |
| **5** | **Liquibase XML**| Создать XML в `modules/core/db/changelog/` + подключить в `db.changelog-master.xml` | `dbms="postgresql"`, `<preConditions onFail="HALT">`, блок `<![CDATA[ ... ]]>` |
| **6** | **web-spring.xml**| Добавить `<entry key="..." value="..."/>` в `modules/web/src/.../web-spring.xml` | В секцию `cuba_WebRemoteProxyBeanCreator` (`remoteServices`) |
| **7** | **XML Визарда** | Создать/обновить `smart-...-upload-screen.xml` | 4 вкладки (AI поиск, Файл, Текст, Ссылка), `candidatesCard`, `previewCard`, `duplicateBox` |
| **8** | **Java Визарда**| Создать/обновить `Smart...UploadScreen.java` | `BackgroundTask` (асинхронность), динамический список кандидатов, `setInitialSearchParams` |
| **9** | **Интеграция** | Добавить кнопку вызова в `EntityEdit.xml` и `EntityEdit.java` | Кнопка `font-icon:MAGIC`, применение данных через `dataContext.merge(...)` |
| **10**| **Тесты и OCR** | Создать автотест, запустить сборку и Alibaba OCR review | Пройти `ocr review --audience agent`, коммит, push, локальный деплой |

---

## 3. Стандарт компоновки UI визарда (`960×760px`)

Визард открывается как модальный диалог `OpenMode.DIALOG` и содержит 3 логических шага в вертикальном скролл-контейнере:

### 3.1. Шаг 1: Зона ввода и поиска (4 вкладки)
- **Вкладка 1: Поиск в интернете (AI)** — основная вкладка:
  - Текстовые поля параметров поиска (например, Наименование/бренд + ИНН / Ключевые слова).
  - Кнопка «Найти в интернете через AI» (`font-icon:MAGIC`, стиль `primary candidate-btn candidate-smartload-btn`).
  - Индикатор выполнения (`progressBar` indeterminate + `statusLabel`).
- **Вкладка 2: Загрузка файла (drag-and-drop)** — загрузка PDF/Word/RTF/TXT с автоматическим извлечением текста через `Tika`/парсеры.
- **Вкладка 3: Вставка текста** — `richTextArea` для копирования текста из буфера обмена.
- **Вкладка 4: Загрузка по ссылке** — ввод URL сайта/страницы с автоматическим выкачиванием HTML через `Jsoup`.

### 3.2. Шаг 2: Выбор из нескольких найденных кандидатов (`candidatesCard`)
- Если AI находит несколько организаций/вариантов, отображается блок `candidatesCard` со списком карточек вариантов.
- Каждая карточка варианта содержит:
  - Торговое и юридическое наименование;
  - Бейджи ключевых реквизитов (ИНН, ОГРН, Город, Руководитель);
  - Краткий сниппет описания;
  - Кнопку выбора («Выбрать» / «✓ Выбрано»).
- При клике на карточку выбранный вариант мгновенно загружается в предпросмотр (Шаг 3).

### 3.3. Шаг 3: Детальный предпросмотр и проверка дубликатов (`previewCard` + `duplicateBox`)
- **Проверка дубликатов**: при совпадении уникальных ключей (ИНН, СНИЛС, Email, Название) отображается желтый предупреждающий блок с информацией о найденной записи в БД.
- **Сетка предпросмотра (`previewGrid`)**: все извлеченные поля (реквизиты, контакты, адреса, гео-структура, директор, описание деятельности и условия).
- **Кнопка подтверждения (`applyBtn`)**: «Применить к карточке» (`icon="CHECK"`, `primary`).

---

## 4. Стандарт сидирования AI-функций в базе данных

### 4.1. SQL-миграция (`modules/core/db/update/postgres/26/26xxxx-x-addMyAiFunction.sql`)
```sql
INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
    ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
    CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
    ADMIN_CONFIGURATION_ID,
    EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
    IS_ACTIVE, CONFIGURATION_VERSION
)
SELECT
    'uuid-v4-here'::uuid,
    1,
    CURRENT_TIMESTAMP,
    'migration',
    'MY_AI_FUNCTION_PARSE_JSON',
    'Понятное название AI-функции (JSON)',
    'Подробное описание назначения AI-функции',
    'TEXT_ANALYSIS',
    'Системный промпт с четкой структурой JSON и правилами...',
    E'Запрос:\n${param1}\n${sourceText}',
    0.2,
    4000,
    (SELECT ID FROM HUNTTECH_ADMIN_AI_CONFIGURATION WHERE IS_ACTIVE = TRUE ORDER BY PRIORITY_ DESC LIMIT 1),
    'USER_OVERRIDE_ALLOWED',
    'FALLBACK_TO_ADMIN',
    FALSE,
    TRUE,
    1
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
     WHERE CODE = 'MY_AI_FUNCTION_PARSE_JSON'
);
```

### 4.2. Liquibase XML (`modules/core/db/changelog/26xxxx-x-addMyAiFunction.xml`)
> [!IMPORTANT]
> Всегда указывайте `dbms="postgresql"`, `<preConditions onFail="HALT">` и оборачивайте SQL в `<![CDATA[ ... ]]>` во избежание подстановки свойств `${...}` парсером Liquibase.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                      http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <changeSet id="26xxxx-x-addMyAiFunction" author="antigravity">
        <preConditions onFail="HALT">
            <tableExists tableName="HUNTTECH_AI_FUNCTION_CONFIGURATION"/>
        </preConditions>
        <comment>Seed AI-функции MY_AI_FUNCTION_PARSE_JSON</comment>
        <sql dbms="postgresql"><![CDATA[
            INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION ( ... )
            SELECT ...
            WHERE NOT EXISTS ( ... );
        ]]></sql>
    </changeSet>
</databaseChangeLog>
```

---

## 5. Правила бизнес-логики и защиты данных (Data Integrity)

1. **Честный Fallback (Запрет фабрикации данных)**:
   - Если AI-сервис не вернул данные, формируется черновой кандидат **строго** на основе введенных пользователем данных.
   - **Категорически запрещено** генерировать случайные ИНН, ОГРН, фальшивые адреса, телефоны и имена директоров. Поля, которые не были получены, остаются `null` или пустыми.
2. **Безопасный парсинг JSON**:
   - Поддерживать парсинг плоского объекта `{...}`, корневого массива `[...]`, а также вложенных массивов/объектов `candidates`, `items`, `data`.
   - Обязательно проверять `if (item != null && item.isObject())` перед обращением к свойствам узла.
3. **Data View Integrity и Detached Objects**:
   - Связанные справочные сущности (`Person`, `Ownershup`, `City`, `Region`, `Country`, `Grade`, `PositionType`), найденные или созданные в middleware, в вызывающей Edit-форме обязательно привязываются через `dataContext.merge(appliedEntity)`:
     ```java
     if (applied.getCompanyDirector() != null) {
         target.setCompanyDirector(dataContext.merge(applied.getCompanyDirector()));
     }
     ```
   - Загрузчики опций (`*Lc.load()`) перезагружаются только при фактическом изменении связанных записей.
4. **Безопасная асинхронная загрузка медиа-ресурсов (Логотипы, Аватары)**:
   - Загрузка изображений по URL выполняется строго асинхронно через `BackgroundTask` (не блокируя UI).
   - Защита от SSRF и DNS-rebinding: проверка публичного IP адреса (`InetAddress`), отклонение приватных диапазонов (`127.0.0.0/8`, `10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `169.254.0.0/16`), соединение с фиксацией IP.
   - Ограничение размера (макс. 5 МБ) и валидация подлинности растрового изображения через `ImageIO.read` и сигнатуры magic bytes (PNG, JPEG, WebP, GIF).
   - В UI формах редактирования оставлять ровно 1 заметную кнопку вызова мастера в тулбаре вверху экрана.

---

## 6. Обязательные автотесты и валидация через Alibaba OCR

Перед каждым коммитом и деплоем обязательно запускаются:

1. **Контрактный тест визарда (`MyWizardContractTest.java`)**:
   - Проверяет наличие AI-констант, полей DTO, элементов XML-диалога, кнопок в родительской Edit-форме и сид-миграции в БД.
2. **Архитектурный тест (`WebServiceProxyRegistryContractTest.java`)**:
   - Проверяет, что все `@Inject` сервисы в `modules/web/` зарегистрированы в `web-spring.xml`.
3. **Alibaba OCR Code Review**:
   ```bash
   ocr review --audience agent
   ```
   - Проверяет diff на чистоту, отсутствие NPE, корректность SQL/Liquibase атрибутов и отсутствие ложных плейсхолдеров.
   - Деплой и коммит выполняются только при `0 finding(s)`.
