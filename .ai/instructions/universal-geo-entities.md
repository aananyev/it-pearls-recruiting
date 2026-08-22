# Универсальная структура сущностей для мульти-провайдерных справочников

## Сравнение провайдеров

| Провайдер | Страны | Регионы | Города | Языки | Лимиты | Цена | Примечание |
|-----------|--------|---------|--------|-------|--------|------|------------|
| **htmlweb.ru** | 255 | ✅ мир | ✅ мир | RU/EN | 20/день бесплатно | 300₽/1к | Удобная иерархия, есть флаги |
| **VK API** | 234 | 3 721 | 2.2М | 11 языков | Без токена, безлимит | Бесплатно | Только для некоммерч.? |
| **GeoNames** | 250+ | ✅ | ✅ | Много | 2000-3000/день | Бесплатно | Нужен username |
| **Oxilor** | 247 | 231К | 4.6М | 182 языка | Платный | ? | GraphQL, богатые данные |
| **DaData** | ❌ | Только РФ | Только РФ | RU/EN | 100-500/день | От 0₽ | Только РФ, КЛАДР/ФИАС |
| **World Bank** | ~200 | ❌ | ❌ | EN | 10к/день | Бесплатно | Только статистика |

---

## Универсальная модель (Provider-Agnostic)

### Базовые поля (обязательные для всех провайдеров)

```java
// Country - универсальная сущность
@Entity(name = "hunttech_Country")
@Table(name = "HUNTTECH_COUNTRY")
public class Country extends StandardEntity {
    
    // === Идентификация (универсально) ===
    @Column(name = "CODE_ISO2", length = 2, unique = true)      // RU, US, GE
    protected String codeIso2;
    
    @Column(name = "CODE_ISO3", length = 3)                     // RUS, USA, GEO
    protected String codeIso3;
    
    @Column(name = "CODE_NUMERIC")                              // 643, 840, 268
    protected Integer codeNumeric;
    
    @Column(name = "NAME_RU", nullable = false, length = 100)   // Россия
    protected String nameRu;
    
    @Column(name = "NAME_EN", length = 100)                     // Russia
    protected String nameEn;
    
    @Column(name = "FULL_NAME_RU", length = 255)                // Российская Федерация
    protected String fullNameRu;
    
    @Column(name = "FULL_NAME_EN", length = 255)                // Russian Federation
    protected String fullNameEn;
    
    @Column(name = "PHONE_CODE")                                // 7, 1, 995
    protected Integer phoneCode;
    
    @Column(name = "CONTINENT", length = 50)                    // Europe, Asia
    protected String continent;
    
    @Column(name = "CAPITAL_CITY_NAME_RU", length = 100)        // Москва
    protected String capitalCityNameRu;
    
    @Column(name = "CAPITAL_CITY_NAME_EN", length = 100)        // Moscow
    protected String capitalCityNameEn;
    
    @Column(name = "TIMEZONE", length = 50)                     // Europe/Moscow
    protected String timezone;
    
    @Column(name = "LANGUAGES", length = 500)                   // ru,en,...
    protected String languages;
    
    @Column(name = "CURRENCY_CODE", length = 3)                 // RUB, USD
    protected String currencyCode;
    
    @Column(name = "TLD", length = 10)                          // .ru, .us
    protected String tld;
    
    @Column(name = "IS_ACTIVE", nullable = false)
    protected Boolean isActive = true;
    
    // === Провайдер-специфичные расширения (JSONB) ===
    @Column(name = "PROVIDER_DATA", columnDefinition = "jsonb")
    protected String providerData;  // JSON с полями конкретного провайдера
    
    // htmlwebId, vkId, geonameId, oxilorId и т.д. храним в providerData
    // Пример: {"htmlweb": {"id": "RU", "mcc": 250}, "vk": {"id": 1}, "geonames": {"id": 2017370}}
}
```

