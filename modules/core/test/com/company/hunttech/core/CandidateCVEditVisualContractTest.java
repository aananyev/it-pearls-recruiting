package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает визуальный рефакторинг CandidateCVEdit от изменения функциональных
 * контрактов контроллера, XML-дескриптора, view и локальных тем.
 */
public class CandidateCVEditVisualContractTest {

    private static final String SCREEN_XML =
            "modules/web/src/com/company/hunttech/web/screens/candidatecv/candidate-cv-edit.xml";
    private static final String CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/candidatecv/CandidateCVEdit.java";
    private static final String VIEWS =
            "modules/global/src/com/company/hunttech/views.xml";
    private static final String ENTITY =
            "modules/global/src/com/company/hunttech/entity/CandidateCV.java";
    private static final String DESIGN_CONCEPT =
            "docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md";

    private static final List<String> THEMES = Arrays.asList(
            "halo",
            "havana",
            "helium",
            "hover",
            "hunttech-modern",
            "hunttech-modern-light",
            "hunttech-modern-dark"
    );

    @Test
    public void entityAndViewsRemainByteIdenticalToBaseline() throws Exception {
        /*
         * Git blob SHA фиксирует модель данных и глобальный view-контракт. Контроллер
         * проверяется семантически, поскольку задача удаляет только устаревший fallback-код.
         */
        assertEquals("59a4f65ab467b8e2b0a636d17d476644d4395e2e",
                gitBlobSha1(readProjectBytes(ENTITY)));
        assertEquals("e433bb937d3b817a8f985c1feec178a691ce85ce",
                gitBlobSha1(readProjectBytes(VIEWS)));
    }

    @Test
    public void screenKeepsLegacyIdsTabsAndLazyLifecycleNames() throws IOException {
        String xml = readProjectFile(SCREEN_XML);
        List<String> protectedIds = Arrays.asList(
                "openPositionsDc", "openPositionsDl", "candidateCVDc", "someFilesesDc", "skillTreesDc",
                "resumePositionsDc", "resumePositionsLc", "usersDc", "usersDl",
                "candidateCvMainLayout", "candidateCvSidebar", "candidateCvWorkspace",
                "candidateLabel", "iteractionListLabelCandidate", "iteractionListLabelPosition",
                "candidateCvSidebarTargetCard", "candidateCvSidebarResumePosition",
                "candidateCvSidebarVacancy", "candidateCvSidebarProject", "candidateCvSidebarMetaCard",
                "candidateCvSectionNavigation", "candidateCvCandidateNavigation", "candidateCvCvNavigation",
                "candidateCvLetterNavigation", "candidateCvSkillNavigation", "candidateCvFilesNavigation",
                "candidateCvMainDataNav", "candidateCvOriginalCvNav", "candidateCvHuntTechCvNav",
                "candidateCvTextNav", "candidateCvRecommendationNav", "candidateCvLetterTemplateNav",
                "candidateCvLetterBodyNav", "candidateCvLetterCommentNav",
                "candidateCvLetterRecommendationNav", "candidateCvSkillActionsNav",
                "candidateCvSkillTreeNav", "candidateCvFilesTableNav",
                "candidateCvSidebarSpacer", "labelLastRecrutier", "machRegexpFromCV", "quoteTextArea",
                "tabSheet", "tabCandidate", "tabCV", "tabLetter", "tabSkillTree", "tabFiles",
                "candidateScrolBox", "candidateVbox", "groupBox", "dropZone", "picVBox",
                "candidateField", "resumePositionField", "candidateCVFieldOpenPosition",
                "onlyMySubscribeCheckBox", "СandidateCVField",
                "textFieldIOriginalCV", "loadToCVTextArea", "originalCVLink", "fileOriginalCVField",
                "textFieldHuntTechCV", "HuntTechCVLink", "fileCVField",
                "candidatePic", "fileImageFaceUpload",
                "rescanSkills", "resumeRecognitionButton", "convertToTextButton", "showOriginalButon",
                "candidateCVRichTextArea", "cvResomandation",
                "questionLetterRichTextArea", "letterRichTextArea", "commentLetterRichTextArea",
                "letterRecommendation", "rescanResume", "checkSkillFromJD", "skillTreesTable",
                "cvVbox", "candidateCVHBox", "letterHBox", "letterVbox", "skillBox",
                "someFilesTable", "datePostField", "editActions"
        );

        for (String id : protectedIds) {
            assertTrue("Отсутствует защищённый component ID: " + id,
                    xml.contains("id=\"" + id + "\""));
        }

        int candidate = xml.indexOf("<tab id=\"tabCandidate\"");
        int cv = xml.indexOf("<tab id=\"tabCV\"");
        int letter = xml.indexOf("<tab id=\"tabLetter\"");
        int skills = xml.indexOf("<tab id=\"tabSkillTree\"");
        int files = xml.indexOf("<tab id=\"tabFiles\"");

        assertTrue(candidate >= 0 && candidate < cv);
        assertTrue(cv < letter);
        assertTrue(letter < skills);
        assertTrue(skills < files);

        assertTrue(xml.contains("caption=\"msg://msgCandidate\""));
        assertTrue(xml.contains("caption=\"msg://msgCV\""));
        assertTrue(xml.contains("caption=\"msg://msgLetter\""));
        assertTrue(xml.contains("caption=\"msg://msgCVSkillTree\""));
        assertTrue(xml.contains("caption=\"mainMsg://msgFiles\""));
        assertTrue(xml.contains("<dialogMode height=\"800\""));
        assertTrue(xml.contains("width=\"1200\"/>"));

        String controller = readProjectFile(CONTROLLER);
        assertTrue(controller.contains("\"tabCV\".equals(selectedTab.getName())"));
        assertTrue(controller.contains("\"tabSkillTree\".equals(selectedTab.getName())"));
        assertTrue(controller.contains("private boolean cvTextInitialized;"));
        assertTrue(controller.contains("private boolean skillTabInitialized;"));
    }

