package com.company.itpearls.web.screens.employeeworkstatus;

import com.haulmont.cuba.gui.components.CheckBox;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.EmployeeWorkStatus;

import javax.inject.Inject;

@UiController("itpearls_EmployeeWorkStatus.edit")
@UiDescriptor("employee-work-status-edit.xml")
@EditedEntityContainer("employeeWorkStatusDc")
@LoadDataBeforeShow
public class EmployeeWorkStatusEdit extends StandardEditor<EmployeeWorkStatus> {
    @Inject
    private CheckBox inStaffField;

    @Subscribe
    public void onBeforeCommitChanges(BeforeCommitChangesEvent event) {
        inStaffField.setValue(inStaffField.getValue() != null ? inStaffField.getValue() : false);
    }
}