# Промпт миграции: Интеграция справочников стран/регионов/городов с htmlweb.ru

## Цель
Расширить сущности `Country`, `Region`, `City` полями из htmlweb.ru API для автоматической синхронизации справочников.

---

## Этап 1: Миграция БД + Entity + Views (БЕЗ UI)

### 1.1 Создать миграцию Liquibase
**Файл:** `modules/core/db/update/postgres/26/260823-1-addHtmlwebGeoFields.sql`

```sql
-- =============================================
-- Country: новые поля из htmlweb.ru
-- =============================================
alter table HUNTTECH_COUNTRY add column if not exists COUNTRY_EN_NAME varchar(100);
alter table HUNTTECH_COUNTRY add column if not exists COUNTRY_ISO3 varchar(3);
alter table HUNTTECH_COUNTRY add column if not exists COUNTRY_NUMERIC_CODE integer;
alter table HUNTTECH_COUNTRY add column if not exists CONTINENT varchar(50);
alter table HUNTTECH_COUNTRY add column if not exists CAPITAL_CITY_ID integer;
alter table HUNTTECH_COUNTRY add column if not exists MCC integer;
alter table HUNTTECH_COUNTRY add column if not exists LANG varchar(100);
alter table HUNTTECH_COUNTRY add column if not exists LANG_CODE varchar(10);
alter table HUNTTECH_COUNTRY add column if not exists HTMLWEB_ID varchar(10);
create index if not exists IDX_COUNTRY_HTMLWEB_ID on HUNTTECH_COUNTRY (HTMLWEB_ID);

-- =============================================
-- Region: новые поля из htmlweb.ru
-- =============================================
alter table HUNTTECH_REGION add column if not exists REGION_EN_NAME varchar(100);
alter table HUNTTECH_REGION add column if not exists FEDERAL_DISTRICT varchar(100);
alter table HUNTTECH_REGION add column if not exists KLADR_CODE varchar(20);
alter table HUNTTECH_REGION add column if not exists REGION_TYPE integer;
alter table HUNTTECH_REGION add column if not exists CAPITAL_CITY_ID integer;
alter table HUNTTECH_REGION add column if not exists HTMLWEB_ID integer;
create index if not exists IDX_REGION_HTMLWEB_ID on HUNTTECH_REGION (HTMLWEB_ID);

-- =============================================
-- City: новые поля из htmlweb.ru
-- =============================================
alter table HUNTTECH_CITY add column if not exists CITY_EN_NAME varchar(100);
alter table HUNTTECH_CITY add column if not exists LATITUDE double precision;
alter table HUNTTECH_CITY add column if not exists LONGITUDE double precision;
alter table HUNTTECH_CITY add column if not exists TIMEZONE varchar(50);
alter table HUNTTECH_CITY add column if not exists DISTRICT_ID integer;
alter table HUNTTECH_CITY add column if not exists SUB_DISTRICT_ID integer;
alter table HUNTTECH_CITY add column if not exists CITY_LEVEL integer;
alter table HUNTTECH_CITY add column if not exists AIRPORT_CODE varchar(10);
alter table HUNTTECH_CITY add column if not exists POSTAL_CODE varchar(20);
alter table HUNTTECH_CITY add column if not exists GEONAME_ID bigint;
alter table HUNTTECH_CITY add column if not exists WIKI_LINK varchar(255);
alter table HUNTTECH_CITY add column if not exists FULL_NAME varchar(255);
alter table HUNTTECH_CITY add column if not exists HTMLWEB_ID integer;
create index if not exists IDX_CITY_HTMLWEB_ID on HUNTTECH_CITY (HTMLWEB_ID);
```

### 1.2 Обновить Entity-классы

**Country.java** — добавить поля + геттеры/сеттеры:
```java
@Column(name = "COUNTRY_EN_NAME", length = 100)
protected String countryEnName;

@Column(name = "COUNTRY_ISO3", length = 3)
protected String countryIso3;

@Column(name = "COUNTRY_NUMERIC_CODE")
protected Integer countryNumericCode;

@Column(name = "CONTINENT", length = 50)
protected String continent;

@Column(name = "CAPITAL_CITY_ID")
protected Integer capitalCityId;

@Column(name = "MCC")
protected Integer mcc;

@Column(name = "LANG", length = 100)
protected String lang;

@Column(name = "LANG_CODE", length = 10)
protected String langCode;

@Column(name = "HTMLWEB_ID", length = 10)
protected String htmlwebId;
```

