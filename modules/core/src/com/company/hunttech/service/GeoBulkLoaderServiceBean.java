package com.company.hunttech.service;

import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.GeoCountryData;
import com.company.hunttech.entity.GeoRegionData;
import com.company.hunttech.entity.Region;
import com.company.hunttech.service.geo.GeoDataProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Реализация массовой загрузки гео-данных (страны) через провайдеры.
 *
 * Источники (по приоритету):
 * 1. restcountries.com (v3.1) — все 250 стран мира, без API-ключа, русские названия, флаги;
 * 2. Локальный справочник популярных стран (lookupLocalCountry) — мгновенно;
 * 3. GeoDataProvider цепочка (Geoapify и др.) — когда настроен API-ключ.
 *
 * Обеспечивает deduplication по ISO-2 коду и скачивание флагов в BLOB (BYTEA).
 */
@Service(GeoBulkLoaderService.NAME)
public class GeoBulkLoaderServiceBean implements GeoBulkLoaderService {

    private static final Logger log = LoggerFactory.getLogger(GeoBulkLoaderServiceBean.class);

    private static final String REST_COUNTRIES_URL =
            "https://raw.githubusercontent.com/mledoze/countries/master/countries.json";

    private static final String FLAG_CDN_URL = "https://flagcdn.com/w320/";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    protected DataManager dataManager;

    @Inject
    protected Metadata metadata;

    @Autowired(required = false)
    private List<GeoDataProvider> geoProviders;

    @Inject
    protected GeoDataEnrichmentService geoDataEnrichmentService;

    // Минимальный набор стран для загрузки без API (lookupLocalCountry)
    private static final String[] DEFAULT_COUNTRIES = {
            "Россия", "Беларусь", "Казахстан", "Узбекистан", "Кыргызстан", "Таджикистан",
            "Армения", "Азербайджан", "Грузия", "Молдова", "Украина",
            "США", "Китай", "Германия", "Франция", "Италия", "Испания", "Польша",
            "Чехия", "Австрия", "Швейцария", "Бельгия", "Нидерланды", "Швеция",
            "Норвегия", "Дания", "Финляндия", "Япония", "Южная Корея", "Индия",
            "Бразилия", "Мексика", "Канада", "Австралия", "Новая Зеландия",
            "Турция", "Иран", "Израиль", "ОАЭ", "Саудовская Аравия", "Египет",
            "ЮАР", "Нигерия", "Кения", "Марокко", "Аргентина", "Чили", "Колумбия"
    };

    @Override
    public String loadAllCountries() {
        long startedAt = System.currentTimeMillis();
        log.info("Начинаем массовую загрузку всех стран мира...");
        // Все счётчики и ошибки — локальны для одного вызова (потокобезопасность синглтона)
        LoadAccumulator acc = new LoadAccumulator();

        // 1. Основной источник: restcountries.com — все страны мира (без API-ключа)
        boolean restOk = loadFromRestCountries(acc);

        // 2. Локальный справочник популярных стран (дозагрузка, если restcountries недоступен)
        if (!restOk) {
            loadFromLocalCatalog(acc);
        }

        // 3. Провайдеры (Geoapify и др.) — только если настроен API-ключ
        if (hasAvailableProviders()) {
            loadFromProviders(acc);
        }

        // 4. Пакетное скачивание флагов в BLOB
        saveFlagsBatch(acc);

        long durationMs = System.currentTimeMillis() - startedAt;
        String summary = buildSummary(acc, durationMs);
        log.info("Массовая загрузка стран завершена: {}", summary);
        return summary;
    }

    /**
     * Локальный аккумулятор счётчиков и ошибок одного вызова loadAllCountries().
     */
    private static class LoadAccumulator {
        private int created;
        private int updated;
        private int skipped;
        private int flagsSaved;
        private final List<String> errors = new ArrayList<>();
    }

    private String buildSummary(LoadAccumulator acc, long durationMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("Загрузка стран завершена за ").append(durationMs / 1000).append(" сек.\n");
        sb.append("Создано новых: ").append(acc.created).append("\n");
        sb.append("Обновлено: ").append(acc.updated).append("\n");
        sb.append("Пропущено (уже есть): ").append(acc.skipped).append("\n");
        sb.append("Флагов сохранено: ").append(acc.flagsSaved).append("\n");
        sb.append("Всего в справочнике: ").append(countTotalCountries()).append("\n");
        if (!acc.errors.isEmpty()) {
            sb.append("Ошибки (").append(acc.errors.size()).append("):\n");
            for (String e : acc.errors) {
                sb.append("- ").append(e).append("\n");
            }
        }
        return sb.toString().trim();
    }

