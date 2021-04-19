package com.company.itpearls.web.screens.project;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;
import org.jsoup.Jsoup;

import javax.inject.Inject;

@UiController("itpearls_Project.browse")
@UiDescriptor("project-browse.xml")
@LookupComponent("projectsTable")
@LoadDataBeforeShow
public class ProjectBrowse extends StandardLookup<Project> {
    @Inject
    private CheckBox onlyOpenProjectCheckBox;
    @Inject
    private CollectionLoader<Project> projectsDl;

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