**Region.java** — добавить:
```java
@Column(name = "REGION_EN_NAME", length = 100)
protected String regionEnName;

@Column(name = "FEDERAL_DISTRICT", length = 100)
protected String federalDistrict;

@Column(name = "KLADR_CODE", length = 20)
protected String kladrCode;

@Column(name = "REGION_TYPE")
protected Integer regionType;

@Column(name = "CAPITAL_CITY_ID")
protected Integer capitalCityId;

@Column(name = "HTMLWEB_ID")
protected Integer htmlwebId;
```

**City.java** — добавить:
```java
@Column(name = "CITY_EN_NAME", length = 100)
protected String cityEnName;

@Column(name = "LATITUDE")
protected Double latitude;

@Column(name = "LONGITUDE")
protected Double longitude;

@Column(name = "TIMEZONE", length = 50)
protected String timezone;

@Column(name = "DISTRICT_ID")
protected Integer districtId;

@Column(name = "SUB_DISTRICT_ID")
protected Integer subDistrictId;

@Column(name = "CITY_LEVEL")
protected Integer cityLevel;

@Column(name = "AIRPORT_CODE", length = 10)
protected String airportCode;

@Column(name = "POSTAL_CODE", length = 20)
protected String postalCode;

@Column(name = "GEONAME_ID")
protected Long geonameId;

@Column(name = "WIKI_LINK", length = 255)
protected String wikiLink;

@Column(name = "FULL_NAME", length = 255)
protected String fullName;

@Column(name = "HTMLWEB_ID")
protected Integer htmlwebId;
```

### 1.3 Обновить views.xml

```xml
<!-- Country -->
<view entity="hunttech_Country" name="country-browse-view" extends="_minimal">
    <property name="countryRuName"/>
    <property name="countryShortName"/>
    <property name="phoneCode"/>
    <property name="fileFlag"/>
    <property name="countryEnName"/>
    <property name="countryIso3"/>
    <property name="htmlwebId"/>
</view>

<view entity="hunttech_Country" name="country-edit-view" extends="_minimal">
    <property name="countryRuName"/>
    <property name="countryShortName"/>
    <property name="phoneCode"/>
    <property name="fileFlag"/>
    <property name="countryEnName"/>
    <property name="countryIso3"/>
    <property name="countryNumericCode"/>
    <property name="continent"/>
    <property name="capitalCityId"/>
    <property name="mcc"/>
    <property name="lang"/>
    <property name="langCode"/>
    <property name="htmlwebId"/>
    <property name="countryOfRegion" view="region-country-child-view"/>
</view>

<!-- Region -->
<view entity="hunttech_Region" name="region-browse-view" extends="_minimal">
    <property name="regionRuName"/>
    <property name="regionCode"/>
    <property name="regionCountry" view="country-picker-view"/>
    <property name="fileRegionEmblem"/>
    <property name="regionEnName"/>
    <property name="federalDistrict"/>
    <property name="regionType"/>
    <property name="htmlwebId"/>
</view>

<view entity="hunttech_Region" name="region-edit-view" extends="_minimal">
    <property name="regionRuName"/>
    <property name="regionCode"/>
    <property name="regionCountry" view="country-picker-view"/>
    <property name="regionOfCity" view="city-region-child-view"/>
    <property name="fileRegionEmblem"/>
    <property name="regionEnName"/>
    <property name="federalDistrict"/>
    <property name="kladrCode"/>
    <property name="regionType"/>
    <property name="capitalCityId"/>
    <property name="htmlwebId"/>
</view>

<!-- City -->
<view entity="hunttech_City" name="city-region-child-view" extends="_minimal">
    <property name="cityRuName"/>
    <property name="cityPhoneCode"/>
    <property name="cityEnName"/>
    <property name="latitude"/>
    <property name="longitude"/>
    <property name="cityLevel"/>
    <property name="htmlwebId"/>
</view>

<view entity="hunttech_City" name="city-browse-view" extends="_minimal">
    <property name="cityRuName"/>
    <property name="cityPhoneCode"/>
    <property name="cityRegion" view="region-picker-view"/>
    <property name="fileCityEmblem"/>
    <property name="cityEnName"/>
    <property name="latitude"/>
    <property name="longitude"/>
    <property name="timezone"/>
    <property name="districtId"/>
    <property name="cityLevel"/>
    <property name="airportCode"/>
    <property name="postalCode"/>
    <property name="htmlwebId"/>
</view>
```

---

## Этап 2: Проверки после миграции (ОБЯЗАТЕЛЬНЫЕ)