    @Test
    public void sidebarUsesLiveCandidateCvContainerBindings() throws IOException {
        String xml = readProjectFile(SCREEN_XML);

        /*
         * Sidebar не хранит копии строк и не требует listener в контроллере:
         * все значения читаются из того же InstanceContainer, что и picker-поля.
         * Поэтому изменение candidate, resumePosition или toVacancy немедленно
         * обновляет связанные Label средствами стандартного CUBA data binding.
         */
        assertTrue(xml.indexOf("id=\"candidateCvSidebar\"")
                < xml.indexOf("<tabSheet id=\"tabSheet\""));
        assertTrue(xml.contains("expand=\"candidateCvWorkspace\""));
        assertTrue(xml.contains("expand=\"candidateCvSidebarSpacer\""));

        assertComponentBinding(xml, "iteractionListLabelCandidate", "candidate.fullName");
        assertComponentBinding(xml, "iteractionListLabelPosition", "candidate.personPosition");
        assertComponentBinding(xml, "candidateCvSidebarResumePosition", "resumePosition");
        assertComponentBinding(xml, "candidateCvSidebarVacancy", "toVacancy");
        assertComponentBinding(xml, "candidateCvSidebarProject", "toVacancy.projectName");

        assertTrue(xml.contains("stylename=\"candidate-cv-sidebar\""));
        assertTrue(xml.contains("stylename=\"candidate-cv-workspace-shell\""));
        assertFalse(xml.contains("stylename=\"candidate-cv-context-card\""));
    }


