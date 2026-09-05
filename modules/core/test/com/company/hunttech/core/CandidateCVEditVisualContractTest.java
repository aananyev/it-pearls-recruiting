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
                "cvActionsPopupButton",
                "candidateCVRichTextArea",
                "questionLetterRichTextArea", "letterRichTextArea", "commentLetterRichTextArea",
                "letterRecommendation", "skillActionsPopupButton", "skillTreesTable",
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

        // Тематические пиктограммы вкладок (FontAwesome) — единый визуальный язык
        // с иконками пунктов label-навигации sidebar. Иконка ищется в диапазоне
        // между соседними вкладками, чтобы не спутать её с иконками навигации.
        int tabCandidateIcon = xml.indexOf("icon=\"font-icon:USER\"", xml.indexOf("id=\"tabCandidate\""));
        int tabCvIcon = xml.indexOf("icon=\"font-icon:FILE_TEXT_O\"", xml.indexOf("id=\"tabCV\""));
        int tabLetterIcon = xml.indexOf("icon=\"font-icon:SEND\"", xml.indexOf("id=\"tabLetter\""));
        int tabSkillIcon = xml.indexOf("icon=\"font-icon:SITEMAP\"", xml.indexOf("id=\"tabSkillTree\""));
        int tabFilesIcon = xml.indexOf("icon=\"font-icon:FOLDER_O\"", xml.indexOf("id=\"tabFiles\""));
        assertTrue("Вкладка «Кандидат» должна нести пиктограмму USER",
                tabCandidateIcon >= 0 && tabCandidateIcon < xml.indexOf("id=\"tabCV\""));
        assertTrue("Вкладка «Резюме» должна нести пиктограмму FILE_TEXT_O",
                tabCvIcon >= 0 && tabCvIcon < xml.indexOf("id=\"tabLetter\""));
        assertTrue("Вкладка «Письмо» должна нести пиктограмму SEND",
                tabLetterIcon >= 0 && tabLetterIcon < xml.indexOf("id=\"tabSkillTree\""));
        assertTrue("Вкладка «Навыки» должна нести пиктограмму SITEMAP",
                tabSkillIcon >= 0 && tabSkillIcon < xml.indexOf("id=\"tabFiles\""));
        assertTrue("Вкладка «Файлы» должна нести пиктограмму FOLDER_O", tabFilesIcon >= 0);
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

        assertTrue(xml.contains("stylename=\"edit-sidebar candidate-cv-sidebar\""));
        assertTrue(xml.contains("stylename=\"candidate-cv-workspace-shell\""));
        assertFalse(xml.contains("stylename=\"candidate-cv-context-card\""));
    }


    @Test
    public void sidebarContainsStaticNavigationAndMandatoryEditFormOrder() throws IOException {
        String xml = readProjectFile(SCREEN_XML);
        String controller = readProjectFile(CONTROLLER);

        int visualImage = xml.indexOf("id=\"candidatePic\"");
        int entityName = xml.indexOf("id=\"iteractionListLabelCandidate\"");
        int entityDetails = xml.indexOf("id=\"candidateCvSidebarTargetCard\"");
        int navigation = xml.indexOf("id=\"candidateCvSectionNavigation\"");
        int optionalContent = xml.indexOf("id=\"candidateCvSidebarMetaCard\"");

        assertTrue("Sidebar должен содержать визуальный образ сущности", visualImage >= 0);
        assertTrue("Sidebar должен содержать наименование сущности", entityName > visualImage);
        // Блок «Резюме для вакансии» (детализация) размещён НА УРОВЕНЬ ВЫШЕ label-навигации —
        // утверждённый дизайн (комментарий в XML: «блок без рамки на уровень выше навигации»).
        assertTrue("Детализация должна идти после наименования", entityDetails > entityName);
        assertTrue("Label-навигация должна идти после детализации", navigation > entityDetails);
        assertTrue("Прочие элементы должны идти после навигации", optionalContent > navigation);

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

        // Тематические пиктограммы навигации (FontAwesome): каждая кнопка sidebar
        // несёт свою иконку по смыслу раздела.
        assertTrue(xml.contains("id=\"candidateCvMainDataNav\" caption=\"msg://candidateCvNavMainData\" icon=\"font-icon:USER\""));
        assertTrue(xml.contains("id=\"candidateCvOriginalCvNav\" caption=\"msg://candidateCvNavOriginalCv\" icon=\"font-icon:FILE_TEXT_O\""));
        assertTrue(xml.contains("id=\"candidateCvHuntTechCvNav\" caption=\"msg://candidateCvNavHuntTechCv\" icon=\"font-icon:FILE_WORD_O\""));
        assertTrue(xml.contains("id=\"candidateCvTextNav\" caption=\"msg://candidateCvNavCvText\" icon=\"font-icon:ALIGN_LEFT\""));
        assertTrue(xml.contains("id=\"candidateCvSkillActionsNav\" caption=\"msg://candidateCvNavSkillActions\" icon=\"font-icon:MAGIC\""));
        assertTrue(xml.contains("id=\"candidateCvSkillTreeNav\" caption=\"msg://candidateCvNavSkillTree\" icon=\"font-icon:SITEMAP\""));
        assertTrue(xml.contains("id=\"candidateCvFilesTableNav\" caption=\"msg://candidateCvNavFiles\" icon=\"font-icon:FOLDER_O\""));
        assertTrue(xml.contains("icon=\"font-icon:REPEAT\""));
        assertTrue(xml.contains("icon=\"font-icon:MAGIC\""));

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

        assertTrue("Отсутствует invoke-контракт: loadToCVTextArea",
                xml.contains("invoke=\"loadToCVTextArea\""));

        // Действия toolbar «Действия» (popupButton) заменили прежние отдельные invoke-кнопки
        // «Распознавание»/«Преобразовать»/«Исходное»: контракт сохраняется через action id.
        for (String action : Arrays.asList(
                "scanSkillsAction", "smartFormatCvAction", "rescanCvAction",
                "resumeRecognitionAction", "showOriginalAction")) {
            assertTrue("Отсутствует action popupButton: " + action,
                    xml.contains("<action id=\"" + action + "\""));
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
    public void resumeRecognitionSupportsPdfDocDocxAndRtf() throws IOException {
        String controller = readProjectFile(CONTROLLER);
        String gradle = readProjectFile("build.gradle");

        // Распознавание резюме из загруженного файла: PDF (существующий parser),
        // DOC/DOCX (Apache POI) и RTF (встроенный в JDK RTFEditorKit) — текст каждого
        // формата попадает в RichTextArea вкладки «Резюме» (общий код конца обработчика).
        assertTrue(controller.contains("private static final String EXTENSION_PDF = \"pdf\";"));
        assertTrue(controller.contains("private static final String EXTENSION_DOC = \"doc\";"));
        assertTrue(controller.contains("private static final String EXTENSION_DOCX = \"docx\";"));
        assertTrue(controller.contains("private static final String EXTENSION_RTF = \"rtf\";"));

        // Word 97-2003 (.doc): HWPF-экстрактор Apache POI (poi-scratchpad).
        assertTrue(controller.contains("import org.apache.poi.hwpf.extractor.WordExtractor;"));
        assertTrue(controller.contains("new WordExtractor(docInputStream)"));
        assertTrue(gradle.contains("poi-scratchpad:4.1.2"));
        // Rich Text Format (.rtf): встроенный в JDK RTFEditorKit (plain text без разметки).
        assertTrue(controller.contains("import javax.swing.text.rtf.RTFEditorKit;"));
        assertTrue(controller.contains("new RTFEditorKit()"));
        assertTrue(controller.contains("javax.swing.text.BadLocationException"));
        // Заглушка «функция загрузки .doc пока не реализована» заменена реальным распознаванием.
        assertFalse(controller.contains("пока не реализована"));
        // Распознанный текст любого формата пишется в lazy-managed RichTextArea вкладки «Резюме».
        assertTrue(controller.contains("candidateCVRichTextArea.setValue(textResume"));
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
        assertTrue(xml.contains("width=\"320px\""));

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
                // Текст внутри всех кнопок экрана выровнен по центру (nav-пункты sidebar,
                // toolbar, footer); иконки выровнены по вертикали с отступом от текста.
                assertTrue(theme, scss.contains("text-align: center !important"));
                assertTrue(theme, scss.contains(".candidate-cv-nav-item.v-button .v-icon"));
                assertTrue(theme, scss.contains("margin-right: 6px"));
            }
            assertTrue(theme, scss.contains(".candidate-cv-nav-item-active.v-button"));
            assertTrue(theme, scss.contains(".candidate-cv-tabs > .v-tabsheet-tabcontainer"));
            assertTrue(theme, scss.contains("overflow-x: auto !important"));
            assertTrue(theme, scss.contains("text-overflow: clip !important"));
            // Тематические пиктограммы вкладок и кнопок «Загрузить»/«Очистить» (FontAwesome)
            // присутствуют во всех темах: иконка caption, центрирование подписи кнопок,
            // fa-upload (\f093) и fa-trash-o (\f1f8).
            assertTrue(theme, scss.contains(".candidate-cv-tabs .v-caption .v-icon"));
            assertTrue(theme, scss.contains(".candidate-cv-document-card .c-fileupload .v-button,"));
            assertTrue(theme, scss.contains(".candidate-cv-document-card .c-fileupload-clear"));
            assertTrue(theme, scss.contains("justify-content: center !important"));
            assertTrue(theme, scss.contains("content: '\\f093'"));
            assertTrue(theme, scss.contains("content: '\\f1f8'"));
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

        // Заголовки разделов sidebar несут свои локальные классы полосы
        // (в составе общего stylename после базовых классов).
        assertTrue(xml.contains("candidate-cv-navigation-title"));
        assertTrue(xml.contains("candidate-cv-sidebar-card-title"));
        assertTrue(xml.contains("stylename=\"label-nav-title job-candidate-section-title candidate-cv-skills-title\""));

        String scss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/candidate-cv-editor.scss");
        String navigationBlock = section(scss,
                ".candidate-cv-navigation-title,",
                ".candidate-cv-tab-navigation {");
        String cardTitleBlock = section(scss,
                ".candidate-cv-sidebar-card-title,",
                ".candidate-cv-sidebar-info-row {");

        // Полоса заголовков навигации и «Основные навыки» повторяет caption инфокарточки
        // (контракт §4.1): две горизонтальные inset-линии (белая сверху, светлая снизу)
        // + разделитель снизу; текст — ЗАГЛАВНЫМИ.
        assertTrue(navigationBlock.contains(".candidate-cv-skills-title,"));
        assertTrue(navigationBlock.contains("min-height: 36px !important;"));
        assertTrue(navigationBlock.contains("padding: 7px 11px !important;"));
        assertTrue(navigationBlock.contains("color: #ffb11b !important;"));
        assertTrue(navigationBlock.contains("font-size: 15px !important;"));
        assertTrue(navigationBlock.contains("font-weight: 700 !important;"));
        assertTrue(navigationBlock.contains("line-height: 21px !important;"));
        assertTrue(navigationBlock.contains("text-transform: uppercase !important;"));
        assertTrue(navigationBlock.contains("background: rgba(255, 255, 255, 0.045) !important;"));
        assertTrue(navigationBlock.contains("border-bottom: 1px solid rgba(255, 255, 255, 0.14) !important;"));
        assertTrue(navigationBlock.contains("box-shadow: rgba(255, 255, 255, 1) 0 1px 0 0 inset,"));
        assertTrue(navigationBlock.contains("rgba(244, 244, 244, 1) 0 -1px 0 0 inset;"));

        // Заголовок «Резюме для вакансии» — лаконичный заголовок раздела без полосы-карточки
        // (по образцу дизайна Antigravity: блок без рамки, заголовок 28px/14px/700), текст — ЗАГЛАВНЫМИ.
        assertTrue(cardTitleBlock.contains("min-height: 28px !important;"));
        assertTrue(cardTitleBlock.contains("padding: 2px 0 6px 0 !important;"));
        assertTrue(cardTitleBlock.contains("margin: 0 0 8px 0 !important;"));
        assertTrue(cardTitleBlock.contains("font-size: 14px !important;"));
        assertTrue(cardTitleBlock.contains("font-weight: 700 !important;"));
        assertTrue(cardTitleBlock.contains("text-transform: uppercase !important;"));
        assertTrue(cardTitleBlock.contains("background: transparent !important;"));
        assertTrue(cardTitleBlock.contains("border: 0 !important;"));
        assertTrue(cardTitleBlock.contains("box-shadow: none !important;"));

        // SCSS обязан оставаться идентичным в пределах группы тем: каноническая группа
        // (halo, havana, hover, hunttech-modern) — 1:1; Antigravity-темы (helium,
        // hunttech-modern-light, hunttech-modern-dark) имеют собственную лаконичную
        // навигацию (min-height 26px), поэтому сравниваются внутри своих подгрупп.
        String canonicalScss = readProjectFile(
                "modules/web/themes/halo/com.company.hunttech/candidate-cv-editor.scss");
        for (String theme : Arrays.asList("havana", "hover", "hunttech-modern")) {
            assertEquals("Канонический candidate-cv-editor.scss должен быть идентичен: " + theme,
                    canonicalScss,
                    readProjectFile("modules/web/themes/" + theme + "/com.company.hunttech/candidate-cv-editor.scss"));
        }
        String lightAntigravityScss = readProjectFile(
                "modules/web/themes/hunttech-modern-light/com.company.hunttech/candidate-cv-editor.scss");
        assertEquals("hunttech-modern-dark должен повторять hunttech-modern-light",
                lightAntigravityScss,
                readProjectFile("modules/web/themes/hunttech-modern-dark/com.company.hunttech/candidate-cv-editor.scss"));
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
