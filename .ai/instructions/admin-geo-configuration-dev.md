# Промпт разработки: AdminGeoConfiguration — управление гео-провайдерами в настройках системы

## Цель
Создать полноценный модуль настройки гео-провайдеров (htmlweb, VK, GeoNames, Oxilor) в системных настройках HRM HuntTech по аналогии с `AdminAiConfiguration` и `ApplicationSetup`.

**Требование:** разработка **обязательно** сопровождается субагентами:
- 🏗 **Архитектор** — проектирование схемы БД, API, интеграции
- ☕ **Java-разработчик** — реализация Entity, Service, Provider, Security
- 🎨 **UI/UX-дизайнер** — проектирование и верстка Browse/Edit форм
- 🧪 **QA-инженер** — автотесты на каждом этапе

---

## Этапы разработки (каждый — отдельный коммит + PR)

### Этап 1: Архитектура и БД (Архитектор + Java)

#### 1.1 Сущность `AdminGeoConfiguration`
**Файл:** `modules/global/src/com/company/hunttech/entity/geo/AdminGeoConfiguration.java`

```java
@SystemLevel
@Table(name = "HUNTTECH_ADMIN_GEO_CONFIGURATION", indexes = {
    @Index(name = "IDX_ADMIN_GEO_PROVIDER", columnList = "PROVIDER_CODE"),
    @Index(name = "IDX_ADMIN_GEO_ACTIVE", columnList = "IS_ACTIVE"),
    @Index(name = "IDX_ADMIN_GEO_PRIMARY", columnList = "IS_PRIMARY")
})
@Entity(name = "hunttech_AdminGeoConfiguration")
@NamePattern("%s|name")
public class AdminGeoConfiguration extends StandardEntity {
    
    @NotNull @Column(name = "NAME", nullable = false, length = 128)
    private String name;                    // "htmlweb.ru", "VK API", "GeoNames"
    
    @NotNull @Column(name = "PROVIDER_CODE", nullable = false, length = 64)
    private String providerCode;            // "htmlweb", "vk", "geonames", "oxilor"
    
    @Column(name = "API_KEY_ENCRYPTED", length = 4096)
    private String apiKeyEncrypted;         // зашифрованный через AiSecretService
    
    @Column(name = "BASE_API_URL", length = 512)
    private String baseApiUrl;              // опциональное переопределение
    
    @Column(name = "USERNAME", length = 128)
    private String username;                // для GeoNames
    
    @Column(name = "EXTRA_PARAMS", columnDefinition = "jsonb")
    private String extraParams;             // JSON: {"timeout": 5000, "retries": 3}
    
    @Column(name = "IS_ACTIVE")
    private Boolean active = true;
    
    @Column(name = "IS_PRIMARY")
    private Boolean primary = false;        // только одна запись может быть primary
    
    @Column(name = "PRIORITY_")
    private Integer priority = 0;           // порядок fallback
    
    @Column(name = "LAST_TEST_STATUS", length = 32)
    private String lastTestStatus;          // SUCCESS, ERROR, TIMEOUT, NOT_TESTED
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "LAST_TEST_AT")
    private Date lastTestAt;
    
    @Column(name = "LAST_ERROR", length = 2000)
    private String lastError;
    
    // getters/setters...
}
```

#### 1.2 Миграция БД
**Файл:** `modules/core/db/update/postgres/26/260824-1-adminGeoConfiguration.sql`

