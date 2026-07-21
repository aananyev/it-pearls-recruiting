package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.Position;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.KeyValueCollectionLoader;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

public class JobCandidateEditComponentRendererTest {

    private static final Path SCREEN_DESCRIPTOR = Paths.get(
            "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml");
    private static final Path SCREEN_CONTROLLER = Paths.get(
            "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java");
    private static final List<String> JOB_CANDIDATE_THEMES = Arrays.asList(
            "halo", "havana", "helium", "hover",
            "hunttech-modern", "hunttech-modern-light", "hunttech-modern-dark");

    @Test
    public void initConfiguresComponentRenderersBeforeFirstClientResponse() throws Exception {
        TrackingJobCandidateEdit screen = new TrackingJobCandidateEdit();
        setField(screen, "openPositionDl", mock(CollectionLoader.class));
        setField(screen, "citiesDl", mock(CollectionLoader.class));
        setField(screen, "personPositionsLc", mock(CollectionLoader.class));
        setField(screen, "lastProjectDl", mock(KeyValueCollectionLoader.class));
        setField(screen, "suggestOpenPositionDl", mock(CollectionLoader.class));
        setField(screen, "interactionCommentDl", mock(CollectionLoader.class));
        setField(screen, "tabSheetSocialNetworks", mock(TabSheet.class));

        screen.onInit(null);

        assertEquals(1, screen.configurationCalls);
    }

    @Test
    public void everyComponentRendererDeclaredInXmlHasAnEarlyGenerator() throws Exception {
        TrackingJobCandidateEdit screen = new TrackingJobCandidateEdit();

        screen.configureAvailableComponentRenderers();

        assertEquals(componentRendererColumnsFromDescriptor(), screen.configuredColumns);
    }

    @Test
    public void everyRequiredRuntimeComponentExistsInDescriptor() throws Exception {
        Document document = parseScreenDescriptor();
        Set<String> descriptorComponentIds = new LinkedHashSet<>();
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (element.hasAttribute("id")) {
                descriptorComponentIds.add(element.getAttribute("id"));
            }
        }

        String controllerSource = new String(Files.readAllBytes(SCREEN_CONTROLLER), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("getComponentNN\\s*\\(\\s*\"([^\"]+)\"\\s*\\)")
                .matcher(controllerSource);
        Set<String> missingComponentIds = new LinkedHashSet<>();
        while (matcher.find()) {
            String componentId = matcher.group(1);
            if (!descriptorComponentIds.contains(componentId)) {
                missingComponentIds.add(componentId);
            }
        }

        assertTrue("Required components missing from job-candidate-edit.xml: " + missingComponentIds,
                missingComponentIds.isEmpty());
    }