```java
// Region - универсальная сущность
@Entity(name = "hunttech_Region")
@Table(name = "HUNTTECH_REGION")
public class Region extends StandardEntity {
    
    @Column(name = "CODE", length = 50, unique = true)          // ISO код региона: RU-MOW, US-CA
    protected String code;
    
    @Column(name = "NAME_RU", nullable = false, length = 100)   // Москва
    protected String nameRu;
    
    @Column(name = "NAME_EN", length = 100)                     // Moscow
    protected String nameEn;
    
    @Column(name = "TYPE", length = 50)                         // republic, oblast, krai, city, state
    protected String type;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUNTRY_ID")
    protected Country country;
    
    @Column(name = "CAPITAL_CITY_NAME_RU", length = 100)
    protected String capitalCityNameRu;
    
    @Column(name = "CAPITAL_CITY_NAME_EN", length = 100)
    protected String capitalCityNameEn;
    
    @Column(name = "TIMEZONE", length = 50)
    protected String timezone;
    
    @Column(name = "KLADR_CODE", length = 20)                   // Только для РФ
    protected String kladrCode;
    
    @Column(name = "OKATO", length = 20)                        // Только для РФ
    protected String okato;
    
    @Column(name = "OKTMO", length = 20)                        // Только для РФ
    protected String oktmo;
    
    @Column(name = "IS_ACTIVE", nullable = false)
    protected Boolean isActive = true;
    
    @Column(name = "PROVIDER_DATA", columnDefinition = "jsonb")
    protected String providerData;
    // {"htmlweb": {"id": 1, "okrug": "ЦФО", "vid": 20}, "vk": {"id": 1045244}}
}
```

```java
// City - универсальная сущность
@Entity(name = "hunttech_City")
@Table(name = "HUNTTECH_CITY")
public class City extends StandardEntity {
    
    @Column(name = "NAME_RU", nullable = false, length = 100)   // Москва
    protected String nameRu;
    
    @Column(name = "NAME_EN", length = 100)                     // Moscow
    protected String nameEn;
    
    @Column(name = "NAME_ALT", length = 500)                    // Альтернативные названия
    protected String nameAlt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REGION_ID")
    protected Region region;
    
    @Column(name = "LATITUDE")
    protected Double latitude;
    
    @Column(name = "LONGITUDE")
    protected Double longitude;
    
    @Column(name = "POPULATION")
    protected Long population;
    
    @Column(name = "TIMEZONE", length = 50)
    protected String timezone;
    
    @Column(name = "POSTAL_CODE", length = 20)
    protected String postalCode;
    
    @Column(name = "PHONE_CODE", length = 20)
    protected String phoneCode;
    
    @Column(name = "AIRPORT_CODE_IATA", length = 10)            // MOW, LED
    protected String airportCodeIata;
    
    @Column(name = "AIRPORT_CODE_ICAO", length = 10)            // UUEE, ULLI
    protected String airportCodeIcao;
    
    @Column(name = "GEONAME_ID")                                // ID в GeoNames
    protected Long geonameId;
    
    @Column(name = "WIKI_LINK", length = 255)
    protected String wikiLink;
    
    @Column(name = "IS_CAPITAL", nullable = false)
    protected Boolean isCapital = false;
    
    @Column(name = "IS_ACTIVE", nullable = false)
    protected Boolean isActive = true;
    
    @Column(name = "PROVIDER_DATA", columnDefinition = "jsonb")
    protected String providerData;
    // {"htmlweb": {"id": 1, "level": 1, "rajon": 0}, "vk": {"id": 1, "area": "Moscow"}}
}
```

---

## Провайдер-специфичные адаптеры

### Интерфейс адаптера

```java
public interface GeoDataProvider {
    String getProviderCode();  // "htmlweb", "vk", "geonames", "oxilor"
    
    List<CountryDTO> fetchCountries();
    List<RegionDTO> fetchRegions(String countryIso2);
    List<CityDTO> fetchCities(String regionCode);
    
    CountryDTO fetchCountryByCode(String iso2);
    RegionDTO fetchRegionByCode(String countryIso2, String regionCode);
    CityDTO fetchCityById(String cityId);
}

// DTO для универсального обмена
@Data
public class CountryDTO {
    String iso2, iso3, numericCode;
    String nameRu, nameEn, fullNameRu, fullNameEn;
    Integer phoneCode;
    String continent, timezone, languages, currencyCode, tld;
    String capitalCityNameRu, capitalCityNameEn;
    Map<String, Object> providerSpecific;  // сырые данные провайдера
}

@Data
public class RegionDTO {
    String code, countryIso2;
    String nameRu, nameEn, type;
    String capitalCityNameRu, capitalCityNameEn;
    String timezone;
    String kladrCode, okato, oktmo;
    Map<String, Object> providerSpecific;
}

@Data
public class CityDTO {
    String regionCode;
    String nameRu, nameEn, nameAlt;
    Double latitude, longitude;
    Long population;
    String timezone, postalCode, phoneCode;
    String airportCodeIata, airportCodeIcao;
    Long geonameId;
    String wikiLink;
    Boolean isCapital;
    Map<String, Object> providerSpecific;
}
```

### Реализация для htmlweb.ru