### 2.1 Автотесты (существующие)

```bash
# Запустить из worktree
cd /Users/alekseyananyev/StudioProjects/hrm-antigravity
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*DictionaryEditFormsDetachedObjectTest*"
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*CountryServiceTest*"
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*RegionServiceTest*"
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*CityServiceTest*"
```

**Критерий успеха:** все тесты проходят (GREEN).

### 2.2 Ручные проверки в запущенном приложении

После деплоя ветки (`bash ../hunttech_recruiting/scripts/start-app.sh --branch "$PWD"`):

| Экран | Действие | Ожидаемый результат |
|-------|----------|---------------------|
| **CountryBrowse** | Открыть список стран | Список загружается, колонки: Название, Краткое имя, Код телефона, Флаг |
| **CountryEdit (новый)** | Создать страну | Форма открывается, сохранение работает |
| **CountryEdit (существующий)** | Открыть "Россия" | Поля заполнены, флаг отображается, регионы в таблице |
| **RegionBrowse** | Открыть список регионов | Список загружается, отображается страна |
| **RegionEdit** | Открыть/создать регион | Форма работает, lookup страны работает |
| **CityBrowse** | Открыть список городов | Список загружается, отображается регион |
| **CityEdit** | Открыть/создать город | Форма работает, lookup региона работает |
| **PersonEdit** | Lookup "Страна позиции" | Lookup работает, страны отображаются |
| **CompanyEdit** | Lookup "Страна компании" | Lookup работает |
| **RegionEdit** | Lookup "Страна региона" | Lookup работает |

### 2.3 Проверка Data View Integrity

```bash
# Проверить, что нет Unfetched Attribute Access в логах
grep -i "unfetched\|lazyinitialization" /opt/app_home/logs/hrm.log
# Должно быть пусто
```

### 2.4 Проверка API подключения (в приложении)

Создать Groovy-скрипт для проверки в консоли приложения:

```groovy
// В Groovy Console (Administration -> Groovy Console)
import com.company.hunttech.service.GeoDataSyncService
def sync = AppBeans.get(GeoDataSyncService.NAME)

// Тест подключения к API
def testUrl = "http://htmlweb.ru/geo/api.php?location=&json&short"
def conn = new URL(testUrl).openConnection()
conn.connectTimeout = 5000
conn.readTimeout = 10000
def response = conn.content.text
def data = new groovy.json.JsonSlurper().parseText(response)

println "Countries loaded: ${data.size()}"
println "Sample RU: ${data.RU}"
println "Sample GE: ${data.GE}"
assert data.size() > 200 : "Too few countries"
assert data.RU.name == "Россия" : "Russia name mismatch"
assert data.GE.name == "Грузия" : "Georgia name mismatch"
println "✅ API connection OK"
```

---

## Этап 3: UI поля (Edit-формы) — ОТДЕЛЬНЫЙ КОММИТ

### 3.1 messages.properties (web модуль)

```properties
# Country
msgCountryEnName=Название на английском
msgCountryIso3=ISO-3 код
msgCountryNumericCode=Числовой код (ISO)
msgContinent=Континент
msgCapitalCityId=ID столицы (htmlweb)
msgMcc=MCC код
msgLang=Основной язык
msgLangCode=Код языка
msgHtmlwebId=HTMLWeb ID

# Region
msgRegionEnName=Название региона (EN)
msgFederalDistrict=Федеральный округ
msgKladrCode=КЛАДР код
msgRegionType=Тип региона (vid)

# City
msgCityEnName=Название города (EN)
msgLatitude=Широта
msgLongitude=Долгота
msgTimezone=Часовой пояс
msgDistrictId=ID района
msgSubDistrictId=ID подрайона
msgCityLevel=Уровень города
msgAirportCode=Код аэропорта (IATA)
msgPostalCode=Почтовый индекс
msgGeonameId=GeoNames ID
msgWikiLink=Ссылка на Википедию
msgFullName=Полное наименование
```

### 3.2 country-edit.xml — добавить в форму "Основные данные"