    @Test
    public void sidebarContainsStaticNavigationAndMandatoryEditFormOrder() throws IOException {
        String xml = readProjectFile(SCREEN_XML);
        String controller = readProjectFile(CONTROLLER);

        int visualImage = xml.indexOf("id=\"candidatePic\"");
        int entityName = xml.indexOf("id=\"iteractionListLabelCandidate\"");
        int navigation = xml.indexOf("id=\"candidateCvSectionNavigation\"");
        int entityDetails = xml.indexOf("id=\"candidateCvSidebarTargetCard\"");
        int optionalContent = xml.indexOf("id=\"candidateCvSidebarMetaCard\"");

        assertTrue("Sidebar должен содержать визуальный образ сущности", visualImage >= 0);
        assertTrue("Sidebar должен содержать наименование сущности", entityName > visualImage);
        assertTrue("Label-навигация должна идти после наименования", navigation > entityName);
        assertTrue("Детализация должна идти после label-навигации", entityDetails > navigation);
        assertTrue("Прочие элементы должны идти после детализации", optionalContent > entityDetails);

        List<String> staticButtons = Arrays.asList(
                "candidateCvMainDataNav", "candidateCvOriginalCvNav", "candidateCvHuntTechCvNav",
                "candidateCvTextNav", "candidateCvRecommendationNav", "candidateCvLetterTemplateNav",
                "candidateCvLetterBodyNav", "candidateCvLetterCommentNav",
                "candidateCvLetterRecommendationNav", "candidateCvSkillActionsNav",
                "candidateCvSkillTreeNav", "candidateCvFilesTableNav");
        for (String buttonId : staticButtons) {
            assertTrue("Навигационная кнопка должна быть объявлена в XML: " + buttonId,
                    xml.contains("<button id=\"" + buttonId + "\""));
        }

        for (String invoke : Arrays.asList(
                "navigateCandidateMainData", "navigateCandidateOriginalCv", "navigateCandidateHuntTechCv",
                "navigateCvText", "navigateCvRecommendations", "navigateLetterTemplate",
                "navigateLetterBody", "navigateLetterComment", "navigateLetterRecommendations",
                "navigateSkillActions", "navigateSkillTree", "navigateFilesTable")) {
            assertTrue("Отсутствует invoke label-навигации: " + invoke,
                    xml.contains("invoke=\"" + invoke + "\""));
            assertTrue("Отсутствует Java handler label-навигации: " + invoke,
                    controller.contains("public void " + invoke + "()"));
        }

        assertTrue(controller.contains("syncSidebarSectionNavigation();"));
        assertTrue(controller.contains("candidateCvCandidateNavigation.setVisible(\"tabCandidate\".equals(selectedTabName))"));
        assertTrue(controller.contains("candidateCvCvNavigation.setVisible(\"tabCV\".equals(selectedTabName))"));
        assertTrue(controller.contains("candidateCvLetterNavigation.setVisible(\"tabLetter\".equals(selectedTabName))"));
        assertTrue(controller.contains("candidateCvSkillNavigation.setVisible(\"tabSkillTree\".equals(selectedTabName))"));
        assertTrue(controller.contains("candidateCvFilesNavigation.setVisible(\"tabFiles\".equals(selectedTabName))"));
        assertFalse("Навигация не должна динамически создавать кнопки",
                controller.contains("uiComponents.create(Button.class)"));
        assertFalse("Навигация не должна удалять XML-компоненты",
                controller.contains("replaceNavigationLabels"));
        assertFalse("Навигация не должна обходить lazy lifecycle вкладок",
                controller.contains("tabSheet.setSelectedTab"));
    }

    @Test
    public void designConceptDefinesMandatoryEditSidebarOrder() throws IOException {
        String concept = readProjectFile(DESIGN_CONCEPT);
        assertTrue(concept.contains("Для каждой Edit-формы HRM HuntTech левая контекстная панель является обязательной"));
        int visual = concept.indexOf("1. **Визуальный образ экземпляра**");
        int name = concept.indexOf("2. **Наименование экземпляра**");
        int navigation = concept.indexOf("3. **Label-навигация**");
        int details = concept.indexOf("4. **Детализация основных элементов**");
        int optional = concept.indexOf("5. **Прочее по необходимости**");
        assertTrue(visual >= 0 && visual < name);
        assertTrue(name < navigation);
        assertTrue(navigation < details);
        assertTrue(details < optional);
    }

