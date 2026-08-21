package com.company.hunttech.core;

import com.company.hunttech.service.SmartCvIngestService;
import com.company.hunttech.service.SmartCvParsedData;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест сервиса умной загрузки резюме SmartCvIngestService и интеграции с экранами.
 */
public class SmartCvIngestServiceContractTest {

    @Test
    public void testMigrationAndServiceContracts() throws Exception {
        // 1. Проверка наличия Liquibase миграции
        File migrationFile = new File("db/changelog/260820-1-addSmartCvParseAiFunction.xml");
        assertTrue("Миграция 260820-1-addSmartCvParseAiFunction.xml должна существовать", migrationFile.exists());

        String migrationContent = Files.readString(migrationFile.toPath());
        assertTrue("Миграция должна содержать код функции CV_SMART_PARSE_JSON", migrationContent.contains("CV_SMART_PARSE_JSON"));
        assertTrue("Миграция должна объявлять capability TEXT_ANALYSIS", migrationContent.contains("TEXT_ANALYSIS"));

        // 2. Проверка регистрации в changelog-master
        String masterContent = Files.readString(Paths.get("db/changelog/db.changelog-master.xml"));
        assertTrue("db.changelog-master.xml должен подключать 260820-1-addSmartCvParseAiFunction.xml",
                masterContent.contains("260820-1-addSmartCvParseAiFunction.xml"));

        // 3. Проверка интерфейса и реализации сервиса
        File interfaceFile = new File("../global/src/com/company/hunttech/service/SmartCvIngestService.java");
        assertTrue("SmartCvIngestService.java должен существовать", interfaceFile.exists());

        File beanFile = new File("src/com/company/hunttech/service/SmartCvIngestServiceBean.java");
        assertTrue("SmartCvIngestServiceBean.java должен существовать", beanFile.exists());

        // 4. Проверка экрана загрузки резюме
        File screenXml = new File("../web/src/com/company/hunttech/web/screens/jobcandidate/smart-cv-upload-screen.xml");
        assertTrue("smart-cv-upload-screen.xml должен существовать", screenXml.exists());

        File screenJava = new File("../web/src/com/company/hunttech/web/screens/jobcandidate/SmartCvUploadScreen.java");
        assertTrue("SmartCvUploadScreen.java должен существовать", screenJava.exists());
    }

    @Test
    public void testParsedDataModel() {
        SmartCvParsedData data = new SmartCvParsedData();
        data.setLastName("Иванов");
        data.setFirstName("Иван");
        data.setMiddleName("Иванович");
        data.setPhone("+7 (999) 111-22-33");
        data.setEmail("ivanov@example.com");
        data.setPosition("Senior Java Developer");
        data.setCity("Москва");

        assertTrue(data.getFullName().equals("Иванов Иван Иванович"));
        assertNotNull(data.getSkills());

        com.company.hunttech.service.dto.cv.SmartCvWorkExperienceDto exp = new com.company.hunttech.service.dto.cv.SmartCvWorkExperienceDto();
        exp.setCompanyName("HuntTech Solutions");
        exp.setPositionName("Lead Developer");
        exp.setStartDate("2021-01-15");
        exp.setEndDate("2023-12-31");
        exp.setDuties("Архитектура и разработка микросервисов");
        exp.setAchievements("Запустил 5 проектов в прод");

        data.setWorkExperience(java.util.Collections.singletonList(exp));
        assertTrue(data.getWorkExperience().size() == 1);
        assertTrue(data.getWorkExperience().get(0).getCompanyName().equals("HuntTech Solutions"));
        assertTrue(data.getWorkExperience().get(0).getFullDescription().contains("Достижения"));

        com.company.hunttech.service.dto.cv.SmartCvEducationDto edu = new com.company.hunttech.service.dto.cv.SmartCvEducationDto();
        edu.setInstitution("МГТУ им. Н.Э. Баумана");
        edu.setSpecialty("Информатика и вычислительная техника");
        edu.setGraduationYear(2018);
        edu.setDegree("Магистр");

        data.setEducation(java.util.Collections.singletonList(edu));
        assertTrue(data.getEducation().size() == 1);
        assertTrue(data.getEducation().get(0).getFormattedSummary().contains("Баумана"));
    }

    @Test
    public void testJobHistoryFieldsAndMigrations() throws Exception {
        File jobHistoryMigration = new File("db/changelog/260821-3-addJobHistoryFields.xml");
        assertTrue("Миграция 260821-3-addJobHistoryFields.xml должна существовать", jobHistoryMigration.exists());

        File promptMigration = new File("db/changelog/260821-4-updateSmartCvParseAiFunction.xml");
        assertTrue("Миграция 260821-4-updateSmartCvParseAiFunction.xml должна существовать", promptMigration.exists());

        String masterContent = Files.readString(Paths.get("db/changelog/db.changelog-master.xml"));
        assertTrue(masterContent.contains("260821-3-addJobHistoryFields.xml"));
        assertTrue(masterContent.contains("260821-4-updateSmartCvParseAiFunction.xml"));
    }
}
