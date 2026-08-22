package com.company.hunttech.service;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.swing.text.rtf.RTFEditorKit;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service(CompanyRequisitesIngestService.NAME)
public class CompanyRequisitesIngestServiceBean implements CompanyRequisitesIngestService {
    private static final Logger log = LoggerFactory.getLogger(CompanyRequisitesIngestServiceBean.class);

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private FileLoader fileLoader;
    @Inject
    private AiExecutionService aiExecutionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String extractTextFromFile(FileDescriptor fileDescriptor) {
        if (fileDescriptor == null) return "";
        byte[] fileBytes;
        try (InputStream is = fileLoader.openStream(fileDescriptor)) {
            fileBytes = is.readAllBytes();
        } catch (Exception e) {
            log.error("Не удалось прочитать файл из FileStorage", e);
            return "";
        }

        String ext = fileDescriptor.getExtension() != null ? fileDescriptor.getExtension().toLowerCase() : "";
        try {
            if ("pdf".equals(ext)) {
                try (PDDocument document = Loader.loadPDF(fileBytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            } else if ("docx".equals(ext)) {
                try (InputStream is = new ByteArrayInputStream(fileBytes);
                     XWPFDocument doc = new XWPFDocument(is);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
                    return extractor.getText();
                }
            } else if ("doc".equals(ext)) {
                try (InputStream is = new ByteArrayInputStream(fileBytes);
                     WordExtractor extractor = new WordExtractor(is)) {
                    return extractor.getText();
                }
            } else if ("rtf".equals(ext)) {
                RTFEditorKit rtfKit = new RTFEditorKit();
                javax.swing.text.Document doc = rtfKit.createDefaultDocument();
                try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                    rtfKit.read(is, doc, 0);
                    return doc.getText(0, doc.getLength());
                }
            } else if ("pages".equals(ext)) {
                try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (entry.getName().endsWith("preview.jpg") || entry.getName().endsWith("preview.png")) {
                            continue;
                        }
                        if (entry.getName().contains("Document.iwa") || entry.getName().endsWith(".xml")) {
                            byte[] buffer = zis.readAllBytes();
                            return new String(buffer, StandardCharsets.UTF_8).replaceAll("[\\x00-\\x1F]", " ");
                        }
                    }
                }
            } else if ("txt".equals(ext)) {
                return new String(fileBytes, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Ошибка извлечения текста из файла {}", fileDescriptor.getName(), e);
            return "";
        }
        return "txt".equals(ext) ? new String(fileBytes, StandardCharsets.UTF_8) : "";
    }

    @Override
    public String extractTextFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) return "";
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            log.warn("Попытка загрузки URL с недопустимым протоколом: {}", trimmed);
            return "";
        }

        try {
            java.net.URI uri = new java.net.URI(trimmed);
            String host = uri.getHost();
            if (host == null || host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1") || host.equals("::1") || host.startsWith("169.254.") || host.startsWith("10.") || host.startsWith("192.168.")) {
                log.warn("Заблокирован доступ к локальному или приватному хосту: {}", host);
                return "";
            }

            Document doc = Jsoup.connect(trimmed)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            return doc.body() != null ? doc.body().text() : doc.text();
        } catch (Exception e) {
            log.error("Ошибка загрузки страницы по URL: {}", url, e);
            return "";
        }
    }

    @Override
    public CompanyRequisitesParsedData parseRequisites(String rawText) {
        CompanyRequisitesParsedData result = new CompanyRequisitesParsedData();
        if (rawText == null || rawText.trim().isEmpty()) {
            return result;
        }

        try {
            Map<String, Object> context = new LinkedHashMap<>();
            context.put("sourceText", rawText.trim());
            context.put("callerSource", "CompanyRequisitesIngestService (parseRequisites)");

            AiExecutionResult aiResult = aiExecutionService.executeText(FUNCTION_COMPANY_REQUISITES_PARSE_JSON, context);
            if (aiResult != null && aiResult.getText() != null && !aiResult.getText().trim().isEmpty()) {
                String json = cleanJson(aiResult.getText().trim());
                JsonNode root = objectMapper.readTree(json);

                result.setCompanyName(textOrNull(root, "companyName"));
                result.setCompanyShortName(textOrNull(root, "companyShortName"));
                result.setLegalEntityName(textOrNull(root, "legalEntityName"));
                result.setOwnership(textOrNull(root, "ownership"));
                result.setInn(textOrNull(root, "inn"));
                result.setKpp(textOrNull(root, "kpp"));
                result.setOgrn(textOrNull(root, "ogrn"));
                result.setOkpo(textOrNull(root, "okpo"));
                result.setOktmo(textOrNull(root, "oktmo"));
                result.setOkved(textOrNull(root, "okved"));
                result.setCountry(textOrNull(root, "country"));
                result.setRegion(textOrNull(root, "region"));
                result.setCity(textOrNull(root, "city"));
                result.setStreetAddress(textOrNull(root, "streetAddress"));
                result.setLegalAddress(textOrNull(root, "legalAddress"));
                result.setActualAddress(textOrNull(root, "actualAddress"));
                result.setPostalAddress(textOrNull(root, "postalAddress"));
                result.setBik(textOrNull(root, "bik"));
                result.setBankName(textOrNull(root, "bankName"));
                result.setSettlementAccount(textOrNull(root, "settlementAccount"));
                result.setCorrespondentAccount(textOrNull(root, "correspondentAccount"));
                result.setPhone(textOrNull(root, "phone"));
                result.setEmail(textOrNull(root, "email"));
                result.setWebsite(textOrNull(root, "website"));

                result.setDirectorLastName(textOrNull(root, "directorLastName"));
                result.setDirectorFirstName(textOrNull(root, "directorFirstName"));
                result.setDirectorMiddleName(textOrNull(root, "directorMiddleName"));
                result.setDirectorPosition(textOrNull(root, "directorPosition"));
                result.setDirectorPhone(textOrNull(root, "directorPhone"));
                result.setDirectorEmail(textOrNull(root, "directorEmail"));
                return result;
            }
        } catch (Exception e) {
            log.warn("AI парсинг реквизитов компании завершился с ошибкой, fallback на локальный regex-парсер: {}", e.getMessage());
        }

        // Fallback: локальный regex-парсер
        parseLocally(rawText, result);
        return result;
    }

    private void parseLocally(String text, CompanyRequisitesParsedData result) {
        // ИНН: 10 или 12 цифр
        Matcher innMatcher = Pattern.compile("(?i)ИНН\\s*[:№]?\\s*(\\d{10,12})").matcher(text);
        if (innMatcher.find()) result.setInn(innMatcher.group(1));

        // КПП: 9 цифр
        Matcher kppMatcher = Pattern.compile("(?i)КПП\\s*[:№]?\\s*(\\d{9})").matcher(text);
        if (kppMatcher.find()) result.setKpp(kppMatcher.group(1));

        // ОГРН: 13 или 15 цифр
        Matcher ogrnMatcher = Pattern.compile("(?i)ОГРН(?:ИП)?\\s*[:№]?\\s*(\\d{13,15})").matcher(text);
        if (ogrnMatcher.find()) result.setOgrn(ogrnMatcher.group(1));

        // БИК: 9 цифр
        Matcher bikMatcher = Pattern.compile("(?i)БИК\\s*[:№]?\\s*(\\d{9})").matcher(text);
        if (bikMatcher.find()) result.setBik(bikMatcher.group(1));

        // Расчетный счет: 20 цифр
        Matcher rsMatcher = Pattern.compile("(?i)(?:Р/с|Р[.]сч|расчетный\\s+счет)\\s*[:№]?\\s*(\\d{20})").matcher(text);
        if (rsMatcher.find()) result.setSettlementAccount(rsMatcher.group(1));

        // Корр. счет: 20 цифр
        Matcher ksMatcher = Pattern.compile("(?i)(?:К/с|К[.]сч|корр(?:еспондентский)?[.]?\\s+счет)\\s*[:№]?\\s*(\\d{20})").matcher(text);
        if (ksMatcher.find()) result.setCorrespondentAccount(ksMatcher.group(1));

        // Email
        Matcher emailMatcher = Pattern.compile("(?i)[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(text);
        if (emailMatcher.find()) result.setEmail(emailMatcher.group());

        // Телефон
        Matcher phoneMatcher = Pattern.compile("(?i)(?:\\+7|8)[\\s\\-]?\\(?\\d{3}\\)?[\\s\\-]?\\d{3}[\\s\\-]?\\d{2}[\\s\\-]?\\d{2}").matcher(text);
        if (phoneMatcher.find()) result.setPhone(phoneMatcher.group());

        // Сайт
        Matcher webMatcher = Pattern.compile("(?i)https?://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(?:/[^\\s]*)?|www\\.[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").matcher(text);
        if (webMatcher.find()) result.setWebsite(webMatcher.group());

        // Город (г. Название или город Название)
        Matcher cityMatcher = Pattern.compile("(?i)(?:г[.]|город)\\s+([А-Яа-яЁёA-Za-z\\-]+)").matcher(text);
        if (cityMatcher.find()) {
            result.setCity(cityMatcher.group(1).trim());
        }
    }

    @Override
    public Person resolveOrCreateDirector(CompanyRequisitesParsedData data) {
        if (data == null) return null;
        String lastName = data.getDirectorLastName() != null ? data.getDirectorLastName().trim() : "";
        String firstName = data.getDirectorFirstName() != null ? data.getDirectorFirstName().trim() : "";
        String middleName = data.getDirectorMiddleName() != null ? data.getDirectorMiddleName().trim() : "";

        if (lastName.isEmpty() && firstName.isEmpty()) {
            return null;
        }

        // Поиск в справочнике Person
        try {
            List<Person> candidates;
            if (!lastName.isEmpty() && !firstName.isEmpty()) {
                candidates = dataManager.load(Person.class)
                        .query("select p from hunttech_Person p where lower(p.secondName) = :lastName and lower(p.firstName) = :firstName")
                        .parameter("lastName", lastName.toLowerCase())
                        .parameter("firstName", firstName.toLowerCase())
                        .view("person-picker-view")
                        .list();
            } else {
                String queryName = !lastName.isEmpty() ? lastName : firstName;
                candidates = dataManager.load(Person.class)
                        .query("select p from hunttech_Person p where lower(p.secondName) like :name or lower(p.firstName) like :name")
                        .parameter("name", "%" + queryName.toLowerCase() + "%")
                        .view("person-picker-view")
                        .list();
            }

            if (candidates != null && !candidates.isEmpty()) {
                return candidates.get(0);
            }
        } catch (Exception e) {
            log.warn("Не удалось найти Person в БД: {}", e.getMessage());
        }

        // Если не найден — создаем нового человека
        if (metadata != null) {
            Person newPerson = metadata.create(Person.class);
            newPerson.setSecondName(lastName);
            newPerson.setFirstName(firstName);
            newPerson.setMiddleName(middleName);
            if (data.getDirectorPhone() != null && !data.getDirectorPhone().trim().isEmpty()) {
                newPerson.setPhone(data.getDirectorPhone().trim());
            }
            if (data.getDirectorEmail() != null && !data.getDirectorEmail().trim().isEmpty()) {
                newPerson.setEmail(data.getDirectorEmail().trim());
            }
            try {
                return dataManager.commit(newPerson);
            } catch (Exception e) {
                return newPerson;
            }
        }
        return null;
    }

    @Override
    public Company applyRequisitesToCompany(Company company, CompanyRequisitesParsedData data) {
        if (company == null) {
            company = metadata != null ? metadata.create(Company.class) : new Company();
        }
        if (data == null) return company;

        if (data.getInn() != null && !data.getInn().trim().isEmpty()) {
            company.setInn(data.getInn().trim());
        }
        if (data.getKpp() != null && !data.getKpp().trim().isEmpty()) {
            company.setKpp(data.getKpp().trim());
        }
        if (data.getOgrn() != null && !data.getOgrn().trim().isEmpty()) {
            company.setOgrn(data.getOgrn().trim());
        }
        if (data.getOkpo() != null && !data.getOkpo().trim().isEmpty()) {
            company.setOkpo(data.getOkpo().trim());
        }
        if (data.getOktmo() != null && !data.getOktmo().trim().isEmpty()) {
            company.setOktmo(data.getOktmo().trim());
        }
        if (data.getOkved() != null && !data.getOkved().trim().isEmpty()) {
            company.setOkved(data.getOkved().trim());
        }

        // Юридическое лицо
        if (data.getLegalEntityName() != null && !data.getLegalEntityName().trim().isEmpty()) {
            company.setLegalEntityName(data.getLegalEntityName().trim());
        } else if (data.getCompanyName() != null && !data.getCompanyName().trim().isEmpty()) {
            if (company.getLegalEntityName() == null || company.getLegalEntityName().trim().isEmpty()) {
                company.setLegalEntityName(data.getCompanyName().trim());
            }
        }

        // Организационно-правовая форма (Ownershup)
        com.company.hunttech.entity.Ownershup ownership = resolveOrCreateOwnership(data.getOwnership());
        if (ownership != null) {
            company.setCompanyOwnership(ownership);
        }

        // Гео-разбиение: Страна, Регион, Город, Адрес
        com.company.hunttech.entity.Country country = resolveOrCreateCountry(data.getCountry());
        com.company.hunttech.entity.Region region = resolveOrCreateRegion(data.getRegion(), country);
        com.company.hunttech.entity.City city = resolveOrCreateCity(data.getCity(), region);

        if (city != null) {
            company.setCityOfCompany(city);
            if (region == null && city.getCityRegion() != null) {
                region = city.getCityRegion();
            }
        }
        if (region != null) {
            company.setRegionOfCompany(region);
            if (country == null && region.getRegionCountry() != null) {
                country = region.getRegionCountry();
            }
        }
        if (country != null) {
            company.setCountryOfCompany(country);
        }

        if (data.getStreetAddress() != null && !data.getStreetAddress().trim().isEmpty()) {
            company.setAddressOfCompany(data.getStreetAddress().trim());
        } else if (data.getLegalAddress() != null && !data.getLegalAddress().trim().isEmpty()) {
            if (company.getAddressOfCompany() == null || company.getAddressOfCompany().trim().isEmpty()) {
                company.setAddressOfCompany(data.getLegalAddress().trim());
            }
        }

        if (data.getLegalAddress() != null && !data.getLegalAddress().trim().isEmpty()) {
            company.setLegalAddress(data.getLegalAddress().trim());
        }
        if (data.getActualAddress() != null && !data.getActualAddress().trim().isEmpty()) {
            company.setActualAddress(data.getActualAddress().trim());
        }
        if (data.getPostalAddress() != null && !data.getPostalAddress().trim().isEmpty()) {
            company.setPostalAddress(data.getPostalAddress().trim());
        }
        if (data.getBik() != null && !data.getBik().trim().isEmpty()) {
            company.setBik(data.getBik().trim());
        }
        if (data.getBankName() != null && !data.getBankName().trim().isEmpty()) {
            company.setBankName(data.getBankName().trim());
        }
        if (data.getSettlementAccount() != null && !data.getSettlementAccount().trim().isEmpty()) {
            company.setSettlementAccount(data.getSettlementAccount().trim());
        }
        if (data.getCorrespondentAccount() != null && !data.getCorrespondentAccount().trim().isEmpty()) {
            company.setCorrespondentAccount(data.getCorrespondentAccount().trim());
        }
        if (data.getPhone() != null && !data.getPhone().trim().isEmpty()) {
            company.setPhone(data.getPhone().trim());
        }
        if (data.getEmail() != null && !data.getEmail().trim().isEmpty()) {
            company.setEmail(data.getEmail().trim());
        }
        if (data.getWebsite() != null && !data.getWebsite().trim().isEmpty()) {
            company.setWebsite(data.getWebsite().trim());
        }
        if ((company.getComanyName() == null || company.getComanyName().trim().isEmpty())
                && data.getCompanyName() != null && !data.getCompanyName().trim().isEmpty()) {
            company.setComanyName(data.getCompanyName().trim());
        }
        if ((company.getCompanyShortName() == null || company.getCompanyShortName().trim().isEmpty())
                && data.getCompanyShortName() != null && !data.getCompanyShortName().trim().isEmpty()) {
            company.setCompanyShortName(data.getCompanyShortName().trim());
        }

        // Генеральный директор
        Person director = resolveOrCreateDirector(data);
        if (director != null) {
            company.setCompanyDirector(director);
        }
        return company;
    }

    private com.company.hunttech.entity.Ownershup resolveOrCreateOwnership(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        String lower = name.toLowerCase();

        String shortType;
        String longType;

        if (lower.contains("ограничен") || lower.equals("ооо")) {
            shortType = "ООО";
            longType = "Общество с ограниченной ответственностью";
        } else if (lower.contains("публичн") || lower.equals("пао")) {
            shortType = "ПАО";
            longType = "Публичное акционерное общество";
        } else if (lower.contains("акционерн") || lower.equals("ао")) {
            shortType = "АО";
            longType = "Акционерное общество";
        } else if (lower.contains("предприним") || lower.equals("ип")) {
            shortType = "ИП";
            longType = "Индивидуальный предприниматель";
        } else if (lower.contains("некоммерческ") || lower.equals("ано")) {
            shortType = "АНО";
            longType = "Автономная некоммерческая организация";
        } else if (lower.equals("зао") || lower.contains("закрытое акционерное")) {
            shortType = "ЗАО";
            longType = "Закрытое акционерное общество";
        } else if (lower.equals("оао") || lower.contains("открытое акционерное")) {
            shortType = "ОАО";
            longType = "Открытое акционерное общество";
        } else {
            shortType = name.length() > 7 ? name.substring(0, 7).toUpperCase() : name.toUpperCase();
            longType = name.length() > 50 ? name.substring(0, 50) : name;
        }

        try {
            List<com.company.hunttech.entity.Ownershup> list = dataManager.load(com.company.hunttech.entity.Ownershup.class)
                    .query("select e from hunttech_Ownershup e where lower(e.shortType) = :shortName or lower(e.longType) = :longName or lower(e.shortType) = :raw or lower(e.longType) = :raw")
                    .parameter("shortName", shortType.toLowerCase())
                    .parameter("longName", longType.toLowerCase())
                    .parameter("raw", lower)
                    .view("_minimal")
                    .list();
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить Ownershup из БД: {}", e.getMessage());
        }

        if (metadata != null) {
            com.company.hunttech.entity.Ownershup o = metadata.create(com.company.hunttech.entity.Ownershup.class);
            o.setShortType(shortType);
            o.setLongType(longType);
            try {
                return dataManager.commit(o);
            } catch (Exception e) {
                return o;
            }
        }
        return null;
    }

    private com.company.hunttech.entity.Country resolveOrCreateCountry(String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "Россия";
        }
        name = name.trim();
        try {
            List<com.company.hunttech.entity.Country> list = dataManager.load(com.company.hunttech.entity.Country.class)
                    .query("select e from hunttech_Country e where lower(e.countryRuName) = :name or lower(e.countryShortName) = :name")
                    .parameter("name", name.toLowerCase())
                    .view("country-picker-view")
                    .list();
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить Country из БД: {}", e.getMessage());
        }
        if (metadata != null) {
            com.company.hunttech.entity.Country c = metadata.create(com.company.hunttech.entity.Country.class);
            c.setCountryRuName(name.length() > 50 ? name.substring(0, 50) : name);
            c.setCountryShortName(name.equalsIgnoreCase("Россия") ? "RU" : (name.length() > 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase()));
            try {
                return dataManager.commit(c);
            } catch (Exception e) {
                return c;
            }
        }
        return null;
    }

    private com.company.hunttech.entity.Region resolveOrCreateRegion(String name, com.company.hunttech.entity.Country country) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        try {
            List<com.company.hunttech.entity.Region> list = dataManager.load(com.company.hunttech.entity.Region.class)
                    .query("select e from hunttech_Region e where lower(e.regionRuName) = :name")
                    .parameter("name", name.toLowerCase())
                    .view("region-browse-view")
                    .list();
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить Region из БД: {}", e.getMessage());
        }
        if (metadata != null) {
            com.company.hunttech.entity.Region r = metadata.create(com.company.hunttech.entity.Region.class);
            r.setRegionRuName(name.length() > 50 ? name.substring(0, 50) : name);
            if (country != null) {
                r.setRegionCountry(country);
            }
            try {
                return dataManager.commit(r);
            } catch (Exception e) {
                return r;
            }
        }
        return null;
    }

    private com.company.hunttech.entity.City resolveOrCreateCity(String name, com.company.hunttech.entity.Region region) {
        if (name == null || name.trim().isEmpty()) return null;
        name = name.trim();
        try {
            List<com.company.hunttech.entity.City> list = dataManager.load(com.company.hunttech.entity.City.class)
                    .query("select e from hunttech_City e where lower(e.cityRuName) = :name")
                    .parameter("name", name.toLowerCase())
                    .view("city-location-view")
                    .list();
            if (list != null && !list.isEmpty()) {
                return list.get(0);
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить City из БД: {}", e.getMessage());
        }
        if (metadata != null) {
            com.company.hunttech.entity.City c = metadata.create(com.company.hunttech.entity.City.class);
            c.setCityRuName(name.length() > 50 ? name.substring(0, 50) : name);
            if (region != null) {
                c.setCityRegion(region);
            }
            try {
                return dataManager.commit(c);
            } catch (Exception e) {
                return c;
            }
        }
        return null;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName)) return null;
        String val = node.get(fieldName).asText();
        return val != null && !val.trim().isEmpty() ? val.trim() : null;
    }

    private String cleanJson(String raw) {
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }
}
