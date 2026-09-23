package com.company.hunttech.core;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест регистрации и настройки ExternalReferenceDataService в CUBA REST API v2.
 */
public class ExternalReferenceDataRestContractTest {

    @Test
    public void testRestServicesConfigurationExistsAndContainsReferenceService() throws Exception {
        File restServicesFile = new File("../web/src/com/company/hunttech/rest-services.xml");
        if (!restServicesFile.exists()) {
            restServicesFile = new File("modules/web/src/com/company/hunttech/rest-services.xml");
        }
        assertTrue("rest-services.xml должен существовать", restServicesFile.exists());

        String content = new String(Files.readAllBytes(restServicesFile.toPath()));
        assertTrue("rest-services.xml должен содержать hunttech_ExternalReferenceDataService",
                content.contains("hunttech_ExternalReferenceDataService"));
        assertTrue("rest-services.xml должен экспортировать метод getCities",
                content.contains("name=\"getCities\""));
        assertTrue("rest-services.xml должен экспортировать метод getPositions",
                content.contains("name=\"getPositions\""));
        assertTrue("rest-services.xml должен экспортировать метод getGrades",
                content.contains("name=\"getGrades\""));
        assertTrue("rest-services.xml должен экспортировать метод getInteractionTypes",
                content.contains("name=\"getInteractionTypes\""));
        assertTrue("rest-services.xml должен экспортировать метод getSkills",
                content.contains("name=\"getSkills\""));
        assertTrue("rest-services.xml должен экспортировать метод getCountries",
                content.contains("name=\"getCountries\""));

        assertTrue("rest-services.xml должен содержать hunttech_ExternalIntegrationService",
                content.contains("hunttech_ExternalIntegrationService"));
        assertTrue("rest-services.xml должен экспортировать метод createCompany",
                content.contains("name=\"createCompany\""));
        assertTrue("rest-services.xml должен экспортировать метод createProjectAndVacancy",
                content.contains("name=\"createProjectAndVacancy\""));
    }


    @Test
    public void testWebAppPropertiesEnablesServicesConfig() throws Exception {
        File webAppPropsFile = new File("../web/src/com/company/hunttech/web-app.properties");
        if (!webAppPropsFile.exists()) {
            webAppPropsFile = new File("modules/web/src/com/company/hunttech/web-app.properties");
        }
        assertTrue("web-app.properties должен существовать", webAppPropsFile.exists());

        String content = new String(Files.readAllBytes(webAppPropsFile.toPath()));
        assertTrue("web-app.properties должен содержать cuba.rest.servicesConfig",
                content.contains("cuba.rest.servicesConfig = +com/company/hunttech/rest-services.xml"));
    }

    @Test
    public void testRestQueriesContainsRequiredReferenceQueries() throws Exception {
        File restQueriesFile = new File("../web/src/com/company/hunttech/rest-queries.xml");
        if (!restQueriesFile.exists()) {
            restQueriesFile = new File("modules/web/src/com/company/hunttech/rest-queries.xml");
        }
        assertTrue("rest-queries.xml должен существовать", restQueriesFile.exists());

        String content = new String(Files.readAllBytes(restQueriesFile.toPath()));
        assertTrue("rest-queries.xml должен содержать cityAll", content.contains("name=\"cityAll\""));
        assertTrue("rest-queries.xml должен содержать positionAll", content.contains("name=\"positionAll\""));
        assertTrue("rest-queries.xml должен содержать gradeAll", content.contains("name=\"gradeAll\""));
        assertTrue("rest-queries.xml должен содержать countryAll", content.contains("name=\"countryAll\""));
        assertTrue("rest-queries.xml должен содержать skillAll", content.contains("name=\"skillAll\""));
        assertTrue("rest-queries.xml должен содержать iteractionTypeAll", content.contains("name=\"iteractionTypeAll\""));
    }
}
