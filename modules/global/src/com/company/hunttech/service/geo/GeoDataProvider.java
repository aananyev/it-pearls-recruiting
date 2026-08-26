package com.company.hunttech.service.geo;

import java.util.List;
import java.util.Map;

/**
 * Универсальный интерфейс гео-провайдера (provider-agnostic).
 * Реализации: Htmlweb, VK, GeoNames, Geoapify, Oxilor, DaData.
 * 
 * @see GeoDataEnrichmentServiceBean — использует цепочку провайдеров
 * @see AdminGeoConfiguration — хранение настроек/ключей в БД
 */
public interface GeoDataProvider {

    /**
     * Код провайдера: "htmlweb", "vk", "geonames", "geoapify", "oxilor", "dadata"
     */
    String getProviderCode();

    /**
     * Человекочитаемое название для UI
     */
    String getProviderName();

    /**
     * Поддерживает ли провайдер страны
     */
    default boolean supportsCountries() { return true; }

    /**
     * Поддерживает ли провайдер регионы
     */
    default boolean supportsRegions() { return true; }

    /**
     * Поддерживает ли провайдер города
     */
    default boolean supportsCities() { return true; }

    /**
     * Требует ли провайдер API ключ
     */
    default boolean requiresApiKey() { return false; }

    /**
     * Работает ли провайдер из РФ без VPN
     */
    default boolean worksFromRussia() { return true; }

    /**
     * Поддерживаемые языки (ISO 639-1): "ru", "en", ...
     */
    default List<String> getSupportedLanguages() { return List.of("ru", "en"); }

    /**
     * Поиск страны по названию/коду
     */
    CountryDTO findCountry(String query, String language);

    /**
     * Поиск региона по названию и коду страны
     */
    RegionDTO findRegion(String regionQuery, String countryIso2, String language);

    /**
     * Поиск города по названию, региону и стране
     */
    CityDTO findCity(String cityQuery, String regionCode, String countryIso2, String language);

    /**
     * Загрузка списка всех стран (для первичной синхронизации)
     */
    default List<CountryDTO> fetchAllCountries(String language) { return List.of(); }

    /**
     * Загрузка регионов страны (для синхронизации)
     */
    default List<RegionDTO> fetchRegionsForCountry(String countryIso2, String language) { return List.of(); }

    /**
     * Загрузка городов региона (для синхронизации)
     */
    default List<CityDTO> fetchCitiesForRegion(String regionCode, String countryIso2, String language) { return List.of(); }

    /**
     * Проверка доступности API
     */
    boolean testConnection(Map<String, String> credentials);

    // ===== DTO =====

    /**
     * DTO страны — универсальный формат обмена
     */
    class CountryDTO {
        private String iso2;           // RU, US, CN (ISO 3166-1 alpha-2)
        private String iso3;           // RUS, USA, CHN (ISO 3166-1 alpha-3)
        private Integer numericCode;   // 643, 840, 156 (ISO 3166-1 numeric)
        private String nameRu;         // Россия
        private String nameEn;         // Russia
        private String fullNameRu;     // Российская Федерация
        private String fullNameEn;     // Russian Federation
        private Integer phoneCode;     // 7, 1, 86
        private String capitalRu;      // Москва
        private String capitalEn;      // Moscow
        private String continent;      // Europe, Asia
        private String timezone;       // Europe/Moscow
        private String languages;      // ru, en, ...
        private String currencyCode;   // RUB, USD, CNY
        private String currencyNameRu; // Российский рубль
        private String currencyNameEn; // Russian Ruble
        private String tld;            // .ru, .us, .cn
        private String flagUrl;        // URL флага (SVG/PNG)
        private String flagEmoji;      // 🇷🇺
        private Map<String, Object> providerSpecific; // сырые данные провайдера