    @Test
    public void bindingsActionsInvokesAndUploadsRemainUnchanged() throws IOException {
        String xml = readProjectFile(SCREEN_XML);

        assertTrue(xml.contains("dataContainer=\"candidateCVDc\""));
        assertTrue(xml.contains("property=\"candidate\""));
        assertTrue(xml.contains("property=\"resumePosition\""));
        assertTrue(xml.contains("property=\"toVacancy\""));
        assertTrue(xml.contains("property=\"owner\""));
        assertTrue(xml.contains("property=\"linkOriginalCv\""));
        assertTrue(xml.contains("property=\"originalFileCV\""));
        assertTrue(xml.contains("property=\"linkHuntTechCV\""));
        assertTrue(xml.contains("property=\"fileCV\""));
        assertTrue(xml.contains("property=\"fileImageFace\""));
        assertTrue(xml.contains("property=\"letter\""));
        assertTrue(xml.contains("property=\"commentLetter\""));
        assertTrue(xml.contains("property=\"datePost\""));

        assertTrue(xml.contains("optionsContainer=\"resumePositionsDc\""));
        assertTrue(xml.contains("optionsContainer=\"openPositionsDc\""));
        assertTrue(xml.contains("optionsContainer=\"usersDc\""));
        assertTrue(xml.contains("<action id=\"lookup\" type=\"picker_lookup\"/>"));
        assertTrue(xml.contains("<action id=\"open\" type=\"picker_open\"/>"));

        for (String invoke : Arrays.asList(
                "loadToCVTextArea", "rescanCV", "resumeRecognition",
                "convertToText", "showOriginalText", "checkSkillFromJD")) {
            assertTrue("Отсутствует invoke-контракт: " + invoke,
                    xml.contains("invoke=\"" + invoke + "\""));
        }

        assertTrue(xml.contains("dropZone=\"dropZone\""));
        assertTrue(xml.contains("fileStoragePutMode=\"IMMEDIATE\""));
        assertTrue(xml.contains("showClearButton=\"true\""));
        assertTrue(xml.contains("dataContainer=\"someFilesesDc\""));
        assertTrue(xml.contains("<action id=\"add\" type=\"add\"/>"));
        assertTrue(xml.contains("<action type=\"create\" id=\"create\"/>"));
        assertTrue(xml.contains("<action id=\"edit\" type=\"edit\"/>"));
        assertTrue(xml.contains("<action id=\"remove\" type=\"remove\"/>"));
        assertTrue(xml.contains("action=\"someFilesTable.create\""));
        assertTrue(xml.contains("action=\"someFilesTable.edit\""));
        assertTrue(xml.contains("action=\"someFilesTable.remove\""));

        assertTrue(xml.contains("select e from hunttech_OpenPosition e order by e.openClose, e.vacansyName"));
        assertTrue(xml.contains("where k.reacrutier = :subscriber"));
        assertTrue(xml.contains("where e.positionRuName not like '%(не использовать)%'"));
        assertTrue(xml.contains("select e from sec$User e order by e.name"));
    }

    @Test
    public void candidatePhotoUsesSingleOvaFallbackImageWithoutManualVisibilitySwitching() throws IOException {
        String xml = readProjectFile(SCREEN_XML);
        String controller = readProjectFile(CONTROLLER);

        /*
         * OvaFallbackImage наследует базовый CUBA Image, поэтому legacy-инъекция
         * candidatePic и программная установка source после выбора PDF-изображения
         * сохраняются, а fallback полностью обслуживает единый компонент.
         */
        assertTrue(xml.contains("<ovaFallbackImage id=\"candidatePic\""));
        assertFalse(xml.contains("<image id=\"candidatePic\""));
        assertFalse(xml.contains("candidateFaceDefaultImage"));
        assertTrue(xml.contains("dataContainer=\"candidateCVDc\""));
        assertTrue(xml.contains("property=\"fileImageFace\""));
        assertTrue(xml.contains("ovalWidth=\"176px\""));
        assertTrue(xml.contains("ovalHeight=\"176px\""));
        assertTrue(xml.contains("scaleMode=\"SCALE_DOWN\""));
        assertTrue(xml.contains("fallbackThemePath=\"icons/no-programmer.jpeg\""));

        assertTrue(controller.contains("private Image candidatePic;"));
        assertTrue(Pattern.compile("candidatePic\\s*\\.setSource\\(FileDescriptorResource\\.class\\)")
                .matcher(controller)
                .find());
        assertFalse(controller.contains("candidateFaceDefaultImage"));
        assertFalse(controller.contains("setCandidatePicImage"));
        assertFalse(controller.contains("@Subscribe(\"candidatePic\")"));
        assertFalse(controller.contains("candidatePic.setVisible"));
    }

