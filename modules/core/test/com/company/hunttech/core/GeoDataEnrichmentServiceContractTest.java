package com.company.hunttech.core;

import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.GeoCityData;
import com.company.hunttech.entity.GeoCountryData;
import com.company.hunttech.entity.GeoRegionData;
import com.company.hunttech.service.GeoDataEnrichmentService;
import com.company.hunttech.service.GeoDataEnrichmentServiceBean;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class GeoDataEnrichmentServiceContractTest {

    @Test
    public void testCountryEnrichmentOfflineMatching() {
        GeoDataEnrichmentServiceBean service = new GeoDataEnrichmentServiceBean();

        // 1. Проверка обогащения России
        GeoCountryData russia = service.enrichCountry("Россия");
        assertNotNull("Данные для России должны быть найдены", russia);
        assertEquals("Россия", russia.getCountryRuName());
        assertEquals("Russia", russia.getCountryEngName());
        assertEquals("RU", russia.getCountryShortName());
        assertEquals("RUS", russia.getAlpha3Code());
        assertEquals("643", russia.getNumericCode());
        assertEquals(Integer.valueOf(7), russia.getPhoneCode());
        assertEquals("RUB", russia.getCurrencyCode());
        assertEquals("Москва", russia.getCapital());
        assertNotNull("URL флага должен быть сформирован", russia.getFlagUrl());

        // 2. Проверка обогащения по ISO-коду
        GeoCountryData belarus = service.enrichCountry("BY");
        assertNotNull("Данные для Беларуси должны быть найдены по коду BY", belarus);
        assertEquals("BY", belarus.getCountryShortName());
        assertEquals("Минск", belarus.getCapital());
    }

    @Test
    public void testRegionEnrichmentOfflineMatching() {
        GeoDataEnrichmentServiceBean service = new GeoDataEnrichmentServiceBean();
        Country country = new Country();
        country.setCountryRuName("Россия");

        GeoRegionData moscow = service.enrichRegion("Москва", country);
        assertNotNull("Данные для Москвы должны быть найдены", moscow);
        assertEquals("Москва", moscow.getRegionRuName());
        assertEquals(Integer.valueOf(77), moscow.getRegionCode());
        assertEquals("RU-MOW", moscow.getIsoCode());
        assertEquals("Город федерального значения", moscow.getRegionType());
        assertNotNull("Герб Москвы должен быть получен", moscow.getEmblemUrl());

        GeoRegionData tatarstan = service.enrichRegion("Республика Татарстан", country);
        assertNotNull("Данные для Татарстана должны быть найдены", tatarstan);
        assertEquals(Integer.valueOf(16), tatarstan.getRegionCode());
        assertEquals("Казань", tatarstan.getCapital());
    }

    @Test
    public void testCityEnrichmentOfflineMatching() {
        GeoDataEnrichmentServiceBean service = new GeoDataEnrichmentServiceBean();

        GeoCityData spb = service.enrichCity("Санкт-Петербург", null, null);
        assertNotNull("Данные для Санкт-Петербурга должны быть найдены", spb);
        assertEquals("Санкт-Петербург", spb.getCityRuName());
        assertEquals("812", spb.getCityPhoneCode());
        assertEquals("190000", spb.getPostalCode());
        assertNotNull("Координаты должны присутствовать", spb.getLatitude());
        assertNotNull("Координаты долготы должны присутствовать", spb.getLongitude());
    }

    private static String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private static Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        return root != null ? root : Paths.get(".").toAbsolutePath();
    }
}
