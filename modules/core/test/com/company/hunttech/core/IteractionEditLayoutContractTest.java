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
 * Контрактный тест визуального рефакторинга IteractionEdit (HRM HuntTech).
 *
 * Рефакторинг привёл форму к эталону IteractionListEdit (sidebar + workspace,
 * label-навигация, карточки-панели, toolbar, footer справа-внизу) с опорой на
 * общий визуальный API edit-экранов (edit-screen-shared-styles.scss). Бизнес-
 * логика формы не изменялась: loaders, bindings, actions и id контроллера
 * остаются нетронутыми (см. existingBindingsActionsAndIdentifiersRemainIntact).
 */
public class IteractionEditLayoutContractTest {

    private static final String DESCRIPTOR =
            "modules/web/src/com/company/hunttech/web/screens/iteraction/iteraction-edit.xml";
    private static final String CONTROLLER =
            "modules/web/src/com/company/hunttech/web/screens/iteraction/IteractionEdit.java";
    private static final String LOCAL_SCSS =
            "modules/web/themes/hover/com.company.hunttech/iteraction-editor.scss";
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
    public void descriptorUsesSharedEditScreenRoles() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        List<String> requiredStyles = Arrays.asList(
                "iteraction-editor",
                "edit-screen-layout",
                "edit-sidebar",
                "edit-sidebar-visual",
                "edit-sidebar-identity",
                "edit-sidebar-title",
                "edit-sidebar-subtitle",
                "edit-sidebar-spacer",
                "label-navigation",
                "label-nav-title",
                "label-nav-item",
                "edit-workspace",
                "edit-toolbar",
                "edit-toolbar-title",
                "edit-toolbar-description",
                "edit-tabs",
                "edit-card",
                "edit-form-control",
                "edit-footer-actions"
        );

        for (String style : requiredStyles) {
            assertTrue("Не найден общий stylename: " + style,
                    descriptor.contains(style));
        }

