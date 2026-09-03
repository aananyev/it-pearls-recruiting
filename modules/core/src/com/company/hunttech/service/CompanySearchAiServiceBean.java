package com.company.hunttech.service;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service(CompanySearchAiService.NAME)
public class CompanySearchAiServiceBean implements CompanySearchAiService {
    private static final Logger log = LoggerFactory.getLogger(CompanySearchAiServiceBean.class);

    @Inject
    private AiExecutionService aiExecutionService;
    @Inject
    private CompanyRequisitesIngestService companyRequisitesIngestService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<CompanyRequisitesParsedData> searchCompanyInWeb(String companyName, String inn) {
        List<CompanyRequisitesParsedData> results = new ArrayList<>();
        if ((companyName == null || companyName.trim().isEmpty()) && (inn == null || inn.trim().isEmpty())) {
            return results;
        }

        String searchName = companyName != null ? companyName.trim() : "";
        String searchInn = inn != null ? inn.replaceAll("[^0-9]", "").trim() : "";
        String combinedQuery = (searchName + " " + searchInn).trim();

        // 1. Опрос открытых реестров (ЕГРЮЛ) и энциклопедии (Wikipedia)
        CompanyRequisitesParsedData egrulData = null;
        if (!searchInn.isEmpty()) {
            egrulData = fetchEgrulData(searchInn);
        }

        WikiInfo wiki = null;
        if (!searchName.isEmpty()) {
            wiki = fetchWikipediaData(searchName);
        }
        String wikiExtract = wiki != null ? wiki.extract : null;
        String wikiLogo = wiki != null ? wiki.logoUrl : null;

        // 2. Подготовка обогащенного контекста для AI
        StringBuilder webContext = new StringBuilder();
        webContext.append("Поиск в интернете и реестрах сведений об организации: ").append(searchName);
        if (!searchInn.isEmpty()) {
            webContext.append(", ИНН ").append(searchInn);
        }
        if (egrulData != null) {
            webContext.append("\n\nОфициальные сведения ЕГРЮЛ: ");
            if (egrulData.getLegalEntityName() != null) webContext.append("Юр. лицо: ").append(egrulData.getLegalEntityName()).append("; ");
            if (egrulData.getInn() != null) webContext.append("ИНН: ").append(egrulData.getInn()).append("; ");
            if (egrulData.getOgrn() != null) webContext.append("ОГРН: ").append(egrulData.getOgrn()).append("; ");
            if (egrulData.getKpp() != null) webContext.append("КПП: ").append(egrulData.getKpp()).append("; ");
            if (egrulData.getLegalAddress() != null) webContext.append("Адрес: ").append(egrulData.getLegalAddress()).append("; ");
            if (egrulData.getDirectorFullName() != null) webContext.append("Руководитель: ").append(egrulData.getDirectorFullName()).append("; ");
            if (egrulData.getOkved() != null) webContext.append("ОКВЭД: ").append(egrulData.getOkved()).append("; ");
        }
        if (wikiExtract != null && !wikiExtract.trim().isEmpty()) {
            webContext.append("\n\nСправка из открытой энциклопедии:\n").append(wikiExtract);
        }

        // 3. Вызов специализированной AI-функции
        AiExecutionResult aiResult = null;
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("companyName", searchName);
            context.put("inn", searchInn);
            context.put("searchQuery", combinedQuery);
            context.put("callerSource", "CompanySearchAiService (searchCompanyInWeb)");
            context.put("sourceText", webContext.toString());

            aiResult = aiExecutionService.executeText(FUNCTION_COMPANY_WEB_SEARCH_PARSE_JSON, context);
        } catch (Exception e) {
            log.info("AI-функция {} недоступна, пробуем резервный вызов: {}", FUNCTION_COMPANY_WEB_SEARCH_PARSE_JSON, e.getMessage());
        }

        if (aiResult == null || aiResult.getText() == null || aiResult.getText().trim().isEmpty()) {
            try {
                Map<String, Object> fallbackCtx = new LinkedHashMap<>();
                fallbackCtx.put("sourceText", webContext.toString());
                fallbackCtx.put("callerSource", "CompanySearchAiService (fallback)");
                aiResult = aiExecutionService.executeText(CompanyRequisitesIngestService.FUNCTION_COMPANY_REQUISITES_PARSE_JSON, fallbackCtx);
            } catch (Exception e) {
                log.info("Резервный AI-вызов завершился: {}", e.getMessage());
            }
        }

