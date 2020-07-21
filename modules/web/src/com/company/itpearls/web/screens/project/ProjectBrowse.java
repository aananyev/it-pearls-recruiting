package com.company.itpearls.web.screens.project;

import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Project;

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
}