```sql
create table HUNTTECH_ADMIN_GEO_CONFIGURATION (
    ID uuid not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME varchar(128) not null,
    PROVIDER_CODE varchar(64) not null,
    API_KEY_ENCRYPTED varchar(4096),
    BASE_API_URL varchar(512),
    USERNAME varchar(128),
    EXTRA_PARAMS jsonb,
    IS_ACTIVE boolean default true,
    IS_PRIMARY boolean default false,
    PRIORITY_ integer default 0,
    LAST_TEST_STATUS varchar(32),
    LAST_TEST_AT timestamp,
    LAST_ERROR varchar(2000),
    --
    primary key (ID)
);

create index IDX_ADMIN_GEO_PROVIDER on HUNTTECH_ADMIN_GEO_CONFIGURATION (PROVIDER_CODE);
create index IDX_ADMIN_GEO_ACTIVE on HUNTTECH_ADMIN_GEO_CONFIGURATION (IS_ACTIVE);
create index IDX_ADMIN_GEO_PRIMARY on HUNTTECH_ADMIN_GEO_CONFIGURATION (IS_PRIMARY);

-- Уникальность primary: только одна активная primary-запись
create unique index IDX_ADMIN_GEO_PRIMARY_UNIQUE 
    on HUNTTECH_ADMIN_GEO_CONFIGURATION (IS_PRIMARY) 
    where IS_PRIMARY = true and DELETE_TS is null;

-- Seed данные (если таблица пуста)
insert into HUNTTECH_ADMIN_GEO_CONFIGURATION 
(ID, VERSION, CREATE_TS, CREATED_BY, NAME, PROVIDER_CODE, BASE_API_URL, IS_ACTIVE, IS_PRIMARY, PRIORITY_, LAST_TEST_STATUS)
select gen_random_uuid(), 1, now(), 'system', 
       'htmlweb.ru', 'htmlweb', 'http://htmlweb.ru/geo/api.php', true, true, 100, 'NOT_TESTED'
where not exists (select 1 from HUNTTECH_ADMIN_GEO_CONFIGURATION);

insert into HUNTTECH_ADMIN_GEO_CONFIGURATION 
(ID, VERSION, CREATE_TS, CREATED_BY, NAME, PROVIDER_CODE, BASE_API_URL, IS_ACTIVE, PRIORITY_, LAST_TEST_STATUS)
select gen_random_uuid(), 1, now(), 'system', 
       'VK API', 'vk', 'https://api.vk.com/method/database.', true, 50, 'NOT_TESTED'
where not exists (select 1 from HUNTTECH_ADMIN_GEO_CONFIGURATION where PROVIDER_CODE = 'vk');

insert into HUNTTECH_ADMIN_GEO_CONFIGURATION 
(ID, VERSION, CREATE_TS, CREATED_BY, NAME, PROVIDER_CODE, BASE_API_URL, IS_ACTIVE, PRIORITY_, LAST_TEST_STATUS)
select gen_random_uuid(), 1, now(), 'system', 
       'GeoNames', 'geonames', 'http://api.geonames.org/', true, 10, 'NOT_TESTED'
where not exists (select 1 from HUNTTECH_ADMIN_GEO_CONFIGURATION where PROVIDER_CODE = 'geonames');
```

#### 1.3 Регистрация в persistence.xml + views.xml
- Добавить `<class>` в `persistence.xml`
- View: `adminGeoConfiguration-view`, `adminGeoConfiguration-browse-view`

---

### Этап 2: Сервисный слой (Java-разработчик + Архитектор)

#### 2.1 Интерфейс сервиса
**Файл:** `modules/global/src/com/company/hunttech/service/geo/AdminGeoConfigurationService.java`

```java
public interface AdminGeoConfigurationService {
    String NAME = "hunttech_AdminGeoConfigurationService";
    
    List<AdminGeoConfiguration> getActiveConfigurations();  // по priority desc
    AdminGeoConfiguration getPrimaryConfiguration();
    AdminGeoConfiguration getByProviderCode(String providerCode);
    AdminGeoConfiguration saveWithEncryption(AdminGeoConfiguration config, String rawApiKey);
    void testConnection(AdminGeoConfiguration config);
    void setPrimary(AdminGeoConfiguration config);  // сбрасывает primary у остальных
}
```

#### 2.2 Реализация
**Файл:** `modules/core/src/com/company/hunttech/service/geo/AdminGeoConfigurationServiceBean.java`