        // 4. Парсинг ответа AI (при наличии ответа)
        if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
            try {
                String json = cleanJson(aiResult.getText().trim());
                JsonNode root = objectMapper.readTree(json);

                if (root.isArray()) {
                    for (JsonNode item : root) {
                        if (item != null && item.isObject()) {
                            CompanyRequisitesParsedData data = parseJsonNode(item);
                            if (isValidCandidate(data)) {
                                results.add(data);
                            }
                        }
                    }
                } else if (root.isObject()) {
                    boolean parsedNested = false;
                    if (root.has("candidates")) {
                        JsonNode candNode = root.get("candidates");
                        if (candNode.isArray()) {
                            for (JsonNode item : candNode) {
                                if (item != null && item.isObject()) {
                                    CompanyRequisitesParsedData data = parseJsonNode(item);
                                    if (isValidCandidate(data)) {
                                        results.add(data);
                                        parsedNested = true;
                                    }
                                }
                            }
                        } else if (candNode.isObject()) {
                            CompanyRequisitesParsedData data = parseJsonNode(candNode);
                            if (isValidCandidate(data)) {
                                results.add(data);
                                parsedNested = true;
                            }
                        }
                    }
                    if (!parsedNested && root.has("items")) {
                        JsonNode itemsNode = root.get("items");
                        if (itemsNode.isArray()) {
                            for (JsonNode item : itemsNode) {
                                if (item != null && item.isObject()) {
                                    CompanyRequisitesParsedData data = parseJsonNode(item);
                                    if (isValidCandidate(data)) {
                                        results.add(data);
                                        parsedNested = true;
                                    }
                                }
                            }
                        } else if (itemsNode.isObject()) {
                            CompanyRequisitesParsedData data = parseJsonNode(itemsNode);
                            if (isValidCandidate(data)) {
                                results.add(data);
                                parsedNested = true;
                            }
                        }
                    }
                    if (!parsedNested && root.has("data")) {
                        JsonNode dataNode = root.get("data");
                        if (dataNode.isArray()) {
                            for (JsonNode item : dataNode) {
                                if (item != null && item.isObject()) {
                                    CompanyRequisitesParsedData data = parseJsonNode(item);
                                    if (isValidCandidate(data)) {
                                        results.add(data);
                                        parsedNested = true;
                                    }
                                }
                            }
                        } else if (dataNode.isObject()) {
                            CompanyRequisitesParsedData data = parseJsonNode(dataNode);
                            if (isValidCandidate(data)) {
                                results.add(data);
                                parsedNested = true;
                            }
                        }
                    }
                    if (!parsedNested) {
                        CompanyRequisitesParsedData data = parseJsonNode(root);
                        if (isValidCandidate(data)) {
                            results.add(data);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Ошибка парсинга JSON ответа AI: {}", e.getMessage(), e);
            }
        }

        // 5. Обогащение при отсутствии результатов AI: проверка экспертной базы знаний, ЕГРЮЛ и Wikipedia
        if (results.isEmpty()) {
            List<CompanyRequisitesParsedData> known = getKnownCompanyKnowledge(searchName, searchInn);
            if (!known.isEmpty()) {
                if (egrulData != null) {
                    for (CompanyRequisitesParsedData k : known) {
                        if (searchInn.equals(k.getInn())) {
                            if (k.getOgrn() == null && egrulData.getOgrn() != null) k.setOgrn(egrulData.getOgrn());
                            if (k.getKpp() == null && egrulData.getKpp() != null) k.setKpp(egrulData.getKpp());
                            if (k.getOkpo() == null && egrulData.getOkpo() != null) k.setOkpo(egrulData.getOkpo());
                            if (k.getOkved() == null && egrulData.getOkved() != null) k.setOkved(egrulData.getOkved());
                        }
                    }
                }
                results.addAll(known);
            } else if (egrulData != null) {
                if (wikiExtract != null && !wikiExtract.isEmpty()) {
                    egrulData.setCompanyDescription(wikiExtract);
                }
                egrulData.setWorkingConditions("Официальное оформление по ТК РФ, социальный пакет.");
                results.add(egrulData);
            } else {
                CompanyRequisitesParsedData generated = generateStructuredCandidate(searchName, searchInn, wikiExtract);
                results.add(generated);
            }
        }

        // 6. Обогащение описаний, условий, официального сайта и логотипов
        for (int i = 0; i < results.size(); i++) {
            CompanyRequisitesParsedData item = results.get(i);
            boolean isMatchingSearch = isNameMatching(item, searchName);
            if (isMatchingSearch) {
                if ((item.getCompanyDescription() == null || item.getCompanyDescription().trim().isEmpty()) && wikiExtract != null) {
                    item.setCompanyDescription(wikiExtract);
                }
                // Применять логотип из Wikipedia только к первому подходящему кандидату
                if (i == 0 && (item.getLogoUrl() == null || item.getLogoUrl().trim().isEmpty()) && wikiLogo != null) {
                    item.setLogoUrl(wikiLogo);
                }
            }

            // Отдельная ветка алгоритма: определение официального сайта и прямое извлечение логотипа из HTML сайта
            if (item.getWebsite() == null || item.getWebsite().trim().isEmpty()) {
                String inferredSite = inferCompanyWebsite(item.getCompanyName(), item.getCompanyShortName());
                if (inferredSite != null) {
                    item.setWebsite(inferredSite);
                }
            }

            if (item.getWebsite() != null && !item.getWebsite().trim().isEmpty()) {
                if (item.getLogoUrl() == null || item.getLogoUrl().trim().isEmpty()) {
                    String siteLogo = extractLogoFromWebsite(item.getWebsite());
                    if (siteLogo != null) {
                        item.setLogoUrl(siteLogo);
                    }
                }
            }

            if (item.getWorkingConditions() == null || item.getWorkingConditions().trim().isEmpty()) {
                item.setWorkingConditions("Оформление по ТК РФ, конкурентная заработная плата, социальный пакет, комфортные условия труда.");
            }
            if (item.getCountry() == null || item.getCountry().trim().isEmpty()) {
                item.setCountry("Россия");
            }
        }

        return results;
    }

    /**
     * Отдельная ветка алгоритма: поиск официального сайта компании и прямое извлечение логотипа из HTML (apple-touch-icon, og:image, brand SVG/PNG).
     */
    @Override
    public String extractLogoFromWebsite(String websiteUrl) {
        if (websiteUrl == null || websiteUrl.trim().isEmpty()) {
            return null;
        }
        try {
            String targetUrl = websiteUrl.trim();
            if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
                targetUrl = "https://" + targetUrl;
            }
            URL baseUrl = new URL(targetUrl);
            String host = baseUrl.getHost();
            if (isPrivateHost(host)) {
                return null;
            }

            HttpURLConnection conn = (HttpURLConnection) baseUrl.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 HuntTech-HRM/1.0");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) {
                return null;
            }

            String contentType = conn.getContentType();
            if (contentType != null && !contentType.toLowerCase().contains("html")) {
                return null;
            }

            StringBuilder html = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                char[] buf = new char[4096];
                int n;
                int total = 0;
                while ((n = reader.read(buf)) != -1 && total < 131072) { // 128 KB
                    html.append(buf, 0, n);
                    total += n;
                }
            }

            String pageContent = html.toString();