    @Test
    public void localScssIsConnectedToAllSupportedThemes() throws IOException {
        String xml = readProjectFile(SCREEN_XML);
        assertTrue(xml.contains("stylename=\"candidate-cv-editor\""));
        assertTrue(xml.contains("id=\"candidateCvSidebar\""));
        assertTrue(xml.contains("width=\"312px\""));

        Pattern forbiddenTopLevelSelector = Pattern.compile(
                "(?m)^\\.(v-button|v-label|v-table|v-grid|v-tabsheet|v-textfield|v-richtextarea)\\b");

        for (String theme : THEMES) {
            String scssPath = "modules/web/themes/" + theme
                    + "/com.company.hunttech/candidate-cv-editor.scss";
            String stylesPath = "modules/web/themes/" + theme + "/styles.scss";
            String scss = readProjectFile(scssPath);
            String styles = readProjectFile(stylesPath);

            assertTrue(theme, scss.contains("@mixin candidate-cv-editor-theme"));
            assertTrue(theme, scss.contains(".candidate-cv-editor {"));
            assertTrue(theme, scss.contains(".v-slot-candidate-cv-sidebar"));
            assertTrue(theme, scss.contains("width: 312px !important"));
            assertTrue(theme, scss.contains(".candidate-cv-navigation {"));
            assertTrue(theme, scss.contains(".candidate-cv-nav-item.v-button"));
            if (theme.equals("helium") || theme.equals("hunttech-modern-light")
                    || theme.equals("hunttech-modern-dark")) {
                // Дизайн Antigravity (лаконичная навигация) — приоритет, не трогаем.
                assertTrue(theme, scss.contains("min-height: 26px !important"));
                assertTrue(theme, scss.contains("padding: 3px 8px !important"));
                assertTrue(theme, scss.contains("line-height: 16px !important"));
            } else {
                // Канон JobCandidateEdit: отступы и интервалы label-навигации 1:1.
                assertTrue(theme, scss.contains("min-height: 27px !important"));
                assertTrue(theme, scss.contains("padding: 3px 10px !important"));
                assertTrue(theme, scss.contains("line-height: 20px !important"));
                assertTrue(theme, scss.contains("padding-top: 6px"));
                assertTrue(theme, scss.contains("padding-bottom: 2px"));
            }
            assertTrue(theme, scss.contains(".candidate-cv-nav-item-active.v-button"));
            assertTrue(theme, scss.contains(".candidate-cv-tabs > .v-tabsheet-tabcontainer"));
            assertTrue(theme, scss.contains("overflow-x: auto !important"));
            assertTrue(theme, scss.contains("text-overflow: clip !important"));
            assertTrue(theme, scss.contains(".candidate-cv-photo-dropzone .v-upload .v-button"));
            assertTrue(theme, scss.contains(".candidate-cv-main-card .v-filterselect-input"));
            assertTrue(theme, scss.contains(".candidate-cv-main-card .c-pickerfield-layout"));
            assertFalse(theme, scss.contains(".job-candidate-editor"));
            assertFalse(theme, scss.contains(".ext-settings-window"));
            assertFalse(theme, forbiddenTopLevelSelector.matcher(scss).find());

            assertTrue(theme, styles.contains(
                    "@import \"com.company.hunttech/candidate-cv-editor\";"));
            assertTrue(theme, styles.contains("@include candidate-cv-editor-theme;"));
        }
    }