        // Двухпанельная компоновка эталона IteractionListEdit.
        assertTrue(descriptor.contains("id=\"iteractionMainLayout\""));
        assertTrue(descriptor.contains("id=\"iteractionSidebar\""));
        assertTrue(descriptor.contains("id=\"iteractionWorkspace\""));
    }

    @Test
    public void navigationTitleUsesSectionStripAndEightItems() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        // Полоса-заголовок «Разделы формы» — класс секции поверх label-nav-title.
        assertEquals(1, countOccurrences(descriptor,
                "stylename=\"label-nav-title iteraction-navigation-title\""));

        // Восемь пунктов навигации — по числу вкладок TabSheet.
        for (String navId : Arrays.asList(
                "typeTabNav", "signsTabNav", "outstaffingNav", "iconsTabNav",
                "notificationsNav", "setupNav", "widgetsNav", "checkTraceNav")) {
            assertTrue("Отсутствует пункт навигации: " + navId,
                    descriptor.contains("id=\"" + navId + "\""));
            assertTrue("Пункт навигации не содержит label-nav-item: " + navId,
                    descriptor.contains("id=\"" + navId + "\""));
        }
        // Активное состояние пункта управляется controller-ом (addStyleName/removeStyleName):
        // в XML статичного active-класса быть не должно, иначе пункт не снимается.
        assertFalse(descriptor.contains("label-nav-item-active iteraction-nav-item-active"));
        assertTrue(descriptor.contains("label-nav-item iteraction-nav-item\""));
        assertTrue(descriptor.contains("label-nav-title iteraction-navigation-title"));
    }

    @Test
    public void cardsRenderAsPanels() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        // Внешние карточки вкладок — v-panel (showAsPanel) с edit-card.
        assertCardIsPanel(descriptor, "typeMainSettingsGroupBox");
        assertCardIsPanel(descriptor, "picGroupBox");
        for (String signCard : Arrays.asList("processStatisticsGroupBox", "otherSignsGroupBox",
                "interviewGroupBox", "reserveGroupBox")) {
            assertCardIsPanel(descriptor, signCard);
        }
        assertCardIsPanel(descriptor, "notificationGroupBox");
        assertCardIsPanel(descriptor, "additionalFieldGroupBox");
        assertCardIsPanel(descriptor, "calendarGroupBox");
        assertCardIsPanel(descriptor, "emailSettingsGroupBox");
        assertCardIsPanel(descriptor, "widgetSetupGroupBox");
        assertCardIsPanel(descriptor, "checkTraceOption");
        assertCardIsPanel(descriptor, "checkTraceTwinGroupBox");

        // Вложенные подкарточки уведомлений — компактный заголовок 15px/42px.
        for (String subCard : Arrays.asList("whenSendGroupBox", "notificationTypeGroupBox",
                "notificationPeriodGroupBox")) {
            assertCardIsPanel(descriptor, subCard);
            int start = descriptor.indexOf("id=\"" + subCard + "\"");
            String fragment = descriptor.substring(start, Math.min(descriptor.length(), start + 400));
            assertTrue("Подкарточка не помечена локальным классом: " + subCard,
                    fragment.contains("iteraction-editor-subcard"));
        }

        // Карточки аутстаффинга — vbox-панели edit-card.
        assertTrue(descriptor.contains("stylename=\"edit-card iteraction-outstaffing-card\""));
    }

    @Test
    public void localScssDefinesDarkSidebarSectionStripAndTabs() throws IOException {
        String scss = readProjectFile(LOCAL_SCSS);

        // Тёмная sidebar 1:1 с эталоном IteractionListEdit.
        assertTrue(scss.contains(".v-slot-iteraction-sidebar"));
        assertTrue(scss.contains("#172638"));
        assertTrue(scss.contains("linear-gradient(180deg, #172638 0%, #132130 58%, #0f1b28 100%)"));

        // Полоса-заголовок контракта §4.1: две inset-линии + жёлтый текст 15px/700.
        assertTrue(scss.contains(".iteraction-navigation-title"));
        assertTrue(scss.contains("min-height: 36px !important"));
        assertTrue(scss.contains("color: #ffb11b !important"));
        assertTrue(scss.contains("font-size: 15px !important"));
        assertTrue(scss.contains("rgba(255, 255, 255, 1) 0 1px 0 0 inset"));

        // Hover пунктов на тёмной sidebar — эталон: белый текст на rgba(255,255,255,.08).
        assertTrue(scss.contains("color: #ffffff !important"));
        assertTrue(scss.contains("background: rgba(255, 255, 255, 0.08) !important"));

        // Активный пункт — #ffb11b (эталон), а не общий $v-selection-color.
        assertTrue(scss.contains("border-left-color: #ffb11b !important"));

        // Вкладки — финальный слой эталона: 48px, 15px/600, нижняя полоса-акцент.
        assertTrue(scss.contains(".iteraction-tabs > .v-tabsheet-tabcontainer"));
        assertTrue(scss.contains("padding: 0 20px"));
        assertTrue(scss.contains("height: 48px"));
        assertTrue(scss.contains("font-size: 15px !important"));
        assertTrue(scss.contains("font-weight: 600 !important"));
        assertTrue(scss.contains("color: #ffb11b !important"));
        assertTrue(scss.contains("border-bottom-color: #ffb11b !important"));
        assertTrue(scss.contains("height: calc(100% - 49px) !important"));

        // Заголовки карточек-панелей — 17px/700/50px (те же значения, что у edit-accordion-section);
        // селектор вложен в блок .iteraction-editor без повторного префикса (иначе scoping даёт дубль).
        assertTrue(scss.contains(".edit-card .v-panel-caption"));
        assertTrue(scss.contains("min-height: 50px"));
        assertTrue(scss.contains("font-size: 17px !important"));

        // Подписи полей — 13px/600, чекбоксы — 14px (1:1 с эталоном).
        assertTrue(scss.contains(".edit-card .v-caption .v-captiontext"));
        assertTrue(scss.contains("font-size: 13px !important"));
        assertTrue(scss.contains(".edit-card .v-checkbox label"));
        assertTrue(scss.contains("font-size: 14px !important"));

        // Глобальных вало-селекторов быть не должно.
        assertFalse(scss.contains("\n  .v-button"));
        assertFalse(scss.contains("\n  .v-label"));
        assertFalse(scss.contains("\n  .v-table"));
    }

    @Test
    public void navigationSwitchesTabsAndKeepsDataUntouched() throws IOException {
        String controller = readProjectFile(CONTROLLER);

        assertTrue(controller.contains("initTabNavigation()"));
        assertTrue(controller.contains("tabSheet.addSelectedTabChangeListener"));
        assertTrue(controller.contains("tabSheet.setSelectedTab("));
        assertTrue(controller.contains("addClickListener"));
        assertTrue(controller.contains("removeStyleName(ACTIVE_NAV_STYLE)"));
        assertTrue(controller.contains("addStyleName(ACTIVE_NAV_STYLE)"));
        // Навигация видна на всех вкладках: скрывающих механизмов быть не должно.
        assertFalse(controller.contains("TABS_WITH_SIDEBAR_NAVIGATION"));
        assertFalse(controller.contains("updateNavigationVisibility("));
        assertFalse(controller.contains("iteractionNavigation.setVisible("));

        // Навигация не трогает данные: ни одного loader/сервиса/commit в методах навигации.
        int navStart = controller.indexOf("private void initTabNavigation");
        int navEnd = controller.indexOf("private void addNotificationWhenSend");
        assertTrue(navStart >= 0 && navEnd > navStart);
        String navigationSection = controller.substring(navStart, navEnd);
        assertFalse(navigationSection.contains("dataManager"));
        assertFalse(navigationSection.contains(".load()"));
        assertFalse(navigationSection.contains(".commit("));
        assertFalse(navigationSection.contains("getItem().set"));
        assertFalse(navigationSection.contains("Service."));
    }

    @Test
    public void existingBindingsActionsAndIdentifiersRemainIntact() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        List<String> protectedContracts = Arrays.asList(
                "dataContainer=\"iteractionDc\"",
                "view=\"iteraction-edit-view\"",
                "id=\"iteractionDl\"",
                "id=\"workStatusDl\"",
                "id=\"iteractionsTreeDl\"",
                "id=\"iteractionElementDl\"",
                "id=\"workStatusDc\"",
                "id=\"iteractionsTreeDc\"",
                "id=\"iteractionElementsDc\"",
                "optionsContainer=\"iteractionsTreeDc\"",
                "optionsContainer=\"workStatusDc\"",
                "optionsContainer=\"iteractionElementsDc\"",
                "select e from hunttech_EmployeeWorkStatus e",
                "select e from hunttech_Iteraction e",
                "id=\"labelItercationName\"",
                "id=\"labelWarning\"",
                "id=\"embeddedPict\"",
                "id=\"tabSheet\"",
                "id=\"tabType\"",
                "id=\"tabSigns\"",
                "id=\"outstaffingTab\"",
                "id=\"tabIcons\"",
                "id=\"tabNotifictions\"",
                "id=\"tabSetup\"",
                "id=\"setupWidgets\"",
                "id=\"checkTrace\"",
                "id=\"iteractionCheckBoxMandatory\"",
                "id=\"iteractionTreeField\"",
                "id=\"iterationNameField\"",
                "id=\"numberField\"",
                "id=\"iteractionFieldPic\"",
                "id=\"signEndCaseCheckBox\"",
                "id=\"statisticsCheckBox\"",
                "id=\"signPriorityNews\"",
                "id=\"signViewOnlyManagersCheckBox\"",
                "id=\"signOurInterviewAssignedCheckBox\"",
                "id=\"signOurInterviewCheckBox\"",
                "id=\"signClientInterviewCheckBox\"",
                "id=\"signSentToClientCheckBox\"",
                "id=\"outstaffingSign\"",
                "id=\"staffInteractionStatusRadioButtons\"",
                "id=\"workStatusPickerField\"",
                "id=\"signStartProject\"",
                "id=\"signEndProject\"",
                "id=\"checkBoxCallDialog\"",
                "id=\"textFieldCallButtonText\"",
                "id=\"textFieldCallForm\"",
                "id=\"notificationNeedSendCheckBox\"",
                "id=\"notificationSetupHBox\"",
                "id=\"whenSendMessageRadioButton\"",
                "id=\"radioButtonTypeNotifications\"",
                "id=\"lookupFieldEmails\"",
                "id=\"notificationPeriodRadioButton\"",
                "id=\"dayBeforeAfterTextField\"",
                "id=\"checkBoxFlag\"",
                "id=\"radioButtonAddType\"",
                "id=\"checkBoxSetDefaultDateTime\"",
                "id=\"textFieldCaption\"",
                "id=\"checkBoxCalendar\"",
                "id=\"textFieldCalendarItemStyle\"",
                "id=\"calendarItemDescriptionTextField\"",
                "id=\"neetToSendEmailCheckBox\"",
                "id=\"neetSendMemoCheckBox\"",
                "id=\"textEmailToSendRichTextArea\"",
                "id=\"commentKeysRichTextArea\"",
                "id=\"myJobCandidatesWingetCheckBox\"",
                "id=\"widgetSetupGroupBox\"",
                "id=\"widgetPausedTab\"",
                "id=\"widgetGraph\"",
                "id=\"typeTraceRadioButtons\"",
                "id=\"checkTraceTwinColumn\"",
                "action=\"windowCommitAndClose\"",
                "action=\"windowClose\""
        );

        for (String contract : protectedContracts) {
            assertTrue("Нарушен защищённый XML-контракт: " + contract,
                    descriptor.contains(contract));
        }

        // Поле номера — редактируемое бизнес-поле вкладки «Тип взаимодействия» (не readonly,
        // не служебное поле sidebar; возвращено из sidebar во вкладку 1, 11.08.2026).
        int numberStart = descriptor.indexOf("id=\"numberField\"");
        assertTrue(numberStart >= 0);
        String numberFragment = descriptor.substring(numberStart,
                Math.min(descriptor.length(), numberStart + 200));
        assertFalse(numberFragment.contains("readonly=\"true\""));
        assertTrue("Номер обязан быть во вкладке tabType, а не в sidebar",
                descriptor.indexOf("id=\"numberField\"") > descriptor.indexOf("id=\"tabType\""));
    }

    @Test
    public void warningStaysInSidebar() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        int sidebarStart = descriptor.indexOf("<vbox id=\"iteractionSidebar\"");
        int sidebarEnd = descriptor.indexOf("<vbox id=\"iteractionWorkspace\"");
        assertTrue(sidebarStart >= 0 && sidebarEnd > sidebarStart);
        String sidebar = descriptor.substring(sidebarStart, sidebarEnd);

        assertFalse("Поле номера не должно оставаться в sidebar",
                sidebar.contains("id=\"numberField\""));
        assertTrue("Предупреждение администратора обязано быть в sidebar",
                sidebar.contains("id=\"labelWarning\""));
        assertTrue(sidebar.contains("iteraction-editor-warning"));
    }

    @Test
    public void localScssIsIdenticalAndConnectedAfterSharedApiInAllThemes() throws IOException {
        String referenceScss = null;
        for (String theme : THEMES) {
            String scss = readProjectFile(
                    "modules/web/themes/" + theme
                            + "/com.company.hunttech/iteraction-editor.scss");
            if (referenceScss == null) {
                referenceScss = scss;
            } else {
                assertEquals("Локальный IteractionEdit SCSS различается между темами",
                        referenceScss, scss);
            }

            String styles = readProjectFile("modules/web/themes/" + theme + "/styles.scss");
            int sharedImport = styles.indexOf(
                    "@import \"com.company.hunttech/edit-screen-shared-styles\";");
            int screenImport = styles.indexOf(
                    "@import \"com.company.hunttech/iteraction-editor\";");
            int sharedInclude = styles.indexOf("@include edit-screen-shared-styles;");
            int screenInclude = styles.indexOf("@include iteraction-editor-theme;");

            assertTrue(sharedImport >= 0 && screenImport > sharedImport);
            assertTrue(sharedInclude >= 0 && screenInclude > sharedInclude);
        }

        assertNotNull(referenceScss);
        assertTrue(referenceScss.contains(".iteraction-editor"));
        assertTrue(referenceScss.contains("#172638"));
        assertTrue(referenceScss.contains("#ffb11b"));
        assertFalse(referenceScss.contains("\n  .v-button"));
        assertFalse(referenceScss.contains("\n  .v-label"));
        assertFalse(referenceScss.contains("\n  .v-table"));
    }

    @Test
    public void tabsheetFollowsIteractionListEditReference() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        assertTrue(descriptor.contains("stylename=\"iteraction-tabs edit-tabs\""));
        assertFalse("framed-рендер вало ломает эталонный стиль вкладок",
                descriptor.contains("stylename=\"framed"));
    }

    @Test
    public void footerPushesActionsToBottomRightCorner() throws IOException {
        String descriptor = readProjectFile(DESCRIPTOR);

        assertTrue(descriptor.contains("id=\"editActions\""));
        assertTrue(descriptor.contains("expand=\"editActionsSpacer\""));
        assertTrue(descriptor.contains("id=\"editActionsSpacer\""));
        assertTrue(descriptor.contains("id=\"editActionsGroup\""));
        assertTrue(descriptor.contains("width=\"AUTO\""));
        assertTrue(descriptor.contains("align=\"MIDDLE_RIGHT\""));
        assertTrue(descriptor.contains("iteraction-footer-actions"));
        assertTrue(descriptor.contains("iteraction-primary-action"));
        assertTrue(descriptor.contains("iteraction-secondary-action"));
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

    private void assertCardIsPanel(String descriptor, String id) {
        int start = descriptor.indexOf("id=\"" + id + "\"");
        assertTrue("Не найдена карточка " + id, start >= 0);
        String fragment = descriptor.substring(start, Math.min(descriptor.length(), start + 500));
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