```java
@Service(AdminGeoConfigurationService.NAME)
public class AdminGeoConfigurationServiceBean implements AdminGeoConfigurationService {
    
    @Autowired private DataManager dataManager;
    @Autowired private AiSecretService secretService;  // шифрование ключей
    @Autowired private Map<String, GeoDataProvider> geoProviders;  // все зарегистрированные
    
    @Override
    @Transactional
    public AdminGeoConfiguration saveWithEncryption(AdminGeoConfiguration config, String rawApiKey) {
        if (rawApiKey != null && !rawApiKey.isEmpty()) {
            config.setApiKeyEncrypted(secretService.encrypt(rawApiKey));
        }
        // Если ставят primary=true — сбросить у остальных
        if (Boolean.TRUE.equals(config.getPrimary())) {
            resetOtherPrimary(config.getId());
        }
        return dataManager.commit(config);
    }
    
    @Override
    public void testConnection(AdminGeoConfiguration config) {
        GeoDataProvider provider = geoProviders.get(config.getProviderCode());
        if (provider == null) {
            config.setLastTestStatus("ERROR");
            config.setLastError("Provider not registered: " + config.getProviderCode());
            config.setLastTestAt(new Date());
            dataManager.commit(config);
            return;
        }
        
        try {
            provider.testConnection(config);  // provider должен принимать config для ключа
            config.setLastTestStatus("SUCCESS");
            config.setLastError(null);
        } catch (Exception e) {
            config.setLastTestStatus("ERROR");
            config.setLastError(e.getMessage());
        }
        config.setLastTestAt(new Date());
        dataManager.commit(config);
    }
    
    private void resetOtherPrimary(Uuid currentId) {
        dataManager.load(AdminGeoConfiguration.class)
            .query("update hunttech_AdminGeoConfiguration e set e.primary = false where e.id != :id and e.primary = true")
            .parameter("id", currentId)
            .executeUpdate();
    }
}
```

#### 2.3 Интеграция в `GeoDataProvider`
Обновить существующие провайдеры (`HtmlwebGeoProvider`, `VkGeoProvider`):
- Инжектить `AdminGeoConfigurationService`
- В `fetchCountries()` брать конфиг через сервис
- Реализовать `testConnection(AdminGeoConfiguration config)`

---

### Этап 3: UI — Browse/Edit экраны (UI/UX-дизайнер + Java)

#### 3.1 Требования UI/UX (эталон: `AdminAiConfiguration` + `ApplicationSetup`)

| Элемент | Спецификация |
|---------|--------------|
| **Browse** | Таблица: Название, Провайдер, Активен, Основной, Приоритет, Статус теста, Последний тест |
| **Edit** | Форма 2 колонки: левая — основные поля, правая — тест подключения |
| **Поля** | Название*, Код провайдера* (select: htmlweb/vk/geonames/oxilor), Base URL, Username, API Key (passwordField), Extra Params (jsonArea), Активен, Основной (radio/checkbox), Приоритет (numberField) |
| **Кнопка "Проверить"** | Вызывает `testConnection()`, показывает спиннер, обновляет статус без перезагрузки |
| **Статус теста** | Цветной бейдж: SUCCESS (green), ERROR (red), TIMEOUT (orange), NOT_TESTED (gray) |
| **Валидация** | Только одна `primary=true` на момент коммита; `providerCode` уникален среди активных |

#### 3.2 Файлы
| Файл | Назначение |
|------|------------|
| `modules/web/src/.../screens/geoconfig/admin-geo-configuration-browse.xml` | Список с действиями |
| `modules/web/src/.../screens/geoconfig/admin-geo-configuration-edit.xml` | Форма редактирования |
| `modules/web/src/.../screens/geoconfig/AdminGeoConfigurationBrowse.java` | Контроллер списка |
| `modules/web/src/.../screens/geoconfig/AdminGeoConfigurationEdit.java` | Контроллер формы + `testConnection` action |
| `modules/web/src/.../messages.properties` | Локализация (RU/EN) |
| `modules/web/src/.../web-menu.xml` | Пункт меню: "Настройки → Гео-провайдеры" |

