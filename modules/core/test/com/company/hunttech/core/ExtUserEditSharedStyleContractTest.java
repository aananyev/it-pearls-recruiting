package com.company.hunttech.core;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает визуальную миграцию ExtUserEdit на общий edit-* / label-* API
 * без изменения legacy CUBA-контрактов пользователя, ролей и замещений.
 */
public class ExtUserEditSharedStyleContractTest {

    private static final String DESCRIPTOR =
            "modules/web/src/com/company/hunttech/web/screens/extuser/ext-user-edit.xml";
    private static final String CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/extuser/ExtUserEditor.java";

    @Test
    public void descriptorUsesSharedEditScreenRoles() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        List<String> requiredStyles = Arrays.asList(
                "ext-user-editor",
                "edit-screen-layout",
                "edit-sidebar",
                "edit-sidebar-visual",
                "edit-sidebar-identity",
                "edit-sidebar-title",
                "edit-sidebar-subtitle",
                "edit-sidebar-summary",
                "ext-user-editor-profile-title",
                "ext-user-editor-profile-status",
                "ext-user-editor-profile-caption",
                "ext-user-editor-profile-value",
                "edit-sidebar-spacer",
                "label-navigation",
                "label-nav-title",
                "label-nav-item",
                "label-nav-item-active",
                "edit-workspace",
                "edit-toolbar",
                "edit-toolbar-title",
                "edit-toolbar-description",
                "edit-toolbar-actions",
                "edit-tabs",
                "edit-card",
                "edit-card-title",
                "edit-help",
                "edit-form-control",
                "edit-accordion-section",
                "edit-footer-actions"
        );

        for (String style : requiredStyles) {
            assertTrue("Не найден общий stylename: " + style,
                    descriptor.contains(style));
        }

