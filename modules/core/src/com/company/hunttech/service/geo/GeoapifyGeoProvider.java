package com.company.hunttech.service.geo;

import com.company.hunttech.core.ai.AiSecretService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Geoapify Geo Provider — провайдер гео-данных через Geoapify API.
 *
 * Особенности:
 * - Русские названия (lang=ru)
 * - Работает из РФ без VPN
 * - Бесплатный тариф: 3000 запросов/день
 * - Поддерживает страны, регионы, города, границы (GeoJSON)
 * - API ключ шифруется через AiSecretService
 *
 * @see <a href="https://apidocs.geoapify.com/docs/geocoding/forward-geocoding">Geoapify Geocoding API</a>
 * @see <a href="https://geoapify.com/boundaries-api">Geoapify Boundaries API</a>
 */
@Service("hunttech_GeoapifyGeoProvider")
public class GeoapifyGeoProvider implements GeoDataProvider {

    private static final Logger log = LoggerFactory.getLogger(GeoapifyGeoProvider.class);

    private static final String BASE_URL = "https://api.geoapify.com/v1";
    private static final String GEOCODING_URL = BASE_URL + "/geocode/search";
    private static final String BOUNDARIES_URL = BASE_URL + "/boundaries";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    protected AiSecretService secretService;

    @Autowired(required = false)
    private String geoapifyApiKeyEncrypted; // зашифрованный ключ из конфигурации/БД

    @Override
    public String getProviderCode() {
        return "geoapify";
    }

    @Override
    public String getProviderName() {
        return "Geoapify";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public boolean worksFromRussia() {
        return true; // Geoapify доступен из РФ
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Arrays.asList("ru", "en", "de", "fr", "es", "it", "pt", "zh", "ja", "ko", "ar");
    }

    @Override
    public CountryDTO findCountry(String query, String language) {
        if (query == null || query.trim().isEmpty()) return null;
        String lang = normalizeLanguage(language);
        try {
            String url = buildUrl(GEOCODING_URL, Map.of(
                    "text", query.trim(),
                    "type", "country",
                    "lang", lang,
                    "limit", "5",
                    "apiKey", getApiKey()
            ));
            String response = fetchHttpText(url, 5000);
            if (response == null) return null;

            JsonNode root = objectMapper.readTree(response);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.size() == 0) return null;

            // Берём первый результат с type=country
            for (JsonNode feature : features) {
                JsonNode props = feature.path("properties");
                if ("country".equals(props.path("type").asText())) {
                    return mapCountry(props, lang);
                }
            }
            // Fallback: первый результат
            return mapCountry(features.get(0).path("properties"), lang);
        } catch (Exception e) {
            log.warn("Geoapify findCountry error for '{}': {}", query, e.getMessage());
            return null;
        }
    }

