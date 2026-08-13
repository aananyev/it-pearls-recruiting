package com.company.hunttech.web.screens.employeeworkstatus;

import com.company.hunttech.entity.EmployeeWorkStatus;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_EmployeeWorkStatus.edit")
@UiDescriptor("employee-work-status-edit.xml")
@EditedEntityContainer("employeeWorkStatusDc")
@LoadDataBeforeShow
public class EmployeeWorkStatusEdit extends StandardEditor<EmployeeWorkStatus> {
    @Inject
    private CheckBox inStaffField;
    @Inject
    private TextField<String> workStatusNameField;
    @Inject
    private Button mainNav;

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        inStaffField.setValue(inStaffField.getValue() != null ? inStaffField.getValue() : false);
    }

    /**
     * Презентационная навигация: переводит фокус к первому полю «Основных данных»
     * и подсвечивает активный пункт sidebar. Entity, loaders и lifecycle не затрагиваются.
     */
    public void focusMainSection() {
        workStatusNameField.focus();
        setActiveNavigation(mainNav);
    }

    private void setActiveNavigation(Button activeButton) {
        activeButton.addStyleName("label-nav-item-active");
    }
}
