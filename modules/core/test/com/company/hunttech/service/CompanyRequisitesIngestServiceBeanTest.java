package com.company.hunttech.service;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Person;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class CompanyRequisitesIngestServiceBeanTest {

    private CompanyRequisitesIngestServiceBean service;
    private DataManager mockDataManager;
    private Metadata mockMetadata;
    private AiExecutionService mockAiService;

    private final AtomicBoolean committed = new AtomicBoolean(false);

    @Before
    public void setUp() throws Exception {
        service = new CompanyRequisitesIngestServiceBean();
        committed.set(false);

        mockMetadata = (Metadata) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{Metadata.class},
                (proxy, method, args) -> {
                    if ("create".equals(method.getName()) && args != null && args.length == 1) {
                        if (args[0] == Person.class) return new Person();
                        if (args[0] == com.company.hunttech.entity.Country.class) return new com.company.hunttech.entity.Country();
                        if (args[0] == com.company.hunttech.entity.Region.class) return new com.company.hunttech.entity.Region();
                        if (args[0] == com.company.hunttech.entity.City.class) return new com.company.hunttech.entity.City();
                        if (args[0] == com.company.hunttech.entity.Ownershup.class) return new com.company.hunttech.entity.Ownershup();
                    }
                    return null;
                }
        );

        mockDataManager = (DataManager) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{DataManager.class},
                (proxy, method, args) -> {
                    if ("commit".equals(method.getName()) && args != null && args.length >= 1) {
                        committed.set(true);
                        return args[0];
                    }
                    if ("load".equals(method.getName()) && args != null && args.length == 1) {
                        return new com.haulmont.cuba.core.global.FluentLoader((Class) args[0], (DataManager) proxy);
                    }
                    if ("loadList".equals(method.getName())) {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        mockAiService = (AiExecutionService) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{AiExecutionService.class},
                (proxy, method, args) -> {
                    if ("executeText".equals(method.getName())) {
                        String json = "{\n" +
                                "  \"companyName\": \"ООО Ромашка Плюс\",\n" +
                                "  \"companyShortName\": \"Ромашка Плюс\",\n" +
                                "  \"legalEntityName\": \"ООО «Ромашка Плюс»\",\n" +
                                "  \"ownership\": \"ООО\",\n" +
                                "  \"inn\": \"7701234567\",\n" +
                                "  \"kpp\": \"770101001\",\n" +
                                "  \"ogrn\": \"1027700132195\",\n" +
                                "  \"okpo\": \"12345678\",\n" +
                                "  \"oktmo\": \"45300000\",\n" +
                                "  \"okved\": \"62.01\",\n" +
                                "  \"country\": \"Россия\",\n" +
                                "  \"region\": \"г. Москва\",\n" +
                                "  \"city\": \"Москва\",\n" +
                                "  \"streetAddress\": \"ул. Ленина, д. 1\",\n" +
                                "  \"legalAddress\": \"123456, г. Москва, ул. Ленина, д. 1\",\n" +
                                "  \"actualAddress\": \"123456, г. Москва, ул. Ленина, д. 1\",\n" +
                                "  \"postalAddress\": \"123456, г. Москва, а/я 10\",\n" +
                                "  \"bik\": \"044525225\",\n" +
                                "  \"bankName\": \"ПАО Сбербанк\",\n" +
                                "  \"settlementAccount\": \"40702810938000001234\",\n" +
                                "  \"correspondentAccount\": \"30101810400000000225\",\n" +
                                "  \"phone\": \"+7 (495) 123-45-67\",\n" +
                                "  \"email\": \"info@romashka.ru\",\n" +
                                "  \"website\": \"https://romashka.ru\",\n" +
                                "  \"directorLastName\": \"Иванов\",\n" +
                                "  \"directorFirstName\": \"Иван\",\n" +
                                "  \"directorMiddleName\": \"Иванович\",\n" +
                                "  \"directorPosition\": \"Генеральный директор\",\n" +
                                "  \"directorPhone\": \"+7 (999) 111-22-33\",\n" +
                                "  \"directorEmail\": \"ivanov@romashka.ru\"\n" +
                                "}";
                        return AiExecutionResult.textResult(
                                "COMPANY_REQUISITES_PARSE_JSON",
                                "Умное распознавание реквизитов компании",
                                com.company.hunttech.entity.ai.AiCapability.TEXT_ANALYSIS,
                                "gpt-4",
                                "openai",
                                AiCredentialOwner.ADMIN,
                                json
                        );
                    }
                    return null;
                }
        );

        setField(service, "dataManager", mockDataManager);
        setField(service, "metadata", mockMetadata);
        setField(service, "aiExecutionService", mockAiService);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testParseRequisites_AiSuccess() {
        String raw = "Карточка компании ООО Ромашка Плюс...";
        CompanyRequisitesParsedData data = service.parseRequisites(raw);

        assertNotNull(data);
        assertEquals("ООО Ромашка Плюс", data.getCompanyName());
        assertEquals("ООО «Ромашка Плюс»", data.getLegalEntityName());
        assertEquals("ООО", data.getOwnership());
        assertEquals("7701234567", data.getInn());
        assertEquals("770101001", data.getKpp());
        assertEquals("1027700132195", data.getOgrn());
        assertEquals("Россия", data.getCountry());
        assertEquals("г. Москва", data.getRegion());
        assertEquals("Москва", data.getCity());
        assertEquals("ул. Ленина, д. 1", data.getStreetAddress());
        assertEquals("044525225", data.getBik());
        assertEquals("40702810938000001234", data.getSettlementAccount());
        assertEquals("30101810400000000225", data.getCorrespondentAccount());
        assertEquals("Иванов", data.getDirectorLastName());
        assertEquals("Иван", data.getDirectorFirstName());
        assertEquals("Иванович", data.getDirectorMiddleName());
        assertEquals("Иванов Иван Иванович", data.getDirectorFullName());
    }

    @Test
    public void testApplyRequisitesToCompany() {
        CompanyRequisitesParsedData data = new CompanyRequisitesParsedData();
        data.setCompanyName("Вектор");
        data.setLegalEntityName("АО «Вектор Солюшнс»");
        data.setOwnership("АО");
        data.setInn("7801234567");
        data.setKpp("780101001");
        data.setOgrn("1037800001122");
        data.setCountry("Россия");
        data.setCity("Санкт-Петербург");
        data.setStreetAddress("Невский пр-т, д. 10");
        data.setLegalAddress("г. Санкт-Петербург, Невский пр-т, д. 10");
        data.setBik("044030653");
        data.setBankName("ПАО Банк ВТБ");
        data.setSettlementAccount("40702810100000005555");
        data.setPhone("+7 812 555-44-33");
        data.setEmail("contact@vector.spb.ru");
        data.setWebsite("https://vector.spb.ru");

        Company company = new Company();
        service.applyRequisitesToCompany(company, data);

        assertEquals("7801234567", company.getInn());
        assertEquals("780101001", company.getKpp());
        assertEquals("1037800001122", company.getOgrn());
        assertEquals("АО «Вектор Солюшнс»", company.getLegalEntityName());
        assertNotNull(company.getCompanyOwnership());
        assertEquals("АО", company.getCompanyOwnership().getShortType());
        assertEquals("г. Санкт-Петербург, Невский пр-т, д. 10", company.getLegalAddress());
        assertEquals("Невский пр-т, д. 10", company.getAddressOfCompany());
        assertNotNull(company.getCityOfCompany());
        assertEquals("Санкт-Петербург", company.getCityOfCompany().getCityRuName());
        assertNotNull(company.getCountryOfCompany());
        assertEquals("Россия", company.getCountryOfCompany().getCountryRuName());
        assertEquals("044030653", company.getBik());
        assertEquals("ПАО Банк ВТБ", company.getBankName());
        assertEquals("40702810100000005555", company.getSettlementAccount());
        assertEquals("+7 812 555-44-33", company.getPhone());
        assertEquals("contact@vector.spb.ru", company.getEmail());
        assertEquals("https://vector.spb.ru", company.getWebsite());
        assertEquals("Вектор", company.getComanyName());
    }

    @Test
    public void testParseAndApplyExactSampleRequisites() {
        CompanyRequisitesParsedData data = new CompanyRequisitesParsedData();
        data.setCompanyName("ХантТек Солюшнс");
        data.setCompanyShortName("ООО «ХантТек»");
        data.setLegalEntityName("Общество с ограниченной ответственностью «ХантТек Солюшнс»");
        data.setOwnership("ООО");
        data.setInn("7701234567");
        data.setKpp("770101001");
        data.setOgrn("1027700132195");
        data.setOkpo("12345678");
        data.setOkved("62.01");
        data.setCountry("Россия");
        data.setRegion("г. Москва");
        data.setCity("Москва");
        data.setStreetAddress("Пресненская наб., д. 12, эт. 45, оф. 2");
        data.setLegalAddress("123112, г. Москва, Пресненская наб., д. 12, эт. 45, оф. 2");
        data.setActualAddress("123112, г. Москва, Пресненская наб., д. 12, эт. 45, оф. 2");
        data.setBik("044525225");
        data.setBankName("ПАО СБЕРБАНК");
        data.setSettlementAccount("40702810938000001234");
        data.setCorrespondentAccount("30101810400000000225");
        data.setPhone("+7 (495) 999-88-77");
        data.setEmail("info@hunttech.ru");
        data.setWebsite("https://hunttech.ru");
        data.setDirectorLastName("Алексеев");
        data.setDirectorFirstName("Алексей");
        data.setDirectorMiddleName("Сергеевич");

        Company company = new Company();
        service.applyRequisitesToCompany(company, data);

        assertEquals("7701234567", company.getInn());
        assertEquals("770101001", company.getKpp());
        assertEquals("1027700132195", company.getOgrn());
        assertEquals("12345678", company.getOkpo());
        assertEquals("62.01", company.getOkved());
        assertEquals("Общество с ограниченной ответственностью «ХантТек Солюшнс»", company.getLegalEntityName());
        assertEquals("ХантТек Солюшнс", company.getComanyName());
        assertEquals("ООО «ХантТек»", company.getCompanyShortName());
        assertNotNull(company.getCompanyOwnership());
        assertEquals("ООО", company.getCompanyOwnership().getShortType());
        assertNotNull(company.getCountryOfCompany());
        assertEquals("Россия", company.getCountryOfCompany().getCountryRuName());
        assertNotNull(company.getCityOfCompany());
        assertEquals("Москва", company.getCityOfCompany().getCityRuName());
        assertEquals("Пресненская наб., д. 12, эт. 45, оф. 2", company.getAddressOfCompany());
        assertEquals("123112, г. Москва, Пресненская наб., д. 12, эт. 45, оф. 2", company.getLegalAddress());
        assertEquals("044525225", company.getBik());
        assertEquals("ПАО СБЕРБАНК", company.getBankName());
        assertEquals("40702810938000001234", company.getSettlementAccount());
        assertEquals("30101810400000000225", company.getCorrespondentAccount());
        assertEquals("+7 (495) 999-88-77", company.getPhone());
        assertEquals("info@hunttech.ru", company.getEmail());
        assertEquals("https://hunttech.ru", company.getWebsite());
        assertNotNull(company.getCompanyDirector());
        assertEquals("Алексеев", company.getCompanyDirector().getSecondName());
        assertEquals("Алексей", company.getCompanyDirector().getFirstName());
        assertEquals("Сергеевич", company.getCompanyDirector().getMiddleName());
    }
}
