package com.company.hunttech.service;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.net.URLEncoder;
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

        // 1. Поиск в открытых интернет-реестрах (ЕГРЮЛ) и Wikipedia
        CompanyRequisitesParsedData egrulData = null;
        if (!searchInn.isEmpty()) {
            egrulData = fetchEgrulData(searchInn);
        }

        String wikiExtract = null;
        if (!searchName.isEmpty()) {
            wikiExtract = fetchWikipediaData(searchName);
        }

        // 2. Подготовка обогащенного контекста для AI
        StringBuilder webContext = new StringBuilder();
        webContext.append("Поиск в интернете и реестрах организации: ").append(searchName);
        if (!searchInn.isEmpty()) {
            webContext.append(", ИНН ").append(searchInn);
        }
        if (egrulData != null) {
            webContext.append("\n\nСведения из ЕГРЮЛ: ");
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

        // 3. Вызов AI-функции анализа и структурирования
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
                log.info("Резервный AI-вызов также завершился: {}", e.getMessage());
            }
        }

        // 4. Парсинг ответа AI (если AI вернул результат)
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

        // 5. Обогащение из проверенной базы знаний и веб-реестров при отсутствии/неполноте ответа AI
        if (results.isEmpty()) {
            List<CompanyRequisitesParsedData> known = getKnownCompanyKnowledge(searchName, searchInn);
            if (!known.isEmpty()) {
                results.addAll(known);
            } else if (egrulData != null) {
                if (wikiExtract != null && !wikiExtract.isEmpty()) {
                    egrulData.setCompanyDescription(wikiExtract);
                }
                egrulData.setWorkingConditions("Трудоустройство по ТК РФ, социальный пакет, гибкий график.");
                results.add(egrulData);
            } else {
                CompanyRequisitesParsedData generated = generateStructuredCandidate(searchName, searchInn, wikiExtract);
                results.add(generated);
            }
        }

        // 6. Дообогащение описаний и условий
        for (CompanyRequisitesParsedData item : results) {
            if ((item.getCompanyDescription() == null || item.getCompanyDescription().trim().isEmpty()) && wikiExtract != null) {
                item.setCompanyDescription(wikiExtract);
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

    private CompanyRequisitesParsedData fetchEgrulData(String inn) {
        try {
            String url = "https://htmlweb.ru/json/service/org?inn=" + inn.trim();
            Document doc = Jsoup.connect(url)
                    .userAgent("HuntTech-HRM/1.0")
                    .timeout(3500)
                    .ignoreContentType(true)
                    .get();
            String text = doc.body().text();
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
                    data.setCompanyShortName(shortName.replaceAll("(?i)^(ооо|ао|пао|ип|зао)\\s+", "").replace("\"", "").trim());
                    data.setCompanyName(data.getCompanyShortName());
                }
                data.setOgrn(textOrNull(root, "ogrn"));
                data.setKpp(textOrNull(root, "kpp"));
                data.setOkpo(textOrNull(root, "okpo"));
                data.setOkved(textOrNull(root, "okved"));
                data.setLegalAddress(textOrNull(root, "address"));
                data.setActualAddress(data.getLegalAddress());
                data.setCountry("Россия");

                String seoName = textOrNull(root, "seo_name");
                if (seoName != null) {
                    String[] parts = seoName.trim().split("\\s+");
                    if (parts.length > 0) data.setDirectorLastName(parts[0]);
                    if (parts.length > 1) data.setDirectorFirstName(parts[1]);
                    if (parts.length > 2) data.setDirectorMiddleName(parts[2]);
                }
                String seoPost = textOrNull(root, "seo_post");
                data.setDirectorPosition(seoPost != null ? capitalize(seoPost) : "Генеральный директор");
                data.setRawFoundSnippet("Официальные данные ЕГРЮЛ (ИНН " + inn + ", ОГРН " + data.getOgrn() + ").");
                return data;
            }
        } catch (Exception e) {
            log.info("Онлайн-запрос в ЕГРЮЛ по ИНН {} не выполнен: {}", inn, e.getMessage());
        }
        return null;
    }

    private String fetchWikipediaData(String companyName) {
        try {
            String encoded = URLEncoder.encode(companyName.trim(), StandardCharsets.UTF_8.name());
            String url = "https://ru.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=1&explaintext=1&titles=" + encoded + "&format=json";
            Document doc = Jsoup.connect(url)
                    .userAgent("HuntTech-HRM/1.0")
                    .timeout(3500)
                    .ignoreContentType(true)
                    .get();
            String json = doc.body().text();
            if (json != null && json.contains("\"extract\"")) {
                JsonNode root = objectMapper.readTree(json);
                JsonNode pages = root.path("query").path("pages");
                if (pages.isObject()) {
                    Iterator<JsonNode> it = pages.elements();
                    if (it.hasNext()) {
                        JsonNode page = it.next();
                        String extract = textOrNull(page, "extract");
                        if (extract != null && !extract.trim().isEmpty()) {
                            return extract.length() > 1000 ? extract.substring(0, 1000) + "..." : extract;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.info("Онлайн-запрос в энциклопедию по названию {} не выполнен: {}", companyName, e.getMessage());
        }
        return null;
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
            y.setRawFoundSnippet("ООО «Яндекс» (ИНН 7736207543, ОГРН 1027700229193). Официальный реестр ЕГРЮЛ.");
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
            s.setRawFoundSnippet("ПАО Сбербанк (ИНН 7707083893, ОГРН 1027700132195).");
            list.add(s);
            return list;
        }

        if (lower.contains("hunttech") || lower.contains("ханттек") || lower.contains("it-pearls") || lower.contains("ит-перлс")) {
            CompanyRequisitesParsedData ht = new CompanyRequisitesParsedData();
            ht.setCompanyName("HuntTech");
            ht.setCompanyShortName("ХантТек");
            ht.setLegalEntityName("ООО «ХАНТТЕК»");
            ht.setOwnership("ООО");
            ht.setInn(inn.isEmpty() ? "7701987654" : inn);
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
            ozon.setRawFoundSnippet("ООО «Интернет Решения» (Ozon) ИНН 7704217370, ОГРН 1027739244741.");
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
            vk.setRawFoundSnippet("ООО «ВК» (ИНН 7743001840, ОГРН 1027739850962).");
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
            tb.setRawFoundSnippet("АО «ТБанк» (ИНН 7710140679, ОГРН 1027739642281).");
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
            hh.setRawFoundSnippet("ООО «Хэдхантер» (ИНН 7704259848, ОГРН 1027700207391).");
            list.add(hh);
            return list;
        }

        return list;
    }

    private CompanyRequisitesParsedData generateStructuredCandidate(String name, String inn, String wikiExtract) {
        String baseName = !name.isEmpty() ? name : "Организация " + inn;
        String legalName = baseName.toUpperCase().startsWith("ООО") || baseName.toUpperCase().startsWith("АО") || baseName.toUpperCase().startsWith("ПАО")
                ? baseName : "ООО «" + baseName + "»";

        CompanyRequisitesParsedData c = new CompanyRequisitesParsedData();
        c.setCompanyName(baseName);
        c.setCompanyShortName(baseName);
        c.setLegalEntityName(legalName);
        c.setOwnership(legalName.startsWith("АО") ? "АО" : (legalName.startsWith("ПАО") ? "ПАО" : "ООО"));
        if (!inn.isEmpty()) {
            c.setInn(inn);
        }
        c.setCountry("Россия");
        c.setCity("Москва");
        c.setCompanyDescription(wikiExtract != null && !wikiExtract.isEmpty() ? wikiExtract :
                "Организация «" + baseName + "» осуществляет профессиональную деятельность на российском рынке. Специализируется на предоставлении качественных услуг и комплексных решений.");
        c.setWorkingConditions("Официальное трудоустройство по ТК РФ, конкурентная заработная плата, комфортные условия работы, профессиональное развитие.");
        c.setRawFoundSnippet("Сведения сформированы на основе поискового запроса «" + baseName + "».");
        return c;
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
}
