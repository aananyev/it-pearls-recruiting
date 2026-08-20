package com.company.hunttech.core;

import com.company.hunttech.service.SmartOpenPositionParsedData;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест сервиса умной загрузки вакансий SmartOpenPositionIngestService и интеграции с экранами.
 */
public class SmartOpenPositionIngestServiceContractTest {

    @Test
    public void testServiceAndScreenContracts() throws Exception {
        // 1. Проверка интерфейса и реализации сервиса
        File interfaceFile = new File("../global/src/com/company/hunttech/service/SmartOpenPositionIngestService.java");
        assertTrue("SmartOpenPositionIngestService.java должен существовать", interfaceFile.exists());

        File beanFile = new File("src/com/company/hunttech/service/SmartOpenPositionIngestServiceBean.java");
        assertTrue("SmartOpenPositionIngestServiceBean.java должен существовать", beanFile.exists());

        // 2. Проверка регистрации в web-spring.xml
        String webSpring = Files.readString(Paths.get("../web/src/com/company/hunttech/web-spring.xml"));
        assertTrue("web-spring.xml должен регистрировать hunttech_SmartOpenPositionIngestService",
                webSpring.contains("hunttech_SmartOpenPositionIngestService"));

        // 3. Проверка экрана загрузки вакансий
        File screenXml = new File("../web/src/com/company/hunttech/web/screens/openposition/smart-open-position-upload-screen.xml");
        assertTrue("smart-open-position-upload-screen.xml должен существовать", screenXml.exists());

        File screenJava = new File("../web/src/com/company/hunttech/web/screens/openposition/SmartOpenPositionUploadScreen.java");
        assertTrue("SmartOpenPositionUploadScreen.java должен существовать", screenJava.exists());
    }

    @Test
    public void testParsedDataModel() {
        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setVacansyName("Senior Java Developer");
        data.setProjectName("HuntTech HRM");
        data.setPositionTypeName("Java Developer");
        data.setGradeName("Senior");
        data.setCityName("Москва");
        data.setRemoteWork(1);

        assertNotNull(data.getVacansyName());
        assertTrue(data.getVacansyName().equals("Senior Java Developer"));
    }
}