```xml
<textField id="countryEnNameField" property="countryEnName" caption="msg://msgCountryEnName" width="100%" stylename="edit-form-control"/>
<textField id="countryIso3Field" property="countryIso3" caption="msg://msgCountryIso3" width="100%" stylename="edit-form-control"/>
<textField id="countryNumericCodeField" property="countryNumericCode" caption="msg://msgCountryNumericCode" width="100%" stylename="edit-form-control"/>
<textField id="continentField" property="continent" caption="msg://msgContinent" width="100%" stylename="edit-form-control"/>
<textField id="capitalCityIdField" property="capitalCityId" caption="msg://msgCapitalCityId" width="100%" stylename="edit-form-control"/>
<textField id="mccField" property="mcc" caption="msg://msgMcc" width="100%" stylename="edit-form-control"/>
<textField id="langField" property="lang" caption="msg://msgLang" width="100%" stylename="edit-form-control"/>
<textField id="langCodeField" property="langCode" caption="msg://msgLangCode" width="100%" stylename="edit-form-control"/>
<textField id="htmlwebIdField" property="htmlwebId" caption="msg://msgHtmlwebId" width="100%" stylename="edit-form-control"/>
```

### 3.3 region-edit.xml, city-edit.xml — аналогично

---

## Этап 4: Сервис синхронизации (ОТДЕЛЬНЫЙ КОММИТ)

### 4.1 Интерфейс
**Файл:** `modules/global/src/com/company/hunttech/service/GeoDataSyncService.java`

```java
package com.company.hunttech.service;

public interface GeoDataSyncService {
    String NAME = "hunttech_GeoDataSyncService";
    
    void syncCountries();
    void syncRegions(String countryHtmlwebId);
    void syncCities(Integer regionHtmlwebId);
    void syncAll();
}
```

### 4.2 Реализация
**Файл:** `modules/core/src/com/company/hunttech/service/GeoDataSyncServiceBean.java`