```java
@Service("hunttech_HtmlwebGeoProvider")
public class HtmlwebGeoProvider implements GeoDataProvider {
    
    private static final String BASE_URL = "http://htmlweb.ru/geo/api.php";
    
    @Override
    public String getProviderCode() { return "htmlweb"; }
    
    @Override
    public List<CountryDTO> fetchCountries() {
        Map<String, Object> resp = restTemplate.getForObject(BASE_URL + "?location=&json&short", Map.class);
        return resp.entrySet().stream()
            .filter(e -> !"limit".equals(e.getKey()) && !"error".equals(e.getKey()))
            .map(e -> mapCountry((Map) e.getValue(), e.getKey()))
            .collect(Collectors.toList());
    }
    
    private CountryDTO mapCountry(Map<String, Object> c, String htmlwebId) {
        CountryDTO dto = new CountryDTO();
        dto.setIso2((String) c.get("id"));
        dto.setIso3((String) c.get("country_code3"));
        dto.setNumericCode(c.get("iso") != null ? ((Number) c.get("iso")).intValue() : null);
        dto.setNameRu((String) c.get("name"));
        dto.setNameEn((String) c.get("english"));
        dto.setPhoneCode(c.get("telcod") != null ? ((Number) c.get("telcod")).intValue() : null);
        dto.setContinent((String) c.get("location"));
        dto.setTimezone(c.get("capital") instanceof Map ? 
            (String) ((Map) c.get("capital")).get("tz") : null);
        dto.setLanguages((String) c.get("langcod"));
        dto.setProviderSpecific(Map.of("htmlwebId", htmlwebId, "mcc", c.get("mcc"), "lang", c.get("lang")));
        return dto;
    }
    
    // Аналогично для регионов и городов...
}
```

### Реализация для VK API

```java
@Service("hunttech_VkGeoProvider")
public class VkGeoProvider implements GeoDataProvider {
    
    private static final String BASE_URL = "https://api.vk.com/method/database.";
    private static final String VERSION = "5.199";
    
    @Override
    public String getProviderCode() { return "vk"; }
    
    @Override
    public List<CountryDTO> fetchCountries() {
        Map<String, Object> resp = restTemplate.getForObject(
            BASE_URL + "getCountries?v=" + VERSION + "&need_all=1&count=1000&lang=ru", Map.class);
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) 
            ((Map<String, Object>) resp.get("response")).get("items");
        
        return items.stream().map(this::mapCountry).collect(Collectors.toList());
    }
    
    private CountryDTO mapCountry(Map<String, Object> c) {
        CountryDTO dto = new CountryDTO();
        dto.setIso2(String.valueOf(c.get("id")));  // VK использует числовые ID
        dto.setNameRu((String) c.get("title"));
        dto.setProviderSpecific(Map.of("vkId", c.get("id")));
        // ISO коды нужно мапить отдельно или получать через getCountriesById
        return dto;
    }
}
```

---

## Сервис синхронизации (универсальный)

```java
@Service(GeoDataSyncService.NAME)
public class GeoDataSyncServiceBean implements GeoDataSyncService {
    
    @Autowired
    private List<GeoDataProvider> providers;  // Spring заинжектит все реализации
    
    @Autowired
    private DataManager dataManager;
    
    @Override
    public void syncFromProvider(String providerCode) {
        GeoDataProvider provider = providers.stream()
            .filter(p -> p.getProviderCode().equals(providerCode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerCode));
        
        syncCountries(provider);
        // Регионы и города синхронизируем только для активных стран
        List<Country> activeCountries = dataManager.load(Country.class)
            .query("select e from hunttech_Country e where e.isActive = true")
            .list();
        
        for (Country country : activeCountries) {
            syncRegions(provider, country.getCodeIso2());
        }
    }
    
    private void syncCountries(GeoDataProvider provider) {
        List<CountryDTO> dtos = provider.fetchCountries();
        
        for (CountryDTO dto : dtos) {
            Country entity = findOrCreateCountry(dto.getIso2());
            updateCountryFromDTO(entity, dto, provider.getProviderCode());
            dataManager.commit(entity);
        }
    }
    
    private Country findOrCreateCountry(String iso2) {
        return dataManager.load(Country.class)
            .query("select e from hunttech_Country e where e.codeIso2 = :iso2")
            .parameter("iso2", iso2)
            .optional()
            .orElseGet(() -> dataManager.create(Country.class));
    }
    
    private void updateCountryFromDTO(Country entity, CountryDTO dto, String providerCode) {
        // Универсальные поля
        entity.setCodeIso2(dto.getIso2());
        entity.setCodeIso3(dto.getIso3());
        entity.setCodeNumeric(dto.getNumericCode());
        entity.setNameRu(dto.getNameRu());
        entity.setNameEn(dto.getNameEn());
        entity.setFullNameRu(dto.getFullNameRu());
        entity.setFullNameEn(dto.getFullNameEn());
        entity.setPhoneCode(dto.getPhoneCode());
        entity.setContinent(dto.getContinent());
        entity.setTimezone(dto.getTimezone());
        entity.setLanguages(dto.getLanguages());
        entity.setCurrencyCode(dto.getCurrencyCode());
        entity.setTld(dto.getTld());
        entity.setCapitalCityNameRu(dto.getCapitalCityNameRu());
        entity.setCapitalCityNameEn(dto.getCapitalCityNameEn());
        
        // Провайдер-специфичные данные в JSONB
        mergeProviderData(entity, providerCode, dto.getProviderSpecific());
    }
    
    private void mergeProviderData(Country entity, String providerCode, Map<String, Object> newData) {
        Map<String, Object> existing = parseProviderData(entity.getProviderData());
        if (existing == null) existing = new HashMap<>();
        
        existing.put(providerCode, newData);
        entity.setProviderData(toJson(existing));
    }
}
```