#### 3.3 Стилизация (по контракту HRM Edit-форм)
- Sidebar 270px: логотип (глобус), название, навигация "Основные / Тест подключения"
- Workspace: toolbar "Настройка гео-провайдера", карточки форм
- Footer: OK/Отмена (primary/secondary стили)
- `stylename="geoconfig-editor"`

---

### Этап 4: Автотесты (QA-инженер — параллельно с каждым этапом)

#### 4.1 Unit-тесты сервиса
**Файл:** `modules/core/test/com/company/hunttech/service/geo/AdminGeoConfigurationServiceTest.java`

```java
@Transactional
public class AdminGeoConfigurationServiceTest extends CoreTest {
    
    @Autowired private AdminGeoConfigurationService service;
    @Autowired private DataManager dataManager;
    
    @Test
    public void testSaveWithEncryption() {
        AdminGeoConfiguration config = metadata.create(AdminGeoConfiguration.class);
        config.setName("Test htmlweb");
        config.setProviderCode("htmlweb");
        config.setActive(true);
        
        AdminGeoConfiguration saved = service.saveWithEncryption(config, "secret-key-123");
        
        assertNotNull(saved.getApiKeyEncrypted());
        assertNotEquals("secret-key-123", saved.getApiKeyEncrypted());
        
        // Проверка расшифровки
        String decrypted = secretService.decrypt(saved.getApiKeyEncrypted());
        assertEquals("secret-key-123", decrypted);
    }
    
    @Test
    public void testOnlyOnePrimary() {
        AdminGeoConfiguration c1 = createConfig("htmlweb", true, 100);
        AdminGeoConfiguration c2 = createConfig("vk", true, 50);
        
        // После сохранения c2 как primary, c1 должен стать primary=false
        AdminGeoConfiguration reloadedC1 = dataManager.reload(c1, "adminGeoConfiguration-view");
        assertFalse(reloadedC1.getPrimary());
        assertTrue(dataManager.reload(c2, "adminGeoConfiguration-view").getPrimary());
    }
    
    @Test
    public void testGetActiveConfigurationsOrder() {
        createConfig("htmlweb", true, 100);
        createConfig("vk", true, 50);
        createConfig("geonames", false, 10);  // inactive
        
        List<AdminGeoConfiguration> active = service.getActiveConfigurations();
        assertEquals(2, active.size());
        assertEquals("htmlweb", active.get(0).getProviderCode());  // priority desc
        assertEquals("vk", active.get(1).getProviderCode());
    }
}
```

#### 4.2 Интеграционные тесты провайдеров
**Файл:** `modules/core/test/com/company/hunttech/service/geo/GeoProviderIntegrationTest.java`

```java
@Category(IntegrationTest.class)
public class GeoProviderIntegrationTest extends CoreTest {
    
    @Autowired private AdminGeoConfigurationService geoConfigService;
    @Autowired private Map<String, GeoDataProvider> geoProviders;
    
    @Test
    @Ignore("Requires network & API keys")
    public void testHtmlwebConnection() {
        AdminGeoConfiguration config = geoConfigService.getByProviderCode("htmlweb");
        assumeTrue(config != null && config.getActive());
        
        geoConfigService.testConnection(config);
        
        AdminGeoConfiguration reloaded = dataManager.reload(config, "adminGeoConfiguration-view");
        assertEquals("SUCCESS", reloaded.getLastTestStatus());
        assertNotNull(reloaded.getLastTestAt());
    }
    
    @Test
    @Ignore("Requires network")
    public void testVkConnection() {
        AdminGeoConfiguration config = geoConfigService.getByProviderCode("vk");
        assumeTrue(config != null && config.getActive());
        
        geoConfigService.testConnection(config);
        assertEquals("SUCCESS", dataManager.reload(config, "adminGeoConfiguration-view").getLastTestStatus());
    }
}
```

