package com.company.hunttech.web.screens.jobcandidate;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.components.DataGrid;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class JobCandidateEditComponentRendererTest {

    private static final Path SCREEN_DESCRIPTOR = Paths.get(
            "modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml");
    private static final Path SCREEN_CONTROLLER = Paths.get(
            "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateEdit.java");

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
        NodeList elements = document.getElementsByTagName("*");
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (id.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        throw new AssertionError("Component not found: " + id);
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
