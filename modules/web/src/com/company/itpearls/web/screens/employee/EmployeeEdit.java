package com.company.itpearls.web.screens.employee;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.Employee;

import javax.inject.Inject;
import java.math.BigDecimal;

@UiController("itpearls_Employee.edit")
@UiDescriptor("employee-edit.xml")
@EditedEntityContainer("employeeDc")
@LoadDataBeforeShow
public class EmployeeEdit extends StandardEditor<Employee> {

    @Inject
    private TextField<BigDecimal> salaryTextField;
    @Inject
    private Notifications notifications;
    @Inject
    private MessageBundle messageBundle;

    @Install(to = "openPositionField", subject = "optionIconProvider")
    private String openPositionFieldOptionIconProvider(OpenPosition openPosition) {
        if (!openPosition.getOpenClose()) {
            return CubaIcon.PLUS_CIRCLE.source();
        } else {
            return CubaIcon.MINUS_CIRCLE.source();
        }
    }

    @Install(to = "fullCostsField", subject = "validator")
    private void fullCostsFieldValidator(BigDecimal bigDecimal) {
        if (bigDecimal.compareTo(salaryTextField.getValue()) < 0) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withType(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgFullCostLessSalary"))
                    .show();
        }
    }
}