    @Override
    public RegionDTO findRegion(String regionQuery, String countryIso2, String language) {
        if (regionQuery == null || regionQuery.trim().isEmpty()) return null;
        String lang = normalizeLanguage(language);
        try {
            String text = regionQuery.trim() + (countryIso2 != null ? ", " + countryIso2 : "");
            String url = buildUrl(GEOCODING_URL, Map.of(
                    "text", text,
                    "type", "state",
                    "lang", lang,
                    "limit", "5",
                    "apiKey", getApiKey()
            ));
            String response = fetchHttpText(url, 5000);
            if (response == null) return null;

            JsonNode root = objectMapper.readTree(response);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.size() == 0) return null;

            for (JsonNode feature : features) {
                JsonNode props = feature.path("properties");
                if ("state".equals(props.path("type").asText()) || "region".equals(props.path("type").asText())) {
                    return mapRegion(props, countryIso2, lang);
                }
            }
            return mapRegion(features.get(0).path("properties"), countryIso2, lang);
        } catch (Exception e) {
            log.warn("Geoapify findRegion error for '{}': {}", regionQuery, e.getMessage());
            return null;
        }
    }

    @Override
    public CityDTO findCity(String cityQuery, String regionCode, String countryIso2, String language) {
        if (cityQuery == null || cityQuery.trim().isEmpty()) return null;
        String lang = normalizeLanguage(language);
        try {
            StringBuilder text = new StringBuilder(cityQuery.trim());
            if (regionCode != null) text.append(", ").append(regionCode);
            if (countryIso2 != null) text.append(", ").append(countryIso2);

            String url = buildUrl(GEOCODING_URL, Map.of(
                    "text", text.toString(),
                    "type", "city",
                    "lang", lang,
                    "limit", "5",
                    "apiKey", getApiKey()
            ));
            String response = fetchHttpText(url, 5000);
            if (response == null) return null;

            JsonNode root = objectMapper.readTree(response);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.size() == 0) return null;

            for (JsonNode feature : features) {
                JsonNode props = feature.path("properties");
                String type = props.path("type").asText();
                if ("city".equals(type) || "town".equals(type) || "village".equals(type) || "municipality".equals(type)) {
                    return mapCity(props, regionCode, countryIso2, lang);
                }
            }
            return mapCity(features.get(0).path("properties"), regionCode, countryIso2, lang);
        } catch (Exception e) {
            log.warn("Geoapify findCity error for '{}': {}", cityQuery, e.getMessage());
            return null;
        }
    }

    @Override
    public List<CountryDTO> fetchAllCountries(String language) {
        String lang = normalizeLanguage(language);
        List<CountryDTO> countries = new ArrayList<>();
        try {
            // Geoapify не имеет прямого эндпоинта "все страны", используем границы мира
            String url = buildUrl(BOUNDARIES_URL, Map.of(
                    "level", "0", // country level
                    "lang", lang,
                    "apiKey", getApiKey()
            ));
            String response = fetchHttpText(url, 10000);
            if (response == null) return countries;

            JsonNode root = objectMapper.readTree(response);
            JsonNode features = root.path("features");
            if (!features.isArray()) return countries;

            for (JsonNode feature : features) {
                JsonNode props = feature.path("properties");
                CountryDTO dto = mapCountryFromBoundary(props, lang);
                if (dto != null) countries.add(dto);
            }
        } catch (Exception e) {
            log.warn("Geoapify fetchAllCountries error: {}", e.getMessage());
        }
        return countries;
    }

    @Override
    public List<RegionDTO> fetchRegionsForCountry(String countryIso2, String language) {
        String lang = normalizeLanguage(language);
        List<RegionDTO> regions = new ArrayList<>();
        try {
            String url = buildUrl(BOUNDARIES_URL, Map.of(
                    "level", "1", // state/region level
                    "country", countryIso2,
                    "lang", lang,
                    "apiKey", getApiKey()
            ));
            String response = fetchHttpText(url, 10000);
            if (response == null) return regions;

            JsonNode root = objectMapper.readTree(response);
            JsonNode features = root.path("features");
            if (!features.isArray()) return regions;

            for (JsonNode feature : features) {
                JsonNode props = feature.path("properties");
                RegionDTO dto = mapRegionFromBoundary(props, countryIso2, lang);
                if (dto != null) regions.add(dto);
            }
        } catch (Exception e) {
            log.warn("Geoapify fetchRegionsForCountry error for {}: {}", countryIso2, e.getMessage());
        }
        return regions;
    }

    @Override
    public List<CityDTO> fetchCitiesForRegion(String regionCode, String countryIso2, String language) {
        // Geoapify не имеет прямого эндпоинта для городов региона через boundaries
        // Используем geocoding с фильтром по региону — пока возвращаем пустой список
        // TODO: реализовать через geocoding с bbox региона
        return Collections.emptyList();
    }

    @Override
    public boolean testConnection(Map<String, String> credentials) {
        String apiKey = credentials != null ? credentials.get("apiKey") : null;
        if (apiKey == null || apiKey.isEmpty()) {
            apiKey = getApiKey();
        }
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Geoapify testConnection: no API key");
            return false;
        }
        try {
            String url = buildUrl(GEOCODING_URL, Map.of(
                    "text", "Moscow",
                    "type", "city",
                    "limit", "1",
                    "apiKey", apiKey
            ));
            String response = fetchHttpText(url, 3000);
            if (response == null) return false;
            JsonNode root = objectMapper.readTree(response);
            return root.path("features").isArray();
        } catch (Exception e) {
            log.warn("Geoapify testConnection error: {}", e.getMessage());
            return false;
        }
    }

    // ===== Mapping methods =====

    private CountryDTO mapCountry(JsonNode props, String lang) {
        CountryDTO dto = new CountryDTO();
        dto.setIso2(props.path("country_code").asText(null));
        dto.setIso3(props.path("country_code3").asText(null));
        // nameRu всегда заполняем (API возвращает country на языке запроса lang)
        dto.setNameRu(props.path("country").asText(null));
        dto.setNameEn(props.path("country_en").asText(null));
        dto.setCapitalRu(props.path("capital").asText(null));
        dto.setCapitalEn(props.path("capital_en").asText(null));
        dto.setContinent(props.path("continent").asText(null));
        dto.setTimezone(props.path("timezone").asText(null));
        dto.setCurrencyCode(props.path("currency").asText(null));
        dto.setFlagUrl(props.path("flag").asText(null));
        dto.setFlagEmoji(props.path("flag_emoji").asText(null));
        dto.setProviderSpecific(Map.of(
                "place_id", props.path("place_id").asText(null),
                "confidence", props.path("confidence").asInt(0)
        ));
        return dto;
    }

    private CountryDTO mapCountryFromBoundary(JsonNode props, String lang) {
        CountryDTO dto = new CountryDTO();
        dto.setIso2(props.path("iso_3166_1_alpha_2").asText(null));
        dto.setIso3(props.path("iso_3166_1_alpha_3").asText(null));
        dto.setNumericCode(props.path("iso_3166_1_numeric").asInt(0) != 0 ? props.path("iso_3166_1_numeric").asInt() : null);
        dto.setNameRu(props.path("name_" + lang).asText(null));
        dto.setNameEn(props.path("name_en").asText(null));
        dto.setCapitalRu(props.path("capital_" + lang).asText(null));
        dto.setCapitalEn(props.path("capital_en").asText(null));
        dto.setContinent(props.path("continent").asText(null));
        dto.setTimezone(props.path("timezone").asText(null));
        dto.setCurrencyCode(props.path("currency").asText(null));
        dto.setFlagUrl(props.path("flag").asText(null));
        dto.setFlagEmoji(props.path("flag_emoji").asText(null));
        dto.setProviderSpecific(Map.of(
                "osm_id", props.path("osm_id").asText(null),
                "boundary_type", props.path("boundary").asText(null)
        ));
        return dto;
    }

    private RegionDTO mapRegion(JsonNode props, String countryIso2, String lang) {
        RegionDTO dto = new RegionDTO();
        dto.setCountryIso2(countryIso2);
        dto.setCode(props.path("state_code").asText(null));
        dto.setNameRu(props.path("state").asText(null));
        dto.setNameEn(props.path("state_en").asText(null));
        dto.setType(mapRegionType(props.path("type").asText()));
        dto.setCapitalRu(props.path("capital").asText(null));
        dto.setCapitalEn(props.path("capital_en").asText(null));
        dto.setTimezone(props.path("timezone").asText(null));
        dto.setProviderSpecific(Map.of(
                "place_id", props.path("place_id").asText(null),
                "confidence", props.path("confidence").asInt(0)
        ));
        return dto;
    }

    private RegionDTO mapRegionFromBoundary(JsonNode props, String countryIso2, String lang) {
        RegionDTO dto = new RegionDTO();
        dto.setCountryIso2(countryIso2);
        dto.setCode(props.path("iso_3166_2").asText(null));
        dto.setNameRu(props.path("name_" + lang).asText(null));
        dto.setNameEn(props.path("name_en").asText(null));
        dto.setType(props.path("type").asText(null));
        dto.setCapitalRu(props.path("capital_" + lang).asText(null));
        dto.setCapitalEn(props.path("capital_en").asText(null));
        dto.setTimezone(props.path("timezone").asText(null));
        dto.setProviderSpecific(Map.of(
                "osm_id", props.path("osm_id").asText(null),
                "boundary_type", props.path("boundary").asText(null)
        ));
        return dto;
    }

    private CityDTO mapCity(JsonNode props, String regionCode, String countryIso2, String lang) {
        CityDTO dto = new CityDTO();
        dto.setCountryIso2(countryIso2);
        dto.setRegionCode(regionCode != null ? regionCode : props.path("state_code").asText(null));
        dto.setNameRu(props.path("city").asText(null));
        dto.setNameEn(props.path("city_en").asText(null));
        dto.setNameAlt(props.path("suburb").asText(null));
        dto.setLatitude(props.path("lat").isNumber() ? props.path("lat").asDouble() : null);
        dto.setLongitude(props.path("lon").isNumber() ? props.path("lon").asDouble() : null);
        dto.setTimezone(props.path("timezone").asText(null));
        dto.setPostalCode(props.path("postcode").asText(null));
        dto.setProviderSpecific(Map.of(
                "place_id", props.path("place_id").asText(null),
                "confidence", props.path("confidence").asInt(0),
                "type", props.path("type").asText(null)
        ));
        return dto;
    }

    // ===== Helpers =====

    private String getApiKey() {
        // 1. Попытка расшифровать из конфигурации
        if (geoapifyApiKeyEncrypted != null && !geoapifyApiKeyEncrypted.isEmpty()) {
            try {
                return secretService.decrypt(geoapifyApiKeyEncrypted);
            } catch (Exception e) {
                log.warn("Не удалось расшифровать Geoapify API ключ: {}", e.getMessage());
            }
        }
        // 2. Fallback: переменная окружения (для dev)
        return System.getenv("GEOAPIFY_API_KEY");
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isEmpty()) return "ru";
        String l = language.toLowerCase();
        if (l.startsWith("ru")) return "ru";
        if (l.startsWith("en")) return "en";
        if (l.startsWith("de")) return "de";
        if (l.startsWith("fr")) return "fr";
        if (l.startsWith("es")) return "es";
        return "ru";
    }

    private String mapRegionType(String type) {
        if (type == null) return "region";
        String t = type.toLowerCase();
        if (t.contains("city") || t.contains("municipal")) return "city";
        if (t.contains("obl") || t.contains("province") || t.contains("state")) return "region";
        if (t.contains("republic")) return "republic";
        if (t.contains("krai")) return "krai";
        if (t.contains("okrug") || t.contains("district")) return "district";
        return "region";
    }

    private String buildUrl(String base, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(base).append("?");
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (!first) sb.append("&");
            first = false;
            sb.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String fetchHttpText(String urlStr, int timeoutMs) {
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "HuntTech-HRM/1.0 (Geoapify Provider)");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) {
                log.debug("Geoapify HTTP {}", code);
                return null;
            }
            try (java.io.InputStream in = conn.getInputStream();
                 java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            log.debug("Geoapify fetch error: {}", e.getMessage());
            return null;
        }
    }
}