    /**
     * Загрузка ВСЕХ стран из restcountries.com (бесплатно, без API-ключа).
     */
    private boolean loadFromRestCountries(LoadAccumulator acc) {
        try {
            String response = fetchHttpText(REST_COUNTRIES_URL, 15000);
            if (response == null || response.trim().isEmpty()) {
                log.warn("restcountries.com вернул пустой ответ");
                return false;
            }

            JsonNode root = objectMapper.readTree(response);
            if (!root.isArray() || root.size() == 0) {
                log.warn("restcountries.com вернул не массив");
                return false;
            }

            log.info("restcountries.com: получено {} стран", root.size());
            for (JsonNode countryNode : root) {
                try {
                    GeoCountryData geoData = parseRestCountry(countryNode);
                    if (geoData != null && geoData.getCountryShortName() != null) {
                        saveOrUpdateCountry(geoData, acc);
                    }
                } catch (Exception e) {
                    log.debug("Ошибка парсинга страны restcountries: {}", e.getMessage());
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("restcountries.com недоступен: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Парсинг одной страны mledoze/countries.json в GeoCountryData.
     */
    private GeoCountryData parseRestCountry(JsonNode node) {
        try {
            GeoCountryData data = new GeoCountryData();

            String cca2 = node.path("cca2").asText(null);
            if (cca2 == null || cca2.isEmpty()) return null;
            data.setCountryShortName(cca2.toUpperCase());

            data.setAlpha3Code(node.path("cca3").asText(null));
            String ccn3 = node.path("ccn3").asText(null);
            if (ccn3 != null && !ccn3.isEmpty() && !"null".equals(ccn3)) {
                data.setNumericCode(ccn3);
            }

            // Английское название
            data.setCountryEngName(node.path("name").path("common").asText(null));

            // Русское название из translations.rus
            JsonNode rus = node.path("translations").path("rus");
            String rusName = rus.path("common").asText(null);
            if (rusName != null && !rusName.isEmpty()) {
                data.setCountryRuName(rusName);
            } else {
                data.setCountryRuName(data.getCountryEngName());
            }

            // Столица
            JsonNode capital = node.path("capital");
            if (capital.isArray() && capital.size() > 0) {
                data.setCapital(capital.get(0).asText(null));
            }

            // Валюта (первый ключ)
            JsonNode currencies = node.path("currencies");
            if (currencies.isObject() && currencies.size() > 0) {
                Iterator<String> it = currencies.fieldNames();
                if (it.hasNext()) {
                    data.setCurrencyCode(it.next());
                }
            }

            // Телефонный код (корень idd + первый суффикс)
            JsonNode idd = node.path("idd");
            String phone = "";
            if (idd.has("root")) {
                phone = idd.get("root").asText().replace("+", "");
            }
            if (idd.has("suffixes") && idd.get("suffixes").isArray() && idd.get("suffixes").size() > 0) {
                // Для стран с единым кодом суффикс пустой; используем корень
            }
            if (!phone.isEmpty()) {
                try {
                    data.setPhoneCode(Integer.parseInt(phone));
                } catch (NumberFormatException e) {
                    log.debug("Не удалось распарсить телефонный код '{}' для {}", phone, data.getCountryShortName());
                }
            }

            // Флаг: flagcdn.com по ISO-2 (бесплатный CDN, PNG 320px)
            data.setFlagUrl(FLAG_CDN_URL + data.getCountryShortName().toLowerCase() + ".png");

            return data;
        } catch (Exception e) {
            log.debug("Ошибка парсинга страны: {}", e.getMessage());
            return null;
        }
    }

    private void loadFromLocalCatalog(LoadAccumulator acc) {
        log.info("Загрузка стран из локального справочника ({} стран)...", DEFAULT_COUNTRIES.length);
        for (String countryName : DEFAULT_COUNTRIES) {
            try {
                GeoCountryData geoData = geoDataEnrichmentService.enrichCountry(countryName);
                if (geoData != null) {
                    saveOrUpdateCountry(geoData, acc);
                }
            } catch (Exception e) {
                log.warn("Ошибка загрузки страны '{}': {}", countryName, e.getMessage());
                acc.errors.add(countryName + ": " + e.getMessage());
            }
        }
    }

    private void loadFromProviders(LoadAccumulator acc) {
        List<GeoDataProvider> providers = getProvidersInOrder();
        if (providers.isEmpty()) {
            log.warn("Нет доступных GeoDataProvider для загрузки стран");
            return;
        }

        GeoDataProvider primaryProvider = providers.get(0);
        log.info("Загрузка всех стран через провайдер: {}", primaryProvider.getProviderName());

        try {
            List<GeoDataProvider.CountryDTO> allCountries = primaryProvider.fetchAllCountries("ru");
            log.info("Получено стран от провайдера: {}", allCountries.size());

            for (GeoDataProvider.CountryDTO dto : allCountries) {
                if (dto.getIso2() == null || dto.getNameRu() == null) continue;

                String iso2 = dto.getIso2().toUpperCase();
                if (existsCountryByIso2(iso2)) {
                    acc.skipped++;
                    continue;
                }

                GeoCountryData geoData = new GeoCountryData();
                mapCountryDTO(dto, geoData);
                saveOrUpdateCountry(geoData, acc);
            }
        } catch (Exception e) {
            log.error("Ошибка загрузки стран через провайдер {}: {}", primaryProvider.getProviderCode(), e.getMessage());
            acc.errors.add("provider:" + primaryProvider.getProviderCode() + ": " + e.getMessage());
        }
    }

    private void saveOrUpdateCountry(GeoCountryData geoData, LoadAccumulator acc) {
        if (geoData.getCountryShortName() == null || geoData.getCountryShortName().isEmpty()) {
            acc.errors.add(geoData.getCountryRuName() + ": отсутствует ISO код");
            return;
        }

        String iso2 = geoData.getCountryShortName().toUpperCase();

        // Ищем существующую страну: сначала по ISO-2, затем по русскому имени (у старых записей ISO может отсутствовать)
        Country existing = dataManager.load(Country.class)
                .query("select e from hunttech_Country e where upper(e.countryShortName) = :iso2 and e.deleteTs is null")
                .parameter("iso2", iso2)
                .optional()
                .orElse(null);

        if (existing == null && geoData.getCountryRuName() != null && !geoData.getCountryRuName().isEmpty()) {
            existing = dataManager.load(Country.class)
                    .query("select e from hunttech_Country e where e.countryRuName = :name and e.deleteTs is null")
                    .parameter("name", geoData.getCountryRuName())
                    .optional()
                    .orElse(null);
        }

        Country country;
        if (existing != null) {
            country = existing;
            acc.updated++;
        } else {
            country = metadata.create(Country.class);
            country.setCountryShortName(iso2);
            acc.created++;
        }

        // Заполняем поля (не затираем существующие значения, если новые пустые)
        if (geoData.getCountryRuName() != null && !geoData.getCountryRuName().isEmpty()) {
            country.setCountryRuName(geoData.getCountryRuName());
        }
        if (geoData.getCountryEngName() != null && !geoData.getCountryEngName().isEmpty()) {
            country.setCountryEngName(geoData.getCountryEngName());
        }
        if (geoData.getAlpha3Code() != null && !geoData.getAlpha3Code().isEmpty()) {
            country.setAlpha3Code(geoData.getAlpha3Code());
        }
        if (geoData.getNumericCode() != null && !geoData.getNumericCode().isEmpty()) {
            country.setNumericCode(geoData.getNumericCode());
        }
        if (geoData.getPhoneCode() != null) {
            country.setPhoneCode(geoData.getPhoneCode());
        }
        if (geoData.getCurrencyCode() != null && !geoData.getCurrencyCode().isEmpty()) {
            country.setCurrencyCode(geoData.getCurrencyCode());
        }
        if (geoData.getCapital() != null && !geoData.getCapital().isEmpty()) {
            country.setCapital(geoData.getCapital());
        }
        if (geoData.getFlagUrl() != null && !geoData.getFlagUrl().isEmpty()) {
            country.setFlagUrl(geoData.getFlagUrl());
        }

        dataManager.commit(country);

        if (existing == null) {
            log.debug("Создана новая страна: {} ({})", country.getCountryRuName(), iso2);
        } else {
            log.debug("Обновлена страна: {} ({})", country.getCountryRuName(), iso2);
        }
    }

    private void saveFlagsBatch(LoadAccumulator acc) {
        log.info("Пакетное скачивание флагов для стран без флага...");
        List<Country> countriesWithoutFlag = dataManager.load(Country.class)
                .query("select e from hunttech_Country e where e.flagImage is null and e.flagUrl is not null and e.deleteTs is null")
                .list();

        if (countriesWithoutFlag.isEmpty()) {
            log.info("Все страны уже имеют флаги");
            return;
        }

        log.info("Найдено стран без флага: {}", countriesWithoutFlag.size());
        for (Country country : countriesWithoutFlag) {
            try {
                byte[] flagBytes = geoDataEnrichmentService.downloadImageAsBytes(country.getFlagUrl());
                if (flagBytes != null && flagBytes.length > 0) {
                    country.setFlagImage(flagBytes);
                    dataManager.commit(country);
                    acc.flagsSaved++;
                    log.debug("Флаг сохранён для {}: {} байт", country.getCountryRuName(), flagBytes.length);
                }
            } catch (Exception e) {
                log.warn("Не удалось сохранить флаг для {}: {}", country.getCountryRuName(), e.getMessage());
            }
        }
    }

    private boolean existsCountryByIso2(String iso2) {
        Long count = dataManager.loadValue(
                        "select count(e) from hunttech_Country e where upper(e.countryShortName) = :iso2 and e.deleteTs is null",
                        Long.class)
                .parameter("iso2", iso2.toUpperCase())
                .optional()
                .orElse(0L);
        return count > 0;
    }

    private int countTotalCountries() {
        return dataManager.loadValue(
                        "select count(e) from hunttech_Country e where e.deleteTs is null",
                        Integer.class)
                .optional()
                .orElse(0);
    }

    private boolean hasAvailableProviders() {
        return geoProviders != null && !geoProviders.isEmpty();
    }

    private List<GeoDataProvider> getProvidersInOrder() {
        if (geoProviders == null || geoProviders.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> providerOrder = Arrays.asList("geoapify", "htmlweb", "geonames");
        return geoProviders.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(p -> {
                    int idx = providerOrder.indexOf(p.getProviderCode());
                    return idx >= 0 ? idx : Integer.MAX_VALUE;
                }))
                .collect(Collectors.toList());
    }

    private void mapCountryDTO(GeoDataProvider.CountryDTO dto, GeoCountryData data) {
        if (dto.getNameRu() != null) data.setCountryRuName(dto.getNameRu());
        if (dto.getNameEn() != null) data.setCountryEngName(dto.getNameEn());
        if (dto.getIso2() != null) data.setCountryShortName(dto.getIso2());
        if (dto.getIso3() != null) data.setAlpha3Code(dto.getIso3());
        if (dto.getNumericCode() != null) data.setNumericCode(dto.getNumericCode().toString());
        if (dto.getPhoneCode() != null) data.setPhoneCode(dto.getPhoneCode());
        if (dto.getCapitalRu() != null) data.setCapital(dto.getCapitalRu());
        else if (dto.getCapitalEn() != null) data.setCapital(dto.getCapitalEn());
        if (dto.getCurrencyCode() != null) data.setCurrencyCode(dto.getCurrencyCode());
        if (dto.getFlagUrl() != null) data.setFlagUrl(dto.getFlagUrl());
    }

    private String fetchHttpText(String urlStr, int timeoutMs) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "HuntTech-HRM/1.0 (GeoBulkLoader)");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) {
                log.debug("restcountries HTTP {}", code);
                return null;
            }
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            } finally {
                conn.disconnect();
            }
        } catch (Exception e) {
            log.debug("fetchHttpText error: {}", e.getMessage());
            return null;
        }
    }