    @Test
    public void sectionTitlesHaveTwoInsetLinesLikeInfoCaption() throws IOException {
        String xml = readProjectFile(SCREEN_XML);

        // Оба заголовка разделов sidebar несут свои локальные классы полосы.
        assertTrue(xml.contains("stylename=\"candidate-cv-navigation-title\""));
        assertTrue(xml.contains("stylename=\"candidate-cv-sidebar-card-title\""));

        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/candidate-cv-editor.scss");
        String navigationBlock = section(scss,
                ".candidate-cv-navigation-title,",
                ".candidate-cv-tab-navigation {");
        String cardTitleBlock = section(scss,
                ".candidate-cv-sidebar-card-title,",
                ".candidate-cv-sidebar-info-row {");

        // Полоса заголовка навигации повторяет caption инфокарточки (контракт §4.1):
        // две горизонтальные inset-линии (белая сверху, светлая снизу) + разделитель снизу.
        assertTrue(navigationBlock.contains("min-height: 36px !important;"));
        assertTrue(navigationBlock.contains("padding: 7px 11px !important;"));
        assertTrue(navigationBlock.contains("color: #ffb11b !important;"));
        assertTrue(navigationBlock.contains("font-size: 15px !important;"));
        assertTrue(navigationBlock.contains("font-weight: 700 !important;"));
        assertTrue(navigationBlock.contains("line-height: 21px !important;"));
        assertTrue(navigationBlock.contains("text-transform: none !important;"));
        assertTrue(navigationBlock.contains("background: rgba(255, 255, 255, 0.045) !important;"));
        assertTrue(navigationBlock.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14) !important;"));
        assertTrue(navigationBlock.contains("box-shadow: rgba(255, 255, 255, 1) 0 1px 0 0 inset,"));
        assertTrue(navigationBlock.contains("rgba(244, 244, 244, 1) 0 -1px 0 0 inset;"));

        // Полоса заголовка «Резюме для вакансии» растягивается на карточку (padding 14px)
        // и несёт те же две inset-линии.
        assertTrue(cardTitleBlock.contains("min-height: 36px !important;"));
        assertTrue(cardTitleBlock.contains("padding: 7px 11px !important;"));
        assertTrue(cardTitleBlock.contains("margin: -14px -14px 12px !important;"));
        assertTrue(cardTitleBlock.contains("border-radius: 8px 8px 0 0 !important;"));
        assertTrue(cardTitleBlock.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14) !important;"));
        assertTrue(cardTitleBlock.contains("box-shadow: rgba(255, 255, 255, 1) 0 1px 0 0 inset,"));
        assertTrue(cardTitleBlock.contains("rgba(244, 244, 244, 1) 0 -1px 0 0 inset;"));

        // SCSS обязан оставаться идентичным во всех 7 темах.
        for (String theme : THEMES) {
            String themeScss = readProjectFile(
                    "modules/web/themes/" + theme + "/com.company.hunttech/candidate-cv-editor.scss");
            assertEquals("candidate-cv-editor.scss должен быть идентичен: " + theme, scss, themeScss);
        }
    }

    private String section(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        assertTrue("Не найден начальный маркер: " + startMarker, start >= 0);
        int end = text.indexOf(endMarker, start);
        assertTrue("Не найден конечный маркер: " + endMarker, end > start);
        return text.substring(start, end);
    }

    private void assertComponentBinding(String xml, String componentId, String property) {
        int idPosition = xml.indexOf("id=\"" + componentId + "\"");
        assertTrue("Не найден компонент sidebar: " + componentId, idPosition >= 0);
        int tagStart = xml.lastIndexOf('<', idPosition);
        int tagEnd = xml.indexOf('>', idPosition);
        assertTrue("Не найден XML-тег компонента: " + componentId,
                tagStart >= 0 && tagEnd > idPosition);
        String componentTag = xml.substring(tagStart, tagEnd + 1);
        assertTrue(componentId + " должен быть привязан к candidateCVDc",
                componentTag.contains("dataContainer=\"candidateCVDc\""));
        assertTrue(componentId + " должен читать property=" + property,
                componentTag.contains("property=\"" + property + "\""));
    }

    private String gitBlobSha1(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(("blob " + content.length + "\0").getBytes(StandardCharsets.UTF_8));
        return toHex(digest.digest(content));
    }

    private String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(readProjectBytes(relativePath), StandardCharsets.UTF_8);
    }

    private byte[] readProjectBytes(String relativePath) throws IOException {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта HRM HuntTech", root);
        return Files.readAllBytes(root.resolve(relativePath));
    }
}
