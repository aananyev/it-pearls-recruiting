package com.company.hunttech.web.screens.employeeworkstatus;

import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.EmployeeWorkStatus;

@UiController("hunttech_EmployeeWorkStatus.browse")
@UiDescriptor("employee-work-status-browse.xml")
@LookupComponent("employeeWorkStatusesTable")
@LoadDataBeforeShow
public class EmployeeWorkStatusBrowse extends StandardLookup<EmployeeWorkStatus> {
}