    // =========================================================================
    // ЗАГРУЗКА РЕГИОНОВ РОССИИ
    // =========================================================================

    @Override
    public String loadAllRegionsForRussia() {
        long startedAt = System.currentTimeMillis();
        log.info("Начинаем массовую загрузку регионов России...");
        LoadAccumulator acc = new LoadAccumulator();

        // Получаем страну Россия
        Country russia = dataManager.load(Country.class)
                .query("select e from hunttech_Country e where e.countryShortName = 'RU' and e.deleteTs is null")
                .optional()
                .orElse(null);
        if (russia == null) {
            acc.errors.add("Страна Россия (RU) не найдена в справочнике");
            return buildSummary(acc, System.currentTimeMillis() - startedAt, "регионов");
        }

        // 1. Провайдеры: Geoapify boundaries level=1 для RU
        if (hasAvailableProviders()) {
            loadRegionsFromProviders(russia, acc);
        }

        // 2. Локальный справочник популярных регионов (fallback)
        loadRegionsFromLocalCatalog(russia, acc);

        // 3. Пакетное скачивание гербов в BLOB
        saveEmblemsBatch(russia, acc);

        long durationMs = System.currentTimeMillis() - startedAt;
        String summary = buildSummary(acc, durationMs, "регионов");
        log.info("Массовая загрузка регионов России завершена: {}", summary);
        return summary;
    }