#### 4.3 UI-тесты (Cuba/Selenide)
**Файл:** `modules/web/test/com/company/hunttech/web/screens/geoconfig/AdminGeoConfigurationEditTest.java`

```java
@Test
public void testEditFormValidation() {
    // Открыть экран создания
    AdminGeoConfigurationEdit edit = openWindow(AdminGeoConfigurationEdit.class, WindowParams.WITHOUT_ENTITY);
    
    // Заполнить обязательные поля
    edit.getNameField().setValue("Test Provider");
    edit.getProviderCodeField().setValue("test");
    edit.getActiveField().setValue(true);
    
    // Проверить валидацию primary
    edit.getPrimaryField().setValue(true);
    edit.commitAndClose();
    
    // Повторить для другого провайдера — должен сброситься первый
    AdminGeoConfigurationEdit edit2 = openWindow(AdminGeoConfigurationEdit.class, WindowParams.WITHOUT_ENTITY);
    edit2.getNameField().setValue("Test Provider 2");
    edit2.getProviderCodeField().setValue("test2");
    edit2.getActiveField().setValue(true);
    edit2.getPrimaryField().setValue(true);
    edit2.commitAndClose();
    
    // Проверить в БД: только второй primary
}
```

#### 4.4 Detached Object тесты (Data View Integrity)
**Файл:** `modules/core/test/com/company/hunttech/core/GeoConfigDetachedObjectTest.java`

```java
public class GeoConfigDetachedObjectTest extends CoreTest {
    
    @Test
    public void testAdminGeoConfigurationView() {
        AdminGeoConfiguration config = createConfig("htmlweb", true, 100);
        config.setApiKeyEncrypted("encrypted");
        config.setExtraParams("{\"timeout\":5000}");
        dataManager.commit(config);
        
        // Загрузка через view — не должно быть Unfetched Attribute Access
        AdminGeoConfiguration loaded = dataManager.load(AdminGeoConfiguration.class)
            .id(config.getId())
            .view("adminGeoConfiguration-view")
            .one();
        
        assertNotNull(loaded.getName());
        assertNotNull(loaded.getProviderCode());
        assertNotNull(loaded.getApiKeyEncrypted());
        assertNotNull(loaded.getExtraParams());
        assertNotNull(loaded.getLastTestStatus());
    }
}
```

---

### Этап 5: Интеграция в существующие провайдеры (Java + Архитектор)