```java
package com.company.hunttech.service;

import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.Region;
import com.company.hunttech.entity.City;
import com.haulmont.cuba.core.global.DataManager;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service(GeoDataSyncService.NAME)
public class GeoDataSyncServiceBean implements GeoDataSyncService {

    private static final String API_BASE = "http://htmlweb.ru/geo/api.php";
    
    @Autowired
    protected DataManager dataManager;
    
    @Autowired
    protected RestTemplate restTemplate;

    @Override
    public void syncCountries() {
        Map<String, Object> params = new HashMap<>();
        params.put("location", "");
        params.put("json", "");
        params.put("short", "");
        
        Map<String, Object> response = restTemplate.getForObject(API_BASE + "?location=&json&short", Map.class);
        if (response == null) throw new RuntimeException("Empty API response");
        
        int created = 0, updated = 0;
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            if ("limit".equals(entry.getKey()) || "error".equals(entry.getKey())) continue;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> c = (Map<String, Object>) entry.getValue();
            String htmlwebId = (String) c.get("id");
            
            Country country = dataManager.load(Country.class)
                .query("select e from hunttech_Country e where e.htmlwebId = :id")
                .parameter("id", htmlwebId)
                .optional()
                .orElse(null);
            
            boolean isNew = false;
            if (country == null) {
                country = dataManager.create(Country.class);
                isNew = true;
            }
            
            country.setCountryRuName((String) c.get("name"));
            country.setCountryEnName((String) c.get("english"));
            country.setCountryShortName((String) c.get("id")); // ISO2
            country.setCountryIso3((String) c.get("country_code3"));
            country.setCountryNumericCode(c.get("iso") != null ? ((Number) c.get("iso")).intValue() : null);
            country.setPhoneCode(c.get("telcod") != null ? ((Number) c.get("telcod")).intValue() : null);
            country.setContinent((String) c.get("location"));
            country.setCapitalCityId(c.get("capital") != null ? ((Number) c.get("capital")).intValue() : null);
            country.setMcc(c.get("mcc") != null ? ((Number) c.get("mcc")).intValue() : null);
            country.setLang((String) c.get("lang"));
            country.setLangCode((String) c.get("langcod"));
            country.setHtmlwebId(htmlwebId);
            
            dataManager.commit(country);
            if (isNew) created++; else updated++;
        }
        log.info("Countries sync: created={}, updated={}", created, updated);
    }

    @Override
    public void syncRegions(String countryHtmlwebId) {
        Map<String, Object> response = restTemplate.getForObject(
            API_BASE + "?country=" + countryHtmlwebId + "&json&short", Map.class);
        
        if (response == null) return;
        
        Country country = dataManager.load(Country.class)
            .query("select e from hunttech_Country e where e.htmlwebId = :id")
            .parameter("id", countryHtmlwebId)
            .optional()
            .orElseThrow(() -> new RuntimeException("Country not found: " + countryHtmlwebId));
        
        int created = 0, updated = 0;
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            if ("limit".equals(entry.getKey()) || "error".equals(entry.getKey())) continue;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> r = (Map<String, Object>) entry.getValue();
            Integer htmlwebId = ((Number) r.get("id")).intValue();
            
            Region region = dataManager.load(Region.class)
                .query("select e from hunttech_Region e where e.htmlwebId = :id")
                .parameter("id", htmlwebId)
                .optional()
                .orElse(null);
            
            boolean isNew = false;
            if (region == null) {
                region = dataManager.create(Region.class);
                isNew = true;
            }
            
            region.setRegionRuName((String) r.get("name"));
            region.setRegionEnName((String) r.get("english"));
            region.setRegionCode(((Number) r.get("id")).intValue());
            region.setRegionCountry(country);
            region.setFederalDistrict((String) r.get("okrug"));
            region.setKladrCode((String) r.get("autocod"));
            region.setRegionType(r.get("vid") != null ? ((Number) r.get("vid")).intValue() : null);
            region.setCapitalCityId(r.get("capital") != null ? ((Number) r.get("capital")).intValue() : null);
            region.setHtmlwebId(htmlwebId);
            
            dataManager.commit(region);
            if (isNew) created++; else updated++;
        }
        log.info("Regions sync for {}: created={}, updated={}", countryHtmlwebId, created, updated);
    }

    @Override
    public void syncCities(Integer regionHtmlwebId) {
        Map<String, Object> response = restTemplate.getForObject(
            API_BASE + "?area=" + regionHtmlwebId + "&json&short", Map.class);
        
        if (response == null) return;
        
        Region region = dataManager.load(Region.class)
            .query("select e from hunttech_Region e where e.htmlwebId = :id")
            .parameter("id", regionHtmlwebId)
            .optional()
            .orElseThrow(() -> new RuntimeException("Region not found: " + regionHtmlwebId));
        
        int created = 0, updated = 0;
        for (Map.Entry<String, Object> entry : response.entrySet()) {
            if ("limit".equals(entry.getKey()) || "error".equals(entry.getKey())) continue;
            
            @SuppressWarnings("unchecked")
            Map<String, Object> c = (Map<String, Object>) entry.getValue();
            Integer htmlwebId = ((Number) c.get("id")).intValue();
            
            City city = dataManager.load(City.class)
                .query("select e from hunttech_City e where e.htmlwebId = :id")
                .parameter("id", htmlwebId)
                .optional()
                .orElse(null);
            
            boolean isNew = false;
            if (city == null) {
                city = dataManager.create(City.class);
                isNew = true;
            }
            
            city.setCityRuName((String) c.get("name"));
            city.setCityEnName((String) c.get("english"));
            city.setCityPhoneCode((String) c.get("telcod"));
            city.setCityRegion(region);
            city.setLatitude(c.get("latitude") != null ? ((Number) c.get("latitude")).doubleValue() : null);
            city.setLongitude(c.get("longitude") != null ? ((Number) c.get("longitude")).doubleValue() : null);
            city.setTimezone((String) c.get("tz"));
            city.setDistrictId(c.get("rajon") != null ? ((Number) c.get("rajon")).intValue() : null);
            city.setSubDistrictId(c.get("sub_rajon") != null ? ((Number) c.get("sub_rajon")).intValue() : null);
            city.setCityLevel(c.get("level") != null ? ((Number) c.get("level")).intValue() : null);
            city.setAirportCode((String) c.get("iso"));
            city.setPostalCode((String) c.get("post"));
            city.setGeonameId(c.get("geonameid") != null ? ((Number) c.get("geonameid")).longValue() : null);
            city.setWikiLink((String) c.get("wiki"));
            city.setFullName((String) c.get("full_name"));
            city.setHtmlwebId(htmlwebId);
            
            dataManager.commit(city);
            if (isNew) created++; else updated++;
        }
        log.info("Cities sync for region {}: created={}, updated={}", regionHtmlwebId, created, updated);
    }

    @Override
    public void syncAll() {
        syncCountries();
        // Синхронизируем регионы для всех стран
        List<Country> countries = dataManager.load(Country.class)
            .query("select e from hunttech_Country e where e.htmlwebId is not null")
            .list();
        for (Country c : countries) {
            syncRegions(c.getHtmlwebId());
        }
        // Синхронизируем города для всех регионов
        List<Region> regions = dataManager.load(Region.class)
            .query("select e from hunttech_Region e where e.htmlwebId is not null")
            .list();
        for (Region r : regions) {
            syncCities(r.getHtmlwebId());
        }
    }
}
```