    private void loadRegionsFromProviders(Country russia, LoadAccumulator acc) {
        List<GeoDataProvider> providers = getProvidersInOrder();
        if (providers.isEmpty()) {
            log.warn("Нет доступных GeoDataProvider для загрузки регионов");
            return;
        }

        GeoDataProvider primaryProvider = providers.get(0);
        log.info("Загрузка регионов России через провайдер: {}", primaryProvider.getProviderName());

        try {
            List<GeoDataProvider.RegionDTO> allRegions = primaryProvider.fetchRegionsForCountry("RU", "ru");
            log.info("Получено регионов от провайдера: {}", allRegions.size());

            for (GeoDataProvider.RegionDTO dto : allRegions) {
                if (dto.getCode() == null || dto.getNameRu() == null) continue;

                String isoCode = dto.getCode().toUpperCase(); // RU-MOW, RU-SPE, etc.
                if (existsRegionByIsoCode(isoCode)) {
                    acc.skipped++;
                    continue;
                }

                GeoRegionData geoData = new GeoRegionData();
                geoData.setRegionRuName(dto.getNameRu());
                geoData.setRegionEngName(dto.getNameEn());
                geoData.setIsoCode(isoCode);
                geoData.setRegionType(dto.getType());
                geoData.setCapital(dto.getCapitalRu() != null ? dto.getCapitalRu() : dto.getCapitalEn());
                geoData.setTimeZone(dto.getTimezone());
                geoData.setKladrCode(dto.getKladrCode());
                geoData.setOkato(dto.getOkato());
                geoData.setOktmo(dto.getOktmo());
                geoData.setFiasId(dto.getFiasId());
                geoData.setCountryName("Россия");

                saveOrUpdateRegion(geoData, russia, acc);
            }
        } catch (Exception e) {
            log.error("Ошибка загрузки регионов через провайдер {}: {}", primaryProvider.getProviderCode(), e.getMessage());
            acc.errors.add("provider:" + primaryProvider.getProviderCode() + ": " + e.getMessage());
        }
    }

    /** Субъект РФ: ISO-код, русское название, тип, адм. центр, часовой пояс, URL герба (Wikimedia). */
    private static class RegionLocalEntry {
        final String iso;
        final String nameRu;
        final String type;
        final String capital;
        final String timeZone;
        final String emblemUrl;

        RegionLocalEntry(String iso, String nameRu, String type, String capital, String timeZone, String emblemUrl) {
            this.iso = iso;
            this.nameRu = nameRu;
            this.type = type;
            this.capital = capital;
            this.timeZone = timeZone;
            this.emblemUrl = emblemUrl;
        }
    }