    @Test
    public void sidebarCreateActionsPrecedeHrMaster() throws Exception {
        Document document = parseScreenDescriptor();
        Element footer = findElementById(document, "candidateProfileFooter");
        List<String> buttonIds = new ArrayList<>();

        NodeList children = footer.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && "button".equals(node.getNodeName())) {
                Element button = (Element) node;
                buttonIds.add(button.getAttribute("id"));
                assertEquals("100%", button.getAttribute("width"));
                assertEquals("small", button.getAttribute("stylename"));
            }
        }

        assertEquals(Arrays.asList(
                "createCandidateCvButton",
                "createCandidateIteractionButton",
                "openPositionMasterBrowseButton"), buttonIds);
        assertSidebarAction(footer, "createCandidateCvButton",
                "Создать резюме", "createCandidateCv");
        assertSidebarAction(footer, "createCandidateIteractionButton",
                "Создать взаимодействие", "createCandidateIteraction");
        assertNotNull(JobCandidateEdit.class.getMethod("createCandidateCv"));
        assertNotNull(JobCandidateEdit.class.getMethod("createCandidateIteraction"));
    }

    @Test
    public void candidatePictureUsesOvalFallbackComponent() throws Exception {
        Document document = parseScreenDescriptor();
        Element candidatePic = findElementById(document, "candidatePic");

        assertEquals("ovaFallbackImage", candidatePic.getNodeName());
        assertEquals("176px", candidatePic.getAttribute("width"));
        assertEquals("176px", candidatePic.getAttribute("height"));
        assertEquals("176px", candidatePic.getAttribute("ovalWidth"));
        assertEquals("176px", candidatePic.getAttribute("ovalHeight"));
        assertEquals("icons/no-programmer.jpeg", candidatePic.getAttribute("fallbackThemePath"));
        assertEquals("jobCandidateDc", candidatePic.getAttribute("dataContainer"));
        assertEquals("fileImageFace", candidatePic.getAttribute("property"));
        assertNull(findElementByIdOrNull(document, "candidateDefaultPic"));
    }

    @Test
    public void candidateProfileShowsNameAndPosition() throws Exception {
        JobCandidateEdit screen = new JobCandidateEdit();
        Label<String> fullName = mock(Label.class);
        Label<String> positionLabel = mock(Label.class);
        JobCandidate candidate = mock(JobCandidate.class);
        Position position = mock(Position.class);

        setField(screen, "fullNameField", fullName);
        setField(screen, "personPositionLabel", positionLabel);
        when(candidate.getValue("secondName")).thenReturn("Иванов");
        when(candidate.getValue("firstName")).thenReturn("Иван");
        when(candidate.getValue("personPosition")).thenReturn(position);
        when(position.getPositionRuName()).thenReturn("Java-разработчик");

        screen.updateCandidateProfileLabels(candidate);

        verify(fullName).setValue("Иванов Иван");
        verify(positionLabel).setValue("Java-разработчик");
    }

    @Test
    public void candidateNameKeepsProfileStyleWhenBlockStateChanges() throws Exception {
        JobCandidateEdit screen = new JobCandidateEdit();
        Label<String> fullName = mock(Label.class);
        setField(screen, "fullNameField", fullName);

        screen.updateFullNameStyle(true);

        verify(fullName).setStyleName("job-candidate-profile-name");
        verify(fullName).addStyleName("h2-red");
    }

    @Test
    public void candidateProfileHandlesNullAndDetachedPosition() throws Exception {
        JobCandidateEdit screen = new JobCandidateEdit();
        Label<String> fullName = mock(Label.class);
        Label<String> positionLabel = mock(Label.class);
        JobCandidate candidate = mock(JobCandidate.class);

        setField(screen, "fullNameField", fullName);
        setField(screen, "personPositionLabel", positionLabel);
        when(candidate.getValue("personPosition"))
                .thenThrow(new IllegalStateException("Cannot get unfetched attribute from detached object"));

        screen.updateCandidateProfileLabels(candidate);

        verify(fullName).setValue("");
        verify(positionLabel).setValue("");
        assertEquals("", JobCandidateEdit.resolvePersonPositionLabel(null));
    }

    @Test
    public void detachedPositionNameDoesNotBreakProfile() {
        Position position = mock(Position.class);
        when(position.getPositionRuName())
                .thenThrow(new IllegalStateException("Cannot get unfetched attribute from detached object"));

        assertEquals("", JobCandidateEdit.resolvePersonPositionLabel(position));
    }

    @Test
    public void cardCompletionCountsAllInputPropertiesWithoutLazyComponents() {
        JobCandidate candidate = mock(JobCandidate.class);
        when(candidate.getValue(anyString())).thenReturn("filled");

        assertEquals(15, JobCandidateEdit.CARD_COMPLETION_PROPERTIES.size());
        assertEquals(15, JobCandidateEdit.countFilledCardFields(candidate));
        assertEquals(100, JobCandidateEdit.calculateCardCompletionPercentage(candidate));
    }

    @Test
    public void cardCompletionTreatsNullBlankAndDetachedValuesAsEmpty() {
        JobCandidate emptyCandidate = mock(JobCandidate.class);
        when(emptyCandidate.getValue("firstName")).thenReturn("   ");
        assertEquals(0, JobCandidateEdit.calculateCardCompletionPercentage(null));
        assertEquals(0, JobCandidateEdit.calculateCardCompletionPercentage(emptyCandidate));

        JobCandidate detachedCandidate = mock(JobCandidate.class);
        when(detachedCandidate.getValue(anyString())).thenReturn("filled");
        when(detachedCandidate.getValue("personPosition"))
                .thenThrow(new IllegalStateException("Cannot get unfetched attribute from detached object"));

        assertEquals(14, JobCandidateEdit.countFilledCardFields(detachedCandidate));
        assertEquals(93, JobCandidateEdit.calculateCardCompletionPercentage(detachedCandidate));
    }

    @Test
    public void cardCompletionPropertiesMatchAllVisibleCandidateInputs() throws Exception {
        Document document = parseScreenDescriptor();
        Set<String> inputTags = new LinkedHashSet<>(Arrays.asList(
                "textField", "suggestionField", "suggestionPickerField",
                "lookupPickerField", "dateField", "radioButtonGroup"));
        Set<String> descriptorProperties = new LinkedHashSet<>();
        NodeList elements = document.getElementsByTagName("*");

        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (inputTags.contains(element.getNodeName())
                    && "jobCandidateDc".equals(element.getAttribute("dataContainer"))
                    && !"false".equals(element.getAttribute("visible"))) {
                descriptorProperties.add(element.getAttribute("property"));
            }
        }

        assertEquals(new LinkedHashSet<>(JobCandidateEdit.CARD_COMPLETION_PROPERTIES),
                descriptorProperties);
    }

    @Test
    public void candidateNameFieldsUseTheSameExpandedWidth() throws Exception {
        Document document = parseScreenDescriptor();

        for (String fieldId : Arrays.asList("firstNameField", "middleNameField", "secondNameField")) {
            Element field = findElementById(document, fieldId);
            Element row = (Element) field.getParentNode();
            Element caption = firstDirectChild(row, "label");

            assertEquals("hbox", row.getNodeName());
            assertEquals(fieldId, row.getAttribute("expand"));
            assertTrue(row.getAttribute("stylename").contains("job-candidate-name-row"));
            assertEquals("100%", field.getAttribute("width"));
            assertEquals("96px", caption.getAttribute("width"));
        }
    }

    @Test
    public void candidatePositionIsVisibleInEveryTheme() throws Exception {
        for (String theme : JOB_CANDIDATE_THEMES) {
            Path stylesheet = Paths.get("modules/web/themes", theme,
                    "com.company.hunttech/job-candidate-editor.scss");
            String source = new String(Files.readAllBytes(stylesheet), StandardCharsets.UTF_8);

            assertTrue(theme + " does not scope the candidate position to the sidebar",
                    source.contains(".job-candidate-sidebar .job-candidate-profile-position"));
            assertFalse(theme + " still hides the candidate position",
                    Pattern.compile("job-candidate-profile-position\\s*\\{[^}]*display:\\s*none",
                            Pattern.DOTALL).matcher(source).find());

            String primarySize = theme.startsWith("hunttech-modern")
                    ? ("hunttech-modern".equals(theme) ? "12px" : "10px")
                    : "19px";
            String sidebarSize = theme.startsWith("hunttech-modern") ? "11px" : "13px";
            String positionSize = theme.startsWith("hunttech-modern")
                    ? ("hunttech-modern".equals(theme) ? "11px" : "9px")
                    : "14px";

            assertTrue(theme + " does not define shared name/rating typography",
                    source.contains("$job-candidate-primary-label-font-size: " + primarySize));
            assertTrue(theme + " does not center the candidate name",
                    Pattern.compile("job-candidate-profile-name\\s*\\{[^}]*text-align:\\s*center",
                            Pattern.DOTALL).matcher(source).find());
            assertTrue(theme + " does not apply the shared size to name and rating",
                    countOccurrences(source,
                            "font-size: $job-candidate-primary-label-font-size") >= 2);
            if (theme.startsWith("hunttech-modern")) {
                assertTrue(theme + " overrides the rating size instead of sharing the name size",
                        Pattern.compile("job-candidate-status \\.h3\\s*\\{[^}]*font-size:\\s*"
                                        + "\\$job-candidate-primary-label-font-size",
                                Pattern.DOTALL).matcher(source).find());
            }
            assertTrue(theme + " does not reduce the sidebar label baseline",
                    Pattern.compile("job-candidate-sidebar \\.v-label\\s*\\{[^}]*font-size:\\s*"
                                    + Pattern.quote(sidebarSize), Pattern.DOTALL)
                            .matcher(source).find());
            assertTrue(theme + " does not reduce the position label",
                    Pattern.compile("job-candidate-profile-position\\s*\\{[^}]*font-size:\\s*"
                                    + Pattern.quote(positionSize), Pattern.DOTALL)
                            .matcher(source).find());
        }
    }

    private static Set<String> componentRendererColumnsFromDescriptor() throws Exception {
        Document document = parseScreenDescriptor();
        Set<String> columns = new LinkedHashSet<>();

        NodeList dataGrids = document.getElementsByTagName("dataGrid");
        for (int gridIndex = 0; gridIndex < dataGrids.getLength(); gridIndex++) {
            Element dataGrid = (Element) dataGrids.item(gridIndex);
            NodeList gridColumns = dataGrid.getElementsByTagName("column");
            for (int columnIndex = 0; columnIndex < gridColumns.getLength(); columnIndex++) {
                Element column = (Element) gridColumns.item(columnIndex);
                if (column.getElementsByTagName("componentRenderer").getLength() > 0) {
                    columns.add(dataGrid.getAttribute("id") + "." + column.getAttribute("id"));
                }
            }
        }

        return columns;
    }

    private static Document parseScreenDescriptor() throws Exception {
        return DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(SCREEN_DESCRIPTOR.toFile());
    }

    private static Element findElementById(Document document, String id) {
        Element element = findElementByIdOrNull(document, id);
        if (element != null) {
            return element;
        }
        throw new AssertionError("Component not found: " + id);
    }

    private static Element findElementByIdOrNull(Document document, String id) {
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        return null;
    }

    private static Element firstDirectChild(Element parent, String nodeName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && nodeName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        throw new AssertionError("Child component not found: " + nodeName);
    }

    private static int countOccurrences(String source, String value) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }

    private static void assertSidebarAction(Element footer,
                                            String id,
                                            String caption,
                                            String invoke) {
        Element button = findElementById(footer.getOwnerDocument(), id);
        assertEquals(caption, button.getAttribute("caption"));
        assertEquals(invoke, button.getAttribute("invoke"));
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = JobCandidateEdit.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class TrackingJobCandidateEdit extends JobCandidateEdit {
        private final Set<String> configuredColumns = new LinkedHashSet<>();
        private int configurationCalls;

        @Override
        void configureAvailableComponentRenderers() {
            configurationCalls++;
            super.configureAvailableComponentRenderers();
        }

        @Override
        <E extends Entity> void configureComponentRenderer(
                String dataGridId,
                String columnId,
                DataGrid.GenericColumnGenerator<E, Object> generator) {
            assertNotNull(generator);
            configuredColumns.add(dataGridId + "." + columnId);
        }
    }
}
