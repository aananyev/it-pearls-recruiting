package com.company.itpearls.web.screens.employeeworkstatus;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.EmployeeWorkStatus;

@UiController("itpearls_EmployeeWorkStatus.edit")
@UiDescriptor("employee-work-status-edit.xml")
@EditedEntityContainer("employeeWorkStatusDc")
@LoadDataBeforeShow
public class EmployeeWorkStatusEdit extends StandardEditor<EmployeeWorkStatus> {
}