### 4.3 Регистрация в spring.xml
**Файл:** `modules/core/src/com/company/hunttech/core-spring.xml` (или web-spring.xml)

```xml
<bean id="hunttech_GeoDataSyncService" class="com.company.hunttech.service.GeoDataSyncServiceBean"/>
```

### 4.4 RestTemplate конфигурация
**Файл:** `modules/core/src/com/company/hunttech/core-spring.xml`

```xml
<bean id="restTemplate" class="org.springframework.web.client.RestTemplate">
    <constructor-arg>
        <bean class="org.springframework.http.client.SimpleClientHttpRequestFactory">
            <property name="connectTimeout" value="5000"/>
            <property name="readTimeout" value="30000"/>
        </bean>
    </constructor-arg>
</bean>
```

---

## Этап 5: Экран управления синхронизацией (ОПЦИОНАЛЬНО)

Добавить кнопки в `CountryBrowse`, `RegionBrowse`, `CityBrowse`:
- "Синхронизировать эту страну" → вызывает `syncRegions(country.htmlwebId)`
- "Синхронизировать этот регион" → вызывает `syncCities(region.htmlwebId)`
- "Полная синхронизация" → вызывает `syncAll()` (долго, лучше в фоне)

---

## Чек-лист приёмки (Definition of Done)

### Этап 1 (БД + Entity + Views) — ОБЯЗАТЕЛЬНО
- [ ] Миграция применяется без ошибок на чистой БД
- [ ] Миграция применяется на существующей БД (data не теряется)
- [ ] Все автотесты проходят (`DictionaryEditFormsDetachedObjectTest`, `CountryServiceTest`, `RegionServiceTest`, `CityServiceTest`)
- [ ] Приложение стартует без ошибок
- [ ] Существующие экраны Country/Region/City Browse/Edit открываются
- [ ] LookupPickerField в PersonEdit, CompanyEdit, RegionEdit работают
- [ ] Нет ошибок Unfetched Attribute Access в логах

### Этап 2 (UI поля) — ПОСЛЕ Этапа 1
- [ ] Новые поля отображаются в Edit-формах
- [ ] Сохранение новых полей работает
- [ ] Валидация не ломается

### Этап 3 (Сервис синхронизации) — ПОСЛЕ Этапа 2
- [ ] `syncCountries()` создаёт/обновляет страны
- [ ] `syncRegions("RU")` создаёт регионы России
- [ ] `syncCities(1)` создаёт города Москвы
- [ ] Идемпотентность: повторный запуск не создаёт дубликаты (по htmlwebId)
- [ ] Обработка ошибок API (timeout, limit, 5xx)

---

## Настройка API-ключа htmlweb.ru

1. Зарегистрироваться на https://htmlweb.ru/
2. Получить API_KEY в профиле
3. Добавить в `local.app.properties`:
   ```properties
   htmlweb.api.key=ВАШ_КЛЮЧ
   htmlweb.api.base.url=http://htmlweb.ru/geo/api.php
   ```
4. В сервисе использовать: `params.put("api_key", apiKey)`

---

## Ограничения и риски

| Риск | Митигация |
|------|-----------|
| Дневной лимит API | Использовать API_KEY, кэшировать ответы, синхронизировать ночью |
| Изменение структуры API | Версионировать ответы, логировать сырые JSON |
| Маппинг htmlwebId ↔ UUID | Всегда искать по htmlwebId перед create/update |
| Разные коды стран (ISO2 vs ISO3) | Хранить оба, мапить при синхронизации |

---

## Команды для работы в Antigravity

```bash
# В worktree
cd /Users/alekseyananyev/StudioProjects/hrm-antigravity

# 1. Применить миграцию (проверка)
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:updateDb

# 2. Запустить тесты
bash ../hunttech_recruiting/scripts/agent-gradle.sh :app-core:test --tests "*DictionaryEditFormsDetachedObjectTest*"

# 3. Поднять приложение для ручной проверки
bash ../hunttech_recruiting/scripts/start-app.sh --branch "$PWD"
# http://localhost:8080/hrm/

# 4. После проверки вернуть master
bash ../hunttech_recruiting/scripts/start-app.sh
```

---

## Примечание по тестированию API в продакшене

На проде (hr.hunttech.ru) IP может отличаться. Нужно:
1. Получить API_KEY
2. Настроить в `local.app.properties` на проде
3. Проверить доступность: `curl "http://htmlweb.ru/geo/api.php?location=&json&short&api_key=KEY"`