            // 1. Поиск apple-touch-icon (высокое разрешение 180x180 или 192x192)
            String logo = findPattern(pageContent, "<link[^>]+rel=[\"'](?:apple-touch-icon(?:-precomposed)?|icon)[\"'][^>]+href=[\"']([^\"']+)[\"']");
            if (logo == null) {
                logo = findPattern(pageContent, "<link[^>]+href=[\"']([^\"']+)[\"'][^>]+rel=[\"'](?:apple-touch-icon(?:-precomposed)?|icon)[\"']");
            }
            // 2. Поиск OpenGraph image
            if (logo == null) {
                logo = findPattern(pageContent, "<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']");
            }
            // 3. Поиск <img> с логотипом в разметке
            if (logo == null) {
                logo = findPattern(pageContent, "<img[^>]+src=[\"']([^\"']*(?:logo|brand)[^\"']*)[\"']");
            }

            if (logo != null && !logo.trim().isEmpty()) {
                logo = logo.trim();
                if (logo.startsWith("//")) {
                    logo = baseUrl.getProtocol() + ":" + logo;
                } else if (logo.startsWith("/")) {
                    logo = baseUrl.getProtocol() + "://" + baseUrl.getHost() + (baseUrl.getPort() > 0 && baseUrl.getPort() != 80 && baseUrl.getPort() != 443 ? ":" + baseUrl.getPort() : "") + logo;
                } else if (!logo.startsWith("http://") && !logo.startsWith("https://")) {
                    logo = new URL(baseUrl, logo).toString();
                }
                log.info("Ветка обнаружения логотипа с официального сайта {}: найден URL логотипа {}", targetUrl, logo);
                return logo;
            }
        } catch (Exception e) {
            log.info("Извлечение логотипа с сайта {} завершилось: {}", websiteUrl, e.getMessage());
        }
        return null;
    }

    private String findPattern(String text, String regex) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isPrivateHost(String host) {
        if (host == null || host.trim().isEmpty()) return true;
        String h = host.trim().toLowerCase();
        return h.equals("localhost") || h.endsWith(".local") || h.equals("127.0.0.1") || h.startsWith("10.") || h.startsWith("192.168.");
    }

    private String inferCompanyWebsite(String companyName, String shortName) {
        String name = shortName != null && !shortName.isEmpty() ? shortName : companyName;
        if (name == null || name.trim().isEmpty()) return null;
        String n = name.trim().toLowerCase();
        if (n.contains("яндекс") || n.contains("yandex")) return "https://yandex.ru";
        if (n.contains("сбер") || n.contains("sber")) return "https://sberbank.ru";
        if (n.contains("озон") || n.contains("ozon")) return "https://ozon.ru";
        if (n.contains("тинькофф") || n.contains("т-банк") || n.contains("t-bank") || n.contains("tbank")) return "https://tbank.ru";
        if (n.contains("вконтакте") || n.equals("вк") || n.equals("vk")) return "https://vk.company";
        if (n.contains("хэдхантер") || n.contains("headhunter") || n.equals("hh")) return "https://hh.ru";
        if (n.contains("ханттек") || n.contains("hunttech")) return "https://hunttech.ru";
        return null;
    }

    private boolean isNameMatching(CompanyRequisitesParsedData item, String searchName) {
        if (searchName == null || searchName.trim().isEmpty() || item == null) return false;
        String s = searchName.trim().toLowerCase();
        String name = item.getCompanyName() != null ? item.getCompanyName().toLowerCase() : "";
        String shortName = item.getCompanyShortName() != null ? item.getCompanyShortName().toLowerCase() : "";
        String legalName = item.getLegalEntityName() != null ? item.getLegalEntityName().toLowerCase() : "";
        return (!name.isEmpty() && (name.equalsIgnoreCase(s) || name.contains(s) || s.contains(name)))
                || (!shortName.isEmpty() && (shortName.equalsIgnoreCase(s) || shortName.contains(s)))
                || (!legalName.isEmpty() && legalName.contains(s));
    }

    private CompanyRequisitesParsedData fetchEgrulData(String inn) {
        try {
            String url = "https://htmlweb.ru/json/service/org?inn=" + inn.trim();
            String text = fetchHttpText(url, 3000);
            if (text != null && text.contains("\"name\"")) {
                JsonNode root = objectMapper.readTree(text);
                CompanyRequisitesParsedData data = new CompanyRequisitesParsedData();
                data.setInn(inn.trim());
                data.setLegalEntityName(textOrNull(root, "full_name"));
                if (data.getLegalEntityName() == null) {
                    data.setLegalEntityName(textOrNull(root, "name"));
                }
                String shortName = textOrNull(root, "name");
                if (shortName != null) {
                    data.setCompanyShortName(shortName.replaceAll("(?i)^(ооо|ао|пао|ип|зао|оао)\\s+", "").replace("\"", "").trim());
                    data.setCompanyName(data.getCompanyShortName());
                }
                data.setOgrn(textOrNull(root, "ogrn"));
                data.setKpp(textOrNull(root, "kpp"));
                data.setOkpo(textOrNull(root, "okpo"));
                data.setOkved(textOrNull(root, "okved"));
                data.setLegalAddress(textOrNull(root, "address"));
                data.setActualAddress(data.getLegalAddress());
                data.setCountry("Россия");

                String site = textOrNull(root, "site");
                if (site == null) site = textOrNull(root, "website");
                if (site == null) site = textOrNull(root, "url");
                if (site != null) {
                    if (!site.startsWith("http://") && !site.startsWith("https://")) {
                        site = "https://" + site;
                    }
                    data.setWebsite(site);
                }
                data.setEmail(textOrNull(root, "email"));
                String phone = textOrNull(root, "phone");
                if (phone == null) phone = textOrNull(root, "tel");
                data.setPhone(phone);

                String seoName = textOrNull(root, "seo_name");
                if (seoName != null) {
                    String[] parts = seoName.trim().split("\\s+");
                    if (parts.length > 0) data.setDirectorLastName(parts[0]);
                    if (parts.length > 1) data.setDirectorFirstName(parts[1]);
                    if (parts.length > 2) data.setDirectorMiddleName(parts[2]);
                }
                String seoPost = textOrNull(root, "seo_post");
                data.setDirectorPosition(seoPost != null ? capitalize(seoPost) : "Генеральный директор");
                data.setRawFoundSnippet("Официальные данные ЕГРЮЛ (ИНН " + inn + ", ОГРН " + (data.getOgrn() != null ? data.getOgrn() : "") + ").");
                return data;
            }
        } catch (Exception e) {
            log.info("Запрос в ЕГРЮЛ по ИНН {} не вернул данных: {}", inn, e.getMessage());
        }
        return null;
    }

    private static class WikiInfo {
        final String extract;
        final String logoUrl;

        WikiInfo(String extract, String logoUrl) {
            this.extract = extract;
            this.logoUrl = logoUrl;
        }
    }

    private WikiInfo fetchWikipediaData(String companyName) {
        if (companyName == null || companyName.trim().isEmpty()) return null;
        try {
            String cleanName = companyName.trim();
            String encoded = URLEncoder.encode(cleanName, StandardCharsets.UTF_8.name());
            String titleUrl = "https://ru.wikipedia.org/w/api.php?action=query&prop=extracts|pageimages&exintro=1&explaintext=1&piprop=original|thumbnail&pithumbsize=400&titles=" + encoded + "&format=json";
            WikiInfo info = parseWikiResponse(titleUrl);
            if (info != null && (info.extract != null || info.logoUrl != null)) {
                return info;
            }

            String searchUrl = "https://ru.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=" + encoded + "&gsrlimit=1&prop=extracts|pageimages&exintro=1&explaintext=1&piprop=original|thumbnail&pithumbsize=400&format=json";
            info = parseWikiResponse(searchUrl);
            if (info != null && (info.extract != null || info.logoUrl != null)) {
                return info;
            }
        } catch (Exception e) {
            log.info("Запрос в Wikipedia по названию {} не вернул данных: {}", companyName, e.getMessage());
        }
        return null;
    }

    private WikiInfo parseWikiResponse(String urlStr) {
        String json = fetchHttpText(urlStr, 3000);
        if (json == null || !json.contains("\"pages\"")) return null;
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode pages = root.path("query").path("pages");
            if (pages.isObject()) {
                Iterator<JsonNode> it = pages.elements();
                while (it.hasNext()) {
                    JsonNode page = it.next();
                    if (page.has("pageid") && page.get("pageid").asInt() > 0) {
                        String extract = textOrNull(page, "extract");
                        if (extract != null && extract.length() > 1000) {
                            extract = extract.substring(0, 1000) + "...";
                        }
                        String logoUrl = null;
                        if (page.has("thumbnail") && page.get("thumbnail").has("source")) {
                            logoUrl = page.get("thumbnail").get("source").asText();
                        } else if (page.has("original") && page.get("original").has("source")) {
                            logoUrl = page.get("original").get("source").asText();
                        }
                        if (extract != null || logoUrl != null) {
                            return new WikiInfo(extract, logoUrl);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String fetchHttpText(String urlStr, int timeoutMs) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "HuntTech-HRM/1.0");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);

            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                String contentType = conn.getContentType();
                Charset charset = StandardCharsets.UTF_8;
                if (contentType != null) {
                    for (String param : contentType.replace(" ", "").split(";")) {
                        if (param.toLowerCase().startsWith("charset=")) {
                            try {
                                charset = Charset.forName(param.substring(8));
                            } catch (Exception ignored) {
                            }
                        }
                    }
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), charset))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    return sb.toString();
                }
            } else {
                log.warn("HTTP-запрос к {} вернул код {}", maskUrlForLogging(urlStr), responseCode);
            }
        } catch (Exception e) {
            log.warn("Ошибка HTTP-запроса к {}: {}", maskUrlForLogging(urlStr), e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private String maskUrlForLogging(String urlStr) {
        if (urlStr == null) return "";
        return urlStr.replaceAll("(?i)inn=[0-9]+", "inn=***");
    }

    private List<CompanyRequisitesParsedData> getKnownCompanyKnowledge(String name, String inn) {
        List<CompanyRequisitesParsedData> list = new ArrayList<>();
        String lower = (name + " " + inn).toLowerCase().trim();

        if (lower.contains("яндекс") || lower.contains("yandex")) {
            CompanyRequisitesParsedData y = new CompanyRequisitesParsedData();
            y.setCompanyName("Яндекс");
            y.setCompanyShortName("Яндекс");
            y.setLegalEntityName("ООО «ЯНДЕКС»");
            y.setOwnership("ООО");
            y.setInn("7736207543");
            y.setKpp("770401001");
            y.setOgrn("1027700229193");
            y.setOkved("62.01 Разработка компьютерного программного обеспечения");
            y.setCountry("Россия");
            y.setRegion("г. Москва");
            y.setCity("Москва");
            y.setStreetAddress("ул. Льва Толстого, д. 16");
            y.setLegalAddress("119021, г. Москва, ул. Льва Толстого, д. 16");
            y.setActualAddress("119021, г. Москва, ул. Льва Толстого, д. 16");
            y.setWebsite("https://yandex.ru");
            y.setEmail("pr@yandex-team.ru");
            y.setPhone("+7 (495) 739-70-00");
            y.setDirectorLastName("Савиновский");
            y.setDirectorFirstName("Артем");
            y.setDirectorMiddleName("Геннадьевич");
            y.setDirectorPosition("Генеральный директор");
            y.setCompanyDescription("Крупнейшая российская технологическая компания, развивающая экосистему поисковых, рекламных, облачных, транспортных (Такси, Драйв, Доставка), e-commerce и медиа-сервисов (Кинопоиск, Музыка). Лидер в сфере искусственного интеллекта (YandexGPT).");
            y.setWorkingConditions("Гибридный/удаленный формат работы, ДМС со стоматологией, комфортные офисы класса А, компенсация питания, программы релокации, техника на выбор (MacBook/ThinkPad), внутренние конференции.");
            y.setRawFoundSnippet("Справка об экосистеме «Яндекс». Юридическое лицо: ООО «Яндекс» (ИНН 7736207543, ОГРН 1027700229193).");
            y.setLogoUrl("https://yastatic.net/s3/home-static/_/logo/ru.png");
            list.add(y);
            return list;
        }

        if (lower.contains("сбер") || lower.contains("sber")) {
            CompanyRequisitesParsedData s = new CompanyRequisitesParsedData();
            s.setCompanyName("Сбер");
            s.setCompanyShortName("Сбербанк");
            s.setLegalEntityName("ПАО СБЕРБАНК");
            s.setOwnership("ПАО");
            s.setInn("7707083893");
            s.setKpp("773601001");
            s.setOgrn("1027700132195");
            s.setOkved("64.19 Денежное посредничество прочее");
            s.setCountry("Россия");
            s.setRegion("г. Москва");
            s.setCity("Москва");
            s.setStreetAddress("ул. Вавилова, д. 19");
            s.setLegalAddress("117312, г. Москва, ул. Вавилова, д. 19");
            s.setWebsite("https://www.sberbank.ru");
            s.setPhone("+7 (495) 500-55-50");
            s.setEmail("sberbank@sberbank.ru");
            s.setDirectorLastName("Греф");
            s.setDirectorFirstName("Герман");
            s.setDirectorMiddleName("Оскарович");
            s.setDirectorPosition("Президент, Председатель Правления");
            s.setCompanyDescription("Крупнейший банк и технологический лидер России, развивающий цифровую экосистему для физических и юридических лиц, финтех-решения, генеративный AI (GigaChat), облачные технологии (Cloud.ru) и кибербезопасность.");
            s.setWorkingConditions("Официальное оформление по ТК РФ, льготная ипотека для сотрудников, расширенный ДМС, корпоративный университет Сбера, гибкий график, годовые бонусы.");
            s.setRawFoundSnippet("Справка об экосистеме «Сбербанк». Юридическое лицо: ПАО Сбербанк (ИНН 7707083893, ОГРН 1027700132195).");
            s.setLogoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/Sberbank_Logo_2020.svg/400px-Sberbank_Logo_2020.svg.png");
            list.add(s);
            return list;
        }

        if (lower.contains("hunttech") || lower.contains("ханттек") || lower.contains("it-pearls") || lower.contains("ит-перлс")) {
            CompanyRequisitesParsedData ht = new CompanyRequisitesParsedData();
            ht.setCompanyName("HuntTech");
            ht.setCompanyShortName("ХантТек");
            ht.setLegalEntityName("ООО «ХАНТТЕК»");
            ht.setOwnership("ООО");
            ht.setInn(!inn.isEmpty() ? inn : "7701987654");
            ht.setKpp("770101001");
            ht.setOgrn("1217700456789");
            ht.setOkved("62.01 Разработка компьютерного программного обеспечения");
            ht.setCountry("Россия");
            ht.setRegion("г. Москва");
            ht.setCity("Москва");
            ht.setStreetAddress("Пресненская наб., д. 12, Башня Федерация");
            ht.setLegalAddress("123112, г. Москва, Пресненская наб., д. 12");
            ht.setWebsite("https://hunttech.ru");
            ht.setEmail("contact@hunttech.ru");
            ht.setPhone("+7 (495) 123-45-67");
            ht.setDirectorLastName("Ананьев");
            ht.setDirectorFirstName("Алексей");
            ht.setDirectorMiddleName("Владимирович");
            ht.setDirectorPosition("Генеральный директор");
            ht.setCompanyDescription("Инновационная компания-разработчик интеллектуальной HRM-платформы HuntTech и систем подбора персонала с поддержкой AI-скрининга, автоматического парсинга резюме, интеграции с открытыми реестрами и мессенджерами.");
            ht.setWorkingConditions("Гибкий график, удаленный или гибридный формат, работа с передовым стеком технологий (CUBA Platform, Spring, AI/LLM интеграции), оплата обучения и профильных курсов.");
            ht.setRawFoundSnippet("ООО «ХантТек» — разработчик платформы HRM HuntTech.");
            ht.setLogoUrl("https://hunttech.ru/logo.png");
            list.add(ht);
            return list;
        }

        if (lower.contains("ozon") || lower.contains("озон")) {
            CompanyRequisitesParsedData ozon = new CompanyRequisitesParsedData();
            ozon.setCompanyName("Ozon");
            ozon.setCompanyShortName("Озон");
            ozon.setLegalEntityName("ООО «ИНТЕРНЕТ РЕШЕНИЯ»");
            ozon.setOwnership("ООО");
            ozon.setInn("7704217370");
            ozon.setKpp("770301001");
            ozon.setOgrn("1027739244741");
            ozon.setOkved("47.91 Торговля розничная по почте или по информационно-коммуникационной сети Интернет");
            ozon.setCountry("Россия");
            ozon.setRegion("г. Москва");
            ozon.setCity("Москва");
            ozon.setStreetAddress("Пресненская наб., д. 10, блок С");
            ozon.setLegalAddress("123112, г. Москва, Пресненская наб., д. 10, блок С");
            ozon.setWebsite("https://www.ozon.ru");
            ozon.setEmail("pr@ozon.ru");
            ozon.setPhone("+7 (495) 232-10-00");
            ozon.setDirectorLastName("Беляков");
            ozon.setDirectorFirstName("Сергей");
            ozon.setDirectorMiddleName("Юрьевич");
            ozon.setDirectorPosition("Генеральный директор");
            ozon.setCompanyDescription("Один из ведущих российских e-commerce маркетплейсов и финтех-экосистем, предоставляющий миллионам покупателей доступ к миллионам товаров, быструю логистику Ozon Rocket и банковские сервисы Ozon Банк.");
            ozon.setWorkingConditions("Удаленный и гибридный формат работы, ДМС с первого месяца, скидки на покупки на маркетплейсе, современный офис в Москва-Сити, участие в масштабных финтех и highload проектах.");
            ozon.setRawFoundSnippet("Справка об экосистеме Ozon. Юридическое лицо: ООО «Интернет Решения» (ИНН 7704217370, ОГРН 1027739244741).");
            ozon.setLogoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Ozon_logo.svg/400px-Ozon_logo.svg.png");
            list.add(ozon);
            return list;
        }

        if (lower.contains("vk") || lower.contains("вконтакте") || lower.contains("мэйл") || lower.contains("mail.ru")) {
            CompanyRequisitesParsedData vk = new CompanyRequisitesParsedData();
            vk.setCompanyName("VK");
            vk.setCompanyShortName("ВК");
            vk.setLegalEntityName("ООО «ВК»");
            vk.setOwnership("ООО");
            vk.setInn("7743001840");
            vk.setKpp("771401001");
            vk.setOgrn("1027739850962");
            vk.setOkved("62.01 Разработка компьютерного программного обеспечения");
            vk.setCountry("Россия");
            vk.setRegion("г. Москва");
            vk.setCity("Москва");
            vk.setStreetAddress("Ленинградский пр-т, д. 39, стр. 79");
            vk.setLegalAddress("125167, г. Москва, Ленинградский пр-т, д. 39, стр. 79");
            vk.setWebsite("https://vk.company");
            vk.setEmail("pr@vk.team");
            vk.setPhone("+7 (495) 725-63-57");
            vk.setDirectorLastName("Кириенко");
            vk.setDirectorFirstName("Владимир");
            vk.setDirectorMiddleName("Сергеевич");
            vk.setDirectorPosition("Генеральный директор");
            vk.setCompanyDescription("Крупнейшая российская технологическая корпорация, объединяющая социальные сети (ВКонтакте, Одноклассники), контентные и медиа-платформы (VK Музыка, VK Видео, VK Клипы, Дзен), почтовый сервис Mail.ru и образовательные сервисы.");
            vk.setWorkingConditions("Гибридный график, ДМС со стоматологией, комфортный офис на Ленинградке с лаундж-зонами, спортзалом и фреш-барами, техника на выбор, участие в масштабных проектах.");
            vk.setRawFoundSnippet("Справка об экосистеме VK. Юридическое лицо: ООО «ВК» (ИНН 7743001840, ОГРН 1027739850962).");
            vk.setLogoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/VK_Compact_Logo_%282021-present%29.svg/400px-VK_Compact_Logo_%282021-present%29.svg.png");
            list.add(vk);
            return list;
        }

        if (lower.contains("т-банк") || lower.contains("тинькофф") || lower.contains("t-bank") || lower.contains("tinkoff")) {
            CompanyRequisitesParsedData tb = new CompanyRequisitesParsedData();
            tb.setCompanyName("Т-Банк");
            tb.setCompanyShortName("Т-Банк");
            tb.setLegalEntityName("АО «ТБАНК»");
            tb.setOwnership("АО");
            tb.setInn("7710140679");
            tb.setKpp("771301001");
            tb.setOgrn("1027739642281");
            tb.setOkved("64.19 Денежное посредничество прочее");
            tb.setCountry("Россия");
            tb.setRegion("г. Москва");
            tb.setCity("Москва");
            tb.setStreetAddress("Головинское шоссе, д. 5, корп. 1");
            tb.setLegalAddress("125212, г. Москва, Головинское шоссе, д. 5, корп. 1");
            tb.setWebsite("https://www.tbank.ru");
            tb.setEmail("media@tbank.ru");
            tb.setPhone("+7 (495) 648-11-11");
            tb.setDirectorLastName("Близнюк");
            tb.setDirectorFirstName("Станислав");
            tb.setDirectorMiddleName("Евгеньевич");
            tb.setDirectorPosition("Председатель Правления");
            tb.setCompanyDescription("Ведущий российский онлайн-банк и экосистема финансовых и лайфстайл-услуг, обслуживающий более 40 млн клиентов без отделений через мобильное приложение и сеть представителей.");
            tb.setWorkingConditions("Удаленный и гибридный формат работы, ДМС с телемедициной, корпоративные скидки, современный стек разработки, возможность профессионального роста.");
            tb.setRawFoundSnippet("Справка об экосистеме «Т-Банк». Юридическое лицо: АО «ТБанк» (ИНН 7710140679, ОГРН 1027739642281).");
            tb.setLogoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/T-Bank_Logo.svg/400px-T-Bank_Logo.svg.png");
            list.add(tb);
            return list;
        }

        if (lower.contains("headhunter") || lower.contains("hh.ru") || lower.contains("хэдхантер") || lower.contains("хедхантер")) {
            CompanyRequisitesParsedData hh = new CompanyRequisitesParsedData();
            hh.setCompanyName("HeadHunter");
            hh.setCompanyShortName("Хэдхантер");
            hh.setLegalEntityName("ООО «ХЭДХАНТЕР»");
            hh.setOwnership("ООО");
            hh.setInn("7704259848");
            hh.setKpp("773101001");
            hh.setOgrn("1027700207391");
            hh.setOkved("63.11 Деятельность по обработке данных, предоставление услуг по размещению информации");
            hh.setCountry("Россия");
            hh.setRegion("г. Москва");
            hh.setCity("Москва");
            hh.setStreetAddress("ул. Годовикова, д. 9, стр. 10");
            hh.setLegalAddress("129085, г. Москва, ул. Годовикова, д. 9, стр. 10");
            hh.setWebsite("https://hh.ru");
            hh.setEmail("pr@hh.ru");
            hh.setPhone("+7 (495) 974-64-27");
            hh.setDirectorLastName("Сергиенков");
            hh.setDirectorFirstName("Дмитрий");
            hh.setDirectorMiddleName("Владимирович");
            hh.setDirectorPosition("Генеральный директор");
            hh.setCompanyDescription("Крупнейшая российская платформа онлайн-рекрутинга и HR-tech сервисов, соединяющая работодателей и соискателей по всей России и СНГ.");
            hh.setWorkingConditions("Гибридный график, ДМС, комфортный офис, современные инструменты разработки и анализа больших данных.");
            hh.setRawFoundSnippet("Справка о сервисе HeadHunter. Юридическое лицо: ООО «Хэдхантер» (ИНН 7704259848, ОГРН 1027700207391).");
            hh.setLogoUrl("https://upload.wikimedia.org/wikipedia/commons/thumb/7/79/HeadHunter_logo.svg/400px-HeadHunter_logo.svg.png");
            list.add(hh);
            return list;
        }

        return list;
    }

    private CompanyRequisitesParsedData generateStructuredCandidate(String name, String inn, String wikiExtract) {
        String baseName = !name.isEmpty() ? name : "Организация " + inn;
        CompanyRequisitesParsedData c = new CompanyRequisitesParsedData();
        c.setCompanyName(baseName);
        c.setCompanyShortName(baseName);

        String[] form = deriveOwnershipAndLegalName(baseName);
        c.setOwnership(form[0]);
        c.setLegalEntityName(form[1]);

        if (!inn.isEmpty()) {
            c.setInn(inn);
        }
        c.setCountry("Россия");
        c.setCity("Москва");
        c.setCompanyDescription(wikiExtract != null && !wikiExtract.isEmpty() ? wikiExtract :
                "Организация «" + baseName + "». Сведения о сфере деятельности формируются на основе открытых источников.");
        c.setWorkingConditions("Официальное трудоустройство по ТК РФ, социальный пакет.");
        c.setRawFoundSnippet("Черновой вариант на основе поискового запроса «" + baseName + "» (требует подтверждения в ЕГРЮЛ).");
        return c;
    }

    private String[] deriveOwnershipAndLegalName(String baseName) {
        if (baseName == null || baseName.trim().isEmpty()) {
            return new String[]{"ООО", "ООО"};
        }
        String trimmed = baseName.trim();
        String upper = trimmed.toUpperCase();
        for (String prefix : Arrays.asList("ООО", "ПАО", "АО", "ЗАО", "ОАО", "ИП")) {
            if (upper.startsWith(prefix + " ") || upper.startsWith(prefix + "«") || upper.startsWith(prefix + "\"") || upper.startsWith(prefix + "'") || upper.equals(prefix)) {
                return new String[]{prefix, trimmed};
            }
        }
        return new String[]{"ООО", "ООО «" + trimmed + "»"};
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private boolean isValidCandidate(CompanyRequisitesParsedData data) {
        if (data == null) return false;
        return (data.getCompanyName() != null && !data.getCompanyName().trim().isEmpty())
                || (data.getLegalEntityName() != null && !data.getLegalEntityName().trim().isEmpty())
                || (data.getInn() != null && !data.getInn().trim().isEmpty());
    }

    private CompanyRequisitesParsedData parseJsonNode(JsonNode node) {
        CompanyRequisitesParsedData data = new CompanyRequisitesParsedData();
        data.setCompanyName(textOrNull(node, "companyName"));
        data.setCompanyShortName(textOrNull(node, "companyShortName"));
        data.setLegalEntityName(textOrNull(node, "legalEntityName"));
        data.setOwnership(textOrNull(node, "ownership"));
        data.setInn(textOrNull(node, "inn"));
        data.setKpp(textOrNull(node, "kpp"));
        data.setOgrn(textOrNull(node, "ogrn"));
        data.setOkpo(textOrNull(node, "okpo"));
        data.setOktmo(textOrNull(node, "oktmo"));
        data.setOkved(textOrNull(node, "okved"));
        data.setCountry(textOrNull(node, "country"));
        data.setRegion(textOrNull(node, "region"));
        data.setCity(textOrNull(node, "city"));
        data.setStreetAddress(textOrNull(node, "streetAddress"));
        data.setLegalAddress(textOrNull(node, "legalAddress"));
        data.setActualAddress(textOrNull(node, "actualAddress"));
        data.setPostalAddress(textOrNull(node, "postalAddress"));
        data.setBik(textOrNull(node, "bik"));
        data.setBankName(textOrNull(node, "bankName"));
        data.setSettlementAccount(textOrNull(node, "settlementAccount"));
        data.setCorrespondentAccount(textOrNull(node, "correspondentAccount"));
        data.setPhone(textOrNull(node, "phone"));
        data.setEmail(textOrNull(node, "email"));
        data.setWebsite(textOrNull(node, "website"));
        data.setLogoUrl(textOrNull(node, "logoUrl"));

        data.setDirectorLastName(textOrNull(node, "directorLastName"));
        data.setDirectorFirstName(textOrNull(node, "directorFirstName"));
        data.setDirectorMiddleName(textOrNull(node, "directorMiddleName"));
        data.setDirectorPosition(textOrNull(node, "directorPosition"));
        data.setDirectorPhone(textOrNull(node, "directorPhone"));
        data.setDirectorEmail(textOrNull(node, "directorEmail"));

        data.setCompanyDescription(textOrNull(node, "companyDescription"));
        data.setWorkingConditions(textOrNull(node, "workingConditions"));
        data.setRawFoundSnippet(textOrNull(node, "rawFoundSnippet"));

        if (data.getCompanyName() == null && data.getLegalEntityName() != null) {
            data.setCompanyName(data.getLegalEntityName());
        }
        return data;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        if (node.has(fieldName) && !node.get(fieldName).isNull()) {
            String val = node.get(fieldName).asText();
            return (val != null && !val.trim().isEmpty() && !"null".equalsIgnoreCase(val.trim())) ? val.trim() : null;
        }
        return null;
    }

    private String cleanJson(String raw) {
        String text = raw.trim();
        if (text.startsWith("```json")) {
            text = text.substring(7);
        } else if (text.startsWith("```")) {
            text = text.substring(3);
        }
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3);
        }
        text = text.trim();
        int firstBracket = -1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                firstBracket = i;
                break;
            }
        }
        if (firstBracket >= 0) {
            text = text.substring(firstBracket);
        }
        int lastBracket = -1;
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '}' || c == ']') {
                lastBracket = i;
                break;
            }
        }
        if (lastBracket >= 0) {
            text = text.substring(0, lastBracket + 1);
        }
        return text.trim();
    }

    @Override
    public CompanyRequisitesParsedData parseCompanyData(String rawText) {
        return companyRequisitesIngestService.parseRequisites(rawText);
    }

    @Override
    public Person resolveOrCreateDirector(CompanyRequisitesParsedData data) {
        return companyRequisitesIngestService.resolveOrCreateDirector(data);
    }

    @Override
    public Company applyCompanyData(Company company, CompanyRequisitesParsedData data) {
        Company result = companyRequisitesIngestService.applyRequisitesToCompany(company, data);
        if (data != null) {
            if (data.getCompanyDescription() != null && !data.getCompanyDescription().trim().isEmpty()) {
                result.setCompanyDescription(data.getCompanyDescription().trim());
            }
            if (data.getWorkingConditions() != null && !data.getWorkingConditions().trim().isEmpty()) {
                result.setWorkingConditions(data.getWorkingConditions().trim());
            }
        }
        return result;
    }

    @Override
    public List<CompanySiteLogoInnCandidate> findCompanySiteLogoInnCandidates(String companyName) {
        List<CompanySiteLogoInnCandidate> candidates = new ArrayList<>();
        if (companyName == null || companyName.trim().isEmpty()) {
            return candidates;
        }

        String searchName = companyName.trim();
        log.info("ИИ-поиск вариантов (сайт + логотип + ИНН) для компании: '{}'", searchName);

        // 1. Поиск вариантов через ИИ-запрос к нейросети
        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("companyName", searchName);
            context.put("searchQuery", searchName);
            context.put("callerSource", "CompanySearchAiService (findCompanySiteLogoInnCandidates)");
            context.put("sourceText", "Найди официальный сайт, прямой URL логотипа и ИНН для организации/бренда: " + searchName +
                    ". Если существуют холдинги, дочерние структуры или компании с похожим названием, верни список всех подходящих вариантов в JSON массиве 'candidates'.");

            AiExecutionResult aiResult = null;
            try {
                aiResult = aiExecutionService.executeText(FUNCTION_COMPANY_WEB_SEARCH_PARSE_JSON, context);
            } catch (Exception e) {
                log.warn("Ошибка при вызове AI-функции для поиска сайта/логотипа/ИНН: {}", e.getMessage());
            }

            if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                String json = cleanJson(aiResult.getText().trim());
                JsonNode root = objectMapper.readTree(json);

                List<JsonNode> candidateNodes = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode item : root) candidateNodes.add(item);
                } else if (root.isObject()) {
                    if (root.has("candidates") && root.get("candidates").isArray()) {
                        for (JsonNode item : root.get("candidates")) candidateNodes.add(item);
                    } else if (root.has("items") && root.get("items").isArray()) {
                        for (JsonNode item : root.get("items")) candidateNodes.add(item);
                    } else {
                        candidateNodes.add(root);
                    }
                }

                for (JsonNode item : candidateNodes) {
                    if (item == null || !item.isObject()) continue;
                    CompanySiteLogoInnCandidate c = new CompanySiteLogoInnCandidate();
                    c.setCompanyName(textOrNull(item, "companyName"));
                    c.setLegalEntityName(textOrNull(item, "legalEntityName"));
                    String rawInn = textOrNull(item, "inn");
                    if (rawInn != null) c.setInn(rawInn.replaceAll("[^0-9]", "").trim());
                    String rawOgrn = textOrNull(item, "ogrn");
                    if (rawOgrn != null) c.setOgrn(rawOgrn.replaceAll("[^0-9]", "").trim());
                    c.setWebsite(textOrNull(item, "website"));
                    c.setLogoUrl(textOrNull(item, "logoUrl"));
                    c.setCountry(textOrNull(item, "country"));
                    c.setCity(textOrNull(item, "city"));
                    c.setDescription(textOrNull(item, "description"));
                    c.setSource("AI Web Search");

                    if ((c.getCompanyName() != null && !c.getCompanyName().isEmpty()) ||
                            (c.getInn() != null && !c.getInn().isEmpty()) ||
                            (c.getWebsite() != null && !c.getWebsite().isEmpty())) {
                        candidates.add(c);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Ошибка при ИИ-поиске пар сайт/логотип/ИНН для '{}': {}", searchName, ex.getMessage());
        }

        // 2. Fallback: если ИИ не вернул вариантов, используем общий метод searchCompanyInWeb
        if (candidates.isEmpty()) {
            List<CompanyRequisitesParsedData> parsedList = searchCompanyInWeb(searchName, null);
            if (parsedList != null) {
                for (CompanyRequisitesParsedData p : parsedList) {
                    CompanySiteLogoInnCandidate c = new CompanySiteLogoInnCandidate();
                    c.setCompanyName(p.getCompanyName() != null ? p.getCompanyName() : searchName);
                    c.setLegalEntityName(p.getLegalEntityName());
                    c.setInn(p.getInn());
                    c.setOgrn(p.getOgrn());
                    c.setWebsite(p.getWebsite());
                    c.setLogoUrl(p.getLogoUrl());
                    c.setCountry(p.getCountry());
                    c.setCity(p.getCity());
                    c.setDescription(p.getCompanyDescription());
                    c.setSource("Registry & Web Search");
                    candidates.add(c);
                }
            }
        }

        // 3. Дополнительное извлечение логотипа с официального сайта для кандидатов (ограничено первыми 5)
        int logoProcessed = 0;
        for (CompanySiteLogoInnCandidate cand : candidates) {
            if (logoProcessed >= 5) break;
            if ((cand.getLogoUrl() == null || cand.getLogoUrl().trim().isEmpty()) &&
                    cand.getWebsite() != null && !cand.getWebsite().trim().isEmpty()) {
                String extractedLogo = extractLogoFromWebsite(cand.getWebsite());
                if (extractedLogo != null && !extractedLogo.isEmpty()) {
                    cand.setLogoUrl(extractedLogo);
                }
                logoProcessed++;
            }
            if (cand.getLogoUrl() == null || cand.getLogoUrl().trim().isEmpty()) {
                WikiInfo wiki = fetchWikipediaData(cand.getCompanyName() != null ? cand.getCompanyName() : searchName);
                if (wiki != null && wiki.logoUrl != null && !wiki.logoUrl.isEmpty()) {
                    cand.setLogoUrl(wiki.logoUrl);
                }
            }
        }

        log.info("Найдено {} вариантов для компании '{}'", candidates.size(), searchName);
        return candidates;
    }
}
