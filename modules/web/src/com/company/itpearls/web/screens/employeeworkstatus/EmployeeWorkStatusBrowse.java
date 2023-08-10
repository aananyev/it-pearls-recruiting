package com.company.itpearls.web.screens.employeeworkstatus;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.EmployeeWorkStatus;

@UiController("itpearls_EmployeeWorkStatus.browse")
@UiDescriptor("employee-work-status-browse.xml")
@LookupComponent("employeeWorkStatusesTable")
@LoadDataBeforeShow
public class EmployeeWorkStatusBrowse extends StandardLookup<EmployeeWorkStatus> {
}