package com.company.hunttech.service;

import com.company.hunttech.entity.*;
import com.company.hunttech.service.geo.GeoDataProvider;
import com.company.hunttech.service.geo.GeoapifyGeoProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.core.global.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service(GeoDataEnrichmentService.NAME)
public class GeoDataEnrichmentServiceBean implements GeoDataEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(GeoDataEnrichmentServiceBean.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    protected DataManager dataManager;
    @Inject
    protected Metadata metadata;
    @Inject
    protected FileLoader fileLoader;

    @Autowired(required = false)
    private List<GeoDataProvider> geoProviders;

    // Порядок fallback провайдеров (можно переопределить через конфигурацию)
    private static final List<String> DEFAULT_PROVIDER_ORDER = Arrays.asList(
            "geoapify",  // основной: русские названия, работает из РФ, 3000 req/day free
            "htmlweb",   // fallback: 20 req/day free
            "geonames"   // fallback: 3000 req/day free, нужен username
    );

    @Override
    public GeoCountryData enrichCountry(String countryNameOrCode) {
        if (countryNameOrCode == null || countryNameOrCode.trim().isEmpty()) {
            return null;
        }
        String query = countryNameOrCode.trim();
        log.info("Обогащение данных страны: query='{}'", query);

        GeoCountryData data = new GeoCountryData();
        data.setCountryRuName(query);

        // 1. Поиск во встроенном справочнике популярных стран (быстрый локальный)
        GeoCountryData localMatch = lookupLocalCountry(query);
        if (localMatch != null) {
            copyProperties(localMatch, data);
        }

        // 2. Онлайн-запрос через цепочку провайдеров
        enrichCountryFromProviders(query, data);

        // 3. Fallback: поиск флага через Wikipedia/Wikimedia, если URL флага не получен
        if (data.getFlagUrl() == null || data.getFlagUrl().isEmpty()) {
            String wikiFlag = fetchWikipediaThumbnail("Флаг_" + (data.getCountryRuName() != null ? data.getCountryRuName() : query));
            if (wikiFlag != null) {
                data.setFlagUrl(wikiFlag);
            }
        }

        data.setRawSnippet("Получены данные страны: " + (data.getCountryRuName() != null ? data.getCountryRuName() : query)
                + " (ISO: " + (data.getCountryShortName() != null ? data.getCountryShortName() : "-") + ")");
        return data;
    }

    /**
     * Обогащение страны через цепочку провайдеров
     */
    private void enrichCountryFromProviders(String query, GeoCountryData data) {
        List<GeoDataProvider> providers = getProvidersInOrder();
        if (providers.isEmpty()) {
            log.warn("Нет доступных GeoDataProvider для обогащения страны: {}", query);
            // Fallback на старый restcountries.com
            enrichCountryFromRestCountries(query, data);
            return;
        }

        for (GeoDataProvider provider : providers) {
            if (!provider.supportsCountries()) continue;
            try {
                log.debug("Попытка обогащения страны '{}' через провайдер: {}", query, provider.getProviderCode());
                GeoDataProvider.CountryDTO dto = provider.findCountry(query, "ru");
                if (dto != null && dto.getNameRu() != null) {
                    mapCountryDTO(dto, data);
                    log.info("Страна '{}' успешно обогащена через провайдер: {}", query, provider.getProviderCode());
                    return; // успех — прерываем цепочку
                }
            } catch (Exception e) {
                log.warn("Провайдер {} ошибка для страны '{}': {}", provider.getProviderCode(), query, e.getMessage());
            }
        }
        log.warn("Все провайдеры не смогли обогатить страну: {}", query);
        // Последний fallback
        enrichCountryFromRestCountries(query, data);
    }

    private void enrichCountryFromRestCountries(String query, GeoCountryData data) {
        try {
            String urlStr = query.length() <= 3 && isAscii(query)
                    ? "https://restcountries.com/v3.1/alpha/" + URLEncoder.encode(query, "UTF-8")
                    : "https://restcountries.com/v3.1/name/" + URLEncoder.encode(query, "UTF-8") + "?fullText=false";

            String response = fetchHttpText(urlStr, 4000);
            if (response != null && response.startsWith("[")) {
                JsonNode root = objectMapper.readTree(response);
                if (root.isArray() && root.size() > 0) {
                    JsonNode countryNode = root.get(0);
                    parseRestCountryJson(countryNode, data);
                }
            }
        } catch (Exception e) {
            log.info("Запрос к restcountries.com для '{}' завершился: {}", query, e.getMessage());
        }
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

    @Override
    public GeoRegionData enrichRegion(String regionName, Country country) {
        if (regionName == null || regionName.trim().isEmpty()) {
            return null;
        }
        String query = regionName.trim();
        log.info("Обогащение данных региона: region='{}', country='{}'", query, country != null ? country.getCountryRuName() : "null");

        GeoRegionData data = new GeoRegionData();
        data.setRegionRuName(query);
        if (country != null) {
            data.setCountryName(country.getCountryRuName());
        }

        // 1. Локальный справочник для РФ
        lookupLocalRegion(query, data);

        // 2. Провайдеры для регионов (если локальный не дал результат)
        if (data.getRegionType() == null || data.getEmblemUrl() == null) {
            enrichRegionFromProviders(query, country, data);
        }

        // 3. Fallback: герб через Wikipedia
        if (data.getEmblemUrl() == null || data.getEmblemUrl().isEmpty()) {
            String emblemUrl = fetchWikipediaThumbnail("Герб_" + query);
            if (emblemUrl != null) {
                data.setEmblemUrl(emblemUrl);
            }
        }

        data.setRawSnippet("Получены данные региона: " + query + " (" + (data.getRegionType() != null ? data.getRegionType() : "регион") + ")");
        return data;
    }

    private void enrichRegionFromProviders(String query, Country country, GeoRegionData data) {
        List<GeoDataProvider> providers = getProvidersInOrder();
        String countryIso2 = country != null ? country.getCountryShortName() : null;

        for (GeoDataProvider provider : providers) {
            if (!provider.supportsRegions()) continue;
            try {
                GeoDataProvider.RegionDTO dto = provider.findRegion(query, countryIso2, "ru");
                if (dto != null && dto.getNameRu() != null) {
                    mapRegionDTO(dto, data);
                    log.info("Регион '{}' обогащён через провайдер: {}", query, provider.getProviderCode());
                    break;
                }
            } catch (Exception e) {
                log.warn("Провайдер {} ошибка для региона '{}': {}", provider.getProviderCode(), query, e.getMessage());
            }
        }
    }

    private void mapRegionDTO(GeoDataProvider.RegionDTO dto, GeoRegionData data) {
        if (dto.getNameRu() != null) data.setRegionRuName(dto.getNameRu());
        if (dto.getNameEn() != null) data.setRegionEngName(dto.getNameEn());
        if (dto.getCode() != null) data.setIsoCode(dto.getCode());
        if (dto.getType() != null) data.setRegionType(dto.getType());
        if (dto.getCapitalRu() != null) data.setCapital(dto.getCapitalRu());
        if (dto.getTimezone() != null) data.setTimeZone(dto.getTimezone());
        if (dto.getKladrCode() != null) data.setKladrCode(dto.getKladrCode());
        if (dto.getOkato() != null) data.setOkato(dto.getOkato());
        if (dto.getOktmo() != null) data.setOktmo(dto.getOktmo());
        if (dto.getFiasId() != null) data.setFiasId(dto.getFiasId());
    }

    @Override
    public GeoCityData enrichCity(String cityName, Region region, Country country) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return null;
        }
        String query = cityName.trim();
        log.info("Обогащение данных города: city='{}', region='{}'", query, region != null ? region.getRegionRuName() : "null");

        GeoCityData data = new GeoCityData();
        data.setCityRuName(query);
        if (region != null) {
            data.setRegionName(region.getRegionRuName());
        }
        if (country != null) {
            data.setCountryName(country.getCountryRuName());
        }

        // 1. Локальный справочник для крупных городов
        lookupLocalCity(query, data);

        // 2. Провайдеры для городов
        if (data.getLatitude() == null || data.getEmblemUrl() == null) {
            enrichCityFromProviders(query, region, country, data);
        }

        // 3. Fallback: герб через Wikipedia
        if (data.getEmblemUrl() == null || data.getEmblemUrl().isEmpty()) {
            String emblemUrl = fetchWikipediaThumbnail("Герб_" + query);
            if (emblemUrl != null) {
                data.setEmblemUrl(emblemUrl);
            }
        }

        data.setRawSnippet("Получены данные города: " + query);
        return data;
    }

    private void enrichCityFromProviders(String query, Region region, Country country, GeoCityData data) {
        List<GeoDataProvider> providers = getProvidersInOrder();
        String regionCode = region != null ? region.getIsoCode() : null;
        String countryIso2 = country != null ? country.getCountryShortName() : null;

        for (GeoDataProvider provider : providers) {
            if (!provider.supportsCities()) continue;
            try {
                GeoDataProvider.CityDTO dto = provider.findCity(query, regionCode, countryIso2, "ru");
                if (dto != null && dto.getNameRu() != null) {
                    mapCityDTO(dto, data);
                    log.info("Город '{}' обогащён через провайдер: {}", query, provider.getProviderCode());
                    break;
                }
            } catch (Exception e) {
                log.warn("Провайдер {} ошибка для города '{}': {}", provider.getProviderCode(), query, e.getMessage());
            }
        }
    }

    private void mapCityDTO(GeoDataProvider.CityDTO dto, GeoCityData data) {
        if (dto.getNameRu() != null) data.setCityRuName(dto.getNameRu());
        if (dto.getNameEn() != null) data.setCityEngName(dto.getNameEn());
        if (dto.getLatitude() != null) data.setLatitude(dto.getLatitude());
        if (dto.getLongitude() != null) data.setLongitude(dto.getLongitude());
        if (dto.getPopulation() != null) data.setPopulation(dto.getPopulation());
        if (dto.getTimezone() != null) data.setTimeZone(dto.getTimezone());
        if (dto.getPostalCode() != null) data.setPostalCode(dto.getPostalCode());
        if (dto.getPhoneCode() != null) data.setCityPhoneCode(dto.getPhoneCode());
        if (dto.getAirportIata() != null) data.setAirportCodeIata(dto.getAirportIata());
        if (dto.getAirportIcao() != null) data.setAirportCodeIcao(dto.getAirportIcao());
        if (dto.getGeonameId() != null) data.setGeonameId(dto.getGeonameId());
        if (dto.getWikiLink() != null) data.setWikiLink(dto.getWikiLink());
        if (dto.getIsCapital() != null) data.setIsCapital(dto.getIsCapital());
    }

    /**
     * Возвращает список провайдеров в порядке приоритета (fallback)
     */
    private List<GeoDataProvider> getProvidersInOrder() {
        if (geoProviders == null || geoProviders.isEmpty()) {
            return Collections.emptyList();
        }
        // Сортируем по DEFAULT_PROVIDER_ORDER
        return geoProviders.stream()
                .filter(p -> p != null)
                .sorted(Comparator.comparingInt(p -> {
                    int idx = DEFAULT_PROVIDER_ORDER.indexOf(p.getProviderCode());
                    return idx >= 0 ? idx : 999;
                }))
                .collect(Collectors.toList());
    }

    @Override
    public FileDescriptor downloadAndSaveImage(String imageUrl, String fileName) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return null;
        }
        try {
            String url = imageUrl.trim();
            if (url.startsWith("//")) {
                url = "https:" + url;
            }
            byte[] imageBytes = fetchHttpBytes(url, 5000);
            if (imageBytes == null || imageBytes.length == 0) {
                return null;
            }

            String ext = "png";
            if (imageBytes.length >= 8 && imageBytes[0] == (byte) 0x89 && imageBytes[1] == (byte) 0x50 && imageBytes[2] == (byte) 0x4E && imageBytes[3] == (byte) 0x47) {
                ext = "png";
            } else if (imageBytes.length >= 3 && imageBytes[0] == (byte) 0xFF && imageBytes[1] == (byte) 0xD8 && imageBytes[2] == (byte) 0xFF) {
                ext = "jpg";
            } else if (url.toLowerCase().endsWith(".svg") || new String(imageBytes, 0, Math.min(imageBytes.length, 100), StandardCharsets.UTF_8).contains("<svg")) {
                ext = "svg";
            } else if (url.toLowerCase().endsWith(".webp")) {
                ext = "webp";
            }

            String baseName = fileName != null && !fileName.trim().isEmpty()
                    ? fileName.trim()
                    : "geo_image_" + System.currentTimeMillis();
            if (baseName.contains(".")) {
                baseName = baseName.substring(0, baseName.lastIndexOf("."));
            }
            String effectiveName = baseName + "." + ext;

            FileDescriptor fd = metadata.create(FileDescriptor.class);
            fd.setName(effectiveName);
            fd.setExtension(ext);
            fd.setSize((long) imageBytes.length);
            fd.setCreateDate(new Date());

            fileLoader.saveStream(fd, () -> new ByteArrayInputStream(imageBytes));
            FileDescriptor committed = dataManager.commit(fd);
            log.info("Гео-изображение успешно сохранено: id={}, name='{}', size={} байт", committed.getId(), committed.getName(), committed.getSize());
            return committed;
        } catch (Exception e) {
            log.warn("Не удалось сохранить изображение по URL '{}': {}", imageUrl, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean testGeoApiConnection(String apiKey, String secretKey, String apiUrl) {
        HttpURLConnection conn = null;
        try {
            String urlStr = apiUrl != null && !apiUrl.trim().isEmpty() ? apiUrl.trim() : "https://suggestions.dadata.ru/suggestions/api/4_1/rs/suggest/address";
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            if (apiKey != null && !apiKey.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Token " + apiKey.trim());
            }
            if (secretKey != null && !secretKey.trim().isEmpty()) {
                conn.setRequestProperty("X-Secret", secretKey.trim());
            }
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write("{\"query\":\"Москва\"}".getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            return code >= 200 && code < 400;
        } catch (Exception e) {
            log.info("Тест подключения к Geo API завершился ошибкой: {}", e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void parseRestCountryJson(JsonNode countryNode, GeoCountryData data) {
        if (countryNode == null || data == null) return;
        try {
            if (countryNode.has("cca2") && (data.getCountryShortName() == null || data.getCountryShortName().isEmpty())) {
                data.setCountryShortName(countryNode.get("cca2").asText());
            }
            if (countryNode.has("cca3") && (data.getAlpha3Code() == null || data.getAlpha3Code().isEmpty())) {
                data.setAlpha3Code(countryNode.get("cca3").asText());
            }
            if (countryNode.has("ccn3") && (data.getNumericCode() == null || data.getNumericCode().isEmpty())) {
                data.setNumericCode(countryNode.get("ccn3").asText());
            }
            if (countryNode.has("name")) {
                JsonNode nameNode = countryNode.get("name");
                if (nameNode.has("common") && (data.getCountryEngName() == null || data.getCountryEngName().isEmpty())) {
                    data.setCountryEngName(nameNode.get("common").asText());
                }
            }
            if (countryNode.has("translations")) {
                JsonNode rusNode = countryNode.get("translations").get("rus");
                if (rusNode != null && rusNode.has("common")) {
                    data.setCountryRuName(rusNode.get("common").asText());
                }
            }
            if (countryNode.has("capital") && countryNode.get("capital").isArray() && countryNode.get("capital").size() > 0) {
                if (data.getCapital() == null || data.getCapital().isEmpty()) {
                    data.setCapital(countryNode.get("capital").get(0).asText());
                }
            }
            if (countryNode.has("currencies")) {
                Iterator<String> fieldNames = countryNode.get("currencies").fieldNames();
                if (fieldNames.hasNext() && (data.getCurrencyCode() == null || data.getCurrencyCode().isEmpty())) {
                    data.setCurrencyCode(fieldNames.next());
                }
            }
            if (countryNode.has("idd")) {
                JsonNode idd = countryNode.get("idd");
                String root = idd.has("root") ? idd.get("root").asText() : "";
                String fullCode = root.replace("+", "").trim();
                if (!fullCode.isEmpty() && data.getPhoneCode() == null) {
                    try {
                        data.setPhoneCode(Integer.parseInt(fullCode));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (countryNode.has("flags")) {
                JsonNode flags = countryNode.get("flags");
                if (flags.has("png") && (data.getFlagUrl() == null || data.getFlagUrl().isEmpty())) {
                    data.setFlagUrl(flags.get("png").asText());
                } else if (flags.has("svg") && (data.getFlagUrl() == null || data.getFlagUrl().isEmpty())) {
                    data.setFlagUrl(flags.get("svg").asText());
                }
            }
        } catch (Exception e) {
            log.warn("Ошибка парсинга JSON страны: {}", e.getMessage());
        }
    }

    private String fetchWikipediaThumbnail(String title) {
        try {
            String url = "https://ru.wikipedia.org/w/api.php?action=query&prop=pageimages&format=json&pithumbsize=400&titles=" + URLEncoder.encode(title.replace(" ", "_"), "UTF-8");
            String json = fetchHttpText(url, 3000);
            if (json != null && json.contains("\"source\"")) {
                JsonNode root = objectMapper.readTree(json);
                JsonNode pages = root.path("query").path("pages");
                Iterator<JsonNode> it = pages.elements();
                if (it.hasNext()) {
                    JsonNode page = it.next();
                    if (page.has("thumbnail") && page.get("thumbnail").has("source")) {
                        return page.get("thumbnail").get("source").asText();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String fetchHttpText(String urlStr, int timeoutMs) {
        try {
            byte[] bytes = fetchHttpBytes(urlStr, timeoutMs);
            return bytes != null ? new String(bytes, StandardCharsets.UTF_8) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] fetchHttpBytes(String urlStr, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 HuntTech-HRM/1.0");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) return null;
            try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int n;
                int total = 0;
                while ((n = in.read(buf)) != -1 && total < 10485760) {
                    out.write(buf, 0, n);
                    total += n;
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean isAscii(String str) {
        for (char c : str.toCharArray()) {
            if (c > 127) return false;
        }
        return true;
    }

    private GeoCountryData lookupLocalCountry(String query) {
        String q = query.toLowerCase().trim();
        GeoCountryData d = new GeoCountryData();
        if (q.contains("росси") || q.contains("russia") || q.equals("ru") || q.equals("rus") || q.contains("рф")) {
            d.setCountryRuName("Россия");
            d.setCountryEngName("Russia");
            d.setCountryShortName("RU");
            d.setAlpha3Code("RUS");
            d.setNumericCode("643");
            d.setPhoneCode(7);
            d.setCurrencyCode("RUB");
            d.setCapital("Москва");
            d.setFlagUrl("https://flagcdn.com/w320/ru.png");
            return d;
        } else if (q.contains("беларус") || q.contains("belarus") || q.equals("by") || q.equals("blr")) {
            d.setCountryRuName("Беларусь");
            d.setCountryEngName("Belarus");
            d.setCountryShortName("BY");
            d.setAlpha3Code("BLR");
            d.setNumericCode("112");
            d.setPhoneCode(375);
            d.setCurrencyCode("BYN");
            d.setCapital("Минск");
            d.setFlagUrl("https://flagcdn.com/w320/by.png");
            return d;
        } else if (q.contains("казах") || q.contains("kazakh") || q.equals("kz") || q.equals("kaz")) {
            d.setCountryRuName("Казахстан");
            d.setCountryEngName("Kazakhstan");
            d.setCountryShortName("KZ");
            d.setAlpha3Code("KAZ");
            d.setNumericCode("398");
            d.setPhoneCode(7);
            d.setCurrencyCode("KZT");
            d.setCapital("Астана");
            d.setFlagUrl("https://flagcdn.com/w320/kz.png");
            return d;
        } else if (q.contains("китай") || q.contains("china") || q.equals("cn") || q.equals("chn")) {
            d.setCountryRuName("Китай");
            d.setCountryEngName("China");
            d.setCountryShortName("CN");
            d.setAlpha3Code("CHN");
            d.setNumericCode("156");
            d.setPhoneCode(86);
            d.setCurrencyCode("CNY");
            d.setCapital("Пекин");
            d.setFlagUrl("https://flagcdn.com/w320/cn.png");
            return d;
        } else if (q.contains("сша") || q.contains("usa") || q.equals("us") || q.contains("штат")) {
            d.setCountryRuName("США");
            d.setCountryEngName("United States");
            d.setCountryShortName("US");
            d.setAlpha3Code("USA");
            d.setNumericCode("840");
            d.setPhoneCode(1);
            d.setCurrencyCode("USD");
            d.setCapital("Вашингтон");
            d.setFlagUrl("https://flagcdn.com/w320/us.png");
            return d;
        } else if (q.contains("герман") || q.contains("germany") || q.equals("de") || q.equals("deu")) {
            d.setCountryRuName("Германия");
            d.setCountryEngName("Germany");
            d.setCountryShortName("DE");
            d.setAlpha3Code("DEU");
            d.setNumericCode("276");
            d.setPhoneCode(49);
            d.setCurrencyCode("EUR");
            d.setCapital("Берлин");
            d.setFlagUrl("https://flagcdn.com/w320/de.png");
            return d;
        }
        return null;
    }

    private void lookupLocalRegion(String query, GeoRegionData data) {
        String q = query.toLowerCase().trim();
        if (q.contains("москва") || q.equals("77")) {
            data.setRegionRuName("Москва");
            data.setRegionEngName("Moscow");
            data.setRegionCode(77);
            data.setIsoCode("RU-MOW");
            data.setRegionType("Город федерального значения");
            data.setCapital("Москва");
            data.setTimeZone("UTC+3");
            data.setEmblemUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Coat_of_Arms_of_Moscow.svg/240px-Coat_of_Arms_of_Moscow.svg.png");
        } else if (q.contains("петербург") || q.contains("спб") || q.equals("78")) {
            data.setRegionRuName("Санкт-Петербург");
            data.setRegionEngName("Saint Petersburg");
            data.setRegionCode(78);
            data.setIsoCode("RU-SPE");
            data.setRegionType("Город федерального значения");
            data.setCapital("Санкт-Петербург");
            data.setTimeZone("UTC+3");
            data.setEmblemUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Coat_of_Arms_of_Saint_Petersburg_%282003%29.svg/240px-Coat_of_Arms_of_Saint_Petersburg_%282003%29.svg.png");
        } else if (q.contains("московск") || q.equals("50")) {
            data.setRegionRuName("Московская область");
            data.setRegionEngName("Moscow Oblast");
            data.setRegionCode(50);
            data.setIsoCode("RU-MOS");
            data.setRegionType("Область");
            data.setCapital("Красногорск");
            data.setTimeZone("UTC+3");
        } else if (q.contains("татарстан") || q.equals("16")) {
            data.setRegionRuName("Республика Татарстан");
            data.setRegionEngName("Republic of Tatarstan");
            data.setRegionCode(16);
            data.setIsoCode("RU-TA");
            data.setRegionType("Республика");
            data.setCapital("Казань");
            data.setTimeZone("UTC+3");
        } else if (q.contains("свердловск") || q.equals("66")) {
            data.setRegionRuName("Свердловская область");
            data.setRegionEngName("Sverdlovsk Oblast");
            data.setRegionCode(66);
            data.setIsoCode("RU-SVE");
            data.setRegionType("Область");
            data.setCapital("Екатеринбург");
            data.setTimeZone("UTC+5");
        } else if (q.contains("новосибирск") || q.equals("54")) {
            data.setRegionRuName("Новосибирская область");
            data.setRegionEngName("Novosibirsk Oblast");
            data.setRegionCode(54);
            data.setIsoCode("RU-NVS");
            data.setRegionType("Область");
            data.setCapital("Новосибирск");
            data.setTimeZone("UTC+7");
        }
    }

    private void lookupLocalCity(String query, GeoCityData data) {
        String q = query.toLowerCase().trim();
        if (q.contains("москва")) {
            data.setCityRuName("Москва");
            data.setCityEngName("Moscow");
            data.setCityPhoneCode("495");
            data.setPostalCode("101000");
            data.setPopulation(13100000L);
            data.setLatitude(55.7558);
            data.setLongitude(37.6173);
            data.setTimeZone("UTC+3");
            data.setRegionName("Москва");
            data.setEmblemUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/c/ca/Coat_of_Arms_of_Moscow.svg/240px-Coat_of_Arms_of_Moscow.svg.png");
        } else if (q.contains("петербург") || q.contains("спб")) {
            data.setCityRuName("Санкт-Петербург");
            data.setCityEngName("Saint Petersburg");
            data.setCityPhoneCode("812");
            data.setPostalCode("190000");
            data.setPopulation(5600000L);
            data.setLatitude(59.9343);
            data.setLongitude(30.3351);
            data.setTimeZone("UTC+3");
            data.setRegionName("Санкт-Петербург");
            data.setEmblemUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/5/53/Coat_of_Arms_of_Saint_Petersburg_%282003%29.svg/240px-Coat_of_Arms_of_Saint_Petersburg_%282003%29.svg.png");
        } else if (q.contains("новосибирск")) {
            data.setCityRuName("Новосибирск");
            data.setCityEngName("Novosibirsk");
            data.setCityPhoneCode("383");
            data.setPostalCode("630000");
            data.setPopulation(1630000L);
            data.setLatitude(55.0084);
            data.setLongitude(82.9357);
            data.setTimeZone("UTC+7");
            data.setRegionName("Новосибирская область");
        } else if (q.contains("екатеринбург")) {
            data.setCityRuName("Екатеринбург");
            data.setCityEngName("Yekaterinburg");
            data.setCityPhoneCode("343");
            data.setPostalCode("620000");
            data.setPopulation(1500000L);
            data.setLatitude(56.8389);
            data.setLongitude(60.6057);
            data.setTimeZone("UTC+5");
            data.setRegionName("Свердловская область");
        } else if (q.contains("казань")) {
            data.setCityRuName("Казань");
            data.setCityEngName("Kazan");
            data.setCityPhoneCode("843");
            data.setPostalCode("420000");
            data.setPopulation(1300000L);
            data.setLatitude(55.8304);
            data.setLongitude(49.0661);
            data.setTimeZone("UTC+3");
            data.setRegionName("Республика Татарстан");
        } else if (q.contains("нижний новгород")) {
            data.setCityRuName("Нижний Новгород");
            data.setCityEngName("Nizhny Novgorod");
            data.setCityPhoneCode("831");
            data.setPostalCode("603000");
            data.setPopulation(1250000L);
            data.setLatitude(56.2965);
            data.setLongitude(43.9361);
            data.setTimeZone("UTC+3");
            data.setRegionName("Нижегородская область");
        }
    }

    private void copyProperties(GeoCountryData src, GeoCountryData dest) {
        if (src.getCountryRuName() != null) dest.setCountryRuName(src.getCountryRuName());
        if (src.getCountryEngName() != null) dest.setCountryEngName(src.getCountryEngName());
        if (src.getCountryShortName() != null) dest.setCountryShortName(src.getCountryShortName());
        if (src.getAlpha3Code() != null) dest.setAlpha3Code(src.getAlpha3Code());
        if (src.getNumericCode() != null) dest.setNumericCode(src.getNumericCode());
        if (src.getPhoneCode() != null) dest.setPhoneCode(src.getPhoneCode());
        if (src.getCurrencyCode() != null) dest.setCurrencyCode(src.getCurrencyCode());
        if (src.getCapital() != null) dest.setCapital(src.getCapital());
        if (src.getFlagUrl() != null) dest.setFlagUrl(src.getFlagUrl());
    }
}
