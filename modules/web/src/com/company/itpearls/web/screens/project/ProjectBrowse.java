package com.company.itpearls.web.screens.project;

import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;
import com.haulmont.cuba.gui.screen.LookupComponent;
import org.jsoup.Jsoup;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UiController("itpearls_Project.browse")
@UiDescriptor("project-browse.xml")
@LookupComponent("projectsTable")
@LoadDataBeforeShow
public class ProjectBrowse extends StandardLookup<Project> {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private CheckBox onlyOpenProjectCheckBox;
    @Inject
    private CollectionLoader<Project> projectsDl;
    @Inject
    private TreeDataGrid<Project> projectsTable;
    @Inject
    private MessageTools messageTools;
    @Inject
    private LookupField columnSelector;
    @Inject
    private CheckBox withOpenPositionCheckBox;

    @Install(to = "projectsTable.projectLogoColumn", subject = "columnGenerator")
    private Object projectsTableProjectLogoColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<Project> event) {
        HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
        retBox.setWidthFull();
        retBox.setHeightFull();

        Image image = uiComponents.create(Image.class);
        image.setDescriptionAsHtml(true);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        image.setWidth("20px");
        image.setHeight("20px");
        image.setStyleName("icon-no-border-20px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setDescription("<h4>"
                + event.getItem().getProjectName()
                + "</h4><br><br>"
                + event.getItem().getProjectDescription());

        if (event.getItem().getProjectLogo() != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(event
                            .getItem()
                            .getProjectLogo());
        } else {
            image.setSource(ThemeResource.class).setPath("icons/no-company.png");
        }

        retBox.add(image);
        return retBox;
    }

    @Subscribe
    public void onInit(InitEvent event) {
        initColumnSelector();
    }

    private void initColumnSelector() {
        List<DataGrid.Column<Project>> columns = projectsTable.getColumns();
        Map<String, String> columnsMap = columns.stream()
                .collect(Collectors.toMap(
                        column -> {
                            MetaPropertyPath propertyPath = column.getPropertyPath();
                            return propertyPath != null
                                    ? messageTools.getPropertyCaption(propertyPath.getMetaProperty())
                                    : column.getId();
                        },
                        DataGrid.Column::getId,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));
        columnSelector.setOptionsMap(columnsMap);

        columnSelector.setValue(columns.get(0).getId());
    }

    @Subscribe("columnSelector")
    protected void onColumnSelectorValueChange(HasValue.ValueChangeEvent<String> event) {
        projectsTable.setHierarchyColumn(event.getValue());
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        onlyOpenProjectCheckBox.setValue(false);
        setProjectClosedFilter();

        withOpenPositionCheckBox.setValue(true);
    }

    @Install(to = "projectsTable", subject = "rowDescriptionProvider")
    private String projectsTableRowDescriptionProvider(Project project) {
        return Jsoup.parse(project.getProjectDescription() != null ? project.getProjectDescription() : "").text();
    }

    private void setProjectClosedFilter() {
        if (onlyOpenProjectCheckBox.getValue()) {
            projectsDl.removeParameter("projectClosed");
            withOpenPositionCheckBox.setValue(false);
        } else {
            projectsDl.setParameter("projectClosed", false);
        }

        projectsDl.load();
    }

    @Subscribe("onlyOpenProjectCheckBox")
    public void onOnlyOpenProjectCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        setProjectClosedFilter();
    }

    @Install(to = "projectsTable.iconProjectDesc", subject = "columnGenerator")
    private Icons.Icon projectsTableIconProjectDescColumnGenerator(DataGrid.ColumnGeneratorEvent<Project> event) {
        if(event.getItem().getProjectDescription() != null) {
            return CubaIcon.PLUS_CIRCLE;
        } else {
            return CubaIcon.MINUS_CIRCLE;
        }
    }

    @Install(to = "projectsTable.iconProjectDesc", subject = "styleProvider")
    private String projectsTableIconProjectDescStyleProvider(Project project) {
        if(project.getProjectDescription() != null) {
            return "pic-center-large-green";
        } else {
            return "pic-center-large-red";
        }
    }

    @Install(to = "projectsTable.iconProjectDesc", subject = "descriptionProvider")
    private String projectsTableIconProjectDescDescriptionProvider(Project project) {
        if(project.getProjectDescription() != null) {
            return Jsoup.parse(project.getProjectDescription()).text();
        } else {
            return null;
        }
    }

    @Subscribe("withOpenPositionCheckBox")
    public void onWithOpenPositionCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (event.getValue()) {
            projectsDl.setParameter("withOpenPosition", true);
            onlyOpenProjectCheckBox.setValue(false);
        } else {
            projectsDl.removeParameter("withOpenPosition");
        }

        projectsDl.load();
    }
}