Обновить:
1. `HtmlwebGeoProvider` — чтение конфига через `AdminGeoConfigurationService`
2. `VkGeoProvider` — аналогично
3. Добавить `GeoNamesGeoProvider` (fallback #2)
4. Обновить `GeoDataSyncService` — использовать активные конфигурации

---

## Чек-лист Definition of Done

| Критерий | Этап | Подтверждение |
|----------|------|---------------|
| Миграция применяется на чистой/существующей БД | 1 | `gradle updateDb` без ошибок |
| Entity проходит Detached Object тест | 1 | `GeoConfigDetachedObjectTest` GREEN |
| Сервис шифрует/расшифровывает ключи | 2 | `AdminGeoConfigurationServiceTest` GREEN |
| Только одна primary-запись | 2 | Unit тест `testOnlyOnePrimary` GREEN |
| Browse открывается, данные отображаются | 3 | Ручная проверка |
| Edit: сохранение, валидация, кнопка "Проверить" | 3 | UI тест + ручная |
| Статус теста обновляется асинхронно | 3 | Не перезагружает форму |
| Интеграционные тесты проходят (с ключами) | 4 | `GeoProviderIntegrationTest` GREEN |
| Провайдеры берут ключи из БД | 5 | `GeoDataSyncService` работает |
| Fallback цепочка работает | 5 | Отключить primary → подхватывает следующий |
| Code review через `ocr` | Все | `ocr review --audience agent` PASS |

---

## Команды для работы в Antigravity

```bash
# В worktree
cd /Users/alekseyananyev/StudioProjects/hrm-antigravity

# 1. Архитектор: проектирование (создает .md в .ai/architecture/)
# 2. Java: Entity + миграция + сервис
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:updateDb

# 3. Java: тесты сервиса
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*AdminGeoConfigurationServiceTest*"

# 4. UI/UX: экраны (после утверждения дизайна)
# 5. QA: UI тесты
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-web:test --tests "*AdminGeoConfigurationEditTest*"

# 6. Полный прогон
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test :app-web:test

# 7. Поднять приложение для ручной проверки
bash ../hunttech_recruiting/scripts/start-app.sh --branch "$PWD"

# 8. Code review
ocr review --audience agent

# 9. PR с меткой WAITING_FOR_HERMES
```

---

## Вызов субагентов (обязательно на каждом этапе)

```yaml
# Этап 1 — Архитектор + Java
subagents:
  - role: architect
    task: "Проектировать схему AdminGeoConfiguration: поля, индексы, уникальность primary, JSONB extraParams, интеграция с AiSecretService. Выдать .ai/architecture/admin-geo-config.md"
  - role: java_developer
    task: "Реализовать Entity, миграцию, persistence.xml, views.xml по проекту архитектора. Запустить updateDb и DetachedObjectTest."

# Этап 2 — Java + Архитектор (review)
subagents:
  - role: java_developer
    task: "Реализовать AdminGeoConfigurationServiceBean, интеграцию в GeoDataProvider (htmlweb, vk). Написать unit-тесты."
  - role: architect
    task: "Ревью сервисного слоя: транзакционность, обработка ошибок, fallback-логика, безопасность ключей."

# Этап 3 — UI/UX + Java
subagents:
  - role: ui_ux_designer
    task: "Спроектировать Browse/Edit экраны по контракту HRM Edit-форм. Макет в Figma/Excalidraw. Утвердить с заказчиком."
  - role: java_developer
    task: "Верстать XML экраны, контроллеры, messages.properties, web-menu.xml по утвержденному дизайну. Написать UI-тесты."

# Этап 4 — QA (параллельно всегда)
subagents:
  - role: qa_engineer
    task: "Написать и запустить: unit-тесты сервиса, интеграционные тесты провайдеров, UI-тесты, DetachedObject тесты. Все GREEN перед PR."

# Этап 5 — Java + Архитектор
subagents:
  - role: java_developer
    task: "Добавить GeoNamesGeoProvider, обновить GeoDataSyncService, настроить fallback цепочку через AdminGeoConfigurationService."
  - role: architect
    task: "Финальный аудит: безопасность, производительность, соответствие Data View Integrity, отсутствие circular deps."
```

---

## Связанные документы

- `.ai/instructions/universal-geo-entities.md` — универсальная модель данных
- `.ai/instructions/migration-htmlweb-geo.md` — миграция справочников
- `.cursor/rules/data-view-integrity.mdc` — правило Data View Integrity
- `.ai/instructions/three-agent-git-protocol-2026-08-15.md` — протокол 3 агентов

---

## Ожидаемый результат

После мержа в master:
1. В меню **"Настройки → Гео-провайдеры"** отображается список настроенных провайдеров
2. Можно добавить/отредактировать провайдера, ввести API Key (шифруется), нажать **"Проверить подключение"**
3. Статус теста отображается цветным бейджем с таймстампом
4. В `GeoDataSyncService` синхронизация использует настройки из БД (приоритет, ключи, URL)
5. Fallback работает: если primary недоступен → пробует следующий по priority
6. Все автотесты проходят в CI

---

**Готово к разработке. Начинаем с Этапа 1: вызов субагентов Архитектора и Java-разработчика.**