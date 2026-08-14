package com.company.hunttech.web.screens.companydepartament;

import com.company.hunttech.entity.CompanyDepartament;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.ViewBuilder;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@UiController("hunttech_CompanyDepartament.edit")
@UiDescriptor("company-departament-edit.xml")
@EditedEntityContainer("companyDepartamentDc")
@LoadDataBeforeShow
public class CompanyDepartamentEdit extends StandardEditor<CompanyDepartament> {

    @Inject
    private DataManager dataManager;
    @Inject
    private TabSheet tabSheetDepartment;
    @Inject
    private Messages messages;
    @Inject
    private Label companyDepartamentSidebarTitle;
    @Inject
    private Button companyDepartamentNavMain;
    @Inject
    private Button companyDepartamentNavProjects;
    @Inject
    private Button companyDepartamentNavTemplate;

    private boolean departamentDescriptionLoaded;
    private boolean templateLetterLoaded;
    private boolean projectsLoaded;

    /** Соответствие «имя вкладки TabSheet → пункт label-навигации». */
    private static final Map<String, String> TAB_TO_NAV_BUTTON =
            Collections.unmodifiableMap(new HashMap<String, String>() {{
                put("tabEditProject", "companyDepartamentNavMain");
                put("tabOpenPosition", "companyDepartamentNavProjects");
                put("tabTemplateLetter", "companyDepartamentNavTemplate");
            }});

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

    // ===== Presentation-only: sidebar-навигация «Разделы» (контракт Edit-форм) =====

    @Subscribe
    public void onBeforeShowSidebar(BeforeShowEvent event) {
        // Динамический title sidebar: название департамента, иначе общий заголовок формы.
        if (getEditedEntity().getDepartamentRuName() != null) {
            companyDepartamentSidebarTitle.setValue(getEditedEntity().getDepartamentRuName());
        } else {
            companyDepartamentSidebarTitle.setValue(messages.getMessage(getClass(), "browseCaption"));
        }
    }

    @Subscribe("companyDepartamentNavMain")
    public void onCompanyDepartamentNavMainClick(Button.ClickEvent event) {
        setNavigationActive(companyDepartamentNavMain);
        tabSheetDepartment.setSelectedTab("tabEditProject");
    }

    @Subscribe("companyDepartamentNavProjects")
    public void onCompanyDepartamentNavProjectsClick(Button.ClickEvent event) {
        setNavigationActive(companyDepartamentNavProjects);
        tabSheetDepartment.setSelectedTab("tabOpenPosition");
    }

    @Subscribe("companyDepartamentNavTemplate")
    public void onCompanyDepartamentNavTemplateClick(Button.ClickEvent event) {
        setNavigationActive(companyDepartamentNavTemplate);
        tabSheetDepartment.setSelectedTab("tabTemplateLetter");
    }

    @Subscribe("tabSheetDepartment")
    public void onTabSheetDepartmentSelectedTabChangeNav(TabSheet.SelectedTabChangeEvent event) {
        // Отдельный обработчик смены вкладки: синхронизирует активный пункт
        // sidebar-навигации; бизнес-логика ленивой загрузки — в основном методе.
        updateActiveNavigation(event.getSelectedTab());
    }

    private void updateActiveNavigation(TabSheet.Tab selectedTab) {
        if (selectedTab == null) {
            return;
        }
        String navButtonId = TAB_TO_NAV_BUTTON.get(selectedTab.getName());
        if (navButtonId == null) {
            return;
        }
        switch (navButtonId) {
            case "companyDepartamentNavMain":
                setNavigationActive(companyDepartamentNavMain);
                break;
            case "companyDepartamentNavProjects":
                setNavigationActive(companyDepartamentNavProjects);
                break;
            case "companyDepartamentNavTemplate":
                setNavigationActive(companyDepartamentNavTemplate);
                break;
            default:
                break;
        }
    }

    private void resetNavigationActiveStyles() {
        companyDepartamentNavMain.removeStyleName("label-nav-item-active");
        companyDepartamentNavProjects.removeStyleName("label-nav-item-active");
        companyDepartamentNavTemplate.removeStyleName("label-nav-item-active");
    }

    private void setNavigationActive(Button activeButton) {
        resetNavigationActiveStyles();
        activeButton.addStyleName("label-nav-item-active");
    }
}