---

## Миграция БД (универсальная)

```sql
-- Country: универсальные поля + JSONB для провайдеров
alter table HUNTTECH_COUNTRY add column if not exists CODE_ISO2 varchar(2);
alter table HUNTTECH_COUNTRY add column if not exists CODE_ISO3 varchar(3);
alter table HUNTTECH_COUNTRY add column if not exists CODE_NUMERIC integer;
alter table HUNTTECH_COUNTRY add column if not exists NAME_RU varchar(100);
alter table HUNTTECH_COUNTRY add column if not exists NAME_EN varchar(100);
alter table HUNTTECH_COUNTRY add column if not exists FULL_NAME_RU varchar(255);
alter table HUNTTECH_COUNTRY add column if not exists FULL_NAME_EN varchar(255);
alter table HUNTTECH_COUNTRY add column if not exists PHONE_CODE integer;
alter table HUNTTECH_COUNTRY add column if not exists CONTINENT varchar(50);
alter table HUNTTECH_COUNTRY add column if not exists CAPITAL_CITY_NAME_RU varchar(100);
alter table HUNTTECH_COUNTRY add column if not exists CAPITAL_CITY_NAME_EN varchar(100);
alter table HUNTTECH_COUNTRY add column if not exists TIMEZONE varchar(50);
alter table HUNTTECH_COUNTRY add column if not exists LANGUAGES varchar(500);
alter table HUNTTECH_COUNTRY add column if not exists CURRENCY_CODE varchar(3);
alter table HUNTTECH_COUNTRY add column if not exists TLD varchar(10);
alter table HUNTTECH_COUNTRY add column if not exists IS_ACTIVE boolean default true;
alter table HUNTTECH_COUNTRY add column if not exists PROVIDER_DATA jsonb;
create index if not exists IDX_COUNTRY_ISO2 on HUNTTECH_COUNTRY (CODE_ISO2);
create index if not exists IDX_COUNTRY_PROVIDER_DATA on HUNTTECH_COUNTRY using gin (PROVIDER_DATA);

-- Region
alter table HUNTTECH_REGION add column if not exists CODE varchar(50);
alter table HUNTTECH_REGION add column if not exists NAME_RU varchar(100);
alter table HUNTTECH_REGION add column if not exists NAME_EN varchar(100);
alter table HUNTTECH_REGION add column if not exists TYPE varchar(50);
alter table HUNTTECH_REGION add column if not exists CAPITAL_CITY_NAME_RU varchar(100);
alter table HUNTTECH_REGION add column if not exists CAPITAL_CITY_NAME_EN varchar(100);
alter table HUNTTECH_REGION add column if not exists TIMEZONE varchar(50);
alter table HUNTTECH_REGION add column if not exists KLADR_CODE varchar(20);
alter table HUNTTECH_REGION add column if not exists OKATO varchar(20);
alter table HUNTTECH_REGION add column if not exists OKTMO varchar(20);
alter table HUNTTECH_REGION add column if not exists IS_ACTIVE boolean default true;
alter table HUNTTECH_REGION add column if not exists PROVIDER_DATA jsonb;
create index if not exists IDX_REGION_CODE on HUNTTECH_REGION (CODE);
create index if not exists IDX_REGION_PROVIDER_DATA on HUNTTECH_REGION using gin (PROVIDER_DATA);

-- City
alter table HUNTTECH_CITY add column if not exists NAME_RU varchar(100);
alter table HUNTTECH_CITY add column if not exists NAME_EN varchar(100);
alter table HUNTTECH_CITY add column if not exists NAME_ALT varchar(500);
alter table HUNTTECH_CITY add column if not exists LATITUDE double precision;
alter table HUNTTECH_CITY add column if not exists LONGITUDE double precision;
alter table HUNTTECH_CITY add column if not exists POPULATION bigint;
alter table HUNTTECH_CITY add column if not exists TIMEZONE varchar(50);
alter table HUNTTECH_CITY add column if not exists POSTAL_CODE varchar(20);
alter table HUNTTECH_CITY add column if not exists PHONE_CODE varchar(20);
alter table HUNTTECH_CITY add column if not exists AIRPORT_CODE_IATA varchar(10);
alter table HUNTTECH_CITY add column if not exists AIRPORT_CODE_ICAO varchar(10);
alter table HUNTTECH_CITY add column if not exists GEONAME_ID bigint;
alter table HUNTTECH_CITY add column if not exists WIKI_LINK varchar(255);
alter table HUNTTECH_CITY add column if not exists IS_CAPITAL boolean default false;
alter table HUNTTECH_CITY add column if not exists IS_ACTIVE boolean default true;
alter table HUNTTECH_CITY add column if not exists PROVIDER_DATA jsonb;
create index if not exists IDX_CITY_PROVIDER_DATA on HUNTTECH_CITY using gin (PROVIDER_DATA);

-- Миграция существующих данных (если были старые поля)
update HUNTTECH_COUNTRY set CODE_ISO2 = COUNTRY_SHORT_NAME where CODE_ISO2 is null;
update HUNTTECH_COUNTRY set NAME_RU = COUNTRY_RU_NAME where NAME_RU is null;
update HUNTTECH_REGION set NAME_RU = REGION_RU_NAME where NAME_RU is null;
update HUNTTECH_CITY set NAME_RU = CITY_RU_NAME where NAME_RU is null;
```

