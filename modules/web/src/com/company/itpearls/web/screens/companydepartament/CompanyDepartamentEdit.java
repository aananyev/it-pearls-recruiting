package com.company.itpearls.web.screens.companydepartament;

import com.company.itpearls.entity.CompanyDepartament;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("itpearls_CompanyDepartament.edit")
@UiDescriptor("company-departament-edit.xml")
@EditedEntityContainer("companyDepartamentDc")
@LoadDataBeforeShow
public class CompanyDepartamentEdit extends StandardEditor<CompanyDepartament> {

    @Inject
    private DataManager dataManager;
    @Inject
    private TabSheet tabSheetDepartment;

    private boolean departamentDescriptionLoaded;
    private boolean templateLetterLoaded;
    private boolean projectsLoaded;

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        if (!PersistenceHelper.isNew(getEditedEntity()) && !departamentDescriptionLoaded) {
            loadDepartamentDescription();
            departamentDescriptionLoaded = true;
        }
    }

    @Subscribe("tabSheetDepartment")
    public void onTabSheetDepartmentSelectedTabChange(TabSheet.SelectedTabChangeEvent event) {
        if (event.getSelectedTab() == null || PersistenceHelper.isNew(getEditedEntity())) {
            return;
        }
        String tabName = event.getSelectedTab().getName();
        if ("tabEditProject".equals(tabName) && !departamentDescriptionLoaded) {
            loadDepartamentDescription();
            departamentDescriptionLoaded = true;
        }
        if ("tabTemplateLetter".equals(tabName) && !templateLetterLoaded) {
            loadTemplateLetter();
            templateLetterLoaded = true;
        }
        if ("tabOpenPosition".equals(tabName) && !projectsLoaded) {
            loadProjects();
            projectsLoaded = true;
        }
    }

    private void loadDepartamentDescription() {
        CompanyDepartament reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(CompanyDepartament.class)
                .add("departamentDescription")
                .build());
        getEditedEntity().setDepartamentDescription(reloaded.getDepartamentDescription());
    }

    private void loadTemplateLetter() {
        CompanyDepartament reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(CompanyDepartament.class)
                .add("templateLetter")
                .build());
        getEditedEntity().setTemplateLetter(reloaded.getTemplateLetter());
    }

    private void loadProjects() {
        CompanyDepartament reloaded = dataManager.reload(getEditedEntity(), ViewBuilder.of(CompanyDepartament.class)
                .add("projectOfDepartment", "project-department-child-view")
                .build());
        getEditedEntity().setProjectOfDepartment(reloaded.getProjectOfDepartment());
    }
}
