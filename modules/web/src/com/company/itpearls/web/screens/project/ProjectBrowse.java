package com.company.itpearls.web.screens.project;

import com.haulmont.chile.core.model.MetaPropertyPath;
import com.haulmont.cuba.core.global.MessageTools;
import com.haulmont.cuba.core.global.PersistenceHelper;
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
    private CheckBox onlyOpenProjectCheckBox;
    @Inject
    private CollectionLoader<Project> projectsDl;
    @Inject
    private TreeDataGrid<Project> projectsTable;
    @Inject
    private MessageTools messageTools;
    @Inject
    private LookupField columnSelector;

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
    }

    @Install(to = "projectsTable", subject = "rowDescriptionProvider")
    private String projectsTableRowDescriptionProvider(Project project) {
        return Jsoup.parse(project.getProjectDescription() != null ? project.getProjectDescription() : "").text();
    }

    private void setProjectClosedFilter() {
        if (onlyOpenProjectCheckBox.getValue()) {
            projectsDl.removeParameter("projectClosed");
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


}