        // Getters/Setters
        public String getIso2() { return iso2; }
        public void setIso2(String iso2) { this.iso2 = iso2; }
        public String getIso3() { return iso3; }
        public void setIso3(String iso3) { this.iso3 = iso3; }
        public Integer getNumericCode() { return numericCode; }
        public void setNumericCode(Integer numericCode) { this.numericCode = numericCode; }
        public String getNameRu() { return nameRu; }
        public void setNameRu(String nameRu) { this.nameRu = nameRu; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public String getFullNameRu() { return fullNameRu; }
        public void setFullNameRu(String fullNameRu) { this.fullNameRu = fullNameRu; }
        public String getFullNameEn() { return fullNameEn; }
        public void setFullNameEn(String fullNameEn) { this.fullNameEn = fullNameEn; }
        public Integer getPhoneCode() { return phoneCode; }
        public void setPhoneCode(Integer phoneCode) { this.phoneCode = phoneCode; }
        public String getCapitalRu() { return capitalRu; }
        public void setCapitalRu(String capitalRu) { this.capitalRu = capitalRu; }
        public String getCapitalEn() { return capitalEn; }
        public void setCapitalEn(String capitalEn) { this.capitalEn = capitalEn; }
        public String getContinent() { return continent; }
        public void setContinent(String continent) { this.continent = continent; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public String getLanguages() { return languages; }
        public void setLanguages(String languages) { this.languages = languages; }
        public String getCurrencyCode() { return currencyCode; }
        public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
        public String getCurrencyNameRu() { return currencyNameRu; }
        public void setCurrencyNameRu(String currencyNameRu) { this.currencyNameRu = currencyNameRu; }
        public String getCurrencyNameEn() { return currencyNameEn; }
        public void setCurrencyNameEn(String currencyNameEn) { this.currencyNameEn = currencyNameEn; }
        public String getTld() { return tld; }
        public void setTld(String tld) { this.tld = tld; }
        public String getFlagUrl() { return flagUrl; }
        public void setFlagUrl(String flagUrl) { this.flagUrl = flagUrl; }
        public String getFlagEmoji() { return flagEmoji; }
        public void setFlagEmoji(String flagEmoji) { this.flagEmoji = flagEmoji; }
        public Map<String, Object> getProviderSpecific() { return providerSpecific; }
        public void setProviderSpecific(Map<String, Object> providerSpecific) { this.providerSpecific = providerSpecific; }
    }

    /**
     * DTO региона
     */
    class RegionDTO {
        private String code;           // ISO код региона: RU-MOW, US-CA, CN-11
        private String countryIso2;    // RU, US, CN
        private String nameRu;         // Москва
        private String nameEn;         // Moscow
        private String type;           // city, oblast, republic, krai, state, province
        private String capitalRu;      // Москва
        private String capitalEn;      // Moscow
        private String timezone;       // Europe/Moscow
        private Integer population;
        private String kladrCode;      // Только РФ: КЛАДР
        private String okato;          // Только РФ: ОКАТО
        private String oktmo;          // Только РФ: ОКТМО
        private String fiasId;         // Только РФ: ФИАС
        private Map<String, Object> providerSpecific;

        // Getters/Setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getCountryIso2() { return countryIso2; }
        public void setCountryIso2(String countryIso2) { this.countryIso2 = countryIso2; }
        public String getNameRu() { return nameRu; }
        public void setNameRu(String nameRu) { this.nameRu = nameRu; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCapitalRu() { return capitalRu; }
        public void setCapitalRu(String capitalRu) { this.capitalRu = capitalRu; }
        public String getCapitalEn() { return capitalEn; }
        public void setCapitalEn(String capitalEn) { this.capitalEn = capitalEn; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public Integer getPopulation() { return population; }
        public void setPopulation(Integer population) { this.population = population; }
        public String getKladrCode() { return kladrCode; }
        public void setKladrCode(String kladrCode) { this.kladrCode = kladrCode; }
        public String getOkato() { return okato; }
        public void setOkato(String okato) { this.okato = okato; }
        public String getOktmo() { return oktmo; }
        public void setOktmo(String oktmo) { this.oktmo = oktmo; }
        public String getFiasId() { return fiasId; }
        public void setFiasId(String fiasId) { this.fiasId = fiasId; }
        public Map<String, Object> getProviderSpecific() { return providerSpecific; }
        public void setProviderSpecific(Map<String, Object> providerSpecific) { this.providerSpecific = providerSpecific; }
    }

    /**
     * DTO города
     */
    class CityDTO {
        private String regionCode;     // RU-MOW, US-CA
        private String countryIso2;    // RU, US
        private String nameRu;         // Москва
        private String nameEn;         // Moscow
        private String nameAlt;        // Альтернативные названия
        private Double latitude;       // 55.7558
        private Double longitude;      // 37.6173
        private Long population;       // 13100000
        private String timezone;       // Europe/Moscow
        private String postalCode;     // 101000
        private String phoneCode;      // 495
        private String airportIata;    // MOW
        private String airportIcao;    // UUEE
        private Long geonameId;        // ID в GeoNames
        private String wikiLink;       // https://ru.wikipedia.org/wiki/Москва
        private Boolean capital;       // true/false
        private Map<String, Object> providerSpecific;

        // Getters/Setters
        public String getRegionCode() { return regionCode; }
        public void setRegionCode(String regionCode) { this.regionCode = regionCode; }
        public String getCountryIso2() { return countryIso2; }
        public void setCountryIso2(String countryIso2) { this.countryIso2 = countryIso2; }
        public String getNameRu() { return nameRu; }
        public void setNameRu(String nameRu) { this.nameRu = nameRu; }
        public String getNameEn() { return nameEn; }
        public void setNameEn(String nameEn) { this.nameEn = nameEn; }
        public String getNameAlt() { return nameAlt; }
        public void setNameAlt(String nameAlt) { this.nameAlt = nameAlt; }
        public Double getLatitude() { return latitude; }
        public void setLatitude(Double latitude) { this.latitude = latitude; }
        public Double getLongitude() { return longitude; }
        public void setLongitude(Double longitude) { this.longitude = longitude; }
        public Long getPopulation() { return population; }
        public void setPopulation(Long population) { this.population = population; }
        public String getTimezone() { return timezone; }
        public void setTimezone(String timezone) { this.timezone = timezone; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public String getPhoneCode() { return phoneCode; }
        public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
        public String getAirportIata() { return airportIata; }
        public void setAirportIata(String airportIata) { this.airportIata = airportIata; }
        public String getAirportIcao() { return airportIcao; }
        public void setAirportIcao(String airportIcao) { this.airportIcao = airportIcao; }
        public Long getGeonameId() { return geonameId; }
        public void setGeonameId(Long geonameId) { this.geonameId = geonameId; }
        public String getWikiLink() { return wikiLink; }
        public void setWikiLink(String wikiLink) { this.wikiLink = wikiLink; }
        public Boolean getCapital() { return capital; }
        public void setCapital(Boolean capital) { this.capital = capital; }
        public Map<String, Object> getProviderSpecific() { return providerSpecific; }
        public void setProviderSpecific(Map<String, Object> providerSpecific) { this.providerSpecific = providerSpecific; }
    }
}