---

## Стратегия переключения провайдера

```java
// Конфигурация активного провайдера
@ConfigurationProperties(prefix = "geo.provider")
public class GeoProviderConfig {
    private String active = "htmlweb";  // htmlweb, vk, geonames, oxilor
    private Map<String, String> apiKeys = new HashMap<>();
    private boolean fallbackEnabled = true;
    private List<String> fallbackOrder = Arrays.asList("htmlweb", "vk", "geonames");
}

// Использование в сервисе
@Service
public class GeoDataService {
    
    @Autowired
    private GeoProviderConfig config;
    
    @Autowired
    private Map<String, GeoDataProvider> providers;  // key = providerCode
    
    public List<Country> getCountries() {
        GeoDataProvider provider = providers.get(config.getActive());
        if (provider == null) throw new IllegalStateException("Active provider not configured");
        
        try {
            return provider.fetchCountries().stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        } catch (Exception e) {
            if (config.isFallbackEnabled()) {
                return tryFallback(e);
            }
            throw e;
        }
    }
}
```

---

## Чек-лист для внедрения

| Этап | Действие | Статус |
|------|----------|--------|
| 1 | Создать универсальные Entity с JSONB providerData | ⬜ |
| 2 | Миграция БД (универсальные поля + GIN индексы) | ⬜ |
| 3 | Создать интерфейс GeoDataProvider + DTO | ⬜ |
| 4 | Реализовать HtmlwebGeoProvider | ⬜ |
| 5 | Реализовать VkGeoProvider (fallback) | ⬜ |
| 6 | Реализовать GeoNamesGeoProvider (fallback) | ⬜ |
| 7 | Универсальный GeoDataSyncService с mergeProviderData | ⬜ |
| 8 | Конфигурация активного провайдера + fallback | ⬜ |
| 9 | UI: выбор провайдера в настройках / экране синхронизации | ⬜ |
| 10 | Тесты: переключение провайдера без потери данных | ⬜ |

---

## Резюме

**Универсальная структура готова к смене провайдера:**
- ✅ Все общие поля вынесены в колонки БД
- ✅ Провайдер-специфичные данные в `providerData` (JSONB)
- ✅ Интерфейс `GeoDataProvider` позволяет добавлять новые провайдеры без изменения ядра
- ✅ Fallback-механизм настроен через конфигурацию
- ✅ Идемпотентность по `codeIso2` / `code` / `regionCode`

**Рекомендация:** Начать с htmlweb.ru как основного, VK API как fallback (бесплатно, без токена). GeoNames — для enrichment (население, timezone, координаты).