        assertTrue(descriptor.contains("id=\"profilePanel\""));
        assertTrue(descriptor.contains("width=\"270px\""));
        assertTrue(descriptor.contains("id=\"tabNavigation\""));
        assertTrue(descriptor.contains("id=\"generalTabNav\""));
        assertTrue(descriptor.contains("id=\"emailTabNav\""));
        assertTrue(descriptor.contains("id=\"aiTabNav\""));
        assertTrue(descriptor.contains("<ovaFallbackImage"));
    }

    @Test
    public void navigationTitleUsesSectionStripAndCardsRenderAsPanels() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        // Полоса-заголовок «Разделы» — класс секции поверх label-nav-title в едином наборе навигации.
        int titleCount = countOccurrences(descriptor,
                "stylename=\"label-nav-title ext-user-editor-navigation-title\"");
        assertEquals(1, titleCount);

        // Полоса-заголовок «Профиль» — тот же паттерн (label-nav-title + локальный класс секции),
        // 1:1 с заголовком «Разделы» (контракт §4.1).
        int profileTitleCount = countOccurrences(descriptor,
                "stylename=\"label-nav-title ext-user-editor-profile-title\"");
        assertEquals(1, profileTitleCount);

        // groupBox-карточки обязаны рендериться как v-panel (showAsPanel) — контракт §5.2.
        assertCardIsPanel(descriptor, "contactsCard");
        assertCardIsPanel(descriptor, "regionalCard");
    }

    @Test
    public void localScssDefinesSectionStripFieldCaptionsAndAvatar() throws IOException {
        String scss = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/ext-user-editor.scss");

        // Полоса-заголовок контракта §4.1: две inset-линии + жёлтый текст 15px/700.
        assertTrue(scss.contains(".ext-user-editor-navigation-title"));
        assertTrue(scss.contains("min-height: 36px !important"));
        assertTrue(scss.contains("color: #ffb11b !important"));
        assertTrue(scss.contains("font-size: 15px !important"));
        assertTrue(scss.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));

        // Подписи полей — контрактные 13px/600 по эталону IteractionListEdit.
        assertTrue(scss.contains(".v-caption .v-captiontext"));
        assertTrue(scss.contains("font-size: 13px !important"));
        assertTrue(scss.contains("font-weight: 600 !important"));

        // Hover пунктов — эталон IteractionListEdit: белый текст на rgba(255,255,255,.08).
        assertTrue(scss.contains("color: #ffffff !important"));
        assertTrue(scss.contains("background: rgba(255, 255, 255, 0.08) !important"));
        assertTrue(scss.contains("border-top-color: rgba(255, 255, 255, 0.16)"));

        // Круглый аватар OvaFallbackImage 180×180 с fallback-обрезкой.
        assertTrue(scss.contains(".ext-user-editor-avatar"));
        assertTrue(scss.contains("border-radius: 50% !important"));
        assertTrue(scss.contains("object-view-box: inset(8%)"));
    }

    private void assertCardIsPanel(String descriptor, String id) {
        int start = descriptor.indexOf("id=\"" + id + "\"");
        assertTrue("Не найдена карточка " + id, start >= 0);
        String fragment = descriptor.substring(start, Math.min(descriptor.length(), start + 300));
        assertTrue(fragment.contains("showAsPanel=\"true\""));
        assertTrue(fragment.contains("edit-card"));
    }

    private int countOccurrences(String text, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    @Test
    public void profileBlockShowsMainUserInfoFromGeneralSettings() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);
        String controller = readProjectFile(CONTROLLER);
        String scss = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/ext-user-editor.scss");
        String messagesRu = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extuser/messages_ru.properties");
        String messagesEn = readProjectFile(
                "modules/web/src/com/company/hunttech/web/screens/extuser/messages.properties");

        // Лейблы основной информации пользователя присутствуют в блоке «Профиль».
        assertTrue(descriptor.contains("id=\"statusLabel\""));
        assertTrue(descriptor.contains("id=\"emailLabel\""));
        assertTrue(descriptor.contains("id=\"positionLabel\""));
        assertTrue(descriptor.contains("msg://msgEmail"));
        assertTrue(descriptor.contains("msg://msgPosition"));

        // Полоса-заголовок «Профиль» — та же секция §4.1, что у «Разделы» (две inset-линии).
        assertTrue(scss.contains(".ext-user-editor-profile-title"));
        assertTrue(scss.contains("min-height: 36px !important"));
        assertTrue(scss.contains("color: #ffb11b !important"));
        assertTrue(scss.contains("font-size: 15px !important"));
        assertTrue(scss.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));

        // Стили статуса, подписей и значений блока «Профиль».
        assertTrue(scss.contains(".ext-user-editor-profile-status"));
        assertTrue(scss.contains(".ext-user-editor-profile-caption"));
        assertTrue(scss.contains(".ext-user-editor-profile-value"));

        // Контроллер заполняет sidebar из userDs (presentation-only, см. refreshProfileLabels).
        assertTrue(controller.contains("userDs.addItemChangeListener"));
        assertTrue(controller.contains("refreshProfileLabels"));
        assertTrue(controller.contains("fioLabel.setValue(buildFio(user))"));
        assertTrue(controller.contains("emailLabel.setValue(user.getEmail()"));
        assertTrue(controller.contains("positionLabel.setValue(user.getPosition()"));

        // Подписи совпадают с капшенами полей вкладки «Общие настройки» (User.email/User.position).
        assertTrue(messagesRu.contains("msgEmail=Email"));
        assertTrue(messagesRu.contains("msgPosition=Должность"));
        assertTrue(messagesEn.contains("msgEmail=Email"));
        assertTrue(messagesEn.contains("msgPosition=Position"));
    }

    @Test
    public void navigationSwitchesTabsAndKeepsDataUntouched() throws IOException {
        String controller = readProjectFile(CONTROLLER);

        assertTrue(controller.contains("addSelectedTabChangeListener"));
        assertTrue(controller.contains("setSelectedTab("));
        assertTrue(controller.contains("addClickListener"));
        assertTrue(controller.contains("removeStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(controller.contains("addStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(controller.contains("fieldComponent.addStyleName(\"edit-form-control\")"));

        // Sidebar-заполнение — только presentation-лейблы; entity и datasource не мутируются.
        assertTrue(controller.contains("statusLabel.setValue("));
        assertTrue(controller.contains("emailLabel.setValue("));
        assertTrue(controller.contains("positionLabel.setValue("));
        assertFalse(controller.contains("userDs.setItem("));
        assertFalse(controller.contains("getItem().set"));

        assertFalse(controller.contains("section.setExpanded(true)"));
        assertFalse(controller.contains("target.focus()"));
        assertFalse(controller.contains("dataManager"));
        assertFalse(controller.contains("userManagementService.changePassword"));
        assertFalse(controller.contains("passwordEncryption"));
        assertFalse(controller.contains("dataManager.commit"));
        assertFalse(controller.contains("rolesDs"));
        assertFalse(controller.contains("substitutionsDs"));
    }

    @Test
    public void existingBindingsActionsAndIdentifiersRemainIntact() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        List<String> protectedContracts = Arrays.asList(
                "class=\"com.company.hunttech.web.screens.extuser.ExtUserEditor\"",
                "datasource=\"userDs\"",
                "view=\"extUser-view\"",
                "property=\"userRoles\"",
                "property=\"substitutions\"",
                "select e from hunttech_UserAiConfiguration e where e.user = :ds$userDs",
                "id=\"fieldGroupLeft\"",
                "id=\"fieldGroupRight\"",
                "id=\"contactsFieldGroup\"",
                "id=\"rolesTable\"",
                "id=\"substTable\"",
                "id=\"propertiesEmailGrid\"",
                "id=\"emailFieldGroupLeft\"",
                "id=\"emailFieldGroupRight\"",
                "id=\"emailFieldPasswordRequired\"",
                "id=\"emailFieldGroupUser\"",
                "id=\"emailFieldGroupPasswords\"",
                "id=\"aiConfigsTable\"",
                "id=\"changePasswordBtn\"",
                "invoke=\"changePassword\"",
                "id=\"tabNavigation\"",
                "id=\"generalTabNav\"",
                "id=\"emailTabNav\"",
                "id=\"aiTabNav\"",
                "caption=\"msg://msgGeneralSettings\"",
                "caption=\"msg://msgEmailSettings\"",
                "caption=\"msg://msgAiSettings\"",
                "<ovaFallbackImage id=\"userPic\"",
                "datasource=\"userDs\" property=\"officialPhoto\"",
                "fallbackThemePath=\"icons/no-programmer.jpeg\"",
                "action=\"rolesTable.edit\"",
                "action=\"rolesTable.remove\"",
                "action=\"substTable.add\"",
                "action=\"substTable.edit\"",
                "action=\"substTable.remove\"",
                "screen=\"editWindowActions\""
        );

        for (String contract : protectedContracts) {
            assertTrue("Нарушен защищённый XML-контракт: " + contract,
                    descriptor.contains(contract));
        }
    }

    @Test
    public void emailSettingsUseFiveFullWidthAccordions() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        assertAccordion(descriptor, "propertiesEmailBox", "false");
        assertAccordion(descriptor, "emailPortsBox", "true");
        assertAccordion(descriptor, "emailAuthenticationBox", "true");
        assertAccordion(descriptor, "emailAccountsBox", "true");
        assertAccordion(descriptor, "emailPasswordsBox", "true");
        assertTrue(descriptor.contains("id=\"propertiesEmailGrid\""));
    }

    @Test
    public void localScssIsIdenticalAndConnectedAfterSharedApiInAllThemes() throws IOException {
        List<String> themes = Arrays.asList(
                "halo",
                "havana",
                "helium",
                "hover",
                "hunttech-modern",
                "hunttech-modern-light",
                "hunttech-modern-dark"
        );

        String referenceScss = null;
        for (String theme : themes) {
            String scss = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/ext-user-editor.scss");
            if (referenceScss == null) {
                referenceScss = scss;
            } else {
                assertEquals("Локальный ExtUserEdit SCSS различается между темами",
                        referenceScss, scss);
            }

            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            int sharedImport = styles.indexOf(
                    "@import \"com.company.hunttech/edit-screen-shared-styles\";");
            int screenImport = styles.indexOf(
                    "@import \"com.company.hunttech/ext-user-editor\";");
            int sharedInclude = styles.indexOf("@include edit-screen-shared-styles;");
            int screenInclude = styles.indexOf("@include ext-user-editor-theme;");

            assertTrue(sharedImport >= 0 && screenImport > sharedImport);
            assertTrue(sharedInclude >= 0 && screenInclude > sharedInclude);
        }

        assertNotNull(referenceScss);
        assertTrue(referenceScss.contains(".ext-user-editor"));
        assertTrue(referenceScss.contains("#172638"));
        assertTrue(referenceScss.contains("#ffb11b"));
        assertFalse(referenceScss.contains("\n  .v-button"));
        assertFalse(referenceScss.contains("\n  .v-label"));
        assertFalse(referenceScss.contains("\n  .v-table"));
    }

    @Test
    public void fallbackIconPresentInAllThemes() {
        List<String> themes = Arrays.asList(
                "halo",
                "havana",
                "helium",
                "hover",
                "hunttech-modern",
                "hunttech-modern-light",
                "hunttech-modern-dark"
        );

        for (String theme : themes) {
            assertTrue("Отсутствует fallback-иконка no-programmer.jpeg в теме " + theme,
                    Files.exists(projectRoot().resolve(
                            "modules/web/themes/" + theme + "/icons/no-programmer.jpeg")));
        }
    }

    @Test
    public void buttonsFollowIteractionListEditReference() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);
        String scss = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/ext-user-editor.scss");

        assertTrue(descriptor.contains("id=\"changePasswordBtn\""));
        assertTrue(descriptor.contains("stylename=\"ext-user-editor-primary-action\""));
        assertTrue(descriptor.contains("stylename=\"edit-footer-actions\""));
        assertTrue(descriptor.contains("screen=\"editWindowActions\""));

        assertTrue(scss.contains("min-height: 38px"));
        assertTrue(scss.contains("padding: 0 16px"));
        assertTrue(scss.contains("font-size: 14px"));
        assertTrue(scss.contains("box-shadow: none !important"));
        assertTrue(scss.contains("filter: brightness(0.98)"));
        assertTrue(scss.contains("outline: 0"));
        assertTrue(scss.contains(".ext-user-editor-primary-action"));
        assertTrue(scss.contains(".edit-footer-actions .c-primary-action"));
        assertTrue(scss.contains(".edit-footer-actions .v-button:not(.c-primary-action)"));
    }

    @Test
    public void tabsheetFollowsIteractionListEditReference() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);
        String scss = readProjectFile(
                "modules/web/themes/hover/com.company.hunttech/ext-user-editor.scss");

        // Заголовки вкладок — точная копия финального слоя эталона IteractionListEdit
        // (iteraction-list-reference-finish.scss): 48px, 15px/600, нижняя полоса-акцент.
        assertTrue(descriptor.contains("stylename=\"edit-tabs\""));
        assertFalse("framed-рендер вало ломает эталонный стиль вкладок",
                descriptor.contains("stylename=\"framed edit-tabs\""));
        assertTrue(scss.contains(".edit-tabs > .v-tabsheet-tabcontainer"));
        assertTrue(scss.contains("padding: 0 20px"));
        assertTrue(scss.contains("height: 48px"));
        assertTrue(scss.contains("font-size: 15px !important"));
        assertTrue(scss.contains("font-weight: 600 !important"));
        assertTrue(scss.contains(".v-tabsheet-tabitem:hover .v-caption"));
        assertTrue(scss.contains("color: #ffb11b !important"));
        assertTrue(scss.contains("border-bottom-color: #ffb11b !important"));
        assertTrue(scss.contains(".edit-tabs > .v-tabsheet-content"));
        assertTrue(scss.contains("height: calc(100% - 49px) !important"));
    }

    @Test
    public void footerPushesActionsToBottomRightCorner() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        // Footer-структура эталона IteractionListEdit: expand-спейсер + группа AUTO
        // прижимают ОК/Отмена к правому нижнему углу экрана.
        assertTrue(descriptor.contains("id=\"bottomActionsBox\""));
        assertTrue(descriptor.contains("expand=\"bottomActionsSpacer\""));
        assertTrue(descriptor.contains("id=\"bottomActionsSpacer\""));
        assertTrue(descriptor.contains("id=\"bottomActionsGroup\""));
        assertTrue(descriptor.contains("width=\"AUTO\""));
        assertTrue(descriptor.contains("align=\"MIDDLE_RIGHT\""));
        assertTrue(descriptor.contains("screen=\"editWindowActions\""));
    }

    @Test
    public void generalSettingsTabScrollsVertically() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        // Единый вертикальный scrollBox занимает всю вкладку (height=100%) и
        // прокручивает элементы ввода на малых разрешениях экрана.
        assertTrue(descriptor.contains(
                "<tab id=\"generalSettingsTab\" caption=\"msg://msgGeneralSettings\""
                        + " margin=\"true\" spacing=\"true\" expand=\"generalScrollBox\">"));
        assertTrue(descriptor.contains(
                "<scrollBox id=\"generalScrollBox\" width=\"100%\" height=\"100%\" spacing=\"true\">"));

        // Split ролей/замещений перенесён внутрь scrollBox: без фиксированной
        // высоты он схлопывался в 0px (expand забирал scrollBox-контент).
        int scrollBoxStart = descriptor.indexOf("<scrollBox id=\"generalScrollBox\"");
        int scrollBoxEnd = descriptor.indexOf("</scrollBox>", scrollBoxStart);
        String scrollBoxBody = descriptor.substring(scrollBoxStart, scrollBoxEnd);
        assertTrue("Split ролей/замещений обязан находиться внутри generalScrollBox",
                scrollBoxBody.contains("<split id=\"rolesSubstSplit\""));
        assertTrue(scrollBoxBody.contains("height=\"300px\""));
    }

    @Test
    public void descriptorIsValidXmlAndEveryOpeningElementIsDocumented() throws Exception {
        Path descriptorPath = projectRoot().resolve(DESCRIPTOR);
        try (InputStream inputStream = Files.newInputStream(descriptorPath)) {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
        }

        List<String> lines = Files.readAllLines(descriptorPath, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
            String trimmed = lines.get(index).trim();
            if (!isOpeningElement(trimmed)) {
                continue;
            }

            int previous = index - 1;
            while (previous >= 0 && lines.get(previous).trim().isEmpty()) {
                previous--;
            }
            assertTrue("Перед opening element на строке " + (index + 1)
                            + " отсутствует смысловой XML-комментарий: " + trimmed,
                    previous >= 0 && lines.get(previous).trim().startsWith("<!--"));
        }
    }

    private void assertAccordion(String descriptor, String id, String collapsed) {
        int start = descriptor.indexOf("id=\"" + id + "\"");
        assertTrue("Не найдена accordion-секция " + id, start >= 0);
        String fragment = descriptor.substring(start, Math.min(descriptor.length(), start + 600));
        assertTrue(fragment.contains("width=\"100%\""));
        assertTrue(fragment.contains("collapsable=\"true\""));
        assertTrue(fragment.contains("collapsed=\"" + collapsed + "\""));
        assertTrue(fragment.contains("showAsPanel=\"true\""));
        assertTrue(fragment.contains("edit-accordion-section"));
    }

    private boolean isOpeningElement(String line) {
        return line.startsWith("<")
                && !line.startsWith("</")
                && !line.startsWith("<?")
                && !line.startsWith("<!--")
                && !line.startsWith("<![CDATA[")
                && !line.startsWith("<!DOCTYPE");
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(
                Files.readAllBytes(projectRoot().resolve(relativePath)),
                StandardCharsets.UTF_8
        );
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