    /** Полный справочник субъектов РФ (ISO 3166-2:RU, 85 регионов) для массовой миграции.
     *  Гербы — официальные файлы Wikimedia Commons (SVG). */
    private static final List<RegionLocalEntry> RU_REGIONS_LOCAL = Arrays.asList(
            new RegionLocalEntry("RU-AD", "Республика Адыгея", "Республика", "Майкоп", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/3/38/Coat_of_arms_of_Adygea.svg"),
            new RegionLocalEntry("RU-AL", "Республика Алтай", "Республика", "Горно-Алтайск", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/1/1d/Coat_of_arms_of_Altai_Republic.svg"),
            new RegionLocalEntry("RU-ALT", "Алтайский край", "Край", "Барнаул", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/3/3b/Coat_of_arms_of_Altai_Krai.svg"),
            new RegionLocalEntry("RU-AMU", "Амурская область", "Область", "Благовещенск", "UTC+9", "https://upload.wikimedia.org/wikipedia/commons/9/94/Coat_of_arms_of_Amur_Oblast.svg"),
            new RegionLocalEntry("RU-ARK", "Архангельская область", "Область", "Архангельск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/8/8b/Coat_of_Arms_of_Arkhangelsk_Oblast.svg"),
            new RegionLocalEntry("RU-AST", "Астраханская область", "Область", "Астрахань", "UTC+4", "https://upload.wikimedia.org/wikipedia/commons/c/c3/Coat_of_arms_of_Astrakhan_Oblast.svg"),
            new RegionLocalEntry("RU-BA", "Республика Башкортостан", "Республика", "Уфа", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/6/63/Coat_of_arms_of_Bashkortostan.svg"),
            new RegionLocalEntry("RU-BEL", "Белгородская область", "Область", "Белгород", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/c/c0/Coat_of_arms_of_Belgorod_Oblast.svg"),
            new RegionLocalEntry("RU-BRY", "Брянская область", "Область", "Брянск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/4/47/Coat_of_arms_of_Bryansk_Oblast.svg"),
            new RegionLocalEntry("RU-BU", "Республика Бурятия", "Республика", "Улан-Удэ", "UTC+8", "https://upload.wikimedia.org/wikipedia/commons/c/c9/Coat_of_arms_of_Buryatia.svg"),
            new RegionLocalEntry("RU-CE", "Чеченская Республика", "Республика", "Грозный", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/4/4b/Coat_of_arms_of_Chechnya.svg"),
            new RegionLocalEntry("RU-CHE", "Челябинская область", "Область", "Челябинск", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/d/dd/Coat_of_arms_of_Chelyabinsk_Oblast.svg"),
            new RegionLocalEntry("RU-CHU", "Чукотский автономный округ", "Автономный округ", "Анадырь", "UTC+12", "https://upload.wikimedia.org/wikipedia/commons/3/3e/Coat_of_arms_of_Chukotka.svg"),
            new RegionLocalEntry("RU-CU", "Чувашская Республика", "Республика", "Чебоксары", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/c/c5/Coat_of_arms_of_Chuvashia.svg"),
            new RegionLocalEntry("RU-DA", "Республика Дагестан", "Республика", "Махачкала", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/2/22/Coat_of_arms_of_Dagestan.svg"),
            new RegionLocalEntry("RU-IN", "Республика Ингушетия", "Республика", "Магас", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/c/c6/Coat_of_arms_of_Ingushetia.svg"),
            new RegionLocalEntry("RU-IRK", "Иркутская область", "Область", "Иркутск", "UTC+8", "https://upload.wikimedia.org/wikipedia/commons/5/5f/Coat_of_arms_of_Irkutsk_Oblast.svg"),
            new RegionLocalEntry("RU-IVA", "Ивановская область", "Область", "Иваново", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/9e/Coat_of_arms_of_Ivanovo_Oblast.svg"),
            new RegionLocalEntry("RU-KAM", "Камчатский край", "Край", "Петропавловск-Камчатский", "UTC+12", "https://upload.wikimedia.org/wikipedia/commons/2/24/Coat_of_arms_of_Kamchatka_Krai.svg"),
            new RegionLocalEntry("RU-KB", "Кабардино-Балкарская Республика", "Республика", "Нальчик", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/5/51/Coat_of_arms_of_Kabardino-Balkaria.svg"),
            new RegionLocalEntry("RU-KC", "Карачаево-Черкесская Республика", "Республика", "Черкесск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/e/e0/Coat_of_arms_of_Karachay-Cherkessia.svg"),
            new RegionLocalEntry("RU-KDA", "Краснодарский край", "Край", "Краснодар", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/8/86/Coat_of_arms_of_Krasnodar_Krai.svg"),
            new RegionLocalEntry("RU-KEM", "Кемеровская область", "Область", "Кемерово", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Coat_of_arms_of_Kemerovo_Oblast.svg"),
            new RegionLocalEntry("RU-KGD", "Калининградская область", "Область", "Калининград", "UTC+2", "https://upload.wikimedia.org/wikipedia/commons/9/9f/Coat_of_arms_of_Kaliningrad_Oblast.svg"),
            new RegionLocalEntry("RU-KGN", "Курганская область", "Область", "Курган", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/b/bd/Coat_of_arms_of_Kurgan_Oblast.svg"),
            new RegionLocalEntry("RU-KHA", "Хабаровский край", "Край", "Хабаровск", "UTC+10", "https://upload.wikimedia.org/wikipedia/commons/f/f1/Coat_of_arms_of_Khabarovsk_Krai.svg"),
            new RegionLocalEntry("RU-KHM", "Ханты-Мансийский автономный округ — Югра", "Автономный округ", "Ханты-Мансийск", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/6/6a/Coat_of_arms_of_Khanty-Mansiysk.svg"),
            new RegionLocalEntry("RU-KIR", "Кировская область", "Область", "Киров", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/1/1f/Coat_of_arms_of_Kirov_Oblast.svg"),
            new RegionLocalEntry("RU-KK", "Республика Хакасия", "Республика", "Абакан", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/c/c7/Coat_of_arms_of_Khakassia.svg"),
            new RegionLocalEntry("RU-KL", "Республика Калмыкия", "Республика", "Элиста", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/99/Coat_of_arms_of_Kalmykia.svg"),
            new RegionLocalEntry("RU-KLU", "Калужская область", "Область", "Калуга", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/3/33/Coat_of_arms_of_Kaluga_Oblast.svg"),
            new RegionLocalEntry("RU-KO", "Республика Коми", "Республика", "Сыктывкар", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/5/5c/Coat_of_arms_of_the_Komi_Republic.svg"),
            new RegionLocalEntry("RU-KOS", "Костромская область", "Область", "Кострома", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/98/Coat_of_arms_of_Kostroma_Oblast.svg"),
            new RegionLocalEntry("RU-KR", "Республика Карелия", "Республика", "Петрозаводск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/e/ea/Coat_of_arms_of_Republic_of_Karelia.svg"),
            new RegionLocalEntry("RU-KRS", "Курская область", "Область", "Курск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/2/2c/Coat_of_arms_of_Kursk_Oblast.svg"),
            new RegionLocalEntry("RU-KYA", "Красноярский край", "Край", "Красноярск", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/2/2e/Coat_of_arms_of_Krasnoyarsk_Krai.svg"),
            new RegionLocalEntry("RU-LEN", "Ленинградская область", "Область", "Санкт-Петербург", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/c/c1/Coat_of_arms_of_Leningrad_Oblast.svg"),
            new RegionLocalEntry("RU-LIP", "Липецкая область", "Область", "Липецк", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/3/3c/Coat_of_arms_of_Lipetsk_Oblast.svg"),
            new RegionLocalEntry("RU-MAG", "Магаданская область", "Область", "Магадан", "UTC+11", "https://upload.wikimedia.org/wikipedia/commons/1/11/Coat_of_arms_of_Magadan_Oblast.svg"),
            new RegionLocalEntry("RU-ME", "Республика Марий Эл", "Республика", "Йошкар-Ола", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/2/2a/Coat_of_arms_of_Mari_El.svg"),
            new RegionLocalEntry("RU-MO", "Республика Мордовия", "Республика", "Саранск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/3/3c/Coat_of_arms_of_Mordovia.svg"),
            new RegionLocalEntry("RU-MOS", "Московская область", "Область", "Красногорск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/5/53/Coat_of_arms_of_Moscow_oblast.svg"),
            new RegionLocalEntry("RU-MOW", "Москва", "Город федерального значения", "Москва", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/c/ca/Coat_of_Arms_of_Moscow.svg"),
            new RegionLocalEntry("RU-MUR", "Мурманская область", "Область", "Мурманск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/95/Coat_of_arms_of_Murmansk_Oblast.svg"),
            new RegionLocalEntry("RU-NEN", "Ненецкий автономный округ", "Автономный округ", "Нарьян-Мар", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/6/67/Coat_of_arms_of_Nenets_Autonomous_Okrug.svg"),
            new RegionLocalEntry("RU-NGR", "Новгородская область", "Область", "Великий Новгород", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/e/e0/Coat_of_arms_of_Novgorod_Oblast.svg"),
            new RegionLocalEntry("RU-NIZ", "Нижегородская область", "Область", "Нижний Новгород", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/9e/Coat_of_arms_of_Nizhny_Novgorod_Oblast.svg"),
            new RegionLocalEntry("RU-NVS", "Новосибирская область", "Область", "Новосибирск", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/4/4b/Coat_of_arms_of_Novosibirsk_Oblast.svg"),
            new RegionLocalEntry("RU-OMS", "Омская область", "Область", "Омск", "UTC+6", "https://upload.wikimedia.org/wikipedia/commons/8/8d/Coat_of_arms_of_Omsk_Oblast.svg"),
            new RegionLocalEntry("RU-ORE", "Оренбургская область", "Область", "Оренбург", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/2/25/Coat_of_arms_of_Orenburg_Oblast.svg"),
            new RegionLocalEntry("RU-ORL", "Орловская область", "Область", "Орёл", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/c/c6/Coat_of_arms_of_Oryol_Oblast.svg"),
            new RegionLocalEntry("RU-PER", "Пермский край", "Край", "Пермь", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/2/28/Coat_of_arms_of_Perm_Krai.svg"),
            new RegionLocalEntry("RU-PNZ", "Пензенская область", "Область", "Пенза", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/8/85/Coat_of_arms_of_Penza_Oblast.svg"),
            new RegionLocalEntry("RU-PRI", "Приморский край", "Край", "Владивосток", "UTC+10", "https://upload.wikimedia.org/wikipedia/commons/c/c3/Coat_of_arms_of_Primorsky_Krai.svg"),
            new RegionLocalEntry("RU-PSK", "Псковская область", "Область", "Псков", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/f/f0/Coat_of_arms_of_Pskov_Oblast.svg"),
            new RegionLocalEntry("RU-ROS", "Ростовская область", "Область", "Ростов-на-Дону", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/1/14/Coat_of_arms_of_Rostov_Oblast.svg"),
            new RegionLocalEntry("RU-RYA", "Рязанская область", "Область", "Рязань", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/8/8d/Coat_of_arms_of_Ryazan_Oblast.svg"),
            new RegionLocalEntry("RU-SA", "Республика Саха (Якутия)", "Республика", "Якутск", "UTC+9", "https://upload.wikimedia.org/wikipedia/commons/c/c1/Coat_of_arms_of_the_Sakha_Republic.svg"),
            new RegionLocalEntry("RU-SAK", "Сахалинская область", "Область", "Южно-Сахалинск", "UTC+11", "https://upload.wikimedia.org/wikipedia/commons/c/c5/Coat_of_arms_of_Sakhalin_Oblast.svg"),
            new RegionLocalEntry("RU-SAM", "Самарская область", "Область", "Самара", "UTC+4", "https://upload.wikimedia.org/wikipedia/commons/4/4a/Coat_of_arms_of_Samara_Oblast.svg"),
            new RegionLocalEntry("RU-SAR", "Саратовская область", "Область", "Саратов", "UTC+4", "https://upload.wikimedia.org/wikipedia/commons/9/95/Coat_of_arms_of_Saratov_Oblast.svg"),
            new RegionLocalEntry("RU-SE", "Республика Северная Осетия — Алания", "Республика", "Владикавказ", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/6/60/Coat_of_arms_of_North_Ossetia.svg"),
            new RegionLocalEntry("RU-SMO", "Смоленская область", "Область", "Смоленск", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/7/72/Coat_of_arms_of_Smolensk_Oblast.svg"),
            new RegionLocalEntry("RU-SPE", "Санкт-Петербург", "Город федерального значения", "Санкт-Петербург", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/5/53/Coat_of_Arms_of_Saint_Petersburg_%282003%29.svg"),
            new RegionLocalEntry("RU-STA", "Ставропольский край", "Край", "Ставрополь", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/98/Coat_of_arms_of_Stavropol_Krai.svg"),
            new RegionLocalEntry("RU-SVE", "Свердловская область", "Область", "Екатеринбург", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/4/4f/Coat_of_arms_of_Sverdlovsk_Oblast.svg"),
            new RegionLocalEntry("RU-TA", "Республика Татарстан", "Республика", "Казань", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/e/eb/Coat_of_arms_of_Tatarstan.svg"),
            new RegionLocalEntry("RU-TAM", "Тамбовская область", "Область", "Тамбов", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/9f/Coat_of_arms_of_Tambov_Oblast.svg"),
            new RegionLocalEntry("RU-TOM", "Томская область", "Область", "Томск", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/c/c6/Coat_of_arms_of_Tomsk_Oblast.svg"),
            new RegionLocalEntry("RU-TUL", "Тульская область", "Область", "Тула", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/3/3f/Coat_of_arms_of_Tula_Oblast.svg"),
            new RegionLocalEntry("RU-TVE", "Тверская область", "Область", "Тверь", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/b/b4/Coat_of_arms_of_Tver_Oblast.svg"),
            new RegionLocalEntry("RU-TY", "Республика Тыва", "Республика", "Кызыл", "UTC+7", "https://upload.wikimedia.org/wikipedia/commons/9/9d/Coat_of_arms_of_Tuva.svg"),
            new RegionLocalEntry("RU-TYU", "Тюменская область", "Область", "Тюмень", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/c/c3/Coat_of_arms_of_Tyumen_Oblast.svg"),
            new RegionLocalEntry("RU-UD", "Удмуртская Республика", "Республика", "Ижевск", "UTC+4", "https://upload.wikimedia.org/wikipedia/commons/c/c1/Coat_of_arms_of_Udmurtia.svg"),
            new RegionLocalEntry("RU-ULY", "Ульяновская область", "Область", "Ульяновск", "UTC+4", "https://upload.wikimedia.org/wikipedia/commons/f/f0/Coat_of_arms_of_Ulyanovsk_Oblast.svg"),
            new RegionLocalEntry("RU-VGG", "Волгоградская область", "Область", "Волгоград", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/9/9d/Coat_of_arms_of_Volgograd_Oblast.svg"),
            new RegionLocalEntry("RU-VLA", "Владимирская область", "Область", "Владимир", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/6/64/Coat_of_arms_of_Vladimir_Oblast.svg"),
            new RegionLocalEntry("RU-VLG", "Вологодская область", "Область", "Вологда", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/7/7a/Coat_of_arms_of_Vologda_Oblast.svg"),
            new RegionLocalEntry("RU-VOR", "Воронежская область", "Область", "Воронеж", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/7/7e/Coat_of_arms_of_Voronezh_Oblast.svg"),
            new RegionLocalEntry("RU-YAN", "Ямало-Ненецкий автономный округ", "Автономный округ", "Салехард", "UTC+5", "https://upload.wikimedia.org/wikipedia/commons/1/1a/Coat_of_arms_of_Yamalo-Nenets_Autonomous_Okrug.svg"),
            new RegionLocalEntry("RU-YAR", "Ярославская область", "Область", "Ярославль", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/7/7b/Coat_of_arms_of_Yaroslavl_Oblast.svg"),
            new RegionLocalEntry("RU-YEV", "Еврейская автономная область", "Автономная область", "Биробиджан", "UTC+10", "https://upload.wikimedia.org/wikipedia/commons/4/4b/Coat_of_arms_of_Jewish_Autonomous_Oblast.svg"),
            new RegionLocalEntry("RU-ZAB", "Забайкальский край", "Край", "Чита", "UTC+9", "https://upload.wikimedia.org/wikipedia/commons/c/c4/Coat_of_arms_of_Zabaykalsky_Krai.svg"),
            new RegionLocalEntry("RU-CR", "Республика Крым", "Республика", "Симферополь", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/8/86/Gerb_kryma.svg"),
            new RegionLocalEntry("RU-SEV", "Севастополь", "Город федерального значения", "Севастополь", "UTC+3", "https://upload.wikimedia.org/wikipedia/commons/1/19/Coat_of_Arms_of_Sevastopol.svg")
    );

    private void loadRegionsFromLocalCatalog(Country russia, LoadAccumulator acc) {
        log.info("Загрузка всех регионов России из локального справочника ({} субъектов)...", RU_REGIONS_LOCAL.size());

        for (RegionLocalEntry entry : RU_REGIONS_LOCAL) {
            try {
                GeoRegionData geoData = new GeoRegionData();
                geoData.setRegionRuName(entry.nameRu);
                geoData.setRegionEngName(null);
                geoData.setIsoCode(entry.iso);
                geoData.setRegionType(entry.type);
                geoData.setCapital(entry.capital);
                geoData.setTimeZone(entry.timeZone);
                geoData.setEmblemUrl(entry.emblemUrl);
                geoData.setCountryName("Россия");
                saveOrUpdateRegion(geoData, russia, acc);
            } catch (Exception e) {
                log.warn("Ошибка загрузки региона '{}': {}", entry.nameRu, e.getMessage());
                acc.errors.add(entry.nameRu + ": " + e.getMessage());
            }
        }
    }

    private void saveOrUpdateRegion(GeoRegionData geoData, Country russia, LoadAccumulator acc) {
        if (geoData.getRegionRuName() == null || geoData.getRegionRuName().isEmpty()) {
            acc.errors.add("регион без названия: " + (geoData.getIsoCode() != null ? geoData.getIsoCode() : "unknown"));
            return;
        }

        // Ищем существующий регион: сначала по ISO коду, затем по названию (без фильтра
        // страны — у старых записей region_country может быть NULL, но unique constraint
        // на region_ru_name глобальный, поэтому поиск только по имени обязателен).
        // View включает regionCountry: чтение региона и проверка страны не должны
        // вызывать Unfetched Attribute Access (правило Data View Integrity).
        Region existing = null;
        if (geoData.getIsoCode() != null && !geoData.getIsoCode().isEmpty()) {
            existing = dataManager.load(Region.class)
                    .query("select e from hunttech_Region e where e.isoCode = :isoCode and e.deleteTs is null")
                    .view("region-edit-view")
                    .parameter("isoCode", geoData.getIsoCode().toUpperCase())
                    .optional()
                    .orElse(null);
        }
        if (existing == null) {
            existing = dataManager.load(Region.class)
                    .query("select e from hunttech_Region e where e.regionRuName = :name and e.deleteTs is null")
                    .view("region-edit-view")
                    .parameter("name", geoData.getRegionRuName())
                    .optional()
                    .orElse(null);
        }

        Region region;
        if (existing != null) {
            region = existing;
            if (region.getRegionCountry() == null) {
                region.setRegionCountry(russia);
            }
            acc.updated++;
        } else {
            region = metadata.create(Region.class);
            region.setRegionCountry(russia);
            if (geoData.getIsoCode() != null) region.setIsoCode(geoData.getIsoCode().toUpperCase());
            acc.created++;
        }

        if (geoData.getRegionRuName() != null && !geoData.getRegionRuName().isEmpty()) {
            region.setRegionRuName(geoData.getRegionRuName());
        }
        if (geoData.getRegionEngName() != null && !geoData.getRegionEngName().isEmpty()) {
            region.setRegionEngName(geoData.getRegionEngName());
        }
        if (geoData.getRegionType() != null && !geoData.getRegionType().isEmpty()) {
            region.setRegionType(geoData.getRegionType());
        }
        if (geoData.getCapital() != null && !geoData.getCapital().isEmpty()) {
            region.setCapital(geoData.getCapital());
        }
        if (geoData.getTimeZone() != null && !geoData.getTimeZone().isEmpty()) {
            region.setTimeZone(geoData.getTimeZone());
        }
        if (geoData.getFiasId() != null && !geoData.getFiasId().isEmpty()) {
            region.setFiasId(geoData.getFiasId());
        }
        if (geoData.getEmblemUrl() != null && !geoData.getEmblemUrl().isEmpty()) {
            region.setEmblemUrl(geoData.getEmblemUrl());
        }

        dataManager.commit(region);

        if (existing == null) {
            log.debug("Создан новый регион: {} ({})", region.getRegionRuName(), region.getIsoCode());
        } else {
            log.debug("Обновлён регион: {} ({})", region.getRegionRuName(), region.getIsoCode());
        }
    }

    private boolean existsRegionByIsoCode(String isoCode) {
        return dataManager.loadValue(
                        "select count(e) from hunttech_Region e where upper(e.isoCode) = :isoCode and e.deleteTs is null",
                        Long.class)
                .parameter("isoCode", isoCode.toUpperCase())
                .optional()
                .orElse(0L) > 0;
    }

    private void saveEmblemsBatch(Country russia, LoadAccumulator acc) {
        log.info("Пакетное скачивание гербов для регионов без герба...");
        List<Region> regionsWithoutEmblem = dataManager.load(Region.class)
                .query("select e from hunttech_Region e where e.emblemImage is null and e.emblemUrl is not null and e.regionCountry = :country and e.deleteTs is null")
                .parameter("country", russia)
                .list();

        if (regionsWithoutEmblem.isEmpty()) {
            log.info("Все регионы уже имеют гербы");
            return;
        }

        log.info("Найдено регионов без герба: {}", regionsWithoutEmblem.size());
        int idx = 0;
        for (Region region : regionsWithoutEmblem) {
            try {
                // Wikimedia rate limit (429): пауза между запросами и ретрай при 429
                idx++;
                byte[] emblemBytes = downloadEmblemWithRetry(region.getEmblemUrl(), idx);
                if (emblemBytes != null && emblemBytes.length > 0) {
                    region.setEmblemImage(emblemBytes);
                    dataManager.commit(region);
                    acc.flagsSaved++; // используем поле flagsSaved как emblemsSaved
                    log.info("Герб сохранён для {}: {} байт", region.getRegionRuName(), emblemBytes.length);
                } else {
                    log.warn("Не удалось скачать герб для {} (url={})", region.getRegionRuName(), region.getEmblemUrl());
                }
            } catch (Exception e) {
                log.warn("Не удалось сохранить герб для {}: {}", region.getRegionRuName(), e.getMessage());
            }
            // Пауза между запросами: Wikimedia ограничивает частоту (429 Too Many Requests)
            try {
                Thread.sleep(1200);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Скачивает герб с ретраями при HTTP 429 (Wikimedia rate limit).
     * После первого 429 ждёт 10 секунд и повторяет до 3 раз.
     */
    private byte[] downloadEmblemWithRetry(String url, int index) {
        byte[] result = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            result = geoDataEnrichmentService.downloadImageAsBytes(url);
            if (result != null && result.length > 0) {
                return result;
            }
            log.warn("Попытка {}/3 скачать герб #{} не удалась (вероятно 429), пауза 10 сек...", attempt, index);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return result;
    }

    private String buildSummary(LoadAccumulator acc, long durationMs, String entityType) {
        StringBuilder sb = new StringBuilder();
        sb.append("Загрузка ").append(entityType).append(" завершена за ").append(durationMs / 1000).append(" сек.\n");
        sb.append("Создано новых: ").append(acc.created).append("\n");
        sb.append("Обновлено: ").append(acc.updated).append("\n");
        sb.append("Пропущено (уже есть): ").append(acc.skipped).append("\n");
        sb.append("Гербов сохранено: ").append(acc.flagsSaved).append("\n");
        sb.append("Всего в справочнике: ").append(countTotalRegions()).append("\n");
        if (!acc.errors.isEmpty()) {
            sb.append("Ошибки (").append(acc.errors.size()).append("):\n");
            for (String e : acc.errors) {
                sb.append("- ").append(e).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private int countTotalRegions() {
        return dataManager.loadValue(
                        "select count(e) from hunttech_Region e where e.deleteTs is null",
                        Integer.class)
                .optional()
                .orElse(0);
    }
}