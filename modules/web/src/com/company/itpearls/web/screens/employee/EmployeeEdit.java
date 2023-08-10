package com.company.itpearls.web.screens.employee;

import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Employee;

@UiController("itpearls_Employee.edit")
@UiDescriptor("employee-edit.xml")
@EditedEntityContainer("employeeDc")
@LoadDataBeforeShow
public class EmployeeEdit extends StandardEditor<Employee> {
}
