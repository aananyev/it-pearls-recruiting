package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает контракт структуры меню раздела «Проекты» (application-project) в HRM HuntTech:
 * 1. Пункты устаревших CRUD-экранов скрыты из меню:
 *    - hunttech_Project.browse
 *    - hunttech_Person.browse
 *    - hunttech_Company.browse
 *    - hunttech_CompanyGroup.browse
 *    - hunttech_CompanyDepartament.browse
 * 2. Пункты современных Reestr-экранов присутствуют с утверждёнными подписями:
 *    - hunttech_ProjectReestr.browse -> "Проекты заказчиков"
 *    - hunttech_PersonReestr.browse -> "Контактные лица"
 *    - hunttech_CompanyDepartamentReestr.browse -> "Департаменты"
 *    - hunttech_CompanyReestr.browse -> "Компании"
 *    - hunttech_CompanyGroupReestr.browse -> "Группы компаний"
 * 3. Локализационные сообщения зарегистрированы в messages_ru.properties и messages.properties.
 */
public class MainMenuReorganizationContractTest {

    private static final String MENU = "modules/web/src/com/company/hunttech/web-menu.xml";
    private static final String MESSAGES_RU = "modules/web/src/com/company/hunttech/web/messages_ru.properties";
    private static final String MESSAGES_EN = "modules/web/src/com/company/hunttech/web/messages.properties";

    @Test
    public void testApplicationProjectMenuStructure() throws IOException {
        String xml = readProjectFile(MENU);

        int menuStart = xml.indexOf("<menu id=\"application-project\"");
        assertTrue("Раздел меню application-project должен существовать", menuStart >= 0);
        int menuEnd = xml.indexOf("</menu>", menuStart);
        assertTrue("Закрывающий тег раздела application-project должен существовать", menuEnd > menuStart);

        String projectMenuSection = xml.substring(menuStart, menuEnd);

        // Устаревшие пункты должны быть скрыты из раздела меню
        assertFalse("Пункт hunttech_Project.browse должен быть скрыт из меню application-project",
                projectMenuSection.contains("screen=\"hunttech_Project.browse\""));
        assertFalse("Пункт hunttech_Person.browse должен быть скрыт из меню application-project",
                projectMenuSection.contains("screen=\"hunttech_Person.browse\""));
        assertFalse("Пункт hunttech_Company.browse должен быть скрыт из меню application-project",
                projectMenuSection.contains("screen=\"hunttech_Company.browse\""));
        assertFalse("Пункт hunttech_CompanyGroup.browse должен быть скрыт из меню application-project",
                projectMenuSection.contains("screen=\"hunttech_CompanyGroup.browse\""));
        assertFalse("Пункт hunttech_CompanyDepartament.browse должен быть скрыт из меню application-project",
                projectMenuSection.contains("screen=\"hunttech_CompanyDepartament.browse\""));

        // Реестровые пункты с утвержденными названиями
        assertTrue("hunttech_ProjectReestr.browse должен присутствовать с caption 'Проекты заказчиков'",
                projectMenuSection.contains("screen=\"hunttech_ProjectReestr.browse\"")
                        && projectMenuSection.contains("caption=\"Проекты заказчиков\""));

        assertTrue("hunttech_PersonReestr.browse должен присутствовать с caption 'Контактные лица'",
                projectMenuSection.contains("screen=\"hunttech_PersonReestr.browse\"")
                        && projectMenuSection.contains("caption=\"Контактные лица\""));

        assertTrue("hunttech_CompanyDepartamentReestr.browse должен присутствовать с caption 'Департаменты'",
                projectMenuSection.contains("screen=\"hunttech_CompanyDepartamentReestr.browse\"")
                        && projectMenuSection.contains("caption=\"Департаменты\""));

        assertTrue("hunttech_CompanyReestr.browse должен присутствовать с caption 'Компании'",
                projectMenuSection.contains("screen=\"hunttech_CompanyReestr.browse\"")
                        && projectMenuSection.contains("caption=\"Компании\""));

        assertTrue("hunttech_CompanyGroupReestr.browse должен присутствовать с caption 'Группы компаний'",
                projectMenuSection.contains("screen=\"hunttech_CompanyGroupReestr.browse\"")
                        && projectMenuSection.contains("caption=\"Группы компаний\""));
    }

    @Test
    public void testMenuLocalizationKeys() throws IOException {
        String ru = readProjectFile(MESSAGES_RU);
        String en = readProjectFile(MESSAGES_EN);

        assertTrue("В messages_ru.properties должен быть ключ menu-config.hunttech_ProjectReestr.browse",
                ru.contains("menu-config.hunttech_ProjectReestr.browse=Проекты заказчиков"));
        assertTrue("В messages.properties должен быть ключ menu-config.hunttech_ProjectReestr.browse",
                en.contains("menu-config.hunttech_ProjectReestr.browse=Customer Projects"));

        assertTrue("В messages_ru.properties должен быть ключ menu-config.hunttech_PersonReestr.browse",
                ru.contains("menu-config.hunttech_PersonReestr.browse=Контактные лица"));
        assertTrue("В messages.properties должен быть ключ menu-config.hunttech_PersonReestr.browse",
                en.contains("menu-config.hunttech_PersonReestr.browse=Contact Persons"));

        assertTrue("В messages_ru.properties должен быть ключ menu-config.hunttech_CompanyDepartamentReestr.browse",
                ru.contains("menu-config.hunttech_CompanyDepartamentReestr.browse=Департаменты"));
        assertTrue("В messages.properties должен быть ключ menu-config.hunttech_CompanyDepartamentReestr.browse",
                en.contains("menu-config.hunttech_CompanyDepartamentReestr.browse=Departments"));

        assertTrue("В messages_ru.properties должен быть ключ menu-config.hunttech_CompanyReestr.browse",
                ru.contains("menu-config.hunttech_CompanyReestr.browse=Компании"));
        assertTrue("В messages.properties должен быть ключ menu-config.hunttech_CompanyReestr.browse",
                en.contains("menu-config.hunttech_CompanyReestr.browse=Companies"));

        assertTrue("В messages_ru.properties должен быть ключ menu-config.hunttech_CompanyGroupReestr.browse",
                ru.contains("menu-config.hunttech_CompanyGroupReestr.browse=Группы компаний"));
        assertTrue("В messages.properties должен быть ключ menu-config.hunttech_CompanyGroupReestr.browse",
                en.contains("menu-config.hunttech_CompanyGroupReestr.browse=Company Groups"));
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8);
    }

    private Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return root;
    }
}
